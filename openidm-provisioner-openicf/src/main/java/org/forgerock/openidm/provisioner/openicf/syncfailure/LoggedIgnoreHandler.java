/*
 * Copyright 2013 ForgeRock, AS.
 *
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
 */
package org.forgerock.openidm.provisioner.openicf.syncfailure;

import org.forgerock.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A SyncFailureHandler that logs the error and ignores it.
 *
 */
public class LoggedIgnoreHandler implements SyncFailureHandler {
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(LoggedIgnoreHandler.class);

    /**
     * Handle sync failure.
     *
     * @param context the request context associated with the invocation
     * @param syncFailure JsonValue that contains the sync failure data
     * @param failureCause the cause of the sync failure
     * @throws SyncHandlerException when retries are not exceeded
     */
    public void invoke(Context context, Map<String, Object> syncFailure, Exception failureCause)
        throws SyncHandlerException {
        logger.warn("{} liveSync failure on sync-token {} for {}, {} - {}",
                syncFailure.get("systemIdentifier"),
                syncFailure.get("token").toString(),
                syncFailure.get("objectType"),
                syncFailure.get("uid"),
                failureCause.toString());
    }
}
