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

// JSON-Fluent
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

/**
 * Instantiates a script object, based-on the supplied configuration node. Each scripting
 * language implementation should implement this interface and add its class name to the
 * {@code META-INF/services/org.forgerock.openidm.script.ScriptFactory} file.
 *
 * @author Paul C. Bryan
 */
public interface ScriptFactory {

    /**
     * Returns a new script object for the provided script configuration node. If the
     * factory does not match the configuration (e.g. different {@code "type"} property,
     * then {@code null} is returned.
     * <p>
     * The configuration node must be a map, and have a {@code "type"} string property, which
     * contains the media type of the script (e.g. {@code "text/javascript"}). Implementations
     * of script factories are free to define any other properties in the configuration node.
     *
     * @param config the configuration node for the script; must be a map.
     * @return a new script instance, or {@code null} if the factory could not create it.
     * @throws JsonNodeException if the configuration object is malformed for the script type.
     * @throws ScriptException if the script in the configuration object is malformed.
     */
    Script newInstance(JsonNode config) throws JsonNodeException, ScriptException;
}
