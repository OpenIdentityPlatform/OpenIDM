/**
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
package org.forgerock.openidm.customendpoint.impl;

import java.util.HashMap;
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
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.ScriptThrownException;
import org.forgerock.openidm.script.Scripts;
import org.forgerock.openidm.script.Utils;
import org.forgerock.openidm.script.javascript.ScriptableWrapper;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A custom endpoints service to provide a scriptable way to
 * extend and customize the system
 *
 * @author Laszlo Hordos
 * @author aegloff
 */
@Component(name = EndpointsService.PID, policy = ConfigurationPolicy.OPTIONAL,
        description = "OpenIDM Custom Endpoints Service", immediate = true)
@Service()
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Custom Endpoints Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = EndpointsService.ROUTER_PREFIX)})
public class EndpointsService implements JsonResource {
    private static final Logger logger = LoggerFactory.getLogger(EndpointsService.class);

    public static final String PID = "org.forgerock.openidm.endpointservice";
    
    public static final String ROUTER_PREFIX = "endpoint";

    // Property names in configuration
    public static final String CONFIG_RESOURCE_CONTEXT = "context";

    ComponentContext context;

    @Reference(referenceInterface = ScopeFactory.class)
    private ScopeFactory scopeFactory;

    /** Additional endpoint service(s) configurations */
    @Reference(
        name = "EndpointConfig",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        referenceInterface = EndpointConfig.class,
        policy = ReferencePolicy.DYNAMIC
    )
    // Maps from resource context to config
    protected Map<String, EndpointConfig> endpointConfig = new HashMap<String, EndpointConfig>();
    protected void bindEndpointConfig(EndpointConfig config, Map properties) {
        logger.debug("Adding {}: {}", config.getName(), config);
        String resourceContext = config.getConfig().get(CONFIG_RESOURCE_CONTEXT).asString();
        // Check if we're replacing an existing
        if (isRegistered(resourceContext)) {
            EndpointConfig found = calculateEffectiveConfig().get(resourceContext);
            if (found != null) {
                unregister(found.getConfig());
            }
        }
        endpointConfig.put(resourceContext, config);
        EndpointConfig effective = calculateEffectiveConfig().get(resourceContext);
        logger.debug("Effective {}: {}", effective.getName(), effective.getConfig());
        register(effective.getConfig(), effective.getName());
    }
    protected void unbindEndpointConfig(EndpointConfig config, Map properties) {
        logger.debug("Removing {}: {}", config.getName(), config);
        String resourceContext = config.getConfig().get(CONFIG_RESOURCE_CONTEXT).asString();
        endpointConfig.remove(config);
        unregister(config.getConfig());
        EndpointConfig effective = calculateEffectiveConfig().get(resourceContext);
        if (effective != null) {
            // if a different config became effective, replace with new config
            register(effective.getConfig(), effective.getName());
        }
    }

    /**
     * The default configuration
     */
    Map<String, EndpointConfig> defaultConfig;

    /**
     * The registered scripts, mapping from resource context to the script instance
     */
    Map<String, RegisteredScript> scripts = new HashMap<String, RegisteredScript>();

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;

        JsonValue scriptConfig = null;
        Script script = null;
        
        // Default config is a placeholder at this point
        defaultConfig = new HashMap<String, EndpointConfig>();
        Map<String, EndpointConfig> effectiveConfig = calculateEffectiveConfig();

        for (EndpointConfig cfg : effectiveConfig.values()) {
            register(cfg.getConfig(), cfg.getName());
        }

