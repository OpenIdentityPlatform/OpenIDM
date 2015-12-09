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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.FAILED;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.SUCCESSFUL;
import static org.forgerock.services.context.ClientContext.newInternalClientContext;

import java.util.List;

import org.forgerock.audit.events.AccessAuditEventBuilder;
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
import org.forgerock.services.context.Context;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.util.Reject;
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


        final OpenIDMAccessAuditEventBuilder accessAuditEventBuilder = new OpenIDMAccessAuditEventBuilder();
        accessAuditEventBuilder
                .rolesFromCrestContext(context)
                .forHttpRequest(context, state.request)
                // TODO CAUD-114 .serverFromHttpContext(context)
                .requestFromCrestRequest(state.request)
                .clientFromContext(context)
                .httpFromContext(context)
                .transactionIdFromContext(context)
                .eventName("access")
                .userId(getUserId(context));

        promise.thenOnResultOrException(
                new ResultHandler<Response>() {
                    @Override
                    public void handleResult(Response result) {
                        long now = System.currentTimeMillis();
                        final long elapsedTime = now - state.actionTime;
                        accessAuditEventBuilder.response(SUCCESSFUL, null, elapsedTime, MILLISECONDS).timestamp(now);
                    }
                },
                new ExceptionHandler<ResourceException>() {
                    @Override
                    public void handleException(ResourceException resourceException) {
                        long now = System.currentTimeMillis();
                        final long elapsedTime = now - state.actionTime;
                        accessAuditEventBuilder.responseWithDetail(FAILED, String.valueOf(resourceException.getCode()),
                                elapsedTime, MILLISECONDS, resourceException.toJsonValue()).timestamp(now);
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

    private String getUserId(Context context) {
        if (context.containsContext(SecurityContext.class)) {
            return context.asContext(SecurityContext.class).getAuthenticationId();
        } else {
            return "";
        }
    }

    /**
     * Extended Commons Audit Event Builder that handles the extended attributes of OpenIdm.
     */
    @SuppressWarnings("unchecked")
    private class OpenIDMAccessAuditEventBuilder<T extends OpenIDMAccessAuditEventBuilder<T>>
            extends AccessAuditEventBuilder<T> {

        public static final String ROLES = "roles";

        public T rolesFromCrestContext(final Context context) {
            if (context.containsContext(SecurityContext.class)) {
                return roles((List<String>) context.asContext(SecurityContext.class).getAuthorization().get("roles"));
            }
            return self();
        }

        public T roles(List<String> roles) {
            Reject.ifNull(roles);
            jsonValue.put(ROLES, roles);
            return self();
        }
    }
}
