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
 *
 * Portions Copyrighted 2024 3A Systems LLC.
 */
package org.forgerock.openidm.provisioner.openicf.syncfailure;

import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.json.JsonValue;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;

/**
 * A factory service to create the SyncFailureHandler strategy from config.
 *
 */
@Component(
        name = SyncFailureHandlerFactoryImpl.PID,
        configurationPolicy = ConfigurationPolicy.IGNORE,
        immediate = true,
        property = Constants.SERVICE_PID + "=" + SyncFailureHandlerFactoryImpl.PID
)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM Sync Failure Handler Factory Service")
public class SyncFailureHandlerFactoryImpl implements SyncFailureHandlerFactory {
    public static final String PID = "org.forgerock.openidm.openicf.syncfailure";

    /* config tokens */
    protected static final String CONFIG_MAX_RETRIES = "maxRetries";
    protected static final String CONFING_POST_RETRY = "postRetryAction";
    protected static final String CONFIG_DEAD_LETTER = "dead-letter-queue";
    protected static final String CONFIG_LOGGED_IGNORE = "logged-ignore";
    protected static final String CONFIG_SCRIPT = "script";

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected volatile ScriptRegistry scriptRegistry;

    private void bindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = service;
    }

    private void unbindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = null;
    }

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC)
    protected IDMConnectionFactory connectionFactory;

    /**
     * Create a <em>SyncFailureHandler</em> from the config.  The config should optionally
     * describe
     * <ul>
     *     <li>number of retries</li>
     *     <li>what to do upon retry exhaustion:
     *     <ul>
     *         <li>whether to save the failed sync item to a dead-letter queue</li>
     *         <li>whether to log and ignore the failure</li>
     *         <li>whether to call an external, user-supplied script</li>
     *     </ul>
     * </ul>
     *
     * @param config the config for the SyncFailureHandler
     * @return the SyncFailureHandler
     */
    public SyncFailureHandler create(JsonValue config) throws Exception {

        if (null == config || config.isNull()) {
            return InfiniteRetrySyncFailureHandler.INSTANCE;
        }

        JsonValue maxRetries = config.get(CONFIG_MAX_RETRIES);
        JsonValue postRetry = config.get(CONFING_POST_RETRY);

        if (maxRetries.isNull() || maxRetries.asInteger() < 0) {
            return InfiniteRetrySyncFailureHandler.INSTANCE;
        } else if (maxRetries.asInteger() == 0) {
            return getPostRetryHandler(postRetry);
        } else {
            return new SimpleRetrySyncFailureHandler(maxRetries.asInteger(),
                    getPostRetryHandler(postRetry));
        }
    }

    /**
     * Create the SycFailureHandler to execute when the retries are exhausted (or if no retries are configured).
     * The config passed here is the value side of
     * {code}
     * "postRetryAction" : <value>
     * {code}
     * It may be either a String which indicates which handler to use, or a Map which indicates a handler
     * with more complex configuration.
     *
     * @param config the config that further specifies the sync failure handler
     * @return the SyncFailureHandler
     */
    private SyncFailureHandler getPostRetryHandler(JsonValue config) throws Exception {
        if (config.isString()) {
            if (CONFIG_DEAD_LETTER.equals(config.asString())) {
                return new DeadLetterQueueHandler(connectionFactory);
            } else if (CONFIG_LOGGED_IGNORE.equals(config.asString())) {
                return new LoggedIgnoreHandler();
            }
        }
        else if (config.isMap()) {
            if (config.get(CONFIG_SCRIPT).isMap()) {
                return new ScriptedSyncFailureHandler(
                        scriptRegistry,
                        config.get(CONFIG_SCRIPT),
                        // pass internal handlers so a script can call them if desired
                        new LoggedIgnoreHandler(),
                        new DeadLetterQueueHandler(connectionFactory));
            }
        }

        return NullSyncFailureHandler.INSTANCE;
    }
}

