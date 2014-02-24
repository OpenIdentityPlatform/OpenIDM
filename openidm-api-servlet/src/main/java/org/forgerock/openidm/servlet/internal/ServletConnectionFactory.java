/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CrossCutFilterResultHandler;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.FilterCondition;
import org.forgerock.json.resource.Filters;
import org.forgerock.json.resource.FutureResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UntypedCrossCutFilter;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.core.filter.ScriptedFilter;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The ConnectionFactory responsible for providing Connections to routing requests initiated
 * from an external request on the api servlet.
 *
 * @author brmiller
 */
@Component(name = ServletConnectionFactory.PID, policy = ConfigurationPolicy.OPTIONAL,
        configurationFactory = false, immediate = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Common REST Servlet Connection Factory"),
})
public class ServletConnectionFactory implements ConnectionFactory {

    public static final String PID = "org.forgerock.openidm.router";

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

    @Activate
    protected void activate(ComponentContext context) throws ServletException, NamespaceException {
        logger.debug("Creating servlet router/connection factory");
        EnhancedConfig config = JSONEnhancedConfig.newInstance();
        String factoryPid = config.getConfigurationFactoryPid(context);
        if (StringUtils.isNotBlank(factoryPid)) {
            throw new IllegalArgumentException(
                    "Factory configuration not allowed, must not have property: "
                            + ServerConstants.CONFIG_FACTORY_PID);
        }
        try {
            connectionFactory = Resources.newInternalConnectionFactory(
                    init(config.getConfigurationAsJson(context), requestHandler));
        } catch (Throwable t) {
            logger.error("Failed to configure the Filtered Router service", t);
        }

        logger.info("Servlet ConnectionFactory created.");
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
    }

    /**
     * Initialize the router with configuration. Supports modifying router configuration.
     *
     * @param configuration the router configuration listing filters that are installed
     * @param handler the request handler (router)
     * @return the RequestHandler decorated with a FilterChain consisting of any filters that are configured
     */
    RequestHandler init(JsonValue configuration, final RequestHandler handler)
            throws ScriptException {
        final JsonValue filterConfig = configuration.get("filters").expect(List.class);
        final List<Filter> filters = new ArrayList<Filter>(filterConfig.size() + 1); // add one for the logging filter

        filters.add(newLoggingFilter());

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
        Pattern pattern = config.get("pattern").asPattern();
        if (null != pattern) {
            filterCondition = Filters.matchResourceName(pattern);
        }

        final EnumSet<RequestType> requestTypes = EnumSet.noneOf(RequestType.class);
        for (JsonValue method : config.get("methods").expect(List.class)) {
            requestTypes.add(method.asEnum(RequestType.class));
        }

        if (!requestTypes.isEmpty()) {
            if (null == filterCondition) {
                filterCondition = Filters.matchRequestType(requestTypes);
            } else {
                filterCondition =
                        Filters.and(filterCondition, Filters.matchRequestType(requestTypes));
            }
        }

        // TODO add condition to filter condition
        Pair<JsonPointer, ScriptEntry> condition = getScript(config.get("condition"));

        Pair<JsonPointer, ScriptEntry> onRequest = getScript(config.get("onRequest"));
        Pair<JsonPointer, ScriptEntry> onResponse = getScript(config.get("onResponse"));
        Pair<JsonPointer, ScriptEntry> onFailure = getScript(config.get("onFailure"));

        if (null == onRequest && null == onResponse && null == onFailure) {
            return null;
        }

        if (null == filterCondition) {
            return Filters.asFilter(new ScriptedFilter(onRequest, onResponse, onFailure));
        } else {
            return Filters.conditionalFilter(filterCondition, Filters.asFilter(
                    new ScriptedFilter(onRequest, onResponse, onFailure)));
        }
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
