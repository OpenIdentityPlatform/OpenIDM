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

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.forgerock.services.context.Context;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
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
import org.forgerock.services.context.SecurityContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.workflow.activiti.ActivitiConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.FormService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.DefaultTaskFormHandler;
import org.activiti.engine.impl.form.EnumFormType;
import org.activiti.engine.impl.form.FormPropertyHandler;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.task.TaskDefinition;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.workflow.activiti.impl.mixin.DateFormTypeMixIn;
import org.forgerock.openidm.workflow.activiti.impl.mixin.EnumFormTypeMixIn;
import org.forgerock.openidm.workflow.activiti.impl.mixin.FormPropertyHandlerMixIn;
import org.forgerock.openidm.workflow.activiti.impl.mixin.TaskDefinitionMixIn;
import org.forgerock.util.promise.Promise;

/**
 * Resource implementation of TaskDefinition related Activiti operations
 *
 */
public class TaskDefinitionResource implements CollectionResourceProvider {

    private final static ObjectMapper mapper;
    private ProcessEngine processEngine;

    static {
        mapper = new ObjectMapper();
        mapper.addMixIn(TaskDefinition.class, TaskDefinitionMixIn.class);
        mapper.addMixIn(EnumFormType.class, EnumFormTypeMixIn.class);
        mapper.addMixIn(DateFormType.class, DateFormTypeMixIn.class);
        mapper.addMixIn(FormPropertyHandler.class, FormPropertyHandlerMixIn.class);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public TaskDefinitionResource(ProcessEngine processEngine) {
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
        return notSupportedOnInstance(request).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId, DeleteRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId, PatchRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request, QueryResourceHandler handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            if (ActivitiConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                String processDefinitionId = ((UriRouterContext) context).getUriTemplateVariables().get("procdefid");
                ProcessDefinitionEntity procdef = (ProcessDefinitionEntity) ((RepositoryServiceImpl) processEngine.getRepositoryService()).getDeployedProcessDefinition(processDefinitionId);
                Map<String, TaskDefinition> taskdefinitions = procdef.getTaskDefinitions();
                for (TaskDefinition taskDefinition : taskdefinitions.values()) {
                    DefaultTaskFormHandler taskFormHandler = (DefaultTaskFormHandler) taskDefinition.getTaskFormHandler();
                    Map value = mapper.convertValue(taskDefinition, HashMap.class);
                    ResourceResponse r = newResourceResponse(taskDefinition.getKey(), null, new JsonValue(value));
                    r.getContent().add(ActivitiConstants.ACTIVITI_FORMRESOURCEKEY, taskFormHandler.getFormKey());
                    handler.handleResource(r);
                }
                return newQueryResponse().asPromise();
            } else {
                return new BadRequestException("Unknown query-id").asPromise();
            }
        } catch (IllegalArgumentException ex) {
            return new InternalServerErrorException(ex.getMessage(), ex).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId, ReadRequest request) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            String processDefinitionId = ((UriRouterContext) context).getUriTemplateVariables().get("procdefid");
            ProcessDefinitionEntity procdef = (ProcessDefinitionEntity) ((RepositoryServiceImpl) processEngine.getRepositoryService()).getDeployedProcessDefinition(processDefinitionId);
            TaskDefinition taskDefinition = procdef.getTaskDefinitions().get(resourceId);
            if (taskDefinition != null) {
                Map value = mapper.convertValue(taskDefinition, HashMap.class);
                ResourceResponse r = newResourceResponse(taskDefinition.getKey(), null, new JsonValue(value));
                FormService formService = processEngine.getFormService();
                String taskFormKey = formService.getTaskFormKey(processDefinitionId, resourceId);
                if (taskFormKey != null) {
                    r.getContent().add(ActivitiConstants.ACTIVITI_FORMRESOURCEKEY, taskFormKey);
                    ByteArrayInputStream startForm = (ByteArrayInputStream) ((RepositoryServiceImpl) processEngine.getRepositoryService()).getResourceAsStream(procdef.getDeploymentId(), taskFormKey);
                    Reader reader = new InputStreamReader(startForm);
                    try {
                        Scanner s = new Scanner(reader).useDelimiter("\\A");
                        String formTemplate = s.hasNext() ? s.next() : "";
                        r.getContent().add(ActivitiConstants.ACTIVITI_FORMGENERATIONTEMPLATE, formTemplate);
                    } finally {
                        reader.close();
                    }
                }
                return r.asPromise();
            } else {
                throw new NotFoundException("Task definition for " + resourceId + " was not found");
            }
        } catch (ResourceException ex) {
            return ex.asPromise();
        } catch (ActivitiObjectNotFoundException ex) {
            return new NotFoundException(ex.getMessage()).asPromise();
        } catch (IllegalArgumentException ex) {
            return new InternalServerErrorException(ex.getMessage(), ex).asPromise();
        } catch (Exception ex) {
            return new InternalServerErrorException(ex.getMessage(), ex).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId, UpdateRequest request) {
        return notSupportedOnInstance(request).asPromise();
    }
}
