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
 * Copyright 2012-2016 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.services.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper to submit source/target recon for a given id for concurrent processing
 */
class ReconTask implements Callable<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconTask.class);
    
    private final String id;
    private final JsonValue objectEntry;
    private final ReconciliationContext reconContext;
    private final Context parentContext;
    private final Map<String, Map<String, Link>> allLinks;
    private final Collection<String> remainingIds;
    private final Recon reconById;

    ReconTask(ResultEntry resultEntry, ReconciliationContext reconContext, Context parentContext,
            Map<String, Map<String, Link>> allLinks, Collection<String> remainingIds, Recon reconById) {
        this.id = resultEntry.getId();
        // This value is null if it wasn't pre-queried
        this.objectEntry = resultEntry.getValue();
        LOGGER.debug("Recon task on {} {}", id, objectEntry);

        this.reconContext = reconContext;
        this.parentContext = parentContext;
        this.allLinks = allLinks;
        this.remainingIds = remainingIds;
        this.reconById = reconById;
    }

    public Void call() throws SynchronizationException {
        //TODO I miss the Request Context
        ObjectSetContext.push(parentContext);
        try {
            reconById.recon(id, objectEntry, reconContext, parentContext, allLinks, remainingIds);
        } finally {
            ObjectSetContext.pop();
        }
        return null;
    }
}
