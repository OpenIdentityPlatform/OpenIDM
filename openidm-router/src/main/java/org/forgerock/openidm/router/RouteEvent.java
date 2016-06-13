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

import org.forgerock.json.resource.Request;
import org.forgerock.services.routing.RouteMatcher;
import org.forgerock.json.resource.Router;

import java.util.EventObject;

/**
 * A NAME does ...
 *
 */
public class RouteEvent extends EventObject {

    static final long serialVersionUID = 1L;
    /**
     * Reference to the service that had a change occur in its lifecycle.
     */
    private final transient RouteMatcher<Request> routeMatcher;

    private final transient Router router;

    /**
     * Type of service lifecycle change.
     */
    private final int type;

    /**
     * This route has been registered.
     * <p>
     * This event is synchronously delivered <strong>after</strong> the route
     * has been registered with the Router.
     *
     */
    public final static int REGISTERED = 0x00000001;

    /**
     * This route is in the process of being unregistered.
     * <p>
     * This event is synchronously delivered <strong>before</strong> the route
     * has completed unregistering.
     *
     */
    public final static int UNREGISTERING = 0x00000004;

    /**
     * Creates a new service event object.
     *
     * @param type
     *            The event type.
     * @param router
     *            A {@code Router} object where a change had occurred in.
     * @param routeMatcher
     *            A {@code RouteMatcher} object that had a lifecycle change.
     */
    public RouteEvent(int type, final Router router, final RouteMatcher<Request> routeMatcher) {
        super(routeMatcher);
        this.routeMatcher = routeMatcher;
        this.router = router;
        this.type = type;
    }

    /**
     * Returns a {@code Route} that had a change occur in its lifecycle.
     * <p>
     * This {@code Route} is the source of the event.
     *
     * @return {@code Route} that had a lifecycle change.
     */
    public RouteMatcher<Request> getRouteMatcher() {
        return routeMatcher;
    }

    /**
     * Returns a {@code Router} where a change had occurred in.
     * <p>
     *
     * @return {@code Router} that had the change.
     */
    public Router getRouter() {
        return router;
    }

    /**
     * Returns the type of event. The event type values are:
     * <ul>
     * <li>{@link #REGISTERED}</li>
     * <li>{@link #UNREGISTERING}</li>
     * </ul>
     *
     * @return Type of service lifecycle change.
     */

    public int getType() {
        return type;
    }
}
