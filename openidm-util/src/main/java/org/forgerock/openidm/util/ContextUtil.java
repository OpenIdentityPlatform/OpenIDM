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
 * Copyright © 2014-2015 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.http.Context;
import org.forgerock.http.context.RootContext;
import org.forgerock.json.resource.ClientContext;
import org.forgerock.json.resource.InternalContext;
import org.forgerock.json.resource.SecurityContext;

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
    /**
     * Create a internal context used for trusted, internal calls.
     * <p>
     * If the request is initiated in a non-authenticated location (
     * {@code BundleActivator}, {@code Scheduler}, {@code ConfigurationAdmin})
     * this context should be used. The AUTHORIZATION module grants full access
     * to this context.
     *
     * @return a new {@link InternalContext}
     */
    public static Context createInternalContext() {
        // Ideally, we would have an internal system user that we could point to;
        // point to it now and build it later
        final Map<String, Object> authzid = new HashMap<String, Object>();
        authzid.put(SecurityContext.AUTHZID_ID, "system");
        List<String> roles = new ArrayList<String>();
        roles.add("system");
        authzid.put(SecurityContext.AUTHZID_ROLES, roles);
        authzid.put(SecurityContext.AUTHZID_COMPONENT, "internal/user");
        return new InternalContext(new SecurityContext(new RootContext(), "system", authzid));
    }
}
