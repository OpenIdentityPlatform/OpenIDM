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

package org.forgerock.openidm.patch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.array;

import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.ResourceException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.testng.annotations.Test;

/**
 * Test JsonValuePatch methods.
 *
 * TODO Write tests for rest of JsonValuePatch class.
 */
public class JsonValuePatchTest {
    private JsonValue subject = json(object(field("key", "value")));

    @Test
    public void testMapReplace() throws ResourceException {
        JsonValue object = json(object(field("key", "value")));
        final List<PatchOperation> operations = PatchOperation.valueOfList(
                json(array(
                        object(
                                field("operation", "replace"),
                                field("field", "/key"),
                                field("value", "pineapple")
                        )
                )));

        final boolean result = JsonValuePatch.apply(object, operations);
        assertThat(result).isTrue();
        assertThat(object).stringAt("key").isEqualTo("pineapple");
    }

    // OPENIDM-3097
    @Test
    public void testArrayReplaceAtIndex() throws ResourceException {
        JsonValue object = json(object(field("fruits", array("apple", "kiwi", "pear", "strawberry"))));
        final List<PatchOperation> operations = PatchOperation.valueOfList(
                json(array(
                        object(
                                field("operation", "replace"),
                                field("field", "/fruits/1"),
                                field("value", "pineapple")
                        )
                )));

        final boolean result = JsonValuePatch.apply(object, operations);
        assertThat(result).isTrue();
        assertThat(object).hasArray("fruits").containsSequence("apple", "pineapple", "pear", "strawberry");
    }

    private ScriptedPatchValueTransformer transformer = new ScriptedPatchValueTransformer() {
        public JsonValue evalScript(JsonValue content, JsonValue script, JsonPointer field) throws BadRequestException {
            if (!script.isString()) {
                throw new BadRequestException("The patch request is garbage.");
            }
            Context cx = Context.enter();
            try {
                Scriptable scope = cx.initStandardObjects();
                String finalScript = "var content = " + content.get(field).toString() + "; " + script.getObject();
                return new JsonValue(cx.evaluateString(scope, finalScript, "script", 1, null));
            } catch (Exception e) {
                throw new BadRequestException("Failed to eval script " + script.toString());
            } finally {
                Context.exit();
            }
        }
    };

    @Test
    public void testScriptedPatchValueTransformer() throws ResourceException {
        JsonValue diff = json(array(object(
                field("operation", "transform"),
                field("field", "/key"),
                field("value", object(
                        field("script", "var source = content; var target = source + 'xformed'; target;")
                        )
                ))));
        List<PatchOperation> operations = PatchOperation.valueOfList(diff);
        boolean modified = JsonValuePatch.apply(subject, operations, transformer);

        assertThat(subject.get("key").isString()).isTrue();
        assertThat(subject.get("key").asString()).isEqualTo("valuexformed");
        assertThat(modified).isTrue();
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testNullScriptedPatchValueTransformer() throws ResourceException {
        JsonValue diff = json(array(object(
                field("operation", "transform"),
                field("field", "/key"),
                field("value", object(
                        field("script", null)
                        )
                ))));

        List<PatchOperation> operations = PatchOperation.valueOfList(diff);
        JsonValuePatch.apply(subject, operations, transformer);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testBadScriptedPatchValueTransformer() throws ResourceException {
        JsonValue diff = json(array(object(
                field("operation", "transform"),
                field("field", "/key"),
                field("value", object(
                        field("script", "hello")
                        )
                ))));

        List<PatchOperation> operations = PatchOperation.valueOfList(diff);
        JsonValuePatch.apply(subject, operations, transformer);
    }
}