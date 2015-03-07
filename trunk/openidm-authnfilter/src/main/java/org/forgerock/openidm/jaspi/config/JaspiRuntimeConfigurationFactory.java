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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.config;

import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.JaspiAuditLogger;
import org.forgerock.jaspi.logging.JaspiLoggingConfigurator;
import org.forgerock.jaspi.runtime.AuditApi;
import org.forgerock.jaspi.runtime.context.config.ModuleConfigurationFactory;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.crypto.util.JettyPropertyUtil;
import org.forgerock.openidm.jaspi.modules.IDMAuthModule;
import org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A singleton instance that implements both the Jaspi ModuleConfigurationFactory and JaspiLoggingConfigurator
 * interfaces, that provides all of the configuration information for the Jaspi Runtime to be configured correctly.
 */
public enum JaspiRuntimeConfigurationFactory implements ModuleConfigurationFactory, JaspiLoggingConfigurator {

    /**
     * The Singleton instance of the JaspiRuntimeConfigurationFactory.
     */
    INSTANCE;

    public static final String MODULE_CONFIG_ENABLED = "enabled";

    private JsonValue moduleConfiguration;
    private DebugLogger debugLogger;

    /**
     * Constructs the instance of the JaspiRuntimeConfigurationFactory.
     */
    private JaspiRuntimeConfigurationFactory() {
        moduleConfiguration = new JsonValue(new HashMap<String, Object>());
        debugLogger = new JaspiDebugLogger();
    }

    /**
     * Gets the ModuleConfigurationFactory that the Jaspi Runtime will use to configure its authentication
     * modules.
     *
     * @return An instance of a ModuleConfigurationFactory.
     */
    public static ModuleConfigurationFactory getModuleConfigurationFactory() {
        return INSTANCE;
    }

    /**
     * Gets the Logging Configurator that the Jaspi Runtime will use to configure its debug and audit logger
     * instances.
     *
     * @return An instance of a JaspiLoggingConfigurator.
     */
    public static JaspiLoggingConfigurator getLoggingConfigurator() {
        return INSTANCE;
    }

    /**
     * Gets the AuditApi instance that the Jaspi Runtime will use to perform auditing.
     *
     * @return An instance of a {@code AuditApi}.
     */
    public static AuditApi getAuditApi() {
        return new JaspiAuditApi();
    }

    /**
     * Sets the module configuration for the Jaspi Runtime to use.
     * <p>
     * Takes the given Json configuration and processes it to check for enabled/disabled modules and resolves
     * IDM auth module aliases to class names, prior to configuring the Jaspi runtime.
     *
     * @param moduleConfiguration The module configuration json.
     * @throws Exception If there is an error when constructing the Audit Logger for the Jaspi Runtime.
     */
    void setModuleConfiguration(final JsonValue moduleConfiguration) throws Exception {

        final JsonValue moduleConfig = new JsonValue(moduleConfiguration);

        JsonValue serverAuthContext = moduleConfig.get(ModuleConfigurationFactory.SERVER_AUTH_CONTEXT_KEY)
                .required();

        if (serverAuthContext.isDefined(ModuleConfigurationFactory.SESSION_MODULE_KEY)) {
            JsonValue sessionModuleConfig = serverAuthContext.get(ModuleConfigurationFactory.SESSION_MODULE_KEY);
            if (!processModuleConfiguration(sessionModuleConfig)) {
                serverAuthContext.remove(ModuleConfigurationFactory.SESSION_MODULE_KEY);
            }
        }

        JsonValue authModulesConfig = serverAuthContext.get(ModuleConfigurationFactory.AUTH_MODULES_KEY).required();

        List<Integer> toRemove = new ArrayList<Integer>();
        for (int i = 0; i < authModulesConfig.size(); i++) {
            JsonValue authModuleConfig = authModulesConfig.get(i);
            if (!processModuleConfiguration(authModuleConfig)) {
                toRemove.add(i);
            }
        }
        int i = 0;
        for (int index : toRemove) {
            authModulesConfig.remove(index - i);
            i++;
        }

        this.moduleConfiguration = moduleConfig;
    }

    /**
     * Process the module configuration for a specific module, checking to see if the module is enabled and
     * resolving the module class name if an alias is used.
     *
     * @param moduleConfig The specific module configuration json.
     * @return Whether the module is enabled or not.
     */
    private boolean processModuleConfiguration(JsonValue moduleConfig) {

        if (moduleConfig.isDefined(MODULE_CONFIG_ENABLED) && !moduleConfig.get(MODULE_CONFIG_ENABLED).asBoolean()) {
            return false;
        }
        moduleConfig.remove(MODULE_CONFIG_ENABLED);

        if (moduleConfig.isDefined("name")) {
            String className = resolveAuthModuleClassName(moduleConfig.get("name").asString());
            moduleConfig.remove("name");
            moduleConfig.add("className", className);
        }

        JsonValue moduleProperties = moduleConfig.get("properties");
        if (moduleProperties.isDefined("privateKeyPassword")) {
            //decrypt/de-obfuscate privateKey password
            moduleProperties.put("privateKeyPassword", JettyPropertyUtil.decryptOrDeobfuscate(moduleProperties.get("privateKeyPassword").asString()));
        }

        if (moduleProperties.isDefined("keystorePassword")) {
            //decrypt/de-obfuscate keystore password
            moduleProperties.put("keystorePassword", JettyPropertyUtil.decryptOrDeobfuscate(moduleProperties.get("keystorePassword").asString()));
        }

        // set the classname config so the actual auth module gets wrapped in a IDMJaspiModuleWrapper:
        // replace the resolved auth module classname with IDMJaspiModuleWrapper's classname
        // and put the resolved auth module classname as a property unless we've already done this;
        // this check is necessary for OPENIDM-1848
        String className = moduleConfig.get("className").asString();
        if (!IDMJaspiModuleWrapper.class.getName().equals(className)) {
            moduleConfig.put("className", IDMJaspiModuleWrapper.class.getName());
            if (!moduleConfig.isDefined("properties")) {
                moduleConfig.put("properties", new HashMap<String, Object>());
            }
            moduleConfig.get("properties").put("authModuleClassName", className);
        }

        return true;
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

    /**
     * Clears the store module configuration and logger instances.
     */
    void clear() {
        moduleConfiguration = new JsonValue(new HashMap<String, Object>());
        debugLogger = new JaspiDebugLogger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue getConfiguration() {
        return moduleConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DebugLogger getDebugLogger() {
        return debugLogger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JaspiAuditLogger getAuditLogger() {
        return null;
    }
}
