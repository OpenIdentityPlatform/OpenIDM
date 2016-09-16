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
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SourceRecon implements Recon {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceRecon.class);

    private final ObjectMapping objectMapping;

    SourceRecon(ObjectMapping objectMapping) {
        this.objectMapping = Reject.checkNotNull(objectMapping);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recon(String id, JsonValue objectEntry, ReconciliationContext reconContext, Context context,
            Map<String, Map<String, Link>> allLinks, SourcePhaseTargetIdRegistration targetIdRegistration)
            throws SynchronizationException {
        reconContext.checkCanceled();
        LazyObjectAccessor sourceObjectAccessor = objectEntry == null
                ? new LazyObjectAccessor(objectMapping.getConnectionFactory(), objectMapping.getSourceObjectSet(), id) // Load source detail on demand
                : new LazyObjectAccessor(objectMapping.getConnectionFactory(), objectMapping.getSourceObjectSet(), id, objectEntry); // Pre-queried source detail
        Status status = Status.SUCCESS;
        final ReconciliationStatistic stats = reconContext.getStatistics();

        if (objectEntry == null) {
            final long sourceObjectQueryStart = ObjectMapping.startNanoTime(reconContext);
            objectEntry = sourceObjectAccessor.getObject();
            stats.addDuration(ReconciliationStatistic.DurationMetric.sourceObjectQuery, sourceObjectQueryStart);
        }

        for (String linkQualifier : objectMapping.getLinkQualifiers(objectEntry, null, false, context, reconContext)) {
            SourceSyncOperation op = new SourceSyncOperation(objectMapping, context);
            op.reconContext = reconContext;
            op.setLinkQualifier(linkQualifier);

            ReconAuditEventLogger auditEvent = new ReconAuditEventLogger(op, objectMapping.getName(), context);
            auditEvent.setLinkQualifier(op.getLinkQualifier());
            op.sourceObjectAccessor = sourceObjectAccessor;
            if (allLinks != null) {
                String normalizedSourceId = objectMapping.getLinkType().normalizeSourceId(id);
                op.initializeLink(allLinks.get(linkQualifier).get(normalizedSourceId));
            }
            auditEvent.setSourceObjectId(LazyObjectAccessor.qualifiedId(objectMapping.getSourceObjectSet(), id));
            op.reconId = reconContext.getReconId();
            try {
                op.sync();
            } catch (SynchronizationException se) {
                if (op.action != ReconAction.EXCEPTION) {
                    status = Status.FAILURE; // exception was not intentional
                    LOGGER.warn("Unexpected failure during source reconciliation {}", op.reconId, se);
                }
                objectMapping.setLogEntryMessage(auditEvent, se);
            }

            // update statistics with status
            reconContext.getStatistics().processStatus(status);

            String[] targetIds = op.getTargetIds();
            for (String handledId : targetIds) {
                // If target system has case insensitive IDs, remove without regard to case
                String normalizedHandledId = objectMapping.getLinkType().normalizeTargetId(handledId);
                targetIdRegistration.targetIdReconciled(normalizedHandledId);
                LOGGER.trace("Removed target from remaining targets: {}", normalizedHandledId);
            }
            if (!ReconAction.NOREPORT.equals(op.action) && (status == Status.FAILURE || op.action != null)) {
                auditEvent.setReconciling("source");
                try {
                    if (op.hasTargetObject()) {
                        auditEvent.setTargetObjectId(LazyObjectAccessor.qualifiedId(objectMapping.getTargetObjectSet(),
                                op.getTargetObjectId()));
                    }
                } catch (SynchronizationException ex) {
                    auditEvent.setMessage("Failure in preparing recon entry " + ex.getMessage() + " for target: "
                            + op.getTargetObjectId() + " original status: " + status + " " +
                            "message: " + auditEvent.getMessage());
                    status = Status.FAILURE;
                }
                auditEvent.setStatus(status);
                auditEvent.setAmbiguousTargetIds(op.getAmbiguousTargetIds());
                auditEvent.setReconId(reconContext.getReconId());
                objectMapping.logEntry(auditEvent, reconContext);
            }
        }
    }
}
