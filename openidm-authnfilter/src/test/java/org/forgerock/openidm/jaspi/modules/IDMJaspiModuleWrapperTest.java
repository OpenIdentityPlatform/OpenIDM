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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.script.ScriptEntry;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.openidm.jaspi.modules.RoleCalculator.GroupComparison.equals;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertEquals;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.AuthModuleConstructor;
import static org.forgerock.openidm.jaspi.modules.RoleCalculator.GroupComparison.caseInsensitive;
import static org.forgerock.openidm.jaspi.modules.RoleCalculator.*;
import static org.testng.Assert.assertTrue;

public class IDMJaspiModuleWrapperTest {

    private IDMJaspiModuleWrapper moduleWrapper;

    private AuthModuleConstructor authModuleConstructor;
    private RoleCalculatorFactory roleCalculatorFactory;
    private AugmentationScriptExecutor augmentationScriptExecutor;

    private ServerAuthModule authModule;
    private ConnectionFactory connectionFactory;
    private ServerContext context;
    private RoleCalculator roleCalculator;
    private ScriptEntry augmentScript;

    @BeforeMethod
    public void setUp() {

        authModuleConstructor = mock(AuthModuleConstructor.class);
        roleCalculatorFactory = mock(RoleCalculatorFactory.class);
        augmentationScriptExecutor = mock(AugmentationScriptExecutor.class);

        authModule = mock(ServerAuthModule.class);
        connectionFactory = mock(ConnectionFactory.class);
        context = mock(ServerContext.class);
        roleCalculator = mock(RoleCalculator.class);
        augmentScript = mock(ScriptEntry.class);

        moduleWrapper = new IDMJaspiModuleWrapper(authModuleConstructor, roleCalculatorFactory,
                augmentationScriptExecutor) {
            @Override
            ConnectionFactory getConnectionFactory() {
                return connectionFactory;
            }

            @Override
            ServerContext createServerContext() {
                return context;
            }

            @Override
            ScriptEntry getAugmentScript(JsonValue scriptConfig) throws JaspiAuthException {
                return augmentScript;
            }
        };
    }

    @Test
    public void shouldGetSupportedMessageTypes() throws AuthException {

        //Given
        initialiseModule(false);

        //When
        moduleWrapper.getSupportedMessageTypes();

        //Then
        verify(authModule).getSupportedMessageTypes();
    }

    @Test
    public void shouldInitialize() throws AuthException {

        //Given
        MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        List<String> defaultRoles = new ArrayList<String>();
        Map<String, List<String>> roleMapping = new HashMap<String, List<String>>();
        Map<String, Object> options = json(object(
                field("authModuleClassName", "CLASS_NAME"),
                field("queryOnResource", "QUERY_ON_RESOURCE"),
                field("clientIPHeader", "CLIENT_IP_HEADER"),
                field("propertyMapping", object(
                        field("authenticationId", "AUTHN_ID"),
                        field("groupMembership", "GROUP_MEMBERSHIP")
                )),
                field("defaultUserRoles", defaultRoles),
                field("groupRoleMapping", roleMapping),
                field("groupComparisonMethod", "caseInsensitive"),
                field("augmentSecurityContext", object())
        )).asMap();

        given(authModuleConstructor.construct("CLASS_NAME")).willReturn(authModule);

        //When
        moduleWrapper.initialize(requestMessagePolicy, responseMessagePolicy, handler, options);

        //Then
        verify(authModule).initialize(requestMessagePolicy, responseMessagePolicy, handler, options);
        verify(roleCalculatorFactory).create(connectionFactory, context, "QUERY_ON_RESOURCE", "AUTHN_ID",
                "GROUP_MEMBERSHIP", defaultRoles, roleMapping, caseInsensitive);
    }

