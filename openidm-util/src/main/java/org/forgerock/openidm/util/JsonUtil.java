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
 * Portions copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.openidm.util;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter.Indenter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonTransformer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.resource.SortKey;
import org.forgerock.openidm.core.PropertyAccessor;
import org.forgerock.openidm.core.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER;

    private static final ObjectWriter PRETTY_WRITER;

    static {
        OBJECT_MAPPER =
                new ObjectMapper().configure(
                        JsonParser.Feature.ALLOW_COMMENTS, true).disable(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(
                        SerializationFeature.INDENT_OUTPUT).enable(
                        MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        // TODO Make it configurable for Audit service
        // .configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);

        Indenter indenter = new PrettyIndenter();
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentObjectsWith(indenter);
        prettyPrinter.indentArraysWith(indenter);
        PRETTY_WRITER = OBJECT_MAPPER.writer(prettyPrinter);
    }

    /**
     * Setup logging for the {@link JsonUtil}. Only for diagnostic reason!
     */
    private final static Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    private JsonUtil() {
    }

    /**
     * In the context of two JsonValue.getObject() values, this will compare those two values.
     * If the values are NOT a simple type of String, Number, or Boolean, the classname of the two objects
     * is used as a fall-back sort order.
     *
     * @param left json object value to compare to the right.
     * @param right json object value to compare to the left.
     * @return less than 0 if left is 'greater' than the right, greater than 0 if right is 'greater' than the left,
     * 0 if equal.
     * @throws NullPointerException if left or right are null.
     */
    public static int compareJsonObjectValues(final Object left, final Object right) {
        if (left == right) {
            return 0;
        } else if (left instanceof String && right instanceof String) {
            final String s1 = (String) left;
            final String s2 = (String) right;
            return s1.compareToIgnoreCase(s2);
        } else if (left instanceof Number && right instanceof Number) {
            final Double n1 = ((Number) left).doubleValue();
            final Double n2 = ((Number) right).doubleValue();
            return n1.compareTo(n2);
        } else if (left instanceof Boolean && right instanceof Boolean) {
            final Boolean b1 = (Boolean) left;
            final Boolean b2 = (Boolean) right;
            return b1.compareTo(b2);
        } else {
            // Different types: we need to ensure predictable ordering,
            // so use class name as secondary key.
            return left.getClass().getName().compareTo(right.getClass().getName());
        }
    }

    /**
     * This returns a comparator that iterates through the provided sortKeys and finds the first comparative difference
     * between the left and right side JsonValues.  To ensure consistency with sort order, at least one sortKey should
     * be provided that will guarantee uniqueness between two JsonValues. A JsonValue that wraps a null object is
     * considered less than one that is not. If both are wrapping a null, they are considered equal.
     *
     * @param sortKeys the sortKeys to utilize for sorting.
     * @return a Comparator of JsonValues.
     * @see #compareJsonObjectValues(Object, Object)
     */
    public static Comparator<JsonValue> getComparator(final List<SortKey> sortKeys) {
        return new Comparator<JsonValue>() {
            @Override
            public int compare(final JsonValue left, final JsonValue right) {
                if (left == right || left.isNull() && right.isNull()) {
                    return 0;
                } else if (left.isNull()) {
                    return -1;
                } else if (right.isNull()) {
                    return 1;
                }

                for (final SortKey sortKey : sortKeys) {
                    int orderMultiplier = sortKey.isAscendingOrder() ? 1: -1;
                    JsonValue leftValue = left.get(sortKey.getField());
                    JsonValue rightValue = right.get(sortKey.getField());

                    if (null == leftValue && null == rightValue) {
                        continue;
                    } else if (null == leftValue) {
                        return -1 * orderMultiplier;
                    } else if (null == rightValue) {
                        return orderMultiplier;
                    }
                    int compareTo = compareJsonObjectValues(leftValue.getObject(), rightValue.getObject());
                    if (compareTo != 0) {
                        return compareTo * orderMultiplier;
                    }
                }
                return 0;
            }
        };
    }

    public static boolean jsonIsNull(JsonValue value) {
        return (value == null || value.isNull());
    }

    public static boolean isEncrypted(String value) {
        boolean encrypted = false;
        // TODO: delegate the sanity check if String is a candidate for parsing
        // to the crypto lib
        boolean candidate =
                value != null && value.startsWith("{\"$crypto\":{") && value.endsWith("}}");
        if (candidate) {
            try {
                JsonValue jsonValue = parseStringified(value);
                encrypted = JsonCrypto.isJsonCrypto(jsonValue);
            } catch (JsonException ex) {
                encrypted = false; // IF we can't parse the string assume it's
                // not in an encrypted format we support
            }
        }
        return encrypted;
    }

    public static ObjectMapper build() {
        final ObjectMapper mapper = OBJECT_MAPPER.copy();
        mapper.getFactory().configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        return mapper;
    }

    public static String writeValueAsString(JsonValue value) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(value.getObject());
    }

    public static String writePrettyValueAsString(JsonValue value) throws JsonProcessingException {
        return PRETTY_WRITER.writeValueAsString(value.getObject());
    }

    /**
     *
     * @param content
     * @return
     */
    public static JsonValue parseStringified(String content) {
        JsonValue jsonValue = null;
        try {
            Object parsedValue = OBJECT_MAPPER.readValue(content, Object.class);
            jsonValue = new JsonValue(parsedValue);
        } catch (IOException ex) {
            throw new JsonException("String passed into parsing is not valid JSON", ex);
        }
        return jsonValue;
    }

    /**
     *
     * @param content
     * @return
     */
    public static JsonValue parseURL(URL content) {
        JsonValue jsonValue = null;
        try {
            Object parsedValue = OBJECT_MAPPER.readValue(content, Object.class);
            jsonValue = new JsonValue(parsedValue);
        } catch (IOException ex) {
            throw new JsonException("URL passed into parsing is not valid JSON", ex);
        }
        return jsonValue;
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
                @SuppressWarnings("unchecked")
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
                    } catch (Exception e) {
                        logger.debug("Failed to substitute variable with unexpected error {}", key, e);
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

        /**
         * {@inheritDoc}
         */
        @Override
        public void transform(JsonValue value) {
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

    /**
     * Indenter, part of formatting Jackson output in pretty print Makes the
     * number of spaces to use per indent configurable.
     *
     */
    private static class PrettyIndenter implements Indenter {
        // Default to 4 spaces per level
        int noOfSpaces = 4;

        final static String SYSTEM_LINE_SEPARATOR;
        static {
            String lf = null;
            try {
                lf = System.getProperty("line.separator");
            } catch (Exception e) {
                // access exception?
            }
            SYSTEM_LINE_SEPARATOR = (lf == null) ? "\n" : lf;
        }

        final static int SPACE_COUNT = 64;
        final static char[] SPACES = new char[SPACE_COUNT];
        static {
            Arrays.fill(SPACES, ' ');
        }

        /**
         * Configure how many spaces to use per indent. Default is 4 spaces.
         *
         * @param noOfSpaces
         */
        public void setIndentSpaces(int noOfSpaces) {
            this.noOfSpaces = noOfSpaces;
        }

        public boolean isInline() {
            return false;
        }

        @Override
        public void writeIndentation(JsonGenerator jg, int level) throws IOException,
                JsonGenerationException {
            jg.writeRaw(SYSTEM_LINE_SEPARATOR);
            level = level * noOfSpaces;
            while (level > SPACE_COUNT) { // should never happen but...
                jg.writeRaw(SPACES, 0, SPACE_COUNT);
                level -= SPACES.length;
            }
            jg.writeRaw(SPACES, 0, level);
        }
    }
}
