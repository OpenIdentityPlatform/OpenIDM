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

// Java Standard Edition
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// OSGi Framework
import org.osgi.framework.ServiceReference;

// JSON Fluent library
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

// OpenIDM
import org.forgerock.openidm.audit.util.Action;
import org.forgerock.openidm.audit.util.ActivityLog;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.Scripts;
import org.forgerock.openidm.script.ScriptThrownException;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.SynchronizationListener;

/**
 * Provides access to a set of managed objects of a given type.
 *
 * @author Paul C. Bryan
 */
class ManagedObjectSet implements ObjectSet {

    /** TODO: Description. */
    private final static Logger LOGGER = LoggerFactory.getLogger(ManagedObjectSet.class);

    /** Name of the managed object type. */
    private String name;

    /** The schema to use to validate the structure and content of the managed object. */
    private JsonNode schema;

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

    /** The managed objects service that instantiated this managed object set. */
    private ManagedObjectService service;

    /**
     * Constructs a new managed object set.
     *
     * @param config configuration object to use to initialize managed object set.
     * @throws JsonNodeException if the configuration is malformed.
     */
    public ManagedObjectSet(ManagedObjectService service, JsonNode config) throws JsonNodeException {
        this.service = service;
        name = config.get("name").required().asString();
        schema = config.get("schema").expect(Map.class); // TODO: parse into json-schema object
        onCreate = Scripts.newInstance(config.get("onCreate"));
        onRead = Scripts.newInstance(config.get("onRead"));
        onUpdate = Scripts.newInstance(config.get("onUpdate"));
        onDelete = Scripts.newInstance(config.get("onDelete"));
        onValidate = Scripts.newInstance(config.get("onValidate"));
        for (JsonNode property : config.get("properties").expect(List.class)) {
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
     * @param script the script to execute, or {@code null} to execute nothing.
     * @param node the JSON node whose value is to be populated in the script scope.
     * @throws ForbiddenException if the script throws an exception.
     * @throws InternalServerErrorException if any other exception is encountered.
     */
    private void execScript(Script script, JsonNode node) throws ForbiddenException, InternalServerErrorException {
        if (script != null) {
            HashMap<String, Object> scope = new HashMap<String, Object>();
            scope.put("object", node.getValue());
            try {
                script.exec(scope); // allows direct modification to the object
            } catch (ScriptThrownException ste) {
                throw new ForbiddenException(ste.getValue().toString()); // script aborting the trigger
            } catch (ScriptException se) {
                throw new InternalServerErrorException(se.getMessage());
            }
        }
    }

    /**
     * Executes all of the necessary trigger scripts when an object is retrieved from the
     * repository.
     *
     * @param node the JSON node that was retrieved from the repository.
     * @throws ForbiddenException if a validation trigger throws an exception.
     * @throws InternalServerErrorException if any other exception occurs.
     */
    private void onRetrieve(JsonNode node) throws ForbiddenException, InternalServerErrorException {
        execScript(onRetrieve, node);
        for (ManagedObjectProperty property : properties) {
            property.onRetrieve(node);
        }
// TODO: schema validation here (w. optimization), using on-the-fly decryption transformations
        execScript(onValidate, node);
        for (ManagedObjectProperty property : properties) {
            property.onValidate(node);
        }
    }

    /**
     * Executes all of the necessary trigger scripts when an object is to be stored in the
     * repository.
     *
     * @param node the JSON node to be stored in the repository.
     * @throws ForbiddenException if a validation trigger throws an exception.
     * @throws InternalServerErrorException if any other exception occurs.
     */
    private void onStore(JsonNode node) throws ForbiddenException, InternalServerErrorException {
        for (ManagedObjectProperty property : properties) {
            property.onValidate(node);
        }
        execScript(onValidate, node);
// TODO: schema validation here (w. optimizations)
        for (ManagedObjectProperty property : properties) {
            property.onStore(node); // includes per-property encryption
        }
        execScript(onStore, node);
    }

    /**
     * TODO: Description.
     *
     * @param object TODO.
     * @return TODO.
     * @throws InternalServerErrorException TODO.
     */ 
    private JsonNode decrypt(Map<String, Object> object) throws InternalServerErrorException {
        try {
            return service.getCryptoService().decrypt(new JsonNode(object)); // makes a copy, which we can modify
        } catch (JsonException je) {
            throw new InternalServerErrorException(je);
        }
    }

    @Override
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        LOGGER.debug("Create name={} id={}", name, id);
        JsonNode node = decrypt(object); // decrypt any incoming encrypted properties
        execScript(onCreate, node);
        onStore(node); // includes per-property encryption
        JsonNode _id = node.get("_id");
        if (_id.isString()) {
            id = _id.asString(); // override requested ID with one specified in object
        }
        if (id == null) { // default is to assign a UUID identifier
            id = UUID.randomUUID().toString();
            node.put("_id", id);
        }
        service.getRouter().create(repoId(id), node.asMap());
        ActivityLog.log(service.getRouter(), Action.CREATE, "", managedId(id), null, node.asMap(), Status.SUCCESS);
        try {
            for (SynchronizationListener listener : service.getListeners()) {
                listener.onCreate(managedId(id), node);
            }
        } catch (SynchronizationException se) {
            throw new InternalServerErrorException(se);
        }
        // workaround until JsonNode works its way into the ObjectSet API
        object.put("_id", node.get("_id").getValue());
        object.put("_rev", node.get("_rev").getValue());
    }

    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        LOGGER.debug("Read name={} id={}", name, id);
        if (id == null) {
            throw new ForbiddenException("cannot read entire set");
        }
        JsonNode node = new JsonNode(service.getRouter().read(repoId(id)));
        onRetrieve(node);
        execScript(onRead, node);
        ActivityLog.log(service.getRouter(), Action.READ, "", managedId(id), node.asMap(), null, Status.SUCCESS);
        return node.asMap();
    }

