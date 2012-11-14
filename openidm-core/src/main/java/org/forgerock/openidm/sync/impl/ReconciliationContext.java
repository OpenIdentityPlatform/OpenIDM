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
*
*/
package org.forgerock.openidm.sync.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.forgerock.json.fluent.JsonValue;

/**
 * Represents the information and functionality for a 
 * reconciliation run
 * 
 * @author aegloff
 */
public class ReconciliationContext {

    ObjectMapping mapping;

    private ReconStage stage = ReconStage.ACTIVE_INITIALIZED;
    private boolean canceled = false;
    private String reconId; 
    private final ReconciliationStatistic reconStat;
    
    // If set, the list of all queried source Ids
    private Set<String> sourceIds; 
    // If set, the list of all queried target Ids
    private Set<String> targetIds;
    
    private Integer totalSourceEntries;
    private Integer totalTargetEntries;
    private Integer totalLinkEntries;

    /**
     * Creates the instance with info from the current call context
     * @param callingContext The resource call context
     */
    public ReconciliationContext(ObjectMapping mapping, JsonValue callingContext) {
        this.mapping = mapping;
        this.reconId = callingContext.get("uuid").required().asString();
        this.reconStat = new ReconciliationStatistic(this);
    }
    
    /**
     * @return A unique identifier for the reconciliation run
     */
    public String getReconId() {
        return reconId;
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
        
        Map<String, Object> progress = new LinkedHashMap<String, Object>();
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

        progress.put("progress", progressDetail);

        return progress;
    }
    
    /**
     * @return the executor for this recon, or null if no executor should be used
     */
    Executor getExcecutor() {
        int noOfThreads = mapping.getTaskThreads();
        if (noOfThreads > 0) {
            return Executors.newFixedThreadPool(noOfThreads);
        } else {
            return null;
        }
    }

    /**
     * @param sourceIds the list of all ids in the source object set
     */
    void setSourceIds(List<String> sourceIds) {
        // Choose a hash based collection as we need fast "contains" handling
        this.sourceIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        this.sourceIds.addAll(sourceIds);
        this.totalSourceEntries = Integer.valueOf(sourceIds.size());
    }
    
    /**
     * @param targetIds the list of all ids in the target object set
     */
    void setTargetIds(List<String> targetIds) {
        // Choose a hash based collection as we need fast "contains" handling
        this.targetIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        this.targetIds.addAll(targetIds);
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
     * @return the list of all ids in the source object set, 
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
     * @param stage Sets the current state and stage in the reconciliation process
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
    private void cleanupState() {
        sourceIds = null;
        targetIds = null;
    }

}
