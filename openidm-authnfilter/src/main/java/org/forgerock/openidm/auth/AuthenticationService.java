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
 * Copyright 2013-2016 ForgeRock AS
 */

package org.forgerock.openidm.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Provider;

import static org.forgerock.json.JsonValueFunctions.enumConstant;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.AUTHENTICATION_ID;
import static org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.PROPERTY_MAPPING;
import static org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.QUERY_ID;
import static org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.QUERY_ON_RESOURCE;
import static org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.USER_CREDENTIAL;
import static org.forgerock.caf.authentication.framework.AuthenticationFilter.AuthenticationModuleBuilder.configureModule;

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

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.framework.AuthenticationFilter;
import org.forgerock.caf.authentication.framework.AuthenticationFilter.AuthenticationModuleBuilder;
import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.base.Optional;
import org.forgerock.guava.common.base.Predicate;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.http.Filter;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.SharedKeyService;
import org.forgerock.openidm.crypto.util.JettyPropertyUtil;
import org.forgerock.openidm.auth.modules.IDMAuthModule;
import org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.idp.impl.IdentityProviderListener;
import org.forgerock.openidm.idp.impl.IdentityProviderService;
import org.forgerock.openidm.idp.impl.ProviderConfigMapper;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Configures the authentication chains based on authentication.json.
 *
 * Example:
 *
 * <pre>
 *     <code>
 * {
 *     "serverAuthConfig" : {
 *         "sessionModule" : {
 *             "name" : "JWT_SESSION",
 *                 "properties" : {
 *                     "someSetting" : "some-value"
 *                 }
 *         },
 *         "authModules" : [
 *             {
 *                 "name" : "IWA",
 *                 "properties" : {
 *                     "someSetting" : "some-value"
 *                 }
 *             },
 *             {
 *                 "name" : "PASSTHROUGH",
 *                 "properties" : {
 *                     "someSetting" : "some-value"
 *                 }
 *             }
 *         ]
 *     }
 * }
 *     </code>
 * </pre>
 */
@Component(name = AuthenticationService.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Authentication Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "/authentication")
})
public class AuthenticationService implements SingletonResourceProvider, IdentityProviderListener {

    /** The PID for this Component. */
    public static final String PID = "org.forgerock.openidm.authentication";

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    /** Re-authentication password header. */
    private static final String HEADER_REAUTH_PASSWORD = "X-OpenIDM-Reauth-Password";

    private static final String SERVER_AUTH_CONTEXT_KEY = "serverAuthContext";
    private static final String SESSION_MODULE_KEY = "sessionModule";
    private static final String AUTH_MODULES_KEY = "authModules";
    private static final String AUTH_MODULE_PROPERTIES_KEY = "properties";
    private static final String AUTH_MODULE_NAME_KEY = "name";
    private static final String AUTH_MODULE_CLASS_NAME_KEY = "className";
    private static final String MODULE_CONFIG_ENABLED = "enabled";
    private static final String RESOLVERS = "resolvers";
    private static final String RESOLVER_NAME_KEY = "name";

    /** the encoded key location in the return value from {@link SharedKeyService#getSharedKey(String)} */
    private static final JsonPointer ENCODED_SECRET_PTR = new JsonPointer("/secret/encoded");

    private JsonValue config;

    /** The authenticators to delegate to.*/
    private List<Authenticator> authenticators = new ArrayList<>();

    // ----- Declarative Service Implementation

    @Reference(policy = ReferencePolicy.DYNAMIC)
    volatile CryptoService cryptoService;

