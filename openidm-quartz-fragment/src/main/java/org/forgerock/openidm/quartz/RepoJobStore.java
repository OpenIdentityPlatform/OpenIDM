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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.DefaultThreadExecutor;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.StringMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.ThreadExecutor;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.spi.TriggerFiredResult;
import org.quartz.utils.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class RepoJobStore implements JobStore {


     /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Constants.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected static final String LOCK_TRIGGER_ACCESS = "TRIGGER_ACCESS";

    protected static final String LOCK_JOB_ACCESS = "JOB_ACCESS";

    protected static final String LOCK_CALENDAR_ACCESS = "CALENDAR_ACCESS";

    protected static final String LOCK_STATE_ACCESS = "STATE_ACCESS";

    protected static final String LOCK_MISFIRE_ACCESS = "MISFIRE_ACCESS";

    String ALL_GROUPS_PAUSED = "_$_ALL_GROUPS_PAUSED_$_";

    // TRIGGER STATES
    String STATE_WAITING = "WAITING";

    String STATE_ACQUIRED = "ACQUIRED";

    String STATE_EXECUTING = "EXECUTING";

    String STATE_COMPLETE = "COMPLETE";

    String STATE_BLOCKED = "BLOCKED";

    String STATE_ERROR = "ERROR";

    String STATE_PAUSED = "PAUSED";

    String STATE_PAUSED_BLOCKED = "PAUSED_BLOCKED";

    String STATE_DELETED = "DELETED";



    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Data members.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected String dsName;

    protected boolean useProperties = false;

    protected String instanceId;

    protected String instanceName;

    protected String delegateClassName;

    protected String delegateInitString;


    protected HashMap<String, Calendar> calendarCache = new HashMap<String, Calendar>();

    private long misfireThreshold = 60000L; // one minute

    private boolean dontSetAutoCommitFalse = false;

    private boolean isClustered = false;

    private boolean useDBLocks = false;

    private boolean lockOnInsert = true;

    private Semaphore lockHandler = null; // set in initialize() method...

    private String selectWithLockSQL = null;

    private long clusterCheckinInterval = 7500L;

    //private ClusterManager clusterManagementThread = null;

    private MisfireHandler misfireHandler = null;

    private ClassLoadHelper classLoadHelper;

    private SchedulerSignaler schedSignaler;

    protected int maxToRecoverAtATime = 20;

    private boolean setTxIsolationLevelSequential = false;

    private boolean acquireTriggersWithinLock = false;

    private long dbRetryInterval = 15000L; // 15 secs

    private boolean makeThreadsDaemons = false;

    private boolean threadsInheritInitializersClassLoadContext = false;
    private ClassLoader initializersLoader = null;

    private boolean doubleCheckLockMisfireHandler = true;

    /**
     * Setup logging for the {@link RepoJobStore}.
     */
    private final static Logger logger = LoggerFactory.getLogger(RepoJobStore.class);

    private ThreadExecutor threadExecutor = new DefaultThreadExecutor();

    private boolean schedulerRunning = false;

    private ServerContext serverContext;

    private ServerContext getServerContext() {
        return serverContext;
    }

    protected Semaphore getLockHandler() {
        return lockHandler;
    }

    public void setLockHandler(Semaphore lockHandler) {
        this.lockHandler = lockHandler;
    }

    public ThreadExecutor getThreadExecutor() {
        return threadExecutor;
    }

    public long getMisfireThreshold() {
        return misfireThreshold;
    }

    protected ClassLoadHelper getClassLoadHelper() {
        return classLoadHelper;
    }

    public boolean isLockOnInsert() {
        return lockOnInsert;
    }

    /**
     * <p>
     * Get the maximum number of misfired triggers that the misfire handling
     * thread will try to recover at one time (within one transaction).  The
     * default is 20.
     * </p>
     */
    public int getMaxMisfiresToHandleAtATime() {
        return maxToRecoverAtATime;
    }

    /**
     * <p>
     * Get the instance Id of the Scheduler (must be unique within a cluster).
     * </p>
     */
    public String getInstanceId() {

        return instanceId;
    }

    /**
     * Inform the <code>JobStore</code> of the Scheduler instance's Id (must be unique within a cluster), prior to
     * initialize being invoked.
     *
     * @since 1.7
     */
    @Override
    public void setInstanceId(String schedInstId) {
        this.instanceId = instanceId;
    }

    /**
     * Inform the <code>JobStore</code> of the Scheduler instance's name, prior
     * to initialize being invoked.
     *
     * @since 1.7
     */
    @Override
    public void setInstanceName(String schedName) {
        this.instanceName = instanceName;
    }

    /**
     * Tells the JobStore the pool size used to execute jobs
     *
     * @param poolSize
     *            amount of threads allocated for job execution
     * @since 2.0
     */
    @Override
    public void setThreadPoolSize(int poolSize) {

    }

    /**
     * The the number of milliseconds by which a trigger must have missed its
     * next-fire-time, in order for it to be considered "misfired" and thus
     * have its misfire instruction applied.
     *
     * @param misfireThreshold
     */
    public void setMisfireThreshold(long misfireThreshold) {
        if (misfireThreshold < 1) {
            throw new IllegalArgumentException(
                    "Misfirethreshold must be larger than 0");
        }
        this.misfireThreshold = misfireThreshold;
    }


    //--------------

    /**
     * Called by the QuartzScheduler before the <code>JobStore</code> is used,
     * in order to give the it a chance to initialize.
     */
    @Override
    public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler)
            throws SchedulerConfigException {

        if (dsName == null) {
            throw new SchedulerConfigException("DataSource name not set.");
        }

        classLoadHelper = loadHelper;
        //if(isThreadsInheritInitializersClassLoadContext()) {
            logger.info("JDBCJobStore threads will inherit ContextClassLoader of thread: " + Thread.currentThread().getName());
            initializersLoader = Thread.currentThread().getContextClassLoader();
        //}

        this.schedSignaler = signaler;

        // If the user hasn't specified an explicit lock handler, then
        // choose one based on CMT/Clustered/UseDBLocks.
        if (getLockHandler() == null) {

            // If the user hasn't specified an explicit lock handler,
            // then we *must* use DB locks with clustering
            /*if (isClustered()) {
                setUseDBLocks(true);
            }

            if (getUseDBLocks()) {
                if(getDriverDelegateClass() != null && getDriverDelegateClass().equals(MSSQLDelegate.class.getName())) {
                    if(getSelectWithLockSQL() == null) {
                        String msSqlDflt = "SELECT * FROM {0}LOCKS WITH (UPDLOCK,ROWLOCK) WHERE " + COL_SCHEDULER_NAME + " = {1} AND LOCK_NAME = ?";
                        logger.info("Detected usage of MSSQLDelegate class - defaulting 'selectWithLockSQL' to '" + msSqlDflt + "'.");
                        setSelectWithLockSQL(msSqlDflt);
                    }
                }
                logger.info("Using db table-based data access locking (synchronization).");
                setLockHandler(new StdRowLockSemaphore(getTablePrefix(), getInstanceName(), getSelectWithLockSQL()));
            } else {
                logger.info(
                        "Using thread monitor-based data access locking (synchronization).");
                setLockHandler(new SimpleSemaphore());
            }*/
        }

    }

    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * the scheduler has started.
     */
    @Override
    public void schedulerStarted() throws SchedulerException {
        if (isClustered()) {
            //clusterManagementThread = new ClusterManager();
            //if(initializersLoader != null) clusterManagementThread.setContextClassLoader(initializersLoader);
            //clusterManagementThread.initialize();
        } else {
            try {
                recoverJobs();
            } catch (SchedulerException se) {
                throw new SchedulerConfigException(
                        "Failure occured during job recovery.", se);
            }
        }

        misfireHandler = new MisfireHandler();
        if(initializersLoader != null)
            misfireHandler.setContextClassLoader(initializersLoader);
        misfireHandler.initialize();
        schedulerRunning = true;

        logger.debug("JobStore background threads started (as scheduler was started).");
    }

    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * the scheduler has been paused.
     */
    @Override
    public void schedulerPaused() {
        schedulerRunning = false;
    }

    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * the scheduler has resumed after being paused.
     */
    @Override
    public void schedulerResumed() {
        schedulerRunning = true;
    }

    /**
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that it
     * should free up all of it's resources because the scheduler is shutting
     * down.
     */
    @Override
    public void shutdown() {
        if (misfireHandler != null) {
            misfireHandler.shutdown();
            try {
                misfireHandler.join();
            } catch (InterruptedException ignoreInterruptionOfThisThread) {
            }
        }

/*        if (clusterManagementThread != null) {
            clusterManagementThread.shutdown();
            try {
                clusterManagementThread.join();
            } catch (InterruptedException ignoreInterruptionOfThisThread) {
            }
        }*/


        logger.debug("JobStore background threads shutdown.");
    }

    @Override
    public boolean supportsPersistence() {
        return true;
    }

    /**
     * How long (in milliseconds) the <code>JobStore</code> implementation
     * estimates that it will take to release a trigger and acquire a new one.
     */
    @Override
    public long getEstimatedTimeToReleaseAndAcquireTrigger() {
        return 70;
    }

    /**
     * <p>
     * Set whether this instance is part of a cluster.
     * </p>
     */
    public void setIsClustered(boolean isClustered) {
        this.isClustered = isClustered;
    }

    /**
     * Whether or not the <code>JobStore</code> implementation is clustered.
     */
    @Override
    public boolean isClustered() {
        return isClustered;
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
    @Override
    public void storeJobAndTrigger(final JobDetail newJob,final  OperableTrigger newTrigger)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        executeInLock(
                (isLockOnInsert()) ? LOCK_TRIGGER_ACCESS : null,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {
                        storeJob(context, newJob, false);
                        storeTrigger(context, newTrigger, newJob, false,
                                STATE_WAITING, false, false);
                        return null;
                    }
                });
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
    @Override
    public void storeJob(final JobDetail newJob, final boolean replaceExisting)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        executeInLock(
                (isLockOnInsert() || replaceExisting) ? LOCK_TRIGGER_ACCESS : null,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {
                        storeJob(context, newJob, replaceExisting);
                        return null;
                    }
                });
    }

    /**
     * <p>
     * Insert or update a job.
     * </p>
     */
    protected void storeJob(ServerContext context,
                            JobDetail newJob, boolean replaceExisting)
            throws ObjectAlreadyExistsException, JobPersistenceException {

        boolean existingJob = jobExists(context, newJob.getKey());
        try {
            if (existingJob) {
                if (!replaceExisting) {
                    throw new ObjectAlreadyExistsException(newJob);
                }
                updateJobDetail(context, newJob);
            } else {
                insertJobDetail(context, newJob);
            }
        } catch (IOException e) {
            throw new JobPersistenceException("Couldn't store job: "
                    + e.getMessage(), e);
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't store job: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public void storeJobsAndTriggers(final Map<JobDetail, List<Trigger>> triggersAndJobs,final boolean replace)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        executeInLock(
                (isLockOnInsert() || replace) ? LOCK_TRIGGER_ACCESS : null,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {

                        // TODO: make this more efficient with a true bulk operation...
                        for(JobDetail job: triggersAndJobs.keySet()) {
                            storeJob(context, job, replace);
                            for(Trigger trigger: triggersAndJobs.get(job)) {
                                storeTrigger(context, (OperableTrigger) trigger, job, replace,
                                        STATE_WAITING, false, false);
                            }
                        }
                        return null;
                    }
                });
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
    @Override
    public boolean removeJob(final JobKey jobKey) throws JobPersistenceException {
        return executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback<Boolean>() {
                    public Boolean execute(final ServerContext context) throws JobPersistenceException {
                        return removeJob(context, jobKey, true) ?
                                Boolean.TRUE : Boolean.FALSE;
                    }
                });
    }


    protected boolean removeJob(final ServerContext context, final JobKey jobKey, boolean activeDeleteSafe)
            throws JobPersistenceException {

        try {
            List<TriggerKey> jobTriggers = selectTriggerKeysForJob(context, jobKey);
            for (TriggerKey jobTrigger: jobTriggers) {
                deleteTriggerAndChildren(context, jobTrigger);
            }

            return deleteJobAndChildren(context, jobKey);
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't remove job: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public boolean removeJobs(final List<JobKey> jobKeys) throws JobPersistenceException {
        return executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback<Boolean>() {
                    public Boolean execute(final ServerContext context) throws JobPersistenceException {
                        boolean allFound = true;

                        // TODO: make this more efficient with a true bulk operation...
                        for(JobKey jobKey: jobKeys)
                            allFound = removeJob(context, jobKey, true) && allFound;

                        return allFound ? Boolean.TRUE : Boolean.FALSE;
                    }
                });
    }

    /**
     * Retrieve the <code>{@link org.quartz.JobDetail}</code> for the given
     * <code>{@link org.quartz.Job}</code>.
     * 
     * @return The desired <code>Job</code>, or null if there is no match.
     */
    @Override
    public JobDetail retrieveJob(final JobKey jobKey) throws JobPersistenceException {
        try {
            return selectJobDetail(getServerContext(), jobKey,
                    getClassLoadHelper());
        } catch (ClassNotFoundException e) {
            throw new JobPersistenceException(
                    "Couldn't retrieve job because a required class was not found: "
                            + e.getMessage(), e);
        } catch (IOException e) {
            throw new JobPersistenceException(
                    "Couldn't retrieve job because the BLOB couldn't be deserialized: "
                            + e.getMessage(), e);
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't retrieve job: "
                    + e.getMessage(), e);
        }
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
    @Override
    public void storeTrigger(final OperableTrigger newTrigger,final  boolean replaceExisting)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        executeInLock(
                (isLockOnInsert() || replaceExisting) ? LOCK_TRIGGER_ACCESS : null,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {
                        storeTrigger(context, newTrigger, null, replaceExisting,
                                STATE_WAITING, false, false);
                        return null;
                    }
                });
    }

    /**
     * <p>
     * Insert or update a trigger.
     * </p>
     */
    protected void storeTrigger(final ServerContext context,
                                OperableTrigger newTrigger, JobDetail job, boolean replaceExisting, String state,
                                boolean forceState, boolean recovering)
            throws ObjectAlreadyExistsException, JobPersistenceException {

        boolean existingTrigger = triggerExists(context, newTrigger.getKey());

        if ((existingTrigger) && (!replaceExisting)) {
            throw new ObjectAlreadyExistsException(newTrigger);
        }

        try {

            boolean shouldBepaused = false;

            if (!forceState) {
                shouldBepaused = isTriggerGroupPaused(
                        context, newTrigger.getKey().getGroup());

                if(!shouldBepaused) {
                    shouldBepaused = isTriggerGroupPaused(context,
                            ALL_GROUPS_PAUSED);

                    if (shouldBepaused) {
                        insertPausedTriggerGroup(context, newTrigger.getKey().getGroup());
                    }
                }

                if (shouldBepaused && (state.equals(STATE_WAITING) || state.equals(STATE_ACQUIRED))) {
                    state = STATE_PAUSED;
                }
            }

            if(job == null) {
                job = selectJobDetail(context, newTrigger.getJobKey(), getClassLoadHelper());
            }
            if (job == null) {
                throw new JobPersistenceException("The job ("
                        + newTrigger.getJobKey()
                        + ") referenced by the trigger does not exist.");
            }

            if (job.isConcurrentExectionDisallowed() && !recovering) {
                state = checkBlockedState(context, job.getKey(), state);
            }

            if (existingTrigger) {
                updateTrigger(context, newTrigger, state, job);
            } else {
               insertTrigger(context, newTrigger, state, job);
            }
        } catch (Exception e) {
            throw new JobPersistenceException("Couldn't store trigger '" + newTrigger.getKey() + "' for '"
                    + newTrigger.getJobKey() + "' job:" + e.getMessage(), e);
        }
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
    @Override
    public boolean removeTrigger(final TriggerKey triggerKey) throws JobPersistenceException {
        return ((Boolean)executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback() {
                    public Object execute(final ServerContext context) throws JobPersistenceException {
                        return removeTrigger(context, triggerKey) ?
                                Boolean.TRUE : Boolean.FALSE;
                    }
                })).booleanValue();
    }

    protected boolean removeTrigger(final ServerContext context, TriggerKey key)
            throws JobPersistenceException {
        boolean removedTrigger = false;
        try {
            // this must be called before we delete the trigger, obviously
            JobDetail job = selectJobForTrigger(context,
                    getClassLoadHelper(), key);

            removedTrigger =
                    deleteTriggerAndChildren(context, key);

            if (null != job && !job.isDurable()) {
                int numTriggers = selectNumTriggersForJob(context,
                        job.getKey());
                if (numTriggers == 0) {
                    // Don't call removeJob() because we don't want to check for
                    // triggers again.
                    deleteJobAndChildren(context, job.getKey());
                }
            }
        } catch (ClassNotFoundException e) {
            throw new JobPersistenceException("Couldn't remove trigger: "
                    + e.getMessage(), e);
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't remove trigger: "
                    + e.getMessage(), e);
        }

        return removedTrigger;
    }

    @Override
    public boolean removeTriggers(final List<TriggerKey> triggerKeys) throws JobPersistenceException {
        return ((Boolean)executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback() {
                    public Object execute(final ServerContext context) throws JobPersistenceException {
                        boolean allFound = true;

                        // TODO: make this more efficient with a true bulk operation...
                        for(TriggerKey triggerKey: triggerKeys)
                            allFound = removeTrigger(context, triggerKey) && allFound;

                        return allFound ? Boolean.TRUE : Boolean.FALSE;
                    }
                })).booleanValue();
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
    @Override
    public boolean replaceTrigger(final TriggerKey triggerKey, final OperableTrigger newTrigger)
            throws JobPersistenceException {
        return ((Boolean)executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback() {
                    public Object execute(final ServerContext context) throws JobPersistenceException {
                        return replaceTrigger(context, triggerKey, newTrigger) ?
                                Boolean.TRUE : Boolean.FALSE;
                    }
                })).booleanValue();
    }

    protected boolean replaceTrigger(final ServerContext context,
                                     TriggerKey key, OperableTrigger newTrigger)
            throws JobPersistenceException {
        try {
            // this must be called before we delete the trigger, obviously
            JobDetail job = selectJobForTrigger(context,
                    getClassLoadHelper(), key);

            if (job == null) {
                return false;
            }

            if (!newTrigger.getJobKey().equals(job.getKey())) {
                throw new JobPersistenceException("New trigger is not related to the same job as the old trigger.");
            }

            boolean removedTrigger =
                    deleteTriggerAndChildren(context, key);

            storeTrigger(context, newTrigger, job, false, STATE_WAITING, false, false);

            return removedTrigger;
        } catch (ClassNotFoundException e) {
            throw new JobPersistenceException("Couldn't remove trigger: "
                    + e.getMessage(), e);
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't remove trigger: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Retrieve the given <code>{@link org.quartz.Trigger}</code>.
     * 
     * @return The desired <code>Trigger</code>, or null if there is no match.
     */
    @Override
    public OperableTrigger retrieveTrigger(final TriggerKey triggerKey) throws JobPersistenceException {
        try {
            return selectTrigger(getServerContext(), triggerKey);
        } catch (Exception e) {
            throw new JobPersistenceException("Couldn't retrieve trigger: "
                    + e.getMessage(), e);
        }
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
    @Override
    public boolean checkExists(JobKey jobKey) throws JobPersistenceException {
        return false;
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
    @Override
    public boolean checkExists(TriggerKey triggerKey) throws JobPersistenceException {
        return false;
    }

    /**
     * Clear (delete!) all scheduling data - all {@link org.quartz.Job}s,
     * {@link org.quartz.Trigger}s {@link org.quartz.Calendar}s.
     * 
     * @throws org.quartz.JobPersistenceException
     * 
     */
    @Override
    public void clearAllSchedulingData() throws JobPersistenceException {
        executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {
                        clearAllSchedulingData(context);
                        return null;
                    }
                });
    }

    protected void clearAllSchedulingData(final ServerContext context) throws JobPersistenceException {
        try {
            clearData(context);
        } catch (ResourceException e) {
            throw new JobPersistenceException("Error clearing scheduling data: " + e.getMessage(), e);
        }
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
    @Override
    public void storeCalendar(final String name,final  Calendar calendar,final  boolean replaceExisting,
           final boolean updateTriggers) throws ObjectAlreadyExistsException, JobPersistenceException {

        executeInLock(
                (isLockOnInsert() || updateTriggers) ? LOCK_TRIGGER_ACCESS : null,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {
                        storeCalendar(context, name, calendar, replaceExisting, updateTriggers);
                        return null;
                    }
                });
    }

    protected void storeCalendar(final ServerContext context,
                                 String calName, Calendar calendar, boolean replaceExisting, boolean updateTriggers)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        try {
            boolean existingCal = calendarExists(context, calName);
            if (existingCal && !replaceExisting) {
                throw new ObjectAlreadyExistsException(
                        "Calendar with name '" + calName + "' already exists.");
            }

            if (existingCal) {
                if (updateCalendar(context, calName, calendar) < 1) {
                    throw new JobPersistenceException(
                            "Couldn't store calendar.  Update failed.");
                }

                if(updateTriggers) {
                    List<OperableTrigger> trigs = selectTriggersForCalendar(context, calName);

                    for(OperableTrigger trigger: trigs) {
                        trigger.updateWithNewCalendar(calendar, getMisfireThreshold());
                        storeTrigger(context, trigger, null, true, STATE_WAITING, false, false);
                    }
                }
            } else {
                if (insertCalendar(context, calName, calendar) < 1) {
                    throw new JobPersistenceException(
                            "Couldn't store calendar.  Insert failed.");
                }
            }

            if (isClustered == false) {
                calendarCache.put(calName, calendar); // lazy-cache
            }

        } catch (IOException e) {
            throw new JobPersistenceException(
                    "Couldn't store calendar because the BLOB couldn't be serialized: "
                            + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new JobPersistenceException("Couldn't store calendar: "
                    + e.getMessage(), e);
        }catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't store calendar: "
                    + e.getMessage(), e);
        }
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
    @Override
    public boolean removeCalendar(String calName) throws JobPersistenceException {
        return false;
    }

    /**
     * Retrieve the given <code>{@link org.quartz.Trigger}</code>.
     * 
     * @param calName
     *            The name of the <code>Calendar</code> to be retrieved.
     * @return The desired <code>Calendar</code>, or null if there is no match.
     */
    @Override
    public Calendar retrieveCalendar(String calName) throws JobPersistenceException {
        return retrieveCalendar(null, calName);
    }

    protected Calendar retrieveCalendar(final ServerContext context,
                                        String calName)
            throws JobPersistenceException {
        // all calendars are persistent, but we can lazy-cache them during run
        // time as long as we aren't running clustered.
        Calendar cal = (isClustered) ? null : calendarCache.get(calName);
        if (cal != null) {
            return cal;
        }

        try {
            cal = selectCalendar(context, calName);
            if (isClustered == false) {
                calendarCache.put(calName, cal); // lazy-cache...
            }
            return cal;
        } catch (ClassNotFoundException e) {
            throw new JobPersistenceException(
                    "Couldn't retrieve calendar because a required class was not found: "
                            + e.getMessage(), e);
        } catch (IOException e) {
            throw new JobPersistenceException(
                    "Couldn't retrieve calendar because the BLOB couldn't be deserialized: "
                            + e.getMessage(), e);
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't retrieve calendar: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Get the number of <code>{@link org.quartz.Job}</code> s that are stored
     * in the <code>JobsStore</code>.
     */
    @Override
    public int getNumberOfJobs() throws JobPersistenceException {
        return 0;
    }

    /**
     * Get the number of <code>{@link org.quartz.Trigger}</code> s that are
     * stored in the <code>JobsStore</code>.
     */
    @Override
    public int getNumberOfTriggers() throws JobPersistenceException {
        return 0;
    }

    /**
     * Get the number of <code>{@link org.quartz.Calendar}</code> s that are
     * stored in the <code>JobsStore</code>.
     */
    @Override
    public int getNumberOfCalendars() throws JobPersistenceException {
        return 0;
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
    @Override
    public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws JobPersistenceException {
        try {
            return selectJobsInGroup(getServerContext(), matcher);
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't obtain job names: "
                    + e.getMessage(), e);
        }
    }

    protected Set<JobKey> getJobNames(ServerContext context,
                                      GroupMatcher<JobKey> matcher) throws JobPersistenceException {
        Set<JobKey> jobNames;

        try {
            jobNames = selectJobsInGroup(context, matcher);
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't obtain job names: "
                    + e.getMessage(), e);
        }

        return jobNames;
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
    @Override
    public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher)
            throws JobPersistenceException {
        try {
            return  selectTriggersInGroup(getServerContext(), matcher);
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't obtain trigger names: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Get the names of all of the <code>{@link org.quartz.Job}</code> groups.
     * <p/>
     * <p>
     * If there are no known group names, the result should be a zero-length
     * array (not <code>null</code>).
     * </p>
     */
    @Override
    public List<String> getJobGroupNames() throws JobPersistenceException {
        try {
            return  selectJobGroups(getServerContext());
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't obtain job groups: "
                    + e.getMessage(), e);
        }
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
    @Override
    public List<String> getTriggerGroupNames() throws JobPersistenceException {
        try {
            return selectTriggerGroups(getServerContext());
        } catch (ResourceException e) {
            throw new JobPersistenceException(
                    "Couldn't obtain trigger groups: " + e.getMessage(), e);
        }
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
    @Override
    public List<String> getCalendarNames() throws JobPersistenceException {
        try {
            return selectCalendars(getServerContext());
        } catch (ResourceException e) {
            throw new JobPersistenceException(
                    "Couldn't obtain trigger groups: " + e.getMessage(), e);
        }
    }

    /**
     * Get all of the Triggers that are associated to the given Job.
     * <p/>
     * <p>
     * If there are no matches, a zero-length array should be returned.
     * </p>
     */
    @Override
    public List<OperableTrigger> getTriggersForJob(JobKey jobKey) throws JobPersistenceException {
        List<OperableTrigger> list = null;

        try {
            list = selectTriggersForJob(getServerContext(), jobKey);
        } catch (Exception e) {
            throw new JobPersistenceException(
                    "Couldn't obtain triggers for job: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Get the current state of the identified
     * <code>{@link org.quartz.Trigger}</code>.
     * 
     * @see org.quartz.Trigger.TriggerState
     */
    @Override
    public Trigger.TriggerState getTriggerState(TriggerKey triggerKey)
            throws JobPersistenceException {
        try {
            String ts = selectTriggerState(getServerContext(), triggerKey);

            if (ts == null) {
                return Trigger.TriggerState.NONE;
            }

            if (ts.equals(STATE_DELETED)) {
                return Trigger.TriggerState.NONE;
            }

            if (ts.equals(STATE_COMPLETE)) {
                return Trigger.TriggerState.COMPLETE;
            }

            if (ts.equals(STATE_PAUSED)) {
                return Trigger.TriggerState.PAUSED;
            }

            if (ts.equals(STATE_PAUSED_BLOCKED)) {
                return Trigger.TriggerState.PAUSED;
            }

            if (ts.equals(STATE_ERROR)) {
                return Trigger.TriggerState.ERROR;
            }

            if (ts.equals(STATE_BLOCKED)) {
                return Trigger.TriggerState.BLOCKED;
            }

            return Trigger.TriggerState.NORMAL;

        } catch (ResourceException e) {
            throw new JobPersistenceException(
                    "Couldn't determine state of trigger (" + triggerKey + "): " + e.getMessage(), e);
        }
    }

    /**
     * Pause the <code>{@link org.quartz.Trigger}</code> with the given key.
     * 
     * @see #resumeTrigger(org.quartz.TriggerKey)
     */
    @Override
    public void pauseTrigger(final TriggerKey triggerKey) throws JobPersistenceException {
        executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {
                        pauseTrigger(context, triggerKey);
                        return null;
                    }
                });
    }

    /**
     * <p>
     * Pause the <code>{@link org.quartz.Trigger}</code> with the given name.
     * </p>
     *
     * @see #resumeTrigger(ServerContext, SchedulingContext, String, String)
     */
    public void pauseTrigger(final ServerContext context,
                             TriggerKey triggerKey)
            throws JobPersistenceException {

        try {
            String oldState = selectTriggerState(context,
                    triggerKey);

            if (oldState.equals(STATE_WAITING)
                    || oldState.equals(STATE_ACQUIRED)) {

                updateTriggerState(context, triggerKey,
                        STATE_PAUSED);
            } else if (oldState.equals(STATE_BLOCKED)) {
                updateTriggerState(context, triggerKey,
                        STATE_PAUSED_BLOCKED);
            }
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't pause trigger '"
                    + triggerKey + "': " + e.getMessage(), e);
        }
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
    @Override
    public Collection<String> pauseTriggers(GroupMatcher<TriggerKey> matcher)
            throws JobPersistenceException {
        return (Set<String>) executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback() {
                    public Set<String> execute(final ServerContext context) throws JobPersistenceException {
                        return pauseTriggerGroup(context, matcher);
                    }
                });
    }

    /**
     * <p>
     * Pause all of the <code>{@link org.quartz.Trigger}s</code> matching the
     * given groupMatcher.
     * </p>
     *
     * @see #resumeTriggerGroup(Connection, SchedulingContext, String)
     */
    public Set<String> pauseTriggerGroup(final ServerContext context,
                                         GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {

        try {

            updateTriggerGroupStateFromOtherStates(
                    context, matcher, STATE_PAUSED, STATE_ACQUIRED,
                    STATE_WAITING, STATE_WAITING);

            updateTriggerGroupStateFromOtherState(
                    context, matcher, STATE_PAUSED_BLOCKED, STATE_BLOCKED);

            List<String> groups = selectTriggerGroups(context, matcher);

            // make sure to account for an exact group match for a group that doesn't yet exist
            StringMatcher.StringOperatorName operator = matcher.getCompareWithOperator();
            if (operator.equals(StringMatcher.StringOperatorName.EQUALS) && !groups.contains(matcher.getCompareToValue())) {
                groups.add(matcher.getCompareToValue());
            }

            for (String group : groups) {
                if (!isTriggerGroupPaused(context, group)) {
                    insertPausedTriggerGroup(context, group);
                }
            }

            return new HashSet<String>(groups);

        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't pause trigger group '"
                    + matcher + "': " + e.getMessage(), e);
        }
    }

    /**
     * Pause the <code>{@link org.quartz.Job}</code> with the given name - by
     * pausing all of its current <code>Trigger</code>s.
     * 
     * @see #resumeJob(org.quartz.JobKey)
     */
    @Override
    public void pauseJob(final JobKey jobKey) throws JobPersistenceException {
        executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {
                        List<OperableTrigger> triggers = getTriggersForJob(context, jobKey);
                        for (OperableTrigger trigger: triggers) {
                            pauseTrigger(context, trigger.getKey());
                        }
                        return null;
                    }
                });
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
    @Override
    public Collection<String> pauseJobs(final GroupMatcher<JobKey> groupMatcher)
            throws JobPersistenceException {
        return (Set<String>) executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback() {
                    public Set<String> execute(final ServerContext context) throws JobPersistenceException {
                        Set<String> groupNames = new HashSet<String>();
                        Set<JobKey> jobNames = getJobNames(context, groupMatcher);

                        for (JobKey jobKey : jobNames) {
                            List<OperableTrigger> triggers = getTriggersForJob(context, jobKey);
                            for (OperableTrigger trigger : triggers) {
                                pauseTrigger(context, trigger.getKey());
                            }
                            groupNames.add(jobKey.getGroup());
                        }

                        return groupNames;
                    }
                }
        );
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
    @Override
    public void resumeTrigger(final TriggerKey triggerKey) throws JobPersistenceException {

        executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {
                        resumeTrigger(context, triggerKey);
                        return null;
                    }
                });
    }

    /**
     * <p>
     * Resume (un-pause) the <code>{@link org.quartz.Trigger}</code> with the
     * given name.
     * </p>
     *
     * <p>
     * If the <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     *
     * @see #pauseTrigger(Connection, SchedulingContext, String, String)
     */
    public void resumeTrigger(final ServerContext context,
                              TriggerKey key)
            throws JobPersistenceException {
        try {

            TriggerStatus status = selectTriggerStatus(context,
                    key);

            if (status == null || status.getNextFireTime() == null) {
                return;
            }

            boolean blocked = false;
            if(STATE_PAUSED_BLOCKED.equals(status.getStatus())) {
                blocked = true;
            }

            String newState = checkBlockedState(context, status.getJobKey(), STATE_WAITING);

            boolean misfired = false;

            if (schedulerRunning && status.getNextFireTime().before(new Date())) {
                misfired = updateMisfiredTrigger(context, key,
                        newState, true);
            }

            if(!misfired) {
                if(blocked) {
                    updateTriggerStateFromOtherState(context,
                            key, newState, STATE_PAUSED_BLOCKED);
                } else {
                    updateTriggerStateFromOtherState(context,
                            key, newState, STATE_PAUSED);
                }
            }

        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't resume trigger '"
                    + key + "': " + e.getMessage(), e);
        }
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
    @Override
    public Collection<String> resumeTriggers(final GroupMatcher<TriggerKey> matcher)
            throws JobPersistenceException {
        return executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback<Set<String>>() {
                    public Set<String> execute(final ServerContext context) throws JobPersistenceException {
                        return resumeTriggerGroup(context, matcher);
                    }
                });

    }

    /**
     * <p>
     * Resume (un-pause) all of the <code>{@link org.quartz.Trigger}s</code>
     * matching the given groupMatcher.
     * </p>
     *
     * <p>
     * If any <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     *
     * @see #pauseTriggers(org.quartz.impl.matchers.GroupMatcher)
     */
    public Set<String> resumeTriggerGroup(final ServerContext context,
                                          GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {

        try {

            deletePausedTriggerGroup(context, matcher);
            HashSet<String> groups = new HashSet<String>();

            Set<TriggerKey> keys = selectTriggersInGroup(context,
                    matcher);

            for (TriggerKey key: keys) {
                resumeTrigger(context, key);
                groups.add(key.getGroup());
            }

            return groups;

            // TODO: find an efficient way to resume triggers (better than the
            // above)... logic below is broken because of
            // findTriggersToBeBlocked()
            /*
             * int res =
             * updateTriggerGroupStateFromOtherState(context,
             * groupName, STATE_WAITING, PAUSED);
             *
             * if(res > 0) {
             *
             * long misfireTime = System.currentTimeMillis();
             * if(getMisfireThreshold() > 0) misfireTime -=
             * getMisfireThreshold();
             *
             * Key[] misfires =
             * selectMisfiredTriggersInGroupInState(context,
             * groupName, STATE_WAITING, misfireTime);
             *
             * List blockedTriggers = findTriggersToBeBlocked(context,
             * groupName);
             *
             * Iterator itr = blockedTriggers.iterator(); while(itr.hasNext()) {
             * Key key = (Key)itr.next();
             * updateTriggerState(context, key.getName(),
             * key.getGroup(), BLOCKED); }
             *
             * for(int i=0; i < misfires.length; i++) {               String
             * newState = STATE_WAITING;
             * if(blockedTriggers.contains(misfires[i])) newState =
             * BLOCKED; updateMisfiredTrigger(context,
             * misfires[i].getName(), misfires[i].getGroup(), newState, true); } }
             */

        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't pause trigger group '"
                    + matcher + "': " + e.getMessage(), e);
        }
    }

    @Override
    public Set<String> getPausedTriggerGroups() throws JobPersistenceException {
        try {
            return selectPausedTriggerGroups(getServerContext());
        } catch (ResourceException e) {
            throw new JobPersistenceException(
                    "Couldn't determine paused trigger groups: " + e.getMessage(), e);
        }
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
    @Override
    public void resumeJob(JobKey jobKey) throws JobPersistenceException {
        executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {
                        List<OperableTrigger> triggers = getTriggersForJob(context, jobKey);
                        for (OperableTrigger trigger: triggers) {
                            resumeTrigger(context, trigger.getKey());
                        }
                        return null;
                    }
                });
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
    @Override
    public Collection<String> resumeJobs(final GroupMatcher<JobKey> matcher)
            throws JobPersistenceException {
        return (Set<String>) executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback() {
                    public Set<String> execute(final ServerContext context) throws JobPersistenceException {
                        Set<JobKey> jobKeys = getJobNames(context, matcher);
                        Set<String> groupNames = new HashSet<String>();

                        for (JobKey jobKey: jobKeys) {
                            List<OperableTrigger> triggers = getTriggersForJob(context, jobKey);
                            for (OperableTrigger trigger: triggers) {
                                resumeTrigger(context, trigger.getKey());
                            }
                            groupNames.add(jobKey.getGroup());
                        }
                        return groupNames;
                    }
                });
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
    @Override
    public void pauseAll() throws JobPersistenceException {

        executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {
                        pauseAll(context);
                        return null;
                    }
                });
    }

    /**
     * <p>
     * Pause all triggers - equivalent of calling <code>pauseTriggerGroup(group)</code>
     * on every group.
     * </p>
     *
     * <p>
     * When <code>resumeAll()</code> is called (to un-pause), trigger misfire
     * instructions WILL be applied.
     * </p>
     *
     * @see #resumeAll(SchedulingContext)
     * @see #pauseTriggerGroup(SchedulingContext, String)
     */
    public void pauseAll(final ServerContext context)
            throws JobPersistenceException {

        List<String> names = getTriggerGroupNames(context);

        for (String name: names) {
            pauseTriggerGroup(context, GroupMatcher.triggerGroupEquals(name));
        }

        try {
            if (!isTriggerGroupPaused(context, ALL_GROUPS_PAUSED)) {
                insertPausedTriggerGroup(context, ALL_GROUPS_PAUSED);
            }

        } catch (ResourceException e) {
            throw new JobPersistenceException(
                    "Couldn't pause all trigger groups: " + e.getMessage(), e);
        }

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
    @Override
    public void resumeAll() throws JobPersistenceException {

        executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {
                        resumeAll(context);
                        return null;
                    }
                });
    }

    /**
     * protected
     * <p>
     * Resume (un-pause) all triggers - equivalent of calling <code>resumeTriggerGroup(group)</code>
     * on every group.
     * </p>
     *
     * <p>
     * If any <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     *
     * @see #pauseAll(SchedulingContext)
     */
    public void resumeAll(final ServerContext context)
            throws JobPersistenceException {

        List<String> names = getTriggerGroupNames(context);

        for (String name: names) {
            resumeTriggerGroup(context, GroupMatcher.triggerGroupEquals(name));
        }

        try {
            deletePausedTriggerGroup(context, ALL_GROUPS_PAUSED);
        } catch (ResourceException e) {
            throw new JobPersistenceException(
                    "Couldn't resume all trigger groups: " + e.getMessage(), e);
        }
    }

    private static long ftrCtr = System.currentTimeMillis();

    protected synchronized String getFiredTriggerRecordId() {
        return getInstanceId() + ftrCtr++;
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
    @Override
    public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow)
            throws JobPersistenceException {
        if(maxCount > 1) {
            return (List<OperableTrigger>)executeInNonManagedTXLock(
                    LOCK_TRIGGER_ACCESS,
                    new TransactionCallback() {
                        public Object execute(final ServerContext context) throws JobPersistenceException {
                            return acquireNextTrigger(context, noLaterThan, maxCount, timeWindow);
                        }
                    });
        }
        else {
            return (List<OperableTrigger>)executeInNonManagedTXLock(
                    null, /* passing null as lock name causes no lock to be made */
                    new TransactionCallback() {
                        public Object execute(final ServerContext context) throws JobPersistenceException {
                            return acquireNextTrigger(context, noLaterThan, maxCount, timeWindow);
                        }
                    });
        }
    }

    // TODO: this really ought to return something like a FiredTriggerBundle,
    // so that the fireInstanceId doesn't have to be on the trigger...
    protected List<OperableTrigger> acquireNextTrigger(final ServerContext context, long noLaterThan, int maxCount, long timeWindow)
            throws JobPersistenceException {
        if (timeWindow < 0) {
            throw new IllegalArgumentException();
        }
        List<OperableTrigger> acquiredTriggers = new ArrayList<OperableTrigger>();
        Set<JobKey> acquiredJobKeysForNoConcurrentExec = new HashSet<JobKey>();
        final int MAX_DO_LOOP_RETRY = 3;
        int currentLoopCount = 0;
        long firstAcquiredTriggerFireTime = 0;

        do {
            currentLoopCount ++;
            try {
                List<TriggerKey> keys = selectTriggerToAcquire(context, noLaterThan + timeWindow, getMisfireTime(), maxCount);

                // No trigger is ready to fire yet.
                if (keys == null || keys.size() == 0)
                    return acquiredTriggers;

                for(TriggerKey triggerKey: keys) {
                    // If our trigger is no longer available, try a new one.
                    OperableTrigger nextTrigger = retrieveTrigger(context, triggerKey);
                    if(nextTrigger == null) {
                        continue; // next trigger
                    }

                    // If trigger's job is set as @DisallowConcurrentExecution, and it has already been added to result, then
                    // put it back into the timeTriggers set and continue to search for next trigger.
                    JobKey jobKey = nextTrigger.getJobKey();
                    JobDetail job = selectJobDetail(context, jobKey, getClassLoadHelper());
                    if (job.isConcurrentExectionDisallowed()) {
                        if (acquiredJobKeysForNoConcurrentExec.contains(jobKey)) {
                            continue; // next trigger
                        } else {
                            acquiredJobKeysForNoConcurrentExec.add(jobKey);
                        }
                    }

                    // We now have a acquired trigger, let's add to return list.
                    // If our trigger was no longer in the expected state, try a new one.
                    int rowsUpdated = updateTriggerStateFromOtherState(context, triggerKey, STATE_ACQUIRED, STATE_WAITING);
                    if (rowsUpdated <= 0) {
                        // TODO: Hum... shouldn't we log a warning here?
                        continue; // next trigger
                    }
                    nextTrigger.setFireInstanceId(getFiredTriggerRecordId());
                    insertFiredTrigger(context, nextTrigger, STATE_ACQUIRED, null);

                    acquiredTriggers.add(nextTrigger);
                    if(firstAcquiredTriggerFireTime == 0)
                        firstAcquiredTriggerFireTime = nextTrigger.getNextFireTime().getTime();
                }

                // if we didn't end up with any trigger to fire from that first
                // batch, try again for another batch. We allow with a max retry count.
                if(acquiredTriggers.size() == 0 && currentLoopCount < MAX_DO_LOOP_RETRY) {
                    continue;
                }

                // We are done with the while loop.
                break;
            } catch (Exception e) {
                throw new JobPersistenceException(
                        "Couldn't acquire next trigger: " + e.getMessage(), e);
            }
        } while (true);

        // Return the acquired trigger list
        return acquiredTriggers;
    }

    /**
     * Inform the <code>JobStore</code> that the scheduler no longer plans to
     * fire the given <code>Trigger</code>, that it had previously acquired
     * (reserved).
     */
    @Override
    public void releaseAcquiredTrigger(final OperableTrigger trigger) throws JobPersistenceException {
        executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {
                        releaseAcquiredTrigger(context, trigger);
                        return null;
                    }
                });
    }

    protected void releaseAcquiredTrigger(final ServerContext context,
                                          OperableTrigger trigger)
            throws JobPersistenceException {
        try {
            updateTriggerStateFromOtherState(context,
                    trigger.getKey(), STATE_WAITING, STATE_ACQUIRED);
            deleteFiredTrigger(context, trigger.getFireInstanceId());
        } catch (ResourceException e) {
            throw new JobPersistenceException(
                    "Couldn't release acquired trigger: " + e.getMessage(), e);
        }
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
    @Override
    public List<TriggerFiredResult> triggersFired(final List<OperableTrigger> triggers)
            throws JobPersistenceException {
        return executeInLock(
                        LOCK_TRIGGER_ACCESS,
                        new TransactionCallback<List<TriggerFiredResult>>() {
                            public List<TriggerFiredResult> execute(final ServerContext context) throws JobPersistenceException {
                                List<TriggerFiredResult> results = new ArrayList<TriggerFiredResult>();

                                TriggerFiredResult result;
                                for (OperableTrigger trigger : triggers) {
                                    try {
                                        TriggerFiredBundle bundle = triggerFired(context, trigger);
                                        result = new TriggerFiredResult(bundle);
                                    } catch (JobPersistenceException jpe) {
                                        result = new TriggerFiredResult(jpe);
                                    } catch(RuntimeException re) {
                                        result = new TriggerFiredResult(re);
                                    }
                                    results.add(result);
                                }

                                return results;
                            }
                        });
    }

    protected TriggerFiredBundle triggerFired(final ServerContext context,
                                              OperableTrigger trigger)
            throws JobPersistenceException {
        JobDetail job = null;
        Calendar cal = null;

        // Make sure trigger wasn't deleted, paused, or completed...
        try { // if trigger was deleted, state will be STATE_DELETED
            String state = selectTriggerState(context,
                    trigger.getKey());
            if (!state.equals(STATE_ACQUIRED)) {
                return null;
            }
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't select trigger state: "
                    + e.getMessage(), e);
        }

        try {
            job = retrieveJob(context, trigger.getJobKey());
            if (job == null) { return null; }
        } catch (JobPersistenceException jpe) {
            try {
                logger.error("Error retrieving job, setting trigger state to ERROR.", jpe);
                updateTriggerState(context, trigger.getKey(),
                        STATE_ERROR);
            } catch (ResourceException sqle) {
                logger.error("Unable to set trigger state to ERROR.", sqle);
            }
            throw jpe;
        }

        if (trigger.getCalendarName() != null) {
            cal = retrieveCalendar(context, trigger.getCalendarName());
            if (cal == null) { return null; }
        }

        try {
            updateFiredTrigger(context, trigger, STATE_EXECUTING, job);
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't insert fired trigger: "
                    + e.getMessage(), e);
        }

        Date prevFireTime = trigger.getPreviousFireTime();

        // call triggered - to update the trigger's next-fire-time state...
        trigger.triggered(cal);

        String state = STATE_WAITING;
        boolean force = true;

        if (job.isConcurrentExectionDisallowed()) {
            state = STATE_BLOCKED;
            force = false;
            try {
                updateTriggerStatesForJobFromOtherState(context, job.getKey(),
                        STATE_BLOCKED, STATE_WAITING);
                updateTriggerStatesForJobFromOtherState(context, job.getKey(),
                        STATE_BLOCKED, STATE_ACQUIRED);
                updateTriggerStatesForJobFromOtherState(context, job.getKey(),
                        STATE_PAUSED_BLOCKED, STATE_PAUSED);
            } catch (ResourceException e) {
                throw new JobPersistenceException(
                        "Couldn't update states of blocked triggers: "
                                + e.getMessage(), e);
            }
        }

        if (trigger.getNextFireTime() == null) {
            state = STATE_COMPLETE;
            force = true;
        }

        storeTrigger(context, trigger, job, true, state, force, false);

        job.getJobDataMap().clearDirtyFlag();

        return new TriggerFiredBundle(job, trigger, cal, trigger.getKey().getGroup()
                .equals(Scheduler.DEFAULT_RECOVERY_GROUP), new Date(), trigger
                .getPreviousFireTime(), prevFireTime, trigger.getNextFireTime());
    }

    /**
     * Inform the <code>JobStore</code> that the scheduler has completed the
     * firing of the given <code>Trigger</code> (and the execution of its
     * associated <code>Job</code> completed, threw an exception, or was
     * vetoed), and that the <code>{@link org.quartz.JobDataMap}</code> in the
     * given <code>JobDetail</code> should be updated if the <code>Job</code> is
     * stateful.
     */
    @Override
    public void triggeredJobComplete(final OperableTrigger trigger,final  JobDetail jobDetail,
            final Trigger.CompletedExecutionInstruction triggerInstCode) throws JobPersistenceException {

        executeInLock(
                LOCK_TRIGGER_ACCESS,
                new TransactionCallback<Void>() {
                    public Void execute(final ServerContext context) throws JobPersistenceException {
                        triggeredJobComplete(context, trigger, jobDetail,triggerInstCode);
                        return null;
                    }
                });
    }

    protected void triggeredJobComplete(final ServerContext context,
                                        OperableTrigger trigger, JobDetail jobDetail,
                                        Trigger.CompletedExecutionInstruction triggerInstCode) throws JobPersistenceException {
        try {
            if (triggerInstCode == Trigger.CompletedExecutionInstruction.DELETE_TRIGGER) {
                if(trigger.getNextFireTime() == null) {
                    // double check for possible reschedule within job
                    // execution, which would cancel the need to delete...
                    TriggerStatus stat = selectTriggerStatus(
                            context, trigger.getKey());
                    if(stat != null && stat.getNextFireTime() == null) {
                        removeTrigger(context, trigger.getKey());
                    }
                } else{
                    removeTrigger(context, trigger.getKey());
                    signalSchedulingChangeOnTxCompletion(0L);
                }
            } else if (triggerInstCode == Trigger.CompletedExecutionInstruction.SET_TRIGGER_COMPLETE) {
                updateTriggerState(context, trigger.getKey(),
                        STATE_COMPLETE);
                signalSchedulingChangeOnTxCompletion(0L);
            } else if (triggerInstCode == Trigger.CompletedExecutionInstruction.SET_TRIGGER_ERROR) {
                logger.info("Trigger " + trigger.getKey() + " set to ERROR state.");
                updateTriggerState(context, trigger.getKey(),
                        STATE_ERROR);
                signalSchedulingChangeOnTxCompletion(0L);
            } else if (triggerInstCode == Trigger.CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE) {
                updateTriggerStatesForJob(context,
                        trigger.getJobKey(), STATE_COMPLETE);
                signalSchedulingChangeOnTxCompletion(0L);
            } else if (triggerInstCode == Trigger.CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR) {
                logger.info("All triggers of Job " +
                        trigger.getKey() + " set to ERROR state.");
                updateTriggerStatesForJob(context,
                        trigger.getJobKey(), STATE_ERROR);
                signalSchedulingChangeOnTxCompletion(0L);
            }

            if (jobDetail.isConcurrentExectionDisallowed()) {
                updateTriggerStatesForJobFromOtherState(context,
                        jobDetail.getKey(), STATE_WAITING,
                        STATE_BLOCKED);

                updateTriggerStatesForJobFromOtherState(context,
                        jobDetail.getKey(), STATE_PAUSED,
                        STATE_PAUSED_BLOCKED);

                signalSchedulingChangeOnTxCompletion(0L);
            }
            if (jobDetail.isPersistJobDataAfterExecution()) {
                try {
                    if (jobDetail.getJobDataMap().isDirty()) {
                        updateJobData(context, jobDetail);
                    }
                } catch (IOException e) {
                    throw new JobPersistenceException(
                            "Couldn't serialize job data: " + e.getMessage(), e);
                } catch (ResourceException e) {
                    throw new JobPersistenceException(
                            "Couldn't update job data: " + e.getMessage(), e);
                }
            }
        } catch (ResourceException e) {
            throw new JobPersistenceException(
                    "Couldn't update trigger state(s): " + e.getMessage(), e);
        }

        try {
            deleteFiredTrigger(context, trigger.getFireInstanceId());
        } catch (ResourceException e) {
            throw new JobPersistenceException("Couldn't delete fired trigger: "
                    + e.getMessage(), e);
        }
    }

    ///////////


    /**
     * <p>
     * Update all triggers having one of the two given states, to the given new
     * state.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param newState
     *          the new state for the triggers
     * @param oldState1
     *          the first old state to update
     * @param oldState2
     *          the second old state to update
     * @return number of rows updated
     */
    int updateTriggerStatesFromOtherStates(final ServerContext context,
                                           String newState, String oldState1, String oldState2)
            throws ResourceException {return  0;};

    /**
     * <p>
     * Get the names of all of the triggers that have misfired - according to
     * the given timestamp.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return an array of <code>{@link
     * org.quartz.utils.Key}</code> objects
     */
    List<TriggerKey> selectMisfiredTriggers(final ServerContext context, long ts)
            throws ResourceException {return null;};

    /**
     * <p>
     * Get the names of all of the triggers in the given state that have
     * misfired - according to the given timestamp.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return an array of <code>{@link
     * org.quartz.utils.Key}</code> objects
     */
    List<TriggerKey> selectMisfiredTriggersInState(final ServerContext context, String state,
                                                   long ts) throws ResourceException {return null;};

    /**
     * <p>
     * Get the names of all of the triggers in the given states that have
     * misfired - according to the given timestamp.  No more than count will
     * be returned.
     * </p>
     *
     * @param context the DB Connection
     * @param count the most misfired triggers to return, negative for all
     * @param resultList Output parameter.  A List of 
     *      <code>{@link org.quartz.utils.Key}</code> objects.  Must not be null.
     *
     * @return Whether there are more misfired triggers left to find beyond
     *         the given count.
     */
    boolean hasMisfiredTriggersInState(final ServerContext context, String state1,
                                       long ts, int count, List<TriggerKey> resultList) throws ResourceException {
        return false;
    };

    /**
     * <p>
     * Get the number of triggers in the given state that have
     * misfired - according to the given timestamp.
     * </p>
     *
     * @param context the DB Connection
     */
    int countMisfiredTriggersInState(
            final ServerContext context, String state1, long ts) throws ResourceException { return  0;};

    /**
     * <p>
     * Get the names of all of the triggers in the given group and state that
     * have misfired - according to the given timestamp.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return an array of <code>{@link
     * org.quartz.utils.Key}</code> objects
     */
    List<TriggerKey> selectMisfiredTriggersInGroupInState(final ServerContext context,
                                                          String groupName, String state, long ts) throws ResourceException {return null;};


    /**
     * <p>
     * Select all of the triggers for jobs that are requesting recovery. The
     * returned trigger objects will have unique "recoverXXX" trigger names and
     * will be in the <code>{@link
     * org.quartz.Scheduler}.DEFAULT_RECOVERY_GROUP</code>
     * trigger group.
     * </p>
     *
     * <p>
     * In order to preserve the ordering of the triggers, the fire time will be
     * set from the <code>COL_FIRED_TIME</code> column in the <code>TABLE_FIRED_TRIGGERS</code>
     * table. The caller is responsible for calling <code>computeFirstFireTime</code>
     * on each returned trigger. It is also up to the caller to insert the
     * returned triggers to ensure that they are fired.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return an array of <code>{@link org.quartz.Trigger}</code> objects
     */
    List<OperableTrigger> selectTriggersForRecoveringJobs(final ServerContext context)
            throws ResourceException, IOException, ClassNotFoundException {return null;};

    /**
     * <p>
     * Delete all fired triggers.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return the number of rows deleted
     */
    int deleteFiredTriggers(final ServerContext context) throws ResourceException {return 0;};

    /**
     * <p>
     * Delete all fired triggers of the given instance.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return the number of rows deleted
     */
    int deleteFiredTriggers(final ServerContext context, String instanceId)
            throws ResourceException {return 0;};

    //---------------------------------------------------------------------------
    // jobs
    //---------------------------------------------------------------------------

    /**
     * <p>
     * Insert the job detail record.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param job
     *          the job to insert
     * @return number of rows inserted
     * @throws IOException
     *           if there were problems serializing the JobDataMap
     */
    int insertJobDetail(final ServerContext context, JobDetail job)
            throws IOException, ResourceException {return 0;};

    /**
     * <p>
     * Update the job detail record.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param job
     *          the job to update
     * @return number of rows updated
     * @throws IOException
     *           if there were problems serializing the JobDataMap
     */
    int updateJobDetail(final ServerContext context, JobDetail job)
            throws IOException, ResourceException {return 0;};

    /**
     * <p>
     * Get the names of all of the triggers associated with the given job.
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @return an array of <code>{@link
     * org.quartz.utils.Key}</code> objects
     */
    List<TriggerKey> selectTriggerKeysForJob(final ServerContext context, JobKey jobKey) throws ResourceException {return null;};


    /**
     * Delete a job and its listeners.
     *
     * @see #removeJob(ServerContext, SchedulingContext, String, String, boolean)
     * @see #removeTrigger(ServerContext, SchedulingContext, String, String)
     */
    private boolean deleteJobAndChildren(final ServerContext context, JobKey key)
            throws ResourceException {
        return (deleteJobDetail(context, key) > 0);
    }

    /**
     * <p>
     * Delete the job detail record for the given job.
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @return the number of rows deleted
     */
    int deleteJobDetail(final ServerContext context, JobKey jobKey)
            throws ResourceException {return 0;};

    /**
     * <p>
     * Check whether or not the given job disallows concurrent execution.
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @return true if the job exists and disallows concurrent execution, false otherwise
     */
    boolean isJobNonConcurrent(final ServerContext context, JobKey jobKey) throws ResourceException {return true;};

