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
import java.util.Map;
import org.activiti.engine.ProcessEngine;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;

/**
 * Implementation of the Activiti Engine Resource
 *
 * @author orsolyamebold
 */
public class ActivitiResource implements RequestHandler {

    private Map<String, CollectionResourceProvider> resources = new HashMap<String, CollectionResourceProvider>(3);

    public ActivitiResource(ProcessEngine engine) {
        resources.put("processdefinition", new ProcessDefinitionResource(engine));
        resources.put("processinstance", new ProcessInstanceResource(engine));
        resources.put("taskinstance", new TaskInstanceResource(engine));
    }

    public void setProcessEngine(ProcessEngine engine) {
        resources.put("processdefinition", new ProcessDefinitionResource(engine));
        resources.put("processinstance", new ProcessInstanceResource(engine));
        resources.put("taskinstance", new TaskInstanceResource(engine));
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            CollectionResourceProvider resource = resources.get(((RouterContext) context).getUriTemplateVariables().get("activitiobj"));
            if (resource == null) {
                handler.handleError(new NotSupportedException("Action on " + ((RouterContext) context).getUriTemplateVariables().get("activitiobj") + " not supported"));
            } else {
                resource.actionInstance(context, ((RouterContext) context).getUriTemplateVariables().get("objid"), request, handler);
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        try {
            CollectionResourceProvider resource = resources.get(((RouterContext) context).getUriTemplateVariables().get("activitiobj"));
            if (resource == null) {
                handler.handleError(new NotSupportedException("Create on " + ((RouterContext) context).getUriTemplateVariables().get("activitiobj") + " not supported"));
            } else {
                resource.createInstance(context, request, handler);
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        try {
            CollectionResourceProvider resource = resources.get(((RouterContext) context).getUriTemplateVariables().get("activitiobj"));
            if (resource == null) {
                handler.handleError(new NotSupportedException("Delete on " + ((RouterContext) context).getUriTemplateVariables().get("activitiobj") + " not supported"));
            } else {
                resource.deleteInstance(context, ((RouterContext) context).getUriTemplateVariables().get("objid"), request, handler);
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Patch on ActivitiResource not supported yet."));
    }

    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            CollectionResourceProvider resource = resources.get(((RouterContext) context).getUriTemplateVariables().get("activitiobj"));
            if (resource == null) {
                handler.handleError(new NotSupportedException("Query on " + ((RouterContext) context).getUriTemplateVariables().get("activitiobj") + " not supported"));
            } else {
                resource.queryCollection(context, request, handler);
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            CollectionResourceProvider resource = resources.get(((RouterContext) context).getUriTemplateVariables().get("activitiobj"));
            if (resource == null) {
                handler.handleError(new NotSupportedException("Read on " + ((RouterContext) context).getUriTemplateVariables().get("activitiobj") + " not supported"));
            } else {
                resource.readInstance(context, ((RouterContext) context).getUriTemplateVariables().get("objid"), request, handler);
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        try {
            CollectionResourceProvider resource = resources.get(((RouterContext) context).getUriTemplateVariables().get("activitiobj"));
            if (resource == null) {
                handler.handleError(new NotSupportedException("Update on " + ((RouterContext) context).getUriTemplateVariables().get("activitiobj") + " not supported"));
            } else {
                resource.updateInstance(context, ((RouterContext) context).getUriTemplateVariables().get("objid"), request, handler);
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }
}