    @Test
    public void shouldInitializeWithoutAugmentScript() throws AuthException {

        //Given
        MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        List<String> defaultRoles = new ArrayList<String>();
        Map<String, List<String>> roleMapping = new HashMap<String, List<String>>();
        Map<String, Object> options = json(object(
                field("authModuleClassName", "CLASS_NAME"),
                field("queryOnResource", "QUERY_ON_RESOURCE"),
                field("clientIPHeader", "CLIENT_IP_HEADER"),
                field("propertyMapping", object(
                        field("authenticationId", "AUTHN_ID"),
                        field("groupMembership", "GROUP_MEMBERSHIP")
                )),
                field("defaultUserRoles", defaultRoles),
                field("groupRoleMapping", roleMapping),
                field("groupComparisonMethod", "caseInsensitive")
        )).asMap();

        given(authModuleConstructor.construct("CLASS_NAME")).willReturn(authModule);

        //When
        moduleWrapper.initialize(requestMessagePolicy, responseMessagePolicy, handler, options);

        //Then
        verify(authModule).initialize(requestMessagePolicy, responseMessagePolicy, handler, options);
        verify(roleCalculatorFactory).create(connectionFactory, context, "QUERY_ON_RESOURCE", "AUTHN_ID",
                "GROUP_MEMBERSHIP", defaultRoles, roleMapping, caseInsensitive);
    }

    @Test
    public void shouldInitializeWithNoDefaultRoles() throws AuthException {

        //Given
        MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        Map<String, List<String>> roleMapping = new HashMap<String, List<String>>();
        Map<String, Object> options = json(object(
                field("authModuleClassName", "CLASS_NAME"),
                field("queryOnResource", "QUERY_ON_RESOURCE"),
                field("clientIPHeader", "CLIENT_IP_HEADER"),
                field("propertyMapping", object(
                        field("authenticationId", "AUTHN_ID"),
                        field("groupMembership", "GROUP_MEMBERSHIP")
                )),
                field("groupRoleMapping", roleMapping),
                field("groupComparisonMethod", "caseInsensitive"),
                field("augmentSecurityContext", object())
        )).asMap();

        given(authModuleConstructor.construct("CLASS_NAME")).willReturn(authModule);

        //When
        moduleWrapper.initialize(requestMessagePolicy, responseMessagePolicy, handler, options);

        //Then
        verify(authModule).initialize(requestMessagePolicy, responseMessagePolicy, handler, options);
        ArgumentCaptor<List> defaultRolesCaptor = ArgumentCaptor.forClass(List.class);
        verify(roleCalculatorFactory).create(eq(connectionFactory), eq(context), eq("QUERY_ON_RESOURCE"),
                eq("AUTHN_ID"), eq("GROUP_MEMBERSHIP"), defaultRolesCaptor.capture(), eq(roleMapping),
                eq(caseInsensitive));
        assertTrue(defaultRolesCaptor.getValue().isEmpty());
    }

    @Test
    public void shouldInitializeWithNoGroupRoleMapping() throws AuthException {

        //Given
        MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        List<String> defaultRoles = new ArrayList<String>();
        Map<String, Object> options = json(object(
                field("authModuleClassName", "CLASS_NAME"),
                field("queryOnResource", "QUERY_ON_RESOURCE"),
                field("clientIPHeader", "CLIENT_IP_HEADER"),
                field("propertyMapping", object(
                        field("authenticationId", "AUTHN_ID"),
                        field("groupMembership", "GROUP_MEMBERSHIP")
                )),
                field("defaultUserRoles", defaultRoles),
                field("groupComparisonMethod", "caseInsensitive"),
                field("augmentSecurityContext", object())
        )).asMap();

        given(authModuleConstructor.construct("CLASS_NAME")).willReturn(authModule);

        //When
        moduleWrapper.initialize(requestMessagePolicy, responseMessagePolicy, handler, options);

        //Then
        verify(authModule).initialize(requestMessagePolicy, responseMessagePolicy, handler, options);
        ArgumentCaptor<Map> roleMappingCaptor = ArgumentCaptor.forClass(Map.class);
        verify(roleCalculatorFactory).create(eq(connectionFactory), eq(context), eq("QUERY_ON_RESOURCE"),
                eq("AUTHN_ID"), eq("GROUP_MEMBERSHIP"), eq(defaultRoles), roleMappingCaptor.capture(),
                eq(caseInsensitive));
        assertTrue(roleMappingCaptor.getValue().isEmpty());
    }

