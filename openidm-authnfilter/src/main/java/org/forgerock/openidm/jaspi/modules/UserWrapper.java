/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.json.resource.SecurityContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * Adapts the servlet request to implement OpenIDM's default authentication.
 *
 * @author Jamie Nelson
 * @author Paul C. Bryan
 */
class UserWrapper extends HttpServletRequestWrapper {

    /** The user principal name, as provided by the authentication filter. */
    private final String username;

    /** The internal user id, as provided by the authentication filter. */
    //private final String userId;

    /** A (case-sensitive) list of roles, as provided by the authentication filter. */
    private final List<String> roles;

    private final SecurityContext securityContext;

    /**
     * Contructs a new user wrapper.
     *
     * @param request the HTTP servlet request being adapted.
     * @param securityContext the authenticated context.
     */
    public UserWrapper(HttpServletRequest request, final SecurityContext securityContext) {
        super(request);
        this.securityContext = securityContext;
        username = securityContext.getAuthenticationId();
        if (securityContext.getAuthorizationId().get(SecurityContext.AUTHZID_ROLES) instanceof List) {
            roles = (List<String>) securityContext.getAuthorizationId().get(SecurityContext.AUTHZID_ROLES);
        } else {
            roles = null;
        }
    }

    public SecurityContext getServerContext() {
        return securityContext;
    }

    /**
     * Returns {@code true} if the header should be suppressed by this filter.
     */
    private static boolean suppress(String header) {
        // optimization: avoid lowercasing every header string
        // For now, only suppress the password header (but not user name or re-auth)
        boolean suppress = (header.length() >= 10 && header.charAt(1) == '-' &&
                header.charAt(9) == '-' && header.toLowerCase().startsWith("x-openidm-") &&
                header.equalsIgnoreCase(IDMServerAuthModule.HEADER_PASSWORD));
        return suppress;
    }

    private static Enumeration<String> emptyEnumeration() {
        return new Enumeration<String>() {
            @Override public boolean hasMoreElements() {
                return false;
            }
            @Override public String nextElement() {
                throw new NoSuchElementException();
            }
        };
    }
        

    @Override
    public Principal getUserPrincipal() {
        return new Principal() {
            @Override public String getName() {
                return username;
            }
        };
    }

    @Override
    public boolean isUserInRole(String role) { 
        return (roles != null && roles.contains(role));
    }

    @Override
    public String getRemoteUser() { 
        return username;
    }

    @Override
    public long getDateHeader(String name) {
        return (suppress(name) ? -1L : super.getDateHeader(name));
    }

    @Override
    public String getHeader(String name) {
        return (suppress(name) ? null : super.getHeader(name));
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return (suppress(name) ? emptyEnumeration() : super.getHeaders(name));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        ArrayList<String> names = new ArrayList<String>();
        Enumeration<String> e = super.getHeaderNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            if (!suppress(name)) {
                names.add(name);
            }
        }
        return Collections.enumeration(names);
    }

    @Override
    public int getIntHeader(String name) {
        return (suppress(name) ? -1 : super.getIntHeader(name));
    }
}
