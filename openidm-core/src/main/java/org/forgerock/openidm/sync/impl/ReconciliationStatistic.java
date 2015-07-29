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

import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.util.DateUtil;

/**
 * Statistic for a reconciliation run
 * 
 */
public class ReconciliationStatistic {
    
    static DateUtil dateUtil = DateUtil.getDateUtil("UTC");
    
    private ReconciliationContext reconContext;    
    private long startTime;
    private long endTime;
    
    long linkQueryStartTime;
    long linkQueryEndTime;
    
    private AtomicInteger sourceProcessed = new AtomicInteger();
    private AtomicInteger linkProcessed = new AtomicInteger();
    private AtomicInteger linkCreated = new AtomicInteger();
    private AtomicInteger targetProcessed = new AtomicInteger();
    private AtomicInteger targetCreated = new AtomicInteger();
    private Map<Status, AtomicInteger> statusProcessed = new EnumMap<Status, AtomicInteger>(Status.class);

    private PhaseStatistic sourceStat;
    private PhaseStatistic targetStat;
    
    private Map<ReconStage, Map> stageStat = new ConcurrentHashMap<ReconStage, Map>();

    public ReconciliationStatistic(ReconciliationContext reconContext) {
        this.reconContext = reconContext;
        sourceStat = new PhaseStatistic(this, PhaseStatistic.Phase.SOURCE, reconContext.getObjectMapping().getSourceObjectSet());
        targetStat = new PhaseStatistic(this, PhaseStatistic.Phase.TARGET, reconContext.getObjectMapping().getTargetObjectSet());
        for (Status status : Status.values()) {
            statusProcessed.put(status, new AtomicInteger());
        }
    }
    
    public PhaseStatistic getSourceStat() {
        return sourceStat;
    }
    
    public PhaseStatistic getTargetStat() {
        return targetStat;
    }
    
    public void reconStart() {
        startTime = System.currentTimeMillis();
    }
    
    public void reconEnd() {
        endTime = System.currentTimeMillis();
    }
    
    public void startStage(ReconStage stage) {
        Map stageEntry = new ConcurrentHashMap();
        stageEntry.put("startTime", System.currentTimeMillis());
        stageStat.put(stage, stageEntry);
    }
    
    public void endStage(ReconStage stage) {
        Map stageEntry = stageStat.get(stage);
        if (stageEntry != null) {
            stageEntry.put("endTime", System.currentTimeMillis());
        }
    }

    public void sourceQueryStart() {
        sourceStat.queryStartTime = System.currentTimeMillis();
    }
    
    public void sourceQueryEnd() {
        sourceStat.queryEndTime = System.currentTimeMillis();
    }
    
    public void targetQueryStart() {
        targetStat.queryStartTime = System.currentTimeMillis();
    }
    
    public void targetQueryEnd() {
        targetStat.queryEndTime = System.currentTimeMillis();
    }
    
    public void linkQueryStart() {
        linkQueryStartTime = System.currentTimeMillis();
    }
    
    public void linkQueryEnd() {
        linkQueryEndTime = System.currentTimeMillis();
    }

    public void sourcePhaseStart() {
        sourceStat.phaseStartTime = System.currentTimeMillis();
    }

    public void sourcePhaseEnd() {
        sourceStat.phaseEndTime = System.currentTimeMillis();
    }

    public void targetPhaseStart() {
        targetStat.phaseStartTime = System.currentTimeMillis();
    }

    public void targetPhaseEnd() {
        targetStat.phaseEndTime = System.currentTimeMillis();
    }

