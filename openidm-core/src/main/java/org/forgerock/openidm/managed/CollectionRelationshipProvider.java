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
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.openidm.util.ResourceUtil.notSupportedOnCollection;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.util.promise.Promises.when;

import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link RelationshipProvider} representing a collection (array) of relationships for the given field.
 */
class CollectionRelationshipProvider extends RelationshipProvider implements CollectionResourceProvider {
    private final RequestHandler requestHandler;

    /**
     * Create a new relationship set for the given managed resource
     * @param connectionFactory Connection factory used to access the repository
     * @param resourcePath Name of the resource we are handling relationships for eg. managed/user
     * @param propertyName Name of property on first object represents the relationship
     */
    public CollectionRelationshipProvider(final ConnectionFactory connectionFactory, final ResourcePath resourcePath, final JsonPointer propertyName) {
        super(connectionFactory, resourcePath, propertyName);

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

            queryCollection(context, queryRequest, new QueryResourceHandler() {
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
                            relationship.get(CollectionRelationshipProvider.FIELD_PROPERTIES.child("_id"));
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
                promises.add(updateInstance(context, toUpdate.get(CollectionRelationshipProvider.FIELD_PROPERTIES.child("_id")).asString(), updateRequest));
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
                    final String id = relationship.get(SchemaField.FIELD_PROPERTIES).get("_id").asString();

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
                    final String id = relationship.get(SchemaField.FIELD_PROPERTIES).get("_id").asString();
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
            final QueryRequest queryRequest = Requests.copyOfQueryRequest(request);
            queryRequest.setResourcePath(REPO_RESOURCE_PATH);
            QueryFilter<JsonPointer> filter = QueryFilter.and(
                    QueryFilter.equalTo(new JsonPointer(REPO_FIELD_FIRST_ID), firstResourcePath(context, request)),
                    QueryFilter.equalTo(new JsonPointer(REPO_FIELD_FIRST_PROPERTY_NAME), propertyName));

            if (request.getQueryFilter() != null) {
                filter.and(request.getQueryFilter());
            }

            queryRequest.setQueryFilter(filter);
            return connectionFactory.getConnection().queryAsync(context, queryRequest, new QueryResourceHandler() {
                @Override
                public boolean handleResource(ResourceResponse resource) {
                    return handler.handleResource(FORMAT_RESPONSE_NO_EXCEPTION.apply(resource));
                }
            });
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }
}
