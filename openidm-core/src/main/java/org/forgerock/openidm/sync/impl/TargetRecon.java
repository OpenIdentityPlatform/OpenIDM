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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl;

import java.util.Collection;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TargetRecon implements Recon {

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetRecon.class);

    private final ObjectMapping objectMapping;

    TargetRecon(ObjectMapping objectMapping) {
        this.objectMapping = Reject.checkNotNull(objectMapping);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recon(String id, JsonValue objectEntry, ReconciliationContext reconContext, Context context,
            Map<String, Map<String, Link>> allLinks, Collection<String> remainingIds)  throws SynchronizationException {
        reconContext.checkCanceled();
        for (String linkQualifier : objectMapping.getAllLinkQualifiers(context, reconContext)) {
            TargetSyncOperation op = new TargetSyncOperation(objectMapping, context);
            op.reconContext = reconContext;
            op.setLinkQualifier(linkQualifier);

            ReconAuditEventLogger event = new ReconAuditEventLogger(op, objectMapping.getName(), context);
            event.setLinkQualifier(op.getLinkQualifier());

            if (objectEntry == null) {
                // Load target detail on demand
                op.targetObjectAccessor = new LazyObjectAccessor(objectMapping.getConnectionFactory(), objectMapping.getTargetObjectSet(), id);
            } else {
                // Pre-queried target detail
                op.targetObjectAccessor = new LazyObjectAccessor(objectMapping.getConnectionFactory(), objectMapping.getTargetObjectSet(), id, objectEntry);
            }
            event.setTargetObjectId(LazyObjectAccessor.qualifiedId(objectMapping.getTargetObjectSet(), id));
            op.reconId = reconContext.getReconId();
            Status status = Status.SUCCESS;
            try {
                op.sync();
            } catch (SynchronizationException se) {
                if (op.action != ReconAction.EXCEPTION) {
                    status = Status.FAILURE; // exception was not intentional
                    LOGGER.warn("Unexpected failure during target reconciliation {}", reconContext.getReconId(),
                            se);
                }
                objectMapping.setLogEntryMessage(event, se);
            }
            // update statistics with status
            reconContext.getStatistics().processStatus(status);

            if (!ReconAction.NOREPORT.equals(op.action) && (status == Status.FAILURE || op.action != null)) {
                event.setReconciling("target");
                if (op.getSourceObjectId() != null) {
                    event.setSourceObjectId(
                            LazyObjectAccessor.qualifiedId(objectMapping.getSourceObjectSet(), op.getSourceObjectId()));
                }
                event.setStatus(status);
                event.setReconId(reconContext.getReconId());
                objectMapping.logEntry(event, reconContext);
            }
        }
    }
}
