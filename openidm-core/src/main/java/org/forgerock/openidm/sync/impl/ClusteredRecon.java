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
 * Copyright 2016-2017 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl;

import static org.forgerock.openidm.sync.impl.ObjectMapping.EVENT_RECON_SOURCE_PAGE;
import static org.forgerock.openidm.sync.impl.ObjectMapping.EVENT_RECON_TARGET;
import static org.forgerock.openidm.sync.impl.ObjectMapping.startNanoTime;

import java.util.Collection;
import java.util.Map;

import org.forgerock.guava.common.base.Optional;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.TriggerContext;
import org.forgerock.openidm.sync.impl.cluster.ClusteredReconJobDispatch;
import org.forgerock.openidm.sync.impl.cluster.ClusteredSourcePhaseTargetIdRegistry;
import org.forgerock.openidm.sync.impl.cluster.ReconciliationStatisticsPersistence;
import org.forgerock.services.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encapsulates the logic to execute the constituents of a clustered recon. It is instantiated and invoked
 * from ObjectMapping#recon when clustered recon is configured.
 */
class ClusteredRecon {
    private static final Logger logger = LoggerFactory.getLogger(ClusteredRecon.class);
    private final ObjectMapping objectMapping;
    private final ReconciliationContext reconContext;
    private final ClusteredSourcePhaseTargetIdRegistry targetIdRegistry;
    private final ClusteredReconJobDispatch schedulerDispatch;
    private final ReconciliationStatisticsPersistence reconStatsPersistence;

    ClusteredRecon(ObjectMapping objectMapping,
                   ReconciliationContext reconContext,
                   ClusteredSourcePhaseTargetIdRegistry clusteredSourcePhaseTargetIdRegistry,
                   ClusteredReconJobDispatch clusteredReconJobDispatch,
                   ReconciliationStatisticsPersistence reconciliationStatisticsPersistence) {
        this.objectMapping = objectMapping;
        this.reconContext = reconContext;
        this.targetIdRegistry = clusteredSourcePhaseTargetIdRegistry;
        this.schedulerDispatch = clusteredReconJobDispatch;
        this.reconStatsPersistence = reconciliationStatisticsPersistence;
    }

    void dispatchClusteredRecon() throws SynchronizationException {
        addAuditTriggerContext();
        final String subActionParam = reconContext.getReconParams().get(ClusteredReconJobDispatch.CLUSTERED_SUB_ACTION_KEY).asString();
        //TODO: see if specifying 'recon initiated' state for all recon invocations would provide a means to have actual state
        //correspond to the initiation of clustered recon, rather than the absence of any state, as is currently the case.
        try {
            if (subActionParam == null) {
                initiateClusteredRecon();
            } else {
                switch (subActionParam) {
                    case ClusteredReconJobDispatch.CLUSTERED_NEXT_PAGE_ACTION_PARAM:
                        clusteredReconNextSourcePage();
                        break;
                    case ClusteredReconJobDispatch.CLUSTERED_SOURCE_COMPLETION_PARAM:
                        clusteredReconSourceCompletionCheck();
                        break;
                    case ClusteredReconJobDispatch.CLUSTERED_TARGET_PHASE_PARAM:
                        clusteredReconTargetPhase();
                        break;
                    default:
                        throw new SynchronizationException("Illegal " + ClusteredReconJobDispatch.CLUSTERED_SUB_ACTION_KEY
                                + " key specified in invocationContext of scheduled recon: " + subActionParam);

                }
            }
        } catch (InterruptedException ex) {
            SynchronizationException syncException;
            if (reconContext.isCanceled()) {
                reconContext.setStage(ReconStage.COMPLETED_CANCELED);
                syncException = new SynchronizationException("Reconciliation canceled: " + reconContext.getReconId());
            }
            else {
                reconContext.setStage(ReconStage.COMPLETED_FAILED);
                syncException = new SynchronizationException("Interrupted execution of reconciliation", ex);
            }
            reconContext.getStatistics().reconEnd();
            persistAndAggregateReconStats();
            objectMapping.doResults(reconContext, ObjectSetContext.get());
            reconStatsPersistence.deletePersistedInstances(reconContext.getReconId());
            targetIdRegistry.deletePersistedTargetIds(reconContext.getReconId());
            logger.error("ClusteredRecon interrupted: " + ex.getMessage(), ex);
            throw syncException;
        } catch (SynchronizationException e) {
            // Make sure that the error did not occur within doResults or last logging for completed success case
            reconContext.setStage(ReconStage.COMPLETED_FAILED);
            if ( reconContext.getStage() != ReconStage.ACTIVE_PROCESSING_RESULTS
                    && reconContext.getStage() != ReconStage.COMPLETED_SUCCESS ) {
                objectMapping.doResults(reconContext, ObjectSetContext.get());
            }
            reconContext.getStatistics().reconEnd();
            persistAndAggregateReconStats();
            objectMapping.logReconEndFailure(reconContext, ObjectSetContext.get());
            reconStatsPersistence.deletePersistedInstances(reconContext.getReconId());
            targetIdRegistry.deletePersistedTargetIds(reconContext.getReconId());
            logger.error("ClusteredRecon failed: " + e.getMessage(), e);
            throw new SynchronizationException("Synchronization failed", e);
        } catch (Exception e) {
            reconContext.setStage(ReconStage.COMPLETED_FAILED);
            objectMapping.doResults(reconContext, ObjectSetContext.get());
            reconContext.getStatistics().reconEnd();
            persistAndAggregateReconStats();
            objectMapping.logReconEndFailure(reconContext, ObjectSetContext.get());
            reconStatsPersistence.deletePersistedInstances(reconContext.getReconId());
            targetIdRegistry.deletePersistedTargetIds(reconContext.getReconId());
            logger.error("ClusteredRecon failed: " + e.getMessage(), e);
            throw new SynchronizationException("Synchronization failed", e);
        } finally {
            ObjectSetContext.pop(); // pop the TriggerContext
        }
    }

