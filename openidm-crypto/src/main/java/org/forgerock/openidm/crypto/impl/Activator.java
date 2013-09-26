/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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

import java.util.Hashtable;

import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.factory.CryptoServiceFactory;
import org.forgerock.openidm.crypto.factory.CryptoUpdateService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crypto bundle activator
 * 
 * @author aegloff
 * @author ckienle
 */
public class Activator implements BundleActivator {
    final static Logger logger = LoggerFactory.getLogger(Activator.class);

    CryptoServiceImpl cryptoSvc;
    
    public void start(BundleContext context) throws Exception {
        logger.debug("Crypto bundle starting");
        
        // Force fragment to resolve
        ensureJettyFragmentResolved(context);
        
        cryptoSvc = (CryptoServiceImpl) CryptoServiceFactory.getInstance();
        cryptoSvc.activate(context);
        
        // Register crypto service 
        Hashtable<String, String> prop = new Hashtable<String, String>();
        prop.put("service.pid", "org.forgerock.openidm.crypto");
// FIXME: Weird... CryptoServiceImpl is not a JsonResource (or legacy ObjectSet).
// Why are we trying to register a router prefix then? Reserving for future use?
        prop.put("openidm.router.prefix", "crypto");
        prop.put("service.description", "OpenIDM cryptography service");
        prop.put("service.vendor", "ForgeRock AS");
        context.registerService(CryptoService.class.getName(), cryptoSvc, prop);
        context.registerService(CryptoUpdateService.class.getName(), cryptoSvc, null);
        logger.info("Registered cryptography service");
        
        logger.debug("Crypto bundle started");
    }

     public void stop(BundleContext context) {
         if (cryptoSvc != null) {
             cryptoSvc.deactivate(context);
         }
         logger.debug("Crypto bundle stopped");
     }
     
     /**
      * Ensures the Jetty Fragment bundle gets resolved and attached
      * to the Jetty bundle.
      * This is a work-around for the felix issue not consistently
      * resolving the fragment.
      */
     public void ensureJettyFragmentResolved(BundleContext context) throws Exception {
         org.osgi.framework.Bundle[] bundles = context.getBundles();
         org.osgi.framework.Bundle jettyBundle= null;
         for (org.osgi.framework.Bundle bundle : bundles) {
             if ("org.ops4j.pax.web.pax-web-jetty-bundle".equals(bundle.getSymbolicName())) {
                 jettyBundle = bundle;
                 logger.trace("org.ops4j.pax.web.pax-web-jetty-bundle state: {}", bundle.getState());
             }
         }
         for (org.osgi.framework.Bundle bundle : bundles) {
             if ("org.forgerock.openidm.jetty-fragment".equals(bundle.getSymbolicName())) {
                 Object expectNull = bundle.getResource("ForceResolve");
                 logger.trace("Fragment state after attempted resolve: {}", bundle.getState());
                 if (bundle.getState() != 4) {
                     jettyBundle.update();
                     jettyBundle.getResource("ForceResolve");
                     logger.debug("Fragment state after Jetty bundle refresh {}", bundle.getState());
                 }
             }
         }
     }
}
