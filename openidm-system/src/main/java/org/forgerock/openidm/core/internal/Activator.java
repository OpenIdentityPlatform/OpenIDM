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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.openidm.core.internal;

import java.util.Map;

import org.forgerock.openidm.core.FrameworkPropertyAccessor;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServicePropertyAccessor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;

/**
 * An Activator opens {@code ServiceTracker<Map<String, Object>, Map<String,
 * Object>>} and tracks all service and initialise the {@link IdentityServer}
 * with {@link org.forgerock.openidm.core.ServicePropertyAccessor}.
 *
 */
public class Activator implements BundleActivator {

    private ServiceTracker<Map<String, Object>, Map<String, Object>> serviceTracker = null;

    public void start(final BundleContext context) throws Exception {
        String customFilter = context.getProperty("org.forgerock.openidm.core.map.filter");
        Filter filter = null;
        if (null != customFilter && customFilter.trim().length() > 4) {
            filter =
                    context.createFilter("(&(" + Constants.OBJECTCLASS + "=java.util.Map)"
                            + customFilter + ")");
        } else {
            filter =
                    context.createFilter("(&(" + Constants.OBJECTCLASS + "=java.util.Map)("
                            + Constants.SERVICE_PID
                            + "=org.forgerock.commons.launcher.BootConfiguration))");
        }
        serviceTracker =
                new ServiceTracker<Map<String, Object>, Map<String, Object>>(context, filter, null);
        serviceTracker.open(true);

        IdentityServer.initInstance(new ServicePropertyAccessor(context, serviceTracker,
                new FrameworkPropertyAccessor(context, null)));
    }

    public void stop(BundleContext context) throws Exception {
        if (null != serviceTracker) {
            serviceTracker.close();
        }
    }
}
