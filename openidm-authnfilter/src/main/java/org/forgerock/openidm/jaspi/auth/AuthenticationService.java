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
 * Copyright 2013-2015 ForgeRock AS
 */

package org.forgerock.openidm.jaspi.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.base.Predicate;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.jaspi.config.AuthenticationConfig;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.forgerock.jaspi.runtime.context.config.ModuleConfigurationFactory.SERVER_AUTH_CONTEXT_KEY;
import static org.forgerock.jaspi.runtime.context.config.ModuleConfigurationFactory.AUTH_MODULES_KEY;
import static org.forgerock.jaspi.runtime.context.config.ModuleConfigurationFactory.AUTH_MODULE_PROPERTIES_KEY;
import static org.forgerock.openidm.jaspi.config.JaspiRuntimeConfigurationFactory.MODULE_CONFIG_ENABLED;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.AUTHENTICATION_ID;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.PROPERTY_MAPPING;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.QUERY_ID;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.QUERY_ON_RESOURCE;
import static org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper.USER_CREDENTIAL;

/**
 * An implementation of the AuthenticationConfig that reads and holds the authentication configuration.
 */
@Component(name = AuthenticationService.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Authentication Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "/authentication")
})
public class AuthenticationService implements AuthenticationConfig, SingletonResourceProvider {

    /** The PID for this Component. */
    public static final String PID = "org.forgerock.openidm.authentication";

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    /** Re-authentication password header. */
    private static final String HEADER_REAUTH_PASSWORD = "X-OpenIDM-Reauth-Password";


    private JsonValue config;


    /** The authenticators to delegate to.*/
    private List<Authenticator> authenticators = new ArrayList<Authenticator>();

    // ----- Declarative Service Implementation

    @Reference(policy = ReferencePolicy.DYNAMIC)
    CryptoService cryptoService;

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;

    /** a factory Function to build an Authenticator from an auth module config */
    private AuthenticatorFactory toAuthenticatorFromProperties;


    /** A {@link Predicate} that returns whether the auth module is enabled */
    private static final Predicate<JsonValue> enabledAuthModules =
            new Predicate<JsonValue>() {
                @Override
                public boolean apply(JsonValue jsonValue) {
                    return jsonValue.get(MODULE_CONFIG_ENABLED).defaultTo(true).asBoolean();
                }
            };

    /** A {@link Function} that returns the auth modules properties as a JsonValue */
    private static final Function<JsonValue, JsonValue> toModuleProperties =
            new Function<JsonValue, JsonValue>() {
                @Override
                public JsonValue apply(JsonValue value) {
                    return value.get(AUTH_MODULE_PROPERTIES_KEY);
                }
            };

    /** A {link Predicate} that validates an auth module's properties for the purposes of instantiating an Authenticator */
    private static final Predicate<JsonValue> authModulesThatHaveValidAuthenticatorProperties =
            new Predicate<JsonValue>() {
                @Override
                public boolean apply(JsonValue jsonValue) {
                    // must have "queryOnResource"
                    return jsonValue.get(QUERY_ON_RESOURCE).isString()
                        // must either not have "queryId"
                        && (jsonValue.get(QUERY_ID).isNull()
                                // or have "queryId" + "authenticationId"/"userCredential" property mapping
                                || (jsonValue.get(QUERY_ID).isString()
                                    && jsonValue.get(PROPERTY_MAPPING).get(AUTHENTICATION_ID).isString()
                                    && jsonValue.get(PROPERTY_MAPPING).get(USER_CREDENTIAL).isString()));
                }
            };

    /**
     * Activates this component.
     *
     * @param context The ComponentContext
     */
    @Activate
    public synchronized void activate(ComponentContext context) {
        logger.info("Activating Authentication Service with configuration {}", context.getProperties());
        config = enhancedConfig.getConfigurationAsJson(context);

        // factory Function that instantiates an Authenticator from an auth module's config properties
        toAuthenticatorFromProperties = new AuthenticatorFactory(connectionFactory, cryptoService);

        // the auth module list config lives under at /serverAuthConfig/authModule
        final JsonValue authModuleConfig = config.get(SERVER_AUTH_CONTEXT_KEY).get(AUTH_MODULES_KEY);

        // filter enabled module configs and get their properties;
        // then filter those with valid auth properties, and build an authenticator
        for (final Authenticator authenticator :
                FluentIterable.from(authModuleConfig)
                .filter(enabledAuthModules)
                .transform(toModuleProperties)
                .filter(authModulesThatHaveValidAuthenticatorProperties)
                .transform(toAuthenticatorFromProperties)) {
            authenticators.add(authenticator);
        }

        logger.debug("OpenIDM Config for Authentication {} is activated.", config.get(Constants.SERVICE_PID));
    }

    /**
     * Nulls the stored authentication JsonValue.
     *
     * @param context The ComponentContext.
     */
    @Deactivate
    public void deactivate(ComponentContext context) {
        logger.debug("OpenIDM Config for Authentication {} is deactivated.", config.get(Constants.SERVICE_PID));
        config = null;
        toAuthenticatorFromProperties = null;
        authenticators.clear();
    }

    // ----- Implementation of AuthenticationConfig interface

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue getConfig() {
        return config;
    }

    // ----- Implementation of SingletonResourceProvider interface

    private enum Action { reauthenticate };

    /**
     * Action support, including reauthenticate action {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            if (Action.reauthenticate.equals(request.getActionAsEnum(Action.class))) {
                if (context.containsContext(HttpContext.class)
                        && context.containsContext(SecurityContext.class)) {
                    String authcid = context.asContext(SecurityContext.class).getAuthenticationId();
                    HttpContext httpContext = context.asContext(HttpContext.class);
                    String password = httpContext.getHeaderAsString(HEADER_REAUTH_PASSWORD);
                    if (StringUtils.isBlank(authcid) || StringUtils.isBlank(password)) {
                        logger.debug("Reauthentication failed, missing or empty headers");
                        throw new ForbiddenException("Reauthentication failed, missing or empty headers");
                    }

                    for (Authenticator authenticator : authenticators) {
                        try {
                            if (authenticator.authenticate(authcid, password, context).isAuthenticated()) {
                                JsonValue result = new JsonValue(new HashMap<String, Object>());
                                result.put("reauthenticated", true);
                                handler.handleResult(result);
                                return;
                            }
                        } catch (ResourceException e) {
                            // log error and try next authentication mechanism
                            logger.debug("Reauthentication failed: {}", e.getMessage());
                        }
                    }

                    throw new ForbiddenException("Reauthentication failed for " + authcid);
                } else {
                    throw new InternalServerErrorException("Failure to reauthenticate - missing context");
                }
            } else {
                throw new IllegalArgumentException();
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (IllegalArgumentException e) { // from getActionAsEnum
            handler.handleError(
                    new BadRequestException(
                            "Action " + request.getAction() + " on authentication service not supported"));
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Read operations are not supported");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update operations are not supported");
        handler.handleError(e);
    }
}
