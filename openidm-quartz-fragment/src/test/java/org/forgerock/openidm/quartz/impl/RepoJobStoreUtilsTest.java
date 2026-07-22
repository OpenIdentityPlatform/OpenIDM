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
package org.forgerock.openidm.quartz.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.InvalidClassException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobPersistenceException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.calendar.AnnualCalendar;
import org.testng.annotations.Test;

/**
 * Tests the serialize/deserialize round trip in {@link RepoJobStoreUtils},
 * ensuring the deserialization filter (GHSA-xjw4-4w2v-rp6g, CWE-502) accepts
 * everything the scheduler legitimately persists — including the OpenIDM job
 * implementation class carried by {@link JobDetail} — while rejecting foreign
 * classes and oversized payloads.
 */
public class RepoJobStoreUtilsTest {

    @Test
    public void shouldRoundTripJobDetailWithSchedulerServiceJobClass() throws Exception {
        JobDetail job = new JobDetail("job", "group", SchedulerServiceJob.class);
        JobDataMap data = job.getJobDataMap();
        data.put("invokeService", "org.forgerock.openidm.script");
        data.put("count", 42L);
        data.put("enabled", Boolean.TRUE);
        data.put("createdAt", new Date(1234567890000L));

        JobDetail restored = (JobDetail) RepoJobStoreUtils.deserialize(RepoJobStoreUtils.serialize(job));

        assertThat(restored.getName()).isEqualTo("job");
        assertThat(restored.getGroup()).isEqualTo("group");
        assertThat(restored.getJobClass()).isEqualTo(SchedulerServiceJob.class);
        assertThat(restored.getJobDataMap().getString("invokeService")).isEqualTo("org.forgerock.openidm.script");
        assertThat(restored.getJobDataMap().getLong("count")).isEqualTo(42L);
        assertThat(restored.getJobDataMap().getBoolean("enabled")).isTrue();
        assertThat(restored.getJobDataMap().get("createdAt")).isEqualTo(new Date(1234567890000L));
    }

    @Test
    public void shouldRoundTripJobDetailWithStatefulSchedulerServiceJobClass() throws Exception {
        JobDetail job = new JobDetail("job", "group", StatefulSchedulerServiceJob.class);

        JobDetail restored = (JobDetail) RepoJobStoreUtils.deserialize(RepoJobStoreUtils.serialize(job));

        assertThat(restored.getJobClass()).isEqualTo(StatefulSchedulerServiceJob.class);
    }

    @Test
    public void shouldRoundTripSimpleTrigger() throws Exception {
        Date startTime = new Date(1234567890000L);
        Date endTime = new Date(1234567990000L);
        SimpleTrigger trigger = new SimpleTrigger("trigger", "group", startTime, endTime, 5, 1000L);

        SimpleTrigger restored = (SimpleTrigger) RepoJobStoreUtils.deserialize(RepoJobStoreUtils.serialize(trigger));

        assertThat(restored.getName()).isEqualTo("trigger");
        assertThat(restored.getGroup()).isEqualTo("group");
        assertThat(restored.getStartTime()).isEqualTo(startTime);
        assertThat(restored.getEndTime()).isEqualTo(endTime);
        assertThat(restored.getRepeatCount()).isEqualTo(5);
        assertThat(restored.getRepeatInterval()).isEqualTo(1000L);
    }

    @Test
    public void shouldRoundTripAnnualCalendarWithTimeZone() throws Exception {
        AnnualCalendar calendar = new AnnualCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        Calendar excluded = new GregorianCalendar(2026, Calendar.JANUARY, 1);
        calendar.setDayExcluded(excluded, true);

        AnnualCalendar restored = (AnnualCalendar) RepoJobStoreUtils.deserialize(RepoJobStoreUtils.serialize(calendar));

        assertThat(restored.getTimeZone().getID()).isEqualTo("America/New_York");
        assertThat(restored.isDayExcluded(excluded)).isTrue();
    }

    @Test
    public void shouldRejectForeignSerializable() throws Exception {
        String serialized = RepoJobStoreUtils.serialize(new File("foreign"));

        assertThatThrownBy(() -> RepoJobStoreUtils.deserialize(serialized))
                .isInstanceOf(JobPersistenceException.class)
                .hasCauseInstanceOf(InvalidClassException.class);
    }

    @Test
    public void shouldRejectSerializedLambda() throws Exception {
        Runnable lambda = (Runnable & Serializable) () -> { };
        String serialized = RepoJobStoreUtils.serialize((Serializable) lambda);

        assertThatThrownBy(() -> RepoJobStoreUtils.deserialize(serialized))
                .isInstanceOf(JobPersistenceException.class)
                .hasCauseInstanceOf(InvalidClassException.class);
    }

    @Test
    public void shouldRejectQuartzNativeJob() throws Exception {
        JobDetail job = new JobDetail("job", "group", org.quartz.jobs.NativeJob.class);
        String serialized = RepoJobStoreUtils.serialize(job);

        assertThatThrownBy(() -> RepoJobStoreUtils.deserialize(serialized))
                .isInstanceOf(JobPersistenceException.class)
                .hasCauseInstanceOf(InvalidClassException.class);
    }

    @Test
    public void shouldRejectOversizedArray() throws Exception {
        String serialized = RepoJobStoreUtils.serialize(new byte[2_000_000]);

        assertThatThrownBy(() -> RepoJobStoreUtils.deserialize(serialized))
                .isInstanceOf(JobPersistenceException.class)
                .hasCauseInstanceOf(InvalidClassException.class);
    }

}
