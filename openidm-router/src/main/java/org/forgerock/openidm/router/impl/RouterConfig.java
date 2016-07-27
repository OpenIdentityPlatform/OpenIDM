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

package org.forgerock.openidm.router.impl;

import static org.forgerock.json.JsonValueFunctions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
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
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.FilterCondition;
import org.forgerock.json.resource.Filters;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestType;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.router.RouterFilterRegistration;
import org.forgerock.openidm.filter.ScriptedFilter;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.context.Context;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A config object holder to manage the router configuration, including the list of CREST router {@link Filter}s
 * to be added to the {@link FilterChain} managed by the {@link RouterFilterRegistration} service.
 */
@Component(name = RouterConfig.PID, policy = ConfigurationPolicy.REQUIRE,
        configurationFactory = false, immediate = true)
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Router Config")
})
public class RouterConfig {

    static final String PID = "org.forgerock.openidm.router";

    /** Logger for this class. */
    private final static Logger logger = LoggerFactory.getLogger(RouterConfig.class);

    /** Script Registry service. */
    @Reference
    private volatile ScriptRegistry scriptRegistry = null;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig = null;

    /** The filter chain is managed by the RouterFilterRegistration */
    @Reference
    private RouterFilterRegistration filterRegistration = null;

    /** the configured router filters */
    private final List<Filter> filters = new ArrayList<>();

    /** the component configuration */
    private JsonValue config;

    @Activate
    protected void activate(ComponentContext context) {
        logger.debug("Creating router config");
        String factoryPid = enhancedConfig.getConfigurationFactoryPid(context);
        if (StringUtils.isNotBlank(factoryPid)) {
            throw new IllegalArgumentException("Factory configuration not allowed, must not have property: "
                    + ServerConstants.CONFIG_FACTORY_PID);
        }
        try {
            config = enhancedConfig.getConfigurationAsJson(context);
            createFiltersFromConfig(config);
        } catch (Exception e) {
            logger.error("Failed to configure router filters", e);
        }
    }

    @Modified
    protected void modified(ComponentContext context) {
        logger.debug("Updating router config");
        try {
            JsonValue modifiedConfig = enhancedConfig.getConfigurationAsJson(context);
            if (config != null
                    && modifiedConfig != null
                    && config.isEqualTo(modifiedConfig)) {
                return;
            }
            deactivate(context);
            createFiltersFromConfig(modifiedConfig);
            config = modifiedConfig;
        } catch (Exception e) {
            logger.error("Failed to reconfigure router filters", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivating router config");
        filterRegistration.setRouterFilterNotReady();
        removeFilters();
        logger.info("Router filters removed.");
    }

    private void createFiltersFromConfig(JsonValue config) throws ScriptException {
        addFilters(createFilterList(config));
        filterRegistration.setRouterFilterReady();
        if (filters.isEmpty()) {
            logger.warn("Configuring router without filters.");
        } else {
            logger.info("Router filters added.");
        }
    }

    private void addFilters(List<Filter> newFilters) throws ScriptException {
        for (Filter filter : newFilters) {
            filterRegistration.addFilter(filter);
            filters.add(filter);
        }
    }

    private void removeFilters() {
        Iterator<Filter> iterator = filters.iterator();
        while (iterator.hasNext()) {
            Filter filter = iterator.next();
            filterRegistration.removeFilter(filter);
            iterator.remove();
        }
    }

    /**
     * Initialize the router filter list from configuration.
     *
     * @param configuration the router configuration listing filters that are installed
     * @return the list of Filters
     * @throws ScriptException on failure to create filter from config
     */
    private List<Filter> createFilterList(JsonValue configuration) throws ScriptException {
        final JsonValue filterConfig = configuration.get("filters").expect(List.class);
        final List<Filter> filters = new ArrayList<>(filterConfig.size());

        for (JsonValue jv : filterConfig) {
            Filter filter = newFilter(jv);
            if (null != filter) {
                filters.add(filter);
            }
        }

        return filters;
    }

    /**
     * Create a Filter from the filter configuration.
     *
     * @param config
     *            the configuration describing a single filter.
     * @return a Filter
     * @throws ScriptException on failure to create filter from config
     * @throws JsonValueException if filter configuration is incorrect
     */
    Filter newFilter(JsonValue config) throws JsonValueException, ScriptException {
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
        Pattern pattern = config.get("pattern").as(pattern());
        if (null != pattern) {
            filterCondition = Filters.matchResourcePath(pattern);
        }

        // Check for condition on type
        final EnumSet<RequestType> requestTypes = EnumSet.noneOf(RequestType.class);
        for (JsonValue method : config.get("methods").expect(List.class)) {
            requestTypes.add(method.as(enumConstant(RequestType.class)));
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
                        final Script script = condition.getValue().getScript(context);
                        script.put("request", request);
                        script.put("context", context);
                        return (Boolean) script.eval();
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

        return Pair.of(scriptJson.getPointer(), scriptRegistry.takeScript(scriptJson));
    }

}
