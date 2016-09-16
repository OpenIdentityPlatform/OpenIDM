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
 * Copyright 2012-2017 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.services.context.Context;

/**
 * Reconcile the source/target phase, multi threaded or single threaded.
 */
class ReconPhase extends ReconFeeder {
    private final Context parentContext;
    private final Map<String, Map<String, Link>> allLinks;
    private final SourcePhaseTargetIdRegistration targetIdRegistration;
    private final Recon reconById;

    ReconPhase(Iterator<ResultEntry> resultIter, ReconciliationContext reconContext, Context parentContext,
            Map<String, Map<String, Link>> allLinks, SourcePhaseTargetIdRegistration targetIdRegistration, Recon reconById) {
        super(resultIter, reconContext);
        this.parentContext = parentContext;
        this.allLinks = allLinks;
        this.targetIdRegistration = targetIdRegistration;
        this.reconById = reconById;
    }
    
    @Override
    Callable<Void> createTask(ResultEntry objectEntry) throws SynchronizationException {
        return new ReconTask(objectEntry, reconContext, parentContext,
                allLinks, targetIdRegistration, reconById);
    }
}
