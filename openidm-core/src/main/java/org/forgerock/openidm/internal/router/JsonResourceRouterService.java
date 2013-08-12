/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.internal.router;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
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
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.FilterCondition;
import org.forgerock.json.resource.Filters;
import org.forgerock.json.resource.FutureResult;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.core.filter.ScriptedFilter;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides internal routing for a top-level object set.
 *
 * @author Paul C. Bryan
 * @author aegloff
 */
@Component(name = JsonResourceRouterService.PID, policy = ConfigurationPolicy.OPTIONAL,
        metatype = true, configurationFactory = false, immediate = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM internal JSON resource router"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/") })
// @References({
// @Reference(referenceInterface = Filter.class,
// cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy =
// ReferencePolicy.DYNAMIC,
// target = "(org.forgerock.openidm.router=*)") })
public class JsonResourceRouterService implements ConnectionFactory
/* ,ConnectionProvider */{

    // Public Constants
    public static final String PID = "org.forgerock.openidm.router";

    /**
     * Setup logging for the {@link JsonResourceRouterService}.
     */
    private final static Logger logger = LoggerFactory.getLogger(JsonResourceRouterService.class);

    /**
     * Event name prefix for monitoring the router
     */
    public final static String EVENT_ROUTER_PREFIX = "openidm/internal/router/";

    private ConnectionFactory connectionFactory = null;

    @Reference(target = "(org.forgerock.openidm.router=*)")
    protected RequestHandler requestHandler = null;

    void bindRequestHandler(final RequestHandler service) {
        requestHandler = service;
    }

    void unbindRequestHandler(final RequestHandler service) {
        requestHandler = null;
    }

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private ScriptRegistry scriptRegistry;

    void bindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = service;
    }

    void unbindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = null;
    }

    /**
     * Initialize the router with configuration. Supports modifying router
     * configuration.
     */
    RequestHandler init(JsonValue configuration, final RequestHandler handler)
            throws ScriptException {

        List<Filter> filters = null;

        // Chain chain = new Chain(router);
        for (JsonValue jv : configuration.get("filters").expect(List.class)) {
            // optional
            Filter filter = newFilter(jv);
            if (null != filter && null == filters) {
                filters = new ArrayList<Filter>(configuration.get("filters").size());
            }
            if (null != filter) {
                filters.add(filter);
            }
        }

        if (null != filters) {
            return new FilterChain(handler, filters);
        } else {
            return handler;
        }
    }

    private ConnectionFactory internal = null;

    @Activate
    void activate(ComponentContext context) {
        EnhancedConfig config = JSONEnhancedConfig.newInstance();

        String factoryPid = config.getConfigurationFactoryPid(context);
        if (StringUtils.isNotBlank(factoryPid)) {
            throw new IllegalArgumentException(
                    "Factory configuration not allowed, must not have property: "
                            + ServerConstants.CONFIG_FACTORY_PID);
        }

        try {
            connectionFactory =
                    Resources.newInternalConnectionFactory(init(config
                            .getConfigurationAsJson(context), requestHandler));
        } catch (Throwable t) {
            logger.error("Failed to configure the Filtered Router service", t);
        }
        logger.info("Reconciliation service activated.");
    }

    @Modified
    void modified(ComponentContext context) {
        activate(context);
        logger.info("Reconciliation service modified.");
    }

    @Deactivate
    void deactivate(ComponentContext context) {
        logger.info("Reconciliation service deactivated.");
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

    /**
     * TODO: Description.
     *
     * @param config
     *            TODO.
     * @throws JsonValueException
     *             TODO.
     */
    public Filter newFilter(JsonValue config) throws JsonValueException, ScriptException {

        FilterCondition filterCondition = null;
        Pattern pattern = config.get("pattern").asPattern();
        if (null != pattern) {
            filterCondition = Filters.matchResourceName(pattern);
        }

        EnumSet<RequestType> requestTypes = null;
        for (JsonValue method : config.get("methods").expect(List.class)) {
            if (null == requestTypes) {
                requestTypes = EnumSet.of(method.asEnum(RequestType.class));
            } else {
                requestTypes.add(method.asEnum(RequestType.class));
            }
        }

        if (null != requestTypes) {
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
            return Filters.conditionalFilter(filterCondition, Filters.asFilter(new ScriptedFilter(
                    onRequest, onResponse, onFailure)));
        }
    }

    private Pair<JsonPointer, ScriptEntry> getScript(JsonValue scriptJson) throws ScriptException {
        if (!scriptJson.expect(Map.class).isNull()) {

            ScriptEntry entry = scriptRegistry.takeScript(scriptJson);
            return Pair.of(scriptJson.getPointer(), entry);
            // TODO throw better exception
            // throw new JsonValueException(scriptJson,
            // "Failed to find script");

        }
        return null;
    }

}
