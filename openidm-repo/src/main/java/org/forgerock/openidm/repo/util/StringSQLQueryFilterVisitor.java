/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.json.JsonPointer;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * An abstract {@link QueryFilterVisitor} to produce SQL using an {@link StringSQLRenderer}.
 * Includes patterns for the standard
 *
 * <ul>
 *     <li>AND</li>
 *     <li>OR</li>
 *     <li>NOT</li>
 *     <li>&gt;=</li>
 *     <li>&gt;</li>
 *     <li>=</li>
 *     <li>&lt;</li>
 *     <li>&lt;=</li>
 * </ul>
 * operators, along with the following implementations for {@link QueryFilter}'s
 * <ul>
 *     <li>contains : field LIKE '%value%'</li>
 *     <li>startsWith : field LIKE 'value%'</li>
 *     <li>literal true : 1 = 1</li>
 *     <li>literal false : 1 &lt;&gt; 1</li>
 * </ul>
 * <p>
 * This implementation does not support extended-match.
 * <p>
 * The implementer is responsible for implementing {@link #visitValueAssertion(Object, String, org.forgerock.json.JsonPointer, Object)}
 * which handles the value assertions - x operand y for the standard operands.  The implementer is also responsible for
 * implementing {@link #visitPresentFilter(Object, org.forgerock.json.JsonPointer)} as "field present" can vary
 * by database implementation (though typically "field IS NOT NULL" is chosen).
 */
public abstract class StringSQLQueryFilterVisitor<P> extends AbstractSQLQueryFilterVisitor<StringSQLRenderer, P> {

    /**
     * A templating method that will generate the actual value assertion.
     * <p>
     * Example:
     * <pre><blockquote>
     *     ?_queryFilter=email+eq+"someone@example.com"
     * </blockquote></pre>
     * is an QueryFilter stating the value assertion "email" equals "someone@example.com".  The correct SQL for that
     * may vary depending on database variant and schema definition.  This method will be invoked as
     * <pre><blockquote>
     *     return visitValueAssertion(parameters, "=", JsonPointer(/email), "someone@example.com");
     * </blockquote></pre>
     * A possible implementation for the above example may be
     * <pre><blockquote>
     *     return getDatabaseColumnFor("email") + "=" + ":email";
     * </blockquote></pre>
     * The parameters argument is implementation-dependent as a way to store placeholder mapping throughout the query-filter visiting.
     *
     * @param parameters storage of parameter-substitutions for the value of the assertion
     * @param operand the operand used to compare
     * @param field the object field as a JsonPointer - implementations need to map this to an appropriate database column
     * @param valueAssertion the value in the assertion
     * @return a query expression or clause
     */
    public abstract StringSQLRenderer visitValueAssertion(P parameters, String operand, JsonPointer field, Object valueAssertion);

    public StringSQLRenderer visitCompositeFilter(final P parameters, List<QueryFilter<JsonPointer>> subFilters, String operand) {
        final String operandDelimiter = new StringBuilder(" ").append(operand).append(" ").toString();
        return new StringSQLRenderer("(")
                .append(StringUtils.join(
                        FluentIterable.from(subFilters)
                            .transform(new Function<QueryFilter<JsonPointer>, String>() {
                                @Override
                                public String apply(QueryFilter<JsonPointer> filter) {
                                    return filter.accept(StringSQLQueryFilterVisitor.this, parameters).toSQL();
                                }
                            }),
                        operandDelimiter))
                .append(")");
    }

    @Override
    public StringSQLRenderer visitAndFilter(P parameters, List<QueryFilter<JsonPointer>> subFilters) {
        return visitCompositeFilter(parameters, subFilters, "AND");
    }

    @Override
    public StringSQLRenderer visitOrFilter(P parameters, List<QueryFilter<JsonPointer>> subFilters) {
        return visitCompositeFilter(parameters, subFilters, "OR");
    }
    @Override
    public StringSQLRenderer visitBooleanLiteralFilter(P parameters, boolean value) {
        return new StringSQLRenderer(value ? "1 = 1" : "1 <> 1");
    }

    @Override
    public StringSQLRenderer visitNotFilter(P parameters, QueryFilter<JsonPointer> subFilter) {
        return new StringSQLRenderer("NOT ")
                .append(subFilter.accept(this, parameters).toSQL());
    }

    @Override
    public abstract StringSQLRenderer visitPresentFilter(P parameters, JsonPointer field);
}
