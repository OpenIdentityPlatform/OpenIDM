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
 */
package org.forgerock.openidm.auth.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.auth.AuthenticationService;
import org.forgerock.openidm.metadata.WaitForMetaData;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test building list of JsonPointers to encrypted properties for authentication.json.
 */
public class ConfigMetaTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private final ConfigMeta configMeta = new ConfigMeta();

    @DataProvider(name = "authModuleConfigs")
    private Object[][] authModuleConfigs() {
        return new Object[][] {
                {"/oneAuthModuleWithOneResolver.json",   new String[] { "secret1" } },
                {"/oneAuthModuleWithTwoResolvers.json",  new String[] { "secret1", "secret2" } },
                {"/twoAuthModulesWithOneResolver.json",  new String[] { "secret1", "secret2" } },
                {"/twoAuthModulesWithTwoResolvers.json", new String[] { "secret1", "secret2", "secret3", "secret4" } }
        };
    }

    @Test(dataProvider = "authModuleConfigs")
    public void toResolversConvertsAuthModuleConfigToResolversArray(String resource, String[] clientSecrets)
            throws IOException, WaitForMetaData {
        // read the config
        final JsonValue authModuleConfig = json(mapper.readValue(getClass().getResource(resource), Map.class));

        // get the list of properties to encrypt from the config
        List<JsonPointer> clientSecretProperties = configMeta.getPropertiesToEncrypt(
                AuthenticationService.PID, "", authModuleConfig);

        // get the list of property values to encrypt using the List<JsonPointer> provided by getPropertiesToEncrypt
        List<String> clientSecretValues = FluentIterable.from(clientSecretProperties)
                .transform(new Function<JsonPointer, String>() {
                    @Override
                    public String apply(JsonPointer pointer) {
                        return authModuleConfig.get(pointer).asString();
                    }
                })
                .toList();

        // assert that these are the proper values for encryption
        assertThat(clientSecretValues).containsExactlyInAnyOrder(clientSecrets);
    }

    @Test
    public void testGetPropertiesToEncryptWithUnsupportedPidOrFactory(String resource, String[] clientSecrets)
            throws IOException, WaitForMetaData {

        // when
        List<JsonPointer> clientSecretProperties = configMeta.getPropertiesToEncrypt(
                "Unsupported PID", "", null);

        // assert that null was returned because the pidOrFactory did not match AuthenticationService.PID
        assertThat(clientSecretProperties).isNull();
    }
}
