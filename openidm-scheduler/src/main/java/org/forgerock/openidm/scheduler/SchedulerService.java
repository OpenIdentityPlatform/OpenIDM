/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012-2014 ForgeRock AS. All Rights Reserved
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

import java.io.IOException;
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
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.cluster.ClusterManagementService;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.quartz.impl.RepoJobStore;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.quartz.impl.SchedulerServiceJob;
import org.forgerock.openidm.quartz.impl.StatefulSchedulerServiceJob;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.openidm.util.ResourceUtil;
import org.osgi.framework.Constants;
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

@Component(name = "org.forgerock.openidm.scheduler", immediate=true, policy=ConfigurationPolicy.REQUIRE)
@Service(value = {SchedulerService.class, RequestHandler.class}, serviceFactory=false)
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Scheduler Service using Quartz"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/scheduler*")
})
public class SchedulerService implements RequestHandler {
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
    public final static String SCHEDULE_INVOKE_LOG_LEVEL = "invokeLogLevel";
    public final static String SCHEDULE_PERSISTED = "persisted";
    public final static String SCHEDULE_MISFIRE_POLICY = "misfirePolicy";
    public final static String SCHEDULE_CONCURRENT_EXECUTION = "concurrentExecution";

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

    final static String CONFIG = "schedule.config";

    private static Scheduler inMemoryScheduler;
    private static Scheduler persistentScheduler = null;
    private static SchedulerConfig schedulerConfig = null;

    private Map<String, ScheduleConfigService> configMap = new HashMap<String, ScheduleConfigService>();
    private static Object CONFIG_SERVICE_LOCK = new Object();

    private static Object LOCK = new Object();

    private boolean executePersistentSchedules = false;
    private boolean started = false;

    EnhancedConfig enhancedConfig = JSONEnhancedConfig.newInstance();

    private final ObjectMapper mapper = new ObjectMapper();

    // Optional user defined name for this instance, derived from the file install name
    String configFactoryPID;

    // Tracks OSGi services that match the configured service PID
    ServiceTracker scheduledServiceTracker;

    @Reference
    ClusterManagementService clusterManager;

    @Reference(name = "ref_SchedulerService_PolicyService", 
            target = "(" + ServerConstants.ROUTER_PREFIX + "=/policy*)")
    protected RouteService policy;

    /** Internal object set router service. */
    @Reference(name = "ref_SchedulerService_RepositoryService", bind = "bindRepo",
            unbind = "unbindRepo", target = "(" + ServerConstants.ROUTER_PREFIX + "=/repo*)")
    protected RouteService repo;

    protected void bindRepo(final RouteService service) throws ResourceException {
        logger.debug("binding RepositoryService");
        RepoJobStore.setServerContext(service.createServerContext());
    }

    protected void unbindRepo(final RouteService service) {
        logger.debug("unbinding RepositoryService");
        RepoJobStore.setServerContext(null);
    }
    
    
    
    @Activate
    void activate(ComponentContext compContext) throws SchedulerException, ParseException {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());

        String pid = (String)compContext.getProperties().get("config.factory-pid");
        if (pid != null) {
            logger.warn("Please rename the schedule configuration file for " + pid
                    + " to conform to the 'schedule-<name>.json' format");
            return;
        }

