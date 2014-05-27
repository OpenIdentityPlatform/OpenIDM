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

import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.servlet.SecurityContextFactory;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterHelper;
import org.forgerock.openidm.router.RouteService;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
* @author Phill Cunnington
*/
public class PassthroughModuleTest {

    private OSGiAuthnFilterHelper authnFilterHelper;

    private PassthroughModule passthroughModule;

    private PassthroughAuthenticator passthroughAuthenticator;

    @BeforeMethod
    public void setUp() throws ResourceException {
        authnFilterHelper = mock(OSGiAuthnFilterHelper.class);
        RouteService router = mock(RouteService.class);
        when(router.createServerContext()).thenReturn(mock(ServerContext.class));
        when(authnFilterHelper.getRouter()).thenReturn(router);

        passthroughAuthenticator = mock(PassthroughAuthenticator.class);

        passthroughModule = new PassthroughModule(authnFilterHelper, passthroughAuthenticator);
    }

    @Test
    public void shouldValidateRequestWhenUsernameHeaderIsNull() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        Map<String, Object> map = new HashMap<String, Object>();
        given(messageInfo.getMap()).willReturn(map);
        Map<String, Object> contextMap = new HashMap<String, Object>();
        map.put(SecurityContextFactory.ATTRIBUTE_AUTHZID, contextMap);
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-Username")).willReturn(null);
        given(request.getHeader("X-OpenIDM-Password")).willReturn("PASSWORD");

        //When
        AuthStatus authStatus = passthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verifyZeroInteractions(passthroughAuthenticator);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWhenUsernameHeaderIsEmptyString() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        Map<String, Object> map = new HashMap<String, Object>();
        given(messageInfo.getMap()).willReturn(map);
        Map<String, Object> contextMap = new HashMap<String, Object>();
        map.put(SecurityContextFactory.ATTRIBUTE_AUTHZID, contextMap);

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-Username")).willReturn("");
        given(request.getHeader("X-OpenIDM-Password")).willReturn("PASSWORD");

        //When
        AuthStatus authStatus = passthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verifyZeroInteractions(passthroughAuthenticator);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWhenPasswordHeaderIsNull() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        Map<String, Object> map = new HashMap<String, Object>();
        given(messageInfo.getMap()).willReturn(map);
        Map<String, Object> contextMap = new HashMap<String, Object>();
        map.put(SecurityContextFactory.ATTRIBUTE_AUTHZID, contextMap);

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-Username")).willReturn("USERNAME");
        given(request.getHeader("X-OpenIDM-Password")).willReturn(null);

        //When
        AuthStatus authStatus = passthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verifyZeroInteractions(passthroughAuthenticator);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWhenPasswordHeaderIsEmptyString() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        Map<String, Object> map = new HashMap<String, Object>();
        given(messageInfo.getMap()).willReturn(map);
        Map<String, Object> contextMap = new HashMap<String, Object>();
        map.put(SecurityContextFactory.ATTRIBUTE_AUTHZID, contextMap);

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-Username")).willReturn("USERNAME");
        given(request.getHeader("X-OpenIDM-Password")).willReturn("");

        //When
        AuthStatus authStatus = passthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verifyZeroInteractions(passthroughAuthenticator);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWhenAuthenticationSuccessful() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        Map<String, Object> map = new HashMap<String, Object>();
        given(messageInfo.getMap()).willReturn(map);
        Map<String, Object> contextMap = new HashMap<String, Object>();
        map.put(SecurityContextFactory.ATTRIBUTE_AUTHZID, contextMap);

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-Username")).willReturn("USERNAME");
        given(request.getHeader("X-OpenIDM-Password")).willReturn("PASSWORD");

        given(passthroughAuthenticator.authenticate(eq("USERNAME"), eq("PASSWORD"),
                Matchers.<ServerContext>anyObject())).willReturn(true);

        //When
        AuthStatus authStatus = passthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void shouldValidateRequestWhenAuthenticationFailed() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        Map<String, Object> map = new HashMap<String, Object>();
        given(messageInfo.getMap()).willReturn(map);
        Map<String, Object> contextMap = new HashMap<String, Object>();
        map.put(SecurityContextFactory.ATTRIBUTE_AUTHZID, contextMap);

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-Username")).willReturn("USERNAME");
        given(request.getHeader("X-OpenIDM-Password")).willReturn("PASSWORD");

        given(passthroughAuthenticator.authenticate(eq("USERNAME"), eq("PASSWORD"),
                Matchers.<ServerContext>anyObject())).willReturn(false);

        //When
        AuthStatus authStatus = passthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldSecureResponse() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();

        //When
        AuthStatus authStatus = passthroughModule.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
        verifyZeroInteractions(messageInfo);
    }
}
