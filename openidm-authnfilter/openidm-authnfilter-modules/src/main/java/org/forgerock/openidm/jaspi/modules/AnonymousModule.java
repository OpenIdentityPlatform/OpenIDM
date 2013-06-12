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

import org.forgerock.json.fluent.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public class AnonymousModule extends IDMServerAuthModule {

    private final static Logger logger = LoggerFactory.getLogger(AnonymousModule.class);

    private final String internalUserQueryId = "credential-internaluser-query";
    private final String queryOnInternalUserResource = "internal/user";

    private AnonymousAuthenticator anonymousAuthenticator;

    /**
     * Required default constructor of OSGi to instantiate.
     */
    public AnonymousModule() {
    }

    /**
     * Initialises the AnonymousModule with the OSGi json configuration.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param options {@inheritDoc}
     * @throws javax.security.auth.message.AuthException {@inheritDoc}
     */
    @Override
    protected void doInitialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
            Map options) throws AuthException {

        JsonValue config = new JsonValue(options);

        List<String> defaultRoles = config.get("defaultUserRoles").asList(String.class);

        // User properties - default to NULL if not defined
        JsonValue properties = config.get("propertyMapping");
        String userIdProperty = properties.get("userId").asString();
        String userCredentialProperty = properties.get("userCredential").asString();
        String userRolesProperty = properties.get("userRoles").asString();

        anonymousAuthenticator = new AnonymousAuthenticator(userRolesProperty, defaultRoles, userIdProperty, userCredentialProperty);
    }

    /**
     *
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    protected AuthStatus doValidateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject, AuthData authData)
            throws AuthException {

        HttpServletRequest request = (HttpServletRequest)messageInfo.getRequestMessage();

        try {
            String password = request.getHeader("X-OpenIDM-password");
            authData.username = request.getHeader("X-OpenIDM-username");

            authData.status = anonymousAuthenticator.authenticate(internalUserQueryId, queryOnInternalUserResource, authData.username, password, authData);
            authData.resource = queryOnInternalUserResource;

            if (authData.status) {
                logger.debug("ADPassthroughModule: Authentication successful");
                logger.debug("Found valid session for {} id {} with roles {}", authData.username, authData.userId, authData.roles);

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
     *
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    protected AuthStatus doSecureResponse(MessageInfo messageInfo, Subject subject) throws AuthException {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        String xOpenIDMUsername = request.getHeader("X-OpenIDM-username");

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
     * @throws AuthException {@inheritDoc}
     */
    @Override
    protected void doCleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
    }
}
