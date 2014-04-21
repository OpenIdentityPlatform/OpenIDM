/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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
import org.forgerock.json.resource.ConflictException;
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
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
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
import org.forgerock.openidm.util.ContextUtil;
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

/**
 * Provides access to a set of managed objects of a given type: managed/[type]/{id}.
 *
 * @author Paul C. Bryan
 * @author aegloff
 * @author brmiller
 */
class ManagedObjectSet implements CollectionResourceProvider, ScriptListener {

    /** Actions supported by this resource provider */
    enum Action {
        patch
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
        onStore
    }

    /**
     * Setup logging for the {@link ManagedObjectSet}.
     */
    private final static Logger logger = LoggerFactory.getLogger(ManagedObjectSet.class);

    /** The managed objects service that instantiated this managed object set. */
    private final CryptoService cryptoService;

    private final ConnectionFactory connectionFactory;

    /** Audit Activity Log helper */
    private final ActivityLogger activityLogger;

    /** Name of the managed object type. */
    private final String name;

    /**
     * The schema to use to validate the structure and content of the managed
     * object.
     */
    private final JsonValue schema;

    /** Map of scripts to execute on specific {@link ScriptHook}s. */
    private final Map<ScriptHook, ScriptEntry> scriptHooks = new EnumMap<ScriptHook, ScriptEntry>(ScriptHook.class);

    /** Properties for which triggers are executed during object set operations. */
    private final ArrayList<ManagedObjectProperty> properties = new ArrayList<ManagedObjectProperty>();

    final AtomicReference<RouteService> syncRoute;

    /** Flag for indicating if policy enforcement is enabled */
    private boolean enforcePolicies;

    /**
     * Constructs a new managed object set.
     *
     *
     *
     * @param scriptRegistry
     * @param cryptoService
     *            the cryptographic service
     * @param syncRoute
     * @param connectionFactory
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
     * @param id
     *            the local managed object identifier to qualify.
     * @return the fully-qualified managed object identifier.
     */
    // TODO: consider moving this logic somewhere else
    private String managedId(String id) {
        StringBuilder sb = new StringBuilder(getManagedObjectPath());
        if (id != null) {
            sb.append('/').append(id);
        }
        return sb.toString();
    }

    /**
     * Generates the managed object path
     * @return the managed object path
     */
    private String getManagedObjectPath() {
        return new StringBuilder("managed/").append(name).toString();
    }

