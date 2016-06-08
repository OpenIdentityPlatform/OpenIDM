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
 * Copyright 2016 ForgeRock AS.
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
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * A filter that returns a Service Unavailable message for all requests.
 */
public class ServiceUnavailableFilter implements Filter {
    private final ResourceException serviceUnavailable;

    public ServiceUnavailableFilter(String message) {
        serviceUnavailable = new ServiceUnavailableException(message);
    }

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(
            Context context, ActionRequest actionRequest, RequestHandler requestHandler) {
        return serviceUnavailable.asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(
            Context context, CreateRequest createRequest, RequestHandler requestHandler) {
        return serviceUnavailable.asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(
            Context context, DeleteRequest deleteRequest, RequestHandler requestHandler) {
        return serviceUnavailable.asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(
            Context context, PatchRequest patchRequest, RequestHandler requestHandler) {
        return serviceUnavailable.asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(
            Context context, QueryRequest queryRequest, QueryResourceHandler queryResourceHandler, RequestHandler requestHandler) {
        return serviceUnavailable.asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(
            Context context, ReadRequest readRequest, RequestHandler requestHandler) {
        return serviceUnavailable.asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(
            Context context, UpdateRequest updateRequest, RequestHandler requestHandler) {
        return serviceUnavailable.asPromise();
    }
}
