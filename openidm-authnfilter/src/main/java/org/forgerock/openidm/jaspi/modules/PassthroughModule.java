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

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.HEADER_PASSWORD;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.HEADER_USERNAME;

/**
 * Authentication Filter modules for the JASPI common Authentication Filter. Validates client requests by passing though
 * to a OpenICF Connector.
 *
 * @author Phill Cunnington
 * @author brmiller
 */
public class PassthroughModule implements ServerAuthModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(PassthroughModule.class);

    // config properties
    private static final String QUERY_ON_RESOURCE = "queryOnResource";

    private static String queryOnResource;

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
    PassthroughModule(PassthroughAuthenticator passthroughAuthenticator) {
        this.passthroughAuthenticator = passthroughAuthenticator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    /**
     * Initialises the Passthrough authentication module with the OSGi json configuration.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
            Map options) {

        queryOnResource = new JsonValue(options).get(QUERY_ON_RESOURCE).asString();

        try {
            passthroughAuthenticator = new PassthroughAuthenticator(
                    OSGiAuthnFilterBuilder.getConnectionFactory(),
                    OSGiAuthnFilterBuilder.getRouter().createServerContext(),
                    queryOnResource
            );
        } catch (ResourceException e) {
            //TODO
            e.printStackTrace();
        }
    }

    /**
     * Validates the client's request by passing through the request to be authenticated against a OpenICF Connector.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException If there is a problem performing the authentication.
     */
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        LOGGER.debug("PassthroughModule: validateRequest START");

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        try {
            LOGGER.debug("PassthroughModule: Delegating call to internal AuthFilter");

            final String username = request.getHeader(HEADER_USERNAME);
            String password = request.getHeader(HEADER_PASSWORD);

            if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
                LOGGER.debug("Failed authentication, missing or empty headers");
                //Auth failure will be logged in IDMServerAuthModule super type.
                return AuthStatus.SEND_FAILURE;
            }

            boolean authenticated = passthroughAuthenticator.authenticate(username, password,
                    SecurityContextMapper.fromMessageInfo(username, messageInfo));

            if (authenticated) {
                LOGGER.debug("PassthroughModule: Authentication successful");
                List<String> roles = SecurityContextMapper.fromMessageInfo(username, messageInfo).getRoles();
                LOGGER.debug("Found valid session for {} with roles {}", username,
                        roles);

                clientSubject.getPrincipals().add(new Principal() {
                    public String getName() {
                        return username;
                    }
                });

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
