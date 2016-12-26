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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.query;

import static org.forgerock.util.query.QueryFilter.*;

import java.util.List;

import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * A {@link QueryFilterVisitor} that applies a given transformer {@link Function} to each field in the filter chain.
 */
public class FieldTransformerQueryFilterVisitor<F> implements QueryFilterVisitor<QueryFilter<F>, Void, F> {

    private final Function<F, F> transformer;

    public FieldTransformerQueryFilterVisitor(final Function<F, F> transformer) {
        this.transformer = transformer;
    }

    @Override
    public QueryFilter<F> visitAndFilter(Void aVoid, List<QueryFilter<F>> list) {
        return and(transformFilters(list));
    }

    @Override
    public QueryFilter<F> visitBooleanLiteralFilter(Void aVoid, boolean bool) {
        return (bool)
                ? QueryFilter.<F>alwaysTrue()
                : QueryFilter.<F>alwaysFalse();
    }

    @Override
    public QueryFilter<F> visitContainsFilter(Void aVoid, F field, Object valueAssertion) {
        return contains(transformer.apply(field), valueAssertion);
    }

    @Override
    public QueryFilter<F> visitEqualsFilter(Void aVoid, F field, Object valueAssertion) {
        return equalTo(transformer.apply(field), valueAssertion);
    }

    @Override
    public QueryFilter<F> visitExtendedMatchFilter(Void aVoid, F field, String operator,
                                                             Object valueAssertion) {
        return extendedMatch(transformer.apply(field), operator, valueAssertion);
    }

    @Override
    public QueryFilter<F> visitGreaterThanFilter(Void aVoid, F field, Object valueAssertion) {
        return greaterThan(transformer.apply(field), valueAssertion);
    }

    @Override
    public QueryFilter<F> visitGreaterThanOrEqualToFilter(Void aVoid, F field,
                                                                    Object valueAssertion) {
        return greaterThanOrEqualTo(transformer.apply(field), valueAssertion);
    }

    @Override
    public QueryFilter<F> visitLessThanFilter(Void aVoid, F field, Object valueAssertion) {
        return lessThan(transformer.apply(field), valueAssertion);
    }

    @Override
    public QueryFilter<F> visitLessThanOrEqualToFilter(Void aVoid, F field, Object valueAssertion) {
        return lessThanOrEqualTo(transformer.apply(field), valueAssertion);
    }

    @Override
    public QueryFilter<F> visitNotFilter(Void aVoid, QueryFilter<F> queryFilter) {
        return not(queryFilter.accept(this, null));
    }

    @Override
    public QueryFilter<F> visitOrFilter(Void aVoid, List<QueryFilter<F>> list) {
        return or(transformFilters(list));
    }

    @Override
    public QueryFilter<F> visitPresentFilter(Void aVoid, F field) {
        return present(transformer.apply(field));
    }

    @Override
    public QueryFilter<F> visitStartsWithFilter(Void aVoid, F field, Object valueAssertion) {
        return startsWith(transformer.apply(field), valueAssertion);
    }

    private List<QueryFilter<F>> transformFilters(List<QueryFilter<F>> queryFilters) {
        return FluentIterable.from(queryFilters)
                .transform(new Function<QueryFilter<F>, QueryFilter<F>>() {
                    @Override
                    public QueryFilter<F> apply(QueryFilter<F> queryFilter) {
                        return queryFilter.accept(FieldTransformerQueryFilterVisitor.this, null);
                    }
                }).toList();
    }

}