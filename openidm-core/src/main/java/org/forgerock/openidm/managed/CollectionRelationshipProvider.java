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
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openidm.util.ResourceUtil.notSupportedOnCollection;
import static org.forgerock.openidm.util.ResourceUtil.notSupportedOnInstance;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.util.promise.Promises.when;

import org.forgerock.http.Context;
import org.forgerock.http.ResourcePath;
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
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.UpdateRequest;
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
public class CollectionRelationshipProvider extends RelationshipProvider implements CollectionResourceProvider {
    /**
     * Create a new relationship set for the given managed resource
     * @param connectionFactory Connection factory used to access the repository
     * @param resourcePath Name of the resource we are handling relationships for eg. managed/user
     * @param propertyName Name of property on first object represents the relationship
     */
    public CollectionRelationshipProvider(final ConnectionFactory connectionFactory, final ResourcePath resourcePath, final JsonPointer propertyName) {
        super(connectionFactory, resourcePath, propertyName);
    }

    @Override
    public RequestHandler asRequestHandler() {
        return Resources.newCollection(this);
    }

    @Override
    public Promise<JsonValue, ResourceException> fetchJson(final Context context, final String resourceId) {
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

    // create/update relationship references. Update if we have UUIDs
    // delete all relationships not in this array
    @Override
    public Promise<JsonValue, ResourceException> persistJson(Context context, String resourceId, JsonValue value) {
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
            if (value != null && value.isNotNull()) {
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


    private Promise<JsonValue, ResourceException> clearNotIn(final Context context, final String resourceId,
            final Set<String> relationshipsToKeep) {
        return fetchJson(context, resourceId).thenAsync(new AsyncFunction<JsonValue, JsonValue, ResourceException>() {
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
                        final JsonValue result = json(object());
                        for (ResourceResponse resourceResponse : resourceResponses) {
                            result.add(resourceResponse.getContent());
                        }
                        return result;
                    }
                });
            }
        });
    }

    @Override
    public Promise<JsonValue, ResourceException> clear(final Context context, final String resourceId) {
        /*
         * FIXME - Performance here is terrible. We read every relationship just to get the id to delete.
         *         Need a deleteByQueryFilter() to remove all relationships for a given firstId
         */
        return fetchJson(context, resourceId).thenAsync(new AsyncFunction<JsonValue, JsonValue, ResourceException>() {
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
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return notSupportedOnCollection(request).asPromise();
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId, ActionRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(final Context context, final CreateRequest request) {
        try {
            final CreateRequest createRequest = Requests.copyOfCreateRequest(request);
            createRequest.setResourcePath(REPO_RESOURCE_PATH);
            createRequest.setContent(convertToRepoObject(firstResourcePath(context, request), request.getContent()));

            return connectionFactory.getConnection().createAsync(context, createRequest).then(FORMAT_RESPONSE);
        } catch (ResourceException e) {
            return e.asPromise();
        }

    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, final String resourceId, final DeleteRequest request) {
        final ResourcePath path = REPO_RESOURCE_PATH.child(resourceId);
        final DeleteRequest deleteRequest = Requests.copyOfDeleteRequest(request);
        deleteRequest.setResourcePath(path);

        try {
            if (deleteRequest.getRevision() == null) {
                /*
                 * If no revision was supplied we must perform a read to get the latest revision
                 */

                final ReadRequest readRequest = Requests.newReadRequest(path);
                final Promise<ResourceResponse, ResourceException> readResult = connectionFactory.getConnection().readAsync(context, readRequest);

                return readResult.thenAsync(new AsyncFunction<ResourceResponse, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(ResourceResponse resourceResponse) throws ResourceException {
                        deleteRequest.setRevision(resourceResponse.getRevision());
                        return connectionFactory.getConnection().deleteAsync(context, deleteRequest).then(FORMAT_RESPONSE);
                    }
                });
            } else {
                return connectionFactory.getConnection().deleteAsync(context, deleteRequest).then(FORMAT_RESPONSE);
            }
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId, PatchRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    /** {@inheritDoc} */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(final Context context, final QueryRequest request, final QueryResourceHandler handler) {
        try {
            final QueryRequest queryRequest = Requests.newQueryRequest(REPO_RESOURCE_PATH);

            queryRequest.setQueryFilter(QueryFilter.and(
                    QueryFilter.equalTo(new JsonPointer(REPO_FIELD_FIRST_ID), firstResourcePath(context, request)),
                    QueryFilter.equalTo(new JsonPointer(REPO_FIELD_FIRST_PROPERTY_NAME), propertyName),
                    request.getQueryFilter()
            ));
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

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId, ReadRequest request) {
        try {
            final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_PATH.child(resourceId));
            return connectionFactory.getConnection().readAsync(context, readRequest).then(FORMAT_RESPONSE);
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

}
