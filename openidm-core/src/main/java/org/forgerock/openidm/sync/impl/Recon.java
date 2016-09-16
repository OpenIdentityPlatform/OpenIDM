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
 * Copyright 2014-2017 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl;

import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.services.context.Context;

/**
 * Reconciliation interface.
 * Implementation is passed to ReconPhase and executed by the ReconTask
 *
 */
interface Recon {
    /**
     * Reconcile a given object ID
     * @param id the object id to reconcile
     * @param entry an optional value if the given entry was pre-loaded, or null if not
     * @param reconContext reconciliation context
     * @param rootContext json resource root ctx
     * @param allLinks all links if pre-queried, or null for on-demand link querying
     * @param targetIdRegistration to register the target ids correlated to a source id during the source phase
     * @throws SynchronizationException if there is a failure reported in reconciling this id
     */
    void recon(String id, JsonValue entry, ReconciliationContext reconContext, Context rootContext,
            Map<String, Map<String, Link>> allLinks, SourcePhaseTargetIdRegistration targetIdRegistration) throws SynchronizationException;
}
