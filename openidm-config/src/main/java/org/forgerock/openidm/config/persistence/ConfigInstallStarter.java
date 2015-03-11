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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.config.persistence;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.repo.RepoBootService;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts the configuration installation handling once the 
 * basic services for it are available
 *
 */
@Component(
    name = "org.forgerock.openidm.config.enhanced.starter",
    policy = ConfigurationPolicy.OPTIONAL,
    immediate = true
)
@Properties({
    @Property(name = "service.description", value = "OpenIDM internal config installation starter"),
    @Property(name = "service.vendor", value = "ForgeRock AS")
})
public class ConfigInstallStarter {
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
