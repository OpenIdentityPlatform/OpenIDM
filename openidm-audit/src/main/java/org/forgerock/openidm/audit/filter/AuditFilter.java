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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openidm.audit.filter;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.FAILED;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.SUCCESSFUL;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.services.context.ClientContext.newInternalClientContext;
import static org.forgerock.util.promise.Promises.newResultPromise;

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
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Response;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A router {@link Filter} to log external CREST requests as access events.
 */
public class AuditFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuditFilter.class);
    private ConnectionFactory connectionFactory;

    public AuditFilter(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> filterAction(final Context context, final ActionRequest request,
            final RequestHandler next) {

        return logAuditAccessEntry(context, request,
                new AsyncFunction<Void, ActionResponse, ResourceException>() {
                    @Override
                    public Promise<ActionResponse, ResourceException> apply(Void v) {
                        return next.handleAction(context, request);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(final Context context, final CreateRequest request,
            final RequestHandler next) {

        return logAuditAccessEntry(context, request,
                new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(Void v) {
                        return next.handleCreate(context, request);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(final Context context, final DeleteRequest request,
            final RequestHandler next) {

        return logAuditAccessEntry(context, request,
                new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(Void v) {
                        return next.handleDelete(context, request);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(final Context context, final PatchRequest request,
            final RequestHandler next) {

        return logAuditAccessEntry(context, request,
                new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(Void v) {
                        return next.handlePatch(context, request);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(final Context context, final QueryRequest request,
            final QueryResourceHandler handler, final RequestHandler next) {

        return logAuditAccessEntry(context, request,
                new AsyncFunction<Void, QueryResponse, ResourceException>() {
                    @Override
                    public Promise<QueryResponse, ResourceException> apply(Void v) {
                        return next.handleQuery(context, request, handler);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(final Context context, final ReadRequest request,
            final RequestHandler next) {

        return logAuditAccessEntry(context, request,
                new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(Void v) {
                        return next.handleRead(context, request);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(final Context context, final UpdateRequest request,
            final RequestHandler next) {

        return logAuditAccessEntry(context, request,
                new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(Void v) {
                        return next.handleUpdate(context, request);
                    }
                });
    }

    /** Promise to which to chain next RequestHandler AsyncFunction */
    private static final Promise<Void, ResourceException> VOID_PROMISE = newResultPromise(null);

    /**
     * Handle the request, and (optionally) create the access audit entry and send it to the audit
     * service over the router.
     *
     * @param context the request context
     * @param request the request
     * @param handleRequest an {@link AsyncFunction} that encapsulates handling the request
     * @param <R> the type of Response
     * @return a Promise of either the request response or exception
     */
    <R extends Response> Promise<R, ResourceException> logAuditAccessEntry(
            final Context context, final Request request,
            final AsyncFunction<Void, R, ResourceException> handleRequest) {

        final long actionTime = System.currentTimeMillis();
        // use a void promise to kick off the request handling to avoid catching a ResourceException
        final Promise<R, ResourceException> promise = VOID_PROMISE.thenAsync(handleRequest);

        // only log external requests
        if (ContextUtil.isExternal(context)) {
            final OpenIDMAccessAuditEventBuilder accessAuditEventBuilder =
                    new OpenIDMAccessAuditEventBuilder(context, request);

            promise.thenOnResultOrException(
                    new ResultHandler<Response>() {
                        @Override
                        public void handleResult(Response result) {
                            long now = System.currentTimeMillis();
                            final long elapsedTime = now - actionTime;
                            accessAuditEventBuilder
                                    .response(SUCCESSFUL, null, elapsedTime, MILLISECONDS)
                                    .timestamp(now);
                        }
                    },
                    new ExceptionHandler<ResourceException>() {
                        @Override
                        public void handleException(ResourceException resourceException) {
                            long now = System.currentTimeMillis();
                            final long elapsedTime = now - actionTime;
                            accessAuditEventBuilder
                                    .responseWithDetail(FAILED, String.valueOf(resourceException.getCode()),
                                            elapsedTime, MILLISECONDS, resourceException.toJsonValue())
                                    .timestamp(now);
                        }
                    })
                    .thenAlways(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // wrap the context in a new internal context since we are using the external connection
                                // factory
                                connectionFactory.getConnection().create(
                                        newInternalClientContext(context),
                                        newCreateRequest("audit/access", accessAuditEventBuilder.toEvent().getValue()));
                            } catch (ResourceException e) {
                                logger.error("Failed to log audit access entry", e);
                            }
                        }
                    });
        }

        return promise;
    }

    /**
     * Extended Commons Audit Event Builder that handles the extended attributes of OpenIDM.
     */
    @SuppressWarnings("unchecked")
    private class OpenIDMAccessAuditEventBuilder<T extends OpenIDMAccessAuditEventBuilder<T>>
            extends AccessAuditEventBuilder<T> {

        static final String ROLES = "roles";
        static final String ACCESS = "access";

        OpenIDMAccessAuditEventBuilder(Context context, Request request) {
            super();
            rolesFromCrestContext(context);
            forHttpRequest(context, request);
            requestFromCrestRequest(request);
            clientFromContext(context);
            httpFromContext(context);
            transactionIdFromContext(context);
            eventName(ACCESS);
            userId(getUserId(context));
        }

        private String getUserId(Context context) {
            return context.containsContext(SecurityContext.class)
                ? context.asContext(SecurityContext.class).getAuthenticationId()
                : "";
        }

        private T rolesFromCrestContext(final Context context) {
            if (context.containsContext(SecurityContext.class)) {
                List<String> roles =
                        (List<String>) context.asContext(SecurityContext.class).getAuthorization().get("roles");
                Reject.ifNull(roles);
                jsonValue.put(ROLES, roles);
            }
            return self();
        }
    }
}
