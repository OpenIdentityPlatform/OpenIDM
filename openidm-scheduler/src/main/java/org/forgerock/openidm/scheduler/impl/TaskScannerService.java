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
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jackson.JsonProcessingException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.script.ScriptRegistry;
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
public class TaskScannerService implements SingletonResourceProvider{
    private final static Logger logger = LoggerFactory.getLogger(TaskScannerService.class);

    private final static String INVOKE_CONTEXT = "invokeContext";

    private int maxCompletedRuns;

    /**
     * Map from TaskScanID ID to the run itself
     * In historical start order, oldest first.
     */
    Map<String, TaskScannerContext> taskScanRuns =
            Collections.synchronizedMap(new LinkedHashMap<String, TaskScannerContext>());

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private ScriptRegistry scopeFactory;

    @Activate
    public void activate(ComponentContext context) {
        String maxCompletedStr =
                IdentityServer.getInstance().getProperty("openidm.taskscanner.maxcompletedruns", "100");
        maxCompletedRuns = Integer.parseInt(maxCompletedStr);
    }

    //TODO Deprecated: Use the action method
//    /**
//     * Invoked by the Task Scanner whenever the task scanner is triggered by the scheduler
//     */
//    @Override
//    public void execute(Map<String, Object> context) throws ExecutionException {
//        String invokerName = (String) context.get(INVOKER_NAME);
//        String scriptName = (String) context.get(CONFIG_NAME);
//        JsonValue params = new JsonValue(context).get(CONFIGURED_INVOKE_CONTEXT);
//
//        startTaskScanJob(invokerName, scriptName, params);
//    }


    @Override
    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>());
        if (request.getResourceName().equals("/")) {
            List<Map<String, Object>> taskList = new ArrayList<Map<String, Object>>();
            for (TaskScannerContext entry : taskScanRuns.values()) {
                JsonValue taskData = buildTaskData(entry);
                taskList.add(taskData.asMap());
            }
            result.put("tasks", taskList);
        } else {
            TaskScannerContext foundRun = taskScanRuns.get(request.getResourceName());
            if (foundRun == null) {
                handler.handleError(new NotFoundException("Task with id '" + request.getResourceName() + "' not found." ));
            }
            result = buildTaskData(foundRun);
        }
        handler.handleResult(new Resource("TODO id", "", result));
    }

    // TODO maybe move this into TaskScannerContext?
    private JsonValue buildTaskData(TaskScannerContext entry) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>());
        result.put("_id", entry.getTaskScanID());
        result.put("progress", entry.getProgress());
        result.put("started", entry.getStatistics().getJobStartTime());
        result.put("ended", entry.getStatistics().getJobEndTime());
        return result;
    }

    @Override
    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        JsonValue result = new JsonValue( new LinkedHashMap<String, Object>());
        
        String action = request.getAction();
        if (request.getResourceName() == "/") {
            try {
                if ("execute".equalsIgnoreCase(action)) {
                    try {
                        result.put("_id", onExecute(context, request));
                    } catch (JsonProcessingException e) {
                        handler.handleError(new InternalServerErrorException(e));
                    } catch (IOException e) {
                        handler.handleError(new InternalServerErrorException(e));
                    }
                } else {
                    handler.handleError( new BadRequestException("Unknown action: " + action));
                }
            } catch (ExecutionException e) {
                logger.warn(e.getMessage());
                handler.handleError( new BadRequestException(e.getMessage(), e));
            }
        } else {
            // operation on individual resource
            TaskScannerContext foundRun = taskScanRuns.get(request.getResourceName());
            if (foundRun == null) {
                handler.handleError( new NotFoundException("Task with id '" + request.getResourceName() + "' not found." ));
            }

            if ("cancel".equalsIgnoreCase(action)) {
                foundRun.cancel();
                result.put("_id", foundRun.getTaskScanID());
                result.put("action", action);
                result.put("status", "SUCCESS");
            } else {
                handler.handleError( new BadRequestException("Action '" + action + "' on Task '" + request.getResourceName() + "' not supported " + request.getAdditionalActionParameters()));
            }
        }

        handler.handleResult(result);
    }

    @Override
    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Operation is not implemented"));
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Operation is not implemented"));
    }

    /**
     * Performs the "execute" action, executing a supplied configuration
     *
     * Expects a field "name" containing the name of some config object that can be found via
     * a read on "config/" + name <br><br>
     *
     * <b><i>e.g.</b></i> "taskscan/sunset" => "config/taskscan/sunset" => "[openidm-directory]/conf/taskscan-sunset.json"<br>
     *
     * @param context //TODO FIXME
     * @param request //TODO FIXME
     * @return the set of parameters supplied
     * @throws ExecutionException
     * @throws JsonProcessingException
     * @throws IOException
     */
    private String onExecute(ServerContext context, ActionRequest request)
            throws ExecutionException, JsonProcessingException, IOException {
        String name = (String) request.getAdditionalActionParameters().get("name");
        JsonValue config;
        try {
            config = context.getConnection().read(context, Requests.newReadRequest("config",name)).getContent();
        } catch (ResourceException e) {
            //TODO Do we need this catch?
            throw new ExecutionException("Error obtaining named config: '" + name + "'", e);
        }
        //TODO Restore the Context
        JsonValue invokeContext = config.get(INVOKE_CONTEXT);

        return startTaskScanJob("REST", name, invokeContext, context);
    }

    private String startTaskScanJob(String invokerName, String scriptName, JsonValue params, ServerContext serverContext) throws ExecutionException {
        TaskScannerContext context = new TaskScannerContext(invokerName, scriptName, params, serverContext);
        addTaskScanRun(context);
        TaskScannerJob taskScanJob = new TaskScannerJob(context, serverContext, scopeFactory);
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
