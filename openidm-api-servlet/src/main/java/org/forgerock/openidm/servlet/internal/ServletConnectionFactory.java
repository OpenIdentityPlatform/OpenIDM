/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.servlet.internal;

import static org.forgerock.util.promise.Promises.newResultPromise;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.http.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.AbstractConnectionWrapper;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.FilterCondition;
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
import org.forgerock.openidm.audit.filter.AuditFilter;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import javax.servlet.ServletException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The ConnectionFactory responsible for providing Connections to routing requests initiated
 * from an external request on the api servlet.
 */
@Component(name = ServletConnectionFactory.PID, policy = ConfigurationPolicy.OPTIONAL,
        configurationFactory = false, immediate = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Common REST Servlet Connection Factory")
})
public class ServletConnectionFactory implements ConnectionFactory {

    public static final String PID = "org.forgerock.openidm.router";
    
    /** Event name prefix for monitoring the router */
    public final static String EVENT_ROUTER_PREFIX = "openidm/internal/router/";

    /**
     * Setup logging for the {@link org.forgerock.openidm.servlet.internal.ServletConnectionFactory}.
     */
    private final static Logger logger = LoggerFactory.getLogger(ServletConnectionFactory.class);

    // the created connection factory
    protected ConnectionFactory connectionFactory;

    /** the Request Handler (Router) */
    @Reference(target = "(org.forgerock.openidm.router=*)")
    protected RequestHandler requestHandler = null;

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private ScriptRegistry scriptRegistry = null;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;

