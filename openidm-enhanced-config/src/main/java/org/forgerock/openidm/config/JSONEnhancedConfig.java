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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.config;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

import org.forgerock.openidm.config.InternalErrorException;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.factory.CryptoServiceFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility to handle enhanced configuration, including nested lists and maps to 
 * represent JSON based structures. 
 *
 * @author aegloff
 */
public class JSONEnhancedConfig implements EnhancedConfig {

    // The key in the OSGi configuration dictionary holding the complete JSON configuration string
    public final static String JSON_CONFIG_PROPERTY = "jsonconfig";
    
    final static Logger logger = LoggerFactory.getLogger(JSONEnhancedConfig.class);
    
    private final ObjectMapper mapper = new ObjectMapper();

    // Keep track of the cryptography OSGi service
    private static ServiceTracker cryptoTracker;
    
    /**
     *  {@inheritdoc}
     */
    public Map<String, Object> getConfiguration(ComponentContext compContext) throws InvalidException, InternalErrorException { 

        JsonValue confValue = getConfigurationAsJson(compContext);
        return confValue.asMap();
    }
    
    /**
     * {@inheritDoc}
     */
    public JsonValue getConfigurationAsJson(ComponentContext compContext) throws InvalidException, InternalErrorException {
        
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
    public JsonValue getConfiguration(Dictionary<String, Object> dict, BundleContext context, String servicePid) 
                throws InvalidException, InternalErrorException {
        return getConfiguration(dict, context, servicePid, true);
    }
        
    /**
     * {@see getConfiguration(Dictionary<String, Object>, BundleContext, String) }
     * @param decrypt true if any encrypted values should be decrypted in the result
     */
    public JsonValue getConfiguration(Dictionary<String, Object> dict, BundleContext context, String servicePid, boolean decrypt) 
                throws InvalidException, InternalErrorException {        
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
                throw new InvalidException("Configuration for " + servicePid + " could not be parsed: " + ex.getMessage(), ex);
            }
            logger.trace("Parsed configuration {}", parsedConfig);
            
            try {
                jv = new JsonValue(parsedConfig);
            } catch (JsonValueException ex) {
                throw new InvalidException("Component configuration for " + servicePid + " is invalid: " + ex.getMessage(), ex);
            }
        }
        logger.debug("Configuration for {}: {}", servicePid , jv);

        JsonValue decrypted = jv;
        if (decrypt && dict != null && !jv.isNull() 
                && context != null && context.getBundle() != null) { // todo: different way to handle mock unit tests
            decrypted = decrypt(jv, context);
        }
        
        return decrypted;
    }
    
    private JsonValue decrypt(JsonValue value, BundleContext context) throws JsonException, InternalErrorException {
        return getCryptoService(context).decrypt(value); // makes a decrypted copy
    }
    
    private CryptoService getCryptoService(BundleContext context)
            throws InternalErrorException {
        return CryptoServiceFactory.getInstance();
    }
}
