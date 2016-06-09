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

package org.forgerock.openidm.script;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.openidm.script.ScriptExecutor;
import org.forgerock.openidm.script.impl.ScriptRegistryService;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.javascript.RhinoScriptEngineFactory;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;

/**
 * Test ScriptedPatchValueTransformerFactory class.
 */
public class ScriptedPatchValueTransformerFactoryTest {
    private JsonValue subject = json(object(field("key", "value")));
    private ScriptRegistryImpl scriptRegistry = null;
    private ScriptExecutor scriptExecutor = null;
    private ScriptedPatchValueTransformerFactory scriptedPatchValueTransformerFactory = new ScriptedPatchValueTransformerFactory();

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

    @BeforeClass
    public void initScriptRegistry() throws Exception {
        Map<String, Object> configuration = new HashMap<>(1);
        configuration.put(getLanguageName(), getConfiguration());

        scriptRegistry = getScriptRegistry(configuration);

        scriptedPatchValueTransformerFactory.bindScriptRegistry(scriptRegistry);
        scriptedPatchValueTransformerFactory.bindScriptExecutor(new ScriptRegistryService());
    }

    @Test
    public void testEvalScript() throws IOException {
        //given
        final Context context = new RootContext();

        JsonValue jsonScript = json(object(
                field("operation", "transform"),
                field("field", "/key"),
                field("value", object(
                        field("script",
                                object(
                                        field("source", "var source = content; var target = source + 'xformed'; target;"),
                                        field("type", getLanguageName())
                                ))
                ))
        ));
        JsonValue result = scriptedPatchValueTransformerFactory.getPatchValueTransformer(context).getTransformedValue(PatchOperation.valueOf(jsonScript), subject);
        assertThat(result.isString()).isTrue();
        assertThat(result.asString()).isEqualTo("valuexformed");
    }
}