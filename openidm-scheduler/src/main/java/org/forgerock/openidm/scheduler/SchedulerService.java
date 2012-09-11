/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.scheduler;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetJsonResource;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.quartz.impl.SchedulerServiceJob;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.RepositoryService;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduler service using Quartz
 * 
 * @author aegloff
 * @author ckienle
 */

@Component(name = "org.forgerock.openidm.scheduler", immediate=true, policy=ConfigurationPolicy.OPTIONAL)
@Service(value = {SchedulerService.class, JsonResource.class}, serviceFactory=false) 
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Scheduler Service using Quartz"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "scheduler")
})
public class SchedulerService extends ObjectSetJsonResource {
    final static Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    // Keys in the OSGi configuration     
    public final static String SCHEDULE_ENABLED = "enabled";
    public final static String SCHEDULE_TYPE = "type";
    public final static String SCHEDULE_START_TIME = "startTime";
    public final static String SCHEDULE_END_TIME = "endTime";
    public final static String SCHEDULE_CRON_SCHEDULE = "schedule";
    public final static String SCHEDULE_TIME_ZONE = "timeZone";
    public final static String SCHEDULE_INVOKE_SERVICE = "invokeService";
    public final static String SCHEDULE_INVOKE_CONTEXT = "invokeContext";
    public final static String SCHEDULE_PERSISTED = "persisted";
    public final static String SCHEDULE_MISFIRE_POLICY = "misfirePolicy";

    // Valid configuration values
    public final static String SCHEDULE_TYPE_CRON = "cron"; 
    
    // Default service PID prefix to use if the invokeService name is a fragment
    public final static String SERVICE_RDN_PREFIX = "org.forgerock.openidm.";
    
    // Misfire Policies
    public final static String MISFIRE_POLICY_DO_NOTHING = "doNothing";
    public final static String MISFIRE_POLICY_FIRE_AND_PROCEED = "fireAndProceed";
    
    // Internal service tracker
    final static String SERVICE_TRACKER = "scheduler.service-tracker";
    final static String SERVICE_PID = "scheduler.service-pid";
    
    final static String GROUP_NAME = "scheduler-service-group";
    
    private static Scheduler inMemoryScheduler;
    private static Scheduler persistentScheduler;
    private SchedulerConfig schedulerConfig = null;
    
    private Map<String, ScheduleConfigService> configMap = new HashMap<String, ScheduleConfigService>();
    private static Object CONFIG_SERVICE_LOCK = new Object();
    
    private static Object LOCK = new Object();
    
    private boolean started = false;
    
    EnhancedConfig enhancedConfig = JSONEnhancedConfig.newInstance();
    
    // Optional user defined name for this instance, derived from the file install name
    String configFactoryPID;
    
    // Scheduling
    Boolean localSchedulePersisted = false;
    Scheduler localScheduler = null;
    String localJobName = null;
    
    // Tracks OSGi services that match the configured service PID
    ServiceTracker scheduledServiceTracker;
    
    @Reference
    ConfigurationAdmin configAdmin;
    
    @Reference
    RepositoryService repo;
    
    @Activate
    void activate(ComponentContext compContext) throws SchedulerException, ParseException { 
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        
        String pid = (String)compContext.getProperties().get("config.factory-pid");
        if (pid != null) {
            logger.warn("Please rename the schedule configuration file for " + pid 
                    + " to conform the 'schedule-<name>.json' format");
            return;
        }
        
        // Initialize the schedulers (if they haven't been already)
        initInMemoryScheduler();
        initPersistentScheduler(compContext);
        
        synchronized (CONFIG_SERVICE_LOCK) {
            // TODO: This should be reworked to start after all "core" services are available
            logger.info("Starting Scheduler Service");
            started = true;
            
            // Start processing schedules
            logger.info("Starting Volatile Scheduler");
            inMemoryScheduler.start();
            logger.info("Starting Persistent Scheduler");
            persistentScheduler.start();
            
            logger.info("There are {} jobs waiting to be scheduled", configMap.size());
            Set<String> keys = configMap.keySet();
            for (String key : keys) {
                ScheduleConfigService service = configMap.get(key);
                try {
                    addSchedule(service.getScheduleConfig(), service.getJobName(), false, true);
                } catch (ObjectAlreadyExistsException e) {
                    logger.debug("Job {} already scheduled", service.getJobName());
                }
            }
            logger.info("Scheduling waiting schedules");
        }
    }
    
