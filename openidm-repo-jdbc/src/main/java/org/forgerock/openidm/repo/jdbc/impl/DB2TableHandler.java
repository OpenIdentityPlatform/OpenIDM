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
import java.sql.SQLException;

import java.util.List;
import java.util.Map;

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
                        + " obj INNER JOIN "
                        + typeTable
                        + " objtype ON obj.objecttypes_id = objtype.id AND objtype.objecttype = ? WHERE obj.objectid = ?");

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
}
