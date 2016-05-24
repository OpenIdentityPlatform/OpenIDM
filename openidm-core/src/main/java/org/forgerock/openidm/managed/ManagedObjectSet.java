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
 * Portions copyright 2011-2016 ForgeRock AS.
 */
package org.forgerock.openidm.managed;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openidm.managed.ManagedObjectSet.ScriptHook.onRead;
import static org.forgerock.openidm.util.ResourceUtil.isEqual;
import static org.forgerock.util.promise.Promises.*;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.patch.JsonPatch;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.SortKey;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.audit.util.RouterActivityLogger;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.patch.JsonValuePatch;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.sync.SyncContext;
import org.forgerock.openidm.sync.impl.SynchronizationService;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.openidm.util.RelationshipUtil;
import org.forgerock.openidm.util.RequestUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptEvent;
import org.forgerock.script.ScriptListener;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to a set of managed objects of a given type: managed/[type]/{id}.
 *
 */
class ManagedObjectSet implements CollectionResourceProvider, ScriptListener, ManagedObjectSetService {

    /** Actions supported by this resource provider */
    enum Action {
        patch,
        triggerSyncCheck
    }

    /** Supported script hooks */
    enum ScriptHook {
        /** Script to execute when the creation of an object is being requested. */
        onCreate,

        /** Script to execute when the read of an object is being requested. */
        onRead,

        /** Script to execute when the update of an object is being requested. */
        onUpdate,

        /** Script to execute when the deletion of an object is being requested. */
        onDelete,

        /** Script to execute after the create of an object has completed. */
        postCreate,

        /** Script to execute after the update of an object has completed. */
        postUpdate,

        /** Script to execute after the delete of an object has completed. */
        postDelete,

        /** Script to execute when a managed object requires validation. */
        onValidate,

        /** Script to execute once an object is retrieved from the repository. */
        onRetrieve,

        /** Script to execute when an object is about to be stored in the repository. */
        onStore,

        /** Script to execute when synchronization of managed objects to external targets is complete. */
        onSync
    }

    /**
     * Setup logging for the {@link ManagedObjectSet}.
     */
    private static final Logger logger = LoggerFactory.getLogger(ManagedObjectSet.class);

    /** The managed objects service that instantiated this managed object set. */
    private final CryptoService cryptoService;

    /** The connection factory for access to the router */
    private final IDMConnectionFactory connectionFactory;

    /** Audit Activity Log helper */
    private final ActivityLogger activityLogger;

    /** Name of the managed object type. */
    private final String name;

    /** the managed object path (e.g. managed/user) as a ResourcePath */
    private final ResourcePath managedObjectPath;

    /** The schema to use to validate the structure and content of the managed object. */
    private final ManagedObjectSchema schema;

    /** Map of scripts to execute on specific {@link ScriptHook}s. */
    private final Map<ScriptHook, ScriptEntry> scriptHooks = new EnumMap<ScriptHook, ScriptEntry>(ScriptHook.class);

    /** reference to the sync service route; used to decided whether or not to perform a sync action */
    private final AtomicReference<RouteService> syncRoute;

    /** Map of relationship property names and their accompanying sets */
    private final Map<JsonPointer, RelationshipProvider> relationshipProviders = new HashMap<>();

    /** Flag for indicating if policy enforcement is enabled */
    private final boolean enforcePolicies;

    /**
     * Constructs a new managed object set.
     *
     * @param scriptRegistry
     *            the script registry
     * @param cryptoService
     *            the cryptographic service
     * @param syncRoute
     *            a reference to the RouteService on "sync"
     * @param connectionFactory
     *            the router connection factory
     * @param config
     *            configuration object to use to initialize managed object set.
     * @throws JsonValueException
     *             when the configuration is malformed
     * @throws ScriptException
     *             when the script configuration is malformed or the script is
     *             invalid.
     */
    public ManagedObjectSet(final ScriptRegistry scriptRegistry, final CryptoService cryptoService,
            final AtomicReference<RouteService> syncRoute, IDMConnectionFactory connectionFactory, JsonValue config)
            throws JsonValueException, ScriptException {
        this.cryptoService = cryptoService;
        this.syncRoute = syncRoute;
        this.connectionFactory = connectionFactory;
        this.activityLogger = new RouterActivityLogger(connectionFactory);
        name = config.get("name").required().asString();
        if (name.trim().isEmpty() || name.indexOf('{') > 0 | name.indexOf('}') > 0) {
            throw new JsonValueException(config.get("name"), "Failed to validate the name");
        }
        this.managedObjectPath = new ResourcePath("managed").child(name);

        this.schema = new ManagedObjectSchema(config.get("schema").expect(Map.class), scriptRegistry, cryptoService);

        for (JsonPointer relationship : schema.getRelationshipFields()) {
            final SchemaField field = schema.getField(relationship);
            relationshipProviders.put(relationship, RelationshipProvider.newProvider(connectionFactory, 
                    managedObjectPath, field, activityLogger, this));
        }
        
        for (ScriptHook hook : ScriptHook.values()) {
            if (config.isDefined(hook.name())) {
                scriptHooks.put(hook, scriptRegistry.takeScript(config.get(hook.name())));
            }
        }
        
        enforcePolicies = Boolean.parseBoolean(IdentityServer.getInstance()
                .getProperty("openidm.policy.enforcement.enabled", "true"));
        logger.debug("Instantiated managed object set: {}", name);
    }

    /**
     * Generates a fully-qualified object identifier for the managed object.
     *
     * @param resourceId
     *            the local managed object identifier to qualify.
     * @return the fully-qualified managed object identifier.
     */
    private ResourcePath managedId(String resourceId) {
        return resourceId != null
                ? managedObjectPath.child(resourceId)
                : managedObjectPath;
    }

    /**
     * Generates a fully-qualified object identifier for the repository.
     *
     * @param resourceId
     *            the local managed object identifier to qualify.
     * @return the fully-qualified repository object identifier.
     */
    private String repoId(String resourceId) {
        return ResourcePath.valueOf("repo").concat(managedId(resourceId)).toString();
    }

    /**
     * Executes a script if it exists, populating an {@code "object"} property in the root scope.
     *
     * @param hook
     *            the script-hook to execute
     * @param value
     *            the object to be populated in the script scope.
     * @param additionalProps
     *            a Map of additional properties to add the the script scope
     * @throws ForbiddenException
     *             if the script throws an exception.
     * @throws InternalServerErrorException
     *             if any other exception is encountered.
     */
    private void execScript(final Context context, ScriptHook hook, JsonValue value, JsonValue additionalProps)
            throws ResourceException {
        final ScriptEntry scriptEntry = scriptHooks.get(hook);
        EventEntry measure = Publisher.start(Name.get("openidm/internal/managed/" + this.getName() + "/execScript/" + hook.name()), null, null);

        try {
            if (null != scriptEntry && scriptEntry.isActive()) {
                Script script = scriptEntry.getScript(context);
                script.put("object", value);
                if (additionalProps != null && !additionalProps.isNull()) {
                    for (String key : additionalProps.keys()) {
                        script.put(key, additionalProps.get(key));
                    }
                }
                try {
                    script.eval(); // allows direct modification to the object
                } catch (ScriptThrownException ste) {
                    // Allow for scripts to set their own exception
                    throw ste.toResourceException(ResourceException.INTERNAL_ERROR,
                            hook.name() + " script encountered exception");
                } catch (ScriptException se) {
                    String msg = hook.name() + " script encountered exception";
                    logger.debug(msg, se);
                    throw new InternalServerErrorException(msg, se);
                }
            }
        } finally {
            measure.end();
        }
    }
    
