package org.forgerock.openidm.bin.defaults.script;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.testng.Assert.assertEquals;

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

        assertEquals(keyAliasValue, "\"&{openidm.https.keystore.cert.alias}\"");
    }

    // This method will be included in COMMONS as per COMMONS-72. We can point to it when it is released.
    // Just leave it for now.
    private JsonValue resourceAsJsonValue(final String resourcePath) throws IOException {
        try (final InputStream configStream = getClass().getResourceAsStream(resourcePath))  {
            return new JsonValue(new ObjectMapper().readValue(configStream, Map.class));
        }
    }
}