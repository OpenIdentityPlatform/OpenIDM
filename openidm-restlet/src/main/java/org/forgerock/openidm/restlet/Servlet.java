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
import java.util.HashMap;
import java.util.Map;

// Java Servlet
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// OSGi
import org.forgerock.openidm.http.ContextRegistrator;
import org.ops4j.pax.web.extender.whiteboard.ServletMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultServletMapping;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.NamespaceException;

// Felix SCR
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Restlet
import org.restlet.Request;
import org.restlet.Restlet;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;

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
@Reference(
        name = "reference_Servlet_Restlet",
        referenceInterface = Restlet.class,
        bind = "bindRestlet",
        unbind = "unbindRestlet",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        strategy = ReferenceStrategy.EVENT
)
public class Servlet extends RestletRouterServlet {

    /** TODO: Description. */
    final static Logger LOGGER = LoggerFactory.getLogger(Servlet.class);
    
    /** TODO: Description. */
    private static final String PATH_PROPERTY = "openidm.restlet.path";

    /** TODO: Description. */
    private ComponentContext context;

    /** TODO: Description. */
    private ServiceRegistration serviceRegistration;
    
    /**
     * Provides automatic binding of {@link Restlet} objects that include the
     * {@code openidm.restlet.path} property.
     */
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
        Restlet restlet = new CustomRestlet(resource);
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
        DefaultServletMapping servletMapping = new DefaultServletMapping();
        servletMapping.setHttpContextId("openidm");
        servletMapping.setAlias(alias);
        servletMapping.setServlet(this);
        servletMapping.setServletName("OpenIDM REST");
        //All WebApplication elements must be registered with the same BundleContext
        serviceRegistration = FrameworkUtil.getBundle(ContextRegistrator.class).getBundleContext().registerService(ServletMapping.class.getName(), servletMapping, null);
        LOGGER.debug("Registered servlet at {}", alias);
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        if (null != serviceRegistration) {
            serviceRegistration.unregister();
        }
        this.context = null;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        ObjectSetContext.clear(); // start with a fresh slate
        try {
            super.service(request, response);
        } finally {
            ObjectSetContext.clear(); // leave with a fresh slate
        }
    }

    private class CustomRestlet extends JsonResourceRestlet {
        public CustomRestlet(JsonResource resource) {
            super(resource);
        }
        @Override public JsonValue newContext(Request request) {
            JsonValue result = super.newContext(request);
            JsonValue security = result.get("security");
            security.put("openidm-roles", request.getAttributes().get("openidm.roles"));
            return result;
        }
    }
}
