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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openidm.servlet.internal;

import static org.forgerock.json.resource.Requests.copyOfCreateRequest;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.resource.AbstractConnectionWrapper;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.Filters;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Response;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.filter.PassthroughFilter;
import org.forgerock.openidm.filter.MutableFilterDecorator;
import org.forgerock.openidm.filter.ServiceUnavailableFilter;
import org.forgerock.openidm.router.RouterFilterRegistration;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ServletConnectionFactory responsible for providing Connections to routing requests initiated
 * from an external request on the api servlet.
 */
@Component(name = ServerConstants.EXTERNAL_ROUTER_SERVICE_PID, policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service({ ConnectionFactory.class, RouterFilterRegistration.class })
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Common REST Servlet Connection Factory")
})
public class ServletConnectionFactory implements ConnectionFactory, RouterFilterRegistration {

    /** Event name prefix for monitoring the router */
    private static final String EVENT_ROUTER_PREFIX = "openidm/internal/router/";

    /** Setup logging for the {@link org.forgerock.openidm.servlet.internal.ServletConnectionFactory}. */
    private static final Logger logger = LoggerFactory.getLogger(ServletConnectionFactory.class);

    /** Router Filter at head of chain while services are still being initialized. */
    private static final Filter SERVICE_UNAVAILABLE_FILTER = new ServiceUnavailableFilter("Service is starting");

    /** the Request Handler (Router) */
    @Reference(target = "(org.forgerock.openidm.router=*)")
    private RequestHandler requestHandler = null;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig = null;

    /**
     * We define 4 filters that are "statically" defined:
     * <ul>
     *     <li>startup filter - throws ServiceUnavailableException until configured router filters are loaded</li>
     *     <li>maintenance filter - toggled based on maintenance mode</li>
     *     <li>logging filter - always enabled, logs trace-level messages</li>
     *     <li>audit filter - enabled once AuditFilter is bound</li>
     * </ul>
     * These are via Java implementation and not sourced from router.json {@see RouterFilterChain}.
     */
    private static final int NUMBER_OF_STATIC_FILTERS = 4;

    /** A wrapper for the startup filter - begin with a service-unavailable filter */
    private final MutableFilterDecorator startupFilter = new MutableFilterDecorator(SERVICE_UNAVAILABLE_FILTER);

