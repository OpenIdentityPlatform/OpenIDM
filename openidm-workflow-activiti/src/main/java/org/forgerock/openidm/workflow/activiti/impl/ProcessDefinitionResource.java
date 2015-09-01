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

import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.util.ResourceUtil.notSupportedOnCollection;
import static org.forgerock.openidm.util.ResourceUtil.notSupportedOnInstance;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.forgerock.http.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.workflow.activiti.ActivitiConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.FormService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.DefaultStartFormHandler;
import org.activiti.engine.impl.form.EnumFormType;
import org.activiti.engine.impl.form.FormPropertyHandler;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.apache.ibatis.exceptions.PersistenceException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.workflow.activiti.impl.mixin.DateFormTypeMixIn;
import org.forgerock.openidm.workflow.activiti.impl.mixin.EnumFormTypeMixIn;
import org.forgerock.openidm.workflow.activiti.impl.mixin.ProcessDefinitionMixIn;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.promise.Promise;

/**
 * Resource implementation of ProcessDefinition related Activiti operations
 *
 */
public class ProcessDefinitionResource implements CollectionResourceProvider {

    private final static ObjectMapper mapper;
    private ProcessEngine processEngine;

    static {
        mapper = new ObjectMapper();
        mapper.addMixIn(ProcessDefinitionEntity.class, ProcessDefinitionMixIn.class);
        mapper.addMixIn(EnumFormType.class, EnumFormTypeMixIn.class);
        mapper.addMixIn(DateFormType.class, DateFormTypeMixIn.class);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public ProcessDefinitionResource(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return notSupportedOnCollection(request).asPromise();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(
            Context context, String resourceId, ActionRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(
            Context context, String resourceId, DeleteRequest request) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            ProcessDefinitionEntity processDefinition =
                    (ProcessDefinitionEntity) processEngine.getRepositoryService().getProcessDefinition(resourceId);
            if (processDefinition != null) {
                ResourceResponse r = convertInstance(processDefinition, request.getFields());
                processEngine.getRepositoryService().deleteDeployment(processDefinition.getDeploymentId(), false);
                return r.asPromise();
            } else {
                throw new NotFoundException();
            }
        } catch (ActivitiObjectNotFoundException ex) {
            return new NotFoundException(ex.getMessage()).asPromise();
        } catch (PersistenceException ex) {
            return new ConflictException("The process definition has running instances, can not be deleted")
                    .asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception ex) {
            return new InternalServerErrorException(ex.getMessage()).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(
            Context context, String resourceId, PatchRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection
            (Context context, QueryRequest request, QueryResourceHandler handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            if (ActivitiConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                List<ProcessDefinition> definitionList =
                        processEngine.getRepositoryService().createProcessDefinitionQuery().list();
                if (definitionList != null && definitionList.size() > 0) {
                    for (ProcessDefinition processDefinition : definitionList) {
                        Map value = mapper.convertValue(processDefinition, HashMap.class);
                        ResourceResponse r = newResourceResponse(processDefinition.getId(), null, new JsonValue(value));
                        handler.handleResource(r);
                    }
                }
                return newQueryResponse().asPromise();
            } else if (ActivitiConstants.QUERY_FILTERED.equals(request.getQueryId())) {
                ProcessDefinitionQuery query = processEngine.getRepositoryService().createProcessDefinitionQuery();
                setProcessDefinitionParams(query, request);
                List<ProcessDefinition> list = query.list();
                for (ProcessDefinition processDefinition : list) {
                    Map value = mapper.convertValue(processDefinition, HashMap.class);
                    ResourceResponse r = newResourceResponse(processDefinition.getId(), null, new JsonValue(value));
                    handler.handleResource(r);
                }
                return newQueryResponse().asPromise();
            } else {
                throw new BadRequestException("Unknown query-id");
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception ex) {
            return new InternalServerErrorException(ex.getMessage(), ex).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(
            Context context, String resourceId, ReadRequest request) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            ProcessDefinitionEntity def =
                    (ProcessDefinitionEntity) ((RepositoryServiceImpl) processEngine.getRepositoryService())
                            .getDeployedProcessDefinition(resourceId);
            return convertInstance(def, request.getFields()).asPromise();
        } catch (ActivitiObjectNotFoundException ex) {
            return new NotFoundException(ex.getMessage()).asPromise();
        } catch (Exception ex) {
            return new InternalServerErrorException(ex.getMessage(), ex).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(
            Context context, String resourceId, UpdateRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    /**
     * Process the query parameters of the request and set it on the
     * ProcessDefinitionQuery
     *
     * @param query Query to update
     * @param request incoming request
     */
    private void setProcessDefinitionParams(ProcessDefinitionQuery query, QueryRequest request) {
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
        query = processDefinitionResourceName == null ? query : query.processDefinitionResourceName(processDefinitionResourceName);
        String processDefinitionResourceNameLike = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_PROCESSDEFINITIONRESOURCENAME + ActivitiConstants.LIKE);
        query = processDefinitionResourceNameLike == null ? query : query.processDefinitionResourceNameLike(processDefinitionResourceNameLike);
        String processDefinitionVersion = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_VERSION);
        query = processDefinitionVersion == null ? query : query.processDefinitionVersion(Integer.getInteger(processDefinitionVersion));
    }

    /**
     * Return the list of FormProperty-related data
     *
     * @param handlers list of handlers to process
     * @return propertyList list of form properties
     */
    private List<Map<String, Object>> getFormHandlerData(List<FormPropertyHandler> handlers) {
        final List<Map<String, Object>> propertyList = new ArrayList<>();
        for (FormPropertyHandler h : handlers) {
            Map<String, Object> entry = new HashMap<>();
            entry.put(ActivitiConstants.ID, h.getId());
            entry.put(ActivitiConstants.FORMPROPERTY_DEFAULTEXPRESSION, h.getDefaultExpression());
            entry.put(ActivitiConstants.FORMPROPERTY_VARIABLEEXPRESSION, h.getVariableExpression());
            entry.put(ActivitiConstants.FORMPROPERTY_VARIABLENAME, h.getVariableName());
            entry.put(ActivitiConstants.ACTIVITI_NAME, h.getName());
            Map<String, Object> type = new HashMap<>(3);
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
        return propertyList;
    }
    
    /**
     * Converts a ProcessDefinitionEntity to Resource object
     *
     * @param processDefinition entity to be converted
     * @param fields the list of requested fields
     * @return converted process definition
     * @throws IOException 
     */
    private ResourceResponse convertInstance(ProcessDefinitionEntity processDefinition, List<JsonPointer> fields)
            throws IOException {
        final String deploymentId = processDefinition.getDeploymentId();
        final JsonValue content = new JsonValue(mapper.convertValue(processDefinition, Map.class));

        // add form data
        if (processDefinition.hasStartFormKey()) {
            FormService formService = processEngine.getFormService();
            StartFormData startFormData = formService.getStartFormData(processDefinition.getId());
            content.put(ActivitiConstants.ACTIVITI_FORMRESOURCEKEY, startFormData.getFormKey());
            try (final InputStream startForm = processEngine.getRepositoryService().getResourceAsStream(
                    deploymentId, startFormData.getFormKey());
                 final Reader reader = new InputStreamReader(startForm)) {

                Scanner s = new Scanner(reader).useDelimiter("\\A");
                String formTemplate = s.hasNext() ? s.next() : "";
                content.put(ActivitiConstants.ACTIVITI_FORMGENERATIONTEMPLATE, formTemplate);
            }
        }

        // add diagram if requested and exists
        if (fields.contains(ActivitiConstants.ACTIVITI_DIAGRAM)
                && processDefinition.getDiagramResourceName() != null) {
            try (final InputStream is = processEngine.getRepositoryService().getResourceAsStream(
                    deploymentId, processDefinition.getDiagramResourceName())) {
                final byte[] data = new byte[is.available()];
                is.read(data);
                content.put(ActivitiConstants.ACTIVITI_DIAGRAM, Base64.encode(data));
            }
        }
        DefaultStartFormHandler startFormHandler = (DefaultStartFormHandler) processDefinition.getStartFormHandler();
        content.put(ActivitiConstants.FORMPROPERTIES, getFormHandlerData(startFormHandler.getFormPropertyHandlers()));
        return newResourceResponse(processDefinition.getId(), null, content);
    }
}
