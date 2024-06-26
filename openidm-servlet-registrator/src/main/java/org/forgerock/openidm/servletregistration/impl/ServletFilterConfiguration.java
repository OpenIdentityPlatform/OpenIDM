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
 * Copyright 2013-2015 ForgeRock Inc.
 * Portions Copyrighted 2024 3A Systems LLC.
 */

package org.forgerock.openidm.servletregistration.impl;

import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_SYSTEM_PROPERTIES;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.servletregistration.RegisteredFilter;
import org.forgerock.openidm.servletregistration.ServletRegistration;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes configuration to register and de-register configured servlet filters,
 * with support to load the filter or supporting classes off a defined class path.
 *
 */
@Component(
        name = "org.forgerock.openidm.servletfilter",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ServletFilterConfiguration {
    private final static Logger logger = LoggerFactory.getLogger(ServletFilterConfiguration.class);

    // Handle to registered servlet filter
    private RegisteredFilter registeredFilter;

    // Original setting of system properties
    Map<String, String> origSystemProperties = new HashMap<String, String>();

    @Reference(
            name = "ref_ServletFilterRegistration",
            service = ServletRegistration.class,
            policy = ReferencePolicy.DYNAMIC,
            cardinality = ReferenceCardinality.MANDATORY
    )
    private volatile ServletRegistration servletFilterRegistration;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    /**
     * Parses the servlet filter configuration and registers a servlet filter in OSGi.
     *
     * @param context The ComponentContext.
     * @throws Exception If a problem occurs whilst registering the filter.
     */
    @Activate
    protected synchronized void activate(ComponentContext context) throws Exception {
        logger.info("Activating servlet registrator with configuration {}", context.getProperties());
        JsonValue config = enhancedConfig.getConfigurationAsJson(context);

        logger.debug("Parsed servlet filter config: {}", config);

        registeredFilter = servletFilterRegistration.registerFilter(config);
        logger.info("Successfully registered servlet filter {}", context.getProperties());

        origSystemProperties = new HashMap<String, String>();
        JsonValue rawSystemProperties = config.get(SERVLET_FILTER_SYSTEM_PROPERTIES);
        for (String key : rawSystemProperties.keys()) {
            String prev = System.setProperty(key, rawSystemProperties.get(key).asString());
            // null value is used to keep track of properties that weren't set before
            origSystemProperties.put(key, prev);
        }
    }

    /**
     * De-registers the corresponding servlet filter.
     *
     * @param context The ComponentContext.
     */
    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        if (registeredFilter != null) {
            try {
                servletFilterRegistration.unregisterFilter(registeredFilter);
                logger.info("Unregistered servlet filter {}.", context.getProperties());
            } catch (Exception ex) {
                logger.warn("Failure reported during unregistering of servlet filter {}: {}",
                        new Object[]{context.getProperties(), ex.getMessage(), ex});
            }
        }

        // Restore the system properties before this config was applied
        for (String key : origSystemProperties.keySet()) {
            String val = origSystemProperties.get(key);
            if (val == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, val);
            }
        }
        logger.debug("Deactivated {}", context);
    }
}
