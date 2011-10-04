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

package org.forgerock.openidm.script;

// Java Standard Edition
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// FEST-Assert
import static org.fest.assertions.Assertions.assertThat;

// TestNG
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

// ForgeRock JSON-Fluent
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

/**
 * @author Paul C. Bryan
 */
public class ScriptTest {

    private HashMap<String, Object> scope;

    // ----- preparation ----------

    @BeforeMethod
    public void beforeMethod() {
        scope = new HashMap<String, Object>();
    }

    // ----- happy path ----------

    @Test
    public void JavaScriptTest() throws JsonNodeException, ScriptException {
        JsonNode config = new JsonNode(new HashMap<String, Object>());
        config.put("type", "text/javascript");
        config.put("source", "1 + 2");
        Script script = Scripts.newInstance("test", config);
        assertThat(script.exec(scope)).isEqualTo(3);
    }

    @Test
    public void FunctionTest() throws JsonNodeException, ScriptException {
        JsonNode config = new JsonNode(new HashMap<String, Object>());
        config.put("type", "text/javascript");
        config.put("source", "x.add(1,2)");
        HashMap<String, Object> x = new HashMap<String, Object>();
        x.put("add", new Function() {
            @Override public Object call(Map<String, Object> scope, Map<String, Object> _this, List<Object> params) throws Throwable {
                JsonNode node = new JsonNode(params);
                int i1 = node.get(0).required().asInteger();
                int i2 = node.get(1).required().asInteger();
                return i1 + i2;
            }
        });
        scope.put("x", x);
        Script script = Scripts.newInstance("test", config);
        assertThat(script.exec(scope)).isEqualTo(3);
    }

    // ---- exceptions ----------

    @Test(expectedExceptions=JsonNodeException.class)
    public void UnknownScriptTest() throws JsonNodeException, ScriptException {
        JsonNode config = new JsonNode(new HashMap<String, Object>());
        config.put("type", "definitely/unknown");
        config.put("source", "lather; rinse; repeat;");
        Scripts.newInstance("test", config); // should throw exception
    }
}
