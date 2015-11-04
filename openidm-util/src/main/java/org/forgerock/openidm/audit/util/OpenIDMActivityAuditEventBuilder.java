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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openidm.audit.util;

import org.forgerock.audit.events.ActivityAuditEventBuilder;
import org.forgerock.util.Reject;

/**
 * Extended Commons Audit Event Builder that handles the extended attributes of OpenIdm.
 */
public class OpenIDMActivityAuditEventBuilder<T extends OpenIDMActivityAuditEventBuilder<T>>
        extends ActivityAuditEventBuilder<T> {

    public static final String MESSAGE = "message";
    public static final String OBJECT_ID = "objectId";
    public static final String PASSWORD_CHANGED = "passwordChanged";
    public static final String STATUS = "status";

    @SuppressWarnings("rawtypes")
    public static OpenIDMActivityAuditEventBuilder<?> auditEventBuilder() {
        return new OpenIDMActivityAuditEventBuilder();
    }

    private OpenIDMActivityAuditEventBuilder() {
    }

    public T message(String message) {
        jsonValue.put(MESSAGE, message);
        return self();
    }

    public T passwordChanged(boolean passwordChanged) {
        jsonValue.put(PASSWORD_CHANGED, passwordChanged);
        return self();
    }

    public T status(Status status) {
        Reject.ifNull(status, "Status should not be null.");
        jsonValue.put(STATUS, status.name());
        return self();
    }
}
