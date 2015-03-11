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

import org.forgerock.openidm.workflow.activiti.ActivitiConstants;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.task.DelegationState;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.openidm.workflow.activiti.impl.mixin.TaskEntityMixIn;

/**
 * Resource implementation of TaskInstance related Activiti operations
 */
public class TaskInstanceResource implements CollectionResourceProvider {

    private final static ObjectMapper mapper;
    private ProcessEngine processEngine;

    static {
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().addMixInAnnotations(TaskEntity.class, TaskEntityMixIn.class);
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY, true);
}

    public TaskInstanceResource(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceUtil.notSupportedOnCollection(request));
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            TaskService taskService = processEngine.getTaskService();
            Task task = processEngine.getTaskService().createTaskQuery().taskId(resourceId).singleResult();
            if (task == null) {
                handler.handleError(new NotFoundException());
            } else {
                if ("claim".equals(request.getAction())) {
                    taskService.claim(resourceId, request.getContent().expect(Map.class).asMap().get("userId").toString());
                } else if ("complete".equals(request.getAction())) {
                    taskService.complete(resourceId, request.getContent().expect(Map.class).asMap());
                } else {
                    handler.handleError(new BadRequestException("Unknown action"));
                }
                Map<String, String> result = new HashMap<String, String>(1);
                result.put("Task action performed", request.getAction());
                handler.handleResult(new JsonValue(result));
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupportedOnInstance(request));
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupportedOnInstance(request));
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupportedOnInstance(request));
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            TaskQuery query = processEngine.getTaskService().createTaskQuery();
            if (ActivitiConstants.QUERY_FILTERED.equals(request.getQueryId())
                    || ActivitiConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                if (ActivitiConstants.QUERY_FILTERED.equals(request.getQueryId())) {
                    setTaskParams(query, request);
                }
                List<Task> list = query.list();
                for (Task taskInstance : list) {
                    Map value = mapper.convertValue(taskInstance, HashMap.class);
                    Resource r = new Resource(taskInstance.getId(), null, new JsonValue(value));
                    if (taskInstance.getDelegationState() == DelegationState.PENDING) {
                        r.getContent().add(ActivitiConstants.ACTIVITI_DELEGATE, taskInstance.getAssignee());
                    } else {
                        r.getContent().add(ActivitiConstants.ACTIVITI_ASSIGNEE, taskInstance.getAssignee());
                    }
                    handler.handleResource(r);
                }
                handler.handleResult(new QueryResult());
            } else {
                handler.handleError(new BadRequestException("Unknown query-id"));
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            TaskQuery query = processEngine.getTaskService().createTaskQuery();
            query.taskId(resourceId);
            Task task = query.singleResult();
            if (task == null) {
                handler.handleError(new NotFoundException());
            } else {
                Map value = mapper.convertValue(task, HashMap.class);
                Resource r = new Resource(task.getId(), null, new JsonValue(value));
                TaskFormData data = processEngine.getFormService().getTaskFormData(task.getId());
                List<Map> propertyValues = new ArrayList<Map>();
                for (FormProperty p : data.getFormProperties()) {
                    Map<String, String> entry = new HashMap<String, String>();
                    entry.put(p.getId(), p.getValue());
                    propertyValues.add(entry);
                }
                r.getContent().add(ActivitiConstants.FORMPROPERTIES, propertyValues);
                if (task.getDelegationState() == DelegationState.PENDING) {
                    r.getContent().add(ActivitiConstants.ACTIVITI_DELEGATE, task.getAssignee());
                } else {
                    r.getContent().add(ActivitiConstants.ACTIVITI_ASSIGNEE, task.getAssignee());
                }
                Map<String, Object> variables = new HashMap<String, Object>(processEngine.getTaskService().getVariables(task.getId()));
                if (variables.containsKey(ActivitiConstants.OPENIDM_CONTEXT)){
                    variables.remove(ActivitiConstants.OPENIDM_CONTEXT);
                }
                r.getContent().add(ActivitiConstants.ACTIVITI_VARIABLES, variables);
                handler.handleResult(r);
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request, ResultHandler<Resource> handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            Task task = processEngine.getTaskService().createTaskQuery().taskId(resourceId).singleResult();
            if (task == null) {
                handler.handleError(new NotFoundException());
            } else {
                Map value = request.getContent().expect(Map.class).asMap();
                if (value.containsKey(ActivitiConstants.ACTIVITI_ASSIGNEE)) {
                    task.setAssignee(value.get(ActivitiConstants.ACTIVITI_ASSIGNEE) == null
                            ? null : value.get(ActivitiConstants.ACTIVITI_ASSIGNEE).toString());
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
                Map<String, String> result = new HashMap<String, String>(1);
                result.put("Task updated", resourceId);
                handler.handleResult(new Resource(resourceId, null, new JsonValue(result)));
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    /**
     * Process the query parameters of the request and set it on the TaskQuery
     *
     * @param query Query to update
     * @param request incoming request
     */
    private void setTaskParams(TaskQuery query, QueryRequest request) {
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

        Map<String, String> wfParams = ActivitiUtil.fetchVarParams(request);
        Iterator<Map.Entry<String, String>> itWf = wfParams.entrySet().iterator();
        while (itWf.hasNext()) {
            Map.Entry<String, String> e = itWf.next();
            query = query.processVariableValueEquals(e.getKey(), e.getValue());
        }
    }
}
