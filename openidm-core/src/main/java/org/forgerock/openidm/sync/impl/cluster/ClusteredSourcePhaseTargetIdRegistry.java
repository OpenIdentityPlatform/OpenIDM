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

import java.util.Collection;

import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.impl.SourcePhaseTargetIdRegistration;

/**
 * Encapsulates the concerns of recording target ids correlated to source ids during the source phase of a distributed
 * recon.
 */
public interface ClusteredSourcePhaseTargetIdRegistry extends SourcePhaseTargetIdRegistration {
    /**
     * Called during the target phase. Will compare the set of target ids correlated to source ids, persisted during the
     * source phase, to the Collection of all target ids. Every target id in the complete collection which
     * is not included in the set of target ids persisted during the source phase will be processed during the
     * target phase.
     * @param reconId the reconId identifying the recon run
     * @param allTargetIds all of the ids in the recon target
     * @return the possibly empty Collection of target identifiers to be processed during the target phase.
     */
    Collection<String> getTargetPhaseIds(String reconId, Collection<String> allTargetIds);

    /**
     * Called when the reconciliation of a page of source identifiers is complete. Allows the target ids registered via
     * SourcePhaseTargetIdRegistration#sourceAndTargetReconciled to be persisted.
     * @param reconId the reconId identifying the recon run for which the registered target ids should be persisted
     * @throws SynchronizationException if persistence of the registered target ids fails
     */
    void persistTargetIds(String reconId) throws SynchronizationException;

    /**
     * Deletes the target ids persisted to a distributed, persistent store as part of the recon.
     * @param reconId the reconId identifying the recon run for which persisted target ids should be deleted
     * @throws SynchronizationException if the target ids could not be purged from the persistent store.
     */
    void deletePersistedTargetIds(String reconId) throws SynchronizationException;
}
