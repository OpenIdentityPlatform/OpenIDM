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
 * Copyright 2016 ForgeRock AS
 * Portions copyright 2026 3A Systems LLC
 */
package org.forgerock.openidm.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Requests.newActionRequest;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.openidm.auth.AuthenticationService.Action;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.security.auth.message.MessageInfo;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.http.Filter;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.idp.impl.IdentityProviderService;
import org.forgerock.openidm.idp.impl.IdentityProviderServiceException;
import org.forgerock.openidm.idp.impl.ProviderConfigMapper;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AuthenticationServiceTest {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper()
                    .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final String OPENID_CONNECT = "OPENID_CONNECT";
    private static final String OAUTH = "OAUTH";
    private static final String AUTHENTICATION_PATH = "authentication";
    private static final JsonPointer AUTH_MODULES = new JsonPointer("serverAuthContext/authModules");

    private JsonValue amendedAuthentication;
    private JsonValue googleIdentityProvider;
    private JsonValue authenticationJson;

    private AuthenticationService authenticationService;

    @BeforeMethod
    public void setUp() throws Exception {
        // amendedAuthentication.json is what the configuration should look after injection
        amendedAuthentication = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/config/amendedAuthentication.json"), Map.class));
        // identityProvider-oidc.json is a sample identityProvider configuration
        googleIdentityProvider = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/config/identityProvider-oidc.json"), Map.class));
        // authentication.json is what a sample authentication.json file will look like on the filesystem
        // Note: The authentication.json file here has been modified to include only the minimum config needed to test
        // the functionality of AuthenticationService.java#amendAuthConfig()
        authenticationJson = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/config/authentication.json"), Map.class));
        // Instantiate the object to be used with proper mocked IdentityProviderService
        authenticationService = new AuthenticationService();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        amendedAuthentication = null;
        googleIdentityProvider = null;
        authenticationJson = null;
    }

    @Test
    public void testAmendAuthConfig() throws Exception {
        // Mock of IdentityProviderService
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        // Add the google provider to the list of provider configs
        final List<ProviderConfig> openIdProviderConfigs = new ArrayList<>();
        openIdProviderConfigs.add(ProviderConfigMapper.toProviderConfig(googleIdentityProvider));

        // Whenever we call getIdentityProviders() return the test case configs
        when(identityProviderService.getIdentityProviders()).thenReturn(openIdProviderConfigs);
        when(identityProviderService.getIdentityProviderByType(OPENID_CONNECT)).thenReturn(openIdProviderConfigs);

        // Instantiate the object to be used with proper mocked IdentityProviderService
        authenticationService.bindIdentityProviderService(identityProviderService);
        // the reference is bound before the component is activated with its configuration
        authenticationService.setConfig(authenticationJson);

        // Call the amendAuthConfig to see the configuration of authentication.json be modified with
        // the injected identityProvider config from the IdentityProviderService
        authenticationService.amendAuthConfig(authenticationJson.get(AUTH_MODULES));

        // Assert that the authenticationJson in memory has been modified to have the resolver that is shown in
        // the amendedAuthentication configuration

        assertThat(amendedAuthentication.get(AUTH_MODULES).asList())
                .containsAll(authenticationJson.get(AUTH_MODULES).asList());

    }

    @Test
    public void testAmendAuthConfigWithTwoAuthTypes() throws Exception {

        // Mock of IdentityProviderService
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        // Add the google provider to the list of provider configs
        final List<ProviderConfig> openIdProviderConfigs = new ArrayList<>();
        openIdProviderConfigs.add(ProviderConfigMapper.toProviderConfig(googleIdentityProvider));


        // Add Facebook provider of type OAuth 2
        final List<ProviderConfig> oAuthProviderConfigs = new ArrayList<>();
        oAuthProviderConfigs.add(ProviderConfigMapper.toProviderConfig(
                        json(OBJECT_MAPPER.readValue(getClass()
                                .getResource("/config/identityProvider-oauth.json"), Map.class))));

        // Whenever we call getIdentityByType("OPENID_CONNECT") return the test case configs for openid_connect
        when(identityProviderService.getIdentityProviderByType(OPENID_CONNECT)).thenReturn(openIdProviderConfigs);
        // Whenever we call getIdentityByType("OAUTH") return the test case configs for openid_connect
        when(identityProviderService.getIdentityProviderByType(OAUTH)).thenReturn(oAuthProviderConfigs);

        final List<ProviderConfig> allConfigs = new ArrayList<>();
        allConfigs.addAll(oAuthProviderConfigs);
        allConfigs.addAll(openIdProviderConfigs);

        when(identityProviderService.getIdentityProviders()).thenReturn(allConfigs);

        // Instantiate the object to be used with proper mocked IdentityProviderService
        authenticationService.bindIdentityProviderService(identityProviderService);
        // the reference is bound before the component is activated with its configuration
        authenticationService.setConfig(authenticationJson);

        // Call the amendAuthConfig to see the configuration of authentication.json be modified with
        // the injected identityProvider config from the IdentityProviderService
        authenticationService.amendAuthConfig(authenticationJson.get(AUTH_MODULES));

        // Assert that the authenticationJson in memory has been modified to have the resolver that is shown in
        // the amendedAuthentication configuration
        assertThat(amendedAuthentication.isEqualTo(authenticationJson)).isTrue();
    }

    @Test
    public void testNoProviderConfigsToInject() throws Exception {
        // This should only have one auth module declared, the auth module that was explicitly defined,
        // the explicitOIDCModule.json holds a config for an explicitly declared authentication module
        final JsonValue explicitAuthModule =
                json(OBJECT_MAPPER.readValue(getClass().getResource("/config/explicitOIDCModule.json"), Map.class));
        final JsonValue authenticationJsonNoMod =
                json(object(field("serverAuthContext", object(field("authModules", array(explicitAuthModule.getObject()))))));

        // Mock of IdentityProviderService
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        // Create an empty providerConfigs list to simulate no identityProviders
        final List<ProviderConfig> providerConfigs = new ArrayList<>();

        // Whenever we call getIdentityProviders() return the test case configs
        when(identityProviderService.getIdentityProviders()).thenReturn(providerConfigs);

        authenticationService.bindIdentityProviderService(identityProviderService);
        // the reference is bound before the component is activated with its configuration
        authenticationService.setConfig(authenticationJson);

        // Call the amendAuthConfig to see the configuration of authentication.json be modified with
        // the injected identityProvider config from the IdentityProviderService; in this test case
        // we should see no modifications taking place and the config should not have been modified
        authenticationService.amendAuthConfig(authenticationJson.get(AUTH_MODULES));

        // Assert that the authenticationJson has not been modified from the original
        // there should only be one auth module, an OPENID_CONNECT module that was created in explicitly
        // and not managed by the IdentityProviderService
        assertThat(authenticationJson.isEqualTo(authenticationJsonNoMod)).isTrue();
    }

    @Test
    public void testReadInstance() throws Exception {
        // set up
        final JsonValue providerList = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/config/providersList.json"), Map.class));

        // Instantiate the object to be used
        AuthenticationService authenticationService = new AuthenticationService();

        // Set the config.
        authenticationService.setConfig(amendedAuthentication);
        authenticationService.setAmendedConfig(amendedAuthentication);

        // Read request
        final ReadRequest readRequest = newReadRequest(ResourcePath.resourcePath(AUTHENTICATION_PATH));

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                authenticationService.readInstance(new RootContext(), readRequest);
        // then
        final ResourceResponse resourceResponse = promise.get();
        assertThat(resourceResponse.getContent().isEqualTo(providerList)).isTrue();

    }

    @Test
    public void amendAuthConfigShouldRemoveSocialProvidersModuleWhenIdentityProviderServiceIsNotConfigured() {
        // Instantiate the object to be used with proper mocked IdentityProviderService
        final AuthenticationService authenticationService = new AuthenticationService();
        authenticationService.setConfig(authenticationJson);

        // Call the amendAuthConfig to see the configuration of authentication.json be unmodified
        authenticationService.amendAuthConfig(authenticationJson.get(AUTH_MODULES));

        // Assert that the authenticationJson in memory has been modified to only have the stand-alone
        // OPENID_CONNECT module declared separately and wasn't generated as part of the IdentityProviderService
        assertThat(authenticationJson.get(AUTH_MODULES).size()).isEqualTo(1);
    }

    @Test
    public void bindIdentityProviderServiceShouldRegisterListener() throws Exception {
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        authenticationService.bindIdentityProviderService(identityProviderService);

        verify(identityProviderService).registerIdentityProviderListener(authenticationService);
    }

    @Test
    public void unbindIdentityProviderServiceShouldUnregisterListenerAndStopInjectingProviders() throws Exception {
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
        final List<ProviderConfig> openIdProviderConfigs = new ArrayList<>();
        openIdProviderConfigs.add(ProviderConfigMapper.toProviderConfig(googleIdentityProvider));
        when(identityProviderService.getIdentityProviders()).thenReturn(openIdProviderConfigs);

        authenticationService.bindIdentityProviderService(identityProviderService);
        authenticationService.unbindIdentityProviderService(identityProviderService);
        authenticationService.setConfig(authenticationJson);
        authenticationService.amendAuthConfig(authenticationJson.get(AUTH_MODULES));

        verify(identityProviderService).unregisterIdentityProviderListener(authenticationService);
        // only the stand-alone OPENID_CONNECT module is left, no module was generated from the provider
        assertThat(authenticationJson.get(AUTH_MODULES).size()).isEqualTo(1);
    }

    @Test
    public void identityProviderServiceBindAndUnbindShouldRebuildAuthModules() throws Exception {
        final AuthenticationService service = spy(new AuthenticationService());
        doNothing().when(service).identityProviderConfigChanged();
        final IdentityProviderService first = mock(IdentityProviderService.class);
        final IdentityProviderService second = mock(IdentityProviderService.class);

        service.bindIdentityProviderService(first);
        verify(service, times(1)).identityProviderConfigChanged();

        // DS replaces a dynamic 0..1 reference by binding the new service before unbinding the old one
        service.bindIdentityProviderService(second);
        service.unbindIdentityProviderService(first);
        verify(service, times(2)).identityProviderConfigChanged();

        service.unbindIdentityProviderService(second);
        verify(service, times(3)).identityProviderConfigChanged();
    }

    @Test
    public void amendAuthConfigShouldSkipProvidersOfUnsupportedType() throws Exception {
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
        final List<ProviderConfig> providerConfigs = new ArrayList<>();
        providerConfigs.add(ProviderConfigMapper.toProviderConfig(googleIdentityProvider));
        // not an IDMAuthModule name at all
        providerConfigs.add(ProviderConfigMapper.toProviderConfig(
                googleIdentityProvider.copy().put("name", "unknown").put("type", "UNKNOWN")));
        // an IDMAuthModule name, but not one a social auth module can be generated for
        providerConfigs.add(ProviderConfigMapper.toProviderConfig(
                googleIdentityProvider.copy().put("name", "managed").put("type", "MANAGED_USER")));
        when(identityProviderService.getIdentityProviders()).thenReturn(providerConfigs);

        authenticationService.bindIdentityProviderService(identityProviderService);
        authenticationService.setConfig(authenticationJson);
        authenticationService.amendAuthConfig(authenticationJson.get(AUTH_MODULES));

        // the stand-alone OPENID_CONNECT module plus the one generated from the supported provider
        assertThat(authenticationJson.get(AUTH_MODULES).size()).isEqualTo(2);
        assertThat(authenticationJson.get(AUTH_MODULES).get(1).get("name").asString()).isEqualTo(OPENID_CONNECT);
    }

    @Test
    public void identityProviderConfigChangedShouldPublishOnlyAfterTheFilterIsSet() throws Exception {
        final AuthenticationService service = spy(new AuthenticationService());
        doReturn(mock(Filter.class)).when(service).configureAuthenticationFilter(any(JsonValue.class));
        final AuthFilterWrapper authFilterWrapper = mock(AuthFilterWrapper.class);
        setField(service, "authFilterWrapper", authFilterWrapper);
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
        final List<ProviderConfig> providerConfigs = new ArrayList<>();
        providerConfigs.add(ProviderConfigMapper.toProviderConfig(googleIdentityProvider));
        when(identityProviderService.getIdentityProviders()).thenReturn(providerConfigs);
        // an enabled stand-alone module whose resolver does not carry its type
        final JsonValue explicitModule = authenticationJson.get(AUTH_MODULES).get(1);
        explicitModule.put("enabled", true);
        explicitModule.get("properties").get("resolvers").get(0).remove("type");

        service.bindIdentityProviderService(identityProviderService);
        service.setConfig(authenticationJson);
        service.identityProviderConfigChanged();
        // the type is in the published config before any request reads it
        final JsonValue first = (JsonValue) getField(service, "amendedConfig");
        for (final JsonValue module : first.get(AUTH_MODULES)) {
            if (OPENID_CONNECT.equals(module.get("name").asString())) {
                assertThat(module.get("properties").get("resolvers").get(0).get("type").asString())
                        .isEqualTo(OPENID_CONNECT);
            }
        }
        final Object snapshot = first.copy().getObject();
        // a second rebuild must not modify the values the first one published
        service.identityProviderConfigChanged();
        assertThat(getField(service, "amendedConfig")).isNotSameAs(first);
        assertThat(first.getObject()).isEqualTo(snapshot);

        assertProviders(service, "explicit-oidc", "oidc");

        // a rebuild whose filter is not set leaves the published config describing the filter in force
        providerConfigs.add(ProviderConfigMapper.toProviderConfig(json(OBJECT_MAPPER.readValue(
                getClass().getResource("/config/identityProvider-oauth.json"), Map.class))));
        doThrow(new IllegalStateException("filter not set")).when(authFilterWrapper).setFilter(any(Filter.class));
        try {
            service.identityProviderConfigChanged();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("filter not set");
        }

        assertProviders(service, "explicit-oidc", "oidc");
    }

    @Test
    public void failedActivationShouldLeaveNothingForUnbindToRebuild() throws Exception {
        final AuthenticationService service = spy(new AuthenticationService());
        // the first filter, at activation, cannot be built; a later one could
        doThrow(new AuthenticationException("invalid module"))
                .doReturn(mock(Filter.class))
                .when(service).configureAuthenticationFilter(any(JsonValue.class));
        final AuthFilterWrapper authFilterWrapper = mock(AuthFilterWrapper.class);
        setField(service, "authFilterWrapper", authFilterWrapper);
        final EnhancedConfig enhancedConfig = mock(EnhancedConfig.class);
        when(enhancedConfig.getConfigurationAsJson(any(ComponentContext.class))).thenReturn(authenticationJson);
        setField(service, "enhancedConfig", enhancedConfig);
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        service.bindIdentityProviderService(identityProviderService);
        try {
            service.activate(mock(ComponentContext.class));
            fail("Expected IdentityProviderServiceException");
        } catch (IdentityProviderServiceException e) {
            assertThat(e.getCause()).isInstanceOf(AuthenticationException.class);
        }
        // DS unbinds the references of a component whose activation failed
        service.unbindIdentityProviderService(identityProviderService);

        verify(authFilterWrapper, never()).setFilter(any(Filter.class));
    }

    @Test
    public void failedActivationOnARuntimeExceptionShouldLeaveNothingForUnbindToRebuild() throws Exception {
        final AuthenticationService service = spy(new AuthenticationService());
        doThrow(new IllegalStateException("invalid module"))
                .doReturn(mock(Filter.class))
                .when(service).configureAuthenticationFilter(any(JsonValue.class));
        final AuthFilterWrapper authFilterWrapper = mock(AuthFilterWrapper.class);
        setField(service, "authFilterWrapper", authFilterWrapper);
        final EnhancedConfig enhancedConfig = mock(EnhancedConfig.class);
        when(enhancedConfig.getConfigurationAsJson(any(ComponentContext.class))).thenReturn(authenticationJson);
        setField(service, "enhancedConfig", enhancedConfig);
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        service.bindIdentityProviderService(identityProviderService);
        try {
            service.activate(mock(ComponentContext.class));
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("invalid module");
        }
        // DS unbinds the references of a component whose activation failed
        service.unbindIdentityProviderService(identityProviderService);

        verify(authFilterWrapper, never()).setFilter(any(Filter.class));
    }

    @Test
    public void activationShouldTolerateMalformedResolversOfDisabledModules() throws Exception {
        for (final Object malformed : new Object[] { array(), object(), array("not a resolver") }) {
            final AuthenticationService service = spy(new AuthenticationService());
            doReturn(mock(Filter.class)).when(service).configureAuthenticationFilter(any(JsonValue.class));
            final AuthFilterWrapper authFilterWrapper = mock(AuthFilterWrapper.class);
            setField(service, "authFilterWrapper", authFilterWrapper);
            final JsonValue config = authenticationJson.copy();
            config.get(AUTH_MODULES).add(object(
                    field("name", OPENID_CONNECT),
                    field("enabled", false),
                    field("properties", object(field("resolvers", malformed)))));
            final EnhancedConfig enhancedConfig = mock(EnhancedConfig.class);
            when(enhancedConfig.getConfigurationAsJson(any(ComponentContext.class))).thenReturn(config);
            setField(service, "enhancedConfig", enhancedConfig);

            // the rebuild sets the resolver type after the filter is set: it must not fail the activation
            service.activate(mock(ComponentContext.class));

            verify(authFilterWrapper).setFilter(any(Filter.class));
            // both OPENID_CONNECT modules are disabled
            assertProviders(service);
        }
    }

    private static void assertProviders(final AuthenticationService service, final String... names)
            throws Exception {
        final JsonValue providers = service.readInstance(new RootContext(), newReadRequest(AUTHENTICATION_PATH))
                .get().getContent().get(IdentityProviderService.PROVIDERS);
        assertThat(providers.size()).isEqualTo(names.length);
        for (int i = 0; i < names.length; i++) {
            assertThat(providers.get(i).get("name").asString()).isEqualTo(names[i]);
            // the type is set during the rebuild, not by the read
            assertThat(providers.get(i).get("type").asString()).isEqualTo(OPENID_CONNECT);
        }
    }

    private static Object getField(final AuthenticationService service, final String name) throws Exception {
        final Field field = AuthenticationService.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(service);
    }

    private static void setField(final AuthenticationService service, final String name, final Object value)
            throws Exception {
        final Field field = AuthenticationService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }

    /**
     * Tests that the attribute that {@link JwtSessionModule#isLogoutRequest(MessageInfo)} expects is present in the
     * attributesContext.
     *
     * @throws Exception
     */
    @Test
    public void testLogoutAction() throws Exception {
        AttributesContext context = new AttributesContext(new RootContext());
        Promise<ActionResponse, ResourceException> promise =
                authenticationService.actionInstance(context, newActionRequest("", Action.logout.name()));
        AssertJPromiseAssert.assertThat(promise).succeeded();
        assertThat(promise.get().getJsonContent().get("success").asBoolean()).isTrue();
        assertThat(context.getAttributes()).containsEntry(JwtSessionModule.LOGOUT_SESSION_REQUEST_ATTRIBUTE_NAME, true);
    }
}