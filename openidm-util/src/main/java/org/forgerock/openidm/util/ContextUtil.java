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
 * Copyright © 2014 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.util;

import org.forgerock.json.resource.ClientContext;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SecurityContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class ContextUtil {

    /**
     * {@code ContextUtil} instances should NOT be constructed in standard
     * programming. Instead, the class should be used as
     * {@code ContextUtil.createInternalSecurityContext(...);}.
     */
    private ContextUtil() {
        super();
    }

    /**
     * Create a default internal {@link org.forgerock.json.resource.SecurityContext} newBuilder used for
     * internal trusted calls.
     * <p/>
     *
     * If the request is initiated in a non-authenticated location (
     * {@code BundleActivator}, {@code Scheduler}, {@code ConfigurationAdmin})
     * this contest should be used. The AUTHORIZATION module grants full access
     * to this context.
     *
     * @param bundleContext
     *            the context of the OSGi Bundle.
     * @return new {@code SecurityContext} newBuilder.
     */
    public static SecurityContext createInternalSecurityContext(final BundleContext bundleContext) {

        // TODO Finalise the default system context
        Map<String, Object> authzid = new HashMap<String, Object>();
        authzid.put(SecurityContext.AUTHZID_COMPONENT, bundleContext.getBundle().getSymbolicName());
        authzid.put(SecurityContext.AUTHZID_ROLES, "system");
        authzid.put(SecurityContext.AUTHZID_DN, "system");
        authzid.put(SecurityContext.AUTHZID_REALM, "system");
        authzid.put(SecurityContext.AUTHZID_ID, "system");
        return new SecurityContext(new RootContext(),
                bundleContext.getProperty(Constants.BUNDLE_SYMBOLICNAME), authzid);

    }

    /**
     * Tests whether the context represents an external routed request, by checking if the client context
     * is present and that it is external
     *
     * @param context the context to inspect
     * @return true if the context represents an external request;
     *      false otherwise, or if the {@link ClientContext} could not be found
     */
    public static boolean isExternal(Context context) {
        return context.containsContext(ClientContext.class)
                && context.asContext(ClientContext.class).isExternal();
    }
}
