/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.AbstractConnectionWrapper;
import org.forgerock.json.resource.ClientContext;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.FutureResult;
import org.forgerock.json.resource.PersistenceConfig;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.core.ServerConstants;
import static org.forgerock.openidm.util.ContextUtil.INTERNAL_PROTOCOL;

import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides internal routing for a top-level object set.
 *
 * @author Paul C. Bryan
 * @author aegloff
 * @author brmiller
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
    public FutureResult<Connection> getConnectionAsync(ResultHandler<? super Connection> handler) {
        return internal.getConnectionAsync(handler);
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
                        return new InternalServerContext(context);
                    }
                };
            }

            @Override
            public FutureResult<Connection> getConnectionAsync(final ResultHandler<? super Connection> handler) {
                try {
                    final Connection connection = getConnection();
                    final FutureResult<Connection> future = Resources.newCompletedFutureResult(connection);
                    if (handler != null) {
                        handler.handleResult(connection);
                    }
                    return future;
                } catch (ResourceException e) {
                    throw new RuntimeException("Can't obtain connection", e);
                }
            }
        };
    }

    static class InternalServerContext extends ServerContext implements ClientContext {
        /** the client-friendly name of this context */
        private static final String CONTEXT_NAME = "internal-server";

        /**
         * {@inheritDoc}
         */
        protected InternalServerContext(Context parent) {
            super(parent);
        }

        /**
         * {@inheritDoc}
         */
        protected InternalServerContext(JsonValue savedContext, PersistenceConfig config) throws ResourceException {
            super(savedContext, config);
        }

        /**
         * Get this Context's name
         *
         * @return this object's name
         */
        public String getContextName() {
            return CONTEXT_NAME;
        }

        /**
         * Return whether this Context has the given {@link Protocol}.  The only
         * Protocol this class has is the internal protocol.
         *
         * @param protocol the Protocol to test
         * @returns true if the protocol is "internal"
         */
        @Override
        public boolean hasProtocol(Protocol protocol) {
            return INTERNAL_PROTOCOL.equals(protocol);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Protocol getProtocol() {
            return INTERNAL_PROTOCOL;
        }
    }

}
