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
package org.forgerock.openidm.info.impl;

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
import org.forgerock.openidm.info.HealthInfo;
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
 * A system information service to provide an external and internal 
 * API to query OpenIDM state and status.
 *
 * @author aegloff
 */
@Component(name = InfoService.PID, policy = ConfigurationPolicy.OPTIONAL,
        description = "OpenIDM Info Service", immediate = true)
@Service()
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Info Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "info")})
public class InfoService implements JsonResource {
    private static final Logger logger = LoggerFactory.getLogger(InfoService.class);

    public static final String PID = "org.forgerock.openidm.infoservice";

    // Property names in configuration
    public static final String CONFIG_INFOCONTEXT = "infocontext";

    // Default info contexts coming out of the box
    private static final String INFOCONTEXT_LOGIN = "login";
    private static final String INFOCONTEXT_PING = "ping";

    ComponentContext context;

    @Reference(referenceInterface = ScopeFactory.class)
    private ScopeFactory scopeFactory;

    @Reference
    private HealthInfo healthInfoSvc;

    /** Additional info service(s) configurations */
    @Reference(
        name = "InfoConfig",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        referenceInterface = InfoConfig.class,
        policy = ReferencePolicy.DYNAMIC
    )
    // Maps from infocontext to config
    protected Map<String, InfoConfig> infoConfig = new HashMap<String, InfoConfig>();
    protected void bindInfoConfig(InfoConfig config, Map properties) {
        logger.debug("Adding {}: {}", config.getName(), config);
        String infoContext = config.getConfig().get(CONFIG_INFOCONTEXT).asString();
        // Check if we're replacing an existing
        if (isRegistered(infoContext)) {
            InfoConfig found = calculateEffectiveConfig().get(infoContext);
            if (found != null) {
                unregister(found.getConfig());
            }
        }
        infoConfig.put(infoContext, config);
        InfoConfig effective = calculateEffectiveConfig().get(infoContext);
        logger.debug("Effective {}: {}", effective.getName(), effective.getConfig());
        register(effective.getConfig(), effective.getName());
    }
    protected void unbindInfoConfig(InfoConfig config, Map properties) {
        logger.debug("Removing {}: {}", config.getName(), config);
        String infoContext = config.getConfig().get(CONFIG_INFOCONTEXT).asString();
        infoConfig.remove(config);
        unregister(config.getConfig());
        InfoConfig effective = calculateEffectiveConfig().get(infoContext);
        if (effective != null) {
            // if a different config became effective, replace with new config
            register(effective.getConfig(), effective.getName());
        }
    }

    /**
     * The default configuration
     */
    Map<String, InfoConfig> defaultConfig;

    /**
     * The registered scripts, mapping from infocontext to the script instance
     */
    Map<String, RegisteredScript> scripts = new HashMap<String, RegisteredScript>();

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;

        JsonValue scriptConfig = null;
        Script script = null;
        
        defaultConfig = new HashMap<String, InfoConfig>();
        
        // Defaults are currently built in. They can be overriden by 
        // Providing info config for the same infocontext
        scriptConfig = new JsonValue(new HashMap<String, Object>());
        scriptConfig.put(CONFIG_INFOCONTEXT, INFOCONTEXT_LOGIN);
        scriptConfig.put("type", "text/javascript");
        scriptConfig.put("file", "bin/defaults/script/info/login.js");
        script = Scripts.newInstance((String)context.getProperties().get(Constants.SERVICE_PID), scriptConfig);
        defaultConfig.put(INFOCONTEXT_LOGIN, new DefaultInfoConfig(scriptConfig, "default-login"));

        scriptConfig = new JsonValue(new HashMap<String, Object>());
        scriptConfig.put(CONFIG_INFOCONTEXT, INFOCONTEXT_PING);
        scriptConfig.put("type", "text/javascript");
        scriptConfig.put("file", "bin/defaults/script/info/ping.js");
        script = Scripts.newInstance((String)context.getProperties().get(Constants.SERVICE_PID), scriptConfig);
        defaultConfig.put(INFOCONTEXT_PING, new DefaultInfoConfig(scriptConfig, "default-ping"));

