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
 * Portions copyright 2011-2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;

/**
 * A NAME does ...
 *
 */
public class Scripts {

    private static Scripts instance = null;

    private final ScriptRegistry registry;

    public static class Script {

        private final ScriptEntry entry;

        private Script(ScriptEntry e) {
            entry = e;
        }

        Object exec(Map<String, Object> scope) throws ScriptException {
            org.forgerock.script.Script s = entry.getScript(ObjectSetContext.get());
            Bindings b = s.createBindings();
            b.putAll(scope);
            return s.eval(b);
        };
    }

    static void init(ScriptRegistry registry) {
        instance = new Scripts(registry);
    }

    private Scripts(ScriptRegistry registry) {
        this.registry = registry;
    }

    public static Script newInstance(JsonValue config) throws JsonValueException {
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
