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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openidm.script.impl;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.groovy.GroovyScriptEngineFactory;
import org.forgerock.script.javascript.RhinoScriptEngineFactory;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.services.context.RootContext;
import org.mockito.ArgumentCaptor;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScriptRegistryServiceTest {
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

    @BeforeClass
    public void initScriptRegistry() throws Exception {
        Map<String, Object> configuration = new HashMap<>(1);
        configuration.put(getLanguageName(), getConfiguration());

        scriptRegistry = getScriptRegistry(configuration);
    }

    public ScriptRegistry getScriptRegistry() {
        return scriptRegistry;
    }

    @DataProvider(name = "languages")
    public Object[][] getLanguages() throws Exception {
        return new Object[][]{
                {GroovyScriptEngineFactory.LANGUAGE_NAME},
                {RhinoScriptEngineFactory.LANGUAGE_NAME}
        };
    }

    @Test(dataProvider = "languages")
    public void testTakeScriptGlobals(String language) throws Exception {
        ScriptRegistryService scriptRegistryService = new ScriptRegistryService();
        JsonValue jsonScript = json(object(
                field("type", language),
                field("source", "test source"),
                field("simpleKey", "simpleValue"),
                field("globals", json(object(field("globalKey", "globalValue"))))));
        ScriptEntry scriptEntry = scriptRegistryService.takeScript(jsonScript);
        Assert.assertEquals("simpleValue", scriptEntry.get("simpleKey"));
        Assert.assertEquals("globalValue", scriptEntry.get("globalKey"));
        Assert.assertEquals(String.class, scriptEntry.get("globalKey").getClass());
    }

    @Test
    public void testAuditScheduledService() throws Exception {
        //given
        final ScriptRegistryService scriptRegistryService = new ScriptRegistryService();
        final IDMConnectionFactory connectionFactory = mock(IDMConnectionFactory.class);
        final Connection connection = mock(Connection.class);
        final Context context = mock(Context.class);
        final AuditEvent auditEvent = mock(AuditEvent.class);
        final ArgumentCaptor<CreateRequest> argumentCaptor = ArgumentCaptor.forClass(CreateRequest.class);

        scriptRegistryService.setConnectionFactory(connectionFactory);

        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.create(any(Context.class), argumentCaptor.capture()))
                .thenReturn(newResourceResponse("id", "rev", null));
        when(auditEvent.getValue()).thenReturn(json(object()));

        //when
        scriptRegistryService.auditScheduledService(context, auditEvent);

        //then
        verify(connection).create(any(Context.class), any(CreateRequest.class));
        assertThat(argumentCaptor.getValue().getContent()).isEqualTo(auditEvent.getValue());
        assertThat(argumentCaptor.getValue().getResourcePath()).isEqualTo("audit/access");
    }

    @Test
    public void testExecScript() throws Exception {
        //given
        final ScriptRegistryService scriptRegistryService = new ScriptRegistryService();
        final Context context = new RootContext();

        JsonValue jsonScript = json(object(
                field("source", "var source = content.key; var target = source + 'xformed'; target;"),
                field("type", getLanguageName())
        ));
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(new JsonValue(jsonScript));
        final Map<String, Object> bindings = new HashMap<>();
        bindings.put("content", json(object(field("key", "value"))));

        JsonValue result = scriptRegistryService.execScript(context, scriptEntry, bindings);
        assertThat(result.isString()).isTrue();
        assertThat(result.asString()).isEqualTo("valuexformed");
    }
}