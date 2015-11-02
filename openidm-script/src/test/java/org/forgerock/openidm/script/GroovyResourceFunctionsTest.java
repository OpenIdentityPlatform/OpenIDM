package org.forgerock.openidm.script;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.script.ScriptName;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.groovy.GroovyScriptEngineFactory;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.script.source.EmbeddedScriptSource;
import org.testng.annotations.Test;


/**
 * Created by brmiller on 11/2/15.
 */
@Test
public class GroovyResourceFunctionsTest extends ResourceFunctionsTest {

    protected Map<String, Object> getConfiguration() {
        return new HashMap<>();
    }

    protected String getLanguageName() {
        return GroovyScriptEngineFactory.LANGUAGE_NAME;
    }

    protected URL getScriptContainer(String name) {
        return GroovyResourceFunctionsTest.class.getResource(name);
    }

    protected EmbeddedScriptSource getScriptSourceWithException() {
        ScriptName scriptName =
                new ScriptName("exception", GroovyScriptEngineFactory.LANGUAGE_NAME);
        return new EmbeddedScriptSource("throw new Exception(\"Access denied\");", scriptName);
    }

    protected ScriptRegistryImpl getScriptRegistry(Map<String, Object> configuration) {
        return new ScriptRegistryImpl(configuration,
                Collections.<ScriptEngineFactory>singleton(new GroovyScriptEngineFactory()), null, null);
    }
}
