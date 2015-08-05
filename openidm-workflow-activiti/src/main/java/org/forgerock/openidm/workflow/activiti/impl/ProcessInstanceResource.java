/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright © 2012-2015 ForgeRock AS. All rights reserved.
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

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.bpmn.diagram.ProcessDiagramGenerator;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.forgerock.openidm.workflow.activiti.ActivitiConstants;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.openidm.workflow.activiti.impl.mixin.HistoricProcessInstanceMixIn;
import org.forgerock.openidm.workflow.activiti.impl.mixin.HistoricTaskInstanceEntityMixIn;
import org.forgerock.util.Function;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.promise.NeverThrowsException;

/**
 * Resource implementation of ProcessInstance related Activiti operations
 * for both currently running processes and historic processes.
 */
public class ProcessInstanceResource implements CollectionResourceProvider {

    private final static ObjectMapper MAPPER;
    private final ProcessEngine processEngine;
    private final Function<ProcessEngine, HistoricProcessInstanceQuery, NeverThrowsException> queryFunction;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.getSerializationConfig().addMixInAnnotations(HistoricProcessInstanceEntity.class, HistoricProcessInstanceMixIn.class);
        MAPPER.getSerializationConfig().addMixInAnnotations(HistoricTaskInstanceEntity.class, HistoricTaskInstanceEntityMixIn.class);
        MAPPER.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.configure(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    /**
     * Create a new ProcessInstanceResource.
     *
     * @param processEngine Activiti engine used for this resource
     * @param queryFunction a Function to provide a the properly-configured HistoricProcessInstanceQuery as appropriate
     *                      for the type of query issued by this CollectionResourceProvider; allows this class to
     *                      support both present and historic queries
     */
    public ProcessInstanceResource(ProcessEngine processEngine, Function<ProcessEngine, HistoricProcessInstanceQuery,
            NeverThrowsException> queryFunction) {
        this.processEngine = processEngine;
        this.queryFunction = queryFunction;
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceUtil.notSupportedOnCollection(request));
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceUtil.notSupportedOnInstance(request));
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            String key = ActivitiUtil.removeKeyFromRequest(request);
            String businessKey = ActivitiUtil.removeBusinessKeyFromRequest(request);
            String processDefinitionId = ActivitiUtil.removeProcessDefinitionIdFromRequest(request);
            Map<String, Object> variables = ActivitiUtil.getRequestBodyFromRequest(request);
            variables.put(ActivitiConstants.OPENIDM_CONTEXT, context.toJsonValue());
            ProcessInstance instance;
            if (processDefinitionId == null) {
                instance = processEngine.getRuntimeService().startProcessInstanceByKey(key, businessKey, variables);
            } else {
                instance = processEngine.getRuntimeService().startProcessInstanceById(processDefinitionId, businessKey, variables);
            }
            if (instance != null) {
                Map<String, String> resultMap = new HashMap<String, String>();
                resultMap.put(ActivitiConstants.ACTIVITI_STATUS, instance.isEnded() ? "ended" : "suspended");
                resultMap.put(ActivitiConstants.ACTIVITI_PROCESSINSTANCEID, instance.getProcessInstanceId());
                resultMap.put(ActivitiConstants.ACTIVITI_BUSINESSKEY, instance.getBusinessKey());
                resultMap.put(ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID, instance.getProcessDefinitionId());
                resultMap.put(ActivitiConstants.ID, instance.getId());
                JsonValue content = new JsonValue(resultMap);
                Resource result = new Resource(instance.getId(), null, content);
                handler.handleResult(result);
            } else {
                handler.handleError(new InternalServerErrorException("The process instance could not be created"));
            }
        } catch (ActivitiObjectNotFoundException ex) {
            handler.handleError(new NotFoundException(ex.getMessage()));
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request, ResultHandler<Resource> handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            HistoricProcessInstance process = processEngine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(resourceId).singleResult();
            if (process != null) {
                Map value = MAPPER.convertValue(process, HashMap.class);
                Resource r = new Resource(process.getId(), null, new JsonValue(value));
                processEngine.getRuntimeService().deleteProcessInstance(resourceId, "Deleted by Openidm");
                handler.handleResult(r);
            } else {
                handler.handleError(new NotFoundException());
            }
        } catch (ActivitiObjectNotFoundException ex) {
            handler.handleError(new NotFoundException(ex.getMessage()));
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupportedOnInstance(request));
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            final HistoricProcessInstanceQuery query = queryFunction.apply(processEngine);
            if (ActivitiConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                for (HistoricProcessInstance i : query.list()) {
                    Map<String, Object> value = MAPPER.convertValue(i, Map.class);
                    // TODO OPENIDM-3603 add relationship support
                    value.put(ActivitiConstants.ACTIVITI_PROCESSDEFINITIONRESOURCENAME, getProcessDefName(i));
                    Resource r = new Resource(i.getId(), null, new JsonValue(value));
                    handler.handleResource(r);
                }
                handler.handleResult(new QueryResult());
            } else if (ActivitiConstants.QUERY_FILTERED.equals(request.getQueryId())) {
                setProcessInstanceParams(query, request);
                setSortKeys(query, request);
                for (HistoricProcessInstance processinstance : query.list()) {
                    Map<String, Object> value = MAPPER.convertValue(processinstance, Map.class);
                    // TODO OPENIDM-3603 add relationship support
                    value.put(ActivitiConstants.ACTIVITI_PROCESSDEFINITIONRESOURCENAME, getProcessDefName(processinstance));
                    handler.handleResource(new Resource(processinstance.getId(), null, new JsonValue(value)));
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
            HistoricProcessInstance instance =
                    processEngine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(resourceId).singleResult();

            if (instance == null) {
                handler.handleError(new NotFoundException());
            } else {
                JsonValue content = new JsonValue(MAPPER.convertValue(instance, Map.class));
                // TODO OPENIDM-3603 add relationship support
                content.put(ActivitiConstants.ACTIVITI_PROCESSDEFINITIONRESOURCENAME, getProcessDefName(instance));
                content.put("tasks", getTasksForProcess(instance.getId()));

                // diagram support
                if (request.getFields().contains(ActivitiConstants.ACTIVITI_DIAGRAM)) {
                    final RuntimeService runtimeService = processEngine.getRuntimeService();
                    final RepositoryServiceImpl repositoryService =
                            (RepositoryServiceImpl) processEngine.getRepositoryService();
                    final ExecutionEntity executionEntity = (ExecutionEntity) runtimeService
                            .createProcessInstanceQuery()
                            .processInstanceId(resourceId)
                            .singleResult();
                    if (executionEntity == null) {
                        throw new ActivitiObjectNotFoundException(
                                "Process instance with id" + resourceId + " could not be found", ProcessInstance.class);
                    }

                    final ProcessDefinitionEntity def =
                            (ProcessDefinitionEntity) repositoryService.getDeployedProcessDefinition(
                                    executionEntity.getProcessDefinitionId());
                    if (def != null && def.isGraphicalNotationDefined()) {
                        final BpmnModel model = repositoryService.getBpmnModel(def.getId());
                        try (final InputStream is = ProcessDiagramGenerator.generateDiagram(model, "png",
                                runtimeService.getActiveActivityIds(resourceId))) {
                            final byte[] data = new byte[is.available()];
                            is.read(data);
                            content.put(ActivitiConstants.ACTIVITI_DIAGRAM, Base64.encode(data));
                        }
                    }
                }

                handler.handleResult(new Resource(instance.getId(), null, content));
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    /**
     * Retrieves all tasks associated with a processId.
     *
     * @param processId process instance id
     * @return Map containing all tasks associated with the processId
     */
    private List<Map<String, Object>> getTasksForProcess(String processId) {
        HistoricTaskInstanceQuery query = processEngine.getHistoryService().createHistoricTaskInstanceQuery();
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (HistoricTaskInstance taskInstance : query.processInstanceId(processId).list()) {
            tasks.add(MAPPER.convertValue(taskInstance, Map.class));
        }
        return tasks;
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupportedOnInstance(request));
    }

    /**
     * Process the query parameters of the request and set it on the ProcessInstanceQuery.
     *
     * @param query Query to update
     * @param request incoming request
     */
    private void setProcessInstanceParams(HistoricProcessInstanceQuery query, QueryRequest request) {

        for (Map.Entry<String, String> param : request.getAdditionalParameters().entrySet()) {
            switch (param.getKey()) {
                case ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID:
                    query.processDefinitionId(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_PROCESSDEFINITIONKEY:
                    query.processDefinitionKey(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_PROCESSINSTANCEBUSINESSKEY:
                    query.processInstanceBusinessKey(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_PROCESSINSTANCEID:
                    query.processInstanceId(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_SUPERPROCESSINSTANCEID:
                    query.superProcessInstanceId(param.getValue());
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
                case ActivitiConstants.ACTIVITI_INVOLVEDUSERID:
                    query.involvedUser(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_STARTUSERID:
                    query.startedBy(param.getValue());
                    break;
            }
        }

        Map<String, String> wfParams = ActivitiUtil.fetchVarParams(request);
        Iterator<Map.Entry<String, String>> itWf = wfParams.entrySet().iterator();
        while (itWf.hasNext()) {
            Map.Entry<String, String> e = itWf.next();
            query.variableValueEquals(e.getKey(), e.getValue());
        }
    }

    /**
     *  Sets what the result set should be filtered by.
     *
     * @param query HisotricProcessInstanceQuery that needs to be modified for filtering
     * @param request incoming request
     * @throws NotSupportedException
     */
    private void setSortKeys(HistoricProcessInstanceQuery query, QueryRequest request) throws NotSupportedException {
        for (SortKey key : request.getSortKeys()) {
            if (key.getField() != null && !key.getField().isEmpty()) {
                switch (key.getField().toString().substring(1)) { // remove leading JsonPointer slash
                    case ActivitiConstants.ACTIVITI_PROCESSINSTANCEID:
                        query.orderByProcessInstanceId();
                        break;
                    case ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID:
                        query.orderByProcessDefinitionId();
                        break;
                    case ActivitiConstants.ACTIVITI_PROCESSINSTANCEBUSINESSKEY:
                        query.orderByProcessInstanceBusinessKey();
                        break;
                    case ActivitiConstants.ACTIVITI_STARTTIME:
                        query.orderByProcessInstanceStartTime();
                        break;
                    case ActivitiConstants.ACTIVITI_ENDTIME:
                        query.orderByProcessInstanceEndTime();
                        break;
                    case ActivitiConstants.ACTIVITI_DURATIONINMILLIS:
                        query.orderByProcessInstanceDuration();
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

    /**
     * Returns the name of the process instance definition.
     *
     * @param process HistoricProcessInstance of a process definition.
     * @return String name of teh process definition.
     */
    private String getProcessDefName(HistoricProcessInstance process) {
        return processEngine.getRepositoryService().getProcessDefinition(process.getProcessDefinitionId()).getName();
    }
}
