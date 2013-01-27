/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openidm.quartz;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredResult;

/**
 * A ResourceJobStore ...
 * 
 * @author Laszlo Hordos
 */
public class DelegatingJobStore implements JobStore {

    static final ThreadLocal<JobStore> jobStoreHolder = new ThreadLocal<JobStore>();
    static final ThreadLocal<Boolean> isInitialisedHolder = new ThreadLocal<Boolean>();

    private volatile JobStore realJobStore;
    private String schedInstId = null;
    private String schedName = null;
    private int poolSize = -1;

    private void init() throws SchedulerConfigException {
        if (realJobStore != null) {
            return;
        }
        realJobStore = jobStoreHolder.get();
        if (null == realJobStore || null == isInitialisedHolder.get()) {
            throw new SchedulerConfigException(
                    "Unable to retrieve JobStore. The ResourceJobStore#setJobStoreHolder was not called");
        }
        jobStoreHolder.remove();
    }

    public static void setJobStoreHolder(JobStore realJobStore) {
        if (null == realJobStore) {
            throw new IllegalArgumentException("");
        }
        jobStoreHolder.set(realJobStore);
        isInitialisedHolder.set(Boolean.FALSE);
    }

    public static void setInitializedJobStoreHolder(JobStore realJobStore) {
        if (null == realJobStore) {
            throw new IllegalArgumentException("");
        }
        jobStoreHolder.set(realJobStore);
        isInitialisedHolder.set(Boolean.TRUE);
    }

