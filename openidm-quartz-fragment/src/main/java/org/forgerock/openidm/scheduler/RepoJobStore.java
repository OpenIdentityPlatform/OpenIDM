/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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
 * $Id$
 */

package org.forgerock.openidm.scheduler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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

import org.apache.commons.codec.binary.Base64;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.repo.RepositoryService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
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
 * 
 * @author ckeinle
 *
 */
public class RepoJobStore implements JobStore {

    final static Logger logger = LoggerFactory.getLogger(RepoJobStore.class);
    
    final static Object lock = new Object();
    
    /**
     * An identifier used to create unique keys for Jobs and Triggers.
     */
    private final static String UNIQUE_ID_SEPARATOR = "_$x$x$_"; 

    /**
     * The SchedulerSignaler to send notification to
     */
    private SchedulerSignaler schedulerSignaler;
    
    /**
     * The ClassLoadHelper (currently unused)
     */
    private ClassLoadHelper loadHelper;
    
    /**
     * The instance ID (currently unused)
     */
    private String instanceId;
    
    /**
     * The instance Name (currently unused)
     */
    private String instanceName;
    
    /**
     * The misfire threshold
     */
    private long misfireThreshold = 5000;
    
    /**
     * A list of all Triggers in the "waiting"/"normal" state.
     */
    private TreeSet<Trigger> waitingTriggers = new TreeSet(new TriggerComparator());
    
    /**
     * A list of all "blocked" jobs.
     */
    private List<String> blockedJobs = new ArrayList<String>();
    
    /**
     * An AtomicLong used for creating record IDs
     */
    private static AtomicLong ftrCtr = new AtomicLong(System.currentTimeMillis());
    
    /**
     * The Repository Service Router
     */
    private static ObjectSet router = null;
    
    /**
     * Creates a new <code>RestJobStore</code>.
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
        logger.info("Initializing RestJobStore");
        this.schedulerSignaler = schedSignaler;
        this.loadHelper = loadHelper;
        if (setRouter()) {
            List<String> waitingTriggersRepoList = null;
            try {
                waitingTriggersRepoList = getOrCreateRepoList(getWaitingTriggersRepoId(), "names");
                for (String id : waitingTriggersRepoList) {
                    TriggerWrapper tw = retrieveTriggerWrapper(id);
                    if (tw == null) {
                       logger.warn("Could not add " + id + " to list of waiting Triggers. Trigger not found in repo");
                    } else {
                        logger.debug("Found waiting trigger " + tw.getName() + " in group " + tw.getGroup());
                        waitingTriggers.add(tw.getTrigger());
                    }
                }
            } catch (Exception e) {
                logger.warn("Error intializing waiting triggers: " + e.getMessage());
            }
            
        }
    }

    /**
     * Sets the Repository Service Router and returns true if successful, false otherwise.
     * 
     * @return  true if successful, false otherwise
     */
    public boolean setRouter() {
        if (router == null) {
            BundleContext ctx = FrameworkUtil.getBundle(RepoJobStore.class).getBundleContext();
            ServiceReference serviceReference = ctx.getServiceReference(RepositoryService.class.getName());
            RepositoryService repoService = RepositoryService.class.cast(ctx.getService(serviceReference));
            if (repoService != null) {
                router = new JsonResourceObjectSet(repoService);
            }
            return !(router == null);
        }
        return true;
    }
    
    protected static void setRouter(ObjectSet router) {
        RepoJobStore.router = router;
    }
    
    @Override
    public void schedulerStarted() throws SchedulerException {
        logger.info("Job Scheduler Started");
    }
    
    /**
     * Gets the repository ID prefix
     * 
     * @return the repository ID prefix
     */
    private String getIdPrefix() {
        return "scheduler/";
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
        return new StringBuilder(getIdPrefix()).append("triggers/").append(getTriggerId(group, name)).toString();
    }

    /**
     * Gets the Trigger's repository ID.
     * 
     * @param id    the Trigger's ID
     * @return  the repository ID
     */
    private String getTriggersRepoId(String triggerId) {
        return new StringBuilder(getIdPrefix()).append("triggers/").append(triggerId).toString();
    }

