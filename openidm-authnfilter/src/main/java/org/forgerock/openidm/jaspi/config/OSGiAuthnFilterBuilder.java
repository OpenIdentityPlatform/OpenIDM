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
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.forgerock.jaspi.container.config.AuthContextConfiguration;
import org.forgerock.jaspi.container.config.Configuration;
import org.forgerock.jaspi.container.config.ConfigurationManager;
import org.forgerock.jaspi.filter.AuthNFilter;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.filterregistration.ServletFilterRegistration;
import org.forgerock.openidm.jaspi.modules.IDMAuthModule;
import org.forgerock.openidm.jaspi.modules.IDMAuthenticationAuditLogger;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.ObjectSet;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.message.AuthException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
 *         "iwaAdPassthrough" : {
 *             "sessionModule" : {
 *                 "name" : "JWT_SESSION"
 *             },
 *             "authModules" : [
 *                 {
 *                     "name" : "IWA",
 *                     "someSetting" : "some-value"
 *                 },
 *                 {
 *                     "name" : "PASSTHROUGH",
 *                     "someSetting" : "some-value"
 *                 }
 *             ]
 *         },
 *         "adPassthroughOnly" : {
 *             "authModules" : [
 *                 {
 *                     "name" : "PASSTHROUGH",
 *                     "passThroughAuth" : "system/AD/account"
 *                 }
 *             ]
 *         }
 *     }
 * }
 *     </code>
 * </pre>
 *
 * @author Jonathan Scudder
 * @author Phill Cunnington
 */
@Component(name = OSGiAuthnFilterBuilder.PID, immediate = true, policy = ConfigurationPolicy.IGNORE)
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Commons Authentication Filter Configuration")
})
public class OSGiAuthnFilterBuilder {

    /** The PID for this component. */
    public static final String PID = "org.forgerock.openidm.authnfilterbuilder";

    private static final String DEFAULT_LOGGER_CLASS_NAME = IDMAuthenticationAuditLogger.class.getCanonicalName();

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference(
            name = "AuthenticationConfig",
            referenceInterface = AuthenticationConfig.class,
            policy = ReferencePolicy.DYNAMIC,
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
        ConfigurationManager.unconfigure();
        registerAuthnFilter();
        configureAuthenticationFilter(config);
    }

    /**
     * Configures the commons Authentication Filter with the given configuration.
     *
     * @param jsonConfig The authentication configuration.
     */
    private void configureAuthenticationFilter(JsonValue jsonConfig) {

        if (jsonConfig == null) {
            // No configurations found
            logger.warn("Could not find any configurations for the AuthnFilter, filter will not function");
            return;
        }

        JsonValue serverAuthConfig = jsonConfig.get("serverAuthConfig").required();

        Configuration configuration = new Configuration();
        String auditLoggerClassName = serverAuthConfig.get("auditLogger").defaultTo(DEFAULT_LOGGER_CLASS_NAME).asString();
        configuration.setAuditLoggerClassName(auditLoggerClassName);

        // For each ServerAuthConfig
        for (String serverAuthConfigKey : serverAuthConfig.keys()) {
            if ("auditLogger".equals(serverAuthConfigKey)) {
                continue;
            } else {
                AuthContextConfiguration authContextConfiguration = configuration.addAuthContext(serverAuthConfigKey);
                addAuthContext(authContextConfiguration, serverAuthConfig.get(serverAuthConfigKey));
                authContextConfiguration.done();
            }
        }

        try {
            ConfigurationManager.configure(configuration);
        } catch (AuthException e) {
            logger.error("Failed to configure the commons Authentication Filter");
        }
    }

    /**
     * Adds a configured Auth Context to the authentication filter.
     * <p>
     * The Auth Context details the list of Session and Auth modules in the authentication chain.
     *
     * @param authContextConfiguration The AuthContextConfiguration builder instance.
     * @param authContextConfig The auth context configuration.
     */
    private void addAuthContext(AuthContextConfiguration authContextConfiguration, JsonValue authContextConfig) {

        JsonValue sessionModuleConfig = authContextConfig.get("sessionModule");
        if (sessionModuleConfig != null) {
            Map<String, Object> moduleProperties = getAuthModuleProperties(sessionModuleConfig);
            if (moduleProperties != null) {
                authContextConfiguration.setSessionModule(moduleProperties);
            }
        }

        Iterator<JsonValue> authModulesIter = authContextConfig.get("authModules").required().iterator();
        while (authModulesIter.hasNext()) {
            JsonValue authModuleConfig = authModulesIter.next();
            Map<String, Object> moduleProperties = getAuthModuleProperties(authModuleConfig);
            if (moduleProperties != null) {
                authContextConfiguration.addAuthenticationModule(moduleProperties);
            }
        }
    }

