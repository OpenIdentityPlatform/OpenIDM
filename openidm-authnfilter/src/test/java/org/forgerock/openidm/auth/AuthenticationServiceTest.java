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
 */
package org.forgerock.openidm.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.idp.impl.IdentityProviderService;
import org.forgerock.openidm.idp.impl.ProviderConfigMapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuthenticationServiceTest {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().configure(
            JsonParser.Feature.ALLOW_COMMENTS, true).disable(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonValue amendedAuthentication;
    private JsonValue googleIdentityProvider;
    private JsonValue authenticationJson;

    @BeforeMethod
    public void setUp() throws Exception {
        // amendedAuthentication.json is what the configuration should look after injection
        amendedAuthentication = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/config/amendedAuthentication.json"), Map.class));
        // identityProvider-google.json is a sample identityProvider configuration
        googleIdentityProvider = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/config/identityProvider-google.json"), Map.class));
        // authentication.json is what a sample authentication.json file will look like on the filesystem
        // Note: The authentication.json file here has been modified to include only the minimum config needed to test
        // the functionality of AuthenticationService.java#amendAuthConfig()
        authenticationJson = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/config/authentication.json"), Map.class));

    }

    @Test
    public void testAmendAuthConfig() throws Exception {
        // Mock of IdentityProviderService
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        // Add the google provider to the list of provider configs
        final List<ProviderConfig> providerConfigs = new ArrayList<>();
        final ProviderConfig googleProviderConfig = ProviderConfigMapper.toProviderConfig(googleIdentityProvider);
        providerConfigs.add(googleProviderConfig);

        // Whenever we call getIdentityProviders() return the test case configs
        when(identityProviderService.getIdentityProviders()).thenReturn(providerConfigs);

        // Instantiate the object to be used with proper mocked IdentityProviderService
        AuthenticationService authenticationService = new AuthenticationService();
        authenticationService.bindIdentityProviderService(identityProviderService);

        // Call the amendAuthConfig to see the configuration of authentication.json be modified with
        // the injected identityProvider config from the IdentityProviderService
        authenticationService.amendAuthConfig(authenticationJson.get("authModules"));

        // Assert that the authenticationJson in memory has been modified to have the resolver that is shown in
        // the amendedAuthentication configuration
        assertThat(amendedAuthentication.isEqualTo(authenticationJson.get("authModules").get(0))).isTrue();
    }

    @Test
    public void testNoProviderConfigsToInject() throws Exception {
        // Copy the authenticationJson for later comparison to prove unmodified
        final JsonValue authenticationJsonNoMod = authenticationJson.copy();

        // Mock of IdentityProviderService
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        // Create an empty providerConfigs list to simulate no identityProviders
        final List<ProviderConfig> providerConfigs = new ArrayList<>();

        // Whenever we call getIdentityProviders() return the test case configs
        when(identityProviderService.getIdentityProviders()).thenReturn(providerConfigs);

        // Instantiate the object to be used with proper mocked IdentityProviderService
        AuthenticationService authenticationService = new AuthenticationService();
        authenticationService.bindIdentityProviderService(identityProviderService);

        // Call the amendAuthConfig to see the configuration of authentication.json be modified with
        // the injected identityProvider config from the IdentityProviderService; in this test case
        // we should see no modifications taking place and the config should not have been modified
        authenticationService.amendAuthConfig(authenticationJson.get("authModules"));

        // Assert that the authenticationJson has not been modified
        assertThat(authenticationJson.isEqualTo(authenticationJsonNoMod)).isTrue();
    }
}