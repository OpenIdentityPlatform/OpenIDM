/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013-2015 ForgeRock AS. All Rights Reserved
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

import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.FAILURE;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.SUCCESS;
import static org.forgerock.audit.events.AccessAuditEventBuilder.TimeUnit.MILLISECONDS;
import static org.forgerock.http.context.ClientContext.newInternalClientContext;

import org.forgerock.audit.events.AccessAuditEventBuilder;
import org.forgerock.http.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Response;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
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
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
            RequestHandler next) {

        AuditState state = new AuditState(request);
        Promise<ActionResponse, ResourceException> promise = next.handleAction(context, request);
        logAuditAccessEntry(context, state, promise);

        return promise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
            RequestHandler next) {

        AuditState state = new AuditState(request);
        Promise<ResourceResponse, ResourceException> promise = next.handleCreate(context, request);
        logAuditAccessEntry(context, state, promise);

        return promise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
            RequestHandler next) {

        AuditState state = new AuditState(request);
        Promise<ResourceResponse, ResourceException> promise = next.handleDelete(context, request);
        logAuditAccessEntry(context, state, promise);

        return promise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
            RequestHandler next) {

        AuditState state = new AuditState(request);
        Promise<ResourceResponse, ResourceException> promise = next.handlePatch(context, request);
        logAuditAccessEntry(context, state, promise);

        return promise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
            QueryResourceHandler handler, RequestHandler next) {

        AuditState state = new AuditState(request);
        Promise<QueryResponse, ResourceException> promise = next.handleQuery(context, request, handler);
        logAuditAccessEntry(context, state, promise);

        return promise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
            RequestHandler next) {

        AuditState state = new AuditState(request);
        Promise<ResourceResponse, ResourceException> promise = next.handleRead(context, request);
        logAuditAccessEntry(context, state, promise);

        return promise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
            RequestHandler next) {

        AuditState state = new AuditState(request);
        Promise<ResourceResponse, ResourceException> promise = next.handleUpdate(context, request);
        logAuditAccessEntry(context, state, promise);

        return promise;
    }

    private void logAuditAccessEntry(final Context context, final AuditState state,
            final Promise<? extends Response, ResourceException> promise) {

        if (!ContextUtil.isExternal(context)) {
            // don't log internal requests
            return;
        }


        final AccessAuditEventBuilder accessAuditEventBuilder = new AccessAuditEventBuilder();
        accessAuditEventBuilder.forHttpCrestRequest(context, state.request)
                .authorizationIdFromSecurityContext(context)
                // TODO CAUD-114 .serverFromHttpContext(context)
                .resourceOperationFromRequest(state.request)
                .clientFromHttpContext(context)
                .transactionIdFromRootContext(context)
                .authenticationFromSecurityContext(context)
                .eventName("access");

        promise.thenOnResultOrException(
                new ResultHandler<Response>() {
                    @Override
                    public void handleResult(Response result) {
                        long now = System.currentTimeMillis();
                        final long elapsedTime = now - state.actionTime;
                        accessAuditEventBuilder.response(SUCCESS, null, elapsedTime, MILLISECONDS).timestamp(now);
                    }
                },
                new ExceptionHandler<ResourceException>() {
                    @Override
                    public void handleException(ResourceException resourceException) {
                        long now = System.currentTimeMillis();
                        final long elapsedTime = now - state.actionTime;
                        accessAuditEventBuilder.responseWithDetail(FAILURE, String.valueOf(resourceException.getCode()),
                                elapsedTime, MILLISECONDS, resourceException.getReason()).timestamp(now);
                    }
                })
                .thenAlways(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //log the log entry
                            final CreateRequest createRequest = Requests.newCreateRequest("audit/access",
                                    accessAuditEventBuilder.toEvent().getValue());

                            //wrap the context in a new internal context since we are using the external connection
                            // factory
                            connectionFactory.getConnection().create(newInternalClientContext(context), createRequest);
                        } catch (ResourceException e) {
                            LOGGER.error("Failed to log audit access entry", e);
                        }
                    }
                });
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
}
