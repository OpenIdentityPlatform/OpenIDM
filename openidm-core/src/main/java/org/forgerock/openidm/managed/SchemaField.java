/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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

import javax.script.ScriptException;

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.crypto.JsonEncryptor;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.util.RelationshipUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.services.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single field or property in a managed object's schema
 */
public class SchemaField {

    /**
     * Setup logging for the {@link SchemaField}.
     */
    private final static Logger logger = LoggerFactory.getLogger(SchemaField.class);
    
    public static JsonPointer FIELD_ALL_RELATIONSHIPS = new JsonPointer("*" + RelationshipUtil.REFERENCE_ID);
    public static JsonPointer FIELD_REFERENCE = new JsonPointer(RelationshipUtil.REFERENCE_ID);
    public static JsonPointer FIELD_PROPERTIES = new JsonPointer(RelationshipUtil.REFERENCE_PROPERTIES);

    /** Schema field types */
    enum SchemaFieldType {
        CORE, 
        RELATIONSHIP
    }

    /** Schema field scopes */
    private static enum Scope {
        PUBLIC, PRIVATE
    }
    
    /** The field name */
    private String name;

    /** The field type */
    private SchemaFieldType type;

    /** A boolean indicating if the field is returned by default */
    private boolean returnByDefault = true;
    
    /** A boolean indicating if the field is nullable */ 
    private boolean nullable = false;
    
    /** A boolean indicating if the field is virtual */
    private boolean virtual;
    
    /** A boolean indicating if the field is an array */
    private boolean isArray = false;

    /** A boolean indicating if the field is a reverse relationship */
    private boolean isReverseRelationship = false;

    /** Matches against firstPropertyName if this is an inverse relationship */
    private String reversePropertyName;

    /** Indicates if the field will be validated before saving or updating the object */
    private boolean validationRequired = false;
    
    /** The CryptoService implementation */
    private CryptoService cryptoService;
    
    /** The encryption configuration */
    private JsonValue encryptionValue;
    
    /** The hashing configuration */
    private JsonValue hashingValue;

    /** Script to execute when a property requires validation. */
    private final ScriptEntry onValidate;

    /** Script to execute once an property is retrieved from the repository. */
    private final ScriptEntry onRetrieve;

    /** Script to execute when an property is about to be stored in the repository. */
    private final ScriptEntry onStore;

    /** The encryptor to use for encrypting JSON values */
    private JsonEncryptor encryptor;

    /** String that indicates the privacy level of the property. */
    private final Scope scope;

    /**
     * Constructor
     */
    SchemaField(final String name, final JsonValue schema, final ScriptRegistry scriptRegistry,
            final CryptoService cryptoService) throws JsonValueException, ScriptException {
        this.name = name;
        this.cryptoService = cryptoService;
        this.scope = schema.get("scope").defaultTo(Scope.PUBLIC.name()).asEnum(Scope.class);
        
        // Initialize the type
        initializeType(schema);
        
        // Set the onRetrieve script if defined.
        this.onRetrieve = schema.isDefined("onRetrieve")
                ? scriptRegistry.takeScript(schema.get("onRetrieve"))
                : null;
        
        // Set the onStore script if defined.
        this.onStore = schema.isDefined("onStore")
                ? scriptRegistry.takeScript(schema.get("onStore"))
                : null;
        
        // Set the onValidate script if defined.
        this.onValidate = schema.isDefined("onValidate")
                ? scriptRegistry.takeScript(schema.get("onValidate"))
                : null;

        // Check if the field is a virtual field
        this.virtual = schema.get("isVirtual").defaultTo(false).asBoolean();
        // Set the returnByDefault value for non-core fields
        if (isRelationship() || isVirtual()) {
            this.returnByDefault = schema.get("returnByDefault").defaultTo(false).asBoolean();
        }

        // Initialize the encryptor if encryption is defined.
        encryptionValue = schema.get("encryption");
        if (encryptionValue.isNotNull()) {
            setEncryptor();
        }
        
        // Set the hashing value, if a secure hash is defined, and make sure the hashing algorithm is defined.
        hashingValue = schema.get("secureHash");
        if (hashingValue.isNotNull()) {
            hashingValue.get("algorithm").required();
        }
    }
    
    /**
     * Initializes the schema field's type. Recursively calls itself on the "items" schema if the base type is an array.
     * 
     * @param schema a JSON object describing the schema field.
     * @throws JsonValueException when error is encountered while parsing the JSON object.
     */
    private void initializeType(JsonValue schema) throws JsonValueException {
        JsonValue type = schema.get("type");
        if (type.isString() && type.asString().equals("array")) {
            isArray = true;
            initializeType(schema.get("items"));
        } else {
            if (type.isString()) {
                setType(type.asString());
            } else if (type.isList()) {
                for (JsonValue t : type) {
                    setType(t.asString());
                }
            } else {
                throw new JsonValueException(type, "Schema field 'type' must be a String or List");
            }

            if (isRelationship()) {
                this.isReverseRelationship = schema.get("reverseRelationship").defaultTo(false).asBoolean();

                if (this.isReverseRelationship) {
                    this.reversePropertyName = schema.get("reversePropertyName").required().asString();
                }
            }
            // Set validation flag
            this.validationRequired = schema.get("validate").defaultTo(false).asBoolean();
        }
    }
    
