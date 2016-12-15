/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.launcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.codehaus.plexus.util.MatchPatterns;

/**
 * A ConfigurationUtil class contains util methods used by
 * {@link OSGiFrameworkService}.
 * 
 * @author Laszlo Hordos
 */
public class ConfigurationUtil {

    private ConfigurationUtil() {
    }

    /**
     * <p/>
     * Retrieve a list of filepaths from a given directory within a jar file. If
     * filtered results are needed, you can supply a |filter| regular expression
     * which will match each entry.
     * 
     * @param location
     * @param includes
     *            to filter the results within a regular expression.
     * @return a list of files within the jar |file|
     */
    public static Vector<URL> getJarFileListing(URL location, List<String> includes,
            List<String> excludes) {
        Vector<URL> files = new Vector<URL>();
        if (location == null || !"jar".equals(location.getProtocol())) {
            return files; // Empty.
        }

        MatchPatterns includesPatterns = null;
        MatchPatterns excludesPatterns = null;

        if (includes == null) {
            // No includes supplied, so set it to 'matches all'
            includesPatterns = MatchPatterns.from("**");
        } else {
            includesPatterns = MatchPatterns.from(includes);
        }
        if (excludes == null) {
            excludesPatterns = MatchPatterns.from();
        } else {
            excludesPatterns = MatchPatterns.from(excludes);
        }

        JarInputStream inputStream = null;
        try {
            // Lets stream the jar file
            inputStream = new JarInputStream(location.openConnection().getInputStream());
            JarEntry jarEntry;

            // Iterate the file entries within that jar. Then make sure it
            // follows the filter given from the user.
            do {
                jarEntry = inputStream.getNextJarEntry();
                if (jarEntry != null && !jarEntry.isDirectory()) {
                    String fileName = jarEntry.getName();

                    if (includesPatterns.matches(fileName, false)
                            && !excludesPatterns.matches(fileName, false)) {
                        files.add(new URL(location, fileName));
                    }
                }
            } while (jarEntry != null);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to get Jar input stream from '" + location + "'",
                    ioe);
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    /* ignore there is nothing to do */
                }
            }
        }

        return files;
    }

    /**
     * <p/>
     * Retrieve a list of filepaths from a given directory within a jar file. If
     * filtered results are needed, you can supply a |filter| regular expression
     * which will match each entry.
     * 
     * @param location
     * @param includes
     *            to filter the results within a regular expression.
     * @return a list of files within the zip |file|
     */
    public static Vector<URL> getZipFileListing(URL location, List<String> includes,
            List<String> excludes) {
        Vector<URL> files = new Vector<URL>();
        if (location == null) {
            return files; // Empty.
        }

        // Wrap the ZIP file location into JAR URL
        URL base = location;
        if (!"jar".equals(location.getProtocol())) {
            try {
                String zip = location.toString();
                if (!zip.endsWith("!/")) {
                    zip = zip + "!/";
                }
                base = new URL("jar:" + zip);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Protocol can not be wrapped into JAR");
            }
        }

        MatchPatterns includesPatterns = null;
        MatchPatterns excludesPatterns = null;

        if (includes == null) {
            // No includes supplied, so set it to 'matches all'
            includesPatterns = MatchPatterns.from("**");
        } else {
            includesPatterns = MatchPatterns.from(includes);
        }
        if (excludes == null) {
            excludesPatterns = MatchPatterns.from();
        } else {
            excludesPatterns = MatchPatterns.from(excludes);
        }

        ZipInputStream inputStream = null;
        try {
            // Lets stream the zip file
            inputStream = new ZipInputStream(location.openConnection().getInputStream());
            ZipEntry jarEntry;

            // Iterate the file entries within that zip. Then make sure it
            // follows the filter given from the user.
            do {
                jarEntry = inputStream.getNextEntry();
                if (jarEntry != null && !jarEntry.isDirectory()) {
                    String fileName = jarEntry.getName();

                    if (includesPatterns.matches(fileName, false)
                            && !excludesPatterns.matches(fileName, false)) {
                        files.add(new URL(base, fileName));
                    }
                }
            } while (jarEntry != null);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to get Zip input stream from '" + location + "'",
                    ioe);
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    /* ignore there is nothing to do */
                }
            }
        }

        return files;
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
     * <p>
     * This method performs property variable substitution on the specified
     * value. If the specified value contains the syntax
     * <tt>&{&lt;prop-name&gt;}</tt> or <tt>${&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt> refers to
     * either a configuration property or a system property, then the
     * corresponding property value is substituted for the variable placeholder.
     * Multiple variable placeholders may exist in the specified value as well
     * as nested variable placeholders, which are substituted from inner most to
     * outer most. Configuration properties override system properties.
     * </p>
     *
     * @param val
     *            The string on which to perform property substitution.
     * @param configuration
     *            Set of configuration properties.
     * @return The value of the specified string after property substitution.
     **/
    public static Object substVars(String val, final PropertyAccessor configuration) {
        return substVars(val, configuration, false);
    }

    /**
     * <p>
     * This method performs property variable substitution on the specified
     * value. If the specified value contains the syntax
     * <tt>&{&lt;prop-name&gt;}</tt> or <tt>${&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt> refers to
     * either a configuration property or a system property, then the
     * corresponding property value is substituted for the variable placeholder.
     * Multiple variable placeholders may exist in the specified value as well
     * as nested variable placeholders, which are substituted from inner most to
     * outer most. Configuration properties override system properties.
     * </p>
     *
     * @param val The string on which to perform property substitution.
     * @param configuration Set of configuration properties.
     * @param doEscape if handling escaped characters is desired
     * @return The value of the specified string after property substitution.
     **/
    public static Object substVars(String val, final PropertyAccessor configuration,
            boolean doEscape) {
        Object substVars = substVars(val, configuration, Delimiter.AMPERSAND, doEscape);
        if (substVars instanceof String) {
            substVars = substVars((String) substVars, configuration, Delimiter.DOLLAR, doEscape);
        }
        return substVars;
    }

    private static Object substVars(String val, final PropertyAccessor propertyAccessor,
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

    private static <T> T getSubstituteValue(Class<T> type, String variable,
            final PropertyAccessor configuration) {
        T substValue = null;
        if (String.class.isAssignableFrom(type)) {
            // Get the value of the deepest nested variable
            // placeholder.
            // Try to configuration properties first.
            substValue = (configuration != null) ? (T) configuration.get(variable) : null;
            if (substValue == null) {
                // Ignore unknown property values.
                substValue = (T) System.getProperty(variable);
            }
        } else {
            substValue = (configuration != null) ? (T) configuration.get(variable) : null;
            if (substValue == null) {
                // Ignore unknown property values.
                substValue = (T) System.getProperty(variable);
            }
        }
        return substValue;
    }
}
