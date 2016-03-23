/**
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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openidm.mocks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;

/**
 * Mocks a RequestHandler that can be used in the Rep, Router, and CSV Audit Loggers
 */
public class MockRequestHandler implements RequestHandler {

    /**
     * Stores the list of requests received.
     */
    private final List<Request> requests = new ArrayList<>();

    /**
     * Stores the resources to be returned by the handlers. The test method should set the expected Resource to return.
     */
    private final List<ResourceResponse> resources = new ArrayList<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        requests.add(request);
        return newActionResponse(json(object())).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        requests.add(request);
        return newResourceResponse(request.getNewResourceId(), "1", request.getContent()).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        requests.add(request);
        return newResourceResponse("", "", json(object())).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        requests.add(request);
        return newResourceResponse("", "", json(object())).asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        requests.add(request);
        assertThat(resources.size()).isNotEqualTo(0);
        for (final ResourceResponse resource : resources) {
            handler.handleResource(resource);
        }
        return newQueryResponse().asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        requests.add(request);
        assertThat(resources.size()).isNotEqualTo(0);
        ResourceResponse returnResource = null;
        String requestID = getRequestID(request);
        for (ResourceResponse resource : resources) {
            if (resource.getId().equals(requestID)) {
                returnResource = resource;
                break;
            }
        }
        assertThat(returnResource).isNotNull();
        return returnResource.asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        requests.add(request);
        return newResourceResponse("", "", json(object())).asPromise();
    }

    public void addResource(final ResourceResponse resource) {
        this.resources.add(resource);
    }

    public void setResources(final List<ResourceResponse> resources) {
        this.resources.addAll(resources);
    }

    public final List<ResourceResponse> getResources() {
        return resources;
    }

    public final List<Request> getRequests() {
        return requests;
    }

    private String getRequestID(final Request request) {
        final JsonPointer jsonPointer = new JsonPointer(request.getResourcePath());
        return jsonPointer.leaf();
    }
}
