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

package org.forgerock.openidm.router;

// Java Standard Edition

import org.apache.felix.scr.annotations.*;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetRouter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import java.util.Iterator;

// OSGi Framework
// Apache Felix Maven SCR Plugin
// JSON-Fluent library
// ForgeRock OpenIDM

/**
 * Provides internal routing for a top-level object set.
 *
 * @author Paul C. Bryan
 */
@Component(
    name="org.forgerock.openidm.router",
    policy=ConfigurationPolicy.IGNORE
)
@Properties({
    @Property(name="service.description", value="OpenIDM internal object set router"),
    @Property(name="service.vendor", value="ForgeRock AS")
})
@Service
public class ObjectSetRouterService extends ObjectSetRouter {

    /** TODO: Description. */
    private static final String REFERENCE_NAME = "reference_ObjectSetRouterService_ObjectSet";

    /** TODO: Description. */
    private ComponentContext context;

    @Reference(
        name=REFERENCE_NAME,
        referenceInterface=ObjectSet.class,
        bind="bind",
        unbind="unbind",
        cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy=ReferencePolicy.DYNAMIC,
        strategy=ReferenceStrategy.EVENT
    )
    protected int _dummy; // whiteboard pattern
    protected void bind(ServiceReference reference) {
        Object prefix = reference.getProperty("openidm.router.prefix");
        if (prefix != null && prefix instanceof String) { // service is specified as internally routable
            routes.put((String)prefix, (ObjectSet)context.locateService(REFERENCE_NAME, reference));
        }
    }
    protected void unbind(ServiceReference reference) {
        ObjectSet toRemove = (ObjectSet)context.locateService(REFERENCE_NAME, reference);
        for (Iterator<ObjectSet> i = routes.values().iterator(); i.hasNext();) {
            if (i.next() == toRemove) { // comparison by reference
                i.remove();
            }
        }
    }
    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        this.context = null;
        routes.clear();
    }
}
