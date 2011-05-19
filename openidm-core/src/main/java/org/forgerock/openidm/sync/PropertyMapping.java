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

package org.forgerock.openidm.sync;

// Java Standard Edition
import java.util.HashMap;

// JSON-Fluent library
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.json.fluent.JsonPath;
import org.forgerock.json.fluent.JsonPathException;

// ForgeRock OpenIDM
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.Scripts;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
class PropertyMapping {

    /** TODO: Description. */
    private final JsonPath targetPath;

    /** TODO: Description. */
    private final JsonPath sourcePath;

    /** TODO: Description. */
    private final Script script;

    /** TODO: Description. */
    private final Object defaultValue;

    /**
     * TODO: Description.
     *
     * @param config TODO.
     * @param property TODO.
     * @param required TODO.
     */
    private static JsonPath asPath(JsonNode config, String property, boolean required) throws JsonNodeException {
        JsonNode node = config.get(property);
        if (required) {
            node.required();
        }
        JsonPath result = null;
        if (!node.isNull()) {
            try {
                result = new JsonPath(node.asString());
            } catch (JsonPathException jpe) {
                throw new JsonNodeException(node, jpe);
            }
        }
        return result;
    }

    /**
     * TODO: Description.
     *
     * @param config TODO.
     * @throws JsonNodeException TODO>
     */
    public PropertyMapping(JsonNode config) throws JsonNodeException {
        targetPath = asPath(config, "target", true);
        sourcePath = asPath(config, "source", false);
        script = Scripts.newInstance(config.get("script"));
        defaultValue = config.get("default").getValue();
    }

    /**
     * TODO: Description.
     *
     * @param source TODO.
     * @param target TODO.
     * @throws MappingException TODO.
     */
    public void apply(JsonNode sourceObject, JsonNode targetObject) throws SynchronizationException {
        try {
            Object result = null;
            if (sourcePath != null) { // optional
                JsonNode node = sourcePath.get(sourceObject);
                if (node != null) { // source property value found
                    result = node.getValue();
                }
            }
            if (script != null) { // optional
                HashMap<String, Object> scope = new HashMap<String, Object>();
                scope.put("source", result);
                result = script.exec(scope); // script yields transformation result
            }
            if (result == null) {
                result = defaultValue; // default null if not specified
            }
            targetPath.put(targetObject, result);
        } catch (JsonNodeException jne) { // malformed JSON node for path
            throw new SynchronizationException(jne);
        } catch (ScriptException se) { // script threw an exception
            throw new SynchronizationException(se);
        }
    }
}
