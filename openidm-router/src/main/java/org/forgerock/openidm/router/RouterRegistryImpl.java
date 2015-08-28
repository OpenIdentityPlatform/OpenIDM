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

import org.forgerock.http.routing.RouteMatcher;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RouterRegistryImpl implements ServiceFactory<RouterRegistry>,
        ServiceTrackerCustomizer<Object, RouteEntryImpl> {

    /**
     * Setup logging for the {@link RouterRegistryImpl}.
     */
    final static Logger logger = LoggerFactory.getLogger(RouterRegistryImpl.class);

    protected final BundleContext context;

    protected final ServiceTracker<Router, Router> routerTracker;

    protected ServiceTracker<Object, RouteEntry> resourceTracker;

    protected final AtomicReference<Router> internalRouter = new AtomicReference<Router>();

    protected final AtomicBoolean isActive = new AtomicBoolean(Boolean.FALSE);

    protected final CopyOnWriteArraySet<RouteEntry> routeCache =
            new CopyOnWriteArraySet<RouteEntry>();

    // the routerTracker.getTrackingCount when the router were last got
    protected int routerCount;

    protected RouterRegistryImpl(BundleContext context) {
        this(context, null);
    }

    protected RouterRegistryImpl(BundleContext context, ServiceTracker<Router, Router> routerTracker) {
        if (null == context) {
            throw new NullPointerException("Failure the BundleContext value is null.");
        }
        this.context = context;
        this.routerTracker = routerTracker;

        String flt =
                "(|" + "(" + Constants.OBJECTCLASS + "=" + CollectionResourceProvider.class.getName() + ")"
                     + "(" + Constants.OBJECTCLASS + "=" + SingletonResourceProvider.class.getName() + ")"
                     + "(" + Constants.OBJECTCLASS + "=" + RequestHandler.class.getName() + ")"
                + ")";
        try {
            resourceTracker = new ServiceTracker(context, FrameworkUtil.createFilter(flt), this);
        } catch (InvalidSyntaxException e) {
            // not expected (filter is tested valid)
        }
        activate();
    }

    protected void activate() {
        isActive.compareAndSet(false, true);
        getInternalRouter();
        resourceTracker.open();
    }

    protected void deactivate() {
        isActive.compareAndSet(true, false);
        for (final RouteEntry entry : routeCache) {
            // Entries in cache has circular references to the cache set to
            // remove themselves
            entry.removeRoute();
        }
        resourceTracker.close();
    }

    public RouteEntryImpl addRoute(final Bundle source, RouteBuilder routeBuilder) {
        return new RouteEntryImpl(context, source, internalRouter, routeBuilder);
    }

    public RouteEntryImpl addRouteCustom(final Bundle source, RouteBuilder routeBuilder) {
        final RouteEntryImpl self =
                new RouteEntryImpl(context, source, internalRouter, routeBuilder) {
                    @Override
                    public boolean removeRoute() {
                        routeCache.remove(this);
                        return super.removeRoute();
                    }
                };
        routeCache.add(self);
        return self;
    }

    public RouteEntryImpl addRoute(ServiceReference<Object> reference, Object service) {
        try {
            RouteBuilder newRoutes =
                    RouteBuilder.newBuilder().bind(service,
                            reference.getProperty(ServerConstants.ROUTER_PREFIX)).seal();
            if (newRoutes.isNotEmpty()) {
                return addRoute(reference.getBundle(), newRoutes);
            }
        } catch (NullPointerException e) {
            logger.debug("There was not service to register in {}", reference);
        }
        return null;
    }

    // ----- Implementation of ServiceFactory interface

    @Override
    public RouterRegistry getService(Bundle bundle,
            ServiceRegistration<RouterRegistry> registration) {
        logger.debug("getService RouterRegistryService {}", bundle);
        return new RouterRegistryServiceImpl(bundle, this);
    }

    @Override
    public void ungetService(Bundle bundle,
            ServiceRegistration<RouterRegistry> registration, RouterRegistry service) {
        logger.debug("ungetService RouterRegistryService {}", bundle);
        ((RouterRegistryServiceImpl) service).dispose();
    }

    // ----- Implementation of ServiceTrackerCustomizer interface

    public RouteEntryImpl addingService(ServiceReference<Object> reference) {
        Object service = context.getService(reference);
        RouteEntryImpl result = null;
        if (service instanceof CollectionResourceProvider) {
            result = addRoute(reference, service);
        } else if (service instanceof SingletonResourceProvider) {
            result = addRoute(reference, service);
        } else if (service instanceof RequestHandler) {
            result = addRoute(reference, service);
        }
        if (null == result) {
            context.ungetService(reference);
        }
        return result;
    }

    public void modifiedService(ServiceReference<Object> reference, RouteEntryImpl service) {
        service.removeRoute();
        Object newService = context.getService(reference);
        RouteEntry result = null;
        if (newService instanceof CollectionResourceProvider) {
            result = addRoute(reference, newService);
        } else if (newService instanceof SingletonResourceProvider) {
            result = addRoute(reference, newService);
        } else if (newService instanceof RequestHandler) {
            result = addRoute(reference, newService);
        }
        if (null == result) {
            context.ungetService(reference);
        }
    }

    public void removedService(ServiceReference<Object> reference, RouteEntryImpl service) {
        context.ungetService(reference);
        service.removeRoute();
    }

    // ----- Protected methods meant to be overwritten

    protected Router getInternalRouter() {
        // The getTrackingCount() is synchronized, avoid it if it's possible
        if (internalRouter.get() == null
                || (routerTracker != null && routerTracker.getTrackingCount() > routerCount)) {
            if (routerTracker == null) {
                internalRouter.set(new Router());
            } else {

                ServiceReference[] refs = routerTracker.getServiceReferences();
                if (refs == null || refs.length == 0) {
                    internalRouter.set(new Router());
                } else {
                    for (int i = 0; i < refs.length; i++) {
                        Router service = routerTracker.getService(refs[i]);
                        if (service != null) {
                            internalRouter.set(service);
                            routerCount = routerTracker.getTrackingCount();
                            break;
                        }
                    }
                }

            }
        }
        return internalRouter.get();
    }

    protected static class RouterRegistryServiceImpl implements RouterRegistry {

        protected Bundle bundle;
        protected RouterRegistryImpl registry;

        public RouterRegistryServiceImpl(Bundle bundle, RouterRegistryImpl registry) {
            this.bundle = bundle;
            this.registry = registry;
        }

        void dispose() {
            registry = null;
            bundle = null;
        }

        @Override
        public RouteEntry addRoute(RouteBuilder routeBuilder) {
            RouteEntry entry = null;
            if (registry.isActive.get()) {
                entry = registry.addRouteCustom(bundle, routeBuilder);
            }
            return entry;
        }
    }

}

