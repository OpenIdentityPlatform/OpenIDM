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

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.framework.AuthenticationFilter;
import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.base.Predicate;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.util.JettyPropertyUtil;
import org.forgerock.openidm.jaspi.auth.AuthenticationService;
import org.forgerock.openidm.jaspi.modules.IDMAuthModule;
import org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper;
import org.forgerock.openidm.servletregistration.ServletRegistration;

import static org.forgerock.caf.authentication.framework.AuthenticationFilter.*;
import static org.forgerock.caf.authentication.framework.AuthenticationFilter.AuthenticationModuleBuilder.configureModule;
import static org.forgerock.openidm.jaspi.auth.AuthenticationService.MODULE_CONFIG_ENABLED;
import static org.forgerock.openidm.jaspi.auth.AuthenticationService.SERVER_AUTH_CONTEXT_KEY;

import javax.security.auth.message.module.ServerAuthModule;

import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

//    private RegisteredFilter filter;

    @Reference(policy = ReferencePolicy.DYNAMIC, target="(service.pid=org.forgerock.openidm.jaspi.config)")
    private AuthFilterWrapper authFilterWrapper;

    /**
     * Configures the commons Authentication Filter with the configuration in the authentication.json file.
     *
     * @param context The ComponentContext.
     */
    @Activate
    protected void activate(ComponentContext context) throws Exception {
        // TODO-crest3/caf2 is there a CHF Filter analogue to script extensions, url patterns
        // JsonValue scriptExtensions = config.get(SERVLET_FILTER_SCRIPT_EXTENSIONS);
        // List<String> additionalUrlPatterns = getAdditionalUrlPatterns(config);

        authFilterWrapper.setFilter(configureAuthenticationFilter(config));

        // TODO-crest3/caf2 no longer registering servlet filter!
        // registerAuthnFilter(scriptExtensions, additionalUrlPatterns);
    }

    /**
     * Gets the additional url parameters value out of the authentication.json configuration.
     *
     * @param config The authentication json configuration.
     * @return The additional url parameters, if any.
     */
    /* TODO-crest3/caf2 chopping block
    private List<String> getAdditionalUrlPatterns(final JsonValue config) {
        List<String> additionalUrlPatterns = config
                .get(SERVER_AUTH_CONTEXT_KEY)
                .get(CONFIG_ADDITIONAL_URL_PATTERNS)
                .defaultTo(new ArrayList<String>(0))
                .asList(String.class);
        config.get(SERVER_AUTH_CONTEXT_KEY).remove(CONFIG_ADDITIONAL_URL_PATTERNS);
        return additionalUrlPatterns;
    }
    */

    /**
     * Configures the commons Authentication Filter with the given configuration.
     *
     * @param jsonConfig The authentication configuration.
     */
    private AuthenticationFilter configureAuthenticationFilter(JsonValue jsonConfig) throws Exception {

        if (jsonConfig == null) {
            // No configurations found
            logger.warn("Could not find any configurations for the AuthnFilter, filter will not function");
            return null;
        }

        // make copy of config
        final JsonValue moduleConfig = new JsonValue(jsonConfig);
        final JsonValue serverAuthContext = moduleConfig.get(SERVER_AUTH_CONTEXT_KEY).required();
        final JsonValue sessionConfig = serverAuthContext.get(AuthenticationService.SESSION_MODULE_KEY);
        final JsonValue authModulesConfig = serverAuthContext.get(AuthenticationService.AUTH_MODULES_KEY);

        return AuthenticationFilter.builder()
                .logger(logger)
                .auditApi(new JaspiAuditApi(this))
                .sessionModule(processModuleConfiguration(sessionConfig))
                        .authModules(
                                FluentIterable.from(authModulesConfig)
                                        // transform each module config to a builder
                                        .transform(new Function<JsonValue, AuthenticationModuleBuilder>() {
                                            @Override
                                            public AuthenticationModuleBuilder apply(JsonValue authModuleConfig) {
                                                return processModuleConfiguration(authModuleConfig);
                                            }
                                        })
                                        // weed out nulls
                                        .filter(new Predicate<AuthenticationModuleBuilder>() {
                                                    @Override
                                                    public boolean apply(AuthenticationModuleBuilder builder) {
                                                        return builder != null;
                                                    }
                                                }
                                        )
                                        .toList())
                        .build();
    }

    /**
     * Process the module configuration for a specific module, checking to see if the module is enabled and
     * resolving the module class name if an alias is used.
     *
     * @param moduleConfig The specific module configuration json.
     * @return Whether the module is enabled or not.
     */
    private AuthenticationModuleBuilder processModuleConfiguration(JsonValue moduleConfig) {

        if (moduleConfig.isDefined(MODULE_CONFIG_ENABLED) && !moduleConfig.get(MODULE_CONFIG_ENABLED).asBoolean()) {
            return null;
        }
        moduleConfig.remove(MODULE_CONFIG_ENABLED);

        AsyncServerAuthModule module;
        if (moduleConfig.isDefined("name")) {
            module = moduleConfig.get("name").asEnum(IDMAuthModule.class).newInstance(this);
        } else {
            logger.warn("Unable to create auth module from config " + moduleConfig.toString());
            return null;
        }

        JsonValue moduleProperties = moduleConfig.get("properties");
        if (moduleProperties.isDefined("privateKeyPassword")) {
            // decrypt/de-obfuscate privateKey password
            moduleProperties.put("privateKeyPassword",
                    JettyPropertyUtil.decryptOrDeobfuscate(moduleProperties.get("privateKeyPassword").asString()));
        }

        if (moduleProperties.isDefined("keystorePassword")) {
            // decrypt/de-obfuscate keystore password
            moduleProperties.put("keystorePassword",
                    JettyPropertyUtil.decryptOrDeobfuscate(moduleProperties.get("keystorePassword").asString()));
        }

        // wrap all auth modules in an IDMJaspiModuleWrapper to apply the IDM business logic
        return configureModule(new IDMJaspiModuleWrapper(this, module))
                .withSettings(moduleProperties.asMap());
    }

    /**
     * Registers the Authentication Filter in OSGi.
     *
     * @param scriptExtensions A map of script extensions to register for the filter, namely,
     *                         the location of the authn augment context script.
     * @param additionalUrlPatterns additional url patterns to which to apply the filter
     * @throws Exception If a problem occurs whilst registering the filter.
     */
/*
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
*/

    /**
     * Unregisters the authentication filter.
     *
     * @param context The ComponentContext.
     */
    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        if (authFilterWrapper != null) {
            try {
// TODO-crest3/caf2 no longer registering as servlet filter!
//                servletFilterRegistration.unregisterFilter(filter);
//                logger.info("Unregistered authentication filter.");
                authFilterWrapper.setFilter(AuthFilterWrapper.PASSTHROUGH_FILTER);
            } catch (Exception ex) {
                logger.warn("Failure reported during unregistering of authentication filter: {}", ex.getMessage(), ex);
            }
        }
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
