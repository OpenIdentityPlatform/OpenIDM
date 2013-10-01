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

import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author Phill Cunnington
 */
public class IWAModuleTest {

    private IWAModule iwaModule;

    private org.forgerock.jaspi.modules.iwa.IWAModule commonsIwaModule;

    @BeforeClass
    public void setUp() {

        commonsIwaModule = mock(org.forgerock.jaspi.modules.iwa.IWAModule.class);

        iwaModule = new IWAModule(commonsIwaModule);
    }

    @Test
    public void shouldInitialize() throws AuthException {

        //Given
        MessagePolicy requestPolicy = mock(MessagePolicy.class);
        MessagePolicy responsePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        JsonValue options = mock(JsonValue.class);
        Map<String, Object> optionsMap = new HashMap<String, Object>();

        given(options.asMap()).willReturn(optionsMap);

        //When
        iwaModule.initialize(requestPolicy, responsePolicy, handler, options);

        //Then
        verify(commonsIwaModule).initialize(requestPolicy, responsePolicy, handler, optionsMap);
    }

    @Test
    public void shouldValidateRequestWhenCommonsIWAModuleReturnsSendContinue() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AuthData authData = mock(AuthData.class);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(request.getRequestURL()).willReturn(new StringBuffer("REQUEST_URL"));

        given(commonsIwaModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_CONTINUE);

        //When
        AuthStatus authStatus = iwaModule.validateRequest(messageInfo, clientSubject, serviceSubject, authData);

        //Then
        verify(commonsIwaModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verifyZeroInteractions(authData);
        assertEquals(authStatus, AuthStatus.SEND_CONTINUE);
    }

    @Test
    public void shouldValidateRequestWhenCommonsIWAModuleReturnsSendSuccess() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AuthData authData = mock(AuthData.class);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(request.getRequestURL()).willReturn(new StringBuffer("REQUEST_URL"));

        given(commonsIwaModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        AuthStatus authStatus = iwaModule.validateRequest(messageInfo, clientSubject, serviceSubject, authData);

        //Then
        verify(commonsIwaModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verifyZeroInteractions(authData);
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void shouldValidateRequestWhenCommonsIWAModuleReturnsSendFailure() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AuthData authData = mock(AuthData.class);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(request.getRequestURL()).willReturn(new StringBuffer("REQUEST_URL"));

        given(commonsIwaModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        AuthStatus authStatus = iwaModule.validateRequest(messageInfo, clientSubject, serviceSubject, authData);

        //Then
        verify(commonsIwaModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verifyZeroInteractions(authData);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWhenCommonsIWAModuleReturnsSuccess() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AuthData authData = mock(AuthData.class);

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
        given(request.getRequestURL()).willReturn(new StringBuffer("REQUEST_URL"));
        clientSubject.getPrincipals().add(principalOne);
        clientSubject.getPrincipals().add(principalTwo);
        clientSubject.getPrincipals().add(principalThree);

        given(commonsIwaModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SUCCESS);

        //When
        AuthStatus authStatus = iwaModule.validateRequest(messageInfo, clientSubject, serviceSubject, authData);

        //Then
        verify(commonsIwaModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(authData).setUsername("USERNAME");
        verify(authData).setResource("system/AD/account");
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void shouldValidateRequestWhenCommonsIWAModuleReturnsSuccessWithNoUsername() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        AuthData authData = mock(AuthData.class);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        Principal principalOne = mock(Principal.class);
        Principal principalTwo = mock(Principal.class);
        given(principalOne.getName()).willReturn(null);
        given(principalTwo.getName()).willReturn("");

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(request.getRequestURL()).willReturn(new StringBuffer("REQUEST_URL"));
        clientSubject.getPrincipals().add(principalOne);
        clientSubject.getPrincipals().add(principalTwo);

        given(commonsIwaModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SUCCESS);

        //When
        AuthStatus authStatus = iwaModule.validateRequest(messageInfo, clientSubject, serviceSubject, authData);

        //Then
        verify(commonsIwaModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(authData, never()).setUsername(anyString());
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldSecureResponse() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, Object> contextMap = new HashMap<String, Object>();
        map.put(IDMServerAuthModule.CONTEXT_REQUEST_KEY, contextMap);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getMap()).willReturn(map);

        //When
        AuthStatus authStatus = iwaModule.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }
}
