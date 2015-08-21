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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.config;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.audit.events.AuthenticationAuditEventBuilder;
import org.forgerock.jaspi.runtime.AuditApi;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Creates audit entries for each authentication attempt.
 */
public class JaspiAuditApi implements AuditApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaspiAuditApi.class);

    private final OSGiAuthnFilterHelper authnFilterHelper;

    public JaspiAuditApi() {
        this(OSGiAuthnFilterBuilder.getInstance());
    }

    JaspiAuditApi(OSGiAuthnFilterHelper authnFilterHelper) {
        this.authnFilterHelper = authnFilterHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void audit(JsonValue auditMessage) {
        List<String> principals = auditMessage.get(AuthenticationAuditEventBuilder.PRINCIPAL).asList(String.class);
        String username = "";
        if (principals != null && !principals.isEmpty()) {
            username = principals.get(0);
        }

        AuditEvent auditEvent = new AuthenticationAuditEventBuilder()
                .context(auditMessage.get(AuthenticationAuditEventBuilder.CONTEXT).asMap())
                .entries(auditMessage.get(AuthenticationAuditEventBuilder.ENTRIES).asList())
                .principal(principals)
                .sessionId(auditMessage.get(AuthenticationAuditEventBuilder.SESSION_ID).asString())
                .result(auditMessage.get(
                        AuthenticationAuditEventBuilder.RESULT).asEnum(AuthenticationAuditEventBuilder.Status.class))
                .authentication(username)
                .transactionId(UUID.randomUUID().toString()) //CAUDTODO Support transactionId OPENIDM-3427
                .timestamp(System.currentTimeMillis())
                .eventName("authentication")
                .toEvent();

        try {
            if (authnFilterHelper.getRouter() != null) {
                // TODO We need Context!!!
                CreateRequest createRequest = Requests.newCreateRequest("audit/authentication", auditEvent.getValue());
                Context ctx = authnFilterHelper.getRouter().createServerContext();
                authnFilterHelper.getConnectionFactory().getConnection().create(ctx, createRequest);
            } else {
                // Filter should have rejected request if router is not available
                LOGGER.warn("Failed to log entry for {} as router is null.", username);
            }
        } catch (ResourceException e) {
            LOGGER.warn("Failed to log entry for {}", username, e);
        }
    }
}
