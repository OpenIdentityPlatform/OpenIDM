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
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.openidm.shell.impl;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.apache.async.AsyncHttpClientProvider;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.header.GenericHeader;
import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.spi.Loader;
import org.forgerock.json.resource.AbstractAsynchronousConnection;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.services.context.Context;
import org.forgerock.util.Options;
import org.forgerock.util.Reject;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Connection} to the remote OpenIDM instance.
 */
public class HttpRemoteJsonResource extends AbstractAsynchronousConnection {

    private static final Logger logger = LoggerFactory.getLogger(HttpRemoteJsonResource.class);

    private final HttpClientHandler httpClientHandler;

    private final RequestHandler client;

    private volatile boolean closed;

    /**
     * Initializes {@code HttpRemoteJsonResource}'s HTTP client.
     *
     * @param uri Base URI of this resource.
     * @param port Override port in {@code baseUri} or {@code null} if not required.
     * @param username Username for HTTP basic authentication or {@code null} if not required.
     * @param password Password for HTTP basic authentication or {@code null} if not required.
     * @throws URISyntaxException Malformed {@code uri}
     */
    public HttpRemoteJsonResource(final String uri, final Integer port, final String username, final String password)
            throws URISyntaxException {

        final URIBuilder uriBuilder = new URIBuilder(Reject.checkNotNull(uri, "uri required"));
        if (port != null) {
            uriBuilder.setPort(port);
        }

        httpClientHandler = newHttpClientHandler();
        final Handler requestHandler;
        if (username != null && password != null) {
            // apply basic-auth header to all client requests
            final String credentials = username + ':' + password;
            final Header authHeader = new GenericHeader(
                    "Authorization", "Basic " + Base64.encode(credentials.getBytes()));
            requestHandler = Handlers.chainOf(httpClientHandler, new Filter() {
                @Override
                public Promise<Response, NeverThrowsException> filter(final Context context,
                        final org.forgerock.http.protocol.Request request, final Handler next) {
                    request.getHeaders().add(authHeader);
                    return next.handle(context, request);
                }
            });
        } else {
            requestHandler = httpClientHandler;
        }

        client = CrestHttp.newRequestHandler(requestHandler, uriBuilder.build());
    }

    /**
     * Closes the underlying {@link HttpClientHandler}, closing the connection pool, and as such should only be called
     * on shutdown.
     */
    @Override
    public void close() {
        if (!closed && httpClientHandler != null) {
            try {
                httpClientHandler.close();
            } catch (IOException e) {
                logger.warn("Error while closing HTTP Client Handler", e);
            } finally {
                closed = true;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return !closed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionAsync(final Context context, final ActionRequest request) {
        return client.handleAction(context, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createAsync(final Context context,
            final CreateRequest request) {
        return client.handleCreate(context, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteAsync(final Context context,
            final DeleteRequest request) {
        return client.handleDelete(context, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchAsync(final Context context, final PatchRequest request) {
        return client.handlePatch(context, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryAsync(final Context context, final QueryRequest request,
            QueryResourceHandler handler) {
        return client.handleQuery(context, request, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readAsync(final Context context, final ReadRequest request) {
        return client.handleRead(context, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateAsync(final Context context,
            final UpdateRequest request) {
        return client.handleUpdate(context, request);
    }

    /**
     * Builds an {@link AsyncHttpClientProvider} instance, which must be closed on shutdown.
     *
     * @return {@link AsyncHttpClientProvider} instance
     */
    private static HttpClientHandler newHttpClientHandler() {
        try {
            return new HttpClientHandler(
                    // this client is currently used in a "synchronous" manner, so configure with minimal resources
                    Options.defaultOptions()
                            .set(HttpClientHandler.OPTION_RETRY_REQUESTS, true)
                            .set(HttpClientHandler.OPTION_REUSE_CONNECTIONS, false)
                            .set(HttpClientHandler.OPTION_MAX_CONNECTIONS, 1)
                            .set(AsyncHttpClientProvider.OPTION_WORKER_THREADS, 1)
                            .set(HttpClientHandler.OPTION_LOADER, new Loader() {
                                @Override
                                public <S> S load(Class<S> service, Options options) {
                                    return service.cast(new AsyncHttpClientProvider());
                                }
                            }));
        } catch (HttpApplicationException e) {
            throw new RuntimeException("Error while building HTTP Client Handler", e);
        }
    }

}
