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

import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
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
     * {@inheritDoc}
     */
    @Override
    public Class<PassThroughAuditEventHandlerConfiguration> getConfigurationClass() {
        return PassThroughAuditEventHandlerConfiguration.class;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(final Context context,
            final String auditEventTopic, final JsonValue auditEventContent) {
        return newResourceResponse(
                auditEventContent.get(ResourceResponse.FIELD_CONTENT_ID).asString(),
                null,
                auditEventContent).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(final Context context, final String auditEventTopic,
            final String auditEventId) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(final Context context, final String auditEventTopic,
            final QueryRequest queryRequest, final QueryResourceHandler queryResourceHandler) {
        return new NotSupportedException().asPromise();
    }

}
