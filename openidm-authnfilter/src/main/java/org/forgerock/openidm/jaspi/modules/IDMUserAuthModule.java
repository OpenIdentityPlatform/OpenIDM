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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterBuilder;
import org.forgerock.openidm.util.Accessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Authentication Module for authenticating users against an OpenIDM users table.
 *
 * @author Phill Cunnington
 * @author brmiller
 */
public class IDMUserAuthModule extends IDMServerAuthModule {

    private final static Logger logger = LoggerFactory.getLogger(IDMUserAuthModule.class);

    // property config keys
    private static final String USER_CREDENTIAL = "userCredential";
    private static final String USER_ROLES = "userRoles";

    // A list of ports that allow authentication purely based on client certificates (SSL mutual auth)
    private Set<Integer> clientAuthOnly = new HashSet<Integer>();

    private AuthHelper authHelper;

    private Accessor<ServerContext> accessor;

    private String queryId;
    private String component;

    /**
     * Constructor used by the commons Authentication Filter framework to create an instance of this authentication
     * module.
     */
    public IDMUserAuthModule() {
        this.accessor = new Accessor<ServerContext>() {
            public ServerContext access() {
                try {
                    return OSGiAuthnFilterBuilder.getRouter().createServerContext();
                } catch (ResourceException e) {
                    throw new IllegalStateException("Router context unavailable", e);
                }
            }
        };
    }

    /**
     * Constructor used by tests to inject dependencies.
     *
     * @param authHelper A mock of an AuthHelper instance.
     */
    public IDMUserAuthModule(AuthHelper authHelper, Accessor<ServerContext> accessor, String queryId, String component) {
        this.queryId = queryId;
        this.component = component;
        this.authHelper = authHelper;
        this.accessor = accessor;
    }

    /**
     * Initialises the IDMUserAuthModule.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    protected void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler)
            throws AuthException {

        String clientAuthOnlyStr = IdentityServer.getInstance().getProperty("openidm.auth.clientauthonlyports");
        if (clientAuthOnlyStr != null) {
            String[] split = clientAuthOnlyStr.split(",");
            for (String entry : split) {
                clientAuthOnly.add(Integer.valueOf(entry));
            }
        }
        logger.info("Authentication disabled on ports: {}", clientAuthOnly);

        try {
            queryId = properties.get(QUERY_ID).required().asString();
            component = properties.get(COMPONENT).required().asString();
            JsonValue propertyMapping = properties.get(PROPERTY_MAPPING);
            String authenticationIdProperty = propertyMapping.get(AUTHENTICATION_ID).asString();
            String userCredentialProperty = propertyMapping.get(USER_CREDENTIAL).asString();
            String userRolesProperty = propertyMapping.get(USER_ROLES).asString();
            List<String> defaultRoles = properties.get(DEFAULT_USER_ROLES).asList(String.class);

            authHelper = new AuthHelper(
                    OSGiAuthnFilterBuilder.getCryptoService(),
                    OSGiAuthnFilterBuilder.getConnectionFactory(),
                    authenticationIdProperty, userCredentialProperty, userRolesProperty, defaultRoles);
        } catch (Exception e) {
            throw new AuthException("Unable to initialize user auth module: " + e.getMessage());
        }
    }

    /**
     * Validates the request by authenticating against an internal resource using either
     * Basic Authentication from the request header or the X-OpenIDM-* request headers.
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

        if (authenticateUser(HEADER_AUTH_CRED_HELPER.getCredential(req), securityContextMapper)
                || authenticateUser(BASIC_AUTH_CRED_HELPER.getCredential(req), securityContextMapper)) {
            final String authcid = securityContextMapper.getAuthenticationId();
            clientSubject.getPrincipals().add(new Principal() {
                public String getName() {
                    return authcid;
                }
            });
            return AuthStatus.SUCCESS;
        } else {
            return AuthStatus.SEND_FAILURE;
        }
    }

    /**
     * Authenticate the username and password.
     *
     * @param cred the username/password credential
     * @param securityContextMapper The SecurityContextMapper.
     * @return whether the authentication is successful
     */
    private boolean authenticateUser(Credential cred, SecurityContextMapper securityContextMapper) {
        // set authenticationId and resource so long as username isn't null
        if (cred.username != null) {
            securityContextMapper.setAuthenticationId(cred.username);
            securityContextMapper.setResource(component);
        }
        if (!cred.isComplete()) {
            logger.debug("Failed authentication, missing or empty headers");
            return false;
        }
        try {
            return authHelper.authenticate(queryId, component, cred.username, cred.password, securityContextMapper, accessor.access());
        } catch (IllegalStateException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * No work to do here so chain super-class result.
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
