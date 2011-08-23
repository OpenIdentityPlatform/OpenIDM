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

package org.forgerock.openidm.script.javascript;

// Java Standard Edition
import java.io.File;

// JSON-Fluent
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

// OpenIDM
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.ScriptFactory;

/**
 * Implementation of a script factory for JavaScript.
 * <p>
 * Expects {@code "type"} configuration property of {@code "text/javascript"} and
 * {@code "source"} property, which contains the script source code.
 * <p>
 * The optional boolean property {@code "sharedScope"} indicates if a shared scope should be
 * used. If {@code true}, a sealed shared scope containing standard JavaScript objects
 * (Object, String, Number, Date, etc.) will be used for script execution rather than
 * allocating a new unsealed scope for each execution.  
 *
 * @author Paul C. Bryan
 */
public class JavaScriptFactory implements ScriptFactory {

    @Override
    public Script newInstance(JsonNode config) throws JsonNodeException {
        String type = config.get("type").asString();
        if (type != null && type.equalsIgnoreCase("text/javascript")) {
            boolean sharedScope = config.get("sharedScope").defaultTo(true).asBoolean();
            if (config.isDefined("source")) {
                try {
                    return new JavaScript(config.get("source").asString(), sharedScope);
                } catch (ScriptException se) { // re-cast to show exact node of failure 
                    throw new JsonNodeException(config.get("source"), se);
                }
            } else if (config.isDefined("file")) { // TEMPORARY
                try {
                    return new JavaScript(IdentityServer.getFileForPath(config.get("file").asString()), sharedScope);
                } catch (ScriptException se) { // re-cast to show exact node of failure 
                    throw new JsonNodeException(config.get("file"), se);
                }
            } else {
                throw new JsonNodeException(config, "expected 'source' or 'file' property");
            }
        }
        return null;
    }
}

