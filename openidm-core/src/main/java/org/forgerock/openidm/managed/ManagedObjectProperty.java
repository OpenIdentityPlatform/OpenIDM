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

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JSON Fluent library
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.json.fluent.JsonTransformer;

// JSON Cryptography library
import org.forgerock.json.crypto.JsonCryptoException;

// OpenIDM
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

    /** TODO: Description. */
    private final static Logger LOGGER = LoggerFactory.getLogger(ManagedObjectProperty.class);

    /** TODO: Description. */
    private ManagedObjectService service;

    /** The name of the property within the managed object. */
    private String name;

    /** Script to execute when a property requires validation. */
    private Script onValidate;

    /** Script to execute once an property is retrieved from the repository. */
    private Script onRetrieve;

    /** Script to execute when an property is about to be stored in the repository. */
    private Script onStore;

    /** TODO: Description. */
    private JsonTransformer encryptionTransformer;

    /**
     * Constructs a new managed object property.
     *
     * @param config configuration object to use to initialize managed object property.
     * @throws JsonNodeException if the configuration is malformed.
     */
    public ManagedObjectProperty(ManagedObjectService service, JsonNode config) throws JsonNodeException {
        this.service = service;
        name = config.get("name").required().asString();
        onRetrieve = Scripts.newInstance(config.get("onRetrieve"));
        onStore = Scripts.newInstance(config.get("onStore"));
        onValidate = Scripts.newInstance(config.get("onValidate"));
        JsonNode encryptionNode = config.get("encryption");
        if (!encryptionNode.isNull()) {
            try {
                encryptionTransformer = service.getCryptoService().getEncryptionTransformer(
                 encryptionNode.get("cipher").defaultTo("AES/CBS/PKCS5Padding").asString(),
                 encryptionNode.get("key").required().asString());
            } catch (JsonCryptoException jce) {
                throw new JsonNodeException(encryptionNode, jce);
            }
        }
    }

    /**
     * Executes a script that performs a transformation of a property. Populates the
     * {@code "property"} property in the script scope with the property value. Any changes
     * to the property are reflected back into the managed object if the script successfully
     * completes.
     *
     * @param type type of script to execute.
     * @param script the script to execute, or {@code null} to execute nothing.
     * @param managedObject the managed object containing the property value.
     * @throws InternalServerErrorException if script execution fails.
     */
    private void execScript(String type, Script script, JsonNode managedObject) throws InternalServerErrorException {
        if (script != null) {
            Map<String, Object> scope = service.newScope();
            scope.put("property", managedObject.get(name).getValue());
            try {
                script.exec(scope);
            } catch (ScriptException se) {
                String msg = name + " " + type + " script encountered exception";
                LOGGER.debug(msg, se);
                throw new InternalServerErrorException(msg, se);
            }
            if (scope.containsKey("property")) { // property (still) defined in scope
                managedObject.put(name, scope.get("property")); // propagate it back to managed object
            } else if (managedObject.isDefined(name)) { // not in scope but was in managed object
                managedObject.remove(name); // remove it from managed object
            }
        }
    }

    /**
     * Executes the script if it exists, to validate a property value.
     *
     * @param node the JSON node containing the property value to be validated.
     * @throws ForbiddenException if validation of the property fails.
     * @throws InternalServerErrorException if any other exception occurs during execution.
     */
    void onValidate(JsonNode node) throws ForbiddenException, InternalServerErrorException {
        if (onValidate != null) {
            Map<String, Object> scope = service.newScope();
            scope.put("property", node.get(name).getValue());
            try {
                onValidate.exec(scope);
            } catch (ScriptThrownException ste) {
                throw new ForbiddenException(ste.getValue().toString()); // validation failed
            } catch (ScriptException se) {
                String msg = name + " onValidate script encountered exception";
                LOGGER.debug(msg, se);
                throw new InternalServerErrorException(msg, se);
            }
        }
    }

    /**
     * Performs tasks when a property has been retrieved from the repository, including:
     * executing the {@code onRetrieve} script.
     *
     * @param node the JSON node that was retrieved from the repository.
     * @throws InternalServerErrorException if an exception occurs processing the property.
     */
    void onRetrieve(JsonNode node) throws InternalServerErrorException {
        execScript("onRetrieve", onRetrieve, node);
    }

    /**
     * Performs tasks when a property is to be stored in the repository, including:
     * executing the {@code onStore} script and encrypting the property.
     *
     * @param object the JSON node to be stored in the repository.
     * @throws InternalServerErrorException if an exception occurs processing the property.
     */
    void onStore(JsonNode node) throws InternalServerErrorException {
        execScript("onStore", onStore, node);
        if (encryptionTransformer != null && node.isDefined(name)) {
            try {
                JsonNode property = node.get(name).copy(); // deep copy; apply all transformations
                encryptionTransformer.transform(property);
                node.put(name, property.getValue());
            } catch (JsonException je) {
                String msg = name + " property encryption exception";
                LOGGER.debug(msg, je);
                throw new InternalServerErrorException(msg, je);
            }
        }
    }

    /**
     * Returns the name of the property.
     */
    String getName() {
        return name;
    }
}