    /**
     * A synchronized method for setting the encryptor is if hasn't already been set and there exists an encryption 
     * configuration.
     */
    private synchronized void setEncryptor() {
        if (encryptor == null && encryptionValue.isNotNull()) {
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
     * Sets the type of the schema field.
     * 
     * @param type the type of this schema field
     */
    private void setType(String type) {
        if (type.equals("relationship")) {
            this.type = SchemaFieldType.RELATIONSHIP;
        } else if (type.equals("null")) {
            this.nullable = true;
        } else {
            this.type = SchemaFieldType.CORE;
        }
    }

    /**
     * Returns the type of this schema field.
     * 
     * @return the type of this schema field
     */
    public SchemaFieldType getType() {
        return type;
    }

    /**
     * Returns a boolean indicating if the field is a reverse relationship.
     * 
     * @return true if the relationship is reverse, otherwise false.
     */
    public boolean isReverseRelationship() {
        return isReverseRelationship;
    }

    /**
     * Returns the name used by the reverse relationship.
     * 
     * @return The property name used by the reverse relationship.
     */
    public String getReversePropertyName() {
        return reversePropertyName;
    }

    /**
     * Returns a boolean indicating if the field is returned by default.
     * 
     * @return true if the field is returned by default, false otherwise.
     */
    public boolean isReturnedByDefault() {
        return returnByDefault;
    }
    
    /**
     * Returns a boolean indicating if the field is a relationship.
     * 
     * @return true if the field is a relationship, false otherwise.
     */
    public boolean isRelationship() {
        return type == SchemaFieldType.RELATIONSHIP;
    }
    
    /**
     * Returns a boolean indicating if the field is virtual.
     * 
     * @return true if the field is virtual, false otherwise.
     */
    public boolean isVirtual() {
        return virtual;
    }
    
    /**
     * Returns a boolean indicating if the field is nullable.
     * 
     * @return true if the field is nullable, false otherwise.
     */
    public boolean isNullable() {
        return nullable;
    }
    
    /**
     * Returns a boolean indicating if the field is an array.
     * 
     * @return true if the field is an array, false otherwise.
     */
    public boolean isArray() {
        return isArray;
    }

    /**
     * Returns a String representing the field's name.
     * 
     * @return the field's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns true if the field should be validated before any action is taken on the managed object.
     *
     * @return True if the field should be validated before any action is taken on the managed object.
     */
    public boolean isValidationRequired() {
        return validationRequired;
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
     * Executes a script that performs a transformation of a property. Populates the {@code "property"} property in the
     * script scope with the property value. Any changes to the property are reflected back into the managed object if
     * the script successfully completes.
     *
     * @param type type of script to execute.
     * @param script the script to execute, or {@code null} to execute nothing.
     * @param managedObject the managed object containing the property value.
     * @throws InternalServerErrorException if script execution fails.
     */
    private void execScript(Context context, String type, ScriptEntry script, JsonValue managedObject)
            throws InternalServerErrorException {
        if (script != null) {
            Object result = null;
            Script scope = script.getScript(context);
            scope.put("property", managedObject.get(name).getObject());
            scope.put("propertyName", name);
            scope.put("object", managedObject.getObject());
            scope.put("context", context);
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
    }
    
    /**
     * Executes the script if it exists, to validate a property value.
     *
     * @param value the JSON value containing the property value to be validated.
     * @throws ForbiddenException if validation of the property fails.
     * @throws InternalServerErrorException if any other exception occurs during execution.
     */
    void onValidate(Context context, JsonValue value) throws ForbiddenException, InternalServerErrorException {
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
     * Performs tasks when a property has been retrieved from the repository, including: executing the 
     * {@code onRetrieve} script.
     *
     * @param value the JSON value that was retrieved from the repository.
     * @throws InternalServerErrorException if an exception occurs processing the property.
     */
    void onRetrieve(Context context, JsonValue value) throws InternalServerErrorException {
        execScript(context, "onRetrieve", onRetrieve, value);
    }
    
    /**
     * Performs tasks when a property is to be stored in the repository, including: executing the {@code onStore} script
     * and encrypting or hashing the property.
     *
     * @param value the JSON value to be stored in the repository.
     * @throws InternalServerErrorException if an exception occurs processing the property.
     */
    void onStore(Context context, JsonValue value) throws InternalServerErrorException {
        execScript(context, "onStore", onStore, value);
        setEncryptor();
        try {
            if (value.isDefined(name)) {
                JsonValue propValue = value.get(name);
                if (encryptor != null && !cryptoService.isEncrypted(propValue)) {
                    // Encrypt the field
                    value.put(name, new JsonCrypto(encryptor.getType(),  encryptor.encrypt(propValue)).toJsonValue());

                } else if (hashingValue.isNotNull() && !cryptoService.isHashed(propValue)) {
                    // Hash the field
                    value.put(name, cryptoService.hash(propValue, hashingValue.get("algorithm").asString()));
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
}
