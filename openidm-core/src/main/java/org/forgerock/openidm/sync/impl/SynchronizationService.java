/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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
*
*/
package org.forgerock.openidm.sync.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.script.ScriptRegistry;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The resource provider for requests on /sync.  Describes the synchronization mappings and dispatches
 * actions to synchronize between source and targets listed in the synchronization mappings described
 * by the sync.json configuration file.  Also supports invocation as a {@link ScheduledService}.
 *
 * @author Paul C. Bryan
 * @author aegloff
 * @author ckienle
 * @author brmiller
 */
@Component(
    name = "org.forgerock.openidm.sync",
    policy = ConfigurationPolicy.REQUIRE,
    immediate = true
)
@Properties({
    @Property(name = "service.description", value = "OpenIDM object synchronization service"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "openidm.router.prefix", value = "/sync/*")
})
@Service
public class SynchronizationService implements SingletonResourceProvider, Mappings, ScheduledService {

    /** Actions suppoorted by this service. */
    enum Action {
        onCreate, onUpdate, onDelete, recon, performAction
    }

    /** Logger */
    private final static Logger logger = LoggerFactory.getLogger(SynchronizationService.class);

    /** Object mappings. Order of mappings evaluated during synchronization is significant. */
    private volatile ArrayList<ObjectMapping> mappings = null;

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    @Reference
    Reconcile reconService;

    /** Script Registry service. */
    @Reference(
            policy = ReferencePolicy.DYNAMIC,
            bind = "bindScriptRegistry",
            unbind = "unbindScriptRegistry")
    ScriptRegistry scriptRegistry;
    
