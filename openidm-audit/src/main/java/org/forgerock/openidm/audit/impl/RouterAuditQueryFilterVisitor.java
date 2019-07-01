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
package org.forgerock.openidm.audit.impl;

import static org.forgerock.util.query.QueryFilter.*;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * A QueryFilterVisitor that will replace references to the /_id json pointer with /eventId.
 */
class RouterAuditQueryFilterVisitor implements QueryFilterVisitor<QueryFilter<JsonPointer>, Void, JsonPointer> {

    private static final JsonPointer idPointer = new JsonPointer(ResourceResponse.FIELD_CONTENT_ID);
    private static final JsonPointer eventIdPointer = new JsonPointer(RouterAuditEventHandler.EVENT_ID);
    private static final RouterAuditQueryFilterVisitor instance = new RouterAuditQueryFilterVisitor();

    private RouterAuditQueryFilterVisitor() {
    }

    /**
     * Returns the singleton instance of this visitor.
     *
     * @return the singleton instance of this visitor
     */
    public static RouterAuditQueryFilterVisitor getInstance() {
        return instance;
    }

    @Override
    public QueryFilter<JsonPointer> visitAndFilter(Void aVoid, List<QueryFilter<JsonPointer>> list) {
        return and(transformFilters(list));
    }

    @Override
    public QueryFilter<JsonPointer> visitBooleanLiteralFilter(Void aVoid, boolean bool) {
        return (bool)
                ? QueryFilter.<JsonPointer>alwaysTrue()
                : QueryFilter.<JsonPointer>alwaysFalse();
    }

    @Override
    public QueryFilter<JsonPointer> visitContainsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        return contains(replaceId(field), valueAssertion);
    }

    @Override
    public QueryFilter<JsonPointer> visitEqualsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        return equalTo(replaceId(field), valueAssertion);
    }

    @Override
    public QueryFilter<JsonPointer> visitExtendedMatchFilter(Void aVoid, JsonPointer field, String operator,
            Object valueAssertion) {
        return extendedMatch(replaceId(field), operator, valueAssertion);
    }

    @Override
    public QueryFilter<JsonPointer> visitGreaterThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        return greaterThan(replaceId(field), valueAssertion);
    }

    @Override
    public QueryFilter<JsonPointer> visitGreaterThanOrEqualToFilter(Void aVoid, JsonPointer field,
            Object valueAssertion) {
        return greaterThanOrEqualTo(replaceId(field), valueAssertion);
    }

    @Override
    public QueryFilter<JsonPointer> visitLessThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        return lessThan(replaceId(field), valueAssertion);
    }

    @Override
    public QueryFilter<JsonPointer> visitLessThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        return lessThanOrEqualTo(replaceId(field), valueAssertion);
    }

    @Override
    public QueryFilter<JsonPointer> visitNotFilter(Void aVoid, QueryFilter<JsonPointer> queryFilter) {
        return not(queryFilter.accept(this, null));
    }

    @Override
    public QueryFilter<JsonPointer> visitOrFilter(Void aVoid, List<QueryFilter<JsonPointer>> list) {
        return or(transformFilters(list));
    }

    @Override
    public QueryFilter<JsonPointer> visitPresentFilter(Void aVoid, JsonPointer field) {
        return present(replaceId(field));
    }

    @Override
    public QueryFilter<JsonPointer> visitStartsWithFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
        return startsWith(replaceId(field), valueAssertion);
    }

    /**
     * Replaces the possible _id pointer with the eventId pointer.
     *
     * @param field the pointer to possibly convert.
     * @return the converted pointer
     */
    private JsonPointer replaceId(JsonPointer field) {
        return (idPointer.equals(field))
                ? eventIdPointer
                : field;
    }

    /**
     * Transforms all the filters into filters that accept this RouterAuditQueryFilterVisitor.
     *
     * @param queryFilters The filters to send through the visitor.
     * @return the list of accepting filters.
     */
    private List<QueryFilter<JsonPointer>> transformFilters(List<QueryFilter<JsonPointer>> queryFilters) {
        return FluentIterable.from(queryFilters)
                .transform(new Function<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>>() {
                    @Override
                    public QueryFilter<JsonPointer> apply(QueryFilter<JsonPointer> queryFilter) {
                        return queryFilter.accept(RouterAuditQueryFilterVisitor.this, null);
                    }
                }).toList();
    }

}