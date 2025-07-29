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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openidm.audit.impl;

import static org.forgerock.json.resource.Requests.copyOfQueryRequest;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.util.promise.Promises.newResultPromise;

import jakarta.inject.Inject;

import org.forgerock.audit.Audit;
import org.forgerock.audit.AuditingContext;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Audit event handler for Repository.  This is implemented to use the router where the resourcePath is
 * hardcoded to be "repo/audit".
 */
public class RepositoryAuditEventHandler extends AuditEventHandlerBase {
    /**
     * Router target resource path.
     */
    private final ResourcePath resourcePath;

    /**
     * The DependencyProvider to provide access to the ConnectionFactory.
     */
    private final ConnectionFactory connectionFactory;

    @Inject
    public RepositoryAuditEventHandler(
            final RepositoryAuditEventHandlerConfiguration configuration,
            final EventTopicsMetaData eventTopicsMetaData,
            @Audit final ConnectionFactory connectionFactory) {
        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());
        this.resourcePath = ResourcePath.valueOf(configuration.getResourcePath());
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void startup() throws ResourceException {
        // do nothing
    }

    @Override
    public void shutdown() throws ResourceException {
        // do nothing
    }

    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(final Context context,
            final String auditEventTopic,
            final JsonValue auditEventContent) {
        try {
            final String auditEventId = auditEventContent.get(ResourceResponse.FIELD_CONTENT_ID).asString();
            return newResultPromise(connectionFactory.getConnection().create(new AuditingContext(context),
                    newCreateRequest(
                            resourcePath.concat(auditEventTopic),
                            auditEventId,
                            auditEventContent)));
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(final Context context, final String auditEventTopic,
            final String auditEventId) {
        try {
            return newResultPromise(connectionFactory.getConnection().read(new AuditingContext(context),
                    newReadRequest(resourcePath.concat(auditEventTopic), auditEventId)));
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(final Context context, final String auditEventTopic,
            final QueryRequest queryRequest, final QueryResourceHandler queryResourceHandler) {
        try {
            final QueryRequest newRequest = copyOfQueryRequest(queryRequest);
            newRequest.setResourcePath(resourcePath.concat(queryRequest.getResourcePathObject()));

            return newResultPromise(
                    connectionFactory.getConnection().query(new AuditingContext(context), newRequest,
                            new QueryResourceHandler() {
                                @Override
                                public boolean handleResource(ResourceResponse resourceResponse) {
                                    return queryResourceHandler.handleResource(resourceResponse);
                                }
                            }));
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }
}
