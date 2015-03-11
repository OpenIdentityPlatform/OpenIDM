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

import org.activiti.engine.ProcessEngine;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.openidm.util.ResourceUtil;

/**
 * Implementation of the Activiti Engine Resource
 *
 */
public class ActivitiResource implements RequestHandler {

    private Router resources = new Router();
    private Router subResources = new Router();

    public ActivitiResource(ProcessEngine engine, PersistenceConfig config) {
        resources.addRoute("/processdefinition", new ProcessDefinitionResource(engine));
        resources.addRoute("/processdefinition/{procdefid}/taskdefinition", new TaskDefinitionResource(engine));
        resources.addRoute("/processinstance", new ProcessInstanceResource(engine, config));
        resources.addRoute("/taskinstance", new TaskInstanceResource(engine));
    }

    public void setProcessEngine(ProcessEngine engine, PersistenceConfig config) {
        resources.addRoute("/processdefinition", new ProcessDefinitionResource(engine));
        resources.addRoute("/processdefinition/{procdefid}/taskdefinition", new TaskDefinitionResource(engine));
        resources.addRoute("/processinstance", new ProcessInstanceResource(engine, config));
        resources.addRoute("/taskinstance", new TaskInstanceResource(engine));
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            resources.handleAction(context, request, handler);
        } catch (Exception ex) {
            handler.handleError(ResourceUtil.adapt(ex));
        }
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        try {
            resources.handleCreate(context, request, handler);
        } catch (Exception ex) {
            handler.handleError(ResourceUtil.adapt(ex));
        }
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        try {
            resources.handleDelete(context, request, handler);
        } catch (Exception ex) {
            handler.handleError(ResourceUtil.adapt(ex));
        }
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            resources.handleQuery(context, request, handler);
        } catch (Exception ex) {
            handler.handleError(ResourceUtil.adapt(ex));
        }
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            resources.handleRead(context, request, handler);
        } catch (Exception ex) {
            handler.handleError(ResourceUtil.adapt(ex));
        }
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        try {
            resources.handleUpdate(context, request, handler);
        } catch (Exception ex) {
            handler.handleError(ResourceUtil.adapt(ex));
        }
    }
}
