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

package org.forgerock.openidm.policy;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.util.FileUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.exception.ScriptThrownException;
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
@Service()
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Policy Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/policy*") })
public class PolicyService implements SingletonResourceProvider {

    public static final String PID = "org.forgerock.openidm.policy";

    /**
     * Setup logging for the {@link PolicyService}.
     */
    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private ScriptRegistry scriptRegistry;

    private void bindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = service;
    }

    private void unbindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = null;
    }

    private ScriptEntry scriptEntry;

    @Activate
    protected void activate(ComponentContext context) throws Exception {
        logger.debug("Activating service with configuration {}", context.getProperties());
        try {
            setConfig(context);
        } catch (Exception ex) {
            logger.warn("Configuration invalid, can not activate service.", ex);
            throw ex;
        }
        logger.info("OpenIDM Policy Service component is activated.");
    }

    /**
     * Configuration modified handling Ensures the service stays registered even
     * whilst configuration changes
     */
    @Modified
    void modified(ComponentContext context) throws Exception {
        logger.debug("Reconfiguring service with configuration {}", context.getProperties());
        try {
            setConfig(context);
        } catch (Exception ex) {
            logger.warn("Configuration invalid, can not reconfigure service.", ex);
            throw ex;
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        logger.info("OpenIDM Policy Service component is deactivated.");
    }

    private void setConfig(ComponentContext context) throws ScriptException {
        JsonValue configuration = JSONEnhancedConfig.newInstance().getConfigurationAsJson(context);

        JsonValue additionalPolicies = configuration.get("additionalFiles");
        if (!additionalPolicies.isNull()) {
            configuration.remove("additionalFiles");
            List<String> list = new ArrayList<String>();
            for (JsonValue policy : additionalPolicies) {
                String fileName = policy.asString();
                try {
                    list.add(FileUtil.readFile(IdentityServer.getFileForInstallPath(fileName)));
                } catch (Exception e) {
                    logger.error("Error loading additional policy script " + fileName, e);
                }
            }
            configuration.add("additionalPolicies", list);
        }

        // Initiate the Script
        scriptEntry = scriptRegistry.takeScript(configuration);
    }

    @Override
    public void patchInstance(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void readInstance(ServerContext context, ReadRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Read operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void actionInstance(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        try {
            Script script = scriptEntry.getScript(context);

            try {

                // JsonValue params = request.get("params");
                // JsonValue caller = params.get("_caller");
                // JsonValue parent = request.get("parent");
                // if (parent.get("_isDirectHttp").isNull()) {
                // boolean isHttp = false;
                // if (!caller.isNull() &&
                // caller.asString().equals("filterEnforcer")) {
                // parent = parent.get("parent");
                // }
                // if (!parent.isNull() && !parent.get("type").isNull()) {
                // isHttp = parent.get("type").asString().equals("http");
                // }
                // request.add("_isDirectHttp", isHttp);
                // } else {
                // request.add("_isDirectHttp",
                // parent.get("_isDirectHttp").asBoolean());
                // }

                script.put("request", request);
                Object o = script.eval();
                if (o instanceof JsonValue) {
                    handler.handleResult((JsonValue) o);
                } else {
                    handler.handleResult(null);
                }
            } catch (ScriptThrownException ste) {
                // /throw ste.toJsonResourceException(null);
            } catch (ScriptException se) {
                // throw
                // se.toJsonResourceException("Failure in executing policy script: "
                // + se.getMessage());
            }
//        } catch (ResourceException e) {
//            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }
}
