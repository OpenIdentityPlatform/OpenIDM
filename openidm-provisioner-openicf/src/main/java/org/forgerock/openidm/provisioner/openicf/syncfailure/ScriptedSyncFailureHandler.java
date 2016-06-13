/*
 * Copyright 2013-2015 ForgeRock, AS.
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

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

/**
 * Handle liveSync failure by calling an external, user-supplied script.
 *
 */
public class ScriptedSyncFailureHandler implements SyncFailureHandler {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(ScriptedSyncFailureHandler.class);

    /** the script to call */
    private final ScriptEntry scriptEntry;

    /** map of references to built-in sync failure handlers made available to user-supplied script */
    private final Map<String,SyncFailureHandler> builtInHandlers;

    /**
     * Construct this sync failure handler.
     *
     * @param config the config
     */
    public ScriptedSyncFailureHandler(ScriptRegistry scriptRegistry, JsonValue config, SyncFailureHandler... builtInHandlers)
        throws ScriptException {

        this.scriptEntry = scriptRegistry.takeScript(config);
        this.builtInHandlers = new HashMap<>();
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
     * @param context the request context associated with the invocation
     * @param syncFailure @throws SyncHandlerException when retries are not exceeded
     * @param failureCause the cause of the sync failure
     */
    public void invoke(Context context, Map<String, Object> syncFailure, Exception failureCause)
        throws SyncHandlerException {

        if (null == scriptEntry) {
            throw new SyncHandlerException("No script registered");
        }

        Script script = scriptEntry.getScript(context);
        script.put("context", context);
        script.put("syncFailure", syncFailure);
        script.put("failureCause", failureCause);
        script.put("failureHandlers", builtInHandlers);
        try {
            script.eval();
        } catch (Exception e) {
            logger.debug("sync failure script on {} encountered exception", syncFailure.get("systemIdentifier"), e);
            throw new SyncHandlerException("Issue with handling the failure during synchronize "
                    + syncFailure.get("uid") + " object: " + failureCause.getMessage()
                    + ". Failure handling reported " + e.getMessage(), e);
        }
    }
}
