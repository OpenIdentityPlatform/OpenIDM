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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.managed;

// Java SE
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// OSGi
import org.osgi.framework.ServiceReference;

// JSON Fluent
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.resource.JsonResourceContext;

// OpenIDM
import org.forgerock.openidm.audit.util.Action;
import org.forgerock.openidm.audit.util.ActivityLog;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.Scripts;
import org.forgerock.openidm.script.ScriptThrownException;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.SynchronizationListener;

// Deprecated
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetJsonResource;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.patch.JsonPatchWrapper;

/**
 * Provides access to a set of managed objects of a given type.
 *
 * @author Paul C. Bryan
 * @author aegloff
 */
class ManagedObjectSet extends ObjectSetJsonResource {

    private final static Logger LOGGER = LoggerFactory.getLogger(ManagedObjectSet.class);

    /** The managed objects service that instantiated this managed object set. */
    private ManagedObjectService service;

    /** Name of the managed object type. */
    private String name;

    /** The schema to use to validate the structure and content of the managed object. */
    private JsonValue schema;

    /** Script to execute when the creation of an object is being requested. */
    private Script onCreate;

    /** Script to execute when the read of an object is being requested. */
    private Script onRead;

    /** Script to execute when the update of an object is being requested. */
    private Script onUpdate;

    /** Script to execute when the deletion of an object is being requested. */
    private Script onDelete;

    /** Script to execute when a managed object requires validation. */
    private Script onValidate;

    /** Script to execute once an object is retrieved from the repository. */
    private Script onRetrieve;

    /** Script to execute when an object is about to be stored in the repository. */
    private Script onStore;

    /** Properties for which triggers are executed during object set operations. */
    private ArrayList<ManagedObjectProperty> properties = new ArrayList<ManagedObjectProperty>();

    /**
     * Constructs a new managed object set.
     *
     * @param service the managed object service
     * @param config configuration object to use to initialize managed object set.
     * @throws JsonValueException if the configuration is malformed.
     */
    public ManagedObjectSet(ManagedObjectService service, JsonValue config) throws JsonValueException {
        this.service = service;
        name = config.get("name").required().asString();
        schema = config.get("schema").expect(Map.class); // TODO: parse into json-schema object
        onCreate = Scripts.newInstance("ManagedObjectSet", config.get("onCreate"));
        onRead = Scripts.newInstance("ManagedObjectSet", config.get("onRead"));
        onUpdate = Scripts.newInstance("ManagedObjectSet", config.get("onUpdate"));
        onDelete = Scripts.newInstance("ManagedObjectSet", config.get("onDelete"));
        onValidate = Scripts.newInstance("ManagedObjectSet", config.get("onValidate"));
        onRetrieve = Scripts.newInstance("ManagedObjectSet", config.get("onRetrieve"));
        onStore = Scripts.newInstance("ManagedObjectSet", config.get("onStore"));
        for (JsonValue property : config.get("properties").expect(List.class)) {
            properties.add(new ManagedObjectProperty(service, property));
        }
        LOGGER.debug("Instantiated managed object set: {}", name);
    }

    /**
     * Generates a fully-qualified object identifier for the managed object.
     *
     * @param id the local managed object identifier to qualify.
     * @return the fully-qualified managed object identifier.
     */
// TODO: consider moving this logic somewhere else
    private String managedId(String id) {
        StringBuilder sb = new StringBuilder("managed/").append(name);
        if (id != null) {
            sb.append('/').append(id);
        }
        return sb.toString();
    }

    /**
     * Generates a fully-qualified object identifier for the repository.
     *
     * @param id the local managed object identifier to qualify.
     * @return the fully-qualified repository object identifier.
     */
    private String repoId(String id) {
        return "repo/" + managedId(id);
    }

    /**
     * Executes a script if it exists, populating an {@code "object"} property in the root
     * scope.
     *
     * @param type the type of script being executed.
     * @param script the script to execute, or {@code null} to execute nothing.
     * @param value the object to be populated in the script scope.
     * @throws ForbiddenException if the script throws an exception.
     * @throws InternalServerErrorException if any other exception is encountered.
     */
    private void execScript(String type, Script script, JsonValue value)
    throws ForbiddenException, InternalServerErrorException {
        if (script != null) {
            Map<String, Object> scope = service.newScope();
            scope.put("object", value.getObject());
            try {
                script.exec(scope); // allows direct modification to the object
            } catch (ScriptThrownException ste) {
                throw new ForbiddenException(ste.getValue().toString()); // script aborting the trigger
            } catch (ScriptException se) {
                String msg = type + " script encountered exception";
                LOGGER.debug(msg, se);
                throw new InternalServerErrorException(msg, se);
            }
        }
    }

