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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.config;

import org.forgerock.jaspi.runtime.AuditApi;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

/**
 * Creates audit entries for each authentication attempt.
 */
public class JaspiAuditApi implements AuditApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaspiAuditApi.class);

    private static final DateUtil DATE_UTIL = DateUtil.getDateUtil("UTC");

    private final OSGiAuthnFilterHelper authnFilterHelper;

    public JaspiAuditApi() {
        this.authnFilterHelper = OSGiAuthnFilterBuilder.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void audit(JsonValue auditMessage) {
        List<String> principals = auditMessage.get("principal").asList(String.class);
        String username = "";
        if (principals != null && !principals.isEmpty()) {
            username = principals.get(0);
        }
        String userId = auditMessage.get("context").get("id").asString();
        List<String> roles = auditMessage.get("context").get("roles").asList(String.class);
        String ipAddress = auditMessage.get("context").get("ipAddress").asString();
        boolean status = "SUCCESSFUL".equalsIgnoreCase(auditMessage.get("result").asString());
        String requestId = auditMessage.get("requestId").asString();
        String sessionId = auditMessage.get("sessionId").asString();

        logAuthRequest(ipAddress, username, userId, roles, status, requestId, sessionId);
    }

    /**
     * Logs the authentication request.
     *
     * @param ipAddress The ip address of the client that made the authentication request.
     * @param username The username of the user that made the authentication request.
     * @param userId The user id of the user that made the authentication request.
     * @param roles The roles of the user that made the authentication request.
     * @param status The status of the authentication request, either true for success or false for failure.
     * @param requestId The unique ID of the request.
     * @param sessionId The unique ID of the session.
     */
    protected void logAuthRequest(String ipAddress, String username, String userId, List<String> roles,
            boolean status, String requestId, String sessionId) {
        try {
            JsonValue entry = new JsonValue(new HashMap<String, Object>());
            entry.put("timestamp", DATE_UTIL.now());
            entry.put("action", "authenticate");
            entry.put("status", status ? Status.SUCCESS.toString() : Status.FAILURE.toString());
            entry.put("principal", username);
            entry.put("userid", userId);
            entry.put("roles", roles);
            entry.put("ip", ipAddress);
            if (authnFilterHelper.getRouter() != null) {
                // TODO We need Context!!!
                CreateRequest createRequest = Requests.newCreateRequest("audit/access", entry);
                ServerContext ctx = authnFilterHelper.getRouter().createServerContext();
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
