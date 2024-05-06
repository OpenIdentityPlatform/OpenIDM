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
 * Copyright 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2024 3A Systems LLC.
 */
package org.forgerock.openidm.config.persistence;

import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.repo.RepoBootService;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts the configuration installation handling once the 
 * basic services for it are available
 *
 */
@Component(
        name = ConfigInstallStarter.PID,
        configurationPolicy = ConfigurationPolicy.OPTIONAL,
        immediate = true)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM internal config installation starter")
public class ConfigInstallStarter {

    static final String PID = "org.forgerock.openidm.config.enhanced.starter";
    final static Logger logger = LoggerFactory.getLogger(ConfigInstallStarter.class);

    @Reference
    ConfigurationAdmin configAdmin;

    @Reference
    CryptoService crypto;
    
    @Reference
    protected RepoBootService repo;
    
    @Reference
    ConfigPersisterMarker configPersisterMarker;
    
    @Activate
    protected synchronized void activate(ComponentContext context) throws Exception {
        logger.info("Services ready to handle config install.");
        configPersisterMarker.checkReady();
        ConfigBootstrapHelper.installAllConfig(configAdmin);
        logger.debug("Config install handling complete.");
    }
    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        logger.debug("Deactivating.");
    }

}