class RouteServiceImpl implements RouteService {

    protected Bundle bundle;

    protected final AtomicReference<Router> internalRouter;

    RouteServiceImpl(Bundle bundle, final AtomicReference<Router> router) {
        this.bundle = bundle;
        this.internalRouter = router;
    }

    void dispose() {
        bundle = null;
    }
}

class RouteEntryImpl extends RouteServiceImpl implements RouteEntry {

    protected RouteMatcher[] registeredRoutes;
    protected ServiceRegistration factoryServiceRegistration;

    RouteEntryImpl(BundleContext parent, Bundle bundle, final AtomicReference<Router> router, RouteBuilder builder) {
        super(bundle, router);

        final Router r = router.get();
        if (r == null) {
            throw new NullPointerException("Router is required to register the routes to");
        }
        registeredRoutes = builder.register(r);

        if (registeredRoutes.length > 0) {
            Dictionary<String, Object> props = builder.buildServiceProperties();
            props.put(Constants.SERVICE_PID, RouteService.class.getName());
            factoryServiceRegistration =
                    parent.registerService(RouteService.class.getName(), new RouteServiceFactory(router), props);
        }
    }

    void dispose() {
        removeRoute();
        registeredRoutes = null;
        bundle = null;
        super.dispose();
    }

    public boolean removeRoute() {
        boolean isModified = false;
        try {
            if (null != factoryServiceRegistration) {
                factoryServiceRegistration.unregister();
                factoryServiceRegistration = null;
            }
        } catch (IllegalStateException e) {
            /* Catch if the service was already removed */
            factoryServiceRegistration = null;
        } finally {

            final Router r = internalRouter.get();
            if (r != null) {
                isModified = r.removeRoute(registeredRoutes);
            }
        }
        return isModified;
    }
}

class RouteServiceFactory implements ServiceFactory<RouteService> {

    final static Logger logger = LoggerFactory.getLogger(RouteServiceFactory.class);
    
    protected final AtomicReference<Router> internalRouter;

    RouteServiceFactory(final AtomicReference<Router> internalRouter) {
        this.internalRouter = internalRouter;
    }

    public RouteService getService(Bundle bundle, ServiceRegistration<RouteService> registration) {
        logger.debug("getService RouteService {}", bundle);
        return new RouteServiceImpl(bundle, internalRouter);
    }

    public void ungetService(Bundle bundle, ServiceRegistration<RouteService> registration, RouteService service) {
        logger.debug("ungetService RouteService {}", bundle);
        ((RouteServiceImpl) service).dispose();
    }
}
