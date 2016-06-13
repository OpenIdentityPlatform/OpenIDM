/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013-2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.router;

import org.forgerock.json.resource.RequestHandler;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

public class Activator implements BundleActivator {

    private RouterRegistryImpl routerRegistry;

    private ServiceRegistration<RouterRegistry> factoryServiceRegistration = null;
    private ServiceRegistration<RequestHandler> routerServiceRegistration = null;

    @Override
    public void start(BundleContext context) throws Exception {
        routerRegistry = new RouterRegistryImpl(context);
        Dictionary<String, Object> properties = new Hashtable<String, Object>(5);
        properties.put(Constants.SERVICE_DESCRIPTION, "Router route group service");
        properties.put(Constants.SERVICE_VENDOR, ServerConstants.SERVER_VENDOR_NAME);
        properties.put(Constants.SERVICE_PID, RouterRegistry.class.getName());

        factoryServiceRegistration =
                context.registerService(RouterRegistry.class, routerRegistry, properties);

        properties = new Hashtable<>(5);
        properties.put(Constants.SERVICE_DESCRIPTION, "Router route group service");
        properties.put(Constants.SERVICE_VENDOR, ServerConstants.SERVER_VENDOR_NAME);
        properties.put("org.forgerock.openidm.router", "true");

        routerServiceRegistration =
                context.registerService(RequestHandler.class, routerRegistry.getInternalRouter(), properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (null != routerRegistry) {
            routerRegistry.deactivate();
        }
        if (null != factoryServiceRegistration) {
            factoryServiceRegistration.unregister();
            factoryServiceRegistration = null;
        }
        if (null != routerServiceRegistration) {
            routerServiceRegistration.unregister();
            routerServiceRegistration = null;
        }
    }
}
