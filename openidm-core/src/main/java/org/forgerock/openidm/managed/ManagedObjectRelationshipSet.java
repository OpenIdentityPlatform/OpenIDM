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

import org.apache.commons.lang3.StringUtils;
import org.forgerock.http.Context;
import org.forgerock.http.ResourcePath;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
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
    final private ConnectionFactory connectionFactory;

    /** Path to this resource in the repo */
    private static final ResourcePath REPO_RESOURCE_PATH = new ResourcePath("repo", "relationships");

    /** Name of the source resource these relationships are "edges" of */
    private final ResourcePath resourcePath;

    /** The property representing this relationship */
    private final JsonPointer propertyName;

    /** The name of the firstId field in the repo */
    private static final String REPO_FIELD_FIRST_ID = "firstId";
    /** The name of the firstPropertyName field in the repo */
    private static final String REPO_FIELD_FIRST_PROPERTY_NAME = "firstPropertyName";
    /** The name of the properties field coming out of the repo service */
    private static final String REPO_FIELD_PROPERTIES = "properties";
    /** The name of the secondId field in the repo */
    private static final String REPO_FIELD_SECOND_ID = "secondId";

    /** The name of the firstId parameter to be used in the uri template */
    public static final String URI_PARAM_FIRST_ID = REPO_FIELD_FIRST_ID;

    /** The name of the firstId parameter to be used in request parameters */
    public static final String PARAM_FIRST_ID = REPO_FIELD_FIRST_ID;

    /** The name of the properties field in resource response */
    public static final JsonPointer FIELD_PROPERTIES = SchemaField.FIELD_PROPERTIES;
    /** The name of the secondId field in resource response */
    public static final JsonPointer FIELD_REFERENCE = SchemaField.FIELD_REFERENCE;

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
                    final Map<String, Object> repoProperties = raw.getContent().get(REPO_FIELD_PROPERTIES).asMap();

                    if (repoProperties != null) {
                        properties.putAll(repoProperties);
                    }

                    properties.put("_id", raw.getId());
                    properties.put("_rev", raw.getRevision());

                    formatted.put(SchemaField.FIELD_REFERENCE, raw.getContent().get(REPO_FIELD_SECOND_ID).asString());
                    formatted.put(SchemaField.FIELD_PROPERTIES, properties);

                    return newResourceResponse(raw.getId(), raw.getRevision(), formatted);
                }
            };

    /**
     * Create a new relationship set for the given managed resource
     * @param connectionFactory Connection factory used to access the repository
     * @param resourcePath Name of the resource we are handling relationships for eg. managed/user
     * @param propertyName Name of property on first object represents the relationship
     */
    public ManagedObjectRelationshipSet(final ConnectionFactory connectionFactory, final ResourcePath resourcePath, final JsonPointer propertyName) {
        this.connectionFactory = connectionFactory;
        this.resourcePath = resourcePath;
        this.propertyName = propertyName;
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return newExceptionPromise(newNotSupportedException("ACTION not supported on relationship collections"));
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId, ActionRequest request) {
        return newExceptionPromise(newNotSupportedException("ACTION not supported on relationship instance"));
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
            return newExceptionPromise(e);
        }

    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, String resourceId, final DeleteRequest request) {
        final DeleteRequest deleteRequest = Requests.copyOfDeleteRequest(request);
        deleteRequest.setResourcePath(REPO_RESOURCE_PATH.child(request.getResourcePath()));

        try {
            if (deleteRequest.getRevision() == null) {
                /*
                 * If no revision was supplied we must perform a read to get the latest revision
                 */

                final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_PATH.child(resourceId));
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

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId, PatchRequest request) {
        return newExceptionPromise(newNotSupportedException("PATCH currently not supported on relationships"));
    }

    /** {@inheritDoc} */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(final Context context, final QueryRequest request, final QueryResourceHandler handler) {
        try {
            final QueryRequest queryRequest = Requests.newQueryRequest(REPO_RESOURCE_PATH);

            queryRequest.setQueryFilter(QueryFilter.and(
                    QueryFilter.equalTo(new JsonPointer(REPO_FIELD_FIRST_ID), firstResourcePath(context, request)),
                    QueryFilter.equalTo(new JsonPointer(REPO_FIELD_FIRST_PROPERTY_NAME), propertyName)
            ));
            return connectionFactory.getConnection().queryAsync(context, queryRequest, new QueryResourceHandler() {
                @Override
                public boolean handleResource(ResourceResponse resource) {
                    return handler.handleResource(FORMAT_RESPONSE_NO_EXCEPTION.apply(resource));
                }
            });
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId, ReadRequest request) {
        try {
            final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_PATH.child(resourceId));
            return connectionFactory.getConnection().readAsync(context, readRequest).then(FORMAT_RESPONSE);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(final Context context, final String resourceId, final UpdateRequest request) {
        try {
            final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_PATH.child(resourceId));
            final JsonValue newValue = convertToRepoObject(firstResourcePath(context, request), request.getContent());

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
                        final UpdateRequest updateRequest = Requests.newUpdateRequest(REPO_RESOURCE_PATH.child(resourceId), newValue);
                        updateRequest.setRevision(rev);

                        return connectionFactory.getConnection().updateAsync(context, updateRequest).then(FORMAT_RESPONSE);
                    }
                }
            });
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    /**
     * Returns the path of the first resource in this relationship using the firstId parameter
     * from either the URI or the Request. If firstId is not found in the URI context then
     * the request parameter is used.
     *
     * @param context Context containing a {@link UriRouterContext} to check for template variables
     * @param request Request containing a fall-back firstId parameter
     *
     * @see #resourcePath
     *
     * @return The resource path of the first resource as a child of resourcePath
     */
    private final ResourcePath firstResourcePath(final Context context, final Request request)
            throws BadRequestException {
        final String uriFirstId =
                context.asContext(UriRouterContext.class).getUriTemplateVariables().get(URI_PARAM_FIRST_ID);
        final String firstId = uriFirstId != null ? uriFirstId : request.getAdditionalParameter(PARAM_FIRST_ID);

        if (StringUtils.isNotBlank(firstId)) {
            return resourcePath.child(firstId);
        } else {
            throw new BadRequestException("Required either URI parameter " + URI_PARAM_FIRST_ID +
                    " or request paremeter " + PARAM_FIRST_ID + " but none were found.");
        }
    }

    /**
     * Convert the given incoming request object to repo format.
     *
     * This converts _ref fields to secondId and populates first* fields.
     *
     * @param firstResourcePath The path of the first object in a relationship instance
     * @param object A {@link JsonValue} object from a resource response or incoming request to be converted for
     *               storage in the repo
     *
     * @return A new JsonValue containing the converted object in a format accepted by the repo
     */
    private JsonValue convertToRepoObject(final ResourcePath firstResourcePath, final JsonValue object) {
        final JsonValue properties = object.get(FIELD_PROPERTIES);

        if (properties != null) {
            // Remove "soft" fields that were placed in properties for the ResourceResponse
            properties.remove("_id");
            properties.remove("_rev");
        }

        return json(object(
                field(REPO_FIELD_FIRST_ID, firstResourcePath.toString()),
                field(REPO_FIELD_FIRST_PROPERTY_NAME, propertyName.toString()),
                field(REPO_FIELD_SECOND_ID, object.get(FIELD_REFERENCE)),
                field(REPO_FIELD_PROPERTIES, properties)
        ));
    }
}
