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

import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionProvider;
import org.forgerock.json.resource.PersistenceConfig;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * A NAME does ...
 *
 */
// TODO Delete this when openidm-core is migrated
public class Activator implements BundleActivator {

    private RouterRegistryImpl routerRegistry;

    private ServiceRegistration factoryServiceRegistration = null;
    private ServiceRegistration<RequestHandler> routerServiceRegistration = null;
    private ServiceRegistration<PersistenceConfig> persistenceConfigRegistration = null;

    @Override
    public void start(BundleContext context) throws Exception {
        routerRegistry = new RouterRegistryImpl(context);
        Dictionary<String, Object> properties = new Hashtable<String, Object>(5);
        properties.put(Constants.SERVICE_DESCRIPTION, "Router route group service");
        properties.put(Constants.SERVICE_VENDOR, ServerConstants.SERVER_VENDOR_NAME);
        properties.put(Constants.SERVICE_PID, RouterRegistry.class.getName());

        factoryServiceRegistration =
                context.registerService(RouterRegistry.class.getName(), routerRegistry, properties);

        properties = new Hashtable<String, Object>(5);
        properties.put(Constants.SERVICE_DESCRIPTION, "Router route group service");
        properties.put(Constants.SERVICE_VENDOR, ServerConstants.SERVER_VENDOR_NAME);
        properties.put("org.forgerock.openidm.router", "true");

        routerServiceRegistration =
                context.registerService(RequestHandler.class, routerRegistry.getInternalRouter(), properties);

        final Connection connection = Resources.newInternalConnection(routerRegistry.getInternalRouter());

        persistenceConfigRegistration =
                context.registerService(PersistenceConfig.class, PersistenceConfig.builder()
                        .connectionProvider(new ConnectionProvider() {
                            @Override
                            public Connection getConnection(String connectionId)
                                    throws ResourceException {
                                return connection;
                            }

                            @Override
                            public String getConnectionId(Connection connection)
                                    throws ResourceException {
                                return "DEFAULT";
                            }
                        }).classLoader(this.getClass().getClassLoader()).build(), properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (null != routerRegistry) {
            routerRegistry.deactivate();
        }
        if (null != persistenceConfigRegistration) {
            persistenceConfigRegistration.unregister();
            persistenceConfigRegistration = null;
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
