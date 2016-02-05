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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.bin.defaults.script;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.JsonValue;
import org.forgerock.script.source.DirectoryContainer;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.javascript.RhinoScriptEngineFactory;

import javax.script.ScriptException;

/**
 * Tests the file policyFilter.js.
 *
 * Currently only getFullResourcePath function is tested.
 */
public class PolicyFilterTest  {

    private ScriptRegistryImpl scriptRegistry = null;

    protected Map<String, Object> getConfiguration() {
        Map<String, Object> configuration = new HashMap<>(1);
        // configuration.put(RhinoScriptEngine.CONFIG_DEBUG_PROPERTY,
        // "transport=socket,suspend=y,address=9888,trace=true");
        return configuration;
    }

    protected String getLanguageName() {
        return RhinoScriptEngineFactory.LANGUAGE_NAME;
    }

    protected ScriptRegistryImpl getScriptRegistry(Map<String, Object> configuration) {
        return new ScriptRegistryImpl(configuration,
                Collections.<ScriptEngineFactory>singleton(new RhinoScriptEngineFactory()), null, null);
    }

    protected URL getScriptContainer(String name) {
        return PolicyFilterTest.class.getResource(name);
    }

    @BeforeClass
    public void initScriptRegistry() throws Exception {
        Map<String, Object> configuration = new HashMap<>(1);
        configuration.put(getLanguageName(), getConfiguration());

        scriptRegistry = getScriptRegistry(configuration);

        URL container = getScriptContainer("/bin/defaults/script/");
        Assert.assertNotNull(container);

        scriptRegistry.addSourceUnit(new DirectoryContainer("bin/defaults/script", container));
    }

    public ScriptRegistry getScriptRegistry() {
        return scriptRegistry;
    }

    @DataProvider
    Object[][] resourcePaths() {
        return new Object[][] {
            { "create", "managed/user", "user%with%encoded%percentage", "managed/user/user%25with%25encoded%25percentage" },
            { "create", "managed/user", "user//with/encoded/%percentage", "managed/user/user%2F%2Fwith%2Fencoded%2F%25percentage" },
            { "create", "managed/user", "user+%2B/", "managed/user/user+%252B%2F" },
            { "create", "managed/user", "$user  /+", "managed/user/$user%20%20%2F+" },
            { "create", "managed/user", null, "managed/user/*" },
            { "create", "managed/user", "null", "managed/user/null" },
            { "create", "", "user%with%encoded%percentage", "user%25with%25encoded%25percentage" },
            { "create", "", "*", "*" },
            { "create", "", "null", "null" },
            { "create", "", null, "*" },
            { "create", "", "", "" },
            { "read", "", "user%with%encoded%percentage", "" },
            { "read", "", null, "" },
            { "read", "system/hrdb", "user%with%encoded%percentage", "system/hrdb" },
            { "CReaTe", "", "user%with%encoded%percentage", "" }
        };
    }

    @Test(dataProvider = "resourcePaths")
    public void testGetFullResourcePath(String method, String resourcePath, String resourceId, String expectedFullResourcePath)
            throws ScriptException {
        JsonValue scriptName = new JsonValue(new LinkedHashMap<String, Object>());
        scriptName.put("type", "text/javascript");
        scriptName.put("source", "require('policyFilter').getFullResourcePath('"
                + method + "', '" + resourcePath + "', " + (resourceId == null ? null : "'" + resourceId + "'") + ")");
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(scriptName);
        Assert.assertNotNull(scriptEntry);

        Script script = scriptEntry.getScript(new RootContext());
        String fullResourcePath = (String) script.eval();
        Assert.assertEquals(expectedFullResourcePath, fullResourcePath);
    }
}
