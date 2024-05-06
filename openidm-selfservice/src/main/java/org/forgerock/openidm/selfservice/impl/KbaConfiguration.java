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
 * Portions Copyrighted 2024 3A Systems LLC.
 */
package org.forgerock.openidm.selfservice.impl;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;

/**
 * A KBA Configuration holder service.
 */
@Component(
        name = KbaConfiguration.PID,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = KbaConfiguration.class)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("KBA Configuration")
public class KbaConfiguration {

    static final String PID = "org.forgerock.openidm.selfservice.kba";

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    private JsonValue config;

    @Activate
    void activate(ComponentContext context) {
        config = enhancedConfig.getConfigurationAsJson(context);
    }

    public JsonValue getConfig() {
        return config;
    }
}
