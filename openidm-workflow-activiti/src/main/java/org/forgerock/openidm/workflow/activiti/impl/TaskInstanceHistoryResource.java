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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.workflow.activiti.impl;

import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.util.ResourceUtil.notSupportedOnInstance;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.SortKey;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.workflow.activiti.ActivitiConstants;
import org.forgerock.openidm.workflow.activiti.impl.mixin.HistoricTaskInstanceEntityMixIn;
import org.forgerock.util.promise.Promise;

import java.util.Arrays;
import java.util.Map;

/**
 *  Resource implementation of HistoricTaskInstance related to Activiti operations.
 */
public class TaskInstanceHistoryResource implements CollectionResourceProvider {

    private static final ObjectMapper MAPPER;
    private ProcessEngine processEngine;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.addMixIn(HistoricTaskInstanceEntity.class,
                HistoricTaskInstanceEntityMixIn.class);
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    /**
     * Construct the TaskInstanceHistoryResource.
     *
     * @param processEngine the Activiti engine used for this resource
     */
    public TaskInstanceHistoryResource(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId, ActionRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId, DeleteRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId, PatchRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request, QueryResourceHandler handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            HistoricTaskInstanceQuery query = processEngine.getHistoryService().createHistoricTaskInstanceQuery();

            if (ActivitiConstants.QUERY_FILTERED.equals(request.getQueryId())
                    || ActivitiConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {

                if (ActivitiConstants.QUERY_FILTERED.equals(request.getQueryId())) {
                    setTaskParams(query, request);
                    setSortKeys(query, request);
                }
                for (HistoricTaskInstance i : query.list()) {
                    Map<String, Object> value = MAPPER.convertValue(i, Map.class);
                    handler.handleResource(newResourceResponse(i.getId(), null, new JsonValue(value)));
                }
                return newQueryResponse().asPromise();
            } else {
                return new BadRequestException("Unknown query-id").asPromise();
            }
        } catch (Exception ex) {
            return new InternalServerErrorException(ex.getMessage(), ex).asPromise();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId, ReadRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId, UpdateRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    /**
     * Process the query parameters of the request and set it on the HistoricTaskInstanceQuery
     * being passed in.
     *
     * @param query Query to update
     * @param request incoming request
     */
    private void setTaskParams(HistoricTaskInstanceQuery query, QueryRequest request) {

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
                    String[] taskCandidateGroups = param.getValue().split(",");
                    if (taskCandidateGroups.length > 1) {
                        query.taskCandidateGroupIn(Arrays.asList(taskCandidateGroups));
                    } else {
                        query.taskCandidateGroup(param.getValue());
                    }
                    break;
                case ActivitiConstants.ACTIVITI_CANDIDATEUSER:
                    query.taskCandidateUser(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_TASKID:
                    query.taskId(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_TASKNAME:
                    query.taskName(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_OWNER:
                    query.taskOwner(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_DESCRIPTION:
                    query.taskDescription(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_FINISHED:
                    if (Boolean.parseBoolean(param.getValue())) {
                        query.finished();
                    }
                    break;
                case ActivitiConstants.ACTIVITI_UNFINISHED:
                    if (Boolean.parseBoolean(param.getValue())) {
                        query.unfinished();
                    }
                    break;
                case ActivitiConstants.ACTIVITI_PROCESSFINISHED:
                    if (Boolean.parseBoolean(param.getValue())) {
                        query.processFinished();
                    }
                    break;
                case ActivitiConstants.ACTIVITI_PROCESSUNFINISHED:
                    if (Boolean.parseBoolean(param.getValue())) {
                        query.processUnfinished();
                    }
                    break;
                case ActivitiConstants.ACTIVITI_PRIORITY:
                    query.taskPriority(Integer.parseInt(param.getValue()));
                    break;
                case ActivitiConstants.ACTIVITI_DELETEREASON:
                    query.taskDeleteReason(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_TENANTID:
                    query.taskTenantId(param.getValue());
                    break;
            }
        }

        // handle instance variables that can be queried on as well
        for (Map.Entry<String, String> entry : ActivitiUtil.fetchVarParams(request).entrySet()) {
            query = query.processVariableValueEquals(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Sets what the result set should be filtered by.
     *
     * @param query TaskQuery that needs to be modified for filtering
     * @param request incoming request
     * @throws NotSupportedException
     */
    private void setSortKeys(HistoricTaskInstanceQuery query, QueryRequest request) throws NotSupportedException {
        for (SortKey key : request.getSortKeys()) {
            if (key.getField() != null && !key.getField().isEmpty()) {
                switch (key.getField().toString().substring(1)) { // remove leading JsonPointer slash
                    case ActivitiConstants.ACTIVITI_TASKID:
                        query.orderByTaskId();
                        break;
                    case ActivitiConstants.ACTIVITI_TASKNAME:
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
                    case ActivitiConstants.ACTIVITI_PROCESSINSTANCEID:
                        query.orderByProcessInstanceId();
                        break;
                    case ActivitiConstants.ACTIVITI_EXECUTIONID:
                        query.orderByExecutionId();
                        break;
                    case ActivitiConstants.ACTIVITI_TENANTID:
                        query.orderByTenantId();
                        break;
                    case ActivitiConstants.ID:
                        query.orderByHistoricActivityInstanceId();
                        break;
                    case ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID:
                        query.orderByProcessDefinitionId();
                        break;
                    case ActivitiConstants.ACTIVITI_DURATIONINMILLIS:
                        query.orderByHistoricTaskInstanceDuration();
                        break;
                    case ActivitiConstants.ACTIVITI_STARTTIME:
                        query.orderByHistoricTaskInstanceStartTime();
                        break;
                    case ActivitiConstants.ACTIVITI_ENDTIME:
                        query.orderByHistoricTaskInstanceEndTime();
                        break;
                    case ActivitiConstants.ACTIVITI_OWNER:
                        query.orderByTaskOwner();
                        break;
                    case ActivitiConstants.ACTIVITI_DUEDATE:
                        query.orderByTaskDueDate();
                        break;
                    case ActivitiConstants.ACTIVITI_DELETEREASON:
                        query.orderByDeleteReason();
                        break;
                    case ActivitiConstants.ACTIVITI_TASKDEFINITIONKEY:
                        query.orderByTaskDefinitionKey();
                        break;
                    default:
                        throw new NotSupportedException(
                                "Sort key: " + key.getField().toString().substring(1) + " is not valid");
                }
                query = key.isAscendingOrder() ? query.asc() : query.desc();
            }
        }
    }
}