    /**
     * Initialize the service configuration
     * @param compContext
     * @throws InvalidException if the configuration is invalid.
     */
    /*private ScheduleConfig initConfig(ComponentContext compContext) throws InvalidException {
        
        // Optional property SERVICE_FACTORY_PID set by JSONConfigInstaller
        configFactoryPID = (String) compContext.getProperties().get("config.factory-pid");
        Map<String, Object> config = enhancedConfig.getConfiguration(compContext);
        logger.debug("Scheduler service activating with configuration {}", config);
        if (config == null) {
            return null;
        }
        return new ScheduleConfig(config);
    }*/
    
    public void registerConfigService(ScheduleConfigService service) throws SchedulerException, ParseException {
        logger.debug("Registering new ScheduleConfigService");
        synchronized (CONFIG_SERVICE_LOCK) {
            configMap.put(service.getJobName(), service);
            if (!started) {
                logger.debug("The Scheduler Service has not been started yet, storing new Schedule {}", service.getJobName());
            } else {
                logger.debug("Adding new Schedule {}", service.getJobName());
                try {
                    addSchedule(service.getScheduleConfig(), service.getJobName(), false, true);
                } catch (ObjectAlreadyExistsException e) {
                    logger.debug("Job {} already scheduled", service.getJobName());
                }
            }
        }
    }
    
