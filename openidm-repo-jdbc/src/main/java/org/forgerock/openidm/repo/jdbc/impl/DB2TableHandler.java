/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2014 ForgeRock AS. All rights reserved.
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
 */
package org.forgerock.openidm.repo.jdbc.impl;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;

import static org.forgerock.openidm.repo.QueryConstants.PAGED_RESULTS_OFFSET;
import static org.forgerock.openidm.repo.QueryConstants.PAGE_SIZE;
import static org.forgerock.openidm.repo.QueryConstants.SORT_KEYS;
import static org.forgerock.openidm.repo.util.Clauses.where;

import org.forgerock.json.resource.SortKey;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;
import org.forgerock.openidm.repo.util.Clause;
import org.forgerock.util.query.QueryFilter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;
import java.util.Map;
import org.forgerock.json.resource.NotFoundException;

/**
 * @version $Revision$ $Date$
 */
public class DB2TableHandler extends GenericTableHandler {

    public DB2TableHandler(JsonValue tableConfig, String dbSchemaName, JsonValue queriesConfig, JsonValue commandsConfig,
            int maxBatchSize, SQLExceptionHandler sqlExceptionHandler) {
        super(tableConfig, dbSchemaName, queriesConfig, commandsConfig, maxBatchSize, sqlExceptionHandler);
    }

    protected Map<QueryDefinition, String> initializeQueryMap() {
        Map<QueryDefinition, String> result = super.initializeQueryMap();
        String typeTable = dbSchemaName == null ? "objecttypes" : dbSchemaName + ".objecttypes";
        String mainTable = dbSchemaName == null ? mainTableName : dbSchemaName + "." + mainTableName;
        String propertyTable = dbSchemaName == null ? propTableName : dbSchemaName + "." + propTableName;

        // Not allowed to use "FOR UPDATE" on multiple tables in DB2
        result.put(
                QueryDefinition.READFORUPDATEQUERYSTR,
                "SELECT obj.* FROM "
                        + mainTable
                        + " obj WHERE obj.objecttypes_id = ? AND obj.objectid = ? FOR UPDATE");

        // Main object table DB2 Script
        result.put(QueryDefinition.DELETEQUERYSTR, "DELETE FROM " + mainTable + " obj WHERE EXISTS (SELECT 1 FROM " + typeTable + " objtype WHERE obj.objecttypes_id = objtype.id AND objtype.objecttype = ?) AND obj.objectid = ? AND obj.rev = ?");
        //TODO Fix this CPU killer query
        result.put(QueryDefinition.PROPDELETEQUERYSTR, "DELETE FROM " + propertyTable + " WHERE " + mainTableName + "_id = (SELECT obj.id FROM " + mainTable + " obj, " + typeTable + " objtype WHERE obj.objecttypes_id = objtype.id AND objtype.objecttype = ? AND obj.objectid  = ?)");
        return result;
    }

    public boolean isRetryable(SQLException ex, Connection connection) {
        // Re-tryable DB2 error codes
        // -911 indicates DB2 rolled back already and expects a retry
        // -912 indicates deadlock or timeout.
        // -904 indicates resource limit was exceeded.
        if (-911 == ex.getErrorCode() || -912 == ex.getErrorCode() || -904 == ex.getErrorCode()) {
            return true;
        } else {
            return false;
        }
    }