    @Activate
    protected void activate(ComponentContext context) throws ServletException, NamespaceException {
        logger.debug("Creating servlet router/connection factory");
        String factoryPid = enhancedConfig.getConfigurationFactoryPid(context);
        if (StringUtils.isNotBlank(factoryPid)) {
            throw new IllegalArgumentException(
                    "Factory configuration not allowed, must not have property: "
                            + ServerConstants.CONFIG_FACTORY_PID);
        }
        try {
            final AuditFilter auditFilter = new AuditFilter(connectionFactory);
            connectionFactory = newWrappedInternalConnectionFactory(Resources.newInternalConnectionFactory(
                    init(enhancedConfig.getConfigurationAsJson(context), requestHandler, auditFilter)));
            auditFilter.setConnectionFactory(connectionFactory);
        } catch (Throwable t) {
            logger.error("Failed to configure the Filtered Router service", t);
        }

        logger.info("Servlet ConnectionFactory created.");
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
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
                            return super.create(context, request);
                        } finally {
                            measure.end();
                        }
                    }
                    @Override
                    public Promise<ResourceResponse, ResourceException> createAsync(
                            Context context, CreateRequest request) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.createAsync(context, request)
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
                    idContext = request.getResourcePathObject().head(request.getResourcePathObject().size() - 1).toString();
                }

                String eventName = new StringBuilder(EVENT_ROUTER_PREFIX)
                        .append(idContext)
                        .append("/")
                        .append(requestType.toString().toLowerCase())
                        .toString();
                
                return Name.get(eventName);
            }
        };
    }

    /**
     * Initialize the router with configuration. Supports modifying router configuration.
     *
     * @param configuration the router configuration listing filters that are installed
     * @param handler the request handler (router)
     * @param auditFilter the audit filter to attach to the request handler
     * @return the RequestHandler decorated with a FilterChain consisting of any filters that are configured
     */
    RequestHandler init(JsonValue configuration, final RequestHandler handler, final AuditFilter auditFilter)
            throws ScriptException, ResourceException {
        final JsonValue filterConfig = configuration.get("filters").expect(List.class);
        final List<Filter> filters = new ArrayList<>(filterConfig.size() + 1); // add one for the logging filter

        filters.add(newLoggingFilter());
        filters.add(Filters.conditionalFilter(Filters.matchResourcePath("^(?!.*(^audit/)).*$"), auditFilter));

        for (JsonValue jv : filterConfig) {
            Filter filter = newFilter(jv);
            if (null != filter) {
                filters.add(filter);
            }
        }

        // filters will always have at least the logging filter
        return new FilterChain(handler, filters);
    }

    /**
     * Create a Filter from the filter configuration.
     *
     * @param config
     *            the configuration describing a single filter.
     * @return a Filter
     * @throws org.forgerock.json.JsonValueException
     *             TODO.
     */
    public Filter newFilter(JsonValue config) throws JsonValueException, ScriptException {
        FilterCondition filterCondition = null;

        final Pair<JsonPointer, ScriptEntry> condition = getScript(config.get("condition"));
        final Pair<JsonPointer, ScriptEntry> onRequest = getScript(config.get("onRequest"));
        final Pair<JsonPointer, ScriptEntry> onResponse = getScript(config.get("onResponse"));
        final Pair<JsonPointer, ScriptEntry> onFailure = getScript(config.get("onFailure"));

        // Require at least one of the following
        if (null == onRequest && null == onResponse && null == onFailure) {
            return null;
        }
        
        // Check for condition on pattern
        Pattern pattern = config.get("pattern").asPattern();
        if (null != pattern) {
            filterCondition = Filters.matchResourcePath(pattern);
        }

        // Check for condition on type
        final EnumSet<RequestType> requestTypes = EnumSet.noneOf(RequestType.class);
        for (JsonValue method : config.get("methods").expect(List.class)) {
            requestTypes.add(method.asEnum(RequestType.class));
        }
        if (!requestTypes.isEmpty()) {
            filterCondition = (null == filterCondition) 
                    ? Filters.matchRequestType(requestTypes)
                    : Filters.and(filterCondition, Filters.matchRequestType(requestTypes));
        }

        // Create the filter
        Filter filter = (null == filterCondition)
                ? new ScriptedFilter(onRequest, onResponse, onFailure)
                : Filters.conditionalFilter(filterCondition, new ScriptedFilter(onRequest, onResponse, onFailure));

        // Check for a condition script
        if (null != condition) {
            FilterCondition conditionFilterCondition = new FilterCondition() {
                @Override
                public boolean matches(final Context context, final Request request) {
                    try {
                        return (Boolean) condition.getValue().getScript(context).eval();
                    } catch (ScriptException e) {
                        logger.warn("Failed to evaluate filter condition: ", e.getMessage(), e);
                    }
                    return false;
                }
            };
            filter = Filters.conditionalFilter(conditionFilterCondition, filter);
        }
        return filter;
    }

    private Pair<JsonPointer, ScriptEntry> getScript(JsonValue scriptJson) throws ScriptException {
        if (scriptJson.expect(Map.class).isNull()) {
            return null;
        }

        ScriptEntry entry = scriptRegistry.takeScript(scriptJson);
        return Pair.of(scriptJson.getPointer(), entry);
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
                        logger.trace("Resource exception: {} {}: \"{}\"", exception.getCode(), exception.getReason(), exception.getMessage(), exception);
                    } else if (code >= 500 && code <= 599) { // log server-side errors
                        logger.warn("Resource exception: {} {}: \"{}\"", exception.getCode(), exception.getReason(), exception.getMessage(), exception);
                    }
                }
            };

    private Filter newLoggingFilter() {
        return new Filter() {
            @Override
            public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request, RequestHandler next) {
                logger.trace("Request: {}", request);
                return next.handleAction(context, request)
                        .thenOnResultOrException(LOGGING_RESULT_HANDLER, LOGGING_EXCEPTION_HANDLER);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request, RequestHandler next) {
                logger.trace("Request: {}", request);
                return next.handleCreate(context, request)
                        .thenOnResultOrException(LOGGING_RESULT_HANDLER, LOGGING_EXCEPTION_HANDLER);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request, RequestHandler next) {
                logger.trace("Request: {}", request);
                return next.handleDelete(context, request)
                        .thenOnResultOrException(LOGGING_RESULT_HANDLER, LOGGING_EXCEPTION_HANDLER);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request, RequestHandler next) {
                logger.trace("Request: {}", request);
                return next.handlePatch(context, request)
                        .thenOnResultOrException(LOGGING_RESULT_HANDLER, LOGGING_EXCEPTION_HANDLER);
            }

            @Override
            public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request, QueryResourceHandler handler, RequestHandler next) {
                logger.trace("Request: {}", request);
                return next.handleQuery(context, request, handler)
                        .thenOnResultOrException(LOGGING_RESULT_HANDLER, LOGGING_EXCEPTION_HANDLER);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request, RequestHandler next) {
                logger.trace("Request: {}", request);
                return next.handleRead(context, request)
                        .thenOnResultOrException(LOGGING_RESULT_HANDLER, LOGGING_EXCEPTION_HANDLER);
            }

            @Override
            public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request, RequestHandler next) {
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

}
