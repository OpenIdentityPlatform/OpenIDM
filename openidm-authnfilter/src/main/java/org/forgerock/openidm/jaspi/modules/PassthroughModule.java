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
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Authentication Filter modules for the JASPI common Authentication Filter. Validates client requests by passing though
 * to a OpenICF Connector.
 *
 * @author Phill Cunnington
 */
public class PassthroughModule extends IDMServerAuthModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(PassthroughModule.class);

    private static String passThroughAuth;
    private static JsonValue propertyMapping;

    private PassthroughAuthenticator passthroughAuthenticator;

    /**
     * Constructor used by the commons Authentication Filter framework to create an instance of this authentication
     * module.
     */
    public PassthroughModule() {
    }

    /**
     * Constructor used by tests to inject dependencies.
     *
     * @param passthroughAuthenticator A mock of an PassthroughAuthenticator instance.
     */
    public PassthroughModule(PassthroughAuthenticator passthroughAuthenticator) {
        this.passthroughAuthenticator = passthroughAuthenticator;
    }

    /**
     * Initialises the Passthrough authentication module with the OSGi json configuration.
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
        passThroughAuth = config.get("passThroughAuth").asString();

        // User properties - default to NULL if not defined
        propertyMapping = config.get("propertyMapping");

        passthroughAuthenticator = new PassthroughAuthenticator(passThroughAuth, propertyMapping, defaultRoles);
    }

    /**
     * Set pass through auth resource in context map on request so can be accessed by authnPopulateContext.js script.
     *
     * @param messageInfo The MessageInfo.
     */
    @SuppressWarnings("unchecked")
    void setPassThroughAuthOnRequest(MessageInfo messageInfo) {
        Map<String, Object> contextMap = (Map<String, Object>) messageInfo.getMap()
                .get(IDMServerAuthModule.CONTEXT_REQUEST_KEY);
        contextMap.put("passThroughAuth", passThroughAuth);
        if (propertyMapping != null) {
            contextMap.put("propertyMapping", propertyMapping.getObject());
        }
    }

    /**
     * Validates the client's request by passing through the request to be authenticated against a OpenICF Connector.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @param authData {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException If there is a problem performing the authentication.
     */
    @Override
    protected AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject,
            AuthData authData) throws AuthException {

        LOGGER.debug("PassthroughModule: validateRequest START");

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        try {
            LOGGER.debug("PassthroughModule: Delegating call to internal AuthFilter");
            //Set pass through auth resource on request so can be accessed by authnPopulateContext.js script.
            setPassThroughAuthOnRequest(messageInfo);

            final String username = request.getHeader("X-OpenIDM-Username");
            String password = request.getHeader("X-OpenIDM-Password");

            if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
                LOGGER.debug("Failed authentication, missing or empty headers");
                //Auth failure will be logged in IDMServerAuthModule super type.
                return AuthStatus.SEND_FAILURE;
            }

            authData.setUsername(username);
            clientSubject.getPrincipals().add(new Principal() {
                public String getName() {
                    return username;
                }
            });
            boolean authenticated = passthroughAuthenticator.authenticate(authData, password);

            if (authenticated) {
                LOGGER.debug("PassthroughModule: Authentication successful");
                LOGGER.debug("Found valid session for {} id {} with roles {}", authData.getUsername(),
                        authData.getUserId(), authData.getRoles());

                //Auth success will be logged in IDMServerAuthModule super type.
                return AuthStatus.SUCCESS;
            } else {
                LOGGER.debug("PassthroughModule: Authentication failed");
                //Auth failure will be logged in IDMServerAuthModule super type.
                return AuthStatus.SEND_FAILURE;
            }
        } finally {
            LOGGER.debug("PassthroughModule: validateRequest END");
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
        return super.secureResponse(messageInfo, serviceSubject);
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