    /**
     * Executes all of the necessary trigger scripts when an object is retrieved from the
     * repository.
     *
     * @param value the JSON value that was retrieved from the repository.
     * @throws ForbiddenException if a validation trigger throws an exception.
     * @throws InternalServerErrorException if any other exception occurs.
     */
    private void onRetrieve(JsonValue value) throws ForbiddenException, InternalServerErrorException {
        execScript("onRetrieve", onRetrieve, value);
        for (ManagedObjectProperty property : properties) {
            property.onRetrieve(value);
        }
    }

    /**
     * Executes all of the necessary trigger scripts when an object is to be stored in the
     * repository.
     *
     * @param value the JSON value to be stored in the repository.
     * @throws ForbiddenException if a validation trigger throws an exception.
     * @throws InternalServerErrorException if any other exception occurs.
     */
    private void onStore(JsonValue value) throws ForbiddenException, InternalServerErrorException {
        for (ManagedObjectProperty property : properties) {
            property.onValidate(value);
        }
        execScript("onValidate", onValidate, value);
// TODO: schema validation here (w. optimizations)
        for (ManagedObjectProperty property : properties) {
            property.onStore(value); // includes per-property encryption
        }
        execScript("onStore", onStore, value);
    }

    /**
     * Decrypt the value
     *
     * @param value an json value with poentially encrypted value(s)
     * @return object with values decrypted
     * @throws InternalServerErrorException if decryption failed for any reason
     */
    private JsonValue decrypt(JsonValue value) throws InternalServerErrorException {
        try {
            return service.getCryptoService().decrypt(value); // makes a copy, which we can modify
        } catch (JsonException je) {
            throw new InternalServerErrorException(je);
        }
    }

    /**
     * Decrypt the value
     *
     * @param object in map format with potentially encrypted value(s)
     * @return object with decrypted values
     * @throws InternalServerErrorException TODO.
     */
    private JsonValue decrypt(Map<String, Object> object) throws InternalServerErrorException {
        return decrypt(new JsonValue(object));
    }

    /**
     * Log the activities on the managed object
     *
     * @param id unqualified managed object id
     * @param msg optional message
     * @param before object state to log as before the operation
     * @param after object state to log as after the operation.
     * @throws ObjectSetException if logging the activiy fails
     */
    private void logActivity(String id, String msg, JsonValue before, JsonValue after) throws ObjectSetException {
        ActivityLog.log(service.getRouter(), ObjectSetContext.get(), msg,
                managedId(id), before, after, Status.SUCCESS);
    }

    /**
     * Forbid the use of sub objects
     *
     * @param id the identifier to check
     * @throws ForbiddenException if the identifier identifies a sub object
     */
    private void noSubObjects(String id) throws ForbiddenException {
        if (id != null && id.indexOf('/') >= 0) {
            throw new ForbiddenException("Sub-objects are not supported");
        }
    }

    /**
     * Forbid operation without id, on the whole object set
     *
     * @param id the identifier to check
     * @throws ForbiddenException if there is no identifier.
     */
    private void idRequired(String id) throws ForbiddenException {
        if (id == null) {
            throw new ForbiddenException("Operation not allowed on entire object set");
        }
    }

    private void update(String id, String rev, JsonValue oldValue, JsonValue newValue) throws ObjectSetException {
        if (newValue.asMap().equals(oldValue.asMap())) { // object hasn't changed
            return; // do nothing
        }
        if (onUpdate != null) {
            Map<String, Object> scope = service.newScope();
            scope.put("oldObject", oldValue.asMap());
            scope.put("newObject", newValue.asMap());
            try {
                onUpdate.exec(scope); // allows direct modification to the objects
            } catch (ScriptThrownException ste) {
                throw new ForbiddenException(ste.getValue().toString()); // script aborting the trigger
            } catch (ScriptException se) {
                String msg = "onUpdate script encountered exception";
                LOGGER.debug(msg, se);
                throw new InternalServerErrorException(msg, se);
            }
        }
        onStore(newValue); // performs per-property encryption
        service.getRouter().update(repoId(id), rev, newValue.asMap());
        try {
            for (SynchronizationListener listener : service.getListeners()) {
                listener.onUpdate(managedId(id), oldValue, newValue);
            }
        } catch (SynchronizationException se) {
            throw new InternalServerErrorException(se);
        }
    }

