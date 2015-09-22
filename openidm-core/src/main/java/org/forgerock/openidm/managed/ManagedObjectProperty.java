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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions copyright 2011-2015 ForgeRock AS.
 */
package org.forgerock.openidm.managed;

import javax.script.ScriptException;

import org.forgerock.services.context.Context;
import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.crypto.JsonEncryptor;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.exception.ScriptThrownException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A property defined within an managed object. Provides for the specification
 * of triggers to execute during the lifecycle of a managed object.
 */
class ManagedObjectProperty {

    private static enum Scope {
        PUBLIC, PRIVATE
    }
    
    private static enum PropertyType {
        VIRTUAL, MEMBER
    }

    /**
     * Setup logging for the {@link ManagedObjectProperty}.
     */
    private final static Logger logger = LoggerFactory.getLogger(ManagedObjectProperty.class);

    /** The name of the property within the managed object. */
    private final String name;

    /** Script to execute when a property requires validation. */
    private final ScriptEntry onValidate;

    /** Script to execute once an property is retrieved from the repository. */
    private final ScriptEntry onRetrieve;

    /**
     * Script to execute when an property is about to be stored in the
     * repository.
     */
    private final ScriptEntry onStore;

    /** TODO: Description. */
    private JsonEncryptor encryptor;

    /** String that indicates the privacy level of the property. */
    private final Scope scope;
    
    /** String that indicates the property type, such as a virtual property. */
    private final PropertyType type;
    
    /** The CryptoService implementation */
    private CryptoService cryptoService;
    
    /** The encryption configuration */
    private JsonValue encryptionValue;
    
    /** The hashing configuration */
    private JsonValue hashingValue;

    /**
     * Constructs a new managed object property.
     *
     * @param scriptRegistry
     * @param cryptoService
     * @param config
     *            configuration object to use to initialize managed object
     *            property.
     * @throws JsonValueException
     *             when the configuration is malformed
     * @throws ScriptException
     *             when the script configuration is malformed or the script is
     *             invalid.
     */
    public ManagedObjectProperty(final ScriptRegistry scriptRegistry,
            final CryptoService cryptoService, JsonValue config) throws JsonValueException,
            ScriptException {
        this.cryptoService = cryptoService;
        name = config.get("name").required().asString();
        if (config.isDefined("onRetrieve")) {
            onRetrieve = scriptRegistry.takeScript(config.get("onRetrieve"));
        } else {
            onRetrieve = null;
        }
        if (config.isDefined("onStore")) {
            onStore = scriptRegistry.takeScript(config.get("onStore"));
        } else {
            onStore = null;
        }
        if (config.isDefined("onValidate")) {
            onValidate = scriptRegistry.takeScript(config.get("onValidate"));
        } else {
            onValidate = null;
        }
        encryptionValue = config.get("encryption");
        if (!encryptionValue.isNull()) {
            setEncryptor();
        }
        hashingValue = config.get("secureHash");
        if (!hashingValue.isNull()) {
            hashingValue.get("algorithm").required();
        }

        scope = config.get("scope").defaultTo(Scope.PUBLIC.name()).asEnum(Scope.class);
        
        type = config.get("type").defaultTo(PropertyType.MEMBER.name()).asEnum(PropertyType.class);
    }

    /**
     * A synchronized method for setting the excryptor is if hasn't already been set and 
     * there exists an encryption configuration.
     */
    private synchronized void setEncryptor() {
        if (encryptor == null && !encryptionValue.isNull()) {
            try {
                encryptor = cryptoService.getEncryptor(
                        encryptionValue.get("cipher").defaultTo("AES/CBC/PKCS5Padding").asString(),
                        encryptionValue.get("key").required().asString());
            } catch (JsonCryptoException jce) {
                logger.warn("Unable to set encryptor");
            }
        }
    }

