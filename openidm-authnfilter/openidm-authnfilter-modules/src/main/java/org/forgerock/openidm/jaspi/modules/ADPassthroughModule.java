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

/**
 * Authentication Filter modules for the JASPI common Authentication Filter. Validates client requests by passing though
 * to Active Directory.
 */
public class ADPassthroughModule extends IDMServerAuthModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(ADPassthroughModule.class);

    private ADPassthroughAuthenticator adPassthroughAuthenticator;

    /**
     * Required default constructor of OSGi to instantiate.
     */
    public ADPassthroughModule() {
    }

    /**
     * For tests purposes.
     *
     * @param adPassthroughAuthenticator A mock of an ADPassthroughAuthenticator instance.
     */
    public ADPassthroughModule(ADPassthroughAuthenticator adPassthroughAuthenticator) {
        this.adPassthroughAuthenticator = adPassthroughAuthenticator;
    }

    /**
     * Initialises the ADPassthroughModule with the OSGi json configuration.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param options {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    protected void doInitialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, Map options) throws AuthException {

        JsonValue config = new JsonValue(options);

        List<String> defaultRoles = config.get("defaultUserRoles").asList(String.class);
        String passThroughAuth = config.get("passThroughAuth").asString();

        // User properties - default to NULL if not defined
        JsonValue properties = config.get("propertyMapping");
        String userRolesProperty = properties.get("userRoles").asString();

        adPassthroughAuthenticator = new ADPassthroughAuthenticator(passThroughAuth, userRolesProperty, defaultRoles);

    }

    /**
     * Validates the client's request by calling through to the existing AuthFilter.authenticate() method.
     * If the authenticate method return null, this indicates a logout and AuthStatus.SEND_SUCCESS will be returned.
     * If the authenticate method returns a valid UserWrapper object, this indicates a successful authentication and
     * AuthStatus.SUCCESS will be returned.
     * If the authenticate method throws an org.forgerock.openidm.filter.AuthException, this indicates an unsuccessful
     * authentication and AuthStatus.SEND_FAILURE will be returned.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    protected AuthStatus doValidateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject, AuthData authData) throws AuthException {

        LOGGER.debug("ADPassthroughModule: validateRequest START");

        HttpServletRequest request = (HttpServletRequest)messageInfo.getRequestMessage();

        try {
            LOGGER.debug("ADPassthroughModule: Delegating call to internal AuthFilter");
            String password = request.getHeader("X-OpenIDM-password");
            authData.username = request.getHeader("X-OpenIDM-username");
            if (authData.username == null || password == null || authData.username.equals("") || password.equals("")) {
                LOGGER.debug("Failed authentication, missing or empty headers");
                return AuthStatus.SEND_FAILURE;
            }
            authData = adPassthroughAuthenticator.authenticate(authData, password);
            if (authData.status) {
                LOGGER.debug("ADPassthroughModule: Authentication successful");
                LOGGER.debug("Found valid session for {} id {} with roles {}", authData.username, authData.userId, authData.roles);

                return AuthStatus.SUCCESS;
            } else {
                LOGGER.debug("ADPassthroughModule: Authentication failed");
                return AuthStatus.SEND_FAILURE;
            }
        } finally {
            LOGGER.debug("ADPassthroughModule: validateRequest END");
        }
    }

    /**
     * No work to do here so always returns AuthStatus.SEND_SUCCESS.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    protected AuthStatus doSecureResponse(MessageInfo messageInfo, Subject subject) throws AuthException {
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
