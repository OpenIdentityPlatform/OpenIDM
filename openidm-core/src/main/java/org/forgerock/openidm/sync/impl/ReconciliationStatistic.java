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

import java.io.ObjectInputStream.GetField;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.forgerock.openidm.util.DateUtil;

/**
 * Statistic for a reconciliation run
 * 
 * @author aegloff
 */
public class ReconciliationStatistic {
    
    static DateUtil dateUtil = DateUtil.getDateUtil("UTC");
    
    private ReconciliationContext reconContext;    
    private long startTime;
    private long endTime;
    
    private long sourceQueryStartTime;
    private long sourceQueryEndTime;
    private long targetQueryStartTime;
    private long targetQueryEndTime;
    private long linkQueryStartTime;
    private long linkQueryEndTime;
    
    private volatile int sourceProcessed;
    private volatile int linkProcessed;
    private volatile int linkCreated;
    private volatile int targetProcessed;
    private volatile int targetCreated;
    
    private PhaseStatistic sourceStat;
    private PhaseStatistic targetStat;
    
    private Map<ReconStage, Map> stageStat = new HashMap<ReconStage, Map>();
    
    public ReconciliationStatistic(ReconciliationContext reconContext) {
        this.reconContext = reconContext;
        sourceStat = new PhaseStatistic(this, PhaseStatistic.Phase.SOURCE);
        targetStat = new PhaseStatistic(this, PhaseStatistic.Phase.TARGET);
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
        Map stageEntry = new HashMap();
        stageEntry.put("startTime", Long.valueOf(System.currentTimeMillis()));
        stageStat.put(stage, stageEntry);
    }
    
    public void endStage(ReconStage stage) {
        Map stageEntry = stageStat.get(stage);
        if (stageEntry != null) {
            stageEntry.put("endTime", Long.valueOf(System.currentTimeMillis()));
        }
    }

    public void sourceQueryStart() {
        sourceQueryStartTime = System.currentTimeMillis();
    }
    
    public void sourceQueryEnd() {
        sourceQueryEndTime = System.currentTimeMillis();
    }
    
    public void targetQueryStart() {
        targetQueryStartTime = System.currentTimeMillis();
    }
    
    public void targetQueryEnd() {
        targetQueryEndTime = System.currentTimeMillis();
    }
    
    public void linkQueryStart() {
        linkQueryStartTime = System.currentTimeMillis();
    }
    
    public void linkQueryEnd() {
        linkQueryEndTime = System.currentTimeMillis();
    }

    
    public void processed(String sourceId, String targetId, boolean linkExisted, String linkId, Situation situation, Action action) {
        if (sourceId != null) {
            ++sourceProcessed;
        }
        
        if (targetId != null) {
            if (Action.CREATE.equals(action)) {
                ++targetCreated;
            } else {
                ++targetProcessed;
            }
        }
        if (linkId != null) {
            if (linkExisted) {
                ++linkProcessed;
            } else {
                ++linkCreated;
            }
        }
    }
    
    /**
     * @return The number of existing source objects processed
     */
    public int getSourceProcessed() {
        return sourceProcessed;
    }
    
    /**
     * @return The number of existing target objects processed
     */
    public int getTargetProcessed() {
        return targetProcessed;
    }
    
    /**
     * @return The number of new target objects created/processed
     */
    public int getTargetCreated() {
        return targetCreated;
    }
    
    /**
     * @return The number of existing links processed
     */
    public int getLinkProcessed() {
        return linkProcessed;
    }
    
    /**
     * @return The number of new links created/processed
     */
    public int getLinkCreated() {
        return linkCreated;
    }
    
    /**
     * @return The reconciliation start time, formatted
     */
    public String getStarted() {
        String startFormatted = "";
        if (startTime > 0) {
            startFormatted = dateUtil.formatDateTime(new Date(startTime));
        } 
        return startFormatted;
    }
    
    /**
     * @return The reconciliation end time, formatted, or empty string if not ended
     */
    public String getEnded() {
        String endFormatted = "";
        if (endTime > 0) {
            endFormatted = dateUtil.formatDateTime(new Date(endTime));
        } 
        return endFormatted;
    }
    
    public Map<String, Object> asMap() {
        Map<String, Object> results = new HashMap();

        results.put("startTime", getStarted());
        results.put("endTime", getEnded());
        results.put("duration", endTime - startTime);
        results.put("reconId", reconContext.getReconId());
        // TODO: what is this name?
        //results.put("reconName", reconContext.getName());
        results.put("mappingName", reconContext.getMapping());

        return results;
    }
    
    public String simpleSummary() {
        Map<String, Integer> simpleSummary = new HashMap<String, Integer>();
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
}
