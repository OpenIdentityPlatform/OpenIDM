/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012-2015 ForgeRock AS. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.SortKey;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;
import org.forgerock.openidm.repo.util.StringSQLQueryFilterVisitor;
import org.forgerock.openidm.repo.util.StringSQLRenderer;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.util.query.QueryFilter;

/**
 * Postgres-specific generic table handler.
 */
public class PostgreSQLTableHandler extends GenericTableHandler {

    private class JsonExtractPathQueryFilterVisitor extends StringSQLQueryFilterVisitor<Map<String, Object>> {
        // value number for each value placeholder
        int objectNumber = 0;

        /**
         * Convenience method to generate the json_extract_path_text fragment:
         *
         * <pre><blockquote>
         *  json_extract_path_text(obj.fullobject, ${p1}, ${p2}, {$p3} ...)
         * </blockquote></pre>
         *
         * where ${pn} are placeholders for the JsonPointer path elements.
         */
        private StringSQLRenderer jsonExtractPathOnField(JsonPointer field, final Map<String, Object> objects) {
            return new StringSQLRenderer("json_extract_path_text(obj.fullobject, ")
                    .append(StringUtils.join(
                            FluentIterable.from(Arrays.asList(field.toArray()))
                                    .transform(new Function<String, String>() {
                                         @Override
                                         public String apply(String jsonPath) {
                                            ++objectNumber;
                                            String placeholder = "p" + objectNumber;
                                            objects.put(placeholder, jsonPath);
                                            return "${" + placeholder + "}";
                                        }
                                    }),
                            ", "))
                    .append(")");
        }

        @Override
        public StringSQLRenderer visitValueAssertion(Map<String, Object> objects, String operand, JsonPointer field, Object valueAssertion) {
            ++objectNumber;
            String value = "v"+objectNumber;
            objects.put(value, valueAssertion);
            if (ResourceUtil.RESOURCE_FIELD_CONTENT_ID_POINTER.equals(field)) {
                return new StringSQLRenderer("(obj.objectid " + operand + " ${" + value + "})");
            } else {
                // cast to numeric for numeric types
                String cast = (valueAssertion instanceof Integer || valueAssertion instanceof Long
                        || valueAssertion instanceof Float || valueAssertion instanceof Double)
                    ? "::numeric"
                    : "";

                return new StringSQLRenderer("(")
                        .append(jsonExtractPathOnField(field, objects).toSQL())
                        .append(cast)
                        .append(" ")
                        .append(operand)
                        .append(" (${")
                        .append(value)
                        .append("})")
                        .append(cast)
                        .append(")");
            }
        }

        @Override
        public StringSQLRenderer visitPresentFilter(Map<String, Object> objects, JsonPointer field) {
            if (ResourceUtil.RESOURCE_FIELD_CONTENT_ID_POINTER.equals(field)) {
                // NOT NULL enforced by the schema
                return new StringSQLRenderer("(obj.objectid IS NOT NULL)");
            } else {
                return new StringSQLRenderer("(" + jsonExtractPathOnField(field, objects).toSQL() + " IS NOT NULL)");
            }
        }
    }

    /**
     * Construct a table handler for Postgres using Postgres-specific json-handling
     *
     * {@inheritDoc}
     */
    public PostgreSQLTableHandler(JsonValue tableConfig, String dbSchemaName, JsonValue queriesConfig, JsonValue commandsConfig,
            int maxBatchSize, SQLExceptionHandler sqlExceptionHandler) {
        super(tableConfig, dbSchemaName, queriesConfig, commandsConfig, maxBatchSize, sqlExceptionHandler);
    }

    protected Map<QueryDefinition, String> initializeQueryMap() {
        Map<QueryDefinition, String> result = super.initializeQueryMap();

        String typeTable = dbSchemaName == null ? "objecttypes" : dbSchemaName + ".objecttypes";
        String mainTable = dbSchemaName == null ? mainTableName : dbSchemaName + "." + mainTableName;
        String propertyTable = dbSchemaName == null ? propTableName : dbSchemaName + "." + propTableName;

        result.put(QueryDefinition.UPDATEQUERYSTR, "UPDATE " + mainTable + " SET objectid = ?, rev = ?, fullobject = ?::json WHERE id = ?");
        result.put(QueryDefinition.CREATEQUERYSTR, "INSERT INTO " + mainTable + " (objecttypes_id, objectid, rev, fullobject) VALUES (?,?,?,?::json)");
        result.put(QueryDefinition.DELETEQUERYSTR, "DELETE FROM " + mainTable + " obj USING " + typeTable + " objtype WHERE obj.objecttypes_id = objtype.id AND objtype.objecttype = ? AND obj.objectid = ? AND obj.rev = ?");
        result.put(QueryDefinition.PROPDELETEQUERYSTR, "DELETE FROM " + propertyTable + " WHERE " + mainTableName + "_id IN (SELECT obj.id FROM " + mainTable + " obj INNER JOIN " + typeTable + " objtype ON obj.objecttypes_id = objtype.id WHERE objtype.objecttype = ? AND obj.objectid = ?)");
        return result;
    }
    
    @Override
    public String renderQueryFilter(QueryFilter<JsonPointer> filter, Map<String, Object> replacementTokens, Map<String, Object> params) {
        final String offsetParam = (String) params.get(PAGED_RESULTS_OFFSET);
        final String pageSizeParam = (String) params.get(PAGE_SIZE);
        String pageClause = " LIMIT " + pageSizeParam + " OFFSET " + offsetParam;
        
        // JsonValue-cheat to avoid an unchecked cast
        final List<SortKey> sortKeys = new JsonValue(params).get(SORT_KEYS).asList(SortKey.class);
        // Check for sort keys and build up order-by syntax
        if (sortKeys != null && sortKeys.size() > 0) {
            List<String> keys = new ArrayList<String>();
            for (int i = 0; i < sortKeys.size(); i++) {
                final SortKey sortKey = sortKeys.get(i);
                final String tokenName = "sortKey" + i;
                keys.add("json_extract_path_text(fullobject, ${" + tokenName + (sortKey.isAscendingOrder() ? "}) ASC" : "}) DESC"));
                replacementTokens.put(tokenName, sortKey.getField().toString().substring(1));
            }
            pageClause = " ORDER BY " + StringUtils.join(keys, ", ") + pageClause;
        }

        replacementTokens.put("otype", params.get("_resource"));
        return "SELECT fullobject::text"
                + " FROM ${_dbSchema}.${_mainTable} obj"
                + " INNER JOIN ${_dbSchema}.objecttypes objtype ON objtype.id = obj.objecttypes_id AND objtype.objecttype = ${otype}"
                + " WHERE "
                + filter.accept(new JsonExtractPathQueryFilterVisitor(), replacementTokens).toSQL() + pageClause;
    }
}
