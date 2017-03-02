/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017 ForgeRock AS.
 */
package org.forgerock.openidm.query;

import static org.forgerock.util.query.QueryFilter.*;

import java.util.List;

import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * A {@link QueryFilterVisitor} with a {@link #transformValue(Object, Object, Object)} method that modifies
 * valueAssertions of the queryFilter.
 *
 * @param <P>
 *            The type of the additional parameter to this visitor's methods.
 *            Use {@link Void} for visitors that do not need an
 *            additional parameter.
 * @param <F>
 *            The type of the field definitions in this visitor's methods.
 */
public abstract class ValueTransformerQueryFilterVisitor<P, F> implements QueryFilterVisitor<QueryFilter<F>, P, F> {

    /**
     * Transform method run against each valueAssertion of the queryFilter.
     *
     * @param param Optional parameter
     * @param field The incoming field parameter
     * @param valueAssertion the incoming value to transform
     * @return the transformed value
     */
    protected abstract Object transformValue(P param, F field, Object valueAssertion);


    @Override
    public QueryFilter<F> visitAndFilter(P param, List<QueryFilter<F>> list) {
        return and(transformFilters(param, list));
    }

    @Override
    public QueryFilter<F> visitBooleanLiteralFilter(P param, boolean bool) {
        return (bool)
                ? QueryFilter.<F>alwaysTrue()
                : QueryFilter.<F>alwaysFalse();
    }

    @Override
    public QueryFilter<F> visitContainsFilter(P param, F field, Object valueAssertion) {
        return contains(field, transformValue(param, field, valueAssertion));
    }

    @Override
    public QueryFilter<F> visitEqualsFilter(P param, F field, Object valueAssertion) {
        return equalTo(field, transformValue(param, field, valueAssertion));
    }

    @Override
    public QueryFilter<F> visitExtendedMatchFilter(P param, F field, String operator,
                                                             Object valueAssertion) {
        return extendedMatch(field, operator, transformValue(param, field, valueAssertion));
    }

    @Override
    public QueryFilter<F> visitGreaterThanFilter(P param, F field, Object valueAssertion) {
        return greaterThan(field, transformValue(param, field, valueAssertion));
    }

    @Override
    public QueryFilter<F> visitGreaterThanOrEqualToFilter(P param, F field,
                                                                    Object valueAssertion) {
        return greaterThanOrEqualTo(field, transformValue(param, field, valueAssertion));
    }

    @Override
    public QueryFilter<F> visitLessThanFilter(P param, F field, Object valueAssertion) {
        return lessThan(field, transformValue(param, field, valueAssertion));
    }

    @Override
    public QueryFilter<F> visitLessThanOrEqualToFilter(P param, F field, Object valueAssertion) {
        return lessThanOrEqualTo(field, transformValue(param, field, valueAssertion));
    }

    @Override
    public QueryFilter<F> visitNotFilter(P param, QueryFilter<F> queryFilter) {
        return not(queryFilter.accept(this, param));
    }

    @Override
    public QueryFilter<F> visitOrFilter(P param, List<QueryFilter<F>> list) {
        return or(transformFilters(param, list));
    }

    @Override
    public QueryFilter<F> visitPresentFilter(P param, F field) {
        return present(field);
    }

    @Override
    public QueryFilter<F> visitStartsWithFilter(P param, F field, Object valueAssertion) {
        return startsWith(field, transformValue(param, field, valueAssertion));
    }

    private List<QueryFilter<F>> transformFilters(final P param, final List<QueryFilter<F>> queryFilters) {
        return FluentIterable.from(queryFilters)
                .transform(new Function<QueryFilter<F>, QueryFilter<F>>() {
                    @Override
                    public QueryFilter<F> apply(QueryFilter<F> queryFilter) {
                        return queryFilter.accept(ValueTransformerQueryFilterVisitor.this, param);
                    }
                }).toList();
    }

}