/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.ScriptException;

import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.patch.JsonPatch;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.router.RouteService;
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
    private final CryptoService service;

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

    /** Script to execute when a managed object requires validation. */
    private final ScriptEntry onValidate;

    /** Script to execute once an object is retrieved from the repository. */
    private final ScriptEntry onRetrieve;

    /**
     * Script to execute when an object is about to be stored in the repository.
     */
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
     * @param service
     *            the managed object service
     * @param config
     *            configuration object to use to initialize managed object set.
     * @throws JsonValueException
     *             if the configuration is malformed.
     */

    /**
     * Constructs a new managed object set.
     *
     * @param scriptRegistry
     * @param cryptoService
     * @param config
     *            configuration object to use to initialize managed object set.
     * @throws JsonValueException
     *             when the configuration is malformed
     * @throws ScriptException
     *             when the script configuration is malformed or the script is
     *             invalid.
     */
    public ManagedObjectSet(final ScriptRegistry scriptRegistry, final CryptoService cryptoService,
                            final AtomicReference<RouteService> syncRefrence, JsonValue config) throws JsonValueException, ScriptException {
        this.service = cryptoService;
        this.syncRoute = syncRefrence;
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
        if (null != onCreate) {
            onRead.deleteScriptListener(this);
        }
        if (null != onCreate) {
            onUpdate.deleteScriptListener(this);
        }
        if (null != onCreate) {
            onDelete.deleteScriptListener(this);
        }
        if (null != onCreate) {
            onValidate.deleteScriptListener(this);
        }
        if (null != onCreate) {
            onRetrieve.deleteScriptListener(this);
        }
        if (null != onCreate) {
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
        StringBuilder sb = new StringBuilder("/managed/").append(name);
        if (id != null) {
            sb.append('/').append(id);
        }
        return sb.toString();
    }

    /**
     * Generates a fully-qualified object identifier for the repository.
     *
     * @param id
     *            the local managed object identifier to qualify.
     * @return the fully-qualified repository object identifier.
     */
    private String repoId(String id) {
        return "/repo" + managedId(id);
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
     * @throws ForbiddenException
     *             if the script throws an exception.
     * @throws InternalServerErrorException
     *             if any other exception is encountered.
     */
    private void execScript(final ServerContext context, String type, ScriptEntry script,
            JsonValue value) throws ForbiddenException, InternalServerErrorException {
        if (null != script && script.isActive()) {
            Script executable = script.getScript(context);
            executable.put("object", value.getObject());
            try {
                executable.eval(); // allows direct modification to the object
            } catch (ScriptThrownException ste) {
                // script aborting the trigger
                throw new ForbiddenException(ste.getValue().toString());
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
    private void onRetrieve(ServerContext context, Resource value) throws ForbiddenException,
            InternalServerErrorException {
        execScript(context, "onRetrieve", onRetrieve, value.getContent());
        for (ManagedObjectProperty property : properties) {
            property.onRetrieve(context, value.getContent());
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
    private void onStore(ServerContext context, JsonValue value) throws ForbiddenException,
            InternalServerErrorException {
        for (ManagedObjectProperty property : properties) {
            property.onValidate(context, value);
        }
        execScript(context, "onValidate", onValidate, value);
        // TODO: schema validation here (w. optimizations)
        for (ManagedObjectProperty property : properties) {
            property.onStore(context, value); // includes per-property
                                              // encryption
        }
        execScript(context, "onStore", onStore, value);
    }

    /**
     * Decrypt the value
     *
     * @param value
     *            an json value with poentially encrypted value(s)
     * @return object with values decrypted
     * @throws InternalServerErrorException
     *             if decryption failed for any reason
     */
    private JsonValue decrypt(final JsonValue value) throws InternalServerErrorException {
        try {
            return service.decrypt(value); // makes a copy, which we can modify
        } catch (JsonException je) {
            throw new InternalServerErrorException(je);
        }
    }

    /**
     * Decrypt the value
     *
     * @param value
     *            an json value with poentially encrypted value(s)
     * @return object with values decrypted
     * @throws InternalServerErrorException
     *             if decryption failed for any reason
     */
    private Resource decrypt(final Resource value) throws InternalServerErrorException {
        try {
            // makes a copy, which we can modify
            return new Resource(value.getId(), value.getRevision(),
                    null != value.getContent() ? service.decrypt(value.getContent()) : null);
        } catch (JsonException je) {
            throw new InternalServerErrorException(je);
        }
    }

    /**
     * Log the activities on the managed object
     *
     * @param id
     *            unqualified managed object id
     * @param msg
     *            optional message
     * @param before
     *            object state to log as before the operation
     * @param after
     *            object state to log as after the operation.
     * @throws ResourceException
     *             if logging the activiy fails
     */
    private void logActivity(ServerContext context, String id, String msg, JsonValue before,
            JsonValue after) throws ResourceException {
        // ActivityLog.log(context, msg, managedId(id), before, after,
        // Status.SUCCESS);
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

    private Resource update(final ServerContext context, String id, String rev, Resource oldValue,
            JsonValue newValue) throws ResourceException {
        if (newValue.equals(oldValue.getContent())) { // object hasn't changed
            return new Resource(id, rev, null);
        }
        if (null != onUpdate && onUpdate.isActive()) {
            Script executable = onUpdate.getScript(context);
            executable.put("oldObject", oldValue.getContent().asMap());
            executable.put("newObject", newValue.asMap());
            try {
                executable.eval(); // allows direct modification to the objects
            } catch (ScriptThrownException ste) {
                // script aborting the trigger
                throw new ForbiddenException(ste.getValue().toString());
            } catch (ScriptException se) {
                String msg = "onUpdate script encountered exception";
                logger.debug(msg, se);
                throw new InternalServerErrorException(msg, se);
            }
        }
        onStore(context, newValue); // performs per-property encryption
        //
        UpdateRequest request = Requests.newUpdateRequest(repoId(id), newValue);
        request.setRevision(rev);
        Resource response = context.getConnection().update(context, request);

        // TODO: Fix the context
        onUpdate(context, managedId(id), oldValue, newValue);

        return response;
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

        QueryRequest queryRequest = Requests.newQueryRequest(repoId(null));
        // TODO build the query request

        final JsonValue[] lastError = new JsonValue[1];

        final JsonValue patch = request.getContent().required().expect(List.class);

        final QueryResultHandler handler = new QueryResultHandler() {
            @Override
            public void handleError(final ResourceException error) {
                lastError[0] = error.toJsonValue();
            }

            @Override
            public boolean handleResource(Resource resource) {
                // TODO This should not fail on first error and the response
                // should contains each result

                try {
                    Object before = resource.getContent().getObject();
                    JsonPatch.patch(resource.getContent(), patch);
                    if (before != request.getContent().getObject()) {
                        lastError[0] =
                                new ConflictException("replacing the root value is not supported")
                                        .toJsonValue();
                    }
                } catch (JsonValueException jve) {
                    lastError[0] =
                            new ConflictException(jve.getMessage(), jve).setDetail(
                                    jve.getJsonValue()).toJsonValue();
                }

                return null != lastError[0];
            }

            @Override
            public void handleResult(QueryResult result) {
                // we don't care
            }
        };

        // JsonValue results = new
        // JsonValue(service.getRouter().query(repoId(null),
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
        // public void create(String id, Map<String, Object> object) throws
        // ResourceException {
        logger.debug("Create name={} id={}", name, request.getNewResourceId());

        try {

            // decrypt any incoming encrypted properties
            JsonValue jv = decrypt(request.getContent());
            execScript(context, "onCreate", onCreate, jv);
            // includes per-property encryption
            onStore(context, jv);

            // Map<String, String> uriTemplateVariables =
            // ContextUtil.getUriTemplateVariables(context);

            // String id1 = null != uriTemplateVariables ?
            // uriTemplateVariables.get("id") : null;
            // String id2 = request.getNewResourceId();
            // String id3 = request.getResourceName();

            // if (_id.isString()) {
            // id = _id.asString(); // override requested ID with one specified
            // in object
            // }
            // if (id == null) { // default is to assign a UUID identifier
            // id = UUID.randomUUID().toString();
            // jv.put("_id", id);
            // }

            CreateRequest createRequest = Requests.copyOfCreateRequest(request);
            createRequest.setResourceName(repoId(null));

            Resource _new = context.getConnection().create(context, createRequest);

            logActivity(context, managedId(_new.getId()), null, null, jv);

            onCreate(context, managedId(_new.getId()), _new /* TODO fix jv */);

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
        // public Map<String, Object> read(String id) throws ResourceException
        // {
        logger.debug("Read name={} id={}", name, resourceId);
        try {

            ReadRequest readRequest = Requests.newReadRequest(repoId(resourceId));
            Resource resource = context.getConnection().read(context, readRequest);

            onRetrieve(context, resource);
            execScript(context, "onRead", onRead, resource.getContent());
            logActivity(context, resourceId, null, resource.getContent(), null);

            handler.handleResult(isPublicContext(context) ? cullPrivateProperties(resource)
                    : resource);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    @Override
    public void updateInstance(final ServerContext context,final String resourceId,final  UpdateRequest request,
            final ResultHandler<Resource> handler) {
        // public void update(String id, String rev, Map<String, Object> object)
        // throws ResourceException {
        logger.debug("update {} ", "name=" + name + " id=" + resourceId + " rev="
                + request.getRevision());

        try {
            // decrypt any incoming encrypted properties
            JsonValue _new = decrypt(request.getNewContent());

            ReadRequest readRequest = Requests.newReadRequest(repoId(resourceId));
            for (JsonPointer pointer: request.getFields()) {
                readRequest.addField(pointer);
            }
            Resource resource = context.getConnection().read(context, readRequest);
            Resource _old = decrypt(resource);

            handler.handleResult(update(context, resourceId, request.getRevision(), _old, _new));

            // TODO Fix the Auditing
            logActivity(context, resourceId, null, _old.getContent(), _new);

        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    @Override
    public void deleteInstance(final ServerContext context, final String resourceId, DeleteRequest request,
            ResultHandler<Resource> handler) {
        // public void delete(String id, String rev) throws ResourceException {
        logger.debug("Delete {} ", "name=" + name + " id=" + resourceId + " rev="
                + request.getRevision());
        try {
            ReadRequest readRequest = Requests.newReadRequest(repoId(resourceId));

            Resource resource = context.getConnection().read(context, readRequest);
            if (onDelete != null) {
                execScript(context, "onDelete", onDelete, decrypt(resource.getContent()));
            }
            DeleteRequest deleteRequest = Requests.copyOfDeleteRequest(request);
            deleteRequest.setResourceName(repoId(resourceId));
            if (deleteRequest.getRevision() == null) {
                deleteRequest.setRevision(resource.getRevision());
            }
            Resource deletedResource = context.getConnection().delete(context, deleteRequest);
            // TODO Fix the Auditing
            logActivity(context, resourceId, null, resource.getContent(), null);

            onDelete(context, resourceId, resource);

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
        // TODO: Consider dropping this Patch object abstraction and just
        // process a patch document directly?
        // public void patch(String id, String rev, Patch patch) throws
        // ResourceException {
        // FIXME: There's no way to decrypt a patch document. :-( Luckily, it'll
        // work for now with patch action.
        // boolean forceUpdate = (rev == null);
        // boolean retry = forceUpdate;
        // String _rev = rev;
        //
        // do {
        // logger.debug("patch name={} id={}", name, id);
        // idRequired(id);
        // noSubObjects(id);
        // JsonValue oldValue = new
        // JsonValue(service.getRouter().read(repoId(id))); // Get the oldest
        // value for diffing in the log
        // JsonValue decrypted = decrypt(oldValue); // decrypt any incoming
        // encrypted properties
        //
        // // If we haven't defined a rev, we need to get the current rev
        // if (rev == null) {
        // _rev = decrypted.get("_rev").asString();
        // }
        //
        // JsonValue newValue = decrypted.copy();
        // patch.apply(newValue.asMap());
        // JsonValue params = new JsonValue(new HashMap<String, Object>());
        // // Validate policies on the patched object
        // params.add("_action", "validateObject");
        // params.add("value", newValue);
        // if (isPublicContext()) {
        // ObjectSetContext.get().add("_isDirectHttp", true);
        // }
        //
        // if (enforcePolicies) {
        // JsonValue result = new JsonValue(service.getRouter().action("policy/"
        // + managedId(id), params.asMap()));
        // if (!result.isNull() && !result.get("result").asBoolean()) {
        // logger.debug("Requested patch failed policy validation: {}", result);
        // throw new ForbiddenException("Failed policy validation",
        // result.asMap());
        // }
        // }
        //
        // try {
        // update(id, _rev, decrypted, newValue);
        // retry = false;
        // logger.debug("Patch successful!");
        // logActivity(id, "Patch " + patch, oldValue, newValue);
        // } catch (PreconditionFailedException e) {
        // if (forceUpdate) {
        // logger.debug("Unable to update due to revision conflict. Retrying.");
        // } else {
        // // If it fails and we're not trying to force an update, we gave it
        // our best shot
        // handler.handleError( e);
        // }
        // }
        // } while(retry);
    }

    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        logger.debug("query name={} id={}", name, request.getResourceName());

        QueryRequest repoRequest = Requests.copyOfQueryRequest(request);
        repoRequest.setResourceName(repoId(null));

        try {
            context.getConnection().query(context, repoRequest, new QueryResultHandler() {
                @Override
                public void handleError(ResourceException error) {
                    handler.handleError(error);
                }

                @Override
                public boolean handleResource(Resource resource) {
                    if (isPublicContext(context)) {
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
        } catch (ResourceException e) {
            handler.handleError(e);
        }
        // logActivity(context, repoRequest.getResourceName(),
        // "Query parameters " + params, new JsonValue(result), null);
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
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
    public void actionCollection(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        // public Map<String, Object> action(String id, Map<String, Object>
        // params) throws ResourceException {
        logger.debug("action name={} id={}", name, request.getResourceName());

        try {
            Action action = Action.valueOf(request.getAction());
            logActivity(context, request.getResourceName(), "Action: " + request.getAction(),
                    null, null);
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
        for (ManagedObjectProperty property : properties) {
            if (property.isPrivate()) {
                jv.getContent().remove(property.getName());
            }
        }
        return jv;
    }

    /**
     * Checks to see if the current request's context came from a public
     * interface (i.e. http)
     *
     * @return true if it came over http, false otherwise
     */
    private boolean isPublicContext(Context context) {
        return context.containsContext(HttpContext.class);
    }

    /**
     * Called when a source object has been created.
     *
     * @param id
     *            the fully-qualified identifier of the object that was created.
     * @param value
     *            the value of the object that was created.
     * @throws ResourceException
     *             if an exception occurs processing the notification.
     */
    public void onCreate(ServerContext context, String id, Resource value) throws ResourceException {
        final RouteService sync = syncRoute.get();
        if (null != sync) {
            ActionRequest request = Requests.newActionRequest("sync", "ONCREATE");
            request.setAdditionalActionParameter("id", id);
            request.setContent(value.getContent());
            try {
                context.getConnection().action(context, request);
            } catch (NotFoundException e) {
                logger.error("Failed to sync onCreate {}:{}",name, id, e);
            }
        }else {
            logger.warn("Sync service was not available.");
        }
    }

    /**
     * Called when a source object has been updated.
     *
     * @param id
     *            the fully-qualified identifier of the object that was updated.
     * @param oldValue
     *            the old value of the object prior to the update.
     * @param newValue
     *            the new value of the object after the update.
     * @throws ResourceException
     *             if an exception occurs processing the notification.
     */
    public JsonValue onUpdate(ServerContext context, String id, Resource oldValue,
            JsonValue newValue) throws ResourceException {
        final RouteService sync = syncRoute.get();
        if (null != sync) {
            ActionRequest request = Requests.newActionRequest("sync", "ONUPDATE");
            request.setAdditionalActionParameter("id", id);
            // TODO Where to store the old value???
            request.setContent(newValue);
            try {
                return context.getConnection().action(context, request);
            } catch (NotFoundException e) {
                logger.error("Failed to sync onUpdate {}:{}",name, id, e);
            }
        } else {
            logger.warn("Sync service was not available.");
        }
        return null;
    }

    /**
     * Called when a source object has been deleted.
     *
     * @param id
     *            the fully-qualified identifier of the object that was deleted.
     * @param oldValue
     *            the value before the delete, or null if not supplied
     * @throws ResourceException
     *             if an exception occurs processing the notification.
     */
    public void onDelete(ServerContext context, String id, Resource oldValue)
            throws ResourceException {
        final RouteService sync = syncRoute.get();
        if (null != sync) {
            ActionRequest request = Requests.newActionRequest("sync", "ONDELETE");
            request.setAdditionalActionParameter("id", id);
            request.setContent(oldValue.getContent());
            try {
                context.getConnection().action(context, request);
            } catch (NotFoundException e) {
               logger.error("Failed to sync onDelete {}:{}",name, id, e);
            }
        } else {
            logger.warn("Sync service was not available.");
        }
    }
}
