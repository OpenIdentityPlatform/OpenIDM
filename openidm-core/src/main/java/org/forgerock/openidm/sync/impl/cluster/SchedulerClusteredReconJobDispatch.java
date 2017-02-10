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

package org.forgerock.openidm.sync.impl.cluster;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.impl.ObjectSetContext;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;

/**
 * A realization of the ClusteredReconJobDispatch interface, leveraging the SchedulerService and the Quartz Scheduler.
 * A scheduled clustered recon invocation will look like the following:
 *
 * "enabled" : true,
 * "persisted" : true,
 * "type" : "simple",
 * "concurrentExecution" : "false",
 * "invokeService": "sync",
 * "nodeExclusiveIdentifier", reconId,
 * "invokeContext": {
 *      "action": "reconcile",
 *      "mapping": "systemLdapAccount_managedUser",
 *      "subAction": "clusteredNextPage",
 *      "pagingCookie" : "dfdfd"
 * }
 *
 * The subAction will specify which of the clustered-recon sub-actions should be executed next.
 *
 * @see ClusteredReconJobDispatch
 */
public class SchedulerClusteredReconJobDispatch implements ClusteredReconJobDispatch {
    private static final String CLUSTERED_RECON_JOB_PREFACE = "clustered_recon";
    private static final String JOB_ID_CONSTITUENT_DELIMITER = "-";
    private static final String SCHEDULER_JOB_RESOURCE_CONTAINER = "scheduler/job";
    private static final String NODE_EXCLUSIVE_IDENTIFIER = "nodeExclusiveIdentifier";
    private static final int CLUSTERED_SOURCE_PHASE_COMPLETION_CHECK_START_LATENCY_MINUTES = 2;
    private final ConnectionFactory connectionFactory;
    private final DateUtil dateUtil;
    private final Logger logger;

    public SchedulerClusteredReconJobDispatch(ConnectionFactory connectionFactory, DateUtil dateUtil, Logger logger) {
        this.connectionFactory = connectionFactory;
        this.dateUtil = dateUtil;
        this.logger = logger;
    }

    @Override
    public void dispatchSourcePageRecon(String reconId, String mappingName, String pagingCookie) throws SynchronizationException {
        scheduleClusteredReconJob(sourcePageJobId(reconId, pagingCookie),
                getSchedulerDispatchState(
                        getNextSourcePageInvocationContext(reconId, mappingName, CLUSTERED_NEXT_PAGE_ACTION_PARAM, pagingCookie)));
        logger.debug("Scheduled clustered source page for recon id {} and paging cookie {}", reconId, pagingCookie);
    }

    @Override
    public void dispatchSourcePhaseCompletionTask(String reconId, String mappingName) throws SynchronizationException {
        scheduleClusteredReconJob(sourcePhaseCompletionCheckJobId(reconId),
                getSchedulerDispatchState(
                        getSourcePhaseCompletionInvocationContext(reconId, mappingName, CLUSTERED_SOURCE_COMPLETION_PARAM)));
        logger.debug("Scheduled clustered source phase completion check for recon id {}", reconId);
    }

    @Override
    public void dispatchTargetPhase(String reconId, String mappingName) throws SynchronizationException {
        scheduleClusteredReconJob(targetPhaseJobId(reconId),
                getSchedulerDispatchState(getBaseInvocationContext(reconId, mappingName, CLUSTERED_TARGET_PHASE_PARAM)));
        logger.debug("Scheduled clustered target phase for recon id {}", reconId);
    }

