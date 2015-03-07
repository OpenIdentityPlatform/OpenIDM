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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.audit.mocks;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Mocks a RequestHandler that can be used in the Rep, Router, and CSV Audit Loggers
 */
public class MockRequestHandler implements RequestHandler {

    /**
     * Stores the list of requests received.
     */
    private final List<Request> requests = new ArrayList<Request>();

    /**
     * Stores the resources to be returned by the handlers. The test method should set the expected Resource to return.
     */
    private final List<Resource> resources = new ArrayList<Resource>();

    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        requests.add(request);
        handler.handleResult(new JsonValue(new HashMap<String,String>()));
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        requests.add(request);
        handler.handleResult(new Resource(request.getNewResourceId(), "1", request.getContent()));
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        requests.add(request);
        handler.handleResult(new Resource("","",new JsonValue(new HashMap<String,String>())));
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        requests.add(request);
        handler.handleResult(new Resource("","",new JsonValue(new HashMap<String,String>())));
    }

    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        requests.add(request);
        for (final Resource resource : resources) {
            handler.handleResource(resource);
        }
        handler.handleResult(new QueryResult());
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        requests.add(request);
        assertThat(resources.size() != 0);
        Resource returnResource = null;
        String requestID = getRequestID(request);
        for (Resource resource : resources) {
            if (resource.getId().equals(requestID)) {
                returnResource = resource;
                break;
            }
        }
        assertThat(returnResource != null);
        handler.handleResult(returnResource);
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        requests.add(request);
        handler.handleResult(new Resource("","",new JsonValue(new HashMap<String,String>())));
    }

    public void addResource(final Resource resource) {
        this.resources.add(resource);
    }

    public void setResources(final List<Resource> resources) {
        this.resources.addAll(resources);
    }

    public final List<Resource> getResources() {
        return resources;
    }

    public final List<Request> getRequests() {
        return requests;
    }

    private String getRequestID(final Request request) {
        final JsonPointer jsonPointer = new JsonPointer(request.getResourceName());
        return jsonPointer.leaf();
    }
}
