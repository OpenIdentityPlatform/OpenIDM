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
package org.forgerock.openidm.maintenance.impl;

import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterCondition;
import org.forgerock.json.resource.Filters;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.filter.PassthroughFilter;
import org.forgerock.openidm.filter.MutableFilterDecorator;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;

/**
 * Maintenance filter to disable CREST write operations while in maintenance mode.
 */
@Component(name = MaintenanceFilter.PID, policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service({ Filter.class, MaintenanceFilter.class })
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Product Maintenance Filter")
})
public class MaintenanceFilter extends MutableFilterDecorator {

    static final String PID = "org.forgerock.openidm.maintenance.filter";

    /** maintenance filter that prevents modification via CREST endpoints during maintenance mode */
    private static Filter MAINTENANCE_FILTER = Filters.conditionalFilter(
            new FilterCondition() {
                // pass requests on audit or maintenance endpoints
                private final Pattern allowedEndpoints = Pattern.compile("^((repo\\/)?audit|maintenance)(/?.*|$)");
                @Override
                public boolean matches(Context context, Request request) {
                    /* The semantic of "matches" is "if this condition is true, then the filter will be entered".
                     *
                     * We want to enter the filter (and disallow writes) if all of the following are TRUE
                     *  a) the request does not matches the allowedEndpoints regex
                     *  b) the request does not have an UpdateContext
                     *
                     * That is, while in maintenance mode,
                     *  - requests on audit,
                     *  - requests on maintenance
                     *  - requests with an UpdateContext
                     * are "passed through" by *avoiding* this filter, which would deny them.  All other requests
                     * enter this filter and are denied.
                     */
                    return !allowedEndpoints.matcher(request.getResourcePath()).matches()
                            && !context.containsContext(UpdateContext.class);
                }
            },
            new Filter() {
                @Override
                public Promise<ActionResponse, ResourceException> filterAction(
                        Context context, ActionRequest actionRequest, RequestHandler requestHandler) {
                    return new ServiceUnavailableException(
                            "Unable to perform action on " + actionRequest.getResourcePath() + " in maintenance mode.")
                            .asPromise();
                }

                @Override
                public Promise<ResourceResponse, ResourceException> filterCreate(
                        Context context, CreateRequest createRequest, RequestHandler requestHandler) {
                    return new ServiceUnavailableException(
                            "Unable to create on " + createRequest.getResourcePath() + " in maintenance mode.")
                            .asPromise();
                }

                @Override
                public Promise<ResourceResponse, ResourceException> filterDelete(
                        Context context, DeleteRequest deleteRequest, RequestHandler requestHandler) {
                    return new ServiceUnavailableException(
                            "Unable to delete on " + deleteRequest.getResourcePath() + " in maintenance mode.")
                            .asPromise();
                }

                @Override
                public Promise<ResourceResponse, ResourceException> filterPatch(
                        Context context, PatchRequest patchRequest, RequestHandler requestHandler) {
                    return new ServiceUnavailableException(
                            "Unable to patch on " + patchRequest.getResourcePath() + " in maintenance mode.")
                            .asPromise();
                }

                @Override
                public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest queryRequest,
                        QueryResourceHandler handler, RequestHandler requestHandler) {
                    return requestHandler.handleQuery(context, queryRequest, handler);
                }

                @Override
                public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest readRequest,
                        RequestHandler requestHandler) {
                    return requestHandler.handleRead(context, readRequest);
                }

                @Override
                public Promise<ResourceResponse, ResourceException> filterUpdate(
                        Context context, UpdateRequest updateRequest, RequestHandler requestHandler) {
                    return new ServiceUnavailableException(
                            "Unable to update on " + updateRequest.getResourcePath() + " in maintenance mode.")
                            .asPromise();
                }
            });

    void enableMaintenanceMode() {
        setDelegate(MAINTENANCE_FILTER);
    }

    void disableMaintenanceMode() {
        setDelegate(PassthroughFilter.PASSTHROUGH_FILTER);
    }
}
