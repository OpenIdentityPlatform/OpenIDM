/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.filter.AuthFilterService;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.ScriptThrownException;
import org.forgerock.openidm.script.Scripts;
import org.forgerock.openidm.script.Utils;
import org.forgerock.openidm.util.FileUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
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
@Service(value = {PolicyService.class, JsonResource.class})
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Policy Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "policy")})
public class PolicyService implements JsonResource {

    public static final String PID = "org.forgerock.openidm.policy";

    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);

    @Reference(referenceInterface = ScopeFactory.class)
    private ScopeFactory scopeFactory;

    @Reference
    private AuthFilterService authFilterService;
    
    /** Internal object set router service. */
    @Reference(
        name = "ref_PolicyService_JsonResourceRouterService",
        referenceInterface = JsonResource.class,
        bind = "bindRouter",
        unbind = "unbindRouter",
        cardinality = ReferenceCardinality.MANDATORY_UNARY,
        policy = ReferencePolicy.STATIC,
        target = "(service.pid=org.forgerock.openidm.router)"
    )
    protected ObjectSet router;
    protected void bindRouter(JsonResource router) {
        this.router = new JsonResourceObjectSet(router);
    }
    protected void unbindRouter(JsonResource router) {
        this.router = null;
    }
    
    private Script script = null;
    private JsonValue parameters = null;
    private ServiceRegistration service = null;

    @Activate
    protected void activate(ComponentContext context) {
        JsonValue configuration = JSONEnhancedConfig.newInstance().getConfigurationAsJson(context);

        // Initiate the Script
        script = Scripts.newInstance((String)context.getProperties().get(Constants.SERVICE_PID), configuration);
        configuration.remove("type");
        configuration.remove("source");
        configuration.remove("file");
        JsonValue additionalPolicies = configuration.get("additionalFiles");
        if (!additionalPolicies.isNull()) {
            configuration.remove("additionalFiles");
            List<String> list = new ArrayList<String>();
            for (JsonValue policy : additionalPolicies) {
                String fileName = policy.asString();
                try {
                    list.add(FileUtil.readFile(new File(fileName)));
                } catch (Exception e) {
                    logger.error("Error loading additional policy script " + fileName, e);
                }
            }
            configuration.add("additionalPolicies", list);
        }
        parameters = configuration;

        logger.info("OpenIDM Policy Service component is activated.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (null != service) {
            service.unregister();
            service = null;
        }
        logger.info("OpenIDM Policy Service component is deactivated.");
    }

    public void registerResource(JsonValue resourceConfig) {
        List<Object> resources = parameters.get("resources").asList();
        String resourceName = resourceConfig.get("resource").asString();
        for (Object obj : resources) {
            JsonValue value = (JsonValue)obj;
            if (value.get("resource").asString().equals(resourceName)) {
                logger.debug("Removing old resource configuration for {}", resourceName);
                resources.remove(obj);
            }
        }
        logger.debug("Registering resource configuration for {}", resourceName);
        resources.add(resourceConfig);
    }
    
    public void unregisterResource(String resourceName) {
        List<Object> resources = parameters.get("resources").asList();
        for (Object obj : resources) {
            JsonValue value = (JsonValue)obj;
            if (value.get("resource").asString().equals(resourceName)) {
                logger.debug("Unregistering resource configuration for {}", resourceName);
                resources.remove(obj);
                return;
            }
        }
        logger.debug("Cannot unregister resource configuration for {}. Resource configuration does not exist", resourceName);
    }

    public JsonValue handle(JsonValue request) throws JsonResourceException {
        Map<String, Object> scope = Utils.deepCopy(parameters.asMap());
        ObjectSetContext.push(request);
        try {
            scope.putAll(scopeFactory.newInstance(ObjectSetContext.get()));
            scope.put("request", request.getObject());
            scope.put("authFilter", authFilterService);
            
            return new JsonValue(script.exec(scope));
        } catch (ScriptThrownException ste) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, ste.getValue().toString(), ste);
        } catch (ScriptException se) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, se);
        } finally {
            ObjectSetContext.pop();
        }
    }
}

