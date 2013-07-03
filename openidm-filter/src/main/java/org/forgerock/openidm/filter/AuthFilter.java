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
 * Copyright © 2011-2013 ForgeRock Inc. All rights reserved.
 */

package org.forgerock.openidm.filter;

// Java Standard Edition

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.jaspi.config.AuthenticationConfig;
import org.forgerock.openidm.jaspi.modules.AuthData;
import org.forgerock.openidm.jaspi.modules.AuthHelper;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * Auth Filter.
 *
 * @author Jamie Nelson
 * @author aegloff
 * @author ckienle
 */
@Component(name = "org.forgerock.openidm.reauthentication", immediate = true, policy = ConfigurationPolicy.IGNORE)
@Service(value = {AuthFilterService.class, JsonResource.class})
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Authentication Filter Service"),
        @Property(name = "openidm.router.prefix", value = "authentication")
})
public class AuthFilter implements AuthFilterService, JsonResource {

    private final static Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    /** Re-authentication password header. */
    public static final String HEADER_REAUTH_PASSWORD = "X-OpenIDM-Reauth-Password";

    private String queryId;
    private String queryOnResource;

    /** The authentication module to delegate to. */
    private AuthHelper authHelper;

    @Reference(
            name = "AuthenticationConfig",
            referenceInterface = AuthenticationConfig.class,
            policy = ReferencePolicy.DYNAMIC,
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            bind = "bindAuthenticationConfig",
            unbind = "unBindAuthenticationConfig"
    )
    private JsonValue config;
    private void bindAuthenticationConfig(AuthenticationConfig authenticationConfig) {
        config = authenticationConfig.getConfig();
    }
    private void unBindAuthenticationConfig(AuthenticationConfig authenticationConfig) {
        config = null;
    }

    /**
     * Activates this component.
     *
     * @param context The ComponentContext.
     */
    @Activate
    protected synchronized void activate(ComponentContext context) {
        logger.info("Activating Auth Filter with configuration {}", context.getProperties());
        setConfig(config);
    }

    private void setConfig(JsonValue config) {

        queryId = config.get("queryId").defaultTo("credential-query").asString();
        queryOnResource = config.get("queryOnResource").defaultTo("managed/user").asString();

        JsonValue properties = config.get("propertyMapping");
        String userIdProperty = properties.get("userId").asString();
        String userCredentialProperty = properties.get("userCredential").asString();
        String userRolesProperty = properties.get("userRoles").asString();
        List<String> defaultRoles = config.get("defaultUserRoles").asList(String.class);

        authHelper = new AuthHelper(userIdProperty, userCredentialProperty, userRolesProperty, defaultRoles);
    }

    /**
     * Action support, including re-authenticate action.
     * {@inheritDoc}
     */
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        JsonValue response = new JsonValue(new HashMap());
        String id = request.get("id").asString();

        JsonValue params = request.get("params");
        String action = params.get("_action").asString();
        if (action == null) {
            throw new BadRequestException("Action parameter is not present or value is null");
        }

        if (id == null) {
            // operation on collection
            if ("reauthenticate".equalsIgnoreCase(action)) {
                try {
                    AuthData reauthenticated = reauthenticate(request);
                    response.put("reauthenticated", Boolean.TRUE);
                    response.put("username", reauthenticated.getUsername());
                } catch (AuthException ex) {
                    throw new ForbiddenException("Reauthentication failed", ex);
                }
            } else {
                throw new BadRequestException("Action " + action + " on authentication service not supported "
                        + params);
            }
        } else {
            throw new BadRequestException("Actions not supported on child resource of authentication service "
                    + params);
        }
        return response;
    }

    /**
     * Re-authenticate based on the context associated with the request.
     *
     * @param request the full request
     * @return authenticated user if success
     * @throws AuthException if reauthentication failed
     */
    public AuthData reauthenticate(JsonValue request) throws AuthException {
        JsonValue secCtx = getSecurityContext(request);
        JsonValue headers = secCtx.get("headers");
        String reauthPassword = null;
        for (Entry<String, Object> entry : headers.asMap().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(HEADER_REAUTH_PASSWORD)) {
                reauthPassword = (String) entry.getValue();
                break;
            }
        }
        AuthData ad = new AuthData();
        String username = secCtx.get("security").get("username").asString();
        ad.setUsername(username);
        if (username == null || reauthPassword == null || username.equals("") || reauthPassword.equals("")) {
            logger.debug("Failed authentication, missing or empty headers");
            throw new AuthException("Failed authentication, missing or empty headers");
        }
        boolean authenticated = authHelper.authenticate(queryId, queryOnResource, username, reauthPassword, ad);
        if (!authenticated) {
            throw new AuthException(ad.getUsername());
        }
        return ad;
    }

    /**
     * Gets the Security Context.
     *
     * @param request the full request
     * @return the security context of the request, or null if does not exist
     */
    private JsonValue getSecurityContext(JsonValue request) {
        JsonValue result = new JsonValue(null);
        while (request != null && !request.isNull()) {
            if ("http".equals(request.get("type").asString())) {
                if (!request.get("security").isNull()) {
                    result = request;
                    break;
                }
            }
            request = request.get("parent");
        }
        return result;
    }
}

