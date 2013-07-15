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

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.Scripts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** map of references to built-in sync failure handlers made available to user-supplied script */
    private final Map<String,SyncFailureHandler> builtInHandlers;

    /**
     * Construct this sync failure handler.
     *
     * @param scopeFactory the ScopeFactory
     * @param config the config
     */
    public ScriptedSyncFailureHandler(ScopeFactory scopeFactory, JsonValue config,
            SyncFailureHandler... builtInHandlers) {
        this.scopeFactory = scopeFactory;
        this.script = Scripts.newInstance(getClass().getSimpleName(), config);
        this.builtInHandlers = new HashMap<String,SyncFailureHandler>();
        for (SyncFailureHandler handler : builtInHandlers) {
            if (handler instanceof LoggedIgnoreHandler) {
                this.builtInHandlers.put("loggedIgnore", handler);
            } else if (handler instanceof DeadLetterQueueHandler) {
                this.builtInHandlers.put("deadLetterQueue", handler);
            }
        }
    }

    /**
     * Handle sync failure by counting retries on this sync token, passing to
     * (optional) post-retry handler when retries are exceeded.
     *
     *
     * @param syncFailure @throws SyncHandlerException when retries are not exceeded
     * @param failureCause the cause of the sync failure
     */
    public void invoke(JsonValue syncFailure, Exception failureCause)
        throws SyncHandlerException {

        Map<String,Object> scope = scopeFactory.newInstance(ObjectSetContext.get());
        scope.put("syncFailure", syncFailure);
        scope.put("failureCause", failureCause);
        scope.put("failureHandlers", builtInHandlers);

        try {
            script.exec(scope);
        } catch (Exception e) {
            logger.debug("sync failure script on {} encountered exception", 
                    syncFailure.get("systemIdentifier").asString(), e);
            throw new SyncHandlerException("Issue with handling the failure during synchronize "
                    + syncFailure.get("uid").asString() + " object. " + failureCause.getMessage()
                    + ". Failure handling reported " + e.getMessage(), e);
        }
    }
}
