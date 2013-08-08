package org.forgerock.openidm.sync.impl;

import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
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

    public static Script newInstance(String id, JsonValue config) throws JsonValueException {
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
