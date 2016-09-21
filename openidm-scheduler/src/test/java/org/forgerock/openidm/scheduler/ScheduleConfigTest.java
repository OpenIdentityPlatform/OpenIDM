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
package org.forgerock.openidm.scheduler;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.io.IOException;

import org.forgerock.http.util.Json;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

public class ScheduleConfigTest {

    @Test
    public void testStartTimeEndTimeAndTimeZoneConvertedToStringWhenGetConfigCalled() throws IOException {
        // given
        final ScheduleConfig scheduleConfig = new ScheduleConfig(getResourceAsJson("/schedule-times-set.json"));

        // when
        final JsonValue config = scheduleConfig.getConfig();

        // then
        assertThat(config.get(SchedulerService.SCHEDULE_START_TIME).isString()).isTrue();
        assertThat(config.get(SchedulerService.SCHEDULE_START_TIME).asString()).isEqualTo("2041-12-31T23:59:59");
        assertThat(config.get(SchedulerService.SCHEDULE_END_TIME).isString()).isTrue();
        assertThat(config.get(SchedulerService.SCHEDULE_END_TIME).asString()).isEqualTo("2041-12-31T23:59:59");
        assertThat(config.get(SchedulerService.SCHEDULE_TIME_ZONE).isString()).isTrue();
        assertThat(config.get(SchedulerService.SCHEDULE_TIME_ZONE).asString()).isEqualTo("America/Los_Angeles");
    }

    private JsonValue getResourceAsJson(final String resource) throws IOException {
        return JsonValue.json(Json.readJsonLenient(getClass().getResourceAsStream(resource)));
    }
}