    /**
     * Generates a fully-qualified object identifier for the repository.
     *
     * @param id
     *            the local managed object identifier to qualify.
     * @return the fully-qualified repository object identifier.
     */
    private String repoId(String id) {
        return "repo/" + managedId(id);
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
     * Executes all of the necessary trigger scripts when an object is retrieved
     * from the repository.
     *
     * @param value
     *            the JSON value that was retrieved from the repository.
     * @throws ForbiddenException
     *             if a validation trigger throws an exception.
     * @throws InternalServerErrorException
     *             if any other exception occurs.
     */
    private void onRetrieve(ServerContext context, Resource value) throws ResourceException {
        execScript(context, onRetrieve, value.getContent(), null);
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
            property.onStore(context, value); // includes per-property
                                              // encryption
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

    private Resource update(final ServerContext context, String id, String rev, Resource oldValue, JsonValue newValue)
            throws ResourceException {
        if (newValue.equals(oldValue.getContent())) { // object hasn't changed
            return new Resource(id, rev, null);
        }
        // Execute the onUpdate script if configured
        JsonValue additionalProps = new JsonValue(new HashMap<String, Object>());
        additionalProps.put("oldObject", oldValue.getContent());
        additionalProps.put("newObject", newValue);
        execScript(context, onUpdate, newValue, additionalProps);
        
        // Perform pre-property encryption
        onStore(context, newValue); // performs per-property encryption

        // Perform update
        UpdateRequest request = Requests.newUpdateRequest(repoId(id), newValue);
        request.setRevision(rev);
        Resource response = connectionFactory.getConnection().update(context, request);

        // Populate the virtual properties
        populateVirtualProperties(context, newValue);

        // Execute the postUpdate script if configured
        executePostScript(context, postUpdate, id, oldValue.getContent(), newValue);
        
        // Perform any onUpdate synchronization
        // TODO: Pass the oldValue
        performSyncAction(context, getManagedObjectPath(), id, "ONUPDATE", newValue);

        return response;
    }
    
    /**
     * Post scripts are executed after the managed object has been updated, but before any synchronization.
     * 
     * @param context the ServerContext of the request
     * @param hook the ScriptHook to execute
     * @param id the id of the managed ob object
     * @param oldObject the old value of the managed object (null for create requests)
     * @param newObject the new value of the managed object (null for delete requests)
     * @throws ResourceException
     */
    private void executePostScript(final ServerContext context, ScriptHook hook,  String id,
            JsonValue oldObject, JsonValue newObject) throws ResourceException {
        JsonValue additionalProps = new JsonValue(new HashMap<String, Object>());
        additionalProps.put("resourceName", managedId(id));
        additionalProps.put("oldObject", oldObject);
        additionalProps.put("newObject", newObject);
        execScript(context, hook, newObject, additionalProps);
    }

    /**
     * Applies a patch document to an object, or by finding an object in the
     * object set itself via query parameters. As this is an action, the patch
     * document to be applied is in the {@code _entity} parameter.
     *
     * @param context
     * @param request
     * @return
     * @throws ResourceException
     */
    private JsonValue patchAction(final ServerContext context, final ActionRequest request)
            throws ResourceException {

        if (!request.getContent().required().isList()) {
            throw new BadRequestException(
                    "The request could nto be processed because the provided content is not a JSON array");
        }

        QueryRequest queryRequest = Requests.newQueryRequest(repoId(null));
        queryRequest.setQueryId(request.getAdditionalParameters().get("_queryId"));
        for (Map.Entry<String,String> entry : request.getAdditionalParameters().entrySet()) {
            queryRequest.setAdditionalParameter(entry.getKey(), entry.getValue());
        }

        final JsonValue[] lastError = new JsonValue[1];

        /*
        final JsonValue patch = request.getContent().required().expect(List.class);
        */
        final List<PatchOperation> operations = PatchOperation.valueOfList(request.getContent());

        connectionFactory.getConnection().query(context, queryRequest,
                new QueryResultHandler() {
                    @Override
                    public void handleError(final ResourceException error) {
                        lastError[0] = error.toJsonValue();
                    }

                    @Override
                    public boolean handleResource(Resource resource) {
                        // TODO This should not fail on first error and the response
                        // should contains each result

                        try {
                            Resource decrypted = decrypt(resource);
                            JsonValue newValue = decrypted.getContent().copy();
                            JsonValuePatch.apply(newValue, operations);

                            Resource updated = update(context, resource.getId(), resource.getRevision(), decrypted, newValue);
                            activityLogger.log(context, request.getRequestType(), "Patch " + operations.toString(), managedId(resource.getId()),
                                    resource.getContent(), updated.getContent(), Status.SUCCESS);
                        } catch (ResourceException e) {
                            lastError[0] = new ConflictException(e.getMessage(), e).toJsonValue();
                        }

                        return null != lastError[0];
                    }

                    @Override
                    public void handleResult(QueryResult result) {
                        // do we care?
                    }
                });

        // JsonValue results = new
        // JsonValue(cryptoService.getRouter().query(repoId(null),
        // params.asMap()), new
        // JsonPointer("results")).get(QueryConstants.QUERY_RESULT);
        // if (!results.isList()) {
        // throw new
        // InternalServerErrorException("Expecting list result from query");
        // } else if (results.size() == 0) {
        // throw new NotFoundException();
        // } else if (results.size() > 1) {
        // throw new ConflictException("Query yielded more than one result");
        // }
        // JsonValue result = results.get(0);
        // _id = result.get("_id").required().asString();
        // if (_rev == null) { // don't override an explicitly supplied revision
        // _rev = result.get("_rev").asString();
        // }
        // } catch (JsonValueException jve) {
        // throw new InternalServerErrorException(jve);
        // }
        // }
        // patch(_id, _rev, new
        // JsonPatchWrapper(decrypt(params.get("_entity"))));
        // empty response (and lack of exception) indicates success
        return new JsonValue(null);
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler) {
        String id = request.getNewResourceId();
        JsonValue content = request.getContent();
        
        // Check if the new id is specified in content, and use it if it is
        if (!content.get(Resource.FIELD_CONTENT_ID).isNull()) {
            id = content.get(Resource.FIELD_CONTENT_ID).asString();
        }
        logger.debug("Create name={} id={}", name, id);

        try {
            // decrypt any incoming encrypted properties
            JsonValue value = decrypt(request.getContent());
            execScript(context, onCreate, value, null);
            // includes per-property encryption
            onStore(context, value);

            CreateRequest createRequest = Requests.copyOfCreateRequest(request);
            createRequest.setNewResourceId(id);
            createRequest.setContent(value);
            createRequest.setResourceName(repoId(null));

            Resource _new = connectionFactory.getConnection().create(context, createRequest);

            activityLogger.log(context, request.getRequestType(), "create", managedId(_new.getId()),
                    null, _new.getContent(), Status.SUCCESS);

            populateVirtualProperties(context, _new.getContent());
            
            // Execute the postCreate script if configured
            executePostScript(context, postCreate, id, new JsonValue(null), _new.getContent());

            // Sync any targets after managed object is created
            performSyncAction(context, getManagedObjectPath(), _new.getId(), "ONCREATE", _new.getContent());

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

            onRetrieve(context, resource);
            execScript(context, onRead, resource.getContent(), null);
            activityLogger.log(context, request.getRequestType(), "read", managedId(resource.getId()),
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
            for (JsonPointer pointer: request.getFields()) {
                readRequest.addField(pointer);
            }
            Resource resource = connectionFactory.getConnection().read(context, readRequest);
            Resource _old = decrypt(resource);

            handler.handleResult(update(context, resourceId, request.getRevision(), _old, _new));

            activityLogger.log(context, request.getRequestType(), "update", managedId(_old.getId()),
                    _old.getContent(), _new, Status.SUCCESS);

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

            activityLogger.log(context, request.getRequestType(), "delete", managedId(resource.getId()),
                    resource.getContent(), null, Status.SUCCESS);
            
            // Execute the postDelete script if configured
            executePostScript(context, postDelete, resourceId, deletedResource.getContent(), null);

            // Perform any onDelete synchronization
            performSyncAction(context, getManagedObjectPath(), resourceId, "ONDELETE", resource.getContent());

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
            Resource resource = patchResource(context, request.getRequestType(), request.getResourceName(),
                    resourceId, request.getRevision(), request.getPatchOperations());
            handler.handleResult(resource);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    private Resource patchResource(ServerContext context, RequestType requestType, String resourceName,
            String resourceId, String revision, List<PatchOperation> patchOperations)
        throws ResourceException {

        // FIXME: There's no way to decrypt a patch document. :-( Luckily, it'll work for now with patch action.

        boolean forceUpdate = (revision == null);
        boolean retry = forceUpdate;
        String _rev = revision;

        do {
            logger.debug("patch name={} id={}", name, resourceName);
            try {
                idRequired(resourceName);
                noSubObjects(resourceName);

                // Get the oldest value for diffing in the log
                // JsonValue oldValue = new JsonValue(cryptoService.getRouter().read(repoId(id)));
                ReadRequest readRequest = Requests.newReadRequest(repoId(resourceId));
                Resource oldValue = connectionFactory.getConnection().read(context, readRequest);

                JsonValue decrypted = decrypt(oldValue.getContent()); // decrypt any incoming encrypted properties

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
                            "policy/" + managedId(resourceName + resourceId), "validateObject");
                    policyAction.setContent(newValue);
                    if (ContextUtil.isExternal(context)) {
                        // this parameter is used in conjunction with the test in policy.js
                        // to ensure that the reauth policy is enforced
                        policyAction.setAdditionalParameter("external", "true");
                    }

                    // JsonValue result = new JsonValue(cryptoService.getRouter().action("policy/"+ managedId(id), params.asMap()));
                    JsonValue result = connectionFactory.getConnection().action(context, policyAction);
                    if (!result.isNull() && !result.get("result").asBoolean()) {
                        logger.debug("Requested patch failed policy validation: {}", result);
                        throw new ForbiddenException("Failed policy validation").setDetail(result);
                    }
                }

                Resource resource = update(context, resourceId, _rev, oldValue, newValue);
                retry = false;
                logger.debug("Patch successful!");
                activityLogger.log(context, requestType, "Patch " + patchOperations.toString(), managedId(resourceId),
                        oldValue.getContent(), newValue, Status.SUCCESS);
                return resource;
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
                            onRetrieve(context, resource);
                        } catch (ResourceException e) {
                            handler.handleError(e);
                            return false;
                        }
                    }
                    results.add(resource.getContent().asMap());
                    if (ContextUtil.isExternal(context)) {
                        // If it came over a public interface we have to cull
                        // each resulting object
                        return handler.handleResource(cullPrivateProperties(resource));
                    }
                    return handler.handleResource(resource);
                }

                @Override
                public void handleResult(QueryResult result) {
                    handler.handleResult(result);
                }
            });

            activityLogger.log(context, request.getRequestType(),
                    "query: " + request.getQueryId() + ", parameters: " + request.getAdditionalParameters(),
                    request.getQueryId(), null, new JsonValue(results), Status.SUCCESS);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        // final ResourceException e = new NotSupportedException("Actions are not supported for resource instances");
        // handler.handleError(e);

        try {
            activityLogger.log(context, request.getRequestType(), "Action: " + request.getAction(), managedId(resourceId),
                    null, null, Status.SUCCESS);
            switch (request.getActionAsEnum(Action.class)) {
                case patch:
                    final List<PatchOperation> operations = PatchOperation.valueOfList(request.getContent());
                    Resource resource = patchResource(context, request.getRequestType(), request.getResourceName(), resourceId, null, operations);
                    handler.handleResult(resource.getContent());
                    break;
                default:
                    throw new BadRequestException("Action " + request.getAction() + " is not supported.");
            }
        } catch (IllegalArgumentException e) {
            handler.handleError(new BadRequestException("Action:" + request.getAction()
                    + " is not supported for resource collection", e));
        } catch (ResourceException e) {
            handler.handleError(e);
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
            activityLogger.log(context, request.getRequestType(), "Action: " + request.getAction(), request.getResourceName(),
                    null, null, Status.SUCCESS);
            switch (request.getActionAsEnum(Action.class)) {
                case patch:
                    handler.handleResult(patchAction(context, request));
                    break;
                default:
                    throw new BadRequestException("Action " + request.getAction() + " is not supported.");
            }
        } catch (IllegalArgumentException e) {
            handler.handleError(new BadRequestException("Action:" + request.getAction()
                    + " is not supported for resource collection", e));
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    // -------- Implements the ScriptListener

    @Override
    public void scriptChanged(ScriptEvent event) {
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
     * @param jv
     *            JsonValue to cull private properties from
     * @return the supplied JsonValue with private properties culled
     */
    private Resource cullPrivateProperties(Resource jv) {
        // TODO Should this return a copy of the Resource?
        for (ManagedObjectProperty property : properties) {
            if (property.isPrivate()) {
                jv.getContent().remove(property.getName());
            }
        }
        return jv;
    }

    /**
     * Sends a sync action request to the synchronization service
     * 
     * @param context the ServerContext of the request
     * @param resourceContainer the additional resourceContainer parameter telling the synchronization service which
     *                          object type is being synchronized
     * @param resourceId the additional resourceId parameter telling the synchronization service which object
     *                   is being synchronized
     * @param actionId the actionId for the SynchronizationService
     * @param value the object value to sync
     * @throws ResourceException in case of a failure that was not handled by the ResultHandler
     */
    private void performSyncAction(ServerContext context, String resourceContainer, String resourceId, String actionId, JsonValue value)
            throws ResourceException {
        final RouteService sync = syncRoute.get();
        if (null != sync) {
            final ActionRequest request = Requests.newActionRequest("sync", actionId)
                    .setAdditionalParameter("resourceContainer", resourceContainer)
                    .setAdditionalParameter("resourceId", resourceId)
                    .setContent(value);
            try {
                connectionFactory.getConnection().actionAsync(context, request, new ResultHandler<JsonValue>() {
                    @Override
                    public void handleError(final ResourceException e) {
                        logger.error("Failed to sync {} {}:{}", request.getAction(), name, request.getResourceName(), e);
                    }

                    @Override
                    public void handleResult(final JsonValue result) {
                        logger.debug("Successfully completed sync {} {}:{}", request.getAction(), name, request.getResourceName());
                    }
                });
            } catch (NotFoundException e) {
                logger.error("Failed to sync {} {}:{}", request.getAction(), name, request.getResourceName(), e);
            }
        } else {
            logger.warn("Sync service was not available.");
        }
    }
}
