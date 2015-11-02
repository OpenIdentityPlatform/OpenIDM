package org.forgerock.openidm.script;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.javascript.RhinoScriptEngineFactory;
import org.forgerock.script.registry.ScriptRegistryImpl;

/**
 *
 */
public class RhinoResourceFunctionsTest extends ResourceFunctionsTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected Map<String, Object> getConfiguration() {
        Map<String, Object> configuration = new HashMap<String, Object>(1);
        // configuration.put(RhinoScriptEngine.CONFIG_DEBUG_PROPERTY,
        // "transport=socket,suspend=y,address=9888,trace=true");
        return configuration;
    }

    protected String getLanguageName() {
        return RhinoScriptEngineFactory.LANGUAGE_NAME;
    }

    protected URL getScriptContainer(String name) {
        return RhinoResourceFunctionsTest.class.getResource(name);
    }

    protected ScriptRegistryImpl getScriptRegistry(Map<String, Object> configuration) {
        return new ScriptRegistryImpl(configuration,
                Collections.<ScriptEngineFactory>singleton(new RhinoScriptEngineFactory()), null, null);
    }
}