    private void initiateClusteredRecon() throws SynchronizationException, InterruptedException {
        logger.debug("Initiating clustered recon for recon id {}", reconContext.getReconId());
        reconContext.getStatistics().reconStart();
        final Context initialContext = ObjectSetContext.get();
        objectMapping.executeOnRecon(initialContext, reconContext);
        objectMapping.logReconStart(reconContext, ObjectSetContext.get());
        final ReconQueryResult queryResult = querySourcePage(null);
        reconSourcePageCommonActions(queryResult);
    }

    /**
     * The existing doRecon method will push a TriggerContext to the ObjectSetContext after executeOnRecon is called,
     * and pop this context in a finally block after all recon phases complete.
     */
    private void addAuditTriggerContext() {
        ObjectSetContext.push(new TriggerContext(ObjectSetContext.get(), "recon"));
    }

    /**
     * Called in the course of clustered recon to recon a specific page.
     * @throws SynchronizationException
     */
    private void clusteredReconNextSourcePage() throws SynchronizationException, InterruptedException {
        final String pagingCookie =
                reconContext.getReconParams().get(ClusteredReconJobDispatch.PAGING_COOKIE_KEY).asString();
        if (pagingCookie == null) {
            throw new SynchronizationException("Illegal state invoking clusteredReconNextSourcePage: paging cookie is null!");
        }
        logger.debug("Performing clustered source page recon for recon {} and paging cookie {}",
                reconContext.getReconId(), pagingCookie);
        final ReconQueryResult queryResult = querySourcePage(pagingCookie);
        reconSourcePageCommonActions(queryResult);
    }

    /**
     * Encapsulates functionality common to reconing a source page. Called when clustered recon is initiated, and in
     * the course of reconciling each subsequent source page. Examines the query for correctness, and if correct,
     * 1. schedules the next source page if paging cookie returned from the query is not null
     * 2. reconciles the current source page
     * 3. performs the completion actions for a given source page
     * @param queryResult the results of the query for the paging cookie corresponding to the current source recon page
     * @throws SynchronizationException thrown if the next page cannot be scheduled, if the source page recon encounters
     * an error, or if the completion actions throw a SynchronizationException
     * @throws InterruptedException thrown if the source page reconciliation is interrupted
     */
    private void reconSourcePageCommonActions(ReconQueryResult queryResult) throws SynchronizationException, InterruptedException  {
        if (validateSourceQueryResults(queryResult)) {
            optionallyScheduleNextSourcePage(queryResult.getPagingCookie(), reconContext.getReconId());
            reconcileSourcePage(queryResult);
            sourcePageCompletionActions(queryResult.getPagingCookie());
        }
    }

    private ReconQueryResult querySourcePage(String pagingCookie) throws SynchronizationException {
        reconContext.setStage(ReconStage.ACTIVE_QUERY_SOURCE_PAGE);
        final ReconciliationStatistic reconStats = reconContext.getStatistics();
        reconStats.sourceQueryStart();
        final long sourceQueryStart = startNanoTime(reconContext);
        final ReconQueryResult sourceQueryResult = reconContext.querySourceIter(objectMapping.getReconSourceQueryPageSize(), pagingCookie);
        reconStats.addDuration(ReconciliationStatistic.DurationMetric.sourceQuery, sourceQueryStart);
        reconStats.sourceQueryEnd();
        return sourceQueryResult;
    }

