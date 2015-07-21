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

package org.forgerock.openidm.audit.events.handlers.impl;

import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.util.ResourceExceptionsUtil;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles AuditEvents by just calling the result Handler.
 */
public class PassThroughAuditEventHandler extends AuditEventHandlerBase<PassThroughAuditEventHandlerConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(PassThroughAuditEventHandlerConfiguration.class);

    /** A message logged when a new entry is added. */
    private String message;

    /** {@inheritDoc} */
    @Override
    public void configure(PassThroughAuditEventHandlerConfiguration config) throws ResourceException {
        this.message = config.getMessage();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws ResourceException {
        // nothing to do
    }

    /**
     * Perform an action on the audit log.
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(
            final ServerContext context,
            final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * Perform an action on the audit log entry.
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(
            final ServerContext context,
            final String resourceId,
            final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * Create a audit log entry.
     * {@inheritDoc}
     */
    @Override
    public void createInstance(
            final ServerContext context,
            final CreateRequest request,
            final ResultHandler<Resource> handler) {
        logger.info("Added an entry. Message: " + message);
        handler.handleResult(new Resource(request.getContent().get(Resource.FIELD_CONTENT_ID).asString(),
                null,
                new JsonValue(request.getContent())));
    }

    /**
     * Perform a query on the audit log.
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(
            final ServerContext context,
            final QueryRequest request,
            final QueryResultHandler handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }

    /**
     * Read from the audit log.
     * {@inheritDoc}
     */
    @Override
    public void readInstance(
            final ServerContext context,
            final String resourceId,
            final ReadRequest request,
            final ResultHandler<Resource> handler) {
        handler.handleError(ResourceExceptionsUtil.notSupported(request));
    }

}
