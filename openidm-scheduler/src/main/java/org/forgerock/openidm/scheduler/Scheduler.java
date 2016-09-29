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

import java.io.IOException;

import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 * An interface that wraps a {@link org.quartz.Scheduler Quartz Scheduler}.
 */
public interface Scheduler {

    /**
     * Gets the job names of the scheduler, returning an empty array if no jobs are found.
     *
     * @return array of job names, or empty array.
     * @throws SchedulerException when there is trouble calling {@link org.quartz.Scheduler#getJobNames(String)}
     */
    String[] getJobNames() throws SchedulerException;

    /**
     * Pauses all scheduler jobs.
     * @throws SchedulerException if unable to pause all the scheduler jobs.
     */
    void pauseAll() throws SchedulerException;

    /**
     * Resumes all scheduler jobs.
     * @throws SchedulerException if unable to resume all the scheduler jobs.
     */
    void resumeAll() throws SchedulerException;

    /**
     * Schedules a job given a {@link JobDetail} and a {@link Trigger}.
     * @param job the {@link JobDetail}
     * @param trigger the {@link Trigger}
     * @throws SchedulerException if unable to schedule the given job
     */
    void scheduleJob(JobDetail job, Trigger trigger) throws SchedulerException;

    /**
     * Adds a job to the scheduler given a {@link JobDetail} and whether or not the job should be replaced if it
     * is already scheduled.
     * @param job the {@link JobDetail}
     * @param replace true if the job should be replaced; false otherwise
     * @throws SchedulerException if unable to add the job to the scheduler
     */
    void addJob(JobDetail job, boolean replace) throws SchedulerException;

    /**
     * Checks if a job exists in the scheduler.
     * @param jobName the job name to check
     * @return true if the job exists; false otherwise
     * @throws SchedulerException if unable to determine if the job exists
     */
    boolean jobExists(String jobName) throws SchedulerException;

    /**
     * Gets the {@link JobDetail JobDetails} for a given job name.
     * @param jobName the job name.
     * @return the {@link JobDetail JobDetails} or null if the job does not exist
     * @throws SchedulerException if unable to get the {@link JobDetail JobDetails} of a {@link Job}
     */
    JobDetail getJobDetail(String jobName) throws SchedulerException;

    /**
     * Deletes a job if it is present in the scheduler.
     * @param jobName the name of the job to delete
     * @throws SchedulerException if unable to delete the job in the scheduler
     */
    void deleteJobIfPresent(String jobName) throws SchedulerException;

    /**
     * Gets a {@link JsonValue} representation of the schedule with a given name. The format of the schedule looks like
     * this:
     * <pre>
     *     // TODO add the schedule format
     * </pre>
     * @param context the {@link Context} in use when this method was called
     * @param scheduleName the schedule name
     * @param instanceId the node instance id.
     * @return a {@link JsonValue} representation of the schedule
     * @throws SchedulerException
     * @throws IOException
     */
    JsonValue getSchedule(Context context, String scheduleName, String instanceId)
            throws SchedulerException, IOException;

    /**
     * Gets the currently executing jobs. Note that this method is not cluster aware and will only return the running
     * jobs for the instance this method was invoked on.
     * @return a {@link JsonValue} list of the currently executing jobs
     * @throws SchedulerException if unable to get the list of currently executing jobs
     * @throws IOException if unable to get the list of currently executing jobs
     */
    JsonValue getCurrentlyExecutingJobs() throws SchedulerException, IOException;

    /**
     * Starts the scheduler.
     * @throws SchedulerException if unable to start the scheduler
     */
    void start() throws SchedulerException;

    /**
     * Checks if the scheduler is started.
     * @return true if the scheduler is started; false otherwise
     * @throws SchedulerException if unable to get the state of the scheduler
     */
    boolean isStarted() throws SchedulerException;

    /**
     * Shutdown the scheduler.
     * @throws SchedulerException if unable to shutdown the scheduler
     */
    void shutdown() throws SchedulerException;
}
