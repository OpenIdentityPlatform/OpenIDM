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
package org.forgerock.openidm.info.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptException;

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
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.info.HealthInfo;
import org.forgerock.openidm.script.ScriptCustomizer;
import org.forgerock.openidm.script.ScriptedRequestHandler;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptEvent;
import org.forgerock.script.ScriptListener;
import org.forgerock.script.ScriptName;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
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
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Info Service")})
public class InfoService implements ScriptCustomizer, ScriptListener {

    public static final String PID = "org.forgerock.openidm.info";

    /**
     * Setup logging for the {@link InfoService}.
     */
    private static final Logger logger = LoggerFactory.getLogger(InfoService.class);

    // Property names in configuration
    private static final String CONFIG_INFOCONTEXT = "infocontext";

    // Default info contexts coming out of the box
    private static final String INFOCONTEXT_LOGIN = "login";
    private static final String INFOCONTEXT_PING = "ping";

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private ScriptRegistry scriptRegistry;

    private void bindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = service;
    }

    private void unbindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = null;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    private HealthInfo healthInfoSvc;

    private void bindHealthInfo(final HealthInfo service) {
        healthInfoSvc = service;
    }

    private void unbindHealthInfo(final HealthInfo service) {
        healthInfoSvc = null;
    }

    private ComponentContext context;

    private ServiceRegistration<RequestHandler> selfRegistration = null;

    private Dictionary properties = null;

    private JsonValue configuration = null;

    private ScriptName scriptName = null;

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;

        properties = context.getProperties();
        EnhancedConfig config = JSONEnhancedConfig.newInstance();

        String factoryPid = config.getConfigurationFactoryPid(context);
        if (StringUtils.isBlank(factoryPid)) {
            throw new IllegalArgumentException("Configuration must have property: "+ ServerConstants.CONFIG_FACTORY_PID);
        }
        properties.put(ServerConstants.ROUTER_PREFIX, "/info/" + String.valueOf(factoryPid) + "*");

        configuration = config.getConfigurationAsJson(context);
        configuration.put(CONFIG_INFOCONTEXT, factoryPid);

        // Just to check
        //String infoContext = configuration.get(CONFIG_INFOCONTEXT).required().asString();

        try {
            ScriptEntry scriptEntry = scriptRegistry.takeScript(configuration);
            scriptEntry.addScriptListener(this);
            scriptName = scriptEntry.getName();

            selfRegistration =
                    context.getBundleContext().registerService(RequestHandler.class,
                            new ScriptedRequestHandler(scriptEntry, this), properties);

        } catch (ScriptException e) {
            throw new ComponentException("Failed to take script: " + factoryPid, e);
        }

        // JsonValue scriptConfig = null;
        // Script script = null;
        //
        // defaultConfig = new HashMap<String, InfoConfig>();
        //
        // // Defaults are currently built in. They can be overriden by
        // // Providing info config for the same infocontext
        // scriptConfig = new JsonValue(new HashMap<String, Object>());
        // scriptConfig.put(CONFIG_INFOCONTEXT, INFOCONTEXT_LOGIN);
        // scriptConfig.put("type", "text/javascript");
        // scriptConfig.put("file", "bin/defaults/script/info/login.js");
        // script =
        // Scripts.newInstance((String)
        // context.getProperties().get(Constants.SERVICE_PID),
        // scriptConfig);
        // defaultConfig.put(INFOCONTEXT_LOGIN, new
        // DefaultInfoConfig(scriptConfig, "default-login"));
        //
        // scriptConfig = new JsonValue(new HashMap<String, Object>());
        // scriptConfig.put(CONFIG_INFOCONTEXT, INFOCONTEXT_PING);
        // scriptConfig.put("type", "text/javascript");
        // scriptConfig.put("file", "bin/defaults/script/info/ping.js");
        // script =
        // Scripts.newInstance((String)
        // context.getProperties().get(Constants.SERVICE_PID),
        // scriptConfig);
        // defaultConfig.put(INFOCONTEXT_PING, new
        // DefaultInfoConfig(scriptConfig, "default-ping"));
        //
        // Map<String, InfoConfig> effectiveConfig = calculateEffectiveConfig();
        //
        // for (InfoConfig cfg : effectiveConfig.values()) {
        // register(cfg.getConfig(), cfg.getName());
        // }

        logger.info("OpenIDM Info Service component is activated.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (null != selfRegistration) {
            selfRegistration.unregister();
        }
        if (null != scriptName) {
            scriptRegistry.deleteScriptListener(scriptName, this);
        }
        this.context = null;
        logger.info("OpenIDM Info Service component is deactivated.");
    }

    @Override
    // TODO implement it in a smarter way
    public void scriptChanged(ScriptEvent event) {
        if (ScriptEvent.REGISTERED == event.getType()) {
            if (null == selfRegistration) {
                /*
                 * selfRegistration =
                 * context.getBundleContext().registerService(
                 * RequestHandler.class, new
                 * ScriptedRequestHandler(event.getScriptLibraryEntry(), this),
                 * properties);
                 */
            }
        } else if (ScriptEvent.UNREGISTERING == event.getType()) {
            /*
             * if (null != selfRegistration) { selfRegistration.unregister(); }
             */
        } else if (ScriptEvent.MODIFIED == event.getType()) {
            /*
             * if (null != selfRegistration) { selfRegistration.unregister(); }
             * if (null == selfRegistration) { selfRegistration =
             * context.getBundleContext().registerService(RequestHandler.class,
             * new ScriptedRequestHandler(event.getScriptLibraryEntry(), this),
             * properties); }
             */
        }
    }

    public void handleAction(ServerContext context, ActionRequest request, Bindings handler)
            throws ResourceException {
        throw new NotSupportedException("Actions are not supported for resource instances");
    }

    public void handleCreate(ServerContext context, CreateRequest request, Bindings handler)
            throws ResourceException {
        throw new NotSupportedException("Create operations are not supported");
    }

    public void handleDelete(ServerContext context, DeleteRequest request, Bindings handler)
            throws ResourceException {
        throw new NotSupportedException("Delete operations are not supported");
    }

    public void handlePatch(ServerContext context, PatchRequest request, Bindings handler)
            throws ResourceException {
        throw new NotSupportedException("Patch operations are not supported");
    }

    public void handleQuery(ServerContext context, QueryRequest request, Bindings handler)
            throws ResourceException {
        throw new NotSupportedException("Query operations are not supported");
    }

    public void handleRead(final ServerContext context, final ReadRequest request,
            final Bindings handler) throws ResourceException {
        handler.put("healthinfo", healthInfoSvc.getHealthInfo().asMap());
        // TODO User the Requests.toJson later
        Map<String, Object> map = new HashMap<String, Object>(1);
        map.put("method", "read");
        if (context.containsContext(SecurityContext.class)) {
            map.put("parent", context.asContext(SecurityContext.class).getAuthorizationId());
        }
        handler.put("request", map);
    }

    public void handleUpdate(ServerContext context, UpdateRequest request, Bindings handler)
            throws ResourceException {
        throw new NotSupportedException("Update operations are not supported");
    }

    /**
     * {@inheritDoc}
     */
    /*
     * public JsonValue handle(JsonValue request) throws JsonResourceException {
     * String id = request.get("id").asString(); RegisteredScript
     * foundRegisteredScript = scripts.get(id); if (foundRegisteredScript !=
     * null) { Script foundScript = foundRegisteredScript.getScript();
     * Map<String, Object> scope =
     * Utils.deepCopy(foundRegisteredScript.getParameters().asMap());
     * 
     * ObjectSetContext.push(request); try {
     * scope.putAll(scriptRegistry.newInstance(ObjectSetContext.get()));
     * scope.put("request", request.getObject()); scope.put("healthinfo",
     * ScriptableWrapper.wrap(healthInfoSvc.getHealthInfo() .asMap())); Object
     * ret = foundScript.exec(scope); if (ret instanceof JsonValue) { return
     * (JsonValue) ret; } else { return new JsonValue(ret); } } catch
     * (ScriptThrownException ste) { throw ste.toJsonResourceException(null); }
     * catch (ScriptException se) { throw
     * se.toJsonResourceException("Failure in executing script for " + id + ": "
     * + se.getMessage()); } finally { ObjectSetContext.pop(); } } else { throw
     * new JsonResourceException(JsonResourceException.NOT_FOUND,
     * "No info service available for " + id); } }
     *//**
     * Hold the default config
     * 
     * @author aegloff
     */
    /*
     * private static class DefaultInfoConfig implements InfoConfig { JsonValue
     * config; String name;
     * 
     * public DefaultInfoConfig(JsonValue config, String name) { this.config =
     * config; this.name = name; }
     * 
     * @Override public JsonValue getConfig() { return this.config; }
     * 
     * @Override public String getName() { return this.name; } }
     *//**
     * Hold the registered script and info
     * 
     * @author aegloff
     */
    /*
     * private static class RegisteredScript { JsonValue parameters; Script
     * script; JsonValue scriptConfig;
     * 
     * public RegisteredScript(JsonValue parameters, Script script, JsonValue
     * scriptConfig) { this.parameters = parameters; this.script = script;
     * this.scriptConfig = scriptConfig; }
     * 
     * public JsonValue getParameters() { return this.parameters; }
     * 
     * public Script getScript() { return this.script; }
     * 
     * public JsonValue getScriptConfig() { return this.scriptConfig; } }
     *//** Additional info service(s) configurations */
    /*
     * @Reference(name = "InfoConfig", cardinality =
     * ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface =
     * InfoConfig.class, policy = ReferencePolicy.DYNAMIC) protected Map<String,
     * InfoConfig> infoConfig = new HashMap<String, InfoConfig>();
     * 
     * protected void bindInfoConfig(InfoConfig config, Map properties) {
     * logger.debug("Adding {}: {}", config.getName(), config); String
     * infoContext = config.getConfig().get(CONFIG_INFOCONTEXT).asString(); //
     * Check if we're replacing an existing if (isRegistered(infoContext)) {
     * InfoConfig found = calculateEffectiveConfig().get(infoContext); if (found
     * != null) { unregister(found.getConfig()); } } infoConfig.put(infoContext,
     * config); InfoConfig effective =
     * calculateEffectiveConfig().get(infoContext);
     * logger.debug("Effective {}: {}", effective.getName(),
     * effective.getConfig()); register(effective.getConfig(),
     * effective.getName()); }
     * 
     * protected void unbindInfoConfig(InfoConfig config, Map properties) {
     * logger.debug("Removing {}: {}", config.getName(), config); String
     * infoContext = config.getConfig().get(CONFIG_INFOCONTEXT).asString();
     * infoConfig.remove(config); unregister(config.getConfig()); InfoConfig
     * effective = calculateEffectiveConfig().get(infoContext); if (effective !=
     * null) { // if a different config became effective, replace with new
     * config register(effective.getConfig(), effective.getName()); } }
     *//**
     * The default configuration
     */
    /*
     * Map<String, InfoConfig> defaultConfig;
     *//**
     * The registered scripts, mapping from infocontext to the script
     * newBuilder
     */
    /*
     * Map<String, RegisteredScript> scripts = new HashMap<String,
     * RegisteredScript>();
     *//**
     * Calculate the effective configuration, based on defaults and explicit
     * info config overrides
     * 
     * @return the effective set of configuration
     */
    /*
     * private Map<String, InfoConfig> calculateEffectiveConfig() { Map<String,
     * InfoConfig> effectiveConfig = new HashMap<String, InfoConfig>(); if
     * (defaultConfig != null) { effectiveConfig.putAll(defaultConfig); }
     * effectiveConfig.putAll(infoConfig); return effectiveConfig; }
     *//**
     * Get the script parameters to pass to the script from the config
     * 
     * @param val
     *            the full configuration
     * @return the parameters
     */
    /*
     * public JsonValue getParameters(JsonValue val) { JsonValue filtered = new
     * JsonValue(Utils.deepCopy(val.asMap())); // Filter the script definition
     * itself filtered.remove("type"); filtered.remove("source");
     * filtered.remove("file"); return val; }
     */
}
