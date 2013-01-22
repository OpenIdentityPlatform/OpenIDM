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

package org.forgerock.openidm.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.SecurityContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class ContextUtil {

    /**
     * <p>
     * {@code ContextUtil} instances should NOT be constructed in standard
     * programming. Instead, the class should be used as
     * {@code ContextUtil.parseResourceName(" /foo/bar/ ");}.
     * </p>
     * 
     * <p>
     * This constructor is public to permit tools that require a JavaBean
     * instance to operate.
     * </p>
     */
    public ContextUtil() {
        super();
    }

    /**
     * Create a default internal {@link SecurityContext} instance used for
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
     * @return new {@code SecurityContext} instance.
     */
    public static SecurityContext createInternalSecurityContext(final BundleContext bundleContext) {

        // TODO Finalise the default system context
        Map<String, Object> authzid = new HashMap<String, Object>();
        authzid.put(SecurityContext.AUTHZID_COMPONENT, bundleContext.getBundle().getSymbolicName());
        authzid.put(SecurityContext.AUTHZID_ROLES, "system");
        authzid.put(SecurityContext.AUTHZID_GROUPS, "system");
        authzid.put(SecurityContext.AUTHZID_DN, "system");
        authzid.put(SecurityContext.AUTHZID_REALM, "system");
        authzid.put(SecurityContext.AUTHZID_ID, "system");
        return new SecurityContext(new RootContext(), bundleContext
                .getProperty(Constants.BUNDLE_SYMBOLICNAME), authzid);

    }

    /**
     * Retrieve the {@code UriTemplateVariables} from the context.
     * <p/>
     * 
     * @param context
     * 
     * @return an unmodifiableMap or null if the {@code context} does not
     *         contains {@link RouterContext}
     */
    public static Map<String, String> getUriTemplateVariables(Context context) {
        RouterContext routerContext =
                context.containsContext(RouterContext.class) ? context
                        .asContext(RouterContext.class) : null;
        if (null != routerContext) {
            return Collections.unmodifiableMap(routerContext.getUriTemplateVariables());
        }
        return null;
    }

    /**
     * Parse the given resource name into {@code resourceName} and possible
     * {@code resourceId}.
     * <p/>
     * Parser ignores the trailing {@code '/'} character in favour of
     * {@link org.forgerock.json.resource.servlet.HttpServletAdapter}. The
     * {@link org.forgerock.json.resource.Router} expects routes that matches
     * {@code '/resourceName/ id}/'} template <b>with</b> trailing {@code '/'}.
     * <p/>
     * The code removes the trailing and leading spaces only!
     * <p>
     * Examples:
     * <ul>
     * <li>{@code null} return null</li>
     * <li>{@code '/'} return {""}</li>
     * <li>{@code '/resourceName'} return {"resourceName"}</li>
     * <li>{@code '  /resourceName  '} return {"resourceName"}</li>
     * <li>{@code '/resourceName/'} return {"resourceName"}</li>
     * <li>{@code '/resourceName/resourceId'} return
     * {"resourceName","resourceId"}</li>
     * <li>{@code '/resourceName/type/resourceId'} return
     * {"resourceName/type","resourceId"}</li>
     * </ul>
     * </p>
     * 
     * 
     * @param resourceName
     *            The name of the JSON resource to which this request should be
     *            targeted.
     * @return The resource name spited at the last {@code '/'}
     */
    public static String[] parseResourceName(String resourceName) {
        if (StringUtils.isBlank(resourceName)) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(resourceName.trim(), "/", false);

        String lastNonBlank = null;
        StringBuilder builder = null;

        while (tokenizer.hasMoreElements()) {
            String next = tokenizer.nextToken();
            if (StringUtils.isNotBlank(next)) {
                if (null != lastNonBlank) {
                    if (null == builder) {
                        builder = new StringBuilder(lastNonBlank);
                    } else {
                        builder.append("/").append(lastNonBlank);
                    }
                }
                lastNonBlank = next;
            }
        }

        if (null != builder) {
            return new String[] { builder.toString(), lastNonBlank };
        } else if (null != lastNonBlank) {
            return new String[] { lastNonBlank };
        }
        return null;
    }
}
