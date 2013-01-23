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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Iterator;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.FormService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.form.DefaultStartFormHandler;
import org.activiti.engine.impl.form.DefaultTaskFormHandler;
import org.activiti.engine.impl.form.FormPropertyHandler;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
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
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.audit.util.ActivityLog;
import org.forgerock.openidm.objset.ObjectSetContext;

/**
 * Implementation of the Activiti Engine Resource
 *
 * @author orsolyamebold
 */
public class ActivitiResource implements JsonResource {

    private ProcessEngine processEngine;
 
    public ActivitiResource(ProcessEngine engine) {
        this.processEngine = engine;
    }

    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        try {
            ActivitiConstants.WorkflowPath path = getPath(request);
            Authentication.setAuthenticatedUserId(ActivityLog.getRequester(ObjectSetContext.get()));
            Method method = request.get("method").required().asEnum(SimpleJsonResource.Method.class);
            switch (path) {
                case processdefinition:     //workflow/processdefinition
                    return processDefinition(method, request);
                case processdefinitionid:     //workflow/processdefinition/{ID}
                    return processDefinitionId(method, request);
                case processinstance:     //workflow/processinstance
                    return processInstance(method, request);
                case processinstanceid:     //workflow/processinstance/{ID}
                    return processInstanceId(method, request);
                case taskdefinition:    //workflow/taskdefinition
                    return taskDefinition(method, request);
                case taskinstance:     //workflow/taskinstance
                    return taskInstance(method, request);
                case taskinstanceid:     //workflow/taskinstance/{ID}
                    return taskInstanceId(method, request);
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
    private ActivitiConstants.WorkflowPath getPath(JsonValue request) {
        String id = request.get("id").asString() == null ? "" : request.get("id").asString();
        if (id.matches(ActivitiConstants.PROCESSDEFINITION_PATTERN)) {
            if (id.matches(ActivitiConstants.PROCESSDEFINITION_ID_PATTERN)) {
                return ActivitiConstants.WorkflowPath.processdefinitionid;
            } else {
                return ActivitiConstants.WorkflowPath.processdefinition;
            }
        } else if (id.matches(ActivitiConstants.PROCESSINSTANCE_PATTERN)) {
            if (id.matches(ActivitiConstants.PROCESSINSTANCE_ID_PATTERN)) {
                return ActivitiConstants.WorkflowPath.processinstanceid;
            } else {
                return ActivitiConstants.WorkflowPath.processinstance;
            }
        } else if (id.matches(ActivitiConstants.TASKINSTANCE_PATTERN)) {
            if (id.matches(ActivitiConstants.TASKINSTANCE_ID_PATTERN)) {
                return ActivitiConstants.WorkflowPath.taskinstanceid;
            } else {
                return ActivitiConstants.WorkflowPath.taskinstance;
            }
        } else if (id.matches(ActivitiConstants.TASKDEFINITION_PATTERN)) {
            if (id.matches(ActivitiConstants.TASKDEFINITION_ID_PATTERN)) {
                return ActivitiConstants.WorkflowPath.taskdefinitionid;
            } else {
                return ActivitiConstants.WorkflowPath.taskdefinition;
            }
        }
        return ActivitiConstants.WorkflowPath.unknown;
    }

    /**
     * Handle the request sent to '/workflow/processdefinition'
     *
     * @param m method to execute
     * @return result
     * @throws JsonResourceException requested method not implemented
     */
    private JsonValue processDefinition(Method m, JsonValue request) throws JsonResourceException {
        switch (m) {
            case query:     //query based on query-id
                JsonValue result = new JsonValue(new HashMap<String, Object>());
                List resultList = new LinkedList();
                String queryId = ActivitiUtil.getQueryIdFromRequest(request);
                if (ActivitiConstants.QUERY_ALL_IDS.equals(queryId)) {
                    return queryWorkflowDefintion();
                } else if (ActivitiConstants.QUERY_FILTERED.equals(queryId)) {
                    ProcessDefinitionQuery query = processEngine.getRepositoryService().createProcessDefinitionQuery();
                    setProcessDefinitionParams(query, request);
                    List<ProcessDefinition> list = query.list();
                    for (ProcessDefinition i : list) {
                        Map entry = new HashMap();
                        entry.put(ActivitiConstants.ID, i.getId());
                        entry.put(ActivitiConstants.ACTIVITI_NAME, i.getName());
                        resultList.add(entry);
                    }
                    result.add("result", resultList);
                    return result;
                }
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, "Unknown query-id");
            case read:
            case action:
            case create:
            case delete:
            case patch:
            case update:
            default:
                throw new JsonResourceException(JsonResourceException.FORBIDDEN, m + " method not implemented on processdefinition");
        }
    }

    /**
     * Handle the request sent to '/workflow/processdefinition/{ID}'
     * @param m method to execute
     * @param request incoming request
     * @return result
     * @throws JsonResourceException requested method not implemented
     */
    private JsonValue processDefinitionId(Method m, JsonValue request) throws JsonResourceException {
        String id = ActivitiUtil.getIdFromRequest(request);
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        switch (m) {
            case read:  //detailed information of a process definition
                try {
                    ProcessDefinitionEntity def = (ProcessDefinitionEntity) ((RepositoryServiceImpl)processEngine.getRepositoryService()).getDeployedProcessDefinition(id);
                    return convertProcessDefinition(result, def);
                } catch (ActivitiException e) {
                    throw new JsonResourceException(JsonResourceException.NOT_FOUND);
                }
            case delete:
            case create:
            case action:
            case patch:
            case query:
            case update:
            default:
                throw new JsonResourceException(JsonResourceException.FORBIDDEN, m + " not implemented on processdefinition/{ID}");
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
                if (ActivitiConstants.QUERY_ALL_IDS.equals(queryId)) {
                    HistoricProcessInstanceQuery query = processEngine.getHistoryService().createHistoricProcessInstanceQuery();
                    query = query.unfinished();
                    List<HistoricProcessInstance> list = query.list();
                    for (HistoricProcessInstance i : list) {
                        Map entry = new HashMap();
                        entry.put(ActivitiConstants.ID, i.getId());
                        entry.put(ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID, i.getProcessDefinitionId());
                        resultList.add(entry);
                    }
                    result.add("result", resultList);
                    return result;
                } else if (ActivitiConstants.QUERY_FILTERED.equals(queryId)) {
                    ProcessInstanceQuery query = processEngine.getRuntimeService().createProcessInstanceQuery();
                    setProcessInstanceParams(query, request);
                    List<ProcessInstance> list = query.list();
                    for (ProcessInstance i : list) {
                        Map entry = new HashMap();
                        entry.put(ActivitiConstants.ID, i.getId());
                        entry.put(ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID, i.getProcessDefinitionId());
                        resultList.add(entry);
                    }
                    result.add("result", resultList);
                    return result;
                }
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, "Unknown query-id");
            case action:    //start new workflow
                return startProcessInstance(request);
            case create:
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
     * Handle the request sent to '/workflow/taskdefinition'
     *
     * @param m method to execute
     * @param request incoming request
     * @return result
     * @throws JsonResourceException requested method not implemented or unknown
     * query-id parameter
     */
    private JsonValue taskDefinition(Method m, JsonValue request) throws JsonResourceException {
        switch (m) {
            case query:     //query based on query-id
                String queryId = ActivitiUtil.getQueryIdFromRequest(request);
                if (ActivitiConstants.QUERY_TASKDEF.equals(queryId)) {
                    String procDefId = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID);
                    String taskDefKey = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_TASKDEFINITIONKEY);
                    return queryTaskDefinition(procDefId, taskDefKey);
                }
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, "Unknown query-id");
            case action:
            case create:
            case delete:
            case patch:
            case read:
            case update:
            default:
                throw new JsonResourceException(JsonResourceException.FORBIDDEN, m + " not implemented on taskdefinition");
        }
    }

    /**
     * Handle the request sent to '/workflow/taskinstance'
     *
     * @param m method to execute
     * @param request incoming request
     * @return result
     * @throws JsonResourceException requested method not implemented
     */
    private JsonValue taskInstance(Method m, JsonValue request) throws JsonResourceException {
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        List resultList = new LinkedList();
        switch (m) {
            case query:     //query based on query-id
                String queryId = ActivitiUtil.getQueryIdFromRequest(request);
                TaskQuery query = processEngine.getTaskService().createTaskQuery();
                if (ActivitiConstants.QUERY_FILTERED.equals(queryId) ||
                        ActivitiConstants.QUERY_ALL_IDS.equals(queryId)){
                    if (ActivitiConstants.QUERY_FILTERED.equals(queryId)) {
                        setTaskParams(query, request);
                    }
                    List<Task> list = query.list();
                    for (Task t : list) {
                        Map entry = new HashMap();
                        entry.put(ActivitiConstants.ID, t.getId());
                        entry.put(ActivitiConstants.ACTIVITI_NAME, t.getName());
                        resultList.add(entry);
                    }
                    result.add("result", resultList);
                    return result;
                }
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, "Unknown query-id");
            case action:
            case create:
            case delete:
            case patch:
            case read:
            case update:
            default:
                throw new JsonResourceException(JsonResourceException.FORBIDDEN, m + " not implemented on taskinstance");
        }
    }

    /**
     * Handle the request sent to '/workflow/taskinstance/{ID}'
     *
     * @param m method to execute
     * @param request incoming request
     * @return result
     * @throws JsonResourceException requested method not implemented or unknown
     * action parameter
     */
    private JsonValue taskInstanceId(Method m, JsonValue request) throws JsonResourceException {
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        String id = ActivitiUtil.getIdFromRequest(request);
        Task task = null;
        switch (m) {
            case update:    //update task data
                task = processEngine.getTaskService().createTaskQuery().taskId(id).singleResult();
                if (task == null) {
                    throw new JsonResourceException(JsonResourceException.NOT_FOUND);
                }
                Map value = new HashMap<String, Object>(request.get(ActivitiConstants.REQUEST_BODY).expect(Map.class).asMap());
                if (value.get(ActivitiConstants.ACTIVITI_ASSIGNEE) != null) {
                    task.setAssignee(value.get(ActivitiConstants.ACTIVITI_ASSIGNEE).toString());
                } else {
                    task.setAssignee(null);
                }
                if (value.get(ActivitiConstants.ACTIVITI_DESCRIPTION) != null) {
                    task.setDescription(value.get(ActivitiConstants.ACTIVITI_DESCRIPTION).toString());
                }
                if (value.get(ActivitiConstants.ACTIVITI_NAME) != null) {
                    task.setName(value.get(ActivitiConstants.ACTIVITI_NAME).toString());
                }
                if (value.get(ActivitiConstants.ACTIVITI_OWNER) != null) {
                    task.setOwner(value.get(ActivitiConstants.ACTIVITI_OWNER).toString());
                }
                processEngine.getTaskService().saveTask(task);
                result.add("Task updated", id);
                return result;
            case action:    //perform some action on a task
                String action = request.get(ActivitiConstants.REQUEST_PARAMS).get("_action").required().asString();
                TaskService taskService = processEngine.getTaskService();
                task = processEngine.getTaskService().createTaskQuery().taskId(id).singleResult();
                if (task == null) {
                    throw new JsonResourceException(JsonResourceException.NOT_FOUND);
                }
                if ("claim".equals(action)) {
                    taskService.claim(id, request.get(ActivitiConstants.REQUEST_BODY).expect(Map.class).asMap().get("userId").toString());
                } else if ("complete".equals(action)) {
                    taskService.complete(id, request.get(ActivitiConstants.REQUEST_BODY).expect(Map.class).asMap());
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
                    result.add(ActivitiConstants.ID, task.getId());
                    result.add(ActivitiConstants.ACTIVITI_NAME, task.getName());
                    result.add(ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID, task.getProcessDefinitionId());
                    result.add(ActivitiConstants.ACTIVITI_PROCESSINSTANCEID, task.getProcessInstanceId());
                    result.add(ActivitiConstants.ACTIVITI_OWNER, task.getOwner());
                    result.add(task.getDelegationState() == DelegationState.PENDING ? 
                            ActivitiConstants.ACTIVITI_DELEGATE : ActivitiConstants.ACTIVITI_ASSIGNEE, task.getAssignee());
                    result.add(ActivitiConstants.ACTIVITI_DESCRIPTION, task.getDescription());
                    result.add(ActivitiConstants.ACTIVITI_CREATETIME, DateUtil.getDateUtil().formatDateTime(task.getCreateTime()));
                    result.add(ActivitiConstants.ACTIVITI_DUEDATE, task.getDueDate() == null ? null : DateUtil.getDateUtil().formatDateTime(task.getDueDate()));
                    result.add(ActivitiConstants.ACTIVITI_EXECUTIONID, task.getExecutionId());
                    result.add(ActivitiConstants.ACTIVITI_PRIORITY, task.getPriority());
                    result.add(ActivitiConstants.ACTIVITI_TASKDEFINITIONKEY, task.getTaskDefinitionKey());
                    result.add(ActivitiConstants.ACTIVITI_VARIABLES, processEngine.getTaskService().getVariables(task.getId()));
                    TaskFormData data = processEngine.getFormService().getTaskFormData(task.getId());
                    List<Map> propertyValues = new ArrayList<Map>();
                    for (FormProperty p : data.getFormProperties()) {
                        Map<String, String> entry = new HashMap<String, String>();
                        entry.put(p.getId(), p.getValue());
                        propertyValues.add(entry);
                    }
                    result.add(ActivitiConstants.FORMPROPERTIES, propertyValues);
                    result.add("_rev", "0");
                }
                return result;
            case create:
            case delete:
            case patch:
            case query:
            default:
                throw new JsonResourceException(JsonResourceException.FORBIDDEN, m + " not implemented on taskinstance/{ID}");
        }
    }

    /**
     * Query the available workflow definitions
     *
     * @return workflow definitions
     * @throws JsonResourceException
     */
    public JsonValue queryWorkflowDefintion() throws JsonResourceException {
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        List resultList = new LinkedList();
        List<ProcessDefinition> definitionList = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        if (definitionList != null && definitionList.size() > 0) {
            for (ProcessDefinition processDefinition : definitionList) {
                Map<String, Object> entry = new HashMap<String, Object>();
                entry.put(ActivitiConstants.ID, processDefinition.getId());
                entry.put(ActivitiConstants.ACTIVITI_NAME, processDefinition.getName());
                resultList.add(entry);
            }
        }
        result.add("result", resultList);
        return result;
    }
    
    /**
     * Query the taskdefinition based on processDefinitionId and taskdefinitionKey
     *
     * @return taskdefinition description
     * @throws JsonResourceException
     */
        public JsonValue queryTaskDefinition(String procDefId, String taskDefinitionKey) throws JsonResourceException {
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        try {
            ProcessDefinitionEntity procdef = (ProcessDefinitionEntity) ((RepositoryServiceImpl)processEngine.getRepositoryService()).getDeployedProcessDefinition(procDefId);
            convertTaskDefinition(result, procdef, taskDefinitionKey);
            return result;
        } catch (ActivitiException e) {
            throw new JsonResourceException(JsonResourceException.NOT_FOUND);
        }
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
        String businessKey = ActivitiUtil.removeBusinessKeyFromRequest(request);
        String processDefinitionId = ActivitiUtil.removeProcessDefinitionIdFromRequest(request);
        Map<String, Object> variables = ActivitiUtil.getRequestBodyFromRequest(request);
        variables.put("openidmcontext", ObjectSetContext.get().get("parent"));

        ProcessInstance instance;
        if (processDefinitionId == null) {
            instance = processEngine.getRuntimeService().startProcessInstanceByKey(key, businessKey, variables);
        } else {
            instance = processEngine.getRuntimeService().startProcessInstanceById(processDefinitionId, businessKey, variables);
        }
        if (instance != null) {
            result.put(ActivitiConstants.ACTIVITI_STATUS, instance.isEnded() ? "ended" : "suspended");
            result.put(ActivitiConstants.ACTIVITI_PROCESSINSTANCEID, instance.getProcessInstanceId());
            result.put(ActivitiConstants.ACTIVITI_BUSINESSKEY, instance.getBusinessKey());
            result.put(ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID, instance.getProcessDefinitionId());
            result.put(ActivitiConstants.ID, instance.getId());
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
        String processDefinitionId = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID);
        query = processDefinitionId == null ? query : query.processDefinitionId(processDefinitionId);
        String processDefinitionKey = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_PROCESSDEFINITIONKEY);
        query = processDefinitionKey == null ? query : query.processDefinitionKey(processDefinitionKey);
        String processInstanceBusinessKey = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_PROCESSINSTANCEBUSINESSKEY);
        query = processInstanceBusinessKey == null ? query : query.processInstanceBusinessKey(processInstanceBusinessKey);
        String processInstanceId = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_PROCESSINSTANCEID);
        query = processInstanceId == null ? query : query.processInstanceId(processInstanceId);
        String superProcessInstanceId = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_SUPERPROCESSINSTANCEID);
        query = superProcessInstanceId == null ? query : query.superProcessInstanceId(superProcessInstanceId);

        Map wfParams = fetchVarParams(request);
        Iterator itWf = wfParams.entrySet().iterator();
        while (itWf.hasNext()) {
            Map.Entry<String, Object> e = (Map.Entry) itWf.next();
            query.variableValueEquals(e.getKey(), e.getValue());
        }
    }
    
    /**
     * Process the query parameters of the request and set it on the
     * ProcessDefinitionQuery
     *
     * @param query Query to update
     * @param request incoming request
     */
    private void setProcessDefinitionParams(ProcessDefinitionQuery query, JsonValue request) {
        String deploymentId = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_DEPLOYMENTID);
        query = deploymentId == null ? query : query.deploymentId(deploymentId);
        String category = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_CATEGORY);
        query = category == null ? query : query.processDefinitionCategory(category);
        String categoryLike = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_CATEGORY + ActivitiConstants.LIKE);
        query = categoryLike == null ? query : query.processDefinitionCategoryLike(categoryLike);
        String processDefinitionId = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ID);
        query = processDefinitionId == null ? query : query.processDefinitionId(processDefinitionId);
        String processDefinitionKey = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_KEY);
        query = processDefinitionKey == null ? query : query.processDefinitionKey(processDefinitionKey);
        String processDefinitionKeyLike = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_KEY + ActivitiConstants.LIKE);
        query = processDefinitionKeyLike == null ? query : query.processDefinitionKeyLike(processDefinitionKeyLike);
        String processDefinitionName = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_NAME);
        query = processDefinitionName == null ? query : query.processDefinitionName(processDefinitionName);
        String processDefinitionNameLike = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_NAME + ActivitiConstants.LIKE);
        query = processDefinitionNameLike == null ? query : query.processDefinitionNameLike(processDefinitionNameLike);
        String processDefinitionResourceName = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_PROCESSDEFINITIONRESOURCENAME);
        query = processDefinitionResourceName == null? query : query.processDefinitionResourceName(processDefinitionResourceName);
        String processDefinitionResourceNameLike = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_PROCESSDEFINITIONRESOURCENAME + ActivitiConstants.LIKE);
        query = processDefinitionResourceNameLike == null ? query : query.processDefinitionResourceNameLike(processDefinitionResourceNameLike);
        String processDefinitionVersion = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_VERSION);
        query = processDefinitionVersion == null ? query : query.processDefinitionVersion(Integer.getInteger(processDefinitionVersion));
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
        Iterator itAll = request.get(ActivitiConstants.REQUEST_PARAMS).asMap().entrySet().iterator();
        while (itAll.hasNext()) {
            Map.Entry<String, Object> e = (Map.Entry) itAll.next();
            if ((e.getKey().startsWith(ActivitiConstants.VARIABLE_QUERY_PREFIX))) {
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
        String executionId = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_EXECUTIONID);
        query = executionId == null ? query : query.executionId(executionId);
        String processDefinitionId = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID);
        query = processDefinitionId == null ? query : query.processDefinitionId(processDefinitionId);
        String processDefinitionKey = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_PROCESSDEFINITIONKEY);
        query = processDefinitionKey == null ? query : query.processDefinitionKey(processDefinitionKey);
        String processInstanceId = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_PROCESSINSTANCEID);
        query = processInstanceId == null ? query : query.processInstanceId(processInstanceId);
        String taskAssignee = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_ASSIGNEE);
        query = taskAssignee == null ? query : query.taskAssignee(taskAssignee);
        String taskCandidateGroup = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_CANDIDATEGROUP);
        if (taskCandidateGroup != null) {
            String[] taskCandidateGroups = taskCandidateGroup.split(",");
            if (taskCandidateGroups.length > 1) {
                query.taskCandidateGroupIn(Arrays.asList(taskCandidateGroups));
            } else {
                query.taskCandidateGroup(taskCandidateGroup);
            }
        }
        String taskCandidateUser = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_CANDIDATEUSER);
        query = taskCandidateUser == null ? query : query.taskCandidateUser(taskCandidateUser);
        String taskId = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_TASKID);
        query = taskId == null ? query : query.taskId(taskId);
        String taskName = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_TASKNAME);
        query = taskName == null ? query : query.taskName(taskName);
        String taskOwner = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_OWNER);
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
        result.add(ActivitiConstants.ACTIVITI_BUSINESSKEY, instance.getBusinessKey());
        result.add(ActivitiConstants.ACTIVITI_DELETEREASON, instance.getDeleteReason());
        result.add(ActivitiConstants.ID, instance.getId());
        result.add(ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID, instance.getProcessDefinitionId());
        result.add(ActivitiConstants.ACTIVITI_STARTUSERID, instance.getStartUserId());
        result.add(ActivitiConstants.ACTIVITI_DURATIONINMILLIS, instance.getDurationInMillis());
        result.add(ActivitiConstants.ACTIVITI_STARTTIME, DateUtil.getDateUtil().formatDateTime(instance.getStartTime()));
        result.add(ActivitiConstants.ACTIVITI_ENDTIME, instance.getEndTime() == null ? null : DateUtil.getDateUtil().formatDateTime(instance.getEndTime()));
        result.add(ActivitiConstants.ACTIVITI_SUPERPROCESSINSTANCEID, instance.getSuperProcessInstanceId());
        result.add("_rev", "0");
        return result;
    }
    
    /**
     * Create a JsonValue from a ProcessDefinition
     *
     * @param result result object containing the data of the
     * ProcessDefinition
     * @param def source of the data
     * @return
     */
    private JsonValue convertProcessDefinition(JsonValue result, ProcessDefinitionEntity def) {
        result.add(ActivitiConstants.ACTIVITI_CATEGORY, def.getCategory());
        result.add(ActivitiConstants.ACTIVITI_DEPLOYMENTID, def.getDeploymentId());
        result.add(ActivitiConstants.ACTIVITI_DESCRIPTION, def.getDescription());
        result.add(ActivitiConstants.ACTIVITI_DIAGRAMRESOURCENAME, def.getDiagramResourceName());
        result.add(ActivitiConstants.ID, def.getId());
        result.add(ActivitiConstants.ACTIVITI_KEY, def.getKey());
        result.add(ActivitiConstants.ACTIVITI_NAME, def.getName());
        FormService formService = processEngine.getFormService();
        StartFormData startFormData = formService.getStartFormData(def.getId());
        if (def.hasStartFormKey()) {
            result.add(ActivitiConstants.ACTIVITI_FORMRESOURCEKEY, startFormData.getFormKey());
        }
        DefaultStartFormHandler handler = (DefaultStartFormHandler) def.getStartFormHandler();
        List<Map> propertyList = new ArrayList<Map>();
        addFormHandlerData(propertyList, handler.getFormPropertyHandlers());
        result.add(ActivitiConstants.FORMPROPERTIES, propertyList);
        result.add("_rev", "0");
        return result;
    }
    
    /**
     * Create a JsonValue from a TaskEntity representing the TaskDefinition
     *
     * @param result result object containing the data of the
     * Task
     * @param t source of the data
     * @return
     */
    private JsonValue convertTaskDefinition(JsonValue result, ProcessDefinitionEntity procdef, String taskDefinitionKey) throws JsonResourceException {
        TaskDefinition def = procdef.getTaskDefinitions().get(taskDefinitionKey);
        if (def != null) {
            DefaultTaskFormHandler handler = (DefaultTaskFormHandler) def.getTaskFormHandler();
            result.add(ActivitiConstants.ID, def.getKey());
            result.add(ActivitiConstants.ACTIVITI_NAME, def.getNameExpression());
            result.add(ActivitiConstants.ACTIVITI_ASSIGNEE, def.getAssigneeExpression());
            result.add(ActivitiConstants.ACTIVITI_CANDIDATEUSER, def.getCandidateUserIdExpressions());
            result.add(ActivitiConstants.ACTIVITI_CANDIDATEGROUP, def.getCandidateGroupIdExpressions());
            result.add(ActivitiConstants.ACTIVITI_FORMRESOURCEKEY, handler.getFormKey());
            result.add(ActivitiConstants.ACTIVITI_DUEDATE, def.getDueDateExpression());
            result.add(ActivitiConstants.ACTIVITI_PRIORITY, def.getPriorityExpression());
            List<Map> propertyList = new ArrayList<Map>();
            addFormHandlerData(propertyList, handler.getFormPropertyHandlers());
            result.add(ActivitiConstants.FORMPROPERTIES, propertyList);
            return result;
        }
        throw new JsonResourceException(JsonResourceException.NOT_FOUND);
    }
    
    /**
     * Add FormProperty related data to the map of task properties
     * @param propertyList map containing the result
     * @param handlers list of handlers to process
     */
    private void addFormHandlerData(List<Map> propertyList, List<FormPropertyHandler> handlers) {
        for (FormPropertyHandler h : handlers) {
            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put(ActivitiConstants.ID, h.getId());
            entry.put(ActivitiConstants.FORMPROPERTY_DEFAULTEXPRESSION, h.getDefaultExpression());
            entry.put(ActivitiConstants.FORMPROPERTY_VARIABLEEXPRESSION, h.getVariableExpression());
            entry.put(ActivitiConstants.FORMPROPERTY_VARIABLENAME, h.getVariableName());
            entry.put(ActivitiConstants.ACTIVITI_NAME, h.getName());
            Map type = new HashMap(2);
            if (h.getType() != null) {
                type.put(ActivitiConstants.ACTIVITI_NAME, h.getType().getName());
                type.put(ActivitiConstants.ENUM_VALUES, h.getType().getInformation("values"));
                type.put(ActivitiConstants.DATE_PATTERN, h.getType().getInformation("datePattern"));
            }
            entry.put(ActivitiConstants.FORMPROPERTY_TYPE, type);
            entry.put(ActivitiConstants.FORMPROPERTY_READABLE, h.isReadable());
            entry.put(ActivitiConstants.FORMPROPERTY_REQUIRED, h.isRequired());
            entry.put(ActivitiConstants.FORMPROPERTY_WRITABLE, h.isWritable());
            propertyList.add(entry);
        }
    }
}