    /**
     * Executes a script that performs a transformation of a property. Populates
     * the {@code "property"} property in the script scope with the property
     * value. Any changes to the property are reflected back into the managed
     * object if the script successfully completes.
     *
     * @param type
     *            type of script to execute.
     * @param script
     *            the script to execute, or {@code null} to execute nothing.
     * @param managedObject
     *            the managed object containing the property value.
     * @throws InternalServerErrorException
     *             if script execution fails.
     */
    private void execScript(Context context, String type, ScriptEntry script,
            JsonValue managedObject) throws InternalServerErrorException {
        try {
            if (script != null) {
                Object result = null;
                Script scope = script.getScript(context);
                scope.put("property", managedObject.get(name).getObject());
                scope.put("propertyName", name);
                scope.put("object", managedObject.getObject());
                try {
                    result = scope.eval();
                } catch (ScriptException se) {
                    String msg = name + " " + type + " script encountered exception";
                    logger.debug(msg, se);
                    throw new InternalServerErrorException(msg, se);
                }

                logger.debug("Script {} result: {}", context, result);
                managedObject.put(name, result);
            }
        } catch (InternalServerErrorException ex ) {
            // This logging can be removed once there is a product wide logging of internal failures
            logger.warn("Failure in invoking script " + type + " on " + name + ": " 
                    + ex.getMessage(), ex);
        }
    }

    /**
     * Executes the script if it exists, to validate a property value.
     *
     * @param value
     *            the JSON value containing the property value to be validated.
     * @throws ForbiddenException
     *             if validation of the property fails.
     * @throws InternalServerErrorException
     *             if any other exception occurs during execution.
     */
    void onValidate(Context context, JsonValue value) throws ForbiddenException,
            InternalServerErrorException {
        if (onValidate != null) {
            Script scope = onValidate.getScript(context);
            scope.put("property", value.get(name).getObject());
            try {
                scope.eval();
            } catch (ScriptThrownException ste) {
                // validation failed
                throw new ForbiddenException(ste.getValue().toString());
            } catch (ScriptException se) {
                String msg = name + " onValidate script encountered exception";
                logger.debug(msg, se);
                throw new InternalServerErrorException(msg, se);
            }
        }
    }

    /**
     * Performs tasks when a property has been retrieved from the repository,
     * including: executing the {@code onRetrieve} script.
     *
     * @param value
     *            the JSON value that was retrieved from the repository.
     * @throws InternalServerErrorException
     *             if an exception occurs processing the property.
     */
    void onRetrieve(Context context, JsonValue value) throws InternalServerErrorException {
        execScript(context, "onRetrieve", onRetrieve, value);
    }

    /**
     * Performs tasks when a property is to be stored in the repository, including: executing 
     * the {@code onStore} script and encrypting or hashing the property.
     *
     * @param value
     *            the JSON value to be stored in the repository.
     * @throws InternalServerErrorException
     *             if an exception occurs processing the property.
     */
    void onStore(Context context, JsonValue value) throws InternalServerErrorException {
        execScript(context, "onStore", onStore, value);
        setEncryptor();
        try {
            if (encryptor != null && value.isDefined(name)) {
                if (!cryptoService.isEncrypted(value)) {
                    value.put(name, new JsonCrypto(encryptor.getType(), 
                            encryptor.encrypt(value.get(name))).toJsonValue());
                } 
            } else if (!hashingValue.isNull() && value.isDefined(name)) {
                // Hash the field if not already hashed
                if (!cryptoService.isEncrypted(value)) {
                    String algorithm = hashingValue.get("algorithm").asString();
                    value.put(name, cryptoService.hash(value.get(name), algorithm));
                }
            }
        } catch (JsonCryptoException jce) {
            String msg = name + " property encryption exception";
            logger.debug(msg, jce);
            throw new InternalServerErrorException(msg, jce);
        } catch (JsonException je) {
            String msg = name + " property transformation exception";
            logger.debug(msg, je);
            throw new InternalServerErrorException(msg, je);
        }
    }

    /**
     * Returns the name of the property.
     *
     * @return
     */
    String getName() {
        return name;
    }

    /**
     * Returns a boolean indicating if the property is private.
     * 
     * @return true if the property is private, false otherwise.
     */
    boolean isPrivate() {
        return Scope.PRIVATE.equals(scope);
    }

    /**
     * Returns a boolean indicating if the property is virtual.
     * 
     * @return true if the property is virtual, false otherwise.
     */
    boolean isVirtual() {
        return PropertyType.VIRTUAL.equals(type);
    }
}
