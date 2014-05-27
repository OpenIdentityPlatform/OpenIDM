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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.auth.common.AuditRecord;
import org.forgerock.auth.common.AuthResult;
import org.forgerock.jaspi.logging.JaspiAuditLogger;
import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.servlet.SecurityContextFactory;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterHelper;
import org.forgerock.openidm.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates audit entries for each authentication attempt.
 *
 * @author Phill Cunnington
 */
public class IDMAuthenticationAuditLogger implements JaspiAuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDMAuthenticationAuditLogger.class);

    private static final DateUtil DATE_UTIL = DateUtil.getDateUtil("UTC");
    public static final String LOG_CLIENT_IP_HEADER_KEY = "logClientIPHeader";

    private final OSGiAuthnFilterHelper authnFilterHelper;

    public IDMAuthenticationAuditLogger(OSGiAuthnFilterHelper authnFilterHelper) {
        this.authnFilterHelper = authnFilterHelper;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void audit(AuditRecord<MessageInfo> auditRecord) {

        MessageInfo messageInfo = auditRecord.getAuditObject();

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        Map<String, Object> messageInfoParams = messageInfo.getMap();
        Map<String, Object> map = (Map<String, Object>) messageInfoParams.get(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT);

        String username = (String) messageInfoParams.get(SecurityContextFactory.ATTRIBUTE_AUTHCID);
        String userId = (String) map.get(SecurityContext.AUTHZID_ID);
        List<String> roles = (List<String>) map.get(SecurityContext.AUTHZID_ROLES);

        boolean status = AuthResult.SUCCESS.equals(auditRecord.getAuthResult());
        String logClientIPHeader = (String) messageInfoParams.get(LOG_CLIENT_IP_HEADER_KEY);
        logAuthRequest(request, username, userId, roles, status, logClientIPHeader);
    }

    /**
     * Logs the authentication request.
     *
     * @param request The HttpServletRequest that made the authentication request.
     * @param username The username of the user that made the authentication request.
     * @param userId The user id of the user that made the authentication request.
     * @param roles The roles of the user that made the authentication request.
     * @param status The status of the authentication request, either true for success or false for failure.
     * @param logClientIPHeader Whether to log the client IP in the header.
     */
    protected void logAuthRequest(HttpServletRequest request, String username, String userId, List<String> roles,
            boolean status, String logClientIPHeader) {
        try {
            JsonValue entry = new JsonValue(new HashMap<String, Object>());
            entry.put("timestamp", DATE_UTIL.now());
            entry.put("action", "authenticate");
            entry.put("status", status ? Status.SUCCESS.toString() : Status.FAILURE.toString());
            entry.put("principal", username);
            entry.put("userid", userId);
            entry.put("roles", roles);
            // check for header sent by load balancer for IPAddr of the client
            String ipAddress;
            if (logClientIPHeader == null) {
                ipAddress = request.getRemoteAddr();
            } else {
                ipAddress = request.getHeader(logClientIPHeader);
                if (ipAddress == null) {
                    ipAddress = request.getRemoteAddr();
                }
            }
            entry.put("ip", ipAddress);
            if (authnFilterHelper.getRouter() != null) {
                // TODO We need Context!!!
                CreateRequest createRequest = Requests.newCreateRequest("/audit/access", entry);
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
