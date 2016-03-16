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
 * Portions copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test the Scripts class
 */
public class ScriptsTest {
    
    // Mocked interfaces
    private ScriptRegistry mockScriptRegistry = mock(ScriptRegistry.class);
    private ScriptEntry mockScriptEntry = mock(ScriptEntry.class);
    private Script mockScript = mock(Script.class);
    private Bindings mockBindings = mock(Bindings.class);
    private Context context = new RootContext();
    
    // A valid script configuration
    private JsonValue testScriptConfig = json(object(
            field("type", "text/javascript"), 
            field("file", "script/test.js")));
    
    @BeforeClass
    public void beforeClass() throws ScriptException {
        when(mockScript.eval(any(Bindings.class))).thenReturn(true);
        when(mockScript.createBindings()).thenReturn(mockBindings);
        when(mockScriptEntry.getScript(context)).thenReturn(mockScript);
        when(mockScriptRegistry.takeScript(any(JsonValue.class))).thenReturn(mockScriptEntry);      
        Scripts.init(mockScriptRegistry);
    }
    
    @Test
    public void testScriptUtil() throws JsonValueException, ScriptException {
        // Test a valid script configuration
        assertThat(Scripts.newScript(testScriptConfig).exec(null, context)).isEqualTo(true);
        // Test invalid script configurations
        assertThat(Scripts.newScript(json(null))).isNull();
        assertThat(Scripts.newScript(null)).isNull();
    }
}
