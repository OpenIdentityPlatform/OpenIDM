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
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxies a read request over the {@link ConnectionFactory}.
 */
class RepoProxyRequestHandler extends AbstractRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(RepoProxyRequestHandler.class);

    /**
     * The resource path for waiting triggers.
     */
    static final String WAITING_TRIGGERS_RESOURCE_PATH = "/waitingTriggers";
    /**
     * The resource path for the acquired triggers.
     */
    static final String ACQUIRED_TRIGGERS_RESOURCE_PATH = "/acquiredTriggers";

    private final String resource;
    private final ConnectionFactory connectionFactory;

    /**
     * Constructs a {@link RepoProxyRequestHandler} given a resource location and a {@link ConnectionFactory}.
     * @param resource the resource location to send the requests.
     * @param connectionFactory the {@link ConnectionFactory} to send the requests over.
     */
    RepoProxyRequestHandler(final String resource, final ConnectionFactory connectionFactory) {
        this.resource = resource;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest readRequest) {
        final ReadRequest request = Requests.newReadRequest(resource);
        try {
            return connectionFactory.getConnection().readAsync(context, request);
        } catch (final ResourceException e) {
            logger.error("Query failed for location: {}", resource, e);
            return e.asPromise();
        }
    }
}
