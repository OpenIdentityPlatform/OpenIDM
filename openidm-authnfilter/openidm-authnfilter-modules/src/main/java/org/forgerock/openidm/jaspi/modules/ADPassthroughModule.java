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
import org.forgerock.openidm.audit.util.Status;
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
     * Constructor used by the commons Authentication Filter framework to create an instance of this authentication
     * module.
     */
    public ADPassthroughModule() {
    }

    /**
     * Constructor used by tests to inject dependencies.
     *
     * @param adPassthroughAuthenticator A mock of an ADPassthroughAuthenticator instance.
     */
    public ADPassthroughModule(ADPassthroughAuthenticator adPassthroughAuthenticator) {
        this.adPassthroughAuthenticator = adPassthroughAuthenticator;
    }

    /**
     * Initialises the AD Passthrough authentication module with the OSGi json configuration.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param options {@inheritDoc}
     */
    @Override
    protected void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
            JsonValue options) {

        JsonValue config = new JsonValue(options);

        List<String> defaultRoles = config.get("defaultUserRoles").asList(String.class);
        String passThroughAuth = config.get("passThroughAuth").asString();

        // User properties - default to NULL if not defined
        JsonValue properties = config.get("propertyMapping");
        String userRolesProperty = properties.get("userRoles").asString();

        adPassthroughAuthenticator = new ADPassthroughAuthenticator(passThroughAuth, userRolesProperty, defaultRoles);

    }

    /**
     * Validates the client's request by passing through the request to be authenticated against AD.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject,
            AuthData authData) {

        LOGGER.debug("ADPassthroughModule: validateRequest START");

        HttpServletRequest request = (HttpServletRequest)messageInfo.getRequestMessage();

        try {
            LOGGER.debug("ADPassthroughModule: Delegating call to internal AuthFilter");

            String username = request.getHeader("X-OpenIDM-Username");
            String password = request.getHeader("X-OpenIDM-Password");

            if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
                LOGGER.debug("Failed authentication, missing or empty headers");
                //Auth failure will be logged in IDMServerAuthModule super type.
                return AuthStatus.SEND_FAILURE;
            }

            authData.setUsername(username);
            boolean authenticated = adPassthroughAuthenticator.authenticate(authData, password);

            if (authenticated) {
                LOGGER.debug("ADPassthroughModule: Authentication successful");
                LOGGER.debug("Found valid session for {} id {} with roles {}", authData.getUsername(),
                        authData.getUserId(), authData.getRoles());

                //Auth success will be logged in IDMServerAuthModule super type.
                return AuthStatus.SUCCESS;
            } else {
                LOGGER.debug("ADPassthroughModule: Authentication failed");
                //Auth failure will be logged in IDMServerAuthModule super type.
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
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) {
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
