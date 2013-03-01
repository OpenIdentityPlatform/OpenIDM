package org.forgerock.openidm.core;

import java.util.Stack;

public class PropertyUtil {

    private static final String DELIM_START = "&{";
    private static final char DELIM_STOP = '}';

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
     * @param identityServer
     *            Set of configuration properties.
     * @return The value of the specified string after property substitution.
     **/
    public static Object substVars(String val, final IdentityServer identityServer, boolean doEscape) {

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
            startDelim = val.indexOf(DELIM_START);
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
                if ('{' == val.charAt(index + 1)) {
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
                                getSubstituteValue(Object.class, variable, identityServer);
                        if (null != substValue) {
                            return substValue;
                        } else {
                            propertyStack.peek().append(DELIM_START).append(variable).append(
                                    DELIM_STOP);
                            return propertyStack.peek().toString();
                        }
                    } else {
                        String substValue =
                                getSubstituteValue(String.class, variable, identityServer);
                        if (null != substValue) {
                            propertyStack.peek().append(substValue);
                        } else {
                            propertyStack.peek().append(DELIM_START).append(variable).append(
                                    DELIM_STOP);
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
            propertyStack.peek().append(DELIM_START).append(top.toString());
        }
        return parentBuilder.toString();
    }
    
    private static <T> T getSubstituteValue(Class<T> type, String variable,
            final IdentityServer identityServer) {
        T substValue = null;
        if (String.class.isAssignableFrom(type)) {
            // Get the value of the deepest nested variable
            // placeholder.
            // Try to configuration properties first.
            substValue =
                    (identityServer != null) ? (T) identityServer.getProperty(variable, null, String.class)
                            : null;
            if (substValue == null) {
                // Ignore unknown property values.
                substValue = (T) System.getProperty(variable);
            }
        } else {
            substValue =
                    (identityServer != null) ? identityServer.getProperty(variable, null, type)
                            : null;
            if (substValue == null) {
                // Ignore unknown property values.
                substValue = (T) System.getProperty(variable);
            }
        }
        return substValue;
    }
}