    /**
     * Applies a patch document to an object, or by finding an object in the object set itself
     * via query parameters. As this is an action, the patch document to be applied is in the
     * {@code _entity} parameter.
     *
     * @param id TODO.
     * @param params TODO.
     * @return TODO.
     * @throws ObjectSetException TODO.
     */
    private JsonValue patchAction(String id, JsonValue params) throws ObjectSetException {
        String _id = id; // identifier provided in path
        if (_id == null) {
            _id = params.get("_id").asString(); // identifier provided as query parameter
        }
        String _rev = params.get("_rev").asString();
        if (_id == null) { // identifier not provided in URI; this is patch-by-query
            try {
                JsonValue results = new JsonValue(service.getRouter().query(repoId(null),
                        params.asMap()), new JsonPointer("results")).get(QueryConstants.QUERY_RESULT);
                if (!results.isList()) {
                    throw new InternalServerErrorException("Expecting list result from query");
                } else if (results.size() == 0) {
                    throw new NotFoundException();
                } else if (results.size() > 1) {
                    throw new ConflictException("Query yielded more than one result");
                }
                JsonValue result = results.get(0);
                _id = result.get("_id").required().asString();
                if (_rev == null) { // don't override an explicitly supplied revision
                    _rev = result.get("_rev").asString();
                }
            } catch (JsonValueException jve) {
                throw new InternalServerErrorException(jve);
            }
        }
        patch(_id, _rev, new JsonPatchWrapper(decrypt(params.get("_entity"))));
        return new JsonValue(null); // empty response (and lack of exception) indicates success
    }

