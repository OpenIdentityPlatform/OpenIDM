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
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.util.Accessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handle a LiveSync failure by saving its detail to a dead-letter queue.  The queue
 * is implemented as a repository target.
 *
 * @author brmiller
 */
public class DeadLetterQueueHandler implements SyncFailureHandler {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueHandler.class);

    /** accessor to the router */
    private final Accessor<JsonResourceAccessor> accessor;

    /**
     * Construct this live sync failure handler.
     *
     * @param accessor an accessor to the router
     */
    public DeadLetterQueueHandler(Accessor<JsonResourceAccessor> accessor) {
        this.accessor = accessor;
    }

    /**
     * Handle sync failure.
     *
     * @param syncFailure JsonValue that contains the sync failure data
     * @param failureCause the cause of the sync failure
     * @throws SyncHandlerException when retries are not exceeded
     */
    public void invoke(JsonValue syncFailure, Exception failureCause)
        throws SyncHandlerException {

        String id = new StringBuffer("repo/synchronisation/deadLetterQueue/")
            .append(syncFailure.get("systemIdentifier").asString())
            .append("/")
            .append(syncFailure.get("token").toString())
            .toString();

        try {
            JsonValue syncDetail = syncFailure.copy();
            syncDetail.put("failureCause", failureCause.toString());
            accessor.access().create(id, syncDetail);
            logger.info("{} saved to dead letter queue", syncFailure.get("uid").asString());
        } catch (JsonResourceException e) {
            throw new SyncHandlerException("Failed reading/writing " + id, e);
        }
    }
}
