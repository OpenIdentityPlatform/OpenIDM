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
package org.forgerock.openidm.datasource.jdbc.impl.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;
import static org.forgerock.json.JsonValue.*;

import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.openidm.datasource.jdbc.impl.JDBCDataSourceService;
import org.forgerock.openidm.util.JsonUtil;
import org.testng.annotations.Test;

/**
 * Tests the {@link ConfigMeta} class.
 */
public class ConfigMetaTest {

    @Test
    public void testGetPropertiesToEncrypt() throws Exception {
        ConfigMeta configMeta = new ConfigMeta();

        // First validate that a password field will NOT be encrypted if it DOES have a property in it.
        List<JsonPointer> propertiesToEncrypt = configMeta.getPropertiesToEncrypt(JDBCDataSourceService.PID, "",
                json(
                        object(
                                field("password", "&{pwd}")
                        )
                ));
        assertThat(propertiesToEncrypt).isEmpty();

        // Second validate that it WILL get encrypted if the password does NOT have a property in it.
        propertiesToEncrypt = configMeta.getPropertiesToEncrypt(JDBCDataSourceService.PID, "",
                json(
                        object(
                                field("password", "Passw0rd!")
                        )
                ));
        assertThat(propertiesToEncrypt).hasSize(1).contains(new JsonPointer("password"), atIndex(0));

        // Third validate that it WILL be treated as an encrypted field if the contents are already a crypto json blob.
        String encryptedString = JsonUtil.writeValueAsString(
                json(
                        object(
                                field("$crypto",
                                        object(
                                                field("type", "x-simple-encryption"),
                                                field("value",
                                                        object(
                                                                field("cipher", "AES/CBC/PKCS5Padding"),
                                                                field("salt", "YDF17jYjnDHBcLJuD0Ck+Q=="),
                                                                field("data", "AumpOjkv7DSxSGWgDXKcuQ=="),
                                                                field("iv", "OPR6fu310RL1GmOit/iplg=="),
                                                                field("key", "openidm-sym-default"),
                                                                field("mac", "mlO5CARKqy3j7TA5EibvoA==")
                                                        )
                                                )
                                        )
                                )
                        )
                ));
        propertiesToEncrypt = configMeta.getPropertiesToEncrypt(JDBCDataSourceService.PID, "",
                json(
                        object(
                                field("password", encryptedString)
                        )
                ));
        assertThat(propertiesToEncrypt).hasSize(1).contains(new JsonPointer("password"), atIndex(0));
    }

}
