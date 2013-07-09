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

import org.forgerock.json.fluent.JsonValue;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple sync failure handler that retries <em>n</em> times before passing control to a subsequent
 * sync failure handler.
 *
 * @author brmiller
 */
public class SimpleRetrySyncFailureHandler implements SyncFailureHandler {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(SimpleRetrySyncFailureHandler.class);

    /** how many times to retry live sync of a particular sync token */
    private int syncFailureRetries;

    /** the handler to call when the retries are exhausted */
    private SyncFailureHandler postRetryHandler;

    /** current token being retried */
    private SyncToken currentSyncToken;

    /** number of retries on current token */
    private int currentRetries;

    /**
     * Construct the SyncFailureHandler according to the config.
     *
     * @param handlerFactory the SyncFailureHandlerFactory used to create the postRetryHandler according to the config
     * @param config the configuration for this handler
     */
    public SimpleRetrySyncFailureHandler(SyncFailureHandlerFactory handlerFactory, JsonValue config) {
        syncFailureRetries = config.get("syncFailureRetries").isNull()
            ? 0
            : config.get("syncFailureRetries").asInteger().intValue();
        postRetryHandler = handlerFactory.create(config.get("postRetryHandler"));
    }

    /**
     * Handle sync failure by counting retries on this sync token, passing to
     * (optional) post-retry handler when retries are exceeded.
     *
     * @param token the sync token that failed
     * @param objectType the type of object being synchronized
     * @param failedRecord the failed record
     * @param failedRecordUid the failed record's id
     * @param exception the Exception that was thrown as part of the failure
     * @throws SyncHandlerException when retries are not exceeded
     */
    public void handleSyncFailure(String systemIdentifierName, SyncToken token, String objectType, 
            String failedRecord, Uid failedRecordUid, Exception exception)
        throws SyncHandlerException {

        if (token.equals(currentSyncToken)) {
            currentRetries++;
        }
        else {
            currentSyncToken = token;
            currentRetries = 0;
        }

        if (currentRetries >= syncFailureRetries) {
            logger.info("sync retries = " + currentRetries + "/" + syncFailureRetries + ", invoking post-retry handler: "
                    +  postRetryHandler.getClass().getSimpleName());
            // retries exhausted; move on...
            postRetryHandler.handleSyncFailure(systemIdentifierName, token, objectType, failedRecord,
                    failedRecordUid, exception);
        } else {
            logger.info("sync retries = " + currentRetries + "/" + syncFailureRetries + ", retrying");
            throw new SyncHandlerException("Failed to synchronize " + failedRecordUid + " object, " +
                    "retries (" + currentRetries + ") not exhausted.", exception);
        }
    }
}
