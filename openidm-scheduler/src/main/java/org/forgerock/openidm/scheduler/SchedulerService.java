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

import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.openidm.scheduler.JobRequestHandler.JOB_RESOURCE_PATH;
import static org.forgerock.openidm.scheduler.RepoProxyRequestHandler.*;
import static org.forgerock.openidm.scheduler.TriggerRequestHandler.TRIGGER_RESOURCE_PATH;
import static org.quartz.CronExpression.isValidExpression;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.cluster.ClusterManagementService;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.quartz.Job;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduler service using Quartz. This service exposes subroutes for the Quartz resources ({@link Trigger Triggers},
 * {@link Job Jobs})
 */
@Component(name = "org.forgerock.openidm.scheduler", immediate=true, policy=ConfigurationPolicy.REQUIRE)
@Service(value = {SchedulerService.class, RequestHandler.class})
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Scheduler Service using Quartz"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/scheduler*")
})
public class SchedulerService implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    // Keys in the OSGi configuration
    static final String SCHEDULE_ENABLED = "enabled";
    static final String SCHEDULE_TYPE = "type";
    static final String SCHEDULE_START_TIME = "startTime";
    static final String SCHEDULE_END_TIME = "endTime";
    static final String SCHEDULE_CRON_SCHEDULE = "schedule";
    static final String SCHEDULE_TIME_ZONE = "timeZone";
    static final String SCHEDULE_INVOKE_SERVICE = "invokeService";
    static final String SCHEDULE_INVOKE_CONTEXT = "invokeContext";
    static final String SCHEDULE_INVOKE_LOG_LEVEL = "invokeLogLevel";
    static final String SCHEDULE_PERSISTED = "persisted";
    static final String SCHEDULE_MISFIRE_POLICY = "misfirePolicy";
    static final String SCHEDULE_CONCURRENT_EXECUTION = "concurrentExecution";

    // Valid configuration values
    static final String SCHEDULE_TYPE_CRON = "cron";
    static final String SCHEDULE_TYPE_SIMPLE = "simple";

    // Misfire Policies
    static final String MISFIRE_POLICY_DO_NOTHING = "doNothing";
    static final String MISFIRE_POLICY_FIRE_AND_PROCEED = "fireAndProceed";

    public static final String GROUP_NAME = "scheduler-service-group";

    static final String CONFIG = "schedule.config";

    private static final String SCHEDULER_REPO_RESOURCE_PATH = "/repo/scheduler/";

    private static final String WAITING_TRIGGERS_REPO_RESOURCE_PATH = SCHEDULER_REPO_RESOURCE_PATH + "waitingTriggers";

    private static final String ACQUIRED_TRIGGERS_REPO_RESOURCE_PATH = SCHEDULER_REPO_RESOURCE_PATH + "acquiredTriggers";

    /**
     * Supported actions on the scheduler service.
     */
    enum SchedulerAction {
        validateQuartzCronExpression
    }

    private static Scheduler inMemoryScheduler;
    private static Scheduler persistentScheduler = null;
    private static SchedulerConfig schedulerConfig = null;

    private Map<String, ScheduleConfigService> configMap = new HashMap<>();
    private static final Object CONFIG_SERVICE_LOCK = new Object();

    private boolean executePersistentSchedules = false;
    private boolean started = false;

    private final Router router = new Router();

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
    @Reference(name = "ref_SchedulerService_RepoService", target = "(" + ServerConstants.ROUTER_PREFIX + "=/repo/*)")
    protected RouteService repoService;

    @Reference
    protected ConnectionFactory connectionFactory;

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

            router.removeAllRoutes();
            setupRouter();

            logger.info("There are {} jobs waiting to be scheduled", configMap.size());
            Set<String> keys = configMap.keySet();
            for (String key : keys) {
                ScheduleConfigService service = configMap.get(key);
                try {
                    createJob(service.getScheduleConfig(), service.getJobName());
                } catch (ObjectAlreadyExistsException e) {
                    logger.debug("Job {} already scheduled", service.getJobName());
                }
            }
            logger.info("Scheduling waiting schedules");
        }
    }

    private void setupRouter() {
        router.addRoute(STARTS_WITH, Router.uriTemplate(JOB_RESOURCE_PATH),
                new JobRequestHandler(persistentScheduler, inMemoryScheduler, clusterManager.getInstanceId()));
        router.addRoute(STARTS_WITH, Router.uriTemplate(TRIGGER_RESOURCE_PATH),
                new TriggerRequestHandler(connectionFactory));
        router.addRoute(STARTS_WITH, Router.uriTemplate(WAITING_TRIGGERS_RESOURCE_PATH),
                new RepoProxyRequestHandler(WAITING_TRIGGERS_REPO_RESOURCE_PATH, connectionFactory));
        router.addRoute(STARTS_WITH, Router.uriTemplate(ACQUIRED_TRIGGERS_RESOURCE_PATH),
                new RepoProxyRequestHandler(ACQUIRED_TRIGGERS_REPO_RESOURCE_PATH, connectionFactory));
    }

    @Deactivate
    @SuppressWarnings({"unused"})
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Scheduler Service {}", compContext);
        router.removeAllRoutes();
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
     * Registers a {@link ScheduleConfigService} and adds the scheduler if the scheduler has been started.
     * 
     * @param service the {@link ScheduleConfigService} to register.
     * @throws SchedulerException if unable to get job name
     * @throws ParseException if unable to parse schedule config
     */
    void registerConfigService(ScheduleConfigService service) throws SchedulerException, ParseException {
        synchronized (CONFIG_SERVICE_LOCK) {
            logger.debug("Registering new ScheduleConfigService");
            configMap.put(service.getJobName(), service);
            if (!started) {
                logger.debug("The Scheduler Service has not been started yet, storing new Schedule {}",
                        service.getJobName());
            } else {
                logger.debug("Adding schedule {}", service.getJobName());
                updateJob(service.getScheduleConfig(), service.getJobName());
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
    void unregisterConfigService(ScheduleConfigService service, boolean frameworkStopping) {
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
                    deleteJob(service.getJobName());
                } catch (SchedulerException e) {
                    logger.warn("Error deleting schedule {}: {}", service.getJobName(), e.getMessage());
                }
            }
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        return router.handleCreate(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        return router.handleRead(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        return router.handleUpdate(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        return router.handleDelete(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException>  handlePatch(Context context, PatchRequest request) {
        return router.handlePatch(context, request);
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest request,
    		final QueryResourceHandler handler) {
        return router.handleQuery(context, request, handler);
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        try {
            final SchedulerAction action = request.getActionAsEnum(SchedulerAction.class);
            switch (action) {
                case validateQuartzCronExpression:
                    return newActionResponse(
                            json(object(
                                    field("valid", isValidExpression(request.getContent().get("cronExpression").asString()))
                            ))).asPromise();
                default:
                    // this should never happen
                    return router.handleAction(context, request);
            }
        } catch (final IllegalArgumentException e) {
            try {
                return router.handleAction(context, request);
            } catch (final IllegalArgumentException e1) {
                // action is unknown to sub routes as well
                return new BadRequestException("Unknown action given: " + request.getAction()).asPromise();
            }
        }
    }

    private void initPersistentScheduler(ComponentContext compContext) throws SchedulerException {
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
        persistentScheduler = new PersistedScheduler(sf.getScheduler(), connectionFactory);
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
                inMemoryScheduler = new MemoryScheduler(DirectSchedulerFactory.getInstance().getScheduler());
                // Set back to the original thread context classloader
                Thread.currentThread().setContextClassLoader(original);
            }
        } catch (SchedulerException ex) {
            logger.warn("Failure in initializing the scheduler facility " + ex.getMessage(), ex);
            throw ex;
        }
        logger.info("Scheduler facility started");
    }

    private void createJob(final ScheduleConfig config, final String jobName) throws SchedulerException {
        try {
            final CreateRequest request = Requests.newCreateRequest(JOB_RESOURCE_PATH, jobName, config.getConfig());
            router.handleCreate(ContextUtil.createInternalContext(), request).getOrThrow();
        } catch (final InterruptedException|ResourceException e) {
            final String error = String.format("Unable to create scheduled job: %s", jobName);
            logger.warn(error, e);
            throw new SchedulerException(error, e);
        }
    }

    private void updateJob(final ScheduleConfig config, final String jobName) throws SchedulerException {
        try {
            final UpdateRequest request = Requests.newUpdateRequest(JOB_RESOURCE_PATH, jobName, config.getConfig());
            router.handleUpdate(ContextUtil.createInternalContext(), request).getOrThrow();
        } catch (final NotFoundException e) {
            createJob(config, jobName);
        } catch (final InterruptedException|ResourceException e) {
            final String error = String.format("Unable to update scheduled job: %s", jobName);
            logger.warn(error, e);
            throw new SchedulerException(error, e);
        }
    }

    private void deleteJob(final String jobName) throws SchedulerException {
        try {
            final DeleteRequest request = Requests.newDeleteRequest(JOB_RESOURCE_PATH, jobName);
            router.handleDelete(ContextUtil.createInternalContext(), request).getOrThrow();
        } catch (final InterruptedException|ResourceException e) {
            final String error = String.format("Unable to delete scheduled job: %s", jobName);
            logger.warn(error, e);
            throw new SchedulerException(error, e);
        }
    }
}
