/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock Inc. All rights reserved.
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

package org.forgerock.openidm.config.enhanced;

import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonTransformer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.PropertyUtil;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.factory.CryptoServiceFactory;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.util.Reject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service to handle enhanced configuration, including nested lists and maps
 * to represent JSON based structures.
 */
@Component(name = JSONEnhancedConfig.PID,
        policy = ConfigurationPolicy.IGNORE,
        description = "OpenIDM Enhanced Config Service",
        immediate = true,
        metatype = true)
@Service
@Properties(
        @Property(name = "suppressMetatypeWarning", value = "true")
)
public class JSONEnhancedConfig implements EnhancedConfig {

    public static final String PID = "org.forgerock.openidm.config.enhanced";

    /**
     * The key in the OSGi configuration dictionary holding the complete JSON
     * configuration string
     */
    public final static String JSON_CONFIG_PROPERTY = "jsonconfig";

    /**
     * Setup logging for the {@link JSONEnhancedConfig}.
     */
    private final static Logger logger = LoggerFactory.getLogger(JSONEnhancedConfig.class);

    public String getConfigurationFactoryPid(ComponentContext compContext) {
        Object o = compContext.getProperties().get(ServerConstants.CONFIG_FACTORY_PID);
        if (o instanceof String) {
            return (String) o;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> getConfiguration(ComponentContext compContext)
            throws InvalidException, InternalErrorException {

        JsonValue confValue = getConfigurationAsJson(compContext);
        return confValue.asMap();
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue getConfigurationAsJson(ComponentContext compContext)
            throws InvalidException, InternalErrorException {
        Reject.ifNull(compContext);

        Dictionary<String, Object> dict = compContext.getProperties();
        String servicePid = (String) dict.get(Constants.SERVICE_PID);

        return getConfiguration(dict, compContext.getBundleContext(), servicePid);
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue getConfiguration(Dictionary<String, Object> dict, BundleContext context,
            String servicePid) throws InvalidException, InternalErrorException {
        return getConfiguration(dict, servicePid, true);
    }

    /**
     * {@see getConfiguration(Dictionary, BundleContext, String)}
     *
     * @param decrypt
     *            true if any encrypted values should be decrypted in the result
     */
    public JsonValue getConfiguration(Dictionary<String, Object> dict, String servicePid,
            boolean decrypt) throws InvalidException, InternalErrorException {
        JsonValue jv = new JsonValue(new LinkedHashMap<String, Object>());

        if (dict != null) {
            String jsonConfig = (String) dict.get(JSON_CONFIG_PROPERTY);
            logger.trace("Get configuration from JSON config property {}", jsonConfig);

            try {
                if (jsonConfig != null && jsonConfig.trim().length() > 0) {
                    jv = JsonUtil.parseStringified(jsonConfig);
                }
            } catch (Exception ex) {
                throw new InvalidException("Configuration for " + servicePid
                        + " could not be parsed: " + ex.getMessage(), ex);
            }
            logger.trace("Parsed configuration {}", jv);

            try {
                jv.required().expect(Map.class);
            } catch (JsonValueException ex) {
                throw new InvalidException("Component configuration for " + servicePid
                        + " is invalid: " + ex.getMessage(), ex);
            }
        }
        logger.debug("Configuration for {}: {}", servicePid, jv);

        JsonValue decrypted = jv;
        if (!jv.isNull()) {
            boolean doEscape = false;
            decrypted.getTransformers().add(new PropertyTransformer(doEscape));
            decrypted.applyTransformers();
            decrypted = decrypted.copy();
        }
        // todo: different way to handle mock unit tests
        if (decrypt && dict != null && !jv.isNull()) {
            decrypted = decrypt(jv);
        }

        return decrypted;
    }

    private JsonValue decrypt(JsonValue value) throws JsonException, InternalErrorException {
        return getCryptoService().decrypt(value); // makes a decrypted copy
    }

    private CryptoService getCryptoService() throws InternalErrorException {
        return CryptoServiceFactory.getInstance();
    }

    private static class PropertyTransformer implements JsonTransformer {

        // Disable the property escaping by default
        private final boolean doEscape;

        public PropertyTransformer(boolean doEscape) {
            this.doEscape = doEscape;
        }

        @Override
        public void transform(JsonValue value) throws JsonException {
            if (null != value && value.isString()) {
                value.setObject(PropertyUtil.substVars(value.asString(), IdentityServer
                        .getInstance(), doEscape));
            }
        }
    }
}