    @Test
    public void shouldInitializeWithNoGroupComparisonMethod() throws AuthException {

        //Given
        MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        List<String> defaultRoles = new ArrayList<String>();
        Map<String, List<String>> roleMapping = new HashMap<String, List<String>>();
        Map<String, Object> options = json(object(
                field("authModuleClassName", "CLASS_NAME"),
                field("queryOnResource", "QUERY_ON_RESOURCE"),
                field("clientIPHeader", "CLIENT_IP_HEADER"),
                field("propertyMapping", object(
                        field("authenticationId", "AUTHN_ID"),
                        field("groupMembership", "GROUP_MEMBERSHIP")
                )),
                field("defaultUserRoles", defaultRoles),
                field("groupRoleMapping", roleMapping),
                field("augmentSecurityContext", object())
        )).asMap();

        given(authModuleConstructor.construct("CLASS_NAME")).willReturn(authModule);

        //When
        moduleWrapper.initialize(requestMessagePolicy, responseMessagePolicy, handler, options);

        //Then
        verify(authModule).initialize(requestMessagePolicy, responseMessagePolicy, handler, options);
        verify(roleCalculatorFactory).create(connectionFactory, context, "QUERY_ON_RESOURCE", "AUTHN_ID",
                "GROUP_MEMBERSHIP", defaultRoles, roleMapping, equals);
    }

    private void initialiseModule(boolean withAugmentScript) throws AuthException {

        MessagePolicy requestMessagePolicy = mock(MessagePolicy.class);
        MessagePolicy responseMessagePolicy = mock(MessagePolicy.class);
        CallbackHandler handler = mock(CallbackHandler.class);
        Map<String, Object> options = new HashMap<String, Object>();

        options.put("clientIPHeader", "CLIENT_IP_HEADER");
        if (withAugmentScript) {
            options.put("augmentSecurityContext", new HashMap<String, Object>());
        }

        given(authModuleConstructor.construct(anyString())).willReturn(authModule);
        given(roleCalculatorFactory.create(eq(connectionFactory), eq(context), anyString(), anyString(), anyString(),
                anyListOf(String.class), Matchers.<Map<String, List<String>>>anyObject(),
                Matchers.<GroupComparison>anyObject())).willReturn(roleCalculator);

        moduleWrapper.initialize(requestMessagePolicy, responseMessagePolicy, handler, options);
    }

    @Test
    public void shouldValidateRequestWhenAuthModuleReturnsSendFailure() throws AuthException {

        //Given
        initialiseModule(true);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoParams = new HashMap<String, Object>();

        given(messageInfo.getMap()).willReturn(messageInfoParams);
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        AuthStatus authStatus = moduleWrapper.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(messageInfoParams.get(IDMAuthenticationAuditLogger.LOG_CLIENT_IP_HEADER_KEY), "CLIENT_IP_HEADER");
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verifyZeroInteractions(roleCalculator, augmentationScriptExecutor);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test (expectedExceptions = JaspiAuthException.class)
    public void validateRequestShouldThrowJaspiAuthExceptionWhenPrincipalNotSet() throws AuthException {

        //Given
        initialiseModule(true);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoParams = new HashMap<String, Object>();

        given(messageInfo.getMap()).willReturn(messageInfoParams);
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SUCCESS);

        //When
        moduleWrapper.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        // Expected JaspiAuthException
    }

    @Test
    public void shouldValidateRequest() throws AuthException, ResourceException {

        //Given
        initialiseModule(true);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoParams = new HashMap<String, Object>();
        Map<String, Object> contextMap = new HashMap<String, Object>();
        SecurityContextMapper securityContextMapper = mock(SecurityContextMapper.class);

        given(messageInfo.getMap()).willReturn(messageInfoParams);
        messageInfoParams.put(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT, contextMap);
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SUCCESS);
        clientSubject.getPrincipals().add(new Principal() {
            @Override
            public String getName() {
                return "PRINCIPAL";
            }
        });
        given(roleCalculator.calculateRoles("PRINCIPAL", messageInfo)).willReturn(securityContextMapper);
        given(securityContextMapper.getAuthenticationId()).willReturn("PRINCIPAL");

        //When
        AuthStatus authStatus = moduleWrapper.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(messageInfoParams.get(IDMAuthenticationAuditLogger.LOG_CLIENT_IP_HEADER_KEY), "CLIENT_IP_HEADER");
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(roleCalculator).calculateRoles("PRINCIPAL", messageInfo);
        verify(augmentationScriptExecutor).executeAugmentationScript(eq(augmentScript), Matchers.<JsonValue>anyObject(),
                Matchers.<SecurityContextMapper>anyObject());
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void shouldValidateRequestWithNoAugmentScript() throws AuthException, ResourceException {

        //Given
        initialiseModule(false);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoParams = new HashMap<String, Object>();
        SecurityContextMapper securityContextMapper = mock(SecurityContextMapper.class);

        given(messageInfo.getMap()).willReturn(messageInfoParams);
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SUCCESS);
        clientSubject.getPrincipals().add(new Principal() {
            @Override
            public String getName() {
                return "PRINCIPAL";
            }
        });
        given(roleCalculator.calculateRoles("PRINCIPAL", messageInfo)).willReturn(securityContextMapper);
        given(securityContextMapper.getAuthenticationId()).willReturn("PRINCIPAL");

