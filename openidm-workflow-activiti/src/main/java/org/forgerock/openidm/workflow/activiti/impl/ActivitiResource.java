/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
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
package org.forgerock.openidm.workflow.activiti.impl;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Iterator;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.DelegationState;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.json.resource.SimpleJsonResource.Method;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.util.DateUtil;

/**
 * Implementation of the Activiti Engine Resource
 *
 * @author orsolyamebold
 */
public class ActivitiResource implements JsonResource {

    private ProcessEngine processEngine;
    private static final String PROCESSINSTANCE_PATTERN = "/?processinstance.*";
    private static final String PROCESSINSTANCE_ID_PATTERN = "/?processinstance/.+$";
    private static final String TASK_PATTERN = "/?task.*";
    private static final String TASK_ID_PATTERN = "/?task/.+$";
    private static final String ID = ServerConstants.OBJECT_PROPERTY_ID;
    private static final String REQUEST_PARAMS = "params";
    private static final String REQUEST_BODY = "value";
    private static final String QUERY_ALL_IDS = "query-all-ids";
    private static final String QUERY_FILTERED = "filtered-query";
    private static final String VARIABLE_QUERY_PREFIX = "_var-";
    private static final String ACTIVITI_PROCESSDEFINITIONID = "processDefinitionId";
    private static final String ACTIVITI_PROCESSDEFINITIONKEY = "processDefinitionKey";
    private static final String ACTIVITI_PROCESSINSTANCEBUSINESSKEY = "processInstanceBusinessKey";
    private static final String ACTIVITI_PROCESSINSTANCEID = "processInstanceId";
    private static final String ACTIVITI_KEY = "key";
    private static final String ACTIVITI_STARTTIME = "startTime";
    private static final String ACTIVITI_ENDTIME = "endTime";
    private static final String ACTIVITI_STATUS = "status";
    private static final String ACTIVITI_BUSINESSKEY = "businessKey";
    private static final String ACTIVITI_DELETEREASON = "deleteReason";
    private static final String ACTIVITI_DURATIONINMILLIS = "durationInMillis";
    private static final String ACTIVITI_TASKNAME = "taskName";
    private static final String ACTIVITI_ASSIGNEE = "assignee";
    private static final String ACTIVITI_DESCRIPTION = "description";
    private static final String ACTIVITI_NAME = "name";
    private static final String ACTIVITI_OWNER = "owner";
    private static final String ACTIVITI_CREATETIME = "createTime";
    private static final String ACTIVITI_DUEDATE = "dueDate";
    private static final String ACTIVITI_EXECUTIONID = "executionId";
    private static final String ACTIVITI_CANDIDATEGROUP = "taskCandidateGroup";
    private static final String ACTIVITI_CANDIDATEUSER = "taskCandidateUser";
    private static final String ACTIVITI_STARTUSERID = "startUserId";
    private static final String ACTIVITI_SUPERPROCESSINSTANCEID = "superProcessInstanceId";
    private static final String ACTIVITI_TASKID = "taskId";
    private static final String ACTIVITI_PRIORITY = "priority";
    private static final String ACTIVITI_TASKDEFINITIONKEY = "taskDefinitionKey";
    private static final String ACTIVITI_VARIABLES = "variables";
    private static final String ACTIVITI_DELEGATE = "delegate";
    private static final String ACTIVITI_VERSION = "version";
    private static final String ACTIVITI_CATEGORY = "category";

