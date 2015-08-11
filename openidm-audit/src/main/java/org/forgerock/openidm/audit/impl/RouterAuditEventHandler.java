/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
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
package org.forgerock.openidm.audit.impl;

import org.forgerock.audit.DependencyProvider;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceName;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit event handler that logs to a router target.
 */
public class RouterAuditEventHandler extends AuditEventHandlerBase<RouterAuditEventHandlerConfiguration> {
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(RouterAuditEventHandler.class);

    /** router target resource path */
    private ResourceName resourcePath;

    /** the DependencyProvider to provide access to the ConnectionFactory */
    private DependencyProvider dependencyProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(RouterAuditEventHandlerConfiguration config) throws ResourceException {
        resourcePath = ResourceName.valueOf(config.getResourcePath());
        logger.info("Audit logging to: {}", resourcePath.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDependencyProvider(DependencyProvider provider) {
        this.dependencyProvider = provider;
    }

    private ConnectionFactory getConnectionFactory() throws ClassNotFoundException {
        return dependencyProvider.getDependency(ConnectionFactory.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws ResourceException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        try {
            final ConnectionFactory connectionFactory = getConnectionFactory();
            handler.handleResult(
                    connectionFactory.getConnection().create(
                            new AuditContext(context),
                            Requests.newCreateRequest(
                                    resourcePath.child(request.getResourceName()),
                                    request.getNewResourceId(),
                                    request.getContent())));
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(ResourceUtil.adapt(e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext context, QueryRequest request, final QueryResultHandler handler) {
        try {
            final ConnectionFactory connectionFactory = getConnectionFactory();
            final QueryRequest newRequest = Requests.copyOfQueryRequest(request);
            newRequest.setResourceName(resourcePath.child(request.getResourceName()));
            connectionFactory.getConnection().query(new AuditContext(context), newRequest,
                    new QueryResultHandler() {
                        @Override
                        public void handleError(ResourceException error) {
                            handler.handleError(error);
                        }

                        @Override
                        public boolean handleResource(Resource resource) {
                            return handler.handleResource(resource);
                        }

                        @Override
                        public void handleResult(QueryResult result) {
                            handler.handleResult(result);
                        }
                    });
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(ResourceUtil.adapt(e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            final ConnectionFactory connectionFactory = getConnectionFactory();
            final ReadRequest newRequest = Requests.copyOfReadRequest(request);
            newRequest.setResourceName(resourcePath.child(request.getResourceName()).child(resourceId));
            handler.handleResult(connectionFactory.getConnection().read(new AuditContext(context), newRequest));
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(ResourceUtil.adapt(e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<RouterAuditEventHandlerConfiguration> getConfigurationClass() {
        return RouterAuditEventHandlerConfiguration.class;
    }

}
