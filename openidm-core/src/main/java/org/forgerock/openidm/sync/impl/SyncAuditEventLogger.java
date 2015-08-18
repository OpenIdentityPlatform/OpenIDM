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

import org.forgerock.http.Context;

/**
 * A sync audit log entry representation.
 * @see SyncAuditEventBuilder
 */
class SyncAuditEventLogger extends AbstractSyncAuditEventLogger {

    public static final String SYNC_AUDIT_PATH = "audit/sync";
    public static final String SYNC_AUDIT_EVENT_NAME = "sync";

    public SyncAuditEventLogger(ObjectMapping.SyncOperation op, String mappingName, Context context) {
        super(op, mappingName, context);
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    protected String getAuditPath() {
        return SYNC_AUDIT_PATH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getEventName() {
        return SYNC_AUDIT_EVENT_NAME;
    }

    /**
     * {@inheritDoc}
     * @return a SyncAuditEventBuilder
     * @see SyncAuditEventBuilder
     */
    @Override
    protected AbstractSyncAuditEventBuilder getEventBuilder() {
        return SyncAuditEventBuilder.auditEventBuilder();
    }

    /**
     * Sync has no fields beyond the base logger, so this just returns the builder.
     * {@inheritDoc}
     * @param builder The builder to customize.
     * @return
     */
    @Override
    protected AbstractSyncAuditEventBuilder applyCustomFields(AbstractSyncAuditEventBuilder builder) {
        // Sync has no additional custom fields at this time.
        return builder;
    }
}
