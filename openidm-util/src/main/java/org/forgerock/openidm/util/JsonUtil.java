/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.util;

import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonTransformer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openidm.core.PropertyAccessor;
import org.forgerock.openidm.core.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtil {

    /**
     * Setup logging for the {@link JsonUtil}. Only for diagnostic reason!
     */
    private final static Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    public static boolean jsonIsNull(JsonValue value) {
        return (value == null || value.isNull());
    }

    public static JsonTransformer getPropertyJsonTransformer(final JsonValue properties,
            boolean allowUnresolved) {
        if (jsonIsNull(properties)) {
            return null;
        }
        return new PropertyTransformer(properties, allowUnresolved);
    }

    public static class PropertyTransformer implements JsonTransformer {

        private final PropertyAccessor properties;
        private final boolean eager;

        PropertyTransformer(final JsonValue properties, boolean allowUnresolved) {
            this.eager = !allowUnresolved;
            this.properties = new PropertyAccessor() {
                @Override
                public <T> T getProperty(String key, T defaultValue, Class<T> expected) {
                    JsonPointer pointer = new JsonPointer(key.split("\\."));
                    try {
                        JsonValue newValue = properties.get(pointer);
                        if (null != newValue) {
                            return (T) newValue.required().expect(expected).getObject();
                        }
                    } catch (JsonValueException e) {
                        logger.trace("Failed to substitute variable {}", key, e);
                        /*
                         * Expected if the value is null or the type does not
                         * match
                         */
                    } catch (Throwable t) {
                        logger.debug("Failed to substitute variable with unexpected error {}", key,
                                t);
                    }
                    if (eager && null == defaultValue) {
                        StringBuilder sb =
                                new StringBuilder("Failed to resolve mandatory property: ")
                                        .append(key);
                        if (null != expected && !Object.class.equals(expected)) {
                            sb.append(" expecting ").append(expected.getSimpleName()).append(
                                    " class");
                        }
                        throw new JsonValueException(null, sb.toString());
                    }
                    return defaultValue;
                }
            };
        }

        @Override
        public void transform(JsonValue value) throws JsonException {
            if (null != value && value.isString()) {
                try {
                    value.setObject(PropertyUtil.substVars(value.asString(), properties,
                            PropertyUtil.Delimiter.DOLLAR, false));
                } catch (JsonValueException e) {
                    throw new JsonValueException(value, e.getMessage());
                }
            }
        }
    }
}
