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
package org.forgerock.openidm.info.impl;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;

import java.util.EnumSet;

import javax.script.Bindings;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestType;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.info.HealthInfo;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.script.AbstractScriptedService;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.context.Context;
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
 * A system information service to provide an external and internal API to query
 * OpenIDM state and status.
 */
@Component(
        name = InfoService.PID,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        // description = "OpenIDM Info Service",
        immediate = true)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM Info Service")
public class InfoService extends AbstractScriptedService {

    public static final String PID = "org.forgerock.openidm.info";

    /**
     * Setup logging for the {@link InfoService}.
     */
    private static final Logger logger = LoggerFactory.getLogger(InfoService.class);

    /** HealthInfo service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile HealthInfo healthInfoSvc;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    /** Script Registry */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile ScriptRegistry scriptRegistry;

    /** The connection factory */
    @Reference(policy = ReferencePolicy.STATIC)
    private IDMConnectionFactory connectionFactory;

    private ComponentContext context;

    public InfoService() {
        super(EnumSet.of(RequestType.READ));
    }

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
        setProperty(ServerConstants.ROUTER_PREFIX, "/info/" + String.valueOf(factoryPid) + "*");
        registerService(context.getBundleContext(), configuration);
        logger.info("OpenIDM Info Service \"{}\" component is activated.", factoryPid);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        unregisterService();
        this.context = null;
        logger.info("OpenIDM Info Service component is deactivated.");
    }

    protected BundleContext getBundleContext() {
        return context.getBundleContext();
    }

    @Override
    protected ScriptRegistry getScriptRegistry() {
        return scriptRegistry;
    }

    @Override
    protected void handleRequest(final Context context, final Request request,
            final Bindings handler) {
        super.handleRequest(context, request, handler);
        handler.put("healthinfo", healthInfoSvc.getHealthInfo().asMap());

        handler.put("version", object(
                field("productVersion", ServerConstants.getVersion()),
                field("productRevision", ServerConstants.getRevision())));
    }
}
