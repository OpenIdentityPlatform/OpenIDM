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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.servlet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
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
 * Wraps a MaintenanceFilter and a PassthroughFilter to allow swapping them at runtime in a FilterChain.
 */
@Component(name = MaintenanceFilterWrapper.PID, policy = ConfigurationPolicy.IGNORE,
        configurationFactory = false, immediate = true)
@Service(value = { Filter.class, MaintenanceFilterWrapper.class })
public class MaintenanceFilterWrapper implements Filter {
    public static final String PID = "org.forgerock.openidm.maintenancemodefilter";

    private Boolean enabled = false;
    private final Filter passthroughFilter = new PassthroughFilter();
    private final Filter maintenanceFilter = new MaintenanceFilter();

    /**
     * Used when entering maintenance mode, prevents write operations.
     */
    public void enable() {
        enabled = true;
    }

    /**
     * Used when exiting maintenance mode, passes through all requests.
     */
    public void disable() {
        enabled = false;
    }

    private Filter getFilter() {
        if (enabled) {
            return maintenanceFilter;
        }
        return passthroughFilter;
    }

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest actionRequest,
            RequestHandler requestHandler) {
        return getFilter().filterAction(context, actionRequest, requestHandler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest createRequest,
            RequestHandler requestHandler) {
        return getFilter().filterCreate(context, createRequest, requestHandler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest deleteRequest,
            RequestHandler requestHandler) {
        return getFilter().filterDelete(context, deleteRequest, requestHandler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest patchRequest,
            RequestHandler requestHandler) {
        return getFilter().filterPatch(context, patchRequest, requestHandler);
    }

    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest queryRequest,
            QueryResourceHandler queryResourceHandler, RequestHandler requestHandler) {
        return getFilter().filterQuery(context, queryRequest, queryResourceHandler, requestHandler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest readRequest,
            RequestHandler requestHandler) {
        return getFilter().filterRead(context, readRequest, requestHandler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest updateRequest,
            RequestHandler requestHandler) {
        return getFilter().filterUpdate(context, updateRequest, requestHandler);
    }
}
