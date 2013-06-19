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

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Commons Authentication Filter module to provide authentication for the anonymous user.
 */
public class InternalUserAuthModule extends IDMServerAuthModule {

    private final static Logger logger = LoggerFactory.getLogger(InternalUserAuthModule.class);

    private final String internalUserQueryId = "credential-internaluser-query";
    private final String queryOnInternalUserResource = "internal/user";

    private AuthHelper authHelper;

    /**
     * Constructor used by the commons Authentication Filter framework to create an instance of this authentication
     * module.
     */
    public InternalUserAuthModule() {
    }

    /**
     * Constructor used by tests to inject dependencies.
     *
     * @param authHelper A mock of the AuthHelper.
     */
    public InternalUserAuthModule(AuthHelper authHelper) {
        this.authHelper = authHelper;
    }

    /**
     * Initialises the InternalUserAuthModule with the OSGi json configuration.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param options {@inheritDoc}
     */
    @Override
    protected void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
            JsonValue options) {

        JsonValue properties = options.get("propertyMapping");
        String userIdProperty = properties.get("userId").asString();
        String userCredentialProperty = properties.get("userCredential").asString();
        String userRolesProperty = properties.get("userRoles").asString();
        List<String> defaultRoles = options.get("defaultUserRoles").asList(String.class);

        authHelper = new AuthHelper(userIdProperty, userCredentialProperty, userRolesProperty, defaultRoles);
    }

    /**
     * Authenticates the user as the anonymous user. If authentication is successful AuthStatus.SUCCESS is returned,
     * otherwise AuthStatus.SEND_FAILURE is returned.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject,
            AuthData authData) {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        try {
            String username = request.getHeader("X-OpenIDM-Username");
            String password = request.getHeader("X-OpenIDM-Password");

            if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
                logger.debug("Failed authentication, missing or empty headers");
                return AuthStatus.SEND_FAILURE;
            }

            authData.setUsername(username);
            boolean authSucceeded = authHelper.authenticate(internalUserQueryId, queryOnInternalUserResource,
                    username, password, authData);
            authData.setResource(queryOnInternalUserResource);

            if (authSucceeded) {
                logger.debug("ADPassthroughModule: Authentication successful");
                logger.debug("Found valid session for {} id {} with roles {}", authData.getUsername(),
                        authData.getUserId(), authData.getRoles());

                return AuthStatus.SUCCESS;
            } else {
                logger.debug("ADPassthroughModule: Authentication failed");
                return AuthStatus.SEND_FAILURE;
            }
        } finally {
            logger.debug("ADPassthroughModule: validateRequest END");
        }
    }

    /**
     * Always returns AuthStatus.SEND_SUCCESS but checks to see if the user that has been authenticated is the
     * anonymous user and if so sets skipSession to prevent the creation of a session by the Session Module,
     * (if it is configured in this case).
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        String xOpenIDMUsername = request.getHeader("X-OpenIDM-Username");

        if ("anonymous".equals(xOpenIDMUsername)) {
            messageInfo.getMap().put("skipSession", true);
        }

        return AuthStatus.SEND_SUCCESS;
    }

    /**
     * Nothing to clean up.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     */
    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) {
    }
}
