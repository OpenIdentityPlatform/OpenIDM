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
import org.forgerock.openidm.audit.util.ActivityLog;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.patch.JsonValuePatch;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.openidm.sync.impl.SynchronizationService;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptEvent;
import org.forgerock.script.ScriptListener;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.exception.ScriptThrownException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to a set of managed objects of a given type.
 *
 * @author Paul C. Bryan
 * @author aegloff
 * @author brmiller
 */
// TODO Consider to use filter instead of Collection Provider
class ManagedObjectSet implements CollectionResourceProvider, ScriptListener {

    /** TODO: Description. */
    private enum Action {
        patch
    }

    /**
     * Setup logging for the {@link ManagedObjectSet}.
     */
    private final static Logger logger = LoggerFactory.getLogger(ManagedObjectSet.class);

    /** The managed objects service that instantiated this managed object set. */
    private final CryptoService cryptoService;

    private final ConnectionFactory connectionFactory;

    /** Name of the managed object type. */
    private final String name;

    /**
     * The schema to use to validate the structure and content of the managed
     * object.
     */
    private final JsonValue schema;

    /** Script to execute when the creation of an object is being requested. */
    private final ScriptEntry onCreate;

    /** Script to execute when the read of an object is being requested. */
    private final ScriptEntry onRead;

    /** Script to execute when the update of an object is being requested. */
    private final ScriptEntry onUpdate;

    /** Script to execute when the deletion of an object is being requested. */
    private final ScriptEntry onDelete;
    
    /** Script to execute after the create of an object has completed. */
    private final ScriptEntry postCreate;
    
    /** Script to execute after the update of an object has completed. */
    private final ScriptEntry postUpdate;
    
    /** Script to execute after the delete of an object has completed. */
    private final ScriptEntry postDelete;

    /** Script to execute when a managed object requires validation. */
    private final ScriptEntry onValidate;

    /** Script to execute once an object is retrieved from the repository. */
    private final ScriptEntry onRetrieve;

    /** Script to execute when an object is about to be stored in the repository. */
    private final ScriptEntry onStore;

