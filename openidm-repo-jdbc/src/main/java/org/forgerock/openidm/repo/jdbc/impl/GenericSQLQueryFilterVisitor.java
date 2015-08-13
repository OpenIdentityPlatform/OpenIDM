/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS. All rights reserved.
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

import static org.forgerock.openidm.repo.util.Clauses.not;
import static org.forgerock.openidm.repo.util.Clauses.where;
import static org.forgerock.openidm.repo.util.Clauses.and;
import static org.forgerock.openidm.repo.util.Clauses.or;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.json.JsonPointer;
import org.forgerock.openidm.repo.util.AbstractSQLQueryFilterVisitor;
import org.forgerock.openidm.repo.util.Clause;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.util.query.QueryFilter;

/**
 * QueryFilterVisitor for generating WHERE clause SQL queries against generic table schema.
 */
class GenericSQLQueryFilterVisitor extends AbstractSQLQueryFilterVisitor<Clause, Map<String, Object>> {

    // key/value number for each key/value placeholder
    int objectNumber = 0;

    private final int searchableLength;
    private final SQLBuilder builder;

    /**
     * Construct a QueryFilterVisitor to produce SQL for managed objects using the generic table structure.
     *
     * @param searchableLength the searchable length; properties longer than this will be trimmed to this length
     * @param builder The {@link SQLBuilder} to use to keep track of the select columns, table joins, and order by lists
     */
    GenericSQLQueryFilterVisitor(final int searchableLength, SQLBuilder builder) {
        this.searchableLength = searchableLength;
        this.builder = builder;
    }

    private boolean isNumeric(final Object valueAssertion) {
        return valueAssertion instanceof Integer || valueAssertion instanceof Long
                || valueAssertion instanceof Float || valueAssertion instanceof Double;
    }

    private boolean isBoolean(final Object valueAssertion) {
        return valueAssertion instanceof Boolean;
    }

    private Object trimValue(final Object value) {
        // Must retain types for getPropTypeValueClause()
        if (isNumeric(value) || isBoolean(value)) {
            return value;
        } else {
            return StringUtils.left(value.toString(), searchableLength);
        }
    }

    /**
     * Generate the WHERE clause for properties table for a numeric value assertion.
     *
     * @param propTable the property table
     * @param operand the comparison operand
     * @param placeholder the value placeholder
     * @return SQL WHERE clause for properties table
     */
    Clause buildNumericValueClause(String propTable, String operand, String placeholder) {
        return where(propTable + ".proptype = 'java.lang.Integer'")
                .or(propTable + ".proptype = 'java.lang.Double'")
                .and("CAST(" + propTable + ".propvalue AS DECIMAL) " + operand + " ${" + placeholder + "}");
    }

    /**
     * Generate the WHERE clause for properties table for a boolean value assertion.
     *
     * @param propTable the property table
     * @param operand the comparison operand
     * @param placeholder the value placeholder
     * @return SQL WHERE clause for properties table
     */
    Clause buildBooleanValueClause(String propTable, String operand, String placeholder) {
        return where(propTable + ".proptype = 'java.lang.Boolean'")
                .and(where(propTable + ".propvalue " + operand + " ${" + placeholder + "}"));
    }

    /**
     * Generate the WHERE clause for properties table for a string value assertion.
     *
     * @param propTable the property table
     * @param operand the comparison operand
     * @param placeholder the value placeholder
     * @return SQL WHERE clause for properties table
     */
    Clause buildStringValueClause(String propTable, String operand, String placeholder) {
        return where(propTable + ".propvalue " + operand + " ${" + placeholder + "}");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clause visitValueAssertion(Map<String, Object> objects, String operand, JsonPointer field, Object valueAssertion) {
        ++objectNumber;
        String key = "k"+objectNumber;
        String value = "v"+objectNumber;
        String propTable = "prop"+objectNumber;

        if (ResourceUtil.RESOURCE_FIELD_CONTENT_ID_POINTER.equals(field)) {
            objects.put(value, valueAssertion);
            return where("obj.objectid " + operand + " ${" + value + "}");

        } else {
            objects.put(key, field.toString());
            objects.put(value, valueAssertion);
            final Clause valueClause;
            if (isNumeric(valueAssertion)) {
                // validate type is integer or double cast all numeric types to decimal
                valueClause = buildNumericValueClause(propTable, operand, value);
            } else if (isBoolean(valueAssertion)) {
                // validate type is boolean if valueAssertion is a boolean
                valueClause = buildBooleanValueClause(propTable, operand, value);
            } else {
                // assume String
                valueClause = buildStringValueClause(propTable, operand, value);
            }
            builder.join("${_dbSchema}.${_propTable}", propTable)
                    .on(where(propTable + ".${_mainTable}_id = obj.id").and(where(propTable + ".propkey = ${" + key + "}")));
            return valueClause;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clause visitAndFilter(final Map<String, Object> parameters, List<QueryFilter<JsonPointer>> subfilters) {
        return and(FluentIterable.from(subfilters).transform(
                new Function<QueryFilter<JsonPointer>, Clause>() {
                    @Override
                    public Clause apply(QueryFilter<JsonPointer> filter) {
                        return filter.accept(GenericSQLQueryFilterVisitor.this, parameters);
                    }
                }));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clause visitOrFilter(final Map<String, Object> parameters, List<QueryFilter<JsonPointer>> subfilters) {
        return or(FluentIterable.from(subfilters).transform(
                new Function<QueryFilter<JsonPointer>, Clause>() {
                    @Override
                    public Clause apply(QueryFilter<JsonPointer> filter) {
                        return filter.accept(GenericSQLQueryFilterVisitor.this, parameters);
                    }
                }));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clause visitPresentFilter(Map<String, Object> objects, JsonPointer field) {
        if (ResourceUtil.RESOURCE_FIELD_CONTENT_ID_POINTER.equals(field)) {
            // NOT NULL is enforced by the schema
            return where("(obj.objectid IS NOT NULL)");
        } else {
            ++objectNumber;
            String key = "k" + objectNumber;
            String propTable = "prop"+objectNumber;
            objects.put(key, field.toString());
            builder.leftJoin("${_dbSchema}.${_propTable}", propTable)
                    .on(where(propTable + ".${_mainTable}_id = obj.id")
                            .and(propTable + ".propkey = ${" + key + "}"));
            return where(propTable + ".propvalue IS NOT NULL");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clause visitBooleanLiteralFilter(Map<String, Object> parameters, boolean value) {
        return where(value ? "1 = 1" : "1 <> 1");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clause visitNotFilter(Map<String, Object> parameters, QueryFilter<JsonPointer> subFilter) {
        return not(subFilter.accept(this, parameters));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clause visitContainsFilter(Map<String, Object> parameters, JsonPointer field, Object valueAssertion) {
        return super.visitContainsFilter(parameters, field, trimValue(valueAssertion));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clause visitEqualsFilter(Map<String, Object> parameters, JsonPointer field, Object valueAssertion) {
        return super.visitEqualsFilter(parameters, field, trimValue(valueAssertion));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clause visitStartsWithFilter(Map<String, Object> parameters, JsonPointer field, Object valueAssertion) {
        return super.visitStartsWithFilter(parameters, field, trimValue(valueAssertion));
    }

}
