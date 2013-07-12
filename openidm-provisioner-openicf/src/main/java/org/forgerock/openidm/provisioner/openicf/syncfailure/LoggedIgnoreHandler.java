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

import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SyncFailureHandler that logs the error and ignores it.
 *
 * @author brmiller
 */
public class LoggedIgnoreHandler implements SyncFailureHandler {
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(LoggedIgnoreHandler.class);

    /*
     * Handle sync failure.
     *
     * @param token the sync token that failed
     * @param objectType the type of object being synchronized
     * @param failedRecord the failed record
     * @param failedRecordUid the failed record's id
     * @param exception the Exception that was thrown as part of the failure
     * @throws SyncHandlerException when retries are not exceeded
     */
    public void handleSyncFailure(
            String systemIdentifierName,
            SyncToken token,
            String objectType,
            String failedRecord,
            Uid failedRecordUid,
            Exception exception)
        throws SyncHandlerException {
        logger.warn("LiveSync failure on {}, sync-token {} for {}, {}", systemIdentifierName, token.getValue(), objectType, failedRecordUid.getUidValue());
    }
}
