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
import org.forgerock.http.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.promise.Promise;

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
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest actionRequest) {
        return routerAuditEventHandler.actionCollection(context, actionRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest actionRequest) {
        return routerAuditEventHandler.actionInstance(context, resourceId, actionRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest createRequest) {
        return routerAuditEventHandler.createInstance(context, createRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest queryRequest,
            QueryResourceHandler queryResourceHandler) {
        return routerAuditEventHandler.queryCollection(context, queryRequest, queryResourceHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest readRequest) {
        return routerAuditEventHandler.readInstance(context, resourceId, readRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<RepositoryAuditEventHandlerConfiguration> getConfigurationClass() {
        return RepositoryAuditEventHandlerConfiguration.class;
    }
}
