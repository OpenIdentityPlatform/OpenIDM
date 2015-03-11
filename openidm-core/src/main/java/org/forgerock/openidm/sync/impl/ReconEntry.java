/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openidm.sync.impl;

import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.openidm.audit.util.AuditConstants;
import org.forgerock.openidm.sync.impl.ObjectMapping.SyncOperation;
import org.forgerock.openidm.util.DateUtil;

/**
 * A recon audit log entry representation.  Contains any additional fields
 * and logic specific to recon entries.
 * 
 */
class ReconEntry extends LogEntry {

    /** 
     * Type of the audit log entry (start/entry/summary) 
     */
    private String entryType = AuditConstants.RECON_LOG_ENTRY_TYPE_RECON_ENTRY;
    
    /** 
     * The id identifying the reconciliation run 
     */
    private String reconId;
    
    /**
     * A string describing what is being reconciled
     */
    public String reconciling;

    /**
     *  A comma delimited formatted representation of any ambiguous identifiers
     */
    private String ambigiousTargetIds;
    
    /**
     * The reconciliation action
     */
    private ReconciliationService.ReconAction reconAction;

    /**
     * Sets the ambiguous target IDs.
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
            ambigiousTargetIds = sb.toString();
        } else {
            ambigiousTargetIds = "";
        }
    }

    /**
     * Returns the reconciliation ID.
     * 
     * @return the reconciliation ID.
     */
    private String getReconId() {
        return (reconId == null && op != null) ? op.reconId : reconId;
    }

    /**
     * Set the result status.
     *
     * @param status
     */
    public void setStatus(ObjectMapping.Status status) {
        this.status = status;
    }

    /**
     * Construct a specific type reconciliation audit log entry from the given {@link SyncOperation} and mapping name.
     *
     * @param op the sync operation
     * @param mappingName the mapping name
     * @param rootContext the root context
     * @param dateUtil a date-formatting object
     * @param reconAction the reconciliation action
     * @param reconId the recon id
     */
    public ReconEntry(SyncOperation op, String mappingName, Context rootContext, String entryType, DateUtil dateUtil,
            ReconciliationService.ReconAction reconAction, String reconId) {
        super(op, mappingName, rootContext, dateUtil);
        this.entryType = entryType;
        this.reconAction = reconAction;
        this.reconId = reconId;
    }

    /**
     * Construct a regular reconciliation audit log entry from the given {@link SyncOperation} and mapping name.
     *
     * @param op the sync operation
     * @param mappingName the mapping name
     * @param rootContext the root context
     * @param dateUtil a date-formatting object
     */
    public ReconEntry(SyncOperation op, String mappingName, Context rootContext, DateUtil dateUtil) {
        super(op, mappingName, rootContext, dateUtil);
    }

    @Override
    protected JsonValue toJsonValue() {
        JsonValue jv = super.toJsonValue();
        jv.put("entryType", entryType);
        jv.put("reconId", getReconId());
        jv.put("reconciling", reconciling);
        jv.put("ambiguousTargetObjectIds", ambigiousTargetIds);
        jv.put("reconAction", reconAction != null ? reconAction.toString() : null);
        return jv;
    }
}
