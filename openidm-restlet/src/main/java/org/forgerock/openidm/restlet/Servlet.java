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

// Java Standard Edition
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Java Servlet
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// OSGi Framework
import org.osgi.service.component.ComponentContext;

// Apache Felix Maven SCR Plugin
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

// Restlet Framework
import org.restlet.Restlet;
import org.restlet.ext.servlet.ServletAdapter; 

// ForgeRock OpenIDM Core
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.context.InvokeContext;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
@Component(
    name = "org.forgerock.openidm.restlet",
    policy = ConfigurationPolicy.IGNORE
)
@Properties({
    @Property(name = "service.description", value = "OpenIDM servlet"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "alias", value = "/openidm")
})
@Service
public class Servlet extends HttpServlet {

    /** TODO: Description. */
    private static final String PATH_PROPERTY = "openidm.restlet.path";

    /** TODO: Description. */
    private final Application application = new Application();

    /** TODO: Description. */
    private ServletAdapter adapter;

    /** TODO: Description. */
    private ComponentContext context;

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
            application.attach((String)path, restlet);
        }
    }
    protected synchronized void unbindRestlet(Restlet restlet, Map<String, Object> properties) {
        Object path = properties.get(PATH_PROPERTY);
        if (path != null && path instanceof String) { // service is specified as internally routable
            application.detach(restlet);
        }
    }

    /**
     * Provides automatic binding of {@link ObjectSet} objects that include the
     * {@code openidm.restlet.path} property.
     */ 
    @Reference(
        name = "reference_Application_ObjectSet",
        referenceInterface = ObjectSet.class,
        bind = "bindObjectSet",
        unbind = "unbindObjectSet",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        strategy = ReferenceStrategy.EVENT
    )
    protected HashMap<ObjectSet, ObjectSetFinder> finders = new HashMap<ObjectSet, ObjectSetFinder>();
    protected synchronized void bindObjectSet(ObjectSet objectSet, Map<String, Object> properties) {
        ObjectSetFinder finder = new ObjectSetFinder(objectSet);
        finders.put(objectSet, finder);
        bindRestlet(finder, properties);
    }
    protected synchronized void unbindObjectSet(ObjectSet objectSet, Map<String, Object> properties) {
        ObjectSetFinder finder = finders.get(objectSet);
        if (finder != null) {
            unbindRestlet(finder, properties);
            finders.remove(objectSet);
        }
    }

    @Activate
    protected synchronized void activate(ComponentContext context) {
        this.context = context;
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        this.context = null;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        adapter = new ServletAdapter(getServletContext(), application);
    }

    @Override
    public void destroy() {
        adapter = null;
        super.destroy();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (adapter == null) {
            throw new ServletException("No adapter to handle request");
        }
        InvokeContext.getContext().pushActivityId(UUID.randomUUID().toString());
        try {
            adapter.service(req, res);
        } finally {
            InvokeContext.getContext().popActivityId();
        }
    }
}
