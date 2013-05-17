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

package org.forgerock.openidm.jaspi.module;

import org.forgerock.openidm.filter.AuthFilter;
import org.forgerock.openidm.filter.UserWrapper;
import org.forgerock.openidm.jaspi.module.ADPassthroughModule;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

import static org.testng.Assert.assertEquals;

public class ADPassthroughModuleTest {

    private ADPassthroughModule adPassthroughModule;

    private AuthFilter authFilter;

    @BeforeClass
    public void setUp() {

        authFilter = mock(AuthFilter.class);

        adPassthroughModule = new ADPassthroughModule(authFilter);
    }

    @Test
    public void shouldGetSupportedMessageTypes() {

        //Given
        Class[] expectedSupportedMessageTypes = new Class[]{HttpServletRequest.class, HttpServletResponse.class};

        //When
        Class[] supportedMessageTypes = adPassthroughModule.getSupportedMessageTypes();

        //Then
        assertEquals(supportedMessageTypes, supportedMessageTypes);
    }

    @Test
    public void shouldSecureResponse() throws AuthException {

        //Given
        MessageInfo messageInfo = null;
        Subject subject = null;

        //When
        AuthStatus authStatus = adPassthroughModule.secureResponse(messageInfo, subject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void shouldValidateRequestForSuccessfulAuthenticationWithSuccess() throws AuthException, ServletException,
            org.forgerock.openidm.filter.AuthException, IOException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = null;
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        UserWrapper userWrapper = mock(UserWrapper.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(authFilter.authenticate(request, response)).willReturn(userWrapper);

        //When
        AuthStatus authStatus = adPassthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void shouldValidateRequestForLogoutWithSendSuccess() throws AuthException, ServletException,
            org.forgerock.openidm.filter.AuthException, IOException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = null;
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        UserWrapper userWrapper = null;

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(authFilter.authenticate(request, response)).willReturn(userWrapper);

        //When
        AuthStatus authStatus = adPassthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }

    @Test
    public void shouldValidateRequestForUnsuccessfulAuthenticationWithSendFailure() throws AuthException,
            ServletException, org.forgerock.openidm.filter.AuthException, IOException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = null;
        Subject serviceSubject = null;
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getResponseMessage()).willReturn(response);
        given(authFilter.authenticate(request, response)).willThrow(org.forgerock.openidm.filter.AuthException.class);

        //When
        AuthStatus authStatus = adPassthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }
}