    public void executePostUpdate(Context context, Request request, String resourceId, JsonValue oldValue, 
            JsonValue newValue) throws ResourceException {
        // Execute the postUpdate script if configured
        execScript(context, ScriptHook.postUpdate, newValue,
                prepareScriptBindings(context, request, resourceId, oldValue, newValue));
    };

    /**
     * Prepares a map of additional bindings for the script hook invocation.
     *
     * @param context the current Context
     * @param request the Request being processed
     * @param resourceId the resourceId of the object being manipulated
     * @param oldObject the old object value
     * @param newObject the new object value
     * @return a JsonValue map of script bindings
     */
    private JsonValue prepareScriptBindings(Context context, Request request, String resourceId,
            JsonValue oldObject, JsonValue newObject) {
        JsonValue scriptBindings = json(object());
        scriptBindings.put("context", context);
        scriptBindings.put("request", request);
        scriptBindings.put("oldObject", oldObject.getObject());
        scriptBindings.put("newObject", newObject.getObject());
        // TODO once SCRIPT-1 is implemented, this can be removed and the resourceName can be obtained via context.router.getBaseUri()
        scriptBindings.put("resourceName", managedId(resourceId));
        return scriptBindings;
    }

    /**
     * Executes all of the necessary trigger scripts when an object is retrieved from the repository.
     *
     * @param context the current Context
     * @param request the Request being processed
     * @param resourceId the resourceId of the object being manipulated
     * @param value
     *            the JSON value that was retrieved from the repository.
     * @throws ForbiddenException
     *             if a validation trigger throws an exception.
     * @throws InternalServerErrorException
     *             if any other exception occurs.
     */
    private void onRetrieve(Context context, Request request, String resourceId, ResourceResponse value) throws ResourceException {
        execScript(context, ScriptHook.onRetrieve, value.getContent(),
                prepareScriptBindings(context, request, resourceId, new JsonValue(null), new JsonValue(null)));
        for (JsonPointer key : Collections.unmodifiableSet(getSchema().getFields().keySet())) {
            getSchema().getField(key).onRetrieve(context, value.getContent());
        }
    }

    private void populateVirtualProperties(final Context context, final Request request, final JsonValue content) throws ForbiddenException,
            InternalServerErrorException {
        for (JsonPointer key : Collections.unmodifiableSet(getSchema().getFields().keySet())) {
            SchemaField field = getSchema().getField(key);
            // Only populate if field is returned by default or explicitly requested
            if (field.isVirtual() && (field.isReturnedByDefault() || request.getFields().contains(key))) {
                field.onRetrieve(context, content);
            }
        }
    }

    /**
     * Executes all of the necessary trigger scripts when an object is to be stored in the repository.
     *
     * @param value
     *            the JSON value to be stored in the repository.
     * @throws ForbiddenException
     *             if a validation trigger throws an exception.
     * @throws InternalServerErrorException
     *             if any other exception occurs.
     */
    private void onStore(Context context, JsonValue value) throws ResourceException  {
        JsonValue scriptBindings = json(object());
        scriptBindings.put("context", context);
        scriptBindings.put("value", value.getObject());

        // Execute all individual onValidate scripts
        for (JsonPointer key : Collections.unmodifiableSet(getSchema().getFields().keySet())) {
            getSchema().getField(key).onValidate(context, value);
        }
        
        // Execute the root onValidate script
        execScript(context, ScriptHook.onValidate, value, scriptBindings);

        // Execute all individual onStore scripts
        for (JsonPointer key : Collections.unmodifiableSet(getSchema().getFields().keySet())) {
            getSchema().getField(key).onStore(context, value); // includes per-property encryption
        }
        
        // Execute the root onStore script
        execScript(context, ScriptHook.onStore, value, scriptBindings);
    }

    /**
     * Decrypt the value
     *
     * @param value
     *            a json value with potentially encrypted value(s)
     * @return object with values decrypted
     * @throws InternalServerErrorException
     *             if decryption failed for any reason
     */
    private JsonValue decrypt(final JsonValue value) throws InternalServerErrorException {
        try {
            return cryptoService.decrypt(value); // makes a copy, which we can modify
        } catch (JsonException je) {
            throw new InternalServerErrorException(je);
        }
    }

    /**
     * Decrypt the value
     *
     * @param value
     *            a json value with potentially encrypted value(s)
     * @return object with values decrypted
     * @throws InternalServerErrorException
     *             if decryption failed for any reason
     */
    private ResourceResponse decrypt(final ResourceResponse value) throws InternalServerErrorException {
        try {
            // makes a copy, which we can modify
            return newResourceResponse(value.getId(), value.getRevision(),
                    null != value.getContent() ? cryptoService.decrypt(value.getContent()) : null);
        } catch (JsonException je) {
            throw new InternalServerErrorException(je);
        }
    }

    /**
     * Forbid the use of sub objects
     *
     * @param id
     *            the identifier to check
     * @throws ForbiddenException
     *             if the identifier identifies a sub object
     */
    private void noSubObjects(String id) throws ForbiddenException {
        if (id != null && id.indexOf('/') >= 0) {
            throw new ForbiddenException("Sub-objects are not supported");
        }
    }

    /**
     * Forbid operation without id, on the whole object set
     *
     * @param id
     *            the identifier to check
     * @throws ForbiddenException
     *             if there is no identifier.
     */
    private void idRequired(String id) throws ForbiddenException {
        if (id == null) {
            throw new ForbiddenException("Operation not allowed on entire object set");
        }
    }

