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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.util.Accessor;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A factory service to create the SyncFailureHandler strategy from config.
 *
 * @author bmiller
 */
@Component(name = SyncFailureHandlerFactoryImpl.PID,
        policy = ConfigurationPolicy.IGNORE,
        description = "OpenIDM Sync Failure Handler Factory Service",
        immediate = true
)
@Service()
public class SyncFailureHandlerFactoryImpl implements SyncFailureHandlerFactory {
    public static final String PID = "org.forgerock.openidm.openicf.syncfailure";

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(SyncFailureHandlerFactory.class);

    /** Scope factory service. */
    @Reference(
            referenceInterface = ScopeFactory.class,
            bind = "bindScopeFactory",
            unbind = "unbindScopeFactory",
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            policy = ReferencePolicy.DYNAMIC
    )
    private ScopeFactory scopeFactory;

    protected void bindScopeFactory(ScopeFactory scopeFactory) {
        this.scopeFactory = scopeFactory;
    }

    protected void unbindScopeFactory(ScopeFactory scopeFactory) {
        this.scopeFactory = null;
    }

    /** the router */
    @Reference(referenceInterface = JsonResource.class,
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            policy = ReferencePolicy.STATIC,
            target = "(service.pid=org.forgerock.openidm.router)")
    private JsonResource router;

    /**
     * Create a <em>SyncFailureHandler</em> from the config.  The config should optionally
     * describe
     * <ul>
     *     <li>number of retries</li>
     *     <li>whether to save the failed sync item to a dead-letter queue</li>
     *     <li>whether to call an external, user-supplied script</li>
     * </ul>
     *
     * @param config the config for the SyncFailureHandler
     * @return the SyncFailureHandler
     */
    public SyncFailureHandler create(JsonValue config) {
        if (!config.isNull()) {
            final List<SyncFailureHandler> handlers = new ArrayList<SyncFailureHandler>();

            // look for retry config FIRST, so sync failure retry handler will be invoked first
            if (!config.get("retries").isNull() && config.get("retries").asInteger() > 0) {
                handlers.add(new SimpleRetrySyncFailureHandler(config.get("retries").asInteger()));
            }
            if (!config.get("deadLetterQueue").isNull() && config.get("deadLetterQueue").asString().length() > 0) {
                handlers.add(new DeadLetterQueueHandler(
                        new Accessor<JsonResourceAccessor>() {
                            public JsonResourceAccessor access() {
                                return new JsonResourceAccessor(router, ObjectSetContext.get());
                            }
                        },
                        config.get("deadLetterQueue").asString()));
            }
            if (!config.get("script").isNull() && config.get("script").isMap()) {
                handlers.add(new ScriptedSyncFailureHandler(scopeFactory, config.get("script")));
            }

            if (handlers.size() > 0) {
                return new SyncFailureHandler() {
                    @Override
                    public void handleSyncFailure(String systemIdentifierName,  SyncToken token,  String objectType,
                            String failedRecord,  Uid failedRecordUid,  Exception exception)
                        throws SyncHandlerException {

                        // run through the list of handlers once, removing ones that successfully executed
                        // (the unsuccessful ones will throw a SyncHandlerException)
                        Iterator<SyncFailureHandler> iter = handlers.iterator();
                        while (iter.hasNext()) {
                            SyncFailureHandler handler = iter.next();
                            handler.handleSyncFailure(systemIdentifierName, token, objectType, failedRecord, failedRecordUid, exception);
                            iter.remove();
                        }
                    }
                };
            }
        }
        // either no config, no retries, or no configured handlers
        return new NullSyncFailureHandler();
    }
}

