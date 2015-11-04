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

import javax.inject.Inject;

import org.forgerock.audit.Audit;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Audit event handler for Repository.  This is a decorator to the RouterAuditEventHandler where the resourcePath is
 * hardcoded to be "repo/audit".
 *
 * @see RouterAuditEventHandler
 */
public class RepositoryAuditEventHandler extends AuditEventHandlerBase {

    private RouterAuditEventHandler routerAuditEventHandler;

    /**
     * Constructs the decorated RouterAuditEventHandler.
     */
    @Inject
    public RepositoryAuditEventHandler(
            final RepositoryAuditEventHandlerConfiguration configuration,
            final EventTopicsMetaData eventTopicsMetaData,
            @Audit final ConnectionFactory connectionFactory) {
        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());
        RouterAuditEventHandlerConfiguration routerConfig = new RouterAuditEventHandlerConfiguration();
        routerConfig.setResourcePath(configuration.getResourcePath());
        routerConfig.setTopics(configuration.getTopics());
        routerConfig.setName(configuration.getName());
        routerConfig.setEnabled(configuration.isEnabled());
        this.routerAuditEventHandler =
                new RouterAuditEventHandler(routerConfig, eventTopicsMetaData, connectionFactory);
    }

    @Override
    public void startup() throws ResourceException {
        // do nothing
    }

    @Override
    public void shutdown() throws ResourceException {
        routerAuditEventHandler.shutdown();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(final Context context,
            final String auditEventTopic, final JsonValue auditEventContent) {
        return routerAuditEventHandler.publishEvent(context, auditEventTopic, auditEventContent);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(final Context context, final String auditEventTopic,
            final String auditEventId) {
        return routerAuditEventHandler.readEvent(context, auditEventTopic, auditEventId);
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(final Context context, final String auditEventTopic,
            final QueryRequest queryRequest, final QueryResourceHandler queryResourceHandler) {
        return routerAuditEventHandler.queryEvents(context, auditEventTopic, queryRequest, queryResourceHandler);
    }
}
