package org.forgerock.openidm.managed;/*
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

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceName;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Set of relationships for a given managed resource's property
 */
public class ManagedObjectRelationshipSet implements CollectionResourceProvider {
    /** Used for accessing the repo */
    private ConnectionFactory connectionFactory;

    /** Path to this resource in the repo */
    private static final ResourceName REPO_RESOURCE_CONTAINER = new ResourceName("repo", "relationships");

    /** Name of the source resource these relationships are "edges" of */
    private final ResourceName resourceName;

    /** The property representing this relationship */
    private final JsonPointer propertyName;

    /**
     * Create a new relationship set for the given managed resource
     * @param connectionFactory Connection factory used to access the repository
     * @param resourceName Name of the resource we are handling relationships for eg. managed/user
     * @param propertyName Name of property on first object represents the relationship
     */
    public ManagedObjectRelationshipSet(final ConnectionFactory connectionFactory, final ResourceName resourceName, final JsonPointer propertyName) {
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
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("ACTION not supported on relationship collections"));
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("ACTION not supported on relationship instance"));
    }

    /**
     * Convert the given incoming request object to repo format.
     *
     * This converts _ref fields to secondId and populates first* fields.
     *
     * @param object
     * @return A new JsonValue containing the converted object
     */
    private JsonValue convertToRepoObject(JsonValue object) {
        final Map<String, Object> forRepo = new HashMap<>();
        final Map<String, Object> fromRequest = object.copy().asMap();

        forRepo.put("firstId", resourceName.child(fromRequest.get("firstId")).toString());
        forRepo.put("firstKey", propertyName.toString());
        forRepo.put("secondId", fromRequest.get("_ref"));
        forRepo.put("_properties", fromRequest.get("_properties"));

        return new JsonValue(forRepo);
    }

    @Override
    public void createInstance(final ServerContext context, final CreateRequest request, final ResultHandler<Resource> handler) {
        final CreateRequest createRequest = Requests.copyOfCreateRequest(request);
        createRequest.setResourceName(REPO_RESOURCE_CONTAINER);
        createRequest.setContent(convertToRepoObject(request.getContent()));

        try {
            final Resource resource = connectionFactory.getConnection().create(context, createRequest);
            handler.handleResult(resource);
        } catch (ResourceException e) {
            handler.handleError(e);
        }

    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, final DeleteRequest _request, ResultHandler<Resource> handler) {
        final DeleteRequest deleteRequest = Requests.copyOfDeleteRequest(_request);
        deleteRequest.setResourceName(REPO_RESOURCE_CONTAINER.child(_request.getResourceName()));

        try {
            if (deleteRequest.getRevision() == null) {
                final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_CONTAINER.child(resourceId));
                final Resource resource = connectionFactory.getConnection().read(context, readRequest);
                deleteRequest.setRevision(resource.getRevision());
            }

            final Resource deletedResource = connectionFactory.getConnection().delete(context, deleteRequest);

            handler.handleResult(deletedResource);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("PATCH currently not supported on relationships"));
    }

    /** {@inheritDoc} */
    // GET /managed/user/{firstId}/{firstKey}
    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request, final QueryResultHandler handler) {
        final String firstId = request.getAdditionalParameter("firstId");

        final QueryRequest _request = Requests.newQueryRequest(REPO_RESOURCE_CONTAINER);

        _request.setQueryFilter(QueryFilter.and(
                QueryFilter.equalTo("firstId", resourceName.child(firstId)),
                QueryFilter.equalTo("firstKey", propertyName.toString())
        ));

        try {
            connectionFactory.getConnection().query(context, _request, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_CONTAINER.child(resourceId));
            Resource resource = connectionFactory.getConnection().read(context, readRequest);
            handler.handleResult(resource);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request, ResultHandler<Resource> handler) {
        try {
            final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_CONTAINER.child(resourceId));
            final JsonValue newValue = request.getContent();

            // current resource in the db
            final Resource oldResource = connectionFactory.getConnection().read(context, readRequest);
            final String rev = request.getRevision();

            if (newValue.asMap().equals(oldResource.getContent().asMap())) { // resource has not changed
                handler.handleResult(new Resource(resourceId, rev, null));
            } else {
                final UpdateRequest updateRequest = Requests.newUpdateRequest(REPO_RESOURCE_CONTAINER.child(resourceId), newValue);
                updateRequest.setRevision(rev);

                final Resource response = connectionFactory.getConnection().update(context, updateRequest);

                handler.handleResult(response);
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }
}
