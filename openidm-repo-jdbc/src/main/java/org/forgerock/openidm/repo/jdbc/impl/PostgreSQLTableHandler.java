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

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.QueryFilterVisitor;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;
import org.forgerock.openidm.repo.util.SQLQueryFilterVisitor;
import org.forgerock.util.Iterables;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;

/**
 * Postgres-specific generic table handler.
 */
public class PostgreSQLTableHandler extends GenericTableHandler {

    private static final QueryFilterVisitor<String, Map<String, Object>> JSON_EXTRACT_PATH_QUERY_FILTER_VISITOR =
            new SQLQueryFilterVisitor<Map<String, Object>>() {
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
                private String jsonExtractPathOnField(JsonPointer field, final Map<String, Object> objects) {
                    return "json_extract_path_text(obj.fullobject, "
                        + StringUtils.join(
                                Iterables.from(Arrays.asList(field.toArray()))
                                .map(new Function<String, String, NeverThrowsException>() {
                                    @Override
                                    public String apply(String jsonPath) throws NeverThrowsException {
                                        ++objectNumber;
                                        String placeholder = "p" + objectNumber;
                                        objects.put(placeholder, jsonPath);
                                        return "${" + placeholder + "}";
                                    }
                                }), ", ")
                        + ")";
                }

                @Override
                public String visitValueAssertion(Map<String, Object> objects, String operand, JsonPointer field, Object valueAssertion) {
                    ++objectNumber;
                    String value = "v"+objectNumber;
                    objects.put(value, valueAssertion);
                    return "(" + jsonExtractPathOnField(field, objects) + " " + operand + " ${" + value + "})";
                }

                @Override
                public String visitPresentFilter(Map<String, Object> objects, JsonPointer field) {
                    return "(" + jsonExtractPathOnField(field, objects) + " IS NOT NULL)";
                }
            };

    /**
     * Construct a table handler for Postgres using Postgres-specific json-handling
     *
     * {@inheritDoc}
     */
    public PostgreSQLTableHandler(JsonValue tableConfig, String dbSchemaName, JsonValue queriesConfig, JsonValue commandsConfig,
            int maxBatchSize, SQLExceptionHandler sqlExceptionHandler) {
        super(tableConfig, dbSchemaName, queriesConfig, commandsConfig, maxBatchSize,
                JSON_EXTRACT_PATH_QUERY_FILTER_VISITOR, sqlExceptionHandler);
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
}
