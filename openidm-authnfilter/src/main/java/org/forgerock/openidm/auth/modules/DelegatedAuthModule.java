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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openidm.auth.modules;

import static javax.security.auth.message.AuthStatus.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.caf.authentication.framework.AuditTrail;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.openidm.auth.Authenticator;
import org.forgerock.openidm.auth.AuthenticatorFactory;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication Filter module for the Common Authentication Filter. Validates client requests by passing though
 * to a OpenICF Connector or other delegated endpoint.
 */
public class DelegatedAuthModule implements AsyncServerAuthModule {

    private final static Logger logger = LoggerFactory.getLogger(DelegatedAuthModule.class);

    /**
     * A NullObject {@link Authenticator} implementation.
     */
    private static final Authenticator NULL_AUTHENTICATOR = new Authenticator() {
        @Override
        public AuthenticatorResult authenticate(String username, String password, Context context)
                throws ResourceException {
            return AuthenticatorResult.FAILED;
        }
    };

    private final AuthenticatorFactory authenticatorFactory;

    private Authenticator authenticator = NULL_AUTHENTICATOR;
    private String queryOnResource = "";
    private JsonValue options = new JsonValue(null);

    /**
     * Constructor used by the commons Authentication Filter framework to create an instance of this authentication
     * module.
     *
     * @param authenticatorFactory
     */
    public DelegatedAuthModule(AuthenticatorFactory authenticatorFactory) {
        Reject.ifNull(authenticatorFactory, "AuthenticationFactory cannot be null");
        this.authenticatorFactory = authenticatorFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Collection<Class<?>> getSupportedMessageTypes() {
        return Arrays.asList(new Class<?>[]{Request.class, Response.class});
    }

    @Override
    public String getModuleId() {
        return "Delegated";
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
    public Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
            CallbackHandler handler, Map<String, Object> options) {
        this.options = new JsonValue(options);
        queryOnResource = new JsonValue(options).get(IDMAuthModuleWrapper.QUERY_ON_RESOURCE).required().asString();
        authenticator = authenticatorFactory.apply(this.options);
        return newResultPromise(null);
    }

    /**
     * Validates the client's request by passing through the request to be authenticated against a OpenICF Connector.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageInfoContext messageInfo,
            Subject clientSubject, Subject serviceSubject) {

        logger.debug("DelegatedAuthModule: validateRequest START");

        SecurityContextMapper securityContextMapper = SecurityContextMapper.fromMessageInfo(messageInfo);
        Request request = messageInfo.getRequest();

        try {
            logger.debug("DelegatedAuthModule: Delegating call to remote authentication");

            if (authenticate(IDMAuthModuleWrapper.HEADER_AUTH_CRED_HELPER.getCredential(request), messageInfo, securityContextMapper)
                    || authenticate(IDMAuthModuleWrapper.BASIC_AUTH_CRED_HELPER.getCredential(request), messageInfo, securityContextMapper)) {

                logger.debug("DelegatedAuthModule: Authentication successful");

                final String authcid = securityContextMapper.getAuthenticationId();
                clientSubject.getPrincipals().add(new Principal() {
                    public String getName() {
                        return authcid;
                    }
                });

                // Auth success will be logged in IDMAuthModuleWrapper
                return newResultPromise(SUCCESS);
            } else {
                logger.debug("DelegatedAuthModule: Authentication failed");
                return newResultPromise(SEND_FAILURE);
            }
        } catch (AuthenticationException e) {
            return newExceptionPromise(e);
        } finally {
            logger.debug("DelegatedAuthModule: validateRequest END");
        }
    }

    private boolean authenticate(IDMAuthModuleWrapper.Credential credential, MessageInfoContext messageInfo,
            SecurityContextMapper securityContextMapper) throws AuthenticationException {

        if (!credential.isComplete()) {
            logger.debug("Failed authentication, missing or empty headers");
            return false;
        }

        // set the authenticationId of the user that is trying to authenticate
        securityContextMapper.setAuthenticationId(credential.username);

        try {
            // construct a rudimentary context chain to validate the auth credentials
            Context context = new SecurityContext(
                    new RootContext(),
                    securityContextMapper.getAuthenticationId(),
                    securityContextMapper.getAuthorizationId());
            Authenticator.AuthenticatorResult result = authenticator.authenticate(
                    credential.username, credential.password, context);
            final ResourceResponse resource = result.getResource();
            if (resource != null) {
                final JsonValue messageMap = new JsonValue(messageInfo.getRequestContextMap());
                messageMap.put(IDMAuthModuleWrapper.AUTHENTICATED_RESOURCE,
                        json(object(
                                field(ResourceResponse.FIELD_CONTENT_ID, resource.getId()),
                                field(ResourceResponse.FIELD_CONTENT_REVISION, resource.getRevision()),
                                field(ResourceResponse.FIELD_CONTENT, resource.getContent().asMap())))
                        .asMap());
            }
            return result.isAuthenticated();
        } catch (ResourceException e) {
            logger.debug("Failed delegated authentication of {} on {}.", credential.username, queryOnResource, e);
            messageInfo.getRequestContextMap().put(AuditTrail.AUDIT_FAILURE_REASON_KEY, e.toJsonValue().asMap());
            if (e.isServerError()) { // HTTP server-side error
                throw new AuthenticationException(
                        "Failed delegated authentication of " + credential.username + " on " + queryOnResource, e);
            }
            // authentication failed
            return false;
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
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
            Subject serviceSubject) {
        return newResultPromise(SEND_SUCCESS);
    }

    /**
     * Nothing to clean up.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo, Subject subject) {
        return newResultPromise(null);
    }
}
