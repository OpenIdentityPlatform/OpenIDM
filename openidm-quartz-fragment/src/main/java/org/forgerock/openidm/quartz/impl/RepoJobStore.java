/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright 2012-2015 ForgeRock AS. All Rights Reserved
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
*
*/
package org.forgerock.openidm.quartz.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.cluster.ClusterEvent;
import org.forgerock.openidm.cluster.ClusterEventListener;
import org.forgerock.openidm.cluster.ClusterManagementService;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.util.ContextUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.core.SchedulingContext;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JobStore implementation used for persistence with the OpenIdm Repository Service.
 */
public class RepoJobStore implements JobStore, ClusterEventListener {

    private final static Logger logger = LoggerFactory.getLogger(RepoJobStore.class);

    private final static Object lock = new Object();

    /**
     * An identifier used to create unique keys for Jobs and Triggers.
     */
    private final static String UNIQUE_ID_SEPARATOR = "_$x$x$_";

    /**
     * Cluster Management Service
     */
    private ClusterManagementService clusterManager;

    /**
     * The SchedulerSignaler to send notification to
     */
    private SchedulerSignaler schedulerSignaler;

    /**
     * The ClassLoadHelper (currently unused)
     */
    private ClassLoadHelper loadHelper;

    /**
     * The instance ID
     */
    private String instanceId;

    /**
     * The listener ID used for registration with the Cluster Management Service
     */
    private String listenerId = "scheduler";

    /**
     * The instance Name (currently unused)
     */
    private String instanceName;

    /**
     * The misfire threshold
     */
    private long misfireThreshold = 10000;

    /**
     * Number of retries on failed writes to the repository (defaults to -1, infinite).
     */
    private int writeRetries = -1;

    /**
     * A list of all "blocked" jobs.
     */
    private List<String> blockedJobs = new ArrayList<String>();

    /**
     * An AtomicLong used for creating record IDs
     */
    private static AtomicLong ftrCtr = new AtomicLong(System.currentTimeMillis());

    /**
     * The Repository Service Accessor
     */
    private static Context context = null;

    /**
     * Boolean indicating if the scheduler has called shutdown()
     */
    private volatile boolean shutdown = false;
    
    /**
     * Creates a new <code>RepoJobStore</code>.
     */
    public RepoJobStore() {
    }

