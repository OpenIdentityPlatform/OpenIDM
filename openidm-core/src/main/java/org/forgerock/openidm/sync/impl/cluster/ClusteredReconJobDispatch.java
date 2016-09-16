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

import org.forgerock.openidm.sync.SynchronizationException;

/**
 *
 * Encapsulates the concern of dispatching the various jobs associated with clustered recon, and querying whether all
 * source page jobs have completed.
 */
public interface ClusteredReconJobDispatch {
    String PAGING_COOKIE_KEY = "pagingCookie";
    String RECON_ID_KEY = "reconId";
    String CLUSTERED_SUB_ACTION_KEY = "subAction";
    String CLUSTERED_NEXT_PAGE_ACTION_PARAM = "clusteredNextPage";
    String CLUSTERED_SOURCE_COMPLETION_PARAM = "clusteredSourceCompletion";
    String CLUSTERED_TARGET_PHASE_PARAM = "clusteredTargetPhase";

    /**
     * Schedules a job which will reconcile a page of source entries starting with the specified paging cookie.
     * @param reconId
     * @param mappingName
     * @param pagingCookie
     * @throws SynchronizationException
     */
    void dispatchSourcePageRecon(String reconId, String mappingName, String pagingCookie) throws SynchronizationException;

    /**
     * Schedules a job which will determine whether a source page recon job is still running, as the target phase job
     * can be scheduled only when all source pages have completed.
     * @param reconId
     * @param mappingName
     * @throws SynchronizationException
     */
    void dispatchSourcePhaseCompletionTask(String reconId, String mappingName) throws SynchronizationException;

    /**
     * Schedules a job to reconcile any target ids not correlated to a source id during the source phase.
     * @param reconId
     * @param mappingName
     * @throws SynchronizationException
     */
    void dispatchTargetPhase(String reconId, String mappingName) throws SynchronizationException;

    /**
     * Queries the scheduler to determine whether any source page is currently executing.
     * @param reconId the recon id for which source page jobs should be checked.
     * @return true if a source page job for the specified recon id is running; false otherwise
     * @throws SynchronizationException if the check cannot be made
     */
    boolean isSourcePageJobActive(String reconId) throws SynchronizationException;
}
