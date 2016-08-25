/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.selfservice.impl;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.sync.PropertyMapping;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.services.context.Context;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JsonValue property mapping service. Currently bound only to mapping of IdP common schema to managed user.
 */
@Component(name = PropertyMappingService.PID, immediate=true, policy = ConfigurationPolicy.REQUIRE)
@Service({ PropertyMappingService.class })
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Property Mapping Service"),
})
public class PropertyMappingService {

    static final String PID = "org.forgerock.openidm.selfservice.propertymap";

    private static final Logger logger = LoggerFactory.getLogger(PropertyMappingService.class);

    /**
     * Enhanced configuration service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    private JsonValue config;

    @Activate
    void activate(ComponentContext context) {
        config = enhancedConfig.getConfigurationAsJson(context);
    }

    @Modified
    void modify(ComponentContext context) {
        config = enhancedConfig.getConfigurationAsJson(context);
    }

    public JsonValue getConfig() {
        return config;
    }

    public JsonValue apply(final JsonValue profile, final Context context) throws SynchronizationException {
        JsonValue target = json(object());
        for (JsonValue mapping : getConfig().get("properties")) {
            new PropertyMapping(mapping).apply(profile, null, target, null, null, context);
        }
        return target;
    }
}