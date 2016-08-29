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
 * Portions copyright 2012-2016 ForgeRock AS.
 */

package org.forgerock.openidm.scheduler;

import static java.lang.Math.min;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openidm.repo.QueryConstants.QUERY_ALL_IDS;
import static org.forgerock.openidm.util.ResourceUtil.notSupported;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SortKey;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.cluster.ClusterManagementService;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.filter.JsonValueFilterVisitor;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.quartz.impl.SchedulerServiceJob;
import org.forgerock.openidm.quartz.impl.StatefulSchedulerServiceJob;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
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
 * Scheduler service using Quartz.
 */

@Component(name = "org.forgerock.openidm.scheduler", immediate=true, policy=ConfigurationPolicy.REQUIRE)
@Service(value = {SchedulerService.class, RequestHandler.class}, serviceFactory=false)
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Scheduler Service using Quartz"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/scheduler*")
})
public class SchedulerService implements RequestHandler {
    private final static Logger logger = LoggerFactory.getLogger(SchedulerService.class);

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

    // Misfire Policies
    public final static String MISFIRE_POLICY_DO_NOTHING = "doNothing";
    public final static String MISFIRE_POLICY_FIRE_AND_PROCEED = "fireAndProceed";

    private final static String GROUP_NAME = "scheduler-service-group";

    private final static String CONFIG = "schedule.config";
    private final static JsonValueFilterVisitor JSONVALUE_FILTER_VISITOR = new JsonValueFilterVisitor();

    /**
     * Supported actions on the scheduler service.
     */
    protected enum SchedulerAction { 
        create,
        listCurrentlyExecutingJobs,
        pauseJobs,
        resumeJobs
    }

    private static Scheduler inMemoryScheduler;
    private static Scheduler persistentScheduler = null;
    private static SchedulerConfig schedulerConfig = null;

    private final Map<String, ScheduleConfigService> configMap = new HashMap<>();
    private final static Object CONFIG_SERVICE_LOCK = new Object();

    private final static Object LOCK = new Object();

    private boolean executePersistentSchedules = false;
    private boolean started = false;

    private final ObjectMapper mapper = new ObjectMapper();

    // Optional user defined name for this instance, derived from the file install name
    String configFactoryPID;

    @Reference
    ClusterManagementService clusterManager;

    @Reference(name = "ref_SchedulerService_PolicyService",
            target = "(" + ServerConstants.ROUTER_PREFIX + "=/policy*)")
    protected RouteService policy;

    /*
    The RepoJobStore will attempt to get/create trigger group names by making calls on the router - see
    RepoJobStore#getTriggerGroupNames, which is ultimately called when this service is activated. This reference insures
    that the repo service implementation has been registered with the router prior to the activation of this service.
     */
    @Reference(name = "ref_SchedulerService_RepoService",
            target = "(" + ServerConstants.ROUTER_PREFIX + "=/repo/*)")
    protected RouteService repoService;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;
    
