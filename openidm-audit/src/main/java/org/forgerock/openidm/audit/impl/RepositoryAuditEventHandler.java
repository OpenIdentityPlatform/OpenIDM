/*
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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openidm.audit.impl;

import org.forgerock.audit.DependencyProvider;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;

/**
 * Audit event handler for Repository.  This is a decorator to the RouterAuditEventHandler where the resourcePath is
 * hardcoded to be "repo/audit".
 *
 * @see RouterAuditEventHandler
 */
public class RepositoryAuditEventHandler extends AuditEventHandlerBase<RepositoryAuditEventHandlerConfiguration> {

    private RouterAuditEventHandler routerAuditEventHandler;

    /**
     * Constructs the decorated RouterAuditEventHandler.
     */
    public RepositoryAuditEventHandler() {
        this.routerAuditEventHandler = new RouterAuditEventHandler();
    }

    /**
     * Configures the decorated RouterAuditEventHandler with a fixed path of "repo/audit"
     */
    @Override
    public void configure(RepositoryAuditEventHandlerConfiguration config) throws ResourceException {
        RouterAuditEventHandlerConfiguration routerConfig = new RouterAuditEventHandlerConfiguration();
        routerConfig.setResourcePath(config.getResourcePath());
        routerAuditEventHandler.configure(routerConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDependencyProvider(DependencyProvider provider) {
        routerAuditEventHandler.setDependencyProvider(provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws ResourceException {
        routerAuditEventHandler.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext serverContext, ActionRequest actionRequest,
            ResultHandler<JsonValue> resultHandler) {
        routerAuditEventHandler.actionCollection(serverContext, actionRequest, resultHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext serverContext, String resourceId, ActionRequest actionRequest,
            ResultHandler<JsonValue> resultHandler) {
        routerAuditEventHandler.actionInstance(serverContext, resourceId, actionRequest, resultHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createInstance(ServerContext serverContext, CreateRequest createRequest,
            ResultHandler<Resource> resultHandler) {
        routerAuditEventHandler.createInstance(serverContext, createRequest, resultHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext serverContext, QueryRequest queryRequest,
            QueryResultHandler queryResultHandler) {
        routerAuditEventHandler.queryCollection(serverContext, queryRequest, queryResultHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext serverContext, String resourceId, ReadRequest readRequest,
            ResultHandler<Resource> resultHandler) {
        routerAuditEventHandler.readInstance(serverContext, resourceId, readRequest, resultHandler);
    }
}
