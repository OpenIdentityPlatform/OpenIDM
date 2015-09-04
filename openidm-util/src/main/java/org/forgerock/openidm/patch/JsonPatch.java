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
 * Copyright 2011-2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.patch;

// Java SE
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

// JSON Fluent
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;

/**
 * Processes partial modifications to JSON values. Implements
 * <a href="http://tools.ietf.org/html/rfc6902#section-4.1">RFC 6902 - JSON Patch</a>.
 */
public final class JsonPatch {

    /**
     * Internet media type for the JSON Patch format.
     */
    public static final String MEDIA_TYPE = "application/json-patch";

    /**
     * Path to the "op" attribute of a patch entry. Required.
     */
    private static final JsonPointer OP_PTR = new JsonPointer("/op");

    /**
     * Path to the "path" attribute of a patch entry. Required.
     */
    public static final JsonPointer PATH_PTR = new JsonPointer("/path");

    /**
     * Path to the "from" attribute of a patch entry. Required only for "move" and "copy"
     * operations. Ignored for all others.
     */
    private static final JsonPointer FROM_PTR = new JsonPointer("/from");

    /**
     * Path to the "value" attribute of a patch entry. Required for "add", "replace" and
     * "test" operations; Ignored for all others.
     *
     * This is public to allow for alternate implementations of {@link JsonPatchValueTransformer}.
     */
    public static final JsonPointer VALUE_PTR = new JsonPointer("/value");

    /**
     * Default transform for patch values; Conforms to RFC6902.
     */
    private static final JsonPatchValueTransformer DEFAULT_TRANSFORM =
            new JsonPatchValueTransformer() {
                public Object getTransformedValue(JsonValue target, JsonValue op) {
                    if (op.get(JsonPatch.VALUE_PTR) != null) {
                        return op.get(JsonPatch.VALUE_PTR).getObject();
                    }
                    throw new JsonValueException(op, "expecting a value member");
                }
            };

    /**
     * Compares two JSON values, and produces a JSON Patch value, which contains the
     * operations necessary to modify the {@code original} value to arrive at the
     * {@code target} value.
     *
     * @param original the original value.
     * @param target the intended target value.
     * @return the resulting JSON Patch value.
     * @throws NullPointerException if either of {@code original} or {@code target} are {@code null}.
     */
    public static JsonValue diff(JsonValue original, JsonValue target) {
        ArrayList<Object> result = new ArrayList<Object>();
        if (differentTypes(original, target)) { // different types cause a replace
            result.add(op("replace", original.getPointer(), target));
        } else if (original.isMap()) {
            for (String key : original.keys()) {
                if (target.isDefined(key)) { // target also has the property
                    JsonValue diff = diff(original.get(key), target.get(key)); // recursively compare properties
                    if (diff.size() > 0) {
                        result.addAll(diff.asList()); // add diff results
                    }
                } else { // property is missing in target
                    result.add(op("remove", original.getPointer().child(key), null));
                }
            }
            for (String key : target.keys()) {
                if (!original.isDefined(key)) { // property is in target, not in original
                    result.add(op("add", original.getPointer().child(key), target.get(key)));
                }
            }
        } else if (original.isList()) {
            boolean replace = false;
            if (original.size() != target.size()) {
                replace = true;
            } else {
                Iterator<JsonValue> i1 = original.iterator();
                Iterator<JsonValue> i2 = target.iterator();
                while (i1.hasNext() && i2.hasNext()) {
                    if (diff(i1.next(), i2.next()).size() > 0) { // recursively compare elements
                        replace = true;
                        break;
                    }
                }
            }
            if (replace) { // replace list entirely
                result.add(op("replace", original.getPointer(), target));
            }
        } else if (!original.isNull() && !original.getObject().equals(target.getObject())) { // simple value comparison
            result.add(op("replace", original.getPointer(), target));
        }
        return new JsonValue(result);
    }

