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

package org.forgerock.openidm.jaspi.modules;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openidm.router.RouteService;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.forgerock.authz.filter.servlet.api.HttpAuthorizationContext.ATTRIBUTE_AUTHORIZATION_CONTEXT;

import org.forgerock.http.Context;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterHelper;

/**
 * Tests IDMUserAuthModule using "internal/user" resource/query
 *
 */
public class IDMJaspiModuleWrapperTest {

    private Context context;
    private OSGiAuthnFilterHelper authnFilterHelper;
    private IDMJaspiModuleWrapper.AuthModuleConstructor authModuleConstructor;
    private RoleCalculatorFactory roleCalculatorFactory;
    private AugmentationScriptExecutor scriptExecutor;
    private ServerAuthModule authModule;
    private Map options = new HashMap();

    @BeforeMethod
    public void setUp() {
        try {
            context = mock(Context.class);
            RouteService routeService = mock(RouteService.class);
            when(routeService.createServerContext()).thenReturn(context);
            authnFilterHelper = mock(OSGiAuthnFilterHelper.class);
            when(authnFilterHelper.getRouter()).thenReturn(routeService);
            when(authnFilterHelper.getConnectionFactory()).thenReturn(mock(ConnectionFactory.class));

            authModule = mock(ServerAuthModule.class);
            authModuleConstructor = mock(IDMJaspiModuleWrapper.AuthModuleConstructor.class);
            when(authModuleConstructor.construct(anyString())).thenReturn(authModule);
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
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoMap = new HashMap<>();
        Map<String, Object> contextMap = new HashMap<>();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(messageInfoMap);
        given(request.getRequestURL()).willReturn(new StringBuffer("REQUEST_URL"));
        messageInfoMap.put(ATTRIBUTE_AUTHORIZATION_CONTEXT, contextMap);

        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_CONTINUE);

        //When
        IDMJaspiModuleWrapper wrapper = new IDMJaspiModuleWrapper(authnFilterHelper, authModuleConstructor, roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals(authStatus, AuthStatus.SEND_CONTINUE);
    }

    @Test
    public void shouldValidateRequestWhenAuthModuleReturnsSendSuccess() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();
        Map<String, Object> contextMap = new HashMap<String, Object>();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(messageInfoMap);
        given(request.getRequestURL()).willReturn(new StringBuffer("REQUEST_URL"));
        messageInfoMap.put(ATTRIBUTE_AUTHORIZATION_CONTEXT, contextMap);

        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        IDMJaspiModuleWrapper wrapper = new IDMJaspiModuleWrapper(authnFilterHelper, authModuleConstructor, roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void shouldValidateRequestWhenAuthModuleReturnsSendFailure() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();
        Map<String, Object> contextMap = new HashMap<String, Object>();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(messageInfoMap);
        given(request.getRequestURL()).willReturn(new StringBuffer("REQUEST_URL"));
        messageInfoMap.put(ATTRIBUTE_AUTHORIZATION_CONTEXT, contextMap);

        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        IDMJaspiModuleWrapper wrapper = new IDMJaspiModuleWrapper(authnFilterHelper, authModuleConstructor, roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWhenAuthModuleReturnsSuccess() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoMap = new HashMap<>();
        Map<String, Object> contextMap = new HashMap<>();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        Principal principalOne = mock(Principal.class);
        Principal principalTwo = mock(Principal.class);
        Principal principalThree = mock(Principal.class);
        given(principalOne.getName()).willReturn(null);
        given(principalTwo.getName()).willReturn("");
        given(principalThree.getName()).willReturn("USERNAME");

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(messageInfoMap);
        given(request.getRequestURL()).willReturn(new StringBuffer("REQUEST_URL"));
        messageInfoMap.put(ATTRIBUTE_AUTHORIZATION_CONTEXT, contextMap);
        clientSubject.getPrincipals().add(principalOne);
        clientSubject.getPrincipals().add(principalTwo);
        clientSubject.getPrincipals().add(principalThree);

        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SUCCESS);

        //When
        IDMJaspiModuleWrapper wrapper = new IDMJaspiModuleWrapper(authnFilterHelper, authModuleConstructor, roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test(expectedExceptions = AuthException.class)
    public void shouldValidateRequestWhenAuthModuleReturnsSuccessWithNoUsername() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoMap = new HashMap<>();
        Map<String, Object> contextMap = new HashMap<>();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        Principal principalOne = mock(Principal.class);
        Principal principalTwo = mock(Principal.class);
        given(principalOne.getName()).willReturn(null);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(messageInfo.getMap()).willReturn(messageInfoMap);
        given(request.getRequestURL()).willReturn(new StringBuffer("REQUEST_URL"));
        messageInfoMap.put(ATTRIBUTE_AUTHORIZATION_CONTEXT, contextMap);
        clientSubject.getPrincipals().add(principalOne);
        clientSubject.getPrincipals().add(principalTwo);

        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SUCCESS);

        //When
        IDMJaspiModuleWrapper wrapper = new IDMJaspiModuleWrapper(authnFilterHelper, authModuleConstructor, roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
    }

    @Test
    public void shouldSecureResponse() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(messageInfo.getRequestMessage()).willReturn(request);

        given(authModule.secureResponse(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        IDMJaspiModuleWrapper wrapper = new IDMJaspiModuleWrapper(authnFilterHelper, authModuleConstructor, roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test(enabled = true)
    public void shouldSecureResponseIfNoSessionHeaderSetSkipSession() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        MessageInfo messageInfo = mock(MessageInfo.class);
        CallbackHandler handler = mock(CallbackHandler.class);

        Map<String, Object> messageInfoMap = mock(Map.class);
        Subject serviceSubject = new Subject();

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(authModule.secureResponse(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(AuthStatus.SUCCESS);
        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-NoSession")).willReturn("true");
        given(messageInfo.getMap()).willReturn(messageInfoMap);

        //When
        IDMJaspiModuleWrapper wrapper = new IDMJaspiModuleWrapper(authnFilterHelper, authModuleConstructor, roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
        verify(messageInfo).getMap();
        verify(messageInfoMap).put("skipSession", true);
    }

    @Test(enabled = true)
    public void shouldSecureResponseIfNotNoSessionHeaderSet() throws AuthException {

        //Given
        MessagePolicy messagePolicy = mock(MessagePolicy.class);
        MessageInfo messageInfo = mock(MessageInfo.class);
        CallbackHandler handler = mock(CallbackHandler.class);

        Map<String, Object> messageInfoMap = mock(Map.class);
        Subject serviceSubject = new Subject();

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(authModule.secureResponse(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(AuthStatus.SUCCESS);
        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-NoSession")).willReturn(null);
        given(messageInfo.getMap()).willReturn(messageInfoMap);

        //When
        IDMJaspiModuleWrapper wrapper = new IDMJaspiModuleWrapper(authnFilterHelper, authModuleConstructor, roleCalculatorFactory, scriptExecutor);
        wrapper.initialize(messagePolicy, messagePolicy, handler, options);
        AuthStatus authStatus = wrapper.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
        verify(messageInfo, never()).getMap();
    }
}
