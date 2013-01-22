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

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Route;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.Constants;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
//TODO Fix the fluent methods
public class RouteBuilder {

    private final Set<RouteItem> routes;

    private CollectionResourceProvider collection;
    private SingletonResourceProvider singleton;
    private RequestHandler handler;
    private RoutingMode mode = RoutingMode.EQUALS;
    private String uriTemplate;

    private RouteBuilder() {
        this.routes = new HashSet<RouteItem>();
    }

    private RouteBuilder(Set<RouteItem> initial) {
        this.routes = initial;
    }

    public static RouteBuilder instance() {
        return new RouteBuilder();
    }

    public RouteBuilder bindService(Object service) {
        if (service instanceof CollectionResourceProvider) {
            return bindCollectionResourceProvider((CollectionResourceProvider) service);
        } else if (service instanceof SingletonResourceProvider) {
            return bindSingletonResourceProvider((SingletonResourceProvider) service);
        } else if (service instanceof RequestHandler) {
            return bindRequestHandler((RequestHandler) service);
        }
        return this;
    }

    public RouteBuilder bind(Object service, Object routerPrefix) {
        if (null == routerPrefix || null == service) {
            return this;
        }
        if (routerPrefix.getClass().isArray()) {
            if (routerPrefix.getClass().getComponentType() == String.class) {
                for (String pattern : (String[]) routerPrefix) {
                    bindService(service).parseURITemplate(pattern).next();
                }
            }
        } else if (routerPrefix instanceof String) {
            bindService(service).parseURITemplate((String) routerPrefix).next();
        }
        return this;
    }

    public RouteBuilder parseURITemplate(String uriTemplate) {
        if (StringUtils.isNotBlank(uriTemplate)) {
            if ('*' == uriTemplate.charAt(uriTemplate.length() - 1)) {
                modeStartsWith().parseURITemplate(
                        uriTemplate.substring(0, uriTemplate.length() - 1));
            } else {
                this.uriTemplate = uriTemplate;
            }
        }
        return this;
    }

    public RouteBuilder verify() {
        if ((null == collection) && (null == singleton) && (null == handler)) {
            throw new NullPointerException("Failed because the service is not set");
        }
        if (StringUtils.isBlank(uriTemplate)) {
            throw new NullPointerException("Failed because the uriTemplate is not set");
        }
        return this;
    }

    public RouteBuilder next() {
        try {
            verify();
            routes.add(new RouteItem(collection, singleton, handler, mode, uriTemplate));
        } catch (NullPointerException e) {

        }
        this.collection = null;
        this.singleton = null;
        this.handler = null;
        this.uriTemplate = null;
        return modeEquals();
    }

    public boolean isNotEmpty() {
        return !routes.isEmpty();
    }

    public RouteBuilder modeEquals() {
        this.mode = RoutingMode.EQUALS;
        return this;
    }

    public RouteBuilder modeStartsWith() {
        this.mode = RoutingMode.STARTS_WITH;
        return this;
    }

    public RouteBuilder seal() {
        return new RouteBuilder(Collections.unmodifiableSet(routes));
    }

    public Dictionary<String, Object> getProperties() {
        if (routes.isEmpty()) {
            return null;
        }
        Dictionary<String, Object> properties = new Hashtable<String, Object>(5);
        properties.put(Constants.SERVICE_DESCRIPTION, "Router route group service");
        properties.put(Constants.SERVICE_VENDOR, ServerConstants.SERVER_VENDOR_NAME);
        if (routes.size() == 1) {
            properties.put(ServerConstants.ROUTER_PREFIX, routes.iterator().next().uriTemplate);
        } else if (routes.size() > 1) {
            Object[] params = routes.toArray();
            for (int i = 0; i < params.length; i++) {
                params[i] = ((RouteItem) params[i]).uriTemplate;
            }
            properties.put(ServerConstants.ROUTER_PREFIX, params);
        }
        return properties;
    }

    public RouteBuilder bindCollectionResourceProvider(CollectionResourceProvider service) {
        this.collection = service;
        this.singleton = null;
        this.handler = null;
        return this;
    }

    public RouteBuilder bindSingletonResourceProvider(SingletonResourceProvider service) {
        this.collection = null;
        this.singleton = service;
        this.handler = null;
        return this;
    }

    public RouteBuilder bindRequestHandler(RequestHandler service) {
        this.collection = null;
        this.singleton = null;
        this.handler = service;
        return this;
    }

    public Route[] register(Router router) {
        if (routes.isEmpty()) {
            return new Route[0];
        }
        Set<Route> registered = new HashSet<Route>();
        for (RouteItem r : routes) {
            Route o = r.register(router);
            if (null != o) {
                registered.add(o);
            }
        }
        return registered.toArray(new Route[registered.size()]);
    }

    private class RouteItem {
        private final CollectionResourceProvider collection;
        private final SingletonResourceProvider singleton;
        private final RequestHandler handler;
        private final RoutingMode mode;
        private final String uriTemplate;

        private RouteItem(CollectionResourceProvider collection,
                SingletonResourceProvider singleton, RequestHandler handler, RoutingMode mode,
                String uriTemplate) {
            this.collection = collection;
            this.singleton = singleton;
            this.handler = handler;
            this.mode = mode;
            this.uriTemplate = uriTemplate;
        }

        private Route register(Router router) {
            if (null != router) {
                if (null != collection) {
                    return router.addRoute(mode, uriTemplate, collection);
                } else if (null != singleton) {
                    return router.addRoute(mode, uriTemplate, singleton);
                } else if (null != handler) {
                    return router.addRoute(mode, uriTemplate, handler);
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            RouteItem routeItem = (RouteItem) o;

            if (collection != null ? !collection.equals(routeItem.collection)
                    : routeItem.collection != null)
                return false;
            if (handler != null ? !handler.equals(routeItem.handler) : routeItem.handler != null)
                return false;
            if (mode != routeItem.mode)
                return false;
            if (singleton != null ? !singleton.equals(routeItem.singleton)
                    : routeItem.singleton != null)
                return false;
            if (!uriTemplate.equals(routeItem.uriTemplate))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = collection != null ? collection.hashCode() : 0;
            result = 31 * result + (singleton != null ? singleton.hashCode() : 0);
            result = 31 * result + (handler != null ? handler.hashCode() : 0);
            result = 31 * result + mode.hashCode();
            result = 31 * result + uriTemplate.hashCode();
            return result;
        }
    }
}
