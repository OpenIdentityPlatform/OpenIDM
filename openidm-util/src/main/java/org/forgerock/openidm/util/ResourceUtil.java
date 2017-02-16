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
 * Copyright 2013-2017 ForgeRock AS.
 */

package org.forgerock.openidm.util;

import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_REVISION;
import static org.forgerock.openidm.util.RelationshipUtil.REFERENCE_PROPERTIES;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;

/**
 * Resource utilities.
 */
public class ResourceUtil {

    /**
     * Transformer that Prepares two jsonValues to be compared for equality.
     */
    private static final Function<JsonValue, JsonValue, JsonValueException> EQUALITY_PREP_TRANSFORMER =
            new Function<JsonValue, JsonValue, JsonValueException>() {
                @Override
                public JsonValue apply(JsonValue value) {
                    // remove _id and _rev
                    value.remove(FIELD_CONTENT_ID);
                    value.remove(FIELD_CONTENT_REVISION);
                    for (JsonValue jsonValue : value) {
                        jsonValue.as(EQUALITY_PREP_TRANSFORMER);
                    }
                    // if the remaining object has an empty _refProperties, remove it.
                    if (value.get(REFERENCE_PROPERTIES).isNotNull()
                            && value.get(REFERENCE_PROPERTIES).size() == 0) {
                        value.remove(REFERENCE_PROPERTIES);
                    }
                    return value;
                }
            };
    /** The name of the field in the resource content which contains the resource ID as a JsonPointer. */
    public static JsonPointer RESOURCE_FIELD_CONTENT_ID_POINTER = new JsonPointer(FIELD_CONTENT_ID);

    /**
     * {@code ResourceUtil} instances should NOT be constructed in standard
     * programming. Instead, the class should be used as
     * {@code ResourceUtil.parseResourceName(" /foo/bar/ ");}.
     */
    private ResourceUtil() {
        super();
    }

    /**
     * Retrieve the {@code UriTemplateVariables} from the context.
     * <p/>
     *
     * @param context
     *
     * @return an unmodifiableMap or null if the {@code context} does not
     *         contains {@link UriRouterContext}
     */
    public static Map<String, String> getUriTemplateVariables(Context context) {
        UriRouterContext routerContext =
                context.containsContext(UriRouterContext.class) ? context
                        .asContext(UriRouterContext.class) : null;
        if (null != routerContext) {
            return Collections.unmodifiableMap(routerContext.getUriTemplateVariables());
        }
        return null;
    }
    
    public static boolean applyPatchOperation(final PatchOperation operation, final JsonValue newContent) throws ResourceException {
    	boolean isModified = false;
    	try {
            if (operation.isAdd()) {
                newContent.putPermissive(operation.getField(), operation.getValue()
                        .getObject());
            } else if (operation.isRemove()) {
                if (operation.getValue().isNull()) {
                    // Remove entire value.
                    newContent.remove(operation.getField());
                } else {
                    // Find matching value(s) and remove (assumes
                    // reference to array).
                    final JsonValue value = newContent.get(operation.getField());
                    if (value != null) {
                        if (value.isList()) {
                            final Object valueToBeRemoved =
                                    operation.getValue().getObject();
                            final Iterator<Object> iterator = value.asList().iterator();
                            while (iterator.hasNext()) {
                                if (valueToBeRemoved.equals(iterator.next())) {
                                    iterator.remove();
                                }
                            }
                        } else {
                            // Single valued field.
                            final Object valueToBeRemoved =
                                    operation.getValue().getObject();
                            if (valueToBeRemoved.equals(value.getObject())) {
                                newContent.remove(operation.getField());
                            }
                        }
                    }
                }
            } else if (operation.isReplace()) {
                newContent.remove(operation.getField());
                if (!operation.getValue().isNull()) {
                    newContent.putPermissive(operation.getField(), operation.getValue()
                            .getObject());
                }
            } else if (operation.isIncrement()) {
                final JsonValue value = newContent.get(operation.getField());
                final Number amount = operation.getValue().asNumber();
                if (value == null) {
                    throw new BadRequestException("The field '" + operation.getField()
                            + "' does not exist");
                } else if (value.isList()) {
                    final List<Object> elements = value.asList();
                    for (int i = 0; i < elements.size(); i++) {
                        elements.set(i, increment(operation, elements.get(i), amount));
                    }
                } else {
                    newContent.put(operation.getField(), increment(operation, value
                            .getObject(), amount));
                }
            }
            isModified = true;
        } catch (final JsonValueException e) {
            throw new ConflictException("The field '" + operation.getField()
                    + "' does not exist");
        }
    	return isModified;
    }

    public static boolean applyPatchOperations(final List<PatchOperation> operations,
            final JsonValue newContent) throws ResourceException {
        boolean isModified = false;
        if (null != operations) {
            for (final PatchOperation operation : operations) {
                isModified = applyPatchOperation(operation, newContent);
            }
        }
        return isModified;
    }

    private static Object increment(final PatchOperation operation, final Object object,
            final Number amount) throws BadRequestException {
        if (object instanceof Long) {
            return ((Long) object) + amount.longValue();
        } else if (object instanceof Integer) {
            return ((Integer) object) + amount.intValue();
        } else if (object instanceof Float) {
            return ((Float) object) + amount.floatValue();
        } else if (object instanceof Double) {
            return ((Double) object) + amount.doubleValue();
        } else {
            throw new BadRequestException("The field '" + operation.getField()
                    + "' is not a number");
        }
    }

    public static ResourceException notSupported(final Request request) {
        return new NotSupportedException(ResourceMessages.ERR_OPERATION_NOT_SUPPORTED_EXPECTATION
                .get(request.getRequestType().name()).toString());
    }

    public static ResourceException notSupportedOnCollection(final Request request) {
        return new NotSupportedException(ResourceMessages.ERR_OPERATION_NOT_SUPPORTED_EXPECTATION
                .get(request.getRequestType().name()).toString());
    }

    public static ResourceException notSupportedOnInstance(final Request request) {
        return new NotSupportedException(ResourceMessages.ERR_OPERATION_NOT_SUPPORTED_EXPECTATION
                .get(request.getRequestType().name()).toString());
    }

    /**
     * Compares the old vs new json to see if the contents are equal, ignoring _id and _rev at any level in the JSON.
     *
     * @param oldValue old json to compare.
     * @param newValue new json to compare against oldValue.
     * @return true if the two values are equal ignoring the _id and _rev at any level in the JSON.
     */
    public static boolean isEqual(JsonValue oldValue, JsonValue newValue) {
        if (null == oldValue && null == newValue) {
            return true;
        } else if (null == oldValue || null == newValue) {
            return false;
        }
        return oldValue.copy().as(EQUALITY_PREP_TRANSFORMER)
                .isEqualTo(newValue.copy().as(EQUALITY_PREP_TRANSFORMER));
    }
}
