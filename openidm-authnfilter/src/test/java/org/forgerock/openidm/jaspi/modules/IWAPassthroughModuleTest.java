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
public class IWAPassthroughModuleTest {

    private IWAPassthroughModule iwaAdPassthroughModule;

    private org.forgerock.jaspi.modules.iwa.IWAModule commonsIwaModule;
    private PassthroughModule passthroughModule;

    @BeforeClass
    public void setUp() {

        commonsIwaModule = mock(org.forgerock.jaspi.modules.iwa.IWAModule.class);
        passthroughModule = mock(PassthroughModule.class);

        iwaAdPassthroughModule = new IWAPassthroughModule(commonsIwaModule, passthroughModule);
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
        iwaAdPassthroughModule.initialize(requestPolicy, responsePolicy, handler, optionsMap);

        //Then
        verify(commonsIwaModule).initialize(requestPolicy, responsePolicy, handler, optionsMap);
        verify(passthroughModule).initialize(requestPolicy, responsePolicy, handler, optionsMap);
    }

    @Test
    public void shouldValidateRequestWhenOpenIDMUsernameAndPasswordAreSet() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-Username")).willReturn("USERNAME");
        given(request.getHeader("X-OpenIDM-Password")).willReturn("PASSWORD");

        given(passthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_CONTINUE);

        //When
        AuthStatus authStatus = iwaAdPassthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(passthroughModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals(authStatus, AuthStatus.SEND_CONTINUE);
    }

    @Test
    public void shouldValidateRequestWhenHeadersNotSetAndCommonsIWAModuleReturnsSendContinue() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(request.getRequestURL()).willReturn(new StringBuffer("REQUEST_URL"));

        given(commonsIwaModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_CONTINUE);

        //When
        AuthStatus authStatus = iwaAdPassthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(commonsIwaModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(passthroughModule).setPassThroughAuthOnRequest(messageInfo);
        assertEquals(authStatus, AuthStatus.SEND_CONTINUE);
    }

    @Test
    public void shouldValidateRequestWhenHeadersNotSetAndCommonsIWAModuleReturnsSendSuccess() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(request.getRequestURL()).willReturn(new StringBuffer("REQUEST_URL"));

        given(commonsIwaModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        AuthStatus authStatus = iwaAdPassthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(commonsIwaModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(passthroughModule).setPassThroughAuthOnRequest(messageInfo);
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void shouldValidateRequestWhenHeadersNotSetAndCommonsIWAModuleReturnsSendFailure() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(request.getRequestURL()).willReturn(new StringBuffer("REQUEST_URL"));

        given(commonsIwaModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);
        given(passthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        AuthStatus authStatus = iwaAdPassthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(commonsIwaModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(passthroughModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void shouldValidateRequestWhenHeadersNotSetAndCommonsIWAModuleReturnsSuccess() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

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
        AuthStatus authStatus = iwaAdPassthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(commonsIwaModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(passthroughModule).setPassThroughAuthOnRequest(messageInfo);
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }
}