    @Override
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        LOGGER.debug("Create name={} id={}", name, id);
        noSubObjects(id);
        JsonValue jv = decrypt(object); // decrypt any incoming encrypted properties
        execScript("onCreate", onCreate, jv);
        onStore(jv); // includes per-property encryption
        JsonValue _id = jv.get("_id");
        if (_id.isString()) {
            id = _id.asString(); // override requested ID with one specified in object
        }
        if (id == null) { // default is to assign a UUID identifier
            id = UUID.randomUUID().toString();
            jv.put("_id", id);
        }
        service.getRouter().create(repoId(id), jv.asMap());
        logActivity(id, null, null, jv);
        try {
            for (SynchronizationListener listener : service.getListeners()) {
                listener.onCreate(managedId(id), jv);
            }
        } catch (SynchronizationException se) {
            throw new InternalServerErrorException(se);
        }
        object.put("_id", jv.get("_id").getObject());
        object.put("_rev", jv.get("_rev").getObject());
    }

    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        LOGGER.debug("Read name={} id={}", name, id);
        idRequired(id);
        noSubObjects(id);
        JsonValue jv = new JsonValue(service.getRouter().read(repoId(id)));
        onRetrieve(jv);
        execScript("onRead", onRead, jv);
        logActivity(id, null, jv, null);

        if (isPublicContext()) {
            // if it came over a public interface we have to cull private properties
            cullPrivateProperties(jv);
        }

        return jv.asMap();
    }

    @Override
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        LOGGER.debug("update {} ", "name=" + name + " id=" + id + " rev=" + rev);
        idRequired(id);
        noSubObjects(id);
        JsonValue _new = decrypt(object); // decrypt any incoming encrypted properties
        Map<String, Object> encrypted = service.getRouter().read(repoId(id));
        JsonValue decrypted = decrypt(encrypted);
        update(id, rev, decrypted, _new);
        logActivity(id, null, new JsonValue(encrypted), _new);
        object.put("_id", _new.get("_id").getObject());
        object.put("_rev", _new.get("_rev").getObject());
    }

    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        LOGGER.debug("Delete {} ", "name=" + name + " id=" + id + " rev=" + rev);
        idRequired(id);
        noSubObjects(id);
        Map<String, Object> encrypted = service.getRouter().read(repoId(id));
        if (onDelete != null) {
            execScript("onDelete", onDelete, decrypt(encrypted));
        }
        service.getRouter().delete(repoId(id), rev);
        logActivity(id, null, new JsonValue(encrypted), null);
        try {
            for (SynchronizationListener listener : service.getListeners()) {
                listener.onDelete(managedId(id));
            }
        } catch (SynchronizationException se) {
            throw new InternalServerErrorException(se);
        }
    }

    // TODO: Consider dropping this Patch object abstraction and just process a patch document directly?
    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        // FIXME: There's no way to decrypt a patch document. :-(  Luckily, it'll work for now with patch action.
        boolean forceUpdate = (rev == null);
        boolean retry = forceUpdate;
        String _rev = rev;

        do {
            LOGGER.debug("patch name={} id={}", name, id);
            idRequired(id);
            noSubObjects(id);
            JsonValue oldValue = new JsonValue(service.getRouter().read(repoId(id))); // Get the oldest value for diffing in the log
            JsonValue decrypted = decrypt(oldValue); // decrypt any incoming encrypted properties

            // If we haven't defined a rev, we need to get the current rev
            if (rev == null) {
                _rev = decrypted.get("_rev").asString();
            }

            JsonValue newValue = decrypted.copy();
            patch.apply(newValue.asMap());
            JsonValue params = new JsonValue(new HashMap<String, Object>());
            // Validate policies on the patched object
            params.add("_action", "validateObject");
            params.add("value", newValue);
            JsonValue result = new JsonValue(service.getRouter().action("policy/" + managedId(id), params.asMap()));
            if (!result.isNull() && !result.get("result").asBoolean()) {
                LOGGER.debug("Requested patch failed policy validation: {}", result);
                throw new ForbiddenException("Failed policy validation", result.asMap());
            }

            try {
                update(id, _rev, decrypted, newValue);
                retry = false;
                logActivity(id, "Patch " + patch, oldValue, newValue);
            } catch (ConflictException e) {
                if (forceUpdate) {
                    LOGGER.debug("Unable to update due to revision conflict. Retrying.");
                } else {
                    // If it fails and we're not trying to force an update, we gave it our best shot
                    throw e;
                }
            }
        } while(retry);
    }

    @Override
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        LOGGER.debug("query name={} id={}", name, id);
        noSubObjects(id);
        Map<String, Object> result = service.getRouter().query(repoId(id), params);
        logActivity(id, "Query parameters " + params, new JsonValue(result), null);

        if (isPublicContext()) {
            // If it came over a public interface we have to cull each resulting object
            JsonValue jv = new JsonValue(result);
            JsonValue resultList = jv.get(QueryConstants.QUERY_RESULT);

            // List will be empty if there are no results
            for (JsonValue val : resultList) {
                cullPrivateProperties(val);
            }

            result = jv.asMap();
        }

        return result;
    }

    /**
     * Processes action requests.
     * <p>
     * If the {@code _action} parameter is {@code patch}, then the request is handled as
     * a partial modification to an object, either explicitly (identifier is supplied) or by
     * query (query parameters specify the query to perform to yield a single object to patch.
     */
    @Override
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        LOGGER.debug("action name={} id={}", name, id);
        noSubObjects(id);
        Object _action = (String)params.get("_action");
        Map<String, Object> result;
        if (_action == null) {
            throw new BadRequestException("Expecting _action parameter");
        } else if (_action.equals("patch")) { // patch by query
            logActivity(id, "Action: " + _action, null, null);
            result = patchAction(id, new JsonValue(params, new JsonPointer("parameters"))).asMap();
        } else {
            throw new BadRequestException("Unsupported _action parameter");
        }
        return result;
    }

    /**
     * Returns the name of the managed object set.
     */
    public String getName() {
        return name;
    }

    /**
     * Culls properties that are marked private
     * @param jv JsonValue to cull private properties from
     * @return the supplied JsonValue with private properties culled
     */
    private JsonValue cullPrivateProperties(JsonValue jv) {
        for (ManagedObjectProperty property : properties) {
            if (property.isPrivate()) {
                jv.remove(property.getName());
            }
        }
        return jv;
    }

    /**
     * Checks to see if the current request's context came from a public interface (i.e. http)
     * @return true if it came over http, false otherwise
     */
    private boolean isPublicContext() {
        JsonValue context = ObjectSetContext.get();
        JsonValue parent = JsonResourceContext.getParentContext(context);
        return "http".equals(parent.get("type").asString());
    }
}
