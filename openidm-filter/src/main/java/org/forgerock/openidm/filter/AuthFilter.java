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
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.text.SimpleDateFormat;

import java.security.Principal;
import java.security.cert.X509Certificate;
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
import org.osgi.service.component.ComponentException;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

// Apache Felix Maven SCR Plugin
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.InvalidException;

// JSON Resource
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceRouter;

// Deprecated
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.objset.ObjectSetJsonResource;

import org.forgerock.openidm.audit.util.Status;

/**
 * Auth Filter
 * @author Jamie Nelson
 * @author aegloff
 */

@Component(
    name = "org.forgerock.openidm.authentication", immediate = true,
    policy = ConfigurationPolicy.OPTIONAL
)

public class AuthFilter implements Filter {

    private final static Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);

    /** Attribute in session containing authenticated username. */
    public static final String USERNAME_ATTRIBUTE = "openidm.username";

    /** Attribute in session and request containing assigned roles. */
    public static final String ROLES_ATTRIBUTE = "openidm.roles";

// FIXME: thread safety
    private final static SimpleDateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    // name of the header containing the client IPAddress, used for the audit record
    // typically X-Forwarded-For
    static String logClientIPHeader = null;

    public enum Action {
        authenticate, logout
    }

    // A list of ports that allow authentication purely based on client certificates (SSL mutual auth)
    static Set<Integer> clientAuthOnly = new HashSet<Integer>();

    private FilterConfig config = null;
    public void init(FilterConfig config) throws ServletException {
          this.config = config;
          
          String clientAuthOnlyStr = System.getProperty("openidm.auth.clientauthonlyports");
          if (clientAuthOnlyStr != null) {
              String[] split = clientAuthOnlyStr.split(",");
              for (String entry : split) {
                  clientAuthOnly.add(Integer.valueOf(entry));
              }
          }
          LOGGER.info("Authentication disabled on ports: {}", clientAuthOnly);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                                                        throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;
        String username = null;
        List<String> roles = new ArrayList<String>();

        HttpSession session = req.getSession(false);
        String logout = req.getHeader("X-OpenIDM-Logout");
        if (logout != null) {
            if (session != null) {
                session.invalidate();
            }
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        // if we see the certficate port this request is for client auth only
        } else if (allowClientCertOnly(req)) {
            LOGGER.debug("Client certificate authentication request");
            if (hasClientCert(req)) {
                username = "openidm-cert"; // FIXME
                roles.add("openidm-authorized");
                logAuth(req, username, roles, Action.authenticate, Status.SUCCESS); 
            } else {
                logAuth(req, username, null, Action.authenticate, Status.FAILURE);
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else if (session == null) {
            LOGGER.debug("No session, authenticating user");
            username = req.getHeader("X-OpenIDM-Username");
            if (authenticateUser(req, username, roles)) {
                logAuth(req, username, roles, Action.authenticate, Status.SUCCESS); 
                createSession(req, session, username, roles);
            } else {
                logAuth(req, username, null, Action.authenticate, Status.FAILURE); 
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            username = (String)session.getAttribute(USERNAME_ATTRIBUTE);
            roles = (List)session.getAttribute(ROLES_ATTRIBUTE);
        }
        LOGGER.debug("Found valid session for {} with roles {}", username, roles);
        req.setAttribute(ROLES_ATTRIBUTE, roles);
        chain.doFilter(new UserWrapper(req, username, roles), res);
    }

    private static void logAuth(HttpServletRequest req, String username, List<String> roles, Action action, Status status) {
        try {
            Map<String,Object> entry = new HashMap<String,Object>();
            entry.put("timestamp", isoFormatter.format(new Date()));
            entry.put("action", action.toString());
            entry.put("status", status.toString());
            entry.put("principal", username);
            entry.put("roles", roles);
            // check for header sent by load balancer for IPAddr of the client
            String ipAddress;
            if (logClientIPHeader == null ) {
                ipAddress = req.getRemoteAddr();
            } else {
                ipAddress = req.getHeader(logClientIPHeader);
                if (ipAddress == null) {
                    ipAddress = req.getRemoteAddr();
                }
            }
            entry.put("ip", ipAddress);
            router.create("audit/access", entry);
        } catch (ObjectSetException ose) {
            LOGGER.warn("Failed to log entry for {}", username, ose);
        }
    }

    private void createSession(HttpServletRequest req, HttpSession sess, String username, List<String> roles) {
        if (req.getHeader("X-OpenIDM-NoSession") == null) {
            sess = req.getSession();
            sess.setAttribute(USERNAME_ATTRIBUTE, username);
            sess.setAttribute(ROLES_ATTRIBUTE, roles);
            LOGGER.debug("Created session for: {} with roles {}", username, roles);
        }
    }

    private boolean authenticateUser(HttpServletRequest req, String username, List<String> roles) {
        String password = req.getHeader("X-OpenIDM-Password");
        if (username == null || password == null || username.equals("") || password.equals("")) {
            LOGGER.debug("Failed authentication, missing or empty headers");
            return false;
        }
        return AuthModule.authenticate(username, password, roles);
    }

    // This is currently Jetty specific
    private boolean hasClientCert(ServletRequest request) {
        X509Certificate[] certs = getClientCerts(request);

        // TODO: reduce the logging level
        if (certs != null) {
            Principal existingPrincipal = request instanceof HttpServletRequest ? ((HttpServletRequest)request).getUserPrincipal() : null;
            LOGGER.info("Request {} existing Principal {} has {} certificates", new Object[] {request, existingPrincipal, certs.length});
            for (X509Certificate cert : certs) {
                LOGGER.info("Request {} client certificate subject DN: {}", request, cert.getSubjectDN());
            }
        }

        return (certs != null && certs.length > 0 && certs[0] != null);
    }

    // This is currently Jetty specific
    private X509Certificate[] getClientCerts(ServletRequest request) {
        Object checkCerts = request.getAttribute("javax.servlet.request.X509Certificate");
        if (checkCerts instanceof X509Certificate[]) {
            return (X509Certificate[]) checkCerts;
        } else {
            LOGGER.warn("Unknown certificate type retrieved {}", checkCerts);
            return null;
        }
    }

    /**
     * Whether to allow authentication purely based on client certificates
     * Note that the checking of the certificates MUST be done by setting 
     * jetty up for client auth required.
     * @return true if authentication via client certificate only is sufficient
     */
    private boolean allowClientCertOnly(ServletRequest request) {
        return clientAuthOnly.contains(Integer.valueOf(request.getLocalPort()));
    }

    public void destroy() {
        config = null;
    }

    @Reference(
        name = "ref_Auth_JsonResourceRouterService",
        referenceInterface = JsonResource.class,
        bind = "bindRouter",
        unbind = "unbindRouter",
        cardinality = ReferenceCardinality.MANDATORY_UNARY,
        policy = ReferencePolicy.STATIC,
        target = "(service.pid=org.forgerock.openidm.router)"
    )
    private static ObjectSet router;

    private void bindRouter(JsonResource router) {
        this.router = new JsonResourceObjectSet(router);
    }
    private void unbindRouter(JsonResource router) {
        this.router = null;
    }

    @Reference 
    HttpService httpService;
    
    @Reference(target="(openidm.contextid=shared)")
    HttpContext httpContext;

    EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
    private ComponentContext context;
    
    @Activate
    protected synchronized void activate(ComponentContext context) throws ServletException, NamespaceException {
        this.context = context;
        LOGGER.info("Activating Auth Filter with configuration {}", context.getProperties());
        JsonValue config = new JsonValue(new JSONEnhancedConfig().getConfiguration(context));
        logClientIPHeader = (String)config.get("clientIPHeader").asString();
        AuthModule.setConfig(config);
        
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

