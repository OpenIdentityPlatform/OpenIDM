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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.Map;

import static org.forgerock.json.JsonValue.json;
import static org.testng.Assert.*;

/**
 * Tests for the AllInOneRegistrationStage class
 */
public class AllInOneRegistrationStageTest {
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper()
                    .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonValue mergeLeft;
    private JsonValue mergeRight;
    private JsonValue mergeGoal;

    @BeforeSuite
    public void setup() throws Exception {
        mergeLeft = json(OBJECT_MAPPER.readValue(getClass().getResource("/mergeLeft.json"), Map.class));
        mergeRight = json(OBJECT_MAPPER.readValue(getClass().getResource("/mergeRight.json"), Map.class));
        mergeGoal = json(OBJECT_MAPPER.readValue(getClass().getResource("/mergeGoal.json"), Map.class));
    }

    @Test
    public void testMergeSucceeds() throws ResourceException {
        // We don't need any of the parameters for this test
        AllInOneRegistrationStage stage = new AllInOneRegistrationStage(null, null, null, null);
        JsonValue result = stage.merge(mergeLeft, mergeRight);
        assertThat(result.isEqualTo(mergeGoal)).isTrue();
    }
}