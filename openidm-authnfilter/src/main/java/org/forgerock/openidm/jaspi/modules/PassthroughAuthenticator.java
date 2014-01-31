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
 * Copyright 2013-2014 ForgeRock Inc.
 */

package org.forgerock.openidm.jaspi.modules;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.message.AuthException;
import java.util.Arrays;
import java.util.List;

/**
 * Contains logic to perform authentication by passing the request through to be authenticated against a OpenICF
 * connector.
 *
 * @author Phill Cunnington
 */
public class PassthroughAuthenticator {

    final static Logger logger = LoggerFactory.getLogger(PassthroughAuthenticator.class);

    private final ConnectionFactory connectionFactory;
    private final ServerContext context;
    private final String passThroughAuth;
    private final String userRolesProperty;
    private final List<String> defaultRoles;

    /**
     * Constructs an instance of the PassthroughAuthenticator.
     *
     * @param passThroughAuth The passThroughAuth resource.
     * @param userRolesProperty The user roles property.
     * @param defaultRoles The list of default roles.
     */
    public PassthroughAuthenticator(ConnectionFactory connectionFactory, ServerContext context, String passThroughAuth, String userRolesProperty, List<String> defaultRoles) {
        this.connectionFactory = connectionFactory;
        this.context = context;
        this.passThroughAuth = passThroughAuth;
        this.userRolesProperty = userRolesProperty;
        this.defaultRoles = defaultRoles;
    }

    /**
     * Performs the AD Passthrough authentication.
     *
     * @param username The user's username
     * @param password The user's password.
     * @param securityContextMapper The SecurityContextMapper object.
     * @return <code>true</code> if authentication is successful.
     * @throws AuthException if there is a problem whilst attempting to authenticate the user.
     */
    public boolean authenticate(String username, String password, SecurityContextMapper securityContextMapper)
            throws AuthException {

        if (!StringUtils.isEmpty(passThroughAuth) && !"anonymous".equals(username)) {
            ActionRequest actionRequest = Requests.newActionRequest(passThroughAuth, "authenticate");
            actionRequest.setAdditionalParameter("username", username);
            actionRequest.setAdditionalParameter("password", password);
            try {
                JsonValue result = connectionFactory.getConnection().action(context, actionRequest);
                boolean authenticated = result.isDefined(Resource.FIELD_CONTENT_ID);
                if (authenticated) {
                    // This is what I was talking about. We don't have a way to
                    // populate this. Use script to overcome it
                    securityContextMapper.setRoles(Arrays.asList("openidm-admin", "openidm-authorized"));
                    securityContextMapper.setResource(passThroughAuth);
                    securityContextMapper.setUserId(result.get(Resource.FIELD_CONTENT_ID).asString());
                    securityContextMapper.setUsername(username);
                    return true;
                }
            } catch (ResourceException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed pass-through authentication of {} on {}.", username, passThroughAuth, e);
                }
                if (e.isServerError()) { // HTTP server-side error
                    throw new AuthException("Failed pass-through authentication of " + username + " on "
                            + passThroughAuth + ".");
                }
                //authentication failed
                return false;
            }

            //TODO need to look at setting resource on authz and setting roles on authz, as well as what uses this to ensure security context is populated, like the old Servlet class used to do!
        }

        return false;
    }
}
