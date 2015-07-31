/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.audit.filter;

import org.forgerock.audit.events.AccessAuditEventBuilder;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.InternalServerContext;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.servlet.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class creates a {@link Filter} that can be placed on the router to log access events.
 */
public class AuditFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditFilter.class);
    private ConnectionFactory connectionFactory;

    private AuditFilter() {
        //prevent instantiation
    }

    public AuditFilter(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public class AuditState {
        private final long actionTime = System.currentTimeMillis();
        private Request request;

        public AuditState(final Request request) {
            this.request = request;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filterAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler,
            RequestHandler next) {
        AuditState state = new AuditState(request);
        next.handleAction(context, request, wrap(context, state, handler));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filterCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler,
            RequestHandler next) {
        AuditState state = new AuditState(request);
        next.handleCreate(context, request, wrap(context, state, handler));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filterDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler,
            RequestHandler next) {
        AuditState state = new AuditState(request);
        next.handleDelete(context, request, wrap(context, state, handler));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filterPatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler,
            RequestHandler next) {
        AuditState state = new AuditState(request);
        next.handlePatch(context, request, wrap(context, state, handler));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filterQuery(ServerContext context, QueryRequest request, QueryResultHandler handler,
            RequestHandler next) {
        AuditState state = new AuditState(request);
        next.handleQuery(context, request, wrap(context, state, handler));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filterRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler,
            RequestHandler next) {
        AuditState state = new AuditState(request);
        next.handleRead(context, request, wrap(context, state, handler));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filterUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler,
            RequestHandler next) {
        AuditState state = new AuditState(request);
        next.handleUpdate(context, request, wrap(context, state, handler));
    }

    private <V> ResultHandler<V> wrap(ServerContext context, AuditState state, ResultHandler<V> handler) {
        return new AuditingResultHandler<>(context, state, handler);
    }

    private QueryResultHandler wrap(ServerContext context, AuditState state, QueryResultHandler handler) {
        return new AuditingQueryResultHandler(context, state, handler);
    }

    private void logAuditAccessEntry(final ServerContext context, final AuditState state,
                                     final ResourceException resourceException) {

        if (!context.containsContext(HttpContext.class)
                || context.containsContext(InternalServerContext.class)) {
            // don't log internal requests
            return;
        }

        final long elapsedTime = System.currentTimeMillis() - state.actionTime;

        final AccessAuditEventBuilder accessAuditEventBuilder = new AccessAuditEventBuilder();
        accessAuditEventBuilder.forHttpCrestRequest(context, state.request)
                .authorizationIdFromSecurityContext(context)
                .serverFromHttpContext(context)
                .resourceOperationFromRequest(state.request)
                .clientFromHttpContext(context)
                .transactionIdFromRootContext(context)
                .timestamp(System.currentTimeMillis())
                .authenticationFromSecurityContext(context)
                .eventName("access");

        if (resourceException != null) {
            accessAuditEventBuilder.responseWithMessage(
                    "FAILURE - " + String.valueOf(resourceException.getCode()),
                    elapsedTime,
                    resourceException.getReason());
        } else {
            accessAuditEventBuilder.response("SUCCESS", elapsedTime);
        }
        try {
            //log the log entry
            final CreateRequest createRequest =
                    Requests.newCreateRequest("audit/access", accessAuditEventBuilder.toEvent().getValue());

            //wrap the context in a new internal server context since we are using the external connection factory
            connectionFactory.getConnection().create(new InternalServerContext(context), createRequest);
        } catch (ResourceException e) {
            LOGGER.error("Failed to log audit access entry", e);
        }
    }

    private class AuditingResultHandler<V> implements ResultHandler<V> {

        private final ServerContext context;
        private final AuditState state;
        private final ResultHandler<V> handler;

        private AuditingResultHandler(ServerContext context, AuditState state, ResultHandler<V> handler) {
            this.context = context;
            this.state = state;
            this.handler = handler;
        }

        ServerContext getContext() {
            return context;
        }

        AuditState getState() {
            return state;
        }

        @Override
        public void handleError(ResourceException error) {
            try {
                logAuditAccessEntry(getContext(), getState(), error);
            } finally {
                handler.handleError(error);
            }
        }

        @Override
        public void handleResult(V result) {
            try {
                logAuditAccessEntry(getContext(), getState(), null);
            } finally {
                handler.handleResult(result);
            }
        }
    }

    private final class AuditingQueryResultHandler extends AuditingResultHandler<QueryResult>
            implements QueryResultHandler {

        private final QueryResultHandler handler;

        private AuditingQueryResultHandler(ServerContext context, AuditState state, QueryResultHandler handler) {
            super(context, state, handler);
            this.handler = handler;
        }

        @Override
        public boolean handleResource(Resource resource) {
            try {
                logAuditAccessEntry(getContext(), getState(), null);
            } finally {
                return handler.handleResource(resource);
            }
        }
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
}
