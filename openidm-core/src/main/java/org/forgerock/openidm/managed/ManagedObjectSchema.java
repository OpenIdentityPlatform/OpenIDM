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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a managed object's schema
 */
public class ManagedObjectSchema {

    /**
     * Setup logging for the {@link ManagedObjectSchema}.
     */
    private static final Logger logger = LoggerFactory.getLogger(ManagedObjectSchema.class);

    /** 
     * The schema to use to validate the structure and content of the managed object. 
     */
    private final Map<JsonPointer, SchemaField> fields;

    /** 
     * The schema to use to validate the structure and content of the managed object.
     */
    private final List<JsonPointer> relationshipFields;

    /** 
     * The schema to use to validate the structure and content of the managed object. 
     */
    private final Map<JsonPointer, SchemaField> hiddenByDefaultFields;
    
    public ManagedObjectSchema(JsonValue schema, ScriptRegistry scriptRegistry, CryptoService cryptoService) 
            throws JsonValueException, ScriptException {
        JsonValue schemaProperties = schema.get("properties").expect(Map.class);
        fields = new LinkedHashMap<>();
        relationshipFields = new ArrayList<>();
        hiddenByDefaultFields = new LinkedHashMap<>();
        if (!schemaProperties.isNull()) {
            for (String propertyKey : schemaProperties.keys()) {
                SchemaField schemaField = new SchemaField(propertyKey, schemaProperties.get(propertyKey), 
                        scriptRegistry, cryptoService);
                fields.put(new JsonPointer(propertyKey), schemaField);
                if (!schemaField.isReturnedByDefault()) {
                    logger.debug("Field {} is not returned by default", propertyKey);
                    hiddenByDefaultFields.put(new JsonPointer(propertyKey), schemaField);
                }
                if (schemaField.isRelationship()) {
                    relationshipFields.add(new JsonPointer(propertyKey));
                }
            }
        }
    }

    /**
     * Returns a {@link Map} containing the schema field names and their corresponding {@link SchemaField} object.
     * 
     * @return a map of the schema fields.
     */
    public Map<JsonPointer, SchemaField> getFields() {
        return fields;
    }

    /**
     * Returns a {@link SchemaField} representing a schema field corresponding to the supplied field name.
     * 
     * @param field a {@link JsonPointer} representing a field name.
     * @return a {@link SchemaField} representing a schema field.
     */
    public SchemaField getField(JsonPointer field) {
        return fields.get(field);
    }

    /**
     * Returns a boolean indicating if the supplied {@link JsonPointer} is a field declared in the schema.
     * 
     * @return true if the field is declared in the schema, false otherwise.
     */
    public boolean hasField(JsonPointer field) {
        return fields.containsKey(field);
    }

    /**
     * Returns a {@link List} of the relationship fields.
     * 
     * @return a list of relationship fields
     */
    public List<JsonPointer> getRelationshipFields() {
        return relationshipFields;
    }

    /**
     * Returns a {@link JsonValue} object representing a map of the fields that are hidden by default.
     * All relationship and virtual fields will be hidden by default unless the returnByDefault flag is set to true.
     * 
     * @return a map of fields that are hidden by default.
     */
    public Map<JsonPointer, SchemaField> getHiddenByDefaultFields() {
        return hiddenByDefaultFields;
    }

    /**
     * Determines if the supplied {@link JsonPointer} represents a resource expanded field name or a relationship 
     * field, and if so, returns a {@link Pair} representing the relationship field's name on the left and the 
     * expanded resource's field name on the right.
     * 
     * @param field a {@link JsonPointer} representing a field name.
     * @return a {@link Pair} representing the relationship field's name on the left and the 
     * expanded resource's field name on the right, or null if the field is not an expanded field.
     */
    public Pair<JsonPointer, JsonPointer> getResourceExpansionField(JsonPointer field) {
        for (JsonPointer relationshipField : relationshipFields) {
            JsonPointer fieldToMatch = field;
            boolean matches = true;
            for (String relationshipFieldToken : relationshipField.toArray()) {
                if (!relationshipFieldToken.equals(fieldToMatch.get(0))) {
                    matches = false;
                    break;
                }
                fieldToMatch = field.relativePointer();
            }
            if (matches) {
            	if (fields.get(relationshipField).isArray()) {
            		// The field is an Array, check if it is followed by an "*", to indicate field expansion
            		if (fieldToMatch.get(0).equals("*")) {
            			if (fieldToMatch.toArray().length > 1) {
            				// Return the remaining field name
            				return Pair.of(relationshipField, fieldToMatch.relativePointer());
            			} else if (fieldToMatch.toArray().length == 1) {
            				// Return all fields "*"
            				return Pair.of(relationshipField, fieldToMatch);
            			} 
            		} else {
            			// No expansion
            			return null;
            		}
            	} else {
            		// The field is not an array, check if it has any remaining field name to indicate field expansion
            		if (fieldToMatch.toArray().length > 0) {
        				return Pair.of(relationshipField, fieldToMatch);
            		} else {
            			// No expansion
            			return null;
            		}
            	}
            }
        }
        return null;
    }

    /**
     * Returns true if the fieldIndexPointer refers to an index of an array field: ie 'roles/0'.  It would not
     * match on field expansions like 'roles/*&#47;description'
     *
     * @param fieldIndexPointer the possible pointer to an index of an array field.
     * @return true if the fieldIndexPointer refers to an index of an array field
     */
    public boolean hasArrayIndexedField(JsonPointer fieldIndexPointer) {
        return fieldIndexPointer.size() == 2
                && hasField(fieldIndexPointer.parent())
                && fieldIndexPointer.leaf().matches("[0-9]+")
                && getField(fieldIndexPointer.parent()).isArray();
    }
}
