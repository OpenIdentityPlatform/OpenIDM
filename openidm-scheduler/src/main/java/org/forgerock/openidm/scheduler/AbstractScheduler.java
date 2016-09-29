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

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.openidm.quartz.impl.RepoJobStore.getTriggerId;
import static org.forgerock.openidm.scheduler.SchedulerService.CONFIG;
import static org.forgerock.openidm.scheduler.SchedulerService.GROUP_NAME;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.forgerock.http.util.Json;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.services.context.Context;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 * An abstract implementation of a {@link Scheduler}.
 */
abstract class AbstractScheduler implements Scheduler {
    static final String TRIGGERS = "triggers";
    static final String NEXT_RUN_DATE = "nextRunDate";
    private static final DateUtil dateUtil = DateUtil.getDateUtil(ServerConstants.TIME_ZONE_UTC);

    private final org.quartz.Scheduler scheduler;

    /**
     * Constructs an {@link AbstractScheduler} given a {@link org.quartz.Scheduler Quartz Scheduler}
     * @param scheduler a {@link org.quartz.Scheduler Quartz Scheduler}
     */
    AbstractScheduler(org.quartz.Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public String[] getJobNames() throws SchedulerException {
        String[] jobNames = scheduler.getJobNames(GROUP_NAME);
        return null == jobNames
                ? new String[0]
                : jobNames;
    }

    /**
     * {@inheritDoc}
     *
     * This method calls {@link org.quartz.Scheduler#pauseAll()}.
     * @throws SchedulerException when there is trouble calling {@link org.quartz.Scheduler#pauseAll()}
     */
    @Override
    public void pauseAll() throws SchedulerException {
        scheduler.pauseAll();
    }

    /**
     * {@inheritDoc}
     *
     * This method calls {@link org.quartz.Scheduler#resumeAll()}.
     * @throws SchedulerException when there is trouble calling {@link org.quartz.Scheduler#resumeAll()}
     */
    @Override
    public void resumeAll() throws SchedulerException {
        scheduler.resumeAll();
    }

    /**
     * {@inheritDoc}
     *
     * This method calls {@link org.quartz.Scheduler#scheduleJob(JobDetail, Trigger)}.
     * @param job the {@link JobDetail} of the job
     * @param trigger the {@link Trigger} of the job
     * @throws SchedulerException when there is trouble calling
     *              {@link org.quartz.Scheduler#scheduleJob(JobDetail, Trigger)}
     */
    @Override
    public void scheduleJob(JobDetail job, Trigger trigger) throws SchedulerException {
        scheduler.scheduleJob(job, trigger);
    }

    /**
     * {@inheritDoc}
     *
     * This method calls {@link org.quartz.Scheduler#addJob(JobDetail, boolean)}.
     * @param job the {@link JobDetail} of the job
     * @param replace whether or not to replace the job if it is already added
     * @throws SchedulerException when there is trouble calling {@link org.quartz.Scheduler#addJob(JobDetail, boolean)}
     */
    @Override
    public void addJob(JobDetail job, boolean replace) throws SchedulerException {
        scheduler.addJob(job, replace);
    }

    @Override
    public boolean jobExists(String jobName) throws SchedulerException {
        return getJobDetail(jobName) != null;
    }

    /**
     * {@inheritDoc}
     *
     * This method calls {@link org.quartz.Scheduler#getJobDetail(String, String)}.
     * @param jobName the job name
     * @return the {@link JobDetail} for the given job name; null if the job does not exist
     * @throws SchedulerException when there is trouble calling
     *      {@link org.quartz.Scheduler#getJobDetail(String, String)}
     */
    @Override
    public JobDetail getJobDetail(String jobName) throws SchedulerException {
        return scheduler.getJobDetail(jobName, GROUP_NAME);
    }

    /**
     * {@inheritDoc}
     *
     * This method calls {@link org.quartz.Scheduler#deleteJob(String, String)}.
     * @param jobName the job name
     * @throws SchedulerException when there is trouble calling
     *      {@link org.quartz.Scheduler#deleteJob(String, String)}
     */
    @Override
    public void deleteJobIfPresent(String jobName) throws SchedulerException {
        if (jobExists(jobName)) {
            scheduler.deleteJob(jobName, GROUP_NAME);
        }
    }

    @Override
    public JsonValue getSchedule(Context context, String scheduleName, String instanceId)
            throws SchedulerException, IOException {
        final JobDetail job = getJobDetail(scheduleName);
        final JsonValue resultMap =
                new ScheduleConfig(json(Json.readJson((String) job.getJobDataMap().get(CONFIG)))).getConfig();
        resultMap.put(TRIGGERS, getAllTriggersOfJob(context, job, instanceId).getObject());
        resultMap.put(NEXT_RUN_DATE, getNextFireTime(job));
        resultMap.put(ResourceResponse.FIELD_CONTENT_ID, scheduleName);
        return resultMap;
    }

    private JsonValue getAllTriggersOfJob(Context context, JobDetail jobDetail, String instanceId)
            throws SchedulerException {
        final JsonValue results = json(array());
        final Trigger[] triggers = scheduler.getTriggersOfJob(jobDetail.getName(), jobDetail.getGroup());
        for (final Trigger trigger : triggers) {
            final String triggerId = getTriggerId(GROUP_NAME, trigger.getKey().getName());
            JsonValue result = getTrigger(context, triggerId, trigger, instanceId);
            if (result.isNotNull()) {
                results.add(result.getObject());
            }
        }
        return results;
    }

    private String getNextFireTime(final JobDetail jobDetail) throws SchedulerException {
        final Trigger[] triggers = scheduler.getTriggersOfJob(jobDetail.getName(), jobDetail.getGroup());
        if (triggers.length <= 0 ) {
            return null;
        }
        Date fireTime = triggers[0].getFireTimeAfter(new Date());
        for (final Trigger trigger : triggers) {
            final Date newFireTime = trigger.getFireTimeAfter(new Date());
            if (newFireTime.before(fireTime)) {
                fireTime = newFireTime;
            }
        }
        return dateUtil.formatDateTime(fireTime);
    }

    abstract JsonValue getTrigger(Context context, String triggerId, Trigger trigger, String instanceId)
            throws SchedulerException;

    /**
     * Checks if a trigger is in the {@link Trigger#STATE_PAUSED paused state}.
     * @param trigger the {@link Trigger} to check
     * @return true if the trigger is paused; false otherwise
     * @throws SchedulerException if unable to get the state of the {@link Trigger}
     */
    boolean isPaused(Trigger trigger) throws SchedulerException {
        return Trigger.STATE_PAUSED == scheduler.getTriggerState(trigger.getName(), trigger.getGroup());
    }

    /**
     * Checks if a trigger is in the {@link Trigger#STATE_BLOCKED acquired state}.
     * @param trigger the {@link Trigger} to check
     * @return true if the trigger is acquired; false otherwise
     * @throws SchedulerException if unable to get the state of the {@link Trigger}
     */
    boolean isAcquired(Trigger trigger) throws SchedulerException {
        return Trigger.STATE_BLOCKED == scheduler.getTriggerState(trigger.getName(), trigger.getGroup());
    }

    @Override
    public JsonValue getCurrentlyExecutingJobs() throws SchedulerException, IOException {
        final JsonValue currentlyExecutingJobs = json(array());
        List<?> jobs = scheduler.getCurrentlyExecutingJobs();
        for (final Object job : jobs) {
            final JobDetail jobDetail = ((JobExecutionContext) job).getJobDetail();
            final JsonValue config =
                    json(Json.readJson((String) jobDetail.getJobDataMap().get(CONFIG)));
            config.put(ResourceResponse.FIELD_CONTENT_ID, jobDetail.getName());
            currentlyExecutingJobs.add(new ScheduleConfig(config).getConfig().getObject());
        }
        return currentlyExecutingJobs;
    }

    /**
     * {@inheritDoc}
     *
     * This method calls {@link org.quartz.Scheduler#start()}.
     * @throws SchedulerException when there is trouble calling {@link org.quartz.Scheduler#start()}
     */
    @Override
    public void start() throws SchedulerException {
        scheduler.start();
    }

    /**
     * {@inheritDoc}
     *
     * This method calls {@link org.quartz.Scheduler#isStarted()}.
     * @return true if the scheduler is started; false otherwise.
     * @throws SchedulerException when there is trouble calling {@link org.quartz.Scheduler#isStarted()}
     */
    @Override
    public boolean isStarted() throws SchedulerException {
        return scheduler.isStarted();
    }

    /**
     * {@inheritDoc}
     *
     * This method calls {@link org.quartz.Scheduler#shutdown()}.
     * @throws SchedulerException when there is trouble calling {@link org.quartz.Scheduler#shutdown()}
     */
    @Override
    public void shutdown() throws SchedulerException {
        scheduler.shutdown();
    }
}
