/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.cluster;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetJsonResource;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.util.DateUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Cluster Management Service.
 *
 * @author Chad Kienle
 */
@Component(name = ClusterManager.PID, policy = ConfigurationPolicy.REQUIRE,
    description = "OpenIDM Policy Service", immediate = true)
@Service(value = {ClusterManagementService.class, JsonResource.class})
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Cluster Management Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "cluster")
})
public class ClusterManager extends ObjectSetJsonResource implements ClusterManagementService {

    private final static Logger logger = LoggerFactory.getLogger(ClusterManager.class);
    
    private final static Object repoLock = new Object();
    private final static Object startupLock = new Object();
    
    public static final String PID = "org.forgerock.openidm.cluster";
    
    private final static String QUERY_FAILED_INSTANCE = "query-cluster-instances";
    private final static String QUERY_INSTANCES = "query-all";
    
    /**
     * The instance ID
     */
    private String instanceId;
    
    /**
     * The Repository Service Accessor
     */
    private JsonResourceAccessor accessor = null;
    
    /**
     * A list of listeners to notify when an instance failes
     */
    private Map<String, ClusterEventListener> listeners = new HashMap<String, ClusterEventListener>();

    private ClusterManagerThread clusterManagerThread = null;
    
    private EnhancedConfig enhancedConfig = JSONEnhancedConfig.newInstance();
    
    private ClusterConfig clusterConfig;
    
    /**
     * The current state of this instance
     */
    private InstanceState currentState = null;
    
    private boolean firstCheckin = true;
    
    private boolean failed = false;
    
    private boolean enabled = false;
    
    /** Internal object set router service. */
    @Reference(
        name = "ref_ClusterManager_RepositoryService",
        bind = "bindRepo",
        unbind = "unbindRepo"
    )
    protected RepositoryService repo;
    
    protected void bindRepo(RepositoryService repo) {
        logger.debug("binding RepositoryService");
        this.accessor = new JsonResourceAccessor(repo, null);
    }
    
    protected void unbindRepo(RepositoryService repo) {
        logger.debug("unbinding RepositoryService");
        this.accessor = null;
    }
    