    /**
     * <p>
     * Called by the QuartzScheduler before the <code>JobStore</code> is
     * used, in order to give the it a chance to initialize.
     * </p>
     */
    @Override
    public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler schedSignaler) {
        logger.debug("Initializing RepoJobStore");
        this.schedulerSignaler = schedSignaler;
        this.loadHelper = loadHelper;
        // Set the number of retries for failed writes to the repository
        this.writeRetries = Integer.parseInt(IdentityServer.getInstance().getProperty("openidm.scheduler.repo.retry", "-1"));
        cleanUpInstance();
    }

    /**
     * Sets the Repository Service Router and returns true if successful, false otherwise.
     *
     * @return  true if successful, false otherwise
     */
    public Context getContext() throws JobPersistenceException {
        if (context == null) {
            context = ContextUtil.createInternalContext();
        }
        return context;
    }

    public static void setContext(Context context) {
        RepoJobStore.context = context;
    }

    public boolean setClusterService() {
        if (clusterManager == null) {
            BundleContext ctx = FrameworkUtil.getBundle(RepoJobStore.class).getBundleContext();
            ServiceReference serviceReference = ctx.getServiceReference(ClusterManagementService.class.getName());
            clusterManager = ClusterManagementService.class.cast(ctx.getService(serviceReference));
            return !(clusterManager == null);
        }
        return true;
    }

    private ConnectionFactory connectionFactory;

    /**
     * Sets the ConnectionFactory and returns true if successful, false otherwise.
     *
     * @return  true if successful, false otherwise
     */
    public ConnectionFactory getConnectionFactory() throws JobPersistenceException {
        if (connectionFactory == null) {
            BundleContext ctx = FrameworkUtil.getBundle(IDMConnectionFactory.class).getBundleContext();
            ServiceReference serviceReference = null;
            try {
                serviceReference = ctx.getServiceReferences(IDMConnectionFactory.class,
                        "(openidm.router.prefix=/)").iterator().next();
            } catch (InvalidSyntaxException e) {
                /* ignore the filter is correct */
            }
            connectionFactory = IDMConnectionFactory.class.cast(ctx.getService(serviceReference));
            if (connectionFactory == null) {
                throw new JobPersistenceException("Connection factory is null");
            }
        }
        return connectionFactory;
    }

    @Override
    public void schedulerStarted() throws SchedulerException {
        logger.debug("Job Scheduler Started");
        if (isClustered()) {
            if (isClustered()) {
                if (setClusterService()) {
                    logger.info("Registering with ClusterManagementService");
                    clusterManager.register(listenerId, this);
                } else {
                    logger.error("ClusterManagementService not available");
                }
            }
        }
    }

    /**
     * Gets the repository ID prefix
     *
     * @return the repository ID prefix
     */
    private String getIdPrefix() {
        return "/repo/scheduler/";
    }

    /**
     * Gets the Calendar's repository ID.
     *
     * @param id    the Calendar's ID
     * @return  the repository ID
     */
    private String getCalendarsRepoId(String id) {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("calendars/").append(id).toString();
    }

    /**
     * Gets the Calendar names repository ID.
     *
     * @return  the repository ID
     */
    private String getCalendarNamesRepoId() {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("calendarNames").toString();
    }

    /**
     * Gets the Trigger's repository ID.
     *
     * @param group the Trigger's group
     * @param name  the Trigger's name
     * @return  the repository ID
     */
    private String getTriggersRepoId(String group, String name) {
        return new StringBuilder(getIdPrefix()).append("triggers/")
                .append(getTriggerId(group, name)).toString();
    }

    /**
     * Gets the Trigger groups repository ID.
     *
     * @return  the repository ID
     */
    private String getTriggerGroupsRepoId(String groupName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("triggerGroups/")
                .append(groupName).toString();
    }

    /**
     * Gets the Trigger group names repository ID.
     *
     * @return  the repository ID
     */
    private String getTriggerGroupNamesRepoId() {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("triggerGroupNames").toString();
    }

    /**
     * Gets the paused Trigger group names repository ID.
     *
     * @return  the repository ID
     */
    private String getPausedTriggerGroupNamesRepoId() {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("triggerPausedGroupNames").toString();
    }

    /**
     * Gets the Job's repository ID.
     *
     * @param group the Job's group
     * @param name  the Job's name
     * @return  the repository ID
     */
    private String getJobsRepoId(String group, String name) {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("jobs/").append(getJobId(group, name)).toString();
    }

    /**
     * Gets the Job groups repository ID.
     *
     * @param id    the group name
     * @return  the repository ID
     */
    private String getJobGroupsRepoId(String groupName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("jobGroups/").append(groupName).toString();
    }

    /**
     * Gets the Job group names repository ID.
     *
     * @return  the repository ID
     */
    private String getJobGroupNamesRepoId() {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("jobGroupNames").toString();
    }

    /**
     * Gets the paused Job groups names repository ID.
     *
     * @return  the repository ID
     */
    private String getPausedJobGroupNamesRepoId() {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("jobPausedGroupNames").toString();
    }

    /**
     * Gets the Waiting Triggers repository ID.
     *
     * @return  the repository ID
     */
    private String getWaitingTriggersRepoId() {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("waitingTriggers").toString();
    }

    /**
     * Gets the Acquired Triggers repository ID.
     *
     * @return  the repository ID
     */
    private String getAcquiredTriggersRepoId() {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("acquiredTriggers").toString();
    }

    /**
     * Gets the Trigger ID.
     *
     * @param group the Trigger group
     * @param name  the Trigger name
     * @return  the Trigger ID
     */
    private String getTriggerId(String group, String name) {
        return new StringBuilder(group).append(UNIQUE_ID_SEPARATOR)
                .append(name).toString();
    }

    /**
     * Gets the Job ID.
     *
     * @param group the Job group
     * @param name  the Job name
     * @return  the Job ID
     */
    private String getJobId(String group, String name) {
        return new StringBuilder(group).append(UNIQUE_ID_SEPARATOR)
                .append(name).toString();
    }

    private String getGroupFromId(String id) {
        return id.substring(0, id.indexOf(UNIQUE_ID_SEPARATOR));
    }

    private String getNameFromId(String id) {
        return id.substring(id.indexOf(UNIQUE_ID_SEPARATOR) + UNIQUE_ID_SEPARATOR.length());
    }

    /**
     * <p>
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * it should free up all of it's resources because the scheduler is
     * shutting down.
     * </p>
     */
    @Override
    public void shutdown() {
        synchronized(lock) {
            shutdown = true;
            logger.debug("Job Scheduler Stopped");
        }
    }

    @Override
    public boolean supportsPersistence() {
        return true;
    }

    @Override
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public void storeCalendar(SchedulingContext context, String name,
            Calendar calendar, boolean replaceExisting, boolean updateTriggers)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        synchronized (lock) {
            try {
                CalendarWrapper cw = new CalendarWrapper(calendar, name);
                if (retrieveCalendar(context, name) == null) {
                    // Create Calendar
                    logger.debug("Creating Calendar: {}", name);
                    getConnectionFactory().getConnection().create(getContext(), getCreateRequest(getCalendarsRepoId(name), cw.getValue().asMap()));
                } else {
                    if (!replaceExisting) {
                        throw new ObjectAlreadyExistsException(name);
                    }
                    // Update Calendar
                    CalendarWrapper oldCw = getCalendarWrapper(name);
                    logger.debug("Updating Calendar: {}", name);
                    UpdateRequest r = Requests.newUpdateRequest(getCalendarsRepoId(name), cw.getValue());
                    r.setRevision(oldCw.getRevision());
                    getConnectionFactory().getConnection().update(getContext(), r);
                }

                if (updateTriggers) {
                    List<TriggerWrapper> twList = getTriggerWrappersForCalendar(name);
                    for (TriggerWrapper tw : twList) {
                        Trigger t = tw.getTrigger();
                        boolean removed = removeWaitingTrigger(t);
                        t.updateWithNewCalendar(calendar, getMisfireThreshold());
                        tw.updateTrigger(t);
                        logger.debug("Updating Trigger {} in group {}", tw.getName(),tw.getGroup());
                        updateTriggerInRepo(tw.getGroup(), tw.getName(), tw, tw.getRevision());
                        if (removed) {
                            addWaitingTrigger(t);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error storing calendar: {}", name, e);
                throw new JobPersistenceException("Error storing calendar", e);
            }
        }
    }

    @Override
    public void storeJob(SchedulingContext context, JobDetail newJob,
            boolean replaceExisting)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        synchronized (lock) {
            logger.debug("Attempting to store job {} ", newJob.getFullName());
            String jobName = newJob.getName();
            String jobGroup = newJob.getGroup();
            String jobId = getJobsRepoId(jobGroup, jobName);
            JobGroupWrapper jgw = null;
            try {
                // Get job group
                jgw = getOrCreateJobGroupWrapper(jobGroup);
            } catch (ResourceException e) {
                logger.warn("Error storing job", e);
                throw new JobPersistenceException("Error" + " storing job", e);
            }
            List<String> jobNames = jgw.getJobNames();
            JobWrapper jw = new JobWrapper(newJob, jgw.isPaused());
            if (jobNames.contains(jobName) && !replaceExisting) {
                throw new ObjectAlreadyExistsException(newJob);
            }
            try {
                // Check if job name exists
                if (jobNames.contains(jobName)) {
                    // Update job
                    JobWrapper oldJw = getJobWrapper(jobGroup, jobName);
                    logger.debug("Updating Job {} in group {}", jobName, jobGroup);
                    UpdateRequest r = Requests.newUpdateRequest(jobId, jw.getValue());
                    r.setRevision(oldJw.getRevision());
                    getConnectionFactory().getConnection().update(getContext(), r);
                } else {
                    // Add job name to list
                    jgw.addJob(jobName);
                    // Update job group
                    UpdateRequest r = Requests.newUpdateRequest(getJobGroupsRepoId(jobGroup), jgw.getValue());
                    r.setRevision(jgw.getRevision());
                    getConnectionFactory().getConnection().update(getContext(), r);

                    // Create job
                    logger.debug("Creating Job {} in group {}", jobName, jobGroup);
                    getConnectionFactory().getConnection().create(getContext(), getCreateRequest(jobId, jw.getValue().asMap()));
                }
            } catch (ResourceException e) {
                logger.warn("Error storing job", e);
                throw new JobPersistenceException("Error" + " storing job", e);
            }
        }
    }

    @Override
    public void storeJobAndTrigger(SchedulingContext context, JobDetail detail, Trigger trigger)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        synchronized (lock) {
            storeJob(context, detail, false);
            storeTrigger(context, trigger, false);
        }
    }

    @Override
    public void storeTrigger(SchedulingContext context, Trigger trigger,
            boolean replaceExisting) throws ObjectAlreadyExistsException, JobPersistenceException {
        synchronized (lock) {
            String triggerName = trigger.getKey().getName();
            String groupName = trigger.getKey().getGroup();
            String triggerId = getTriggersRepoId(groupName, triggerName);
            TriggerGroupWrapper tgw = null;
            try {
                // Get trigger group
                tgw = getOrCreateTriggerGroupWrapper(groupName);
            } catch (ResourceException e) {
                logger.warn("Error storing trigger", e);
                throw new JobPersistenceException("Error" + " storing trigger", e);
            }
            List<String> triggerNames = tgw.getTriggerNames();
            TriggerWrapper tw;
            try {
                tw = new TriggerWrapper(trigger, tgw.isPaused());
            } catch (Exception e) {
                logger.warn("Error storing trigger", e);
                throw new JobPersistenceException("Error" + " storing trigger", e);
            }
            if (triggerNames.contains(triggerName) && !replaceExisting) {
                throw new ObjectAlreadyExistsException(trigger);
            }
            try {
                // Check if trigger name exists
                if (triggerNames.contains(triggerName)) {
                    TriggerWrapper oldTw = getTriggerWrapper(groupName, triggerName);
                    // Update trigger
                    logger.debug("Updating Trigger {}", triggerId);
                    UpdateRequest r = Requests.newUpdateRequest(triggerId, tw.getValue());
                    r.setRevision(oldTw.getRevision());
                    getConnectionFactory().getConnection().update(getContext(), r);
                } else {
                    // Add trigger name to list
                    tgw.addTrigger(triggerName);
                    // Update trigger group
                    UpdateRequest r = Requests.newUpdateRequest(getTriggerGroupsRepoId(groupName), tgw.getValue());
                    r.setRevision(tgw.getRevision());
                    getConnectionFactory().getConnection().update(getContext(), r);

                    // Create trigger
                    logger.debug("Creating Trigger {}", triggerId);
                    getConnectionFactory().getConnection().create(getContext(), getCreateRequest(triggerId, tw.getValue().asMap()));
                }
            } catch (ResourceException e) {
                logger.warn("Error storing trigger", e);
                throw new JobPersistenceException("Error" + " storing trigger", e);
            }
            logger.debug("Adding waiting trigger {}", trigger.getName());
            addWaitingTrigger(trigger);
        }
    }

    @Override
    public Trigger acquireNextTrigger(SchedulingContext context, long noLaterThan)
            throws JobPersistenceException {
        synchronized (lock) {
            logger.debug("Attempting to acquire the next trigger");
            Trigger trigger = null;
            WaitingTriggers waitingTriggers = null;
            while (trigger == null && !shutdown) {
                try {
                    waitingTriggers = getWaitingTriggers();
                    trigger = waitingTriggers.getTriggers().first();
                } catch (NoSuchElementException e1) {
                    logger.debug("No waiting triggers to acquire");
                    return null;
                }

                if (trigger == null) {
                    logger.debug("No waiting triggers to acquire");
                    return null;
                }


                Date nextFireTime = trigger.getNextFireTime();
                if (nextFireTime == null) {
                    logger.debug("Trigger next fire time = null, removing");
                    removeWaitingTrigger(trigger);
                    trigger = null;
                    continue;
                }

                if (noLaterThan > 0) {
                    if (nextFireTime.getTime() > noLaterThan) {
                        logger.debug("Trigger fire time {} is later than {}, not acquiring",  nextFireTime, new Date(noLaterThan));
                        return null;
                    }
                }

                if(!removeWaitingTrigger(trigger)) {
                    trigger = null;
                    continue;
                }

                TriggerWrapper tw = getTriggerWrapper(trigger.getGroup(), trigger.getName());

                if (hasTriggerMisfired(trigger)) {
                    logger.debug("Attempting to process misfired trigger");
                    processTriggerMisfired(tw);
                    if (trigger.getNextFireTime() != null) {
                        addWaitingTrigger(trigger);
                    }
                    trigger = null;
                    continue;
                }

                tw.setAcquired(true);
                trigger.setFireInstanceId(getFiredTriggerRecordId());
                try {
                    tw.updateTrigger(trigger);
                } catch (Exception e) {
                    logger.warn("Error serializing trigger", e);
                    addWaitingTrigger(trigger);
                    throw new JobPersistenceException("Error serializing trigger", e);
                }

                updateTriggerInRepo(trigger.getGroup(), trigger.getName(), tw, tw.getRevision());

                addAcquiredTrigger(trigger, instanceId);

                logger.debug("Acquired next trigger {} to be fired at {}", trigger.getName(), trigger.getNextFireTime());
                return (Trigger)trigger.clone();
            }
            logger.debug("No waiting triggers to acquire");
            return null;
        }
    }



    @Override
    public void releaseAcquiredTrigger(SchedulingContext arg0, Trigger trigger)
            throws JobPersistenceException {
        synchronized (lock) {
            TriggerWrapper tw = getTriggerWrapper(trigger.getGroup(), trigger.getName());
            if (tw == null) {
                logger.debug("Cannot release acquired trigger {} in group {}, trigger does not exist", trigger.getName(), trigger.getGroup());
                return;
            }
            if (tw.isAcquired()) {
                tw.setAcquired(false);
                updateTriggerInRepo(trigger.getGroup(), trigger.getName(), tw, tw.getRevision());
                addWaitingTrigger(trigger);
                removeAcquiredTrigger(trigger, instanceId);
            } else {
                logger.warn("Cannot release acquired trigger {} in group {}, trigger has not been acquired", trigger.getName(), trigger.getGroup());
            }
        }
    }

    @Override
    public String[] getCalendarNames(SchedulingContext arg0)
            throws JobPersistenceException {
        List<String> names = null;
        try {
            names = getOrCreateRepoList(getCalendarNamesRepoId(), "names");
        } catch (ResourceException e) {
            logger.warn("Error getting calendar names", e);
            throw new JobPersistenceException("Error getting calendar names", e);
        }
        if (names.size() > 0) {
            return names.toArray(new String[names.size()]);
        } else {
            return null;
        }
    }

    private List<TriggerWrapper> getTriggerWrappersForCalendar(String calName) throws JobPersistenceException {
        synchronized (lock) {
            ArrayList<TriggerWrapper> trigList = new ArrayList<TriggerWrapper>();
            String[] groups = getTriggerGroupNames(null);
            for (String group : groups) {
                String[] names = getTriggerNames(null, group);
                for (String name : names) {
                    TriggerWrapper tw = getTriggerWrapper(group, name);
                    Trigger trigger = tw.getTrigger();
                    if (trigger.getCalendarName().equals(calName)) {
                        trigList.add(tw);
                    }
                }
            }
            return trigList;
        }
    }


    @Override
    public long getEstimatedTimeToReleaseAndAcquireTrigger() {
        return 5;
    }

    @Override
    public String[] getJobGroupNames(SchedulingContext context) throws JobPersistenceException {
        List<String> names = null;
        try {
            names = getOrCreateRepoList(getJobGroupNamesRepoId(), "names");
        } catch (ResourceException e) {
            logger.warn("Error getting job group names", e);
            throw new JobPersistenceException("Error getting job group names", e);
        }
        return names.toArray(new String[names.size()]);
    }

    @Override
    public String[] getJobNames(SchedulingContext context, String groupName)
            throws JobPersistenceException {
        try {
            JsonValue fromRepo = readFromRepo(getJobGroupsRepoId(groupName));
            if (fromRepo != null && !fromRepo.isNull()) {
                JobGroupWrapper jgw = new JobGroupWrapper(fromRepo);
                List<String> names = jgw.getJobNames();
                return names.toArray(new String[names.size()]);
            }
        } catch (ResourceException e) {
            logger.warn("Error getting job names", e);
            throw new JobPersistenceException("Error getting job names", e);
        }
        return null;
    }

    @Override
    public int getNumberOfCalendars(SchedulingContext context)
            throws JobPersistenceException {
        String [] names = getCalendarNames(context);
        if (names != null) {
           return names.length;
        }
        return 0;
    }

    @Override
    public int getNumberOfJobs(SchedulingContext arg0)
            throws JobPersistenceException {
        String [] groupNames = getJobGroupNames(null);
        if (groupNames == null || groupNames.length == 0) {
            return 0;
        }
        int numOfJobs = 0;
        for (String groupName : groupNames) {
            String [] jobNames = getJobNames(null, groupName);
            if (jobNames != null) {
                numOfJobs += jobNames.length;
            }
        }
        return numOfJobs;
    }

    @Override
    public int getNumberOfTriggers(SchedulingContext arg0)
            throws JobPersistenceException {
        String [] groupNames = getTriggerGroupNames(null);
        if (groupNames == null || groupNames.length == 0) {
            return 0;
        }
        int numOfTriggers = 0;
        for (String groupName : groupNames) {
            String [] triggerNames = getTriggerNames(null, groupName);
            if (triggerNames != null) {
                numOfTriggers += triggerNames.length;
            }
        }
        return numOfTriggers;
    }

    @Override
    public Set getPausedTriggerGroups(SchedulingContext context)
            throws JobPersistenceException {
        List<String> names = null;
        try {
            Map<String, Object> pauseMap = getOrCreateRepo(getPausedTriggerGroupNamesRepoId());
            names = (List<String>)pauseMap.get("paused");
            if (names == null) {
                names = new ArrayList<String>();
            }
        } catch (ResourceException e) {
            logger.warn("Error getting paused trigger groups", e);
            throw new JobPersistenceException("Error getting paused trigger groups", e);
        }
        return new HashSet<String>(names);
    }

    @Override
    public String[] getTriggerGroupNames(SchedulingContext context) throws JobPersistenceException {
        List<String> names = null;
        try {
            names = getOrCreateRepoList(getTriggerGroupNamesRepoId(), "names");
        } catch (ResourceException e) {
            logger.warn("Error getting trigger group names", e);
            throw new JobPersistenceException("Error getting trigger group names", e);
        }
        return names.toArray(new String[names.size()]);
    }

    @Override
    public String[] getTriggerNames(SchedulingContext context, String groupName)
            throws JobPersistenceException {
        try {
            JsonValue fromRepo = readFromRepo(getTriggerGroupsRepoId(groupName));
            if (fromRepo != null && !fromRepo.isNull()) {
                TriggerGroupWrapper tgw = new TriggerGroupWrapper(fromRepo);
                List<String> names = tgw.getTriggerNames();
                return names.toArray(new String[names.size()]);
            }
        } catch (ResourceException e) {
            logger.warn("Error getting trigger names", e);
            throw new JobPersistenceException("Error getting trigger names", e);
        }
        return new String[0];
    }

    @Override
    public int getTriggerState(SchedulingContext context, String triggerName, String triggerGroup)
            throws JobPersistenceException {
        String id = getTriggerId(triggerGroup, triggerName);
        int state = 0;
        logger.trace("Getting trigger state {}", id);
        JsonValue trigger = getTriggerFromRepo(triggerGroup, triggerName);
        if (trigger.isNull()) {
            return Trigger.STATE_NONE;
        }
        state = trigger.get("state").asInteger();
        return state;
    }

    @Override
    public Trigger[] getTriggersForJob(SchedulingContext context, String jobName, String groupName)
            throws JobPersistenceException {
        synchronized (lock) {
            String[] triggerNames = getTriggerNames(context, groupName);
            List<Trigger> triggers = new ArrayList<Trigger>();
            for (String name : triggerNames) {
                TriggerWrapper tw = getTriggerWrapper(groupName, name);
                Trigger trigger = tw.getTrigger();
                if (trigger.getJobName().equals(jobName)) {
                    triggers.add(trigger);
                }
            }
            logger.debug("Found {} triggers for group {}", triggers.size(), groupName);
            return triggers.toArray(new Trigger[triggers.size()]);
        }
    }

    @Override
    public boolean isClustered() {
        return true;
    }

    @Override
    public void pauseAll(SchedulingContext context) throws JobPersistenceException {
        String [] names = getTriggerGroupNames(context);
        for (String name : names) {
            pauseTriggerGroup(context, name);
        }
    }

    @Override
    public void pauseJob(SchedulingContext context, String jobName, String groupName)
            throws JobPersistenceException {
        synchronized (lock) {
            Trigger[] triggers = getTriggersForJob(context, jobName, groupName);
            for (Trigger trigger : triggers) {
                pauseTrigger(context, trigger.getName(), trigger.getGroup());
            }
        }
    }

    @Override
    public void pauseJobGroup(SchedulingContext context, String groupName)
            throws JobPersistenceException {
        synchronized (lock) {
            try {
                // Get job group, set paused, and update
                JobGroupWrapper jgw = getOrCreateJobGroupWrapper(groupName);
                jgw.pause();
                UpdateRequest r = Requests.newUpdateRequest(getJobGroupsRepoId(groupName), jgw.getValue());
                r.setRevision(jgw.getRevision());
                getConnectionFactory().getConnection().update(getContext(), r);

                // Get list of paused groups, add this group (if already paused, return), and update
                Map<String, Object> pauseMap = getOrCreateRepo(getPausedJobGroupNamesRepoId());
                List<String> pausedGroups = (List<String>) pauseMap.get("paused");
                if (pausedGroups == null) {
                    pausedGroups = new ArrayList<String>();
                    pauseMap.put("paused", pausedGroups);
                } else if (pausedGroups.contains(groupName)) {
                    return;
                }
                pausedGroups.add(groupName);
                String rev = (String)pauseMap.get("_rev");
                r = Requests.newUpdateRequest(getPausedJobGroupNamesRepoId(), new JsonValue(pauseMap));
                r.setRevision(rev);
                getConnectionFactory().getConnection().update(getContext(), r);

                List<String> jobNames = jgw.getJobNames();
                for (String jobName : jobNames) {
                    pauseJob(context, jobName, groupName);
                }
            } catch (JsonValueException e) {
                logger.warn("Error pausing job group {}", groupName, e);
                throw new JobPersistenceException("Error pausing job group", e);
            } catch (Exception e) {
                logger.warn("Error pausing job group {}", groupName, e);
                throw new JobPersistenceException("Error pausing job group", e);
            }
        }
    }

    @Override
    public void pauseTrigger(SchedulingContext context, String triggerName, String triggerGroup)
            throws JobPersistenceException {
        synchronized (lock) {
            TriggerWrapper tw = getTriggerWrapper(triggerGroup, triggerName);
            if (tw == null) {
                logger.warn("Cannot pause trigger {} in group {}, trigger does not exist", triggerName, triggerGroup);
                return;
            }
            Trigger trigger;
            try {
                trigger = tw.getTrigger();
            } catch (Exception e) {
                logger.warn("Error deserializing trigger", e);
                throw new JobPersistenceException("Error deserializing trigger", e);
            }
            tw.pause();
            // Update the trigger
            updateTriggerInRepo(triggerGroup, triggerName, tw, tw.getRevision());
            // Remove trigger from waitingTriggers
            removeWaitingTrigger(trigger);
        }
    }

    @Override
    public void pauseTriggerGroup(SchedulingContext context, String groupName)
            throws JobPersistenceException {
        synchronized (lock) {
            try {
                // Get trigger group, set paused, and update
                TriggerGroupWrapper tgw = getOrCreateTriggerGroupWrapper(groupName);
                tgw.pause();
                UpdateRequest r = Requests.newUpdateRequest(getTriggerGroupsRepoId(groupName), tgw.getValue());
                r.setRevision(tgw.getRevision());
                getConnectionFactory().getConnection().update(getContext(), r);

                // Get list of paused groups, add this group (if already paused, return), and update
                Map<String, Object> pauseMap = getOrCreateRepo(getPausedTriggerGroupNamesRepoId());
                List<String> pausedGroups = (List<String>) pauseMap.get("paused");
                if (pausedGroups == null) {
                    pausedGroups = new ArrayList<String>();
                    pauseMap.put("paused", pausedGroups);
                } else if (pausedGroups.contains(groupName)) {
                    return;
                }
                pausedGroups.add(groupName);
                String rev = (String)pauseMap.get("_rev");
                r = Requests.newUpdateRequest(getPausedTriggerGroupNamesRepoId(), new JsonValue(pauseMap));
                r.setRevision(rev);
                getConnectionFactory().getConnection().update(getContext(), r);

                List<String> triggerNames = tgw.getTriggerNames();
                for (String triggerName : triggerNames) {
                    pauseTrigger(context, triggerName, groupName);
                }
            } catch (JsonValueException e) {
                logger.warn("Error pausing trigger group {}", groupName, e);
                throw new JobPersistenceException("Error pausing trigger group", e);
            } catch (Exception e) {
                logger.warn("Error pausing trigger group {}", groupName, e);
                throw new JobPersistenceException("Error pausing trigger group", e);
            }
        }
    }

    @Override
    public boolean removeCalendar(SchedulingContext context, String name)
            throws JobPersistenceException {
        synchronized (lock) {
            List<TriggerWrapper> twList = getTriggerWrappersForCalendar(name);
            if (twList.size() > 0) {
                throw new JobPersistenceException("Calender cannot be removed if it is referenced by a trigger!");
            }
            // Delete calendar
            logger.debug("Deleting calendar {}", name);
            try {
                CalendarWrapper cw = getCalendarWrapper(name);
                if (cw != null) {
                    DeleteRequest r = Requests.newDeleteRequest(getCalendarsRepoId(name));
                    r.setRevision(cw.getRevision());
                    getConnectionFactory().getConnection().delete(getContext(), r);
                    return true;
                } else {
                    return false;
                }
            } catch (ResourceException e) {
                logger.warn("Error removing calendar {}", name, e);
                throw new JobPersistenceException("Error deleting calendar", e);
            } catch (Exception e) {
                logger.warn("Error removing calendar {}", name, e);
                throw new JobPersistenceException("Error deleting calendar", e);
            }
        }
    }

    @Override
    public boolean removeJob(SchedulingContext arg0, String jobName, String groupName)
            throws JobPersistenceException {
        synchronized (lock) {
            String jobId = getJobsRepoId(groupName, jobName);
            try {
                // Get job group
                JobGroupWrapper jgw = getOrCreateJobGroupWrapper(groupName);
                List<String> jobNames = jgw.getJobNames();
                // Check if job name exists
                if (jobNames.contains(jobName)) {
                    // Remove job from list
                    jgw.removeJob(jobName);
                    // Update job group
                    UpdateRequest r = Requests.newUpdateRequest(getJobGroupsRepoId(groupName), jgw.getValue());
                    r.setRevision(jgw.getRevision());
                    getConnectionFactory().getConnection().update(getContext(), r);
                }
                // Delete job
                JobWrapper oldJw = getJobWrapper(groupName, jobName);
                if (oldJw == null) {
                   return false;
                }
                logger.debug("Deleting job {} in group {}", jobName, groupName);
                DeleteRequest r = Requests.newDeleteRequest(jobId);
                r.setRevision(oldJw.getRevision());
                getConnectionFactory().getConnection().delete(getContext(), r);
                return true;
            } catch (ResourceException e) {
                logger.warn("Error removing job {} ", jobName, e);
                throw new JobPersistenceException("Error removing job", e);
            } catch (Exception e) {
                logger.warn("Error removing job {} ", jobName, e);
                throw new JobPersistenceException("Error removing job", e);
            }
        }
    }

    @Override
    public boolean removeTrigger(SchedulingContext context, String triggerName, String groupName)
            throws JobPersistenceException {
        synchronized (lock) {
            String triggerId = getTriggersRepoId(groupName, triggerName);
            try {
                // Get trigger group
                TriggerGroupWrapper tgw = getOrCreateTriggerGroupWrapper(groupName);
                String rev = tgw.getRevision();
                List<String> triggerNames = tgw.getTriggerNames();
                // Check if trigger name exists
                if (triggerNames.contains(triggerName)) {
                    // Remove trigger from list
                    tgw.removeTrigger(triggerName);
                    // Update trigger group
                    UpdateRequest r = Requests.newUpdateRequest(getTriggerGroupsRepoId(groupName), tgw.getValue());
                    r.setRevision(tgw.getRevision());
                    getConnectionFactory().getConnection().update(getContext(), r);
                }
                // Attempt to remove from waiting triggers
                TriggerWrapper tw = getTriggerWrapper(groupName, triggerName);
                if (tw != null)  {
                    removeWaitingTrigger(tw.getTrigger());
                    removeAcquiredTrigger(tw.getTrigger(), instanceId);

                    // Delete trigger
                    rev = tw.getRevision();
                    logger.debug("Deleting trigger {} in group {}", triggerName, groupName);
                    DeleteRequest r = Requests.newDeleteRequest(triggerId);
                    r.setRevision(rev);
                    getConnectionFactory().getConnection().delete(getContext(), r);

                    String jobName = tw.getTrigger().getJobName();
                    JobWrapper jw = getJobWrapper(groupName, jobName);
                    if (jw != null) {
                        if (!jw.getJobDetail().isDurable()) {
                            String jobId = getJobsRepoId(groupName, jobName);
                            // Get job group
                            JobGroupWrapper jgw = getOrCreateJobGroupWrapper(groupName);
                            List<String> jobNames = jgw.getJobNames();
                            // Check if job name exists
                            if (jobNames.contains(jobName)) {
                                // Remove job from list
                                jgw.removeJob(jobName);
                                // Update job group
                                UpdateRequest ru = Requests.newUpdateRequest(getJobGroupsRepoId(groupName), jgw.getValue());
                                ru.setRevision(jgw.getRevision());
                                getConnectionFactory().getConnection().update(getContext(), ru);
                            }
                            // Delete job
                            logger.debug("Deleting job {} in group {}", jobName, groupName);
                            r = Requests.newDeleteRequest(jobId);
                            r.setRevision(jw.getRevision());
                            getConnectionFactory().getConnection().delete(getContext(), r);
                        }
                    }
                    return true;
                }
                return false;
            } catch (Exception e) {
                logger.warn("Error removing trigger {} ", triggerName, e);
                throw new JobPersistenceException("Error removing trigger", e);
            }
        }
    }

    @Override
    public boolean replaceTrigger(SchedulingContext context, String triggerName, String groupName,
            Trigger newTrigger)  throws JobPersistenceException {
        synchronized (lock) {
            boolean deleted = false;
            Trigger oldTrigger = null;
            TriggerWrapper tw = getTriggerWrapper(groupName, triggerName);
            if (tw != null) {
                oldTrigger = tw.getTrigger();
                if (!oldTrigger.getJobName().equals(newTrigger.getJobName())
                        || !oldTrigger.getJobGroup().equals(newTrigger.getJobGroup())) {
                    throw new JobPersistenceException("Error replacing trigger, new trigger references a different job");
                }
                logger.debug("Replacing trigger {} in group {} with trigger {} in group {}", triggerName, groupName, newTrigger.getName(), groupName);
                deleted = removeTrigger(context, triggerName, groupName);
            }
            try {
                storeTrigger(context, newTrigger, false);
            } catch (JobPersistenceException e) {
                logger.warn("Error replacing trigger {}, restoring old trigger", triggerName, e);
                if (oldTrigger != null) {
                    storeTrigger(context, oldTrigger, false);
                }
                throw e;
            }
            return deleted;
        }

    }

    @Override
    public void resumeAll(SchedulingContext context)
            throws JobPersistenceException {
        Set<String> names = getPausedTriggerGroups(context);
        for (String name : names) {
            resumeTriggerGroup(context, name);
        }
    }

    @Override
    public void resumeJob(SchedulingContext context, String jobName, String groupName)
            throws JobPersistenceException {
        synchronized (lock) {
            Trigger[] triggers = getTriggersForJob(context, jobName, groupName);
            for (Trigger trigger : triggers) {
                resumeTrigger(context, trigger.getName(), trigger.getGroup());
            }
        }
    }

    @Override
    public void resumeJobGroup(SchedulingContext context, String groupName)
            throws JobPersistenceException {
        synchronized (lock) {
            try {
                // Get job group, resume, and update
                JobGroupWrapper jgw = getOrCreateJobGroupWrapper(groupName);
                jgw.resume();
                String rev = jgw.getRevision();
                UpdateRequest r = Requests.newUpdateRequest(getJobGroupsRepoId(groupName), jgw.getValue());
                r.setRevision(rev);
                getConnectionFactory().getConnection().update(getContext(), r);

                // Get list of paused groups, remove this group (if not paused, return), and update
                Map<String, Object> pauseMap = getOrCreateRepo(getPausedJobGroupNamesRepoId());
                List<String> pausedGroups = (List<String>) pauseMap.get("paused");
                if (pausedGroups == null || !pausedGroups.contains(groupName)) {
                    return;
                }
                pausedGroups.remove(groupName);
                rev = (String)pauseMap.get("_rev");
                r = Requests.newUpdateRequest(getPausedJobGroupNamesRepoId(), new JsonValue(pauseMap));
                r.setRevision(rev);
                getConnectionFactory().getConnection().update(getContext(), r);

                List<String> jobNames = jgw.getJobNames();
                for (String jobName : jobNames) {
                    resumeJob(context, jobName, groupName);
                }
            } catch (JsonValueException e) {
                logger.warn("Error resuming job group {}", groupName, e);
                throw new JobPersistenceException("Error resuming job group", e);
            } catch (Exception e) {
                logger.warn("Error resuming job group {}", groupName, e);
                throw new JobPersistenceException("Error resuming job group", e);
            }
        }
    }

    @Override
    public void resumeTrigger(SchedulingContext arg0, String triggerName, String triggerGroup)
            throws JobPersistenceException {
        synchronized (lock) {
            TriggerWrapper tw = getTriggerWrapper(triggerGroup, triggerName);
            if (tw == null) {
                logger.warn("Cannot resume trigger {} in group {}, trigger does not exist", triggerName, triggerGroup);
                return;
            }
            Trigger trigger;
            try {
                trigger = tw.getTrigger();
            } catch (Exception e) {
                logger.warn("Error deserializing trigger", e);
                throw new JobPersistenceException("Error deserializing trigger", e);
            }
            tw.resume();
            // Update the trigger
            updateTriggerInRepo(triggerGroup, triggerName, tw, tw.getRevision());
            // Add trigger to waitingTriggers
            addWaitingTrigger(trigger);
        }
    }

    @Override
    public void resumeTriggerGroup(SchedulingContext context, String groupName)
            throws JobPersistenceException {
        synchronized (lock) {
            try {
                // Get trigger group, resume, and update
                TriggerGroupWrapper tgw = getOrCreateTriggerGroupWrapper(groupName);
                tgw.resume();
                String rev = tgw.getRevision();
                UpdateRequest r = Requests.newUpdateRequest(getTriggerGroupsRepoId(groupName), tgw.getValue());
                r.setRevision(rev);
                getConnectionFactory().getConnection().update(getContext(), r);

                // Get list of paused groups, remove this group (if not present, return), and update
                Map<String, Object> pauseMap = getOrCreateRepo(getPausedTriggerGroupNamesRepoId());
                List<String> pausedGroups = (List<String>) pauseMap.get("paused");
                if (pausedGroups == null) {
                    pausedGroups = new ArrayList<String>();
                    pauseMap.put("paused", pausedGroups);
                } else if (pausedGroups.contains(groupName)) {
                    pausedGroups.remove(groupName);
                }
                rev = (String)pauseMap.get("_rev");
                r = Requests.newUpdateRequest(getPausedTriggerGroupNamesRepoId(), new JsonValue(pauseMap));
                r.setRevision(rev);
                getConnectionFactory().getConnection().update(getContext(), r);

                List<String> triggerNames = tgw.getTriggerNames();
                for (String triggerName : triggerNames) {
                    resumeTrigger(context, triggerName, groupName);
                }
            } catch (JsonValueException e) {
                logger.warn("Error pausing trigger group", groupName, e);
                throw new JobPersistenceException("Error deserializing trigger", e);
            } catch (Exception e) {
                logger.warn("Error pausing trigger group", groupName, e);
                throw new JobPersistenceException("Error pausing trigger group", e);
            }
        }
    }

    @Override
    public Calendar retrieveCalendar(SchedulingContext context, String name)
            throws JobPersistenceException {
        synchronized (lock) {
            if (name != null) {
                CalendarWrapper cw = getCalendarWrapper(name);
                if (cw != null) {
                    try {
                        return cw.getCalendar();
                    } catch (Exception e) {
                        logger.warn("Error retrieving calendar", e);
                        throw new JobPersistenceException("Error retrieving calendar", e);
                    }
                }
            }
            return null;
        }
    }

    @Override
    public JobDetail retrieveJob(SchedulingContext context, String jobName,
            String jobGroup) throws JobPersistenceException {
        synchronized (lock) {
            if (logger.isTraceEnabled()) {
                logger.trace("Getting job {}", getJobsRepoId(jobGroup, jobName));
            }
            JobWrapper jw = getJobWrapper(jobGroup, jobName);
            if (jw == null) {
                return null;
            }
            try {
                return jw.getJobDetail();
            } catch (Exception e) {
                logger.warn("Error retrieving job", e);
                throw new JobPersistenceException("Error retrieving job", e);
            }
        }
    }

    public JobWrapper getJobWrapper(String jobGroup, String jobName) throws JobPersistenceException {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("Getting job {}", getJobsRepoId(jobGroup, jobName));
            }
            Map<String, Object> jobMap = readFromRepo(getJobsRepoId(jobGroup, jobName)).asMap();
            if (jobMap == null) {
                return null;
            }
            JobWrapper jw = new JobWrapper(jobMap);
            return jw;
        } catch (ResourceException e) {
            logger.warn("Error retrieving job", e);
            throw new JobPersistenceException("Error retrieving job", e);
        } catch (Exception e) {
            logger.warn("Error retrieving job", e);
            throw new JobPersistenceException("Error retrieving job", e);
        }
    }

    public CalendarWrapper getCalendarWrapper(String name)
            throws JobPersistenceException {
        synchronized (lock) {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace("Getting calendar {}", getCalendarsRepoId(name));
                }
                Map<String, Object> calMap = readFromRepo(getCalendarsRepoId(name)).asMap();
                if (calMap == null) {
                    return null;
                }
                CalendarWrapper cal = new CalendarWrapper(calMap);
                return cal;
            } catch (ResourceException e) {
                logger.warn("Error retrieving calendar", e);
                throw new JobPersistenceException("Error retrieving calendar", e);
            } catch (Exception e) {
                logger.warn("Error retrieving calendar", e);
                throw new JobPersistenceException("Error retrieving calendar", e);
            }
        }
    }

    @Override
    public Trigger retrieveTrigger(SchedulingContext context, String triggerName, String triggerGroup)
            throws JobPersistenceException {
        synchronized (lock) {
            try {
                TriggerWrapper tw = getTriggerWrapper(triggerGroup, triggerName);
                if (tw == null) {
                    return null;
                }
                return tw.getTrigger();
            } catch (Exception e) {
                logger.warn("Error retrieving trigger", e);
                throw new JobPersistenceException("Error retrieving trigger", e);
            }
        }
    }

    @Override
    public TriggerFiredBundle triggerFired(SchedulingContext context, Trigger trigger)
            throws JobPersistenceException {
        synchronized (lock) {
            logger.debug("Trigger {} has fired", trigger.getFullName());
            TriggerWrapper tw;
            try {
                tw = getTriggerWrapper(trigger.getGroup(), trigger.getName());
            } catch (Exception e) {
                logger.warn("Error setting trigger fired", e);
                throw new JobPersistenceException("Error setting trigger fired", e);
            }
            if (tw == null) {
                logger.warn("Error setting trigger fired, trigger does not exist");
                return null;
            }
            if (!tw.isAcquired()) {
                logger.warn("Error setting trigger fired, trigger was not in acquired state");
            }
            Trigger localTrigger;
            try {
                localTrigger = tw.getTrigger();
            } catch (Exception e) {
                logger.warn("Error setting trigger fired", e);
                throw new JobPersistenceException("Error setting trigger fired", e);
            }
            Calendar triggerCalendar = null;
            if (localTrigger.getCalendarName() != null) {
                CalendarWrapper cw = getCalendarWrapper(localTrigger.getCalendarName());
                if (cw == null) {
                    logger.warn("Error setting trigger fired, cannot find trigger's calendar");
                    return null;
                } else {
                    try {
                        triggerCalendar = cw.getCalendar();
                    } catch (Exception e) {
                        logger.warn("Error retrieving calendar", e);
                        throw new JobPersistenceException("Error retrieving calendar", e);
                    }
                }
            }

            Date previousFireTime = trigger.getPreviousFireTime();
            removeWaitingTrigger(trigger);

            localTrigger.triggered(triggerCalendar);
            tw.updateTrigger(localTrigger);
            updateTriggerInRepo(localTrigger.getGroup(), localTrigger.getName(), tw, tw.getRevision());

            trigger.triggered(triggerCalendar);

            // Set trigger into the normal/waiting state
            tw.setState(Trigger.STATE_NORMAL);
            TriggerFiredBundle tfb = new TriggerFiredBundle(retrieveJob(context, trigger.getJobName(),
                    trigger.getJobGroup()),
                    trigger,
                    triggerCalendar,
                    false,
                    new Date(),
                    trigger.getPreviousFireTime(),
                    previousFireTime,
                    trigger.getNextFireTime());

            JobDetail job = tfb.getJobDetail();

            if (job.isStateful()) {
                Trigger[] triggers = getTriggersForJob(context, job.getName(), job.getGroup());
                for (Trigger t : triggers) {
                    TriggerWrapper tmpTw = getTriggerWrapper(t.getGroup(), t.getName());
                    if (tmpTw != null) {
                        if (tmpTw.getState() == Trigger.STATE_NORMAL || tmpTw.getState() == Trigger.STATE_PAUSED) {
                            tmpTw.block();
                        }
                        // update trigger in repo
                        updateTriggerInRepo(t.getGroup(), tmpTw.getName(), tmpTw, tmpTw.getRevision());
                        removeWaitingTrigger(t);
                    }
                }
                blockedJobs.add(getJobNameKey(job));
            } else if (localTrigger.getNextFireTime() != null) {
                addWaitingTrigger(localTrigger);
            }
            return tfb;
        }
    }

    @Override
    public void triggeredJobComplete(SchedulingContext context, Trigger trigger,
            JobDetail jobDetail, int triggerInstCode) throws JobPersistenceException {
        synchronized(lock) {
            logger.debug("Job {} has completed", jobDetail.getFullName());
            String jobKey = getJobNameKey(jobDetail);
            JobWrapper jw = getJobWrapper(jobDetail.getGroup(), jobDetail.getName());
            JsonValue triggerValue = getTriggerFromRepo(trigger.getGroup(), trigger.getName());
            TriggerWrapper tw = null;
            if (triggerValue != null && !triggerValue.isNull()) {
                tw = new TriggerWrapper(triggerValue.asMap());
            }

            // Remove the acquired trigger (if acquired)
            removeAcquiredTrigger(trigger, instanceId);

            if (jw != null) {
                JobDetail jd;
                try {
                    jd = jw.getJobDetail();
                } catch (Exception e) {
                    throw new JobPersistenceException("Error triggering job complete", e);
                }
                if (jd.isStateful()) {
                    JobDataMap newData = jobDetail.getJobDataMap();
                    if (newData != null) {
                        newData = (JobDataMap)newData.clone();
                        newData.clearDirtyFlag();
                    }
                    jd.setJobDataMap(newData);
                    blockedJobs.remove(getJobNameKey(jd));
                    Trigger[] triggers = getTriggersForJob(context, jd.getName(), jd.getGroup());
                    for (Trigger t : triggers) {
                        TriggerWrapper tmpTw = getTriggerWrapper(t.getGroup(), t.getName());
                        if (tmpTw != null) {
                            if (tmpTw.getState() == Trigger.STATE_BLOCKED) {
                                tmpTw.unblock();
                            }
                            // update trigger in repo
                            updateTriggerInRepo(t.getGroup(), tmpTw.getName(), tmpTw, tmpTw.getRevision());
                            if (!tmpTw.isPaused()) {
                                addWaitingTrigger(t);
                            }
                        }
                    }
                    schedulerSignaler.signalSchedulingChange(0L);
                }
            } else {
                blockedJobs.remove(jobKey);
            }

            if (tw != null) {
                if (triggerInstCode == Trigger.INSTRUCTION_DELETE_TRIGGER) {
                    if (trigger.getNextFireTime() == null) {
                        if (tw.getTrigger().getNextFireTime() == null) {
                            removeTrigger(context, trigger.getName(), trigger.getGroup());
                        }
                    } else {
                        removeTrigger(context, trigger.getName(), trigger.getGroup());
                        schedulerSignaler.signalSchedulingChange(0L);
                    }
                } else if (triggerInstCode == Trigger.INSTRUCTION_SET_TRIGGER_COMPLETE) {
                    tw.setState(Trigger.STATE_COMPLETE);
                    removeWaitingTrigger(tw.getTrigger());
                    schedulerSignaler.signalSchedulingChange(0L);
                } else if (triggerInstCode == Trigger.INSTRUCTION_SET_TRIGGER_ERROR) {
                    logger.debug("Trigger {} set to ERROR state.", trigger.getFullName());
                    tw.setState(Trigger.STATE_ERROR);
                    schedulerSignaler.signalSchedulingChange(0L);
                } else if (triggerInstCode == Trigger.INSTRUCTION_SET_ALL_JOB_TRIGGERS_ERROR) {
                    logger.debug("All triggers of Job {} set to ERROR state.", trigger.getFullJobName());
                    setAllTriggersOfJobToState(trigger.getJobName(), trigger.getJobGroup(), Trigger.STATE_ERROR);
                    schedulerSignaler.signalSchedulingChange(0L);
                } else if (triggerInstCode == Trigger.INSTRUCTION_SET_ALL_JOB_TRIGGERS_COMPLETE) {
                    setAllTriggersOfJobToState(trigger.getJobName(), trigger.getJobGroup(), Trigger.STATE_COMPLETE);
                    schedulerSignaler.signalSchedulingChange(0L);
                }
            }
        }

    }
    
    private CreateRequest getCreateRequest(String id, Map<String, Object> map) {
        String container = id.substring(0,id.lastIndexOf("/"));
        String newId = id.substring(id.lastIndexOf("/")+1);
        return Requests.newCreateRequest(container, newId, new JsonValue(map));
    }

    private JsonValue readFromRepo(String repoId) throws ResourceException {
        try {
            return getConnectionFactory().getConnection().read(getContext(), Requests.newReadRequest(repoId)).getContent();
        } catch (JobPersistenceException e) {
            return new JsonValue(null);
        } catch (NotFoundException e) {
            return new JsonValue(null);
        }
    }

    /**
     * Returns the misfire threshold for this JobStore
     *
     * @return the misfire threshold
     */
    public long getMisfireThreshold() {
        return misfireThreshold;
    }

    /**
     * Sets the misfire threshold for this JobStore
     *
     * @param misfireThreshold the misfire threshold
     */
    public void setMisfireThreshold(long misfireThreshold) {
        this.misfireThreshold = misfireThreshold;
    }

    /**
     * Sets all the Triggers of a Job to the same state.
     *
     * @param jobName   the name of the Job
     * @param jobGroup  the name of the Job Group
     * @param state     the state
     * @throws JobPersistenceException
     */
    protected void setAllTriggersOfJobToState(String jobName, String jobGroup, int state)
            throws JobPersistenceException {
        synchronized (lock) {
            Trigger[] triggers = getTriggersForJob(null, jobName, jobGroup);
            for (Trigger t : triggers) {
                TriggerWrapper tw = new TriggerWrapper(getTriggerFromRepo(
                        t.getGroup(), t.getName()).asMap());
                tw.setState(state);
                if (state != Trigger.STATE_NORMAL) {
                    removeWaitingTrigger(tw.getTrigger());
                }
            }
        }
    }

    /**
     * Gets a Trigger group from the repo and wraps it in a TriggerGroupWapper().
     * Creates the group if it doesn't already exist
     *
     * @param groupName name of group
     * @return a trigger group wrapped in a TriggerGroupWrapper
     * @throws ObjectSetException
     */
    private TriggerGroupWrapper getOrCreateTriggerGroupWrapper(String groupName)
            throws JobPersistenceException, ResourceException {
        synchronized (lock) {
            Map<String, Object> map;

            map = readFromRepo(getTriggerGroupsRepoId(groupName)).asMap();

            TriggerGroupWrapper tgw = null;
            if (map == null) {
                // create if null
                tgw = new TriggerGroupWrapper(groupName);
                // create in repo
                JsonValue newValue = getConnectionFactory().getConnection().create(getContext(),
                        getCreateRequest(getTriggerGroupsRepoId(groupName), tgw.getValue().asMap())).getContent();
                tgw = new TriggerGroupWrapper(newValue);
                // Add to list of group names
                addTriggerGroupName(groupName);
            } else {
                // else build from map
                tgw = new TriggerGroupWrapper(map);
            }
            return tgw;
        }
    }

    /**
     * Gets a Job group from the repo and wraps it in a JobGroupWapper().
     * Creates the group if it doesn't already exist
     *
     * @param groupName name of group
     * @return a job group wrapped in a JobGroupWrapper
     * @throws ObjectSetException
     */
    private JobGroupWrapper getOrCreateJobGroupWrapper(String groupName)
            throws JobPersistenceException, ResourceException {
        synchronized (lock) {
            Map<String, Object> map;

            map = readFromRepo(getJobGroupsRepoId(groupName)).asMap();

            JobGroupWrapper jgw = null;
            if (map == null) {
                // create if null
                jgw = new JobGroupWrapper(groupName);
                // create in repo
                JsonValue newValue = getConnectionFactory().getConnection().create(getContext(),
                        getCreateRequest(getJobGroupsRepoId(groupName), jgw.getValue().asMap())).getContent();
                jgw = new JobGroupWrapper(newValue);
                // Add to list of group names
                addJobGroupName(groupName);
            } else {
                // else build from map
                jgw = new JobGroupWrapper(map);
            }
            return jgw;
        }
    }

    /**
     * Adds a Trigger to the list of waiting triggers.
     *
     * @param trigger   the Trigger to add
     * @throws JobPersistenceException
     * @throws ObjectSetException
     */
    private void addWaitingTrigger(Trigger trigger) throws JobPersistenceException {
        synchronized (lock) {
            try {
                int retries = 0;
                while (writeRetries == -1 || retries <= writeRetries && !shutdown) {
                    try {
                        // update repo
                        addRepoListName(getTriggerId(trigger.getGroup(), trigger.getName()),
                                getWaitingTriggersRepoId(), "names");
                        break;
                    } catch (PreconditionFailedException e) {
                        logger.debug("Adding waiting trigger failed {}, retrying", e);
                        retries++;
                    }
                }
            } catch (ResourceException e) {
                throw new JobPersistenceException("Error adding waiting trigger", e);
            }
        }
    }

    /**
     * Removes a Trigger from the list of waiting triggers.
     *
     * @param trigger   the Trigger to remove
     * @throws JobPersistenceException
     * @throws ObjectSetException
     */
    private boolean removeWaitingTrigger(Trigger trigger) throws JobPersistenceException {
        synchronized (lock) {
            try {
                boolean result = false;
                int retries = 0;
                while (writeRetries == -1 || retries <= writeRetries && !shutdown) {
                    try {
                        result = removeRepoListName(getTriggerId(trigger.getGroup(), trigger.getName()),
                                getWaitingTriggersRepoId(), "names");
                        break;
                    } catch (PreconditionFailedException e) {
                        logger.debug("Removing waiting trigger failed {}, retrying", e);
                        retries++;
                    }
                }
                return result;
            } catch (ResourceException e) {
                throw new JobPersistenceException("Error removing waiting trigger", e);
            }
        }
    }

    /**
     * Adds a Trigger to the list of acquired triggers.
     *
     * @param trigger    the Trigger to add
     * @param instanceId the instance ID
     * @throws JobPersistenceException
     * @throws ObjectSetException
     */
    private void addAcquiredTrigger(Trigger trigger, String instanceId) throws JobPersistenceException {
        synchronized (lock) {
            try {
                logger.debug("Adding acquired trigger {} for instance {}", trigger.getName(), instanceId);
                int retries = 0;
                while (writeRetries == -1 || retries <= writeRetries && !shutdown) {
                    try {
                        addRepoListName(getTriggerId(trigger.getGroup(), trigger.getName()),
                                getAcquiredTriggersRepoId(), instanceId);
                        break;
                    } catch (PreconditionFailedException e) {
                        logger.debug("Adding acquired trigger failed {}, retrying", e);
                        retries++;
                    }
                }
            } catch (ResourceException e) {
                throw new JobPersistenceException("Error adding waiting trigger", e);
            }
        }
    }

    /**
     * Removes a Trigger from the list of acquired triggers.
     *
     * @param trigger    the Trigger to remove
     * @param instanceId the instance ID
     * @throws JobPersistenceException
     * @throws ObjectSetException
     */
    private boolean removeAcquiredTrigger(Trigger trigger, String instanceId) throws JobPersistenceException {
        synchronized (lock) {
            try {
                logger.debug("Removing acquired trigger {} for instance {}", trigger.getName(), instanceId);
                boolean result = false;
                int retries = 0;
                while (writeRetries == -1 || retries <= writeRetries && !shutdown) {
                    try {
                        result = removeRepoListName(getTriggerId(trigger.getGroup(), trigger.getName()),
                                getAcquiredTriggersRepoId(), instanceId);
                        break;
                    } catch (PreconditionFailedException e) {
                        logger.debug("Removing acquired trigger failed {}, retrying", e);
                        retries++;
                    }
                }
                return result;
            } catch (ResourceException e) {
                throw new JobPersistenceException("Error removing waiting trigger", e);
            }
        }
    }

    /**
     * Returns the an AcquiredTriggers object which wraps the List of all triggers in the "acquired" state
     *
     * @param instanceId    the ID of the instance that acquired the triggers
     * @return  the WaitingTriggers object
     * @throws JobPersistenceException
     */
    private AcquiredTriggers getAcquiredTriggers(String instanceId) throws JobPersistenceException {
        List<Trigger> acquiredTriggers = new ArrayList<Trigger>();
        List<String> acquiredTriggerIds = new ArrayList<String>();
        String repoId = getAcquiredTriggersRepoId();
        String revision = null;
        Map<String, Object> map;
        try {
            try {
                map = getConnectionFactory().getConnection().read(getContext(), Requests.newReadRequest(repoId)).getContent().asMap();
            } catch (NotFoundException e) {
                logger.trace("repo list {} not found, lets create it", "names");
                map = null;
            }
            if (map == null) {
                map = new HashMap<String, Object>();
                map.put(instanceId, acquiredTriggerIds);
                // create in repo
                map = getConnectionFactory().getConnection().create(getContext(), getCreateRequest(repoId, map)).getContent().asMap();
                revision = (String)map.get("_rev");
            } else {
                // else check if list exists in map
                acquiredTriggerIds = (List<String>) map.get(instanceId);
                revision = (String)map.get("_rev");
                if (acquiredTriggerIds == null) {
                    acquiredTriggerIds = new ArrayList<String>();
                    map.put(instanceId, acquiredTriggerIds);
                    UpdateRequest r = Requests.newUpdateRequest(repoId, new JsonValue(map));
                    r.setRevision(revision);
                    JsonValue updatedValue = getConnectionFactory().getConnection().update(getContext(), r).getContent();;
                    revision = (String) updatedValue.asMap().get("_rev");
                }
            }
            for (String id : acquiredTriggerIds) {
                TriggerWrapper tw = getTriggerWrapper(getGroupFromId(id), getNameFromId(id));
                if (tw == null) {
                    logger.warn("Could not add {} to list of waiting Triggers. Trigger not found in repo", id);
                } else {
                    logger.trace("Found acquired trigger {} in group {}", tw.getName(),tw.getGroup());
                    acquiredTriggers.add(tw.getTrigger());
                }
            }
            return new AcquiredTriggers(acquiredTriggers, revision);
        } catch (ResourceException e) {
            logger.warn("Error initializing waiting triggers", e);
            throw new JobPersistenceException("Error initializing waiting triggers", e);
        }
    }

    /**
     * Returns the a WaitingTriggers object which wraps the Tree of all triggers in the "waiting" state
     *
     * @return  the WaitingTriggers object
     * @throws JobPersistenceException
     */
    private WaitingTriggers getWaitingTriggers() throws JobPersistenceException {
        TreeSet<Trigger> waitingTriggers = new TreeSet(new TriggerComparator());
        List<String> waitingTriggersRepoList = null;
        String repoId = getWaitingTriggersRepoId();
        String revision = null;
        Map<String, Object> map;
        try {
            try {
                map = getConnectionFactory().getConnection().read(getContext(), Requests.newReadRequest(repoId)).getContent().asMap();
            } catch (NotFoundException e) {
                logger.debug("repo list {} not found, lets create it", "names");
                map = null;
            }
            if (map == null) {
                map = new HashMap<String, Object>();
                waitingTriggersRepoList = new ArrayList<String>();
                map.put("names", waitingTriggersRepoList);
                // create in repo
                map = getConnectionFactory().getConnection().create(getContext(), getCreateRequest(repoId, map)).getContent().asMap();
                revision = (String)map.get("_rev");
            } else {
                // else check if list exists in map
                waitingTriggersRepoList = (List<String>) map.get("names");
                revision = (String)map.get("_rev");
                if (waitingTriggersRepoList == null) {
                    waitingTriggersRepoList = new ArrayList<String>();
                    map.put("names", waitingTriggersRepoList);
                    UpdateRequest r = Requests.newUpdateRequest(repoId, new JsonValue(map));
                    r.setRevision(revision);
                    JsonValue updatedValue = getConnectionFactory().getConnection().update(getContext(), r).getContent();
                    revision = (String) updatedValue.asMap().get("_rev");
                }
            }
            for (String id : waitingTriggersRepoList) {
                TriggerWrapper tw = getTriggerWrapper(getGroupFromId(id), getNameFromId(id));
                if (tw == null) {
                    logger.warn("Could not add {} to list of waiting Triggers. Trigger not found in repo", id);
                } else {
                    logger.debug("Found waiting trigger {} in group {}", tw.getName(),tw.getGroup());
                    waitingTriggers.add(tw.getTrigger());
                }
            }
            return new WaitingTriggers(waitingTriggers, revision);
        } catch (ResourceException e) {
            logger.warn("Error initializing waiting triggers", e);
            throw new JobPersistenceException("Error initializing waiting triggers", e);
        }
    }

    /**
     * Adds a Trigger group name to the list of Trigger group names
     *
     * @param groupName the name to add
     * @throws JobPersistenceException
     * @throws ObjectSetException
     */
    private void addTriggerGroupName(String groupName)
            throws JobPersistenceException, ResourceException {
        addRepoListName(groupName, getTriggerGroupNamesRepoId(), "names");
    }

    /**
     * Adds a Job group name to the list of Job group names
     *
     * @param groupName the name to add
     * @throws JobPersistenceException
     * @throws ObjectSetException
     */
    private void addJobGroupName(String groupName)
            throws JobPersistenceException, ResourceException {
        addRepoListName(groupName, getJobGroupNamesRepoId(), "names");
    }

    /**
     * Adds a name to a list of names in the repo.
     *
     * @param name  the name to add
     * @param id    the repo id
     * @throws JobPersistenceException
     * @throws ObjectSetException
     */
    private void addRepoListName(String name, String id, String list)
            throws JobPersistenceException, ResourceException {
        synchronized (lock) {
            logger.trace("Adding name: {} to {}", name, id);
            Map<String, Object> map = getOrCreateRepo(id);
            String rev = (String)map.get("_rev");

            List<String> names = (List<String>) map.get(list);
            if (names == null) {
                names = new ArrayList<String>();
                map.put(list, names);
            }
            if (!names.contains(name)) {
                names.add(name);
            }
            // update repo
            UpdateRequest r = Requests.newUpdateRequest(id, new JsonValue(map));
            r.setRevision(rev);
            getConnectionFactory().getConnection().update(getContext(), r);
        }

    }

    /**
     * Removes a name from a list of names in the repo.
     *
     * @param name  the name to remove
     * @param id    the repo id
     * @return  true if the name was removed, false otherwise (the name may not have been present)
     * @throws JobPersistenceException
     * @throws ObjectSetException
     */
     private boolean removeRepoListName(String name, String id, String list)
            throws JobPersistenceException, ResourceException {
        synchronized (lock) {
            logger.trace("Removing name: {} from {}", name, id);
            Map<String, Object> map = getOrCreateRepo(id);
            String rev = (String)map.get("_rev");

            List<String> names = (List<String>) map.get(list);
            if (names == null) {
                names = new ArrayList<String>();
                map.put(list, names);
            }
            boolean result = names.remove(name);
            if (result) {
                // update repo
                UpdateRequest r = Requests.newUpdateRequest(id, new JsonValue(map));
                r.setRevision(rev);
                getConnectionFactory().getConnection().update(getContext(), r);
            }
            return result;
        }

    }


    private Map<String, Object> getOrCreateRepo(String repoId)
            throws JobPersistenceException, ResourceException {
        synchronized (lock) {
            Map<String, Object> map;

            map = readFromRepo(repoId).asMap();

            if (map == null) {
                map = new HashMap<String, Object>();
                // create in repo
                logger.debug("Creating repo {}", repoId);
                map = getConnectionFactory().getConnection().create(getContext(), getCreateRequest(repoId, map)).getContent().asMap();
            }
            return map;
        }
    }

    private List<String> getOrCreateRepoList(String repoId, String listId)
            throws JobPersistenceException, ResourceException {
        synchronized (lock) {
            List<String> list = null;
            Map<String, Object> map;
            String revision = null;
            try {
                map = getConnectionFactory().getConnection().read(getContext(), Requests.newReadRequest(repoId)).getContent().asMap();
            } catch (NotFoundException e) {
                logger.debug("repo list {} not found, lets create it", listId);
                map = null;
            }
            if (map == null) {
                map = new HashMap<String, Object>();
                list = new ArrayList<String>();
                map.put(listId, list);
                // create in repo
                map = getConnectionFactory().getConnection().create(getContext(), getCreateRequest(repoId, map)).getContent().asMap();
            } else {
                // else check if list exists in map
                list = (List<String>) map.get(listId);
                if (list == null) {
                    list = new ArrayList<String>();
                    map.put(listId, list);
                    revision = (String)map.get("_rev");
                    UpdateRequest r = Requests.newUpdateRequest(repoId, new JsonValue(map));
                    r.setRevision(revision);
                    getConnectionFactory().getConnection().update(getContext(), r);
                }
            }
            return list;
        }
    }

    /**
     * Gets a trigger container from the repo as a JsonValue.
     *
     * @param name the name of the trigger
     * @param group the group id of the trigger
     * @return A JsonValue object containing the serialized trigger and other metadata
     * @throws JobPersistenceException
     */
    private JsonValue getTriggerFromRepo(String group, String name)
            throws JobPersistenceException {
        synchronized (lock) {
            try {
                logger.trace("Getting trigger {} in group {} from repo", name, group);
                return readFromRepo(getTriggersRepoId(group, name));
            } catch (ResourceException e) {
                logger.warn("Error getting trigger from repo", e);
                throw new JobPersistenceException("Error getting trigger from repo", e);
            }
        }
    }

    /**
     * Updates a trigger in the repo.
     *
     * @param name the name of the trigger
     * @param group the group id of the trigger
     * @param tw the TriggerWrapper representing the updated trigger
     * @throws JobPersistenceException
     */
    private void updateTriggerInRepo(String group, String name, TriggerWrapper tw, String rev)
            throws JobPersistenceException {
        synchronized (lock) {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace("Getting trigger {}", getTriggersRepoId(group, name));
                }
                String repoId = getTriggersRepoId(group, name);
                UpdateRequest r = Requests.newUpdateRequest(repoId, tw.getValue());
                r.setRevision(rev);
                getConnectionFactory().getConnection().update(getContext(), r);
            } catch (ResourceException e) {
                logger.warn("Error updating trigger in repo", e);
                throw new JobPersistenceException("Error updating trigger in repo", e);
            }
        }
    }

    /**
     * Gets a trigger as a TriggerWrapper object.
     *
     * @param group the group id of the trigger
     * @param name the name of the trigger
     * @return
     * @throws JobPersistenceException
     */
    private TriggerWrapper getTriggerWrapper(String group, String name) throws JobPersistenceException {
        JsonValue triggerValue = getTriggerFromRepo(group, name);
        if (triggerValue.isNull()) {
            return null;
        }
        try {
            return new TriggerWrapper(triggerValue.asMap());
        } catch (Exception e) {
            logger.warn("Error getting trigger", e);
            throw new JobPersistenceException("Error getting trigger", e);
        }
    }

    /**
     * Returns the record ID of a fired Trigger.
     *
     * @return  the record ID
     */
    private String getFiredTriggerRecordId() {
        return String.valueOf(ftrCtr.incrementAndGet());
    }

    /**
     * Returns true if a Trigger has misfired, false otherwise.
     *
     * @param trigger   the Trigger to check if misfired
     * @return  true if a Trigger has misfired, false otherwise
     */
    private boolean hasTriggerMisfired(Trigger trigger) {
        long now = System.currentTimeMillis();
        Date nextFireTime = trigger.getNextFireTime();
        if (nextFireTime.getTime() <= (now - misfireThreshold)) {
            return true;
        }
        return false;
    }

    /**
     * Processes a misfired Trigger.
     *
     * @param trigger the Trigger to process
     * @throws JobPersistenceException
     */
    private void processTriggerMisfired(TriggerWrapper triggerWrapper)
            throws JobPersistenceException {
        Trigger trigger = triggerWrapper.getTrigger();
        logger.trace("Signaling Trigger Listener Misfired");
        schedulerSignaler.notifyTriggerListenersMisfired(trigger);
        Calendar calendar = retrieveCalendar(null, trigger.getCalendarName());
        trigger.updateAfterMisfire(calendar);
        triggerWrapper.updateTrigger(trigger);
        updateTriggerInRepo(trigger.getGroup(), trigger.getName(), triggerWrapper, triggerWrapper.getRevision());
        if (trigger.getNextFireTime() == null) {
            schedulerSignaler.notifySchedulerListenersFinalized(trigger);
            triggerWrapper.setState(Trigger.STATE_COMPLETE);
            // update trigger in repo
            updateTriggerInRepo(trigger.getGroup(), trigger.getName(), triggerWrapper, triggerWrapper.getRevision());
            removeWaitingTrigger(trigger);
        }
    }

    /**
     * Returns a job name key used to uniquely identify a specific job.
     *
     * @param jobDetail
     * @return
     */
    private String getJobNameKey(JobDetail jobDetail) {
        return new StringBuilder(jobDetail.getGroup()).append(UNIQUE_ID_SEPARATOR)
                .append(jobDetail.getName()).toString();
    }
    
    /**
     * Cleans up any triggers previously acquired by this instance and processes any misfires.
     */
    private void cleanUpInstance() {
        synchronized (lock) {
            try {
                logger.trace("Cleaning up instance");
                
                // Get the list of all stored triggers
                List<Trigger> storedTriggers = new ArrayList<Trigger>();
                String[] groupNames = getTriggerGroupNames(null);
                for (String groupName : groupNames) {
                    String[] triggerNames = getTriggerNames(null, groupName);
                    for (String triggerName : triggerNames) {
                        storedTriggers.add(getTriggerWrapper(groupName, triggerName).getTrigger());
                    }
                }

                // Ignore triggers which are already present in the waiting list.
                WaitingTriggers wt = getWaitingTriggers();
                TreeSet<Trigger> waitingTriggers = wt.getTriggers();
                for (Trigger t : waitingTriggers) {
                    storedTriggers.remove(t);
                }
                
                // Process and release any triggers which are acquired
                AcquiredTriggers at = getAcquiredTriggers(instanceId);
                List<Trigger> acquiredTriggers = at.getTriggers();
                for (Trigger t : acquiredTriggers) {
                    if (hasTriggerMisfired(t)) {
                        logger.trace("Trigger {} has misfired", t.getName());
                        processTriggerMisfired(getTriggerWrapper(t.getGroup(), t.getName()));
                        if (t.getNextFireTime() != null) {
                            // Remove the trigger from the "acquired" triggers list
                            removeAcquiredTrigger(t, instanceId);
                        }
                    } else {
                        releaseAcquiredTrigger(null, t);
                    }
                }
                
                // Add remaining triggers to the "waiting" triggers tree
                for (Trigger t : storedTriggers) {
                    logger.trace("Adding trigger {} waitingTriggers", t.getName());
                    addWaitingTrigger(t);
                }
            } catch (JobPersistenceException e) {
                logger.warn("Error initializing RepoJobStore", e);
            }
        }
    }

    /**
     * A Comparator used to compare two Triggers
     */
    protected class TriggerComparator implements Comparator {

        public int compare(Object t1, Object t2) {
            Trigger trigger1 = (Trigger)t1;
            Trigger trigger2 = (Trigger)t2;
            // First compare by nextFireTime()
            int result = ((Trigger)t1).compareTo((Trigger)t2);
            if (result == 0) {
                // If that didn't work, compare by priority
                result = trigger2.getPriority() - trigger1.getPriority();
                if (result == 0) {
                    // If that didn't work, compare by name and group
                    result = trigger1.getFullName().compareTo(trigger2.getFullName());
                }
            }
            return result;
        }
    }

    /**
     * A wrapper for the tree of waiting triggers
     */
    protected class WaitingTriggers {

        private TreeSet<Trigger> triggers;
        private String revision;

        public WaitingTriggers(TreeSet<Trigger> triggers, String rev) {
            this.triggers = triggers;
            revision = rev;
        }

        public TreeSet<Trigger> getTriggers() {
            return triggers;
        }

        public String getRevision() {
            return revision;
        }

        public void setRevision(String rev) {
            revision = rev;
        }

        public List<String> getTriggerNamesList() {
            List<String> names = new ArrayList<String>();
            Iterator<Trigger> iterator = triggers.iterator();
            while (iterator.hasNext()) {
                Trigger t = iterator.next();
                names.add(getTriggerId(t.getGroup(), t.getName()));
            }
            return names;
        }

    }

    /**
     * A wrapper for the list of acquired triggers
     */
    protected class AcquiredTriggers {

        private List<Trigger> triggers;
        private String revision;

        public AcquiredTriggers(List<Trigger> triggers, String revision) {
            this.triggers = triggers;
            this.revision = revision;
        }

        public String getRevision() {
            return revision;
        }

        public List<Trigger> getTriggers() {
            return triggers;
        }

        public void setRevision(String rev) {
            revision = rev;
        }

    }

    public SchedulerSignaler getSchedulerSignaler() {
        return schedulerSignaler;
    }

    public void setSchedulerSignaler(SchedulerSignaler schedulerSignaler) {
        this.schedulerSignaler = schedulerSignaler;
    }

    @Override
    public boolean handleEvent(ClusterEvent event) {
        String eventInstanceId = event.getInstanceId();
        switch (event.getType()) {
        case RECOVERY_INITIATED:
            try {
                // Free acquired triggers
                AcquiredTriggers triggers = getAcquiredTriggers(eventInstanceId);
                logger.debug("Found {} acquired triggers while recovering instance {}", triggers.getTriggers().size(), eventInstanceId);
                for (Trigger trigger : triggers.getTriggers()) {
                    boolean removed = false;
                    int retry = 0;
                    // Remove the acquired trigger
                    while (writeRetries == -1 || retry <= writeRetries && !shutdown) {
                        try {
                            removed = removeAcquiredTrigger(trigger, eventInstanceId);
                            break;
                        } catch (JobPersistenceException e) {
                            logger.debug("Failed to remove acquired trigger", e);
                            retry++;
                        }
                    }
                    // Check if trigger was removed
                    if (removed) {
                        // Attempt to add to the trigger to the waiting trigger pool
                        retry = 0;
                        while (writeRetries == -1 || retry <= writeRetries && !shutdown) {
                            try {
                                addWaitingTrigger(trigger);
                                break;
                            } catch (JobPersistenceException e) {
                                logger.debug("Failed to add waiting trigger", e);
                                retry++;
                            }
                        }
                    }
                    logger.info("Recovered trigger {} from failed instance {}", trigger.getName(), eventInstanceId);

                    // Update the recovery timestamp
                    clusterManager.renewRecoveryLease(eventInstanceId);
                }

                // send notification
                schedulerSignaler.signalSchedulingChange(0L);
            } catch (JobPersistenceException e) {
                logger.warn("Error freeing acquired triggers of instance {}:  {}", eventInstanceId, e.getMessage());
                return false;
            }
            break;
        case INSTANCE_FAILED:
            break;
        case INSTANCE_RUNNING:
            break;
        }
        return true;
    }
}