    /** A wrapper for the maintenance filter - populated when the MaintenanceFilter is bound */
    @Reference(name = "MaintenanceFilter", referenceInterface = Filter.class,
            target = "(service.pid=org.forgerock.openidm.maintenance.filter)",
            bind = "bindMaintenanceFilter", unbind = "unbindMaintenanceFilter",
            cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private final MutableFilterDecorator maintenanceFilter = new MutableFilterDecorator();

    /** A filter to emit trace messages on the request and response */
    private final Filter loggingFilter = newTraceLoggingFilter();

    /** A wrapper for the audit filter - populated when the AuditFilter is bound */
    @Reference(name = "AuditFilter", referenceInterface = Filter.class,
            target = "(service.pid=org.forgerock.openidm.audit.filter)",
            bind = "bindAuditFilter", unbind = "unbindAuditFilter",
            cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private final MutableFilterDecorator auditFilter = new MutableFilterDecorator();

    /** the constructed filter chain */
    private FilterChain filterChain;

    /** the created connection factory */
    protected ConnectionFactory connectionFactory;

    /**
     * Assign the maintenance filter delegate to the provided filter.
     *
     * @param filter the active maintenance filter
     */
    void bindMaintenanceFilter(Filter filter) {
        maintenanceFilter.setDelegate(filter);
    }

    /**
     * Remove the maintenance filter by settings its delegate to a passthrough filter.
     *
     * @param filter the maintenance filter
     */
    void unbindMaintenanceFilter(Filter filter) {
        maintenanceFilter.setDelegate(PassthroughFilter.PASSTHROUGH_FILTER);
    }

    /**
     * Assign the audit filter delegate to the provided filter.
     *
     * @param filter the active audit filter
     */
    void bindAuditFilter(Filter filter) {
        auditFilter.setDelegate(filter);
    }

    /**
     * Remove the audit filter by settings its delegate to a passthrough filter.
     *
     * @param filter the audit filter
     */
    void unbindAuditFilter(Filter filter) {
        auditFilter.setDelegate(PassthroughFilter.PASSTHROUGH_FILTER);
    }

    @Activate
    protected void activate(ComponentContext context) {
        logger.debug("Creating servlet router/connection factory");
        String factoryPid = enhancedConfig.getConfigurationFactoryPid(context);
        if (StringUtils.isNotBlank(factoryPid)) {
            throw new IllegalArgumentException("Factory configuration not allowed, must not have property: "
                    + ServerConstants.CONFIG_FACTORY_PID);
        }

        List<Filter> filters = new ArrayList<>(NUMBER_OF_STATIC_FILTERS);

        // static filters - order is important here
        filters.add(startupFilter);
        filters.add(maintenanceFilter);
        filters.add(loggingFilter);
        filters.add(Filters.conditionalFilter(Filters.matchResourcePath("^(?!.*(^audit/)).*$"), auditFilter));

        filterChain = new FilterChain(requestHandler, filters);
        connectionFactory = newWrappedInternalConnectionFactory(Resources.newInternalConnectionFactory(filterChain));

        logger.info("Servlet ConnectionFactory created.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
    }

    private ConnectionFactory newWrappedInternalConnectionFactory(final ConnectionFactory connectionFactory) {
        return new ConnectionFactory() {
            @Override
            public void close() {
                connectionFactory.close();
            }

            @Override
            public Connection getConnection() throws ResourceException {
                return new AbstractConnectionWrapper<Connection>(connectionFactory.getConnection()) {

                    @Override
                    public ResourceResponse create(Context context, CreateRequest request) throws ResourceException {
                        EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.create(context, copyOfCreateRequest(request));
                        } finally {
                            measure.end();
                        }
                    }
                    @Override
                    public Promise<ResourceResponse, ResourceException> createAsync(
                            Context context, CreateRequest request) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.createAsync(context, copyOfCreateRequest(request))
                                .thenAlways(new Runnable() {
                                    @Override
                                    public void run() {
                                        measure.end();
                                    }
                                });
                    }
                    @Override
                    public ResourceResponse read(Context context, ReadRequest request)
                            throws ResourceException {
                        EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.read(context, request);
                        } finally {
                            measure.end();
                        }
                    }
                    @Override
                    public Promise<ResourceResponse, ResourceException> readAsync(
                            Context context, ReadRequest request) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.readAsync(context, request)
                                .thenAlways(new Runnable() {
                                    @Override
                                    public void run() {
                                        measure.end();
                                    }
                                });
                    }
                    @Override
                    public ResourceResponse update(Context context, UpdateRequest request) throws ResourceException {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.update(context, request);
                        } finally {
                            measure.end();
                        }
                    }
                    @Override
                    public Promise<ResourceResponse, ResourceException> updateAsync(
                            Context context, UpdateRequest request) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.updateAsync(context, request)
                                .thenAlways(new Runnable() {
                                    @Override
                                    public void run() {
                                        measure.end();
                                    }
                                });
                    }
                    @Override
                    public ResourceResponse delete(Context context, DeleteRequest request) throws ResourceException {
                        EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.delete(context, request);
                        } finally {
                            measure.end();
                        }
                    }
                    @Override
                    public Promise<ResourceResponse, ResourceException> deleteAsync(
                            Context context, DeleteRequest request) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.deleteAsync(context, request)
                                .thenAlways(new Runnable() {
                                    @Override
                                    public void run() {
                                        measure.end();
                                    }
                                });
                    }
                    @Override
                    public ResourceResponse patch(Context context, PatchRequest request) throws ResourceException {
                        EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.patch(context, request);
                        } finally {
                            measure.end();
                        }
                    }
                    @Override
                    public Promise<ResourceResponse, ResourceException> patchAsync(
                            Context context, PatchRequest request) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.patchAsync(context, request)
                                .thenAlways(new Runnable() {
                                    @Override
                                    public void run() {
                                        measure.end();
                                    }
                                });
                    }
                    @Override
                    public ActionResponse action(Context context, ActionRequest request) throws ResourceException {
                        EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.action(context, request);
                        } finally {
                            measure.end();
                        }
                    }                    
                    @Override
                    public Promise<ActionResponse, ResourceException> actionAsync(
                            Context context, ActionRequest request) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.actionAsync(context, request)
                                .thenAlways(new Runnable() {
                                    @Override
                                    public void run() {
                                        measure.end();
                                    }
                                });
                    }
                    @Override
                    public QueryResponse query(Context context, QueryRequest request, QueryResourceHandler handler)
                            throws ResourceException {
                        EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.query(context, request, handler);
                        } finally {
                            measure.end();
                        }
                    }
                    @Override
                    public Promise<QueryResponse, ResourceException> queryAsync(
                            Context context, QueryRequest request, QueryResourceHandler handler) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.queryAsync(context, request, handler)
                                .thenAlways(new Runnable() {
                                    @Override
                                    public void run() {
                                        measure.end();
                                    }
                                });
                    }
                };
            }
            
            @Override
            public Promise<Connection, ResourceException> getConnectionAsync() {
                try {
                    return newResultPromise(getConnection());
                } catch (ResourceException e) {
                    return e.asPromise();
                }
            }
           
            /**
             * @param request the router request
             * @return an event name For monitoring purposes
             */
            private Name getRouterEventName(Request request) {
                RequestType requestType = request.getRequestType();
                String idContext;

                // For query and action group statistics by full URI
                // Create has only the component name in the getResourceName to start with
                if (RequestType.QUERY.equals(requestType) || RequestType.ACTION.equals(requestType) 
                        || RequestType.CREATE.equals(requestType)) {
                    idContext = request.getResourcePath();
                } else {
                    // For RUD, patch group statistics without the local resource identifier
                    idContext = request.getResourcePathObject()
                            .head(request.getResourcePathObject().size() - 1)
                            .toString();
                }

                String eventName = EVENT_ROUTER_PREFIX + idContext + "/" + requestType.toString().toLowerCase();
                
                return Name.get(eventName);
            }
        };
    }

    private static final ResultHandler<Response> LOGGING_RESULT_HANDLER =
            new ResultHandler<Response>() {
                @Override
                public void handleResult(Response response) {
                    logger.trace("Result: {}", response);
                }
            };

    private static final ExceptionHandler<ResourceException> LOGGING_EXCEPTION_HANDLER =
            new ExceptionHandler<ResourceException>() {
                @Override
                public void handleException(ResourceException exception) {
                    int code = exception.getCode();
                    if (logger.isTraceEnabled()) {
                        logger.trace("Resource exception: {} {}: \"{}\"",
                                exception.getCode(), exception.getReason(), exception.getMessage(), exception);
                    } else if (code >= 500 && code <= 599) { // log server-side errors
                        logger.warn("Resource exception: {} {}: \"{}\"",
                                exception.getCode(), exception.getReason(), exception.getMessage(), exception);
                    }
                }
            };

    private Filter newTraceLoggingFilter() {
        return new Filter() {
            @Override
            public Promise<ActionResponse, ResourceException> filterAction(
                    Context context, ActionRequest request, RequestHandler next) {
                logger.trace("Request: {}", request);
                return next.handleAction(context, request)
                        .thenOnResultOrException(LOGGING_RESULT_HANDLER, LOGGING_EXCEPTION_HANDLER);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> filterCreate(
                    Context context, CreateRequest request, RequestHandler next) {
                logger.trace("Request: {}", request);
                return next.handleCreate(context, request)
                        .thenOnResultOrException(LOGGING_RESULT_HANDLER, LOGGING_EXCEPTION_HANDLER);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> filterDelete(
                    Context context, DeleteRequest request, RequestHandler next) {
                logger.trace("Request: {}", request);
                return next.handleDelete(context, request)
                        .thenOnResultOrException(LOGGING_RESULT_HANDLER, LOGGING_EXCEPTION_HANDLER);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> filterPatch(
                    Context context, PatchRequest request, RequestHandler next) {
                logger.trace("Request: {}", request);
                return next.handlePatch(context, request)
                        .thenOnResultOrException(LOGGING_RESULT_HANDLER, LOGGING_EXCEPTION_HANDLER);
            }

            @Override
            public Promise<QueryResponse, ResourceException> filterQuery(
                    Context context, QueryRequest request, QueryResourceHandler handler, RequestHandler next) {
                logger.trace("Request: {}", request);
                return next.handleQuery(context, request, handler)
                        .thenOnResultOrException(LOGGING_RESULT_HANDLER, LOGGING_EXCEPTION_HANDLER);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> filterRead(
                    Context context, ReadRequest request, RequestHandler next) {
                logger.trace("Request: {}", request);
                return next.handleRead(context, request)
                        .thenOnResultOrException(LOGGING_RESULT_HANDLER, LOGGING_EXCEPTION_HANDLER);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> filterUpdate(
                    Context context, UpdateRequest request, RequestHandler next) {
                logger.trace("Request: {}", request);
                return next.handleUpdate(context, request)
                        .thenOnResultOrException(LOGGING_RESULT_HANDLER, LOGGING_EXCEPTION_HANDLER);
            }
        };
    }

    // ----- Implementation of ConnectionFactory

    @Override
    public Connection getConnection() throws ResourceException {
        return connectionFactory.getConnection();
    }

    @Override
    public Promise<Connection, ResourceException> getConnectionAsync() {
        return connectionFactory.getConnectionAsync();
    }

    @Override
    public void close() {
        connectionFactory.close();
    }

    // ----- Implementation of RouterFilterRegistration

    @Override
    public void addFilter(Filter filter) {
        // add the new filter directly to the filter chain at the end
        filterChain.getFilters().add(filter);
    }

    @Override
    public void removeFilter(Filter filter) {
        // remove the filter directly from the filter chain
        filterChain.getFilters().remove(filter);
    }

    @Override
    public void setRouterFilterReady() {
        startupFilter.setDelegate(PassthroughFilter.PASSTHROUGH_FILTER);
    }

    @Override
    public void setRouterFilterNotReady() {
        startupFilter.setDelegate(SERVICE_UNAVAILABLE_FILTER);
    }
}
