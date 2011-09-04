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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.restlet;

// Restlet Framework
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
public class Application extends org.restlet.Application {

    /**
     * TODO: Description.
     */
    public Application() {
        setName("OpenIDM");
        getTunnelService().setQueryTunnel(false); // query string purism
    }

    /**
     * TODO: Description.
     *
     * @param path
     * @param restlet TODO.
     */
    void attach(String path, Restlet restlet) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Illegal path");
        }
        restlet.setContext(getContext()); // set to this application's context
        Router router = (Router)getInboundRoot();
        router.attach((String)path, restlet, Template.MODE_EQUALS); // request for object set itself
        router.attach((String)path + (path.equals("/") ? "" : "/"), restlet, Template.MODE_STARTS_WITH); // object within set
    }
    
    /**
     * TODO: Description.
     *
     * @param restlet TODO.
     */
    void detach(Restlet restlet) {
        Router router = (Router)getInboundRoot();
        router.detach(restlet); // all routes to restlet are removed
    }

    @Override
    public Restlet createInboundRoot() {
        return new Router(); // will be populated by attachObjectSetFinder
    }
}
