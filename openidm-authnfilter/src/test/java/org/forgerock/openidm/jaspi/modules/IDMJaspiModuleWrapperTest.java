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

import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openidm.router.RouteService;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterHelper;

/**
 * Tests IDMUserAuthModule using "internal/user" resource/query
 *
 * @author Phill Cunnington
 * @author brmiller
 */
public class IDMJaspiModuleWrapperTest {

    private ServerContext context;
    private OSGiAuthnFilterHelper authnFilterHelper;
    private IDMJaspiModuleWrapper.AuthModuleConstructor authModuleConstructor;
    private RoleCalculatorFactory roleCalculatorFactory;
    private AugmentationScriptExecutor scriptExecutor;
    private ServerAuthModule authModule;
    private Map options = new HashMap();

    @BeforeMethod
    public void setUp() {
        try {
            context = mock(ServerContext.class);
            RouteService routeService = mock(RouteService.class);
            when(routeService.createServerContext()).thenReturn(context);
            authnFilterHelper = mock(OSGiAuthnFilterHelper.class);
            when(authnFilterHelper.getRouter()).thenReturn(routeService);
            when(authnFilterHelper.getConnectionFactory()).thenReturn(mock(ConnectionFactory.class));

            authModule = mock(ServerAuthModule.class);
            authModuleConstructor = mock(IDMJaspiModuleWrapper.AuthModuleConstructor.class);
            when(authModuleConstructor.construct(anyString())).thenReturn(authModule);
            roleCalculatorFactory = mock(RoleCalculatorFactory.class);
            scriptExecutor = mock(AugmentationScriptExecutor.class);
            options.put("queryOnResource", "foo/user");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