    protected void bindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = service;
        Scripts.init(service);
    }

    protected void unbindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = null;
    }

    @Activate
    protected void activate(ComponentContext context) {
        JsonValue config = new JsonValue(new JSONEnhancedConfig().getConfiguration(context));
        try {
            mappings = new ArrayList<ObjectMapping>();
            initMappings(mappings, config);
        } catch (JsonValueException jve) {
            throw new ComponentException("Configuration error: " + jve.getMessage(), jve);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        mappings = null;
    }

    @Modified
    protected void modified(ComponentContext context) {
        JsonValue config = new JsonValue(new JSONEnhancedConfig().getConfiguration(context));
        ArrayList<ObjectMapping> newMappings = new ArrayList<ObjectMapping>();
        try {
            initMappings(newMappings, config);
            mappings = newMappings;
        } catch (JsonValueException jve) {
            throw new ComponentException("Configuration error: " + jve.getMessage(), jve);
        }
    }

    private void initMappings(ArrayList<ObjectMapping> mappingList, JsonValue config) {
        for (JsonValue jv : config.get("mappings").expect(List.class)) {
            mappingList.add(new ObjectMapping(this, jv)); // throws JsonValueException
        }
        for (ObjectMapping mapping : mappingList) {
            mapping.initRelationships(this, mappingList);
        }
    }

    /**
     * Return the {@link ObjectMapping} for a the mapping {@code name}.
     *
     * @param name the mapping name
     * @return the ObjectMapping
     * @throws SynchronizationException if no mapping exists by the given name.
     */
    public ObjectMapping getMapping(String name) throws SynchronizationException {
        for (ObjectMapping mapping : mappings) {
            if (mapping.getName().equals(name)) {
                return mapping;
            }
        }
        throw new SynchronizationException("No such mapping: " + name);
    }

    /**
     * Instantiate an {@link ObjectMapping} with the given config
     *
     * @param mappingConfig the mapping configuration
     * @return the created ObjectMapping
     */
    public ObjectMapping createMapping(JsonValue mappingConfig) {
        ObjectMapping createdMapping = new ObjectMapping(this, mappingConfig);
        List<ObjectMapping> augmentedMappings = new ArrayList<ObjectMapping>(mappings);
        augmentedMappings.add(createdMapping);
        createdMapping.initRelationships(this, augmentedMappings);
        return createdMapping;
    }

    /**
     * Retrieves the current {@link ServerContext}.
     *
     * @return a ServerContext
     */
    ServerContext getServerContext() {
        return ObjectSetContext.get();
    }

    private void onCreate(String resourceContainer, String resourceId, JsonValue object) throws SynchronizationException {
        PendingLink.handlePendingLinks(mappings, resourceContainer, resourceId, object);
        for (ObjectMapping mapping : mappings) {
            if (mapping.isSyncEnabled()) {
                mapping.onCreate(resourceContainer, resourceId, object);
            }
        }
    }

    private void onUpdate(String resourceContainer, String resourceId, JsonValue oldValue, JsonValue newValue) throws SynchronizationException {
        for (ObjectMapping mapping : mappings) {
            if (mapping.isSyncEnabled()) {
                mapping.onUpdate(resourceContainer, resourceId, oldValue, newValue);
            }
        }
    }

    private void onDelete(String resourceContainer, String resourceId, JsonValue oldValue) throws SynchronizationException {
        for (ObjectMapping mapping : mappings) {
            if (mapping.isSyncEnabled()) {
                mapping.onDelete(resourceContainer, resourceId, oldValue);
            }
        }
    }

    /**
     * ScheduledService interface for supporting scheduled recon.
     */
    @Override
    public void execute(ServerContext context, Map<String, Object> scheduledContext) throws ExecutionException {
        try {
            JsonValue params = new JsonValue(scheduledContext).get(CONFIGURED_INVOKE_CONTEXT);
            String action = params.get("action").asString();

            // "reconcile" in schedule config is the legacy equivalent of the action "recon"
            if ("reconcile".equals(action)
                    || ReconciliationService.ReconAction.isReconAction(action)) {
                JsonValue mapping = params.get("mapping");
                ObjectSetContext.push(context);

                // Legacy support for spelling recon action as reconcile
                if ("reconcile".equals(action)) {
                    params.put("_action", ReconciliationService.ReconAction.recon.toString());
                } else {
                    params.put("_action", action);
                }

                try {
                    reconService.reconcile(ReconciliationService.ReconAction.recon, mapping, Boolean.TRUE, params, null);
                } finally {
                    ObjectSetContext.pop();
                }
            } else {
                throw new ExecutionException("Action '" + action +
                        "' configured in schedule not supported.");
            }
        } catch (JsonValueException jve) {
            throw new ExecutionException(jve);
        } catch (SynchronizationException se) {
            throw new ExecutionException(se);
        }
    }

    @Override
    public void actionInstance(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        try {
            ObjectSetContext.push(context);
            Map<String, Object> result = null;
            JsonValue _params = new JsonValue(request.getAdditionalParameters(), new JsonPointer("params"));
            String resourceContainer;
            String resourceId;
            switch (request.getActionAsEnum(Action.class)) {
                case onCreate:
                    resourceContainer = _params.get("resourceContainer").required().asString();
                    resourceId = _params.get("resourceId").required().asString();
                    logger.debug("Synchronization action=onCreate, resourceContainer={}, resourceId={} ", resourceContainer, resourceId);
                    onCreate(resourceContainer, resourceId, request.getContent());
                    break;
                case onUpdate:
                    resourceContainer =  _params.get("resourceContainer").required().asString();
                    resourceId = _params.get("resourceId").required().asString();
                    logger.debug("Synchronization action=onUpdate, resourceContainer={}, resourceId={}", resourceContainer, resourceId);
                    onUpdate(resourceContainer, resourceId, null, request.getContent());
                    break;
                case onDelete:
                    resourceContainer =  _params.get("resourceContainer").required().asString();
                    resourceId = _params.get("resourceId").required().asString();
                    logger.debug("Synchronization action=onDelete, resourceContainer={}, resourceId={}", resourceContainer, resourceId);
                    onDelete(resourceContainer, resourceId, null);
                    break;
                case recon:
                    result = new HashMap<String, Object>();
                    JsonValue mapping = _params.get("mapping").required();
                    logger.debug("Synchronization action=recon, mapping={}", mapping);
                    String reconId = reconService.reconcile(ReconciliationService.ReconAction.recon, mapping, Boolean.TRUE, _params, request.getContent());
                    result.put("reconId", reconId);
                    result.put("_id", reconId);
                    result.put("comment1", "Deprecated API on sync service. Call recon action on recon service instead.");
                    result.put("comment2", "Deprecated return property reconId, use _id instead.");
                    break;
                case performAction:
                    logger.debug("Synchronization action=performAction, params={}", _params);
                    ObjectMapping objectMapping = getMapping(_params.get("mapping").required().asString());
                    objectMapping.performAction(_params);
                    result = new HashMap<String, Object>();
                    //result.put("status", performAction(_params));
                    break;
                default:
                    throw new BadRequestException("Action" + request.getAction() + " is not supported.");
            }
            handler.handleResult(new JsonValue(result));
        } catch (IllegalArgumentException e) {
            handler.handleError(new BadRequestException("Action:" + request.getAction()
                    + " is not supported for resource collection", e));
        } catch (SynchronizationException se) {
            handler.handleError(new ConflictException(se.getMessage(), se));
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        } finally {
            ObjectSetContext.pop();
        }
    }

    @Override
    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    @Override
    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }
}
