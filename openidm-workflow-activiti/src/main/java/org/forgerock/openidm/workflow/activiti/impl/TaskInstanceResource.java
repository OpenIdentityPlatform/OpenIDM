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
 * Copyright 2012-2015 ForgeRock AS.
 */
package org.forgerock.openidm.workflow.activiti.impl;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.util.ResourceUtil.*;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.task.IdentityLink;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.json.resource.SortKey;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.workflow.activiti.ActivitiConstants;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
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
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.workflow.activiti.impl.mixin.TaskEntityMixIn;
import org.forgerock.util.promise.Promise;


/**
 * Resource implementation of TaskInstance related Activiti operations
 */
public class TaskInstanceResource implements CollectionResourceProvider {

    private final static ObjectMapper mapper;
    private ProcessEngine processEngine;

    static {
        mapper = new ObjectMapper();
        mapper.addMixIn(TaskEntity.class, TaskEntityMixIn.class);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public TaskInstanceResource(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return notSupportedOnCollection(request).asPromise();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId, ActionRequest request) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            TaskService taskService = processEngine.getTaskService();
            Task task = processEngine.getTaskService().createTaskQuery().taskId(resourceId).singleResult();
            if (task == null) {
                return new NotFoundException().asPromise();
            } else {
                if ("claim".equals(request.getAction())) {
                    taskService.claim(resourceId, request.getContent().expect(Map.class).asMap().get("userId").toString());
                } else if ("complete".equals(request.getAction())) {
                    taskService.complete(resourceId, request.getContent().expect(Map.class).asMap());
                } else {
                    return new BadRequestException("Unknown action").asPromise();
                }
                Map<String, String> result = new HashMap<String, String>(1);
                result.put("Task action performed", request.getAction());
                return newActionResponse(new JsonValue(result)).asPromise();
            }
        } catch (Exception ex) {
            return new InternalServerErrorException(ex.getMessage(), ex).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId, DeleteRequest request) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());

            Task task = processEngine.getTaskService().createTaskQuery().taskId(resourceId).singleResult();
            if (task == null) {
                return new NotFoundException("Task " + resourceId + " not found.").asPromise();
            }

            JsonValue deletedTask = json(mapper.convertValue(task, Map.class));
            processEngine.getTaskService()
                    .deleteTask(resourceId, request.getAdditionalParameter(ActivitiConstants.ACTIVITI_DELETEREASON));
            return newResourceResponse(task.getId(), null, deletedTask).asPromise();
        } catch (ActivitiObjectNotFoundException ex) {
            return new NotFoundException(ex.getMessage()).asPromise();
        } catch (Exception ex) {
            return new InternalServerErrorException(ex.getMessage(), ex).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId, PatchRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request, QueryResourceHandler handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            TaskQuery query = processEngine.getTaskService().createTaskQuery();
            if (ActivitiConstants.QUERY_FILTERED.equals(request.getQueryId())
                    || ActivitiConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                if (ActivitiConstants.QUERY_FILTERED.equals(request.getQueryId())) {
                    setTaskParams(query, request);
                }
                setSortKeys(query, request);
                List<Task> list = query.list();
                for (Task taskInstance : list) {
                    JsonValue value = json(mapper.convertValue(taskInstance, Map.class));
                    ResourceResponse r = newResourceResponse(taskInstance.getId(), null, value);
                    if (taskInstance.getDelegationState() == DelegationState.PENDING) {
                        r.getContent().add(ActivitiConstants.ACTIVITI_DELEGATE, taskInstance.getAssignee());
                    } else {
                        r.getContent().add(ActivitiConstants.ACTIVITI_ASSIGNEE, taskInstance.getAssignee());
                    }
                    handler.handleResource(r);
                }
                return newQueryResponse().asPromise();
            } else {
                return new BadRequestException("Unknown query-id").asPromise();
            }
        } catch (NotSupportedException e) {
            return e.asPromise();
        } catch (Exception ex) {
            return new InternalServerErrorException(ex.getMessage(), ex).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId, ReadRequest request) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            TaskQuery query = processEngine.getTaskService().createTaskQuery();
            query.taskId(resourceId);
            Task task = query.singleResult();
            if (task == null) {
                return new NotFoundException().asPromise();
            } else {
                JsonValue value = json(mapper.convertValue(task, Map.class));
                TaskFormData data = processEngine.getFormService().getTaskFormData(task.getId());
                List<Map<String, String>> propertyValues = new ArrayList<>();
                for (FormProperty p : data.getFormProperties()) {
                    Map<String, String> entry = new HashMap<>();
                    entry.put(p.getId(), p.getValue());
                    propertyValues.add(entry);
                }
                value.put(ActivitiConstants.FORMPROPERTIES, propertyValues);

                if (task.getDelegationState() == DelegationState.PENDING) {
                    value.put(ActivitiConstants.ACTIVITI_DELEGATE, task.getAssignee());
                } else {
                    value.put(ActivitiConstants.ACTIVITI_ASSIGNEE, task.getAssignee());
                }
                Map<String, Object> variables = processEngine.getTaskService().getVariables(task.getId());
                if (variables.containsKey(ActivitiConstants.OPENIDM_CONTEXT)){
                    variables.remove(ActivitiConstants.OPENIDM_CONTEXT);
                }

                value.put(ActivitiConstants.ACTIVITI_VARIABLES, variables);
                value.put("candidates", getCandidateIdentities(task).getObject());

                return newResourceResponse(task.getId(), null, value).asPromise();
            }
        } catch (Exception ex) {
            return new InternalServerErrorException(ex.getMessage(), ex).asPromise();
        }
    }

    /**
     * Retrieves candidate users and groups from a Task.
     *
     * @param task Task that needs to be searched
     * @return JsonValue of candidates
     */
    private JsonValue getCandidateIdentities(Task task) {
        JsonValue candidates = json(object())
                .add("candidateUsers", new HashSet<>())
                .add("candidateGroups", new HashSet<>());
        List<IdentityLink> candidateIdentity = processEngine.getTaskService().getIdentityLinksForTask(task.getId());
        for (IdentityLink identityLink : candidateIdentity) {
            if (identityLink.getUserId() != null) {
                candidates.get("candidateUsers").asSet().add(identityLink.getUserId());
            }
            if (identityLink.getGroupId() != null) {
                candidates.get("candidateGroups").asSet().add(identityLink.getGroupId());
            }
        }
        return candidates;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId, UpdateRequest request) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            Task task = processEngine.getTaskService().createTaskQuery().taskId(resourceId).singleResult();
            if (task == null) {
                return new NotFoundException().asPromise();
            } else {
                Map<String, Object> value = request.getContent().expect(Map.class).asMap();
                if (value.containsKey(ActivitiConstants.ACTIVITI_ASSIGNEE)) {
                    task.setAssignee(value.get(ActivitiConstants.ACTIVITI_ASSIGNEE) == null
                            ? null
                            : value.get(ActivitiConstants.ACTIVITI_ASSIGNEE).toString());
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
                Map<String, String> result = new HashMap<>(1);
                result.put("Task updated", resourceId);
                return newResourceResponse(resourceId, null, new JsonValue(result)).asPromise();
            }
        } catch (Exception ex) {
            return new InternalServerErrorException(ex.getMessage(), ex).asPromise();
        }
    }

    /**
     * Process the query parameters of the request and set it on the TaskQuery.
     *
     * @param query Query to update
     * @param request incoming request
     */
    private void setTaskParams(TaskQuery query, QueryRequest request) {

        for (Map.Entry<String, String> param : request.getAdditionalParameters().entrySet()) {
            switch (param.getKey()) {
                case ActivitiConstants.ACTIVITI_EXECUTIONID:
                    query.executionId(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID:
                    query.processDefinitionId(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_PROCESSDEFINITIONKEY:
                    query.processDefinitionKey(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_PROCESSINSTANCEID:
                    query.processInstanceId(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_ASSIGNEE:
                    query.taskAssignee(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_CANDIDATEGROUP:
                    String taskCandidateGroup = param.getValue();
                    String[] taskCandidateGroups = taskCandidateGroup.split(",");
                    if (taskCandidateGroups.length > 1) {
                        query.taskCandidateGroupIn(Arrays.asList(taskCandidateGroups));
                    } else {
                        query.taskCandidateGroup(taskCandidateGroup);
                    }
                    break;
                case ActivitiConstants.ACTIVITI_CANDIDATEUSER:
                    query.taskCandidateUser(param.getValue());
                    break;
                case ActivitiConstants.ID:
                    query.taskId(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_NAME:
                    query.taskName(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_OWNER:
                    query.taskOwner(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_DESCRIPTION:
                    query.taskDescription(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_PRIORITY:
                    query.taskPriority(Integer.parseInt(param.getValue()));
                    break;
                case ActivitiConstants.ACTIVITI_UNASSIGNED:
                    if (Boolean.parseBoolean(param.getValue())) {
                        query.taskUnassigned();
                    }
                    break;
                case ActivitiConstants.ACTIVITI_TENANTID:
                    query.taskTenantId(param.getValue());
                    break;
            }
        }

        Map<String, String> wfParams = ActivitiUtil.fetchVarParams(request);
        Iterator<Map.Entry<String, String>> itWf = wfParams.entrySet().iterator();
        while (itWf.hasNext()) {
            Map.Entry<String, String> e = itWf.next();
            query = query.processVariableValueEquals(e.getKey(), e.getValue());
        }
    }

    /**
     * Sets what the result set should be filtered by.
     *
     * @param query TaskQuery that needs to be modified for filtering
     * @param request incoming request
     * @throws NotSupportedException
     */
    private void setSortKeys(TaskQuery query, QueryRequest request) throws NotSupportedException {
        for (SortKey key : request.getSortKeys()) {
            if (key.getField() != null && !key.getField().isEmpty()) {
                switch (key.getField().toString().substring(1)) { // remove leading JsonPointer slash
                    case ActivitiConstants.ID:
                        query.orderByTaskId();
                        break;
                    case ActivitiConstants.ACTIVITI_NAME:
                        query.orderByTaskName();
                        break;
                    case ActivitiConstants.ACTIVITI_DESCRIPTION:
                        query.orderByTaskDescription();
                        break;
                    case ActivitiConstants.ACTIVITI_PRIORITY:
                        query.orderByTaskPriority();
                        break;
                    case ActivitiConstants.ACTIVITI_ASSIGNEE:
                        query.orderByTaskAssignee();
                        break;
                    case ActivitiConstants.ACTIVITI_CREATETIME:
                        query.orderByTaskCreateTime();
                        break;
                    case ActivitiConstants.ACTIVITI_PROCESSINSTANCEID:
                        query.orderByProcessInstanceId();
                        break;
                    case ActivitiConstants.ACTIVITI_EXECUTIONID:
                        query.orderByExecutionId();
                        break;
                    case ActivitiConstants.ACTIVITI_DUEDATE:
                        query.orderByDueDate();
                        break;
                    case ActivitiConstants.ACTIVITI_TENANTID:
                        query.orderByTenantId();
                        break;
                    default:
                        throw new NotSupportedException("Sort key: " + key.getField().toString().substring(1) + " is not valid");
                }
                query = key.isAscendingOrder() ? query.asc() : query.desc();
            }
        }

    }
}
