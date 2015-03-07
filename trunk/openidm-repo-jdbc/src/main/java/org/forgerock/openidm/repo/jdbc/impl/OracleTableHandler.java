/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012-2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * $Id$
 */
package org.forgerock.openidm.repo.jdbc.impl;

import static org.forgerock.openidm.repo.QueryConstants.PAGED_RESULTS_OFFSET;
import static org.forgerock.openidm.repo.QueryConstants.PAGE_SIZE;
import static org.forgerock.openidm.repo.QueryConstants.SORT_KEYS;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.SortKey;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OracleTableHandler extends GenericTableHandler {
    final static Logger logger = LoggerFactory.getLogger(OracleTableHandler.class);

    public OracleTableHandler(JsonValue tableConfig, String dbSchemaName, JsonValue queriesConfig, JsonValue commandsConfig,
            int maxBatchSize, SQLExceptionHandler sqlExceptionHandler) {
        super(tableConfig, dbSchemaName, queriesConfig, commandsConfig, maxBatchSize,
                new GenericSQLQueryFilterVisitor() {
                    @Override
                    String getPropTypeValueClause(String operand, String placeholder, Object valueAssertion) {
                        // validate type is integer or double cast all numeric types to decimal
                        if (isNumeric(valueAssertion)) {
                            return "(prop.proptype = 'java.lang.Integer' OR prop.proptype = 'java.lang.Double') "
                                    + "AND TO_NUMBER(prop.propvalue) " + operand + " ${" + placeholder + "}";
                        } else if (isBoolean(valueAssertion)) {
                            // validate type is boolean if valueAssertion is a boolean
                            return "prop.proptype = 'java.lang.Boolean' AND prop.propvalue " + operand + " ${" + placeholder + "}";
                        } else {
                            // assume String
                            return "prop.propvalue " + operand + " ${" + placeholder + "}";
                        }
                    }
                }, sqlExceptionHandler);
    }

    @Override
    public void create(String fullId, String type, String localId, Map<String, Object> obj, Connection connection)
            throws SQLException, IOException, InternalServerErrorException {
        connection.setAutoCommit(true);
        long typeId = getTypeId(type, connection);

        connection.setAutoCommit(false);

        PreparedStatement createStatement = null;
        try {
            // Since ORACLE returns the ROWID instead of an autoincremented column, we have to tell the PreparedStatement to
            // return the value of the "id-column" instead of the rowid. This is done by passing the following array to the PreparedStatement
            String generatedColumns[] = {"id"};
            createStatement = queries.getPreparedStatement(connection, queryMap.get(QueryDefinition.CREATEQUERYSTR), generatedColumns);

            logger.debug("Create with fullid {}", fullId);
            String rev = "0";
            obj.put("_id", localId); // Save the id in the object
            obj.put("_rev", rev); // Save the rev in the object, and return the changed rev from the create.
            String objString = mapper.writeValueAsString(obj);

            logger.trace("Populating statement {} with params {}, {}, {}, {}",
                    new Object[]{createStatement, typeId, localId, rev, objString});
            createStatement.setLong(1, typeId);
            createStatement.setString(2, localId);
            createStatement.setString(3, rev);
            createStatement.setString(4, objString);
            logger.debug("Executing: {}", createStatement);
            int val = createStatement.executeUpdate();

            ResultSet keys = createStatement.getGeneratedKeys();
            boolean validKeyEntry = keys.next();
            if (!validKeyEntry) {
                throw new InternalServerErrorException("Object creation for " + fullId + " failed to retrieve an assigned ID from the DB.");
            }

            // Should now contain the value of the autoincremented column
            long dbId = keys.getLong(1);

            logger.debug("Created object for id {} with rev {}", fullId, rev);
            JsonValue jv = new JsonValue(obj);
            writeValueProperties(fullId, dbId, localId, jv, connection);
        } finally {
            CleanupHelper.loggedClose(createStatement);
        }
    }

    @Override
    protected Map<QueryDefinition, String> initializeQueryMap() {
        Map<QueryDefinition, String> result = super.initializeQueryMap();
        String typeTable = dbSchemaName == null ? "objecttypes" : dbSchemaName + ".objecttypes";
        String mainTable = dbSchemaName == null ? mainTableName : dbSchemaName + "." + mainTableName;
        String propertyTable = dbSchemaName == null ? propTableName : dbSchemaName + "." + propTableName;

        // ORACLE is not capable of using the DELETE statements defined in the StandardHandler, therefore we are changing them to
        // something more ORACLEfriendly (thanks to the one that wrote the DB2 adapter
        result.put(QueryDefinition.DELETEQUERYSTR, "DELETE FROM " + mainTable + " obj WHERE EXISTS (SELECT 1 FROM " + typeTable + " objtype WHERE obj.objecttypes_id = objtype.id AND objtype.objecttype = ?) AND obj.objectid = ? AND obj.rev = ?");
        result.put(QueryDefinition.PROPDELETEQUERYSTR, "DELETE FROM " + propertyTable + " WHERE " + mainTableName + "_id = (SELECT obj.id FROM " + mainTable + " obj, " + typeTable + " objtype WHERE obj.objecttypes_id = objtype.id AND objtype.objecttype = ? AND obj.objectid  = ?)");

        return result;
    }    
    
    @Override
    public String buildRawQuery(QueryFilter filter, Map<String, Object> replacementTokens, Map<String, Object> params) {
        final int offsetParam = Integer.parseInt((String)params.get(PAGED_RESULTS_OFFSET));
        final int pageSizeParam = Integer.parseInt((String)params.get(PAGE_SIZE));
        String filterString = getFilterString(filter, replacementTokens);
        String innerJoinClause = "";
        String keysClause = "";
        
        // Check for sort keys and build up order-by syntax
        final List<SortKey> sortKeys = (List<SortKey>)params.get(SORT_KEYS);
        if (sortKeys != null && sortKeys.size() > 0) {
            List<String> innerJoins = new ArrayList<String>();
            List<String> keys = new ArrayList<String>();
            prepareSortKeyStatements(sortKeys, innerJoins, keys, replacementTokens);
            innerJoinClause = StringUtils.join(innerJoins, " ");
            keysClause = StringUtils.join(keys, ", ");
        } else {
            keysClause = "obj.id DESC";
        }
        

        return "SELECT * FROM ( SELECT obj.fullobject, row_number() over (ORDER BY " 
                + keysClause
                + " ) rn FROM ${_dbSchema}.${_mainTable} obj " 
                + innerJoinClause
                + filterString 
                + "ORDER BY "
                + keysClause
                + ") WHERE rn BETWEEN " 
                + (offsetParam+1)
                + " AND " 
                + (offsetParam + pageSizeParam)
                + " ORDER BY rn";
    }
}
