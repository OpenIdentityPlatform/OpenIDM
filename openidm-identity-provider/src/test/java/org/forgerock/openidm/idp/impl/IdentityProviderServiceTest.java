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
package org.forgerock.openidm.idp.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test the IdentityProviderService.
 */
public class IdentityProviderServiceTest {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().configure(
            JsonParser.Feature.ALLOW_COMMENTS, true).disable(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private ProviderConfig googleIdentityProvider;

    @BeforeSuite
    public void setUp() throws Exception {
        // identityProvider-google.json is a sample identityProvider configuration
        googleIdentityProvider = ProviderConfigMapper.toProviderConfig(
                json(OBJECT_MAPPER.readValue(getClass().getResource("/identityProvider-google.json"), Map.class)));
    }

    @Test
    public void testReadInstance() throws Exception {
        IdentityProviderConfig idpConfig = mock(IdentityProviderConfig.class);
        when(idpConfig.getIdentityProviderConfig()).thenReturn(googleIdentityProvider);

        JsonValue expected = json(object(
                field("name", "google"),
                field("type", "openid_connect"),
                field("icon", "google"),
                field("authorization_endpoint", "authorization_endpoint"),
                field("token_endpoint", "token_endpoint"),
                field("userinfo_endpoint", "userinfo_endpoint"),
                field("well-known", ""),
                field("client_id", ""),
                field("enabled", true)));

        IdentityProviderService service = new IdentityProviderService();
        service.bindIdentityProviderConfig(idpConfig);

        final Promise<ResourceResponse, ResourceException> promise =
                service.readInstance(new RootContext(), newReadRequest(""));
        assertThat(promise).succeeded();
        final JsonValue response = promise.get().getContent();

        assertThat(response).hasArray("providers").hasSize(1);
        JsonValue google = response.get("providers").get(0);
        assertThat(google).doesNotContain("client_secret"); // it should be removed by readInstance
        assertThat(google.isEqualTo(expected)).isTrue();
    }
}