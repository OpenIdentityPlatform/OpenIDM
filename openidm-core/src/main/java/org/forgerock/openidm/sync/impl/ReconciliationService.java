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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.osgi.OsgiName;
import org.forgerock.openidm.osgi.ServiceUtil;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetJsonResource;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.sync.SynchronizationException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
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
public class ReconciliationService extends ObjectSetJsonResource
        implements Reconcile {
    final static Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

    public static final String PID = "org.forgerock.openidm.recon";

    final EnhancedConfig enhancedConfig = new JSONEnhancedConfig();

    @Reference(
            cardinality = ReferenceCardinality.OPTIONAL_UNARY,
            policy = ReferencePolicy.DYNAMIC
    )
    Mappings mappings;
    
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
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        
        if (localId == null && type == null) {
            List<Map> runList = new ArrayList<Map>();
            for (ReconciliationContext entry : reconRuns.values()) {
                runList.add(entry.getSummary());
            }
            result.put("reconciliations", runList);
        } else {
            // TODO: support
            throw new ForbiddenException("Operation not supported"); // really "not supported"
            // NotFoundException
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        throw new ForbiddenException("Operation not supported"); // really "not supported"
    }

    /**
     * {@inheritDoc}
     */
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {
        //if (rev == null) {
        //    throw new ConflictException("Object passed into update does not have revision it expects set.");
        //}
        throw new ForbiddenException("Operation not supported"); // really "not supported"
    }

    /**
     * {@inheritDoc}
     */
    public void delete(String fullId, String rev) throws ObjectSetException {
        // TODO: support
        //if (rev == null) {
        //    throw new ConflictException("Object passed into delete does not have revision it expects set.");
        //}
        throw new ForbiddenException("Operation not supported"); // really "not supported"
    }

    /**
     * {@inheritDoc}
     */
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new ForbiddenException("Operation not supported"); // really "not supported"
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        // TODO: support
        throw new ForbiddenException("Operation not supported"); // really "not supported"
        //String type = fullId;
        //Map<String, Object> result = new HashMap<String, Object>();
        //result.put(QueryConstants.QUERY_RESULT, docs);
        //result.put(QueryConstants.STATISTICS_QUERY_TIME, Long.valueOf(end - start));
        //return result;
    }

    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        
        JsonValue paramsVal = new JsonValue(params);
        String action = paramsVal.get("_action").asString();
        if (action == null) {
            throw new BadRequestException("Action parameter is not present or value is null");                
        }
        
        if (id == null) {
            // operation on collection
            if ("recon".equalsIgnoreCase(action)) {
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
                    result.put("_id", reconcile(mapping, waitForCompletion));
                } catch (SynchronizationException se) {
                   throw new ConflictException(se);
                }
            } else {
                throw new BadRequestException("Action " + action + " on reconciliation not supported " + params);
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
                throw new BadRequestException("Action " + action + " on recon run " + id + " not supported " + params);
            }
        } 
        
        return result;
    }
    
    /**
     * Full reconciliation
     * @param mapping the 
     * @param synchronous whether to synchrnously (TRUE) wait for the reconciliation run, or 
     *  to return immediately (FALSE) with the recon id, which can then be used for subsequent
     *  queries / actions on that reconciliation run.
     */
    public String reconcile(final JsonValue mapping, Boolean synchronous) throws SynchronizationException {
        final ReconciliationContext reconContext = newReconContext(mapping);
        if (Boolean.TRUE.equals(synchronous)) {
            reconcile(mapping, reconContext);
        } else {
            final JsonValue threadContext = ObjectSetContext.get();
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
    private ReconciliationContext newReconContext(JsonValue mapping) throws SynchronizationException {
        if (mappings == null) {
            throw new SynchronizationException("Unknown mapping type, no mappings configured");
        }
        
        JsonValue context = ObjectSetContext.get();
        ObjectMapping objMapping = null;
        if (mapping.isString()) {
            objMapping = mappings.getMapping(mapping.asString());
        } else if (mapping.isMap()) {
// FIXME: Entire mapping configs defined in scheduled jobs?! Not a good idea! –PB 
            objMapping = mappings.createMapping(mapping);
        } else {
            throw new SynchronizationException("Unknown mapping type");
        }
        ReconciliationContext reconContext = new ReconciliationContext(objMapping, context);
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
                localId = id.substring(id.lastIndexOf("/") + 1);
            }
            logger.trace("Full id: {} Extracted local id: {}", id, localId);
        }
        return localId;
    }

    // TODO: replace with common utility to handle ID, this is temporary
    private String getObjectType(String id) {
        String type = null;
        if (id != null) {
            int lastSlashPos = id.lastIndexOf("/");
            if (lastSlashPos > -1) {
                int startPos = 0;
                // This should not be necessary as relative URI should not start with slash
                if (id.startsWith("/")) {
                    startPos = 1;
                }
                type = id.substring(startPos, lastSlashPos);
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
}
