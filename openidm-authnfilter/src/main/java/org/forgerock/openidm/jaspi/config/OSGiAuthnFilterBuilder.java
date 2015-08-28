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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.config;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;

import org.forgerock.jaspi.JaspiRuntimeFilter;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.servletregistration.RegisteredFilter;
import org.forgerock.openidm.servletregistration.ServletRegistration;

import static org.forgerock.openidm.jaspi.auth.AuthenticationService.SERVER_AUTH_CONTEXT_KEY;
import static org.forgerock.openidm.servletregistration.RegisteredFilter.FILTER_ORDER;
import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_CLASS;
import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_CLASS_PATH_URLS;
import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_PRE_INVOKE_ATTRIBUTES;
import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_URL_PATTERNS;
import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_INIT_PARAMETERS;
import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_SYSTEM_PROPERTIES;
import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_SCRIPT_EXTENSIONS;

import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 *         "scriptExtensions" : {
 *             "augmentSecurityContext" : {
 *                 "type" : "text/javascript",
 *                 "file" : "auth/authnPopulateContext.js"
 *             }
 *         },
 *         "sessionModule" : {
 *             "name" : "JWT_SESSION",
 *                 "properties" : {
 *                     "someSetting" : "some-value"
 *                 }
 *         },
 *         "authModules" : [
 *             {
 *                 "name" : "IWA",
 *                 "properties" : {
 *                     "someSetting" : "some-value"
 *                 }
 *             },
 *             {
 *                 "name" : "PASSTHROUGH",
 *                 "properties" : {
 *                     "someSetting" : "some-value"
 *                 }
 *             }
 *         ]
 *     }
 * }
 *     </code>
 * </pre>
 *
 * Also,
 * <pre>
 *     <code>
 *         OSGiAuthnFilterBuilder.getInstance()
 *     </code>
 * </pre>
 * may be used to inject the OSGi dependencies into other modules as an OSGiAuthnFilterHelper.
 *
 */
@Component(name = OSGiAuthnFilterBuilder.PID, immediate = true, policy = ConfigurationPolicy.IGNORE)
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Commons Authentication Filter Configuration")
})
public class OSGiAuthnFilterBuilder implements OSGiAuthnFilterHelper {

    /** The PID for this component. */
    public static final String PID = "org.forgerock.openidm.authnfilterbuilder";

    // config tokens
    private static final String CONFIG_ADDITIONAL_URL_PATTERNS = "additionalUrlPatterns";

    private static final int DEFAULT_FILTER_ORDER = 100;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static OSGiAuthnFilterBuilder instance;

    // ----- Declarative Service Implementation