    // blatantly copied from OracleTableHandler...
    /**
     * @inheritDoc
     */
    @Override
    public String renderQueryFilter(QueryFilter<JsonPointer> filter, Map<String, Object> replacementTokens, Map<String, Object> params) {
        final int offsetParam = Integer.parseInt((String)params.get(PAGED_RESULTS_OFFSET));
        final int pageSizeParam = Integer.parseInt((String)params.get(PAGE_SIZE));

        // Create custom builder which overrides SQL output syntax
        // note enclosing offsetParam and pageSizeParam - we don't bother passing these to the builder to deal with
        final SQLBuilder builder =
                new SQLBuilder() {
                    @Override
                    public String toSQL() {
                        return "SELECT * FROM ( SELECT obj.fullobject, row_number() OVER ("
                                + getOrderByClause().toSQL()
                                + " ) rn "
                                + getFromClause().toSQL()
                                + getJoinClause().toSQL()
                                + getWhereClause().toSQL()
                                + getOrderByClause().toSQL()
                                + ") WHERE rn BETWEEN "
                                + (offsetParam + 1)
                                + " AND "
                                + (offsetParam + pageSizeParam)
                                + " ORDER BY rn";
                    }
                };

        // "SELECT obj.* FROM mainTable obj..."
        builder.from("${_dbSchema}.${_mainTable} obj")

                // join objecttypes to fix OPENIDM-2773
                .join("${_dbSchema}.objecttypes", "objecttypes")
                .on(where("obj.objecttypes_id = objecttypes.id")
                        .and("objecttypes.objecttype = ${otype}"))

                .where(filter.accept(
                        new GenericSQLQueryFilterVisitor(SEARCHABLE_LENGTH, builder) {
                            // override numeric value clause generation to cast propvalue to a number
                            @Override
                            Clause buildNumericValueClause(String propTable, String operand, String placeholder) {
                                return where(propTable + ".proptype = 'java.lang.Integer'")
                                        .or(propTable + ".proptype = 'java.lang.Double'")
                                        .and("TO_NUMBER(" + propTable + ".propvalue) " + operand + " ${" + placeholder + "}");
                            }
                        },
                        replacementTokens));

        // other half of OPENIDM-2773 fix
        replacementTokens.put("otype", params.get("_resource"));

        // JsonValue-cheat to avoid an unchecked cast
        final List<SortKey> sortKeys = new JsonValue(params).get(SORT_KEYS).asList(SortKey.class);
        // Check for sort keys and build up order-by syntax
        if (sortKeys != null && sortKeys.size() > 0) {
            prepareSortKeyStatements(builder, sortKeys, replacementTokens);
        } else {
            builder.orderBy("obj.id", false);
        }

        return builder.toSQL();
    }
    
    /**
     * Reads an object with for update locking applied
     *
     * Note: statement associated with the returned resultset
     * is not closed upon return.
     * Aside from taking care to close the resultset it also is
     * the responsibility of the caller to close the associated
     * statement. Although the specification specifies that drivers/pools
     * should close the statement automatically, not all do this reliably.
     *
     * @param fullId qualified id of component type and id
     * @param type the component type
     * @param localId the id of the object within the component type
     * @param connection the connection to use
     * @return the row for the requested object, selected FOR UPDATE
     * @throws NotFoundException if the requested object was not found in the DB
     * @throws java.sql.SQLException for general DB issues
     */
    public ResultSet readForUpdate(String fullId, String type, String localId, Connection connection)
            throws NotFoundException, SQLException {
        
        PreparedStatement readForUpdateStatement = null;
        ResultSet rs = null;
        try {
            long typeId = readTypeId(type, connection);
            if (typeId < 0) {
                throw new NotFoundException("Object " + fullId + " not found. No id could be retrieved for type " + type);
            }
            readForUpdateStatement = getPreparedStatement(connection, QueryDefinition.READFORUPDATEQUERYSTR);
            logger.trace("Populating prepared statement {} for {}", readForUpdateStatement, fullId);
            readForUpdateStatement.setString(1, String.valueOf(typeId));
            readForUpdateStatement.setString(2, localId);

            logger.debug("Executing: {}", readForUpdateStatement);
            rs = readForUpdateStatement.executeQuery();
            if (rs.next()) {
                logger.debug("Read for update full id: {}", fullId);
                return rs;
            } else {
                CleanupHelper.loggedClose(rs);
                CleanupHelper.loggedClose(readForUpdateStatement);
                throw new NotFoundException("Object " + fullId + " not found in " + type);
            }
        } catch (SQLException ex) {
            CleanupHelper.loggedClose(rs);
            CleanupHelper.loggedClose(readForUpdateStatement);
            throw ex;
        }
    }
}