        logger.info("OpenIDM Custom Endpoints Service component is activated.");
    }

    /**
     * Calculate the effective configuration, based on defaults 
     * and explicit custom endpoint config overrides
     * @return the effective set of configuration
     */
    private Map<String, EndpointConfig> calculateEffectiveConfig() {
        Map<String, EndpointConfig> effectiveConfig = new HashMap<String, EndpointConfig>();
        if (defaultConfig != null) {
            effectiveConfig.putAll(defaultConfig);
        }
        effectiveConfig.putAll(endpointConfig);
        return effectiveConfig;
    }

    /**
     * Get the script parameters to pass to the script from the config
     * @param val the full configuration
     * @return the parameters
     */
    public JsonValue getParameters(JsonValue val) {
        JsonValue filtered = new JsonValue(Utils.deepCopy(val.asMap()));
        // Filter the script definition itself
        filtered.remove("type");
        filtered.remove("source");
        filtered.remove("file");
        return val;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        // Remove all resource context registrations and script instances
        scripts.clear();
        this.context = null;
        logger.info("OpenIDM Custom Endpoints Service component is deactivated.");
    }

    /**
     * Register a service implementation if this component is active
     * If a service already exists at the resource context, this replaces it
     * @param scriptConfig the configuration for the script as a service to 
     * register the resource context
     */
    protected void register(JsonValue scriptConfig, String configName) {
        // Only register once this is active
        if (context != null) {
            String resourceContext = scriptConfig.get(CONFIG_RESOURCE_CONTEXT).asString();
            if (resourceContext != null) {
                Script script = Scripts.newInstance((String)context.getProperties().get(Constants.SERVICE_PID), scriptConfig);
                scripts.put(resourceContext, new RegisteredScript(getParameters(scriptConfig), script, scriptConfig));
                logger.info("Registered custom endpoint at : {} with {}", resourceContext, scriptConfig.get("file"));
            } else {
                logger.warn("Invalid configuration {} : {}", configName, scriptConfig);
            }
        } else {
            logger.debug("Not registering custom endpoint at this time as the custom endpoint service is not active.");
        }
    }

    /**
     * Unregister a service implementation 
     * @param scriptConfig the configuration for the script as a service to 
     * unregister from resource context
     */
    protected void unregister(JsonValue scriptConfig) {
        String resourceContext = scriptConfig.get(CONFIG_RESOURCE_CONTEXT).asString();
        scripts.remove(resourceContext);
        logger.info("Deregistered custom endpoint service at : {} with {}", resourceContext, scriptConfig.get("source"));
    }

    /**
     * Whether a service implementation is registered under a context
     * @param the info context to check
     * @return if a script is registered
     */
    protected boolean isRegistered(String resourceContext) {
        return scripts.containsKey(resourceContext);
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        String id = request.get("id").asString();
        
        // TODO: support registering under any context on the router
        // Currently only registering under endpoint context is supported
        String qualifiedId = ROUTER_PREFIX + "/" + id;
        
        RegisteredScript foundRegisteredScript = scripts.get(qualifiedId);
        if (foundRegisteredScript != null) {
            Script foundScript = foundRegisteredScript.getScript();
            Map<String, Object> scope = Utils.deepCopy(foundRegisteredScript.getParameters().asMap());
            
            ObjectSetContext.push(request);
            try {            
                scope.putAll(scopeFactory.newInstance(ObjectSetContext.get()));
                scope.put("request", request.getObject());
                Object ret = foundScript.exec(scope);
                if (ret instanceof JsonValue) {
                    return (JsonValue) ret;
                } else {
                    return new JsonValue(ret);
                }
            } catch (ScriptThrownException ste) {
                throw ste.toJsonResourceException(null);
            } catch (ScriptException se) {
                throw se.toJsonResourceException("Failure in executing script for " 
                        + qualifiedId + ": " + se.getMessage());
            } finally {
                ObjectSetContext.pop();
            }
        } else {
            throw new JsonResourceException(JsonResourceException.NOT_FOUND, "No custom endpoint available for " + id);
        }
    }

    /**
     * Hold the default config
     * @author aegloff
     */
    private static class DefaultEndpointConfig implements EndpointConfig {
        JsonValue config;
        String name;
        public DefaultEndpointConfig(JsonValue config, String name) {
            this.config = config;
            this.name = name;
        }
        @Override
        public JsonValue getConfig() {
            return this.config;
        }
        @Override
        public String getName() {
            return this.name;
        }
    }

    /**
     * Hold the registered script and info
     * @author aegloff
     */
    private static class RegisteredScript {
        JsonValue parameters;
        Script script;
        JsonValue scriptConfig;
        public RegisteredScript(JsonValue parameters, Script script, JsonValue scriptConfig) {
            this.parameters = parameters;
            this.script = script;
            this.scriptConfig = scriptConfig;
        }
        public JsonValue getParameters() {
            return this.parameters;
        }
        public Script getScript() {
            return this.script;
        }
        public JsonValue getScriptConfig() {
            return this.scriptConfig;
        }
    }
}

