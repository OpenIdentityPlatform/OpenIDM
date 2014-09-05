/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012-2014 ForgeRock AS. All Rights Reserved
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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ServerContext;


/**
 * Represents the information and functionality for a
 * reconciliation run
 *
 * @author aegloff
 */
public class ReconciliationContext {

    ObjectMapping mapping;
    ReconciliationService service;

    // The additional recon parameters
    private JsonValue reconParams;
    
    // The overriding configuration
    private JsonValue overridingConfig;

    private ReconStage stage = ReconStage.ACTIVE_INITIALIZED;
    private String reconId;

    private boolean canceled = false;
    private ReconTypeHandler reconTypeHandler;
    private final ReconciliationStatistic reconStat;
    private ExecutorService executor;

    // If set, the list of all queried source Ids
    private Set<String> sourceIds;
    // If set, the list of all queried target Ids
    private Set<String> targetIds;

    private Integer totalSourceEntries;
    private Integer totalTargetEntries;
    private Integer totalLinkEntries;

    /**
     * Creates the instance with info from the current call context
     * @param reconAction the recon action
     * @param mapping the mapping configuration
     * @param callingContext The resource call context
     * @param reconParams configuration options for the recon
     */
    public ReconciliationContext(
            ReconciliationService.ReconAction reconAction,
            ObjectMapping mapping,
            ServerContext callingContext,
            JsonValue reconParams,
            JsonValue overridingConfig,
            ReconciliationService service)
        throws BadRequestException {

        this.mapping = mapping;
        this.reconId = callingContext.getId();
        this.reconStat = new ReconciliationStatistic(this);
        this.reconParams = reconParams;
        this.overridingConfig = overridingConfig;
        this.service = service;
        
        reconTypeHandler = createReconTypeHandler(reconAction);
        
        // Initialize the executor for this recon, or null if no executor should be used
        int noOfThreads = mapping.getTaskThreads();
        if (noOfThreads > 0) {
            executor = Executors.newFixedThreadPool(noOfThreads);
        } else {
            executor = null;
        }
    }

    /**
     * Factory method for the recon type handlers
     * @param reconAction the recon action
     * @return the handler appropriate for this recon action type
     */
    private ReconTypeHandler createReconTypeHandler(ReconciliationService.ReconAction reconAction) throws BadRequestException {
        switch (reconAction) {
        case recon :
            return new ReconTypeByQuery(this);
        case reconById :
            return new ReconTypeById(this);
        default:
            throw new BadRequestException("Unknown action " + reconAction.toString());
        }
    }
    
    /**
     * @return A unique identifier for the reconciliation run
     */
    public String getReconId() {
        return reconId;
    }

    /**
     * @return the type of reconciliation
     */
    public ReconTypeHandler getReconHandler() {
        return reconTypeHandler;
    }

    /**
     * @return the reconciliation parameters
     */
    public JsonValue getReconParams() {
        return reconParams;
    }

    /**
     * @return the overriding configuration
     */
    public JsonValue getOverridingConfig() {
        return overridingConfig;
    }

    /**
     * Cancel the reconciliation run.
     * May not take immediate effect in stopping the reconciliation logic.
     */
    public void cancel() {
        stage = ReconStage.ACTIVE_CANCELING;
        canceled = true;
    }

    /**
     * @return Whether the reconciliation run has been canceled.
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Check if a given reconciliation instance has requested to be canceled
     * and throw an exception if it has
     * @throws SynchronizationException if the reconciliation has been aborted
     */
    public void checkCanceled() throws SynchronizationException {
        if (isCanceled()) {
            throw new SynchronizationException("Reconciliation canceled: " + getReconId());
        }
    }

    /**
     * @return Statistics about this reconciliation run
     */
    public ReconciliationStatistic getStatistics() {
        return reconStat;
    }

    /**
     * @return The name of the ObjectMapping associated
     * with the reconciliation run
     */
    public String getMapping() {
        return mapping.getName();
    }

    /**
     * @return The ObjectMapping associated with the reconciliation run
     */
    public ObjectMapping getObjectMapping() {
        return mapping;
    }

    public String getState() {
        return stage.getState();
    }

    public ReconStage getStage() {
        return stage;
    }