    protected void bindEnhancedConfig(EnhancedConfig enhancedConfig) {
    	this.enhancedConfig = enhancedConfig;
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

    /**
     * Registers a {@link ScheduleConfigService} and adds the scheduler if the scheduler has been started.
     * 
     * @param service the {@link ScheduleConfigService} to register.
     * @throws SchedulerException
     * @throws ParseException
     */
    public void registerConfigService(ScheduleConfigService service) throws SchedulerException, ParseException {
        synchronized (CONFIG_SERVICE_LOCK) {
            logger.debug("Registering new ScheduleConfigService");
            configMap.put(service.getJobName(), service);
            if (!started) {
                logger.debug("The Scheduler Service has not been started yet, storing new Schedule {}", service.getJobName());
            } else {
                try {
                    logger.debug("Adding schedule {}", service.getJobName());
                    addSchedule(service.getScheduleConfig(), service.getJobName(), true);
                } catch (ObjectAlreadyExistsException e) {
                    logger.debug("Job {} already scheduled", service.getJobName());
                }
            }

        }
    }

    /**
     * Unregisters a {@link ScheduleConfigService} and deletes the schedule if the scheduler has been started, and
     * if the service deactivation has not occurred as part of the shutdown of an idm node.
     * 
     * @param service the {@link ScheduleConfigService} to remove.
     * @param frameworkStopping indicates whether the osgi container is stopping
     */
    public void unregisterConfigService(ScheduleConfigService service, boolean frameworkStopping) {
        synchronized (CONFIG_SERVICE_LOCK) {
            logger.debug("Unregistering ScheduleConfigService");
            configMap.remove(service.getJobName());
            /*
            The deleteSchedule call below will remove the schedule from any configured persistent store only if
            the config deactivation is not taking place due to IDM shutdown. This check is present to insure that
            the shutdown of a node cluster does not remove a scheduled task, which would prevent that schedule from
            firing again on any remaining node clusters.
             */
            if (started && !frameworkStopping) {
                try {
                    logger.debug("Deleting schedule {}", service.getJobName());
                    deleteSchedule(service.getJobName());
                } catch (SchedulerException e) {
                    logger.warn("Error deleting schedule {}: {}", service.getJobName(), e.getMessage());
                }
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
            throws SchedulerException, ParseException {
        try {
            // Lock access to the scheduler so that a schedule is not added during a config update
            synchronized (LOCK) {
                // Determine the schedule class based on whether the job has concurrent execution enabled/disabled
                final Class<?> scheduleClass = scheduleConfig.getConcurrentExecution()
                        ? SchedulerServiceJob.class
                        : StatefulSchedulerServiceJob.class;

                // Attempt to add the schedule
                if (scheduleConfig.getCronSchedule() != null
                        && scheduleConfig.getCronSchedule().length() > 0) {
                    JobDetail job = new JobDetail(jobName, GROUP_NAME, scheduleClass);
                    job.setVolatility(scheduleConfig.isPersisted());
                    job.setJobDataMap(createJobDataMap(scheduleConfig));
                    Trigger trigger = createTrigger(scheduleConfig, jobName);
                    final Scheduler scheduler = scheduleConfig.isPersisted() ? persistentScheduler : inMemoryScheduler;

                    if (update) {
                        // Update the job by first deleting it, then scheduling the new version
                        deleteSchedule(jobName);
                    }

                    // check if it is enabled
                    if (scheduleConfig.isEnabled()) {
                        // Set to non-durable so that jobs won't persist after last firing
                        job.setDurability(false);
                        // Schedule the Job (with trigger)
                        scheduler.scheduleJob(job, trigger);
                        logger.info("Job {} scheduled with schedule {}, timezone {}, start time {}, end time {}.",
                                jobName, scheduleConfig.getCronSchedule(), scheduleConfig.getTimeZone(),
                                scheduleConfig.getStartTime(), scheduleConfig.getEndTime());
                    } else {
                        // Set the job to durable so that it can exist without a trigger (since the job is "disabled")
                        job.setDurability(true);
                        // Add the job (no trigger)
                        scheduler.addJob(job, false);
                        logger.info("Job {} added with schedule {}, timezone {}, start time {}, end time {}.",
                                jobName, scheduleConfig.getCronSchedule(), scheduleConfig.getTimeZone(),
                                scheduleConfig.getStartTime(), scheduleConfig.getEndTime());
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
     * Deletes a schedule from the scheduler
     * 
     * @param jobName the job name associated with this schedule.
     * @throws SchedulerException 
     */
    public void deleteSchedule(String jobName) throws SchedulerException {
        if (inMemoryScheduler.getJobDetail(jobName, GROUP_NAME) != null) {
            inMemoryScheduler.deleteJob(jobName, GROUP_NAME);
        }
        if (persistentScheduler.getJobDetail(jobName, GROUP_NAME) != null) {
            persistentScheduler.deleteJob(jobName, GROUP_NAME);
        }
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
     * Determines if a job already exists.
     *
     * @param jobName       The name of the job
     * @return              True if the job exists, false otherwise
     * @throws SchedulerException
     */
    private boolean jobExists(String jobName) throws SchedulerException {
        final boolean existsInMemory = inMemoryScheduler.getJobDetail(jobName, GROUP_NAME) != null;
        final boolean existsInPersistent = persistentScheduler.getJobDetail(jobName, GROUP_NAME) != null;
        return existsInMemory || existsInPersistent;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        try {
            String id = request.getNewResourceId() == null
                    ? UUID.randomUUID().toString()
                    : request.getNewResourceId();
            Map<String, Object> object = request.getContent().asMap();
            object.put("_id", id);
            
            if (jobExists(id)) {
                throw new PreconditionFailedException("Schedule already exists");
            }

            ScheduleConfig scheduleConfig = new ScheduleConfig(new JsonValue(object));

            // Check defaults
            if (scheduleConfig.isEnabled() == null) {
                scheduleConfig.setEnabled(true);
            }
            if (scheduleConfig.isPersisted() == null) {
                scheduleConfig.setPersisted(true);
            }

            addSchedule(scheduleConfig, id, false);
            
            return newResourceResponse(id, null, getSchedule(id)).asPromise();
        } catch (ParseException e) {
        	return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (ObjectAlreadyExistsException e) {
            return new PreconditionFailedException(e.getMessage(), e).asPromise();
        } catch (SchedulerException e) {
        	return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } catch (JsonException e) {
        	return new BadRequestException("Error creating schedule", e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
        	return new InternalServerErrorException(e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            if (request.getResourcePathObject().isEmpty()) {
                throw new BadRequestException("Empty resourceId");
            }
            // Get the schedule
            JsonValue schedule = getSchedule(request.getResourcePath());
            // Return the result
            return newResourceResponse(request.getResourcePath(), null, schedule).asPromise();
        } catch (SchedulerException e) {
        	return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } catch (JsonValueException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        try {
            if (request.getResourcePathObject().isEmpty()) {
                throw new BadRequestException("Empty resourceId");
            }
            Map<String, Object> object = request.getContent().asMap();
            object.put("_id", request.getResourcePath());

            // Default incoming config to "persisted" if not specified
            Object persistedValue = object.get(SchedulerService.SCHEDULE_PERSISTED);
            if (persistedValue == null) {
                object.put(SchedulerService.SCHEDULE_PERSISTED, true);
            }

            ScheduleConfig scheduleConfig = new ScheduleConfig(new JsonValue(object));

            if (!jobExists(request.getResourcePath())) {
                throw new NotFoundException();
            } else {
                // Update the Job
                addSchedule(scheduleConfig, request.getResourcePath(), true);
                return newResultPromise(newResourceResponse(request.getResourcePath(), null, 
                		getSchedule(request.getResourcePath())));
            }
        } catch (ParseException e) {
        	return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (ObjectAlreadyExistsException e) {
        	return new ConflictException(e.getMessage(), e).asPromise();
        } catch (SchedulerException e) {
        	return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } catch (JsonException e) {
        	return new BadRequestException("Error updating schedule", e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        try {
            if (request.getResourcePathObject().isEmpty()) {
                throw new BadRequestException("Empty resourceId");
            }
            if (!jobExists(request.getResourcePath())) {
                throw new NotFoundException();
            }
            // Get the schedule
            JsonValue schedule = getSchedule(request.getResourcePath());
            // Delete the schedule
            deleteSchedule(request.getResourcePath());
            // Return the deleted resource
            return newResourceResponse(request.getResourcePath(), null, schedule).asPromise();
        } catch (JsonException e) {
        	return new BadRequestException("Error deleting schedule", e).asPromise();
        } catch (SchedulerException e) {
        	return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException>  handlePatch(Context context, PatchRequest request) {
    	return notSupported(request).asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest request,
    		final QueryResourceHandler handler) {
        try {
            QueryFilter<JsonPointer> queryFilter = request.getQueryFilter();
            if (null == queryFilter) {
                queryFilter = QueryFilter.alwaysTrue();
            }

            // default the sortKeys to include the _id field for consistency.
            final List<SortKey> sortKeys = request.getSortKeys();
            sortKeys.add(SortKey.ascendingOrder(FIELD_CONTENT_ID));

            // if a queryId is set, verify that it is query-all-ids; no other is supported.
            final String queryId = request.getQueryId();
            if (null != queryId && !QUERY_ALL_IDS.equals(queryId)) {
                throw new BadRequestException("only query-id of '" + QUERY_ALL_IDS + "' is supported.");
            }

            // Get all the jobs and then filter them.
            final List<String> allJobs = new ArrayList<>();
            allJobs.addAll(Arrays.asList(getJobNames(persistentScheduler)));
            allJobs.addAll(Arrays.asList(getJobNames(inMemoryScheduler)));
            final List<JsonValue> results = new ArrayList<>();
            for (final String jobName : allJobs) {
                if (queryId != null) {  // query-all-ids
                    results.add(json(object(field(FIELD_CONTENT_ID, jobName))));
                } else {
                    final JsonValue schedule = getSchedule(jobName);
                    if (queryFilter.accept(JSONVALUE_FILTER_VISITOR, schedule)) {
                        results.add(schedule);
                    }
                }
            }
            final int totalResultsFound = results.size();

            // we have to sort all the records so that pagination works consistently
            Collections.sort(results, JsonUtil.getComparator(sortKeys));

            // default the pageSize to all the records if it isn't set.
            int pageSize = request.getPageSize();
            if (pageSize <= 0) {
                pageSize = totalResultsFound;
            }

            // default the page results Cookie to 0 if not passed in or isn't a integer.
            String pagedResultsCookie = request.getPagedResultsCookie();
            if (null == pagedResultsCookie || pagedResultsCookie.isEmpty() || !pagedResultsCookie.matches("\\d+")) {
                pagedResultsCookie = "0";
            }

            // calculate what the last index will be after this page is returned, -1 if last page.
            final int fromIndex = request.getPagedResultsOffset() + Integer.valueOf(pagedResultsCookie);

            // extract the desired results.
            final List<JsonValue> pageOfResults =
                    results.subList(fromIndex, min(fromIndex + pageSize, totalResultsFound));

            final String newPageCookie = fromIndex + pageSize >= totalResultsFound
                    ? null
                    : Integer.toString(fromIndex + pageSize);

            for (final JsonValue result : pageOfResults) {
                handler.handleResource(
                        newResourceResponse(result.get(FIELD_CONTENT_ID).asString(), null, json(result)));
            }
            return newQueryResponse(newPageCookie, CountPolicy.EXACT, totalResultsFound).asPromise();
        } catch (JsonException e) {
        	return new BadRequestException("Error performing query", e).asPromise();
        } catch (SchedulerException e) {
        	return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }

    /**
     * Gets the job names of the scheduler, returning an empty array if no jobs are found.
     *
     * @param scheduler to get the jobNames from.
     * @return array of job names, or empty array.
     * @throws SchedulerException when there is trouble calling {@link Scheduler#getJobNames(String)}
     */
    private String[] getJobNames(Scheduler scheduler) throws SchedulerException {
        String[] jobNames = scheduler.getJobNames(GROUP_NAME);
        return null == jobNames
                ? new String[0]
                : jobNames;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        try {
            Map<String, String> params = request.getAdditionalParameters();
            String action = request.getAction();

            switch (request.getActionAsEnum(SchedulerAction.class)) {
            case create:
                String id = UUID.randomUUID().toString();
                params.put("_id", id);
                if (jobExists(id)) {
                    throw new BadRequestException("Schedule already exists");
                }
                CreateRequest createRequest = Requests.newCreateRequest(id, new JsonValue(params));
                ResourceResponse response = handleCreate(context, createRequest)
                        .getOrThrow(IdentityServer.getPromiseTimeout(), TimeUnit.MILLISECONDS);
                return newActionResponse(response.getContent()).asPromise();
            case listCurrentlyExecutingJobs:
                JsonValue currentlyExecutingJobs = json(array());
                List<?> jobs = persistentScheduler.getCurrentlyExecutingJobs();
                for (Object job : jobs) {
                    JsonValue config = parseStringified((String)((JobExecutionContext)job).getJobDetail().getJobDataMap().get(CONFIG));
                    currentlyExecutingJobs.add(new ScheduleConfig(config).getConfig().getObject());
                }
                jobs = inMemoryScheduler.getCurrentlyExecutingJobs();
                for (Object job : jobs) {
                    JsonValue config = parseStringified((String)((JobExecutionContext)job).getJobDetail().getJobDataMap().get(CONFIG));
                    currentlyExecutingJobs.add(new ScheduleConfig(config).getConfig().getObject());
                }
                return newActionResponse(currentlyExecutingJobs).asPromise();
            case pauseJobs:
                persistentScheduler.pauseAll();
                inMemoryScheduler.pauseAll();
                return newActionResponse(json(object(field("success",true)))).asPromise();
            case resumeJobs:
                persistentScheduler.resumeAll();
                inMemoryScheduler.resumeAll();
                return newActionResponse(json(object(field("success",true)))).asPromise();
            default:
                throw new BadRequestException("Unknown action: " + action);
            }
        } catch (JsonException e) {
        	return new BadRequestException("Error performing action " + request.getAction(), e).asPromise();
        } catch (SchedulerException e) {
        	return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e).asPromise();
        }
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
        executePersistentSchedules = schedulerConfig.executePersistentSchedulesEnabled();
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
                // Must use DirectSchedulerFactory instance so that it does not conflict with
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
    
    /**
     * Returns a JsonValue representation of a schedule with the supplied name from the supplied scheduler.
     * 
     * @param scheduleName the name of the scheduler
     * @return the JsonValue representation of the schedule
     * @throws SchedulerException
     * @throws ResourceException
     */
    private JsonValue getSchedule(String scheduleName) throws SchedulerException, ResourceException {
        final Scheduler scheduler;
        if (inMemoryScheduler.getJobDetail(scheduleName, GROUP_NAME) != null) {
            scheduler = inMemoryScheduler;
        } else if (persistentScheduler.getJobDetail(scheduleName, GROUP_NAME) != null) {
            scheduler = persistentScheduler;
        } else {
            throw new NotFoundException("Schedule does not exist");
        }
        JobDetail job = scheduler.getJobDetail(scheduleName, GROUP_NAME);
        JobDataMap dataMap = job.getJobDataMap();
        JsonValue resultMap = new ScheduleConfig(parseStringified((String) dataMap.get(CONFIG))).getConfig();
        resultMap.put(FIELD_CONTENT_ID, scheduleName);
        return resultMap;
    }

    private JsonValue parseStringified(String stringified) {
        try {
            return json(mapper.readValue(stringified, Map.class));
        } catch (IOException ex) {
            throw new JsonException("String passed into parsing is not valid JSON", ex);
        }
    }
}
