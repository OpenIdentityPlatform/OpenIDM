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
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.AbstractConnectionWrapper;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.CrossCutFilterResultHandler;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.FilterCondition;
import org.forgerock.json.resource.Filters;
import org.forgerock.json.resource.FutureResult;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UntypedCrossCutFilter;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.filter.AuditFilter;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.core.filter.ScriptedFilter;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import javax.servlet.ServletException;

import java.util.ArrayList;
import java.util.Collection;
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
                    public Resource create(Context context,
                            CreateRequest request) throws ResourceException {
                        EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.create(context, request);
                        } finally {
                            measure.end();
                        }
                    }
                    @Override
                    public FutureResult<Resource> createAsync(Context context,
                            CreateRequest request,
                            ResultHandler<? super Resource> handler) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.createAsync(context, request, newInstrumentedResultHandler(handler, measure));
                    }
                    @Override
                    public Resource read(Context context, ReadRequest request)
                            throws ResourceException {
                        EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.read(context, request);
                        } finally {
                            measure.end();
                        }
                    }
                    @Override
                    public FutureResult<Resource> readAsync(Context context,
                            ReadRequest request,
                            final ResultHandler<? super Resource> handler) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.readAsync(context, request, newInstrumentedResultHandler(handler, measure));
                    }
                    @Override
                    public Resource update(Context context,
                            UpdateRequest request) throws ResourceException {
                        EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.update(context, request);
                        } finally {
                            measure.end();
                        }
                    }
                    @Override
                    public FutureResult<Resource> updateAsync(Context context,
                            UpdateRequest request,
                            ResultHandler<? super Resource> handler) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.updateAsync(context, request, newInstrumentedResultHandler(handler, measure));
                    }
                    @Override
                    public Resource delete(Context context,
                            DeleteRequest request) throws ResourceException {
                        EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.delete(context, request);
                        } finally {
                            measure.end();
                        }
                    }
                    @Override
                    public FutureResult<Resource> deleteAsync(Context context,
                            DeleteRequest request,
                            ResultHandler<? super Resource> handler) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.deleteAsync(context, request, newInstrumentedResultHandler(handler, measure));
                    }
                    @Override
                    public Resource patch(Context context, PatchRequest request)
                            throws ResourceException {
                        EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.patch(context, request);
                        } finally {
                            measure.end();
                        }
                    }
                    @Override
                    public FutureResult<Resource> patchAsync(Context context,
                            PatchRequest request,
                            ResultHandler<? super Resource> handler) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.patchAsync(context, request, newInstrumentedResultHandler(handler, measure));
                    }
                    @Override
                    public JsonValue action(Context context,
                            ActionRequest request) throws ResourceException {
                        EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.action(context, request);
                        } finally {
                            measure.end();
                        }
                    }                    
                    @Override
                    public FutureResult<JsonValue> actionAsync(Context context,
                            ActionRequest request,
                            ResultHandler<? super JsonValue> handler) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.actionAsync(context, request, newInstrumentedResultHandler(handler, measure));
                    }
                    @Override
                    public QueryResult query(Context context, QueryRequest request,
                            Collection<? super Resource> results) throws ResourceException {
                        EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        try {
                            return super.query(context, request, results);
                        } finally {
                            measure.end();
                        }
                    }
                    @Override
                    public FutureResult<QueryResult> queryAsync(
                            Context context, QueryRequest request,
                            QueryResultHandler handler) {
                        final EventEntry measure = Publisher.start(getRouterEventName(request), request, null);
                        return super.queryAsync(context, request, newInstrumentedQueryResultHandler(handler, measure));
                    }
                };
            }
            
            @Override
            public FutureResult<Connection> getConnectionAsync(final ResultHandler<? super Connection> handler) {
                try {
                    final Connection connection = getConnection();
                    final FutureResult<Connection> future = Resources.newCompletedFutureResult(connection);
                    if (handler != null) {
                        handler.handleResult(connection);
                    }
                    return future;
                } catch (ResourceException e) {
                    throw new RuntimeException("Can't obtain connection", e);
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
                    idContext = request.getResourceName();
                } else {
                    // For RUD, patch group statistics without the local resource identifier
                    idContext = request.getResourceNameObject().head(request.getResourceNameObject().size() - 1).toString();
                }

                String eventName = new StringBuilder(EVENT_ROUTER_PREFIX)
                        .append(idContext)
                        .append("/")
                        .append(requestType.toString().toLowerCase())
                        .toString();
                
                return Name.get(eventName);
            }
            
            /**
             * A result handler wrapper for smartevent measurement handling
             * @param handler the handler to wrap
             * @param measure the started smartevent to end upon response
             * @return the wrapped handler
             */
            private ResultHandler newInstrumentedResultHandler(final ResultHandler handler, final EventEntry measure) {
                return new ResultHandler<Object>() {  
                    @Override
                    public void handleResult(Object result) {
                        try {
                            if (handler != null) {
                                handler.handleResult(result);
                            }
                        } finally {
                            measure.end();
                        }
                    }                    
                    @Override
                    public void handleError(ResourceException error) {
                        try {
                            if (handler != null) {
                                handler.handleError(error);
                            }
                        } finally {
                            measure.end();
                        }
                    }                    
                };
            }
            
            /**
             * A query result handler wrapper for smartevent measurement handling
             * @param handler the handler to wrap
             * @param measure the started smartevent to end upon response
             * @return the wrapped handler
             */
            private QueryResultHandler newInstrumentedQueryResultHandler(final QueryResultHandler handler, final EventEntry measure) {
                return new QueryResultHandler() {
                    @Override
                    public boolean handleResource(Resource resource) {
                        return handler.handleResource(resource);    
                    }
                    public void handleResult(QueryResult result) {
                        try {
                            if (handler != null) {
                                handler.handleResult(result);
                            }
                        } finally {
                            measure.end();
                        }
                    }                    
                    @Override
                    public void handleError(ResourceException error) {
                        try {
                            if (handler != null) {
                                handler.handleError(error);
                            }
                        } finally {
                            measure.end();
                        }
                    }                    
                };
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
        filters.add(Filters.conditionalFilter(Filters.matchResourceName("^(?!.*(^audit/)).*$"), auditFilter));

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
     * @throws org.forgerock.json.fluent.JsonValueException
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
            filterCondition = Filters.matchResourceName(pattern);
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
                ? Filters.asFilter(new ScriptedFilter(onRequest, onResponse, onFailure))
                : Filters.conditionalFilter(filterCondition, Filters.asFilter(
                        new ScriptedFilter(onRequest, onResponse, onFailure)));

        // Check for a condition script
        if (null != condition) {
            FilterCondition conditionFilterCondition = new FilterCondition() {
                @Override
                public boolean matches(final ServerContext context, final Request request) {
                    try {
                        return (Boolean)condition.getValue().getScript(context).eval();
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

    private Filter newLoggingFilter() {
        return Filters.asFilter(
                new UntypedCrossCutFilter<Void>() {
                    @Override
                    public void filterGenericError(ServerContext context, Void state, ResourceException error, ResultHandler<Object> handler) {
                        int code = error.getCode();
                        if (logger.isTraceEnabled()) {
                            logger.trace("Resource exception: {} {}: \"{}\"", new Object[] { error.getCode(), error.getReason(), error.getMessage(), error });
                        } else if (code >= 500 && code <= 599) { // log server-side errors
                            logger.warn("Resource exception: {} {}: \"{}\"", new Object[] { error.getCode(), error.getReason(), error.getMessage(), error });
                        }
                        handler.handleError(error);
                    }

                    @Override
                    public void filterGenericRequest(ServerContext context, Request request, RequestHandler next, CrossCutFilterResultHandler<Void, Object> handler) {
                        logger.trace("Request: {}", request);
                        handler.handleContinue(context, null);
                    }

                    @Override
                    public <R> void filterGenericResult(ServerContext context, Void state, R result, ResultHandler<R> handler) {
                        logger.trace("Result: {}", result);
                        handler.handleResult(result);
                    }

                    @Override
                    public void filterQueryResource(ServerContext context, Void state, Resource resource, QueryResultHandler handler) {
                        logger.trace("Response: {}", resource);
                        handler.handleResource(resource);
                    }
                });
    }

    // ----- Implementation of ConnectionFactory

    @Override
    public Connection getConnection() throws ResourceException {
        return connectionFactory.getConnection();
    }

    @Override
    public FutureResult<Connection> getConnectionAsync(ResultHandler<? super Connection> handler) {
        return connectionFactory.getConnectionAsync(handler);
    }
    @Override
    public void close() {
        connectionFactory.close();
    }

}