    private boolean validateSourceQueryResults(ReconQueryResult reconQueryResult) throws SynchronizationException{
        if (!reconQueryResult.getIterator().hasNext()) {
            if (!reconContext.getReconHandler().allowEmptySourceSet()) {
                logger.warn("Cannot reconcile from an empty data source, unless allowEmptySourceSet is true.");
                reconContext.setStage(ReconStage.COMPLETED_FAILED);
                reconContext.getStatistics().reconEnd();
                persistAndAggregateReconStats();
                objectMapping.logReconEndFailure(reconContext, ObjectSetContext.get());
                return false;
            }
        }
        return true;
    }

    private void optionallyScheduleNextSourcePage(String pagingCookie, String reconId) throws SynchronizationException {
        if (pagingCookie != null) {
            schedulerDispatch.dispatchSourcePageRecon(reconId, objectMapping.getName(), pagingCookie);
        }
    }

    /**
     * After a source page reconciliation is complete, this method will be called to schedule the next action. If the
     * paging cookie was not null, then the next source page was scheduled prior to the current source page was reconciled.
     * If the paging cookie is null, and the target phase is to be run, then the source phase completion task must be
     * scheduled. If no target phase is configured, then the successful recon completion tasks must be undertaken. If there
     * are additional pages to be run, then the statistics for this page must be persisted.
     * @param pagingCookie the cookie returned from the source page query
     * @throws SynchronizationException if any actions could not be completed.
     */
    private void sourcePageCompletionActions(String pagingCookie) throws SynchronizationException {
        if (pagingCookie == null) {
            if (reconContext.getReconHandler().isRunTargetPhase()) {
                schedulerDispatch.dispatchSourcePhaseCompletionTask(reconContext.getReconId(), objectMapping.getName());
                reconStatsPersistence.persistInstance(reconContext.getReconId(), reconContext.getStatistics()); ;
            } else {
                doSuccessfulReconCompletionTasks();
            }
        } else {
            reconStatsPersistence.persistInstance(reconContext.getReconId(), reconContext.getStatistics());
        }
    }

    private void reconcileSourcePage(ReconQueryResult reconQueryResult)
            throws SynchronizationException, InterruptedException {
        // Links will not be pre-fetched as only a single page is being reconciled.
        // In the future, it might make sense to prefetch the links corresponding to the source ids.
        Map<String, Map<String, Link>> allLinks = null;

        EventEntry measureSource = Publisher.start(EVENT_RECON_SOURCE_PAGE, reconContext.getReconId(), null);
        reconContext.setStage(ReconStage.ACTIVE_RECONCILING_SOURCE_PAGE);
        final ReconciliationStatistic reconStats = reconContext.getStatistics();
        reconStats.sourcePhaseStart();
        final long sourcePhasePageStart = startNanoTime(reconContext);

        logger.debug("Performing clustered source sync for recon {} on mapping {}", reconContext.getReconId(), objectMapping.getName());

        ReconPhase sourcePhase =
                new ReconPhase(reconQueryResult.getIterator(), reconContext, ObjectSetContext.get(), allLinks, targetIdRegistry, objectMapping.getSourceRecon());
        sourcePhase.setFeedSize(objectMapping.getFeedSize());
        sourcePhase.execute();
        targetIdRegistry.persistTargetIds(reconContext.getReconId());

        reconStats.addDuration(ReconciliationStatistic.DurationMetric.sourcePagePhase, sourcePhasePageStart);
        reconStats.sourcePhaseEnd();
        measureSource.end();
    }

