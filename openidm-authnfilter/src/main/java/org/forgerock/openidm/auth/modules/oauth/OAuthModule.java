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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.auth.modules.oauth;

import static org.forgerock.caf.authentication.framework.AuthenticationFramework.LOG;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static javax.security.auth.message.AuthStatus.SEND_FAILURE;
import static javax.security.auth.message.AuthStatus.SEND_SUCCESS;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openidm.auth.modules.oauth.exceptions.OAuthVerificationException;
import org.forgerock.openidm.auth.modules.oauth.resolvers.OAuthResolverImpl;
import org.forgerock.openidm.auth.modules.oauth.resolvers.service.OAuthResolverService;
import org.forgerock.openidm.auth.modules.oauth.resolvers.service.OAuthResolverServiceConfigurator;
import org.forgerock.openidm.auth.modules.oauth.resolvers.service.OAuthResolverServiceConfiguratorImpl;
import org.forgerock.openidm.auth.modules.oauth.resolvers.service.OAuthResolverServiceImpl;
import org.forgerock.util.promise.Promise;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Authentication Module used for OAuth authentication(Authorization).
 */
public class OAuthModule implements AsyncServerAuthModule {

    /**
     * Lookup key for the configured HTTP header used for auth token.
     */
    public static final String HEADER_TOKEN = "authTokenHeader";

    /**
     * Lookup key for the configured HTTP header used for auth resolver name.
     */
    public static final String HEADER_AUTH_RESOLVER = "authResolverHeader";

    /**
     * Lookup key for the configured resolvers which will be used by this module.
     */
    public static final String RESOLVERS_KEY = "resolvers";

    private String authTokenHeader;
    private String authResolverHeader;

    private final OAuthResolverServiceConfigurator serviceConfigurator;
    private OAuthResolverService resolverService;
    private  CallbackHandler callbackHandler;

    /**
     * Default constructor.
     */
    public OAuthModule() {
        serviceConfigurator = new OAuthResolverServiceConfiguratorImpl();
    }

    /**
     * Used for unit tests.
     */
    OAuthModule(final OAuthResolverServiceConfigurator serviceConfigurator,
            final OAuthResolverService resolverService,
            final CallbackHandler callbackHandler,
            final String authTokenHeader,
            final String authResolverHeader) {
        this.serviceConfigurator = serviceConfigurator;
        this.resolverService = resolverService;
        this.callbackHandler = callbackHandler;
        this.authTokenHeader = authTokenHeader;
        this.authResolverHeader = authResolverHeader;
    }

    @Override
    public String getModuleId() {
        return "OAuth";
    }

    @Override
    public Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
            CallbackHandler callbackHandler, Map<String, Object> config) {

        this.authTokenHeader = (String) config.get(HEADER_TOKEN);
        this.authResolverHeader = (String) config.get(HEADER_AUTH_RESOLVER);

        this.callbackHandler = callbackHandler;

        if (authTokenHeader == null || authTokenHeader.isEmpty()) {
            LOG.debug("OAuthModule config is invalid. You must include the auth token header key parameter");
            return newExceptionPromise(new AuthenticationException("OAuthModule configuration is invalid."));
        }

        if (authResolverHeader == null || authResolverHeader.isEmpty()) {
            LOG.debug("OAuthModule config is invalid. You must include the auth provider header key parameter");
            return newExceptionPromise(new AuthenticationException("OAuthModule configuration is invalid."));
        }

        final List<Map<String, String>> resolvers = (List<Map<String, String>>) config.get(RESOLVERS_KEY);

        resolverService = new OAuthResolverServiceImpl();

        // if we weren't able to set up the service, or any one of the supplied resolver configs was invalid,
        // error out here
        if (!serviceConfigurator.configureService(resolverService, resolvers)) {
            LOG.debug("OAuth config is invalid. You must configure at least one valid resolver.");
            return newExceptionPromise(new AuthenticationException("OpenIdConnectModule configuration is invalid."));
        }

        return newResultPromise(null);
    }

    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageInfoContext messageInfo,
            Subject clientSubject, Subject serviceSubject) {

        final Request request = messageInfo.getRequest();

        final String access_token = request.getHeaders().getFirst(authTokenHeader);
        final String resolverName = request.getHeaders().getFirst(authResolverHeader);

        if (access_token == null || access_token.isEmpty()) {
            return newResultPromise(SEND_FAILURE);
        }

        if (resolverName == null || access_token.isEmpty()) {
            return newResultPromise(SEND_FAILURE);
        }

        final OAuthResolverImpl resolver = resolverService.getResolver(resolverName);

        // if no resolver for this idp found, abort
        if (resolver == null) {
            LOG.debug("No resolver found for the idp: {}", resolverName);
            return newResultPromise(SEND_FAILURE);
        }

        try {
            // validate the access token against the Idp
            resolver.validateIdentity(access_token);
            callbackHandler.handle(new Callback[]{
                    new CallerPrincipalCallback(clientSubject, resolver.getSubject())
            });

        } catch (OAuthVerificationException e) {
            LOG.debug("Unable to validate authenticated identity from access token", e);
            return newResultPromise(SEND_FAILURE);
        } catch (IOException | UnsupportedCallbackException e) {
            LOG.debug("Error setting user principal", e);
            return newExceptionPromise(new AuthenticationException(e.getMessage()));
        }

        return newResultPromise(AuthStatus.SUCCESS);
    }

    /**
     * Sends SEND_SUCCESS automatically. As we're on our way out of the system at this point, there's
     * no need to hold them up, or append anything new to the response.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
            Subject subject) {
        return newResultPromise(SEND_SUCCESS);
    }

    /**
     * Nothing to clean.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo, Subject subject) {
        return newResultPromise(null);
    }

    @Override
    public Collection<Class<?>> getSupportedMessageTypes() {
        return Arrays.asList(new Class<?>[]{Request.class, Response.class});
    }
}
