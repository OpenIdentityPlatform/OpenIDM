/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openidm.managed;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.ScriptException;

import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceName;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.audit.util.RouterActivityLogger;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.patch.JsonValuePatch;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.openidm.sync.impl.SynchronizationService;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.openidm.util.RequestUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptEvent;
import org.forgerock.script.ScriptListener;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.exception.ScriptThrownException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.forgerock.openidm.managed.ManagedObjectSet.ScriptHook.onCreate;
import static org.forgerock.openidm.managed.ManagedObjectSet.ScriptHook.onRead;
import static org.forgerock.openidm.managed.ManagedObjectSet.ScriptHook.onUpdate;
import static org.forgerock.openidm.managed.ManagedObjectSet.ScriptHook.onDelete;
import static org.forgerock.openidm.managed.ManagedObjectSet.ScriptHook.postCreate;
import static org.forgerock.openidm.managed.ManagedObjectSet.ScriptHook.postUpdate;
import static org.forgerock.openidm.managed.ManagedObjectSet.ScriptHook.postDelete;
import static org.forgerock.openidm.managed.ManagedObjectSet.ScriptHook.onValidate;
import static org.forgerock.openidm.managed.ManagedObjectSet.ScriptHook.onRetrieve;
import static org.forgerock.openidm.managed.ManagedObjectSet.ScriptHook.onStore;
import static org.forgerock.openidm.managed.ManagedObjectSet.ScriptHook.onSync;
import static org.forgerock.openidm.sync.impl.SynchronizationService.ACTION_PARAM_RESOURCE_CONTAINER;
import static org.forgerock.openidm.sync.impl.SynchronizationService.ACTION_PARAM_RESOURCE_ID;

/**
 * Provides access to a set of managed objects of a given type: managed/[type]/{id}.
 *
 */
class ManagedObjectSet implements CollectionResourceProvider, ScriptListener {

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
    private final ConnectionFactory connectionFactory;

    /** Audit Activity Log helper */
    private final ActivityLogger activityLogger;

    /** Name of the managed object type. */
    private final String name;

    /** the managed object path (e.g. managed/user) as a ResourceName */
    private final ResourceName managedObjectPath;

    /** The schema to use to validate the structure and content of the managed object. */
    private final JsonValue schema;

    /** Map of scripts to execute on specific {@link ScriptHook}s. */
    private final Map<ScriptHook, ScriptEntry> scriptHooks = new EnumMap<ScriptHook, ScriptEntry>(ScriptHook.class);

    /** Properties for which triggers are executed during object set operations. */
    private final ArrayList<ManagedObjectProperty> properties = new ArrayList<ManagedObjectProperty>();