        Map<String, InfoConfig> effectiveConfig = calculateEffectiveConfig();

        for (InfoConfig cfg : effectiveConfig.values()) {
            register(cfg.getConfig(), cfg.getName());
        }

        logger.info("OpenIDM Info Service component is activated.");
    }

    /**
     * Calculate the effective configuration, based on defaults 
     * and explicit info config overrides
     * @return the effective set of configuration
     */
    private Map<String, InfoConfig> calculateEffectiveConfig() {
        Map<String, InfoConfig> effectiveConfig = new HashMap<String, InfoConfig>();
        if (defaultConfig != null) {
            effectiveConfig.putAll(defaultConfig);
        }
        effectiveConfig.putAll(infoConfig);
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
        // Remove all info context registrations and script instances
        scripts.clear();
        this.context = null;
        logger.info("OpenIDM Info Service component is deactivated.");
    }

    /**
     * Register a service implementation if this component is active
     * If a service already exists at the infocontext, this replaces it
     * @param scriptConfig the configuration for the script as a service to 
     * register under info/ context
     */
    protected void register(JsonValue scriptConfig, String configName) {
        // Only register once this is active
        if (context != null) {
            String infoContext = scriptConfig.get(CONFIG_INFOCONTEXT).asString();
            if (infoContext != null) {
                Script script = Scripts.newInstance((String)context.getProperties().get(Constants.SERVICE_PID), scriptConfig);
                scripts.put(infoContext, new RegisteredScript(getParameters(scriptConfig), script, scriptConfig));
                logger.info("Registered info service at : {} with {}", infoContext, scriptConfig.get("file"));
            } else {
                logger.warn("Invalid configuration {} : {}", configName, scriptConfig);
            }
        } else {
            logger.debug("Not registering info config at this time as the infoservice is not active.");
        }
    }

    /**
     * Unregister a service implementation 
     * @param scriptConfig the configuration for the script as a service to 
     * unregister from info/ context
     */
    protected void unregister(JsonValue scriptConfig) {
        String infoContext = scriptConfig.get(CONFIG_INFOCONTEXT).asString();
        scripts.remove(infoContext);
        logger.info("Deregistered info service at : {} with {}", infoContext, scriptConfig.get("source"));
    }

    /**
     * Whether a service implementation is registered under a context
     * @param the info context to check
     * @return if a script is registered
     */
    protected boolean isRegistered(String infoContext) {
        return scripts.containsKey(infoContext);
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        String id = request.get("id").asString();
        RegisteredScript foundRegisteredScript = scripts.get(id);
        if (foundRegisteredScript != null) {
            Script foundScript = foundRegisteredScript.getScript();
            Map<String, Object> scope = Utils.deepCopy(foundRegisteredScript.getParameters().asMap());
            
            ObjectSetContext.push(request);
            try {            
                scope.putAll(scopeFactory.newInstance(ObjectSetContext.get()));
                scope.put("request", request.getObject());
                scope.put("healthinfo", ScriptableWrapper.wrap(healthInfoSvc.getHealthInfo().asMap()));
                Object ret = foundScript.exec(scope);
                if (ret instanceof JsonValue) {
                    return (JsonValue) ret;
                } else {
                    return new JsonValue(ret);
                }
            } catch (ScriptThrownException ste) {
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, ste.getValue().toString(), ste);
            } catch (ScriptException se) {
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, se);
            } finally {
                ObjectSetContext.pop();
            }
        } else {
            throw new JsonResourceException(JsonResourceException.NOT_FOUND, "No info service available for " + id);
        }
    }

    /**
     * Hold the default config
     * @author aegloff
     */
    private static class DefaultInfoConfig implements InfoConfig {
        JsonValue config;
        String name;
        public DefaultInfoConfig(JsonValue config, String name) {
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

