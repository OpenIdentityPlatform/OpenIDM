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

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openidm.auth.modules.oauth.resolvers.service.OAuthResolverService;
import org.forgerock.openidm.auth.modules.oauth.resolvers.service.OAuthResolverServiceConfigurator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Test basic OAuth Module.
 */
public class OAuthModuleTest {

    OAuthModule testModule;
    OAuthResolverServiceConfigurator mockConfigurator;
    OAuthResolverService mockService;
    CallbackHandler mockCallback;

    @BeforeMethod
    public void setUp() {
        mockConfigurator = mock(OAuthResolverServiceConfigurator.class);
        mockService = mock(OAuthResolverService.class);
        mockCallback = mock(CallbackHandler.class);
        testModule = new OAuthModule(mockConfigurator, mockService, mockCallback,
                OAuthModule.HEADER_TOKEN, OAuthModule.HEADER_AUTH_RESOLVER);
    }

    private Map<String, Object> getConfig() throws UnsupportedEncodingException {

        final Map<String, Object> options = new HashMap<>();
        options.put(OAuthModule.HEADER_TOKEN, "authToken");
        options.put(OAuthModule.HEADER_AUTH_RESOLVER, "provider");

        return options;
    }

    @Test(expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionWithNoHeaderInConfig() throws Exception {
        //given
        final MessagePolicy requestPolicy = mock(MessagePolicy.class);
        final MessagePolicy responsePolicy =  mock(MessagePolicy.class);
        final CallbackHandler callback =  mock(CallbackHandler.class);
        final Map<String, Object> config = new HashMap<>();

        //when
        testModule.initialize(requestPolicy, responsePolicy, callback, config).getOrThrowUninterruptibly();

        //then - covered by caught exception
    }

    @Test(expectedExceptions = AuthException.class)
    public void shouldThrowAuthExceptionWhenConfigureServiceFails() throws Exception {
        //given
        final MessagePolicy requestPolicy = mock(MessagePolicy.class);
        final MessagePolicy responsePolicy =  mock(MessagePolicy.class);
        final CallbackHandler callback =  mock(CallbackHandler.class);
        final Map<String, Object> config = getConfig();

        given(mockConfigurator.configureService(any(OAuthResolverService.class), any(List.class))).willReturn(false);

        //when
        testModule.initialize(requestPolicy, responsePolicy, callback, config).getOrThrowUninterruptibly();

        //then - covered by caught exception
    }

    @Test
    public void shouldReturnFailureWhenNoAccessToken() throws AuthException {
        //given
        final Request request = new Request();
        final Response response = new Response();

        final MessageInfoContext mockMessage = mock(MessageInfoContext.class);
        final String accessToken = null;

        given(mockMessage.getRequest()).willReturn(request);
        given(mockMessage.getResponse()).willReturn(response);

        request.getHeaders().put(OAuthModule.HEADER_TOKEN, accessToken);

        //when
        final AuthStatus res = testModule.validateRequest(mockMessage, null, null).getOrThrowUninterruptibly();

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldReturnFailureWhenNoResolverName() throws AuthException {
        //given
        final Request request = new Request();
        final Response response = new Response();

        final MessageInfoContext mockMessage = mock(MessageInfoContext.class);
        final String resolverName = null;

        given(mockMessage.getRequest()).willReturn(request);
        given(mockMessage.getResponse()).willReturn(response);

        request.getHeaders().put(OAuthModule.HEADER_AUTH_RESOLVER, resolverName);

        //when
        final AuthStatus res = testModule.validateRequest(mockMessage, null, null).getOrThrowUninterruptibly();

        //then
        assertEquals(res, AuthStatus.SEND_FAILURE);
    }


}