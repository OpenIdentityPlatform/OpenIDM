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

import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterBuilder;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterHelper;
import org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.Credential;
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
import java.util.Map;

import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.AUTHENTICATION_ID;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.PROPERTY_MAPPING;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.QUERY_ID;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.QUERY_ON_RESOURCE;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.HEADER_AUTH_CRED_HELPER;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.BASIC_AUTH_CRED_HELPER;

/**
 * Authentication Module for authenticating users against a managed users table.
 *
 * @author Phill Cunnington
 * @author brmiller
 */
public class IDMUserAuthModule implements ServerAuthModule {

    private final static Logger logger = LoggerFactory.getLogger(IDMUserAuthModule.class);

    // property config keys
    private static final String USER_CREDENTIAL = "userCredential";

    private ResourceQueryAuthenticator authenticator;

    private final OSGiAuthnFilterHelper authnFilterHelper;

    private String queryOnResource;
    private String queryId;

    /**
     * Constructor used by the commons Authentication Filter framework to create an instance of this authentication
     * module.
     */
    public IDMUserAuthModule() throws AuthException {
        this.authnFilterHelper = OSGiAuthnFilterBuilder.getInstance();
        if (authnFilterHelper == null) {
            throw new AuthException("OSGiAuthnFilterHelper is not ready.");
        }
    }

    /**
     * Constructor used by tests to inject dependencies.
     *
     * @param authenticator mock of an ResourceQueryAuthenticator instance.
     * @param authnFilterHelper mock of an OSGiAuthnFilterHelper instance.
     * @param queryOnResource the resource to query
     * @param queryId the queryId
     */
    IDMUserAuthModule(ResourceQueryAuthenticator authenticator, OSGiAuthnFilterHelper authnFilterHelper,
            String queryOnResource, String queryId) {
        this.authenticator = authenticator;
        this.authnFilterHelper = authnFilterHelper;
        this.queryOnResource = queryOnResource;
        this.queryId = queryId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    /**
     * Initialises the IDMUserAuthModule.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
            Map options) {

        final JsonValue properties = new JsonValue(options);

        queryOnResource = properties.get(QUERY_ON_RESOURCE).required().asString();
        queryId = properties.get(QUERY_ID).required().asString();
        JsonValue propertyMapping = properties.get(PROPERTY_MAPPING);
        String authenticationIdProperty = propertyMapping.get(AUTHENTICATION_ID).required().asString();
        String userCredentialProperty = propertyMapping.get(USER_CREDENTIAL).required().asString();

        authenticator = new ResourceQueryAuthenticator(
                authnFilterHelper.getCryptoService(),
                authnFilterHelper.getConnectionFactory(),
                queryOnResource, queryId,
                authenticationIdProperty, userCredentialProperty);
    }

    /**
     * Validates the request by authenticating against a user table using either OpenIDM username/password
     * headers or basic auth headers from the request.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        SecurityContextMapper securityContextMapper = SecurityContextMapper.fromMessageInfo(null, messageInfo);

        HttpServletRequest req = (HttpServletRequest) messageInfo.getRequestMessage();

        if (authenticateUser(HEADER_AUTH_CRED_HELPER.getCredential(req), securityContextMapper)
                || authenticateUser(BASIC_AUTH_CRED_HELPER.getCredential(req), securityContextMapper)) {

            final String authcid = securityContextMapper.getAuthenticationId();
            clientSubject.getPrincipals().add(new Principal() {
                public String getName() {
                    return authcid;
                }
            });
            return AuthStatus.SUCCESS;
        } else {
            return AuthStatus.SEND_FAILURE;
        }
    }

    /**
     * Authenticate the username and password.
     *
     * @param credential the username/password credential
     * @param securityContextMapper The SecurityContextMapper.
     * @return whether the authentication is successful
     */
    private boolean authenticateUser(Credential credential, SecurityContextMapper securityContextMapper)
            throws AuthException {

        if (!credential.isComplete()) {
            logger.debug("Failed authentication, missing or empty headers");
            return false;
        }

        // set authenticationId and resource so long as username isn't null
        if (credential.username != null) {
            securityContextMapper.setAuthenticationId(credential.username);
        }

        try {
            return authenticator.authenticate(credential.username, credential.password,
                    authnFilterHelper.getRouter().createServerContext());
        } catch (ResourceException e) {
            logger.error(e.getMessage(), e);
            throw new JaspiAuthException(e.getMessage(), e);
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
