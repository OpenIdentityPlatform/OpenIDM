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

// Java Standard Edition
import java.io.IOException;
import java.util.Dictionary;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;

// OSGi Framework
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

// Apache Felix Maven SCR Plugin
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to handle the REST interface
 *
 * @author Paul C. Bryan
 * @author aegloff
 */
@Component(
    name = "org.forgerock.openidm.filter", immediate = true,
    policy = ConfigurationPolicy.IGNORE
)
public class AuthFilter implements Filter {

    final static Logger logger  = LoggerFactory.getLogger(AuthFilter.class);

    private FilterConfig config = null;

    public void init(FilterConfig config) throws ServletException {
          this.config = config;
    }

    public void destroy() {
        config = null;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                                                        throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;
        String username = null;
        List roles = new ArrayList();

        HttpSession session = req.getSession(false);
        String logout = req.getHeader("X-OpenIDM-Logout");
        if (logout != null) {
            if (session != null) {
                session.invalidate();
            }
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        } else if (session == null) {
            logger.debug("No session, authenticating user");
            username = req.getHeader("X-OpenIDM-Username");
            if (authenticateUser(req, username, roles)) {
                createSession(req, session, username, roles);
            } else {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            username = (String)session.getAttribute("openidm-username");
            getRolesFromSession(session, roles);
        }
        logger.debug("Found valid session for {}", username);
        chain.doFilter(new UserWrapper(username, roles, req), res);
    }

    // temporary limit to one role, should support a list of roles
    private void setSessionRoles(HttpSession sess, List roles) {
        if (roles !=null && roles.size() > 0) {
            sess.setAttribute("X-OpenIDM-Role", roles.get(0));
        }
    }

    // temporary limit to one role, should support a list of roles
    private void getRolesFromSession(HttpSession sess, List roles) {
        String r = (String)sess.getAttribute("X-OpenIDM-Role");
        if (r != null) {
            roles.add(r);
        }
    }
 
    private void createSession(HttpServletRequest req, HttpSession sess, String username, List roles) {

        if (req.getHeader("X-OpenIDM-NoSession") == null) {
            sess = req.getSession();
            sess.setAttribute("openidm-username", username);
            setSessionRoles(sess, roles);
            logger.debug("Created session for: {}", username);
        }
    }

    private boolean authenticateUser(HttpServletRequest req, String username, List roles) {

        String password = req.getHeader("X-OpenIDM-Password");
        if (username == null || password == null || username.equals("") || password.equals("")) {
            logger.debug("Failed authentication, missing or empty headers");
            return false;
        }
        return AuthModule.authenticate(username, password, roles);
    }

    @Reference 
    HttpService httpService;
    
    @Reference(target="(openidm.contextid=shared)")
    HttpContext httpContext;
    
    @Activate
    protected synchronized void activate(ComponentContext context) throws ServletException, NamespaceException {

        org.ops4j.pax.web.service.WebContainer webContainer = (org.ops4j.pax.web.service.WebContainer) httpService;
        String urlPatterns[] = {"/openidm/*"};
        String servletNames[] = null;
        Dictionary initParams = null;
        webContainer.registerFilter((Filter)new AuthFilter(), urlPatterns, servletNames, initParams, httpContext);
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
    }

}

