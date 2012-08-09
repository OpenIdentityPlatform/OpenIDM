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
package org.forgerock.openidm.script.impl;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.scope.impl.ScopeFactoryService;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

import org.forgerock.json.fluent.JsonValue;

/**
 * TODO: Description.
 *
 * @author nati
 */
@Test
public class SchedulableScriptServiceTest {

    class IsJsonValue extends org.mockito.ArgumentMatcher<JsonValue> {
        @Override public boolean matches(Object o) {
            return (o instanceof JsonValue);
        }
    }

    public void testExecute() throws Exception {
        ScopeFactoryService scopeFS = mock(ScopeFactoryService.class);
        when(scopeFS.newInstance(argThat(new IsJsonValue()))).thenReturn(new HashMap<String, Object>());
        Map<String, Object> context = new HashMap<String, Object>();
        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> input = new HashMap<String, Object>();
        Map<String, Object> script = new HashMap<String, Object>();
        context.put(ScheduledService.INVOKER_NAME, "scriptTest");
        context.put(SchedulableScriptService.CONFIGURED_INVOKE_CONTEXT, params);
        input.put("edit", 26);
        script.put("type", "text/javascript");
        script.put("source", "java.lang.System.out.println('It is working: ' + input.edit);");
        params.put("input", input);
        params.put("script", script);
        SchedulableScriptService srv = new SchedulableScriptService();
        srv.scopeFactory = scopeFS;
        srv.execute(context);
    }
}