    /**
     * Update a resource as part of an update or patch request. This method will also be invoked from a triggerSyncCheck
     * (via the updateInstance method). Its fundamental concern is to perform diff logic between between the oldValue, 
     * which is obtained via a repo read, and the newValue, which is provided by the caller, and to trigger the 
     * appropriate repo persistence and sync actions as dictated by the specific differences between the oldValue and 
     * newValue.
     *
     * @param context the current Context
     * @param request the source Request
     * @param resourceId the resource id of the object being modified
     * @param rev the revision of the object being modified
     * @param oldValue the old value of the object, as read from the repo
     * @param newValue the new value of the object, as specified by the user, or as constituted via a router read 
     *                 request in the triggerSyncCheck action.
     * @param relationshipFields a set of relationship fields to persist. These fields must match the relationship
     *                           fields present in the oldValue JsonValue.
     * @return a {@link ResourceResponse} object representing the updated resource
     * @throws ResourceException
     */
    public ResourceResponse update(final Context context, Request request, String resourceId, String rev,
    		JsonValue oldValue, JsonValue newValue, Set<JsonPointer> relationshipFields)
            throws ResourceException {
        Context managedContext = new ManagedObjectContext(context);

        JsonValue decryptedNew = decrypt(newValue);
        JsonValue decryptedOld = decrypt(oldValue);
        
        if (isEqual(decryptedOld, decryptedNew)) { // object hasn't changed
            return newResourceResponse(resourceId, rev, oldValue);
        }

        // Execute the onUpdate script if configured
        execScript(context, ScriptHook.onUpdate, decryptedNew,
                prepareScriptBindings(context, request, resourceId, decryptedOld, decryptedNew));

        // determine if any onUpdate script manipulated a relationship field, and update the relationshipFields Set accordingly
        // and insure that the oldObject contains the repo-resident relationship state so that diff logic can be performed
        // appropriately.
        updateRelationshipFields(context, resourceId, relationshipFields, decryptedOld, decryptedNew);

        // Validate relationships before persisting
        validateRelationshipFields(managedContext, decryptedOld, decryptedNew);

        // Populate the virtual properties (so they are updated for sync-ing)
        populateVirtualProperties(context, request, decryptedNew);

        // Remove relationships so they don't get persisted in the repository with the managed object details.
        JsonValue strippedRelationshipFields = stripRelationshipFields(decryptedNew);

        // Perform pre-property encryption
        onStore(context, decryptedNew); // performs per-property encryption

        // Perform update
        UpdateRequest updateRequest = Requests.newUpdateRequest(repoId(resourceId), decryptedNew);
        updateRequest.setRevision(rev);
        ResourceResponse response = connectionFactory.getConnection().update(context, updateRequest);
        JsonValue responseContent = response.getContent();

        // Put relationships back in before we respond
        responseContent.asMap().putAll(strippedRelationshipFields.asMap());

        // Persists all relationship fields that are present in the new value and updates their values.
        responseContent.asMap().putAll(persistRelationships(false, managedContext, resourceId, oldValue, responseContent, relationshipFields)
                .asMap());

        // Execute the postUpdate script if configured

        executePostUpdate(context, request, resourceId, decryptedOld, responseContent);

        performSyncAction(context, request, resourceId, SynchronizationService.SyncServiceAction.notifyUpdate,
                decryptedOld, responseContent);

        ResourceResponse readResponse =
                connectionFactory.getConnection().read(context, Requests.newReadRequest(repoId(resourceId)));
        readResponse.getContent().asMap().putAll(strippedRelationshipFields.asMap());

        return readResponse;
    }

