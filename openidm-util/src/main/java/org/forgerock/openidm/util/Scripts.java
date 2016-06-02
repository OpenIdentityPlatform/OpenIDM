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
package org.forgerock.openidm.util;

import javax.script.ScriptException;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.script.ScriptRegistry;

/**
 * A singleton utility class that provides static methods for retrieving scripts from the {@link ScriptRegistry}.
 */
public class Scripts {

    private static volatile Scripts instance = null;

    private final ScriptRegistry registry;

    private Scripts(ScriptRegistry registry) {
        this.registry = registry;
    }

    /**
     * Initializes the {@link ScriptRegistry} service.
     * 
     * @param registry the {@link ScriptRegistry} service.
     */
    public static void init(ScriptRegistry registry) {
        instance = new Scripts(registry);
    }

    /**
     * Returns a new {@link Script} object representing a {@link ScriptEntry} from the {@link ScriptRegistry} service 
     * based on the passed in script configuration.  Returns null if the passed is script configuration is null.
     * 
     * @param config a script configuration.
     * @return a {@link Script} object representing a {@link ScriptEntry} from the {@link ScriptRegistry} service.
     * @throws JsonValueException
     */
    public static Script newScript(JsonValue config) throws JsonValueException {
        if (config == null || config.isNull()) {
            return null;
        }
        try {
            return new Script(instance.registry.takeScript(config));
        } catch (ScriptException e) {
            throw new JsonValueException(config, e);
        }
    }
}
