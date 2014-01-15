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

import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.servlet.SecurityContextFactory;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    private IDMServerAuthModule getIDMServerAuthModule() {
        return new IDMServerAuthModule() {
            @Override
            protected void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
                    CallbackHandler handler, JsonValue options) throws AuthException {
            }

            @Override
            protected AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject,
                    SecurityContextMapper securityContextWrapper) throws AuthException {
                securityContextWrapper.setUserId("USER_ID");
                securityContextWrapper.setUsername("USERNAME");
                securityContextWrapper.setRoles(roles);
                securityContextWrapper.setResource("RESOURCE");
                return AuthStatus.SEND_CONTINUE;
            }

            @Override
            public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
            }
        };
    }

    @Test
    public void shouldGetSupportedMessageTypes() {

        //Given
        IDMServerAuthModule idmServerAuthModule = getIDMServerAuthModule();

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
        IDMServerAuthModule idmServerAuthModule = getIDMServerAuthModule();
        MessageInfo messageInfo = mock(MessageInfo.class);
        Map<String, Object> messageInfoMap = spy(new HashMap<String, Object>());
        Map<String, Object> contextMap = spy(new HashMap<String, Object>());
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();

        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfo.getMap()).willReturn(messageInfoMap);
        given(messageInfoMap.get(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT)).willReturn(contextMap);

        //When
        AuthStatus authStatus = idmServerAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        verify(contextMap).put(SecurityContext.AUTHZID_ID, "USER_ID");
        verify(contextMap).put(SecurityContext.AUTHZID_ROLES, roles);
        verify(contextMap).put(SecurityContext.AUTHZID_COMPONENT, "RESOURCE");
        verify(messageInfoMap).put(SecurityContextFactory.ATTRIBUTE_AUTHCID, "USERNAME");

        assertEquals(authStatus, AuthStatus.SEND_CONTINUE);
    }
}
