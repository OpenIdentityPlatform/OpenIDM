/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.internal.sync;

import java.util.HashMap;

import javax.script.ScriptException;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.exception.ScriptThrownException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Description.
 * 
 * @author Paul C. Bryan
 */
class PropertyMapping {

    /**
     * Setup logging for the {@link PropertyMapping}.
     */
    private final static Logger logger = LoggerFactory.getLogger(PropertyMapping.class);

    /** TODO: Description. */
    private final ScriptEntry condition;

    /** TODO: Description. */
    private final JsonPointer targetPointer;

    /** TODO: Description. */
    private final JsonPointer sourcePointer;

    /** TODO: Description. */
    private final ScriptEntry transform;

    /** TODO: Description. */
    private final Object defaultValue;

    /**
     * TODO: Description.
     * 
     * @param targetObject
     *            TODO.
     * @param pointer
     *            TODO.
     * @param value
     *            TODO.
     * @throws ResourceException
     *             TODO.
     */
    private static void put(JsonValue targetObject, JsonPointer pointer, Object value)
            throws ResourceException {
        String[] tokens = pointer.toArray();
        if (tokens.length == 0) {
            throw new BadRequestException("cannot replace root object");
        }
        JsonValue jv = targetObject;
        for (int n = 0; n < tokens.length - 1; n++) {
            JsonValue child = jv.get(tokens[n]);
            if (child.isNull() && !jv.isDefined(tokens[n])) {
                try {
                    jv.put(tokens[n], new HashMap());
                } catch (JsonValueException jve) {
                    throw new BadRequestException(jve.getMessage(), jve);
                }
                child = jv.get(tokens[n]);
            }
            jv = child;
        }
        try {
            jv.put(tokens[tokens.length - 1], value);
        } catch (JsonValueException jve) {
            throw new BadRequestException(jve.getMessage(), jve);
        }
    }

    /**
     * TODO: Description.
     * 
     * @param service
     * @param config
     *            TODO.
     * @throws JsonValueException
     *             TODO.
     */
    public PropertyMapping(ScriptRegistry service, JsonValue config) throws JsonValueException,
            ScriptException {
        condition = service.takeScript(config.get("condition"));
        targetPointer = config.get("target").required().asPointer();
        sourcePointer = config.get("source").asPointer(); // optional
        transform = service.takeScript(config.get("transform"));
        defaultValue = config.get("default").getObject();
    }

    /**
     * TODO: Description.
     * 
     * @param sourceObject
     *            TODO.
     * @param targetObject
     *            TODO.
     * @throws ResourceException
     *             TODO.
     */
    public void apply(final Context context, JsonValue sourceObject, JsonValue targetObject)
            throws ResourceException {
        if (condition != null) {
            // optional property mapping condition
            Script scope = condition.getScript(context);
            try {
                scope.put("object", sourceObject.copy().asMap());
                Object o = scope.eval();
                if (o == null || !(o instanceof Boolean) || Boolean.FALSE.equals(o)) {
                    return; // property mapping is not applicable; do not apply
                }
            } catch (ScriptThrownException e) {
                logger.warn("Unexpected JSON value exception", e);
                throw e.toResourceException(ResourceException.BAD_REQUEST, e.getMessage());
            } catch (ScriptException se) {
                logger.warn("Property mapping " + targetPointer
                        + " condition script encountered exception", se);
                throw new BadRequestException(se.getMessage(), se);
            }
        }
        Object result = null;
        if (sourcePointer != null) { // optional source property
            JsonValue jv = sourceObject.get(sourcePointer);
            if (jv != null) { // null indicates no value
                result = jv.getObject();
            }
        }
        if (transform != null) { // optional property mapping script
            Script scope = transform.getScript(context);
            scope.put("source", result);
            try {
                // script yields transformation result
                result = scope.eval();
            } catch (ScriptException se) {
                logger.warn("Property mapping " + targetPointer
                        + " transformation script encountered exception", se);
                throw new BadRequestException(se.getMessage(), se);
            }
        }
        if (result == null) {
            result = defaultValue; // remains null if default not specified
        }
        put(targetObject, targetPointer, result);
    }
}