    @Activate
    void activate(ComponentContext compContext) throws ParseException {
        logger.debug("Activating Cluster Management Service with configuration {}", 
                compContext.getProperties());
        JsonValue config = enhancedConfig.getConfigurationAsJson(compContext);
        ClusterConfig clstrCfg = new ClusterConfig(config);
        clusterConfig = clstrCfg;
        instanceId = clusterConfig.getInstanceId();
        
        if (clusterConfig.isEnabled()) {
            enabled = true;
            clusterManagerThread = new ClusterManagerThread(
                    clusterConfig.getInstanceCheckInInterval(),
                    clusterConfig.getInstanceCheckInOffset());
        }
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Cluster Management Service {}", compContext);
        if (clusterConfig.isEnabled()) {
            clusterManagerThread.shutdown();
            synchronized (repoLock) {
                try {
                    InstanceState state = getInstanceState(instanceId);
                    state.updateShutdown();
                    state.setState(InstanceState.STATE_DOWN);
                    updateInstanceState(instanceId, state);
                } catch (JsonResourceException e) {
                    logger.warn("Failed to update instance shutdown timestamp", e);
                }
            }  
        }  
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void startClusterManagement() {
        synchronized (startupLock) {
            if (clusterConfig.isEnabled() && !clusterManagerThread.isRunning()) {
                // Start thread
                logger.info("Starting Cluster Management");
                clusterManagerThread.startup();
            }
        }
    }

    @Override
    public void stopClusterManagement() {
        synchronized (startupLock) {
            if (clusterConfig.isEnabled() && clusterManagerThread.isRunning()) {
                logger.info("Stopping Cluster Management");
                // Start thread
                clusterManagerThread.shutdown();
                checkOut();
            }
        }
    }
    
    @Override
    public boolean isStarted() {
        return clusterManagerThread.isRunning();
    }
    
    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            if (accessor == null) {
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, "Repo router is null");
            }
            if (id == null) {
                // Return a list of all nodes in the cluster
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("_queryId", QUERY_INSTANCES);
                logger.debug("Attempt query {}", QUERY_INSTANCES);
                JsonValue jv = accessor.query(getInstanceStateRepoResource(), new JsonValue(map));
                logger.debug("Query result: " + jv.toString());
                List<Object> list = new ArrayList<Object>();
                for (JsonValue resultEntry : jv.get("result")) {
                    list.add(getInstanceMap(resultEntry));
                }
                resultMap.put("results", list);
            } else {
                logger.debug("Attempting to read instance {} from the database", instanceId); 
                JsonValue instanceValue = accessor.read("cluster/states/" + id);
                if (!instanceValue.isNull()) {
                    resultMap.put("results", getInstanceMap(instanceValue));
                } else {
                    resultMap.put("results", "{}");
                }
            }
        } catch (JsonResourceException e) {
            e.printStackTrace();
            throw new ObjectSetException(ObjectSetException.INTERNAL_ERROR, e);
        }
        
        return resultMap;
    }
    
    /**
     * Creates a map representing an instance's state and recovery statistics that can be used for 
     * responses to read requests.
     * 
     * @param instanceValue an instances state object
     * @return a map representing an instance's state and recovery statistics
     */
    private Map<String, Object> getInstanceMap(JsonValue instanceValue) {
        DateUtil dateUtil = DateUtil.getDateUtil();
        Map<String, Object> instanceInfo = new HashMap<String, Object>();
        String instanceId = instanceValue.get("instanceId").asString();
        InstanceState state = new InstanceState(instanceId, instanceValue.asMap());
        instanceInfo.put("instanceId", instanceId);
        instanceInfo.put("startup", dateUtil.formatDateTime(new Date(state.getStartup())));
        instanceInfo.put("shutdown", "");
        Map<String, Object> recoveryMap = new HashMap<String, Object>();
        switch (state.getState()) {
        case InstanceState.STATE_RUNNING:
            instanceInfo.put("state", "running");
            break;
        case InstanceState.STATE_DOWN:
            instanceInfo.put("state", "down");
            if (!state.hasShutdown()) {
                if (state.getRecoveryAttempts() > 0) {
                    recoveryMap.put("recoveredBy", state.getRecoveringInstanceId());
                    recoveryMap.put("recoveryAttempts", state.getRecoveryAttempts());
                    recoveryMap.put("recoveryStarted", dateUtil.formatDateTime(new Date(state.getRecoveryStarted())));
                    recoveryMap.put("recoveryFinished", dateUtil.formatDateTime(new Date(state.getRecoveryFinished())));
                    recoveryMap.put("detectedDown", dateUtil.formatDateTime(new Date(state.getDetectedDown())));
                    instanceInfo.put("recovery", recoveryMap);
                } else {
                    // Should never reach this state
                    logger.error("Instance {} is in 'down' but has not been shutdown or recovered", instanceId);
                }
            } else {
                instanceInfo.put("shutdown", dateUtil.formatDateTime(new Date(state.getShutdown())));
            }
            break;
        case InstanceState.STATE_PROCESSING_DOWN:
            recoveryMap.put("state", "processing-down");
            recoveryMap.put("recoveryAttempts", state.getRecoveryAttempts());
            recoveryMap.put("recoveringBy", state.getRecoveringInstanceId());
            recoveryMap.put("recoveryStarted", 
                    dateUtil.formatDateTime(new Date(state.getRecoveryStarted())));
            recoveryMap.put("detectedDown", 
                    dateUtil.formatDateTime(new Date(state.getDetectedDown())));
            instanceInfo.put("recovery", recoveryMap);
        }
        return instanceInfo;
    }
    
    
    /**
     * Gets the repository ID prefix
     * 
     * @return the repository ID prefix
     */
    private String getIdPrefix() {
        return "cluster/";
    }
    
    /**
     * Gets the Instance State repository ID.
     */
    private String getInstanceStateRepoId(String instanceId) {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("states/").append(instanceId).toString();
    }
    
    private String getInstanceStateRepoResource() {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("states").toString();
    }
    
    
    public void register(String listenerId, ClusterEventListener listener) {
        logger.debug("Registering listener {}", listenerId);
        listeners.put(listenerId, listener);
    }
    
    
    public void unregister(String listenerId) {
        logger.debug("Unregistering listener {}", listenerId);
        listeners.remove(listenerId);
    }
    
    public void renewRecoveryLease(String instanceId) {
        synchronized (repoLock) {
            try {
                InstanceState state = getInstanceState(instanceId);
                // Update the recovery timestamp
                state = getInstanceState(instanceId);
                state.updateRecoveringTimestamp();
                updateInstanceState(instanceId, state);
                logger.debug("Updated recovery timestamp of instance {}",
                        instanceId);
            } catch (JsonResourceException e) {
                if (e.getCode() != JsonResourceException.CONFLICT) {
                    logger.warn(
                            "Failed to update recovery timestamp of instance {}: {}",
                            instanceId, e.getMessage());
                }
            }
        }
    }
    
    /**
     * Updates an instance's state.
     * 
     * @param instanceId the id of the instance to update
     * @param instanceState the updated InstanceState object
     * @throws JobPersistenceException
     * @throws JsonResourceException
     */
    private void updateInstanceState(String instanceId, InstanceState instanceState) 
            throws JsonResourceException {
        synchronized (repoLock) {
            if (accessor == null) {
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, "Repo router is null");
            }
            String repoId = getInstanceStateRepoId(instanceId);
            
            accessor.update(repoId, instanceState.getRevision(), new JsonValue(instanceState.toMap()));
        }
    }
    
    private InstanceState getInstanceState(String instanceId) throws JsonResourceException {
        synchronized (repoLock) {
            return new InstanceState(instanceId, getOrCreateRepo(getInstanceStateRepoId(instanceId)));
        }
    }
    
    private Map<String, Object> getOrCreateRepo(String repoId) 
            throws JsonResourceException {
        synchronized (repoLock) {
            if (accessor == null) {
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, "Repo router is null");
            }
            Map<String, Object> map;

            map = readFromRepo(repoId).asMap();
            if (map == null) {
                map = new HashMap<String, Object>();
                // create in repo
                logger.debug("Creating repo {}", repoId);
                map = accessor.create(repoId, new JsonValue(map)).asMap();
            }
            return map;
        }
    }
    
    private JsonValue readFromRepo(String repoId) throws JsonResourceException {
        try {
            if (accessor == null) {
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, "Repo router is null");
            }
            logger.debug("Reading from repo {}", repoId);
            return accessor.read(repoId);
        } catch (NotFoundException e) {
            return new JsonValue(null);
        }
    }
    
    /**
     * Updates the timestamp for this instance in the instance check-in map.
     * 
     * @ return the InstanceState object, or null if an expected failure (MVCC) was encountered
     */
    private InstanceState checkIn() {
        InstanceState state = null;
        try {
            logger.debug("Getting instance state for {}", instanceId);
            state = getInstanceState(instanceId);
            if (firstCheckin) {
               state.updateStartup();
               state.clearShutdown();
               firstCheckin = false;
            }
            switch (state.getState()) {
            case InstanceState.STATE_RUNNING:
                // just update the timestamp
                state.updateTimestamp();
                break;
            case InstanceState.STATE_DOWN:
                // instance has been recovered, so switch to "normal" state and update timestamp
                state.setState(InstanceState.STATE_RUNNING);
                logger.debug("Instance {} state changing from {} to {}", new Object[]{instanceId, 
                        InstanceState.STATE_DOWN, InstanceState.STATE_RUNNING});
                state.updateTimestamp();
                break;
            case InstanceState.STATE_PROCESSING_DOWN:
                // rare case, do not update state or timestamp
                // system may attempt to recover itself if recovery timeout has elapsed
                logger.debug("Instance {} is in state {}, waiting for recovery attempt to finish",
                        new Object[]{instanceId, state.getState()});
                return state;
            }
            updateInstanceState(instanceId, state);
            logger.debug("Instance {} state updated successfully");
        } catch (JsonResourceException e) {
            if (e.getCode() != JsonResourceException.CONFLICT) {
                logger.warn("Error updating instance timestamp", e);
            } else {
                // MVCC failure, return null
                logger.info("Failed to set this instance state to {}", state.getState());
                return null;
            }
        }
        return state;
    }
    
    private void checkOut() {
        logger.debug("checkOut()");
        InstanceState state = null;
        try {
            logger.debug("Getting instance state for {}", instanceId);
            state = getInstanceState(instanceId);
            switch (state.getState()) {
            case InstanceState.STATE_RUNNING:
                // just update the timestamp
                state.setState(InstanceState.STATE_DOWN);
                updateInstanceState(instanceId, state);
                logger.debug("Instance {} state updated successfully");
                break;
            case InstanceState.STATE_DOWN:
                // Already down
                break;
            case InstanceState.STATE_PROCESSING_DOWN:
                // Some other instance is processing this down state
                // Leave in this state
                break;
            }
        } catch (JsonResourceException e) {
            if (e.getCode() != JsonResourceException.CONFLICT) {
                logger.warn("Error checking out instance", e);
            } else {
                // MVCC failure, return null
                logger.info("Failed to set this instance state to {}", state.getState());
            }
        }
    }
    
    /**
     * Returns a list of all instances who have timed out.
     * 
     * @return a map of all instances who have timed out (failed).
     */
    private Map<String, InstanceState> findFailedInstances() {
        Map<String, InstanceState> failedInstances = new HashMap<String, InstanceState>();
        try {
            if (accessor == null) {
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, "Repo router is null");
            }
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("_queryId", QUERY_FAILED_INSTANCE);
            String time = InstanceState.pad(System.currentTimeMillis() - clusterConfig.getInstanceTimeout());
            map.put(InstanceState.PROP_TIMESTAMP_LEASE, time);
            logger.debug("Attempt query {} for failed instances", QUERY_FAILED_INSTANCE);
            JsonValue jv = accessor.query(getInstanceStateRepoResource(), new JsonValue(map));
            JsonValue result = jv.get("result");
            if (!result.isNull()) {
                for (JsonValue value : result) {
                    Map<String, Object> valueMap = value.asMap();
                    String id = (String)valueMap.get("instanceId");
                    InstanceState state = new InstanceState(id, valueMap);
                    switch (state.getState()) {
                    case InstanceState.STATE_RUNNING:
                        failedInstances.put(id, state);
                        break;
                    case InstanceState.STATE_PROCESSING_DOWN:
                        // Check if recovering has failed
                        if (state.hasRecoveringFailed(clusterConfig.getInstanceRecoveryTimeout())) {
                            failedInstances.put(id, state);
                        }
                        break;
                    case InstanceState.STATE_DOWN:
                        // allready recovered, do nothing
                        break;
                    }
                }
            }
        } catch (JsonResourceException e) {
            logger.error("Error reading instance check in map", e);
        }
        
        return failedInstances;
    }
    
    /**
     * Recovers a failed instance by looping through all listeners and calling 
     * their instanceFailed method.
     * 
     * @param instanceId the id of the instance to recover
     * @return true if any triggers were "freed", false otherwise
     */
    private boolean recoverFailedInstance(String instanceId, InstanceState state) {
        // First attempt to "claim" the failed instance
        try {
            if (state.getState() == InstanceState.STATE_RUNNING) {
                state.updateDetectedDown();
                state.clearRecoveryAttempts();
            }
            // Update the instance state to recovered
            state.setState(InstanceState.STATE_PROCESSING_DOWN);
            state.setRecoveringInstanceId(this.instanceId);
            state.updateRecoveringTimestamp();
            state.startRecovery();
            updateInstanceState(instanceId, state);
        } catch (JsonResourceException e) {
            if (e.getCode() != JsonResourceException.CONFLICT) {
                logger.warn("Failed to update instance state", e);
            }
            return false;
        }
        
        // Then, attempt recovery
        boolean success = sendEventToListeners(new ClusterEvent(ClusterEventType.RECOVERY_INITIATED, instanceId));
        
        if (success) {
            logger.info("Instance {} recovered successfully", instanceId);
            try {
                // Update the instance state to recovered
                InstanceState newState = getInstanceState(instanceId);
                newState.setState(InstanceState.STATE_DOWN);
                newState.finishRecovery();
                updateInstanceState(instanceId, newState);
            } catch (JsonResourceException e) {
                if (e.getCode() != JsonResourceException.CONFLICT) {
                    logger.warn("Failed to update instance state", e);
                }
                return false;
            }
        } else {
            logger.warn("Instance {} was not successfully recovered", instanceId);
            return false;
        }
        
        return true;
        
    }
    
    /**
     * Sends a ClusterEvent to all registered listeners
     * 
     * @param event the ClusterEvent to handle
     * @return true if the event was handled appropriately, false otherwise
     */
    private boolean sendEventToListeners(ClusterEvent event) {
        boolean success = true;
        for (String listenerId : listeners.keySet()) {
            logger.debug("Notifying listener {} of event {} for instance {}", 
                    new Object[]{listenerId, event.getType(), instanceId});
            ClusterEventListener listener = listeners.get(listenerId);
            if (listener != null && !listener.handleEvent(event)) {
                success = false;
            }
        }
        return success;
    }
    
    /**
     * A thread for managing this instance's lease and detecting cluster events.
     */
    class ClusterManagerThread {
        
        private long checkinInterval;
        private long checkinOffset;
        private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        private ScheduledFuture handler;
        private boolean running = false;
        
        public ClusterManagerThread(long checkinInterval, long checkinOffset) {
            this.checkinInterval = checkinInterval;
        }
        
        public void startup() {
            running = true;
            logger.info("Starting the cluster manager thread");
            handler = scheduler.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    try {
                        // Check in this instance
                        logger.debug("Instance check-in");
                        InstanceState state = checkIn();
                        if (state == null) {
                            if (!failed) {
                                logger.debug("This instance has failed");
                                failed = true;
                                // Notify listeners that this instance has failed
                                sendEventToListeners(new ClusterEvent(ClusterEventType.INSTANCE_FAILED, instanceId));
                                // Set current state to null
                                currentState = null;
                            }
                            return;
                        } else if (failed) {
                            logger.debug("This instance is no longer failed");
                            failed = false;
                        }
                        
                        // If transitioning to a "running" state, send events
                        if (state.getState() == InstanceState.STATE_RUNNING) {
                            if (currentState == null || currentState.getState() != InstanceState.STATE_RUNNING) {
                                sendEventToListeners(new ClusterEvent(ClusterEventType.INSTANCE_RUNNING, instanceId));
                            }
                        }
                        
                        // set current state
                        currentState = state;

                        // Find failed instances
                        logger.debug("Finding failed instances");
                        Map<String, InstanceState> failedInstances = findFailedInstances();
                        logger.debug("{} failed instances found", failedInstances.size());
                        if (failedInstances.size() > 0) {
                            logger.info("Attempting recovery");
                            // Recover failed instance's triggers
                            for (String id : failedInstances.keySet()) {
                                recoverFailedInstance(id, failedInstances.get(id));
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error performing cluster manager thread logic");
                        e.printStackTrace();
                    }
                }
            }, checkinOffset, checkinInterval + checkinOffset, TimeUnit.MILLISECONDS);
        }
        
        public void shutdown() {
            logger.info("Shutting down the cluster manager thread");
            if (handler != null) {
                handler.cancel(true);
            }
            running = false;
        }
        
        public boolean isRunning() {
            return running;
        }
    }
}