    /**
     * Returns {@code true} if the type contained by {@code v1} is different than the type
     * contained by {@code v2}.
     * <p>
     * Note: If an unexpected (non-JSON) type is encountered, this method returns
     * {@code true}, triggering a change in the resulting patch.
     */
    private static boolean differentTypes(JsonValue v1, JsonValue v2) {
        return !(v1.isNull() && v2.isNull())
                && !(v1.isMap() && v2.isMap())
                && !(v1.isList() && v2.isList())
                && !(v1.isString() && v2.isString())
                && !(v1.isNumber() && v2.isNumber())
                && !(v1.isBoolean() && v2.isBoolean());
    }

    private static HashMap<String, Object> op(String op, JsonPointer pointer, JsonValue value) {
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put(OP_PTR.leaf(), op);
        result.put(PATH_PTR.leaf(), pointer.toString());
        if (value != null) {
            result.put(VALUE_PTR.leaf(), value.copy().getObject());
        }
        return result;
    }

    /**
     * Applies a set of modifications in a JSON patch value to an original value, resulting
     * in the intended target value. In the event of a failure, this method does not revert
     * any modifications applied up to the point of failure.
     *
     * @param original the original value on which to apply the modifications.
     * @param patch the JSON Patch value, specifying the modifications to apply to the original value.
     * @throws JsonValueException if application of the patch failed.
     */
    public static void patch(JsonValue original, JsonValue patch) {
        patch(original, patch, DEFAULT_TRANSFORM);
    }

    /**
     * Applies a set of modifications in a JSON patch value to an original value, resulting
     * in the intended target value. In the event of a failure, this method does not revert
     * any modifications applied up to the point of failure.
     *
     * @param original the original value on which to apply the modifications.
     * @param patch the JSON Patch value, specifying the modifications to apply to the original value.
     * @param transform a custom transform used to determine the target value.
     * @throws JsonValueException if application of the patch failed.
     */
    public static void patch(JsonValue original, JsonValue patch, JsonPatchValueTransformer transform) {
        for (JsonValue operation : patch.required().expect(List.class)) {
            if (!operation.isDefined("op")) {
                throw new JsonValueException(operation, "op not specified");
            }
            PatchOperation op = PatchOperation.valueOf(operation.get(OP_PTR));
            if (op == null) {
                throw new JsonValueException(operation, "invalid op specified");
            }
            op.execute(original, operation, transform);
        }
    }

