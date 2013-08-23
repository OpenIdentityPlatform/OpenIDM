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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openidm.jaspi.config;

import org.forgerock.jaspi.filter.AuthNFilter;
import org.forgerock.json.resource.servlet.SecurityContextFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Context mapper for IDM that will convert the request headers/attributes/parameters set by the authentication and
 * authorization filters into what IDM expects/requires.
 *
 * @author Phill Cunnington
 */
public class IDMAuthContextMapper implements Filter {

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /**
     * Copies the authentication principal header, set by the authentication filter, into the request as an attribute
     * with the key SecurityContextFactory.ATTRIBUTE_AUTHCID and copies the authentication context attribute, set
     * by the authentication filter, into the request as an attributes with the key
     * SecurityContextFactory.ATTRIBUTES_AUTHZID.
     *
     * @param servletRequest {@inheritDoc}
     * @param servletResponse {@inheritDoc}
     * @param filterChain {@inheritDoc}
     * @throws IOException {@inheritDoc}
     * @throws ServletException {@inheritDoc}
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        if ((!HttpServletRequest.class.isAssignableFrom(servletRequest.getClass())
                || !HttpServletResponse.class.isAssignableFrom(servletResponse.getClass()))) {
            throw new ServletException("Unsupported protocol");
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;

        Map<String, Object> authzid = (Map<String, Object>) request.getAttribute(AuthNFilter.ATTRIBUTE_AUTH_CONTEXT);
        String authcid = null;
		if (authzid != null) {
			authcid = (String) authzid.get("id");
		}
        
        request.setAttribute(SecurityContextFactory.ATTRIBUTE_AUTHCID, authcid);
        request.setAttribute(SecurityContextFactory.ATTRIBUTE_AUTHZID, authzid);

        filterChain.doFilter(request, servletResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
    }
}
