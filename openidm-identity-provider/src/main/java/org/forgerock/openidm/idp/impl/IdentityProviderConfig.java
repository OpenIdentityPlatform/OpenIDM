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
 * Copyright 2016 ForgeRock AS.
 * Portions Copyrighted 2024 3A Systems LLC.
 */
package org.forgerock.openidm.idp.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A identity provider configuration holder.
 */
@Component(
        name = IdentityProviderConfig.PID,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        // configurationFactory = true, - TODO not sure what to do about this
        service = IdentityProviderConfig.class)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM Identity Provider Config Service")
public class IdentityProviderConfig {

    /** The PID for this Component. */
    public static final String PID = "org.forgerock.openidm.identityProvider";

    private static final Logger logger = LoggerFactory.getLogger(IdentityProviderConfig.class);

    private ProviderConfig identityProviderConfig;

    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    @Activate
    public void activate(ComponentContext context) throws Exception {
        logger.info("Activating Identity Provider Config with configuration {}", context.getProperties());
        identityProviderConfig = mapper.convertValue(
                enhancedConfig.getConfigurationAsJson(context).asMap(), ProviderConfig.class);
        identityProviderConfig.setName(context.getProperties().get(ServerConstants.CONFIG_FACTORY_PID).toString());
        if (!identityProviderConfig.isEnabled()) {
            logger.debug("Identity Provider {} has been loaded, however it is disabled.", identityProviderConfig.getName());
            return;
        }
        logger.debug("OpenIDM Config for Identity Provider {} is activated.", identityProviderConfig.getName());
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        logger.info("Identity Provider Service provider {} is deactivated.", identityProviderConfig.getName());
    }

    /**
     * Returns the Identity Provider configuration
     * as a ProviderConfig object.
     *
     * @return a ProviderConfig object
     */
    public ProviderConfig getIdentityProviderConfig() {
        return identityProviderConfig;
    }
}
