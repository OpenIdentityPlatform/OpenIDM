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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openidm.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.testng.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.JsonValueException;
import org.testng.annotations.Test;

/**
 * Tests for the numeric settings read by {@link ClusterConfig} and {@link InstanceState}.
 */
public class ClusterConfigTest {

    @Test
    public void numericSettingsAreAcceptedAsStringsOrNumbers() {
        ClusterConfig config = new ClusterConfig(json(object(
                field("instanceTimeout", "30000"),
                field("instanceRecoveryTimeout", 45000))));

        assertThat(config.getInstanceTimeout()).isEqualTo(30000L);
        assertThat(config.getInstanceRecoveryTimeout()).isEqualTo(45000L);
    }

    @Test
    public void nonNumericSettingIsReportedAsConfigurationError() {
        try {
            new ClusterConfig(json(object(field("instanceTimeout", "thirty seconds"))));
            fail("expected JsonValueException");
        } catch (JsonValueException e) {
            assertThat(e.getMessage()).contains("instanceTimeout");
        }
    }

    @Test
    public void corruptInstanceTimestampNamesTheField() {
        Map<String, Object> map = new HashMap<>();
        map.put(InstanceState.PROP_TIMESTAMP_LEASE, "yesterday");

        try {
            new InstanceState("node1", map);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains(InstanceState.PROP_TIMESTAMP_LEASE).contains("yesterday");
        }
    }
}
