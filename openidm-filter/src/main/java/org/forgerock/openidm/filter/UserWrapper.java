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

package org.forgerock.openidm.filter;

// Java SE
import java.security.Principal;
import java.util.ArrayList;  
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

// Servlet  
import javax.servlet.http.HttpServletRequest;  
import javax.servlet.http.HttpServletRequestWrapper;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final String userId;

    /** A (case-sensitive) list of roles, as provided by the authentication filter. */
    private final List<String> roles;

    /**
     * Contructs a new user wrapper.
     *
     * @param request the HTTP servlet request being adapted.
     * @param username the name of the authenticated user.
     * @param roles the roles assigned to the authenticated user.
     */
    public UserWrapper(HttpServletRequest request, String username, 
            String userId, List<String> roles) {
        super(request);
        this.username = username;
        this.userId = userId;
        this.roles = roles;
    }

    /**
     * Returns {@code true} if the header should be suppressed by this filter.
     */
    private static boolean suppress(String header) {
        // optimiziation: avoid lowercasing every header string
        return (header.length() >= 10 && header.charAt(1) == '-' &&
                header.charAt(9) == '-' && header.toLowerCase().startsWith("x-openidm-") && 
                !header.equals("X-OpenIDM-Reauth-Password"));
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
    public Enumeration getHeaders(String name) {
        return (suppress(name) ? emptyEnumeration() : super.getHeaders(name));
    }

    @Override
    public Enumeration getHeaderNames() {
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