    private enum PatchOperation {
        ADD {
            // http://tools.ietf.org/html/rfc6902#section-4.1
            @Override
            void execute(JsonValue original, JsonValue operation, JsonPatchValueTransformer transform) {
                JsonPointer modifyPath = operation.get(PATH_PTR).expect(String.class).asPointer();
                JsonValue parent = parentValue(modifyPath, original);
                if (parent == null) {
                    // patch specifies a new root object
                    if (original.getObject() != null) {
                        throw new JsonValueException(operation, "root value already exists");
                    }
                    original.setObject(transform.getTransformedValue(original, operation));
                } else {
                    try {
                        if (parent.isList()) {
                            try {
                                // if the path points to an array index then we should insert the value
                                Integer index = Integer.valueOf(modifyPath.leaf());
                                parent.add(index, transform.getTransformedValue(original, operation));
                            } catch (Exception e) {
                                // leaf is not an array index, replace value
                                parent.add(modifyPath.leaf(), transform.getTransformedValue(original, operation));
                            }
                        } else if (original.get(modifyPath) != null && original.get(modifyPath).isList()) {
                            // modifyPath does not indicate an index, use the whole object
                            JsonValue target = original.get(modifyPath);
                            target.asList().add(transform.getTransformedValue(original, operation));
                        } else {
                            // this will replace the value even if present
                            parent.add(modifyPath.leaf(), transform.getTransformedValue(original, operation));
                        }
                    } catch (JsonException je) {
                        throw new JsonValueException(operation, je);
                    }
                }
            }
        },
        REMOVE {
            //http://tools.ietf.org/html/rfc6902#section-4.2
            @Override
            void execute(JsonValue original, JsonValue operation, JsonPatchValueTransformer transform) {
                JsonPointer modifyPath = operation.get(PATH_PTR).expect(String.class).asPointer();
                JsonValue parent = parentValue(modifyPath, original);
                String leaf = modifyPath.leaf();
                if (parent == null) {
                    // patch specifies root object
                    original.setObject(null);
                } else {
                    if (!parent.isDefined(leaf)) {
                        throw new JsonValueException(operation, "value to remove not found");
                    }
                    try {
                        parent.remove(leaf);
                    } catch (JsonException je) {
                        throw new JsonValueException(operation, je);
                    }
                }
            }
        },
        REPLACE {
            //http://tools.ietf.org/html/rfc6902#section-4.3
            @Override
            void execute(JsonValue original, JsonValue operation, JsonPatchValueTransformer transform) {
                JsonPointer modifyPath = operation.get(PATH_PTR).expect(String.class).asPointer();
                JsonValue parent = parentValue(modifyPath, original);
                if (parent != null) {
                    // replacing a child
                    String leaf = modifyPath.leaf();
                    if (!parent.isDefined(leaf)) {
                        throw new JsonValueException(operation, "value to replace not found");
                    }
                    parent.put(leaf, transform.getTransformedValue(original, operation));
                } else {
                    // replacing the root value itself
                    original.setObject(transform.getTransformedValue(original, operation));
                }
            }
        },
        MOVE {
            // http://tools.ietf.org/html/rfc6902#section-4.4
            @Override
            void execute(JsonValue original, JsonValue operation, JsonPatchValueTransformer transform) {
                JsonPointer sourcePath = operation.get(FROM_PTR).expect(String.class).asPointer();
                JsonPointer destPath = operation.get(PATH_PTR).expect(String.class).asPointer();
                JsonValue sourceParent = parentValue(sourcePath, original);
                if (sourceParent == null) {
                    throw new JsonValueException(operation, "cannot move root object");
                }
                JsonValue object = sourceParent.get(sourcePath.leaf());
                JsonValue destParent = parentValue(destPath, original);
                if (destParent == null) {
                    // replacing root object with moved object
                    original.setObject(object);
                } else {
                    sourceParent.remove(sourcePath.leaf());
                    destParent.put(destPath.leaf(), object);
                }
            }
        },
        COPY {
            // http://tools.ietf.org/html/rfc6902#section-4.5
            @Override
            void execute(JsonValue original, JsonValue operation, JsonPatchValueTransformer transform) {
                JsonPointer sourcePath = operation.get(FROM_PTR).expect(String.class).asPointer();
                JsonPointer destPath = operation.get(PATH_PTR).expect(String.class).asPointer();
                JsonValue sourceParent = parentValue(sourcePath, original);
                JsonValue object = sourceParent.get(sourcePath.leaf());
                JsonValue destParent = parentValue(destPath, original);
                if (destParent == null) {
                    // replacing root object with copied object
                    original.setObject(object);
                } else {
                    destParent.put(destPath.leaf(), object);
                }
            }
        },
        TEST {
            // http://tools.ietf.org/html/rfc6902#section-4.6
            @Override
            void execute(JsonValue original, JsonValue operation, JsonPatchValueTransformer transform) {
                JsonPointer testPath = operation.get(PATH_PTR).expect(String.class).asPointer();
                JsonValue testTarget = parentValue(testPath, original).get(testPath.leaf());
                JsonValue testValue = new JsonValue(transform.getTransformedValue(original, operation));

                if (diff(testTarget, testValue).asList().size() > 0) {
                    throw new JsonValueException(operation, "test failed");
                }
            }
        };

        void execute(JsonValue original, JsonValue operation, JsonPatchValueTransformer transform) {
            throw new JsonValueException(original, "unsupported operation");
        }

        static PatchOperation valueOf(JsonValue op) {
            return valueOf(op.expect(String.class).asString().toUpperCase());
        }
    }

    /**
     * Returns the parent value of the value identified by the JSON pointer.
     *
     * @param pointer the pointer to the value whose parent value is to be returned.
     * @param target the JSON value against which to resolve the JSON pointer.
     * @return the parent value of the value identified by the JSON pointer.
     * @throws JsonException if the parent value could not be found.
     */
    private static JsonValue parentValue(JsonPointer pointer, JsonValue target) {
        JsonValue result = null;
        JsonPointer parent = pointer.parent();
        if (parent != null) {
            result = target.get(parent);
            if (result == null) {
                throw new JsonException("parent value not found");
            }
        }
        return result;
    }

    // prevent construction
    private JsonPatch() {
    }
}
