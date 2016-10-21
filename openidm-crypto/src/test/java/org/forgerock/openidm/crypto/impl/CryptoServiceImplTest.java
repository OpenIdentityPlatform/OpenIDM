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

package org.forgerock.openidm.crypto.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openidm.util.JsonUtil.writeValueAsString;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.forgerock.json.JsonValue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CryptoServiceImplTest {

    private final static JsonValue JSON_OBJECT = json(object(field("password", "myPassw0rd")));
    private final static JsonValue JSON_ARRAY = json(array("v1", "v2"));

    @DataProvider
    public Object[][] normalizeValueBeforeHashData() throws JsonProcessingException {
        return new Object[][]{
                { json("myString"), "myString" },
                { json(1), "1" },
                { json(true), "true" },
                { json(null), "null" },
                { JSON_OBJECT, writeValueAsString(JSON_OBJECT) },
                { JSON_ARRAY, writeValueAsString(JSON_ARRAY) },
        };
    }

    @Test(dataProvider = "normalizeValueBeforeHashData")
    public void normalizeValueBeforeHashTest(final JsonValue input, final String expectedOutput) {
        // given
        final CryptoServiceImpl cryptoService = new CryptoServiceImpl();

        // when
        final String actualOutput = cryptoService.normalizeValueBeforeHash(input);

        // then
        assertThat(actualOutput).isEqualTo(expectedOutput);
    }

}
