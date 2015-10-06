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
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_REVISION;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.util.ResourceUtil.notSupportedOnInstance;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.patch.JsonValuePatch;
import org.forgerock.openidm.sync.impl.SynchronizationService.SyncServiceAction;
import org.forgerock.openidm.util.RelationshipUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RelationshipProvider {
    
    /**
     * Setup logging for the {@link RelationshipProvider}.
     */
    private static final Logger logger = LoggerFactory.getLogger(RelationshipProvider.class);
    
    /** Used for accessing the repo */
    protected final ConnectionFactory connectionFactory;

    /** Path to this resource in the repo */
    protected static final ResourcePath REPO_RESOURCE_PATH = new ResourcePath("repo", "relationships");

    /** Name of the source resource these relationships are "edges" of */
    protected final ResourcePath resourcePath;

    /** The property representing this relationship */
    protected final JsonPointer propertyName;

    /** If this is a reverse relationship */
    protected final boolean isReverse;
    
    /**
     * The activity logger.
     */
    protected final ActivityLogger activityLogger;
    
    /**
     * A service for sending sync events on managed objects
     */
    protected final ManagedObjectSyncService managedObjectSyncService;

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
    /** The name of the field containing the id */
    public static final JsonPointer FIELD_ID = FIELD_PROPERTIES.child(FIELD_CONTENT_ID);
    /** The name of the field containing the revision */
    public static final JsonPointer FIELD_REV = FIELD_PROPERTIES.child(FIELD_CONTENT_REVISION);

    /**
     * Function to format a resource from the repository to that expected by the provider consumer. This is simply a 
     * wrapper of {@link #FORMAT_RESPONSE_NO_EXCEPTION} with a {@link ResourceException} in the signature to allow for 
     * use against {@code Promise<ResourceResponse, ResourceException>}
     *
     * @see #FORMAT_RESPONSE_NO_EXCEPTION
     */
    protected final Function<ResourceResponse, ResourceResponse, ResourceException> FORMAT_RESPONSE =
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
    protected final Function<ResourceResponse, ResourceResponse, NeverThrowsException> FORMAT_RESPONSE_NO_EXCEPTION =
            new Function<ResourceResponse, ResourceResponse, NeverThrowsException>() {
                @Override
                public ResourceResponse apply(final ResourceResponse raw) {
                    final JsonValue formatted = json(object());
                    final Map<String, Object> properties = new LinkedHashMap<>();
                    final Map<String, Object> repoProperties = raw.getContent().get(REPO_FIELD_PROPERTIES).asMap();

                    if (repoProperties != null) {
                        properties.putAll(repoProperties);
                    }

                    properties.put(FIELD_CONTENT_ID, raw.getId());
                    properties.put(FIELD_CONTENT_REVISION, raw.getRevision());

                    formatted.put(SchemaField.FIELD_REFERENCE, raw.getContent().get(
                            isReverse ? REPO_FIELD_FIRST_ID : REPO_FIELD_SECOND_ID).asString());
                    formatted.put(SchemaField.FIELD_PROPERTIES, properties);

                    // Return the resource without _id or _rev
                    return newResourceResponse(null, null, formatted);
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
            final ResourcePath resourcePath, final SchemaField relationshipField, final ActivityLogger activityLogger,
            final ManagedObjectSyncService managedObjectSyncService) {
        final String propertyName;
        final boolean reverse;

        if (relationshipField.isReverseRelationship()) {
            propertyName = relationshipField.getReversePropertyName();
            reverse = true;
        } else {
            propertyName = relationshipField.getName();
            reverse = false;
        }

        if (relationshipField.isArray()) {
            return new CollectionRelationshipProvider(connectionFactory, resourcePath, 
                    new JsonPointer(propertyName), relationshipField.getName(), reverse,
                    activityLogger, managedObjectSyncService);
        } else {
            return new SingletonRelationshipProvider(connectionFactory, resourcePath, 
                    new JsonPointer(propertyName), relationshipField.getName(), reverse,
                    activityLogger, managedObjectSyncService);
        }
    }

    /**
     * Create a new relationship set for the given managed resource
     * 
     * @param connectionFactory Connection factory used to access the repository
     * @param resourcePath Name of the resource we are handling relationships for eg. managed/user
     * @param propertyName Name of property on first object represents the relationship
     * @param isReverse Wether or not this relationship is isReverse (matching on secondId instead of first)
     */
    protected RelationshipProvider(final ConnectionFactory connectionFactory, final ResourcePath resourcePath, 
            final JsonPointer propertyName, final boolean isReverse, ActivityLogger activityLogger,
            final ManagedObjectSyncService managedObjectSyncService) {
        this.connectionFactory = connectionFactory;
        this.resourcePath = resourcePath;
        this.propertyName = propertyName;
        this.isReverse = isReverse;
        this.activityLogger = activityLogger;
        this.managedObjectSyncService = managedObjectSyncService;
    }

    /**
     * Return a {@link RequestHandler} instance representing this provider
     * @return
     */
    public abstract RequestHandler asRequestHandler();

    /**
     * Get the full relationship representation for this provider as a JsonValue.
     *
     * @param context Context of this request
     * @param resourceId Id of resource to fetch relationships on
     *
     * @return A promise containing the full representation of the relationship on the supplied resourceId
     *         or a ResourceException if an error occurred
     */
    public abstract Promise<JsonValue, ResourceException> getRelationshipValueForResource(Context context, 
            String resourceId);

    /**
     * Set the supplied {@link JsonValue} as the current state of this relationship. This will support updating any 
     * existing relationship (_id is present) and remove any relationship not present in the value from the repository.
     *
     * @param context The context of this request
     * @param resourceId Id of the resource relation fields in value are to be memebers of
     * @param value A {@link JsonValue} map of relationship fields and their values
     *
     * @throws NullPointerException If the supplied value is null
     *
     * @return A promise containing a JsonValue of the persisted relationship(s) for the given resourceId or
     *         ResourceException if an error occurred
     */
    public abstract Promise<JsonValue, ResourceException> setRelationshipValueForResource(final Context context,
            final String resourceId, final JsonValue value);

    /**
     * Clear any relationship associated with the given resource. This could be used if for example a resource no longer 
     * exists.
     *
     * @param context The current context.
     * @param resourceId The resource whose relationship we wish to clear
     *
     */
    public abstract Promise<JsonValue, ResourceException> clear(Context context, String resourceId);

    /**
     * Creates a relationship object.
     * 
     * @param context The current context.
     * @param request The current request.
     * @return A promise containing the created relationship object
     */
    public Promise<ResourceResponse, ResourceException> createInstance(final Context context, 
            final CreateRequest request) {
        try {
            final CreateRequest createRequest = Requests.copyOfCreateRequest(request);
            createRequest.setResourcePath(REPO_RESOURCE_PATH);
            createRequest.setContent(convertToRepoObject(firstResourcePath(context, request), request.getContent()));
            
            // If the request is from ManagedObjectSet then create and return the promise after formatting.
            if (context.containsContext(ManagedObjectSetContext.class)) {
                return getConnection().createAsync(context, createRequest).then(FORMAT_RESPONSE);
            }
            
            // Get the before value of the managed object
            final ResourceResponse beforeValue = getManagedObject(context);
            
            // Create the relationship
            final ResourceResponse response = 
                    getConnection().createAsync(context, createRequest).then(FORMAT_RESPONSE).getOrThrow();
         
            // Get the before value of the managed object
            final ResourceResponse afterValue = getManagedObject(context);
            
            // Do activity logging. 
            // Log an "update" for the managed object, even though this is a "create" request on relationship field.
            activityLogger.log(context, request, "update", getManagedObjectPath(context), beforeValue.getContent(), 
                    afterValue.getContent(), Status.SUCCESS);
            
            // Do sync on the managed object
            managedObjectSyncService.performSyncAction(context, request, getManagedObjectId(context), 
                    SyncServiceAction.notifyUpdate, beforeValue.getContent(), afterValue.getContent());
            
            return expandFields(context, request, response);
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    /**
     * Reads a relationship object.
     * 
     * @param context The current context.
     * @param relationshipId The relationship id.
     * @param request The current request.
     * @return A promise containing the relationship object
     */
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String relationshipId, 
            ReadRequest request) {
        try {
            final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_PATH.child(relationshipId));
            
            Promise<ResourceResponse, ResourceException> promise = 
                    getConnection().readAsync(context, readRequest).then(FORMAT_RESPONSE);
            
            // If the request is from ManagedObjectSet then create and return the promise after formatting.
            if (context.containsContext(ManagedObjectSetContext.class)) {
                return promise;
            }
            
            // Read the relationship
            final ResourceResponse response = promise.getOrThrow();
            
            // Get the value of the managed object
            final ResourceResponse value = getManagedObject(context);
            
            // Do activity logging.
            activityLogger.log(context, request, "read", getManagedObjectPath(context), null, value.getContent(), 
                    Status.SUCCESS);

            return expandFields(context, request, response);
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    /**
     * Updates a relationship object.
     * 
     * @param context The current context.
     * @param relationshipId The relationship id.
     * @param request The current request.
     * @return A promise containing the updated relationship object
     */
    public Promise<ResourceResponse, ResourceException> updateInstance(final Context context, 
            final String relationshipId, final UpdateRequest request) {
        try {
            final String rev = request.getRevision();
            final ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_PATH.child(relationshipId));
            final JsonValue newValue = convertToRepoObject(firstResourcePath(context, request), request.getContent());

            // If the request is from ManagedObjectSet then update (if changed) and return the promise after formatting.
            if (context.containsContext(ManagedObjectSetContext.class)) {
                return getConnection()
                        // current resource in the db
                        .readAsync(context, readRequest)
                        // update once we have the current record
                        .thenAsync(new AsyncFunction<ResourceResponse, ResourceResponse, ResourceException>() {
                            @Override
                            public Promise<ResourceResponse, ResourceException> apply(ResourceResponse oldResource)
                                    throws ResourceException {
                                return updateIfChanged(context, relationshipId, rev, oldResource, newValue);
                            }
                        });
            }
            ResourceResponse result;
            
            // Get the before value of the managed object
            final ResourceResponse beforeValue = getManagedObject(context);

            // Read the relationship
            final ResourceResponse oldResource = getConnection().readAsync(context, readRequest).getOrThrow();
            
            // Perform update
            result = updateIfChanged(context, relationshipId, rev, oldResource, newValue).getOrThrow();
            
            // Get the after value of the managed object
            final ResourceResponse afterValue = getManagedObject(context);
            
            // Do activity logging.
            activityLogger.log(context, request, "update", getManagedObjectPath(context), beforeValue.getContent(), 
                    afterValue.getContent(), Status.SUCCESS);

            // Do sync on the managed object
            managedObjectSyncService.performSyncAction(context, request, getManagedObjectId(context), 
                    SyncServiceAction.notifyUpdate, beforeValue.getContent(), afterValue.getContent());

            return expandFields(context, request, result);
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    /**
     * Deletes a relationship object.
     * 
     * @param context The current context.
     * @param relationshipId The relationship id.
     * @param request The current request.
     * @return A promise containing the deleted relationship object
     */
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, 
            final String relationshipId, final DeleteRequest request) {
        final ResourcePath path = REPO_RESOURCE_PATH.child(relationshipId);
        final DeleteRequest deleteRequest = Requests.copyOfDeleteRequest(request);
        deleteRequest.setResourcePath(path);

        try {
            // If the request is from ManagedObjectSet then delete and return the promise after formatting.
            if (context.containsContext(ManagedObjectSetContext.class)) {
                return deleteAsync(context, path, deleteRequest);
            }

            // The result of the delete
            ResourceResponse result;
            
            // Get the before value of the managed object
            final ResourceResponse beforeValue = getManagedObject(context);
            
            // Perform the delete and wait for result
            result = deleteAsync(context, path, deleteRequest).getOrThrow();
            
            // Do activity logging.
            activityLogger.log(context, request, "delete", getManagedObjectPath(context), beforeValue.getContent(), 
                    null, Status.SUCCESS);

            // Do sync on the managed object
            managedObjectSyncService.performSyncAction(context, request, getManagedObjectId(context), 
                    SyncServiceAction.notifyDelete, beforeValue.getContent(), null);
            
            return expandFields(context, request, result);
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }
    
    /**
     * Performs the deletion of a relationship object.
     * 
     * @param context the current context
     * @param path the path to the relationship field
     * @param deleteRequest the current delete request
     * @return a Promise representing the result of the delete operation
     */
    private Promise<ResourceResponse, ResourceException> deleteAsync(final Context context, final ResourcePath path, 
            final DeleteRequest deleteRequest) {
        try {
            if (deleteRequest.getRevision() == null) {
                // If no revision was supplied we must perform a read to get the latest revision
                final ReadRequest readRequest = Requests.newReadRequest(path);
                final Promise<ResourceResponse, ResourceException> readResult = 
                        getConnection().readAsync(context, readRequest);

                return readResult.thenAsync(new AsyncFunction<ResourceResponse, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(ResourceResponse resourceResponse) 
                            throws ResourceException {
                        deleteRequest.setRevision(resourceResponse.getRevision());
                        return getConnection().deleteAsync(context, deleteRequest).then(FORMAT_RESPONSE);
                    }
                });
            } else {
                return getConnection().deleteAsync(context, deleteRequest).then(FORMAT_RESPONSE);
            }
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }
    
    /**
     * Updates the relationship object if the value has changed and returns a formatted result.
     * 
     * @param context the current context
     * @param id the id of the relationship object
     * @param rev the revision of the relationship object
     * @param oldResource the old value of the relationship object
     * @param newValue the new value of the relationship object
     * @return the updated, formatted relationship object
     * @throws ResourceException
     */
    private Promise<ResourceResponse, ResourceException> updateIfChanged(final Context context, String id, String rev,
            ResourceResponse oldResource, JsonValue newValue) throws ResourceException {
        if (newValue.asMap().equals(oldResource.getContent().asMap())) { 
            // resource has not changed, return the old resource
            return newResourceResponse(oldResource.getId(), oldResource.getRevision(), null).asPromise();
        } else {
            // resource has changed, update the relationship
            final UpdateRequest updateRequest = 
                    Requests.newUpdateRequest(REPO_RESOURCE_PATH.child(id), newValue).setRevision(rev);
            return getConnection().updateAsync(context, updateRequest).then(FORMAT_RESPONSE);
        }
    }

    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String relationshipId, 
            PatchRequest request) {
        Promise<ResourceResponse, ResourceException> promise = null;
        String revision = request.getRevision();
        boolean forceUpdate = (revision == null);
        boolean retry = forceUpdate;
        boolean fromManagedObjectSet = context.containsContext(ManagedObjectSetContext.class);

        do {
            logger.debug("Attempting to patch relationship {}", request.getResourcePath());
            try {

                // Read in object
                ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_PATH.child(relationshipId));
                ResourceResponse oldResource = connectionFactory.getConnection().readAsync(context, readRequest)
                        .then(FORMAT_RESPONSE).getOrThrow();
                
                // If we haven't defined a revision, we need to get the current revision
                if (revision == null) {
                    revision = oldResource.getRevision();
                }
                
                ResourceResponse managedObjectBefore = null;
                
                if (!fromManagedObjectSet) {
                    // Get the before value of the managed object
                    managedObjectBefore = getManagedObject(context);
                }
                
                JsonValue newValue = oldResource.getContent().copy();
                boolean modified = JsonValuePatch.apply(newValue, request.getPatchOperations());
                if (!modified) {
                    logger.debug("Patching did not modify the relatioship {}", request.getResourcePath());
                    return newResultPromise(null);
                }
                
                // Update (if changed) and format
                promise = updateIfChanged(context, relationshipId, revision, 
                        newResourceResponse(oldResource.getId(), oldResource.getRevision(), 
                        convertToRepoObject(firstResourcePath(context, request), oldResource.getContent())), 
                        convertToRepoObject(firstResourcePath(context, request), newValue));
                
                // If the request is from ManagedObjectSet then return the promise
                if (fromManagedObjectSet) {
                    return promise;
                }
                
                // Get the before value of the managed object
                final ResourceResponse managedObjectAfter = getManagedObject(context);

                // Do activity logging.
                activityLogger.log(context, request, "update", getManagedObjectPath(context), 
                        managedObjectBefore.getContent(), managedObjectAfter.getContent(), Status.SUCCESS);

                // Do sync on the managed object
                managedObjectSyncService.performSyncAction(context, request, getManagedObjectId(context), 
                        SyncServiceAction.notifyUpdate, managedObjectBefore.getContent(), 
                        managedObjectAfter.getContent());


                retry = false;
                logger.debug("Patch retationship successful!");
            } catch (PreconditionFailedException e) {
                if (forceUpdate) {
                    logger.debug("Unable to update due to revision conflict. Retrying.");
                } else {
                    // If it fails and we're not trying to force an update, we gave it our best shot
                    return e.asPromise();
                }
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            }
        } while (retry);
        
        // Return the result
        return promise;
    }

    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String relationshipId, 
            ActionRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    /**
     * Returns the path of the first resource in this relationship using the firstId parameter from either the URI or 
     * the Request. If firstId is not found in the URI context then the request parameter is used.
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

        if (properties != null) {
            // Remove "soft" fields that were placed in properties for the ResourceResponse
            properties.remove(FIELD_CONTENT_ID);
            properties.remove(FIELD_CONTENT_REVISION);
        }

        if (isReverse) {
            return json(object(
                    field(REPO_FIELD_FIRST_ID, object.get(FIELD_REFERENCE).asString()),
                    field(REPO_FIELD_FIRST_PROPERTY_NAME, propertyName.toString()),
                    field(REPO_FIELD_SECOND_ID, firstResourcePath.toString()),
                    field(REPO_FIELD_PROPERTIES, properties == null ? null : properties.asMap())
            ));
        } else {
            return json(object(
                    field(REPO_FIELD_FIRST_ID, firstResourcePath.toString()),
                    field(REPO_FIELD_FIRST_PROPERTY_NAME, propertyName.toString()),
                    field(REPO_FIELD_SECOND_ID, object.get(FIELD_REFERENCE).asString()),
                    field(REPO_FIELD_PROPERTIES, properties == null ? null : properties.asMap())
            ));
        }
    }
    
    /**
     * Returns the managed object's ID corresponding to the passed in {@link Context}.
     * 
     * @param context the Context object.
     * @return a String representing the managed object's ID.
     */
    protected String getManagedObjectId(Context context) {
        return context.asContext(UriRouterContext.class).getUriTemplateVariables().get(URI_PARAM_FIRST_ID);
    }
    
    /**
     * Returns the managed object's full path corresponding to the passed in {@link Context}.
     * 
     * @param context the {@link Context} object.
     * @return a String representing the managed object's ID.
     */
    protected String getManagedObjectPath(Context context) {
        return resourcePath.child(getManagedObjectId(context)).toString();
    }
    
    /**
     * Reads and returns the managed object associated with the specified context.
     * 
     * @param context the {@link Context} object.
     * @return the managed object.
     * @throws ResourceException if an error was encountered while reading the managed object.
     */
    protected ResourceResponse getManagedObject(Context context) throws ResourceException {
        String managedObjectPath = resourcePath.child(getManagedObjectId(context)).toString();
        return getConnection().read(context, Requests.newReadRequest(managedObjectPath));
    }
    
    /**
     * Returns a {@link Connection} object.
     * 
     * @return a {@link Connection} object.
     * @throws ResourceException
     */
    protected Connection getConnection() throws ResourceException {
        return connectionFactory.getConnection();
    }
    
    /**
     * Performs resourceExpansion on the supplied response based on the fields specified in the current request.
     * 
     * @param context the current {@link Context} object
     * @param request the current {@link Request} object
     * @param response the {@link ResourceResponse} to expand fields on.
     * @return A promise containing the response with the expanded fields, if any.
     * @throws ResourceException
     */
    @SuppressWarnings("unchecked")
    protected Promise<ResourceResponse, ResourceException> expandFields(final Context context, final Request request, 
            ResourceResponse response) throws ResourceException {
        List<JsonPointer> refFields = new ArrayList<JsonPointer>();
        List<JsonPointer> otherFields = new ArrayList<JsonPointer>();
        for (JsonPointer field : (List<JsonPointer>)request.getFields()) {
            if (!field.toString().startsWith(SchemaField.FIELD_REFERENCE.toString())
                    && !field.toString().startsWith(SchemaField.FIELD_PROPERTIES.toString())) {
                refFields.add(field);
            } else {
                otherFields.add(field);
            }
        }
        if (!refFields.isEmpty()) {
            // Perform the field expansion
            ReadRequest readRequest = 
                    Requests.newReadRequest(response.getContent().get(SchemaField.FIELD_REFERENCE).asString());
            readRequest.addField(refFields.toArray(new JsonPointer[refFields.size()]));
            ResourceResponse readResponse = getConnection().read(context, readRequest);
            response.getContent().asMap().putAll(readResponse.getContent().asMap());

            for (JsonPointer field : otherFields) {
                response.addField(field);
            }
            for (JsonPointer field : refFields) {
                if (field.equals(new JsonPointer("*"))) {
                    response.addField(new JsonPointer(""));
                } else {
                    response.addField(field);
                }
            }
        }
        return newResultPromise(response);  
    }
}