    /**
     * Parses the auth module configuration into a map of module properties.
     *
     * @param authModuleConfig The auth module configuration.
     * @return The auth module properties.
     */
    private Map<String, Object> getAuthModuleProperties(JsonValue authModuleConfig) {

        boolean enabled = authModuleConfig.get("enabled").defaultTo(true).asBoolean();
        if (enabled) {

            String className = resolveAuthModuleClassName(authModuleConfig.get("name").asString());

            Map<String, Object> moduleProperties = new HashMap<String, Object>(authModuleConfig.asMap());
            moduleProperties.remove("enabled");
            moduleProperties.remove("name");
            moduleProperties.put("className", className);

            return moduleProperties;
        }

        return null;
    }

    /**
     * Resolves the given auth module name from either a core IDM auth module name from {@link IDMAuthModule} or
     * a fully qualified class name of the auth module.
     *
     * @param authModuleName The auth module name.
     * @return The auth module class name.
     */
    private String resolveAuthModuleClassName(String authModuleName) {
        try {
            return IDMAuthModule.valueOf(authModuleName).getAuthModuleClass().getCanonicalName();
        } catch (IllegalArgumentException e) {
            return authModuleName;
        }
    }

    @Reference(
            name = "ref_Auth_JsonResourceRouterService",
            referenceInterface = JsonResource.class,
            bind = "bindRouter",
            unbind = "unbindRouter",
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            policy = ReferencePolicy.STATIC,
            target = "(service.pid=org.forgerock.openidm.router)"
    )
    /** The Router service. */
    private static ObjectSet router;

    /**
     * Binds the JsonResource router to the router member variable.
     *
     * @param router The JsonResource router to bind.
     */
    private void bindRouter(JsonResource router) {
        OSGiAuthnFilterBuilder.router = new JsonResourceObjectSet(router);
    }

    /**
     * Unbinds the JsonResource router from the router member variable.
     *
     * @param router The JsonResource router to unbind.
     */
    private void unbindRouter(JsonResource router) {
        OSGiAuthnFilterBuilder.router = null;
    }

    /**
     * Returns the Router instance.
     *
     * @return The Router instance.
     */
    public static ObjectSet getRouter() {
        return router;
    }

    @Reference(
            name = "ref_ServletFilterRegistration",
            referenceInterface = ServletFilterRegistration.class,
            policy = ReferencePolicy.DYNAMIC,
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            bind = "bindServletFilterRegistration",
            unbind = "unbindServletFilterRegistration"
    )
    private ServletFilterRegistration servletFilterRegistration;
    private void bindServletFilterRegistration(ServletFilterRegistration servletFilterRegistration) {
        this.servletFilterRegistration = servletFilterRegistration;
    }
    private void unbindServletFilterRegistration(ServletFilterRegistration servletFilterRegistration) {
        this.servletFilterRegistration = null;
    }

    private ServiceRegistration serviceRegistration;

    /**
     * Registers the Authentication Filter in OSGi.
     *
     * @throws Exception If a problem occurs whilst registering the filter.
     */
    private void registerAuthnFilter() throws Exception {

        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("moduleConfiguration", "idmAuth");

        Map<String, String> augmentSecurityContext = new HashMap<String, String>();
        augmentSecurityContext.put("type", "text/javascript");
        augmentSecurityContext.put("file", "script/authnPopulateContext.js");

        Map<String, Object> scriptExtensions = new HashMap<String, Object>();
        scriptExtensions.put("augmentSecurityContext", augmentSecurityContext);

        List<String> urlPatterns = new ArrayList<String>();
        urlPatterns.add("/openidm/*");

        Map<String, Object> filterConfig = new HashMap<String, Object>();
        filterConfig.put("classPathURLs", new ArrayList<String>());
        filterConfig.put("systemProperties", new HashMap<String, Object>());
        filterConfig.put("requestAttributes", new HashMap<String, Object>());
        filterConfig.put("initParams", initParams);
        filterConfig.put("scriptExtensions", scriptExtensions);
        filterConfig.put("urlPatterns", urlPatterns);
        filterConfig.put("filterClass", AuthNFilter.class.getCanonicalName());

        JsonValue filterConfigJson = new JsonValue(filterConfig);

        servletFilterRegistration.registerFilter(filterConfigJson);
    }

    /**
     * De-registers the authentication filter.
     *
     * @param context The ComponentContext.
     */
    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        if (serviceRegistration != null) {
            try {
                serviceRegistration.unregister();
                logger.info("Unregistered authentication filter.");
            } catch (Exception ex) {
                logger.warn("Failure reported during unregistering of authentication filter: {}", ex.getMessage(), ex);
            }
        }
    }
}