    @Reference(
            name = "AuthenticationConfig",
            referenceInterface = AuthenticationConfig.class,
            policy = ReferencePolicy.STATIC,
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            bind = "bindAuthenticationConfig",
            unbind = "unBindAuthenticationConfig"
    )
    private JsonValue config;
    private void bindAuthenticationConfig(AuthenticationConfig authenticationConfig) {
        config = authenticationConfig.getConfig();
    }
    private void unBindAuthenticationConfig(AuthenticationConfig authenticationConfig) {
        config = null;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    CryptoService cryptoService;

    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    ConnectionFactory connectionFactory;

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected ScriptRegistry scriptRegistry;

    @Reference(
            name = "ref_ServletFilterRegistration",
            referenceInterface = ServletRegistration.class,
            policy = ReferencePolicy.STATIC,
            cardinality = ReferenceCardinality.MANDATORY_UNARY
    )
    ServletRegistration servletFilterRegistration;

    private RegisteredFilter filter;

    /**
     * Configures the commons Authentication Filter with the configuration in the authentication.json file.
     *
     * @param context The ComponentContext.
     */
    @Activate
    protected void activate(ComponentContext context) throws Exception {
        instance = this;
        JsonValue scriptExtensions = config.get(SERVLET_FILTER_SCRIPT_EXTENSIONS);
        List<String> additionalUrlPatterns = getAdditionalUrlPatterns(config);
        configureAuthenticationFilter(config);

        registerAuthnFilter(scriptExtensions, additionalUrlPatterns);
    }

    /**
     * Gets the additional url parameters value out of the authentication.json configuration.
     *
     * @param config The authentication json configuration.
     * @return The additional url parameters, if any.
     */
    private List<String> getAdditionalUrlPatterns(final JsonValue config) {
        List<String> additionalUrlPatterns = config
                .get(SERVER_AUTH_CONTEXT_KEY)
                .get(CONFIG_ADDITIONAL_URL_PATTERNS)
                .defaultTo(new ArrayList<String>(0))
                .asList(String.class);
        config.get(SERVER_AUTH_CONTEXT_KEY).remove(CONFIG_ADDITIONAL_URL_PATTERNS);
        return additionalUrlPatterns;
    }

    /**
     * Configures the commons Authentication Filter with the given configuration.
     *
     * @param jsonConfig The authentication configuration.
     */
    private void configureAuthenticationFilter(JsonValue jsonConfig) throws Exception {

        if (jsonConfig == null) {
            // No configurations found
            logger.warn("Could not find any configurations for the AuthnFilter, filter will not function");
            return;
        }
        JaspiRuntimeConfigurationFactory.INSTANCE.setModuleConfiguration(jsonConfig);
    }

    /**
     * Registers the Authentication Filter in OSGi.
     *
     * @param scriptExtensions A map of script extensions to register for the filter, namely,
     *                         the location of the authn augment context script.
     * @param additionalUrlPatterns additional url patterns to which to apply the filter
     * @throws Exception If a problem occurs whilst registering the filter.
     */
    private void registerAuthnFilter(JsonValue scriptExtensions, List<String> additionalUrlPatterns)
            throws Exception {

        Map<String, String> initParams = new HashMap<>();
        initParams.put("module-configuration-factory-class", JaspiRuntimeConfigurationFactory.class.getName());
        initParams.put("logging-configurator-class", JaspiRuntimeConfigurationFactory.class.getName());
        initParams.put("audit-api-class", JaspiRuntimeConfigurationFactory.class.getName());

        List<String> urlPatterns = new ArrayList<>();
        urlPatterns.add("/openidm/*");
        urlPatterns.addAll(additionalUrlPatterns);

        Map<String, Object> filterConfig = new HashMap<>();
        filterConfig.put(SERVLET_FILTER_CLASS_PATH_URLS, new ArrayList<String>());
        filterConfig.put(SERVLET_FILTER_SYSTEM_PROPERTIES, new HashMap<String, Object>());
        filterConfig.put(SERVLET_FILTER_PRE_INVOKE_ATTRIBUTES, new HashMap<String, Object>());
        filterConfig.put(SERVLET_FILTER_INIT_PARAMETERS, initParams);
        filterConfig.put(SERVLET_FILTER_URL_PATTERNS, urlPatterns);
        filterConfig.put(SERVLET_FILTER_CLASS, JaspiRuntimeFilter.class.getCanonicalName());
        filterConfig.put(FILTER_ORDER, DEFAULT_FILTER_ORDER);
        if (!scriptExtensions.isNull() && scriptExtensions.isMap()) {
            filterConfig.put(SERVLET_FILTER_SCRIPT_EXTENSIONS, scriptExtensions.asMap());
        }

        JsonValue filterConfigJson = new JsonValue(filterConfig);

        filter = servletFilterRegistration.registerFilter(filterConfigJson);
    }

    /**
     * Unregisters the authentication filter.
     *
     * @param context The ComponentContext.
     */
    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        if (filter != null) {
            try {
                servletFilterRegistration.unregisterFilter(filter);
                JaspiRuntimeConfigurationFactory.INSTANCE.clear();
                logger.info("Unregistered authentication filter.");
            } catch (Exception ex) {
                logger.warn("Failure reported during unregistering of authentication filter: {}", ex.getMessage(), ex);
            }
        }
        instance = null;
    }

    /**
     * Get this instance of the OSGiAuthnFilterHelper.
     *
     * @return the instance of the OSGiAuthnFilterHelper.
     */
    public static OSGiAuthnFilterHelper getInstance() {
        return instance;
    }

    // ----- OSGiAuthnFilterHelper Implementation

    /**
     * Returns the Crypto Service instance.
     *
     * @return The Crypto Service instance.
     */
    public CryptoService getCryptoService() {
        return cryptoService;
    }

    /**
     * Returns the ScriptRegistry instance
     *
     * @return the ScriptRegistry instance
     */
    public ScriptRegistry getScriptRegistry() {
        return scriptRegistry;
    }

    /**
     * Returns the ConnectionFactory instance
     *
     * @return The ConnectionFactory instance
     */
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
}