    private void clusteredReconTargetPhase() throws SynchronizationException, InterruptedException {
        if (!reconContext.getReconHandler().isRunTargetPhase()) {
            //Sanity check - the target phase should never be scheduled if target phase not configured
            throw new SynchronizationException("ReconciliationContext indicates that the target phase should not be run, " +
                    "but a scheduled target phase job is executing!");
        }
        logger.debug("Performing clustered target phase for recon {}", reconContext.getReconId());
        ReconciliationStatistic reconStats = reconContext.getStatistics();
        reconStats.targetQueryStart();
        final long targetQueryStart = startNanoTime(reconContext);

        final ResultIterable initialTargetIterable = reconContext.queryTarget();
        Collection<String> targetIdsNotProcessedInSourcePhase = targetIdRegistry.getTargetPhaseIds(
                reconContext.getReconId(), initialTargetIterable.getAllIds());
        final ResultIterable finalTargetIterable = initialTargetIterable.removeNotMatchingEntries(targetIdsNotProcessedInSourcePhase);

        logger.debug("Going to perform clustered recon target phase for recon {} on the following target ids: {}",
                reconContext.getReconId(), finalTargetIterable.getAllIds());
        reconStats.addDuration(ReconciliationStatistic.DurationMetric.targetQuery, targetQueryStart);
        reconStats.targetQueryEnd();

        //Won't be pre-fetching links for the target phase
        // In the future, it might make sense to prefetch the links corresponding to the target ids.
        Map<String, Map<String, Link>> allLinks = null;
        EventEntry measureTarget = Publisher.start(EVENT_RECON_TARGET, reconContext.getReconId(), null);
        final long targetPhaseStart = startNanoTime(reconContext);
        reconContext.setStage(ReconStage.ACTIVE_RECONCILING_TARGET);
        reconStats.targetPhaseStart();
        ReconPhase targetPhase = new ReconPhase(finalTargetIterable.iterator(), reconContext, ObjectSetContext.get(),
                allLinks, null, objectMapping.getTargetRecon());
        targetPhase.setFeedSize(objectMapping.getFeedSize());
        targetPhase.execute();
        reconStats.addDuration(ReconciliationStatistic.DurationMetric.targetPhase, targetPhaseStart);
        reconStats.targetPhaseEnd();
        measureTarget.end();
        doSuccessfulReconCompletionTasks();
    }

    /**
     * When a source page has completed, and the pagingCookie indicates that no other pages are available, a
     * job will be scheduled to determine if all source pages have completed. This method will be invoked when this
     * scheduled job is picked-up by a cluster node.
     */
    private void clusteredReconSourceCompletionCheck() throws SynchronizationException {
        logger.debug("Performing a clustered recon source page completion check for recon id {}", reconContext.getReconId());
        if (schedulerDispatch.isSourcePageJobActive(reconContext.getReconId())) {
            schedulerDispatch.dispatchSourcePhaseCompletionTask(reconContext.getReconId(), objectMapping.getName());
        } else {
            if (reconContext.getReconHandler().isRunTargetPhase()) {
                schedulerDispatch.dispatchTargetPhase(reconContext.getReconId(), objectMapping.getName());
            } else {
                doSuccessfulReconCompletionTasks();
            }
        }
    }

    private void doSuccessfulReconCompletionTasks() throws SynchronizationException {
        logger.debug("Performing successful clustered recon completion tasks for recon {}", reconContext.getReconId());
        reconContext.setStage(ReconStage.ACTIVE_PROCESSING_RESULTS);
        objectMapping.doResults(reconContext, ObjectSetContext.get());
        reconContext.setStage(ReconStage.COMPLETED_SUCCESS);
        reconContext.getStatistics().reconEnd();

        persistAndAggregateReconStats();
        objectMapping.logReconEndSuccess(reconContext, ObjectSetContext.get());
        reconStatsPersistence.deletePersistedInstances(reconContext.getReconId());
        targetIdRegistry.deletePersistedTargetIds(reconContext.getReconId());
    }

    /**
     * Persists the current ReconStats, and aggregates the previous stats in the current value, and sets this
     * aggregated value in the ReconciliationContext, so that the aggregated state is audited and logged.
     * @throws SynchronizationException if the current ReconStatisitics state cannot be persisted, or the aggregated
     * state cannot be obtained.
     */
    private void persistAndAggregateReconStats() throws SynchronizationException {
        reconStatsPersistence.persistInstance(reconContext.getReconId(), reconContext.getStatistics());
        aggregateReconStatsAndSetAsCurrent();
    }
    /**
     * Called prior to logging the recon summary for both successful, and failed, recons. Serves to aggregate the
     * statistics, and set this aggregated value in the ReconciliationContext, so that this aggregated instance is audited
     * and logged.
     * @throws SynchronizationException if the current ReconStatisitics state cannot be persisted, or the aggregated
     * state cannot be obtained.
     */
    private void aggregateReconStatsAndSetAsCurrent() throws SynchronizationException {
        final Optional<ReconciliationStatistic> aggregatedInstance =
                reconStatsPersistence.getAggregatedInstance(reconContext.getReconId());
        if (aggregatedInstance.isPresent()) {
            reconContext.setStatistics(aggregatedInstance.get());
        }
    }
}
