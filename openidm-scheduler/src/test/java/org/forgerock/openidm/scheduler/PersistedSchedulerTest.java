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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.resource.Resources.newInternalConnectionFactory;
import static org.forgerock.openidm.scheduler.SchedulerService.CONFIG;
import static org.forgerock.openidm.scheduler.SchedulerService.GROUP_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Router;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.openidm.quartz.impl.TriggerWrapper;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.services.context.Context;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobPersistenceException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PersistedSchedulerTest {

    public static final String GROUP = "group";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String JOB_NAME = "jobName";
    private static final String TRIGGER_NAME = "triggerName";
    private static final String CRON_EXPRESSION = "30 0/1 * * * ?";
    private static final String SCHEDULE_PERSISTED_JSON = "/schedule-persisted.json";
    private static final String INSTANCE_ID = "instanceId";
    public static final String NAME = "name";
    public static final String SCHEDULER_TRIGGER_RESOURCE_PATH = "/scheduler/trigger";

    @Test
    public void testGetTrigger() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final ConnectionFactory connectionFactory = createConnectionFactory();
        final PersistedScheduler scheduler = new PersistedScheduler(quartzScheduler, connectionFactory);
        createTrigger(connectionFactory);
        // when
        final JsonValue trigger = scheduler.getTrigger(
                mock(Context.class),
                TRIGGER_NAME,
                null,
                INSTANCE_ID);

        // then
        AssertJJsonValueAssert.assertThat(trigger).isNotNull();
        assertThat(trigger.get(NAME).asString()).isEqualTo(TRIGGER_NAME);
        assertThat(trigger.get(GROUP).asString()).isEqualTo(GROUP_NAME);
    }

    @Test()
    public void testGetTriggerWhenNotFound() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final ConnectionFactory connectionFactory = createConnectionFactory();
        final PersistedScheduler scheduler = new PersistedScheduler(quartzScheduler, connectionFactory);

        // when
        final JsonValue trigger = scheduler.getTrigger(
                mock(Context.class),
                TRIGGER_NAME,
                null,
                INSTANCE_ID);

        // then
        AssertJJsonValueAssert.assertThat(trigger).isNotNull();
        assertThat(trigger.asMap()).isEmpty();
    }

    private org.quartz.Scheduler createQuartzScheduler() throws SchedulerException, IOException, ParseException {
        final org.quartz.Scheduler scheduler = mock(org.quartz.Scheduler.class);
        when(scheduler.getJobNames(GROUP_NAME)).thenReturn(new String[]{JOB_NAME});
        doNothing().when(scheduler).pauseAll();
        doNothing().when(scheduler).resumeAll();
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenReturn(new Date());
        doNothing().when(scheduler).addJob(any(JobDetail.class), anyBoolean());
        when(scheduler.getJobDetail(JOB_NAME, GROUP_NAME)).thenReturn(createJobDetail());
        when(scheduler.getTriggersOfJob(JOB_NAME, GROUP_NAME)).thenReturn(
                new Trigger[]{
                        new CronTrigger(TRIGGER_NAME, GROUP_NAME, CRON_EXPRESSION)});
        when(scheduler.getCurrentlyExecutingJobs()).thenReturn(Collections.emptyList());
        doNothing().when(scheduler).start();
        when(scheduler.isStarted()).thenReturn(true);
        doNothing().when(scheduler).shutdown();
        return scheduler;
    }

    private JobDetail createJobDetail() throws IOException {
        final JsonValue config = getConfig(SCHEDULE_PERSISTED_JSON);

        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(CONFIG, mapper.writeValueAsString(config.asMap()));

        final JobDetail jobDetail = new JobDetail();
        jobDetail.setName(JOB_NAME);
        jobDetail.setGroup(GROUP_NAME);
        jobDetail.setJobDataMap(jobDataMap);
        return jobDetail;
    }

    private JsonValue getConfig(final String configName) throws IOException {
        InputStream configStream = getClass().getResourceAsStream(configName);
        return new JsonValue(new ObjectMapper().readValue(configStream, LinkedHashMap.class));
    }

    private ConnectionFactory createConnectionFactory() {
        final Router router = new Router();
        router.addRoute(Router.uriTemplate(SCHEDULER_TRIGGER_RESOURCE_PATH), new MemoryBackend());
        return newInternalConnectionFactory(router);
    }

    private void createTrigger(final ConnectionFactory connectionFactory)
            throws ResourceException, ParseException, JobPersistenceException {
        connectionFactory.getConnection().create(
                ContextUtil.createInternalContext(),
                Requests.newCreateRequest(SCHEDULER_TRIGGER_RESOURCE_PATH, TRIGGER_NAME,
                        new TriggerWrapper(new CronTrigger(TRIGGER_NAME, GROUP_NAME, CRON_EXPRESSION), true)
                                .getValue()));
    }
}
