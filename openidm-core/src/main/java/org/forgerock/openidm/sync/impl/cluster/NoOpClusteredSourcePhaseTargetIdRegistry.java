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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl.cluster;

import org.forgerock.openidm.sync.SynchronizationException;

import java.util.Collection;

/**
 * When clustered source recon is enabled, but the target phase is not configured, an instance of this class will
 * be created to fulfill the ClusteredSourcePhaseTargetIdRegistry contract.
 * @see ClusteredSourcePhaseTargetIdRegistry
 */
public class NoOpClusteredSourcePhaseTargetIdRegistry implements ClusteredSourcePhaseTargetIdRegistry {
    @Override
    public void targetIdReconciled(String targetId) {
        //invocation valid - will be called by SourceRecon for each reconciled entry during the source phase
    }

    @Override
    public Collection<String> getTargetPhaseIds(String reconId, Collection<String> allTargetIds) {
        throw new IllegalStateException("Illegal sate in getTargetPhaseIds for recon " + reconId +
                ": getTargetPhaseIds should not be called as the recon target phase should not be enabled!.");
    }

    @Override
    public void persistTargetIds(String reconId) throws SynchronizationException {
        //invocation valid - called at the conclusion of each source page
    }

    @Override
    public void deletePersistedTargetIds(String reconId) throws SynchronizationException {
        //invocation valid - called when clustered recon complete
    }
}
