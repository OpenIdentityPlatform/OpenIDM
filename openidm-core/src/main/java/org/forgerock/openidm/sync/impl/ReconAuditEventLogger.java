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
 * Portions copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import java.util.List;

import org.forgerock.services.context.Context;

/**
 * A recon audit log entry representation.  Contains additional fields
 * and logic specific to recon entries.
 * @see ReconAuditEventBuilder
 */
class ReconAuditEventLogger extends AbstractSyncAuditEventLogger<ReconAuditEventBuilder> {

    public static final String RECON_AUDIT_PATH = "audit/recon";
    public static final String RECON_AUDIT_EVENT_NAME = "recon";

    /**
     * entryType for a "start" recon audit log entry
     */
    public static final String RECON_LOG_ENTRY_TYPE_RECON_START = "start";
    /**
     * entryType for a "summary" recon audit log entry
     */
    public static final String RECON_LOG_ENTRY_TYPE_RECON_END = "summary";
    /**
     * entryType for an "entry" recon audit log entry
     */
    public static final String RECON_LOG_ENTRY_TYPE_RECON_ENTRY = "entry";

    private String reconId;
    private String entryType = RECON_LOG_ENTRY_TYPE_RECON_ENTRY;
    private String reconciling;
    private String ambiguousTargetIds;
    private ReconciliationService.ReconAction reconciliationServiceReconAction;

    public ReconAuditEventLogger(ObjectMapping.SyncOperation syncOperation, String mapping,
            Context context) {
        super(syncOperation, mapping, context);
    }

    /**
     * Simple setter.
     * @param reconId
     */
    public void setReconId(String reconId) {
        this.reconId = reconId;
    }

    /**
     * Simple setter.
     * @param entryType
     */
    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    /**
     * Sets the ambiguous target IDs. The List is converted to a comma delimited string.
     *
     * @param ambiguousIds A list of IDs.
     */
    public void setAmbiguousTargetIds(List<String> ambiguousIds) {
        if (ambiguousIds != null) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String id : ambiguousIds) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(id);
            }
            ambiguousTargetIds = sb.toString();
        } else {
            ambiguousTargetIds = "";
        }
    }

    /**
     * Simple setter.
     *
     * @param reconciling
     */
    public void setReconciling(String reconciling) {
        this.reconciling = reconciling;
    }

    /**
     * Sets the reconAction value from the ReconciliationService: recon, reconByQuery, reconById.
     *
     * @param reconAction
     */
    public void setReconciliationServiceReconAction(ReconciliationService.ReconAction reconAction) {
        this.reconciliationServiceReconAction = reconAction;
    }

    /**
     * Returns the audit path for the RECON event.
     *
     * @see #RECON_AUDIT_PATH
     * @return
     */
    @Override
    protected String getAuditPath() {
        return RECON_AUDIT_PATH;
    }

    /**
     * Returns the RECON event name.
     *
     * @see #RECON_AUDIT_EVENT_NAME
     * @return
     */
    @Override
    protected String getEventName() {
        return RECON_AUDIT_EVENT_NAME;
    }

    /**
     * Applies the additional fields beyond for RECON.
     *
     * @param builder The builder to apply the custom fields to.
     * @return The updated builder.
     */
    @Override
    protected ReconAuditEventBuilder applyCustomFields(ReconAuditEventBuilder builder) {
        return builder.reconciling(reconciling)
                .ambiguousTargetIds(ambiguousTargetIds)
                .reconAction(reconciliationServiceReconAction)
                .entryType(entryType)
                .reconId(reconId);
    }

    /**
     * {@inheritDoc}
     *
     * @return a ReconAuditEventBuilder
     * @see ReconAuditEventBuilder
     */
    @Override
    protected ReconAuditEventBuilder getEventBuilder() {
        return ReconAuditEventBuilder.auditEventBuilder();
    }

}