    /** Properties for which triggers are executed during object set operations. */
    private final ArrayList<ManagedObjectProperty> properties =
            new ArrayList<ManagedObjectProperty>();

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
        name = config.get("name").required().asString();
        if (name.trim().isEmpty() || name.indexOf('{') > 0 | name.indexOf('}') > 0) {
            throw new JsonValueException(config.get("name"), "Failed to validate the name");
        }
        // TODO: parse into json-schema object
        schema = config.get("schema").expect(Map.class);
        if (config.isDefined("onCreate")) {
            onCreate = scriptRegistry.takeScript(config.get("onCreate"));
            // onCreate.addScriptListener(this);
        } else {
            onCreate = null;
        }
        if (config.isDefined("onRead")) {
            onRead = scriptRegistry.takeScript(config.get("onRead"));
            // onRead.addScriptListener(this);
        } else {
            onRead = null;
        }
        if (config.isDefined("onUpdate")) {
            onUpdate = scriptRegistry.takeScript(config.get("onUpdate"));
            // onUpdate.addScriptListener(this);
        } else {
            onUpdate = null;
        }
        if (config.isDefined("onDelete")) {
            onDelete = scriptRegistry.takeScript(config.get("onDelete"));
            // onDelete.addScriptListener(this);
        } else {
            onDelete = null;
        }
        if (config.isDefined("postCreate")) {
            postCreate = scriptRegistry.takeScript(config.get("postCreate"));
        } else {
            postCreate = null;
        }
        if (config.isDefined("postUpdate")) {
            postUpdate = scriptRegistry.takeScript(config.get("postUpdate"));
        } else {
            postUpdate = null;
        }
        if (config.isDefined("postDelete")) {
            postDelete = scriptRegistry.takeScript(config.get("postDelete"));
        } else {
            postDelete = null;
        }
        if (config.isDefined("onValidate")) {
            onValidate = scriptRegistry.takeScript(config.get("onValidate"));
            // onValidate.addScriptListener(this);
        } else {
            onValidate = null;
        }
        if (config.isDefined("onRetrieve")) {
            onRetrieve = scriptRegistry.takeScript(config.get("onRetrieve"));
            // onRetrieve.addScriptListener(this);
        } else {
            onRetrieve = null;
        }
        if (config.isDefined("onStore")) {
            onStore = scriptRegistry.takeScript(config.get("onStore"));
            // onStore.addScriptListener(this);
        } else {
            onStore = null;
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
     * Deallocate every resource use by this service.
     */
    void dispose() {
        if (null != onCreate) {
            onCreate.deleteScriptListener(this);
        }
        if (null != onRead) {
            onRead.deleteScriptListener(this);
        }
        if (null != onUpdate) {
            onUpdate.deleteScriptListener(this);
        }
        if (null != onDelete) {
            onDelete.deleteScriptListener(this);
        }
        if (null != postCreate) {
            postCreate.deleteScriptListener(this);
        }
        if (null != postUpdate) {
            postUpdate.deleteScriptListener(this);
        }
        if (null != postDelete) {
            postDelete.deleteScriptListener(this);
        }
        if (null != onValidate) {
            onValidate.deleteScriptListener(this);
        }
        if (null != onRetrieve) {
            onRetrieve.deleteScriptListener(this);
        }
        if (null != onStore) {
            onStore.deleteScriptListener(this);
        }
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
     * @param type
     *            the type of script being executed.
     * @param script
     *            the script to execute, or {@code null} to execute nothing.
     * @param value
     *            the object to be populated in the script scope.
     * @param additionalProps
     *            a Map of additional properties to add the the script scope
     * @throws ForbiddenException
     *             if the script throws an exception.
     * @throws InternalServerErrorException
     *             if any other exception is encountered.
     */
    private void execScript(final ServerContext context, String type, ScriptEntry script,
            JsonValue value, JsonValue additionalProps) throws ResourceException {
        if (null != script && script.isActive()) {
            Script executable = script.getScript(context);
            executable.put("object", value);
            if (additionalProps != null && !additionalProps.isNull()) {
                for (String key : additionalProps.keys()) {
                    executable.put(key, additionalProps.get(key));
                }
            }
            try {
                executable.eval(); // allows direct modification to the object
            } catch (ScriptThrownException ste) {
                // Allow for scripts to set their own exception
                throw ste.toResourceException(ResourceException.INTERNAL_ERROR, 
                        type + " script encountered exception");
            } catch (ScriptException se) {
                String msg = type + " script encountered exception";
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
        execScript(context, "onRetrieve", onRetrieve, value.getContent(), null);
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
        execScript(context, "onValidate", onValidate, value, null);
        // TODO: schema validation here (w. optimizations)
        for (ManagedObjectProperty property : properties) {
            property.onStore(context, value); // includes per-property
                                              // encryption
        }
        execScript(context, "onStore", onStore, value, null);
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
        execScript(context, "onUpdate", onUpdate, newValue, additionalProps);
        
        // Perform pre-property encryption
        onStore(context, newValue); // performs per-property encryption

        // Perform update
        UpdateRequest request = Requests.newUpdateRequest(repoId(id), newValue);
        request.setRevision(rev);
        Resource response = connectionFactory.getConnection().update(context, request);

        // Populate the virtual properties
        populateVirtualProperties(context, newValue);

        // Execute the postUpdate script if configured
        executePostScript(context, "postUpdate", postUpdate, id, oldValue.getContent(), newValue);
        
        // Perform any onUpdate synchronization
        // TODO: Fix the context
        onUpdate(context, getManagedObjectPath(), id, oldValue, newValue);

        return response;
    }
    
    /**
     * Post scripts are executed after the managed object has been updated, but before any synchronization.
     * 
     * @param context the ServerContext of the request
     * @param type the script type
     * @param script the post script to execute
     * @param id the id of the managed ob object
     * @param oldObject the old value of the managed object (null for create requests)
     * @param newObject the new value of the managed object (null for delete requests)
     * @throws ResourceException
     */
    private void executePostScript(final ServerContext context, String type, ScriptEntry script,  String id, 
            JsonValue oldObject, JsonValue newObject) throws ResourceException {
        JsonValue additionalProps = new JsonValue(new HashMap<String, Object>());
        additionalProps.put("resourceName", managedId(id));
        additionalProps.put("oldObject", oldObject);
        additionalProps.put("newObject", newObject);
        execScript(context, type, script, newObject, additionalProps);
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
                            ActivityLog.log(connectionFactory, context, request.getRequestType(), "Patch " + operations.toString(), managedId(resource.getId()),
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
            execScript(context, "onCreate", onCreate, value, null);
            // includes per-property encryption
            onStore(context, value);

            CreateRequest createRequest = Requests.copyOfCreateRequest(request);
            createRequest.setNewResourceId(id);
            createRequest.setContent(value);
            createRequest.setResourceName(repoId(null));

            Resource _new = connectionFactory.getConnection().create(context, createRequest);

            ActivityLog.log(connectionFactory, context, request.getRequestType(), "create", managedId(_new.getId()),
                    null, _new.getContent(), Status.SUCCESS);

            populateVirtualProperties(context, _new.getContent());
            
            // Execute the postCreate script if configured
            executePostScript(context, "postCreate", postCreate, id, new JsonValue(null), _new.getContent());
            
            onCreate(context, getManagedObjectPath(), _new.getId(), _new);

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
            execScript(context, "onRead", onRead, resource.getContent(), null);
            ActivityLog.log(connectionFactory, context, request.getRequestType(), "read", managedId(resource.getId()),
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

            ActivityLog.log(connectionFactory, context, request.getRequestType(), "update", managedId(_old.getId()),
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
            if (onDelete != null) {
                execScript(context, "onDelete", onDelete, decrypt(resource.getContent()), null);
            }
            DeleteRequest deleteRequest = Requests.copyOfDeleteRequest(request);
            deleteRequest.setResourceName(repoId(resourceId));
            if (deleteRequest.getRevision() == null) {
                deleteRequest.setRevision(resource.getRevision());
            }
            Resource deletedResource = connectionFactory.getConnection().delete(context, deleteRequest);

            ActivityLog.log(connectionFactory, context, request.getRequestType(), "delete", managedId(resource.getId()),
                    resource.getContent(), null, Status.SUCCESS);
            
            // Execute the postDelete script if configured
            executePostScript(context, "postDelete", postDelete, resourceId, deletedResource.getContent(), null);

            onDelete(context, getManagedObjectPath(), resourceId, resource);

            handler.handleResult(deletedResource);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    // TODO: Consider dropping this Patch object abstraction and just process a patch document directly?
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
                ActivityLog.log(connectionFactory, context, requestType, "Patch " + patchOperations.toString(), managedId(resourceId),
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

        final List<Map<String,Object>> results = new ArrayList<Map<String,Object>>();
        try {
            connectionFactory.getConnection().query(context, repoRequest, new QueryResultHandler() {
                @Override
                public void handleError(ResourceException error) {
                    handler.handleError(error);
                }

                @Override
                public boolean handleResource(Resource resource) {
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

            ActivityLog.log(connectionFactory, context, request.getRequestType(),
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
            Action action = Action.valueOf(request.getAction());
            ActivityLog.log(connectionFactory, context, request.getRequestType(), "Action: " + request.getAction(), managedId(resourceId),
                    null, null, Status.SUCCESS);
            switch (action) {
                case patch:
                    final List<PatchOperation> operations = PatchOperation.valueOfList(request.getContent());
                    Resource resource = patchResource(context, request.getRequestType(), request.getResourceName(), resourceId, null, operations);
                    handler.handleResult(resource.getContent());
                    break;
                default:
                    throw new BadRequestException("Action" + request.getAction() + " is not supported.");
            }
        } catch (IllegalArgumentException e) {
            handler.handleError(new BadRequestException("Action:" + request.getAction()
                    + " is not supported for resource collection", e));
        } catch (BadRequestException e) {
            handler.handleError(e);
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
            Action action = Action.valueOf(request.getAction());
            ActivityLog.log(connectionFactory, context, request.getRequestType(), "Action: " + request.getAction(), request.getResourceName(),
                    null, null, Status.SUCCESS);
            switch (action) {
                case patch:
                    handler.handleResult(patchAction(context, request));
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
        return name.indexOf('/') == 1 ? name : '/' + name;
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
     * Called when a source object has been created.
     *
     * @param resourceContainer location where the object is located
     * @param resourceId the object that was created.
     * @param value
     *            the value of the object that was created.
     * @throws ResourceException
     *             if an exception occurs processing the notification.
     */
    public void onCreate(ServerContext context, String resourceContainer, String resourceId, Resource value) throws ResourceException {
        final RouteService sync = syncRoute.get();
        if (null != sync) {
            ActionRequest request = Requests.newActionRequest("sync", "ONCREATE")
                    .setAdditionalParameter("resourceContainer", resourceContainer)
                    .setAdditionalParameter("resourceId", resourceId)
                    .setContent(value.getContent());
            try {
                connectionFactory.getConnection().action(context, request);
            } catch (NotFoundException e) {
                logger.error("Failed to sync onCreate {}:{}",name, request.getResourceName(), e);
            }
        } else {
            logger.warn("Sync service was not available.");
        }
    }

    /**
     * Called when a source object has been updated.
     *
      @param resourceContainer location where the object is located
     * @param resourceId the object that was updated.
     * @param oldValue
     *            the old value of the object prior to the update.
     * @param newValue
     *            the new value of the object after the update.
     * @throws ResourceException
     *             if an exception occurs processing the notification.
     */
    public JsonValue onUpdate(ServerContext context, String resourceContainer, String resourceId, Resource oldValue,
            JsonValue newValue) throws ResourceException {
        final RouteService sync = syncRoute.get();
        if (null != sync) {
            ActionRequest request = Requests.newActionRequest("sync", "ONUPDATE")
                    .setAdditionalParameter("resourceContainer", resourceContainer)
                    .setAdditionalParameter("resourceId", resourceId)
                    .setContent(newValue);  // TODO Where to store the old value???
            try {
                return connectionFactory.getConnection().action(context, request);
            } catch (NotFoundException e) {
                logger.error("Failed to sync onUpdate {}:{}",name, request.getResourceName(), e);
            }
        } else {
            logger.warn("Sync service was not available.");
        }
        return null;
    }

    /**
     * Called when a source object has been deleted.
     *
     * @param resourceContainer location where the object is located
     * @param resourceId the object that was deleted.
     * @param oldValue
     *            the value before the delete, or null if not supplied
     * @throws ResourceException
     *             if an exception occurs processing the notification.
     */
    public void onDelete(ServerContext context, String resourceContainer, String resourceId, Resource oldValue)
            throws ResourceException {
        final RouteService sync = syncRoute.get();
        if (null != sync) {
            ActionRequest request = Requests.newActionRequest("sync", "ONDELETE")
                    .setAdditionalParameter("resourceContainer", resourceContainer)
                    .setAdditionalParameter("resourceId", resourceId)
                    .setContent(oldValue.getContent());
            try {
                connectionFactory.getConnection().action(context, request);
            } catch (NotFoundException e) {
               logger.error("Failed to sync onDelete {}:{}",name, request.getResourceName(), e);
            }
        } else {
            logger.warn("Sync service was not available.");
        }
    }
}
