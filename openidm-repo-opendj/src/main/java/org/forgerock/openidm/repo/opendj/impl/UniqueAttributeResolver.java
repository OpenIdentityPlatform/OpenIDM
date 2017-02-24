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
package org.forgerock.openidm.repo.opendj.impl;

import static org.forgerock.util.Reject.checkNotNull;

import java.util.LinkedList;
import java.util.List;

import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * Checks a resource for uniqueness according to a set of unique constraints.
 */
class UniqueAttributeResolver {

    private final List<List<JsonPointer>> uniqueConstraints;
    private final RequestHandler handler;
    private final ResourcePath resourcePath;
    private final QueryFilter<JsonPointer> uniqueResolverQueryFilterTemplate;
    private final UniqueConstraintQueryFilterVisitor fieldReplacementQueryFilterVisitor =
            new UniqueConstraintQueryFilterVisitor();

    /**
     * Constructs a {@link UniqueAttributeResolver} given a set of unique constraints, a {@link RequestHandler} to the
     * opendj repo, and the {@link ResourcePath} for the given resource.
     *
     * @param uniqueConstraints the set of unique constraints
     * @param handler the handler to the opendj repo
     * @param resourcePath the resource path for the given resource
     */
    UniqueAttributeResolver(final List<List<JsonPointer>> uniqueConstraints, final RequestHandler handler,
            final ResourcePath resourcePath) {
        this.uniqueConstraints = checkNotNull(uniqueConstraints);
        this.handler = checkNotNull(handler);
        this.resourcePath = checkNotNull(resourcePath);
        this.uniqueResolverQueryFilterTemplate = createUniqueConstraintQueryFilterTemplate();
    }

    /**
     * Tests a given object for uniqueness.
     *
     * @param context the {@link Context} that triggered the uniqueness check
     * @param resource the resource to check for uniqueness
     * @return true if the resource is unique; false otherwise
     */
    public boolean isUnique(final Context context, final JsonValue resource) throws ResourceException {
        return uniqueConstraints.isEmpty() || queryForUniqueness(context, resource);
    }

    private boolean queryForUniqueness(final Context context, final JsonValue resource) throws ResourceException {
        final List<ResourceResponse> resources = new LinkedList<>();
        final QueryRequest queryRequest = Requests.newQueryRequest(resourcePath)
                .setQueryFilter(populateQueryFilterTemplate(resource));

        return handler.handleQuery(context, queryRequest, new QueryResourceHandler() {
            @Override
            public boolean handleResource(final ResourceResponse resource) {
                resources.add(resource);
                // return false once the first resource is found since we only care about the first resource.
                return false;
            }
        }).then(new org.forgerock.util.Function<QueryResponse, Boolean, ResourceException>() {
            @Override
            public Boolean apply(QueryResponse queryResponse) throws ResourceException {
                return resources.isEmpty();
            }
        }).getOrThrowUninterruptibly();
    }

    /**
     * Populates the {@link QueryFilter.EqualsImpl} value assertion in the query filter template
     * with the values in the object.
     *
     * @param resource the {@link JsonValue} object that contains the values to replace in the query filter template
     * @return a new {@link QueryFilter<JsonPointer>} with the replaced values
     */
    private QueryFilter<JsonPointer> populateQueryFilterTemplate(final JsonValue resource) {
        return uniqueResolverQueryFilterTemplate.accept(fieldReplacementQueryFilterVisitor, resource);
    }

    private QueryFilter<JsonPointer> createUniqueConstraintQueryFilterTemplate() {
        return QueryFilter.or(FluentIterable.from(uniqueConstraints)
                .transform(TO_AND_QUERY_FILTER)
                .toList());
    }

    private static Function<JsonPointer, QueryFilter<JsonPointer>> TO_EQUALS_QUERY_FILTER =
            new Function<JsonPointer, QueryFilter<JsonPointer>>() {
                @Override
                public QueryFilter<JsonPointer> apply(final JsonPointer uniqueField) {
                    return QueryFilter.equalTo(uniqueField, uniqueField);
                }
            };

    private static Function<List<JsonPointer>, QueryFilter<JsonPointer>> TO_AND_QUERY_FILTER =
            new Function<List<JsonPointer>, QueryFilter<JsonPointer>>() {
                @Override
                public QueryFilter<JsonPointer> apply(final List<JsonPointer> jsonPointers) {
                    return QueryFilter.and(FluentIterable.from(jsonPointers)
                            .transform(TO_EQUALS_QUERY_FILTER)
                            .toList());
                }
            };

    /**
     * A {@link QueryFilterVisitor} that replaces the visitEqualsFilter valueAssertion with the value retrieved from
     * the passed in {@link JsonValue} object
     */
    private static class UniqueConstraintQueryFilterVisitor
            implements QueryFilterVisitor<QueryFilter<JsonPointer>, JsonValue, JsonPointer> {

        @Override
        public QueryFilter<JsonPointer> visitAndFilter(final JsonValue resource,
                final List<QueryFilter<JsonPointer>> subFilters) {
            return QueryFilter.and(processSubFilters(subFilters, resource));
        }

        @Override
        public QueryFilter<JsonPointer> visitBooleanLiteralFilter(final JsonValue resource, final boolean value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public QueryFilter<JsonPointer> visitContainsFilter(final JsonValue resource, final JsonPointer field,
                Object valueAssertion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public QueryFilter<JsonPointer> visitEqualsFilter(final JsonValue resource, final JsonPointer field,
                final Object valueAssertion) {
            return QueryFilter.equalTo(field, resource.get(field) == null ? null : resource.get(field).getObject());
        }

        @Override
        public QueryFilter<JsonPointer> visitExtendedMatchFilter(final JsonValue resource, final JsonPointer field,
                final String operator, final Object valueAssertion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public QueryFilter<JsonPointer> visitGreaterThanFilter(final JsonValue resource, final JsonPointer field,
                final Object valueAssertion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public QueryFilter<JsonPointer> visitGreaterThanOrEqualToFilter(final JsonValue resource,
                final JsonPointer field, final Object valueAssertion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public QueryFilter<JsonPointer> visitLessThanFilter(final JsonValue resource, final JsonPointer field,
                final Object valueAssertion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public QueryFilter<JsonPointer> visitLessThanOrEqualToFilter(final JsonValue resource, final JsonPointer field,
                final Object valueAssertion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public QueryFilter<JsonPointer> visitNotFilter(final JsonValue resource,
                final QueryFilter<JsonPointer> subFilter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public QueryFilter<JsonPointer> visitOrFilter(final JsonValue resource,
                final List<QueryFilter<JsonPointer>> subFilters) {
            return QueryFilter.or(processSubFilters(subFilters, resource));
        }

        @Override
        public QueryFilter<JsonPointer> visitPresentFilter(final JsonValue resource, final JsonPointer field) {
            throw new UnsupportedOperationException();
        }

        @Override
        public QueryFilter<JsonPointer> visitStartsWithFilter(final JsonValue resource, final JsonPointer field,
                final Object valueAssertion) {
            throw new UnsupportedOperationException();
        }

        private List<QueryFilter<JsonPointer>> processSubFilters(final List<QueryFilter<JsonPointer>> subFilters,
                final JsonValue resource) {
            return FluentIterable.from(subFilters)
                    .transform(
                            new Function<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>>() {
                                @Override
                                public QueryFilter<JsonPointer> apply(final QueryFilter<JsonPointer> queryFilter) {
                                    return queryFilter.accept(UniqueConstraintQueryFilterVisitor.this, resource);
                                }
                            }
                    )
                    .toList();
        }
    }
}
