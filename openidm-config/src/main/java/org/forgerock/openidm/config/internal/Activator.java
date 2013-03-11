/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.config.internal;

import org.forgerock.json.resource.RequestHandler;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.logging.LogServiceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * OSGi bundle activator
 * 
 * @author aegloff
 */
public class Activator implements BundleActivator {

    /**
     * Setup logging for the {@link Activator}.
     */
    final static Logger logger = LoggerFactory.getLogger(Activator.class);

    private LogServiceTracker logServiceTracker = null;

    private ConfigurationManagerImpl configurationManager = null;
    
    private ServiceRegistration<RequestHandler> configHandler = null;

    public void start(final BundleContext context) {
        logger.debug("Config Bundle starting");
        // Re-direct OSGi logging to the same OpenIDM log
        logServiceTracker = new LogServiceTracker(context);
        logServiceTracker.open();
        logger.debug("Log handler service registered");

        configurationManager = new ConfigurationManagerImpl(context);
        configurationManager.start();

        Dictionary<String, Object> properties = new Hashtable<String, Object>(5);
        properties.put(Constants.SERVICE_VENDOR, ServerConstants.SERVER_VENDOR_NAME);
        properties.put(Constants.SERVICE_DESCRIPTION, "OpenIDM configuration service");
        properties.put(ServerConstants.ROUTER_PREFIX, "/config*");

        configHandler = context.registerService(RequestHandler.class, new ConfigObjectService(configurationManager),properties);

        // Initiate configuration bootstrapping by registering the repository
        // based
        // persistence manager plug-in to store and manipulate configuration
        // Hashtable<String, String> persistenceProp = new Hashtable<String,
        // String>();
        // persistenceProp.put(ConfigurationPlugin.CM_RANKING, "0");
        // RepoPersistenceManager persistenceMgr = new
        // RepoPersistenceManager(context);
        // context.registerService(new String[]
        // {PersistenceManager.class.getName(),
        // ConfigPersisterMarker.class.getName()},
        // persistenceMgr, persistenceProp);
        // logger.debug("Repository persistence manager service registered");

        // osgiLogHandlerServiceRegistration =
        // context.registerService(LogServiceTracker.class, new
        // LogServiceTracker(context), logHandlerProp);

        // Register the optional "file view" handling of configuration
        // It will not start polling configuration files until configuration for
        // it is set

        logger.info("OpenIDM is starting from {}", IdentityServer.getInstance().getServerRoot());
    }

    public void stop(BundleContext context) {
        if (null != configHandler) {
            configHandler.unregister();
            configHandler = null;
        }
        
        if (null != logServiceTracker) {
            logServiceTracker.close();
            logServiceTracker = null;
        }

        if (null != configurationManager) {
            configurationManager.stop();
            configurationManager = null;
        }
        logger.debug("Config Bundle stopped");
    }
}
