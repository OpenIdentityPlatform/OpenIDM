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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertEquals;

/**
 * @author Phill Cunnington
 */
public class IDMServerAuthModuleTest {

    private List<String> roles;

    @BeforeClass
    public void setUp() {
        roles = new ArrayList<String>();
    }

    private IDMServerAuthModule getIDMServerAuthModule(final AuthStatus validateRequestAuthStatus,
            final AuthData returnAuthData) {
        return new IDMServerAuthModule() {
            @Override
            protected void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
                    CallbackHandler handler, JsonValue options) throws AuthException {
            }

            @Override
            protected AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject,
                    AuthData authData) throws AuthException {
                if (returnAuthData != null) {
                    authData.setUserId(returnAuthData.getUserId());
                    authData.setUsername(returnAuthData.getUsername());
                    authData.setRoles(returnAuthData.getRoles());
                    authData.setResource(returnAuthData.getResource());
                }
                return validateRequestAuthStatus;
            }

            @Override
            public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
            }
        };
    }

    @Test
    public void shouldGetSupportedMessageTypes() {

        //Given
        IDMServerAuthModule idmServerAuthModule = getIDMServerAuthModule(null, null);

        //When
        Class[] supportedMessageTypes = idmServerAuthModule.getSupportedMessageTypes();

        //Then
        assertEquals(supportedMessageTypes.length, 2);
        assertEquals(supportedMessageTypes[0], HttpServletRequest.class);
        assertEquals(supportedMessageTypes[1], HttpServletResponse.class);
    }

    @Test
    public void shouldValidateRequestWhenSuccessful() throws AuthException {

        //Given
        AuthData authData = new AuthData();
        authData.setUserId("USER_ID");
        authData.setUsername("USERNAME");
        authData.setRoles(roles);
        authData.setResource("RESOURCE");
        IDMServerAuthModule idmServerAuthModule = getIDMServerAuthModule(AuthStatus.SUCCESS, authData);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Map<String, Object> messageInfoMap = mock(Map.class);
        Map<String, Object> contextMap = mock(Map.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getMap()).willReturn(messageInfoMap);
        given(messageInfoMap.get("org.forgerock.security.context")).willReturn(contextMap);

        //When
        AuthStatus authStatus = idmServerAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(messageInfo).getMap();

        verify(request).setAttribute("openidm.userid", "USER_ID");
        verify(request).setAttribute("openidm.username", "USERNAME");
        verify(request).setAttribute("openidm.roles", roles);
        verify(request).setAttribute("openidm.resource", "RESOURCE");
        verify(request).setAttribute("openidm.authinvoked", "authnfilter");

        verify(contextMap).put("openidm.userid", "USER_ID");
        verify(contextMap).put("openidm.username", "USERNAME");
        verify(contextMap).put("openidm.roles", roles);
        verify(contextMap).put("openidm.resource", "RESOURCE");
        verify(contextMap).put("openidm.authinvoked", "authnfilter");
        verify(contextMap).put("openidm.auth.status", true);

        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void shouldValidateRequestWhenNotSuccessful() throws AuthException {

        //Given
        IDMServerAuthModule idmServerAuthModule = getIDMServerAuthModule(AuthStatus.SEND_FAILURE, null);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Map<String, Object> messageInfoMap = mock(Map.class);
        Map<String, Object> contextMap = mock(Map.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getMap()).willReturn(messageInfoMap);
        given(messageInfoMap.get("org.forgerock.security.context")).willReturn(contextMap);

        //When
        AuthStatus authStatus = idmServerAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verifyZeroInteractions(request);
        verify(contextMap).put("openidm.auth.status", false);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }
}