    public ActivitiResource(ProcessEngine engine) {
        this.processEngine = engine;
    }

    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        try {
            int path = getPath(request);
            Method method = request.get("method").required().asEnum(SimpleJsonResource.Method.class);
            switch (path) {
                case 1:     //workflow
                    return workflow(method);
                case 2:     //workflow/processinstance
                    return processInstance(method, request);
                case 3:     //workflow/processinstance/{ID}
                    return processInstanceId(method, request);
                case 4:     //workflow/task
                    return task(method, request);
                case 5:     //workflow/task/{ID}
                    return taskId(method, request);
                default:
                    throw new JsonResourceException(JsonResourceException.FORBIDDEN, "The path in the request is not valid");
            }
        } catch (JsonValueException jve) {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST, jve);
        }
    }

    /**
     * Parse the path element of the request
     *
     * @param request incoming request
     * @return integer value representing the path
     */
    private int getPath(JsonValue request) {
        String id = request.get("id").asString();
        if (id == null || "/".equals(id)) {
            return 1;
        } else if (id.matches(PROCESSINSTANCE_PATTERN)) {
            if (id.matches(PROCESSINSTANCE_ID_PATTERN)) {
                return 3;
            } else {
                return 2;
            }
        } else if (id.matches(TASK_PATTERN)) {
            if (id.matches(TASK_ID_PATTERN)) {
                return 5;
            } else {
                return 4;
            }
        }
        return 0;
    }

    /**
     * Handle the request sent to '/workflow'
     *
     * @param m method to execute
     * @return result
     * @throws JsonResourceException requested method not implemented
     */
    private JsonValue workflow(Method m) throws JsonResourceException {
        switch (m) {
            case read:
                return readWorkflow();
            case action:
            case create:
            case delete:
            case patch:
            case query:
            case update:
            default:
                throw new JsonResourceException(JsonResourceException.FORBIDDEN, m + " method not implemented on workflow");
        }
    }

    /**
     * Handle the request sent to '/workflow/processinstance'
     *
     * @param m method to execute
     * @param request incoming request
     * @return result
     * @throws JsonResourceException requested method not implemented or unknown
     * query-id parameter
     */
    private JsonValue processInstance(Method m, JsonValue request) throws JsonResourceException {
        switch (m) {
            case query:     //query based on query-id
                JsonValue result = new JsonValue(new HashMap<String, Object>());
                List resultList = new LinkedList();
                String queryId = ActivitiUtil.getQueryIdFromRequest(request);
                if (QUERY_ALL_IDS.equals(queryId)) {
                    HistoricProcessInstanceQuery query = processEngine.getHistoryService().createHistoricProcessInstanceQuery();
                    query = query.unfinished();
                    List<HistoricProcessInstance> list = query.list();
                    for (HistoricProcessInstance i : list) {
                        Map entry = new HashMap();
                        entry.put(ID, i.getId());
                        entry.put(ACTIVITI_PROCESSDEFINITIONID, i.getProcessDefinitionId());
                        resultList.add(entry);
                    }
                    result.add("result", resultList);
                    return result;
                } else if (QUERY_FILTERED.equals(queryId)) {
                    ProcessInstanceQuery query = processEngine.getRuntimeService().createProcessInstanceQuery();
                    setProcessInstanceParams(query, request);
                    List<ProcessInstance> list = query.list();
                    for (ProcessInstance i : list) {
                        Map entry = new HashMap();
                        entry.put(ID, i.getId());
                        entry.put(ACTIVITI_PROCESSDEFINITIONID, i.getProcessDefinitionId());
                        resultList.add(entry);
                    }
                    result.add("result", resultList);
                    return result;
                }
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, "Unknown query-id");
            case create:    //start new workflow
            case action:
                return startProcessInstance(request);
            case delete:
            case patch:
            case read:
            case update:
            default:
                throw new JsonResourceException(JsonResourceException.FORBIDDEN, m + " not implemented on processinstance");
        }
    }

    /**
     * Handle the request sent to '/workflow/processinstance/{ID}'
     *
     * @param m method to execute
     * @param request incoming request
     * @return result
     * @throws JsonResourceException requested method not implemented
     */
    private JsonValue processInstanceId(Method m, JsonValue request) throws JsonResourceException {
        String id = ActivitiUtil.getIdFromRequest(request);
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        switch (m) {
            case read:  //detailed information of a running process instance
                HistoricProcessInstance instance =
                        processEngine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(id).singleResult();
                if (instance == null) {
                    throw new JsonResourceException(JsonResourceException.NOT_FOUND);
                }
                return convertHistoricProcessInstance(result, instance);
            case delete:    //stop process instance
                if (processEngine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(id).singleResult() != null) {
                    processEngine.getRuntimeService().deleteProcessInstance(id, "Deleted by Openidm");
                    result.add("Process instance deleted", id);
                    return result;
                } else {
                    throw new JsonResourceException(JsonResourceException.NOT_FOUND);
                }
            case create:
            case action:
            case patch:
            case query:
            case update:
            default:
                throw new JsonResourceException(JsonResourceException.FORBIDDEN, m + " not implemented on processinstance/{ID}");
        }
    }

    /**
     * Handle the request sent to '/workflow/task'
     *
     * @param m method to execute
     * @param request incoming request
     * @return result
     * @throws JsonResourceException requested method not implemented
     */
    private JsonValue task(Method m, JsonValue request) throws JsonResourceException {
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        List resultList = new LinkedList();
        switch (m) {
            case query:     //query based on query-id
                String queryId = ActivitiUtil.getQueryIdFromRequest(request);
                TaskQuery query = processEngine.getTaskService().createTaskQuery();
                if (QUERY_FILTERED.equals(queryId)) {
                    setTaskParams(query, request);
                } else if (!QUERY_ALL_IDS.equals(queryId)) {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST, "Unknown query-id");
                }
                List<Task> list = query.list();
                for (Task t : list) {
                    Map entry = new HashMap();
                    entry.put(ID, t.getId());
                    entry.put(ACTIVITI_NAME, t.getName());
                    resultList.add(entry);
                }
                result.add("result", resultList);
                return result;
            case action:
            case create:
            case delete:
            case patch:
            case read:
            case update:
            default:
                throw new JsonResourceException(JsonResourceException.FORBIDDEN, m + " not implemented on task");
        }
    }

    /**
     * Handle the request sent to '/workflow/task/{ID}'
     *
     * @param m method to execute
     * @param request incoming request
     * @return result
     * @throws JsonResourceException requested method not implemented or unknown
     * action parameter
     */
    private JsonValue taskId(Method m, JsonValue request) throws JsonResourceException {
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        String id = ActivitiUtil.getIdFromRequest(request);
        Task task = null;
        switch (m) {
            case update:    //update task data
                task = processEngine.getTaskService().createTaskQuery().taskId(id).singleResult();
                if (task == null) {
                    throw new JsonResourceException(JsonResourceException.NOT_FOUND);
                }
                Map value = new HashMap<String, Object>(request.get(REQUEST_BODY).expect(Map.class).asMap());
                if (value.get(ACTIVITI_ASSIGNEE) != null) {
                    task.setAssignee(value.get(ACTIVITI_ASSIGNEE).toString());
                }
                if (value.get(ACTIVITI_DESCRIPTION) != null) {
                    task.setDescription(value.get(ACTIVITI_DESCRIPTION).toString());
                }
                if (value.get(ACTIVITI_NAME) != null) {
                    task.setName(value.get(ACTIVITI_NAME).toString());
                }
                if (value.get(ACTIVITI_OWNER) != null) {
                    task.setOwner(value.get(ACTIVITI_OWNER).toString());
                }
                processEngine.getTaskService().saveTask(task);
                result.add("Task updated", id);
                return result;
            case action:    //perform some action on a task
                String action = request.get(REQUEST_PARAMS).get("_action").required().asString();
                TaskService taskService = processEngine.getTaskService();
                task = processEngine.getTaskService().createTaskQuery().taskId(id).singleResult();
                if (task == null) {
                    throw new JsonResourceException(JsonResourceException.NOT_FOUND);
                }
                if ("claim".equals(action)) {
                    taskService.claim(id, request.get(REQUEST_BODY).expect(Map.class).asMap().get("userId").toString());
                } else if ("complete".equals(action)) {
                    taskService.complete(id, request.get(REQUEST_BODY).expect(Map.class).asMap());
                } else {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST, "Unknown action");
                }
                result.add("Task action performed", action);
                return result;
            case read:      //detailed information of a running task
                TaskQuery query = processEngine.getTaskService().createTaskQuery();
                query.taskId(id);
                task = query.singleResult();
                if (task == null) {
                    throw new JsonResourceException(JsonResourceException.NOT_FOUND);
                }
                if (task != null) {
                    result.add(ID, task.getId());
                    result.add(ACTIVITI_NAME, task.getName());
                    result.add(ACTIVITI_PROCESSDEFINITIONID, task.getProcessDefinitionId());
                    result.add(ACTIVITI_PROCESSINSTANCEID, task.getProcessInstanceId());
                    result.add(ACTIVITI_OWNER, task.getOwner());
                    result.add(task.getDelegationState() == DelegationState.PENDING ? ACTIVITI_DELEGATE : ACTIVITI_ASSIGNEE, task.getAssignee());
                    result.add(ACTIVITI_DESCRIPTION, task.getDescription());
                    result.add(ACTIVITI_CREATETIME, DateUtil.getDateUtil().formatDateTime(task.getCreateTime()));
                    result.add(ACTIVITI_DUEDATE, task.getDueDate() == null ? null : DateUtil.getDateUtil().formatDateTime(task.getDueDate()));
                    result.add(ACTIVITI_EXECUTIONID, task.getExecutionId());
                    result.add(ACTIVITI_PRIORITY, task.getPriority());
                    result.add(ACTIVITI_TASKDEFINITIONKEY, task.getTaskDefinitionKey());
                    result.add(ACTIVITI_VARIABLES, processEngine.getTaskService().getVariables(task.getId()));
                    result.add("_rev","0");
                }
                return result;
            case create:
            case delete:
            case patch:
            case query:
            default:
                throw new JsonResourceException(JsonResourceException.FORBIDDEN, m + " not implemented on task/{ID}");
        }
    }

    /**
     * Query the available workflow definitions
     *
     * @return workflow definitions
     * @throws JsonResourceException
     */
    public JsonValue readWorkflow() throws JsonResourceException {
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        List resultList = new LinkedList();
        List<ProcessDefinition> definitionList = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        if (definitionList != null && definitionList.size() > 0) {
            for (ProcessDefinition processDefinition : definitionList) {
                Map<String, Object> entry = new HashMap<String, Object>();
                entry.put(ACTIVITI_KEY, processDefinition.getKey());
                entry.put(ACTIVITI_NAME, processDefinition.getName());
                entry.put(ACTIVITI_PROCESSDEFINITIONID, processDefinition.getId());
                entry.put(ACTIVITI_VERSION, processDefinition.getVersion());
                entry.put(ACTIVITI_CATEGORY, processDefinition.getCategory());
                resultList.add(entry);
            }
        }
        result.add("result", resultList);
        return result;
    }

    /**
     * Start a workflow process instance
     *
     * @param request incoming request
     * @return description of the started process instance
     * @throws JsonResourceException
     */
    public JsonValue startProcessInstance(JsonValue request) throws JsonResourceException {
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        String key = ActivitiUtil.removeKeyFromRequest(request);
        String processDefinitionId = ActivitiUtil.removeProcessDefinitionIdFromRequest(request);
        Map<String, Object> variables = ActivitiUtil.getRequestBodyFromRequest(request);

        //TODO consider to put only the parent into the params. parent/security may contain confidential access token
        //variables.put("openidm-context", new HashMap(params.get("parent").asMap()));
        ProcessInstance instance;
        if (processDefinitionId == null) {
            instance = processEngine.getRuntimeService().startProcessInstanceByKey(key, variables);
        } else {
            instance = processEngine.getRuntimeService().startProcessInstanceById(processDefinitionId, variables);
        }
        if (instance != null) {
            result.put(ACTIVITI_STATUS, instance.isEnded() ? "ended" : "suspended");
            result.put(ACTIVITI_PROCESSINSTANCEID, instance.getProcessInstanceId());
            result.put(ACTIVITI_BUSINESSKEY, instance.getBusinessKey());
            result.put(ACTIVITI_PROCESSDEFINITIONID, instance.getProcessDefinitionId());
            result.put(ID, instance.getId());
        }
        return result;
    }

    /**
     * Process the query parameters of the request and set it on the
     * ProcessInstanceQuery
     *
     * @param query Query to update
     * @param request incoming request
     */
    private void setProcessInstanceParams(ProcessInstanceQuery query, JsonValue request) {
        String processDefinitionId = request.get(REQUEST_PARAMS).get(ACTIVITI_PROCESSDEFINITIONID).asString();
        query = processDefinitionId == null ? query : query.processDefinitionId(processDefinitionId);
        String processDefinitionKey = request.get(REQUEST_PARAMS).get(ACTIVITI_PROCESSDEFINITIONKEY).asString();
        query = processDefinitionKey == null ? query : query.processDefinitionKey(processDefinitionKey);
        String processInstanceBusinessKey = request.get(REQUEST_PARAMS).get(ACTIVITI_PROCESSINSTANCEBUSINESSKEY).asString();
        query = processInstanceBusinessKey == null ? query : query.processInstanceBusinessKey(processInstanceBusinessKey);
        String processInstanceId = request.get(REQUEST_PARAMS).get(ACTIVITI_PROCESSINSTANCEID).asString();
        query = processInstanceId == null ? query : query.processInstanceId(processInstanceId);
        String superProcessInstanceId = request.get(REQUEST_PARAMS).get(ACTIVITI_SUPERPROCESSINSTANCEID).asString();
        query = superProcessInstanceId == null ? query : query.superProcessInstanceId(superProcessInstanceId);

        Map wfParams = fetchVarParams(request);
        Iterator itWf = wfParams.entrySet().iterator();
        while (itWf.hasNext()) {
            Map.Entry<String, Object> e = (Map.Entry) itWf.next();
            query.variableValueEquals(e.getKey(), e.getValue());
        }
    }

    /**
     * Process the query parameters if they are workflow/task specific
     * (prefixed: _var-...)
     *
     * @param request incoming request
     * @return map of the workflow/task parameters
     * @throws JsonException
     */
    private Map fetchVarParams(JsonValue request) throws JsonException {
        Map wfParams = new HashMap();
        Iterator itAll = request.get(REQUEST_PARAMS).asMap().entrySet().iterator();
        while (itAll.hasNext()) {
            Map.Entry<String, Object> e = (Map.Entry) itAll.next();
            if ((e.getKey().startsWith(VARIABLE_QUERY_PREFIX))) {
                wfParams.put(e.getKey().substring(5), e.getValue());
            }
        }
        return wfParams;
    }

    /**
     * Process the query parameters of the request and set it on the TaskQuery
     *
     * @param query Query to update
     * @param request incoming request
     */
    private void setTaskParams(TaskQuery query, JsonValue request) {
        String executionId = request.get(REQUEST_PARAMS).get(ACTIVITI_EXECUTIONID).asString();
        query = executionId == null ? query : query.executionId(executionId);
        String processDefinitionId = request.get(REQUEST_PARAMS).get(ACTIVITI_PROCESSDEFINITIONID).asString();
        query = processDefinitionId == null ? query : query.processDefinitionId(processDefinitionId);
        String processDefinitionKey = request.get(REQUEST_PARAMS).get(ACTIVITI_PROCESSDEFINITIONKEY).asString();
        query = processDefinitionKey == null ? query : query.processDefinitionKey(processDefinitionKey);
        String processInstanceId = request.get(REQUEST_PARAMS).get(ACTIVITI_PROCESSINSTANCEID).asString();
        query = processInstanceId == null ? query : query.processInstanceId(processInstanceId);
        String taskAssignee = request.get(REQUEST_PARAMS).get(ACTIVITI_ASSIGNEE).asString();
        query = taskAssignee == null ? query : query.taskAssignee(taskAssignee);
        String taskCandidateGroup = request.get(REQUEST_PARAMS).get(ACTIVITI_CANDIDATEGROUP).asString();
        query = taskCandidateGroup == null ? query : query.taskCandidateGroup(taskCandidateGroup);
        String taskCandidateUser = request.get(REQUEST_PARAMS).get(ACTIVITI_CANDIDATEUSER).asString();
        query = taskCandidateUser == null ? query : query.taskCandidateUser(taskCandidateUser);
        String taskId = request.get(REQUEST_PARAMS).get(ACTIVITI_TASKID).asString();
        query = taskId == null ? query : query.taskId(taskId);
        String taskName = request.get(REQUEST_PARAMS).get(ACTIVITI_TASKNAME).asString();
        query = taskName == null ? query : query.taskName(taskName);
        String taskOwner = request.get(REQUEST_PARAMS).get(ACTIVITI_OWNER).asString();
        query = taskOwner == null ? query : query.taskOwner(taskOwner);

        Map wfParams = fetchVarParams(request);
        Iterator itWf = wfParams.entrySet().iterator();
        while (itWf.hasNext()) {
            Map.Entry<String, Object> e = (Map.Entry) itWf.next();
            query = query.processVariableValueEquals(e.getKey(), e.getValue());
        }
    }

    /**
     * Create a JsonValue from a HistoricProcessInstance
     *
     * @param result result object containing the data of the
     * HistoricProcessInstance
     * @param instance source of the data
     * @return
     */
    private JsonValue convertHistoricProcessInstance(JsonValue result, HistoricProcessInstance instance) {
        result.add(ACTIVITI_BUSINESSKEY, instance.getBusinessKey());
        result.add(ACTIVITI_DELETEREASON, instance.getDeleteReason());
        result.add(ID, instance.getId());
        result.add(ACTIVITI_PROCESSDEFINITIONID, instance.getProcessDefinitionId());
        result.add(ACTIVITI_STARTUSERID, instance.getStartUserId());
        result.add(ACTIVITI_DURATIONINMILLIS, instance.getDurationInMillis());
        result.add(ACTIVITI_STARTTIME, DateUtil.getDateUtil().formatDateTime(instance.getStartTime()));
        result.add(ACTIVITI_ENDTIME, instance.getEndTime() == null ? null : DateUtil.getDateUtil().formatDateTime(instance.getEndTime()));
        result.add(ACTIVITI_SUPERPROCESSINSTANCEID, instance.getSuperProcessInstanceId());
        result.add("_rev", "0");
        return result;
    }
}
