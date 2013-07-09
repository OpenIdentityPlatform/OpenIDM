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
import org.forgerock.openidm.util.Accessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** the router */
    @Reference(referenceInterface = JsonResource.class,
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            policy = ReferencePolicy.STATIC,
            target = "(service.pid=org.forgerock.openidm.router)")
    private JsonResource router;

    /**
     * Create a <em>SyncFailureHandler</em> from the config.  The config should describe a strategy and
     * any necessary parameters needed by that handler.
     *
     * @param config the config for the SyncFailureHandler
     * @return the SyncFailureHandler
     */
    public SyncFailureHandler create(JsonValue config) {
        if (!config.isNull()) {
            if ("simple-retry".equals(config.get("strategy").asString())) {
                return new SimpleRetrySyncFailureHandler(this, config);
            }
            else if ("dead-letter".equals(config.get("strategy").asString())) {
                return new DeadLetterQueueHandler(
                        new Accessor<JsonResourceAccessor>() {
                            public JsonResourceAccessor access() {
                                return new JsonResourceAccessor(router, ObjectSetContext.get());
                            }
                        },
                        config);
            }
            /*
            else if ("scripted".equals(config.get("strategy").asString())) {
                ...
                TODO implement scripted handler
            }
             */
        }
        return new NullSyncFailureHandler();
    }
}

