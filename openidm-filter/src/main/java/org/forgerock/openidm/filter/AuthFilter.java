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
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.http.ContextRegistrator;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.util.DateUtil;
import org.ops4j.pax.web.extender.whiteboard.FilterMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultFilterMapping;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auth Filter
 * @author Jamie Nelson
 * @author aegloff
 */

@Component(
    name = "org.forgerock.openidm.authentication", immediate = true,
    policy = ConfigurationPolicy.REQUIRE
)

public class AuthFilter implements Filter {


    private final static Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);

    /** Attribute in session containing authenticated username. */
    public static final String USERNAME_ATTRIBUTE = "openidm.username";

    /** Attribute in session and request containing assigned roles. */
    public static final String ROLES_ATTRIBUTE = "openidm.roles";

    /** Attribute in session containing user's resource (managed_user or internal_user) */
    public static final String RESOURCE_ATTRIBUTE = "openidm.resource";


    // name of the header containing the client IPAddress, used for the audit record
    // typically X-Forwarded-For
    static String logClientIPHeader = null;
    private static DateUtil dateUtil;

    public enum Action {
        authenticate, logout
    }

    private class AuthData {
       String username;
       List<String> roles = new ArrayList<String>();
       boolean status = false;
       String resource = "default";
    };

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
        if (router == null) {
            throw new ServletException("Internal services not ready to process requests.");
        }

        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;
        AuthData authData = new AuthData();

        try {
            HttpSession session = req.getSession(false);
            String logout = req.getHeader("X-OpenIDM-Logout");
            String headerLogin = req.getHeader("X-OpenIDM-Username");
            String basicAuth = req.getHeader("Authorization");
            if (logout != null) {
                if (session != null) {
                    session.invalidate();
                }
                res.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
              // if we see the certficate port this request is for client auth only
            } else if (allowClientCertOnly(req)) {
                authData = hasClientCert(req);
                logAuth(req, authData.username, authData.roles, Action.authenticate, Status.SUCCESS);
            } else if (session == null && headerLogin != null) {
                authData = authenticateUser(req);
                logAuth(req, authData.username, authData.roles, Action.authenticate, Status.SUCCESS);
                createSession(req, session, authData);
            } else if (session == null && basicAuth != null) {
                authData = doBasicAuth(basicAuth);
                logAuth(req, authData.username, authData.roles, Action.authenticate, Status.SUCCESS);
                createSession(req, session, authData);
            } else if (session != null) {
                authData.username = (String)session.getAttribute(USERNAME_ATTRIBUTE);
                authData.roles = (List)session.getAttribute(ROLES_ATTRIBUTE);
                authData.resource = (String)session.getAttribute(RESOURCE_ATTRIBUTE);
            } else {
                authFailed(req, res, authData.username);
                return;
            }
        } catch (AuthException s) {
            authFailed(req, res, s.getMessage());
            return;
        }
        LOGGER.debug("Found valid session for {} with roles {}", authData.username, authData.roles);
        req.setAttribute(ROLES_ATTRIBUTE, authData.roles);
        req.setAttribute(RESOURCE_ATTRIBUTE, authData.resource);
        chain.doFilter(new UserWrapper(req, authData.username, authData.roles), res);
    }

    private void authFailed(HttpServletRequest req, HttpServletResponse res, String username) throws IOException {
        logAuth(req, username, null, Action.authenticate, Status.FAILURE);
        JsonResourceException jre = new JsonResourceException(401, "Access denied");
        res.getWriter().write(jre.toJsonValue().toString());
    }

    private static void logAuth(HttpServletRequest req, String username,
                            List<String> roles, Action action, Status status) {
        try {
            Map<String,Object> entry = new HashMap<String,Object>();
            entry.put("timestamp", dateUtil.now());
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
            if (router != null) {
                router.create("audit/access", entry);
            } else {
                // Filter should have rejected request if router is not available
                LOGGER.warn("Failed to log entry for {} as router is null.", username);
            }
        } catch (ObjectSetException ose) {
            LOGGER.warn("Failed to log entry for {}", username, ose);
        }
    }

        //res.sendError(HttpServletResponse.SC_UNAUTHORIZED, new JsonValue(respMap).toString());
    private AuthData doBasicAuth(String data) throws AuthException {

        LOGGER.debug("HTTP basic authentication request");
        AuthData ad = new AuthData();
        StringTokenizer st = new StringTokenizer(data);
        String isBasic = st.nextToken();
        if (isBasic == null || !isBasic.equalsIgnoreCase("Basic")) {
            throw new AuthException("");
        }
        String creds= st.nextToken();
        if (creds == null) {
            throw new AuthException("");
        }
        String dcreds = new String(Base64.decodeBase64(creds.getBytes()));
        String[] t = dcreds.split(":");
        if (t.length != 2) {
            throw new AuthException("");
        }
        ad.username = t[0];
        StringBuilder resource = new StringBuilder();
        ad.status = AuthModule.authenticate(ad.username, t[1], ad.roles, resource);
        ad.resource = resource.toString();
        if (ad.status == false) {
            throw new AuthException(ad.username);
        }
        return ad;
    }

    private void createSession(HttpServletRequest req, HttpSession sess, AuthData ad) {
        if (req.getHeader("X-OpenIDM-NoSession") == null) {
            sess = req.getSession();
            sess.setAttribute(USERNAME_ATTRIBUTE, ad.username);
            sess.setAttribute(ROLES_ATTRIBUTE, ad.roles);
            sess.setAttribute(RESOURCE_ATTRIBUTE, ad.resource);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Created session for: {} with roles {} and resource: {}",
                    new Object[] {ad.username, ad.roles, ad.resource});
            }
        }
    }

    private AuthData authenticateUser(HttpServletRequest req) throws AuthException {

        LOGGER.debug("No session, authenticating user");
        AuthData ad = new AuthData();
        String password = req.getHeader("X-OpenIDM-Password");
        ad.username = req.getHeader("X-OpenIDM-Username");
        if (ad.username == null || password == null || ad.username.equals("") || password.equals("")) {
            LOGGER.debug("Failed authentication, missing or empty headers");
            throw new AuthException();
        }
        StringBuilder resource = new StringBuilder();
        ad.status = AuthModule.authenticate(ad.username, password, ad.roles, resource);
        ad.resource = resource.toString();
        if (ad.status == false) {
            throw new AuthException(ad.username);
        }
        return ad;
    }

    // This is currently Jetty specific
    private AuthData hasClientCert(ServletRequest request) throws AuthException {

        LOGGER.debug("Client certificate authentication request");
        AuthData ad = new AuthData();
        X509Certificate[] certs = getClientCerts(request);

        // TODO: reduce the logging level
        if (certs != null) {
            Principal existingPrincipal = request instanceof HttpServletRequest ? ((HttpServletRequest)request).getUserPrincipal() : null;
            LOGGER.debug("Request {} existing Principal {} has {} certificates", new Object[] {request, existingPrincipal, certs.length});
            for (X509Certificate cert : certs) {
                LOGGER.debug("Request {} client certificate subject DN: {}", request, cert.getSubjectDN());
            }
        }
        ad.status = (certs != null && certs.length > 0 && certs[0] != null);
        if (ad.status == false) {
            throw new AuthException(ad.username);
        }
        ad.username = certs[0].getSubjectDN().getName();
        ad.roles.add("openidm-cert");
        LOGGER.debug("Authentication client certificate subject {}", ad.username );
        return ad;
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

    /** TODO: Description. */
    private ComponentContext context;

    /** TODO: Description. */
    private ServiceRegistration serviceRegistration;

    @Activate
    protected synchronized void activate(ComponentContext context) throws ServletException, NamespaceException {
        this.context = context;
        LOGGER.info("Activating Auth Filter with configuration {}", context.getProperties());
        JsonValue config = new JsonValue(new JSONEnhancedConfig().getConfiguration(context));
        logClientIPHeader = (String) config.get("clientIPHeader").asString();
        AuthModule.setConfig(config);
        // TODO make this configurable
        dateUtil = DateUtil.getDateUtil("UTC");

        /*String urlPatterns[] = {"/openidm/*"};
        String servletNames[] = null;

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ExtenderConstants.PROPERTY_URL_PATTERNS, urlPatterns);
        props.put(ExtenderConstants.PROPERTY_SERVLET_NAMES, servletNames);
        props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "openidm");
        serviceRegistration = context.getBundleContext().registerService(Filter.class.getName(), this, props);*/

        DefaultFilterMapping filterMapping = new DefaultFilterMapping();
        filterMapping.setFilter(this);
        filterMapping.setHttpContextId("openidm");
        filterMapping.setServletNames("OpenIDM REST");//, "OpenIDM Web");
        filterMapping.setUrlPatterns("/openidm/*");//, "/openidmui/*");
        //filterMapping.setInitParams(null);
        serviceRegistration = FrameworkUtil.getBundle(ContextRegistrator.class).getBundleContext().registerService(FilterMapping.class.getName(), filterMapping, null);
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        if (serviceRegistration != null) {
            try {
                serviceRegistration.unregister();
                LOGGER.info("Unregistered authentication filter.");
            } catch (Exception ex) {
                LOGGER.warn("Failure reported during unregistering of authentication filter: {}", ex.getMessage(), ex);
            }
        }
    }

}

