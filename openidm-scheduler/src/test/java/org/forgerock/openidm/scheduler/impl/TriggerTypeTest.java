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

package org.forgerock.openidm.scheduler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.scheduler.ScheduleConfig;
import org.quartz.CronTrigger;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class TriggerTypeTest {
    private static final String TRIGGER_NAME = "name";

    private JsonValue getConfig(final String configName) throws IOException {
        InputStream configStream = getClass().getResourceAsStream(configName);
        return new JsonValue(new ObjectMapper().readValue(configStream, LinkedHashMap.class));
    }

    private ScheduleConfig getScheduleConfig(final String configName) throws IOException {
        return new ScheduleConfig(getConfig(configName));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCronTriggerCreation() throws IOException, SchedulerException {
        //given
        final ScheduleConfig cronConfig = getScheduleConfig("/schedule-persisted.json");

        //when
        final Trigger trigger = cronConfig.getTriggerType().newTrigger(cronConfig, TRIGGER_NAME);

        //then
        assertThat(trigger).isInstanceOf(CronTrigger.class);
        //check draws attention to fact that although the misfire instruction is fireAndProceed in the
        //defined schedule, it gets set to CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW. This is
        //correct, as there are only two valid misfire instructions for a CronTrigger:
        //CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW and CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING. The
        //semantics of CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW correspond to the documented semantics of fireAndProceed.
        assertThat(trigger.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSimpleTriggerCreation() throws IOException, SchedulerException {
        //given
        final ScheduleConfig cronConfig = getScheduleConfig("/schedule-simple.json");

        //when
        final Trigger trigger = cronConfig.getTriggerType().newTrigger(cronConfig, TRIGGER_NAME);

        //then
        assertThat(trigger).isInstanceOf(SimpleTrigger.class);
        //the TriggerFactory impl will set the SimpleTrigger's misfire instruction to the SimpleTrigger misfire
        //instruction most appropriate for clustered recon. If SimpleTrigger functionality is supported publicly,
        //the ScheduleConfig (or successor) can be enhanced so that the appropriate set can be specified in the config.
        assertThat(trigger.getMisfireInstruction()).isEqualTo(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
    }
}
