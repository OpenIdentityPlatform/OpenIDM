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

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

/**
 * Represents a single field or property in a managed object's schema
 */
public class SchemaField {

    public static JsonPointer FIELD_ALL_RELATIONSHIPS = new JsonPointer("*_ref");
    public static JsonPointer FIELD_REFERENCE = new JsonPointer("_ref");
    public static JsonPointer FIELD_PROPERTIES = new JsonPointer("_refProperties");

    /** Schema field types */
    enum SchemaFieldType {
        CORE, 
        RELATIONSHIP
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
    
    /**
     * Constructor
     */
    SchemaField(String name, JsonValue schema) {
        this.name = name;
        initialize(schema);
    }
    
    /**
     * Initializes the schema field.
     * 
     * @param schema
     *             a JSON object describing the schema field.
     * @throws JsonValueException
     *             when error is encountered while parsing the JSON object.
     */
    private void initialize(JsonValue schema) throws JsonValueException {
        JsonValue type = schema.get("type");
        if (type.isString() && type.asString().equals("array")) {
            isArray = true;
            initialize(schema.get("items"));
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
            
            // Check if the field is a virtual field
            this.virtual = schema.get("isVirtual").defaultTo(false).asBoolean();
            // Set the returnByDefault value for non-core fields
            if (isRelationship() || isVirtual()) {
                this.returnByDefault = schema.get("returnByDefault").defaultTo(false).asBoolean();
            }
        }
    }
    
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
}
