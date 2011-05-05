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

/**
 * Provides access to a set of managed objects of a given type.
 *
 * @author Paul C. Bryan
 */
class ManagedObjectSet implements ObjectSet {

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
     * Creates a new instance of a script.
     *
     * @param config the script configuration object to instantiate the script with.
     * @return the new script object, or {@code null} if configuration was {@code null}. 
     * @throws JsonNodeException if the script configuration object or source is malformed.
     */
    static Script newScript(JsonNode config) throws JsonNodeException {
        return (config == null ? null : Scripts.newInstance(config));
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
        onCreate = newScript(config.get("onCreate"));
        onRead = newScript(config.get("onRead"));
        onUpdate = newScript(config.get("onUpdate"));
        onDelete = newScript(config.get("onDelete"));
        onValidate = newScript(config.get("onValidate"));
        for (JsonNode node : config.get("properties").expect(List.class)) {
            properties.add(new ManagedObjectProperty(node));
        }
    }

    /**
     * Generates a fully-qualified object identifier for the repository. Currently,
     * hard-codes the identifier.
     *
     * @param id the local managed object identifier to qualify.
     * @return the fully-qualified repository object identifier.
     */
    private String repoId(String id) {
        // TODO: make this configurable as not every repository will likely adhere to the same scheme
        return "managed/" + name + "/" + id;
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
        // TODO: schema validation here (w. optimization)
        for (ManagedObjectProperty property : properties) {
            property.onStore(object);
        }
        execScript(onStore, object);
    }

    @Override
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        execScript(onCreate, object);
        onStore(object);
        service.getRepository().create(repoId(id), object);
    }

    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        Map<String, Object> object = service.getRepository().read(repoId(id));
        onRetrieve(object);
        execScript(onRead, object);
        return object;
    }

    @Override
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        if (onUpdate != null) {
            Map<String, Object> oldObject = service.getRepository().read(id);
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
    }

    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        if (onDelete != null) {
            Map<String, Object> object = service.getRepository().read(repoId(id));
            execScript(onDelete, object);
        }
        service.getRepository().delete(repoId(id), rev);
    }

    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new InternalServerErrorException("patch not yet implemented");
    }

    @Override
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        // TODO: yikes, how will triggers redact this?! 
        throw new InternalServerErrorException("query not yet implemented");
    }

    /**
     * Returns the name of the managed object set.
     */
    public String getName() {
        return name;
    }
}
