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
 * Portions copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.openidm.scheduler.impl;

import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
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
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.util.promise.Promise;
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
    
    protected void bindConnectionFactory(ConnectionFactory connectionFactory) {
    	this.connectionFactory = connectionFactory;
    }

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
    public void execute(Context context, Map<String, Object> contextMap) throws ExecutionException {
        String invokerName = (String) contextMap.get(INVOKER_NAME);
        String scriptName = (String) contextMap.get(CONFIG_NAME);
        JsonValue params = new JsonValue(contextMap).get(CONFIGURED_INVOKE_CONTEXT);
        startTaskScanJob(context, invokerName, scriptName, params);
    }

    @Override
    public void auditScheduledService(final Context context, final AuditEvent auditEvent)
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
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            String id = request.getResourcePath();
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            if (request.getResourcePathObject().isEmpty()) {
                List<Map<String, Object>> taskList = new ArrayList<Map<String, Object>>();
                for (TaskScannerContext entry : taskScanRuns.values()) {
                    Map<String, Object> taskData = buildTaskData(entry);
                    taskList.add(taskData);
                }
                result.put("tasks", taskList);
            } else {
                TaskScannerContext foundRun = taskScanRuns.get(request.getResourcePath());
                if (foundRun == null) {
                    throw new NotFoundException("Task with id '" + request.getResourcePath() + "' not found." );
                }
                result = buildTaskData(foundRun);
            }
            return newResultPromise(newResourceResponse(id, null, new JsonValue(result)));
        } catch (Exception e) {
        	return newExceptionPromise(ResourceUtil.adapt(e));
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
    public Promise<ActionResponse, ResourceException>  handleAction(Context context, ActionRequest request) {
        try {
            Map<String, String> params = request.getAdditionalParameters();
            Map<String, Object> result = new LinkedHashMap<String, Object>();

            String action = request.getAction();
            if (request.getResourcePathObject().isEmpty()) {
                try {
                    if ("execute".equalsIgnoreCase(action)) {
                        try {
                            result.put("_id", onExecute(context, request.getResourcePath(), params));
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
                TaskScannerContext foundRun = taskScanRuns.get(request.getResourcePath());
                if (foundRun == null) {
                    throw new NotFoundException("Task with id '" + request.getResourcePath() + "' not found." );
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
                    throw new BadRequestException("Action '" + action + "' on Task '" + request.getResourcePath() + "' not supported " + params);
                }
            }

            return newResultPromise(newActionResponse(new JsonValue(result)));
        } catch (Exception e) {
        	return newExceptionPromise(ResourceUtil.adapt(e));
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
    private String onExecute(Context context, String id, Map<String, String> params)
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

    private String startTaskScanJob(Context context, String invokerName, String scriptName, JsonValue params) throws ExecutionException {
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
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        return newExceptionPromise(ResourceUtil.notSupported(request));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        return newExceptionPromise(ResourceUtil.notSupported(request));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return newExceptionPromise(ResourceUtil.notSupported(request));
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request, QueryResourceHandler handler) {
        return newExceptionPromise(ResourceUtil.notSupported(request));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        return newExceptionPromise(ResourceUtil.notSupported(request));
    }
}
