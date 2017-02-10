/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openidm.selfservice.stage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

import java.util.Map;


/**
 * Tests for the IDMUserDetailsStage
 */
public class IDMUserDetailsStageTest {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper()
                    .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    public void testRegistrationFormObject() throws Exception {
        IDMUserDetailsConfig config = OBJECT_MAPPER.readValue(getClass().getResource("/registrationform.json"),
                IDMUserDetailsConfig.class);
        IDMUserDetailsStage stage = new IDMUserDetailsStage(null, null, null, null);
        assertThat(stage.gatherInitialRequirements(null, config)
                .isEqualTo(json(OBJECT_MAPPER.readValue(getClass().getResource("/registrationform-requirements.json"),
                        Map.class))
        )).isTrue();
    }
}