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
 * Portions copyright 2012-2017 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import static org.forgerock.openidm.util.DurationStatistics.nanoToMillis;
import static org.forgerock.util.Reject.checkNotNull;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.guava.common.base.Optional;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.util.DurationStatistics;

/**
 * Statistic for a reconciliation run
 * 
 */
public class ReconciliationStatistic {
    
    private static final DateUtil dateUtil = DateUtil.getDateUtil(ServerConstants.TIME_ZONE_UTC);

    public enum DurationMetric {
        activePolicyPostActionScript,
        activePolicyScript,
        auditLog,
        correlationQuery,
        correlationScript,
        defaultMappingScript,
        deleteLinkObject,
        deleteTargetObject,
        linkQualifiersScript,
        linkQuery,
        onCreateScript,
        onDeleteScript,
        onLinkScript,
        onReconScript,
        onUnlinkScript,
        onUpdateScript,
        postMappingScript,
        propertyMappingScript,
        resultScript,
        sourceLinkQuery,
        sourceObjectQuery,
        sourcePhase,
        sourcePagePhase,
        sourceQuery,
        targetLinkQuery,
        targetObjectQuery,
        targetPhase,
        targetQuery,
        updateTargetObject,
        validSourceScript,
        validTargetScript
    }
    
    private final ReconciliationContext reconContext;
    private long startTime;
    private long endTime;
    
    long linkQueryStartTime;
    long linkQueryEndTime;
    
    private final AtomicInteger sourceProcessed;
    private final AtomicInteger linkProcessed;
    private final AtomicInteger linkCreated;
    private final AtomicInteger targetProcessed;
    private final AtomicInteger targetCreated;
    private final Map<Status, AtomicInteger> statusProcessed;

    private final PhaseStatistic sourceStat;
    private final PhaseStatistic targetStat;
    
    private final Map<ReconStage, Map<String, Object>> stageStat;
    private final ConcurrentHashMap<String, DurationStatistics> durationStat;

    public ReconciliationStatistic(ReconciliationContext reconContext) {
        this.reconContext = reconContext;
        sourceStat = new PhaseStatistic(this, PhaseStatistic.Phase.SOURCE, reconContext.getObjectMapping().getSourceObjectSet());
        targetStat = new PhaseStatistic(this, PhaseStatistic.Phase.TARGET, reconContext.getObjectMapping().getTargetObjectSet());
        stageStat = new ConcurrentHashMap<>();
        durationStat = new ConcurrentHashMap<>();
        sourceProcessed = new AtomicInteger();
        linkProcessed = new AtomicInteger();
        linkCreated = new AtomicInteger();
        targetProcessed = new AtomicInteger();
        targetCreated = new AtomicInteger();
        statusProcessed = new EnumMap<>(Status.class);

        for (Status status : Status.values()) {
            statusProcessed.put(status, new AtomicInteger());
        }
    }

    /**
     * Consumed by the ReconciliationStatisticsPersistence when creating aggregated statics for a clustered recon run.
     * @param instances The ReconciliationStatistic instances constituted from repo-persisted state corresponding to each
     *                  source page and the target phase of a clustered recon run. Note that this Collection could be empty
     *                  if e.g. the logging corresponding to a failure on another node already cleared the persisted
     *                  ReconciliationStatistic instances for this recon run.
     * @return an {@code Optional<ReconciliationStatistic>} instance which encapsulated state aggregated from the members of the instances array
     */
    public static Optional<ReconciliationStatistic> getAggregatedInstance(Collection<ReconciliationStatistic> instances) {
        if ((instances == null) || instances.size() == 0) {
            return Optional.absent();
        }
        final ReconciliationStatistic aggregator = new ReconciliationStatistic(instances.iterator().next().reconContext);
        for (ReconciliationStatistic instance : instances) {
            aggregator.sourceProcessed.addAndGet(instance.sourceProcessed.get());
            aggregator.linkProcessed.addAndGet(instance.linkProcessed.get());
            aggregator.linkCreated.addAndGet(instance.linkCreated.get());
            aggregator.targetProcessed.addAndGet(instance.targetProcessed.get());
            aggregator.targetCreated.addAndGet(instance.targetCreated.get());
            if ((aggregator.startTime == 0) || (aggregator.startTime > instance.startTime)) {
                aggregator.startTime = instance.startTime;
            }
            if (aggregator.endTime < instance.endTime) {
                aggregator.endTime = instance.endTime;
            }
            for (Status status : Status.values()) {
                aggregator.statusProcessed.put(status,
                        new AtomicInteger(aggregator.statusProcessed.get(status).intValue() +
                                instance.statusProcessed.get(status).intValue()));
            }
            aggregator.sourceStat.aggregateValues(instance.getSourceStat());
            aggregator.targetStat.aggregateValues(instance.getTargetStat());
        }
        /** TODO: until OPENIDM-6776 is implemented, where ReconciliationStatistics are persisted in the repo,
         * it is possible that clustered recon will be started, and finished, on different nodes. The node which
         * finishes clustered recon always logs the aggregated ReconciliationStatistics for that node, so the
         * endTime will always be set. It is possible, however, that the startTime is still 0, if the current
         * clustered recon run was not started on this node (as stats are only aggregated per node, pending
         * repo persistence in OPENIDM-6776). A recon start-time of 0 confuses the UI, so if the value has not
         * been set, set it to an arbitrary value so that the UI displays correctly.
         */
        if (aggregator.startTime == 0) {
            aggregator.startTime = aggregator.endTime - (1000 * 60 * 5);
        }
        // Note that the linkQueryStartTime and linkQueryEndTime are not set, as links are not prefetched for clustered recon
        // as the link prefetch cannot be segmented to a set of source ids. Possible future enhancement.
        return Optional.of(aggregator);
    }

