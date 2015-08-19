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

import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.util.promise.Promises.newExceptionPromise;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.forgerock.http.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Implementation of the Activiti Engine Resource
 *
 */
public class ActivitiResource implements RequestHandler {

    private Router resources = new Router();
    private Router subResources = new Router();

    public ActivitiResource(ProcessEngine engine) {
        resources.addRoute(uriTemplate("/processdefinition"), new ProcessDefinitionResource(engine));
        resources.addRoute(uriTemplate("/processdefinition/{procdefid}/taskdefinition"),
                new TaskDefinitionResource(engine));
        resources.addRoute(uriTemplate("/processinstance"), new ProcessInstanceResource(engine,
                new Function<ProcessEngine, HistoricProcessInstanceQuery, NeverThrowsException>() {
                    public HistoricProcessInstanceQuery apply(ProcessEngine engine) {
                        return engine.getHistoryService().createHistoricProcessInstanceQuery().unfinished();
                     }
                }));
        resources.addRoute(uriTemplate("/processinstance/history"), new ProcessInstanceResource(engine,
                new Function<ProcessEngine, HistoricProcessInstanceQuery, NeverThrowsException>() {
                    public HistoricProcessInstanceQuery apply(ProcessEngine engine) {
                        return engine.getHistoryService().createHistoricProcessInstanceQuery();
                    }
                }));
        resources.addRoute(uriTemplate("/taskinstance"), new TaskInstanceResource(engine));
        resources.addRoute(uriTemplate("/taskinstance/history"), new TaskInstanceHistoryResource(engine));
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        try {
            return resources.handleAction(context, request);
        } catch (Exception ex) {
            return newExceptionPromise(ResourceUtil.adapt(ex));
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        try {
            return resources.handleCreate(context, request);
        } catch (Exception ex) {
            return newExceptionPromise(ResourceUtil.adapt(ex));
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        try {
            return resources.handleDelete(context, request);
        } catch (Exception ex) {
            return newExceptionPromise(ResourceUtil.adapt(ex));
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return newExceptionPromise(ResourceUtil.notSupported(request));
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(
            Context context, QueryRequest request, QueryResourceHandler handler) {
        try {
            return resources.handleQuery(context, request, handler);
        } catch (Exception ex) {
            return newExceptionPromise(ResourceUtil.adapt(ex));
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            return resources.handleRead(context, request);
        } catch (Exception ex) {
            return newExceptionPromise(ResourceUtil.adapt(ex));
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        try {
            return resources.handleUpdate(context, request);
        } catch (Exception ex) {
            return newExceptionPromise(ResourceUtil.adapt(ex));
        }
    }
}