    /**
     * Handle the processed notification to update the statistics appropriately
     * @param sourceId The source id processed, or null if none
     * @param targetId The target id processed, or null if none
     * @param linkExisted indication if the link existed before the operation
     * @param linkId the link identifier, if available. For created links this may not currently be available.
     * @param linkWasCreated indication if a new link was created during the operation
     * @param situation the assessed situation
     * @param action the action that was processed
     */
    public void processed(String sourceId, String targetId, boolean linkExisted, String linkId, boolean linkWasCreated,
            Situation situation, ReconAction action) {
        if (sourceId != null) {
            sourceProcessed.incrementAndGet();
        }
        
        if (targetId != null) {
            if (ReconAction.CREATE.equals(action)) {
                targetCreated.incrementAndGet();
            } else {
                targetProcessed.incrementAndGet();
            }
        }
        if (linkExisted) {
            linkProcessed.incrementAndGet();
        }
        if (linkWasCreated) {
            linkCreated.incrementAndGet();
        }
    }

    public void processStatus(Status status) {
        statusProcessed.get(status).incrementAndGet();
    }

    /**
     * @return The number of existing source objects processed
     */
    public int getSourceProcessed() {
        return sourceProcessed.get();
    }
    
    /**
     * @return The number of existing target objects processed
     */
    public int getTargetProcessed() {
        return targetProcessed.get();
    }
    
    /**
     * @return The number of new target objects created/processed
     */
    public int getTargetCreated() {
        return targetCreated.get();
    }
    
    /**
     * @return The number of existing links processed
     */
    public int getLinkProcessed() {
        return linkProcessed.get();
    }
    
    /**
     * @return The number of new links created/processed
     */
    public int getLinkCreated() {
        return linkCreated.get();
    }

    /**
     * @return the duration, in millisconds, that the reconciliation took, or the duration of the current run
     */
    public long getDuration() {
        return getDuration(this.startTime, hasEnded() ? this.endTime : System.currentTimeMillis());
    }

    /**
     * @return the duration, in milliseconds, of an event defined by a {@code startTime} and an {@code endTime}
     *
     * @param startTime the start time
     * @param endTime the end time
     * @return the difference, in milliseconds; -1 if on endpoint is non-positve
     */
    public long getDuration(long startTime, long endTime) {
        return (startTime > 0 && endTime > 0)
                ? endTime - startTime
                : -1;
    }

    public String getFormattedTime(long epoch) {
        String startFormatted = "";
        if (epoch > 0) {
            startFormatted = dateUtil.formatDateTime(new Date(epoch));
        }
        return startFormatted;
    }

    /**
     * @return The reconciliation start time, formatted
     */
    public String getStarted() {
        return getFormattedTime(startTime);
    }
    
    /**
     * @return The reconciliation end time, formatted, or empty string if not ended
     */
    public String getEnded() {
        return getFormattedTime(endTime);
    }

    public boolean hasEnded() {
        if (endTime == 0) {
            return false;
        }
        return true;
    }
    
    public Map<String, Object> asMap() {
        Map<String, Object> results = new HashMap<String, Object>();

        results.put("startTime", getStarted());
        results.put("endTime", getEnded());
        results.put("duration", getDuration());
        results.put("reconId", reconContext.getReconId());
        results.put("mappingName", reconContext.getMapping());

        return results;
    }
    
    public String simpleSummary() {
        Map<String, Integer> simpleSummary = new ConcurrentHashMap<String, Integer>();
        getSourceStat().updateSummary(simpleSummary);
        getTargetStat().updateSummary(simpleSummary);
        
        StringBuilder sb = new StringBuilder();
        for (Entry<String, Integer> e : simpleSummary.entrySet()) {
            sb.append(e.getKey());
            sb.append(": ");
            sb.append(e.getValue());
            sb.append(" ");
        }
        return sb.toString();
    }
    
    public Map<String, Integer> getSituationSummary() {
        Map<String, Integer> situationSummary = new ConcurrentHashMap<String, Integer>();
        getSourceStat().updateSummary(situationSummary);
        getTargetStat().updateSummary(situationSummary);
        return situationSummary;
    }

    public Map<String, Integer> getStatusSummary() {
        Map<String, Integer> statusSummary = new ConcurrentHashMap<String, Integer>();
        for (Map.Entry<Status, AtomicInteger> entry : statusProcessed.entrySet()) {
            statusSummary.put(entry.getKey().toString(), entry.getValue().intValue());
        }
        return statusSummary;
    }
}
