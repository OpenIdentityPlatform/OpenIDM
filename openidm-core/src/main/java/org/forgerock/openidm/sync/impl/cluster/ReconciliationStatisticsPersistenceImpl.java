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

import org.forgerock.guava.common.base.Optional;
import org.forgerock.guava.common.collect.ArrayListMultimap;
import org.forgerock.guava.common.collect.Multimap;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.impl.ReconciliationStatistic;

/**
 * @see ReconciliationStatisticsPersistence
 * This is a temporary implementation of this interface which will aggregate statistics for a single cluster node using
 * an in-memory structure.
 * Statistics aggregation will be implemented across cluster nodes as part of OPENIDM-6776.
 *
 */
public class ReconciliationStatisticsPersistenceImpl implements ReconciliationStatisticsPersistence {
    private static final Multimap<String, ReconciliationStatistic> nodeStats = ArrayListMultimap.create();

    @Override
    public void persistInstance(String reconId, ReconciliationStatistic instance) throws SynchronizationException {
        nodeStats.put(reconId, instance);
    }

    @Override
    public Optional<ReconciliationStatistic> getAggregatedInstance(String reconId) throws SynchronizationException {
        return ReconciliationStatistic.getAggregatedInstance(nodeStats.get(reconId));
    }

    @Override
    public void deletePersistedInstances(String reconId) throws SynchronizationException {
        nodeStats.removeAll(reconId);
    }
}
