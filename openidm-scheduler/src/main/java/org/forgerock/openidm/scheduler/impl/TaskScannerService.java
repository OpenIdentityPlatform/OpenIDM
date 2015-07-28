/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012-2015 ForgeRock AS. All Rights Reserved
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

import javax.script.ScriptException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jackson.JsonProcessingException;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "org.forgerock.openidm.taskscanner", policy = ConfigurationPolicy.IGNORE, immediate = true)
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM TaskScanner Service"),
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/taskscanner*")
})
@Service
public class TaskScannerService implements RequestHandler, ScheduledService {
    private final static Logger logger = LoggerFactory.getLogger(TaskScannerService.class);

    private final static String INVOKE_CONTEXT = "invokeContext";

    private int maxCompletedRuns;

    /**
     * Map from TaskScanID ID to the run itself
     * In historical start order, oldest first.
     */
    Map<String, TaskScannerContext> taskScanRuns =
            Collections.synchronizedMap(new LinkedHashMap<String, TaskScannerContext>());

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    @Reference(policy = ReferencePolicy.DYNAMIC)
    private ScriptRegistry scriptRegistry;

    @Activate
    public void activate(ComponentContext context) {
        String maxCompletedStr =
                IdentityServer.getInstance().getProperty("openidm.taskscanner.maxcompletedruns", "100");
        maxCompletedRuns = Integer.parseInt(maxCompletedStr);
    }

    /**
     * Invoked by the Task Scanner whenever the task scanner is triggered by the scheduler
     */
    @Override
    public void execute(ServerContext context, Map<String, Object> contextMap) throws ExecutionException {
        String invokerName = (String) contextMap.get(INVOKER_NAME);
        String scriptName = (String) contextMap.get(CONFIG_NAME);
        JsonValue params = new JsonValue(contextMap).get(CONFIGURED_INVOKE_CONTEXT);
        startTaskScanJob(context, invokerName, scriptName, params);
    }

    @Override
    public void auditScheduledService(final ServerContext context, final AuditEvent auditEvent)
            throws ExecutionException {
        try {
            connectionFactory.getConnection().create(
                    context, Requests.newCreateRequest("audit/access", auditEvent.getValue()));
        } catch (ResourceException e) {
            logger.error("Unable to audit scheduled service {}", auditEvent.toString());
            throw new ExecutionException("Unable to audit scheduled service", e);
        }
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            String id = request.getResourceName();
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            if (request.getResourceNameObject().isEmpty()) {
                List<Map<String, Object>> taskList = new ArrayList<Map<String, Object>>();
                for (TaskScannerContext entry : taskScanRuns.values()) {
                    Map<String, Object> taskData = buildTaskData(entry);
                    taskList.add(taskData);
                }
                result.put("tasks", taskList);
            } else {
                TaskScannerContext foundRun = taskScanRuns.get(request.getResourceName());
                if (foundRun == null) {
                    throw new NotFoundException("Task with id '" + request.getResourceName() + "' not found." );
                }
                result = buildTaskData(foundRun);
            }
            handler.handleResult(new Resource(id, null, new JsonValue(result)));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
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
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            Map<String, String> params = request.getAdditionalParameters();
            Map<String, Object> result = new LinkedHashMap<String, Object>();

            String action = request.getAction();
            if (request.getResourceNameObject().isEmpty()) {
                try {
                    if ("execute".equalsIgnoreCase(action)) {
                        try {
                            result.put("_id", onExecute(context, request.getResourceName(), params));
                        } catch (JsonProcessingException e) {
                            throw new InternalServerErrorException(e);
                        } catch (IOException e) {
                            throw new InternalServerErrorException(e);
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
                TaskScannerContext foundRun = taskScanRuns.get(request.getResourceName());
                if (foundRun == null) {
                    throw new NotFoundException("Task with id '" + request.getResourceName() + "' not found." );
                }

                if ("cancel".equalsIgnoreCase(action)) {
                    if (foundRun.isCompleted()) {
                        result.put("status", "FAILURE");
                    } else {
                        foundRun.cancel();
                        result.put("status", "SUCCESS");
                    }
                    result.put("_id", foundRun.getTaskScanID());
                    result.put("action", action);
                } else {
                    throw new BadRequestException("Action '" + action + "' on Task '" + request.getResourceName() + "' not supported " + params);
                }
            }

            handler.handleResult(new JsonValue(result));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
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
    private String onExecute(ServerContext context, String id, Map<String, String> params)
            throws ExecutionException, JsonProcessingException, IOException, ScriptException {
        String name = params.get("name");
        JsonValue config;
        try {
            config = connectionFactory.getConnection().read(context, Requests.newReadRequest("config/" + name)).getContent();
        } catch (ResourceException e) {
            throw new ExecutionException("Error obtaining named config: '" + name + "'", e);
        }
        JsonValue invokeContext = config.get(INVOKE_CONTEXT);

        return startTaskScanJob(context, "REST", name, invokeContext);
    }

    private String startTaskScanJob(ServerContext context, String invokerName, String scriptName, JsonValue params) throws ExecutionException {
        TaskScannerContext taskScannerContext = null;

        try {
            JsonValue scriptConfig = params.get("task").expect(Map.class).get("script").expect(Map.class);
            ScriptEntry script = scriptRegistry.takeScript(scriptConfig);

            taskScannerContext = new TaskScannerContext(invokerName, scriptName, params, context, script);
        } catch (ScriptException e) {
            throw new ExecutionException(e);
        }

        addTaskScanRun(taskScannerContext);
        TaskScannerJob taskScanJob = new TaskScannerJob(connectionFactory, taskScannerContext);
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

    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }
}
