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

import static org.forgerock.caf.authentication.framework.AuthenticationFilter.AuthenticationModuleBuilder.configureModule;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_LOADER;
import static org.forgerock.jaspi.modules.session.jwt.JwtSessionModule.LOGOUT_SESSION_REQUEST_ATTRIBUTE_NAME;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.JsonValueFunctions.enumConstant;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.*;
import static org.forgerock.openidm.idp.impl.IdentityProviderService.withoutClientSecret;

import javax.inject.Provider;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.api.annotations.Actions;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.framework.AuthenticationFilter;
import org.forgerock.caf.authentication.framework.AuthenticationFilter.AuthenticationModuleBuilder;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.forgerock.http.Client;
import org.forgerock.http.Filter;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.apache.async.AsyncHttpClientProvider;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.spi.Loader;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openidm.auth.api.GetAuthTokenActionRequest;
import org.forgerock.openidm.auth.api.GetAuthTokenActionResponse;
import org.forgerock.openidm.auth.api.LogoutActionResponse;
import org.forgerock.openidm.auth.api.ReauthenticateActionResponse;
import org.forgerock.openidm.auth.modules.IDMAuthModule;
import org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.idp.impl.api.IdentityProviderServiceResourceWithNoSecret;
import org.forgerock.openidm.keystore.SharedKeyService;
import org.forgerock.openidm.idp.client.OAuthHttpClient;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.idp.impl.IdentityProviderListener;
import org.forgerock.openidm.idp.impl.IdentityProviderService;
import org.forgerock.openidm.idp.impl.IdentityProviderServiceException;
import org.forgerock.openidm.idp.impl.ProviderConfigMapper;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.util.HeaderUtil;
import org.forgerock.openidm.util.JettyPropertyUtil;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.util.Options;
import org.forgerock.util.encode.Base64;
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
@SingletonProvider(@Handler(
        id = "authenticationService:0",
        title = "Authentication",
        description = "Utilities related to authentication.",
        mvccSupported = false,
        resourceSchema = @Schema(fromType = IdentityProviderServiceResourceWithNoSecret.class)))
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

    private static final JwtReconstruction jwtReconstruction = new JwtReconstruction();
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    /** Re-authentication password header. */
    private static final String HEADER_REAUTH_PASSWORD = "X-OpenIDM-Reauth-Password";

    /** The serverAuthContext key in the authentication config. */
    public static final String SERVER_AUTH_CONTEXT_KEY = "serverAuthContext";
    /** The sessionModule key in the authentication config. */
    public static final String SESSION_MODULE_KEY = "sessionModule";
    /** The authModules key in the authentication config. */
    public static final String AUTH_MODULES_KEY = "authModules";
    /** The properties key within an auth module stanza in the authentication config. */
    public static final String AUTH_MODULE_PROPERTIES_KEY = "properties";
    /** The propertyMapping key within an auth module stanza in the authentication config. */
    public static final String AUTH_MODULE_PROPERTY_MAPPING_KEY = "propertyMapping";
    /** The queryOnResource key within an auth module stanza in the authentication config. */
    public static final String AUTH_MODULE_QUERY_ON_RESOURCE = "queryOnResource";
    /** The name key within an auth module stanza in the authentication config. */
    public static final String AUTH_MODULE_NAME_KEY = "name";
    /** The className key within an auth module stanza in the authentication config. */
    public static final String AUTH_MODULE_CLASS_NAME_KEY = "className";
    /** The enabled key within an auth module stanza in the authentication config. */
    public static final String AUTH_MODULE_CONFIG_ENABLED = "enabled";
    /** The resolvers key within an auth module stanza in the authentication config. */
    public static final String AUTH_MODULE_RESOLVERS_KEY = "resolvers";

    private static final String SOCIAL_PROVIDERS = "SOCIAL_PROVIDERS";
    private static final String MANAGED = "managed";

    /**
     * Configuration as it is represented in the authentication.json file.
     */
    private JsonValue config;

    /**
     * Amended configuration of what the {@link AuthenticationService} configuration is in memory.
     * It has all the injected configuration from the services it depends on. We need this because if
     * SOCIAL_PROVIDERS configuration is present in the authentication.json it needs to auto populate
     * all the associated auth modules (OAUTH and OPENID_CONNECT) and remove the SOCIAL_PROVIDERS
     * authentication module in memory only so that it does not get initialized.
     */
    private JsonValue amendedConfig;

    /** The authenticators to delegate to.*/
    private List<Authenticator> authenticators = new ArrayList<>();

    // ----- Declarative Service Implementation

    @Reference
    CryptoService cryptoService;

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

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private volatile IdentityProviderService identityProviderService;

    void bindIdentityProviderService(IdentityProviderService identityProviderService) {
        this.identityProviderService = identityProviderService;
        identityProviderService.registerIdentityProviderListener(this);
    }

    void unbindIdentityProviderService() {
        identityProviderService.unregisterIdentityProviderListener(this);
        identityProviderService = null;
    }

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
                    return jsonValue.get(AUTH_MODULE_CONFIG_ENABLED).defaultTo(true).asBoolean();
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

    /** A {@link Predicate} that determines if an auth module is either OPENID_CONNECT or OAUTH */
    public static final Predicate<JsonValue> oidcAndOauth2Modules =
            new Predicate<JsonValue>() {
                @Override
                public boolean apply(JsonValue jsonValue) {
                    return jsonValue.get(AUTH_MODULE_NAME_KEY).asString().equals(IDMAuthModule.OPENID_CONNECT.name())
                            || jsonValue.get(AUTH_MODULE_NAME_KEY).asString().equals(IDMAuthModule.OAUTH.name());
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

    /** A {@link Function} that converts a JsonValue to a Map */
    private static final Function<JsonValue, Map<String, Object>> jsonValueToMap =
            new Function<JsonValue, Map<String, Object>>() {
                @Override
                public Map<String, Object> apply(JsonValue jsonValue) {
                    return jsonValue.asMap();
                }
            };

    /** A {@link Function} that returns the auth module resolvers as JsonValue */
    private static final Function<JsonValue, JsonValue> resolvers = new Function<JsonValue, JsonValue>() {
        @Override
        public JsonValue apply(JsonValue jsonValue) {
            setType.apply(jsonValue);
            return jsonValue.get(AUTH_MODULE_PROPERTIES_KEY).get(AUTH_MODULE_RESOLVERS_KEY);
        }
    };

    /** A {@link Predicate} that returns whether the resolver is enabled */
    private static final Predicate<JsonValue> enabledResolvers =
            new Predicate<JsonValue>() {
                @Override
                public boolean apply(JsonValue jsonValue) {
                    return jsonValue.get(AUTH_MODULE_CONFIG_ENABLED).defaultTo(true).asBoolean();
                }
            };

    /** A {@link Predicate} that returns whether the idp config is associated with the provider name */
    private static final Predicate<JsonValue> forProvider(final String providerName) {
        return new Predicate<JsonValue>() {
            @Override
            public boolean apply(JsonValue jsonValue) {
                return jsonValue.get(AUTH_MODULE_NAME_KEY).asString().equals(providerName);
            }
        };
    }

    /** A {@link Function} that sets the type of the resolver from the auth module name */
    private static final Function<JsonValue, JsonValue> setType = new Function<JsonValue, JsonValue>() {
        @Override
        public JsonValue apply(JsonValue jsonValue) {
            final JsonValue resolvers = jsonValue.get(AUTH_MODULE_PROPERTIES_KEY).get(AUTH_MODULE_RESOLVERS_KEY);
            if (resolvers.isNotNull()) {
                // currently we only support one resolver per auth module
                return resolvers.get(0).put("type", jsonValue.get(AUTH_MODULE_NAME_KEY).asString());
            }
            // return with no modification
            return jsonValue;
        }
    };

    /**
     * Returns the SOCIAL_PROVIDERS template if one
     * is available and it is enabled.
     *
     * @return social auth template as JsonValue
     */
    private JsonValue getSocialAuthTemplate() {
        for (final JsonValue authModule : config.get(SERVER_AUTH_CONTEXT_KEY).get(AUTH_MODULES_KEY)) {
            if (authModule.get(AUTH_MODULE_NAME_KEY).asString().equals(SOCIAL_PROVIDERS)
                    && authModule.get(AUTH_MODULE_CONFIG_ENABLED).defaultTo(true).asBoolean()) {
                return authModule.copy();
            }
        }
        return json(object());
    }

    /**
     * Amends the config so that the we generate the appropriate
     * OPENID_CONNECT and OAUTH modules.
     *
     * @param authModuleConfig configuration of the authentication modules.
     */
    void amendAuthConfig(final JsonValue authModuleConfig) {
        final JsonValue socialAuthTemplate = getSocialAuthTemplate();
        if (socialAuthTemplate.asMap().isEmpty()) {
            // because we don't have a SOCIAL_PROVIDER configured
            // or it's not enabled, no need to modify config
            // or no configured identityProviders found to inject
            logger.debug("SOCIAL_PROVIDERS module was not found.");
            return;
        }

        // We need to remove the SOCIAL_PROVIDERS module from the list because it isn't an authentic
        // module, it doesn't have an implementation, it is used as a template to create
        // OAUTH and OPENID_CONNECT auth modules
        authModuleConfig.asList(Map.class).remove(socialAuthTemplate.asMap());

        if (identityProviderService != null) {
            authModuleConfig.asList().addAll(
                    FluentIterable.from(identityProviderService.getIdentityProviders())
                            .transform(new SocialAuthModuleConfigFactory(socialAuthTemplate))
                            .toList());
        }
    }

    /**
     * Factory used to create OPENID_CONNECT and OAUTH auth module configurations.
     */
    private class SocialAuthModuleConfigFactory implements Function<ProviderConfig, Map<String, Object>> {

        /** Header used to create OPENID_CONNECT Auth module */
        private static final String OPENID_CONNECT_HEADER = "openIdConnectHeader";

        /** Headers used to create OAUTH Module */
        private static final String AUTH_TOKEN_HEADER = "authTokenHeader";
        private static final String AUTH_RESOLVER_HEADER = "authResolverHeader";
        private static final String PROVIDER = "provider";

        private static final String AUTH_TOKEN = "authToken";
        private static final String IDP_DATA = "idpData";
        private static final String SUBJECT = "subject";

        private final JsonValue socialAuthModuleTemplate;

        SocialAuthModuleConfigFactory(final JsonValue socialAuthModuleTemplate) {
            this.socialAuthModuleTemplate = socialAuthModuleTemplate;
        }

        @Override
        public Map<String, Object> apply(final ProviderConfig providerConfig) {
            final JsonValue authModule = socialAuthModuleTemplate.copy();
            final JsonValue properties = authModule.get(AUTH_MODULE_PROPERTIES_KEY);
            switch (IDMAuthModule.valueOf(providerConfig.getType())) {
            case OPENID_CONNECT:
                authModule.put(AUTH_MODULE_NAME_KEY, providerConfig.getType());
                properties.put(AUTH_MODULE_RESOLVERS_KEY,
                        array(ProviderConfigMapper.toJsonValue(providerConfig).asMap()));
                properties.put(AUTH_MODULE_QUERY_ON_RESOURCE, MANAGED + "/" + providerConfig.getName());
                properties.put(new JsonPointer(AUTH_MODULE_PROPERTY_MAPPING_KEY).child(AUTHENTICATION_ID),
                        providerConfig.getAuthenticationId());
                properties.put(OPENID_CONNECT_HEADER, AUTH_TOKEN);
                authModule.put(AUTH_MODULE_CONFIG_ENABLED, providerConfig.isEnabled());
                return authModule.asMap();
            case OAUTH:
                authModule.put(AUTH_MODULE_NAME_KEY, providerConfig.getType());
                properties.put(AUTH_MODULE_RESOLVERS_KEY,
                        array(ProviderConfigMapper.toJsonValue(providerConfig).asMap()));
                properties.put(AUTH_MODULE_QUERY_ON_RESOURCE, MANAGED + "/" + providerConfig.getName());
                properties.put(new JsonPointer(AUTH_MODULE_PROPERTY_MAPPING_KEY).child(AUTHENTICATION_ID),
                        providerConfig.getAuthenticationId());
                properties.put(AUTH_TOKEN_HEADER, AUTH_TOKEN);
                properties.put(AUTH_RESOLVER_HEADER, PROVIDER);
                authModule.put(AUTH_MODULE_CONFIG_ENABLED, providerConfig.isEnabled());
                return authModule.asMap();
            default :
                // we will never get here because Enum.ValueOf will throw IllegalArgumentException
                // if we do not recognize the authentication module type
                return null;
            }
        }
    }

    /** Implementation of IdentityProviderListener */

    @Override
    public String getListenerName() {
        return PID;
    }

    @Override
    public void identityProviderConfigChanged() throws IdentityProviderServiceException {
        if (config == null) {
            logger.debug("No configuration for Authentication Service");
            return;
        }
        amendedConfig = config.copy();
        // the auth module list config lives under at /serverAuthConfig/authModule
        final JsonValue authModuleConfig = amendedConfig.get(SERVER_AUTH_CONTEXT_KEY).get(AUTH_MODULES_KEY);
        amendAuthConfig(authModuleConfig);

        try {
            authFilterWrapper.setFilter(configureAuthenticationFilter(amendedConfig));
        } catch (AuthenticationException e) {
            logger.debug("Error in configuration for Authentication Service. Filter not set.", e);
            throw new IdentityProviderServiceException(e.getMessage(), e);
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
    public void activate(final ComponentContext context)
            throws AuthenticationException, IdentityProviderServiceException {
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
                final Key key = sharedKeyService.getSharedKey(
                        IdentityServer.getInstance().getProperty(
                                ServerConstants.JWTSESSION_SIGNING_KEY_ALIAS_PROPERTY,
                                ServerConstants.DEFAULT_JWTSESSION_SIGNING_KEY_ALIAS));
                final String signingKey = Base64.encode(key.getEncoded());
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

        if (moduleConfig.isDefined(AUTH_MODULE_CONFIG_ENABLED) && !moduleConfig.get(AUTH_MODULE_CONFIG_ENABLED).asBoolean()) {
            return null;
        }
        moduleConfig.remove(AUTH_MODULE_CONFIG_ENABLED);

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

        JsonValue moduleProperties = moduleConfig.get(AUTH_MODULE_PROPERTIES_KEY);
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

    enum Action {reauthenticate, getAuthToken, logout}

    /**
     * Action support, including reauthenticate action {@inheritDoc}
     */
    @Actions({
            @org.forgerock.api.annotations.Action(
                    operationDescription = @Operation(
                            description ="Returns an auth-token used for OpenID Connect or OAuth flow.",
                            errors = {
                                    @ApiError(
                                            code = 400,
                                            description = "Bad request"),
                                    @ApiError(
                                            code = 404,
                                            description = "Identity provider not found"),
                                    @ApiError(
                                            code = 404,
                                            description = "Unable to retrieve token"),
                                    @ApiError(
                                            code = 500,
                                            description = "Unexpected condition"),
                            }),
                    name = "getAuthToken",
                    request = @Schema(fromType = GetAuthTokenActionRequest.class),
                    response = @Schema(fromType = GetAuthTokenActionResponse.class)
            ),
            @org.forgerock.api.annotations.Action(
                    operationDescription = @Operation(description = "Invalidates the current JWT cookie's value."),
                    name = "logout",
                    response = @Schema(fromType = LogoutActionResponse.class)
            ),
            @org.forgerock.api.annotations.Action(
                    operationDescription = @Operation(
                            description = "Reauthenticates a session using the provided \"X-OpenIDM-Reauth-Password\""
                                    + " header. RFC 5987 encoding can optionally be used with UTF-8 or ISO-8859-1."
                                    + " Note that the API Explorer cannot currently input this header field,"
                                    + " but that it does work if provided using traditional REST clients.",
                            errors = {
                                    @ApiError(
                                            code = 403,
                                            description = "Reauthentication failure")
                            }),
                    name = "reauthenticate",
                    response = @Schema(fromType = ReauthenticateActionResponse.class)
            )
    })
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
        try {
            switch (request.getActionAsEnum(Action.class)) {
                case reauthenticate:
                    if (context.containsContext(HttpContext.class)
                            && context.containsContext(SecurityContext.class)) {
                        String authcid = context.asContext(SecurityContext.class).getAuthenticationId();
                        HttpContext httpContext = context.asContext(HttpContext.class);
                        String password = httpContext.getHeaderAsString(HEADER_REAUTH_PASSWORD);
                        try {
                            password = HeaderUtil.decodeRfc5987(password);
                        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                            logger.debug("Malformed RFC 5987 value for {} with authcid: {}",
                                    HEADER_REAUTH_PASSWORD, authcid, e);
                        }
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
                case getAuthToken:
                    final String authToken =
                            new OAuthHttpClient(
                                    getIdentityProviderConfig(request.getContent()
                                            .get(OAuthHttpClient.PROVIDER).required().asString()),
                                    newHttpClient())
                            .getAuthToken(
                                    jwtReconstruction,
                                    request.getContent().get(OAuthHttpClient.CODE).required().asString(),
                                    request.getContent().get(OAuthHttpClient.NONCE).required().asString(),
                                    request.getContent().get(OAuthHttpClient.REDIRECT_URI).required().asString())
                            .getOrThrow();
                    // get auth token
                    return newActionResponse(json(object(field(OAuthHttpClient.AUTH_TOKEN, authToken)))).asPromise();
                case logout:
                    // adding the logout attribute will instruct CAF to clobber the JWT cookie's value.
                    context.asContext(AttributesContext.class).getAttributes()
                            .put(LOGOUT_SESSION_REQUEST_ATTRIBUTE_NAME, true);
                    return newActionResponse(json(object(field("success", true)))).asPromise();
                default:
                    return new BadRequestException("Action " + request.getAction() +
                            " on authentication service not supported").asPromise();
            }
        }  catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException("Error processing action", e).asPromise();
        }
    }

    /**
     * Returns a {@link ProviderConfig} for a specific identity provider iff the identity provider
     * is enabled and available for authentication.
     *
     * @param providerName name of the identity provider
     * @return a ProviderConfig if one is available
     * @throws NotFoundException if no identity provider has been found
     *         associated with that provider name
     */
    private ProviderConfig getIdentityProviderConfig(final String providerName) throws NotFoundException {
        final Optional<ProviderConfig> providerConfig = FluentIterable
                .from(amendedConfig.get(SERVER_AUTH_CONTEXT_KEY).get(AUTH_MODULES_KEY))
                .filter(oidcAndOauth2Modules)
                .transformAndConcat(resolvers)
                .filter(enabledResolvers)
                .filter(forProvider(providerName))
                .transform(setType)
                .transform(ProviderConfigMapper.toProviderConfig)
                .first();

        if (providerConfig.isPresent()) {
            return providerConfig.get();
        } else {
            throw new NotFoundException("Identity provider " + providerName + " was not found.");
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
    @Read(operationDescription = @Operation(description = "Lists enabled OpenID Connect and OAuth modules."))
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        if (amendedConfig == null) {
            return newResourceResponse(null, null,
                        json(object(
                                field(IdentityProviderService.PROVIDERS, array()))))
                    .asPromise();
        }
        return newResourceResponse(null, null,
                    json(object(
                            field(IdentityProviderService.PROVIDERS, FluentIterable
                                .from(amendedConfig.get(SERVER_AUTH_CONTEXT_KEY).get(AUTH_MODULES_KEY))
                                .filter(enabledAuthModules)
                                .filter(oidcAndOauth2Modules)
                                .transformAndConcat(resolvers)
                                .filter(enabledResolvers)
                                .transform(withoutClientSecret)
                                .transform(jsonValueToMap)
                                .toList()))))
                .asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        return new NotSupportedException("Update operation not supported").asPromise();
    }

    /**
     * Sets the configuration.
     * This is created for testing purpose only.
     */
    void setConfig(final JsonValue config) {
        this.config = config;
    }


    /**
     * Sets the amendedConfiguration.
     * This is only used for testing.
     */
    void setAmendedConfig(final JsonValue config) {
        this.amendedConfig = config;
    }

    private Client newHttpClient() throws HttpApplicationException {
        return new Client(
                new HttpClientHandler(
                        Options.defaultOptions()
                                .set(OPTION_LOADER, new Loader() {
                                    @Override
                                    public <S> S load(Class<S> service, Options options) {
                                        return service.cast(new AsyncHttpClientProvider());
                                    }
                                })));
    }
}
