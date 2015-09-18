/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.policy;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.script.AbstractScriptedService;
import org.forgerock.openidm.util.FileUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Policy Service for policy validation.
 * 
 */
@Component(name = PolicyService.PID, policy = ConfigurationPolicy.REQUIRE, metatype = true,
        description = "OpenIDM Policy Service", immediate = true)
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Policy Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/policy*") })
public class PolicyService extends AbstractScriptedService {

    public static final String PID = "org.forgerock.openidm.policy";

    /**
     * Setup logging for the {@link PolicyService}.
     */
    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;

    private ComponentContext context;
    
    private JsonValue configuration;

    public PolicyService() {
        super(EnumSet.of(RequestType.ACTION, RequestType.READ));
    }

    public PolicyService(JsonValue configuration) {
        super(EnumSet.of(RequestType.ACTION, RequestType.READ));
        init(configuration);
    }

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;

        Dictionary properties = context.getProperties();
        setProperties(properties);

        configuration = getConfiguration(context);

        activate(context.getBundleContext(), null, configuration);

        logger.info("OpenIDM Policy Service component is activated.");
    }

    /**
     * Configuration modified handling Ensures the service stays registered even
     * whilst configuration changes
     */
    @Modified
    void modified(ComponentContext context) throws Exception {
        configuration = getConfiguration(context);
        modified(null, configuration);
        logger.info("OpenIDM Policy Service component is modified.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        deactivate();
        this.context = null;
        logger.info("OpenIDM Policy Service component is deactivated.");
    }

    @Override
    protected Object getRouterPrefixes(String factoryPid, JsonValue configuration) {
        return null;
    }

    protected BundleContext getBundleContext() {
        return context.getBundleContext();
    }

    private JsonValue getConfiguration(ComponentContext context) {
        JsonValue configuration = enhancedConfig.getConfigurationAsJson(context);
        init(configuration);
        return configuration;
    }
    
    private void init(JsonValue configuration) {
        JsonValue additionalPolicies = configuration.get("additionalFiles");
        if (!additionalPolicies.isNull()) {
            configuration.remove("additionalFiles");
            List<String> list = new ArrayList<String>();
            for (JsonValue policy : additionalPolicies) {
                try {
                    list.add(FileUtil.readFile(IdentityServer.getFileForProjectPath(policy.asString())));
                } catch (Exception e) {
                    logger.error("Error loading additional policy script " + policy.asString(), e);
                }
            }
            configuration.add("additionalPolicies", list);
        }
    }
    
    @Override
    public void handleAction(final Context context, final ActionRequest request,
            final Bindings handler) throws ResourceException {
        super.handleAction(context, request, handler);
        for (Map.Entry<String, String> entry : request.getAdditionalParameters().entrySet()) {
            if (handler.containsKey(entry.getKey())) {
                continue;
            }
            if (bindings != null) {
                bindings.put(entry.getKey(), entry.getValue());
            }
        }

        handler.put("request", request);
        handler.put("resources", configuration.get("resources").getObject());
    }
    
    @Override
    public void handleRead(final Context context, final ReadRequest request,
            final Bindings handler) throws ResourceException {
        super.handleRead(context, request, handler);

        handler.put("request", request);
        handler.put("resources", configuration.get("resources").getObject());
    }
}
