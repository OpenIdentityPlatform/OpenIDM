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

import static org.forgerock.caf.authentication.framework.AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import java.net.URI;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promises;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


/**
 * Tests IDMUserAuthModule using "internal/user" resource/query
 *
 */
public class IDMAuthModuleWrapperTest {

    private ConnectionFactory connectionFactory;
    private RoleCalculatorFactory roleCalculatorFactory;
    private AugmentationScriptExecutor scriptExecutor;
    private AsyncServerAuthModule authModule;
    private Map options = new HashMap();

    @BeforeMethod
    public void setUp() {
        try {
            connectionFactory = mock(ConnectionFactory.class);

            authModule = mock(AsyncServerAuthModule.class);
            when(authModule.initialize(any(MessagePolicy.class), any(MessagePolicy.class), any(CallbackHandler.class),
                    anyMapOf(String.class, Object.class)))
                    .thenReturn(Promises.<Void, AuthenticationException>newResultPromise(null));
            roleCalculatorFactory = mock(RoleCalculatorFactory.class);
            when(roleCalculatorFactory.create(anyList(), anyString(), anyString(), anyMap(), Matchers.<MappingRoleCalculator.GroupComparison>anyObject()))
                    .thenReturn(mock(RoleCalculator.class));
            scriptExecutor = mock(AugmentationScriptExecutor.class);
            options.put("queryOnResource", "foo/user");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void shouldValidateRequestWhenAuthModuleReturnsSendContinue() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        MessageInfoContext messageInfo = mockMessageInfoContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoMap = new HashMap<>();
        Map<String, Object> contextMap = new HashMap<>();

        Request request = new Request();
        Response response = new Response();

        given(messageInfo.getRequest()).willReturn(request);
        given(messageInfo.getResponse()).willReturn(response);
        given(messageInfo.getRequestContextMap()).willReturn(messageInfoMap);
        request.setUri(URI.create("REQUEST_URL"));
        messageInfoMap.put(ATTRIBUTE_AUTH_CONTEXT, contextMap);

        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_CONTINUE));

