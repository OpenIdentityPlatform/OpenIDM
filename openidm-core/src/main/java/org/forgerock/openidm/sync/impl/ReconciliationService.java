/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions copyright 2012-2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.util.ResourceUtil.notSupported;
import static org.forgerock.util.query.QueryFilter.and;
import static org.forgerock.util.query.QueryFilter.equalTo;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import javax.management.MBeanServer;
import javax.management.ObjectName;

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
import org.forgerock.json.JsonValueException;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.sync.ReconContext;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.util.promise.Promise;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reconciliation service implementation.
 */
@Component(name = ReconciliationService.PID, immediate = true, policy = ConfigurationPolicy.OPTIONAL)
@Service()
@Properties({
        @Property(name = "service.description", value = "Reconciliation Service"),
        @Property(name = "service.vendor", value = "ForgeRock AS"),
        @Property(name = "openidm.router.prefix", value = "/recon/*")
})
public class ReconciliationService
        implements RequestHandler, Reconcile, ReconciliationServiceMBean {
    final static Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

    public static final String PID = "org.forgerock.openidm.recon";
    private static final String MBEAN_NAME = "org.forgerock.openidm.recon:type=Reconciliation";
    private static final String AUDIT_RECON = "audit/recon";
    private static final String SUMMARY = "summary";

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
    }

    /**
     * The Connection Factory
     */
    @Reference(policy = ReferencePolicy.STATIC)
    protected IDMConnectionFactory connectionFactory;

    protected void bindConnectionFactory(IDMConnectionFactory connectionFactory) {
    	this.connectionFactory = connectionFactory;
    }
    
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

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
    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            if (request.getResourcePathObject().isEmpty()) {
                List<Map> runList = new ArrayList<>();
                for (ReconciliationContext entry : reconRuns.values()) {
                    runList.add(entry.getSummary());
                }
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("reconciliations", runList);
                return newResourceResponse("", null, new JsonValue(result)).asPromise();
            } else {
                final String localId = request.getResourcePathObject().leaf();
                // First try and get it from in memory
                if (reconRuns.containsKey(localId)) {
                	return newResourceResponse(localId, null, new JsonValue(reconRuns.get(localId).getSummary())).asPromise();
                } else {
                    // Next, if not in memory, try and get it from audit log
                    final Collection<ResourceResponse> queryResult = new ArrayList<>();
                    getConnectionFactory().getConnection().query(
                            context,
                            Requests.newQueryRequest(AUDIT_RECON).setQueryFilter(
                                    and(
                                            equalTo(new JsonPointer(ReconAuditEventBuilder.RECON_ID), localId),
                                            equalTo(new JsonPointer(ReconAuditEventBuilder.ENTRY_TYPE), SUMMARY)
                                    )
                            ),
                            queryResult);
                    
                    ResourceResponse response = null;
                    
                    if (queryResult.isEmpty()) {
                    	return new NotFoundException("Reconciliation with id " + localId + " not found." ).asPromise();
                    } else {
                        for (ResourceResponse resource : queryResult) {
                        	response = newResourceResponse(localId, null, 
                                        resource.getContent().get(ReconAuditEventBuilder.MESSAGE_DETAIL).expect(Map.class));
                        	break;
                        }
                    }

                    return response.asPromise();

                }
            }
        } catch (ResourceException e) {
        	return e.asPromise();
        } catch (JsonValueException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (Exception e) {
        	return new InternalServerErrorException(e).asPromise();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException>  handleCreate(Context context, CreateRequest request) {
        return notSupported(request).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        return notSupported(request).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return notSupported(request).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest request, 
    		final QueryResourceHandler handler) {
        return notSupported(request).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        return notSupported(request).asPromise();
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        ObjectSetContext.push(context);
        try {
            if (request.getAction() == null) {
                throw new BadRequestException("Action parameter is not present or value is null");
            }

            Map<String, Object> result = new LinkedHashMap<String, Object>();
            JsonValue paramsVal = new JsonValue(request.getAdditionalParameters());

            if (request.getResourcePathObject().isEmpty()) {
                // operation on collection
                if (ReconciliationService.ReconAction.isReconAction(request.getAction())) {
                    String reconId;
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
                        reconId = reconcile(ReconAction.valueOf(request.getAction()), mapping, waitForCompletion, 
                                paramsVal, request.getContent());
                        result.put("_id",  reconId);
                        result.put("state", reconRuns.get(reconId).getState());
                    } catch (SynchronizationException se) {
                        throw new ConflictException(se);
                    }
                } else {
                    throw new BadRequestException("Action " + request.getAction() + " on reconciliation not supported " 
                            + request.getAdditionalParameters());
                }
            } else {
                // operation on individual resource
                final String id = request.getResourcePathObject().leaf();
                ReconciliationContext foundRun = reconRuns.get(id);
                if (foundRun == null) {
                    throw new NotFoundException("Reconciliation with id " + id + " not found." );
                }

                if ("cancel".equalsIgnoreCase(request.getAction())) {
                    foundRun.cancel();
                    result.put("_id", foundRun.getReconId());
                    result.put("action", request.getAction());
                    result.put("status", "SUCCESS");
                } else {
                    throw new BadRequestException("Action " + request.getAction() + " on recon run " + id 
                            + " not supported " + request.getAdditionalParameters());
                }
            }
            return newActionResponse(new JsonValue(result)).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (JsonValueException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } finally {
            ObjectSetContext.pop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String reconcile(ReconAction reconAction, final JsonValue mapping, Boolean synchronous, 
            JsonValue reconParams, JsonValue config) throws ResourceException {
        
        ObjectMapping objMapping = null;
        if (mapping.isString()) {
            objMapping = mappings.getMapping(mapping.asString());
        } else if (mapping.isMap()) {
            // FIXME: Entire mapping configs defined in scheduled jobs?! Not a good idea! –PB
            objMapping = mappings.createMapping(mapping);
        } else {
            throw new BadRequestException("Unknown mapping type");
        }

        // Set the ReconContext on the request context chain.
        Context currentContext = ObjectSetContext.pop();
        ObjectSetContext.push(new ReconContext(currentContext, objMapping.getName()));
        
        final ReconciliationContext reconciliationContext =
                newReconContext(reconAction, objMapping, reconParams, config);
        
        
        addReconRun(reconciliationContext);
        if (Boolean.TRUE.equals(synchronous)) {
            reconcile(reconciliationContext);
        } else {
            final Context threadContext = ObjectSetContext.get();
            Runnable command = new Runnable() {
                @Override
                public void run() {
                    try {
                        ObjectSetContext.push(threadContext);
                        reconcile(reconciliationContext);
                    } catch (SynchronizationException ex) {
                        logger.info("Reconciliation reported exception", ex);
                    } catch (Exception ex) {
                        logger.warn("Reconciliation failed with unexpected exception", ex);
                    }
                    finally {
                        ObjectSetContext.pop();
                    }
                }
            };
            fullReconExecutor.execute(command);
        }
        return reconciliationContext.getReconId();
    }

    /**
     * Allocates a new reconciliation run's context, including its identifier
     * Separate from the actual execution so that the execution can happen asynchronously,
     * whilst we hand back the identifier to the caller.
     *
     * @param reconAction the recon action
     * @param mapping the mapping configuration
     * @param reconParams
     * @return a new reconciliation context
     */
    private ReconciliationContext newReconContext(ReconAction reconAction, ObjectMapping mapping, JsonValue reconParams,
            JsonValue config) throws ResourceException {
        if (mappings == null) {
            throw new BadRequestException("Unknown mapping type, no mappings configured");
        }

        Context context = ObjectSetContext.get();
        return new ReconciliationContext(reconAction, mapping, context, reconParams, config, this);
    }

    /**
     * Start a full reconciliation run
     *
     * @param reconContext a new reconciliation context. Do not re-use these contexts for more than one call to reconcile.
     * @throws SynchronizationException
     */
    private void reconcile(ReconciliationContext reconContext) throws SynchronizationException {
        try {
            reconContext.getObjectMapping().recon(reconContext); // throws SynchronizationException
        } catch (SynchronizationException ex) {
            if (reconContext.isCanceled()) {
                reconContext.setStage(ReconStage.COMPLETED_CANCELED);
            } else {
                reconContext.setStage(ReconStage.COMPLETED_FAILED);
                throw ex;
            }
        } catch (RuntimeException ex) {
            reconContext.setStage(ReconStage.COMPLETED_FAILED);
            throw ex;
        }
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

    @Activate
    void activate(ComponentContext compContext) {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());

        try {
            // Until we have a recon service config, allow overrides via (unsupported) properties
            String maxCompletedStr =
                    IdentityServer.getInstance().getProperty("openidm.recon.maxcompletedruns", "100");
            maxCompletedRuns = Integer.parseInt(maxCompletedStr);

            int maxConcurrentFullRecons = 10; // TODO: make configurable
            fullReconExecutor = Executors.newFixedThreadPool(maxConcurrentFullRecons);

            registerMBean();
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
        unregisterMBean();
        logger.info("Reconciliation service stopped.");
    }

    /**
     * Returns the {@link Context}
     * 
     * @return the {@link Context}
     */
    Context getContext() {
        return ObjectSetContext.get();
    }

    private void registerMBean() {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName mbeanObjectName = new ObjectName(MBEAN_NAME);
            mBeanServer.registerMBean(this, mbeanObjectName);
        } catch (Exception ex) {
            logger.error("Failed to register reconciliation MBean", ex);
            throw new RuntimeException("Failed to register reconciliation MBean", ex);
        }
    }

    private void unregisterMBean() {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName mbeanObjectName = new ObjectName(MBEAN_NAME);
            mBeanServer.unregisterMBean(mbeanObjectName);
        } catch (Exception ex) {
            logger.error("Failed to unregister reconciliation MBean", ex);
            throw new RuntimeException("Failed to unregister reconciliation MBean", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutorService getThreadPool() {
        return fullReconExecutor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getActiveThreads() throws ResourceException {
        if (fullReconExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) fullReconExecutor).getActiveCount();
        } else if (fullReconExecutor instanceof ScheduledThreadPoolExecutor) {
            return ((ScheduledThreadPoolExecutor) fullReconExecutor).getActiveCount();
        } else {
            logger.error("Unable to get the number of active threads in recon thread pool");
            throw new InternalServerErrorException("Unable to get the number of active threads in recon thread pool");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCorePoolSize() throws ResourceException {
        if (fullReconExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) fullReconExecutor).getCorePoolSize();
        } else if (fullReconExecutor instanceof ScheduledThreadPoolExecutor) {
            return ((ScheduledThreadPoolExecutor) fullReconExecutor).getCorePoolSize();
        } else {
            logger.error("Unable to get the core pool size in recon thread pool");
            throw new InternalServerErrorException("Unable to get the core pool size in recon thread pool");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPoolSize() throws ResourceException {
        if (fullReconExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) fullReconExecutor).getPoolSize();
        } else if (fullReconExecutor instanceof ScheduledThreadPoolExecutor) {
            return ((ScheduledThreadPoolExecutor) fullReconExecutor).getPoolSize();
        } else {
            logger.error("Unable to get the pool size in recon thread pool");
            throw new InternalServerErrorException("Unable to get the pool size in recon thread pool");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLargestPoolSize() throws ResourceException {
        if (fullReconExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) fullReconExecutor).getLargestPoolSize();
        } else if (fullReconExecutor instanceof ScheduledThreadPoolExecutor) {
            return ((ScheduledThreadPoolExecutor) fullReconExecutor).getLargestPoolSize();
        } else {
            logger.error("Unable to get the largest pool size in recon thread pool");
            throw new InternalServerErrorException("Unable to get the largest pool size in recon thread pool");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaximumPoolSize() throws ResourceException {
        if (fullReconExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) fullReconExecutor).getMaximumPoolSize();
        } else if (fullReconExecutor instanceof ScheduledThreadPoolExecutor) {
            return ((ScheduledThreadPoolExecutor) fullReconExecutor).getMaximumPoolSize();
        } else {
            logger.error("Unable to get the maximum pool size in recon thread pool");
            throw new InternalServerErrorException("Unable to get the maximum pool size in recon thread pool");
        }
    }
}
