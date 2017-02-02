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
 * Copyright 2016-2017 ForgeRock AS.
 */
package org.forgerock.openidm.selfservice.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.idp.impl.IdentityProviderService;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Used to test SelfService.
 */
public class SelfServiceTest {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper()
                    .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonValue kbaConfig;
    private ProviderConfig googleIdentityProvider;
    private JsonValue selfServiceRegistration;
    private JsonValue amendedSelfServiceRegistration;

    @BeforeSuite
    public void setUp() throws Exception {
        kbaConfig = json(OBJECT_MAPPER.readValue(getClass().getResource("/selfservice.kba.json"), Map.class));

        // identityProvider-google.json is a sample identityProvider configuration
        googleIdentityProvider = OBJECT_MAPPER.readValue(
                getClass().getResource("/identityProvider-google.json"), ProviderConfig.class);
        // selfservice-registration.json sample, simplified to only have one stage that is being bested here the
        // "userDetails" stage
        selfServiceRegistration = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/selfservice-registration.json"), Map.class));
        // what the in-memory injected "userDetails" stage should look like
        amendedSelfServiceRegistration = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/amended-selfservice-registration.json"), Map.class));
    }

    @Test
    public void testAmendConfig() throws Exception {
        // Mock of IdentityProviderService
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
        final KbaConfiguration kbaConfiguration =  mock(KbaConfiguration.class);

        // Add the google provider to the list of provider configs
        final List<ProviderConfig> providerConfigs = new ArrayList<>();
        providerConfigs.add(googleIdentityProvider);

        // Whenever we call getIdentityProviders() return the test case configs
        when(identityProviderService.getIdentityProviders()).thenReturn(providerConfigs);

        when(kbaConfiguration.getConfig()).thenReturn(kbaConfig);

        // Set up the selfService object
        SelfService selfService = new SelfService();
        selfService.bindIdentityProviderService(identityProviderService);
        selfService.bindKbaConfiguration(kbaConfiguration);

        // call the amendConfig which will modify the in memory version of selfServiceRegistration to
        // look like the amendedSelfServiceRegistration
        JsonValue amendedConfig = selfService.amendedConfig.apply(selfServiceRegistration);

        assertThat(amendedConfig.isEqualTo(amendedSelfServiceRegistration)).isTrue();
    }
}