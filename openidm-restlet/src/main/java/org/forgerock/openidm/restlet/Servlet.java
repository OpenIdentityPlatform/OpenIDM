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

// Java SE
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

// Java Servlet
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// OSGi
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

// Felix SCR
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Restlet
import org.restlet.Restlet;

// JSON Resource
import org.forgerock.json.resource.JsonResource;

// JSON Resource Restlet
import org.forgerock.json.resource.restlet.JsonResourceRestlet;

// Restlet Utilities
import org.forgerock.restlet.RestletRouterServlet;

// Deprecated
import org.forgerock.openidm.objset.ObjectSetContext;

/**
 * Servlet to handle the REST interface.
 *
 * @author Paul C. Bryan
 * @author aegloff
 */
@Component(
    name = "org.forgerock.openidm.restlet",
    immediate = true,
    policy = ConfigurationPolicy.IGNORE
)
public class Servlet extends RestletRouterServlet {

    /** TODO: Description. */
    final static Logger LOGGER = LoggerFactory.getLogger(Servlet.class);
    
    /** TODO: Description. */
    private static final String PATH_PROPERTY = "openidm.restlet.path";

    /** TODO: Description. */
    private ComponentContext context;

    /**
     * OSGi http service. May be backed by an embedded felix; or may for example come from the java ee container.
     */
    @Reference 
    HttpService httpService;
    
    @Reference(target="(openidm.contextid=shared)")
    HttpContext httpContext;
    
    /**
     * Provides automatic binding of {@link Restlet} objects that include the
     * {@code openidm.restlet.path} property.
     */
    @Reference(
        name = "reference_Servlet_Restlet",
        referenceInterface = Restlet.class,
        bind = "bindRestlet",
        unbind = "unbindRestlet",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        strategy = ReferenceStrategy.EVENT
    )
    protected int _dummy_Restlets; // whiteboard pattern
    protected synchronized void bindRestlet(Restlet restlet, Map<String, Object> properties) {
        Object path = properties.get(PATH_PROPERTY);
        if (path != null && path instanceof String) { // service is specified as internally routable
            attach((String)path, restlet);
        }
    }
    protected synchronized void unbindRestlet(Restlet restlet, Map<String, Object> properties) {
        Object path = properties.get(PATH_PROPERTY);
        if (path != null && path instanceof String) { // service is specified as internally routable
            detach(restlet);
        }
    }

    /**
     * Provides automatic binding of {@code JsonResource} objects that include the
     * {@code openidm.restlet.path} property.
     */
    @Reference(
        name = "reference_Servlet_JsonResource",
        referenceInterface = JsonResource.class,
        bind = "bindJsonResource",
        unbind = "unbindJsonResource",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        strategy = ReferenceStrategy.EVENT
    )
    protected HashMap<JsonResource, Restlet> restlets = new HashMap<JsonResource, Restlet>();
    protected synchronized void bindJsonResource(JsonResource resource, Map<String, Object> properties) {
        JsonResourceRestlet restlet = new JsonResourceRestlet(resource);
        restlets.put(resource, restlet);
        bindRestlet(restlet, properties);
    }
    protected synchronized void unbindJsonResource(JsonResource resource, Map<String, Object> properties) {
        Restlet restlet = restlets.get(resource);
        if (restlet != null) {
            unbindRestlet(restlet, properties);
            restlets.remove(resource);
        }
    }

    @Activate
    protected synchronized void activate(ComponentContext context) throws ServletException, NamespaceException {
        this.context = context;
        String alias = "/openidm";
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        httpService.registerServlet(alias, this,  props, httpContext);
        LOGGER.debug("Registered servlet at {}", alias);
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        this.context = null;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        ObjectSetContext.clear();
        try {
            super.service(request, response);
        } finally {
            ObjectSetContext.clear();
        }
    }
}
