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
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.openidm.core;

import static org.forgerock.json.JsonValueFunctions.deepTransformBy;

import java.util.Stack;
import java.util.regex.Pattern;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.util.Function;

public class PropertyUtil {

    private PropertyUtil() {
    }

    public enum Delimiter {
        DOLLAR {
            @Override
            char getStartChar() {
                return '$';
            }

            @Override
            String getStartString() {
                return DELIM_START_DOLLAR;
            }
        },
        AMPERSAND {
            @Override
            char getStartChar() {
                return '&';
            }

            @Override
            String getStartString() {
                return DELIM_START_AMPERSAND;
            }
        };

        abstract char getStartChar();

        abstract String getStartString();
    }

    public static final String DELIM_START_DOLLAR = "${";
    public static final String DELIM_START_AMPERSAND = "&{";
    public static final char DELIM_STOP = '}';
    /**
     * Tests that the string contains an ampersand delimited property.
     * For Example: "This has a &{property.key} in it", and "&{property.key}" => matches
     */
    private static final Pattern AMPERSAND_PATTERN = Pattern.compile("^.*\\&\\{.+\\}.*$");
    /**
     * Tests that the string contains a dollar delimited property.
     * For Example: "This has a ${property.key} in it", and "${property.key}" => matches
     */
    private static final Pattern DOLLAR_PATTERN = Pattern.compile("^.*\\$\\{.+\\}.*$");

    /**
     * Function that deeply traverses the json replacing any referenced properties with their Identity value.
     * @see #substVars(String, PropertyAccessor, boolean)
     */
    public static Function<JsonValue, JsonValue, JsonValueException> propertiesEvaluated =
            deepTransformBy(
                    new Function<JsonValue, JsonValue, JsonValueException>() {
                        @Override
                        public JsonValue apply(JsonValue value) throws JsonValueException {
                            if (value == null) {
                                return null;
                            }
                            if (value.isString()) {
                                return new JsonValue(
                                        PropertyUtil.substVars(value.asString(), IdentityServer.getInstance(), false),
                                        value.getPointer());
                            }
                            return value;
                        }
                    });

    /**
     * Tests if the passed in value contains the delimited property, ie &{property} or ${property}
     * @param value the value to test
     * @param property the property to look for in the passed value.
     * @return true if the passed value contains the delimited property.
     */
    public static boolean containsProperty(String value, String property) {
        return null != value && (value.contains(DELIM_START_AMPERSAND + property + DELIM_STOP)
                || value.contains(DELIM_START_DOLLAR + property + DELIM_STOP));
    }

    /**
     * Tests if the passed in value contains any delimited property.
     *
     * @param value the value to test.
     * @return true if the passed value contains any delimited property.
     */
    public static boolean containsProperty(String value) {
        return null != value && (AMPERSAND_PATTERN.matcher(value).matches() || DOLLAR_PATTERN.matcher(value).matches());
    }

    /**
     * <p>
     * This method performs property variable substitution on the specified
     * value. If the specified value contains the syntax
     * <tt>&{&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt> refers to
     * either a configuration property or a system property, then the
     * corresponding property value is substituted for the variable placeholder.
     * Multiple variable placeholders may exist in the specified value as well
     * as nested variable placeholders, which are substituted from inner most to
     * outer most. Configuration properties override system properties.
     * </p>
     *
     * @param val
     *            The string on which to perform property substitution.
     * @param propertyAccessor
     *            Set of configuration properties.
     * @return The value of the specified string after property substitution.
     **/
    public static Object substVars(String val, final PropertyAccessor propertyAccessor,
            boolean doEscape) {
        return substVars(val, propertyAccessor, Delimiter.AMPERSAND, doEscape);

    }

    public static Object substVars(String val, final PropertyAccessor propertyAccessor,
            Delimiter delimiter, boolean doEscape) {

        // Assume we have a value that is something like:
        // "leading &{foo.&{bar}} middle ${baz} trailing"

        int stopDelim = -1;
        int startDelim = -1;

        if (!doEscape) {
            stopDelim = val.indexOf(DELIM_STOP, stopDelim + 1);
            // If there is no stopping delimiter, then just return
            // the value since there is no variable declared.
            if (stopDelim < 0) {
                return val;
            }
            startDelim = val.indexOf(delimiter.getStartString());
            // If there is no starting delimiter, then just return
            // the value since there is no variable declared.
            if (startDelim < 0) {
                return val;
            }
        }

        StringBuilder parentBuilder = new StringBuilder(val.length());
        Stack<StringBuilder> propertyStack = new Stack<StringBuilder>();
        propertyStack.push(parentBuilder);

        for (int index = 0; index < val.length(); index++) {
            switch (val.charAt(index)) {
            case '\\': {
                if (doEscape) {
                    index++;
                    if (index < val.length()) {
                        propertyStack.peek().append(val.charAt(index));
                    }
                } else {
                    propertyStack.peek().append(val.charAt(index));
                }
                break;
            }
            case '&': {
                if ('{' == val.charAt(index + 1) && val.charAt(index) == delimiter.getStartChar()) {
                    // This is a start of a new property
                    propertyStack.push(new StringBuilder(val.length()));
                    index++;
                } else {
                    propertyStack.peek().append(val.charAt(index));
                }
                break;
            }
            case '$': {
                if ('{' == val.charAt(index + 1) && val.charAt(index) == delimiter.getStartChar()) {
                    // This is a start of a new property
                    propertyStack.push(new StringBuilder(val.length()));
                    index++;
                } else {
                    propertyStack.peek().append(val.charAt(index));
                }
                break;
            }
            case DELIM_STOP: {
                // End of the actual property
                if (propertyStack.size() == 1) {
                    // There is no start delimiter
                    propertyStack.peek().append(val.charAt(index));
                } else {
                    String variable = propertyStack.pop().toString();
                    if ((index == val.length() - 1) && propertyStack.size() == 1
                            && parentBuilder.length() == 0) {
                        // Replace entire value with an Object
                        Object substValue =
                                getSubstituteValue(Object.class, variable, propertyAccessor);
                        if (null != substValue) {
                            return substValue;
                        } else {
                            propertyStack.peek().append(delimiter.getStartString())
                                    .append(variable).append(DELIM_STOP);
                            return propertyStack.peek().toString();
                        }
                    } else {
                        String substValue =
                                getSubstituteValue(String.class, variable, propertyAccessor);
                        if (null != substValue) {
                            propertyStack.peek().append(substValue);
                        } else {
                            propertyStack.peek().append(delimiter.getStartString())
                                    .append(variable).append(DELIM_STOP);
                        }
                    }
                }
                break;
            }
            default: {
                propertyStack.peek().append(val.charAt(index));
            }
            }
        }

        // Close the open &{ tags.
        for (int index = propertyStack.size(); index > 1; index--) {
            StringBuilder top = propertyStack.pop();
            propertyStack.peek().append(delimiter.getStartString()).append(top.toString());
        }
        return parentBuilder.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T> T getSubstituteValue(Class<T> type, String variable,
            final PropertyAccessor propertyAccessor) {
        T substValue = null;
        if (String.class.isAssignableFrom(type)) {
            // Get the value of the deepest nested variable
            // placeholder.
            // Try to configuration properties first.
            substValue =
                    (propertyAccessor != null) ? (T) propertyAccessor.getProperty(variable, null,
                            String.class) : null;
        } else {
            substValue =
                    (propertyAccessor != null) ? propertyAccessor.getProperty(variable, null, type)
                            : null;
        }
        return substValue;
    }
}