//    /**
//     * <p>
//     * Check whether or not the given job exists.
//     * </p>
//     *
//     * @param context
//     *          the DB Connection
//     *
//     * @return true if the job exists, false otherwise
//     */
//    boolean jobExists(final ServerContext context, JobKey jobKey)
//            throws ResourceException  {return false;};

    /**
     * <p>
     * Update the job data map for the given job.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param job
     *          the job to update
     * @return the number of rows updated
     * @throws IOException
     *           if there were problems serializing the JobDataMap
     */
    int updateJobData(final ServerContext context, JobDetail job)
            throws IOException, ResourceException {return 0;};

    /**
     * <p>
     * Select the JobDetail object for a given job name / group name.
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @return the populated JobDetail object
     * @throws ClassNotFoundException
     *           if a class found during deserialization cannot be found or if
     *           the job class could not be found
     * @throws IOException
     *           if deserialization causes an error
     */
    JobDetail selectJobDetail(final ServerContext context, JobKey jobKey,
                              ClassLoadHelper loadHelper)
            throws ClassNotFoundException, IOException, ResourceException {return null;};

    /**
     * <p>
     * Select the total number of jobs stored.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return the total number of jobs stored
     */
    int selectNumJobs(final ServerContext context) throws ResourceException {return 0;};

    /**
     * <p>
     * Select all of the job group names that are stored.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return an array of <code>String</code> group names
     */
    List<String> selectJobGroups(final ServerContext context) throws ResourceException {return null;};

    /**
     * <p>
     * Select all of the jobs contained in a given group.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param matcher
     *          the group matcher to evaluate against the known jobs
     * @return an array of <code>String</code> job names
     */
    Set<JobKey> selectJobsInGroup(final ServerContext context, GroupMatcher<JobKey> matcher)
            throws ResourceException {return null;};

    //---------------------------------------------------------------------------
    // triggers
    //---------------------------------------------------------------------------

    /**
     * <p>
     * Insert the base trigger data.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param trigger
     *          the trigger to insert
     * @param state
     *          the state that the trigger should be stored in
     * @return the number of rows inserted
     */
    int insertTrigger(final ServerContext context, OperableTrigger trigger, String state,
                      JobDetail jobDetail) throws ResourceException, IOException {return 0;};

    /**
     * <p>
     * Update the base trigger data.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param trigger
     *          the trigger to insert
     * @param state
     *          the state that the trigger should be stored in
     * @return the number of rows updated
     */
    int updateTrigger(final ServerContext context, OperableTrigger trigger, String state,
                      JobDetail jobDetail) throws ResourceException, IOException {return 0;};

    /**
     * <p>
     * Check whether or not a trigger exists.
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @return the number of rows updated
     */
    boolean triggerExists(final ServerContext context, TriggerKey triggerKey) throws JobPersistenceException {return false;};

    /**
     * <p>
     * Update the state for a given trigger.
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @param state
     *          the new state for the trigger
     * @return the number of rows updated
     */
    int updateTriggerState(final ServerContext context, TriggerKey triggerKey,
                           String state) throws ResourceException {return 0;};

    /**
     * <p>
     * Update the given trigger to the given new state, if it is in the given
     * old state.
     * </p>
     *
     * @param context
     *          the DB contextection
     *
     * @param newState
     *          the new state for the trigger
     * @param oldState
     *          the old state the trigger must be in
     * @return int the number of rows updated
     * @throws ResourceException
     */
    int updateTriggerStateFromOtherState(final ServerContext context,
                                         TriggerKey triggerKey, String newState, String oldState) throws ResourceException {return 0;};

    /**
     * <p>
     * Update the given trigger to the given new state, if it is one of the
     * given old states.
     * </p>
     *
     * @param context
     *          the DB contextection
     *
     * @param newState
     *          the new state for the trigger
     * @param oldState1
     *          one of the old state the trigger must be in
     * @param oldState2
     *          one of the old state the trigger must be in
     * @param oldState3
     *          one of the old state the trigger must be in
     * @return int the number of rows updated
     * @throws ResourceException
     */
    int updateTriggerStateFromOtherStates(final ServerContext context,
                                          TriggerKey triggerKey, String newState, String oldState1,
                                          String oldState2, String oldState3)
            throws ResourceException {return 0;};

    /**
     * <p>
     * Update all triggers in the given group to the given new state, if they
     * are in one of the given old states.
     * </p>
     *
     * @param context
     *          the DB contextection
     * @param matcher
     *          the group matcher to evaluate against the known triggers
     * @param newState
     *          the new state for the trigger
     * @param oldState1
     *          one of the old state the trigger must be in
     * @param oldState2
     *          one of the old state the trigger must be in
     * @param oldState3
     *          one of the old state the trigger must be in
     * @return int the number of rows updated
     * @throws ResourceException
     */
    int updateTriggerGroupStateFromOtherStates(final ServerContext context,
                                               GroupMatcher<TriggerKey> matcher, String newState, String oldState1,
                                               String oldState2, String oldState3) throws ResourceException{return  0;};

    /**
     * <p>
     * Update all of the triggers of the given group to the given new state, if
     * they are in the given old state.
     * </p>
     *
     * @param context
     *          the DB contextection
     * @param matcher
     *          the matcher to evaluate against the known triggers
     * @param newState
     *          the new state for the trigger group
     * @param oldState
     *          the old state the triggers must be in
     * @return int the number of rows updated
     * @throws ResourceException
     */
    int updateTriggerGroupStateFromOtherState(final ServerContext context,
                                              GroupMatcher<TriggerKey> matcher, String newState, String oldState)
            throws ResourceException{return  0;};

    /**
     * <p>
     * Update the states of all triggers associated with the given job.
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @param state
     *          the new state for the triggers
     * @return the number of rows updated
     */
    int updateTriggerStatesForJob(final ServerContext context, JobKey jobKey,
                                  String state) throws ResourceException{return  0;};

    /**
     * <p>
     * Update the states of any triggers associated with the given job, that
     * are the given current state.
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @param state
     *          the new state for the triggers
     * @param oldState
     *          the old state of the triggers
     * @return the number of rows updated
     */
    int updateTriggerStatesForJobFromOtherState(final ServerContext context,
                                                JobKey jobKey, String state, String oldState)
            throws ResourceException{return  0;};


    /**
     * Delete a trigger, its listeners, and its Simple/Cron/BLOB sub-table entry.
     *
     * @see #removeJob(ServerContext, SchedulingContext, String, String, boolean)
     * @see #removeTrigger(ServerContext, SchedulingContext, String, String)
     * @see #replaceTrigger(ServerContext, SchedulingContext, String, String, Trigger)
     */
    private boolean deleteTriggerAndChildren(final ServerContext context, TriggerKey key)
            throws ResourceException {

        return (deleteTrigger(context, key) > 0);
    }

    /**
     * <p>
     * Delete the base trigger data for a trigger.
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @return the number of rows deleted
     */
    int deleteTrigger(final ServerContext context, TriggerKey triggerKey) throws ResourceException{return  0;};

    /**
     * <p>
     * Select the number of triggers associated with a given job.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return the number of triggers for the given job
     */
    int selectNumTriggersForJob(final ServerContext context, JobKey jobKey) throws ResourceException{return  0;};

    /**
     * <p>
     * Select the job to which the trigger is associated.
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @return the <code>{@link org.quartz.JobDetail}</code> object
     *         associated with the given trigger
     */
    JobDetail selectJobForTrigger(final ServerContext context, ClassLoadHelper loadHelper,
                                  TriggerKey triggerKey)
            throws ClassNotFoundException, ResourceException {return null;};

    /**
     * <p>
     * Select the triggers for a job
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @return an array of <code>(@link org.quartz.Trigger)</code> objects
     *         associated with a given job.
     * @throws ResourceException
     * @throws JobPersistenceException
     */
    List<OperableTrigger> selectTriggersForJob(final ServerContext context, JobKey jobKey) throws ResourceException, ClassNotFoundException,
            IOException, JobPersistenceException{return null;};

    /**
     * <p>
     * Select the triggers for a calendar
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param calName
     *          the name of the calendar
     * @return an array of <code>(@link org.quartz.Trigger)</code> objects
     *         associated with the given calendar.
     * @throws ResourceException
     * @throws JobPersistenceException
     */
    List<OperableTrigger> selectTriggersForCalendar(final ServerContext context, String calName)
            throws ResourceException, ClassNotFoundException, IOException, JobPersistenceException{return null;};
    /**
     * <p>
     * Select a trigger.
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @return the <code>{@link org.quartz.Trigger}</code> object
     * @throws JobPersistenceException
     */
    OperableTrigger selectTrigger(final ServerContext context, TriggerKey triggerKey) throws ResourceException, ClassNotFoundException,
            IOException, JobPersistenceException{return null;};

    /**
     * <p>
     * Select a trigger's JobDataMap.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return the <code>{@link org.quartz.JobDataMap}</code> of the Trigger,
     * never null, but possibly empty.
     */
    JobDataMap selectTriggerJobDataMap(final ServerContext context, String triggerName,
                                       String groupName) throws ResourceException, ClassNotFoundException,
            IOException{return  null;};

    /**
     * <p>
     * Select a trigger' state value.
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @return the <code>{@link org.quartz.Trigger}</code> object
     */
    String selectTriggerState(final ServerContext context, TriggerKey triggerKey) throws ResourceException{return null;};

    /**
     * <p>
     * Select a trigger' status (state & next fire time).
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @return a <code>TriggerStatus</code> object, or null
     */
    //TriggerStatus selectTriggerStatus(final ServerContext context, TriggerKey triggerKey) throws ResourceException{return null;};

    /**
     * <p>
     * Select the total number of triggers stored.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return the total number of triggers stored
     */
    int selectNumTriggers(final ServerContext context) throws ResourceException{return  0;};

    /**
     * <p>
     * Select all of the trigger group names that are stored.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return an array of <code>String</code> group names
     */
    List<String> selectTriggerGroups(final ServerContext context) throws ResourceException{return null;};

    List<String> selectTriggerGroups(final ServerContext context, GroupMatcher<TriggerKey> matcher) throws ResourceException{return null;};

    /**
     * <p>
     * Select all of the triggers contained in a given group.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param matcher
     *          to evaluate against known triggers
     * @return a Set of <code>TriggerKey</code>s
     */
    Set<TriggerKey> selectTriggersInGroup(final ServerContext context, GroupMatcher<TriggerKey> matcher)
            throws ResourceException{return null;};

    /**
     * <p>
     * Select all of the triggers in a given state.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param state
     *          the state the triggers must be in
     * @return an array of trigger <code>Key</code> s
     */
    List<TriggerKey> selectTriggersInState(final ServerContext context, String state)
            throws ResourceException{return null;};

    int insertPausedTriggerGroup(final ServerContext context, String groupName)
            throws ResourceException{return 0;};

    int deletePausedTriggerGroup(final ServerContext context, String groupName)
            throws ResourceException{return  0;};

    int deletePausedTriggerGroup(final ServerContext context, GroupMatcher<TriggerKey> matcher)
            throws ResourceException{return  0;};

    int deleteAllPausedTriggerGroups(final ServerContext context)
            throws ResourceException{return  0;};

    boolean isTriggerGroupPaused(final ServerContext context, String groupName)
            throws ResourceException{return  false;};

    Set<String> selectPausedTriggerGroups(final ServerContext context)
            throws ResourceException{return null;};

    boolean isExistingTriggerGroup(final ServerContext context, String groupName)
            throws ResourceException{return false;};

    //---------------------------------------------------------------------------
    // calendars
    //---------------------------------------------------------------------------

    /**
     * <p>
     * Insert a new calendar.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param calendarName
     *          the name for the new calendar
     * @param calendar
     *          the calendar
     * @return the number of rows inserted
     * @throws IOException
     *           if there were problems serializing the calendar
     */
    int insertCalendar(final ServerContext context, String calendarName,
                       Calendar calendar) throws IOException, ResourceException{return  0;};

    /**
     * <p>
     * Update a calendar.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param calendarName
     *          the name for the new calendar
     * @param calendar
     *          the calendar
     * @return the number of rows updated
     * @throws IOException
     *           if there were problems serializing the calendar
     */
    int updateCalendar(final ServerContext context, String calendarName,
                       Calendar calendar) throws IOException, ResourceException{return  0;};

    /**
     * <p>
     * Check whether or not a calendar exists.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param calendarName
     *          the name of the calendar
     * @return true if the trigger exists, false otherwise
     */
    boolean calendarExists(final ServerContext context, String calendarName)
            throws ResourceException{return false;};

    /**
     * <p>
     * Select a calendar.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param calendarName
     *          the name of the calendar
     * @return the Calendar
     * @throws ClassNotFoundException
     *           if a class found during deserialization cannot be found be
     *           found
     * @throws IOException
     *           if there were problems deserializing the calendar
     */
    Calendar selectCalendar(final ServerContext context, String calendarName)
            throws ClassNotFoundException, IOException, ResourceException{return null;};

    /**
     * <p>
     * Check whether or not a calendar is referenced by any triggers.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param calendarName
     *          the name of the calendar
     * @return true if any triggers reference the calendar, false otherwise
     */
    boolean calendarIsReferenced(final ServerContext context, String calendarName)
            throws ResourceException{return false;};

    /**
     * <p>
     * Delete a calendar.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param calendarName
     *          the name of the trigger
     * @return the number of rows deleted
     */
    int deleteCalendar(final ServerContext context, String calendarName)
            throws ResourceException{return  0;};

    /**
     * <p>
     * Select the total number of calendars stored.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return the total number of calendars stored
     */
    int selectNumCalendars(final ServerContext context) throws ResourceException{return  0;};

    /**
     * <p>
     * Select all of the stored calendars.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return an array of <code>String</code> calendar names
     */
    List<String> selectCalendars(final ServerContext context) throws ResourceException{return null;};

    //---------------------------------------------------------------------------
    // trigger firing
    //---------------------------------------------------------------------------

    /**
     * <p>
     * Select the trigger that will be fired at the given fire time.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param fireTime
     *          the time that the trigger will be fired
     * @return a <code>{@link org.quartz.utils.Key}</code> representing the
     *         trigger that will be fired at the given fire time, or null if no
     *         trigger will be fired at that time
     */
    Key<?> selectTriggerForFireTime(final ServerContext context, long fireTime)
            throws ResourceException{return null;};

    /**
     * <p>
     * Select the next trigger which will fire to fire between the two given timestamps 
     * in ascending order of fire time, and then descending by priority.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param noLaterThan
     *          highest value of <code>getNextFireTime()</code> of the triggers (exclusive)
     * @param noEarlierThan
     *          highest value of <code>getNextFireTime()</code> of the triggers (inclusive)
     *
     * @return A (never null, possibly empty) list of the identifiers (Key objects) of the next triggers to be fired.
     *
     * @deprecated - This remained for compatibility reason. Use {@link #selectTriggerToAcquire(Connection, long, long, int)} instead. 
     */
    public List<TriggerKey> selectTriggerToAcquire(final ServerContext context, long noLaterThan, long noEarlierThan)
            throws ResourceException{return null;};

    /**
     * <p>
     * Select the next trigger which will fire to fire between the two given timestamps 
     * in ascending order of fire time, and then descending by priority.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param noLaterThan
     *          highest value of <code>getNextFireTime()</code> of the triggers (exclusive)
     * @param noEarlierThan
     *          highest value of <code>getNextFireTime()</code> of the triggers (inclusive)
     * @param maxCount
     *          maximum number of trigger keys allow to acquired in the returning list.
     *
     * @return A (never null, possibly empty) list of the identifiers (Key objects) of the next triggers to be fired.
     */
    public List<TriggerKey> selectTriggerToAcquire(final ServerContext context, long noLaterThan, long noEarlierThan, int maxCount)
            throws ResourceException{return null;};

    /**
     * <p>
     * Insert a fired trigger.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param trigger
     *          the trigger
     * @param state
     *          the state that the trigger should be stored in
     * @return the number of rows inserted
     */
    int insertFiredTrigger(final ServerContext context, OperableTrigger trigger,
                           String state, JobDetail jobDetail) throws ResourceException{return null;};

    /**
     * <p>
     * Update a fired trigger record.  Will update the fields  
     * "firing instance", "fire time", and "state".
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param trigger
     *          the trigger
     * @param state
     *          the state that the trigger should be stored in
     * @return the number of rows inserted
     */
    int updateFiredTrigger(final ServerContext context, OperableTrigger trigger,
                           String state, JobDetail jobDetail) throws ResourceException{return 0;};

    /**
     * <p>
     * Select the states of all fired-trigger records for a given trigger, or
     * trigger group if trigger name is <code>null</code>.
     * </p>
     *
     * @return a List of FiredTriggerRecord objects.
     */
    List<FiredTriggerRecord> selectFiredTriggerRecords(final ServerContext context, String triggerName, String groupName) throws ResourceException{return null;};

    /**
     * <p>
     * Select the states of all fired-trigger records for a given job, or job
     * group if job name is <code>null</code>.
     * </p>
     *
     * @return a List of FiredTriggerRecord objects.
     */
    List<FiredTriggerRecord> selectFiredTriggerRecordsByJob(final ServerContext context, String jobName, String groupName) throws ResourceException{return null;};

    /**
     * <p>
     * Select the states of all fired-trigger records for a given scheduler
     * instance.
     * </p>
     *
     * @return a List of FiredTriggerRecord objects.
     */
    List<FiredTriggerRecord> selectInstancesFiredTriggerRecords(final ServerContext context, String instanceName) throws ResourceException{return null;};


    /**
     * <p>
     * Select the distinct instance names of all fired-trigger records.
     * </p>
     *
     * <p>
     * This is useful when trying to identify orphaned fired triggers (a 
     * fired trigger without a scheduler state record.) 
     * </p>
     *
     * @return a Set of String objects.
     */
    Set<String> selectFiredTriggerInstanceNames(final ServerContext context)
            throws ResourceException{return null;};

    /**
     * <p>
     * Delete a fired trigger.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @param entryId
     *          the fired trigger entry to delete
     * @return the number of rows deleted
     */
    int deleteFiredTrigger(final ServerContext context, String entryId)
            throws ResourceException{return 0;};

    /**
     * <p>
     * Get the number instances of the identified job currently executing.
     * </p>
     *
     * @param context
     *          the DB Connection
     *
     * @return the number instances of the identified job currently executing.
     */
    int selectJobExecutionCount(final ServerContext context, JobKey jobKey) throws ResourceException{return 0;};

    /**
     * <p>
     * Insert a scheduler-instance state record.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return the number of inserted rows.
     */
    int insertSchedulerState(final ServerContext context, String instanceId,
                             long checkInTime, long interval)
            throws ResourceException{return 0;};

    /**
     * <p>
     * Delete a scheduler-instance state record.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return the number of deleted rows.
     */
    int deleteSchedulerState(final ServerContext context, String instanceId)
            throws ResourceException{return 0;};


    /**
     * <p>
     * Update a scheduler-instance state record.
     * </p>
     *
     * @param context
     *          the DB Connection
     * @return the number of updated rows.
     */
    int updateSchedulerState(final ServerContext context, String instanceId, long checkInTime)
            throws ResourceException{return 0;};

    /**
     * <p>
     * A List of all current <code>SchedulerStateRecords</code>.
     * </p>
     *
     * <p>
     * If instanceId is not null, then only the record for the identified
     * instance will be returned.
     * </p>
     *
     * @param context
     *          the DB Connection
     */
    List<SchedulerStateRecord> selectSchedulerStateRecords(final ServerContext context, String instanceId) throws ResourceException{return null;};

    /**
     * Clear (delete!) all scheduling data - all {@link org.quartz.Job}s, {@link Trigger}s
     * {@link Calendar}s.
     *
     * @throws JobPersistenceException
     */
    void clearData(final ServerContext context)
            throws ResourceException{};
    
    
    ///////////



    /**
     * Recover any failed or misfired jobs and clean up the data store as
     * appropriate.
     *
     * @throws JobPersistenceException if jobs could not be recovered
     */
    protected void recoverJobs() throws JobPersistenceException {
        executeInNonManagedTXLock(
                LOCK_TRIGGER_ACCESS,
                new VoidTransactionCallback() {
                    public void execute(Connection conn) throws JobPersistenceException {
                        recoverJobs(conn);
                    }
                });
    }

    /**
     * <p>
     * Will recover any failed or misfired jobs and clean up the data store as
     * appropriate.
     * </p>
     *
     * @throws JobPersistenceException
     *           if jobs could not be recovered
     */
    protected void recoverJobs(final ServerContext context) throws JobPersistenceException {
        try {
            // update inconsistent job states
            int rows = updateTriggerStatesFromOtherStates(context,
                    STATE_WAITING, STATE_ACQUIRED, STATE_BLOCKED);

            rows += updateTriggerStatesFromOtherStates(context,
                    STATE_PAUSED, STATE_PAUSED_BLOCKED, STATE_PAUSED_BLOCKED);

            logger.info(
                    "Freed " + rows
                            + " triggers from 'acquired' / 'blocked' state.");

            // clean up misfired jobs
            recoverMisfiredJobs(context, true);

            // recover jobs marked for recovery that were not fully executed
            List<OperableTrigger> recoveringJobTriggers = selectTriggersForRecoveringJobs(context);
            logger.info(
                    "Recovering "
                            + recoveringJobTriggers.size()
                            + " jobs that were in-progress at the time of the last shut-down.");

            for (OperableTrigger recoveringJobTrigger: recoveringJobTriggers) {
                if (jobExists(context, recoveringJobTrigger.getJobKey())) {
                    recoveringJobTrigger.computeFirstFireTime(null);
                    storeTrigger(context, recoveringJobTrigger, null, false,
                            STATE_WAITING, false, true);
                }
            }
            logger.info("Recovery complete.");

            // remove lingering 'complete' triggers...
            List<TriggerKey> cts = selectTriggersInState(context, STATE_COMPLETE);
            for(TriggerKey ct: cts) {
                removeTrigger(context, ct);
            }
            logger.info(
                    "Removed " + cts.size() + " 'complete' triggers.");

            // clean up any fired trigger entries
            int n = deleteFiredTriggers(context);
            logger.info("Removed " + n + " stale fired job entries.");
        } catch (JobPersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new JobPersistenceException("Couldn't recover jobs: "
                    + e.getMessage(), e);
        }
    }

    protected long getMisfireTime() {
        long misfireTime = System.currentTimeMillis();
        if (getMisfireThreshold() > 0) {
            misfireTime -= getMisfireThreshold();
        }

        return (misfireTime > 0) ? misfireTime : 0;
    }

    /**
     * Helper class for returning the composite result of trying
     * to recover misfired jobs.
     */
    protected static class RecoverMisfiredJobsResult {
        public static final RecoverMisfiredJobsResult NO_OP =
                new RecoverMisfiredJobsResult(false, 0, Long.MAX_VALUE);

        private boolean _hasMoreMisfiredTriggers;
        private int _processedMisfiredTriggerCount;
        private long _earliestNewTime;

        public RecoverMisfiredJobsResult(
                boolean hasMoreMisfiredTriggers, int processedMisfiredTriggerCount, long earliestNewTime) {
            _hasMoreMisfiredTriggers = hasMoreMisfiredTriggers;
            _processedMisfiredTriggerCount = processedMisfiredTriggerCount;
            _earliestNewTime = earliestNewTime;
        }

        public boolean hasMoreMisfiredTriggers() {
            return _hasMoreMisfiredTriggers;
        }
        public int getProcessedMisfiredTriggerCount() {
            return _processedMisfiredTriggerCount;
        }
        public long getEarliestNewTime() {
            return _earliestNewTime;
        }
    }


    protected RecoverMisfiredJobsResult recoverMisfiredJobs(
            final ServerContext context, boolean recovering)
            throws JobPersistenceException, ResourceException {

        // If recovering, we want to handle all of the misfired
        // triggers right away.
        int maxMisfiresToHandleAtATime =
                (recovering) ? -1 : getMaxMisfiresToHandleAtATime();

        List<TriggerKey> misfiredTriggers = new LinkedList<TriggerKey>();
        long earliestNewTime = Long.MAX_VALUE;
        // We must still look for the MISFIRED state in case triggers were left
        // in this state when upgrading to this version that does not support it.
        boolean hasMoreMisfiredTriggers =
                hasMisfiredTriggersInState(
                        context, STATE_WAITING, getMisfireTime(),
                        maxMisfiresToHandleAtATime, misfiredTriggers);

        if (hasMoreMisfiredTriggers) {
            logger.info(
                    "Handling the first " + misfiredTriggers.size() +
                            " triggers that missed their scheduled fire-time.  " +
                            "More misfired triggers remain to be processed.");
        } else if (misfiredTriggers.size() > 0) {
            logger.info(
                    "Handling " + misfiredTriggers.size() +
                            " trigger(s) that missed their scheduled fire-time.");
        } else {
            logger.debug(
                    "Found 0 triggers that missed their scheduled fire-time.");
            return RecoverMisfiredJobsResult.NO_OP;
        }

        for (TriggerKey triggerKey: misfiredTriggers) {

            OperableTrigger trig =
                    retrieveTrigger(context, triggerKey);

            if (trig == null) {
                continue;
            }

            doUpdateOfMisfiredTrigger(context, trig, false, STATE_WAITING, recovering);

            if(trig.getNextFireTime() != null && trig.getNextFireTime().getTime() < earliestNewTime)
                earliestNewTime = trig.getNextFireTime().getTime();
        }

        return new RecoverMisfiredJobsResult(
                hasMoreMisfiredTriggers, misfiredTriggers.size(), earliestNewTime);
    }


    private void doUpdateOfMisfiredTrigger(final ServerContext context,final OperableTrigger trig, boolean forceState, String newStateIfNotComplete, boolean recovering) throws JobPersistenceException {
        Calendar cal = null;
        if (trig.getCalendarName() != null) {
            cal = retrieveCalendar(context, trig.getCalendarName());
        }

        schedSignaler.notifyTriggerListenersMisfired(trig);

        trig.updateAfterMisfire(cal);

        if (trig.getNextFireTime() == null) {
            storeTrigger(context, trig,
                    null, true, STATE_COMPLETE, forceState, recovering);
            schedSignaler.notifySchedulerListenersFinalized(trig);
        } else {
            storeTrigger(context, trig, null, true, newStateIfNotComplete,
                    forceState, false);
        }
    }


    /**
     * <p>
     * Check existence of a given job.
     * </p>
     */
    protected boolean jobExists(ServerContext context, JobKey jobKey) throws JobPersistenceException {
        try {
            return jobExists(context, jobKey);
        } catch (ResourceException e) {
            throw new JobPersistenceException(
                    "Couldn't determine job existence (" + jobKey + "): " + e.getMessage(), e);
        }
    }


    /**
     * Execute the given callback having optionally acquired the given lock.
     * Because CMT assumes that the contextection is already part of a managed
     * transaction, it does not attempt to commit or rollback the
     * enclosing transaction.
     *
     * @param lockName The name of the lock to acquire, for example
     * "TRIGGER_ACCESS".  If null, then no lock is acquired, but the
     * txCallback is still executed in a transaction.
     *
     * @see JobStoreSupport#executeInNonManagedTXLock(String, TransactionCallback)
     * @see JobStoreTX#executeInLock(String, TransactionCallback)
     * @see JobStoreSupport#getNonManagedTXConnection()
     * @see JobStoreSupport#getConnection()
     */
    @Override
    protected <R> R executeInLock(
            String lockName,
            TransactionCallback<R> txCallback) throws JobPersistenceException {
        boolean transOwner = false;
        final ServerContext context = getServerContext();
        try {
            if (lockName != null) {
                transOwner = getLockHandler().obtainLock(context, lockName);
            }

            return txCallback.execute(context);
        } finally {
            try {
                releaseLock(context, LOCK_TRIGGER_ACCESS, transOwner);
            } finally {
                cleanupConnection(context);
            }
        }
    }

    /**
     * Determines if a Trigger for the given job should be blocked.
     * State can only transition to STATE_PAUSED_BLOCKED/BLOCKED from
     * PAUSED/STATE_WAITING respectively.
     *
     * @return STATE_PAUSED_BLOCKED, BLOCKED, or the currentState.
     */
    protected String checkBlockedState(
            final ServerContext context, JobKey jobKey, String currentState)
            throws JobPersistenceException {

        // State can only transition to BLOCKED from PAUSED or WAITING.
        if ((currentState.equals(STATE_WAITING) == false) &&
                (currentState.equals(STATE_PAUSED) == false)) {
            return currentState;
        }

        try {
            List<FiredTriggerRecord> lst = selectFiredTriggerRecordsByJob(context,
                    jobKey.getName(), jobKey.getGroup());

            if (lst.size() > 0) {
                FiredTriggerRecord rec = lst.get(0);
                if (rec.isJobDisallowsConcurrentExecution()) { // TODO: worry about failed/recovering/volatile job  states?
                    return (STATE_PAUSED.equals(currentState)) ? STATE_PAUSED_BLOCKED : STATE_BLOCKED;
                }
            }

            return currentState;
        } catch (ResourceException e) {
            throw new JobPersistenceException(
                    "Couldn't determine if trigger should be in a blocked state '"
                            + jobKey + "': "
                            + e.getMessage(), e);
        }

    }


    protected interface TransactionCallback<R> {
        R execute(ServerContext context) throws JobPersistenceException;
    }

    //---------------------------------------------------------------------------
    // Management methods
    //---------------------------------------------------------------------------

    protected RecoverMisfiredJobsResult doRecoverMisfires() throws JobPersistenceException {
        boolean transOwner = false;
        Connection conn = getNonManagedTXConnection();
        try {
            RecoverMisfiredJobsResult result = RecoverMisfiredJobsResult.NO_OP;

            // Before we make the potentially expensive call to acquire the
            // trigger lock, peek ahead to see if it is likely we would find
            // misfired triggers requiring recovery.
            int misfireCount = (getDoubleCheckLockMisfireHandler()) ?
                    countMisfiredTriggersInState(
                            conn, STATE_WAITING, getMisfireTime()) :
                    Integer.MAX_VALUE;

            if (misfireCount == 0) {
                logger.debug(
                        "Found 0 triggers that missed their scheduled fire-time.");
            } else {
                transOwner = getLockHandler().obtainLock(conn, LOCK_TRIGGER_ACCESS);

                result = recoverMisfiredJobs(conn, false);
            }

            commitConnection(conn);
            return result;
        } catch (JobPersistenceException e) {
            rollbackConnection(conn);
            throw e;
        } catch (ResourceException e) {
            rollbackConnection(conn);
            throw new JobPersistenceException("Database error recovering from misfires.", e);
        } catch (RuntimeException e) {
            rollbackConnection(conn);
            throw new JobPersistenceException("Unexpected runtime exception: "
                    + e.getMessage(), e);
        } finally {
            try {
                releaseLock(conn, LOCK_TRIGGER_ACCESS, transOwner);
            } finally {
                cleanupConnection(conn);
            }
        }
    }

    protected ThreadLocal<Long> sigChangeForTxCompletion = new ThreadLocal<Long>();
    protected void signalSchedulingChangeOnTxCompletion(long candidateNewNextFireTime) {
        Long sigTime = sigChangeForTxCompletion.get();
        if(sigTime == null && candidateNewNextFireTime >= 0L)
            sigChangeForTxCompletion.set(candidateNewNextFireTime);
        else {
            if(sigTime == null || candidateNewNextFireTime < sigTime)
                sigChangeForTxCompletion.set(candidateNewNextFireTime);
        }
    }

    protected Long clearAndGetSignalSchedulingChangeOnTxCompletion() {
        Long t = sigChangeForTxCompletion.get();
        sigChangeForTxCompletion.set(null);
        return t;
    }

    protected void signalSchedulingChangeImmediately(long candidateNewNextFireTime) {
        schedSignaler.signalSchedulingChange(candidateNewNextFireTime);
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    // MisfireHandler Thread
    //
    /////////////////////////////////////////////////////////////////////////////

    class MisfireHandler extends Thread {

        private volatile boolean shutdown = false;

        private int numFails = 0;


        MisfireHandler() {
            this.setName("QuartzScheduler_" + instanceName + "-" + instanceId + "_MisfireHandler");
            this.setDaemon(false);
        }

        public void initialize() {
            ThreadExecutor executor = getThreadExecutor();
            executor.execute(MisfireHandler.this);
        }

        public void shutdown() {
            shutdown = true;
            this.interrupt();
        }

        private RecoverMisfiredJobsResult manage() {
            try {
                logger.debug("MisfireHandler: scanning for misfires...");

                RecoverMisfiredJobsResult res = doRecoverMisfires();
                numFails = 0;
                return res;
            } catch (Exception e) {
                if(numFails % 4 == 0) {
                    logger.error(
                            "MisfireHandler: Error handling misfires: {}",
                            e.getMessage(), e);
                }
                numFails++;
            }
            return RecoverMisfiredJobsResult.NO_OP;
        }

        @Override
        public void run() {

            while (!shutdown) {

                long sTime = System.currentTimeMillis();

                RecoverMisfiredJobsResult recoverMisfiredJobsResult = manage();

                if (recoverMisfiredJobsResult.getProcessedMisfiredTriggerCount() > 0) {
                    signalSchedulingChangeImmediately(recoverMisfiredJobsResult.getEarliestNewTime());
                }

                if (!shutdown) {
                    long timeToSleep = 50l;  // At least a short pause to help balance threads
                    if (!recoverMisfiredJobsResult.hasMoreMisfiredTriggers()) {
                        timeToSleep = getMisfireThreshold() - (System.currentTimeMillis() - sTime);
                        if (timeToSleep <= 0) {
                            timeToSleep = 50l;
                        }

                        if(numFails > 0) {
                            timeToSleep = Math.max(getDbRetryInterval(), timeToSleep);
                        }
                    }

                    try {
                        Thread.sleep(timeToSleep);
                    } catch (Exception ignore) {
                    }
                }//while !shutdown
            }
        }
    }

}