    /**
     * Gets the Trigger groups repository ID.
     * 
     * @return  the repository ID
     */
    private String getTriggerGroupsRepoId(String groupName) {
        StringBuilder sb = new StringBuilder();
        return sb.append(getIdPrefix()).append("triggerGroups/").append(groupName).toString();
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
     * Gets the Trigger ID.
     * 
     * @param group the Trigger group
     * @param name  the Trigger name
     * @return  the Trigger ID
     */
    private String getTriggerId(String group, String name) {
        return new StringBuilder(group).append(UNIQUE_ID_SEPARATOR).append(name).toString();
    }

    /**
     * Gets the Job ID.
     * 
     * @param group the Job group
     * @param name  the Job name
     * @return  the Job ID
     */
    private String getJobId(String group, String name) {
        return new StringBuilder(group).append(UNIQUE_ID_SEPARATOR).append(name).toString();
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
        logger.info("Job Scheduler Stopped");
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
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            try {
                CalendarWrapper cw = new CalendarWrapper(calendar, name);
                if (retrieveCalendar(context, name) == null) {
                    // Create Calendar
                    logger.debug("Creating Calendar: " + name);
                    router.create(getCalendarsRepoId(name), cw.getValue().asMap());
                } else {
                    if (!replaceExisting) {
                        throw new ObjectAlreadyExistsException(name);
                    }
                    // Update Calendar
                    logger.debug("Updating Calendar: " + name);
                    String rev = getRevision(getCalendarsRepoId(name));
                    router.update(getCalendarsRepoId(name), rev, cw.getValue().asMap());
                }
                
                if (updateTriggers) {
                    List<TriggerWrapper> twList = getTriggerWrappersForCalendar(name);
                    for (TriggerWrapper tw : twList) {
                        Trigger t = tw.getTrigger();
                        boolean removed = removeWaitingTrigger(t);
                        t.updateWithNewCalendar(calendar, getMisfireThreshold());
                        tw.updateTrigger(t);
                        logger.debug("Updating Trigger " + tw.getName() + " in group " + tw.getGroup());
                        updateTriggerInRepo(tw.getGroup(), tw.getName(), tw);
                        if (removed) {
                            addWaitingTrigger(t);
                        }
                    }
                }
            } catch (ObjectSetException e) {
                logger.error("Error reading from repo: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (Exception e) {
                logger.error("Error storing job: " + e.getMessage());
                e.printStackTrace();
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }

    @Override
    public void storeJob(SchedulingContext context, JobDetail newJob, boolean replaceExisting)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            String jobName = newJob.getName();
            String jobGroup = newJob.getGroup();
            String jobId = getJobsRepoId(jobGroup, jobName);
            try {
                // Get job group
                JobGroupWrapper jgw = getOrCreateJobGroupWrapper(jobGroup);
                List<String> jobNames = jgw.getJobNames();
                JobWrapper jw = new JobWrapper(newJob, jgw.isPaused());
                // Check if job name exists
                if (jobNames.contains(jobName)) {
                    if (!replaceExisting) {
                        throw new ObjectAlreadyExistsException(newJob);
                    }
                    // Update job
                    logger.debug("Updating Job: " + jobName + " in group " + jobGroup);
                    String rev = getRevision(jobId);
                    router.update(jobId, rev, jw.getValue().asMap());
                } else {
                    // Add job name to list
                    jgw.addJob(jobName);
                    // Update job group
                    String rev = getRevision(getJobGroupsRepoId(jobGroup));
                    router.update(getJobGroupsRepoId(jobGroup), rev, jgw.getValue().asMap());

                    // Create job
                    logger.debug("Creating Job: " + jobName + " in group " + jobGroup);
                    router.create(jobId, jw.getValue().asMap());
                }
            } catch (ObjectSetException e) {
                e.printStackTrace();
                logger.error("Error reading from repo: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (Exception e) {
                logger.error("Error storing job: " + e.getMessage());
                e.printStackTrace();
                throw new JobPersistenceException(e.getMessage());
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
    public void storeTrigger(SchedulingContext context, Trigger trigger, boolean replaceExisting)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            String triggerName = trigger.getKey().getName();
            String groupName = trigger.getKey().getGroup();
            String triggerId = getTriggersRepoId(groupName, triggerName);
            try {
                // Get trigger group
                TriggerGroupWrapper tgw = getOrCreateTriggerGroupWrapper(groupName);
                List<String> triggerNames = tgw.getTriggerNames();
                TriggerWrapper tw = new TriggerWrapper(trigger, tgw.isPaused());
                // Check if trigger name exists
                if (triggerNames.contains(triggerName)) {
                    if (!replaceExisting) {
                        throw new ObjectAlreadyExistsException(trigger);
                    }
                    // Update trigger
                    logger.debug("Updating Trigger: " + triggerId);
                    String rev = getRevision(triggerId);
                    router.update(triggerId, rev, tw.getValue().asMap());
                } else {
                    // Add trigger name to list
                    tgw.addTrigger(triggerName);
                    // Update trigger group
                    String rev = getRevision(getTriggerGroupsRepoId(groupName));
                    router.update(getTriggerGroupsRepoId(groupName), rev, tgw.getValue().asMap());

                    // Create trigger
                    logger.debug("Creating Trigger: " + triggerId);
                    router.create(triggerId, tw.getValue().asMap());
                }
            } catch (ObjectSetException e) {
                logger.error("Error reading from repo: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (Exception e) {
                logger.error("Error storing trigger: " + e.getMessage());
                e.printStackTrace();
                throw new JobPersistenceException(e.getMessage());
            }
            addWaitingTrigger(trigger);
            logger.debug("There are now " + waitingTriggers.size() + " triggers waiting");
        }
    }
    
    @Override
    public Trigger acquireNextTrigger(SchedulingContext context, long noLaterThan)
            throws JobPersistenceException {
        synchronized (lock) {
            Trigger trigger = null;
            while (trigger == null) {
                try {
                    trigger = waitingTriggers.first();
                } catch (NoSuchElementException e1) {
                    return null;
                }
                
                if (trigger == null) {
                    return null;
                }
                
                
                Date nextFireTime = trigger.getNextFireTime();
                if (nextFireTime == null) {
                    logger.debug("Trigger next fire time = null, removing");
                    removeWaitingTrigger(trigger);
                    trigger = null;
                    continue;
                }
                
                removeWaitingTrigger(trigger);
                
                if (hasTriggerMisfired(trigger)) {
                    logger.debug("Attempting to process misfired trigger");
                    processTriggerMisfired(trigger);
                    if (trigger.getNextFireTime() != null) {
                        addWaitingTrigger(trigger);
                    }
                    trigger = null;
                    continue;
                }
                
                if (noLaterThan > 0) {
                    if (nextFireTime.getTime() > noLaterThan) {
                        addWaitingTrigger(trigger);
                        return null;
                    }
                }
                
                JsonValue jv = getTriggerFromRepo(trigger.getGroup(), trigger.getName());
                TriggerWrapper tw = new TriggerWrapper(jv.asMap());
                tw.setAcquired(true);
                trigger.setFireInstanceId(getFiredTriggerRecordId());
                try {
                    tw.updateTrigger(trigger);
                } catch (Exception e) {
                    logger.error("Error updating trigger's serialized value");
                    e.printStackTrace();
                }

                updateTriggerInRepo(trigger.getGroup(), trigger.getName(), tw);

                
                return (Trigger)trigger.clone();
            }
            return null;
        }
    }

    
    
    @Override
    public void releaseAcquiredTrigger(SchedulingContext arg0, Trigger trigger)
            throws JobPersistenceException {
        synchronized (lock) {
            JsonValue triggerValue = getTriggerFromRepo(trigger.getGroup(), trigger.getName());
            if (triggerValue.isNull()) {
                logger.warn("Cannot release acquired trigger " + trigger.getName() + 
                        " in group " + trigger.getGroup() + ", trigger does not exist");
                return;
            }
            TriggerWrapper tw = new TriggerWrapper(triggerValue.asMap());
            if (tw.isAcquired()) {
                tw.setAcquired(false);
                updateTriggerInRepo(trigger.getGroup(), trigger.getName(), tw);
                addWaitingTrigger(trigger);
            } else {
                logger.warn("Cannot release acquired trigger " + trigger.getName() + 
                        " in group " + trigger.getGroup() + ", trigger has not been acquired");
            }
        }
    }
    
    @Override
    public String[] getCalendarNames(SchedulingContext arg0)
            throws JobPersistenceException {
        List<String> names = null;
        try {
            names = getOrCreateRepoList(getCalendarNamesRepoId(), "names");
        } catch (ObjectSetException e) {
            logger.error("Error reading from repo: " + e.getMessage());
            throw new JobPersistenceException(e.getMessage());
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
                    TriggerWrapper tw = new TriggerWrapper(getTriggerFromRepo(
                            group, name).asMap());
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
        } catch (ObjectSetException e) {
            logger.error("Error reading from repo: " + e.getMessage());
            throw new JobPersistenceException(e.getMessage());
        }
        return names.toArray(new String[names.size()]);
    }

    @Override
    public String[] getJobNames(SchedulingContext context, String groupName)
            throws JobPersistenceException {
        try {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            JobGroupWrapper jgw = new JobGroupWrapper(router.read(getJobGroupsRepoId(groupName)));
            List<String> names = jgw.getJobNames();
            return names.toArray(new String[names.size()]);    
        } catch (ObjectSetException e) {
            logger.error("Error reading from repo: " + e.getMessage());
            throw new JobPersistenceException(e.getMessage());
        }
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
        } catch (ObjectSetException e) {
            logger.error("Error reading from repo: " + e.getMessage());
            throw new JobPersistenceException(e.getMessage());
        }
        return new HashSet<String>(names);
    }

    @Override
    public String[] getTriggerGroupNames(SchedulingContext context) throws JobPersistenceException {
        List<String> names = null;
        try {
            names = getOrCreateRepoList(getTriggerGroupNamesRepoId(), "names");
        } catch (ObjectSetException e) {
            logger.error("Error reading from repo: " + e.getMessage());
            throw new JobPersistenceException(e.getMessage());
        }
        return names.toArray(new String[names.size()]);
    }

    @Override
    public String[] getTriggerNames(SchedulingContext context, String groupName)
            throws JobPersistenceException {
        try {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            TriggerGroupWrapper tgw = new TriggerGroupWrapper(router.read(getTriggerGroupsRepoId(groupName)));
            List<String> names = tgw.getTriggerNames();
            return names.toArray(new String[names.size()]);    
        } catch (ObjectSetException e) {
            logger.error("Error reading from repo: " + e.getMessage());
            throw new JobPersistenceException(e.getMessage());
        }
    }

    @Override
    public int getTriggerState(SchedulingContext context, String triggerName, String triggerGroup)
            throws JobPersistenceException {
        String id = getTriggerId(triggerGroup, triggerName);
        int state = 0;
        logger.debug("Getting trigger state: " + id);
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
                Trigger trigger = getTrigger(groupName, name);
                if (trigger.getJobName().equals(jobName)) {
                    triggers.add(trigger);
                }
            }
            logger.debug("Found " + triggers.size() + " triggers for group: "
                    + groupName);
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
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            try {
                // Get job group, set paused, and update
                JobGroupWrapper jgw = getOrCreateJobGroupWrapper(groupName);
                jgw.pause();
                String rev = getRevision(getJobGroupsRepoId(groupName));
                router.update(getJobGroupsRepoId(groupName), rev, jgw.getValue().asMap());

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
                rev = getRevision(getPausedJobGroupNamesRepoId());
                router.update(getPausedJobGroupNamesRepoId(), rev, pauseMap);

                List<String> jobNames = jgw.getJobNames();
                for (String jobName : jobNames) {
                    pauseJob(context, jobName, groupName);
                }
            } catch (JsonValueException e) {
                logger.error("Error pausing job group: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (ObjectSetException e) {
                logger.error("Error pausing job group: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }

    @Override
    public void pauseTrigger(SchedulingContext context, String triggerName, String triggerGroup)
            throws JobPersistenceException {
        synchronized (lock) {
            JsonValue triggerValue = getTriggerFromRepo(triggerGroup, triggerName);
            if (triggerValue.isNull()) {
                logger.warn("Cannot pause trigger " + triggerName
                        + " in group " + triggerGroup
                        + ", trigger does not exist");
                return;
            }
            TriggerWrapper tw = new TriggerWrapper(triggerValue.asMap());
            Trigger trigger;
            try {
                trigger = tw.getTrigger();
            } catch (Exception e) {
                logger.error("Error deserializing trigger: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            }
            tw.pause();
            // Update the trigger
            updateTriggerInRepo(triggerGroup, triggerName, tw);
            // Remove trigger from waitingTriggers
            removeWaitingTrigger(trigger);
        }
    }

    @Override
    public void pauseTriggerGroup(SchedulingContext context, String groupName)
            throws JobPersistenceException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            try {
                // Get trigger group, set paused, and update
                TriggerGroupWrapper tgw = getOrCreateTriggerGroupWrapper(groupName);
                tgw.pause();
                String rev = getRevision(getTriggerGroupsRepoId(groupName));
                router.update(getTriggerGroupsRepoId(groupName), rev, tgw.getValue().asMap());

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
                rev = getRevision(getPausedTriggerGroupNamesRepoId());
                router.update(getPausedTriggerGroupNamesRepoId(), rev, pauseMap);

                List<String> triggerNames = tgw.getTriggerNames();
                for (String triggerName : triggerNames) {
                    pauseTrigger(context, triggerName, groupName);
                }
            } catch (JsonValueException e) {
                logger.error("Error pausing trigger group: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (ObjectSetException e) {
                logger.error("Error pausing trigger group: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }

    @Override
    public boolean removeCalendar(SchedulingContext context, String name)
            throws JobPersistenceException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            List<TriggerWrapper> twList = getTriggerWrappersForCalendar(name);
            if (twList.size() > 0) {
                throw new JobPersistenceException(
                        "Calender cannot be removed if it referenced by a Trigger!");
            }
            // Delete calendar
            logger.debug("Deleting Calendar: " + name);
            try {
                String rev = getRevision(getCalendarsRepoId(name));
                router.delete(getCalendarsRepoId(name), rev);
                return true;
            } catch (ObjectSetException e) {
                logger.error("Error reading from repo: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (Exception e) {
                logger.error("Error deleting calendar: " + e.getMessage());
                e.printStackTrace();
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }

    @Override
    public boolean removeJob(SchedulingContext arg0, String jobName, String groupName)
            throws JobPersistenceException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
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
                    String rev = getRevision(getJobGroupsRepoId(groupName));
                    router.update(getJobGroupsRepoId(groupName), rev, jgw.getValue().asMap());
                }
                // Delete job
                String rev = getRevision(jobId);
                logger.debug("Deleting Job: " + jobId);
                router.delete(jobId, rev);
                return true;
            } catch (ObjectSetException e) {
                logger.error("Error reading from repo: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (Exception e) {
                logger.error("Error deleting job: " + e.getMessage());
                e.printStackTrace();
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }

    @Override
    public boolean removeTrigger(SchedulingContext context, String triggerName, String groupName) 
            throws JobPersistenceException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            String triggerId = getTriggersRepoId(groupName, triggerName);
            try {
                // Get trigger group
                TriggerGroupWrapper tgw = getOrCreateTriggerGroupWrapper(groupName);
                List<String> triggerNames = tgw.getTriggerNames();
                // Check if trigger name exists
                if (triggerNames.contains(triggerName)) {
                    // Remove trigger from list
                    tgw.removeTrigger(triggerName);
                    // Update trigger group
                    String rev = getRevision(getTriggerGroupsRepoId(groupName));
                    router.update(getTriggerGroupsRepoId(groupName), rev, tgw.getValue().asMap());
                }
                // Attempt to remove from waiting triggers
                Trigger trigger = retrieveTrigger(context, triggerName, groupName);
                if (trigger != null)  {
                    removeWaitingTrigger(trigger);
                }
                // Delete trigger
                String rev = getRevision(triggerId);
                logger.debug("Deleting Trigger: " + triggerId);
                router.delete(triggerId, rev);
                return true;
            } catch (ObjectSetException e) {
                logger.error("Error reading from repo: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (Exception e) {
                logger.error("Error deleting trigger: " + e.getMessage());
                e.printStackTrace();
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }

    @Override
    public boolean replaceTrigger(SchedulingContext context, String triggerName, String groupName, Trigger newTrigger) 
            throws JobPersistenceException {
        synchronized (lock) {
            Trigger trigger = getTrigger(groupName, triggerName);
            if (trigger == null) {
                logger.debug("Cannot resume " + triggerName + " in " + groupName + ", trigger does not exist");
                return false;
            }
            if (!trigger.getJobName().equals(newTrigger.getJobName())
                    || !trigger.getJobGroup().equals(newTrigger.getJobGroup())) {
                throw new JobPersistenceException(
                        "Error replacing trigger, new trigger references a different job");
            }
            logger.debug("Replacing trigger " + triggerName + "/" + groupName + " with " + newTrigger.getName() + "/"
                    + newTrigger.getGroup());
            removeTrigger(context, triggerName, groupName);
            try {
                storeTrigger(context, newTrigger, false);
            } catch (JobPersistenceException e) {
                logger.error("Error replacing trigger");
                storeTrigger(context, trigger, false);
                throw e;
            }
            return true;
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
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            try {
                // Get job group, resume, and update
                JobGroupWrapper jgw = getOrCreateJobGroupWrapper(groupName);
                jgw.resume();
                String rev = getRevision(getJobGroupsRepoId(groupName));
                router.update(getJobGroupsRepoId(groupName), rev, jgw.getValue().asMap());

                // Get list of paused groups, remove this group (if not paused, return), and update
                Map<String, Object> pauseMap = getOrCreateRepo(getPausedJobGroupNamesRepoId());
                List<String> pausedGroups = (List<String>) pauseMap.get("paused");
                if (pausedGroups == null || !pausedGroups.contains(groupName)) {
                    return;
                }
                pausedGroups.remove(groupName);
                rev = getRevision(getPausedJobGroupNamesRepoId());
                router.update(getPausedJobGroupNamesRepoId(), rev, pauseMap);

                List<String> jobNames = jgw.getJobNames();
                for (String jobName : jobNames) {
                    resumeJob(context, jobName, groupName);
                }
            } catch (JsonValueException e) {
                logger.error("Error resuming job group: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (ObjectSetException e) {
                logger.error("Error resuming job group: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }

    @Override
    public void resumeTrigger(SchedulingContext arg0, String triggerName, String triggerGroup)
            throws JobPersistenceException {
        synchronized (lock) {
            JsonValue triggerValue = getTriggerFromRepo(triggerGroup,
                    triggerName);
            if (triggerValue.isNull()) {
                logger.warn("Cannot resume trigger " + triggerName + " in group " + triggerGroup
                        + ", trigger does not exist");
                return;
            }
            TriggerWrapper tw = new TriggerWrapper(triggerValue.asMap());
            Trigger trigger;
            try {
                trigger = tw.getTrigger();
            } catch (Exception e) {
                logger.error("Error deserializing trigger: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            }
            tw.resume();
            // Update the trigger
            updateTriggerInRepo(triggerGroup, triggerName, tw);
            // Add trigger to waitingTriggers
            addWaitingTrigger(trigger);
        }
    }

    @Override
    public void resumeTriggerGroup(SchedulingContext context, String groupName)
            throws JobPersistenceException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            try {
                // Get trigger group, resume, and update
                TriggerGroupWrapper tgw = getOrCreateTriggerGroupWrapper(groupName);
                tgw.resume();
                String rev = getRevision(getTriggerGroupsRepoId(groupName));
                router.update(getTriggerGroupsRepoId(groupName), rev, tgw.getValue().asMap());

                // Get list of paused groups, remove this group (if not present, return), and update
                Map<String, Object> pauseMap = getOrCreateRepo(getPausedTriggerGroupNamesRepoId());
                List<String> pausedGroups = (List<String>) pauseMap.get("paused");
                if (pausedGroups == null) {
                    pausedGroups = new ArrayList<String>();
                    pauseMap.put("paused", pausedGroups);
                } else if (pausedGroups.contains(groupName)) {
                    pausedGroups.remove(groupName);
                }
                rev = getRevision(getPausedTriggerGroupNamesRepoId());
                router.update(getPausedTriggerGroupNamesRepoId(), rev, pauseMap);

                List<String> triggerNames = tgw.getTriggerNames();
                for (String triggerName : triggerNames) {
                    resumeTrigger(context, triggerName, groupName);
                }
            } catch (JsonValueException e) {
                logger.error("Error pausing trigger group: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (ObjectSetException e) {
                logger.error("Error pausing trigger group: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }

    @Override
    public Calendar retrieveCalendar(SchedulingContext context, String name)
            throws JobPersistenceException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            try {
                if (name == null) {
                    return null;
                }
                logger.debug("Getting calendar: " + getCalendarsRepoId(name));
                Map<String, Object> calMap = router.read(getCalendarsRepoId(name));
                if (calMap == null) {
                    return null;
                }
                CalendarWrapper cal = new CalendarWrapper(calMap);
                return cal.getCalendar();
            } catch (ObjectSetException e) {
                logger.error("Error reading from repo: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (Exception e) {
                logger.error("Error retrieving calendar: " + e.getMessage());
                e.printStackTrace();
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }

    @Override
    public JobDetail retrieveJob(SchedulingContext context, String jobName,
            String jobGroup) throws JobPersistenceException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            try {
                logger.debug("Getting job: " + getJobsRepoId(jobGroup, jobName));
                JobWrapper jw = new JobWrapper(router.read(getJobsRepoId(jobGroup, jobName)));
                return jw.getJobDetail();
            } catch (ObjectSetException e) {
                logger.error("Error reading from repo: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error retrieving job: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }
    
    public JobWrapper retrieveJob(JobDetail jobDetail) throws JobPersistenceException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            String jobGroup = jobDetail.getGroup();
            String jobName = jobDetail.getName();
            try {
                logger.debug("Getting job: " + getJobsRepoId(jobGroup, jobName));
                return new JobWrapper(router.read(getJobsRepoId(jobGroup, jobName)));
            } catch (ObjectSetException e) {
                logger.error("Error reading from repo: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (Exception e) {
                logger.error("Error retrieving job: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }

    public TriggerWrapper retrieveTriggerWrapper(String triggerName, String triggerGroup) throws ObjectSetException, Exception {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            logger.debug("Getting trigger: " + getTriggersRepoId(triggerGroup, triggerName));
            Map<String, Object> triggerMap = router.read(getTriggersRepoId(triggerGroup, triggerName));
            if (triggerMap == null) {
                return null;
            }
            return new TriggerWrapper(triggerMap);
        }
    }
    
    public TriggerWrapper retrieveTriggerWrapper(String triggerId) throws ObjectSetException, Exception {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            logger.debug("Getting trigger: " + getTriggersRepoId(triggerId));
            Map<String, Object> triggerMap = router.read(getTriggersRepoId(triggerId));
            if (triggerMap == null) {
                return null;
            }
            return new TriggerWrapper(triggerMap);
        }
    }
    
    @Override
    public Trigger retrieveTrigger(SchedulingContext context, String triggerName, String triggerGroup) 
            throws JobPersistenceException {
        synchronized (lock) {
            try {
                TriggerWrapper tw = retrieveTriggerWrapper(triggerName, triggerGroup);
                if (tw == null) {
                    return null;
                }
                return tw.getTrigger();
            } catch (ObjectSetException e) {
                logger.error("Error reading from repo: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            } catch (Exception e) {
                logger.error("Error retrieving trigger: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }

    @Override
    public TriggerFiredBundle triggerFired(SchedulingContext context, Trigger trigger)
            throws JobPersistenceException {
        synchronized (lock) {
            
            String tGroup = trigger.getGroup();
            String tName = trigger.getName();
            TriggerWrapper tw;
            try {
                tw = retrieveTriggerWrapper(tName, tGroup);

            } catch (Exception e) {
                logger.error("Error retrieving trigger: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            }
            if (tw == null) {
                logger.error("TriggerFired failed, trigger does not exist");
                return null;
            }
            if (!tw.isAcquired()) {
                logger.error("TriggerFired failed, trigger was not in acquired state");
            }
            Trigger localTrigger;
            try {
                localTrigger = tw.getTrigger();
            } catch (Exception e) {
                logger.error("Error retrieving trigger: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            }
            Calendar triggerCalendar = null;
            if (localTrigger.getCalendarName() != null) {
                triggerCalendar = retrieveCalendar(context,localTrigger.getCalendarName());
                if (triggerCalendar == null) {
                    logger.error("TriggerFired failed, Cannot find trigger's calendar");
                    return null;
                }
            }
            Date previousFireTime = trigger.getPreviousFireTime();
            removeWaitingTrigger(trigger);
            
            localTrigger.triggered(triggerCalendar);
            tw.updateTrigger(localTrigger);
            updateTriggerInRepo(localTrigger.getGroup(), localTrigger.getName(), tw);

            trigger.triggered(triggerCalendar);
            
            // Set trigger into the normal/waiting state
            tw.setState(Trigger.STATE_NORMAL);
            TriggerFiredBundle tfb = new TriggerFiredBundle(retrieveJob(context, trigger.getJobName(), trigger.getJobGroup()), 
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
                    TriggerWrapper tmpTw = new TriggerWrapper(getTriggerFromRepo(t.getGroup(), t.getName()).asMap());
                    if (tmpTw.state == Trigger.STATE_NORMAL ||
                            tmpTw.state == Trigger.STATE_PAUSED) {
                        tmpTw.block();
                    }
                    // update trigger in repo
                    updateTriggerInRepo(t.getGroup(), tmpTw.getName(), tmpTw);
                    removeWaitingTrigger(t);
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
            String jobKey = getJobNameKey(jobDetail);
            JobWrapper jw = retrieveJob(jobDetail);
            TriggerWrapper tw = new TriggerWrapper(getTriggerFromRepo(trigger.getGroup(), trigger.getName()).asMap());
            
            if (jw != null) {
                JobDetail jd;
                try {
                    jd = jw.getJobDetail();
                } catch (Exception e) {
                    throw new JobPersistenceException(e.getMessage());
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
                        TriggerWrapper tmpTw = new TriggerWrapper(getTriggerFromRepo(t.getGroup(), t.getName()).asMap());
                        if (tmpTw.state == Trigger.STATE_BLOCKED) {
                            tmpTw.unblock();
                        }
                        // update trigger in repo
                        updateTriggerInRepo(t.getGroup(), tmpTw.getName(), tmpTw);
                        
                        if (!tmpTw.isPaused()) {
                            addWaitingTrigger(t);
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
                    tw.state = Trigger.STATE_COMPLETE;
                    removeWaitingTrigger(tw.getTrigger());
                    schedulerSignaler.signalSchedulingChange(0L);
                } else if (triggerInstCode == Trigger.INSTRUCTION_SET_TRIGGER_ERROR) {
                    logger.info("Trigger " + trigger.getFullName() + " set to ERROR state.");
                    tw.state = Trigger.STATE_ERROR;
                    schedulerSignaler.signalSchedulingChange(0L);
                } else if (triggerInstCode == Trigger.INSTRUCTION_SET_ALL_JOB_TRIGGERS_ERROR) {
                    logger.info("All triggers of Job " + trigger.getFullJobName() + " set to ERROR state.");
                    setAllTriggersOfJobToState(trigger.getJobName(), trigger.getJobGroup(), Trigger.STATE_ERROR);
                    schedulerSignaler.signalSchedulingChange(0L);
                } else if (triggerInstCode == Trigger.INSTRUCTION_SET_ALL_JOB_TRIGGERS_COMPLETE) {
                    setAllTriggersOfJobToState(trigger.getJobName(), trigger.getJobGroup(), Trigger.STATE_COMPLETE);
                    schedulerSignaler.signalSchedulingChange(0L);
                }
            }
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
    protected void setAllTriggersOfJobToState(String jobName, String jobGroup, int state) throws JobPersistenceException {
        synchronized (lock) {
            Trigger[] triggers = getTriggersForJob(null, jobName, jobGroup);
            for (Trigger t : triggers) {
                TriggerWrapper tw = new TriggerWrapper(getTriggerFromRepo(
                        t.getGroup(), t.getName()).asMap());
                tw.state = state;
                if (state != Trigger.STATE_NORMAL) {
                    removeWaitingTrigger(tw.getTrigger());
                }
            }
        }
    }
    
    /**
     * Gets a Trigger group from the repo and wraps it in a TriggerGroupWapper(). Creates the group if it
     * doesn't already exist
     * 
     * @param groupName name of group
     * @return a trigger group wrapped in a TriggerGroupWrapper
     * @throws ObjectSetException
     */
    private TriggerGroupWrapper getOrCreateTriggerGroupWrapper(String groupName) throws JobPersistenceException, ObjectSetException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            Map<String, Object> map;
            try {
                map = router.read(getTriggerGroupsRepoId(groupName));
            } catch (NotFoundException e) {
                logger.debug("Trigger Group not found, lets create it");
                map = null;
            }
            TriggerGroupWrapper tgw = null;
            if (map == null) {
                // create if null
                tgw = new TriggerGroupWrapper(groupName);
                // create in repo
                router.create(getTriggerGroupsRepoId(groupName), tgw.getValue()
                        .asMap());
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
     * Gets a Job group from the repo and wraps it in a JobGroupWapper(). Creates the group if it
     * doesn't already exist
     * 
     * @param groupName name of group
     * @return a job group wrapped in a JobGroupWrapper
     * @throws ObjectSetException
     */
    private JobGroupWrapper getOrCreateJobGroupWrapper(String groupName) throws JobPersistenceException, ObjectSetException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            Map<String, Object> map;
            try {
                map = router.read(getJobGroupsRepoId(groupName));
            } catch (NotFoundException e) {
                logger.debug("Group Name not found, lets create it");
                map = null;
            }
            JobGroupWrapper jgw = null;
            if (map == null) {
                // create if null
                jgw = new JobGroupWrapper(groupName);
                // create in repo
                router.create(getJobGroupsRepoId(groupName), jgw.getValue().asMap());
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
            waitingTriggers.add(trigger);
            try {
                addRepoListName(getTriggerId(trigger.getGroup(), trigger.getName()), getWaitingTriggersRepoId());
            } catch (ObjectSetException e) {
                throw new JobPersistenceException(e.getMessage());
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
            waitingTriggers.remove(trigger);
            try {
                return removeRepoListName(getTriggerId(trigger.getGroup(), trigger.getName()), getWaitingTriggersRepoId());
            } catch (ObjectSetException e) {
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }
    
    /**
     * Adds a Trigger group name to the list of Trigger group names
     * 
     * @param groupName the name to add
     * @throws JobPersistenceException
     * @throws ObjectSetException
     */
    private void addTriggerGroupName(String groupName) throws JobPersistenceException, ObjectSetException {
        addRepoListName(groupName, getTriggerGroupNamesRepoId());
    }
    
    /**
     * Adds a Job group name to the list of Job group names
     * 
     * @param groupName the name to add
     * @throws JobPersistenceException
     * @throws ObjectSetException
     */
    private void addJobGroupName(String groupName) throws JobPersistenceException, ObjectSetException {
        addRepoListName(groupName, getJobGroupNamesRepoId());
    }

    /**
     * Adds a name to a list of names in the repo.
     * 
     * @param name  the name to add
     * @param id    the repo id
     * @throws JobPersistenceException
     * @throws ObjectSetException
     */
    private boolean addRepoListName(String name, String id) throws JobPersistenceException, ObjectSetException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            logger.debug("Adding name: " + name + " to " + id);
            Map<String, Object> map = getOrCreateRepo(id);
            String rev = (String)map.get("_rev");
            if (rev == null) {
                rev = "0";
            }
            List<String> names = (List<String>) map.get("names");
            if (names == null) {
                names = new ArrayList<String>();
                map.put("names", names);
            }
            boolean result = names.add(name);
            // update repo
            router.update(id, rev, map);
            return result;
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
    private boolean removeRepoListName(String name, String id) throws JobPersistenceException, ObjectSetException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            logger.debug("Removing name: " + name + " from " + id);
            Map<String, Object> map = getOrCreateRepo(id);
            String rev = (String)map.get("_rev");
            if (rev == null) {
                rev = "0";
            }
            List<String> names = (List<String>) map.get("names");
            if (names == null) {
                names = new ArrayList<String>();
                map.put("names", names);
            }
            boolean result = names.remove(name);
            // update repo
            router.update(id, rev, map);
            return result;
        }

    }
    
    private Map<String, Object> getOrCreateRepo(String repoId) throws JobPersistenceException, ObjectSetException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            Map<String, Object> map;
            try {
                map = router.read(repoId);
            } catch (NotFoundException e) {
                logger.debug("repo " + repoId + " not found, lets create it");
                map = null;
            }
            if (map == null) {
                map = new HashMap<String, Object>();
                // create in repo
                logger.debug("Creating repo: " + repoId);
                router.create(repoId, map);
            }
            return map;
        }
    }
    
    private List<String> getOrCreateRepoList(String repoId, String listId) throws JobPersistenceException, ObjectSetException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            List<String> list = null;
            Map<String, Object> map;
            String revision = "0";
            try {
                map = router.read(repoId);
                revision = (String)map.get("_rev");
                if (revision == null) {
                    revision = "0";
                }
            } catch (NotFoundException e) {
                logger.debug("repo list " + listId + " not found, lets create it");
                map = null;
            }
            if (map == null) {
                map = new HashMap<String, Object>();
                list = new ArrayList<String>();
                map.put(listId, list);
                // create in repo
                router.create(repoId, map);
            } else {
                // else check if list exists in map
                list = (List<String>) map.get(listId);
                if (list == null) {
                    list = new ArrayList<String>();
                    map.put(listId, list);
                    router.update(repoId, revision, map);
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
    private JsonValue getTriggerFromRepo(String group, String name) throws JobPersistenceException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            try {
                logger.debug("Getting trigger from repo: "
                        + getTriggersRepoId(group, name));
                return new JsonValue(router.read(getTriggersRepoId(group, name)));
            } catch (ObjectSetException e) {
                logger.error("Error reading from repo: " + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }
    
    private String getRevision(String repoId) {
        Map<String, Object> map;
        String revision;
        try {
            map = router.read(repoId);
            revision = (String)map.get("_rev");
            if (revision != null) {
                return revision;
            }
        } catch (Exception e) {
            return "0";
        }
        return "0";
    }
    
    /**
     * Updates a trigger in the repo.
     *  
     * @param name the name of the trigger
     * @param group the group id of the trigger
     * @param tw the TriggerWrapper representing the updated trigger
     * @throws JobPersistenceException
     */
    private void updateTriggerInRepo(String group, String name, TriggerWrapper tw) throws JobPersistenceException {
        synchronized (lock) {
            if (!setRouter()) {
                throw new JobPersistenceException("Repo router is null");
            }
            try {
                logger.debug("Getting trigger: " + getTriggersRepoId(group, name));
                String repoId = getTriggersRepoId(group, name);
                router.update(repoId, getRevision(repoId), tw.getValue().asMap());
            } catch (ObjectSetException e) {
                logger.error("Error updating trigger in repo: "
                        + e.getMessage());
                throw new JobPersistenceException(e.getMessage());
            }
        }
    }
    
    /**
     * Gets a trigger as a Trigger object.
     * 
     * @param group the group id of the trigger
     * @param name the name of the trigger
     * @return
     * @throws JobPersistenceException
     */
    private Trigger getTrigger(String group, String name) throws JobPersistenceException {
        JsonValue triggerContainer = getTriggerFromRepo(group, name);
        if (triggerContainer.isNull()) {
            return null;
        }
        try {
            TriggerWrapper tw = new TriggerWrapper(triggerContainer.asMap());
            return (Trigger)deserialize(tw.getSerialized());
        } catch (JsonValueException e) {
            logger.error("Error reading trigger value: " + e.getMessage());
            throw new JobPersistenceException(e.getMessage());
        } catch (JsonException e) {
            logger.error("Error reading trigger: " + e.getMessage());
            throw new JobPersistenceException(e.getMessage());
        } catch (Exception e) {
            logger.error("Error reading trigger: " + e.getMessage());
            e.printStackTrace();
            throw new JobPersistenceException(e.getMessage());
        }
    }
    
    /**
     * Converts a serializable object into a String.
     * 
     * @param object the object to serialize.
     * @return a string representation of the serialized object.
     * @throws Exception
     */
    private String serialize(Serializable object) throws JobPersistenceException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.flush();
            oos.close();
            //return new String(Base64Coder.encode(baos.toByteArray()));
            return new String(Base64.encodeBase64(baos.toByteArray()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new JobPersistenceException(e.getMessage());
        }
    }
    
    /**
     * Converts a String representation of a serialized object back
     * into an object.
     * 
     * @param str the representation of the serialized object
     * @return the deserialized object
     * @throws Exception
     */
    private Object deserialize(String str) throws JobPersistenceException {
        try {
            //byte [] bytes = Base64Coder.decode(str.toCharArray());
            byte [] bytes = Base64.decodeBase64(str);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object o  = ois.readObject();
            ois.close();
            return o;
        } catch (Exception e) {
            e.printStackTrace();
            throw new JobPersistenceException(e.getMessage());
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
    private void processTriggerMisfired(Trigger trigger) throws JobPersistenceException {
        schedulerSignaler.notifyTriggerListenersMisfired(trigger);
        Calendar calendar = retrieveCalendar(null, trigger.getCalendarName());
        trigger.updateAfterMisfire(calendar);
        if (trigger.getNextFireTime() == null) {
            schedulerSignaler.notifySchedulerListenersFinalized(trigger);
            synchronized (calendar) {
                TriggerWrapper tw = new TriggerWrapper(getTriggerFromRepo(
                        trigger.getGroup(), trigger.getName()).asMap());
                tw.setState(Trigger.STATE_COMPLETE);
                // update trigger in repo
                updateTriggerInRepo(trigger.getGroup(), trigger.getName(), tw);
                removeWaitingTrigger(trigger);
            }
        }
    }

    /**
     * Returns a job name key used to uniquely identify a specific job.
     * 
     * @param jobDetail
     * @return
     */
    public String getJobNameKey(JobDetail jobDetail) {
        return jobDetail.getGroup() + UNIQUE_ID_SEPARATOR + jobDetail.getName();
    }
    
    /**
     * A wrapper that contains all necessary information about a Job.
     */
    protected class JobWrapper {
        
        private String serialized;
        private String key;
        private boolean paused = false;
        
        /**
         * Creates a new JobWrapper from a JobDetail object
         * 
         * @param jobDetail a JobDetail object
         * @param paused    if the job is paused
         * @throws JobPersistenceException
         */
        public JobWrapper(JobDetail jobDetail, boolean paused) throws JobPersistenceException {
            this.key = jobDetail.getKey().toString();
            this.serialized = serialize(jobDetail);
            this.paused = paused;
        }
        
        /**
         * Creates a new JobWrapper from an object map.
         * 
         * @param map an object map
         */
        public JobWrapper(Map<String, Object> map) {
            serialized = (String)map.get("serialized");
            key = (String)map.get("key");
            paused = (Boolean)map.get("paused");
        }
        
        /**
         * Returns a JsonValue object representing the JobWrapper
         * 
         * @return  a JsonValue object
         */
        public JsonValue getValue() {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("serialized", serialized);
            map.put("key", key);
            map.put("paused", paused);
            return new JsonValue(map);
        }
        
        /**
         * Returns the serialized JobDetail object
         * 
         * @return  the serialized JobDetail object
         */
        public String getSerialized() {
            return serialized;
        }
        
        /**
         * Retuns the Job key
         * 
         * @return the Job key
         */
        public String getKey() {
            return key;
        }
        
        /**
         * Returns the deserialized JobDetail object.
         * 
         * @return the JobDetail object
         * @throws Exception
         */
        public JobDetail getJobDetail() throws Exception {
            return (JobDetail)deserialize(serialized);
        }
        
        /**
         * Returns true if the JobWrapper is in the "paused" state, false otherwise.
         * 
         * @return  true if the JobWrapper is in the "paused" state, false otherwise
         */
        public boolean isPaused() {
            return paused;
        }
        
        /**
         * Sets the JobWrapper in the "paused" state.
         * 
         * @return the paused JobWrapper
         */
        public void pause() {
            setPaused(true);
        }
        
        /**
         * Resumes the JobWrapper from the paused state
         * 
         * @return the resumed JobWrapper
         */
        public void resume() {
            setPaused(false);
        }

        /**
         * Sets the "paused" state of the JobWrapper
         * 
         * @param paused true if "paused", false otherwise
         */
        public void setPaused(boolean paused) {
            this.paused = paused;
        }
    }

    /**
     * A wrapper that contains the name and serialize form of a calendar.
     */
    protected class CalendarWrapper {
        private String serialized;
        private String name;
        
        /**
         * Creates a new CalendarWrapper from a specified Calendar object and name.
         * 
         * @param cal   the Calendar object
         * @param name  the name of the calendar
         * @throws JobPersistenceException
         */
        public CalendarWrapper(Calendar cal, String name) throws JobPersistenceException {
            this.name = name;
            this.serialized = serialize(cal);
        }
        
        /**
         * Creates a new CalendarWrapper from an object map.
         * 
         * @param map   a object map
         */
        public CalendarWrapper(Map<String, Object> map) {
            serialized = (String)map.get("serialized");
            name = (String)map.get("name");
        }
        
        /**
         * Deserializes and returns a Calendar object.
         * 
         * @return  the deserialized Calendar object
         * @throws Exception
         */
        public Calendar getCalendar() throws Exception {
            return (Calendar) deserialize(serialized);
        }
        
        /**
         * Returns the name of the calendar
         * 
         * @return  the name of the calendar
         */
        public String getName() {
            return name;
        }
        
        /**
         * Returns a JsonValue object wrapper around the object map for the calendar.
         * 
         * @return a JsonValue object
         */
        public JsonValue getValue() {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("serialized", serialized);
            map.put("name", name);
            return new JsonValue(map);
        }
    }
    
    /**
     * A wrapper that contains all necessary information for a Trigger.
     */
    protected class TriggerWrapper {
        
        private String serialized;
        private String name;
        private String group;
        private boolean acquired;
        private int state;
        private int previous_state = Trigger.STATE_NONE;
        
        /**
         * Create a new TriggerWrapper from the specified trigger.
         * 
         * @param trigger the Trigger object
         * @param paused if the trigger is paused
         * @throws Exception
         */
        public TriggerWrapper(Trigger trigger, boolean paused) throws Exception {
            this(trigger.getName(), trigger.getGroup(), serialize(trigger), paused);
        }
        
        /**
         * Create a new TriggerWrapper from parameters.
         * 
         * @param name the name of the trigger
         * @param group the trigger group
         * @param serializedValue a string representing the serialized trigger
         * @param paused if the trigger is paused
         */
        public TriggerWrapper(String name, String group, String serializedValue, boolean paused) {
            this.name = name;
            this.group = group;
            this.acquired = false;
            this.serialized = serializedValue;
            if (paused) {
                state = Trigger.STATE_PAUSED;
            } else {
                state = Trigger.STATE_NORMAL;
            }
            
        }
        
        /**
         * Create a new TriggerWrapper from a JsonValue object representing the trigger.
         * 
         * @param value the JsonValue object
         * @param paused if the trigger is paused
         */
        public TriggerWrapper(JsonValue value, boolean paused) {
            this(value.asMap(), paused);
        }
        
        /**
         * Create a new TriggerWrapper from a repo Map object.
         * 
         * @param map repo Map object
         * @param paused if the trigger is paused
         */
        public TriggerWrapper(Map<String, Object> map, boolean paused) {
            serialized = (String)map.get("serialized");
            name = (String)map.get("name");
            group = (String)map.get("group");
            previous_state = (Integer)map.get("previous_state");
            acquired = (Boolean)map.get("acquired");
            if (paused) {
                state = Trigger.STATE_PAUSED;
            } else {
                state = Trigger.STATE_NORMAL;
            }
        }
        
        /**
         * Create a new TriggerWrapper from a repo Map object.
         * 
         * @param map repo Map object
         */
        public TriggerWrapper(Map<String, Object> map) {
            serialized = (String)map.get("serialized");
            name = (String)map.get("name");
            group = (String)map.get("group");
            state = (Integer)map.get("state");
            previous_state = (Integer)map.get("previous_state");
            acquired = (Boolean)map.get("acquired");
        }

        /**
         * Sets the TriggerWrapper in the "paused" state.
         * 
         * @return the paused TriggerWrapper
         */
        public TriggerWrapper pause() {
            // It doesn't make sense to pause a completed trigger
            if (state != Trigger.STATE_COMPLETE) {
                previous_state = state;
                state = Trigger.STATE_PAUSED;
            }
            return this;
        }

        /**
         * Resumes the TriggerWrapper from the paused state
         * 
         * @return the resumed TriggerWrapper
         */
        public TriggerWrapper resume() {
            return reState(Trigger.STATE_PAUSED);
        }
        
        /**
         * Returns true if the TriggerWrapper is paused, false otherwise.
         * 
         * @return  true if the TriggerWrapper is paused, false otherwise
         */
        public boolean isPaused() {
            if (state == Trigger.STATE_PAUSED) {
                return true;
            }
            return false;
        }

        /**
         * Sets the TriggerWrapper in the "blocked" state
         * 
         * @return the blocked TriggerWrapper
         */
        public TriggerWrapper block() {
            // It doesn't make sense to pause a completed trigger
            if (state != Trigger.STATE_COMPLETE) {
                previous_state = state;
                state = Trigger.STATE_BLOCKED;
            }
            return this;
        }

        /**
         * Unblocks the TriggerWrapper
         * 
         * @return the unblocked TriggerWrapper
         */
        public TriggerWrapper unblock() {
            return reState(Trigger.STATE_BLOCKED);
        }
        
        private TriggerWrapper reState(int fromState) {
            if (state == fromState) {
                if (previous_state != Trigger.STATE_NONE) {
                    state = previous_state;
                    previous_state = Trigger.STATE_NONE;
                } else {
                    state = Trigger.STATE_NORMAL;
                }
            }
            return this;
        }
        
        /**
         * Updates the TriggerWrappers serialized Trigger object
         * 
         * @param trigger   The trigger update
         * @throws JobPersistenceException
         */
        public void updateTrigger(Trigger trigger) throws JobPersistenceException {
            serialized = serialize(trigger);
        }
        
        /**
         * Deserializes and returns the Trigger object for this TriggerWrapper
         * 
         * @return  the deserialized Trigger object
         * @throws JobPersistenceException
         */
        public Trigger getTrigger() throws JobPersistenceException {
            return (Trigger)deserialize(serialized);
        }
        
        /**
         * Gets the seriailized Trigger object.
         * 
         * @return  the serialized Trigger object
         */
        public String getSerialized() {
            return serialized;
        }

        /**
         * Sets the serialized Trigger object.
         * 
         * @param serialized    the serialized Trigger object
         */
        public void setSerialized(String serialized) {
            this.serialized = serialized;
        }

        /**
         * Returns the state of the Trigger.
         * 
         * @return the state of the Trigger
         */
        public int getState() {
            return state;
        }

        /**
         * Sets the state of the Trigger.
         * 
         * @param state the state of the Trigger
         */
        public void setState(int state) {
            this.state = state;
        }

        /**
         * Returns a JsonValue object wrapper around the object map for the TriggerWrapper.
         * 
         * @return a JsonValue object
         */
        public JsonValue getValue() {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("serialized", serialized);
            map.put("name", name);
            map.put("group", group);
            map.put("previous_state", previous_state);
            map.put("state", state);
            map.put("acquired", acquired);
            return new JsonValue(map);
        }

        /**
         * Returns a String representing the details of the TriggerWrapper.
         * 
         * @return  a String representing the TriggerWrapper details
         */
        public String toDetails() {
            StringBuilder sb = new StringBuilder();
            sb.append("name:     ").append(name).append("\n");
            sb.append("group:    ").append(group).append("\n");
            sb.append("state:    ").append(state).append("\n");
            sb.append("p-state:  ").append(previous_state).append("\n");
            sb.append("acquired: ").append(acquired).append("\n");
            return sb.toString();
        }
        
        /**
         * Returns the name of the Trigger.
         * 
         * @return  the name of the Trigger
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the group name that the Trigger belongs to.
         * 
         * @return  the group name
         */
        public String getGroup() {
            return group;
        }

        /**
         * Return true if the Trigger is in the "acquired" state, false otherwise.
         * 
         * @return  true if the Trigger is acquired, false otherwise
         */
        public boolean isAcquired() {
            return acquired;
        }
        
        /**
         * Sets the TriggerWrapper's "acquired" state.
         * 
         * @param acquired  true if "acquired", false otherwise
         */
        public void setAcquired(boolean acquired) {
            this.acquired = acquired;
        }
    }
    
    
    /**
     * A wrapper the contains all necessary information for a Trigger's group
     */
    protected class TriggerGroupWrapper {
        
        private String name;
        private List<String> triggers;
        private boolean paused;
        
        /**
         * Creates a new TriggerGroupWrapper from a group name
         * 
         * @param triggerName the group name
         */
        public TriggerGroupWrapper(String triggerName) {
            triggers = new ArrayList<String>();
            name = triggerName;
            paused = false;
            
        }
        
        /**
         * Creates a new TriggerGroupWrapper from a JsonValue object
         * 
         * @param value the JsonValue object
         */
        public TriggerGroupWrapper(JsonValue value) {
            this(value.asMap());
        }
        
        /**
         * Creates a new TriggerGroupWrapper from a object map.
         * 
         * @param map   the object map
         */
        public TriggerGroupWrapper(Map<String, Object> map) {
            name = (String)map.get("name");
            paused = (Boolean)map.get("paused");
            triggers = (List<String>)map.get("triggers");
        }
        
        /**
         * Returns the name of the group
         * 
         * @return the name of the group
         */
        public String getName() {
            return name;
        }
        
        /**
         * Returns true if the group is in the "paused" state, false otherwise.
         * 
         * @return  true if the group is in the "paused" state, false otherwise
         */
        public boolean isPaused() {
            return paused;
        }
        
        /**
         * Sets the TriggerGroupWrapper in the "paused" state.
         */
        public void pause() {
            setPaused(true);
        }
        
        /**
         * Resumes the TriggerGroupWrapper form the "paused" state.
         */
        public void resume() {
            setPaused(false);
        }
        
        /**
         * Sets the "paused" state of the TriggerGroupWrapper
         * 
         * @param paused true if "paused", false otherwise
         */
        public void setPaused(boolean paused) {
            this.paused = paused;
        }
        
        /**
         * Adds a Trigger's ID to the list of Triggers in this group.
         * 
         * @param triggerId a Trigger's ID
         */
        public void addTrigger(String triggerId) {
            if (!triggers.contains(triggerId)) {
                triggers.add(triggerId);
            }
        }
        
        /**
         * Removes a Trigger's ID from the list of Triggers in this group.
         * 
         * @param triggerId a Trigger's ID
         */
        public void removeTrigger(String triggerId) {
            if (triggers.contains(triggerId)) {
                triggers.remove(triggerId);
            }
        }
        
        /**
         * Returns a list of all Trigger's names (IDs) in this group.
         * 
         * @return a list of Trigger's names
         */
        public List<String> getTriggerNames() {
            return triggers;
        }
        
        /**
         * Returns a JsonValue object wrapper around the object map for the TriggerGroupWrapper.
         * 
         * @return a JsonValue object
         */
        public JsonValue getValue() {
            Map<String, Object> valueMap = new HashMap<String, Object>();
            valueMap.put("triggers", triggers);
            valueMap.put("name", name);
            valueMap.put("paused", paused);
            return new JsonValue(valueMap);
        }
    }
    
    
    /**
     * A wrapper the contains all necessary information for a Jobs's group
     */
    protected class JobGroupWrapper {
       
       private String name;
       private List<String> jobs;
       private boolean paused;
       
       /**
        * Creates a JobGroupWrapper from a job name
        * 
        * @param jobName a job name
        */
       public JobGroupWrapper(String jobName) {
           jobs = new ArrayList<String>();
           name = jobName;
           paused = false;
           
       }
       
       /**
        * Creates a JobGroupWrapper from a JsonValue object
        * 
        * @param value a JsonValue object
        */
       public JobGroupWrapper(JsonValue value) {
           this(value.asMap());
       }
       
       /**
        * Creates a JobGroupWrapper from an object map
        * 
        * @param map    an object map
        */
       public JobGroupWrapper(Map<String, Object> map) {
           name = (String)map.get("name");
           paused = (Boolean)map.get("paused");
           jobs = (List<String>)map.get("jobs");
       }
       
       /**
        * Return the name of the Job
        * 
        * @return the name of the Job
        */
       public String getName() {
           return name;
       }
       
       /**
        * Returns true if the Job is in the "paused" state, false otherwise
        * 
        * @return   true if the Job is in the "paused" state, false otherwise
        */
       public boolean isPaused() {
           return paused;
       }

       /**
        * Sets the JobGroupWrapper in the "paused" state.
        */
       public void pause() {
           setPaused(true);
       }

       /**
        * Resumes the JobGroupWrapper form the "paused" state.
        */
       public void resume() {
           setPaused(false);
       }

       /**
        * Sets the "paused" state of the JobGroupWrapper
        * 
        * @param paused true if "paused", false otherwise
        */
       public void setPaused(boolean paused) {
           this.paused = paused;
       }
       
       /**
        * Adds a Job's ID to the list of Jobs in this group.
        * 
        * @param jobId a Job's ID
        */
       public void addJob(String jobId) {
           if (!jobs.contains(jobId)) {
               jobs.add(jobId);
           }
       }

       /**
        * Removes a Job's ID from the list of Jobs in this group.
        * 
        * @param jobId a Job's ID
        */
       public void removeJob(String jobId) {
           if (jobs.contains(jobId)) {
               jobs.remove(jobId);
           }
       }
       
       /**
        * Returns a list of all Job's names (IDs) in this group.
        * 
        * @return a list of Job's names
        */
       public List<String> getJobNames() {
           return jobs;
       }
       
       /**
        * Returns a JsonValue object wrapper around the object map for the JobGroupWrapper.
        * 
        * @return a JsonValue object
        */
       public JsonValue getValue() {
           Map<String, Object> valueMap = new HashMap<String, Object>();
           valueMap.put("jobs", jobs);
           valueMap.put("name", name);
           valueMap.put("paused", paused);
           return new JsonValue(valueMap);
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

    

}
