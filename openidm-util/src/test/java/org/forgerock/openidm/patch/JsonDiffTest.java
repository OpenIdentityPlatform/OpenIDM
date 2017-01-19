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

package org.forgerock.openidm.patch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.util.Utils.closeSilently;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonPatch;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.util.JsonUtil;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test JsonDiff method.
 *
 */
public class JsonDiffTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Test
    public void testJsonDiffMain() throws IOException {
        // parameters to be passed in main method
        final String[] pathArgs = new String[]{
                    "/original.json",
                    "/target.json"
        };

        final JsonValue expected = getResource("/result.patch", List.class);

        final String result = JsonDiff.getDiff(
                getResource(pathArgs[0], Map.class),
                getResource(pathArgs[1], Map.class)
        );
        assertThat(JsonPatch.diff(JsonUtil.parseStringified(result), expected).size()).isEqualTo(0);
    }

    @Test
    public void testJsonDiffMainInValidParameters() throws IOException {
        System.setErr(new PrintStream(baos));
        JsonDiff.main(new String[]{""});
        baos.flush();
        assertThat(new String(baos.toByteArray()).contains("Usage java -cp")).isTrue();
    }

    private JsonValue getResource(final String resourceFile, final Class<?> valueType) throws IOException {
        final InputStream resource = getClass().getResourceAsStream(resourceFile);
        try {
            return json(mapper.readValue(resource, valueType));
        } finally {
            if (resource != null) {
                closeSilently(resource);
            }
        }
    }
}