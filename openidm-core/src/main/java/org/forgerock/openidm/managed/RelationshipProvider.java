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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openidm.managed;

import static java.text.MessageFormat.format;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Requests.newUpdateRequest;
import static org.forgerock.json.resource.ResourcePath.resourcePath;
import static org.forgerock.json.resource.ResourceResponse.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.sync.impl.SynchronizationService.*;
import static org.forgerock.openidm.sync.impl.SynchronizationService.SyncServiceAction.notifyUpdate;
import static org.forgerock.openidm.util.RelationshipUtil.*;
import static org.forgerock.openidm.util.ResourceUtil.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RelationshipProvider {
    
    /**
     * Setup logging for the {@link RelationshipProvider}.
     */
    private static final Logger logger = LoggerFactory.getLogger(RelationshipProvider.class);
    
    /**
     * The activity logger.
     */
    protected final ActivityLogger activityLogger;
    
    /**
     * A service for sending sync events on managed objects
     */
    protected final ManagedObjectSetService managedObjectSetService;
    
    /** Used for accessing the repo */
    protected final ConnectionFactory connectionFactory;

    /** Path to this resource in the repo */
    protected static final ResourcePath REPO_RESOURCE_PATH = new ResourcePath("repo", "relationships");

    /** The resource container that property associated with this provider is a field of.  This will typically be a 
     *  managed object path such as: "managed/user" or "managed/role". */
    protected final ResourcePath resourceContainer;

    /** The schemaField representing this relationship */
    protected final SchemaField schemaField;

    /** A {@link JsonPointer} to the property representing this relationship in the parent object. */
    protected final JsonPointer propertyPtr;

    /** An optimized relationship query ID */
    protected static final String RELATIONSHIP_QUERY_ID = "find-relationships-for-resource";
    
    /** A query field representing the full path of the managed object instance of this relationship field  */
    protected static final String QUERY_FIELD_RESOURCE_PATH = "fullResourceId";
    
    /** A query field representing the field name of this relationship field  */
    protected static final String QUERY_FIELD_FIELD_NAME = "resourceFieldName";

    /** The name of the firstId field in the repo */
    protected static final String REPO_FIELD_FIRST_ID = "firstId";

    /** The name of the secondId field in the repo */
    protected static final String REPO_FIELD_SECOND_ID = "secondId";
    
    /** The name of the firstPropertyName field in the repo */
    protected static final String REPO_FIELD_FIRST_PROPERTY_NAME = "firstPropertyName";
    
    /** The name of the secondPropertyName field in the repo */
    protected static final String REPO_FIELD_SECOND_PROPERTY_NAME = "secondPropertyName";

    /** The name of the properties field coming out of the repo service */
    protected static final String REPO_FIELD_PROPERTIES = "properties";

    /** The name of the parameter to be used carry the managed object's ID in the Request and/or Context */
    public static final String PARAM_MANAGED_OBJECT_ID = "managedObjectId";

    /** The name of the properties field in resource response */
    public static final JsonPointer FIELD_PROPERTIES = SchemaField.FIELD_PROPERTIES;
    
    /** The name of the secondId field in resource response */
    public static final JsonPointer FIELD_REFERENCE = SchemaField.FIELD_REFERENCE;
    
    /** The name of the field containing the id */
    public static final JsonPointer FIELD_ID = FIELD_PROPERTIES.child(FIELD_CONTENT_ID);
    
    /** The name of the field containing the revision */
    public static final JsonPointer FIELD_REV = FIELD_PROPERTIES.child(FIELD_CONTENT_REVISION);

    /**
     * The validator responsible for testing if the relationship request is valid.
     */
    protected final RelationshipValidator relationshipValidator;

    /**
     * Returns a Function to format a resource from the repository to that expected by the provider consumer. This is 
     * simply a wrapper of {@link #formatResponseNoException} with a {@link ResourceException} in the signature to
     * allow for use against {@code Promise<ResourceResponse, ResourceException>}
     *
     * @see #formatResponseNoException(Context, Request)
     */
    protected Function<ResourceResponse, ResourceResponse, ResourceException> formatResponse(
            final Context context, final Request request) {
        return new Function<ResourceResponse, ResourceResponse, ResourceException>() {
                @Override
                public ResourceResponse apply(ResourceResponse resourceResponse) throws ResourceException {
                    return formatResponseNoException(context, request).apply(resourceResponse);
                }
            };
    }

    /**
     * Returns a Function to format a resource from the repository to that expected by the provider consumer. First
     * object properties are removed and {@code secondId} (or {@code firstId} if it's a reverse relationship)
     * will be converted to {@code _ref}
     * <p/>
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
     * <p/>
     * To a provider response format of:
     *
     * <pre>
     *     {
     *         "_ref": "/managed/roles/uuid",
     *         "_refProperties": {
     *             "_id": "someId",
     *             "_rev": "someRev",
     *             ...
     *         },
     *         "_refError": true,
     *         "_refErrorMessage": "some error message"
     *     }
     * </pre>
     */
    protected Function<ResourceResponse, ResourceResponse, NeverThrowsException> formatResponseNoException(
            final Context context, final Request request) {
        return new Function<ResourceResponse, ResourceResponse, NeverThrowsException>() {
            
                public String resourceFullPath = getResourceFullPath(context, request).toString();
                
                @Override
                public ResourceResponse apply(final ResourceResponse raw) {
                    final JsonValue rawContent = raw.getContent();
                    final JsonValue formatted = json(object());
                    final Map<String, Object> properties = new LinkedHashMap<>();
                    final Map<String, Object> repoProperties = rawContent.get(REPO_FIELD_PROPERTIES).asMap();
                    final String ref;

                    // set the field reference
                    if (schemaField.isReverseRelationship()
                            && !rawContent.get(REPO_FIELD_FIRST_ID).asString().equals(resourceFullPath)) {
                        ref = rawContent.get(REPO_FIELD_FIRST_ID).asString();
                    } else {
                        ref = rawContent.get(REPO_FIELD_SECOND_ID).asString();
                    }

                    if (repoProperties != null) {
                        properties.putAll(repoProperties);
                    }

                    properties.put(FIELD_CONTENT_ID, raw.getId());
                    properties.put(FIELD_CONTENT_REVISION, raw.getRevision());

                    formatted.put(SchemaField.FIELD_REFERENCE, ref);
                    formatted.put(SchemaField.FIELD_PROPERTIES, properties);

                    // If has error, append error flag and message.
                    if (rawContent.get(REFERENCE_ERROR).defaultTo(false).asBoolean()) {
                        formatted.put(REFERENCE_ERROR, true);
                        formatted.put(REFERENCE_ERROR_MESSAGE,
                                rawContent.get(REFERENCE_ERROR_MESSAGE).defaultTo("").asString());
                    }

                    // Return the resource without _id or _rev
                    return newResourceResponse(null, null, formatted);
                }
            };
    }

    /**
     * On a create of a relationship, this will sync the referenced object after the update is completed.
     */
    private final SyncReferencedObjectRequestHandler<CreateRequest> syncReferencedObjectCreateHandler =
            new SyncReferencedObjectRequestHandler<CreateRequest>() {
                @Override
                protected Promise<ResourceResponse, ResourceException> invokeRequest(Context context,
                        CreateRequest request) throws ResourceException {
                    return getConnection().createAsync(context, request);
                }
            };

    /**
     * On a update of a relationship, this will sync the referenced object after the update is completed.
     */
    private final SyncReferencedObjectRequestHandler<UpdateRequest> syncReferencedObjectUpdateHandler =
            new SyncReferencedObjectRequestHandler<UpdateRequest>() {
                @Override
                protected Promise<ResourceResponse, ResourceException> invokeRequest(Context context,
                        UpdateRequest request) throws ResourceException {
                    return getConnection().updateAsync(context, request);
                }
            };

    /**
     * On a delete of a relationship, this will sync the referenced object after the delete is completed.
     */
    private final SyncReferencedObjectRequestHandler<DeleteRequest> syncReferencedObjectDeleteHandler =
            new SyncReferencedObjectRequestHandler<DeleteRequest>() {
                @Override
                protected Promise<ResourceResponse, ResourceException> invokeRequest(Context context,
                        DeleteRequest request) throws ResourceException {
                    return getConnection().deleteAsync(context, request);
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
            final ManagedObjectSetService managedObjectSetService) {
        if (relationshipField.isArray()) {
            return new CollectionRelationshipProvider(connectionFactory, resourcePath, relationshipField,
                    activityLogger, managedObjectSetService);
        } else {
            return new SingletonRelationshipProvider(connectionFactory, resourcePath, relationshipField,
                    activityLogger, managedObjectSetService);
        }
    }

    /**
     * Create a new relationship set for the given managed resource
     * 
     * @param connectionFactory Connection factory used to access the repository
     * @param resourcePath Name of the resource we are handling relationships for eg. managed/user
     * @param schemaField The field used to represent this relationship in the parent object
     * @param activityLogger The audit activity logger to use
     * @param managedObjectSetService Service to send sync events to
     */
    protected RelationshipProvider(final ConnectionFactory connectionFactory, final ResourcePath resourcePath, 
            final SchemaField schemaField, final ActivityLogger activityLogger,
            final ManagedObjectSetService managedObjectSetService) {
        this.connectionFactory = connectionFactory;
        this.resourceContainer = resourcePath;
        this.schemaField = schemaField;
        this.propertyPtr = new JsonPointer(schemaField.getName());
        this.activityLogger = activityLogger;
        this.managedObjectSetService = managedObjectSetService;
        this.relationshipValidator = (schemaField.isReverseRelationship())
                ? new ReverseRelationshipValidator(this)
                : new ForwardRelationshipValidator(this);
    }

    /**
     * Return a {@link RequestHandler} instance representing this provider
     * @return a {@link RequestHandler} instance representing this provider
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
     * @param clearExisting If existing (non-present) relationships should be cleared
     * @param context The context of this request
     * @param resourceId Id of the resource relation fields in value are to be memebers of
     * @param value A {@link JsonValue} map of relationship fields and their values
     *
     * @throws NullPointerException If the supplied value is null
     *
     * @return A promise containing a JsonValue of the persisted relationship(s) for the given resourceId or
     *         ResourceException if an error occurred
     */
    public abstract Promise<JsonValue, ResourceException> setRelationshipValueForResource(boolean clearExisting,
            final Context context, final String resourceId, final JsonValue value);

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
     * Tests that all references in the relationship field are valid according to this provider's validator.
     *
     * @param context context of the request working with the relationship.
     * @param oldValue old value of field to refer to during validation of the newValue
     * @param newValue new value of field to validate
     * @throws ResourceException BadRequestException if the relationship is found to be not valid, otherwise for other
     * issues.
     */
    public abstract void validateRelationshipField(Context context, JsonValue oldValue, JsonValue newValue)
            throws ResourceException;

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
            if (context.containsContext(ManagedObjectContext.class)) {
                return syncReferencedObjectCreateHandler
                        .performRequest(request.getContent().get(REFERENCE_ID).asString(), createRequest, context)
                        .then(formatResponse(context, request));
            }

            /*
                Calls directly from ManagedObjectSet will return in the block above. If we get to this point, it is because
                the RelationshipProvider is functioning as a CollectionResourceProvider, and servicing a create call directly
                on a relationship endpoint. When this occurs, the MO specified in the _ref in the request needs to be
                validated for existence, the presence of the reversePropertyName confirmed. Finally, the invocation needs
                to be rejected if the reversePropertyName is not a collection, and presently set. The latter check will
                have to rely on the state obtained from the MO corresponding to the _ref itself, as the SchemaField in this
                class will specify the reversePropertyName, but not its type.
             */
            validateRelationshipEndpointOperand(request.getContent().get(REFERENCE_ID).asString(), context);
            // Get the before value of the managed object
            final JsonValue beforeValue = getManagedObject(context).getContent();

            // Create the relationship
            ResourceResponse response = syncReferencedObjectCreateHandler
                    .performRequest(request.getContent().get(REFERENCE_ID).asString(), createRequest, context)
                    .then(formatResponse(context, request))
                    .getOrThrow();

            // Get the before value of the managed object
            final JsonValue afterValue = getManagedObject(context).getContent();

            // Perform update operations on the managed object
            performUpdateOperations(context, request, afterValue, beforeValue);
            
            return expandFields(context, request, response);
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    /**
     * Called to validate the operand passed as the _ref to the relationship endpoint
     * @param managedObjectRef
     * @throws BadRequestException
     */
    private void validateRelationshipEndpointOperand(String managedObjectRef, Context context) throws BadRequestException {
        if (schemaField.isValidationRequired() && schemaField.isReverseRelationship()) {
            ResourceResponse response;
            try {
                response = getConnection().read(context,
                        Requests.newReadRequest(managedObjectRef).addField(schemaField.getReversePropertyName()));
            } catch (ResourceException e) {
                throw new BadRequestException(format(
                        "In relationship endpoint ''{0}'', could not read referenced managed object ''{1}''.",
                        resourceContainer.toString() + "/" + schemaField.getName(), managedObjectRef));
            }
            final JsonValue relationshipField = response.getContent().get(schemaField.getReversePropertyName());
            if (relationshipField.isNotNull() && !relationshipField.isCollection()) {
                throw new BadRequestException(format(
                        "In relationship endpoint ''{0}'', field ''{1}'' of managed object ''{2}'' is neither null nor a collection, " +
                                "and thus not available for assignment.",
                        resourceContainer.toString() + "/" + schemaField.getName(), schemaField.getReversePropertyName(),
                        managedObjectRef));
            }
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
                    getConnection().readAsync(context, readRequest).then(formatResponse(context, request));
            
            // If the request is from ManagedObjectSet then create and return the promise after formatting.
            if (context.containsContext(ManagedObjectContext.class)) {
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
            if (context.containsContext(ManagedObjectContext.class)) {
                return getConnection()
                        // current resource in the db
                        .readAsync(context, readRequest)
                        // update once we have the current record
                        .thenAsync(new AsyncFunction<ResourceResponse, ResourceResponse, ResourceException>() {
                            @Override
                            public Promise<ResourceResponse, ResourceException> apply(ResourceResponse oldResource)
                                    throws ResourceException {
                                return updateIfChanged(context, request, relationshipId, rev, oldResource, newValue);
                            }
                        });
            }
            ResourceResponse result;
            
            // Get the before value of the managed object
            final JsonValue beforeValue = getManagedObject(context).getContent();

            // Read the relationship
            final ResourceResponse oldResource = getConnection().readAsync(context, readRequest).getOrThrow();
            
            // Perform update
            result = updateIfChanged(context, request, relationshipId, rev, oldResource, newValue).getOrThrow();
            
            // Get the after value of the managed object
            final JsonValue afterValue = getManagedObject(context).getContent();

            // Perform update operations on the managed object
            performUpdateOperations(context, request, afterValue, beforeValue);

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
            if (context.containsContext(ManagedObjectContext.class)) {
                return deleteAsync(context, path, deleteRequest);
            }

            // The result of the delete
            ResourceResponse result;
            
            // Get the before value of the managed object
            final JsonValue beforeValue = getManagedObject(context).getContent();
            
            // Perform the delete and wait for result
            result = deleteAsync(context, path, deleteRequest).getOrThrow();
            
            // Get the after value of the managed object
            final JsonValue afterValue = getManagedObject(context).getContent();

            // Perform update operations on the managed object
            performUpdateOperations(context, request, afterValue, beforeValue);
            
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
            // Read the relationship that needs to be deleted.
            return getConnection().readAsync(context, Requests.newReadRequest(path))
                    .thenAsync(new AsyncFunction<ResourceResponse, ResourceResponse, ResourceException>() {
                        /**
                         * Sets the revision on the request if needed, and then performs the delete via the
                         * syncReferencedObjectDeleteHandler.
                         *
                         * @param readResponse the response from reading the relationship.
                         * @return the promise of the delete request.
                         * @throws ResourceException
                         * @see SyncReferencedObjectRequestHandler
                         */
                        public Promise<ResourceResponse, ResourceException> apply(final ResourceResponse readResponse)
                                throws ResourceException {
                            if (deleteRequest.getRevision() == null) {
                                deleteRequest.setRevision(readResponse.getRevision());
                            }
                            return syncReferencedObjectDeleteHandler.performRequest(readResponse.getContent(),
                                    deleteRequest, context);
                        }
                    }).then(formatResponse(context, deleteRequest));
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /**
     * Updates the relationship object if the value has changed and returns a formatted result.
     * 
     * @param context the current context
     * @param request The current request.
     * @param id the id of the relationship object
     * @param rev the revision of the relationship object
     * @param oldResource the old value of the relationship object
     * @param newValue the new value of the relationship object
     * @return the updated, formatted relationship object
     * @throws ResourceException
     */
    private Promise<ResourceResponse, ResourceException> updateIfChanged(final Context context, Request request,
            String id, String rev, ResourceResponse oldResource, JsonValue newValue) throws ResourceException {

        // Find changes, ignoring ID and REV as the newValue won't have those set.
        if (isEqual(oldResource.getContent(), newValue)) {
            // resource has not changed, return the old resource
            return newResourceResponse(oldResource.getId(), oldResource.getRevision(), oldResource.getContent())
                    .asPromise()
                    .then(formatResponse(context, request));
        } else {
            // resource has changed, update the relationship
            UpdateRequest updateRequest =
                    Requests.newUpdateRequest(REPO_RESOURCE_PATH.child(id), newValue).setRevision(rev);
            return syncReferencedObjectUpdateHandler
                    .performRequest(newValue, updateRequest, context)
                    .then(formatResponse(context, request));
        }
    }

    /**
     * Patch a relationship instance. Used by RequestHandler child classes.
     *
     * @param context the current context
     * @param relationshipId The id of the relationship instance to patch
     * @param request The patch request
     * @return A promised patch response or exception
     */
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String relationshipId, 
            PatchRequest request) {
        Promise<ResourceResponse, ResourceException> promise = null;
        String revision = request.getRevision();
        boolean forceUpdate = (revision == null);
        boolean retry = forceUpdate;
        boolean fromManagedObjectSet = context.containsContext(ManagedObjectContext.class);

        do {
            logger.debug("Attempting to patch relationship {}", request.getResourcePath());
            try {

                // Read in object
                ReadRequest readRequest = Requests.newReadRequest(REPO_RESOURCE_PATH.child(relationshipId));
                ResourceResponse oldResource = connectionFactory.getConnection().readAsync(context, readRequest)
                        .then(formatResponse(context, request)).getOrThrow();
                
                // If we haven't defined a revision, we need to get the current revision
                if (revision == null) {
                    revision = oldResource.getRevision();
                }

                // Get the before value of the managed object
                final JsonValue beforeValue = !fromManagedObjectSet 
                        ? getManagedObject(context).getContent()
                        : null;
                
                JsonValue newValue = oldResource.getContent().copy();
                boolean modified = JsonValuePatch.apply(newValue, request.getPatchOperations());
                if (!modified) {
                    logger.debug("Patching did not modify the relatioship {}", request.getResourcePath());
                    return newResultPromise(null);
                }
                
                // Update (if changed) and format
                promise = updateIfChanged(context, request, relationshipId, revision, 
                        newResourceResponse(oldResource.getId(), oldResource.getRevision(), 
                        convertToRepoObject(firstResourcePath(context, request), oldResource.getContent())), 
                        convertToRepoObject(firstResourcePath(context, request), newValue));
                
                // If the request is from ManagedObjectSet then return the promise
                if (fromManagedObjectSet) {
                    return promise;
                }
                
                // Get the before value of the managed object
                final JsonValue afterValue = getManagedObject(context).getContent();

                // Perform update operations on the managed object
                performUpdateOperations(context, request, afterValue, beforeValue);

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
    
    /**
     * Performs operations associated with the updated managed object.  The operations include: activity logging,
     * executing the postUpdate script of the managed object, and performing a notifyUpdate sync action on the managed
     * object. 
     * 
     * @param context the current context
     * @param request the current request
     * @param afterValue the value of the managed object after the update
     * @param beforeValue the value of the managed object before the update
     * @throws ResourceException
     */
    private void performUpdateOperations(Context context, Request request, JsonValue afterValue, JsonValue beforeValue) 
            throws ResourceException {
        final String managedId = getManagedObjectId(context);
        
        // Do activity logging.
        activityLogger.log(context, request, "update", getManagedObjectPath(context), beforeValue, null, 
                Status.SUCCESS);
        
        // Perform an update on the managed object
        // Note that the second-to-last parameter corresponds to the to-be-created relationships according to the
        // javadocs in ManagedObjectSet#update, but that this relationship has already been added. If this value is
        // empty however, the diff between the new and old objects in ManagedObjectSet#updateRelationshipFields will add
        // this relationship back to the set of to-be-persisted relationships. Thus this field is retained here, as
        // a slight performance enhancement. Note that this semantic impurity will be addressed when the RelationshipProvider
        // class is refactored. TODO
        managedObjectSetService.update(context, newUpdateRequest(managedId, afterValue), managedId, null, beforeValue, 
                afterValue, new HashSet<>(Arrays.asList(propertyPtr)), new HashSet<>(Arrays.asList(propertyPtr)));
    }

    /**
     * Perform an action on a relationship instance. Used by child RequsetHandler classes.
     *
     * @param context the current context
     * @param relationshipId The id of the relationship instance to perform the action on
     * @param request The action request
     * @return A promised action response or exception
     */
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
     * @see #resourceContainer
     *
     * @return The resource path of the first resource as a child of resourcePath
     */
    protected final ResourcePath firstResourcePath(final Context context, final Request request)
            throws BadRequestException {
        final String uriFirstId =
                context.asContext(UriRouterContext.class).getUriTemplateVariables().get(PARAM_MANAGED_OBJECT_ID);
        final String firstId = uriFirstId != null ? uriFirstId : request.getAdditionalParameter(PARAM_MANAGED_OBJECT_ID);

        if (StringUtils.isNotBlank(firstId)) {
            return resourceContainer.child(firstId);
        } else {
            throw new BadRequestException("Required either URI parameter " + PARAM_MANAGED_OBJECT_ID +
                    " or request paremeter " + PARAM_MANAGED_OBJECT_ID + " but none were found.");
        }
    }

    /**
     * Returns the full path of the resource in this relationship using the managedObjectId parameter from either the 
     * URI or the Request. If managedObejctId is not found in the URI context then the request parameter is used.
     *
     * @param context Context containing a {@link UriRouterContext} to check for template variables
     * @param request Request containing a fall-back firstId parameter
     *
     * @see #resourceContainer
     *
     * @return The resource path of the first resource as a child of resourcePath
     */
    protected final ResourcePath getResourceFullPath(final Context context, final Request request) {
        try {
            return firstResourcePath(context, request);
        } catch (BadRequestException e) {
            logger.error("Error getting resource path", e);
        }
        return null;
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
     * @see #formatResponseNoException(Context, Request)
     */
    protected JsonValue convertToRepoObject(final ResourcePath firstResourcePath, final JsonValue object) throws BadRequestException {
        final JsonValue properties = object.get(FIELD_PROPERTIES);

        if (properties != null) {
            // Remove "soft" fields that were placed in properties for the ResourceResponse
            properties.remove(FIELD_CONTENT_ID);
            properties.remove(FIELD_CONTENT_REVISION);
            // Currently only 1 temporal constraint is allowed per grant
            if (properties.get("temporalConstraints").isNotNull()
                    && properties.get("temporalConstraints").expect(List.class).asList().size() > 1) {
                throw new BadRequestException("Only 1 temporal constraint is supported per grant.");
            }
        }

        if (schemaField.isReverseRelationship()) {
            // Compare the resource paths and set firstId/secondId and firstPropertyName/secondPropertyName based on
            // lexicographic ordering to ensure consistency for reverse (bidirectional) relationships.
            // We want to prevent the case where a bidirectional relationship may be updated from a operation on either
            // end resulting in the firstId/firstPropName and secondId/secondPropName getting swapped.
            if (firstResourcePath.toString().compareTo(object.get(FIELD_REFERENCE).asString()) < 0) {
                return json(object(
                        field(REPO_FIELD_FIRST_ID, firstResourcePath.toString()),
                        field(REPO_FIELD_FIRST_PROPERTY_NAME, schemaField.getName()),
                        field(REPO_FIELD_SECOND_ID, object.get(FIELD_REFERENCE).asString()),
                        field(REPO_FIELD_SECOND_PROPERTY_NAME, schemaField.getReversePropertyName()),
                        field(REPO_FIELD_PROPERTIES, properties == null ? null : properties.asMap())
                ));
            } else {
                return json(object(
                        field(REPO_FIELD_FIRST_ID, object.get(FIELD_REFERENCE).asString()),
                        field(REPO_FIELD_FIRST_PROPERTY_NAME, schemaField.getReversePropertyName()),
                        field(REPO_FIELD_SECOND_ID, firstResourcePath.toString()),
                        field(REPO_FIELD_SECOND_PROPERTY_NAME, schemaField.getName()),
                        field(REPO_FIELD_PROPERTIES, properties == null ? null : properties.asMap())
                ));
            }
        } else {
            return json(object(
                    field(REPO_FIELD_FIRST_ID, firstResourcePath.toString()),
                    field(REPO_FIELD_FIRST_PROPERTY_NAME, schemaField.getName()),
                    field(REPO_FIELD_SECOND_ID, object.get(FIELD_REFERENCE).asString()),
                    field(REPO_FIELD_SECOND_PROPERTY_NAME, null),
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
        return context.asContext(UriRouterContext.class).getUriTemplateVariables().get(PARAM_MANAGED_OBJECT_ID);
    }
    
    /**
     * Returns the managed object's full path corresponding to the passed in {@link Context}.
     * 
     * @param context the {@link Context} object.
     * @return a String representing the managed object's ID.
     */
    protected String getManagedObjectPath(Context context) {
        return resourceContainer.child(getManagedObjectId(context)).toString();
    }
    
    /**
     * Reads and returns the managed object associated with the specified context.
     * 
     * @param context the {@link Context} object.
     * @return the managed object.
     * @throws ResourceException if an error was encountered while reading the managed object.
     */
    protected ResourceResponse getManagedObject(Context context) throws ResourceException {
        String managedObjectPath = resourceContainer.child(getManagedObjectId(context)).toString();
        return getConnection().read(context, Requests.newReadRequest(managedObjectPath)
                .addField(SchemaField.FIELD_ALL).addField(propertyPtr));
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
    protected Promise<ResourceResponse, ResourceException> expandFields(final Context context, final Request request,
            ResourceResponse response) throws ResourceException {
        List<JsonPointer> refFields = new ArrayList<JsonPointer>();
        List<JsonPointer> otherFields = new ArrayList<JsonPointer>();
        for (JsonPointer field : request.getFields()) {
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
                if (field.equals(SchemaField.FIELD_ALL)) {
                    response.addField(SchemaField.FIELD_EMPTY);
                } else {
                    response.addField(field);
                }
            }
        }
        return newResultPromise(response);
    }

    /**
     * When modifying bidirectional relationships, the objects that refer to the relationship should be sync'd when
     * the request is made.  The direct object will already be sync'd.  Using this class will also sync the
     * referenced object.
     * <p/>
     * For example: removing a role from a user via the following command will need to also sync the user linked by
     * the member id.
     * <pre>
     * curl -X DELETE -H "X-OpenIDM-Password: openidm-admin" -H "X-OpenIDM-Username: openidm-admin"
     * -H "Content-Type: application/json" -H "Cache-Control: no-cache"
     * 'http://localhost:8080/openidm/managed/role/sample-role-1/members/4dc21ceb-ef4c-4006-9188-06236f11c0b1'
     * </pre>
     * <p/>
     * The steps to perform the request and sync the referenced object is:
     * <ol>
     * <li>Determine the ID of the referenced object on the opposite side of 'this' relationship.</li>
     * <li>Read the referenced object before the request is made, to save the 'before'.</li>
     * <li>invoke the request. invokeRequest()</li>
     * <li>Read the referenced object to retrieve the 'after'.</li>
     * <li>Perform sync on the referenced object.</li>
     * <li>Return the results of invokeRequest()</li>
     * </ol>
     *
     * @param <T> The Type of Request that will be made.
     */
    private abstract class SyncReferencedObjectRequestHandler<T extends Request> {

        /**
         * Determines the reverse property ID using the reversePropertyName for the relationship being supported,
         * then calls #performRequest(String, Request, Context) with the id.
         *
         * @param relationshipJson the relationship json to determine the referenceToSync.
         * @param request the request to make on the relationship.
         * @param context context of the call.
         * @return the results of the invokeRequest once the sync is promised.
         * @throws ResourceException
         * @see #performRequest(String, Request, Context)
         */
        public final Promise<ResourceResponse, ResourceException> performRequest(final JsonValue relationshipJson,
                final T request, final Context context) throws ResourceException {

            // If not in a bidirectional (aka reverse) relationship, then referenced object doesn't need to sync.
            if (!isReverseSyncNeeded()) {
                return invokeRequest(context, request);
            }
            return performRequest(isReversePropertyFirst(relationshipJson)
                    ? relationshipJson.get(REPO_FIELD_FIRST_ID).asString()
                            : relationshipJson.get(REPO_FIELD_SECOND_ID).asString(),
                    request, context);
        }

        /**
         * Performs the request once it has gathered the before state of the referenced object and then will execute
         * a sync on the referenced object.
         *
         * @param refToSync the reference to the reverse property that the sync will be applied to.
         * @param request the request to make on the relationship.
         * @param context context of the call.
         * @return the results of the invokeRequest once the sync is promised.
         * @throws ResourceException
         */
        public final Promise<ResourceResponse, ResourceException> performRequest(final String refToSync,
                final T request, final Context context) throws ResourceException {

            // If not in a bidirectional (aka reverse) relationship, then referenced object doesn't need to sync.
            if (!isReverseSyncNeeded()) {
                return invokeRequest(context, request);
            }

            // First read the state of the referenced object to save the before.
            return getConnection().readAsync(context, Requests.newReadRequest(refToSync))
                    .thenAsync(
                            //make the request
                            new AsyncFunction<ResourceResponse, ResourceResponse, ResourceException>() {
                                @Override
                                public Promise<ResourceResponse, ResourceException> apply(final ResourceResponse before)
                                        throws ResourceException {
                                    return invokeRequest(context, request)
                                            // Perform the sync after reading the new state of the referenced obj.
                                            .thenOnResult(
                                                    new PerformSyncHandler<>(context, refToSync, request, before));
                                }
                            }, new AsyncFunction<ResourceException, ResourceResponse, ResourceException>() {
                                @Override
                                public Promise<ResourceResponse, ResourceException> apply(ResourceException e)
                                        throws ResourceException {
                                    // Since the read failed, the sync can't happen, but we still want to proceed with
                                    // the request on the relationship.
                                    logger.warn("Unable to read '{}', no sync will occur", refToSync);
                                    return invokeRequest(context, request);
                                }
                            });
        }

        /**
         * Implementers should invoke the call that actually performs the request on the relationship that affects
         * the referenced side of this relationship.
         *
         * @param context context of the request.
         * @param request the request to be made.
         * @return the promise of the request execution.
         * @throws ResourceException
         */
        protected abstract Promise<ResourceResponse, ResourceException> invokeRequest(Context context, T request)
                throws ResourceException;

        /**
         * After the makeRequest is made this function will when lookup the referenced object to collect the 'after'
         * state; then this will call a sync on the referenced object.
         *
         * @param <U> The Type of Request that was made.
         */
        private class PerformSyncHandler<U extends Request> implements ResultHandler<ResourceResponse> {
            private final Context context;
            private final String referenceToSync;
            private final U request;
            private final ResourceResponse before;

            /**
             * Constructs the Sync handler with state needed to call sync on the referenced object.
             *
             * @param context context of the request made on the relationship.
             * @param request the original request made on the relationship to be sent to the sync call.
             * @param before the state of the referenced object before the request on the relationship is made.
             * @param referenceToSync the resource path the the object that needs to get synced.
             */
            public PerformSyncHandler(Context context, String referenceToSync, U request, ResourceResponse before) {
                this.context = context;
                this.referenceToSync = referenceToSync;
                this.request = request;
                this.before = before;
            }

            @Override
            public void handleResult(ResourceResponse invokeResponse) {
                try {
                    // now re-read the referenced object to see the aftermath of the request
                    ResourceResponse afterResponse = getConnection()
                            .read(context, Requests.newReadRequest(referenceToSync));
                    // now perform the sync
                    logger.debug("after relationship change on {}{}, making sync request on {}", resourceContainer,
                            schemaField.getName(), referenceToSync);
                    ResourcePath resourcePath = resourcePath(referenceToSync);
                    final ActionRequest syncRequest = Requests.newActionRequest("sync", notifyUpdate.name())
                            .setAdditionalParameter(ACTION_PARAM_RESOURCE_CONTAINER, resourcePath.parent().toString())
                            .setAdditionalParameter(ACTION_PARAM_RESOURCE_ID, resourcePath.leaf())
                            .setContent(
                                    json(
                                            object(
                                                    field("oldValue", before.getContent().getObject()),
                                                    field("newValue", afterResponse.getContent().getObject()))
                                    ));
                    getConnection().action(context, syncRequest);
                } catch (Exception e) {
                    logger.warn("request on relationship was successful, however the reverse referenced object " +
                            referenceToSync + " failed to request a sync.", e);
                }
            }
        }
    }

    /**
     * Sync on the reverse relationship is only possible and needed on reverse relationships and if the reverse
     * property name is set correctly.
     *
     * @return true if isReverseRelationship and the reversePropertyName is set.
     */
    private boolean isReverseSyncNeeded() {
        return schemaField.isReverseRelationship() && null != schemaField.getReversePropertyName();
    }

    /**
     * Given a relationship Json this will determine if "this" relationship's reversePropertyName is equal to the
     * relationship's first property.
     * <p/>
     * For example: given the reverse property name of "/members" in a role managed object and relationshipJson like
     * below, this would return true.
     * <pre>
     * {
     *     "firstId": "managed/role/sample-role-1",
     *     "firstPropertyName": "/members",
     *     "secondId": "managed/user/test",
     *     "secondPropertyName": "/roles",
     *     "properties": { "name": "samplerole1" },
     *     "_id": "56733e76-8ca2-4df2-ad3c-bfd7a9d88d47",
     *     "_rev": "1"
     * }
     * </pre>
     *
     * @param relationshipJson The repo json of the relationship.
     * @return true if the repo json's firstPropertyName matches this relationship's reversePropertyName
     */
    private boolean isReversePropertyFirst(JsonValue relationshipJson) {
        return relationshipJson.get(REPO_FIELD_FIRST_PROPERTY_NAME).asString().equals(schemaField.getReversePropertyName());
    }

    public SchemaField getSchemaField() {
        return schemaField;
    }
}