    @Override
    public boolean isSourcePageJobActive(String reconId) throws SynchronizationException {
        try {
            final QueryRequest schedulerJobs = Requests.newQueryRequest(SCHEDULER_JOB_RESOURCE_CONTAINER).setQueryId("query-all-ids");
            final List<ResourceResponse> responses = new LinkedList<>();
            connectionFactory.getConnection().query(ObjectSetContext.get(), schedulerJobs, responses);
            int numJobsForRecon = 0;
            for (ResourceResponse response : responses) {
                if (response.getContent().get(FIELD_CONTENT_ID).isString() &&
                        response.getContent().get(FIELD_CONTENT_ID).asString().startsWith(getClusteredReconJobPreface(reconId))) {
                    numJobsForRecon++;
                }
            }
            /*
            At least one job for this recon id will be running - namely the job which is currently performing this query.
            If more than one job is running, however, it indicates that a source page job has not yet completed.
             */
            return numJobsForRecon > 1;
        } catch (ResourceException e) {
            throw new SynchronizationException(e);
        }
    }

    private void scheduleClusteredReconJob(String scheduledJobId, JsonValue invocationState) throws SynchronizationException {
        final CreateRequest createRequest = Requests.newCreateRequest(SCHEDULER_JOB_RESOURCE_CONTAINER, scheduledJobId, invocationState);
        try {
            connectionFactory.getConnection().create(ObjectSetContext.get(), createRequest);
        } catch (ResourceException e) {
            throw new SynchronizationException(e);
        }
    }

    @VisibleForTesting
    String getClusteredReconJobPreface(String reconId) {
        return CLUSTERED_RECON_JOB_PREFACE + JOB_ID_CONSTITUENT_DELIMITER + reconId;
    }

    private String sourcePageJobId(String reconId, String pagingCookie) {
        return  getClusteredReconJobPreface(reconId) + JOB_ID_CONSTITUENT_DELIMITER + "sourcePage" +
                JOB_ID_CONSTITUENT_DELIMITER + pagingCookie;
    }

    private String targetPhaseJobId(String reconId) {
        return getClusteredReconJobPreface(reconId) + JOB_ID_CONSTITUENT_DELIMITER + "targetPhase";
    }

    private String sourcePhaseCompletionCheckJobId(String reconId) {
        /*
        A source-phase completion check may be scheduled several times, and a new completion check will be scheduled
        while the current job is running if source recon has not yet completed as the first completion check task is
        running. Thus the identifier of each such job should be different, a requirement fulfilled by appending the
        current time in millis.
         */
        return getClusteredReconJobPreface(reconId) + JOB_ID_CONSTITUENT_DELIMITER + "sourcePageCompletionCheck"
                + JOB_ID_CONSTITUENT_DELIMITER + System.currentTimeMillis();
    }

    private JsonValue getSchedulerDispatchState(JsonValue invocationContext) {
        return json(object(
                field("invokeContext", invocationContext.asMap()),
                field("enabled", true),
                field("persisted", true),
                field("concurrentExecution", false),
                field("type", "simple"),
                field("invokeService", "sync")));
    }

    private JsonValue getBaseInvocationContext(String reconId, String mappingName, String subAction) {
        return json(object(
                field("action", "reconcile"),
                field("mapping", mappingName),
                field(RECON_ID_KEY, reconId),
                // add a nodeExclusiveIdentifier field that will ensure that only a single job with this identifier runs on
                // a cluster node at any given time. 
                field(NODE_EXCLUSIVE_IDENTIFIER, reconId),
                field(CLUSTERED_SUB_ACTION_KEY, subAction)));
    }

    private JsonValue getSourcePhaseCompletionInvocationContext(String reconId, String mappingName, String subAction) {
        JsonValue value  = getBaseInvocationContext(reconId, mappingName, subAction);
        Calendar fireTime = Calendar.getInstance();
        fireTime.add(Calendar.MINUTE, CLUSTERED_SOURCE_PHASE_COMPLETION_CHECK_START_LATENCY_MINUTES);
        value.add("startTime", dateUtil.getFormattedTime(fireTime.getTimeInMillis()));
        return value;
    }

    private JsonValue getNextSourcePageInvocationContext(String reconId, String mappingName, String subAction, String pagingCookie) {
        JsonValue invocationContext = getBaseInvocationContext(reconId, mappingName, subAction);
        invocationContext.add(ClusteredReconJobDispatch.PAGING_COOKIE_KEY, pagingCookie);
        return invocationContext;
    }
}
