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
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.Router.UriTemplate;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.Constants;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/**
 * A RouteBuilder does ...
 *
 */
public final class RouteBuilder {

    private final Set<RouteItem> routes;

    private CollectionResourceProvider collection;
    private SingletonResourceProvider singleton;
    private RequestHandler handler;
    private RoutingMode mode = RoutingMode.EQUALS;
    private UriTemplate uriTemplate;

    private RouteBuilder() {
        this.routes = new HashSet<>();
    }

    private RouteBuilder(Set<RouteItem> initial) {
        this.routes = initial;
    }

    public static RouteBuilder newBuilder() {
        return new RouteBuilder();
    }

    public RouteBuilder withService(Object service) {
        if (service instanceof CollectionResourceProvider) {
            return withCollectionResourceProvider((CollectionResourceProvider) service);
        } else if (service instanceof SingletonResourceProvider) {
            return withSingletonResourceProvider((SingletonResourceProvider) service);
        } else if (service instanceof RequestHandler) {
            return withRequestHandler((RequestHandler) service);
        }
        return this;
    }

    public RouteBuilder withServiceAndTemplate(Object service, String uriTemplate) {
        return bind(service, uriTemplate);
    }

    public RouteBuilder withServiceAndTemplate(Object service, String[] uriTemplate) {
        return bind(service, uriTemplate);
    }

    public RouteBuilder bind(Object service, Object routerPrefix) {
        if (null == routerPrefix || null == service) {
            return this;
        }
        if (routerPrefix.getClass().isArray()) {
            if (routerPrefix.getClass().getComponentType() == String.class) {
                for (String pattern : (String[]) routerPrefix) {
                    withService(service).withTemplate(pattern).buildNext();
                }
            }
        } else if (routerPrefix instanceof String) {
            withService(service).withTemplate((String) routerPrefix).buildNext();
        }
        return this;
    }

    public RouteBuilder withTemplate(String uriTemplate) {
        if (uriTemplate != null && uriTemplate.length() > 0) {
            if ('*' == uriTemplate.charAt(uriTemplate.length() - 1)) {
                withModeStartsWith().withTemplate(
                        uriTemplate.substring(0, uriTemplate.length() - 1));
            } else {
                this.uriTemplate = Router.uriTemplate(uriTemplate);
            }
        }
        return this;
    }

    public RouteBuilder verify() {
        if ((null == collection) && (null == singleton) && (null == handler)) {
            throw new NullPointerException("Failed because the service is not set");
        }
        if (uriTemplate == null || uriTemplate.toString().length() == 0) {
            throw new NullPointerException("Failed because the uriTemplate is not set");
        }
        return this;
    }

    public RouteBuilder buildNext() {
        try {
            verify();
            routes.add(new RouteItem(collection, singleton, handler, mode, uriTemplate));
        } catch (NullPointerException e) {
            /* ignore */
        }
        this.collection = null;
        this.singleton = null;
        this.handler = null;
        this.uriTemplate = null;
        return withModeEquals();
    }

    public boolean isNotEmpty() {
        return !routes.isEmpty();
    }

    public RouteBuilder withModeEquals() {
        this.mode = RoutingMode.EQUALS;
        return this;
    }

    public RouteBuilder withModeStartsWith() {
        this.mode = RoutingMode.STARTS_WITH;
        return this;
    }

    public RouteBuilder seal() {
        return new RouteBuilder(Collections.unmodifiableSet(buildNext().routes));
    }

    public Dictionary<String, Object> buildServiceProperties() {
        if (routes.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("Route group of {");
        Dictionary<String, Object> properties = new Hashtable<>(5);
        properties.put(Constants.SERVICE_VENDOR, ServerConstants.SERVER_VENDOR_NAME);
        if (routes.size() == 1) {
            String template = routes.iterator().next().uriTemplate.toString();
            sb.append(template);
            properties.put(ServerConstants.ROUTER_PREFIX, template);
        } else if (routes.size() > 1) {
            Object[] params = routes.toArray();
            for (int i = 0; i < params.length; i++) {
                params[i] = ((RouteItem) params[i]).uriTemplate.toString();
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(params[i]);
            }
            properties.put(ServerConstants.ROUTER_PREFIX, params);
        }
        properties.put(Constants.SERVICE_DESCRIPTION, sb.append("}").toString());
        return properties;
    }

    public RouteBuilder withCollectionResourceProvider(CollectionResourceProvider service) {
        this.collection = service;
        this.singleton = null;
        this.handler = null;
        return this;
    }

    public RouteBuilder withSingletonResourceProvider(SingletonResourceProvider service) {
        this.collection = null;
        this.singleton = service;
        this.handler = null;
        return this;
    }

    public RouteBuilder withRequestHandler(RequestHandler service) {
        this.collection = null;
        this.singleton = null;
        this.handler = service;
        return this;
    }

    public RouteMatcher[] register(Router router) {
        if (routes.isEmpty()) {
            return new RouteMatcher[0];
        }
        Set<RouteMatcher> registered = new HashSet<>();
        for (RouteItem r : routes) {
            RouteMatcher o = r.register(router);
            if (null != o) {
                registered.add(o);
            }
        }
        return registered.toArray(new RouteMatcher[registered.size()]);
    }

    private static class RouteItem {
        private final CollectionResourceProvider collection;
        private final SingletonResourceProvider singleton;
        private final RequestHandler handler;
        private final RoutingMode mode;
        private final UriTemplate uriTemplate;

        private RouteItem(CollectionResourceProvider collection,
                SingletonResourceProvider singleton, RequestHandler handler, RoutingMode mode,
                UriTemplate uriTemplate) {
            this.collection = collection;
            this.singleton = singleton;
            this.handler = handler;
            this.mode = mode;
            this.uriTemplate = uriTemplate;
        }

        private RouteMatcher register(Router router) {
            if (null != router) {
                if (null != collection) {
                    return router.addRoute(uriTemplate, collection);
                } else if (null != singleton) {
                    return router.addRoute(uriTemplate, singleton);
                } else if (null != handler) {
                    return router.addRoute(mode, uriTemplate, handler);
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            RouteItem routeItem = (RouteItem) o;

            if (collection != null ? !collection.equals(routeItem.collection)
                    : routeItem.collection != null) {
                return false;
            }
            if (handler != null ? !handler.equals(routeItem.handler) : routeItem.handler != null) {
                return false;
            }
            if (mode != routeItem.mode) {
                return false;
            }
            if (singleton != null ? !singleton.equals(routeItem.singleton)
                    : routeItem.singleton != null) {
                return false;
            }
            if (!uriTemplate.equals(routeItem.uriTemplate)) {
                return false;
            }

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
