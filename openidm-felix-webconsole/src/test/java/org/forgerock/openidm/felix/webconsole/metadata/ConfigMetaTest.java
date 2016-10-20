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
package org.forgerock.openidm.felix.webconsole.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.openidm.felix.webconsole.WebConsoleSecurityProviderService;
import org.forgerock.openidm.metadata.NotConfiguration;
import org.forgerock.openidm.metadata.WaitForMetaData;
import org.testng.annotations.Test;

public class ConfigMetaTest {

    @Test
    public void testGetPropertiesToEncryptWithValidPid() throws NotConfiguration, WaitForMetaData {
        // given
        final ConfigMeta configMeta = new ConfigMeta();

        // when
        final List<JsonPointer> propertiesToEncrypt =
                configMeta.getPropertiesToEncrypt(WebConsoleSecurityProviderService.PID, "", json(object()));

        // then
        assertThat(propertiesToEncrypt).hasSize(1).contains(new JsonPointer("password"));
    }

    @Test
    public void testGetPropertiesToEncryptWithInvalidPid() throws NotConfiguration, WaitForMetaData {
        // given
        final ConfigMeta configMeta = new ConfigMeta();

        // when
        final List<JsonPointer> propertiesToEncrypt =
                configMeta.getPropertiesToEncrypt("some.pid", "", json(object()));

        // then
        assertThat(propertiesToEncrypt).isNull();
    }
}
