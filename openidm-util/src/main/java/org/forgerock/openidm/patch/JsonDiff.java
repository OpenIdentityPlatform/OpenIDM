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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonPatch;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.util.JsonUtil;

/**
 * Outputs the JsonPath.diff between the two files.
 * @see org.forgerock.json.patch.JsonPatch#diff(JsonValue, JsonValue)
 */
public class JsonDiff {
    final static String ERROR_MESSAGE =
              "Usage java -cp slf4j-jdk14.jar\n" +
                      "     :slf4j-api.jar\n" +
                      "     :jackson-annotations.jar\n" +
                      "     :jackson-databind.jar\n" +
                      "     :jackson-core.jar\n" +
                      "     :forgerock-util.jar\n" +
                      "     :openidm-util.jar org.forgerock.openidm.patch.JsonDiff\n" +
                      " [base_json_file] [updated_json_file]\n"
            + "Direct the output to the desired patch file\n";

    /**
     *
          Usage java -cp slf4j-jdk14.jar
                     :slf4j-api.jar
                     :jackson-annotations.jar
                     :jackson-databind.jar
                     :jackson-core.jar
                     :forgerock-util.jar
                     :openidm-util.jar org.forgerock.openidm.patch.JsonDiff
                     [base_json_file] [updated_json_file]
          Direct the output to the desired patch file
     *
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println(ERROR_MESSAGE);
            return;
        }

        try {
            final JsonValue original = JsonUtil.parseStringified(readFile(args[0], Charset.defaultCharset()));
            final JsonValue target = JsonUtil.parseStringified(readFile(args[1], Charset.defaultCharset()));
            System.out.println(getDiff(original, target));
        } catch (JsonException | NoSuchFileException e) {
            System.err.println("Path does not exist or URL passed into parsing is not valid JSON.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String readFile(String path, Charset encoding)
            throws IOException {
        final byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    static String getDiff(JsonValue original, JsonValue target) throws IOException {
        return JsonUtil.writePrettyValueAsString(JsonPatch.diff(original, target));
    }
}
