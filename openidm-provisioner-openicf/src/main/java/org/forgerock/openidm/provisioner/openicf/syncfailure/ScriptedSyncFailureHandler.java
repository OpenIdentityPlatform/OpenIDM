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
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.Scripts;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handle liveSync failure by calling an external, user-supplied script.
 *
 * @author brmiller
 */
public class ScriptedSyncFailureHandler implements SyncFailureHandler {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(ScriptedSyncFailureHandler.class);

    /** ScopeFactory */
    private final ScopeFactory scopeFactory;

    /** the script to call */
    private final Script script;

    /**
     * Construct this sync failure handler.
     *
     * @param scopeFactory the ScopeFactory
     * @param config the config
     */
    public ScriptedSyncFailureHandler(ScopeFactory scopeFactory, JsonValue config) {
        this.scopeFactory = scopeFactory;
        this.script = Scripts.newInstance(getClass().getSimpleName(), config);
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

        Map<String,Object> scope = scopeFactory.newInstance(ObjectSetContext.get());
        scope.put("systemIdentifier", systemIdentifierName);
        scope.put("objectType", objectType);
        scope.put("exception", exception);
        scope.put("failedRecord", failedRecord);
        scope.put("failedRecordUid", failedRecordUid);

        try {
            script.exec(scope);
        } catch (Exception e) {
            logger.debug("sync failure script on {} encountered exception", systemIdentifierName, e);
            throw new SyncHandlerException("Issue with handling the failure during synchronize "
                    + failedRecordUid + " object. " + exception.getMessage() + ". Failure handling reported "
                    + e.getMessage(), e);
        }
    }
}
