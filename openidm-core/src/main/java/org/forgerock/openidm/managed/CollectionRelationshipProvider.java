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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openidm.managed;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_REVISION;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.openidm.util.ResourceUtil.notSupportedOnCollection;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.util.promise.Promises.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.SortKey;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpUtils;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RelationshipProvider} representing a collection (array) of relationships for the given field.
 */
class CollectionRelationshipProvider extends RelationshipProvider implements CollectionResourceProvider {

    /**
     * Setup logging for the {@link CollectionRelationshipProvider}.
     */
    private static final Logger logger = LoggerFactory.getLogger(CollectionRelationshipProvider.class);
    
    final static QueryFilterVisitor<QueryFilter<JsonPointer>, Boolean, JsonPointer> VISITOR = new RelationshipQueryFilterVisitor();

    private final RequestHandler requestHandler;

    /**
     * Create a new relationship set for the given managed resource
     * @param connectionFactory Connection factory used to access the repository
     * @param resourcePath Name of the resource we are handling relationships for eg. managed/user
     * @param propertyName Name of property on first object represents the relationship
     */
    public CollectionRelationshipProvider(final ConnectionFactory connectionFactory, final ResourcePath resourcePath, 
            final JsonPointer propertyName, final boolean isReverse, ActivityLogger activityLogger,
            final ManagedObjectSyncService managedObjectSyncService) {
        super(connectionFactory, resourcePath, propertyName, isReverse, activityLogger,managedObjectSyncService);

        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, uriTemplate("{firstId}/" + propertyName.leaf()), Resources.newCollection(this));
        this.requestHandler = router;
    }

    /** {@inheritDoc} */
    @Override
    public RequestHandler asRequestHandler() {
        return requestHandler;
    }

    /** {@inheritDoc} */
    @Override
    public Promise<JsonValue, ResourceException> getRelationshipValueForResource(final Context context, final String resourceId) {
        try {
            final QueryRequest queryRequest = Requests.newQueryRequest("");
            queryRequest.setAdditionalParameter(PARAM_FIRST_ID, resourceId);
            final List<ResourceResponse> relationships = new ArrayList<>();

            queryCollection(new ManagedObjectSetContext(context), queryRequest, new QueryResourceHandler() {
                @Override
                public boolean handleResource(ResourceResponse resourceResponse) {
                    relationships.add(resourceResponse);
                    return true;
                }
            }).getOrThrowUninterruptibly(); // call get() so we block until we have all items

            final JsonValue buf = json(array());

            for (ResourceResponse resource : relationships) {
                buf.add(resource.getContent().getObject());
            }

            return newResultPromise(buf);
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /** {@inheritDoc} */
    // create/update relationship references. Update if we have UUIDs
    // delete all relationships not in this array
    @Override
    public Promise<JsonValue, ResourceException> setRelationshipValueForResource(Context context, String resourceId, JsonValue value) {
        value.expect(List.class);

        // Set of relation ids for updating (don't delete)
        final Set<String> relationshipsToKeep = new HashSet<>();

        // Set of relationships to perform an update on (have an _id)
        final List<JsonValue> relationshipsToUpdate = new ArrayList<>();

        // Set of relationships to create (no _id field)
        final List<JsonValue> relationshipsToCreate = new ArrayList<>();

        // JsonValue array to contain persisted relations
        final JsonValue results = json(array());

        try {
            if (value.isNotNull()) {
                // Split relationships in to to-be-updated (_id present) and to-be-created
                for (JsonValue relationship : value) {
                    final JsonValue id =
                            relationship.get(FIELD_ID);
                    if (id != null && id.isNotNull()) { // need update
                        relationshipsToUpdate.add(relationship);
                        relationshipsToKeep.add(id.asString());
                    } else { // no id. create
                        relationshipsToCreate.add(relationship);
                    }
                }

                // Call get() so we block until they are deleted.
                clearNotIn(context, resourceId, relationshipsToKeep).getOrThrowUninterruptibly();
            } else {
                // We didn't get any relations to persist. Clear and return empty array.
                clear(context, resourceId);
                return newResultPromise(results);
            }

            /*
             * Create or update relationships
             */

            // List of promises returned by update and create to when() on later
            final List<Promise<ResourceResponse, ResourceException>> promises = new ArrayList<>();

            for (JsonValue toUpdate : relationshipsToUpdate) {
                final UpdateRequest updateRequest = Requests.newUpdateRequest("", toUpdate);
                updateRequest.setAdditionalParameter(PARAM_FIRST_ID, resourceId);
                promises.add(updateInstance(context, toUpdate.get(FIELD_ID).asString(), updateRequest));
            }

            for (JsonValue toCreate : relationshipsToCreate) {
                final CreateRequest createRequest = Requests.newCreateRequest("", toCreate);
                createRequest.setAdditionalParameter(PARAM_FIRST_ID, resourceId);
                promises.add(createInstance(context, createRequest));
            }

            return when(promises).then(new Function<List<ResourceResponse>, JsonValue, ResourceException>() {
                @Override
                public JsonValue apply(List<ResourceResponse> responses) throws ResourceException {
                    final JsonValue value = json(array());
                    for (final ResourceResponse response : responses) {
                        value.add(response.getContent().getObject());
                    }
                    return value;
                }
            });
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /**
     * Clear all relationships not present in {@code relationshipsToKeep}.
     *
     * @param context The current context.
     * @param resourceId The resource whose relationships we wish to clear
     * @param relationshipsToKeep Set of relationship ids that should not be deleted
     * @return A promised JsonValue array of delete responses
     */
    private Promise<JsonValue, ResourceException> clearNotIn(final Context context, final String resourceId,
            final Set<String> relationshipsToKeep) {
        return getRelationshipValueForResource(context, resourceId).thenAsync(new AsyncFunction<JsonValue, JsonValue, ResourceException>() {
            @Override
            public Promise<JsonValue, ResourceException> apply(JsonValue existingRelationships) throws ResourceException {
                final List<Promise<ResourceResponse, ResourceException>> promises = new ArrayList<>();

                for (JsonValue relationship : existingRelationships) {
                    final String id = relationship.get(FIELD_ID).asString();

                    // Delete if we're not told to keep this id
                    if (!relationshipsToKeep.contains(id)) {
                        final DeleteRequest deleteRequest = Requests.newDeleteRequest("", id);
                        promises.add(deleteInstance(context, id, deleteRequest));
                    }
                }

                return when(promises).then(new Function<List<ResourceResponse>, JsonValue, ResourceException>() {
                    @Override
                    public JsonValue apply(List<ResourceResponse> resourceResponses) throws ResourceException {
                        final JsonValue result = json(array());
                        for (ResourceResponse resourceResponse : resourceResponses) {
                            result.add(resourceResponse.getContent());
                        }
                        return result;
                    }
                });
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<JsonValue, ResourceException> clear(final Context context, final String resourceId) {
        /*
         * FIXME - Performance here is terrible. We read every relationship just to get the id to delete.
         *         Need a deleteByQueryFilter() to remove all relationships for a given firstId
         */
        return getRelationshipValueForResource(context, resourceId).thenAsync(new AsyncFunction<JsonValue, JsonValue, ResourceException>() {
            @Override
            public Promise<JsonValue, ResourceException> apply(JsonValue existing) throws ResourceException {
                final List<Promise<ResourceResponse, ResourceException>> deleted = new ArrayList<>();

                for (JsonValue relationship : existing) {
                    final String id = relationship.get(FIELD_ID).asString();
                    deleted.add(deleteInstance(context, id, Requests.newDeleteRequest("")));
                }

                return when(deleted).then(new Function<List<ResourceResponse>, JsonValue, ResourceException>() {
                    @Override
                    public JsonValue apply(List<ResourceResponse> resourceResponses) throws ResourceException {
                        final JsonValue deleted = json(array());
                        for (ResourceResponse resourceResponse : resourceResponses) {
                            deleted.add(resourceResponse.getContent());
                        }
                        return deleted;
                    }
                });
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(final Context context, final CreateRequest request) {
        return super.createInstance(context, request);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId, ReadRequest request) {
        return super.readInstance(context, resourceId, request);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId, UpdateRequest request) {
        return super.updateInstance(context, resourceId, request);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, final String resourceId, final DeleteRequest request) {
        return super.deleteInstance(context, resourceId, request);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId, PatchRequest request) {
        return super.patchInstance(context, resourceId, request);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return notSupportedOnCollection(request).asPromise();
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId, ActionRequest request) {
        return super.actionInstance(context, resourceId, request);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(final Context context, final QueryRequest request, final QueryResourceHandler handler) {
        try {
            if (request.getQueryExpression() != null) {
                return new BadRequestException(HttpUtils.PARAM_QUERY_EXPRESSION + " not supported").asPromise();
            }

            /*
             * Create new request copying all attributes but fields.
             * This must be done so field filtering can be handled by CREST externally on
             * the transformed resource response.
             */
            final QueryRequest queryRequest = Requests.newQueryRequest(REPO_RESOURCE_PATH);
            final boolean queryAllIds = ServerConstants.QUERY_ALL_IDS.equals(request.getQueryId());

            if (request.getQueryId() != null) {
                request.setQueryFilter(QueryFilter.<JsonPointer>alwaysTrue());
                if (ServerConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                    // This should be the only field ever set on queryRequest
                    request.addField(FIELD_ID);
                } else if (!"query-all".equals(request.getQueryId())) {
                    return new BadRequestException("Invalid " + HttpUtils.PARAM_QUERY_ID + ": only query-all and query-all-ids supported").asPromise();
                }
            }

            queryRequest.setQueryFilter(request.getQueryFilter());
            queryRequest.setPageSize(request.getPageSize());
            queryRequest.setPagedResultsOffset(request.getPagedResultsOffset());
            queryRequest.setPagedResultsCookie(request.getPagedResultsCookie());
            queryRequest.setTotalPagedResultsPolicy(request.getTotalPagedResultsPolicy());
            queryRequest.addSortKey(request.getSortKeys().toArray(new SortKey[request.getSortKeys().size()]));
            for (String key : request.getAdditionalParameters().keySet()) {
                queryRequest.setAdditionalParameter(key, request.getAdditionalParameter(key));
            }

            QueryFilter<JsonPointer> filter = QueryFilter.and(
                    QueryFilter.equalTo(new JsonPointer(isReverse ? REPO_FIELD_SECOND_ID : REPO_FIELD_FIRST_ID), firstResourcePath(context, request)),
                    QueryFilter.equalTo(new JsonPointer(REPO_FIELD_FIRST_PROPERTY_NAME), propertyName));

            if (request.getQueryFilter() != null) {
                filter = QueryFilter.and(filter, asRelationshipQueryFilter(request.getQueryFilter()));
            }

            queryRequest.setQueryFilter(filter);
            
            final Promise<QueryResponse, ResourceException> response = getConnection().queryAsync(context, queryRequest, 
                    new QueryResourceHandler() {
                @Override
                public boolean handleResource(ResourceResponse resource) {
                    ResourceResponse filteredResourceResponse = FORMAT_RESPONSE_NO_EXCEPTION.apply(resource);
                    if (queryAllIds) {
                        // Special case, return just the ids, no expansion
                        filteredResourceResponse.addField(FIELD_ID);
                        return handler.handleResource(filteredResourceResponse);
                    }
                    try {
                        filteredResourceResponse = 
                                expandFields(context, request, filteredResourceResponse).getOrThrow();
                    } catch (Exception e) {
                        logger.error("Error expanding resource: " + e.getMessage(), e);
                    }
                    return handler.handleResource(filteredResourceResponse);
                }
            });
            
            if (context.containsContext(ManagedObjectSetContext.class)) {
                return response;   
            }
            
            QueryResponse result = response.getOrThrow();
            
            // Get the value of the managed object
            final ResourceResponse value = getManagedObject(context);
            
            // Do activity logging.
            activityLogger.log(context, request, "query", getManagedObjectPath(context), null, value.getContent(), 
                    Status.SUCCESS);
            
            return newResultPromise(result);
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    static QueryFilter<JsonPointer> asRelationshipQueryFilter(QueryFilter<JsonPointer> filter) {
        return filter.accept(VISITOR, null);
    }

    /**
     * A {@link QueryFilterVisitor} implementation which modifies the {@link JsonPointer} fields by prepending them
     * with the appropriate key where the full config object is located.
     */
    private static class RelationshipQueryFilterVisitor implements QueryFilterVisitor<QueryFilter<JsonPointer>, Boolean, JsonPointer> {

        @Override
        public QueryFilter<JsonPointer> visitAndFilter(Boolean isReverse, List<QueryFilter<JsonPointer>> subFilters) {
            return QueryFilter.and(visitQueryFilters(subFilters));
        }

        @Override
        public QueryFilter<JsonPointer> visitBooleanLiteralFilter(Boolean isReverse, boolean value) {
            return value ? QueryFilter.<JsonPointer>alwaysTrue() : QueryFilter.<JsonPointer>alwaysFalse();
        }

        @Override
        public QueryFilter<JsonPointer> visitContainsFilter(Boolean isReverse, JsonPointer field, Object valueAssertion) {
            return QueryFilter.contains(getRelationshipPointer(isReverse, field), valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitEqualsFilter(Boolean isReverse, JsonPointer field, Object valueAssertion) {
            return QueryFilter.equalTo(getRelationshipPointer(isReverse, field), valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitExtendedMatchFilter(Boolean isReverse, JsonPointer field, String operator, Object valueAssertion) {
            return QueryFilter.comparisonFilter(getRelationshipPointer(isReverse, field), operator, valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitGreaterThanFilter(Boolean isReverse, JsonPointer field, Object valueAssertion) {
            return QueryFilter.greaterThan(getRelationshipPointer(isReverse, field), valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitGreaterThanOrEqualToFilter(Boolean isReverse, JsonPointer field, Object valueAssertion) {
            return QueryFilter.greaterThanOrEqualTo(getRelationshipPointer(isReverse, field), valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitLessThanFilter(Boolean isReverse, JsonPointer field, Object valueAssertion) {
            return QueryFilter.lessThan(getRelationshipPointer(isReverse, field), valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitLessThanOrEqualToFilter(Boolean isReverse, JsonPointer field, Object valueAssertion) {
            return QueryFilter.lessThanOrEqualTo(getRelationshipPointer(isReverse, field), valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitNotFilter(Boolean isReverse, QueryFilter<JsonPointer> subFilter) {
            return QueryFilter.not(subFilter.accept(new RelationshipQueryFilterVisitor(), null));
        }

        @Override
        public QueryFilter<JsonPointer> visitOrFilter(Boolean isReverse, List<QueryFilter<JsonPointer>> subFilters) {
            return QueryFilter.or(visitQueryFilters(subFilters));
        }

        @Override
        public QueryFilter<JsonPointer> visitPresentFilter(Boolean isReverse, JsonPointer field) {
            return QueryFilter.present(getRelationshipPointer(isReverse, field));
        }

        @Override
        public QueryFilter<JsonPointer> visitStartsWithFilter(Boolean isReverse, JsonPointer field, Object valueAssertion) {
            return QueryFilter.startsWith(getRelationshipPointer(isReverse, field), valueAssertion);
        }

        /**
         * Visits each {@link QueryFilter} in a list of filters and returns a list of the
         * visited filters.
         *
         * @param subFilters a list of the filters to visit
         * @return a list of visited filters
         */
        private List<QueryFilter<JsonPointer>> visitQueryFilters(List<QueryFilter<JsonPointer>> subFilters) {
            List<QueryFilter<JsonPointer>> visitedFilters = new ArrayList<>();
            for (QueryFilter<JsonPointer> filter : subFilters) {
                visitedFilters.add(asRelationshipQueryFilter(filter));
            }
            return visitedFilters;
        }

        /**
         * Converts relationship client object pointers to repo format.
         *
         * Converts /_refProperties/_id to /_id
         * Converts /_refProperties/_rev to /_rev
         * Converts /_ref to /secondId
         * Converts /_refProperties/... to /properties/...
         *
         * @param field a {@link JsonPointer} representing the field to modify.
         * @return a {@link JsonPointer} representing the modified field
         */
        private JsonPointer getRelationshipPointer(Boolean isReverse, JsonPointer field) {
            // /_revProperties/_id to /_id
            if (FIELD_ID.equals(field)) {
                return new JsonPointer(FIELD_CONTENT_ID);
            }
            // /_refProperties/_rev to /_rev
            if (FIELD_REV.equals(field)) {
                return new JsonPointer(FIELD_CONTENT_REVISION);
            }

            if (isReverse) {
                // /_ref to /firstId
                if (FIELD_REFERENCE.equals(field)) {
                    return new JsonPointer(REPO_FIELD_FIRST_ID);
                }
            } else {
                // /_ref to /secondId
                if (FIELD_REFERENCE.equals(field)) {
                    return new JsonPointer(REPO_FIELD_SECOND_ID);
                }
            }

            // /_ref to /secondId
            // /_refProperties/... to /properties/...
            if (FIELD_PROPERTIES.leaf().equals(field.get(0))) {
                JsonPointer ptr = new JsonPointer(REPO_FIELD_PROPERTIES);
                for (String s : field.relativePointer(field.size() - 1)) {
                    ptr = ptr.child(s);
                }
                return ptr;
            }

            // TODO: OPENIDM-4153 don't expose direct repo properties
            return field;
        }
    }
}
