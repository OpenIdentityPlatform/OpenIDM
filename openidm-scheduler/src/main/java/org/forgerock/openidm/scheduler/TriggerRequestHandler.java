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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.scheduler;

import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a {@link RequestHandler} that handles crudpaq operations for {@link Trigger}'s.
 */
class TriggerRequestHandler extends AbstractRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(TriggerRequestHandler.class);

    /**
     * The sub route resource path for triggers.
     */
    static final String TRIGGER_RESOURCE_PATH = "/trigger";

    static final String TRIGGERS_REPO_RESOURCE_PATH = "/repo/scheduler/triggers";

    private final ConnectionFactory connectionFactory;

    /**
     * Creates a {@link TriggerRequestHandler} given a {@link ConnectionFactory}.
     * @param connectionFactory the {@link ConnectionFactory}.
     */
    TriggerRequestHandler(final ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        final QueryRequest queryRequest = Requests.copyOfQueryRequest(request);
        queryRequest.setResourcePath(TRIGGERS_REPO_RESOURCE_PATH);
        try {
            return connectionFactory.getConnection().queryAsync(context, queryRequest, handler);
        } catch (final ResourceException e) {
            logger.error("Unable to query triggers", e);
            return e.asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        if (request.getResourcePathObject().isEmpty()) {
            return new BadRequestException("No id was supplied to read").asPromise();
        }
        final String triggerId = request.getResourcePath();
        final ReadRequest readRequest = Requests.newReadRequest(TRIGGERS_REPO_RESOURCE_PATH, triggerId);
        try {
            return connectionFactory.getConnection().readAsync(context, readRequest);
        } catch (final ResourceException e) {
            logger.error("Unable to read trigger with id {}", triggerId, e);
            return e.asPromise();
        }
    }
}
