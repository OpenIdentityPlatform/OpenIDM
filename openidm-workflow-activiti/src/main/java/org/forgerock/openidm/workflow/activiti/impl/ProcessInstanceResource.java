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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.openidm.workflow.activiti.impl.mixin.HistoricProcessInstanceMixIn;

/**
 * Resource implementation of ProcessInstance related Activiti operations
 * @author orsolyamebold
 */
public class ProcessInstanceResource implements CollectionResourceProvider {

    private final static ObjectMapper mapper;
    private ProcessEngine processEngine;
    private PersistenceConfig persistenceConfig;

    static {
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().addMixInAnnotations(HistoricProcessInstanceEntity.class, HistoricProcessInstanceMixIn.class);
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public ProcessInstanceResource(ProcessEngine processEngine, PersistenceConfig config) {
        this.processEngine = processEngine;
        this.persistenceConfig = config;
    }

    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("ActionCollection on ProcessInstanceResource not supported."));
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("ActionInstance on ProcessInstanceResource not supported."));
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        try {
            String key = ActivitiUtil.removeKeyFromRequest(request);
            String businessKey = ActivitiUtil.removeBusinessKeyFromRequest(request);
            String processDefinitionId = ActivitiUtil.removeProcessDefinitionIdFromRequest(request);
            Map<String, Object> variables = ActivitiUtil.getRequestBodyFromRequest(request);
            variables.put(ActivitiConstants.OPENIDM_CONTEXT, ServerContext.saveToJson(context, persistenceConfig));
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
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request, ResultHandler<Resource> handler) {
        try {
            if (processEngine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(resourceId).singleResult() != null) {
                processEngine.getRuntimeService().deleteProcessInstance(resourceId, "Deleted by Openidm");
                Map<String, String> result = new HashMap<String, String>(1);
                result.put("Process instance deleted", resourceId);
                handler.handleResult(new Resource(resourceId, null, new JsonValue(result)));
            } else {
                handler.handleError(new NotFoundException());
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("PatchInstance on ProcessInstanceResource not supported."));
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            if (ActivitiConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                HistoricProcessInstanceQuery query = processEngine.getHistoryService().createHistoricProcessInstanceQuery();
                query = query.unfinished();
                List<HistoricProcessInstance> list = query.list();
                for (HistoricProcessInstance i : list) {
                    Map value = mapper.convertValue(i, HashMap.class);
                    Resource r = new Resource(i.getId(), null, new JsonValue(value));
                    handler.handleResource(r);
                }
                handler.handleResult(new QueryResult());
            } else if (ActivitiConstants.QUERY_FILTERED.equals(request.getQueryId())) {
                ProcessInstanceQuery query = processEngine.getRuntimeService().createProcessInstanceQuery();
                setProcessInstanceParams(query, request);
                List<ProcessInstance> list = query.list();
                for (ProcessInstance processinstance : list) {
                    Map value = mapper.convertValue(processinstance, HashMap.class);
                    Resource r = new Resource(processinstance.getId(), null, new JsonValue(value));
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
            HistoricProcessInstance instance =
                    processEngine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(resourceId).singleResult();
            if (instance == null) {
                handler.handleError(new NotFoundException());
            } else {
                Map value = mapper.convertValue(instance, HashMap.class);
                Resource r = new Resource(instance.getId(), null, new JsonValue(value));
                handler.handleResult(r);
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("UpdateInstance on ProcessInstanceResource not supported."));
    }

    /**
     * Process the query parameters of the request and set it on the
     * ProcessInstanceQuery
     *
     * @param query Query to update
     * @param request incoming request
     */
    private void setProcessInstanceParams(ProcessInstanceQuery query, QueryRequest request) {
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

        Map<String, String> wfParams = ActivitiUtil.fetchVarParams(request);
        Iterator<Map.Entry<String, String>> itWf = wfParams.entrySet().iterator();
        while (itWf.hasNext()) {
            Map.Entry<String, String> e = itWf.next();
            query.variableValueEquals(e.getKey(), e.getValue());
        }
    }
}
