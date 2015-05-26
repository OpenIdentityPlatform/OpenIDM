/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openidm.patch;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.ResourceException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.List;

/**
 */
public class JsonValuePatch {

    /** Apply an "add" PatchOperation. */
    private static boolean add(JsonValue subject, PatchOperation operation) throws BadRequestException {
        if (!operation.isAdd()) {
            throw new BadRequestException("Operation is an " + operation.getOperation() + ", not an add!");
        }
        subject.putPermissive(operation.getField(), operation.getValue().getObject());
        return true;
    }

    /** Apply a "remove" PatchOperation */
    private static boolean remove(JsonValue subject, PatchOperation operation) throws BadRequestException {
        if (!operation.isRemove()) {
            throw new BadRequestException("Operation is an " + operation.getOperation() + ", not a remove!");
        }
        final JsonValue current = subject.get(operation.getField());
        if (current == null || current.isNull()) {
            return false;
        }
        if (operation.getValue() == null || operation.getValue().isNull()) {
            // remove the field
            subject.remove(operation.getField());
            return true;
        }
        else {
            // remove the value
            if (current.isList()) {
                // remove each instance of operation.getValue() from the list of values
                while (current.asList().remove(operation.getValue().getObject())) {
                    // "iterate" until no operation.getValue() objects are in the list
                }
                return true;
            } else {
                if (operation.getValue().getObject().equals(current.getObject())) {
                    subject.remove(operation.getField());
                    return true;
                }
            }
            return false;
        }
    }

    /** Apply a "replace" PatchOperation */
    private static boolean replace(JsonValue subject, PatchOperation operation) throws BadRequestException {
        if (!operation.isReplace()) {
            throw new BadRequestException("Operation is an " + operation.getOperation() + ", not a replace!");
        }
        subject.remove(operation.getField());
        if (!operation.getValue().isNull()) {
            subject.putPermissive(operation.getField(), operation.getValue().getObject());
        }
        return true;
    }

    /** Apply a "increment" PatchOperation */
    private static boolean increment(JsonValue subject, PatchOperation operation) throws BadRequestException {
        if (!operation.isIncrement()) {
            throw new BadRequestException("Operation is an " + operation.getOperation() + ", not an increment!");
        }
        final JsonValue current = subject.get(operation.getField());
        if (current == null) {
            throw new BadRequestException("The field '" + operation.getField() + "' does not exist");
        } else if (current.isList()) {
            final List<Object> elements = current.asList();
            for (int i = 0; i < elements.size(); i++) {
                elements.set(i,  increment(elements.get(i), operation.getValue().asNumber(), operation.getField()));
            }
        } else {
            subject.put(operation.getField(), increment(current.getObject(), operation.getValue().asNumber(), operation.getField()));
        }
        return true;
    }

    /** Helper function to deal with typecasting and incrementing of Object to appropriate Number object). */
    private static Object increment(final Object object, final Number amount, final JsonPointer field) throws BadRequestException {
        if (object instanceof Long) {
            return ((Long) object) + amount.longValue();
        } else if (object instanceof Integer) {
            return ((Integer) object) + amount.intValue();
        } else if (object instanceof Float) {
            return ((Float) object) + amount.floatValue();
        } else if (object instanceof Double) {
            return ((Double) object) + amount.doubleValue();
        } else {
            throw new BadRequestException("The field '" + field + "' is not a number");
        }
    }

    /** Apply a move patch operation */
    private static boolean move(JsonValue subject, PatchOperation operation) throws BadRequestException {
        if (!operation.isMove()) {
            throw new BadRequestException("Operation is a " + operation.getOperation() + ", not a move!");
        }

        JsonValue value = subject.get(operation.getFrom());
        if (value == null || value.isNull()) {
            return false;
        }
        subject.remove(operation.getFrom());
        subject.add(operation.getField(), value.getObject());

        return true;
    }

    /** Apply a copy patch operation */
    private static boolean copy(JsonValue subject, PatchOperation operation) throws BadRequestException {
        if (!operation.isCopy()) {
            throw new BadRequestException("Operation is a " + operation.getOperation() + ", not a copy!");
        }

        JsonValue value = subject.get(operation.getFrom());
        if (value == null || value.isNull()) {
            return false;
        }
        subject.add(operation.getField(), value.getObject());

        return true;
    }

    /** Apply a transform patch operation */
    private static boolean transform(JsonValue subject, PatchOperation operation, PatchValueTransformer transformer) throws BadRequestException {
        if (!operation.isTransform()) {
            throw new BadRequestException("Operation is a " + operation.getOperation() + ", not a transform!");
        }

        Object value = transformer.getTransformedValue(operation, subject);
        if (value == null) {
            subject.remove(operation.getField());
        } else {
            subject.put(operation.getField(), value);
        }

        return true;
    }

    /** An "unknown", or bad operation, implementation of patch application */
    private static boolean unknown(JsonValue subject, PatchOperation operation) throws BadRequestException {
        throw new BadRequestException("Operation " + operation.getOperation() + " is not supported");
    }

    private static final PatchValueTransformer DEFAULT_TRANSFORMER = new PatchValueTransformer() {
        @Override
        public Object getTransformedValue(PatchOperation patch, JsonValue subject) throws JsonValueException {
            if (patch.getValue() != null) {
                return evalScript(subject, patch.getValue());
            }
            throw new JsonValueException(patch.toJsonValue(), "expecting a value member");
        }

        private String evalScript(JsonValue content, JsonValue script) {
            if (script == null || script.getObject() == null || !script.isString()) {
                return null;
            }
            Context cx = Context.enter();
            try {
                Scriptable scope = cx.initStandardObjects();
                String finalScript = "var content = " + content.toString() + "; " + script.getObject();
                Object result = cx.evaluateString(scope, finalScript, "script", 1, null);
                return Context.toString(result);
            } catch (Exception e) {
                throw new JsonValueException(script, "failed to eval script", e);
            } finally {
                Context.exit();
            }
        }
    };

    /**
     * Apply a list of PatchOperations.
     *
     * @param subject the JsonValue to which to apply the patch operation(s).
     * @return whether the subject was modified.
     * @throws ResourceException on failure to apply PatchOperation.
     */
    public static boolean apply(JsonValue subject, List<PatchOperation> operations) throws BadRequestException {
        return apply(subject, operations, DEFAULT_TRANSFORMER);
    }

    /**
     * Apply a list of PatchOperations.
     *
     * @param subject the JsonValue to which to apply the patch operation(s).
     * @param transformer the value transformer used to compute the value to use for the operation.
     * @return whether the subject was modified.
     * @throws ResourceException on failure to apply PatchOperation.
     */
    public static boolean apply(JsonValue subject, List<PatchOperation> operations, PatchValueTransformer transformer)
            throws BadRequestException {

        boolean isModified = false;

        if (operations != null) {
            for (final PatchOperation operation : operations) {
                isModified |=
                        operation.isAdd() ? add(subject, operation)
                                : operation.isRemove() ? remove(subject, operation)
                                : operation.isReplace() ? replace(subject, operation)
                                : operation.isIncrement() ? increment(subject, operation)
                                : operation.isMove() ? move(subject, operation)
                                : operation.isCopy() ? copy(subject, operation)
                                : operation.isTransform() ? transform(subject, operation, transformer)
                                : unknown(subject, operation);
            }
        }

        return isModified;
    }
}

