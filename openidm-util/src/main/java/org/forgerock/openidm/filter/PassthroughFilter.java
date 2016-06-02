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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openidm.filter;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Passthrough filter forwards all requests to the next handler unchanged.
 */
public class PassthroughFilter implements Filter {

    /** Singleton instance of the PassthroughFilter */
    public static final Filter PASSTHROUGH_FILTER = new PassthroughFilter();

    private PassthroughFilter() {
        // prevent direct instantiation - use the singleton constant instead
    }

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest actionRequest,
            RequestHandler handler) {
        return handler.handleAction(context, actionRequest);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest createRequest,
            RequestHandler handler) {
        return handler.handleCreate(context, createRequest);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest deleteRequest,
            RequestHandler handler) {
        return handler.handleDelete(context, deleteRequest);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest patchRequest,
            RequestHandler handler) {
        return handler.handlePatch(context, patchRequest);
    }

    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest queryRequest,
            QueryResourceHandler queryResourceHandler, RequestHandler handler) {
        return handler.handleQuery(context, queryRequest, queryResourceHandler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest readRequest,
            RequestHandler handler) {
        return handler.handleRead(context, readRequest);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest updateRequest,
            RequestHandler handler) {
        return handler.handleUpdate(context, updateRequest);
    }
}