    @Reference
    SharedKeyService sharedKeyService;

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC)
    protected IDMConnectionFactory connectionFactory;

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected volatile ScriptRegistry scriptRegistry;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    /** The CHF filter to wrap the CAF filter */
    @Reference(policy = ReferencePolicy.DYNAMIC, target="(service.pid=org.forgerock.openidm.auth.config)")
    private volatile AuthFilterWrapper authFilterWrapper;

    /** An on-demand Provider for the ConnectionFactory */
    private final Provider<ConnectionFactory> connectionFactoryProvider =
            new Provider<ConnectionFactory>() {
                @Override
                public ConnectionFactory get() {
                    return connectionFactory;
                }
            };

    /** An on-demand Provider for the CryptoService */
    private final Provider<CryptoService> cryptoServiceProvider =
            new Provider<CryptoService>() {
                @Override
                public CryptoService get() {
                    return cryptoService;
                }
            };

    /** a factory Function to build an Authenticator from an auth module config */
    private final AuthenticatorFactory toAuthenticatorFromProperties =
            new AuthenticatorFactory(connectionFactoryProvider, cryptoServiceProvider);

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
     * Amends the config so that the OPENID_CONNECT
     * module has the appropriate resolvers.
     *
     * @param authModuleConfig configuration of the authentication modules.
     */
    void amendAuthConfig(JsonValue authModuleConfig) {
        if (identityProviderService.getIdentityProviders().isEmpty()) {
            // no configured identityProviders found to inject
            return;
        }
        // get OpenIDConnect Properties
        Optional<JsonValue> openidConnectModulePropertiesOption = FluentIterable.from(authModuleConfig)
                .firstMatch(new Predicate<JsonValue>() {
                    @Override
                    public boolean apply(JsonValue authModuleConfig) {
                        return authModuleConfig.get(AUTH_MODULE_NAME_KEY).asString()
                                .equals(IDMAuthModule.OPENID_CONNECT.name());
                    }
                });

        if (openidConnectModulePropertiesOption.isPresent()) {
            JsonValue openidConnectModuleProperties = openidConnectModulePropertiesOption
                    .get()
                    .get(AUTH_MODULE_PROPERTIES_KEY);

            if (openidConnectModuleProperties.isNotNull()) {
                // populate the resolvers from IdentityProviderConfigs
                openidConnectModuleProperties.put(RESOLVERS,
                        ProviderConfigMapper
                                .toJsonValue(identityProviderService.getIdentityProviders())
                                .asList()
                );
            }
        }
    }


    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile IdentityProviderService identityProviderService;

    protected void bindIdentityProviderService(IdentityProviderService identityProviderService) {
        this.identityProviderService = identityProviderService;
        identityProviderService.registerIdentityProviderListener(this);
    }

    protected void unbindIdentityProviderService() {
        identityProviderService.unregisterIdentityProviderListener(this);
        identityProviderService = null;
    }

    /** Implementation of IdentityProviderListener */

    @Override
    public String getListenerName() {
        return PID;
    }

    @Override
    public void identityProviderConfigChanged() {
        if (config == null) {
            logger.debug("No configuration for Authentication Service");
            return;
        }
        // the auth module list config lives under at /serverAuthConfig/authModule
        JsonValue authModuleConfig = config.get(SERVER_AUTH_CONTEXT_KEY).get(AUTH_MODULES_KEY);
        amendAuthConfig(authModuleConfig);

        try {
            authFilterWrapper.setFilter(configureAuthenticationFilter(config));
        } catch (AuthenticationException e) {
            logger.error("Error configuring authentication filter.", e);
        }

        // filter enabled module configs and get their properties;
        // then filter those with valid auth properties, and build an authenticator
        authenticators.clear();
        authenticators.addAll(FluentIterable.from(authModuleConfig)
                .filter(enabledAuthModules)
                .transform(toModuleProperties)
                .filter(authModulesThatHaveValidAuthenticatorProperties)
                .transform(toAuthenticatorFromProperties)
                .toList());
    }

    /**
     * Activates this component.
     *
     * @param context The ComponentContext
     */
    @Activate
    public void activate(final ComponentContext context) throws AuthenticationException {
        logger.info("Activating Authentication Service with configuration {}", context.getProperties());
        config = enhancedConfig.getConfigurationAsJson(context);
        identityProviderConfigChanged();
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
        authenticators.clear();

        // remove CAF filter from CHF filter wrapper
        if (authFilterWrapper != null) {
            try {
                authFilterWrapper.reset();
            } catch (Exception ex) {
                logger.warn("Failure reported during unregistering of authentication filter: {}", ex.getMessage(), ex);
            }
        }
    }

    /**
     * Configures the commons Authentication Filter with the given configuration.
     *
     * @param jsonConfig The authentication configuration.
     * @return the CAF filter produced from the json config
     * @throws AuthenticationException on missing or incorrect configuration, or failure to construct an auth module
     *      from the config
     */
    private Filter configureAuthenticationFilter(JsonValue jsonConfig) throws AuthenticationException {
        if (jsonConfig == null || jsonConfig.size() == 0) {
            throw new AuthenticationException("No auth modules configured");
        }

        // make copy of config
        final JsonValue moduleConfig = jsonConfig.copy();
        final JsonValue serverAuthContext = moduleConfig.get(SERVER_AUTH_CONTEXT_KEY).required();
        final JsonValue sessionConfig = serverAuthContext.get(AuthenticationService.SESSION_MODULE_KEY);
        final JsonValue authModulesConfig = serverAuthContext.get(AuthenticationService.AUTH_MODULES_KEY);

        if (sessionConfig.get(AUTH_MODULE_PROPERTIES_KEY).get(JwtSessionModule.HMAC_SIGNING_KEY).isNull()) {
            try {
                // amend session config to include the hmac key stored in the keystore
                String signingKey = sharedKeyService.getSharedKey(
                        IdentityServer.getInstance().getProperty(
                                ServerConstants.JWTSESSION_SIGNING_KEY_ALIAS_PROPERTY,
                                ServerConstants.DEFAULT_JWTSESSION_SIGNING_KEY_ALIAS))
                        .get(ENCODED_SECRET_PTR)
                        .required()
                        .asString();
                sessionConfig.get(AUTH_MODULE_PROPERTIES_KEY).put(JwtSessionModule.HMAC_SIGNING_KEY, signingKey);
            } catch (Exception e) {
                throw new AuthenticationException("Cannot read hmac signing key", e);
            }
        }

        final List<AuthenticationModuleBuilder> authModuleBuilders = new ArrayList<>();
        for (final JsonValue authModuleConfig : authModulesConfig) {
            AuthenticationModuleBuilder moduleBuilder = processModuleConfiguration(authModuleConfig);
            if (moduleBuilder != null) {
                authModuleBuilders.add(moduleBuilder);
            }
        }

        return AuthenticationFilter.builder()
                .logger(logger)
                .auditApi(new IDMAuditApi(connectionFactory))
                .sessionModule(processModuleConfiguration(sessionConfig))
                .authModules(authModuleBuilders)
                .build();
    }

    /**
     * Process the module configuration for a specific module, checking to see if the module is enabled and
     * resolving the module class name if an alias is used.
     *
     * @param moduleConfig The specific module configuration json.
     * @return Whether the module is enabled or not.
     */
    private AuthenticationModuleBuilder processModuleConfiguration(JsonValue moduleConfig)
            throws AuthenticationException {

        if (moduleConfig.isDefined(MODULE_CONFIG_ENABLED) && !moduleConfig.get(MODULE_CONFIG_ENABLED).asBoolean()) {
            return null;
        }
        moduleConfig.remove(MODULE_CONFIG_ENABLED);

        AsyncServerAuthModule module;
        if (moduleConfig.isDefined(AUTH_MODULE_NAME_KEY)) {
            module = moduleConfig.get(AUTH_MODULE_NAME_KEY).as(enumConstant(IDMAuthModule.class))
                    .newInstance(toAuthenticatorFromProperties);
        } else if (moduleConfig.isDefined(AUTH_MODULE_CLASS_NAME_KEY)) {
            module = constructAuthModuleByClassName(moduleConfig.get(AUTH_MODULE_CLASS_NAME_KEY).asString());
        } else {
            logger.warn("Unable to create auth module from config " + moduleConfig.toString());
            throw new AuthenticationException("Auth module config lacks 'name' and 'className' attribute");
        }

        JsonValue moduleProperties = moduleConfig.get("properties");
        if (moduleProperties.isDefined("privateKeyPassword")) {
            // decrypt/de-obfuscate privateKey password
            moduleProperties.put("privateKeyPassword",
                    JettyPropertyUtil.decryptOrDeobfuscate(moduleProperties.get("privateKeyPassword").asString()));
        }

        if (moduleProperties.isDefined("keystorePassword")) {
            // decrypt/de-obfuscate keystore password
            moduleProperties.put("keystorePassword",
                    JettyPropertyUtil.decryptOrDeobfuscate(moduleProperties.get("keystorePassword").asString()));
        }

        // wrap all auth modules in our wrapper to apply the IDM business logic
        return configureModule(new IDMAuthModuleWrapper(module, connectionFactory, cryptoService, scriptRegistry))
                .withSettings(moduleProperties.asMap());
    }

    private AsyncServerAuthModule constructAuthModuleByClassName(String authModuleClassName)
            throws AuthenticationException {
        try {
            return Class.forName(authModuleClassName).asSubclass(AsyncServerAuthModule.class).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            logger.error("Failed to construct Auth Module instance", e);
            throw new AuthenticationException("Failed to construct Auth Module instance", e);
        }
    }

    // ----- Implementation of SingletonResourceProvider interface

    private enum Action { reauthenticate }

    /**
     * Action support, including reauthenticate action {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
        try {
            if (Action.reauthenticate.equals(request.getActionAsEnum(Action.class))) {
                if (context.containsContext(HttpContext.class)
                        && context.containsContext(SecurityContext.class)) {
                    String authcid = context.asContext(SecurityContext.class).getAuthenticationId();
                    HttpContext httpContext = context.asContext(HttpContext.class);
                    String password = httpContext.getHeaderAsString(HEADER_REAUTH_PASSWORD);
                    if (StringUtils.isBlank(authcid) || StringUtils.isBlank(password)) {
                        logger.debug("Reauthentication failed, missing or empty headers");
                        return new ForbiddenException("Reauthentication failed, missing or empty headers")
                                .asPromise();
                    }

                    for (Authenticator authenticator : authenticators) {
                        try {
                            if (authenticator.authenticate(authcid, password, context).isAuthenticated()) {
                                JsonValue result = new JsonValue(new HashMap<String, Object>());
                                result.put("reauthenticated", true);
                                return newActionResponse(result).asPromise();
                            }
                        } catch (ResourceException e) {
                            // log error and try next authentication mechanism
                            logger.debug("Reauthentication failed: {}", e.getMessage());
                        }
                    }

                    return new ForbiddenException("Reauthentication failed for " + authcid).asPromise();
                } else {
                    return new InternalServerErrorException("Failure to reauthenticate - missing context").asPromise();
                }
            } else {
                return new BadRequestException("Action " + request.getAction() + " on authentication service not supported")
                        .asPromise();
            }
        } catch (IllegalArgumentException e) { // from getActionAsEnum
            return new BadRequestException("Action " + request.getAction() + " on authentication service not supported")
                    .asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException("Error processing action", e).asPromise();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        return new NotSupportedException("Patch operation not supported").asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        return new NotSupportedException("Read operation not supported").asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        return new NotSupportedException("Update operation not supported").asPromise();
    }
}