    /** reference to the sync service route; used to decided whether or not to perform a sync action */
    private final AtomicReference<RouteService> syncRoute;

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
            final AtomicReference<RouteService> syncRoute, ConnectionFactory connectionFactory, JsonValue config)
            throws JsonValueException, ScriptException {
        this.cryptoService = cryptoService;
        this.syncRoute = syncRoute;
        this.connectionFactory = connectionFactory;
        this.activityLogger = new RouterActivityLogger(connectionFactory);
        name = config.get("name").required().asString();
        if (name.trim().isEmpty() || name.indexOf('{') > 0 | name.indexOf('}') > 0) {
            throw new JsonValueException(config.get("name"), "Failed to validate the name");
        }
        this.managedObjectPath = new ResourceName("managed").child(name);
        // TODO: parse into json-schema object
        schema = config.get("schema").expect(Map.class);
        for (ScriptHook hook : ScriptHook.values()) {
            if (config.isDefined(hook.name())) {
                scriptHooks.put(hook, scriptRegistry.takeScript(config.get(hook.name())));
            }
        }
        for (JsonValue property : config.get("properties").expect(List.class)) {
            properties.add(new ManagedObjectProperty(scriptRegistry, cryptoService, property));
        }
        enforcePolicies =
                Boolean.parseBoolean(IdentityServer.getInstance().getProperty(
                        "openidm.policy.enforcement.enabled", "true"));
        logger.debug("Instantiated managed object set: {}", name);
    }

    /**
     * Generates a fully-qualified object identifier for the managed object.
     *
     * @param resourceId
     *            the local managed object identifier to qualify.
     * @return the fully-qualified managed object identifier.
     */
    private ResourceName managedId(String resourceId) {
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
        return ResourceName.valueOf("repo").concat(managedId(resourceId)).toString();
    }

    /**
     * Executes a script if it exists, populating an {@code "object"} property
     * in the root scope.
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
    private void execScript(final ServerContext context, ScriptHook hook, JsonValue value, JsonValue additionalProps)
            throws ResourceException {
        final ScriptEntry scriptEntry = scriptHooks.get(hook);
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
    }

    /**
     * Prepares a map of additional bindings for the script hook invocation.
     *
     * @param context the current ServerContext
     * @param request the Request being processed
     * @param resourceId the resourceId of the object being manipulated
     * @param oldObject the old object value
     * @param newObject the new object value
     * @return a JsonValue map of script bindings
     */
    private JsonValue prepareScriptBindings(ServerContext context, Request request, String resourceId,
            JsonValue oldObject, JsonValue newObject) {
        JsonValue scriptBindings = new JsonValue(new HashMap<String, Object>());
        scriptBindings.put("context", context);
        scriptBindings.put("request", request);
        scriptBindings.put("oldObject", oldObject.getObject());
        scriptBindings.put("newObject", newObject.getObject());
        // TODO once SCRIPT-1 is implemented, this can be removed and the resourceName can be obtained via context.router.getBaseUri()
        scriptBindings.put("resourceName", managedId(resourceId));
        return scriptBindings;
    }

    /**
     * Executes all of the necessary trigger scripts when an object is retrieved
     * from the repository.
     *
     * @param context the current ServerContext
     * @param request the Request being processed
     * @param resourceId the resourceId of the object being manipulated
     * @param value
     *            the JSON value that was retrieved from the repository.
     * @throws ForbiddenException
     *             if a validation trigger throws an exception.
     * @throws InternalServerErrorException
     *             if any other exception occurs.
     */
    private void onRetrieve(ServerContext context, Request request, String resourceId, Resource value) throws ResourceException {
        execScript(context, onRetrieve, value.getContent(),
                prepareScriptBindings(context, request, resourceId, new JsonValue(null), new JsonValue(null)));
        for (ManagedObjectProperty property : properties) {
            property.onRetrieve(context, value.getContent());
        }
    }

    private void populateVirtualProperties(ServerContext context, JsonValue content) throws ForbiddenException,
            InternalServerErrorException {
        for (ManagedObjectProperty property : properties) {
            if (property.isVirtual()) {
                property.onRetrieve(context, content);
            }
        }
    }

    /**
     * Executes all of the necessary trigger scripts when an object is to be
     * stored in the repository.
     *
     * @param value
     *            the JSON value to be stored in the repository.
     * @throws ForbiddenException
     *             if a validation trigger throws an exception.
     * @throws InternalServerErrorException
     *             if any other exception occurs.
     */
    private void onStore(ServerContext context, JsonValue value) throws ResourceException  {
        for (ManagedObjectProperty property : properties) {
            property.onValidate(context, value);
        }
        execScript(context, onValidate, value, null);
        // TODO: schema validation here (w. optimizations)
        for (ManagedObjectProperty property : properties) {
            property.onStore(context, value); // includes per-property encryption
        }
        execScript(context, onStore, value, null);
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
    private Resource decrypt(final Resource value) throws InternalServerErrorException {
        try {
            // makes a copy, which we can modify
            return new Resource(value.getId(), value.getRevision(),
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
     * Update a resource as part of an update or patch request.
     *
     * @param context the current ServerContext
     * @param request the source Request
     * @param resourceId the resource id of the object being modified
     * @param rev the revision of hte object being modified
     * @param oldValue the old value of the object
     * @param newValue the new value of the object
     * @return the updated resource
     * @throws ResourceException
     */
    private Resource update(final ServerContext context, Request request, String resourceId, String rev,
            Resource oldValue, JsonValue newValue)
            throws ResourceException {

        if (newValue.asMap().equals(oldValue.getContent().asMap())) { // object hasn't changed
            return new Resource(resourceId, rev, null);
        }
        // Execute the onUpdate script if configured
        execScript(context, onUpdate, newValue,
                prepareScriptBindings(context, request, resourceId, oldValue.getContent(), newValue));

        // Perform pre-property encryption
        onStore(context, newValue); // performs per-property encryption

        // Populate the virtual properties (so they are updated for sync-ing)
        populateVirtualProperties(context, newValue);
        
        // Perform update
        UpdateRequest updateRequest = Requests.newUpdateRequest(repoId(resourceId), newValue);
        updateRequest.setRevision(rev);
        Resource response = connectionFactory.getConnection().update(context, updateRequest);

        // Execute the postUpdate script if configured
        execScript(context, postUpdate, response.getContent(),
                prepareScriptBindings(context, request, resourceId, oldValue.getContent(), response.getContent()));

        performSyncAction(context, request, resourceId, SynchronizationService.SyncServiceAction.notifyUpdate,
                oldValue.getContent(), response.getContent());

        return response;
    }

    /**
     * Applies a patch document to an object, or by finding an object in the
     * object set itself via query parameters. As this is an action, the patch
     * document to be applied is in the {@code _entity} parameter.
     *
     * @param context the current ServerContext
     * @param request the ActionRequest
     * @param handler the ResultHandler
     * @throws ResourceException
     */
    private void patchAction(final ServerContext context, final ActionRequest request, final ResultHandler<JsonValue> handler)
            throws ResourceException {

        if (!request.getContent().required().isList()) {
            throw new BadRequestException(
                    "The request could not be processed because the provided content is not a JSON array");
        }

        // Build query request from action parameters looking for query parameters
        // use JsonValue to coerce Map<String, String> to Map<String, Object> - blech
        QueryRequest queryRequest = RequestUtil.buildQueryRequestFromParameterMap(repoId(null),
                new JsonValue(request.getAdditionalParameters()).asMap());

        final List<PatchOperation> operations = PatchOperation.valueOfList(request.getContent());

        connectionFactory.getConnection().query(context, queryRequest,
                new QueryResultHandler() {
                    final List<Resource> resources = new ArrayList<Resource>();
            
                    @Override
                    public void handleError(final ResourceException error) {
                        handler.handleError(error);
                    }

                    @Override
                    public boolean handleResource(Resource resource) {
                        logger.debug("Patch by query found resource " + resource.getId());
                        resources.add(resource);
                        return true;
                    }

                    @Override
                    public void handleResult(QueryResult result) {
                        if (resources.size() > 1) {
                            handler.handleError(new InternalServerErrorException("Query result must yield one matching object"));
                        } else if (resources.size() == 1) {
                            try {
                                Resource resource = resources.get(0);

                                Resource response = patchResource(context, request, resource, null, operations);

                                handler.handleResult(response.getContent());
                            } catch (ResourceException e) {
                                handler.handleError(e);
                            } catch (Exception e) {
                                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
                            }
                        } else {
                            handler.handleError(new NotFoundException("Query returned no results"));
                        }
                        
                    }
                });
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        String resourceId = request.getNewResourceId();
        JsonValue content = request.getContent();

        // Check if the new id is specified in content, and use it if it is
        if (!content.get(Resource.FIELD_CONTENT_ID).isNull()) {
            resourceId = content.get(Resource.FIELD_CONTENT_ID).asString();
        }
        logger.debug("Create name={} id={}", name, resourceId);

        try {
            // decrypt any incoming encrypted properties
            JsonValue value = decrypt(content);
            execScript(context, onCreate, value, null);
            
            // Populate the virtual properties (so they are available for sync-ing)
            populateVirtualProperties(context, value);
            
            // includes per-property encryption
            onStore(context, value);

            CreateRequest createRequest = Requests.copyOfCreateRequest(request)
                    .setNewResourceId(resourceId)
                    .setResourceName(repoId(null))
                    .setContent(value);

            Resource _new = connectionFactory.getConnection().create(context, createRequest);

            activityLogger.log(context, request, "create", managedId(_new.getId()).toString(),
                    null, _new.getContent(), Status.SUCCESS);

            // Execute the postCreate script if configured
            execScript(context, postCreate, _new.getContent(),
                    prepareScriptBindings(context, request, resourceId, new JsonValue(null), _new.getContent()));

            // Sync any targets after managed object is created
            performSyncAction(context, request, _new.getId(), SynchronizationService.SyncServiceAction.notifyCreate,
                    new JsonValue(null), _new.getContent());

            if (ContextUtil.isExternal(context)) {
                _new = cullPrivateProperties(_new);
            }

            // TODO Check the relative id
            handler.handleResult(_new);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    @Override
    public void readInstance(final ServerContext context, String resourceId, ReadRequest request,
            ResultHandler<Resource> handler) {
        logger.debug("Read name={} id={}", name, resourceId);
        try {

            ReadRequest readRequest = Requests.newReadRequest(repoId(resourceId));
            Resource resource = connectionFactory.getConnection().read(context, readRequest);

            onRetrieve(context, request, resourceId, resource);
            execScript(context, onRead, resource.getContent(), null);
            activityLogger.log(context, request, "read", managedId(resource.getId()).toString(),
                    null, resource.getContent(), Status.SUCCESS);

            handler.handleResult(ContextUtil.isExternal(context) ? cullPrivateProperties(resource) : resource);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    @Override
    public void updateInstance(final ServerContext context, final String resourceId, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        logger.debug("update {} ", "name=" + name + " id=" + resourceId + " rev="
                + request.getRevision());

        try {
            // decrypt any incoming encrypted properties
            JsonValue _new = decrypt(request.getContent());

            ReadRequest readRequest = Requests.newReadRequest(repoId(resourceId));
            for (JsonPointer pointer : request.getFields()) {
                readRequest.addField(pointer);
            }
            Resource resource = connectionFactory.getConnection().read(context, readRequest);
            Resource _old = decrypt(resource);

            Resource updated = update(context, request, resourceId, request.getRevision(), _old, _new);
            activityLogger.log(context, request, "update",
                    managedId(resource.getId()).toString(), resource.getContent(), updated.getContent(),
                    Status.SUCCESS);

            if (ContextUtil.isExternal(context)) {
                updated = cullPrivateProperties(updated);
            }

            handler.handleResult(updated);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    @Override
    public void deleteInstance(final ServerContext context, final String resourceId, DeleteRequest request,
            ResultHandler<Resource> handler) {
        logger.debug("Delete {} ", "name=" + name + " id=" + resourceId + " rev="
                + request.getRevision());
        try {
            ReadRequest readRequest = Requests.newReadRequest(repoId(resourceId));

            Resource resource = connectionFactory.getConnection().read(context, readRequest);
            execScript(context, onDelete, decrypt(resource.getContent()), null);

            DeleteRequest deleteRequest = Requests.copyOfDeleteRequest(request);
            deleteRequest.setResourceName(repoId(resourceId));
            if (deleteRequest.getRevision() == null) {
                deleteRequest.setRevision(resource.getRevision());
            }
            Resource deletedResource = connectionFactory.getConnection().delete(context, deleteRequest);

            activityLogger.log(context, request, "delete", managedId(resource.getId()).toString(),
                    resource.getContent(), null, Status.SUCCESS);

            // Execute the postDelete script if configured
            execScript(context, postDelete, null,
                    prepareScriptBindings(context, request, resourceId, deletedResource.getContent(), new JsonValue(null)));

            // Perform notifyDelete synchronization
            performSyncAction(context, request, resourceId, SynchronizationService.SyncServiceAction.notifyDelete,
                    resource.getContent(), new JsonValue(null));

            // only cull private properties if this is an external call
            if (ContextUtil.isExternal(context)) {
                deletedResource = cullPrivateProperties(deletedResource);
            }

            handler.handleResult(deletedResource);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
            ResultHandler<Resource> handler) {
        try {
            Resource patched = patchResourceById(context, request, resourceId, request.getRevision(), request.getPatchOperations());

            handler.handleResult(patched);
        } catch (ResourceException e) {
            handler.handleError(e);
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
     * @return The patched Resource with private properties omitted if called externally.
     *
     * @throws ResourceException
     */
    private Resource patchResourceById(ServerContext context, Request request,
                                       String resourceId, String revision, List<PatchOperation> patchOperations)
            throws ResourceException {
        idRequired(request.getResourceName());
        noSubObjects(request.getResourceName());

        // Get the oldest value for diffing in the log
        // JsonValue oldValue = new JsonValue(cryptoService.getRouter().read(repoId(id)));
        ReadRequest readRequest = Requests.newReadRequest(repoId(resourceId));
        Resource resource = connectionFactory.getConnection().read(context, readRequest);

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
     * @return The patched Resource with private properties omitted if called externally.
     *
     * @throws ResourceException
     */
    private Resource patchResource(ServerContext context, Request request,
                                   Resource resource, String revision, List<PatchOperation> patchOperations)
        throws ResourceException {

        // FIXME: There's no way to decrypt a patch document. :-( Luckily, it'll work for now with patch action.

        boolean forceUpdate = (revision == null);
        boolean retry = forceUpdate;
        String _rev = revision;

        do {
            logger.debug("patch name={} id={}", name, request.getResourceName());
            try {

                JsonValue decrypted = decrypt(resource.getContent()); // decrypt any incoming encrypted properties

                // If we haven't defined a rev, we need to get the current rev
                if (revision == null) {
                    _rev = decrypted.get("_rev").asString();
                }

                JsonValue newValue = decrypted.copy();
                boolean modified = JsonValuePatch.apply(newValue, patchOperations);
                if (!modified) {
                    return null;
                }

                if (enforcePolicies) {
                    ActionRequest policyAction = Requests.newActionRequest(
                            ResourceName.valueOf("policy").concat(managedId(resource.getId())).toString(), "validateObject");
                    policyAction.setContent(newValue);
                    if (ContextUtil.isExternal(context)) {
                        // this parameter is used in conjunction with the test in policy.js
                        // to ensure that the reauth policy is enforced
                        policyAction.setAdditionalParameter("external", "true");
                    }

                    JsonValue result = connectionFactory.getConnection().action(context, policyAction);
                    if (!result.isNull() && !result.get("result").asBoolean()) {
                        logger.debug("Requested patch failed policy validation: {}", result);
                        throw new ForbiddenException("Failed policy validation").setDetail(result);
                    }
                }

                Resource patchedResource = update(context, request, resource.getId(), _rev, resource, newValue);

                activityLogger.log(context, request, "Patch " + patchOperations.toString(),
                        managedId(patchedResource.getId()).toString(), resource.getContent(), patchedResource.getContent(),
                        Status.SUCCESS);
                retry = false;
                logger.debug("Patch successful!");

                if (ContextUtil.isExternal(context)) {
                    patchedResource = cullPrivateProperties(patchedResource);
                }
                return patchedResource;
            } catch (PreconditionFailedException e) {
                if (forceUpdate) {
                    logger.debug("Unable to update due to revision conflict. Retrying.");
                } else {
                    // If it fails and we're not trying to force an update, we gave it our best shot
                    throw e;
                }
            } catch (ResourceException e) {
                throw e;
            }
        } while (retry);
        return null;
    }

    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        logger.debug("query name={} id={}", name, request.getResourceName());

        QueryRequest repoRequest = Requests.copyOfQueryRequest(request);
        repoRequest.setResourceName(repoId(null));
        
        // The "executeOnRetrieve" parameter is used to indicate if is returning a full managed object
        String executeOnRetrieve = request.getAdditionalParameter("executeOnRetrieve");
        
        // The onRetrieve script should only be run queries that return full managed objects
        final boolean onRetrieve = executeOnRetrieve == null
                ? false
                : Boolean.parseBoolean(executeOnRetrieve);

        final List<Map<String,Object>> results = new ArrayList<Map<String,Object>>();
        try {
            connectionFactory.getConnection().query(context, repoRequest, new QueryResultHandler() {
                @Override
                public void handleError(ResourceException error) {
                    handler.handleError(error);
                }

                @Override
                public boolean handleResource(Resource resource) {
                    // Check if the onRetrieve script should be run
                    if (onRetrieve) {
                        try {
                            onRetrieve(context, request, resource.getId(), resource);
                        } catch (ResourceException e) {
                            handler.handleError(e);
                            return false;
                        }
                    }
                    results.add(resource.getContent().asMap());
                    if (ContextUtil.isExternal(context)) {
                        // If it came over a public interface we have to cull each resulting object
                        return handler.handleResource(cullPrivateProperties(resource));
                    }
                    return handler.handleResource(resource);
                }

                @Override
                public void handleResult(QueryResult result) {
                    handler.handleResult(result);
                }
            });

            activityLogger.log(context, request,
                    "query: " + request.getQueryId() + ", parameters: " + request.getAdditionalParameters(),
                    request.getQueryId(), null, new JsonValue(results), Status.SUCCESS);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {

        try {
            activityLogger.log(context, request, "Action: " + request.getAction(),
                    managedId(resourceId).toString(), null, null, Status.SUCCESS);
            switch (request.getActionAsEnum(Action.class)) {
                case patch:
                    final List<PatchOperation> operations = PatchOperation.valueOfList(request.getContent());
                    Resource resource = patchResourceById(context, request, resourceId, null, operations);
                    handler.handleResult(resource.getContent());
                    break;
                case triggerSyncCheck:
                    // Sync changes if required, in particular virtual/calculated attribute changes
                    final ReadRequest readRequest = Requests.newReadRequest(managedId(resourceId).toString());
                    logger.debug("Attempt sync of {}", readRequest.getResourceName());
                    Resource currentResource = connectionFactory.getConnection().read(context, readRequest);
                    UpdateRequest updateRequest = Requests.newUpdateRequest(readRequest.getResourceName(), currentResource.getContent());
                    final ResultHandler<JsonValue> resultHandler = handler;
                    updateInstance(context, resourceId, updateRequest, new ResultHandler<Resource>() {
                        @Override
                        public void handleError(ResourceException error) {
                            resultHandler.handleError(error);
                        }

                        @Override
                        public void handleResult(Resource result) {
                            logger.debug("Sync of {} complete", readRequest.getResourceName());
                            resultHandler.handleResult(result.getContent());
                        }
                    });
                    break;
                default:
                    throw new BadRequestException("Action " + request.getAction() + " is not supported.");
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (IllegalArgumentException e) { // from getActionAsEnum
            handler.handleError(new BadRequestException(e.getMessage(), e));
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
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        logger.debug("action name={} id={}", name, request.getResourceName());

        try {
            activityLogger.log(context, request, "Action: " + request.getAction(),
                    request.getResourceName(), null, null, Status.SUCCESS);
            switch (request.getActionAsEnum(Action.class)) {
                case patch:
                    patchAction(context, request, handler);
                    break;
                default:
                    throw new BadRequestException("Action " + request.getAction() + " is not supported.");
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (IllegalArgumentException e) { // from getActionAsEnum
            handler.handleError(new BadRequestException(e.getMessage(), e));
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
     * Culls properties that are marked private
     *
     * @param resource Resource to cull private properties from
     * @return the supplied Resource with private properties culled
     */
    private Resource cullPrivateProperties(Resource resource) {
        for (ManagedObjectProperty property : properties) {
            if (property.isPrivate()) {
                resource.getContent().remove(property.getName());
            }
        }
        return resource;
    }
    
    /**
     * Sends a sync action request to the synchronization service
     *
     * @param context the ServerContext of the request
     * @param request the Request being processed
     * @param resourceId the additional resourceId parameter telling the synchronization service which object
     *                   is being synchronized
     * @param action the {@link org.forgerock.openidm.sync.impl.SynchronizationService.SyncServiceAction}
     * @param oldValue the previous object value before the change (if applicable, or null if not)
     * @param newValue the object value to sync
     * @throws ResourceException in case of a failure that was not handled by the ResultHandler
     */
    private void performSyncAction(final ServerContext context, final Request request, final String resourceId,
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

        try {
            JsonValue content = new JsonValue(new LinkedHashMap<String, Object>(2));
            content.put("oldValue", oldValue.getObject());
            content.put("newValue", newValue.getObject());
            final ActionRequest syncRequest = Requests.newActionRequest("sync", action.name())
                    .setAdditionalParameter(ACTION_PARAM_RESOURCE_CONTAINER, managedObjectPath.toString())
                    .setAdditionalParameter(ACTION_PARAM_RESOURCE_ID, resourceId)
                    .setContent(content);

            final ResourceException[] syncScriptError = new ResourceException[] { null };
            connectionFactory.getConnection().actionAsync(context, syncRequest, new ResultHandler<JsonValue>() {
                @Override
                public void handleError(final ResourceException e) {
                    logger.warn("Failed to sync {} {}:{}", syncRequest.getAction(), name, syncRequest.getResourceName(), e);
                    execSyncScript(false, e.getDetail());
                }

                @Override
                public void handleResult(final JsonValue result) {
                    logger.debug("Successfully completed sync {} {}:{}", syncRequest.getAction(), name, syncRequest.getResourceName());
                    execSyncScript(true, result);
                }

                private void execSyncScript(boolean success, JsonValue syncDetails) {
                    try {
                        JsonValue scriptBindings = prepareScriptBindings(context, request, resourceId, oldValue, newValue);
                        Map<String,Object> syncResults = new HashMap<String,Object>();
                        syncResults.put("success", success);
                        syncResults.put("action", action.name());
                        syncResults.put("syncDetails", syncDetails.getObject());
                        scriptBindings.put("syncResults", syncResults);
                        execScript(context, onSync, null, scriptBindings);
                    } catch (ResourceException re) {
                        logger.warn("Failed executing onSync script on {} {}:{}",
                                syncRequest.getAction(), name, syncRequest.getResourceName(), re);
                        // this needs to passed to the caller's handleError so the client hears about it
                        syncScriptError[0] = re;
                    }
                }
            });
            if (syncScriptError[0] != null) {
                throw syncScriptError[0];
            }
        } catch (NotFoundException e) {
            logger.error("Failed to sync {} {}:{}", action.name(), name, resourceId, e);
            throw e;
        }
    }
}