        //When
        IDMAuthModuleWrapper wrapper = new IDMAuthModuleWrapper(authModule,
                connectionFactory, mock(CryptoService.class), mock(ScriptRegistry.class),
                roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.validateRequest(messageInfo, clientSubject, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals(authStatus, AuthStatus.SEND_CONTINUE);
    }

    @Test
    public void shouldValidateRequestWhenAuthModuleReturnsSendSuccess() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        MessageInfoContext messageInfo = mockMessageInfoContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();
        Map<String, Object> contextMap = new HashMap<String, Object>();

        Request request = new Request();
        Response response = new Response();

        given(messageInfo.getRequest()).willReturn(request);
        given(messageInfo.getResponse()).willReturn(response);
        given(messageInfo.getRequestContextMap()).willReturn(messageInfoMap);
        request.setUri(URI.create("REQUEST_URL"));
        messageInfoMap.put(ATTRIBUTE_AUTH_CONTEXT, contextMap);

        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_SUCCESS));

        //When
        IDMAuthModuleWrapper wrapper = new IDMAuthModuleWrapper(authModule,
                connectionFactory, mock(CryptoService.class), mock(ScriptRegistry.class),
                roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.validateRequest(messageInfo, clientSubject, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void shouldValidateRequestWhenAuthModuleReturnsSendFailure() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        MessageInfoContext messageInfo = mockMessageInfoContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();
        Map<String, Object> contextMap = new HashMap<String, Object>();

        Request request = new Request();
        Response response = new Response();

        given(messageInfo.getRequest()).willReturn(request);
        given(messageInfo.getResponse()).willReturn(response);
        given(messageInfo.getRequestContextMap()).willReturn(messageInfoMap);
        request.setUri(URI.create("REQUEST_URL"));
        messageInfoMap.put(ATTRIBUTE_AUTH_CONTEXT, contextMap);

        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_FAILURE));

        //When
        IDMAuthModuleWrapper wrapper = new IDMAuthModuleWrapper(authModule,
                connectionFactory, mock(CryptoService.class), mock(ScriptRegistry.class),
                roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.validateRequest(messageInfo, clientSubject, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWhenAuthModuleReturnsSuccess() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        MessageInfoContext messageInfo = mockMessageInfoContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoMap = new HashMap<>();
        Map<String, Object> contextMap = new HashMap<>();

        Request request = new Request();
        Response response = new Response();

        Principal principalOne = mock(Principal.class);
        Principal principalTwo = mock(Principal.class);
        Principal principalThree = mock(Principal.class);
        given(principalOne.getName()).willReturn(null);
        given(principalTwo.getName()).willReturn("");
        given(principalThree.getName()).willReturn("USERNAME");

        given(messageInfo.getRequest()).willReturn(request);
        given(messageInfo.getResponse()).willReturn(response);
        given(messageInfo.getRequestContextMap()).willReturn(messageInfoMap);
        request.setUri(URI.create("REQUEST_URL"));
        messageInfoMap.put(ATTRIBUTE_AUTH_CONTEXT, contextMap);
        clientSubject.getPrincipals().add(principalOne);
        clientSubject.getPrincipals().add(principalTwo);
        clientSubject.getPrincipals().add(principalThree);

        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SUCCESS));

        //When
        IDMAuthModuleWrapper wrapper = new IDMAuthModuleWrapper(authModule,
                connectionFactory, mock(CryptoService.class), mock(ScriptRegistry.class),
                roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.validateRequest(messageInfo, clientSubject, serviceSubject)
                .getOrThrowUninterruptibly();

        //Then
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test(expectedExceptions = AuthenticationException.class)
    public void shouldValidateRequestWhenAuthModuleReturnsSuccessWithNoUsername() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        MessageInfoContext messageInfo = mockMessageInfoContext();
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoMap = new HashMap<>();
        Map<String, Object> contextMap = new HashMap<>();

        Request request = new Request();
        Response response = new Response();

        Principal principalOne = mock(Principal.class);
        Principal principalTwo = mock(Principal.class);
        given(principalOne.getName()).willReturn(null);

        given(messageInfo.getRequest()).willReturn(request);
        given(messageInfo.getResponse()).willReturn(response);
        given(messageInfo.getRequestContextMap()).willReturn(messageInfoMap);
        request.setUri(URI.create("REQUEST_URL"));
        messageInfoMap.put(ATTRIBUTE_AUTH_CONTEXT, contextMap);
        clientSubject.getPrincipals().add(principalOne);
        clientSubject.getPrincipals().add(principalTwo);

        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SUCCESS));

        //When
        IDMAuthModuleWrapper wrapper = new IDMAuthModuleWrapper(authModule,
                connectionFactory, mock(CryptoService.class), mock(ScriptRegistry.class),
                roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        wrapper.validateRequest(messageInfo, clientSubject, serviceSubject).getOrThrowUninterruptibly();

        //Then
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
    }

    @Test
    public void shouldSecureResponse() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        MessageInfoContext messageInfo = mockMessageInfoContext();
        Subject serviceSubject = new Subject();
        Request request = new Request();
        given(messageInfo.getRequest()).willReturn(request);

        given(authModule.secureResponse(Matchers.<MessageInfoContext>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SEND_SUCCESS));

        //When
        IDMAuthModuleWrapper wrapper = new IDMAuthModuleWrapper(authModule,
                connectionFactory, mock(CryptoService.class), mock(ScriptRegistry.class),
                roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.secureResponse(messageInfo, serviceSubject).getOrThrowUninterruptibly();

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test(enabled = true)
    public void shouldSecureResponseIfNoSessionHeaderSetSkipSession() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        MessageInfoContext messageInfo = mockMessageInfoContext();
        CallbackHandler handler = mock(CallbackHandler.class);

        Map<String, Object> messageInfoMap = mock(Map.class);
        Subject serviceSubject = new Subject();

        Request request = new Request();

        given(authModule.secureResponse(Matchers.<MessageInfoContext>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SUCCESS));
        given(messageInfo.getRequest()).willReturn(request);
        request.getHeaders().put("X-OpenIDM-NoSession", "true");
        given(messageInfo.getRequestContextMap()).willReturn(messageInfoMap);

        //When
        IDMAuthModuleWrapper wrapper = new IDMAuthModuleWrapper(authModule,
                connectionFactory, mock(CryptoService.class), mock(ScriptRegistry.class),
                roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.secureResponse(messageInfo, serviceSubject).getOrThrowUninterruptibly();

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
        verify(messageInfo).getRequestContextMap();
        verify(messageInfoMap).put("skipSession", true);
    }

    @Test(enabled = true)
    public void shouldSecureResponseIfNotNoSessionHeaderSet() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        MessageInfoContext messageInfo = mockMessageInfoContext();
        CallbackHandler handler = mock(CallbackHandler.class);

        Map<String, Object> messageInfoMap = mock(Map.class);
        Subject serviceSubject = new Subject();

        Request request = new Request();

        given(authModule.secureResponse(Matchers.<MessageInfoContext>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(Promises.<AuthStatus, AuthenticationException>newResultPromise(AuthStatus.SUCCESS));
        given(messageInfo.getRequest()).willReturn(request);
        request.getHeaders().put("X-OpenIDM-NoSession", null);
        given(messageInfo.getRequestContextMap()).willReturn(messageInfoMap);

        //When
        IDMAuthModuleWrapper wrapper = new IDMAuthModuleWrapper(authModule,
                connectionFactory, mock(CryptoService.class), mock(ScriptRegistry.class),
                roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.secureResponse(messageInfo, serviceSubject).getOrThrowUninterruptibly();

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
        verify(messageInfo, never()).getRequestContextMap();
    }

    private MessageInfoContext mockMessageInfoContext() {
        MessageInfoContext messageInfo = mock(MessageInfoContext.class);
        given(messageInfo.asContext(ClientContext.class))
                .willReturn(ClientContext.newInternalClientContext(new RootContext()));
        return messageInfo;
    }
}
