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

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.audit.util.ResourceExceptionsUtil;
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.promise.Promise;
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
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest actionRequest) {
        return newExceptionPromise(ResourceExceptionsUtil.notSupported(actionRequest));
    }

    /**
     * Perform an action on the audit log entry.
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest actionRequest) {
        return newExceptionPromise(ResourceExceptionsUtil.notSupported(actionRequest));
    }

    /**
     * Create a audit log entry.
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest createRequest) {
        logger.info("Added an entry. Message: " + message);

        ResourceResponse resourceResponse = newResourceResponse(
                createRequest.getContent().get(ResourceResponse.FIELD_CONTENT_ID).asString(), null,
                new JsonValue(createRequest.getContent()));
        return newResultPromise(resourceResponse);
    }

    /**
     * Perform a query on the audit log.
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest queryRequest,
            QueryResourceHandler queryResourceHandler) {
        return newExceptionPromise(ResourceExceptionsUtil.notSupported(queryRequest));
    }

    /**
     * Read from the audit log.
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest readRequest) {
        return newExceptionPromise(ResourceExceptionsUtil.notSupported(readRequest));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PassThroughAuditEventHandlerConfiguration> getConfigurationClass() {
        return PassThroughAuditEventHandlerConfiguration.class;
    }

}
