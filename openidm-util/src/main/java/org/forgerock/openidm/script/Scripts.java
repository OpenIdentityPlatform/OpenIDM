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

package org.forgerock.openidm.script;

// Java Standard Edition
import java.util.ServiceLoader;

// JSON-Fluent
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

/**
 * Instantiates script objects using registered script factory implementations.
 *
 * @author Paul C. Bryan
 * @see ScriptFactory
 */
public class Scripts {

    /** Service loader instance for script factories. */
    private static final ServiceLoader<ScriptFactory> FACTORIES = ServiceLoader.load(ScriptFactory.class);

    /**
     * Returns a new script object for the provided script configuration object.
     *
     * @param config configuration object for script.
     * @return a new script instance, or {@code null} if {@code config} is {@code null}.
     * @throws JsonNodeException if the script configuration object or source is malformed.
     */
    public static Script newInstance(JsonNode config) throws JsonNodeException {
        if (config == null || config.isNull()) {
            return null;
        }
        for (ScriptFactory factory : FACTORIES) {
            Script script = factory.newInstance(config);
            if (script != null) {
                return script;
            }
        }
        JsonNode type = config.get("type");
        throw new JsonNodeException(type, "script type " + type.asString() + " unsupported"); // no matching factory
    }
}
