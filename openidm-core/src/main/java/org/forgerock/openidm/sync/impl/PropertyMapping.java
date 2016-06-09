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
 * Portions copyright 2011-2016 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValueFunctions.pointer;


// Java SE
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


// SLF4J
import org.forgerock.openidm.condition.Conditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JSON Fluent
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.JsonPointer;

import javax.script.ScriptException;

import org.forgerock.openidm.util.Script;
import org.forgerock.openidm.util.Scripts;
import org.forgerock.services.context.Context;
import org.forgerock.openidm.condition.Condition;

/**
 * This class contains the necessary logic to map an attribute from the source object to an attribute
 * on the target object.  It optionally contains a condition and transform scripts.. 
 */
class PropertyMapping {

    /** Logger */
    private final static Logger LOGGER = LoggerFactory.getLogger(PropertyMapping.class);

    /** A condition script */
    private final Condition condition;

    /** A transform script */
    private final Script transform;
    
    /** A {@link JsonPointer} for the target */
    private final JsonPointer targetPointer;

    /** A {@link JsonPointer} for the source */
    private final JsonPointer sourcePointer;

    /** A default value */
    private final Object defaultValue;
    
    /**
     * Constructor
     *
     * @param config a {@link JsonValue} representing the property mapping configuration.
     * @throws JsonValueException if any errors are encountered when processing the configuration.
     */
    public PropertyMapping(JsonValue config) throws JsonValueException {
        condition = Conditions.newCondition(config.get("condition"));
        targetPointer = config.get("target").required().as(pointer());
        sourcePointer = config.get("source").as(pointer()); // optional
        transform = Scripts.newScript(config.get("transform"));
        defaultValue = config.get("default").getObject();
    }

    /**
     * Puts the value on the target object attribute.
     *
     * @param targetObject the target object.
     * @param pointer the target attribute.
     * @param value the target attribute's value.
     * @throws SynchronizationException if errors are encountered.
     */
    protected static void put(JsonValue targetObject, JsonPointer pointer, Object value) throws SynchronizationException {
        String[] tokens = pointer.toArray();
        if (tokens.length == 0) {
            throw new SynchronizationException("cannot replace root object");
        }
        JsonValue jv = targetObject;
        for (int n = 0; n < tokens.length - 1; n++) {
            JsonValue child = jv.get(tokens[n]);
            if (child.isNull() && !jv.isDefined(tokens[n])) {
                try {
                    jv.put(tokens[n], new LinkedHashMap<String, Object>());
                } catch (JsonValueException jve) {
                    throw new SynchronizationException(jve);
                }
                child = jv.get(tokens[n]);
            }
            jv = child;
        }
        try {
            jv.put(tokens[tokens.length - 1], value);
        } catch (JsonValueException jve) {
            throw new SynchronizationException(jve);
        }
    }

    /**
     * Applies this property mapping on the supplied source and target objects if no link qualifier has been configured,
     * or the supplied link qualifier matches the configure one.  This may execute condition and transform scripts if 
     * any are configured.
     *
     * @param sourceObject Current specified source property/object to map from
     * @param oldSource oldSource an optional previous source object before the change(s) that triggered the sync, null
     * if not provided
     * @param targetObject Current specified target property/object to modify
     * @param oldTarget Existing, unmodified, target object
     * @param linkQualifier the link qualifier associated with the current sync
     * @param context a {@link Context} associated with this call
     * @throws SynchronizationException if errors are encountered.
     */
    public void apply(JsonValue sourceObject, JsonValue oldSource, JsonValue targetObject, JsonValue oldTarget, String linkQualifier,
            Context context) throws SynchronizationException {
        // optional property mapping condition
        if (!evaluateCondition(sourceObject, oldSource, targetObject, oldTarget, linkQualifier, context)) {
            return;
        }
        Object result = null;
        if (sourcePointer != null) { // optional source property
            JsonValue jv = sourceObject.get(sourcePointer);
            if (jv != null) { // null indicates no value
                result = jv.getObject();
            }
        }
        if (transform != null) { // optional property mapping script
            Map<String, Object> scope = new HashMap<String, Object>();
            scope.put("source", result);
            scope.put("linkQualifier", linkQualifier);
            try {
                result = transform.exec(scope, context); // script yields transformation result
            } catch (ScriptException se) {
                LOGGER.warn("Property mapping " + targetPointer + " transformation script encountered exception", se);
                throw new SynchronizationException("Transformation script error :  " + se.getMessage() + 
                        " for attribute '" + targetPointer + "'");
            }
        }
        if (result == null) {
            result = defaultValue; // remains null if default not specified
        }
        put(targetObject, targetPointer, result);
    }
    
    /**
     * Evaluates a property mapping condition.  Returns true if the condition passes, false otherwise.
     * 
     * @param sourceObject Current specified source property/object to map from
     * @param oldSource oldSource an optional previous source object before the change(s) that triggered the sync, null
     * if not provided
     * @param targetObject Current specified target property/object to modify
     * @param linkQualifier the link qualifier associated with the current sync
     * @param context a {@link Context} associated with this call
     * @return true if the condition passes, false otherwise.
     * @throws SynchronizationException if errors are encountered.
     */
    public boolean evaluateCondition(JsonValue sourceObject, JsonValue oldSource, JsonValue targetObject, JsonValue oldTarget,
            String linkQualifier, Context context) throws SynchronizationException {
        JsonValue params = json(object(field("object", sourceObject), field("target", targetObject), field("linkQualifier", linkQualifier)));
        if (oldSource != null) {
            params.put("oldSource", oldSource);
        }
        if (oldTarget != null) {
            params.put("oldTarget", oldTarget);
        }
        return condition.evaluate(params, context);
    }
}
