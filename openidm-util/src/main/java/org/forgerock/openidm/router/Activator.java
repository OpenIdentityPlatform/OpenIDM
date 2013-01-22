/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

import java.util.Dictionary;
import java.util.Hashtable;

import org.forgerock.json.resource.InMemoryBackend;
import org.forgerock.json.resource.Router;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
//TODO Delete this when openidm-core is migrated
public class Activator implements BundleActivator {

    private AnonymousClass a = null;

    private ServiceRegistration factoryServiceRegistration = null;
    private ServiceRegistration<Router> routerServiceRegistration = null;

    @Override
    public void start(BundleContext context) throws Exception {
        a = new AnonymousClass(context);
        Dictionary<String, Object> properties = new Hashtable<String, Object>(5);
        properties.put(Constants.SERVICE_DESCRIPTION, "Router route group service");
        properties.put(Constants.SERVICE_VENDOR, ServerConstants.SERVER_VENDOR_NAME);
        properties.put(Constants.SERVICE_PID, RouterRegistryService.class.getName());

        factoryServiceRegistration =
                context.registerService(RouterRegistryService.class.getName(), a, properties);

        properties = new Hashtable<String, Object>(5);
        properties.put(Constants.SERVICE_DESCRIPTION, "Router route group service");
        properties.put(Constants.SERVICE_VENDOR, ServerConstants.SERVER_VENDOR_NAME);

        routerServiceRegistration = context.registerService(Router.class,a.getInternalRouter(),properties);

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (null != a) {
            a.deactivate();
        }
        if (null != factoryServiceRegistration) {
            factoryServiceRegistration.unregister();
        }
        if (null != routerServiceRegistration) {
            routerServiceRegistration.unregister();
        }
    }

    //TODO Replace this later
    private class AnonymousClass extends AbstractRouterRegistry {

        private AnonymousClass(BundleContext context) {
            super(context);
        }

        @Override
        protected void activate() {
            isActive.compareAndSet(false,true);
            getInternalRouter();
            resourceTracker.open();
        }
    }

}
