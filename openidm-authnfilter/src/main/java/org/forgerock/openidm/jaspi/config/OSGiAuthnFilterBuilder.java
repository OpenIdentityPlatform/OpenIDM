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
 * Copyright 2013-2014 ForgeRock AS.
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
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.servletregistration.RegisteredFilter;
import org.forgerock.openidm.servletregistration.ServletRegistration;
import org.forgerock.openidm.router.RouteService;
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
 * @author Jonathan Scudder
 * @author Phill Cunnington
 * @author brmiller
 * @author ckienle
 */
@Component(name = OSGiAuthnFilterBuilder.PID, immediate = true, policy = ConfigurationPolicy.IGNORE)
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Commons Authentication Filter Configuration")
})
public class OSGiAuthnFilterBuilder {

    /** The PID for this component. */
    public static final String PID = "org.forgerock.openidm.authnfilterbuilder";

    // config tokens
    private static final String CONFIG_SERVER_AUTH_CONFIG = "serverAuthConfig";
    private static final String CONFIG_AUTHN_POPULATE_CONTEXT_SCRIPT = "authnPopulateContextScript";
    private static final String CONFIG_ADDITIONAL_URL_PATTERNS = "additionalUrlPatterns";

    private static final int DEFAULT_FILTER_ORDER = 100;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

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

    /**
     * Configures the commons Authentication Filter with the configuration in the authentication.json file.
     *
     * @param context The ComponentContext.
     */
    @Activate
    protected void activate(ComponentContext context) throws Exception {
        instance = this;
        String authnPopulateScriptLocation = getAuthnPopulateContextScript(config);
        List<String> additionalUrlPatterns = getAdditionalUrlPatterns(config);
        configureAuthenticationFilter(config);

        registerAuthnFilter(authnPopulateScriptLocation, additionalUrlPatterns);
    }

    /**
     * Gets the authnPopulateContextScript value out of the authentication.json configuration.
     * <p>
     * If the property is not defined then the default script is used.
     *
     * @param config The authentication json configuration.
     * @return The authnPopulateContextScript location.
     */
    private String getAuthnPopulateContextScript(final JsonValue config) {
        String authnPopulateScriptLocation = config.get(CONFIG_SERVER_AUTH_CONFIG).get(CONFIG_AUTHN_POPULATE_CONTEXT_SCRIPT)
                .defaultTo("bin/defaults/script/auth/authnPopulateContext.js").asString();
        config.get(CONFIG_SERVER_AUTH_CONFIG).remove(CONFIG_AUTHN_POPULATE_CONTEXT_SCRIPT);
        return authnPopulateScriptLocation;
    }

    /**
     * Getes the additional url parameters value out of the authentication.json configuration.
     *
     * @param config The authentication json configuration.
     * @return The additional url parameters, if any.
     */
    private List<String> getAdditionalUrlPatterns(final JsonValue config) {
        List<String> additionalUrlPatterns = config.get(CONFIG_SERVER_AUTH_CONFIG).get(CONFIG_ADDITIONAL_URL_PATTERNS)
                .defaultTo(new ArrayList<String>(0)).asList(String.class);
        config.get(CONFIG_SERVER_AUTH_CONFIG).remove(CONFIG_ADDITIONAL_URL_PATTERNS);
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

    // ----- Declarative Service Implementation
    private static OSGiAuthnFilterBuilder instance;

    @Reference(target = "("+ServerConstants.ROUTER_PREFIX+"=/repo/*)")
    RouteService repositoryRoute;

    @Reference(policy = ReferencePolicy.DYNAMIC)
    CryptoService cryptoService;

    /**
     * Returns the Router instance.
     *
     * @return The Router instance.
     */
    public static RouteService getRouter() {
        return instance.repositoryRoute;
    }

    /**
     * Returns the Crypto Service instance.
     *
     * @return The Crypto Service instance.
     */
    public static CryptoService getCryptoService() {
        return instance.cryptoService;
    }

    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    /**
     * Returns the ConnectionFactory instance
     *
     * @return The ConnectionFactory instance
     */
    public static ConnectionFactory getConnectionFactory() {
        return instance.connectionFactory;
    }

    @Reference(
            name = "ref_ServletFilterRegistration",
            referenceInterface = ServletRegistration.class,
            policy = ReferencePolicy.STATIC,
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            bind = "bindServletFilterRegistration",
            unbind = "unbindServletFilterRegistration"
    )
    private ServletRegistration servletFilterRegistration;
    private void bindServletFilterRegistration(ServletRegistration servletFilterRegistration) {
        this.servletFilterRegistration = servletFilterRegistration;
    }
    private void unbindServletFilterRegistration(ServletRegistration servletFilterRegistration) {
        this.servletFilterRegistration = null;
    }

    private RegisteredFilter filter;

    /**
     * Registers the Authentication Filter in OSGi.
     *
     * @param authnPopulateScriptLocation The location of the authn augment context script.
     * @param additionalUrlPatterns additional url patterns to which to apply the filter
     * @throws Exception If a problem occurs whilst registering the filter.
     */
    private void registerAuthnFilter(String authnPopulateScriptLocation, List<String> additionalUrlPatterns)
            throws Exception {

        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("module-configuration-factory-class", JaspiRuntimeConfigurationFactory.class.getName());
        initParams.put("logging-configurator-class", JaspiRuntimeConfigurationFactory.class.getName());

        Map<String, String> augmentSecurityContext = new HashMap<String, String>();
        augmentSecurityContext.put("type", "text/javascript");
        augmentSecurityContext.put("file", authnPopulateScriptLocation);

        Map<String, Object> scriptExtensions = new HashMap<String, Object>();
        scriptExtensions.put("augmentSecurityContext", augmentSecurityContext);

        List<String> urlPatterns = new ArrayList<String>();
        urlPatterns.add("/openidm/*");
        urlPatterns.addAll(additionalUrlPatterns);

        Map<String, Object> filterConfig = new HashMap<String, Object>();
        filterConfig.put("classPathURLs", new ArrayList<String>());
        filterConfig.put("systemProperties", new HashMap<String, Object>());
        filterConfig.put("requestAttributes", new HashMap<String, Object>());
        filterConfig.put("initParams", initParams);
        filterConfig.put("scriptExtensions", scriptExtensions);
        filterConfig.put("urlPatterns", urlPatterns);
        filterConfig.put("filterClass", JaspiRuntimeFilter.class.getCanonicalName());
        filterConfig.put("order", DEFAULT_FILTER_ORDER);

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
    }
}
