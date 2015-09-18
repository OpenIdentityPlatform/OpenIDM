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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openidm.audit.impl;

import org.forgerock.services.context.Context;
import org.forgerock.json.resource.CreateRequest;

/**
 * An audit log filter.
 *
 */
interface AuditLogFilter {
    /**
     * Test whether the log message described in the request should be filtered out; i.e. not logged.
     *
     * @param context the Context associated with the request
     * @param request the audit log create request.  Implementations will typically
     *                examine request.getContent()
     * @return true if the log message should be dropped, false if it should be logged
     */
    boolean isFiltered(Context context, CreateRequest request);
}
