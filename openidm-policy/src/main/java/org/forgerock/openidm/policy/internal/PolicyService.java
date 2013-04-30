/*
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

package org.forgerock.openidm.policy.internal;

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
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.config.JSONEnhancedConfig;
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
 * @author Chad Kienle
 */
@Component(name = PolicyService.PID, policy = ConfigurationPolicy.REQUIRE,
        description = "OpenIDM Policy Service", immediate = true)
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Policy Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/policy") })
public class PolicyService extends AbstractScriptedService {

    public static final String PID = "org.forgerock.openidm.policy";

    /**
     * Setup logging for the {@link PolicyService}.
     */
    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);

    private ComponentContext context;

    public PolicyService() {
        super(EnumSet.of(RequestType.ACTION));
    }

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;

        Dictionary properties = context.getProperties();
        setProperties(properties);

        JsonValue configuration = getConfiguration(context);

        activate(context.getBundleContext(), null, configuration);

        logger.info("OpenIDM Policy Service component is activated.");
    }

    /**
     * Configuration modified handling Ensures the service stays registered even
     * whilst configuration changes
     */
    @Modified
    void modified(ComponentContext context) throws Exception {
        JsonValue configuration = getConfiguration(context);
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
        JsonValue configuration = JSONEnhancedConfig.newInstance().getConfigurationAsJson(context);
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
        return configuration;
    }

    @Override
    public void handleAction(final ServerContext context, final ActionRequest request,
            final Bindings handler) throws ResourceException {
        super.handleAction(context, request, handler);
        for (Map.Entry<String, String> entry : request.getAdditionalActionParameters().entrySet()) {
            if (handler.containsKey(entry.getKey())) {
                continue;
            }
            bindings.put(entry.getKey(), entry.getValue());
        }

        try {
            bindings.put("_isDirectHttp", context
                    .containsContext(org.forgerock.json.resource.servlet.HttpContext.class));
        } catch (Throwable e) {
            /* Catch if the class is not loaded */
        }
    }

    // TODO Remove this reminder for old behaviour
    public void handle(JsonValue request) {
        JsonValue params = request.get("params");
        JsonValue caller = params.get("_caller");
        JsonValue parent = request.get("parent");
        if (parent.get("_isDirectHttp").isNull()) {
            boolean isHttp = false;
            if (!caller.isNull() && caller.asString().equals("filterEnforcer")) {
                parent = parent.get("parent");
            }
            if (!parent.isNull() && !parent.get("type").isNull()) {
                isHttp = parent.get("type").asString().equals("http");
            }
            request.add("_isDirectHttp", isHttp);
        } else {
            request.add("_isDirectHttp", parent.get("_isDirectHttp").asBoolean());
        }
    }
}