    /**
     * For the given {@code key}, calculates and records the time-duration, since
     * {@link DurationStatistics#startNanoTime() startNanoTime} occurred in the current thread.
     * <p>
     * Calling thread must be the same thread used to call {@link DurationStatistics#startNanoTime()}.
     *
     * @param metric Metric for statistic
     * @param startNanoTime Start-time, in nanoseconds
     */
    public void addDuration(final DurationMetric metric, final long startNanoTime) {
        DurationStatistics entry = durationStat.get(checkNotNull(metric).name());
        if (entry == null) {
            entry = new DurationStatistics();
            final DurationStatistics existing = durationStat.putIfAbsent(metric.name(), entry);
            if (existing != null) {
                entry = existing;
            }
        }
        entry.stopNanoTime(startNanoTime);
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
        Map<String, Object> stageEntry = new ConcurrentHashMap<>();
        stageEntry.put("startTime", System.currentTimeMillis());
        stageStat.put(stage, stageEntry);
    }
    
    public void endStage(ReconStage stage) {
        Map<String, Object> stageEntry = stageStat.get(stage);
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

    /**
     * @return The reconciliation start time, formatted
     */
    public String getStarted() {
        return dateUtil.getFormattedTime(startTime);
    }
    
    /**
     * @return The reconciliation end time, formatted, or empty string if not ended
     */
    public String getEnded() {
        return dateUtil.getFormattedTime(endTime);
    }

    public boolean hasEnded() {
        if (endTime == 0) {
            return false;
        }
        return true;
    }
    
    public Map<String, Object> asMap() {
        Map<String, Object> results = new HashMap<>();

        results.put("startTime", getStarted());
        results.put("endTime", getEnded());
        results.put("duration", getDuration());
        results.put("reconId", reconContext.getReconId());
        results.put("mappingName", reconContext.getMapping());

        return results;
    }
    
    public String simpleSummary() {
        Map<String, Integer> simpleSummary = new HashMap<>();
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
        Map<String, Integer> situationSummary = new HashMap<>();
        getSourceStat().updateSummary(situationSummary);
        getTargetStat().updateSummary(situationSummary);
        return situationSummary;
    }

    public Map<String, Integer> getStatusSummary() {
        Map<String, Integer> statusSummary = new HashMap<>();
        for (Map.Entry<Status, AtomicInteger> entry : statusProcessed.entrySet()) {
            statusSummary.put(entry.getKey().toString(), entry.getValue().intValue());
        }
        return statusSummary;
    }

    /**
     * Exposes current duration statistics, gathered from calls to {@link #addDuration(DurationMetric, long)}.
     *
     * @return Map of duration statistics
     */
    public Map<String, Map<String, Long>> getDurationSummary() {
        final Map<String, Map<String, Long>> resultMap = new HashMap<>(durationStat.size() * 2);
        for (final Entry<String, DurationStatistics> entry : durationStat.entrySet()) {
            final Map<String, Long> valueMap = new HashMap<>();
            final DurationStatistics stats = entry.getValue();

            // normalize mean, which is an approximation, to never be lower than min
            final long min = nanoToMillis(stats.min());
            final long mean = Math.max(nanoToMillis(stats.mean()), min);

            valueMap.put("count", stats.count());
            valueMap.put("sum", nanoToMillis(stats.sum()));
            valueMap.put("min", min);
            valueMap.put("max", nanoToMillis(stats.max()));
            valueMap.put("mean", mean);
            valueMap.put("stdDev", nanoToMillis(stats.stdDev()));
            resultMap.put(entry.getKey(), valueMap);
        }
        return resultMap;
    }
}
