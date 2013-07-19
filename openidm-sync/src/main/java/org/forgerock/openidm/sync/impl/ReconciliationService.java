/**
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
*
*/
package org.forgerock.openidm.sync.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.util.ResourceUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reconciliation service implementation
 *
 * @author aegloff
 */
@Component(name = ReconciliationService.PID, immediate = true, policy = ConfigurationPolicy.OPTIONAL)
@Service()
@Properties({
        @Property(name = "service.description", value = "Reconciliation Service"),
        @Property(name = "service.vendor", value = "ForgeRock AS"),
        @Property(name = "openidm.router.prefix", value = "recon")
})
public class ReconciliationService
        implements RequestHandler, Reconcile {
    final static Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

    public static final String PID = "org.forgerock.openidm.recon";

    public enum ReconAction {
        recon, reconByQuery, reconById;

        /**
         * Convenience helper that checks if a given string
         * is contained in this enum
         * @param action the stringified action to check
         * @return true if it is contained in this enum, false if not
         */
        public static boolean isReconAction(String action) {
            try {
                valueOf(action);
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
    };

    final EnhancedConfig enhancedConfig = new JSONEnhancedConfig();

    @Reference(
            cardinality = ReferenceCardinality.OPTIONAL_UNARY,
            policy = ReferencePolicy.DYNAMIC
    )
    Mappings mappings;
    protected void bindMappings(final Mappings service) {
        mappings = service;
    }

    protected void unbindMappings(final Mappings service) {
        mappings = null;
    }

    /**
     * The thread pool for executing full reconciliation runs.
     */
    ExecutorService fullReconExecutor;

    /**
     * Map from reconciliation ID to the run itself
     * In historical start order, oldest first.
     */
    Map<String, ReconciliationContext> reconRuns =
            Collections.synchronizedMap(new LinkedHashMap<String, ReconciliationContext>());

    /**
     *  The approximate max number of runs in COMPLETED state to keep in the recon runs list
     */
    private int maxCompletedRuns;

    /**
     * Get the the list of all reconciliations, or details of one specific recon instance
     *
     * {@inheritDoc}
     */
    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        try {
        String localId = getLocalId(request.getResourceName());
        Map<String, Object> result = null;
        result = new LinkedHashMap<String, Object>();

        if (localId == null) {
            List<Map> runList = new ArrayList<Map>();
            for (ReconciliationContext entry : reconRuns.values()) {
                runList.add(entry.getSummary());
            }
            result.put("reconciliations", runList);
        } else {
            Map<String, Object> summaryMap = null;
            // First try and get it from in memory
            for (ReconciliationContext entry : reconRuns.values()) {
                if (entry.getReconId().equals(localId)) {
                    handler.handleResult(new Resource(localId, null, new JsonValue(entry.getSummary())));
                    return;
                }
            }
            // Next, if not in memory, try and get it from audit log
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("_queryId", "audit-by-recon-id-type");
            params.put("reconId", localId);
            params.put("entryType", "summary");
            Map<String, Object> queryResult = null;//TODO FIXME getRouter().query("audit/recon", params);
            summaryMap = (Map<String, Object>)queryResult.get("summary");

            if (summaryMap == null) {
                throw new NotFoundException();
            }
            result = (Map<String, Object>)summaryMap.get("messageDetail");
            result.put("_id", localId);
        }
            handler.handleResult(new Resource(localId, null, new JsonValue(result)));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        String id = request.getResourceName();
        JsonValue paramsVal = new JsonValue(request.getAdditionalActionParameters());
        String action = paramsVal.get("_action").asString();
        if (action == null) {
            throw new BadRequestException("Action parameter is not present or value is null");
        }

        if (id == null) {
            // operation on collection
            if (ReconciliationService.ReconAction.isReconAction(action)) {
                try {
                    JsonValue mapping = paramsVal.get("mapping").required();
                    logger.debug("Reconciliation action of mapping {}", mapping);
                    Boolean waitForCompletion = Boolean.FALSE;
                    JsonValue waitParam = paramsVal.get("waitForCompletion").defaultTo(Boolean.FALSE);
                    if (waitParam.isBoolean()) {
                        waitForCompletion = waitParam.asBoolean();
                    } else {
                        waitForCompletion = Boolean.parseBoolean(waitParam.asString());
                    }
                    result.put("_id", reconcile(mapping, waitForCompletion, paramsVal));
                } catch (SynchronizationException se) {
                   throw new ConflictException(se);
                }
            } else {
                throw new BadRequestException("Action " + action + " on reconciliation not supported " + request.getAdditionalActionParameters());
            }
        } else {
            // operation on individual resource
            ReconciliationContext foundRun = reconRuns.get(id);
            if (foundRun == null) {
                throw new NotFoundException("Reconciliation with id " + id + " not found." );
            }

            if ("cancel".equalsIgnoreCase(action)) {
                foundRun.cancel();
                result.put("_id", foundRun.getReconId());
                result.put("action", action);
                result.put("status", "SUCCESS");
            } else {
                throw new BadRequestException("Action " + action + " on recon run " + id + " not supported " + request.getAdditionalActionParameters());
            }
        }
            handler.handleResult(new JsonValue(result));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    /**
     * {@inheritDoc}
     */
    public String reconcile(final JsonValue mapping, Boolean synchronous, JsonValue reconParams) throws SynchronizationException {
        final ReconciliationContext reconContext = newReconContext(mapping, reconParams);
        if (Boolean.TRUE.equals(synchronous)) {
            reconcile(mapping, reconContext);
        } else {
            final ServerContext threadContext = ObjectSetContext.get();
            Runnable command = new Runnable() {
                @Override
                public void run() {
                    try {
                        ObjectSetContext.push(threadContext);
                        reconcile(mapping, reconContext);
                    } catch (SynchronizationException ex) {
                        logger.info("Reconciliation reported exception", ex);
                    } catch (Exception ex) {
                        logger.warn("Reconciliation failed with unexpected exception", ex);
                    }
                }
            };
            fullReconExecutor.execute(command);
        }
        return reconContext.getReconId();
    }

    /**
     * Allocates a new reconciliation run's context, including its identifier
     * Separate from the actual execution so that the execution can happen asynchronously,
     * whilst we hand back the identifier to the caller.
     * @return a new reconciliation context
     */
    private ReconciliationContext newReconContext(JsonValue mapping, JsonValue reconParams) throws SynchronizationException {
        ReconciliationContext reconContext = null;
        if (mappings == null) {
            throw new SynchronizationException("Unknown mapping type, no mappings configured");
        }

        ServerContext context = ObjectSetContext.get();
        ObjectMapping objMapping = null;
        if (mapping.isString()) {
            objMapping = mappings.getMapping(mapping.asString());
        } else if (mapping.isMap()) {
// FIXME: Entire mapping configs defined in scheduled jobs?! Not a good idea! –PB
            objMapping = mappings.createMapping(mapping);
        } else {
            throw new SynchronizationException("Unknown mapping type");
        }
        try {
            reconContext = new ReconciliationContext(objMapping, context, reconParams, this);
        } catch (BadRequestException ex) {
            throw new SynchronizationException("Failure in initializing reconciliation: "
                    + ex.getMessage(), ex);
        }
        return reconContext;
    }

    /**
     * Start a full reconcliation run
     * @param mapping the object mapping to reconclie
     * @param reconContext a new reconciliation context. Do not re-use these contexts for more than one call to reconcile.
     * @throws SynchronizationException
     */
    private void reconcile(JsonValue mapping, ReconciliationContext reconContext) throws SynchronizationException {
        addReconRun(reconContext);
        try {
            reconContext.getObjectMapping().recon(reconContext); // throws SynchronizationException
        } catch (SynchronizationException ex) {
            if (reconContext.isCanceled()) {
                reconContext.setStage(ReconStage.COMPLETED_CANCELED);
            } else {
                reconContext.setStage(ReconStage.COMPLETED_FAILED);
            }
            throw ex;
        } catch (RuntimeException ex) {
            reconContext.setStage(ReconStage.COMPLETED_FAILED);
            throw ex;
        }
        reconContext.setStage(ReconStage.COMPLETED_SUCCESS);
    }

    /**
     * Add a reconciliation run to the cached list of reconcliation runs.
     * May clean out old entries of completed reconciliation runs.
     * @param reconContext the reconciliation run specific context
     */
    private void addReconRun(ReconciliationContext reconContext) {
        // Clean out run history if needed
        // Since it only checks for completed runs when a new run is started this
        // only provides for approximate adherence to maxCompleteRuns
        synchronized(reconRuns) {
            if (reconRuns.size() > maxCompletedRuns) {
                int completedCount = 0;
                // Since oldest runs are first in the list, inspect backwards
                ListIterator<String> iter = new ArrayList<String>(reconRuns.keySet())
                        .listIterator(reconRuns.size());
                while (iter.hasPrevious()) {
                    String key = iter.previous();
                    ReconciliationContext aRun = reconRuns.get(key);
                    if (aRun.getStage().isComplete()) {
                        ++completedCount;
                        if (completedCount > maxCompletedRuns) {
                            reconRuns.remove(key);
                        }
                    }
                }
            }
            reconRuns.put(reconContext.getReconId(), reconContext);
        }
    }

    // TODO: replace with common utility to handle ID, this is temporary
    private String getLocalId(String id) {
        String localId = null;
        if (id != null) {
            int lastSlashPos = id.lastIndexOf("/");
            if (lastSlashPos > -1) {
                localId = id.substring(0, id.lastIndexOf("/"));
            } else {
                localId = id;
            }
            logger.trace("Full id: {} Extracted local id: {}", id, localId);
        }
        return localId;
    }

    // TODO: replace with common utility to handle ID, this is temporary
    private String getObjectField(String id) {
        String type = null;
        if (id != null) {
            int slashPos = id.indexOf("/");
            if (slashPos > -1) {
                type = id.substring(slashPos+1, id.length());
                logger.trace("Full id: {} Extracted type: {}", id, type);
            }
        }
        return type;
    }

    @Activate
    void activate(ComponentContext compContext) {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());

        JsonValue config = null;
        try {
            // Until we have a recon service config, allow overrides via (unsupported) properties
            String maxCompletedStr =
                    IdentityServer.getInstance().getProperty("openidm.recon.maxcompletedruns", "100");
            maxCompletedRuns = Integer.parseInt(maxCompletedStr);

            int maxConcurrentFullRecons = 10; // TODO: make configurable
            fullReconExecutor = Executors.newFixedThreadPool(maxConcurrentFullRecons);

            config = enhancedConfig.getConfigurationAsJson(compContext);
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid and could not be parsed, can not start reconciliation service: "
                    + ex.getMessage(), ex);
            throw ex;
        }

        logger.info("Reconciliation service started.");
    }

    /* Currently rely on deactivate/activate to be called by DS if config changes instead
    @Modified
    void modified(ComponentContext compContext) {
        logger.info("Configuration of service changed.");
        deactivate(compContext);
        activate(compContext);
    }
    */


    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext);
        logger.info("Reconciliation service stopped.");
    }

    /**
     * Accessor to router
     * @return handle to router accessor
     */
    ServerContext getRouter() {
        return ObjectSetContext.get();
    }

}
