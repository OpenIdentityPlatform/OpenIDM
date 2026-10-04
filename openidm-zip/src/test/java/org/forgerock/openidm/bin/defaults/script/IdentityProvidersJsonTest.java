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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openidm.bin.defaults.script;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks the catalog of supported identity providers shipped in conf/identityProviders.json.
 *
 * IdentityProviderService does not start without this file, and the Admin UI offers its entries as
 * templates, keyed on the provider name, and copies their icon into the provider's own configuration.
 */
public class IdentityProvidersJsonTest {
    @Test
    public void testCatalogEntries() throws Exception {
        // unlike ProviderConfigMapper, fail on unknown keys: the service would silently drop them
        final ObjectMapper mapper = new ObjectMapper();
        final JsonValue catalog;
        try (final InputStream configStream = getClass().getResourceAsStream("/conf/identityProviders.json")) {
            assertThat(configStream).isNotNull();
            catalog = new JsonValue(mapper.readValue(configStream, Map.class));
        }
        final List<String> names = new ArrayList<>();
        for (JsonValue entry : catalog.get("providers")) {
            final ProviderConfig config = mapper.convertValue(entry.getObject(), ProviderConfig.class);
            names.add(config.getName());
            assertThat(config.getIcon()).isNotEmpty();
            assertThat(config.getScope()).isNotEmpty();
            assertThat(config.getAuthorizationEndpoint()).startsWith("https://");
            assertThat(config.getTokenEndpoint()).startsWith("https://");
            assertThat(config.getUserInfoEndpoint()).startsWith("https://");
        }
        assertThat(names).containsExactly("google", "facebook", "linkedIn");
    }
}
