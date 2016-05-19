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
import org.forgerock.json.JsonValueException;
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

    // "add" is always #putPermissive, so ...

    // ... if the key doesn't exist, it will be added with the given value
    @Test
    public void addShouldResultInNewKeyValue() throws ResourceException {
        JsonValue object = json(object());
        final List<PatchOperation> operations = buildPatchOperation("add", "/key", "pineapple");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).stringAt("key").isEqualTo("pineapple");
    }

    // ... if the key does exist, its value will be overwritten with the new value
    @Test
    public void permissiveAddShouldResultInUpdatedKeyValue() throws ResourceException {
        JsonValue object = json(object(field("key", "value")));
        final List<PatchOperation> operations = buildPatchOperation("add", "/key", "pineapple");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).stringAt("key").isEqualTo("pineapple");
    }

    // ... even if the old value was an array (or an object)
    @Test
    public void addOnMultiValueAttributeShouldReplace() throws ResourceException {
        JsonValue object = json(object(field("key", array("apple", "kiwi", "pear", "strawberry"))));
        final List<PatchOperation> operations = buildPatchOperation("add", "/key", "pineapple");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).stringAt("key").isEqualTo("pineapple");
    }

    // "hypen"-syntax on the field has special behaviors:

    // 1) it throws an exception on a scalar
    @Test(expectedExceptions = JsonValueException.class)
    public void addUsingHyphenOnScalarThrowsException() throws ResourceException {
        JsonValue object = json(object(field("key", "value")));
        final List<PatchOperation> operations = buildPatchOperation("add", "/key/-", "pineapple");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isFalse();
    }

    // 2) adds the element to an array
    @Test
    public void addUsingHyphenOnMultiValuedAttributeShouldAddElement() throws ResourceException {
        JsonValue object = json(object(field("key", array("apple", "kiwi", "pear", "strawberry"))));
        final List<PatchOperation> operations = buildPatchOperation("add", "/key/-", "pineapple");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).hasArray("key").containsOnly("apple", "kiwi", "pear", "strawberry", "pineapple");
    }

    // 3) adds a new field ("-", value) to a map
    @Test
    public void addUsingHyphenOnMapShouldAddKeyValuePairWithHyphenKey() throws ResourceException {
        JsonValue object = json(object(field("key", object(field("a", "1"), field("b", "2")))));
        final List<PatchOperation> operations = buildPatchOperation("add", "/key/-", "pineapple");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).stringAt("key/-").isEqualTo("pineapple");
    }

    @Test
    public void removeShouldResultInKeyRemoval() throws ResourceException {
        JsonValue object = json(object(field("key", "value")));
        final List<PatchOperation> operations = buildPatchOperation("remove", "/key");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).hasNull("key");
    }

    @Test
    public void removeWithValueThatMatchesShouldResultInKeyRemoval() throws ResourceException {
        JsonValue object = json(object(field("key", "value")));
        final List<PatchOperation> operations = buildPatchOperation("remove", "/key", "value");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).hasNull("key");
    }

    @Test
    public void removeFromMultiValuedAttributeWithValueThatMatchesElementShouldResultInElementRemoval() throws ResourceException {
        JsonValue object = json(object(field("key", array("a", "b", "c"))));
        final List<PatchOperation> operations = buildPatchOperation("remove", "/key", "b");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).hasArray("key").containsOnly("a", "c");
    }

    // CUI-140 test case
    @Test
    public void removeWithComplexValueThatMatchesShouldResultInKeyRemoval() throws ResourceException {
        JsonValue object = json(object(field("favorites", array(
                object(field("type", "candy"), field("flavor", "lemon")),
                object(field("type", "candy"), field("flavor", "cherry"))
        ))));
        final List<PatchOperation> operations = buildPatchOperation("remove", "/favorites",
                object(field("type", "candy"), field("flavor", "cherry")));

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).hasArray("favorites").containsOnly(
                object(field("type", "candy"), field("flavor", "lemon")));

    }

    @Test
    public void removeWhenKeyDoesNotExistShouldReturnFalse() throws ResourceException {
        JsonValue object = json(object());
        final List<PatchOperation> operations = buildPatchOperation("remove", "/key");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isFalse();
    }

    @Test
    public void removeWithScalarValueThatDoesNotMatchDoesNotResultInKeyRemoval() throws ResourceException {
        JsonValue object = json(object(field("key", "value")));
        final List<PatchOperation> operations = buildPatchOperation("remove", "/key", "anothervalue");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isFalse();
        assertThat(object).stringAt("key").isEqualTo("value");
    }

    @Test
    public void removeWithComplexValueThatDoesNotMatchDoesNotResultInKeyRemoval() throws ResourceException {
        JsonValue object = json(object(field("key", "value")));
        final List<PatchOperation> operations = buildPatchOperation("remove", "/key",
                object(field("innerKey", "innerValue")));

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isFalse();
        assertThat(object).stringAt("key").isEqualTo("value");
    }

    @Test
    public void removeWithComplexValueThatDoesNotMatchShouldReturnFalse() throws ResourceException {
        JsonValue object = json(object(field("key", object(field("subattribute", "value")))));
        final List<PatchOperation> operations = buildPatchOperation("remove", "/key", object(field("somekey", "somevalue")));

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isFalse();
    }

    @Test
    public void replaceValueAtKey() throws ResourceException {
        JsonValue object = json(object(field("key", "value")));
        final List<PatchOperation> operations = buildPatchOperation("replace", "/key", "pineapple");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).stringAt("key").isEqualTo("pineapple");
    }

    @Test
    public void replaceMultiValuedAttributeAtKey() throws ResourceException {
        JsonValue object = json(object(field("key", array("value1", "value2", "value3"))));
        final List<PatchOperation> operations = buildPatchOperation("replace", "/key", array("a", "b", "c"));

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).hasArray("key").containsOnly("a", "b", "c");
    }

    // OPENIDM-3097
    @Test
    public void replaceAtIndexOnMultiValuedAttribute() throws ResourceException {
        JsonValue object = json(object(field("fruits", array("apple", "kiwi", "pear", "strawberry"))));
        final List<PatchOperation> operations = buildPatchOperation("replace", "/fruits/1", "pineapple");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).hasArray("fruits").containsSequence("apple", "pineapple", "pear", "strawberry");
    }

    @Test
    public void incrementOnIntegerValue() throws ResourceException {
        JsonValue object = json(object(field("key", 1)));
        final List<PatchOperation> operations = buildPatchOperation("increment", "/key", 1);

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).integerAt("key").isEqualTo(2);
    }

    @Test
    public void incrementOnDoubleValue() throws ResourceException {
        JsonValue object = json(object(field("key", 1.0)));
        final List<PatchOperation> operations = buildPatchOperation("increment", "/key", 1.0);

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).doubleAt("key").isEqualTo(2.0);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void incrementOnMissingKey() throws ResourceException {
        JsonValue object = json(object());
        final List<PatchOperation> operations = buildPatchOperation("increment", "/key", 1);

        JsonValuePatch.apply(object, operations);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void incrementOnNonNumericValue() throws ResourceException {
        JsonValue object = json(object(field("key", "value")));
        final List<PatchOperation> operations = buildPatchOperation("increment", "/key", 1);

        JsonValuePatch.apply(object, operations);
    }

    @Test
    public void incrementOnMultiValuedAttribute() throws ResourceException {
        JsonValue object = json(object(field("key", array(1, 2, 3))));
        final List<PatchOperation> operations = buildPatchOperation("increment", "/key", 1);

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).hasArray("key").containsSequence(2, 3, 4);
    }

    @Test
    public void incrementOnMixedNumericMultiValuedAttribute() throws ResourceException {
        JsonValue object = json(object(field("key", array(1, 2.0, 3))));
        final List<PatchOperation> operations = buildPatchOperation("increment", "/key", 1);

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).hasArray("key").containsSequence(2, 3.0, 4);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void incrementOnMixedNonNumericMultiValuedAttribute() throws ResourceException {
        JsonValue object = json(object(field("key", array(1, "value", 3))));
        final List<PatchOperation> operations = buildPatchOperation("increment", "/key", 1);

        JsonValuePatch.apply(object, operations);
    }

    @Test
    public void moveValueAtKey() throws ResourceException {
        JsonValue object = json(object(field("key", "value")));
        final List<PatchOperation> operations = buildMoveOperation("/key", "/new");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).hasNull("key");
        assertThat(object).stringAt("new").isEqualTo("value");
    }

    @Test
    public void moveValueAtKeyNotPresent() throws ResourceException {
        JsonValue object = json(object(field("ki", "value")));
        final List<PatchOperation> operations = buildMoveOperation("/key", "/new");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isFalse();
    }

    @Test
    public void copyValueAtKey() throws ResourceException {
        JsonValue object = json(object(field("key", "value")));
        final List<PatchOperation> operations = buildCopyOperation("/key", "/new");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isTrue();
        assertThat(object).stringAt("key").isEqualTo("value");
        assertThat(object).stringAt("new").isEqualTo("value");
    }

    @Test
    public void copyValueAtKeyNotPresent() throws ResourceException {
        JsonValue object = json(object(field("ki", "value")));
        final List<PatchOperation> operations = buildCopyOperation("/key", "/new");

        final boolean result = JsonValuePatch.apply(object, operations);

        assertThat(result).isFalse();
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
        List<PatchOperation> operations = buildPatchOperation("transform", "/key",
                object(field("script", "var target = content + 'xformed'; target;")));

        boolean modified = JsonValuePatch.apply(subject, operations, transformer);

        assertThat(subject.get("key").isString()).isTrue();
        assertThat(subject.get("key").asString()).isEqualTo("valuexformed");
        assertThat(modified).isTrue();
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testNullScriptedPatchValueTransformer() throws ResourceException {
        List<PatchOperation> operations = buildPatchOperation("transform", "/key", object(field("script", null)));

        JsonValuePatch.apply(subject, operations, transformer);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testBadScriptedPatchValueTransformer() throws ResourceException {
        List<PatchOperation> operations = buildPatchOperation("transform", "/key", object(field("script", "hello")));

        JsonValuePatch.apply(subject, operations, transformer);
    }

    private List<PatchOperation> buildPatchOperation(String operation, String field) throws BadRequestException {
        return PatchOperation.valueOfList(
                json(array(
                        object(
                                field("operation", operation),
                                field("field", field)
                        )
                )));
    }

    private List<PatchOperation> buildPatchOperation(String operation, String field, Object value)
            throws BadRequestException {
        return PatchOperation.valueOfList(
                json(array(
                        object(
                                field("operation", operation),
                                field("field", field),
                                field("value", value)
                        )
                )));
    }

    private List<PatchOperation> buildMoveOperation(String from, String field) throws BadRequestException {
        return PatchOperation.valueOfList(
                json(array(
                        object(
                                field("operation", "move"),
                                field("field", field),
                                field("from", from)
                        )
                )));
    }

    private List<PatchOperation> buildCopyOperation(String from, String field) throws BadRequestException {
        return PatchOperation.valueOfList(
                json(array(
                        object(
                                field("operation", "copy"),
                                field("field", field),
                                field("from", from)
                        )
                )));
    }
}