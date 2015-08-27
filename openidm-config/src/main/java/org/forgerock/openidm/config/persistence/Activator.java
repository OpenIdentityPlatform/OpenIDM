/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS. All Rights Reserved
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
 *
 */
package org.forgerock.openidm.config.persistence;

import java.util.Hashtable;

import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.forgerock.openidm.config.installer.JSONConfigInstaller;
import org.forgerock.openidm.config.paxweb.PaxWeb;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.logging.OsgiLogHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi bundle activator
 */
public class Activator implements BundleActivator {
    final static Logger logger = LoggerFactory.getLogger(Activator.class);

    JSONConfigInstaller installer;
    
    public void start(BundleContext context) {
        logger.debug("Config Bundle starting");
        // Re-direct OSGi logging to the same openidm log
        Hashtable<String, String> logHandlerProp = new Hashtable<String, String>();
        logHandlerProp.put("service.description", "OpenIDM OSGi log handler");
        logHandlerProp.put("service.vendor", "ForgeRock AS");
        OsgiLogHandler logHandler = new OsgiLogHandler(context);
        context.registerService(OsgiLogHandler.class.getName(), logHandler, logHandlerProp);
        logger.debug("Log handler service registered");

        // Initiate configuration bootstrapping by registering the repository based
        // persistence manager plug-in to store and manipulate configuration
        Hashtable<String, String> persistenceProp = new Hashtable<String, String>();
        persistenceProp.put("service.cmRanking", "0");
        RepoPersistenceManager persistenceMgr = new RepoPersistenceManager(context);
        context.registerService(new String[] {PersistenceManager.class.getName(), ConfigPersisterMarker.class.getName()},
                persistenceMgr, persistenceProp);
        logger.debug("Repository persistence manager service registered");

        // Register the optional "file view" handling of configuration
        // It will not start polling configuration files until configuration for it is set
        JSONConfigInstaller installer = new JSONConfigInstaller();
        installer.start(context);
        Hashtable<String, String> installerProp = new Hashtable<String, String>();
        installerProp.put("service.description", "Config installer for JSON files");
        context.registerService(new String[] {ArtifactInstaller.class.getName(), ConfigurationListener.class.getName()}, 
                 installer, installerProp);
        logger.debug("JSON configuration installer service registered");

        // Configure pax web properties
        PaxWeb.configurePaxWebProperties();

        logger.info("OpenIDM is starting from {}", IdentityServer.getInstance().getServerRoot());
    }

    public void stop(BundleContext context) {
        if (installer != null) {
            installer.stop(context);
        }
        logger.debug("Config Bundle stopped");
    }
}