        //When
        AuthStatus authStatus = moduleWrapper.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(messageInfoParams.get(IDMAuthenticationAuditLogger.LOG_CLIENT_IP_HEADER_KEY), "CLIENT_IP_HEADER");
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(roleCalculator).calculateRoles("PRINCIPAL", messageInfo);
        verifyZeroInteractions(augmentationScriptExecutor);
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void validateRequestShouldReturnSendFailureWhenRoleCalulcationFails() throws AuthException,
            ResourceException {

        //Given
        initialiseModule(true);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoParams = new HashMap<String, Object>();
        SecurityContextMapper securityContextMapper = mock(SecurityContextMapper.class);

        given(messageInfo.getMap()).willReturn(messageInfoParams);
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SUCCESS);
        clientSubject.getPrincipals().add(new Principal() {
            @Override
            public String getName() {
                return "PRINCIPAL";
            }
        });
        ResourceException resourceException = mock(ResourceException.class);
        given(resourceException.isServerError()).willReturn(false);
        given(roleCalculator.calculateRoles("PRINCIPAL", messageInfo)).willThrow(resourceException);
        given(securityContextMapper.getAuthenticationId()).willReturn("PRINCIPAL");

        //When
        AuthStatus authStatus = moduleWrapper.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(messageInfoParams.get(IDMAuthenticationAuditLogger.LOG_CLIENT_IP_HEADER_KEY), "CLIENT_IP_HEADER");
        verify(authModule).validateRequest(messageInfo, clientSubject, serviceSubject);
        verify(roleCalculator).calculateRoles("PRINCIPAL", messageInfo);
        verifyZeroInteractions(augmentationScriptExecutor);
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test (expectedExceptions = JaspiAuthException.class)
    public void validateRequestShouldReturnSendFailureWhenRoleCalulcationFailsDueToServerError() throws AuthException,
            ResourceException {

        //Given
        initialiseModule(true);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        Map<String, Object> messageInfoParams = new HashMap<String, Object>();
        SecurityContextMapper securityContextMapper = mock(SecurityContextMapper.class);

        given(messageInfo.getMap()).willReturn(messageInfoParams);
        given(authModule.validateRequest(messageInfo, clientSubject, serviceSubject))
                .willReturn(AuthStatus.SUCCESS);
        clientSubject.getPrincipals().add(new Principal() {
            @Override
            public String getName() {
                return "PRINCIPAL";
            }
        });
        ResourceException resourceException = mock(ResourceException.class);
        given(resourceException.isServerError()).willReturn(true);
        given(roleCalculator.calculateRoles("PRINCIPAL", messageInfo)).willThrow(resourceException);
        given(securityContextMapper.getAuthenticationId()).willReturn("PRINCIPAL");

        //When
        moduleWrapper.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        // Expected JaspiAuthException
    }

    @Test
    public void shouldSecureResponse() throws AuthException {

        //Given
        initialiseModule(false);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);

        //When
        moduleWrapper.secureResponse(messageInfo, serviceSubject);

        //Then
        verify(authModule).secureResponse(messageInfo, serviceSubject);
    }

    @Test
    public void shouldSecureResponseAndSkipSession() throws AuthException {

        //Given
        initialiseModule(false);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, Object> map = new HashMap<String, Object>();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getHeader(IDMJaspiModuleWrapper.NO_SESSION)).willReturn("true");
        given(messageInfo.getMap()).willReturn(map);

        //When
        moduleWrapper.secureResponse(messageInfo, serviceSubject);

        //Then
        verify(authModule).secureResponse(messageInfo, serviceSubject);
        assertTrue((Boolean) map.get("skipSession"));
    }

    @Test
    public void shouldCleanSubject() throws AuthException {

        //Given
        initialiseModule(false);
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();

        //When
        moduleWrapper.cleanSubject(messageInfo, clientSubject);

        //Then
        verify(authModule).cleanSubject(messageInfo, clientSubject);
    }
}