    public void unregisterConfigService(ScheduleConfigService service) {
        synchronized (CONFIG_SERVICE_LOCK) {
            if (!started) {
                configMap.remove(service.getJobName());                                                                                                                    
            } else {
                
            }
        }
    }
    
    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Scheduler Service {}", compContext);
    }   
    
    /**
     * Schedules a job.
     * 
     * @param scheduleConfig    The schedule configuration
     * @param jobName           The job name
     * @param update            Whether to delete the old job if present
     * @param fromConfFile      Whether the job was configured in a "/conf" file
     * @return                  true if the job was scheduled, false otherwise
     * @throws SchedulerException
     * @throws ParseException
     * @throws ObjectAlreadyExistsException
     */
    public boolean addSchedule(ScheduleConfig scheduleConfig, String jobName, boolean update, boolean fromConfFile) 
            throws SchedulerException, ParseException, ObjectAlreadyExistsException {
        Scheduler scheduler = null;
        if (!scheduleConfig.getEnabled()) {
            logger.info("Scheduler for {} is disabled", configFactoryPID);
            return false;
        }
        
        try {
            // Lock access to the scheduler so that a schedule is not added during a config update
            synchronized (LOCK) {
                scheduler = getScheduler(scheduleConfig);
                if (fromConfFile) {
                    // Schedule is configured from a file in the "conf" directory, store local parameters
                    localScheduler = scheduler;
                    localJobName = jobName;
                    localSchedulePersisted = scheduleConfig.getPersisted();
                }
                if (scheduler != null && scheduleConfig.getCronSchedule() != null 
                        && scheduleConfig.getCronSchedule().length() > 0) {
                    JobDetail job = new JobDetail(jobName, GROUP_NAME, SchedulerServiceJob.class);
                    job.setVolatility(scheduleConfig.getPersisted());
                    job.setJobDataMap(createJobDataMap(scheduleConfig));
                    Trigger trigger = createTrigger(scheduleConfig, jobName);
                    if (update) {
                        // Update the job by first deleting it, then scheduling the new version
                        scheduler.deleteJob(jobName, GROUP_NAME);
                    }
                    // Schedule the Job
                    scheduler.scheduleJob(job, trigger);
                    logger.info("Job {} scheduled with schedule {}, timezone {}, start time {}, end time {}.",
                            new Object[] { jobName, scheduleConfig.getCronSchedule(), scheduleConfig.getTimeZone(),
                                    scheduleConfig.getStartTime(), scheduleConfig.getEndTime() });
                }
            }
        } catch (ParseException ex) {
            logger.warn("Parsing of scheduler configuration failed, can not create scheduler service for " 
                    + jobName + ": " + ex.getMessage(), ex);
            throw ex;
        } catch (ObjectAlreadyExistsException ex) {
            throw ex;
        } catch (SchedulerException ex) {
            logger.warn("Failed to create scheduler service for " + jobName + ": " + ex.getMessage(), ex);
            throw ex;
        }
        
        return true;
    }
    
    /**
     * Creates and returns a CronTrigger using the supplied schedule configuration.
     * 
     * @param scheduleConfig    The schedule configuration
     * @param jobName           The name of the job to associate the trigger with
     * @return                  The created Trigger
     * @throws ParseException
     */
    private Trigger createTrigger(ScheduleConfig scheduleConfig, String jobName) throws ParseException {
        String cronSchedule = scheduleConfig.getCronSchedule();
        Date startTime = scheduleConfig.getStartTime();
        Date endTime = scheduleConfig.getEndTime();
        String misfirePolicy = scheduleConfig.getMisfirePolicy();
        TimeZone timeZone = scheduleConfig.getTimeZone();
        
        CronTrigger trigger = new CronTrigger("trigger-" + jobName, GROUP_NAME, cronSchedule);
        
        if (startTime != null) {
            trigger.setStartTime(startTime); // TODO: review time zone consistency with cron trigger timezone
        }
        
        if (endTime != null) {
            trigger.setEndTime(endTime);
        }
        if (timeZone != null) {
            trigger.setTimeZone(timeZone);
        }
        
        if (misfirePolicy.equals(MISFIRE_POLICY_FIRE_AND_PROCEED)) {
            trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
        } else if (misfirePolicy.equals(MISFIRE_POLICY_DO_NOTHING)) {
            trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        }
        return trigger;
    }
    
    /**
     * Creates and returns a JobDataMap using the supplied schedule configuration.
     * 
     * @param scheduleConfig    The schedule configuration
     * @return                  The created JobDataMap
     */
    private JobDataMap createJobDataMap(ScheduleConfig scheduleConfig) {
        String invokeService = scheduleConfig.getInvokeService();
        Object invokeContext = scheduleConfig.getInvokeContext();
        JobDataMap map = new JobDataMap();
        map.put(ScheduledService.CONFIG_NAME, "scheduler"+ (configFactoryPID != null ? "-" + configFactoryPID : ""));
        map.put(ScheduledService.CONFIGURED_INVOKE_SERVICE, invokeService);
        map.put(ScheduledService.CONFIGURED_INVOKE_CONTEXT, invokeContext);
        return map;
    }
    
    /**
     * Returns the Scheduler corresponding to whether the supplied schedule configuration is persistent.
     * 
     * @param scheduleConfig    The schedule configuration
     * @return                  The Scheduler
     * @throws SchedulerException
     */
    private Scheduler getScheduler(ScheduleConfig scheduleConfig) throws SchedulerException {
        if (scheduleConfig.getPersisted()) {
            return persistentScheduler;
        }
        return inMemoryScheduler;
    }
    
    /**
     * Creates a random Job name.
     * 
     * @return  A job name
     */
    private String createJobName() {
        StringBuilder sb = new StringBuilder("job_");
        sb.append(UUID.randomUUID());
        return sb.toString();
    }
    
    /**
     * Determines if a job already exists.
     * 
     * @param jobName       The name of the job
     * @param persisted     If the job is persisted or not
     * @return              True if the job exists, false otherwise
     * @throws SchedulerException
     */
    private boolean jobExists(String jobName, boolean persisted) throws SchedulerException {
        if (!persisted) {
            return (inMemoryScheduler.getJobDetail(jobName, GROUP_NAME) != null);
            
        }
        return (persistentScheduler.getJobDetail(jobName, GROUP_NAME) != null);
        
    }

    @Override
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        try {
            if (id == null) {
                id = createJobName();
            } else {
                id = trimTrailingSlash(id);
            }
            object.put("_id", id);
            ScheduleConfig scheduleConfig = new ScheduleConfig(new JsonValue(object));

            // Check defaults
            if (scheduleConfig.getEnabled() == null) {
                scheduleConfig.setEnabled(true);
            }
            if (scheduleConfig.getPersisted() == null) {
                scheduleConfig.setPersisted(true);
            }

            try {
                addSchedule(scheduleConfig, id, false, false);
            } catch (ParseException e) {
                throw new ObjectSetException(ObjectSetException.BAD_REQUEST, e);
            } catch (ObjectAlreadyExistsException e) {
                throw new ObjectSetException(ObjectSetException.CONFLICT, e);
            } catch (SchedulerException e) {
                throw new ObjectSetException(ObjectSetException.INTERNAL_ERROR, e);
            }
        } catch (JsonException e) {
            throw new ObjectSetException(ObjectSetException.BAD_REQUEST, "Error creating schedule", e);
        }
    }

    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        Map<String, Object> resultMap = null;
        try {
            Scheduler scheduler = null;
            JobDetail job = null;
            boolean persisted = false;
            if (jobExists(id, true)) {
                persisted = true;
                scheduler = persistentScheduler;
            } else if (jobExists(id, false)) {
                scheduler = inMemoryScheduler;
            } else {
                throw new ObjectSetException(ObjectSetException.NOT_FOUND, "Schedule does not exist");
            }
            job = scheduler.getJobDetail(id, GROUP_NAME);
            CronTrigger trigger = (CronTrigger)scheduler.getTrigger("trigger-" + id, GROUP_NAME);
            JobDataMap dataMap = job.getJobDataMap();
            ScheduleConfig config = new ScheduleConfig(trigger, dataMap, persisted);
            resultMap = (Map<String, Object>)config.getConfig().getObject();
            resultMap.put("_id", id);

        } catch (SchedulerException e) {
            throw new ObjectSetException(ObjectSetException.INTERNAL_ERROR, e);
        }
        return resultMap;
    }

    @Override
    public void update(String id, String rev, Map<String, Object> object)
            throws ObjectSetException {
        try {
            if (id == null) {
                throw new ObjectSetException(ObjectSetException.BAD_REQUEST, "No ID specified");
            }
            object.put("_id", id);
            
            // Default incoming config to "persisted" if not specified
            Object persistedValue = object.get(SchedulerService.SCHEDULE_PERSISTED);
            if (persistedValue == null) {
                object.put(SchedulerService.SCHEDULE_PERSISTED, new Boolean(true));
            }
            
            ScheduleConfig scheduleConfig = new ScheduleConfig(new JsonValue(object));

            try {
                if (!jobExists(id, scheduleConfig.getPersisted())) {
                    throw new NotFoundException();
                } else {
                    // Update the Job
                    addSchedule(scheduleConfig, id, true, false);
                }
            } catch (ParseException e) {
                throw new ObjectSetException(ObjectSetException.BAD_REQUEST, e);
            } catch (ObjectAlreadyExistsException e) {
                throw new ObjectSetException(ObjectSetException.CONFLICT, e);
            } catch (SchedulerException e) {
                throw new ObjectSetException(ObjectSetException.INTERNAL_ERROR, e);
            }
        } catch (JsonException e) {
            throw new ObjectSetException(ObjectSetException.BAD_REQUEST, "Error updating schedule", e);
        }
    }

    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        try {
            if (id == null) {
                throw new ObjectSetException(ObjectSetException.BAD_REQUEST, "No ID specified");
            }
            try {
                if (jobExists(id, true)) {
                    persistentScheduler.deleteJob(id, GROUP_NAME);
                } else if (jobExists(id, false)) {
                    inMemoryScheduler.deleteJob(id, GROUP_NAME);
                } else {
                    throw new ObjectSetException(ObjectSetException.BAD_REQUEST, "Schedule does not exist");
                }
            } catch (SchedulerException e) {
                throw new ObjectSetException(ObjectSetException.INTERNAL_ERROR, e);
            }
        } catch (JsonException e) {
            throw new ObjectSetException(ObjectSetException.BAD_REQUEST, "Error updating schedule", e);
        }
    }

    @Override
    public void patch(String id, String rev, Patch patch)
            throws ObjectSetException {
        throw new ForbiddenException();
    }

    @Override
    public Map<String, Object> query(String id, Map<String, Object> params)
            throws ObjectSetException {
        String queryId = (String)params.get(QueryConstants.QUERY_ID);
        if (queryId == null) {
            throw new ObjectSetException(ObjectSetException.BAD_REQUEST, "query-id parameters");
        }
        Map<String, Object> resultMap = null;
        try {
            try {
                if (queryId.equals("query-all-ids")) {
                    // Query all the Job IDs in both schedulers
                    String[] persistentJobNames = persistentScheduler.getJobNames(GROUP_NAME);
                    String[] inMemoryJobNames = inMemoryScheduler.getJobNames(GROUP_NAME);
                    List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
                    if (persistentJobNames != null) {
                        for (String job : persistentJobNames) {
                            Map<String, String> idMap = new HashMap<String, String>();
                            idMap.put("_id", job);
                            resultList.add(idMap);
                        }
                    }
                    if (inMemoryJobNames != null) {
                        for (String job : inMemoryJobNames) {
                            Map<String, String> idMap = new HashMap<String, String>();
                            idMap.put("_id", job);
                            resultList.add(idMap);
                        }
                    }
                    resultMap = new HashMap<String, Object>();
                    resultMap.put(QueryConstants.QUERY_RESULT, resultList);
                } else {
                    throw new ObjectSetException(ObjectSetException.FORBIDDEN, "Unsupported query-id: " + queryId);
                }
            } catch (SchedulerException e) {
                throw new ObjectSetException(ObjectSetException.INTERNAL_ERROR, e);
            }
        } catch (JsonException e) {
            throw new ObjectSetException(ObjectSetException.BAD_REQUEST, "Error updating schedule", e);
        }
        return resultMap;
    }

    @Override
    public Map<String, Object> action(String id, Map<String, Object> params)
            throws ObjectSetException {
        String action = (String)params.get("_action");
        try {
            if (action.equals("create")) {
                id = createJobName();
                params.put("_id", id);
                try {
                    if (jobExists(id, true) || jobExists(id, false)) {
                        throw new ObjectSetException(ObjectSetException.BAD_REQUEST, "Schedule already exists");
                    }
                    create(id, params);
                    return params;
                } catch (SchedulerException e) {
                    throw new ObjectSetException(ObjectSetException.INTERNAL_ERROR, e);
                }
            } else {
                throw new ObjectSetException(ObjectSetException.BAD_REQUEST, "Unknown action: " + action);
            }
        } catch (JsonException e) {
            throw new ObjectSetException(ObjectSetException.BAD_REQUEST, "Error updating schedule", e);
        }
    }
    
    private String trimTrailingSlash(String id) {
        if (id.endsWith("/")) {
            return id.substring(0, id.length()-1);
        }
        return id;
    }
    
    public boolean isConfigured() {
        if (schedulerConfig == null) {
            return false;
        }
        return true;
    }
    
    public void initPersistentScheduler(ComponentContext compContext) throws SchedulerException {
        boolean alreadyConfigured = (schedulerConfig != null);
        JsonValue configValue = enhancedConfig.getConfigurationAsJson(compContext);
        schedulerConfig = new SchedulerConfig(configValue);
        if (alreadyConfigured) {
            // Close current scheduler
            if (persistentScheduler != null && persistentScheduler.isStarted()) {
                persistentScheduler.shutdown();
            }
        }
        createPersistentScheduler();
    }
    
    private void createPersistentScheduler() throws SchedulerException {
        // Get the persistent scheduler using our custom JobStore implementation
        logger.info("Creating Persistent Scheduler");
        StdSchedulerFactory sf = new StdSchedulerFactory();
        sf.initialize(schedulerConfig.toProps());
        persistentScheduler = sf.getScheduler();
    }

    private void initInMemoryScheduler() throws SchedulerException {
        try {
            if (inMemoryScheduler == null) {
                // Quartz tries to be too smart about classloading, 
                // but relies on the thread context classloader to load classload helpers
                // That is not a good idea in OSGi, 
                // hence, hand it the OSGi classloader for the ClassLoadHelper we want it to find
                ClassLoader original = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(CascadingClassLoadHelper.class.getClassLoader());
                // Get the in-memory scheduler
                // Must use DirectSchedulerFactory instance so that it does not confict with
                // the StdSchedulerFactory (used to create the persistent schedulers).
                logger.info("Creating In-Memory Scheduler");
                DirectSchedulerFactory.getInstance().createVolatileScheduler(10);
                inMemoryScheduler = DirectSchedulerFactory.getInstance().getScheduler();
                // Set back to the original thread context classloader
                Thread.currentThread().setContextClassLoader(original);
            }
        } catch (SchedulerException ex) {
            logger.warn("Failure in initializing the scheduler facility " + ex.getMessage(), ex);
            throw ex;
        }
        logger.info("Scheduler facility started");
    }
}
