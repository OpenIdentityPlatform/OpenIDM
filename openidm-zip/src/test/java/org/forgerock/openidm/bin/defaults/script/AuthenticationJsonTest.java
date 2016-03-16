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
package org.forgerock.openidm.bin.defaults.script;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;


/**
 * A unit test to test the raw key-value in Authentication.json file.
 *
 * Specifically, this example tests the value for keyAlias should be equal to "&{openidm.https.keystore.cert.alias}".
 */
public class AuthenticationJsonTest {
    @Test
    public void testGetConfiguration() throws Exception {
        JsonValue authValue = resourceAsJsonValue("/conf/authentication.json");
        String keyAliasValue = authValue
                .get("serverAuthContext")
                .get("sessionModule")
                .get("properties")
                .get("keyAlias")
                .toString();

        assertThat(keyAliasValue).isEqualTo("\"&{openidm.https.keystore.cert.alias}\"");
    }

    // This method will be included in COMMONS as per COMMONS-72. We can point to it when it is released.
    // Just leave it for now.
    private JsonValue resourceAsJsonValue(final String resourcePath) throws IOException {
        try (final InputStream configStream = getClass().getResourceAsStream(resourcePath))  {
            return new JsonValue(new ObjectMapper().readValue(configStream, Map.class));
        }
    }
}