    /**
     * It is possible that a script updates a relationship field in one of the updated objects. If this is the case,
     * the relationshipFields set must be updated with this field name, so that the corresponding relationships can
     * be persisted. So if the relationshipFields does not contain a relationship field, and a difference between the
     * oldObject and newObject includes this field, then this field must be added to the relationshipFields collection.
     * In addition, if a relationship has been added to the newObject, then the oldObject should be updated with the
     * repo-resident state corresponding to this relationship, so that the diff logic has the correct state.
     * @param relationshipFields the set of relationship fields that should be updated
     * @param oldObject the managed object prior to the script onUpdate invocation
     * @param newObject the managed object following script onUpdate invocation
     */
    private void updateRelationshipFields(Context context, String resourceId, Set<JsonPointer> relationshipFields,
                                          JsonValue oldObject, JsonValue newObject) throws ResourceException {
        final Set<JsonPointer> systemRelationships = relationshipProviders.keySet();
        if (!relationshipFields.containsAll(systemRelationships)) {
            final JsonValue diff = JsonPatch.diff(oldObject, newObject);
            for (Map<String, Object> diffOp : diff.asList(Map.class)) {
                //not descriminating on type of diff - replace/add/remove will all result in relationshipField additions
                JsonPointer pathPointer = new JsonPointer((String) diffOp.get(JsonPatch.PATH_PTR.leaf()));
                if (systemRelationships.contains(pathPointer) && !relationshipFields.contains(pathPointer)) {
                    relationshipFields.add(pathPointer);
                    logger.info("In updateRelationshipFields, adding onUpdate-script-modified relationship to " +
                            "processed relationship set: {}", pathPointer);
                    // if the relationshipFields did not include a relationship which we have added to the newObject,
                    // populate the oldObject with the repo-resident state corresponding to the relationship to insure
                    // that the update diff logic has the correct state.
                    if ("add".equals(diffOp.get(JsonPatch.OP_PTR.leaf()))) {
                        try {
                            final JsonValue relationships = fetchRelationshipFields(context, resourceId,
                                    Collections.singletonList(pathPointer));
                            oldObject.asMap().putAll(relationships.asMap());
                            logger.info("In updateRelationshipFields, adding relationships {} to managed object {}.",
                                    relationships.toString(), resourceId);
                        } catch (ExecutionException | InterruptedException e) {
                            throw new InternalServerErrorException(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Persist all relationship fields contained in the JsonValue map to their accompanying
     * {@link #relationshipProviders}
     *
     * @param clearExisting If existing (those not present in the object) should be cleared
     * @param context The current context
     * @param resourceId The id of the resource these relationships are associated with
     * @param oldValue A JsonValue map of the old value
     * @param json A JsonValue map that contains relationship fields and value(s) to be persisted
     * @param relationshipFields a set of relationship fields to persist
     * @return A {@link JsonValue} map containing each relationship field and its persisted value(s)
     */
    private JsonValue persistRelationships(final boolean clearExisting, Context context, String resourceId,
            final JsonValue oldValue, final JsonValue json, Set<JsonPointer> relationshipFields) throws ResourceException {
        EventEntry measurement = Publisher.start(Name.get("openidm/internal/managedobjectset/persistRelationships"), json, context);

        try {
            final List<Promise<JsonValue, ResourceException>> persisted = new ArrayList<>();

            for (final JsonPointer relationshipField : relationshipFields) {
                // value of the relationship in the managed object
                JsonValue relationshipValue = json.expect(Map.class).get(relationshipField);
                if (relationshipValue == null) {
                    // If the relationship existed in the old value it means that it has been removed
                    // in the new object so we will remove the relationships
                    relationshipValue =
                            (oldValue.isNotNull() && oldValue.expect(Map.class).get(relationshipField) != null)
                                ? json(null)
                                : null;
                }
                // Relationships not present in the request will be null
                // Relationships present in the request but set to null will be JsonValue(null)
                if (relationshipValue != null) {
                    RelationshipProvider provider = relationshipProviders.get(relationshipField);
                    persisted.add(provider.setRelationshipValueForResource(clearExisting, context, resourceId,
                            relationshipValue).then(new Function<JsonValue, JsonValue, ResourceException>() {
                                                        @Override
                                                        public JsonValue apply(JsonValue jsonValue) throws ResourceException {
                                                            return json(object(field(relationshipField.leaf(), jsonValue.getObject())));
                                                        }
                                                    }
                    ));
                }
            }

            return when(persisted).then(new Function<List<JsonValue>, JsonValue, ResourceException>() {
                @Override
                public JsonValue apply(List<JsonValue> jsonValues) throws ResourceException {
                    final JsonValue joined = json(object());

                    // Join json maps
                    for (JsonValue value : jsonValues) {
                        joined.asMap().putAll(value.asMap());
                    }

                    return joined;
                }
            }).getOrThrowUninterruptibly();
        } finally {
            measurement.end();
        }
    }

    /**
     * Applies a patch document to an object, or by finding an object in the object set itself via query parameters. As 
     * this is an action, the patch document to be applied is in the {@code _entity} parameter.
     *
     * @param context the current Context
     * @param request the {@link ActionRequest}
     * @return a {@link ResourceResponse} representing the patched object.
     * @throws ResourceException
     */
    private ResourceResponse patchAction(final Context context, final ActionRequest request) throws ResourceException {
        if (!request.getContent().required().isList()) {
            throw new BadRequestException(
                    "The request could not be processed because the provided content is not a JSON array");
        }

        // Build query request from action parameters looking for query parameters
        // use JsonValue to coerce Map<String, String> to Map<String, Object> - blech
        QueryRequest queryRequest = RequestUtil.buildQueryRequestFromParameterMap(repoId(null),
                new JsonValue(request.getAdditionalParameters()).asMap());

        final List<PatchOperation> operations = PatchOperation.valueOfList(request.getContent());
        final List<ResourceResponse> resources = new ArrayList<ResourceResponse>();

        connectionFactory.getConnection().query(context, queryRequest,
                new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(ResourceResponse resource) {
                        logger.debug("Patch by query found resource " + resource.getId());
                        resources.add(resource);
                        return true;
                    }
                });
        if (resources.size() == 1) {
            try {
            	return patchResource(context, request, resources.get(0), null, operations);
            } catch (ResourceException e) {
                throw e;
            } catch (Exception e) {
            	throw new InternalServerErrorException(e.getMessage(), e);
            }
        } else if (resources.size() > 1) {
        	throw new InternalServerErrorException("Query result must yield one matching object");
        } else {
            throw new NotFoundException("Query returned no results");
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException>  createInstance(Context context, CreateRequest request) {
        String resourceId = request.getNewResourceId();
        JsonValue content = request.getContent();
        Context managedContext = new ManagedObjectContext(context);

        // Check if the new id is specified in content, and use it if it is.
        if (!content.get(FIELD_CONTENT_ID).isNull()) {
            resourceId = content.get(FIELD_CONTENT_ID).asString();
        }
        logger.debug("Create name={} id={}", name, resourceId);

        try {
            // decrypt any incoming encrypted properties
            JsonValue value = decrypt(content);

            // Execute onCreate script
            execScript(managedContext, ScriptHook.onCreate, value, 
                    prepareScriptBindings(managedContext, request, resourceId, new JsonValue(null), content));

            // Validate relationships before persisting
            validateRelationshipFields(managedContext, json(object()), value);

            // Populate the virtual properties (so they are available for sync-ing)
            populateVirtualProperties(managedContext, request, value);

            // Remove relationships so they don't get persisted in the repository with the managed object details.
            final JsonValue strippedRelationshipFields = stripRelationshipFields(value);

            // includes per-property encryption
            onStore(managedContext, value);

            // Persist the managed object in the repository
            CreateRequest createRequest = Requests.newCreateRequest(repoId(null), resourceId, value);
            ResourceResponse createResponse = connectionFactory.getConnection().create(managedContext, createRequest);
            content = createResponse.getContent();
            resourceId = createResponse.getId();

            activityLogger.log(managedContext, request, "create", managedId(resourceId).toString(), null, content, 
                    Status.SUCCESS);

            // Place stripped relationships back in content
            content.asMap().putAll(strippedRelationshipFields.asMap());

            // Persists all relationship fields and place their persisted values in content
            content.asMap().putAll(persistRelationships(true, managedContext, resourceId, json(null), content,
                    relationshipProviders.keySet()).asMap());

            // Execute the postCreate script if configured
            execScript(managedContext, ScriptHook.postCreate, content,
                    prepareScriptBindings(managedContext, request, resourceId, new JsonValue(null), content));

            // Sync any targets after managed object is created
            performSyncAction(managedContext, request, resourceId, SynchronizationService.SyncServiceAction.notifyCreate,
                    new JsonValue(null), content);

            ResourceResponse readResponse =
                    connectionFactory.getConnection().read(managedContext, Requests.newReadRequest(repoId(resourceId)));
            
            return prepareResponse(managedContext, readResponse, request.getFields()).asPromise();
        } catch (ResourceException e) {
        	return e.asPromise();
        } catch (Exception e) {
        	return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(final Context context, String resourceId, 
    		ReadRequest request) {
        logger.debug("Read name={} id={}", name, resourceId);
        Context managedContext = new ManagedObjectContext(context);
        try {

            ReadRequest readRequest = Requests.newReadRequest(repoId(resourceId));
            ResourceResponse readResponse = connectionFactory.getConnection().read(managedContext, readRequest);

            final JsonValue relationships = fetchRelationshipFields(managedContext, resourceId, request.getFields());
            readResponse.getContent().asMap().putAll(relationships.asMap());

            onRetrieve(managedContext, request, resourceId, readResponse);
            execScript(managedContext, onRead, readResponse.getContent(), null);
            activityLogger.log(managedContext, request, "read", managedId(readResponse.getId()).toString(),
                    null, readResponse.getContent(), Status.SUCCESS);
            
            return prepareResponse(managedContext, readResponse, request.getFields()).asPromise();
        } catch (ResourceException e) {
        	return e.asPromise();
        } catch (Exception e) {
        	return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    /**
     * Fetch the current relationship(s) for relationship fields set to be returned by default
     * or specified in the {@link ReadRequest#getFields()}
     *
     * @param context The current context
     * @param resourceId The id of the resource to fetch relationships of
     * @param requestFields The fields requested in the initial request
     * @return A {@link JsonValue} map containing all relationship fields and their values
     * @throws ResourceException 
     */
    private JsonValue fetchRelationshipFields(final Context context, final String resourceId,
            final List<JsonPointer> requestFields)
            throws ExecutionException, InterruptedException, ResourceException {
        EventEntry measure = Publisher.start(Name.get("openidm/internal/managed/set/fetchRealtionshipFields"), resourceId, context);

        try {
            final JsonValue joined = json(object());

            /*
             * Create set only containing the head of request fields
             * Allows for a relationship to be fetched when only an expansion is requested.
             * ie. a field of foo/name will retrieve the foo relationship
             */
            final Set<JsonPointer> fieldHeads = new HashSet<>();
            for (JsonPointer field : requestFields) {
                // A blank _fields param can yield a single '/' (empty) pointer
                if (!field.isEmpty()) {
                    fieldHeads.add(new JsonPointer(field.get(0)));
                }
            }

            for (Map.Entry<JsonPointer, RelationshipProvider> entry : relationshipProviders.entrySet()) {
                final JsonPointer field = entry.getKey();
                final RelationshipProvider provider = entry.getValue();

                if (requestFields.contains(SchemaField.FIELD_ALL_RELATIONSHIPS)
                        || provider.getSchemaField().isReturnedByDefault()
                        || fieldHeads.contains(field)) { // only check head of request fields (see above)
                    try {
                        joined.put(field, provider.getRelationshipValueForResource(context,
                                resourceId).getOrThrow().getObject());
                    } catch (NotFoundException e) {
                        logger.debug("No {} relationships found for {}", field, resourceId);
                        joined.put(field, null);
                    }
                } else {
                    // relationship was not requested or set to return by default
                    logger.debug("Relationship field {} skipped", field);
                }
            }

            return joined;
        } finally {
            measure.end();
        }
    }

    /**
     * This will traverse the jsonValue and validate that all relationship references are valid.
     *
     * @param oldValue previous state of the json.
     * @param newValue json object that will get its relationship fields validated.
     * @param context context of the request that is in progress.
     * @throws ResourceException BadRequestException when the first invalid relationship reference is discovered,
     * otherwise for other issues.
     */
    private void validateRelationshipFields(Context context, JsonValue oldValue, JsonValue newValue)
            throws ResourceException {
        for (JsonPointer field : schema.getRelationshipFields()) {
            if (schema.getField(field).isValidationRequired()) {
                relationshipProviders.get(field).validateRelationshipField(context,
                        oldValue.get(field) == null ? json(null) : oldValue.get(field),
                        newValue.get(field) == null ? json(null) : newValue.get(field));
            }
        }
    }

    /**
     * Called from the triggerSyncCheck action, or as part of the CollectionResourceProvider contract. When called from
     * triggerSyncCheck, the UpdateRequest will contain the ManagedObject as populated via a router READ - i.e. with
     * the virtual attributes, and any necessary relationships, populated. This method will read the specified resource
     * from the repo, and pass this (old) object, and the (new) object passed in the UpdateRequest parameter, to the update
     * method, so that the appropriate fields can be updated and synced.
     * Note that it is important to the diff logic in the update method that the repo-read (old) object and the caller-dispatched
     * (new) object reference the same fields - in particular relationship fields.
     * @param context the caller's context
     * @param resourceId the resource identifier
     * @param request the updated resource representation, as specified by the caller
     * @return the updated resource, referencing the fields as specified in the UpdateRequest.
     */
    @Override
    public Promise<ResourceResponse, ResourceException>  updateInstance(final Context context, final String resourceId,
    		final UpdateRequest request) {
        logger.debug("update {} ", "name=" + name + " id=" + resourceId + " rev=" + request.getRevision());
        Context managedContext = new ManagedObjectContext(context);

        /*
        First constitute the repo read request, including all fields specified in the UpdateRequest, minus the relationship
        fields, as these are stored in a separate table.
         */
        try {
            ReadRequest repoReadRequest = Requests.newReadRequest(repoId(resourceId));
            for (JsonPointer pointer : request.getFields()) {
                if (pointer.equals(SchemaField.FIELD_ALL)) {
                    repoReadRequest.addField("");
                } else if (!pointer.equals(SchemaField.FIELD_ALL_RELATIONSHIPS)) {
                    repoReadRequest.addField(pointer);
                }
            }
            ResourceResponse repoReadResponse = connectionFactory.getConnection().read(managedContext, repoReadRequest);

            /*
            Now populate the relationship fields as specified in the UpdateRequest.
             */
            final JsonValue relationships = fetchRelationshipFields(managedContext, resourceId, request.getFields());
            repoReadResponse.getContent().asMap().putAll(relationships.asMap());

            /*
            Now call the update method, passing the repo-read object as the old object, and the caller-specified object
            as the new object, so that the update method can perform the appropriate diff logic and the resulting sync
            actions.
             */
            ResourceResponse updatedResponse = update(managedContext, request, resourceId, request.getRevision(),
            		repoReadResponse.getContent(), request.getContent(), relationshipProviders.keySet());
            
            activityLogger.log(managedContext, request, "update", managedId(repoReadResponse.getId()).toString(),
                    repoReadResponse.getContent(), updatedResponse.getContent(), Status.SUCCESS);

            return prepareResponse(managedContext, updatedResponse, request.getFields()).asPromise();
        } catch (ResourceException e) {
        	return e.asPromise();
        } catch (Exception e) {
        	return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, final String resourceId, 
    		final DeleteRequest request) {
        logger.debug("Delete {} ", "name=" + name + " id=" + resourceId + " rev=" + request.getRevision());
        Context managedContext = new ManagedObjectContext(context);
        try {
            ReadRequest readRequest = Requests.newReadRequest(repoId(resourceId));
            ResourceResponse resource = connectionFactory.getConnection().read(managedContext, readRequest);
            
            // Populate the relationship fields in the read resource
            final JsonValue relationships = fetchRelationshipFields(managedContext, resourceId, request.getFields());
            resource.getContent().asMap().putAll(relationships.asMap());
            
            execScript(managedContext, ScriptHook.onDelete, decrypt(resource.getContent()), null);

            // Delete the resource
            DeleteRequest deleteRequest = Requests.newDeleteRequest(repoId(resourceId));

            if (request.getRevision() != null) {
                deleteRequest.setRevision(request.getRevision());
            } else {
                deleteRequest.setRevision(resource.getRevision());
            }

            connectionFactory.getConnection().delete(managedContext, deleteRequest);

            // Delete any relationships associated with this resource
            final List<Promise<JsonValue, ResourceException>> deleted = new ArrayList<>();
            for (RelationshipProvider relationshipProvider : relationshipProviders.values()) {
                deleted.add(relationshipProvider.clear(managedContext, resourceId));
            }
            // Wait for deletions to complete before continuing
            when(deleted).getOrThrowUninterruptibly();

            activityLogger.log(managedContext, request, "delete", managedId(resource.getId()).toString(),
                    resource.getContent(), null, Status.SUCCESS);

            // Execute the postDelete script if configured
            execScript(managedContext, ScriptHook.postDelete, null, prepareScriptBindings(managedContext, request, resourceId,
                    resource.getContent(), new JsonValue(null)));

            // Perform notifyDelete synchronization
            performSyncAction(managedContext, request, resourceId, SynchronizationService.SyncServiceAction.notifyDelete,
                    resource.getContent(), new JsonValue(null));

            return prepareResponse(managedContext, resource, request.getFields()).asPromise();
        } catch (ResourceException e) {
        	return e.asPromise();
        } catch (Exception e) {
        	return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId, 
    		PatchRequest request) {
        try {
        	return newResultPromise(patchResourceById(new ManagedObjectContext(context), request, resourceId, 
        	        request.getRevision(), request.getPatchOperations()));
        } catch (ResourceException e) {
        	return e.asPromise();
        }
    }

    /**
     * Patches the given resource and will also remove private properties if it is an external call based upon context.
     *
     * @param context
     * @param request
     * @param resourceId
     * @param revision Expected revision of the resource. Patch will fail if non-null and not matching.
     * @param patchOperations
     *
     * @return The patched ResourceResponse with private properties omitted if called externally.
     *
     * @throws ResourceException
     */
    private ResourceResponse patchResourceById(Context context, Request request, String resourceId, String revision, 
            List<PatchOperation> patchOperations)
            throws ResourceException {
        idRequired(request.getResourcePath());
        noSubObjects(request.getResourcePath());

        // Get the oldest value for diffing in the log
        // JsonValue oldValue = new JsonValue(cryptoService.getRouter().read(repoId(id)));
        ReadRequest readRequest = Requests.newReadRequest(repoId(resourceId));
        ResourceResponse resource = connectionFactory.getConnection().read(context, readRequest);

        return patchResource(context, request, resource, revision, patchOperations);
    }

    /**
     * Patches the given resource and will also remove private properties if it is an external call based upon context.
     *
     * @param context
     * @param request
     * @param resource The resource to be patched
     * @param revision
     * @param patchOperations
     *
     * @return The patched ResourceResponse with private properties omitted if called externally.
     *
     * @throws ResourceException
     */
    private ResourceResponse patchResource(Context context, Request request, ResourceResponse resource, String revision, 
            List<PatchOperation> patchOperations)
        throws ResourceException {

        // FIXME: There's no way to decrypt a patch document. :-( Luckily, it'll work for now with patch action.

        boolean forceUpdate = (revision == null);
        boolean retry = forceUpdate;
        String rev = revision;

        do {
            logger.debug("patch name={} id={}", name, request.getResourcePath());
            try {
                // Keep a copy of the oldValue
                JsonValue oldValue = resource.getContent().copy();

                // If we haven't defined a revision, we need to get the current revision
                if (revision == null) {
                    rev = oldValue.get("_rev").asString();
                }
                
                // Create a Set containing all the patched relationship fields
                Set<JsonPointer> patchedRelationshipFields = new HashSet<JsonPointer>();
                for (PatchOperation operation : patchOperations) {
                    // Getting the first token as we currently only support top-level relationship fields
                    // This allows us to ignore trailing array index's or '-' characters.
                    JsonPointer field = new JsonPointer(operation.getField().get(0));
                    SchemaField schemaField = schema.getField(field);
                    if (schemaField != null && schemaField.isRelationship()) {
                        if (schemaField.isArray() && operation.getValue().isNull()) {
                            throw new BadRequestException("Cannot delete collection: " + field.toString());
                        }
                        patchedRelationshipFields.add(field);
                    }
                }
                
                // Merge the relationship fields with the fields specified in the request
                final Set<JsonPointer> allFields = new HashSet<JsonPointer>(request.getFields());
                allFields.addAll(patchedRelationshipFields);
                
                // Fetch the relationship fields
                final JsonValue relationships = fetchRelationshipFields(context, resource.getId(), 
                        new ArrayList<JsonPointer>(allFields));

                // Populate the oldValue with the relationship fields
                oldValue.asMap().putAll(relationships.asMap());

                JsonValue newValue = decrypt(oldValue);
                boolean modified = JsonValuePatch.apply(newValue, patchOperations);
                if (!modified) {
                    ResourceResponse response = newResourceResponse(resource.getId(), revision, oldValue);
                    return prepareResponse(context, response, request.getFields());
                }

                // Check if policies should be enforced
                if (enforcePolicies) {
                    // Build up a map of properties to validate (only the patched properties)
                    JsonValue propertiesToValidate = json(object());
                    for (PatchOperation operation : patchOperations) {
                        // Getting the first token as we currently only support top-level relationship fields
                        // This allows us to ignore trailing array index's or '-' characters.
                        String field = operation.getField().get(0);
                        if (newValue.keys().contains(field)) {
                            propertiesToValidate.put(field, newValue.get(field));
                        }
                    }
                    // The action request to validate the policy of all the patched properties
                    ActionRequest policyAction = Requests.newActionRequest(
                            ResourcePath.valueOf("policy").concat(managedId(resource.getId())).toString(), 
                            "validateProperty").setContent(propertiesToValidate);
                    if (ContextUtil.isExternal(context)) {
                        // this parameter is used in conjunction with the test in policy.js to ensure that the 
                        // re-authentication policy is enforced.
                        policyAction.setAdditionalParameter("external", "true");
                    }
                    JsonValue result = connectionFactory.getConnection().action(context, policyAction).getJsonContent();
                    if (!result.isNull() && !result.get("result").asBoolean()) {
                        logger.debug("Requested patch failed policy validation: {}", result);
                        throw new ForbiddenException("Failed policy validation").setDetail(result);
                    }
                }

                ResourceResponse patchedResource =
                        update(context, request, resource.getId(), rev, oldValue, newValue, patchedRelationshipFields);

                activityLogger.log(context, request, "", managedId(patchedResource.getId()).toString(),
                        oldValue, patchedResource.getContent(), Status.SUCCESS);
                retry = false;
                logger.debug("Patch successful!");

                return prepareResponse(context, patchedResource, request.getFields());
            } catch (PreconditionFailedException e) {
                if (forceUpdate) {
                    logger.debug("Unable to update due to revision conflict. Retrying.");
                } else {
                    // If it fails and we're not trying to force an update, we gave it our best shot
                    throw e;
                }
            } catch (ResourceException e) {
                throw e;
            } catch (Exception e) {
                throw new InternalServerErrorException(e.getMessage(), e);
            }
        } while (retry);
        return null;
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(final Context context, final QueryRequest request,
            final QueryResourceHandler handler) {
        logger.debug("query name={} id={}", name, request.getResourcePath());
        final Context managedContext = new ManagedObjectContext(context);
        
        // The "executeOnRetrieve" parameter is used to indicate if is returning a full managed object
        String executeOnRetrieve = request.getAdditionalParameter("executeOnRetrieve");
        
        // The onRetrieve script should only be run queries that return full managed objects
        final boolean onRetrieve = executeOnRetrieve == null
                ? false
                : Boolean.parseBoolean(executeOnRetrieve);

        final List<Map<String,Object>> results = new ArrayList<Map<String,Object>>();
        final ResourceException[] ex = new ResourceException[]{null};
        try {
            // Create new QueryRequest to send to the repository
            // Does not include any fields specified in the current request
            QueryRequest repoRequest = Requests.newQueryRequest(repoId(null));
            repoRequest.setQueryId(request.getQueryId());
            repoRequest.setQueryFilter(request.getQueryFilter());
            repoRequest.setQueryExpression(request.getQueryExpression());
            repoRequest.setPageSize(request.getPageSize());
            repoRequest.setPagedResultsOffset(request.getPagedResultsOffset());
            repoRequest.setPagedResultsCookie(request.getPagedResultsCookie());
            repoRequest.setTotalPagedResultsPolicy(request.getTotalPagedResultsPolicy());
            repoRequest.addSortKey(request.getSortKeys().toArray(new SortKey[request.getSortKeys().size()]));
            for (String key : request.getAdditionalParameters().keySet()) {
                repoRequest.setAdditionalParameter(key, request.getAdditionalParameter(key));
            }
        	
        	QueryResponse queryResponse = connectionFactory.getConnection().query(managedContext, repoRequest,
            		new QueryResourceHandler() {
                @Override
                public boolean handleResource(ResourceResponse resource) {
                    ResourceResponse resourceResponse = null;
                    // Check if the onRetrieve script should be run
                    if (onRetrieve) {
                        try {
                            onRetrieve(managedContext, request, resource.getId(), resource);
                        } catch (ResourceException e) {
                        	ex[0] = e;
                            return false;
                        }
                    }
                    if (ServerConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                        // Don't populate relationships if this is a query-all-ids query.
                        resourceResponse = resource;    
                    } else {
                        // Populate the relationship fields
                        try {
                            JsonValue relationships = fetchRelationshipFields(managedContext, resource.getId(), request.getFields());
                            resource.getContent().asMap().putAll(relationships.asMap());
                            resourceResponse = prepareResponse(managedContext, resource, request.getFields());
                        } catch (ResourceException e) {
                            ex[0] = e;
                            return false;
                        } catch (Exception e) {
                            ex[0] = new InternalServerErrorException(e.getMessage(), e);
                            return false;
                        }
                    }
                    results.add(resourceResponse.getContent().asMap());
                    return handler.handleResource(prepareResponse(managedContext, resourceResponse, request.getFields()));
                }
            });
        	
        	if(ex[0] != null) {
            	return ex[0].asPromise();
        	}
        	
            activityLogger.log(managedContext, request, 
            		"query: " + request.getQueryId() + ", parameters: " + request.getAdditionalParameters(), 
            		request.getQueryId(), null, new JsonValue(results), Status.SUCCESS);
            
        	return queryResponse.asPromise();

        } catch (ResourceException e) {
        	return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId, 
    		ActionRequest request) {
        final Context managedContext = new ManagedObjectContext(context);

        try {
            activityLogger.log(managedContext, request, "Action: " + request.getAction(),
                    managedId(resourceId).toString(), null, null, Status.SUCCESS);
            switch (request.getActionAsEnum(Action.class)) {
                case patch:
                    final List<PatchOperation> operations = PatchOperation.valueOfList(request.getContent());
                    ResourceResponse patchResponse = patchResourceById(managedContext, request, resourceId, null, operations);
                    return newActionResponse(patchResponse.getContent()).asPromise();
                case triggerSyncCheck:
                    // Sync changes if required
                    // Read in managed object via the router to get updated virtual attributes. The result of the read request will be
                    // compared against the last sync'd value stored in the repository (in the updateInstance() request 
                    // below) to determine an update/sync is required.
                    final List<JsonPointer> requestFields = request.getFields();
                    final ReadRequest readRequest = Requests.newReadRequest(managedId(resourceId).toString());
                    if (!requestFields.isEmpty()) {
                        readRequest.addField(requestFields.toArray(new JsonPointer[requestFields.size()]));
                    }
                    logger.debug("Attempt sync of {}", readRequest.getResourcePath());
                    ResourceResponse currentResource = connectionFactory.getConnection().read(managedContext, readRequest);
                    UpdateRequest updateRequest = Requests.newUpdateRequest(readRequest.getResourcePath(),
                    		currentResource.getContent());
                    if (!requestFields.isEmpty()) {
                        updateRequest.addField(requestFields.toArray(new JsonPointer[requestFields.size()]));
                    }
                    ResourceResponse updateResponse = updateInstance(managedContext, resourceId, updateRequest).get();
                    logger.debug("Sync of {} complete", readRequest.getResourcePath());
                    return Responses.newActionResponse(updateResponse.getContent()).asPromise();
                default:
                    throw new BadRequestException("Action " + request.getAction() + " is not supported.");
            }
        } catch (ResourceException e) {
        	return e.asPromise();
        } catch (IllegalArgumentException e) { 
        	// from getActionAsEnum
        	return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (Exception e) {
        	return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    /**
     * Processes action requests.
     * <p>
     * If the {@code _action} parameter is {@code patch}, then the request is
     * handled as a partial modification to an object, either explicitly
     * (identifier is supplied) or by query (query parameters specify the query
     * to perform to yield a single object to patch.
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        logger.debug("action name={} id={}", name, request.getResourcePath());
        final Context managedContext = new ManagedObjectContext(context);

        try {
            activityLogger.log(managedContext, request, "Action: " + request.getAction(),
                    request.getResourcePath(), null, null, Status.SUCCESS);
            switch (request.getActionAsEnum(Action.class)) {
                case patch:
                    return newActionResponse(patchAction(managedContext, request).getContent()).asPromise();
                default:
                    throw new BadRequestException("Action " + request.getAction() + " is not supported.");
            }
        } catch (ResourceException e) {
        	return e.asPromise();
        } catch (IllegalArgumentException e) { 
        	// from getActionAsEnum
        	return new BadRequestException(e.getMessage(), e).asPromise();
        }
    }

    // -------- Implements the ScriptListener

    @Override
    public void scriptChanged(ScriptEvent event) throws ScriptException {
        switch (event.getType()) {
        case ScriptEvent.UNREGISTERING:
            logger.error("Script {} became unavailable", event.getScriptLibraryEntry().getName());
        }
    }

    /**
     * Returns the name of the managed object set.
     */
    public String getName() {
        return name;
    }

    public String getTemplate() {
        return name.indexOf('/') == 0 ? name : '/' + name;
    }
    

    /**
     * Prepares the response contents by removing the following: any private properties (if the request is from an 
     * external call), any virtual or relationship properties that are not set to returnByDefault.
     * 
     * @param context the current ServerContext
     * @param resource the Resource to prepare
     * @param requestFields a list of fields to return specified in the request
     * @return the prepared Resource object
     * @throws ResourceException 
     */
    private ResourceResponse prepareResponse(Context context, ResourceResponse resource,
            final List<JsonPointer> requestFields) {
        Map<JsonPointer, SchemaField> fieldsToRemove = new HashMap<>(schema.getHiddenByDefaultFields());
        Map<JsonPointer, List<JsonPointer>> resourceExpansionMap = new HashMap<>();
        List<JsonPointer> fields = new ArrayList<>();
        if (requestFields != null && requestFields.size() > 0) {
            fields.addAll(requestFields);
            for (JsonPointer field : new ArrayList<>(fields)) {
                if (field.equals(SchemaField.FIELD_ALL_RELATIONSHIPS)) {
                    // Return all relationship fields, so remove them from fieldsToRemove map
                    for (JsonPointer key : schema.getRelationshipFields()) {
                        logger.debug("Allowing field {} to be returned, due to *_ref", key);
                        fieldsToRemove.remove(key);
                        fields.add(key);
                    }
                    fields.remove(field);
                } else if (!field.equals(SchemaField.FIELD_ALL) && !field.equals(SchemaField.FIELD_EMPTY)){
                    if (schema.hasField(field)) {
                        // Allow the field by removing it from the fieldsToRemove list.
                        logger.debug("Allowing field {} to be returned", field);
                        fieldsToRemove.remove(field);
                    } else if (schema.hasArrayIndexedField(field)) {
                        // Allow the indexed array field (ex: role/0) by removing it from the fieldsToRemove list.
                        logger.debug("Allowing field {} to be returned", field.parent());
                        fieldsToRemove.remove(field.parent());
                    } else {
                        // Check for resource expansion and build up map of fields to expand
                        Pair<JsonPointer, JsonPointer> expansionPair = schema.getResourceExpansionField(field);
                        if (expansionPair != null) {
                            JsonPointer relationshipField = expansionPair.getFirst();
                            // Allow the field by removing it from the fieldsToRemove list (if there)
                            fieldsToRemove.remove(relationshipField);
                            // Add the field to the expansion map
                            if (!resourceExpansionMap.containsKey(relationshipField)) {
                            	// Initialize the list of fields in the resource expansion map
                                resourceExpansionMap.put(relationshipField, new ArrayList<JsonPointer>());
                                // Add the relationship field to the fields list (so it is included in the response)
                                fields.add(relationshipField);
                            }
                            // Remove the expanded field from the list of fields (since it will be included as part of
                            // the relationship field (after resource expansion) in the response.
                            fields.remove(field);
                            resourceExpansionMap.get(relationshipField).add(expansionPair.getSecond());
                        }
                    }

                } else if (field.equals(SchemaField.FIELD_ALL)) {
                	fields.remove(field);
                	fields.add(SchemaField.FIELD_EMPTY);
                }
            }
        }

        // Remove all relationship and virtual fields that are not returned by default, or explicitly listed
        for (JsonPointer key : fieldsToRemove.keySet()) {
            logger.debug("Removing field {} from the response object", key);
            resource.getContent().remove(key);
        }

        // List of promises representing results of resource expansion
        List<Promise<ResourceResponse, ResourceException>> promises = new ArrayList<>();
        // Loop over the relationship fields to expand
        for (JsonPointer fieldToExpand : resourceExpansionMap.keySet()) {
            // The schema for the field to expand
            SchemaField schemaField = schema.getField(fieldToExpand);
            // The list of fields to include from the expanded resource
            List<JsonPointer> fieldsList = resourceExpansionMap.get(fieldToExpand);
            // The value of the relationship field
            JsonValue fieldValue = resource.getContent().get(fieldToExpand);
            try {
                // Perform the resource expansion
                if (fieldValue != null && !fieldValue.isNull()) {
                    if (schemaField.isArray()) {
                        // The field is an array of relationship objects
                        for (JsonValue value : fieldValue) {
                            promises.add(expandResource(context, value, fieldsList));
                        }
                    } else {
                        // The field is a relationship object  
                        promises.add(expandResource(context, fieldValue, fieldsList));
                    }
                } else {
                    logger.debug("Cannot expand a null relationship object");
                }
            } catch (ResourceException e) {
                logger.error("Error expanding resource " + fieldToExpand + " with value " + fieldValue, e);
            }
        }
        
        try {
            when(promises).getOrThrowUninterruptibly();
        } catch (ResourceException e) {
            // Exceptions are already handled in expandResource, so this should never happen.
            logger.error("Error performing resource expansion", e);
        }
        
        // only cull private properties if this is an external call
        if (ContextUtil.isExternal(context)) {
            for (JsonPointer key : Collections.unmodifiableSet(getSchema().getFields().keySet())) {
                SchemaField field = getSchema().getField(key);
                if (field.isPrivate()) {
                    resource.getContent().remove(field.getName());
                }
            }
        }
        
        // Update the list of fields in the response
        if (fields.size() > 0) {
        	resource.addField(fields.toArray(new JsonPointer[fields.size()]));
        }
        
        return resource;
    }

    /**
     * Expands the provided resource represented by a {@link JsonValue} relationship object.  A read request  will be 
     * issued for the resource identified by the "_ref" field in the supplied relationship object. A supplied 
     * {@link List} of fields indicates which fields to read and then merge with the relationship object.
     *    
     * @param context the {@link Context} of the request
     * @param value the value of the relationship object
     * @param fieldsList the list of fields to read and merge with the relationship object.
     * @throws ResourceException if an error is encountered.
     */
    private Promise<ResourceResponse, ResourceException> expandResource(Context context, final JsonValue value, 
            List<JsonPointer> fieldsList) throws ResourceException {
        if (!value.isNull() && value.get(SchemaField.FIELD_REFERENCE) != null) {
            final Connection connection = ContextUtil.isExternal(context) 
                    ? connectionFactory.getExternalConnection()
                    : connectionFactory.getConnection();
            // Create and issue a read request on the referenced resource with the specified list of fields
            ReadRequest request = Requests.newReadRequest(value.get(SchemaField.FIELD_REFERENCE).asString());
            request.addField(fieldsList.toArray(new JsonPointer[fieldsList.size()]));
            return connection.readAsync(context, request).thenOnResultOrException(
                    new ResultHandler<ResourceResponse>() {
                        @Override
                        public void handleResult(ResourceResponse resource) {
                            // Merge the result with the supplied relationship object
                            value.asMap().putAll(resource.getContent().asMap());
                        }
                    }, new ExceptionHandler<ResourceException>() {
                        @Override
                        public void handleException(ResourceException exception) {
                            Map<String, Object> valueMap = value.asMap();
                            valueMap.put(RelationshipUtil.REFERENCE_ERROR, true);
                            valueMap.put(RelationshipUtil.REFERENCE_ERROR_MESSAGE, exception.getMessage());
                        }
                    });
        } else {
            logger.warn("Cannot expand a null relationship object");
            return newResourceResponse(null, null, null).asPromise();
        }
    }

    /**
     * Removes all relationship fields from the supplied {@link JsonValue} instance of a managed object.  Returns a 
     * {@link JsonValue} object containing the stripped fields.
     *
     * @param value The JsonValue map to strip relationship fields from
     * @return A {@link JsonValue} containing the stripped fields.
     */
    private JsonValue stripRelationshipFields(final JsonValue value) {
        value.expect(Map.class);

        final JsonValue stripped = json(object());

        for (JsonPointer field : schema.getRelationshipFields()) {
            final JsonValue fieldValue = value.get(field);
            final Object strippedValue;
            if (null != fieldValue) {
                strippedValue = fieldValue.getObject();
                stripped.put(field, strippedValue);
                value.remove(field);
            }
        }

        return stripped;
    }

    public void performSyncAction(final Context context, final Request request, final String resourceId,
            final SynchronizationService.SyncServiceAction action, final JsonValue oldValue, final JsonValue newValue)
        throws ResourceException {

        // The "sync" route may be down (unconfigured) or in the process of being re-configured;
        // if this is the case, we don't want a router error on the ActionRequest below.  Just log
        // the warning and return.  When the SynchronizationService comes back up (or when the
        // reconfiguration is complete), the AtomicReference<RouteService> in ManagedObjectService
        // will get set again.
        if (null == syncRoute.get()) {
            logger.warn("Sync service was not available.");
            return;
        }
        if (context.containsContext(SyncContext.class)
                && !context.asContext(SyncContext.class).isSyncEnabled()) {
            // Do not try to sync if sync has been disabled
            logger.debug("Sync has been disabled. {} ", context.asContext(SyncContext.class));
            return;
        }

        try {
            JsonValue content = new JsonValue(new LinkedHashMap<String, Object>(2));
            content.put("oldValue", oldValue.getObject());
            content.put("newValue", newValue.getObject());
            final ActionRequest syncRequest = Requests.newActionRequest("sync", action.name())
                    .setAdditionalParameter(SynchronizationService.ACTION_PARAM_RESOURCE_CONTAINER, managedObjectPath.toString())
                    .setAdditionalParameter(SynchronizationService.ACTION_PARAM_RESOURCE_ID, resourceId)
                    .setContent(content);

            JsonValue details;
            boolean success = false;
            try {
				ActionResponse actionResponse = connectionFactory.getConnection().action(context, syncRequest);
				success = true;
				details = actionResponse.getJsonContent();
			} catch (ResourceException e) {
				success = false;
				details = e.getDetail();
			} catch (Exception e) {
				success = false;
				details = new InternalServerErrorException(e.getMessage(), e).getDetail();
			}
            
            try {
            	// Execute the sync script
                ResourceResponse readResponse = newValue.isNotNull()
                        ? connectionFactory.getConnection().read(context, Requests.newReadRequest(repoId(resourceId)))
                        : newResourceResponse(null, null, json(null));
                JsonValue scriptBindings = prepareScriptBindings(context, request, resourceId, oldValue,
                        readResponse.getContent());
                Map<String,Object> syncResults = new HashMap<>();
                syncResults.put("success", success);
                syncResults.put("action", action.name());
                syncResults.put("syncDetails", details.getObject());
                scriptBindings.put("syncResults", syncResults);
                execScript(context, ScriptHook.onSync, null, scriptBindings);
            } catch (ResourceException e) {
                logger.warn("Failed executing onSync script on {} {}:{}",
                        syncRequest.getAction(), name, syncRequest.getResourcePath(), e);
            	throw e;
            }
        } catch (NotFoundException e) {
            logger.error("Failed to sync {} {}:{}", action.name(), name, resourceId, e);
            throw e;
        }
    }

    /**
     * Get the {@link ResourcePath} associated with this set.
     * @return The {@link ResourcePath} associated with this object set.
     */
    public ResourcePath getPath() {
        return managedObjectPath;
    }

    /**
     * Get the {@link ManagedObjectSchema} associated with this set.
     * @return The {@link ManagedObjectSchema} associated with this object set.
     */
    public ManagedObjectSchema getSchema() {
        return schema;
    }

    /**
     * Get the current map of {@link RelationshipProvider} for each relationship field.
     */
    Map<JsonPointer, RelationshipProvider> getRelationshipProviders() {
        return relationshipProviders;
    }
}