        synchronized (CONFIG_SERVICE_LOCK) {
            // TODO: This should be reworked to start after all "core" services are available
            logger.info("Starting Scheduler Service");
            started = true;

            // Initialize the schedulers (if they haven't been already)
            initInMemoryScheduler();
            initPersistentScheduler(compContext);

            // Start processing schedules
            logger.info("Starting Volatile Scheduler");
            inMemoryScheduler.start();

            if (executePersistentSchedules) {
                logger.info("Starting Persistent Scheduler");
                persistentScheduler.start();
            } else {
                logger.info("Persistent Schedules will not be executed on this node");
            }

            logger.info("There are {} jobs waiting to be scheduled", configMap.size());
            Set<String> keys = configMap.keySet();
            for (String key : keys) {
                ScheduleConfigService service = configMap.get(key);
                try {
                    addSchedule(service.getScheduleConfig(), service.getJobName(), false);
                } catch (ObjectAlreadyExistsException e) {
                    logger.debug("Job {} already scheduled", service.getJobName());
                }
            }
            logger.info("Scheduling waiting schedules");
        }
    }

    public void registerConfigService(ScheduleConfigService service) throws SchedulerException, ParseException {
        synchronized (CONFIG_SERVICE_LOCK) {
            boolean update = false;
            if (configMap.containsKey(service.getJobName())) {
                logger.debug("Updating Schedule");
                configMap.put(service.getJobName(), service);
                update = true;
            }
            logger.debug("Registering new ScheduleConfigService");
            configMap.put(service.getJobName(), service);
            if (!started) {
                logger.debug("The Scheduler Service has not been started yet, storing new Schedule {}", service.getJobName());
            } else {
                logger.debug("Adding schedule {}", service.getJobName());
                try {
                    addSchedule(service.getScheduleConfig(), service.getJobName(), update);
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
        try {
            if (inMemoryScheduler != null && inMemoryScheduler.isStarted()) {
                inMemoryScheduler.shutdown();
            }
        } catch (SchedulerException e) {
            logger.error("Error shutting down in-memory scheduler", e);
        } finally {
            inMemoryScheduler = null;
        }
        try {
            if (persistentScheduler != null && persistentScheduler.isStarted()) {
                persistentScheduler.shutdown();
            }
        } catch (SchedulerException e) {
            logger.error("Error shutting down persistent scheduler", e);
        }
    }

    /**
     * Schedules a job.
     *
     * @param scheduleConfig    The schedule configuration
     * @param jobName           The job name
     * @param update            Whether to delete the old job if present
     * @return                  true if the job was scheduled, false otherwise
     * @throws SchedulerException
     * @throws ParseException
     * @throws ObjectAlreadyExistsException
     */
    public boolean addSchedule(ScheduleConfig scheduleConfig, String jobName, boolean update)
            throws SchedulerException, ParseException, ObjectAlreadyExistsException {
        Scheduler scheduler = null;

        try {
            // Lock access to the scheduler so that a schedule is not added during a config update
            synchronized (LOCK) {
                scheduler = getScheduler(scheduleConfig);
                JobDetail existingJob = scheduler.getJobDetail(jobName, GROUP_NAME);
                boolean exists = existingJob != null;

                // Determine the schedule class based on whether the job has concurrent execution enabled/disabled
                Class scheduleClass = null;
                if (scheduleConfig.getConcurrentExecution()) {
                    scheduleClass = SchedulerServiceJob.class;
                } else {
                    scheduleClass = StatefulSchedulerServiceJob.class;
                }

                // Check if the new or updated job is disabled
                /*if (!scheduleConfig.getEnabled()) {
                    logger.info("Schedule {} is disabled", jobName);
                    if (jobExists(jobName, scheduleConfig.getPersisted()) &&
                            scheduler.deleteJob(jobName, GROUP_NAME)) {
                        logger.debug("Schedule was deleted from scheduler");
                    }
                    return false;
                }*/

                // Attempt to add the scheduler
                if (scheduler != null && scheduleConfig.getCronSchedule() != null
                        && scheduleConfig.getCronSchedule().length() > 0) {
                    JobDetail job = new JobDetail(jobName, GROUP_NAME, scheduleClass);
                    job.setVolatility(scheduleConfig.getPersisted());
                    job.setJobDataMap(createJobDataMap(scheduleConfig));
                    Trigger trigger = createTrigger(scheduleConfig, jobName);

                    if (update) {
                        if (exists) {
                            // Update the job by first deleting it, then scheduling the new version
                            scheduler.deleteJob(jobName, GROUP_NAME);
                        }
                    }

                    // check if it is enabled
                    if (scheduleConfig.getEnabled()) {
                        // Set to non-durable so that jobs won't persist after last firing
                        job.setDurability(false);
                        // Schedule the Job (with trigger)
                        scheduler.scheduleJob(job, trigger);
                        logger.info("Job {} scheduled with schedule {}, timezone {}, start time {}, end time {}.",
                                new Object[] { jobName, scheduleConfig.getCronSchedule(), scheduleConfig.getTimeZone(),
                                scheduleConfig.getStartTime(), scheduleConfig.getEndTime() });
                    } else {
                        // Set the job to durable so that it can exist without a trigger (since the job is "disabled")
                        job.setDurability(true);
                        // Add the job (no trigger)
                        scheduler.addJob(job, false);
                        logger.info("Job {} added with schedule {}, timezone {}, start time {}, end time {}.",
                                new Object[] { jobName, scheduleConfig.getCronSchedule(), scheduleConfig.getTimeZone(),
                                scheduleConfig.getStartTime(), scheduleConfig.getEndTime() });
                    }

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
    private CronTrigger createTrigger(ScheduleConfig scheduleConfig, String jobName) throws ParseException {
        String cronSchedule = scheduleConfig.getCronSchedule();
        Date startTime = scheduleConfig.getStartTime();
        Date endTime = scheduleConfig.getEndTime();
        String misfirePolicy = scheduleConfig.getMisfirePolicy();
        TimeZone timeZone = scheduleConfig.getTimeZone();

        CronTrigger trigger = new CronTrigger("trigger-" + jobName, GROUP_NAME, cronSchedule);
        trigger.setJobName(jobName);
        trigger.setJobGroup(GROUP_NAME);

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
        String invokeLogLevel = scheduleConfig.getInvokeLogLevel();
        JobDataMap map = new JobDataMap();
        map.put(ScheduledService.CONFIG_NAME, "scheduler"+ (configFactoryPID != null ? "-" + configFactoryPID : ""));
        map.put(ScheduledService.CONFIGURED_INVOKE_SERVICE, invokeService);
        map.put(ScheduledService.CONFIGURED_INVOKE_CONTEXT, invokeContext);
        map.put(ScheduledService.CONFIGURED_INVOKE_LOG_LEVEL, invokeLogLevel);
        map.put(CONFIG, scheduleConfig.getConfig().toString());
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
        } else {
            return (persistentScheduler.getJobDetail(jobName, GROUP_NAME) != null);
        }
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        try {
            String id = request.getNewResourceId() == null
                    ? UUID.randomUUID().toString()
                    : trimTrailingSlash(request.getNewResourceId()); // is the trim necessary?
            Map<String, Object> object = request.getContent().asMap();
            object.put("_id", id);

            ScheduleConfig scheduleConfig = new ScheduleConfig(new JsonValue(object));

            // Check defaults
            if (scheduleConfig.getEnabled() == null) {
                scheduleConfig.setEnabled(true);
            }
            if (scheduleConfig.getPersisted() == null) {
                scheduleConfig.setPersisted(true);
            }

            addSchedule(scheduleConfig, id, false);
            handler.handleResult(new Resource(id, null, new JsonValue(object)));
        } catch (ParseException e) {
            handler.handleError(new BadRequestException(e.getMessage(), e));
        } catch (ObjectAlreadyExistsException e) {
            handler.handleError(new ConflictException(e.getMessage(), e));
        } catch (SchedulerException e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        } catch (JsonException e) {
            handler.handleError(new BadRequestException("Error creating schedule", e));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            if (request.getResourceNameObject().isEmpty()) {
                throw new BadRequestException("Empty resourceId");
            }
            
            Scheduler scheduler = null;
            if (jobExists(request.getResourceName(), true)) {
                scheduler = persistentScheduler;
            } else if (jobExists(request.getResourceName(), false)) {
                scheduler = inMemoryScheduler;
            } else {
                throw new NotFoundException("Schedule does not exist");
            }
            JobDetail job = scheduler.getJobDetail(request.getResourceName(), GROUP_NAME);
            JobDataMap dataMap = job.getJobDataMap();
            ScheduleConfig config = new ScheduleConfig(parseStringified((String)dataMap.get(CONFIG)));
            Map<String, Object> resultMap = (Map<String, Object>) config.getConfig().getObject();
            resultMap.put("_id", request.getResourceName());
            handler.handleResult(new Resource(request.getResourceName(), null, new JsonValue(resultMap)));
        } catch (SchedulerException e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        try {
            if (request.getResourceNameObject().isEmpty()) {
                throw new BadRequestException("Empty resourceId");
            }
            Map<String, Object> object = request.getContent().asMap();
            object.put("_id", request.getResourceName());

            // Default incoming config to "persisted" if not specified
            Object persistedValue = object.get(SchedulerService.SCHEDULE_PERSISTED);
            if (persistedValue == null) {
                object.put(SchedulerService.SCHEDULE_PERSISTED, new Boolean(true));
            }

            ScheduleConfig scheduleConfig = new ScheduleConfig(new JsonValue(object));

            if (!jobExists(request.getResourceName(), scheduleConfig.getPersisted())) {
                throw new NotFoundException();
            } else {
                // Update the Job
                addSchedule(scheduleConfig, request.getResourceName(), true);
                handler.handleResult(new Resource(request.getResourceName(), null, new JsonValue(null)));
            }
        } catch (ParseException e) {
            handler.handleError(new BadRequestException(e.getMessage(), e));
        } catch (ObjectAlreadyExistsException e) {
            handler.handleError(new ConflictException(e.getMessage(), e));
        } catch (SchedulerException e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        } catch (JsonException e) {
            handler.handleError(new BadRequestException("Error updating schedule", e));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        try {
            if (request.getResourceNameObject().isEmpty()) {
                throw new BadRequestException("Empty resourceId");
            }

            if (jobExists(request.getResourceName(), true)) {
                persistentScheduler.deleteJob(request.getResourceName(), GROUP_NAME);
            } else if (jobExists(request.getResourceName(), false)) {
                inMemoryScheduler.deleteJob(request.getResourceName(), GROUP_NAME);
            } else {
                throw new NotFoundException("Schedule does not exist");
            }
            handler.handleResult(new Resource(request.getResourceName(), null, new JsonValue(null)));
        } catch (JsonException e) {
            handler.handleError(new BadRequestException("Error updating schedule", e));
        } catch (SchedulerException e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            String queryId = request.getQueryId();
            if (queryId == null) {
                throw new BadRequestException( "query-id parameters");
            }
            Map<String, Object> resultMap = null;
            if (queryId.equals("query-all-ids")) {
                // Query all the Job IDs in both schedulers
                String[] persistentJobNames = null;
                persistentJobNames = persistentScheduler.getJobNames(GROUP_NAME);
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
                resultMap.put(QueryResult.FIELD_RESULT, resultList);
            } else {
                throw new ForbiddenException( "Unsupported query-id: " + queryId);
            }

            for (Map<String, String> r: (List<Map<String, String>>)resultMap.get(QueryResult.FIELD_RESULT)){
                handler.handleResource(new Resource(r.get("_id"), null, new JsonValue(r)));
            }
            handler.handleResult(new QueryResult());
        } catch (SchedulerException e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        } catch (JsonException e) {
            handler.handleError(new BadRequestException("Error updating schedule", e));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request, final ResultHandler<JsonValue> handler) {
        try {
            Map<String, String> params = request.getAdditionalParameters();

            if (params.get("_action") == null) {
                throw new BadRequestException("Expecting _action parameter");
            }

            String action = params.get("_action");
            if ("create".equals(action)) {
                String id = UUID.randomUUID().toString();
                params.put("_id", id);
                if (jobExists(id, true) || jobExists(id, false)) {
                    throw new BadRequestException("Schedule already exists");
                }
                CreateRequest createRequest = Requests.newCreateRequest(id, new JsonValue(params));
                handleCreate(context, createRequest, new ResultHandler<Resource>() {
                    @Override
                    public void handleError(ResourceException error) {
                        handler.handleError(error);
                    }

                    @Override
                    public void handleResult(Resource result) {
                        handler.handleResult(result.getContent());
                    }
                });
                handler.handleResult(new JsonValue(params));
            } else {
                throw new BadRequestException("Unknown action: " + action);
            }
        } catch (SchedulerException e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        } catch (JsonException e) {
            handler.handleError(new BadRequestException("Error updating schedule", e));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    private String trimTrailingSlash(String id) {
        if (id.endsWith("/")) {
            return id.substring(0, id.length()-1);
        }
        return id;
    }

    public void initPersistentScheduler(ComponentContext compContext) throws SchedulerException {
        boolean alreadyConfigured = (schedulerConfig != null);
        JsonValue configValue = enhancedConfig.getConfigurationAsJson(compContext);
        schedulerConfig = new SchedulerConfig(configValue, clusterManager.getInstanceId());
        if (alreadyConfigured) {
            // Close current scheduler
            if (persistentScheduler != null && persistentScheduler.isStarted()) {
                persistentScheduler.shutdown();
            }
        }
        if (schedulerConfig.executePersistentSchedulesEnabled()) {
            executePersistentSchedules = true;
        } else {
            executePersistentSchedules = false;
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

    private JsonValue parseStringified(String stringified) {
        JsonValue jsonValue = null;
        try {
            Map parsedValue = (Map) mapper.readValue(stringified, Map.class);
            jsonValue = new JsonValue(parsedValue);
        } catch (IOException ex) {
            throw new JsonException("String passed into parsing is not valid JSON", ex);
        }
        return jsonValue;
    }
}
