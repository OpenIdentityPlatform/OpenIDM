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
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openidm.scheduler.AbstractScheduler.NEXT_RUN_DATE;
import static org.forgerock.openidm.scheduler.AbstractScheduler.TRIGGERS;
import static org.forgerock.openidm.scheduler.SchedulerService.CONFIG;
import static org.forgerock.openidm.scheduler.SchedulerService.GROUP_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;

import org.forgerock.json.JsonValue;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.services.context.Context;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AbstractSchedulerTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String JOB_NAME = "jobName";
    private static final String TRIGGER = "trigger";
    private static final String TRIGGER_NAME = "triggerName";
    private static final String INSTANCE_ID = "instanceId";
    private static final String CRON_EXPRESSION = "30 0/1 * * * ?";
    private static final String SCHEDULE_PERSISTED_JSON = "/schedule-persisted.json";

    @Test
    public void testGetJobNames() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final Scheduler scheduler = new TestScheduler(quartzScheduler);

        // when
        final String[] jobNames = scheduler.getJobNames();

        // then
        assertThat(jobNames).isNotNull().hasSize(1).containsOnly(JOB_NAME);
        verify(quartzScheduler, times(1)).getJobNames(GROUP_NAME);
    }

    @Test
    public void testPauseAll() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final Scheduler scheduler = new TestScheduler(quartzScheduler);

        // when
        scheduler.pauseAll();

        // then
        verify(quartzScheduler, times(1)).pauseAll();
    }

    @Test
    public void testResumeAll() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final Scheduler scheduler = new TestScheduler(quartzScheduler);

        // when
        scheduler.resumeAll();

        // then
        verify(quartzScheduler, times(1)).resumeAll();
    }

    @Test
    public void testScheduleJob() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final Scheduler scheduler = new TestScheduler(quartzScheduler);
        final JobDetail jobDetail = mock(JobDetail.class);
        final Trigger trigger = mock(Trigger.class);

        // when
        scheduler.scheduleJob(jobDetail, trigger);

        // then
        verify(quartzScheduler, times(1)).scheduleJob(jobDetail, trigger);
    }

    @Test
    public void testAddJob() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final Scheduler scheduler = new TestScheduler(quartzScheduler);
        final JobDetail jobDetail = mock(JobDetail.class);

        // when
        scheduler.addJob(jobDetail, true);

        // then
        verify(quartzScheduler, times(1)).addJob(jobDetail, true);
    }

    @Test
    public void testJobExists() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final Scheduler scheduler = new TestScheduler(quartzScheduler);

        // when
        final boolean exists = scheduler.jobExists(JOB_NAME);

        // then
        assertThat(exists).isTrue();
        verify(quartzScheduler, times(1)).getJobDetail(JOB_NAME, GROUP_NAME);
    }

    @Test
    public void testGetJobDetail() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final Scheduler scheduler = new TestScheduler(quartzScheduler);

        // when
        final JobDetail jobDetail = scheduler.getJobDetail(JOB_NAME);

        // then
        assertThat(jobDetail).isNotNull();
        verify(quartzScheduler, times(1)).getJobDetail(JOB_NAME, GROUP_NAME);
    }

    @Test
    public void testDeleteJobIfPresent() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final Scheduler scheduler = new TestScheduler(quartzScheduler);

        // when
        scheduler.deleteJobIfPresent(JOB_NAME);

        // then
        verify(quartzScheduler, times(1)).deleteJob(JOB_NAME, GROUP_NAME);
    }

    @Test
    public void testGetSchedule() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final Scheduler scheduler = new TestScheduler(quartzScheduler);

        // when
        final JsonValue schedule = scheduler.getSchedule(mock(Context.class), JOB_NAME, INSTANCE_ID);

        // then
        verify(quartzScheduler, times(1)).getJobDetail(JOB_NAME, GROUP_NAME);
        AssertJJsonValueAssert.assertThat(schedule.get(TRIGGERS)).isNotNull();
        AssertJJsonValueAssert.assertThat(schedule.get(NEXT_RUN_DATE)).isNotNull();
        assertThat(schedule.get(FIELD_CONTENT_ID).asString()).isNotNull().isEqualTo(JOB_NAME);
    }

    @Test
    public void testGetCurrentlyExecutingJobs() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final Scheduler scheduler = new TestScheduler(quartzScheduler);

        // when
        final JsonValue jobs = scheduler.getCurrentlyExecutingJobs();

        // then
        verify(quartzScheduler, times(1)).getCurrentlyExecutingJobs();
        AssertJJsonValueAssert.assertThat(jobs).isNotNull();
    }

    @Test
    public void testStart() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final Scheduler scheduler = new TestScheduler(quartzScheduler);

        // when
        scheduler.start();

        // then
        verify(quartzScheduler, times(1)).start();
    }

    @Test
    public void testIsStarted() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final Scheduler scheduler = new TestScheduler(quartzScheduler);

        // when
        final boolean isStarted = scheduler.isStarted();

        // then
        assertThat(isStarted).isTrue();
        verify(quartzScheduler, times(1)).isStarted();
    }

    @Test
    public void testShutdown() throws SchedulerException, IOException, ParseException {
        // given
        final org.quartz.Scheduler quartzScheduler = createQuartzScheduler();
        final Scheduler scheduler = new TestScheduler(quartzScheduler);

        // when
        scheduler.shutdown();

        // then
        verify(quartzScheduler, times(1)).shutdown();
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

    private static class TestScheduler extends AbstractScheduler {

        TestScheduler(org.quartz.Scheduler scheduler) {
            super(scheduler);
        }

        @Override
        JsonValue getTrigger(Context context, String triggerId, Trigger trigger, String instanceId) throws SchedulerException {
            return JsonValue.json(object(field(TRIGGER, TRIGGER_NAME)));
        }
    }
}
