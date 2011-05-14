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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// OSGi Framework
import org.osgi.framework.ServiceReference;

// Apache Felix Maven SCR Plugin
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;

// JSON-Fluent library
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

// ForgeRock OpenIDM
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

    /** TODO: Description. */
    @Reference(
        name="Reference_ManagedObjectSet_SynchronizationListener",
        referenceInterface=SynchronizationListener.class,
        bind="bindListener",
        unbind="unbindListener",
        cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy=ReferencePolicy.DYNAMIC,
        strategy=ReferenceStrategy.EVENT
    )
    protected final HashSet<SynchronizationListener> listeners = new HashSet<SynchronizationListener>();
    protected void bindListener(SynchronizationListener listener) {
        listeners.add(listener);
    }
    protected void unbindListener(SynchronizationListener listener) {
        listeners.remove(listener);
    }

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
        for (JsonNode node : config.get("properties").expect(List.class)) {
            properties.add(new ManagedObjectProperty(node));
        }
        LOGGER.debug("Created managed object set: {}", name);
    }

    /**
     * Generates a fully-qualified object identifier for the repository. Currently,
     * hard-codes the identifier.
     *
     * @param id the local managed object identifier to qualify.
     * @return the fully-qualified repository object identifier.
     */
// TODO: Make this configurable as not every repository will likely adhere to the same scheme.
    private String repoId(String id) {
        StringBuilder sb = new StringBuilder("managed/").append(name);
        if (id != null) {
            sb.append('/').append(id);
        }
        return sb.toString();
    }

    /**
     * Generates a fully-qualified object identifier for the managed object. This is the
     * identifier used to access the managed object with the internal router. Currently
     * hard-codes the identifer.
     *
     * @param id the local managed object identifier to qualify.
     * @return the fully-qualified managed object identifier.
     */
// TODO: Just happen to be using repoId because it's the same. There should be a better
// way to know the fully qualified identifier of a managed object.
    private String routeId(String id) {
        return repoId(id);
    }

    /**
     * Executes a script if it exists, populating an {@code "object"} property in the root
     * scope.
     *
     * @param script the script to execute, or {@code null} to execute nothing.
     * @param object the object to populate in the script scope.
     * @throws ForbiddenException if the script throws an exception.
     * @throws InternalServerErrorException if any other exception is encountered.
     */
    private void execScript(Script script, Map<String, Object> object)
    throws ForbiddenException, InternalServerErrorException {
        if (script != null) {
            HashMap<String, Object> scope = new HashMap<String, Object>();
            scope.put("object", object);
            try {
                script.exec(scope); // allows direct modification to the object
            }
            catch (ScriptThrownException ste) {
                throw new ForbiddenException(ste.getValue().toString()); // script aborting the trigger
            }
            catch (ScriptException se) {
                throw new InternalServerErrorException(se.getMessage());
            }
        }
    }

    /**
     * Executes all of the necessary trigger scripts when an object is retrieved from the
     * repository.
     *
     * @param object the object that was retrieved from the repository.
     * @throws ForbiddenException if a validation trigger throws an exception.
     * @throws InternalServerErrorException if any other exception occurs.
     */
    private void onRetrieve(Map<String, Object> object) throws ForbiddenException, InternalServerErrorException {
        execScript(onRetrieve, object);
        for (ManagedObjectProperty property : properties) {
            property.onRetrieve(object);
        }
        // TODO: schema validation here (w. optimization)
        execScript(onValidate, object);
        for (ManagedObjectProperty property : properties) {
            property.onValidate(object);
        }
    }

    /**
     * Executes all of the necessary trigger scripts when an object is to be stored in the
     * repository.
     *
     * @param object the object to be stored in the repository.
     * @throws ForbiddenException if a validation trigger throws an exception.
     * @throws InternalServerErrorException if any other exception occurs.
     */
    private void onStore(Map<String, Object> object) throws ForbiddenException, InternalServerErrorException {
        for (ManagedObjectProperty property : properties) {
            property.onValidate(object);
        }
        execScript(onValidate, object);
// TODO: schema validation here (w. optimizations)
        for (ManagedObjectProperty property : properties) {
            property.onStore(object);
        }
        execScript(onStore, object);
    }

    @Override
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        LOGGER.debug("Create name={} id={}", name, id);
        execScript(onCreate, object);
        onStore(object);
        if (object.containsKey("_id")) { // trigger assigned an identifier
            id = object.get("_id").toString(); // override requested id
        }
        if (id == null) { // default is to assign a UUID identifier
            id = UUID.randomUUID().toString();
            object.put("_id", id);
        }
        service.getRepository().create(repoId(id), object);
        try {
            for (SynchronizationListener listener : listeners) {
                listener.onCreate(routeId(id), object);
            }
        }
        catch (SynchronizationException se) {
// TODO: invert action to provide undo-like functionality
            throw new InternalServerErrorException(se);
        }
    }

    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        LOGGER.debug("Read name={} id={}", name, id);
        Map<String, Object> object = service.getRepository().read(repoId(id));
        onRetrieve(object);
        execScript(onRead, object);
        return object;
    }

    @Override
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        LOGGER.debug("Update {} ", "name=" + name + " id=" + id + " rev=" + rev);
        Map<String, Object> oldObject = service.getRepository().read(repoId(id));
        if (onUpdate != null) {
            HashMap<String, Object> scope = new HashMap<String, Object>();
            scope.put("oldObject", oldObject);
            scope.put("newObject", object);
            try {
                onUpdate.exec(scope); // allows direct modification to the objects
            }
            catch (ScriptThrownException ste) {
                throw new ForbiddenException(ste.getValue().toString()); // script aborting the trigger
            }
            catch (ScriptException se) {
                throw new InternalServerErrorException(se.getMessage());
            }
        }
        onStore(object);
        service.getRepository().update(repoId(id), rev, object);
        try {
            for (SynchronizationListener listener : listeners) {
                listener.onUpdate(routeId(id), oldObject, object);
            }
        }
        catch (SynchronizationException se) {
// TODO: invert action to provide undo-like functionality
            throw new InternalServerErrorException(se);
        }
    }

    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        LOGGER.debug("Delete {} ", "name=" + name + " id=" + id + " rev=" + rev);
        if (onDelete != null) {
            Map<String, Object> object = service.getRepository().read(repoId(id));
            execScript(onDelete, object);
        }
        service.getRepository().delete(repoId(id), rev);
        try {
            for (SynchronizationListener listener : listeners) {
                listener.onDelete(routeId(id));
            }
        }
        catch (SynchronizationException se) {
// TODO: invert action to provide undo-like functionality
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
        return service.getRepository().query(repoId(id), params);
// TODO: provide trigger to filter query results?
    }

    /**
     * Returns the name of the managed object set.
     */
    public String getName() {
        return name;
    }
}
