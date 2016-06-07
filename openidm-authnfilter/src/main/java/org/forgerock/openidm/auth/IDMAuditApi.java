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

package org.forgerock.openidm.auth;

import static org.forgerock.audit.events.AuthenticationAuditEventBuilder.CONTEXT;
import static org.forgerock.audit.events.AuthenticationAuditEventBuilder.ENTRIES;
import static org.forgerock.audit.events.AuthenticationAuditEventBuilder.PRINCIPAL;
import static org.forgerock.audit.events.AuthenticationAuditEventBuilder.RESULT;
import static org.forgerock.audit.events.AuthenticationAuditEventBuilder.authenticationEvent;
import static org.forgerock.json.JsonValueFunctions.enumConstant;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.audit.events.AuthenticationAuditEventBuilder.Status;
import org.forgerock.caf.authentication.framework.AuditApi;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.auth.modules.IDMAuthModule;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.generator.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates audit entries for each authentication attempt.
 */
public class IDMAuditApi implements AuditApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDMAuditApi.class);
    public static final String SESSION_ID = "sessionId";
    public static final String REQUEST_ID = "requestId";
    public static final String TRANSACTION_ID = "transactionId";

    // Copy of CAF audit message keys defined in AuditTrail, but not exposed
    // TODO they should be public in CAF
    private static final String ENTRIES_KEY = "entries";
    private static final String RESULT_KEY = "result";
    private static final String MODULE_ID_KEY = "moduleId";
    private static final String SUCCESSFUL_RESULT = "SUCCESSFUL";

    private final ConnectionFactory connectionFactory;

    IDMAuditApi(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void audit(JsonValue auditMessage) {
        for (JsonValue entry : auditMessage.get(ENTRIES_KEY)) {
            if (SUCCESSFUL_RESULT.equals(entry.get(RESULT_KEY).asString())
                    && IDMAuthModule.STATIC_USER.name().equals(entry.get(MODULE_ID_KEY).asString())) {
                return;
            }
        }

        List<String> principals = auditMessage.get(PRINCIPAL).asList(String.class);
        String username = "";
        if (principals != null && !principals.isEmpty()) {
            username = principals.get(0);
        }

        final Set<String> trackingIds = new HashSet<>();
        if (auditMessage.isDefined(REQUEST_ID)) {
            trackingIds.add(auditMessage.get(REQUEST_ID).asString());
        }
        if (auditMessage.isDefined(SESSION_ID)) {
            trackingIds.add(auditMessage.get(SESSION_ID).asString());
        }

        AuditEvent auditEvent = authenticationEvent()
                .context(auditMessage.get(CONTEXT).asMap())
                .entries(auditMessage.get(ENTRIES).asList())
                .principal(principals)
                .result(auditMessage.get(RESULT).as(enumConstant(Status.class)))
                .userId(username)
                .trackingIds(trackingIds)
                .transactionId(getTransactionId(auditMessage))
                .timestamp(System.currentTimeMillis())
                .eventName("authentication")
                .toEvent();

        try {
            final CreateRequest createRequest = Requests.newCreateRequest("audit/authentication", auditEvent.getValue());
            final Context context = ContextUtil.createInternalContext();
            connectionFactory.getConnection().create(context, createRequest);
        } catch (ResourceException e) {
            LOGGER.warn("Failed to log entry for {}", username, e);
        }
    }

    private String getTransactionId(JsonValue auditMessage) {
        final JsonValue transactionId = auditMessage.get(TRANSACTION_ID);
        if (transactionId.isNotNull()) {
            return transactionId.asString();
        } else {
            return IdGenerator.DEFAULT.generate();
        }
    }
}
