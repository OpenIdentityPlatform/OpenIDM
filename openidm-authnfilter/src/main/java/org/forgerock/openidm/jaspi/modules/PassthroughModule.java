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
import java.util.Map;

import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterHelper;
import org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.Credential;

import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.QUERY_ON_RESOURCE;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.BASIC_AUTH_CRED_HELPER;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.HEADER_AUTH_CRED_HELPER;

/**
 * Authentication Filter modules for the JASPI common Authentication Filter. Validates client requests by passing though
 * to a OpenICF Connector.
 *
 * @author Phill Cunnington
 * @author brmiller
 */
public class PassthroughModule implements ServerAuthModule {

    private final static Logger logger = LoggerFactory.getLogger(PassthroughModule.class);

    private final OSGiAuthnFilterHelper authnFilterHelper;

    private PassthroughAuthenticator passthroughAuthenticator;

    /**
     * Constructor used by the commons Authentication Filter framework to create an instance of this authentication
     * module.
     */
    public PassthroughModule() throws AuthException {
        authnFilterHelper = OSGiAuthnFilterBuilder.getInstance();
        if (authnFilterHelper == null) {
            throw new AuthException("OSGiAuthnFilterHelper is not ready.");
        }
    }

    /**
     * Constructor used by tests to inject dependencies.
     *
     * @param authnFilterHelper A mock of an OSGiAuthnFilterHelper
     * @param passthroughAuthenticator A mock of an PassthroughAuthenticator instance.
     */
    PassthroughModule(OSGiAuthnFilterHelper authnFilterHelper, PassthroughAuthenticator passthroughAuthenticator) {
        this.authnFilterHelper = authnFilterHelper;
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
            Map options) throws AuthException {

        final String queryOnResource = new JsonValue(options).get(QUERY_ON_RESOURCE).required().asString();
        passthroughAuthenticator = new PassthroughAuthenticator(authnFilterHelper.getConnectionFactory(), queryOnResource);
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

        logger.debug("PassthroughModule: validateRequest START");

        SecurityContextMapper securityContextMapper = SecurityContextMapper.fromMessageInfo(null, messageInfo);
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        try {
            logger.debug("PassthroughModule: Delegating call to remote authentication");

            if (authenticate(HEADER_AUTH_CRED_HELPER.getCredential(request), securityContextMapper)
                    || authenticate(BASIC_AUTH_CRED_HELPER.getCredential(request), securityContextMapper)) {

                logger.debug("PassthroughModule: Authentication successful");
                logger.debug("Found valid session for {}", securityContextMapper.getAuthenticationId());

                final String authcid = securityContextMapper.getAuthenticationId();
                clientSubject.getPrincipals().add(new Principal() {
                    public String getName() {
                        return authcid;
                    }
                });

                //Auth success will be logged in IDMServerAuthModule super type.
                return AuthStatus.SUCCESS;
            } else {
                logger.debug("PassthroughModule: Authentication failed");
                return AuthStatus.SEND_FAILURE;
            }
        } finally {
            logger.debug("PassthroughModule: validateRequest END");
        }
    }

    private boolean authenticate(Credential credential, SecurityContextMapper securityContextMapper)
            throws AuthException {

        if (!credential.isComplete()) {
            logger.debug("Failed authentication, missing or empty headers");
            return false;
        }

        try {
            boolean authenticated = passthroughAuthenticator.authenticate(credential.username, credential.password,
                    authnFilterHelper.getRouter().createServerContext());

            if (authenticated) {
                // user is authenticated; partially populate security context, rest is done by the role calculation wrapper.
                securityContextMapper.setAuthenticationId(credential.username);
            }
            return authenticated;
        } catch (ResourceException e) {
            throw new AuthException(e.getMessage());
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
