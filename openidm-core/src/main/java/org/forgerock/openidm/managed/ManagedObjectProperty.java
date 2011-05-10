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
import java.util.HashMap;
import java.util.Map;

// JSON-Fluent library
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

// ForgeRock OpenIDM
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.Scripts;
import org.forgerock.openidm.script.ScriptThrownException;

/**
 * A property defined within an managed object. Provides for the specification of triggers
 * to execute during the lifecycle of a managed object.
 *
 * @author Paul C. Bryan
 */
class ManagedObjectProperty {

    /** The name of the property within the managed object. */
    private String name;

    /** Script to execute when a property requires validation. */
    private Script onValidate;

    /** Script to execute once an property is retrieved from the repository. */
    private Script onRetrieve;

    /** Script to execute when an property is about to be stored in the repository. */
    private Script onStore;

    /**
     * Constructs a new managed object property.
     *
     * @param config configuration object to use to initialize managed object property.
     * @throws JsonNodeException if the configuration is malformed.
     */
    public ManagedObjectProperty(JsonNode config) throws JsonNodeException {
        name = config.get("name").required().asString();
        onRetrieve = Scripts.newInstance(config.get("onRetrieve"));
        onStore = Scripts.newInstance(config.get("onStore"));
        onValidate = Scripts.newInstance(config.get("onValidate"));
    }

    /**
     * Executes a script that performs transformation of a property. Populates the
     * {@code "property"} property in the script scope with the property value. Any changes
     * to the scope are reflected back into the managed object once the script successfully
     * completes.
     *
     * @param script the script to execute, or {@code null} to execute nothing.
     * @param object the object containing the property value.
     * @throws InternalServerErrorException if script execution fails.
     */
    private void execTransformation(Script script, Map<String, Object> object) throws InternalServerErrorException {
        HashMap<String, Object> scope = new HashMap<String, Object>();
        scope.put("property", object.get(name));
        try {
            script.exec(scope);
        }
        catch (ScriptException se) {
            throw new InternalServerErrorException(se.getMessage());
        }
        if (scope.containsKey("property")) { // property (still) defined in scope
            object.put(name, scope.get("property")); // propagate it back to managed object
        }
        else { // property was removed from scope
            if (object.containsKey(name)) { // was defined in object
                object.remove(name); // remove it from object
            }
        }
    }

    /**
     * Executes the script if it exists, to validate a property value.
     *
     * @param object the object containing the property value to be validated.
     * @throws ForbiddenException if validation of the property fails.
     * @throws InternalServerErrorException if any other exception occurs during execution.
     */
    public void onValidate(Map<String, Object> object) throws ForbiddenException, InternalServerErrorException {
        if (onValidate != null) {
            HashMap<String, Object> scope = new HashMap<String, Object>();
            scope.put("property", object.get(name));
            try {
                onValidate.exec(scope);
            }
            catch (ScriptThrownException ste) {
                throw new ForbiddenException(ste.getValue().toString()); // validation failed
            }
            catch (ScriptException se) {
                throw new InternalServerErrorException(se.getMessage()); // other scripting error
            }
        }
    }

    /**
     * Executes the script if it exists, for when a property is retrieved from the repository.
     *
     * @param object the object that was retrieved from the repository.
     * @throws InternalServerErrorException if an exception occurs executing the script.
     */
    public void onRetrieve(Map<String, Object> object) throws InternalServerErrorException {
        execTransformation(onRetrieve, object);
    }

    /**
     * Executes the script if it exists, for when a property is to be stored in the repository.
     *
     * @param object the object is to be stored in the repository.
     * @throws InternalServerErrorException if an exception occurs executing the script.
     */
    public void onStore(Map<String, Object> object) throws InternalServerErrorException {
        execTransformation(onStore, object);
    }

    /**
     * Returns the name of the property.
     */
    public String getName() {
        return name;
    }
}
