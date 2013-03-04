/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.info.internal;

import java.util.Dictionary;

import javax.script.Bindings;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.PersistenceConfig;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.info.HealthInfo;
import org.forgerock.openidm.script.AbstractScriptedService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A system information service to provide an external and internal API to query
 * OpenIDM state and status.
 * 
 * @author aegloff
 */
@Component(name = InfoService.PID, policy = ConfigurationPolicy.REQUIRE,
        description = "OpenIDM Info Service", immediate = true)
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Info Service") })
public class InfoService extends AbstractScriptedService {

    public static final String PID = "org.forgerock.openidm.info";

    /**
     * Setup logging for the {@link InfoService}.
     */
    private static final Logger logger = LoggerFactory.getLogger(InfoService.class);

    /** HealthInfo service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private HealthInfo healthInfoSvc;

    private void bindHealthInfo(final HealthInfo service) {
        healthInfoSvc = service;
    }

    private void unbindHealthInfo(final HealthInfo service) {
        healthInfoSvc = null;
    }

    /** PersistenceConfig service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private PersistenceConfig persistenceConfig;

    private void bindPersistenceConfig(final PersistenceConfig service) {
        persistenceConfig = service;
    }

    private void unbindPersistenceConfig(final PersistenceConfig service) {
        persistenceConfig = null;
    }

    private ComponentContext context;

    public InfoService() {
        super(READ);
    }

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;

        Dictionary properties = context.getProperties();
        setProperties(properties);
        EnhancedConfig config = JSONEnhancedConfig.newInstance();

        String factoryPid = config.getConfigurationFactoryPid(context);
        if (StringUtils.isBlank(factoryPid)) {
            throw new IllegalArgumentException("Configuration must have property: "
                    + ServerConstants.CONFIG_FACTORY_PID);
        }

        JsonValue configuration = config.getConfigurationAsJson(context);
        configuration.put(ServerConstants.CONFIG_FACTORY_PID, factoryPid);

        activate(context.getBundleContext(), factoryPid, configuration);
        logger.info("OpenIDM Info Service component is activated.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        deactivate();
        this.context = null;
        logger.info("OpenIDM Info Service component is deactivated.");
    }

    protected Object getRouterPrefixes(String factoryPid, JsonValue configuration) {
        return "/info/" + String.valueOf(factoryPid) + "*";
    }

    protected JsonValue serialiseServerContext(ServerContext context) throws ResourceException {
        if (null != context && null != persistenceConfig) {
            return ServerContext.saveToJson(context, persistenceConfig);
        }
        return null;
    }

    protected BundleContext getBundleContext() {
        return context.getBundleContext();
    }

    @Override
    protected void handleRequest(final ServerContext context, final Request request,
            final Bindings handler) {
        super.handleRequest(context, request, handler);
        handler.put("healthinfo", healthInfoSvc.getHealthInfo().asMap());

    }
}
