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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2026 3A Systems LLC
 */
package org.forgerock.openidm.selfservice.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.idp.impl.IdentityProviderService;
import org.forgerock.openidm.osgi.ComponentContextUtil;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
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

    private ProviderConfig googleIdentityProvider;
    private JsonValue selfServiceRegistration;
    private JsonValue amendedSelfServiceRegistration;


    @BeforeSuite
    public void setUp() throws Exception {
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

        // Add the google provider to the list of provider configs
        final List<ProviderConfig> providerConfigs = new ArrayList<>();
        providerConfigs.add(googleIdentityProvider);

        // Whenever we call getIdentityProviders() return the test case configs
        when(identityProviderService.getIdentityProviders()).thenReturn(providerConfigs);

        // Set up the selfService object
        SelfService selfService = new SelfService();
        selfService.bindIdentityProviderService(identityProviderService);

        // when the listener is being registered to nothing for testing purposes
        doNothing().when(identityProviderService).registerIdentityProviderListener(selfService);

        // call the amendConfig which will modify the in memory version of selfServiceRegistration to
        // look like the amendedSelfServiceRegistration
        selfService.amendConfig(selfServiceRegistration);

        assertThat(selfServiceRegistration.isEqualTo(amendedSelfServiceRegistration)).isTrue();
    }

    @Test
    public void identityProviderConfigChangedShouldIgnoreChangeWithoutConfiguration() throws Exception {
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
        final SelfService selfService = new SelfService();
        selfService.bindIdentityProviderService(identityProviderService);

        // a provider change that arrives before activate or after deactivate has nothing to rebuild
        selfService.identityProviderConfigChanged();

        // the first call past the guard is the debug log argument
        verify(identityProviderService, never()).getIdentityProviders();
    }

    @Test
    public void identityProviderServiceBindAndUnbindShouldRebuild() throws Exception {
        final SelfService selfService = spy(new SelfService());
        doNothing().when(selfService).identityProviderConfigChanged();
        final IdentityProviderService first = mock(IdentityProviderService.class);
        final IdentityProviderService second = mock(IdentityProviderService.class);

        selfService.bindIdentityProviderService(first);
        verify(selfService, times(1)).identityProviderConfigChanged();

        // DS replaces a dynamic 0..1 reference by binding the new service before unbinding the old one
        selfService.bindIdentityProviderService(second);
        selfService.unbindIdentityProviderService(first);
        verify(selfService, times(2)).identityProviderConfigChanged();

        selfService.unbindIdentityProviderService(second);
        verify(selfService, times(3)).identityProviderConfigChanged();
    }

    @Test
    public void unbindIdentityProviderServiceShouldUnregisterListener() throws Exception {
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
        final SelfService selfService = new SelfService();
        final Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(ComponentContextUtil.COMPONENT_NAME, SelfService.PID);
        final ComponentContext context = mock(ComponentContext.class);
        when(context.getProperties()).thenReturn(properties);
        final Field contextField = SelfService.class.getDeclaredField("context");
        contextField.setAccessible(true);
        contextField.set(selfService, context);

        selfService.bindIdentityProviderService(identityProviderService);
        selfService.unbindIdentityProviderService(identityProviderService);

        verify(identityProviderService).unregisterIdentityProviderListener(selfService);
    }

    @Test
    public void amendConfigShouldDropProvidersOfUnboundService() throws Exception {
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
        final List<ProviderConfig> providerConfigs = new ArrayList<>();
        providerConfigs.add(googleIdentityProvider);
        when(identityProviderService.getIdentityProviders()).thenReturn(providerConfigs);
        final JsonValue registration = selfServiceRegistration.copy();
        final SelfService selfService = new SelfService();

        selfService.bindIdentityProviderService(identityProviderService);
        selfService.amendConfig(registration);
        assertThat(registration.get("stageConfigs").get(0).get("providers").size()).isEqualTo(1);

        // the config is amended in place, so the providers of the unbound service must be replaced
        selfService.unbindIdentityProviderService(identityProviderService);
        selfService.amendConfig(registration);
        assertThat(registration.get("stageConfigs").get(0).get("providers").size()).isEqualTo(0);
    }
}
