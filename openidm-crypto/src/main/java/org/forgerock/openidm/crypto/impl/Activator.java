/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.crypto.impl;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.scr.annotations.Property;
import org.forgerock.openidm.config.persistence.ConfigBootstrapHelper;

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.repo.RepoBootService;
import org.forgerock.openidm.repo.RepositoryService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crypto bundle activator
 * @author aegloff
 */
public class Activator implements BundleActivator {
    final static Logger logger = LoggerFactory.getLogger(Activator.class);

    CryptoServiceImpl cryptoSvc;
    
    public void start(BundleContext context) throws Exception {
        logger.debug("Crypto bundle starting");
        
//JsonNode keystoreConfig = ConfigBootstrapHelper.getBootConfig("keystore");

        cryptoSvc = new CryptoServiceImpl();
        cryptoSvc.activate(context); 
//, keystoreConfig);
        
        // Register crypto service 
        Hashtable<String, String> prop = new Hashtable<String, String>();
        prop.put("service.pid", "org.forgerock.openidm.crypto");
        prop.put("openidm.router.prefix", "crypto");
        prop.put("service.description", "OpenIDM cryptography service");
        prop.put("service.vendor", "ForgeRock AS");
        context.registerService(CryptoService.class.getName(), cryptoSvc, prop);
        logger.info("Registered cryptography service");
        
        logger.debug("Crypto bundle started");
    }

     public void stop(BundleContext context) {
         if (cryptoSvc != null) {
             cryptoSvc.deactivate(context);
         }
         logger.debug("Crypto bundle stopped");
     }
}