/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock Inc. All rights reserved.
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

package org.forgerock.openidm.config;

import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonTransformer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.factory.CryptoServiceFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility to handle enhanced configuration, including nested lists and maps
 * to represent JSON based structures.
 * 
 * @author aegloff
 */
public class JSONEnhancedConfig implements EnhancedConfig {

    // The key in the OSGi configuration dictionary holding the complete JSON
    // configuration string
    public final static String JSON_CONFIG_PROPERTY = "jsonconfig";

    private final static Logger logger = LoggerFactory.getLogger(JSONEnhancedConfig.class);

    private final ObjectMapper mapper = new ObjectMapper();

    // Disable the property escaping by default
    private boolean doEscape = false;

    // Keep track of the cryptography OSGi service
    private static ServiceTracker cryptoTracker;

    /**
     * Creates a new instance of {@code JSONEnhancedConfig}.
     *
     * @return new instance of this class.
     */
    public static JSONEnhancedConfig newInstance() {
        return new JSONEnhancedConfig();
    }

    /**
     * Sets the escaping mode.
     * <p/>
     * If {@code true} then this instance processes the escapes {@code \} character in the sting values otherwise it
     * does not handle specially the {@code \} character.
     *
     * @param escape
     *         {@code true} to enable or {@code false} to disable.
     * @return this instance.
     */
    public JSONEnhancedConfig setEscaping(boolean escape) {
        doEscape = escape;
        return this;
    }

    /**
     * {@inheritdoc}
     */
    public Map<String, Object> getConfiguration(ComponentContext compContext)
            throws InvalidException, InternalErrorException {

        JsonValue confValue = getConfigurationAsJson(compContext);
        return confValue.asMap();
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue getConfigurationAsJson(ComponentContext compContext) throws InvalidException,
            InternalErrorException {

        Dictionary dict = null;
        if (compContext != null) {
            dict = compContext.getProperties();
        }
        String servicePid = (String) compContext.getProperties().get(Constants.SERVICE_PID);

        JsonValue conf = getConfiguration(dict, compContext.getBundleContext(), servicePid);

        return conf;
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue getConfiguration(Dictionary<String, Object> dict, BundleContext context,
            String servicePid) throws InvalidException, InternalErrorException {
        return getConfiguration(dict, context, servicePid, true);
    }

    /**
     * {@see getConfiguration(Dictionary<String, Object>, BundleContext, String)
     * * }
     * 
     * @param decrypt
     *            true if any encrypted values should be decrypted in the result
     */
    public JsonValue getConfiguration(Dictionary<String, Object> dict, BundleContext context,
            String servicePid, boolean decrypt) throws InvalidException, InternalErrorException {
        JsonValue jv = new JsonValue(new LinkedHashMap<String, Object>());

        if (dict != null) {
            Map<String, Object> parsedConfig = null;
            String jsonConfig = (String) dict.get(JSON_CONFIG_PROPERTY);
            logger.trace("Get configuration from JSON config property {}", jsonConfig);

            try {
                if (jsonConfig != null && jsonConfig.trim().length() > 0) {
                    parsedConfig = mapper.readValue(jsonConfig, Map.class);
                }
            } catch (Exception ex) {
                throw new InvalidException("Configuration for " + servicePid
                        + " could not be parsed: " + ex.getMessage(), ex);
            }
            logger.trace("Parsed configuration {}", parsedConfig);

            try {
                jv = new JsonValue(parsedConfig);
            } catch (JsonValueException ex) {
                throw new InvalidException("Component configuration for " + servicePid
                        + " is invalid: " + ex.getMessage(), ex);
            }
        }
        logger.debug("Configuration for {}: {}", servicePid, jv);

        JsonValue decrypted = jv;
        if (!jv.isNull()) {
            decrypted.getTransformers().add(new PropertyTransformer());
            decrypted.applyTransformers();
            decrypted = decrypted.copy();
        }
        if (decrypt && dict != null && !jv.isNull() && context != null
                && context.getBundle() != null) { // todo: different way to
                                                  // handle mock unit tests
            decrypted = decrypt(jv, context);
        }

        return decrypted;
    }

    private JsonValue decrypt(JsonValue value, BundleContext context) throws JsonException,
            InternalErrorException {
        return getCryptoService(context).decrypt(value); // makes a decrypted
                                                         // copy
    }

    private CryptoService getCryptoService(BundleContext context) throws InternalErrorException {
        return CryptoServiceFactory.getInstance();
    }

    private class PropertyTransformer implements JsonTransformer {
        @Override
        public void transform(JsonValue value) throws JsonException {
            if (null != value && value.isString()) {
                value.setObject(substVars(value.asString(), IdentityServer.getInstance()));
            }
        }
    }

    private <T> T getSubstituteValue(Class<T> type, String variable,
            final IdentityServer identityServer) {
        T substValue = null;
        if (String.class.isAssignableFrom(type)) {
            // Get the value of the deepest nested variable
            // placeholder.
            // Try to configuration properties first.
            substValue =
                    (identityServer != null) ? (T) identityServer.getProperty(variable, null)
                            : null;
            if (substValue == null) {
                // Ignore unknown property values.
                substValue = (T) System.getProperty(variable);
            }
        } else {
            // TODO Support this in the future releases
            substValue =
                    (identityServer != null) ? (T) identityServer.getProperty(variable, null)
                            : null;
            if (substValue == null) {
                // Ignore unknown property values.
                substValue = (T) System.getProperty(variable);
            }
        }
        if (null == substValue){
            logger.warn("Undefined property '{}' used for substitution.", variable);
        }
        return substValue;
    }

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
    private Object substVars(String val, final IdentityServer identityServer) {

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

}
