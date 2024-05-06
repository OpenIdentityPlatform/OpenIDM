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
 * Copyright 2012-2016 ForgeRock AS.
 * Portions Copyrighted 2024 3A Systems LLC.
 */
package org.forgerock.openidm.customendpoint.impl;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.script.AbstractScriptedService;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A custom endpoints service to provide a scriptable way to extend and
 * customize the system
 *
 */
@Component(
        name = EndpointsService.PID,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
//        description = "OpenIDM Custom Endpoints Service",
        immediate = true)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM Custom Endpoints Service")
public class EndpointsService extends AbstractScriptedService {

    public static final String PID = "org.forgerock.openidm.endpoint";

    /**
     * Setup logging for the {@link EndpointsService}.
     */
    private static final Logger logger = LoggerFactory.getLogger(EndpointsService.class);

    public static final String CONFIG_RESOURCE_CONTEXT = "context";

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    /** Script Registry */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile ScriptRegistry scriptRegistry;

    private ComponentContext context;

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;

        String factoryPid = enhancedConfig.getConfigurationFactoryPid(context);
        if (StringUtils.isBlank(factoryPid)) {
            throw new IllegalArgumentException("Configuration must have property: "
                    + ServerConstants.CONFIG_FACTORY_PID);
        }

        JsonValue configuration = enhancedConfig.getConfigurationAsJson(context);
        configuration.put(ServerConstants.CONFIG_FACTORY_PID, factoryPid);

        setProperties(context);
        setProperty(ServerConstants.ROUTER_PREFIX, getRouterPrefixes(factoryPid, configuration));
        registerService(context.getBundleContext(), configuration);
        logger.info("OpenIDM Endpoints Service \"{}\" component is activated.", factoryPid);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        unregisterService();
        this.context = null;
        logger.info("OpenIDM Endpoints Service component is deactivated.");
    }

    private String[] getRouterPrefixes(String factoryPid, JsonValue configuration) {
        JsonValue resourceContext = configuration.get(CONFIG_RESOURCE_CONTEXT);
        final String routerPrefix;
        if (!resourceContext.isNull() && resourceContext.isString()) {
            // use the resource context as the router prefix
            routerPrefix = resourceContext.asString();
        } else {
            // build the router prefix from the factory PID
            routerPrefix = "endpoint/" + String.valueOf(factoryPid) + "*";
        }
        return new String[] { routerPrefix };
    }

    protected BundleContext getBundleContext() {
        return context.getBundleContext();
    }

    @Override
    protected ScriptRegistry getScriptRegistry() {
        return scriptRegistry;
    }
}
