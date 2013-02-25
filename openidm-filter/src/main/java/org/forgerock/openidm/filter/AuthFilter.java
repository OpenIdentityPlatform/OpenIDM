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

package org.forgerock.openidm.filter;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.json.resource.servlet.HttpServletContextFactory;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.http.ContextRegistrator;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.openidm.util.DateUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auth Filter
 * 
 * @author Jamie Nelson
 * @author aegloff
 * @author ckienle
 */

@Component(name = AuthFilter.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service({HttpServletContextFactory.class, SingletonResourceProvider.class})
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Authentication Filter Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "authentication")
})
public class AuthFilter implements Filter, HttpServletContextFactory, SingletonResourceProvider {

    public static final String PID = "org.forgerock.openidm.authentication";

    /**
     * Setup logging for the {@link AuthFilter}.
     */
    private final static Logger logger = LoggerFactory.getLogger(AuthFilter.class);

//    /** Attribute in session containing authenticated username. */
//    public static final String USERNAME_ATTRIBUTE = "openidm.username";
//
//    /** Attribute in session containing authenticated userid. */
//    public static final String USERID_ATTRIBUTE = "openidm.userid";
//
//    /** Attribute in session and request containing assigned roles. */
//    public static final String ROLES_ATTRIBUTE = "openidm.roles";
//
//    /**
//     * Attribute in session containing user's resource (managed_user or
//     * internal_user)
//     */
//    public static final String RESOURCE_ATTRIBUTE = "openidm.resource";
//
//    /**
//     * Attribute in request to indicate to openidm down stream that an
//     * authentication filter has secured the request
//     */
//    public static final String OPENIDM_AUTHINVOKED = "openidm.authinvoked";

    /** Authentication username header */
    public static final String HEADER_USERNAME = "X-OpenIDM-Username";

    /** Authentication password header */
    public static final String HEADER_PASSWORD = "X-OpenIDM-Password";

    /** Re-authentication password header */
    public static final String HEADER_REAUTH_PASSWORD = "X-OpenIDM-Reauth-Password";

    /** The authentication module to delegate to */
    private AuthModule authModule;

    // name of the header containing the client IPAddress, used for the audit
    // record
    // typically X-Forwarded-For
    static String logClientIPHeader = null;
    private static DateUtil dateUtil;

    public enum Action {
        authenticate, logout
    }


    // A list of ports that allow authentication purely based on client
    // certificates (SSL mutual auth)
    static Set<Integer> clientAuthOnly = new HashSet<Integer>();

    private FilterConfig config = null;

    // ----- Implementation of Filter interface

    @Override
    public Context createContext(HttpServletRequest request) throws ResourceException {
        Context requestContext = null;
        //Try the session first
        HttpSession session = request.getSession(false);
        if (null != session) {
            Object o = session.getAttribute(Context.class.getName());
            if (o instanceof Context) {
                requestContext = (Context) o;
            }
        }
        if (null == requestContext) {
            Object o = request.getAttribute(Context.class.getName());
            if (o instanceof Context) {
                requestContext = (Context) o;
            }
        }
        if (null == requestContext) {
            //Anonymous context
            requestContext =  new SecurityContext(new RootContext(),"anonymous", null);
        }
        return requestContext;
    }


    // ----- Implementation of Filter interface