    @Override
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        LOGGER.debug("Update {} ", "name=" + name + " id=" + id + " rev=" + rev);
        if (id == null) {
            throw new ForbiddenException("cannot update entire set");
        }
        JsonNode node = decrypt(object); // decrypt any incoming encrypted properties
        Map<String, Object> oldEncrypted = service.getRouter().read(repoId(id));
        JsonNode oldDecrypted = decrypt(oldEncrypted);
        if (node.asMap().equals(oldDecrypted.asMap())) {
            return; // object hasn't changed; do not update
        }
        if (onUpdate != null) {
            HashMap<String, Object> scope = new HashMap<String, Object>();
            scope.put("oldObject", oldDecrypted.getValue());
            scope.put("newObject", node.asMap());
            try {
                onUpdate.exec(scope); // allows direct modification to the objects
            } catch (ScriptThrownException ste) {
                throw new ForbiddenException(ste.getValue().toString()); // script aborting the trigger
            } catch (ScriptException se) {
                throw new InternalServerErrorException(se.getMessage());
            }
        }
        onStore(node); // performs per-property encryption
        service.getRouter().update(repoId(id), rev, node.asMap());
        ActivityLog.log(service.getRouter(), Action.UPDATE, "", managedId(id), oldEncrypted, node.asMap(), Status.SUCCESS);
        try {
            JsonNode oldEncryptedNode = new JsonNode(oldEncrypted);
            for (SynchronizationListener listener : service.getListeners()) {
                listener.onUpdate(managedId(id), oldEncryptedNode, node);
            }
        } catch (SynchronizationException se) {
            throw new InternalServerErrorException(se);
        }
        // workaround until JsonNode works its way into the ObjectSet API
        object.put("_id", node.get("_id").getValue());
        object.put("_rev", node.get("_rev").getValue());
    }

    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        LOGGER.debug("Delete {} ", "name=" + name + " id=" + id + " rev=" + rev);
        if (id == null) {
            throw new ForbiddenException("cannot delete entire set");
        }
        Map<String, Object> encrypted = service.getRouter().read(repoId(id));
        if (onDelete != null) {
            execScript(onDelete, decrypt(encrypted));
        }
        service.getRouter().delete(repoId(id), rev);
        ActivityLog.log(service.getRouter(), Action.DELETE, "", managedId(id), encrypted, null, Status.SUCCESS);
        try {
            for (SynchronizationListener listener : service.getListeners()) {
                listener.onDelete(managedId(id));
            }
        } catch (SynchronizationException se) {
            throw new InternalServerErrorException(se);
        }
    }

    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new InternalServerErrorException("patch not yet implemented");
    }

    @Override
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        LOGGER.debug("Query name={} id={}", name, id);
        Map<String, Object> result = service.getRouter().query(repoId(id), params);
        ActivityLog.log(service.getRouter(), Action.QUERY, "Query parameters " + params,
         managedId(id), result, null, Status.SUCCESS);
        return result;
    }

    @Override
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        throw new ForbiddenException("action not yet implemented");
    }

    /**
     * Returns the name of the managed object set.
     */
    public String getName() {
        return name;
    }
}
