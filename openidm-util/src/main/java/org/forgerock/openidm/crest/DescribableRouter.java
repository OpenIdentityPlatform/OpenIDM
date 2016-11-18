/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.crest;

import static org.forgerock.json.resource.Resources.newHandler;
import static org.forgerock.json.resource.RouteMatchers.requestUriMatcher;

import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.services.routing.RouteMatcher;

/**
 * Extends the CREST Router to allow the annotation of {@link RequestHandler RequestHandlers}. This is necessary until
 * RAPID-209 is resolved at the crest level.
 */
public class DescribableRouter extends Router {

    public DescribableRouter() {
        super();
    }

    /**
     * Processes the annotation of the {@link RequestHandler} if the request handler is annotated with the
     * {@link org.forgerock.api.annotations.RequestHandler} annotation.
     *
     * {@inheritDoc}
     */
    @Override
    public RouteMatcher<Request> addRoute(RoutingMode mode, UriTemplate uriTemplate, RequestHandler handler) {
        RouteMatcher<Request> routeMatcher = requestUriMatcher(mode, uriTemplate.toString());
        addRoute(routeMatcher, processHandlerAnnotations(handler));
        return routeMatcher;
    }

    private RequestHandler processHandlerAnnotations(final RequestHandler handler) {
        if (handler != null) {
            return handler.getClass().isAnnotationPresent(org.forgerock.api.annotations.RequestHandler.class)
                    ? newHandler(handler)
                    : handler;
        }
        return null;
    }
}