    public void init(FilterConfig config) throws ServletException {
        this.config = config;

        String clientAuthOnlyStr = IdentityServer.getInstance().getProperty("openidm.auth.clientauthonlyports");
        if (clientAuthOnlyStr != null) {
            String[] split = clientAuthOnlyStr.split(",");
            for (String entry : split) {
                clientAuthOnly.add(Integer.valueOf(entry));
            }
        }
        logger.info("Authentication disabled on ports: {}", clientAuthOnly);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (repositoryRoute == null) {
            throw new ServletException("Internal services not ready to process requests.");
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        SecurityContext authData = null;

        try {
            HttpSession session = req.getSession(false);
            String logout = req.getHeader("X-OpenIDM-Logout");
            String headerLogin = req.getHeader(HEADER_USERNAME);
            String basicAuth = req.getHeader("Authorization");
            if (logout != null) {
                if (session != null) {
                    session.invalidate();
                }
                res.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
                // if we see the certficate port this request is for client auth
                // only
            } else if (allowClientCertOnly(req)) {
                authData = hasClientCert(req);
                logAuth(req, authData,
                        Action.authenticate, Status.SUCCESS);
            } else if (session == null && headerLogin != null) {
                authData = authenticateUser(req);
                logAuth(req, authData,
                        Action.authenticate, Status.SUCCESS);
                createSession(req, authData);
            } else if (session == null && basicAuth != null) {
                authData = doBasicAuth(basicAuth);
                logAuth(req, authData,
                        Action.authenticate, Status.SUCCESS);
                createSession(req, authData);
            } else {
                authFailed(req, res, null != authData ? authData.getAuthenticationId() : null);
                return;
            }
        } catch (AuthException s) {
            authFailed(req, res, s.getMessage());
            return;
        }

        req.setAttribute(Context.class.getName(), authData);

//        logger.debug("Found valid session for {} id {} with roles {}", new Object[] {
//            authData.username, authData.userId, authData.roles });

        //req.setAttribute(OPENIDM_AUTHINVOKED, "openidmfilter");

        chain.doFilter(new UserWrapper(req, authData), res);
    }

    public void destroy() {
        config = null;
    }

    private void authFailed(HttpServletRequest req, HttpServletResponse res, String username)
            throws IOException {
        logAuth(req, username, null, null, Action.authenticate, Status.FAILURE);
        ResourceException jre = ResourceException.getException(401, "", null);
        res.getWriter().write(jre.toJsonValue().toString());
        res.setContentType("application/json");
        res.setStatus(401);
    }

    private void logAuth(HttpServletRequest req, SecurityContext securityContext, Action action,
            Status status) {
        logAuth(req, securityContext.getAuthenticationId(), (String) securityContext
                .getAuthorizationId().get(SecurityContext.AUTHZID_ID),
                (List<String>) securityContext.getAuthorizationId().get(
                        SecurityContext.AUTHZID_ROLES), action, status);
    }


    private void logAuth(HttpServletRequest req, String username, String userId,
            List<String> roles, Action action, Status status) {
        try {
            JsonValue entry = new JsonValue(new HashMap<String, Object>());
            entry.put("timestamp", dateUtil.now());
            entry.put("action", action.toString());
            entry.put("status", status.toString());
            entry.put("principal", username);
            entry.put("userid", userId);
            entry.put("roles", roles);
            // check for header sent by load balancer for IPAddr of the client
            String ipAddress;
            if (logClientIPHeader == null) {
                ipAddress = req.getRemoteAddr();
            } else {
                ipAddress = req.getHeader(logClientIPHeader);
                if (ipAddress == null) {
                    ipAddress = req.getRemoteAddr();
                }
            }
            entry.put("ip", ipAddress);
            if (repositoryRoute != null) {
                // TODO We need Context!!!
                CreateRequest request = Requests.newCreateRequest("/audit/access", entry);
                ServerContext ctx = repositoryRoute.createServerContext();
                ctx.getConnection().create(ctx, request);
            } else {
                // Filter should have rejected request if router is not
                // available
                logger.warn("Failed to log entry for {} as router is null.", username);
            }
        } catch (ResourceException ose) {
            logger.warn("Failed to log entry for {}", username, ose);
        }
    }

    private SecurityContext doBasicAuth(String data) throws AuthException {

        logger.debug("HTTP basic authentication request");
        //SecurityContext ad = SecurityContext(new RootContext());
        StringTokenizer st = new StringTokenizer(data);
        String isBasic = st.nextToken();
        if (isBasic == null || !isBasic.equalsIgnoreCase("Basic")) {
            throw new AuthException("");
        }
        String creds = st.nextToken();
        if (creds == null) {
            throw new AuthException("");
        }
        String dcreds = new String(Base64.decodeBase64(creds.getBytes()));
        String[] t = dcreds.split(":");
        if (t.length != 2) {
            throw new AuthException("");
        }
        return authModule.authenticate(t[0], t[1], new HashMap<String, Object>(2));
    }

    private void createSession(HttpServletRequest req, SecurityContext sc) {
        if (req.getHeader("X-OpenIDM-NoSession") == null) {
            HttpSession session = req.getSession(true);
            session.setAttribute(Context.class.getName(), sc);
            if (logger.isDebugEnabled()) {
                logger.debug("Created session for: {} with id {}, roles {} and resource: {}",
                        new Object[] { sc.getAuthenticationId(),
                            sc.getAuthorizationId().get(SecurityContext.AUTHZID_ID),
                            sc.getAuthorizationId().get(SecurityContext.AUTHZID_ROLES),
                            sc.getAuthorizationId().get(SecurityContext.AUTHZID_COMPONENT) });

            }
        }
    }



    private SecurityContext authenticateUser(HttpServletRequest req) throws AuthException {

        logger.debug("No session, authenticating user");
        Map<String, Object> authzid = new HashMap<String, Object>(2);
        String authcid = req.getHeader(HEADER_USERNAME);
        String password = req.getHeader(HEADER_PASSWORD);

        if (StringUtils.isBlank(authcid) || StringUtils.isBlank(password)) {
            logger.debug("Failed authentication, missing or empty headers");
            throw new AuthException();
        }
        return  authModule.authenticate(authcid, password, authzid);
    }

    // This is currently Jetty specific
    private SecurityContext hasClientCert(ServletRequest request) throws AuthException {

        logger.debug("Client certificate authentication request");
        Map<String, Object> authzid = new HashMap<String, Object>(2);
        X509Certificate[] certs = getClientCerts(request);

        if (certs != null) {
            Principal existingPrincipal =
                    request instanceof HttpServletRequest ? ((HttpServletRequest) request)
                            .getUserPrincipal() : null;
            logger.debug("Request {} existing Principal {} has {} certificates", new Object[] {
                request, existingPrincipal, certs.length });
            for (X509Certificate cert : certs) {
                logger.debug("Request {} client certificate subject DN: {}", request, cert
                        .getSubjectDN());
            }
        }
        String authcid = certs[0].getSubjectDN().getName();
        if (certs == null || certs.length < 1 || certs[0] != null) {
            throw new AuthException(authcid);
        }
        List<String> roles = new ArrayList<String>(1);
        authzid.put(SecurityContext.AUTHZID_ROLES, Collections.unmodifiableList(roles));

        roles.add("openidm-cert");
        logger.debug("Authentication client certificate subject {}", authcid);
        return new SecurityContext(new RootContext(), authcid, authzid);
    }

    // This is currently Jetty specific
    private X509Certificate[] getClientCerts(ServletRequest request) {

        Object checkCerts = request.getAttribute("javax.servlet.request.X509Certificate");
        if (checkCerts instanceof X509Certificate[]) {
            return (X509Certificate[]) checkCerts;
        } else {
            logger.warn("Unknown certificate type retrieved {}", checkCerts);
            return null;
        }
    }

    /**
     * Whether to allow authentication purely based on client certificates Note
     * that the checking of the certificates MUST be done by setting jetty up
     * for client auth required.
     * 
     * @return true if authentication via client certificate only is sufficient
     */
    private boolean allowClientCertOnly(ServletRequest request) {
        return clientAuthOnly.contains(Integer.valueOf(request.getLocalPort()));
    }


    // ----- Declarative Service Implementation

    @Reference(target = "("+ServerConstants.ROUTER_PREFIX+"=/repo/*)")
    RouteService repositoryRoute;

    private void bindRouteService(final RouteService service) {
        repositoryRoute = service;
    }

    private void unbindRouteService(final RouteService service) {
        repositoryRoute = null;
    }


    @Reference(policy = ReferencePolicy.DYNAMIC)
    CryptoService cryptoService;

    private void bindCryptoService(final CryptoService service) {
        cryptoService = service;
    }

    private void unbindCryptoService(final CryptoService service) {
        cryptoService = null;
    }

    /** TODO: Description. */
    private ComponentContext context;

    /** TODO: Description. */
    private ServiceRegistration serviceRegistration;

    @Activate
    protected synchronized void activate(ComponentContext context) throws Exception {
        this.context = context;
        logger.info("Activating Auth Filter with configuration {}", context.getProperties());
        setConfig(context);
        // TODO make this configurable
        dateUtil = DateUtil.getDateUtil("UTC");

        String urlPatterns[] = { "/openidm/*" };
        String servletNames = "OpenIDM Authentication Filter Service";

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("urlPatterns", urlPatterns);
        props.put("servletNames", servletNames);
        props.put("httpContext.id", "openidm");
        serviceRegistration =
                FrameworkUtil.getBundle(ContextRegistrator.class).getBundleContext()
                        .registerService(Filter.class, this, props);

        /*
         * String urlPatterns[] = {"/openidm/*"}; String servletNames[] = null;
         *
         * Dictionary<String, Object> props = new Hashtable<String, Object>();
         * props.put(ExtenderConstants.PROPERTY_URL_PATTERNS, urlPatterns);
         * props.put(ExtenderConstants.PROPERTY_SERVLET_NAMES, servletNames);
         * props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "openidm");
         * serviceRegistration =
         * context.getBundleContext().registerService(Filter.class.getName(),
         * this, props);
         */

/*        org.ops4j.pax.web.extender.whiteboard.runtime.DefaultFilterMapping filterMapping =
                new org.ops4j.pax.web.extender.whiteboard.runtime.DefaultFilterMapping();
        filterMapping.setFilter(this);
        filterMapping.setHttpContextId("openidm");
        filterMapping.setServletNames("OpenIDM REST");// , "OpenIDM Web");
        filterMapping.setUrlPatterns("/openidm*//*");// , "/openidmui*//*");
        // filterMapping.setInitParams(null);
        serviceRegistration =
                FrameworkUtil.getBundle(ContextRegistrator.class).getBundleContext()
                        .registerService(org.ops4j.pax.web.extender.whiteboard.FilterMapping.class,
                                filterMapping, null);*/
    }

    @Modified
    void modified(ComponentContext context) throws Exception {
        logger.info("Modified auth Filter with configuration {}", context.getProperties());
        setConfig(context);
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        if (serviceRegistration != null) {
            try {
                serviceRegistration.unregister();
                logger.info("Unregistered authentication filter.");
            } catch (Exception ex) {
                logger.warn("Failure reported during unregistering of authentication filter: {}",
                        ex.getMessage(), ex);
            }
        }
    }

    private void setConfig(ComponentContext context) throws ResourceException {
        JsonValue config = new JsonValue(new JSONEnhancedConfig().getConfiguration(context));
        logClientIPHeader = config.get("clientIPHeader").asString();
        authModule = new AuthModule(cryptoService, repositoryRoute.createServerContext(),  config);
    }

    // ----- Implementation of SingletonResourceProvider interface

    /**
     * Action support, including reauthenticate action {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        try {
            if ("reauthenticate".equalsIgnoreCase(request.getActionId())) {
                try {
                    if (context.containsContext(HttpContext.class)
                            && context.containsContext(SecurityContext.class)) {
                        String authcid =
                                context.asContext(SecurityContext.class).getAuthenticationId();
                        HttpContext httpContext = context.asContext(HttpContext.class);
                        String password = httpContext.getHeaderAsString(HEADER_REAUTH_PASSWORD);
                        if (StringUtils.isBlank(authcid) || StringUtils.isBlank(password)) {
                            logger.debug("Failed authentication, missing or empty headers");
                            handler.handleError(new ForbiddenException(
                                    "Failed authentication, missing or empty headers"));
                            return;
                        }
                        authModule.authenticate(authcid, password, null);
                    }
                    //TODO Handle message
                } catch (AuthException ex) {
                    handler.handleError(new ForbiddenException("Reauthentication failed", ex));
                }
            } else {
                handler.handleError(new BadRequestException("Action " + request.getActionId()
                        + " on authentication service not supported"));
            }
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, ReadRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Read operations are not supported");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update operations are not supported");
        handler.handleError(e);
    }
}
