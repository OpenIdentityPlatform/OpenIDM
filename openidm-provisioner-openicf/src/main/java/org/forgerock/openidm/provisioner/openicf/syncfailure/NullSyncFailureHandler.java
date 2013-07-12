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

/**
 * A Null-object SyncFailureHandler that implements the equivalent of an "ignore"
 * operation.
 *
 * @author brmiller
 */
public class NullSyncFailureHandler implements SyncFailureHandler {
    /**
     * Handle sync failure.
     *
     *
     * @param syncFailure contains the sync failure data
     * @param failureCause the cause of the sync failure
     * @throws SyncHandlerException when retries are not exceeded
     */
    public void invoke(JsonValue syncFailure, Exception failureCause)
        throws SyncHandlerException {
        // Null object pattern - does nothing - everything is "OK"
    }
}
