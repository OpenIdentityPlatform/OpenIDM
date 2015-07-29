/*
 *  The contents of this file are subject to the terms of the Common Development and
 *  Distribution License (the License). You may not use this file except in compliance with the
 *  License.
 *
 *  You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 *  specific language governing permission and limitations under the License.
 *
 *  When distributing Covered Software, include this CDDL Header Notice in each file and include
 *  the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 *  Header, with the fields enclosed by brackets [] replaced by your own identifying
 *  information: "Portions copyright [year] [name of copyright owner]".
 *
 *  Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl;

/**
 * This is the audit event builder for RECON event.
 */
public class ReconAuditEventBuilder<T extends ReconAuditEventBuilder<T>>
        extends AbstractSyncAuditEventBuilder<T> {

    public static final String ENTRY_TYPE = "entryType";
    public static final String RECON_ACTION = "reconAction";
    public static final String AMBIGUOUS_TARGET_IDS = "ambiguousTargetObjectIds";
    public static final String RECONCILING = "reconciling";
    public static final String RECON_ID = "reconId";

    @SuppressWarnings("rawtypes")
    public static ReconAuditEventBuilder<?> auditEventBuilder() {
        return new ReconAuditEventBuilder();
    }

    private ReconAuditEventBuilder() {
    }

    /**
     * Sets the entryType on the builder.
     *
     * @param entryType
     * @return Self.
     * @see #ENTRY_TYPE
     */
    public T entryType(String entryType) {
        jsonValue.put(ENTRY_TYPE, entryType);
        return self();
    }

    /**
     * Sets the reconAction from the ReconciliationService.
     *
     * @param action
     * @return Self.
     * @see #RECON_ACTION
     */
    public T reconAction(ReconciliationService.ReconAction action) {
        if (null != action) {
            jsonValue.put(RECON_ACTION, action.name());
        }
        return self();
    }

    /**
     * Sets the ambiguousTargetIds that are already expected to be a common delimited list of unquoted ids.
     *
     * @param ambiguousTargetIds
     * @return Self.
     * @see #AMBIGUOUS_TARGET_IDS
     */
    public T ambiguousTargetIds(String ambiguousTargetIds) {
        jsonValue.put(AMBIGUOUS_TARGET_IDS, ambiguousTargetIds);
        return self();
    }

    /**
     * Sets the reconciling value on the builder.
     *
     * @param reconciling
     * @return Self.
     * @see #RECONCILING
     */
    public T reconciling(String reconciling) {
        jsonValue.put(RECONCILING, reconciling);
        return self();
    }

    /**
     * Sets the reconId
     *
     * @param reconId
     * @return
     * @see #RECON_ID
     */
    public T reconId(String reconId) {
        jsonValue.put(RECON_ID, reconId);
        return self();
    }

}