    /**
     * @return the populated run progress structure
     */
    public Map<String, Object> getProgress() {
        // Unknown total entries are currently represented via question mark string.
        String totalSourceEntriesStr = (totalSourceEntries == null ? "?" : Integer.toString(totalSourceEntries));
        String totalTargetEntriesStr = (totalTargetEntries == null ? "?" : Integer.toString(totalTargetEntries));

        String totalLinkEntriesStr = "?";
        if (totalLinkEntries == null) {
            if (getStage() == ReconStage.COMPLETED_SUCCESS) {
                totalLinkEntriesStr = Integer.toString(getStatistics().getLinkProcessed());
            }
        } else {
            totalLinkEntriesStr = Integer.toString(totalLinkEntries);
        }

        Map<String, Object> progressDetail = new LinkedHashMap<String, Object>();
        Map<String, Object> sourceDetail = new LinkedHashMap<String, Object>();
        Map<String, Object> sourceExisting = new LinkedHashMap<String, Object>();
        Map<String, Object> targetDetail = new LinkedHashMap<String, Object>();
        Map<String, Object> targetExisting = new LinkedHashMap<String, Object>();
        Map<String, Object> linkDetail = new LinkedHashMap<String, Object>();
        Map<String, Object> linkExisting = new LinkedHashMap<String, Object>();

        sourceExisting.put("processed", getStatistics().getSourceProcessed());
        sourceExisting.put("total", totalSourceEntriesStr);
        sourceDetail.put("existing", sourceExisting);
        progressDetail.put("source", sourceDetail);

        targetExisting.put("processed", getStatistics().getTargetProcessed());
        targetExisting.put("total", totalTargetEntriesStr);
        targetDetail.put("existing", targetExisting);
        targetDetail.put("created", getStatistics().getTargetCreated());
        progressDetail.put("target", targetDetail);

        linkExisting.put("processed", getStatistics().getLinkProcessed());
        linkExisting.put("total", totalLinkEntriesStr);
        linkDetail.put("existing", linkExisting);
        linkDetail.put("created", getStatistics().getLinkCreated());
        progressDetail.put("links", linkDetail);

        return progressDetail;
    }

    /**
     * @return the executor for this recon, or null if no executor should be used
     */
    Executor getExcecutor() {
        return executor;
    }

    /**
     * @return the source ids to reconcile in this recon scope
     * @throws SynchronizationException if getting the ids to reconcile failed
     */
    Iterator<ResultEntry> querySourceIter() throws SynchronizationException {
        ResultIterable result = getReconHandler().querySource();
        setSourceIds(result.getAllIds());
        return result.iterator();
    }

    /**
     * @param sourceIds the list of all source object ids in the reconciliation scope
     */
    void setSourceIds(Collection<String> sourceIds) {
        // Choose a hash based collection as we need fast "contains" handling
        this.sourceIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        this.sourceIds.addAll(sourceIds);
        this.totalSourceEntries = Integer.valueOf(sourceIds.size());
    }

    /**
     * @param targetIds the list of all ids in the target object set
     * If the target system IDs are case insensitive, the ids are kept in normalized (lower case) form
     */
    void setTargetIds(Collection<String> targetIds) {
        // Choose a hash based collection as we need fast "contains" handling
        this.targetIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        if (targetIds != null) {
            this.targetIds.addAll(targetIds);
        }
        this.totalTargetEntries = Integer.valueOf(targetIds.size());
    }

    /**
     * Set all pre-fetched links
     * Since pre-fetching all links is optional, links may be gotten individually rather than
     * this getting set.
     * @param allLinks the list of all links for a given mapping
     */
    void setAllLinks(Map<String, Link> allLinks) {
        if (allLinks != null) {
            this.totalLinkEntries = Integer.valueOf(allLinks.size());
        }
    }

    /**
     * @return the list of all source object ids in the reconciliation scope,
     * queried at the outset of reconciliation.
     * Null if no bulk source id query was done.
     */
    public Set<String> getSourceIds() {
        return sourceIds;
    }

    /**
     * @return the list of all ids in the target object set,
     * queried at the outset of reconciliation.
     * Null if no bulk target id query was done.
     */
    public Set<String> getTargetIds() {
        return targetIds;
    }

    /**
     * @param newStage Sets the current state and stage in the reconciliation process
     */
    public void setStage(ReconStage newStage) {
        // If there is already a stage in progress, end it first
        if (this.stage != ReconStage.ACTIVE_INITIALIZED) {
            reconStat.endStage(this.stage);
        }
        if (canceled) {
            if (newStage.isComplete()) {
                this.stage = ReconStage.COMPLETED_CANCELED;
            } else {
                this.stage = ReconStage.ACTIVE_CANCELING;
            }
        } else {
            this.stage = newStage;
        }
        if (newStage.isComplete()) {
            cleanupState();
        } else {
            reconStat.startStage(newStage);
        }
    }

    /**
     * Remove any state from memory that should not be kept
     * past the completion of the reconciliation run
     */
    private synchronized void cleanupState() {
        sourceIds = null;
        targetIds = null;
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    /**
     * Returns a summary of the reconciliation run.
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> reconSummary = new LinkedHashMap<String, Object>();
        reconSummary.put("_id", getReconId());
        reconSummary.put("mapping", getMapping());
        reconSummary.put("state", getState());
        reconSummary.put("stage", getStage().toString());
        reconSummary.put("stageDescription", getStage().getDescription());
        reconSummary.put("progress", getProgress());
        reconSummary.put("situationSummary", getStatistics().getSituationSummary());
        reconSummary.put("statusSummary", getStatistics().getStatusSummary());
        reconSummary.put("started", getStatistics().getStarted());
        reconSummary.put("ended", getStatistics().getEnded());
        reconSummary.put("duration", getStatistics().getDuration());
        return reconSummary;
    }

    /**
     * Accessor to service wrapping and registering the reconciliation capabilities
     * @return handle to recon service
     */
    ReconciliationService getService() {
        return service;
    }
}
