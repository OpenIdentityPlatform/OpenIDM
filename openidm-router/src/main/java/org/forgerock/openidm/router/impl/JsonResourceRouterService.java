/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openidm.router.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.http.Context;
import org.forgerock.json.resource.AbstractConnectionWrapper;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides internal routing for a top-level object set.
 *
 */
@Component(name = JsonResourceRouterService.PID, policy = ConfigurationPolicy.OPTIONAL,
        metatype = true, configurationFactory = false, immediate = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM internal JSON resource router"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/") })
public class JsonResourceRouterService implements ConnectionFactory {

    // Public Constants
    public static final String PID = "org.forgerock.openidm.internal";

    /** Setup logging for the {@link JsonResourceRouterService}. */
    private final static Logger logger = LoggerFactory.getLogger(JsonResourceRouterService.class);

    /** Event name prefix for monitoring the router */
    public final static String EVENT_ROUTER_PREFIX = "openidm/internal/router/";

    @Reference(target = "(service.pid=org.forgerock.openidm.router)")
    private ConnectionFactory connectionFactory = null;

    /** internal connection factory for internal routing */
    private ConnectionFactory internal = null;

    @Activate
    void activate(ComponentContext context) {
        internal = newInternalConnectionFactory(connectionFactory);
        logger.info("Reconciliation service activated.");
    }

    @Modified
    void modified(ComponentContext context) {
        activate(context);
        logger.info("Reconciliation service modified.");
    }

    @Deactivate
    void deactivate(ComponentContext context) {
        logger.info("Reconciliation service deactivated.");
    }

    // ----- Implementation of ConnectionFactory - delegate to this object's internal ConnectionFactory
    // which decorates the router ConnectionFactory

    @Override
    public Connection getConnection() throws ResourceException {
        return internal.getConnection();
    }

    @Override
    public Promise<Connection, ResourceException> getConnectionAsync() {
        return internal.getConnectionAsync();
    }
    @Override
    public void close() {
        internal.close();
    }

    private ConnectionFactory newInternalConnectionFactory(final ConnectionFactory connectionFactory) {
        return new ConnectionFactory() {
            @Override
            public void close() {
                connectionFactory.close();
            }

            @Override
            public Connection getConnection() throws ResourceException {
                return new AbstractConnectionWrapper<Connection>(connectionFactory.getConnection()) {
                    @Override
                    protected Context transform(Context context) {
                        return context;
                    }
                };
            }

            @Override
            public Promise<Connection, ResourceException> getConnectionAsync() {
                try {
                    return Promises.newResultPromise(getConnection());
                } catch (ResourceException e) {
                    return Promises.newExceptionPromise(e);
                }
            }
        };
    }


}
