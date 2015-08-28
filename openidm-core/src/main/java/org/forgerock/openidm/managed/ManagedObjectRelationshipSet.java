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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.newNotSupportedException;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

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
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Set of relationships for a given managed resource's property
 */
public class ManagedObjectRelationshipSet implements CollectionResourceProvider {
    /** Used for accessing the repo */
    private ConnectionFactory connectionFactory;

    /** Path to this resource in the repo */
    private static final ResourcePath REPO_RESOURCE_CONTAINER = new ResourcePath("repo", "relationships");

    /** Name of the source resource these relationships are "edges" of */
    private final ResourcePath resourceName;

    /** The property representing this relationship */
    private final JsonPointer propertyName;

    /**
     * Function to format resources from the repository
     */
    private static final Function<ResourceResponse, ResourceResponse, ResourceException> FORMAT_RESPONSE =
            new Function<ResourceResponse, ResourceResponse, ResourceException>() {
                @Override
                public ResourceResponse apply(ResourceResponse resourceResponse) throws ResourceException {
                    return FORMAT_RESPONSE_NO_EXCEPTION.apply(resourceResponse);
                }
            };

    /**
     * Function to format resources from the repository
     */
    private static final Function<ResourceResponse, ResourceResponse, NeverThrowsException> FORMAT_RESPONSE_NO_EXCEPTION =
            new Function<ResourceResponse, ResourceResponse, NeverThrowsException>() {
                @Override
                public ResourceResponse apply(final ResourceResponse raw) {
                    final JsonValue formatted = json(object());
                    final Map<String, Object> properties = new LinkedHashMap<>();
                    final Map<String, Object> repoProperties = raw.getContent().get("_properties").asMap();

                    if (repoProperties != null) {
                        properties.putAll(repoProperties);
                    }

                    properties.put("_id", raw.getId());
                    properties.put("_rev", raw.getRevision());

                    formatted.put(SchemaField.FIELD_REFERENCE, raw.getContent().get("secondId").asString());
                    formatted.put(SchemaField.FIELD_PROPERTIES, properties);

                    return newResourceResponse(raw.getId(), raw.getRevision(), formatted);
                }
            };

    /**
     * Create a new relationship set for the given managed resource
     * @param connectionFactory Connection factory used to access the repository
     * @param resourceName Name of the resource we are handling relationships for eg. managed/user
     * @param propertyName Name of property on first object represents the relationship
     */
    public ManagedObjectRelationshipSet(final ConnectionFactory connectionFactory, final ResourcePath resourceName, final JsonPointer propertyName) {
        this.connectionFactory = connectionFactory;
        this.resourceName = resourceName;
        this.propertyName = propertyName;
    }

//    /**
//     * Fetch relationship for the given resource
//     * @param resource
//     * @param resourceId
//     * @param key
//     * @return
//     */
//    public JsonValue getRelations(ResourceName resource, String resourceId, String key) {
//        final QueryRequest request = Requests.newQueryRequest(REPO_PREFIX);
//        final QueryResult result = connectionFactory.getConnection().queryAsync(null, request, null).get()
//
//    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return newExceptionPromise(newNotSupportedException("ACTION not supported on relationship collections"));
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId, ActionRequest request) {
        return newExceptionPromise(newNotSupportedException("ACTION not supported on relationship instance"));
    }

    /**
     * Convert the given incoming request object to repo format.
     *
     * This converts _ref fields to secondId and populates first* fields.
     *
     * @param object
     * @return A new JsonValue containing the converted object
     */
    private JsonValue convertToRepoObject(final JsonValue object) {
        return json(object(
                field("firstId", resourceName.child(object.get("firstId").asString()).toString()),
                field("firstKey", propertyName.toString()),
                field("secondId", object.get(SchemaField.FIELD_REFERENCE)),
                field("_properties", object.get(SchemaField.FIELD_PROPERTIES))
        ));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(final Context context, final CreateRequest request) {
        final CreateRequest createRequest = Requests.copyOfCreateRequest(request);
        createRequest.setResourcePath(REPO_RESOURCE_CONTAINER);
        createRequest.setContent(convertToRepoObject(request.getContent()));

        try {
            return connectionFactory.getConnection().createAsync(context, createRequest).then(FORMAT_RESPONSE);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }

    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, String resourceId, final DeleteRequest _request) {
        final DeleteRequest deleteRequest = Requests.copyOfDeleteRequest(_request);
        deleteRequest.setResourcePath(REPO_RESOURCE_CONTAINER.child(_request.getResourcePath()));

        try {
            if (deleteRequest.getRevision() == null) {
                /*
                 * If no revision was supplied we must perform a read to get the latest revision
                 */

                final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_CONTAINER.child(resourceId));
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
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId, PatchRequest request) {
        return newExceptionPromise(newNotSupportedException("PATCH currently not supported on relationships"));
    }

    /** {@inheritDoc} */
    // GET /managed/user/{firstId}/{firstKey}
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(final Context context, final QueryRequest request, final QueryResourceHandler handler) {
        final String firstId = request.getAdditionalParameter("firstId");

        final QueryRequest _request = Requests.newQueryRequest(REPO_RESOURCE_CONTAINER);

        _request.setQueryFilter(QueryFilter.and(
                QueryFilter.equalTo(new JsonPointer("firstId"), resourceName.child(firstId)),
                QueryFilter.equalTo(new JsonPointer("firstKey"), propertyName)
        ));

        try {
            return connectionFactory.getConnection().queryAsync(context, _request, new QueryResourceHandler() {
                @Override
                public boolean handleResource(ResourceResponse resource) {
                    return handler.handleResource(FORMAT_RESPONSE_NO_EXCEPTION.apply(resource));
                }
            });
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId, ReadRequest request) {
        try {
            final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_CONTAINER.child(resourceId));
            return connectionFactory.getConnection().readAsync(context, readRequest).then(FORMAT_RESPONSE);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(final Context context, final String resourceId, final UpdateRequest request) {
        try {
            final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_CONTAINER.child(resourceId));
            final JsonValue newValue = request.getContent();

            // current resource in the db
            final Promise<ResourceResponse, ResourceException> promisedOldResult = connectionFactory.getConnection().readAsync(context, readRequest);

            // update once we have the current record
            return promisedOldResult.thenAsync(new AsyncFunction<ResourceResponse, ResourceResponse, ResourceException>() {
                @Override
                public Promise<ResourceResponse, ResourceException> apply(ResourceResponse oldResource) throws ResourceException {
                    final String rev = oldResource.getRevision();

                    if (newValue.asMap().equals(oldResource.getContent().asMap())) { // resource has not changed
                        return newResultPromise(newResourceResponse(resourceId, rev, null));
                    } else {
                        final UpdateRequest updateRequest = Requests.newUpdateRequest(REPO_RESOURCE_CONTAINER.child(resourceId), newValue);
                        updateRequest.setRevision(rev);

                        return connectionFactory.getConnection().updateAsync(context, updateRequest).then(FORMAT_RESPONSE);
                    }
                }
            });
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }
}
