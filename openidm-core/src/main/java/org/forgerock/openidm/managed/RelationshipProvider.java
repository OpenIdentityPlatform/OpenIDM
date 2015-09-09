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
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.util.ResourceUtil.notSupportedOnInstance;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.http.Context;
import org.forgerock.http.ResourcePath;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class RelationshipProvider {
    /** Used for accessing the repo */
    protected final ConnectionFactory connectionFactory;

    /** Path to this resource in the repo */
    protected static final ResourcePath REPO_RESOURCE_PATH = new ResourcePath("repo", "relationships");

    /** Name of the source resource these relationships are "edges" of */
    protected final ResourcePath resourcePath;

    /** The property representing this relationship */
    protected final JsonPointer propertyName;

    /** The name of the firstId field in the repo */
    protected static final String REPO_FIELD_FIRST_ID = "firstId";
    /** The name of the firstPropertyName field in the repo */
    protected static final String REPO_FIELD_FIRST_PROPERTY_NAME = "firstPropertyName";
    /** The name of the properties field coming out of the repo service */
    protected static final String REPO_FIELD_PROPERTIES = "properties";
    /** The name of the secondId field in the repo */
    protected static final String REPO_FIELD_SECOND_ID = "secondId";

    /** The name of the firstId parameter to be used in the uri template */
    public static final String URI_PARAM_FIRST_ID = REPO_FIELD_FIRST_ID;

    /** The name of the firstId parameter to be used in request parameters */
    public static final String PARAM_FIRST_ID = REPO_FIELD_FIRST_ID;

    /** The name of the properties field in resource response */
    public static final JsonPointer FIELD_PROPERTIES = SchemaField.FIELD_PROPERTIES;
    /** The name of the secondId field in resource response */
    public static final JsonPointer FIELD_REFERENCE = SchemaField.FIELD_REFERENCE;

    /**
     * Function to format a resource from the repository to that expected by the provider consumer. This
     * is simply a wrapper of {@link #FORMAT_RESPONSE_NO_EXCEPTION} with a {@link ResourceException}
     * in the signature to allow for use against {@code Promise<ResourceResponse, ResourceException>}
     *
     * @see #FORMAT_RESPONSE_NO_EXCEPTION
     */
    protected static final Function<ResourceResponse, ResourceResponse, ResourceException> FORMAT_RESPONSE =
            new Function<ResourceResponse, ResourceResponse, ResourceException>() {
                @Override
                public ResourceResponse apply(ResourceResponse resourceResponse) throws ResourceException {
                    return FORMAT_RESPONSE_NO_EXCEPTION.apply(resourceResponse);
                }
            };

     /**
     * Function to format a resource from the repository to that expected by the provider consumer. First object
     * properties are removed and {@code secondId} will be converted to {@code _ref}
     *
     * This will convert repo resources in the format of:
     * <pre>
     *     {
     *         "_id": "someId",
     *         "_rev": "someRev",
     *         "firstId": "/managed/object/uuid",
     *         "firstPropertyName": "roles",
     *         "secondId": "/managed/roles/uuid"
     *         "properties": { ... }
     *     }
     * </pre>
     *
     * To a provider response format of:
     * <pre>
     *     {
     *         "_ref": "/managed/roles/uuid"
     *         "_refProperties": {
     *             "_id": "someId",
     *             "_rev": "someRev",
     *             ...
     *         }
     *     }
     * </pre>
     */
    protected static final Function<ResourceResponse, ResourceResponse, NeverThrowsException> FORMAT_RESPONSE_NO_EXCEPTION =
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
     * Get a new {@link RelationshipProvider} instance associated with the given resource path and field
     *
     * @param connectionFactory The connection factory used to access the repository
     * @param resourcePath Path of the resource to provide relationships for
     * @param relationshipField Field on the resource representing the provided relationship
     * @return A new relationship provider instance
     */
    public static RelationshipProvider newProvider(final ConnectionFactory connectionFactory,
            final ResourcePath resourcePath, final SchemaField relationshipField) {
        if (relationshipField.isArray()) {
            return new CollectionRelationshipProvider(connectionFactory,
                    resourcePath, new JsonPointer(relationshipField.getName()));
        } else {
            return new SingletonRelationshipProvider(connectionFactory,
                    resourcePath, new JsonPointer(relationshipField.getName()));
        }
    }

    /**
     * Create a new relationship set for the given managed resource
     * @param connectionFactory Connection factory used to access the repository
     * @param resourcePath Name of the resource we are handling relationships for eg. managed/user
     * @param propertyName Name of property on first object represents the relationship
     */
    protected RelationshipProvider(final ConnectionFactory connectionFactory, final ResourcePath resourcePath, final JsonPointer propertyName) {
        this.connectionFactory = connectionFactory;
        this.resourcePath = resourcePath;
        this.propertyName = propertyName;
    }

    /**
     * Return a {@link RequestHandler} instance representing this provider
     * @return
     */
    public abstract RequestHandler asRequestHandler();

    /**
     * Fetch the full relationship representation for this provider as a JsonValue.
     *
     * @param context Context of this request
     * @param resourceId Id of resource to fetch relationships on
     *
     * @return A promise containing the full representation of the relationship on the supplied resourceId
     *         or a ResourceException if an error occurred
     */
    public abstract Promise<JsonValue, ResourceException> fetchJson(Context context, String resourceId);

    /**
     * Persist the supplied {@link JsonValue} as the current state of this relationship. This will support updating
     * any existing relationship (_id is present) and remove any relationship not present in the value from the
     * repository.
     *
     * @param context The context of this request
     * @param resourceId Id of the resource relation fields in value are to be memebers of
     * @param value A {@link JsonValue} map of relationship fields and their values
     *
     * @return A promise containing a JsonValue of the persisted relationship(s) for the given resourceId or
     *         ResourceException if an error occurred
     */
    public abstract Promise<JsonValue, ResourceException> persistJson(final Context context,
            final String resourceId, final JsonValue value);

    /**
     * Clear any relationship associated with the given resource. This could be used if for example a resource
     * no longer exists.
     *
     * @param context The current context.
     * @param resourceId The resource whose relationship we wish to clear
     *
     */
    public abstract Promise<JsonValue, ResourceException> clear(Context context, String resourceId);

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

    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String relationshipId, ReadRequest request) {
        try {
            final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_PATH.child(relationshipId));
            return connectionFactory.getConnection().readAsync(context, readRequest).then(FORMAT_RESPONSE);
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> updateInstance(final Context context, final String relationshipId, final UpdateRequest request) {
        try {
            final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_PATH.child(relationshipId));
            final JsonValue newValue = convertToRepoObject(firstResourcePath(context, request), request.getContent());

            // current resource in the db
            final Promise<ResourceResponse, ResourceException> promisedOldResult = connectionFactory.getConnection().readAsync(context, readRequest);

            // update once we have the current record
            return promisedOldResult.thenAsync(new AsyncFunction<ResourceResponse, ResourceResponse, ResourceException>() {
                @Override
                public Promise<ResourceResponse, ResourceException> apply(ResourceResponse oldResource) throws ResourceException {
                    if (newValue.asMap().equals(oldResource.getContent().asMap())) { // resource has not changed
                        return newResourceResponse(oldResource.getId(), oldResource.getRevision(), null).asPromise();
                    } else {
                        final UpdateRequest updateRequest = Requests.newUpdateRequest(REPO_RESOURCE_PATH.child(relationshipId), newValue);
                        updateRequest.setRevision(request.getRevision());

                        return connectionFactory.getConnection().updateAsync(context, updateRequest).then(FORMAT_RESPONSE);
                    }
                }
            });
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, final String relationshipId, final DeleteRequest request) {
        final ResourcePath path = REPO_RESOURCE_PATH.child(relationshipId);
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

    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String relationshipId, PatchRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String relationshipId, ActionRequest request) {
        return notSupportedOnInstance(request).asPromise();
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
    protected final ResourcePath firstResourcePath(final Context context, final Request request)
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
     * @see #FORMAT_RESPONSE_NO_EXCEPTION
     */
    protected JsonValue convertToRepoObject(final ResourcePath firstResourcePath, final JsonValue object) {
        final JsonValue properties = object.get(FIELD_PROPERTIES);
        final String idProperty = properties == null ? null : properties.get("_id").asString();
        final String revProperty = properties == null ? null : properties.get("_rev").asString();

        if (properties != null) {
            // Remove "soft" fields that were placed in properties for the ResourceResponse
            properties.remove("_id");
            properties.remove("_rev");
        }

        return json(object(
                field("_id", idProperty),
                field("_rev", revProperty),
                field(REPO_FIELD_FIRST_ID, firstResourcePath.toString()),
                field(REPO_FIELD_FIRST_PROPERTY_NAME, propertyName.toString()),
                field(REPO_FIELD_SECOND_ID, object.get(FIELD_REFERENCE).asString()),
                field(REPO_FIELD_PROPERTIES, properties == null ? null : properties.asMap())
        ));
    }

}