    /**
     * Called by the QuartzScheduler before the <code>JobStore</code> is used,
     * in order to give the it a chance to initialize.
     */
    public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler)
            throws SchedulerConfigException {
        init();
        if (!isInitialisedHolder.get()) {
            // fire up job store
            realJobStore.setInstanceId(schedInstId);
            realJobStore.setInstanceName(schedName);
            realJobStore.setThreadPoolSize(poolSize);
            realJobStore.initialize(loadHelper, signaler);
            isInitialisedHolder.set(Boolean.TRUE);
        }
    }

    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * the scheduler has started.
     */
    public void schedulerStarted() throws SchedulerException {
        realJobStore.schedulerStarted();
    }

    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * the scheduler has been paused.
     */
    public void schedulerPaused() {
        realJobStore.schedulerPaused();
    }

    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * the scheduler has resumed after being paused.
     */
    public void schedulerResumed() {
        realJobStore.schedulerResumed();
    }

    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that it
     * should free up all of it's resources because the scheduler is shutting
     * down.
     */
    public void shutdown() {
        realJobStore.shutdown();
    }

    public boolean supportsPersistence() {
        return realJobStore.supportsPersistence();
    }

    /**
     * How long (in milliseconds) the <code>JobStore</code> implementation
     * estimates that it will take to release a trigger and acquire a new one.
     */
    public long getEstimatedTimeToReleaseAndAcquireTrigger() {
        return realJobStore.getEstimatedTimeToReleaseAndAcquireTrigger();
    }

    /**
     * Whether or not the <code>JobStore</code> implementation is clustered.
     */
    public boolean isClustered() {
        return realJobStore.isClustered();
    }

    /**
     * Store the given <code>{@link org.quartz.JobDetail}</code> and
     * <code>{@link org.quartz.Trigger}</code>.
     * 
     * @param newJob
     *            The <code>JobDetail</code> to be stored.
     * @param newTrigger
     *            The <code>Trigger</code> to be stored.
     * @throws org.quartz.ObjectAlreadyExistsException
     *             if a <code>Job</code> with the same name/group already
     *             exists.
     */
    public void storeJobAndTrigger(JobDetail newJob, OperableTrigger newTrigger)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        realJobStore.storeJobAndTrigger(newJob, newTrigger);
    }

    /**
     * Store the given <code>{@link org.quartz.JobDetail}</code>.
     * 
     * @param newJob
     *            The <code>JobDetail</code> to be stored.
     * @param replaceExisting
     *            If <code>true</code>, any <code>Job</code> existing in the
     *            <code>JobStore</code> with the same name & group should be
     *            over-written.
     * @throws org.quartz.ObjectAlreadyExistsException
     *             if a <code>Job</code> with the same name/group already
     *             exists, and replaceExisting is set to false.
     */
    public void storeJob(JobDetail newJob, boolean replaceExisting)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        realJobStore.storeJob(newJob, replaceExisting);
    }

    public void storeJobsAndTriggers(Map<JobDetail, List<Trigger>> triggersAndJobs, boolean replace)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        realJobStore.storeJobsAndTriggers(triggersAndJobs, replace);
    }

    /**
     * Remove (delete) the <code>{@link org.quartz.Job}</code> with the given
     * key, and any <code>{@link org.quartz.Trigger}</code> s that reference it.
     * <p/>
     * <p>
     * If removal of the <code>Job</code> results in an empty group, the group
     * should be removed from the <code>JobStore</code>'s list of known group
     * names.
     * </p>
     * 
     * @return <code>true</code> if a <code>Job</code> with the given name &
     *         group was found and removed from the store.
     */
    public boolean removeJob(JobKey jobKey) throws JobPersistenceException {
        return realJobStore.removeJob(jobKey);
    }

    public boolean removeJobs(List<JobKey> jobKeys) throws JobPersistenceException {
        return realJobStore.removeJobs(jobKeys);
    }

    /**
     * Retrieve the <code>{@link org.quartz.JobDetail}</code> for the given
     * <code>{@link org.quartz.Job}</code>.
     * 
     * @return The desired <code>Job</code>, or null if there is no match.
     */
    public JobDetail retrieveJob(JobKey jobKey) throws JobPersistenceException {
        return realJobStore.retrieveJob(jobKey);
    }

    /**
     * Store the given <code>{@link org.quartz.Trigger}</code>.
     * 
     * @param newTrigger
     *            The <code>Trigger</code> to be stored.
     * @param replaceExisting
     *            If <code>true</code>, any <code>Trigger</code> existing in the
     *            <code>JobStore</code> with the same name & group should be
     *            over-written.
     * @throws org.quartz.ObjectAlreadyExistsException
     *             if a <code>Trigger</code> with the same name/group already
     *             exists, and replaceExisting is set to false.
     * @see #pauseTriggers(org.quartz.impl.matchers.GroupMatcher)
     */
    public void storeTrigger(OperableTrigger newTrigger, boolean replaceExisting)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        realJobStore.storeTrigger(newTrigger, replaceExisting);
    }

    /**
     * Remove (delete) the <code>{@link org.quartz.Trigger}</code> with the
     * given key.
     * <p/>
     * <p>
     * If removal of the <code>Trigger</code> results in an empty group, the
     * group should be removed from the <code>JobStore</code>'s list of known
     * group names.
     * </p>
     * <p/>
     * <p>
     * If removal of the <code>Trigger</code> results in an 'orphaned'
     * <code>Job</code> that is not 'durable', then the <code>Job</code> should
     * be deleted also.
     * </p>
     * 
     * @return <code>true</code> if a <code>Trigger</code> with the given name &
     *         group was found and removed from the store.
     */
    public boolean removeTrigger(TriggerKey triggerKey) throws JobPersistenceException {
        return realJobStore.removeTrigger(triggerKey);
    }

    public boolean removeTriggers(List<TriggerKey> triggerKeys) throws JobPersistenceException {
        return realJobStore.removeTriggers(triggerKeys);
    }

    /**
     * Remove (delete) the <code>{@link org.quartz.Trigger}</code> with the
     * given key, and store the new given one - which must be associated with
     * the same job.
     * 
     * @param newTrigger
     *            The new <code>Trigger</code> to be stored.
     * @return <code>true</code> if a <code>Trigger</code> with the given name &
     *         group was found and removed from the store.
     */
    public boolean replaceTrigger(TriggerKey triggerKey, OperableTrigger newTrigger)
            throws JobPersistenceException {
        return realJobStore.replaceTrigger(triggerKey, newTrigger);
    }

    /**
     * Retrieve the given <code>{@link org.quartz.Trigger}</code>.
     * 
     * @return The desired <code>Trigger</code>, or null if there is no match.
     */
    public OperableTrigger retrieveTrigger(TriggerKey triggerKey) throws JobPersistenceException {
        return realJobStore.retrieveTrigger(triggerKey);
    }

    /**
     * Determine whether a {@link org.quartz.Job} with the given identifier
     * already exists within the scheduler.
     * 
     * @param jobKey
     *            the identifier to check for
     * @return true if a Job exists with the given identifier
     * @throws org.quartz.SchedulerException
     */
    public boolean checkExists(JobKey jobKey) throws JobPersistenceException {
        return realJobStore.checkExists(jobKey);
    }

    /**
     * Determine whether a {@link org.quartz.Trigger} with the given identifier
     * already exists within the scheduler.
     * 
     * @param triggerKey
     *            the identifier to check for
     * @return true if a Trigger exists with the given identifier
     * @throws org.quartz.SchedulerException
     */
    public boolean checkExists(TriggerKey triggerKey) throws JobPersistenceException {
        return realJobStore.checkExists(triggerKey);
    }

    /**
     * Clear (delete!) all scheduling data - all {@link org.quartz.Job}s,
     * {@link org.quartz.Trigger}s {@link org.quartz.Calendar}s.
     * 
     * @throws org.quartz.JobPersistenceException
     * 
     */
    public void clearAllSchedulingData() throws JobPersistenceException {
        realJobStore.clearAllSchedulingData();
    }

    /**
     * Store the given <code>{@link org.quartz.Calendar}</code>.
     * 
     * @param calendar
     *            The <code>Calendar</code> to be stored.
     * @param replaceExisting
     *            If <code>true</code>, any <code>Calendar</code> existing in
     *            the <code>JobStore</code> with the same name & group should be
     *            over-written.
     * @param updateTriggers
     *            If <code>true</code>, any <code>Trigger</code>s existing in
     *            the <code>JobStore</code> that reference an existing Calendar
     *            with the same name with have their next fire time re-computed
     *            with the new <code>Calendar</code>.
     * @throws org.quartz.ObjectAlreadyExistsException
     *             if a <code>Calendar</code> with the same name already exists,
     *             and replaceExisting is set to false.
     */
    public void storeCalendar(String name, Calendar calendar, boolean replaceExisting,
            boolean updateTriggers) throws ObjectAlreadyExistsException, JobPersistenceException {
        realJobStore.storeCalendar(name, calendar, replaceExisting, updateTriggers);
    }

    /**
     * Remove (delete) the <code>{@link org.quartz.Calendar}</code> with the
     * given name.
     * <p/>
     * <p>
     * If removal of the <code>Calendar</code> would result in
     * <code>Trigger</code>s pointing to non-existent calendars, then a
     * <code>JobPersistenceException</code> will be thrown.
     * </p>
     * *
     * 
     * @param calName
     *            The name of the <code>Calendar</code> to be removed.
     * @return <code>true</code> if a <code>Calendar</code> with the given name
     *         was found and removed from the store.
     */
    public boolean removeCalendar(String calName) throws JobPersistenceException {
        return realJobStore.removeCalendar(calName);
    }

    /**
     * Retrieve the given <code>{@link org.quartz.Trigger}</code>.
     * 
     * @param calName
     *            The name of the <code>Calendar</code> to be retrieved.
     * @return The desired <code>Calendar</code>, or null if there is no match.
     */
    public Calendar retrieveCalendar(String calName) throws JobPersistenceException {
        return realJobStore.retrieveCalendar(calName);
    }

    /**
     * Get the number of <code>{@link org.quartz.Job}</code> s that are stored
     * in the <code>JobsStore</code>.
     */
    public int getNumberOfJobs() throws JobPersistenceException {
        return realJobStore.getNumberOfJobs();
    }

    /**
     * Get the number of <code>{@link org.quartz.Trigger}</code> s that are
     * stored in the <code>JobsStore</code>.
     */
    public int getNumberOfTriggers() throws JobPersistenceException {
        return realJobStore.getNumberOfTriggers();
    }

    /**
     * Get the number of <code>{@link org.quartz.Calendar}</code> s that are
     * stored in the <code>JobsStore</code>.
     */
    public int getNumberOfCalendars() throws JobPersistenceException {
        return realJobStore.getNumberOfCalendars();
    }

    /**
     * Get the keys of all of the <code>{@link org.quartz.Job}</code> s that
     * have the given group name.
     * <p/>
     * <p>
     * If there are no jobs in the given group name, the result should be an
     * empty collection (not <code>null</code>).
     * </p>
     */
    public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws JobPersistenceException {
        return realJobStore.getJobKeys(matcher);
    }

    /**
     * Get the names of all of the <code>{@link org.quartz.Trigger}</code> s
     * that have the given group name.
     * <p/>
     * <p>
     * If there are no triggers in the given group name, the result should be a
     * zero-length array (not <code>null</code>).
     * </p>
     */
    public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher)
            throws JobPersistenceException {
        return realJobStore.getTriggerKeys(matcher);
    }

    /**
     * Get the names of all of the <code>{@link org.quartz.Job}</code> groups.
     * <p/>
     * <p>
     * If there are no known group names, the result should be a zero-length
     * array (not <code>null</code>).
     * </p>
     */
    public List<String> getJobGroupNames() throws JobPersistenceException {
        return realJobStore.getJobGroupNames();
    }

    /**
     * Get the names of all of the <code>{@link org.quartz.Trigger}</code>
     * groups.
     * <p/>
     * <p>
     * If there are no known group names, the result should be a zero-length
     * array (not <code>null</code>).
     * </p>
     */
    public List<String> getTriggerGroupNames() throws JobPersistenceException {
        return realJobStore.getTriggerGroupNames();
    }

    /**
     * Get the names of all of the <code>{@link org.quartz.Calendar}</code> s in
     * the <code>JobStore</code>.
     * <p/>
     * <p>
     * If there are no Calendars in the given group name, the result should be a
     * zero-length array (not <code>null</code>).
     * </p>
     */
    public List<String> getCalendarNames() throws JobPersistenceException {
        return realJobStore.getCalendarNames();
    }

    /**
     * Get all of the Triggers that are associated to the given Job.
     * <p/>
     * <p>
     * If there are no matches, a zero-length array should be returned.
     * </p>
     */
    public List<OperableTrigger> getTriggersForJob(JobKey jobKey) throws JobPersistenceException {
        return realJobStore.getTriggersForJob(jobKey);
    }

    /**
     * Get the current state of the identified
     * <code>{@link org.quartz.Trigger}</code>.
     * 
     * @see org.quartz.Trigger.TriggerState
     */
    public Trigger.TriggerState getTriggerState(TriggerKey triggerKey)
            throws JobPersistenceException {
        return realJobStore.getTriggerState(triggerKey);
    }

    /**
     * Pause the <code>{@link org.quartz.Trigger}</code> with the given key.
     * 
     * @see #resumeTrigger(org.quartz.TriggerKey)
     */
    public void pauseTrigger(TriggerKey triggerKey) throws JobPersistenceException {
        realJobStore.pauseTrigger(triggerKey);
    }

    /**
     * Pause all of the <code>{@link org.quartz.Trigger}s</code> in the given
     * group.
     * <p/>
     * <p/>
     * <p>
     * The JobStore should "remember" that the group is paused, and impose the
     * pause on any new triggers that are added to the group while the group is
     * paused.
     * </p>
     * 
     * @see #resumeTriggers(GroupMatcher)
     */
    public Collection<String> pauseTriggers(GroupMatcher<TriggerKey> matcher)
            throws JobPersistenceException {
        return realJobStore.pauseTriggers(matcher);
    }

    /**
     * Pause the <code>{@link org.quartz.Job}</code> with the given name - by
     * pausing all of its current <code>Trigger</code>s.
     * 
     * @see #resumeJob(org.quartz.JobKey)
     */
    public void pauseJob(JobKey jobKey) throws JobPersistenceException {
        realJobStore.pauseJob(jobKey);
    }

    /**
     * Pause all of the <code>{@link org.quartz.Job}s</code> in the given group
     * - by pausing all of their <code>Trigger</code>s.
     * <p/>
     * <p>
     * The JobStore should "remember" that the group is paused, and impose the
     * pause on any new jobs that are added to the group while the group is
     * paused.
     * </p>
     * 
     * @see #resumeJobs(GroupMatcher)
     */
    public Collection<String> pauseJobs(GroupMatcher<JobKey> groupMatcher)
            throws JobPersistenceException {
        return realJobStore.pauseJobs(groupMatcher);
    }

    /**
     * Resume (un-pause) the <code>{@link org.quartz.Trigger}</code> with the
     * given key.
     * <p/>
     * <p>
     * If the <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     * 
     * @see #pauseTrigger(org.quartz.TriggerKey)
     */
    public void resumeTrigger(TriggerKey triggerKey) throws JobPersistenceException {
        realJobStore.resumeTrigger(triggerKey);
    }

    /**
     * Resume (un-pause) all of the <code>{@link org.quartz.Trigger}s</code> in
     * the given group.
     * <p/>
     * <p>
     * If any <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     * 
     * @see #pauseTriggers(GroupMatcher)
     */
    public Collection<String> resumeTriggers(GroupMatcher<TriggerKey> matcher)
            throws JobPersistenceException {
        return realJobStore.resumeTriggers(matcher);
    }

    public Set<String> getPausedTriggerGroups() throws JobPersistenceException {
        return realJobStore.getPausedTriggerGroups();
    }

    /**
     * Resume (un-pause) the <code>{@link org.quartz.Job}</code> with the given
     * key.
     * <p/>
     * <p>
     * If any of the <code>Job</code>'s<code>Trigger</code> s missed one or more
     * fire-times, then the <code>Trigger</code>'s misfire instruction will be
     * applied.
     * </p>
     * 
     * @see #pauseJob(org.quartz.JobKey)
     */
    public void resumeJob(JobKey jobKey) throws JobPersistenceException {
        realJobStore.resumeJob(jobKey);
    }

    /**
     * Resume (un-pause) all of the <code>{@link org.quartz.Job}s</code> in the
     * given group.
     * <p/>
     * <p>
     * If any of the <code>Job</code> s had <code>Trigger</code> s that missed
     * one or more fire-times, then the <code>Trigger</code>'s misfire
     * instruction will be applied.
     * </p>
     * 
     * @see #pauseJobs(GroupMatcher)
     */
    public Collection<String> resumeJobs(GroupMatcher<JobKey> matcher)
            throws JobPersistenceException {
        return realJobStore.resumeJobs(matcher);
    }

    /**
     * Pause all triggers - equivalent of calling
     * <code>pauseTriggerGroup(group)</code> on every group.
     * <p/>
     * <p>
     * When <code>resumeAll()</code> is called (to un-pause), trigger misfire
     * instructions WILL be applied.
     * </p>
     * 
     * @see #resumeAll()
     * @see #pauseTriggers(GroupMatcher)
     */
    public void pauseAll() throws JobPersistenceException {
        realJobStore.pauseAll();
    }

    /**
     * Resume (un-pause) all triggers - equivalent of calling
     * <code>resumeTriggerGroup(group)</code> on every group.
     * <p/>
     * <p>
     * If any <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     * 
     * @see #pauseAll()
     */
    public void resumeAll() throws JobPersistenceException {
        realJobStore.resumeAll();
    }

    /**
     * Get a handle to the next trigger to be fired, and mark it as 'reserved'
     * by the calling scheduler.
     * 
     * @param noLaterThan
     *            If > 0, the JobStore should only return a Trigger that will
     *            fire no later than the time represented in this value as
     *            milliseconds.
     * @see #releaseAcquiredTrigger(OperableTrigger)
     */
    public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow)
            throws JobPersistenceException {
        return realJobStore.acquireNextTriggers(noLaterThan, maxCount, timeWindow);
    }

    /**
     * Inform the <code>JobStore</code> that the scheduler no longer plans to
     * fire the given <code>Trigger</code>, that it had previously acquired
     * (reserved).
     */
    public void releaseAcquiredTrigger(OperableTrigger trigger) throws JobPersistenceException {
        realJobStore.releaseAcquiredTrigger(trigger);
    }

    /**
     * Inform the <code>JobStore</code> that the scheduler is now firing the
     * given <code>Trigger</code> (executing its associated <code>Job</code>),
     * that it had previously acquired (reserved).
     * 
     * @return may return null if all the triggers or their calendars no longer
     *         exist, or if the trigger was not successfully put into the
     *         'executing' state. Preference is to return an empty list if none
     *         of the triggers could be fired.
     */
    public List<TriggerFiredResult> triggersFired(List<OperableTrigger> triggers)
            throws JobPersistenceException {
        return realJobStore.triggersFired(triggers);
    }

    /**
     * Inform the <code>JobStore</code> that the scheduler has completed the
     * firing of the given <code>Trigger</code> (and the execution of its
     * associated <code>Job</code> completed, threw an exception, or was
     * vetoed), and that the <code>{@link org.quartz.JobDataMap}</code> in the
     * given <code>JobDetail</code> should be updated if the <code>Job</code> is
     * stateful.
     */
    public void triggeredJobComplete(OperableTrigger trigger, JobDetail jobDetail,
            Trigger.CompletedExecutionInstruction triggerInstCode) throws JobPersistenceException {
        realJobStore.triggeredJobComplete(trigger, jobDetail, triggerInstCode);
    }

    /**
     * Inform the <code>JobStore</code> of the Scheduler instance's Id, prior to
     * initialize being invoked.
     * 
     * @since 1.7
     */
    public void setInstanceId(String schedInstId) {
        this.schedInstId = schedInstId;
    }

    /**
     * Inform the <code>JobStore</code> of the Scheduler instance's name, prior
     * to initialize being invoked.
     * 
     * @since 1.7
     */
    public void setInstanceName(String schedName) {
        this.schedName = schedName;
    }

    /**
     * Tells the JobStore the pool size used to execute jobs
     * 
     * @param poolSize
     *            amount of threads allocated for job execution
     * @since 2.0
     */
    public void setThreadPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }
}
