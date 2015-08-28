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

import org.forgerock.jaspi.runtime.AuditTrail;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.http.Context;
import org.forgerock.openidm.jaspi.auth.Authenticator;
import org.forgerock.openidm.jaspi.auth.Authenticator.AuthenticatorResult;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterHelper;
import org.mockito.Matchers;
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
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
* Test the DelegatedAuthModule.
*/
public class DelegatedAuthModuleTest {

    private OSGiAuthnFilterHelper authnFilterHelper;

    private DelegatedAuthModule module;

    private Authenticator authenticator;

    @BeforeMethod
    public void setUp() throws ResourceException {
        authnFilterHelper = mock(OSGiAuthnFilterHelper.class);
        authenticator = mock(Authenticator.class);
        module = new DelegatedAuthModule(authnFilterHelper, authenticator);
    }

    @Test
    public void shouldValidateRequestWhenUsernameHeaderIsNull() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-Username")).willReturn(null);
        given(request.getHeader("X-OpenIDM-Password")).willReturn("PASSWORD");

        //When
        AuthStatus authStatus = module.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verifyZeroInteractions(authenticator);
        assertTrue(clientSubject.getPrincipals().isEmpty());
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWhenUsernameHeaderIsEmptyString() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-Username")).willReturn("");
        given(request.getHeader("X-OpenIDM-Password")).willReturn("PASSWORD");

        //When
        AuthStatus authStatus = module.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verifyZeroInteractions(authenticator);
        assertTrue(clientSubject.getPrincipals().isEmpty());
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWhenPasswordHeaderIsNull() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-Username")).willReturn("USERNAME");
        given(request.getHeader("X-OpenIDM-Password")).willReturn(null);

        //When
        AuthStatus authStatus = module.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verifyZeroInteractions(authenticator);
        assertTrue(clientSubject.getPrincipals().isEmpty());
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWhenPasswordHeaderIsEmptyString() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-Username")).willReturn("USERNAME");
        given(request.getHeader("X-OpenIDM-Password")).willReturn("");

        //When
        AuthStatus authStatus = module.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verifyZeroInteractions(authenticator);
        assertTrue(clientSubject.getPrincipals().isEmpty());
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWhenAuthenticationSuccessful() throws ResourceException, AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        AuthenticatorResult authResult = mock(AuthenticatorResult.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();
        Map<String, Object> auditInfoMap = new HashMap<String, Object>();

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-Username")).willReturn("USERNAME");
        given(request.getHeader("X-OpenIDM-Password")).willReturn("PASSWORD");
        given(messageInfo.getMap()).willReturn(messageInfoMap);
        messageInfoMap.put(AuditTrail.AUDIT_INFO_KEY, auditInfoMap);

        given(authResult.isAuthenticated()).willReturn(true);
        given(authenticator.authenticate(eq("USERNAME"), eq("PASSWORD"),
                Matchers.<Context>anyObject())).willReturn(authResult);

        //When
        AuthStatus authStatus = module.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals("USERNAME", clientSubject.getPrincipals().iterator().next().getName());
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void shouldValidateRequestWhenAuthenticationFailed() throws ResourceException, AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        AuthenticatorResult authResult = mock(AuthenticatorResult.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();
        Map<String, Object> auditInfoMap = new HashMap<String, Object>();

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader("X-OpenIDM-Username")).willReturn("USERNAME");
        given(request.getHeader("X-OpenIDM-Password")).willReturn("PASSWORD");
        given(messageInfo.getMap()).willReturn(messageInfoMap);
        messageInfoMap.put(AuditTrail.AUDIT_INFO_KEY, auditInfoMap);

        given(authResult.isAuthenticated()).willReturn(false);
        given(authenticator.authenticate(eq("USERNAME"), eq("PASSWORD"),
                Matchers.<Context>anyObject())).willReturn(authResult);

        //When
        AuthStatus authStatus = module.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertTrue(clientSubject.getPrincipals().isEmpty());
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldSecureResponse() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();

        //When
        AuthStatus authStatus = module.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
        verifyZeroInteractions(messageInfo);
    }
}
