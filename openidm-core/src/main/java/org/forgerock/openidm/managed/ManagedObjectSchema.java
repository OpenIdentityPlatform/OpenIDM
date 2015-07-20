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

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
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
    private final Map<String, SchemaField> fields;

    /** 
     * The schema to use to validate the structure and content of the managed object.
     */
    private final List<String> relationshipFields;

    /** 
     * The schema to use to validate the structure and content of the managed object. 
     */
    private final JsonValue hiddenByDefaultFields;
    
    public ManagedObjectSchema(JsonValue schema) {
        JsonValue schemaProperties = schema.get("properties").expect(Map.class);
        fields = new HashMap<String, SchemaField>();
        relationshipFields = new ArrayList<String>();
        hiddenByDefaultFields = json(object());
        if (!schemaProperties.isNull()) {
            for (String propertyKey : schemaProperties.keys()) {
                SchemaField schemaField = new SchemaField(propertyKey, schemaProperties.get(propertyKey));
                fields.put(propertyKey, schemaField);
                if (!schemaField.isReturnedByDefault()) {
                    logger.debug("Field {} is not returned by default", propertyKey);
                    hiddenByDefaultFields.put(propertyKey, schemaProperties.get(propertyKey));
                }
                if (schemaField.isRelationship()) {
                    relationshipFields.add(propertyKey);
                }
            }
        }
    }

    /**
     * Returns a {@link Map} containing the schema field names and their corresponding {@link SchemaField} object.
     * 
     * @return a map of the schema fields.
     */
    public Map<String, SchemaField> getFields() {
        return fields;
    }

    /**
     * Returns a {@link List} of the relationship fields.
     * 
     * @return a list of relationship fields
     */
    public List<String> getRelationshipFields() {
        return relationshipFields;
    }

    /**
     * Returns a {@link JsonValue} object representing a map of the fields that are hidden by default.
     * All relationship and virtual fields will be hidden by default unless the returnByDefault flag is set to true.
     * 
     * @return a map of fields that are hidden by default.
     */
    public JsonValue getHiddenByDefaultFields() {
        return hiddenByDefaultFields;
    }
}
