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

import java.util.Dictionary;
import java.util.Enumeration;
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
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.ScriptThrownException;
import org.forgerock.openidm.script.Scripts;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
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

    public void registerComponent(JsonValue componentConfig) {
        List<Object> components = parameters.get("components").asList();
        String componentName = componentConfig.get("component").asString();
        for (Object obj : components) {
            JsonValue value = (JsonValue)obj;
            if (value.get("component").asString().equals(componentName)) {
                logger.debug("Removing old component configuration for {}", componentName);
                components.remove(obj);
            }
        }
        logger.debug("Registering component configuration for {}", componentName);
        components.add(componentConfig);
    }
    
    public void unregisterComponent(String componentName) {
        List<Object> components = parameters.get("components").asList();
        for (Object obj : components) {
            JsonValue value = (JsonValue)obj;
            if (value.get("component").asString().equals(componentName)) {
                logger.debug("Unregistering component configuration for {}", componentName);
                components.remove(obj);
                return;
            }
        }
        logger.debug("Cannot unregister component configuration for {}. Component configuration does not exist", componentName);
    }
    
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        Map<String, Object> scope = Utils.deepCopy(parameters.asMap());
        scope.putAll(scopeFactory.newInstance(ObjectSetContext.get()));
        scope.put("request", request.getObject());
        scope.put("openidm", router);
        
        try {
            return new JsonValue(script.exec(scope));
        } catch (ScriptThrownException ste) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, ste.getValue().toString(), ste);
        } catch (ScriptException se) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, se);
        }
    }
}

