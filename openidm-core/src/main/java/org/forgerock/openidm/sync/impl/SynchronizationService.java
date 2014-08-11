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

import static org.forgerock.util.Iterables.filter;

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
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.util.Predicate;
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

    /** Actions supported by this service. */
    public enum SyncServiceAction {
        notifyCreate, notifyUpdate, notifyDelete, recon, performAction
    }

    /** Logger */
    private final static Logger logger = LoggerFactory.getLogger(SynchronizationService.class);

    /** The resource container action parameter. */
    public static final String ACTION_PARAM_RESOURCE_CONTAINER = "resourceContainer";
    /** The resource id action parameter. */
    public static final String ACTION_PARAM_RESOURCE_ID = "resourceId";

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

    /**
     * Results we can expect from synchronizing a specific source object to a given mapping.
     */
    private enum MappingSyncResult {
        SUCCESSFUL, SKIPPED, FAILED
    }

    /**
     * Local interface to encapsulate the notifyCreate/notifyUpdate/notifyDelete ObjectMapping synchronization
     * across all mappings.
     *
     * @see #syncAllMappings(org.forgerock.openidm.sync.impl.SynchronizationService.SyncAction, String, String)
     */
    private interface SyncAction {
        JsonValue sync(ObjectMapping mapping) throws SynchronizationException;
    }

    /**
     * Synchronize all mappings; keeping track of success/failure conditions.
     *
     * @param action the {@code SyncAction} to perform
     * @param resourceContainer the source object set
     * @param resourceId the source object id
     * @returns a JsonValue list of ObjectMappings' sync results
     * @throws SynchronizationException on failure to sync one of the mappings
     */
    private JsonValue syncAllMappings(SyncAction action, final String resourceContainer, final String resourceId)
            throws SynchronizationException {
        final JsonValue syncDetails = new JsonValue(new ArrayList<Object>());
        SynchronizationException exceptionPending = null;

        // cannot sync anything with empty resourceId
        if (resourceId.isEmpty()) {
            return syncDetails;
        }

        // mappings that should be synced are those which are enabled and whose
        // source object set matches the resource container
        final Predicate<ObjectMapping> thatMatchSource = new Predicate<ObjectMapping>() {
            public boolean apply(ObjectMapping objectMapping) {
                return objectMapping.isSyncEnabled()
                        && objectMapping.isSourceObject(resourceContainer, resourceId);
            }
        };

        for (final ObjectMapping mapping : filter(mappings, thatMatchSource)) {
            JsonValue mappingResult = new JsonValue(new HashMap<String,Object>());
            MappingSyncResult result = MappingSyncResult.SUCCESSFUL;
            try {
                if (exceptionPending == null) {
                    // no failures yet, sync this one
                    mappingResult = action.sync(mapping);
                } else {
                    // we've already failed, skip the sync attempt
                    result = MappingSyncResult.SKIPPED;
                }
            } catch (SynchronizationException e) {
                // failed to sync; store the exception and mark as failed
                exceptionPending = new SynchronizationException(e.getMessage(), e.getCause());
                // the exception detail contains the mapping result
                mappingResult = e.getDetail();
                mappingResult.put("cause", exceptionPending.toJsonValue().getObject());
                result = MappingSyncResult.FAILED;
            } finally {
                mappingResult.put("result", result.name());
                mappingResult.put("mapping", mapping.getName());
                mappingResult.put("targetObjectSet", mapping.getTargetObjectSet());
                syncDetails.add(mappingResult);
            }
        }

        // If there was a failure, send the synchronization results along in the exception...
        if (exceptionPending != null) {
            exceptionPending.setDetail(syncDetails);
            throw exceptionPending;
        }

        // ...otherwise, return them
        return syncDetails;
    }

    private JsonValue notifyCreate(ServerContext context, final String resourceContainer, final String resourceId, final JsonValue object)
            throws SynchronizationException {
        // Handle pending link action if present
        PendingAction.handlePendingActions(context, ReconAction.LINK, mappings, resourceContainer, resourceId, object);
        return syncAllMappings(new SyncAction() {
            @Override
            public JsonValue sync(ObjectMapping mapping) throws SynchronizationException {
                return mapping.notifyCreate(resourceContainer, resourceId, object);
            }
        }, resourceContainer, resourceId);
    }

    private JsonValue notifyUpdate(ServerContext context, final String resourceContainer, final String resourceId, final JsonValue oldValue, final JsonValue newValue)
            throws SynchronizationException {
        return syncAllMappings(new SyncAction() {
            @Override
            public JsonValue sync(ObjectMapping mapping) throws SynchronizationException {
                return mapping.notifyUpdate(resourceContainer, resourceId, oldValue, newValue);
            }
        }, resourceContainer, resourceId);
    }

    private JsonValue notifyDelete(ServerContext context, final String resourceContainer, final String resourceId, final JsonValue oldValue)
            throws SynchronizationException {
        // Handle pending unlink action if present
        PendingAction.handlePendingActions(context, ReconAction.UNLINK, mappings, resourceContainer, resourceId, oldValue);
        return syncAllMappings(new SyncAction() {
            @Override
            public JsonValue sync(ObjectMapping mapping) throws SynchronizationException {
                return mapping.notifyDelete(resourceContainer, resourceId, oldValue);
            }
        }, resourceContainer, resourceId);
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
        } catch (ResourceException re) {
            throw new ExecutionException(re);
        }
    }

    @Override
    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            ObjectSetContext.push(context);
            JsonValue _params = new JsonValue(request.getAdditionalParameters(), new JsonPointer("params"));
            String resourceContainer;
            String resourceId;
            switch (request.getActionAsEnum(SyncServiceAction.class)) {
                case notifyCreate:
                    resourceContainer = _params.get(ACTION_PARAM_RESOURCE_CONTAINER).required().asString();
                    resourceId = _params.get(ACTION_PARAM_RESOURCE_ID).required().asString();
                    logger.debug("Synchronization action=notifyCreate, resourceContainer={}, resourceId={} ", resourceContainer, resourceId);
                    handler.handleResult(notifyCreate(context, resourceContainer, resourceId, request.getContent().get("newValue")));
                    break;
                case notifyUpdate:
                    resourceContainer = _params.get(ACTION_PARAM_RESOURCE_CONTAINER).required().asString();
                    resourceId = _params.get(ACTION_PARAM_RESOURCE_ID).required().asString();
                    logger.debug("Synchronization action=notifyUpdate, resourceContainer={}, resourceId={}", resourceContainer, resourceId);
                    handler.handleResult(notifyUpdate(context, resourceContainer, resourceId, request.getContent().get("oldValue"), request.getContent().get("newValue")));
                    break;
                case notifyDelete:
                    resourceContainer = _params.get(ACTION_PARAM_RESOURCE_CONTAINER).required().asString();
                    resourceId = _params.get(ACTION_PARAM_RESOURCE_ID).required().asString();
                    logger.debug("Synchronization action=notifyDelete, resourceContainer={}, resourceId={}", resourceContainer, resourceId);
                    handler.handleResult(notifyDelete(context, resourceContainer, resourceId, request.getContent().get("oldValue")));
                    break;
                case recon:
                    JsonValue result = new JsonValue(new HashMap<String, Object>());
                    JsonValue mapping = _params.get("mapping").required();
                    logger.debug("Synchronization action=recon, mapping={}", mapping);
                    String reconId = reconService.reconcile(ReconciliationService.ReconAction.recon, mapping, Boolean.TRUE, _params, request.getContent());
                    result.put("reconId", reconId);
                    result.put("_id", reconId);
                    result.put("comment1", "Deprecated API on sync service. Call recon action on recon service instead.");
                    result.put("comment2", "Deprecated return property reconId, use _id instead.");
                    handler.handleResult(result);
                    break;
                case performAction:
                    logger.debug("Synchronization action=performAction, params={}", _params);
                    ObjectMapping objectMapping = getMapping(_params.get("mapping").required().asString());
                    objectMapping.performAction(_params);
                    //result.put("status", performAction(_params));
                    handler.handleResult(new JsonValue(new HashMap<String, Object>()));
                    break;
                default:
                    throw new BadRequestException("Action" + request.getAction() + " is not supported.");
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (IllegalArgumentException e) { // from getActionAsEnum
            handler.handleError(new BadRequestException(e.getMessage(), e));
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
