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
 * Copyright 2013-2014 ForgeRock Inc.
 */

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.openidm.core.IdentityServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Authentication Module for authenticating users using a client certificate.
 *
 * @author brmiller
 */
public class ClientCertAuthModule extends IDMServerAuthModule {

    private final static Logger logger = LoggerFactory.getLogger(ClientCertAuthModule.class);

    // property config keys
    private static final String ALLOWED_AUTHENTICATION_ID_PATTERNS = "allowedAuthenticationIdPatterns";

    // A list of ports that allow authentication purely based on client certificates (SSL mutual auth)
    private Set<Integer> clientAuthOnly = new HashSet<Integer>();

    private String component;
    private List<String> defaultRoles;
    private List<String> allowedAuthenticationIdPatterns;


    /**
     * Initialises the ClientCertAuthModule.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    protected void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler)
            throws AuthException{

        String clientAuthOnlyStr = IdentityServer.getInstance().getProperty("openidm.auth.clientauthonlyports");
        if (clientAuthOnlyStr != null) {
            String[] split = clientAuthOnlyStr.split(",");
            for (String entry : split) {
                clientAuthOnly.add(Integer.valueOf(entry));
            }
        }
        logger.info("Authentication disabled on ports: {}", clientAuthOnly);

        try {
            component = properties.get(COMPONENT).required().asString();
            defaultRoles = properties.get(DEFAULT_USER_ROLES).asList(String.class);
            allowedAuthenticationIdPatterns = properties.get(ALLOWED_AUTHENTICATION_ID_PATTERNS)
                    .defaultTo(new ArrayList<String>())
                    .asList(String.class);
        } catch (Exception e) {
            throw new AuthException("Unable to initialize client-cert auth module: " + e.getMessage());
        }
    }

    /**
     * Validates the request by authenticating against either the client certificate in the request, internally or
     * Basic Authentication from the request header internally.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @param securityContextMapper {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject,
            SecurityContextMapper securityContextMapper) {

        HttpServletRequest req = (HttpServletRequest) messageInfo.getRequestMessage();

        // if the request's local port is not an allowed client auth port, we cannot proceed with client auth
        if (!allowClientCertOnly(req)) {
            return AuthStatus.SEND_FAILURE;
        }

        boolean authenticated = authenticateUsingClientCert(req, securityContextMapper);
        securityContextMapper.setResource(component);
        
        if (authenticated) {
            final String authcid = securityContextMapper.getAuthenticationId();
            clientSubject.getPrincipals().add(new Principal() {
                public String getName() {
                    return authcid;
                }
            });
        }

        return authenticated ? AuthStatus.SUCCESS : AuthStatus.SEND_FAILURE;
    }

    /**
     * Whether to allow authentication purely based on client certificates.
     *
     * Note that the checking of the certificates MUST be done by setting jetty up for client auth required.
     *
     * @return true if authentication via client certificate only is sufficient.
     */
    private boolean allowClientCertOnly(ServletRequest request) {
        return clientAuthOnly.contains(Integer.valueOf(request.getLocalPort()));
    }

    /**
     * Authenticates the request using the client certificate from the request.
     *
     * @param request The ServletRequest.
     */
    // This is currently Jetty specific
    private boolean authenticateUsingClientCert(ServletRequest request, SecurityContextMapper securityContextMapper) {

        logger.debug("Client certificate authentication request");
        X509Certificate[] certs = getClientCerts(request);

        if (certs == null || certs.length < 1 || certs[0] == null) {
            return false;
        }

        Principal existingPrincipal = request instanceof HttpServletRequest
                ? ((HttpServletRequest) request).getUserPrincipal()
                : null;
        logger.debug("Request {} existing Principal {} has {} certificates", request, existingPrincipal, certs.length);
        for (X509Certificate cert : certs) {
            logger.debug("Request {} client certificate subject DN: {}", request, cert.getSubjectDN());
        }

        // Q: is it possible to pass multiple client certs
        String username = certs[0].getSubjectDN().getName();

        if (!usernameMatchesPatterns(username)) {
            logger.debug("Client certificate subject {} did not match allowed patterns", username);
            return false;
        }
        securityContextMapper.setAuthenticationId(username);
        securityContextMapper.setUserId(username);
        securityContextMapper.setRoles(defaultRoles);

        logger.debug("Authentication client certificate subject {}", username);
        return true;
    }

    private boolean usernameMatchesPatterns(String username) {
        for (String pattern : allowedAuthenticationIdPatterns) {
            if (username.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the client certificates from the request.
     *
     * @param request The ServletRequest.
     * @return An array of X509Certificates.
     */
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
     * No work to do here so always returns AuthStatus.SEND_SUCCESS.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) {
        return super.secureResponse(messageInfo, serviceSubject);
    }

    /**
     * Nothing to clean up.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     */
    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) {
    }
}
