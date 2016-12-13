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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;

import org.forgerock.json.JsonPatch;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.util.JsonUtil;
import org.testng.annotations.Test;

/**
 * Test JsonDiff method.
 *
 */
public class JsonDiffTest {
    private final String rootFile = JsonDiffTest.class.getResource("/").getFile();
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Test
    public void testJsonDiffMain() throws IOException {
        // parameters to be passed in main method
        final String[] pathArgs = new String[]{
                rootFile + "original.json",
                rootFile + "target.json"
        };

        final JsonValue expected = JsonUtil.parseStringified(JsonDiff.readFile(rootFile + "result.patch", Charset.defaultCharset()));

        System.setOut(new PrintStream(baos));
        JsonDiff.main(pathArgs);
        baos.flush();
        assertThat(JsonPatch.diff(JsonUtil.parseStringified(new String(baos.toByteArray())), expected).size()).isEqualTo(0);
    }

    @Test
    public void testJsonDiffMainInValidParameters() throws IOException {
        System.setErr(new PrintStream(baos));
        JsonDiff.main(new String[]{""});
        baos.flush();
        assertThat(new String(baos.toByteArray()).indexOf("Usage java -cp")).isGreaterThan(0);
    }
}