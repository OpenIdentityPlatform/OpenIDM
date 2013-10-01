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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.jaspi.container.AuditLogger;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterBuilder;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
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
public class IDMAuthenticationAuditLogger implements AuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDMAuthenticationAuditLogger.class);

    private static final DateUtil DATE_UTIL = DateUtil.getDateUtil("UTC");
    public static final String LOG_CLIENT_IP_HEADER_KEY = "logClientIPHeader";

    /**
     * {@inheritDoc}
     */
    @Override
    public void audit(MessageInfo messageInfo) {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        Map<String, Object> map = (Map<String, Object>) messageInfo.getMap()
                .get(IDMServerAuthModule.CONTEXT_REQUEST_KEY);
        String username = (String) map.get(IDMServerAuthModule.USERNAME_ATTRIBUTE);
        String userId = (String) map.get(IDMServerAuthModule.USERID_ATTRIBUTE);
        List<String> roles = (List<String>) map.get(IDMServerAuthModule.ROLES_ATTRIBUTE);
        boolean status = (Boolean) map.get(IDMServerAuthModule.OPENIDM_AUTH_STATUS);
        String logClientIPHeader = (String) map.get(LOG_CLIENT_IP_HEADER_KEY);
        logAuthRequest(request, username, userId, roles, status, logClientIPHeader);
    }

    /**
     * Gets the Router used for logging.
     *
     * @return The instance of the Router.
     */
    private ObjectSet getRouter() {
        return OSGiAuthnFilterBuilder.getRouter();
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
            Map<String, Object> entry = new HashMap<String, Object>();
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
            if (getRouter() != null) {
                getRouter().create("audit/access", entry);
            } else {
                // Filter should have rejected request if router is not available
                LOGGER.warn("Failed to log entry for {} as router is null.", username);
            }
        } catch (ObjectSetException ose) {
            LOGGER.warn("Failed to log entry for {}", username, ose);
        }
    }
}
