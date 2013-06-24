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
 * Copyright 2013 ForgeRock Inc. All rights reserved.
 */

package org.forgerock.openidm.jaspi.config;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Modified;
import org.forgerock.jaspi.container.config.Configuration;
import org.forgerock.jaspi.container.config.ConfigurationManager;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.message.AuthException;
import java.util.HashMap;
import java.util.Map;

/**
 * Configures the authentication chains based on configuration obtained through OSGi.
 *
 * Example:
 *
 * <pre>
 *     <code>
 * {
 *     "serverAuthConfig" : {
 *         "iwaAdPassthrough" : {
 *             "sessionModule" : {
 *                 "className" : "org.forgerock.jaspi.modules.JwtSessionModule"
 *             },
 *             "authModules" : [
 *                 {
 *                     "className" : "org.forgerock.openidm.jaspi.modules.IWAModule",
 *                     "someSetting" : "some-value"
 *                 },
 *                 {
 *                     "className" : "org.forgerock.openidm.jaspi.modules.ADPassthroughModule",
 *                     "someSetting" : "some-value"
 *                 }
 *             ]
 *         },
 *         "adPassthroughOnly" : {
 *             "authModules" : [
 *                 {
 *                     "className" : "org.forgerock.openidm.jaspi.modules.ADPassthroughModule",
 *                     "passThroughAuth" : "system/AD/account"
 *                 }
 *             ]
 *         }
 *     }
 * }
 *     </code>
 * </pre>
 *
 */
@Component(name = OSGiAuthnFilterBuilder.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
public class OSGiAuthnFilterBuilder {

    public static final String PID = "org.forgerock.openidm.authnfilter";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Configures the commons Authentication Filter with the configuration in the authentication.json file.
     *
     * @param context The ComponentContext.
     */
    @Activate
    protected void activate(ComponentContext context) {
        EnhancedConfig config = JSONEnhancedConfig.newInstance();
        JsonValue jsonConfig = config.getConfigurationAsJson(context);
        configureAuthenticationFilter(jsonConfig);
    }

    @Modified
    public void modified(ComponentContext context) throws Exception {
        EnhancedConfig config = JSONEnhancedConfig.newInstance();
        JsonValue jsonConfig = config.getConfigurationAsJson(context);
        ConfigurationManager.unconfigure();
        configureAuthenticationFilter(jsonConfig);
    }

    private void configureAuthenticationFilter(JsonValue jsonConfig) {

        if (jsonConfig == null ) {
            // No configurations found
            logger.warn("Could not find any configurations for the AuthnFilter, filter will not function");
            return;
        }

        Configuration configuration = new Configuration();
        // For each ServerAuthConfig
        for (String s : jsonConfig.get("serverAuthConfig").required().keys()) {
            Map<String, Object> contextProperties = jsonConfig.get("serverAuthConfig").required().get(s).asMap();
            configuration.addAuthContext(s, contextProperties);
        }

        try {
            ConfigurationManager.configure(configuration);
        } catch (AuthException e) {
            logger.error("Failed to configure the commons Authentication Filter");
        }
    }
}