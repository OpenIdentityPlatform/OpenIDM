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
*/
package org.forgerock.openidm.scheduler.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jackson.JsonProcessingException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetJsonResource;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.scope.ScopeFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "org.forgerock.openidm.taskscanner", policy = ConfigurationPolicy.IGNORE, immediate = true)
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM TaskScanner Service"),
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "taskscanner")
})
@Service
public class TaskScannerService extends ObjectSetJsonResource implements ScheduledService {
    private final static Logger logger = LoggerFactory.getLogger(TaskScannerService.class);

    private final static String INVOKE_CONTEXT = "invokeContext";

    private int maxCompletedRuns;

    /**
     * Map from TaskScanID ID to the run itself
     * In historical start order, oldest first.
     */
    Map<String, TaskScannerContext> taskScanRuns =
            Collections.synchronizedMap(new LinkedHashMap<String, TaskScannerContext>());

    @Reference
    private ScopeFactory scopeFactory;

    @Reference(
        name = "ref_ManagedObjectService_JsonResourceRouterService",
        referenceInterface = JsonResource.class,
        bind = "bindRouter",
        unbind = "unbindRouter",
        cardinality = ReferenceCardinality.MANDATORY_UNARY,
        policy = ReferencePolicy.STATIC,
        target = "(service.pid=org.forgerock.openidm.router)"
    )
    private JsonResource router;

    private JsonResourceAccessor accessor() {
        return new JsonResourceAccessor(router, ObjectSetContext.get());
    }

    @Activate
    public void activate(ComponentContext context) {
        String maxCompletedStr =
                IdentityServer.getInstance().getProperty("openidm.taskscanner.maxcompletedruns", "100");
        maxCompletedRuns = Integer.parseInt(maxCompletedStr);
    }

    protected void bindRouter(JsonResource router) {
        this.router = router;
    }

    protected void unbindRouter(JsonResource router) {
        this.router = null;
    }

    /**
     * Invoked by the Task Scanner whenever the task scanner is triggered by the scheduler
     */
    @Override
    public void execute(Map<String, Object> context) throws ExecutionException {
        String invokerName = (String) context.get(INVOKER_NAME);
        String scriptName = (String) context.get(CONFIG_NAME);
        JsonValue params = new JsonValue(context).get(CONFIGURED_INVOKE_CONTEXT);

        startTaskScanJob(invokerName, scriptName, params);
    }

    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (id == null) {
            List<Map<String, Object>> taskList = new ArrayList<Map<String, Object>>();
            for (TaskScannerContext entry : taskScanRuns.values()) {
                Map<String, Object> taskData = buildTaskData(entry);
                taskList.add(taskData);
            }
            result.put("tasks", taskList);
        } else {
            TaskScannerContext foundRun = taskScanRuns.get(id);
            if (foundRun == null) {
                throw new NotFoundException("Task with id '" + id + "' not found." );
            }
            result = buildTaskData(foundRun);
        }
        return result;
    }

    // TODO maybe move this into TaskScannerContext?
    private Map<String, Object> buildTaskData(TaskScannerContext entry) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("_id", entry.getTaskScanID());
        result.put("progress", entry.getProgress());
        result.put("started", entry.getStatistics().getJobStartTime());
        result.put("ended", entry.getStatistics().getJobEndTime());
        return result;
    }

    @Override
    public Map<String, Object> action(String id, Map<String, Object> params)
            throws ObjectSetException {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        if (params.get("_action") == null) {
            throw new BadRequestException("Expecting _action parameter");
        }
        
        String action = (String) params.get("_action");
        if (id == null) {
            try {
                if ("execute".equalsIgnoreCase(action)) {
                    try {
                        result.put("_id", onExecute(id, params));
                    } catch (JsonProcessingException e) {
                        throw new ObjectSetException(ObjectSetException.INTERNAL_ERROR, e);
                    } catch (IOException e) {
                        throw new ObjectSetException(ObjectSetException.INTERNAL_ERROR, e);
                    }
                } else {
                    throw new BadRequestException("Unknown action: " + action);
                }
            } catch (ExecutionException e) {
                logger.warn(e.getMessage());
                throw new BadRequestException(e.getMessage(), e);
            }
        } else {
            // operation on individual resource
            TaskScannerContext foundRun = taskScanRuns.get(id);
            if (foundRun == null) {
                throw new NotFoundException("Task with id '" + id + "' not found." );
            }

            if ("cancel".equalsIgnoreCase(action)) {
                foundRun.cancel();
                result.put("_id", foundRun.getTaskScanID());
                result.put("action", action);
                result.put("status", "SUCCESS");
            } else {
                throw new BadRequestException("Action '" + action + "' on Task '" + id + "' not supported " + params);
            }
        }

        return result;
    }

    /**
     * Performs the "execute" action, executing a supplied configuration
     *
     * Expects a field "name" containing the name of some config object that can be found via
     * a read on "config/" + name <br><br>
     *
     * <b><i>e.g.</b></i> "taskscan/sunset" => "config/taskscan/sunset" => "[openidm-directory]/conf/taskscan-sunset.json"<br>
     *
     * @param id the id to perform the action on
     * @param params field contaning the parameters of execution
     * @return the set of parameters supplied
     * @throws ExecutionException
     * @throws JsonProcessingException
     * @throws IOException
     */
    private String onExecute(String id, Map<String, Object> params)
            throws ExecutionException, JsonProcessingException, IOException {
        String name = (String) params.get("name");
        JsonValue config;
        try {
            config = accessor().read("config/" + name);
        } catch (JsonResourceException e) {
            throw new ExecutionException("Error obtaining named config: '" + name + "'", e);
        }
        JsonValue invokeContext = config.get(INVOKE_CONTEXT);

        return startTaskScanJob("REST", name, invokeContext);
    }

    private String startTaskScanJob(String invokerName, String scriptName, JsonValue params) throws ExecutionException {
        TaskScannerContext context = new TaskScannerContext(invokerName, scriptName, params, ObjectSetContext.get());
        addTaskScanRun(context);
        TaskScannerJob taskScanJob = new TaskScannerJob(context, router, scopeFactory);
        return taskScanJob.startTask();
    }

    private void addTaskScanRun(TaskScannerContext context) {
        // Clean out run history if needed
        // Since it only checks for completed runs when a new run is started this
        // only provides for approximate adherence to maxCompleteRuns
        synchronized(taskScanRuns) {
            if (taskScanRuns.size() > maxCompletedRuns) {
                int completedCount = 0;
                // Since oldest runs are first in the list, inspect backwards
                ListIterator<String> iter = new ArrayList<String>(taskScanRuns.keySet())
                        .listIterator(taskScanRuns.size());
                while (iter.hasPrevious()) {
                    String key = iter.previous();
                    TaskScannerContext aRun = taskScanRuns.get(key);
                    if (aRun.isCompleted()) {
                        ++completedCount;
                        if (completedCount > maxCompletedRuns) {
                            taskScanRuns.remove(key);
                        }
                    }
                }
            }
            taskScanRuns.put(context.getTaskScanID(), context);
        }
    }
}
