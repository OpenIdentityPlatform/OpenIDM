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
 * Copyright 2015-2017 ForgeRock AS.
 */
package org.forgerock.openidm.selfservice.impl;

import static org.forgerock.http.handler.HttpClientHandler.OPTION_LOADER;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourcePath.resourcePath;
import static org.forgerock.openidm.core.ServerConstants.SELF_SERVICE_CERT_ALIAS;
import static org.forgerock.openidm.core.ServerConstants.SELF_SERVICE_DEFAULT_SHARED_KEY_ALIAS;
import static org.forgerock.openidm.core.ServerConstants.SELF_SERVICE_SHARED_KEY_PROPERTY;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

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
import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.apache.async.AsyncHttpClientProvider;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.spi.Loader;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.tokenHandler.TokenHandlerService;
import org.forgerock.openidm.keystore.SharedKeyService;
import org.forgerock.openidm.idp.impl.IdentityProviderListener;
import org.forgerock.openidm.idp.impl.IdentityProviderService;
import org.forgerock.openidm.idp.impl.IdentityProviderServiceException;
import org.forgerock.openidm.osgi.ComponentContextUtil;
import org.forgerock.openidm.selfservice.stage.AllInOneRegistrationConfig;
import org.forgerock.openidm.selfservice.stage.SocialUserClaimConfig;
import org.forgerock.openidm.selfservice.stage.IDMUserDetailsConfig;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.ProgressStageProvider;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.config.StageConfigException;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.json.JsonAnonymousProcessServiceBuilder;
import org.forgerock.selfservice.stages.captcha.CaptchaStageConfig;
import org.forgerock.selfservice.stages.terms.TermsAndConditionsConfig;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandler;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandlerConfig;
import org.forgerock.util.Function;
import org.forgerock.util.Options;
import org.forgerock.util.promise.NeverThrowsException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service supports self-registration, password reset, and forgotten username
 * per the Commons Self-Service stage configuration.
 */
@Component(name = SelfService.PID, immediate = true, configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Properties({
    @Property(name = "service.description", value = "OpenIDM SelfService Service"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
})
public class SelfService implements IdentityProviderListener {
    static final String PID = "org.forgerock.openidm.selfservice";

    private static final Logger LOGGER = LoggerFactory.getLogger(SelfService.class);

    /** the registered parent router-path for self-service flows */
    static final String ROUTER_PREFIX = "selfservice";

    /** this config key is present if the config represents a self-service process */
    private static final String STAGE_CONFIGS = "stageConfigs";

    /** config key present if config requires KBA questions */
    private static final String KBA_CONFIG = "kbaConfig";

    /** config key to enable allInOneRegistration config transform */
    private static final String ALL_IN_ONE_REGISTRATION = "allInOneRegistration";

    /** the shared key alias */
    public static final String SHARED_KEY_ALIAS =
            IdentityServer.getInstance().getProperty(
                    SELF_SERVICE_SHARED_KEY_PROPERTY,
                    SELF_SERVICE_DEFAULT_SHARED_KEY_ALIAS);

    // ----- Declarative Service Implementation

    /**
     * Use the external servlet connection factory so that self-service requests are subject to authz rules
     * as "external" requests.
     */
    @Reference(policy = ReferencePolicy.STATIC, target = ServerConstants.EXTERNAL_ROUTER_SERVICE_PID_FILTER)
    private ConnectionFactory connectionFactory;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    /** The KBA Configuration. */
    @Reference(policy = ReferencePolicy.STATIC)
    KbaConfiguration kbaConfiguration;

    /** The shared key service. Used to get the shared key for self service. */
    @Reference
    private SharedKeyService sharedKeyService;

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private volatile IdentityProviderService identityProviderService =
            IdentityProviderService.nullIdentityProviderService;

    void bindIdentityProviderService(IdentityProviderService service) {
        service.registerIdentityProviderListener(this);
        identityProviderService = service;
    }

    void unbindIdentityProviderService(IdentityProviderService service) {
        service.unregisterIdentityProviderListener(this);
        identityProviderService = IdentityProviderService.nullIdentityProviderService;
    }

    @Reference(policy = ReferencePolicy.STATIC)
    private PropertyMappingService mappingService;

    @Reference
    private TokenHandlerService tokenHandlerService;

    private Dictionary<String, Object> properties = null;
    private JsonValue config;
    private RequestHandler processService;
    private ServiceRegistration<RequestHandler> serviceRegistration = null;
    private ComponentContext context;
    private ProgressStageProvider progressStageProvider;
    final Function<JsonValue, JsonValue, NeverThrowsException> amendedConfig = new SelfServiceConfigAmendment(
            new Provider<IdentityProviderService>() {
                @Override
                public IdentityProviderService get() {
                    return identityProviderService;
                }
            },
            new Provider<KbaConfiguration>() {
                @Override
                public KbaConfiguration get() {
                    return kbaConfiguration;
                }
            });

    @Activate
    void activate(ComponentContext context) throws Exception {
        this.context = context;
        LOGGER.debug("Activating Service with configuration {}", context.getProperties());
        try {
            config = enhancedConfig.getConfigurationAsJson(context);
            // begin service registration prep

            String factoryPid = enhancedConfig.getConfigurationFactoryPid(context);
            if (StringUtils.isBlank(factoryPid)) {
                throw new IllegalArgumentException("Configuration must have property: "
                        + ServerConstants.CONFIG_FACTORY_PID);
            }

            // context.getProperties() is unmodifiable, so make a copy to add router prefix
            properties = ComponentContextUtil.getModifiableProperties(context);
            properties.put(ServerConstants.ROUTER_PREFIX,
                    resourcePath(ROUTER_PREFIX).concat(resourcePath(factoryPid)).toString());
            progressStageProvider = newProgressStageProvider(newHttpClient());
            identityProviderConfigChanged();

        } catch (Exception ex) {
            LOGGER.warn("Configuration invalid, can not start self-service.", ex);
            throw ex;
        }
        LOGGER.info("Self-service started.");
    }

    /**
     * If any of the 4 possible stages that must be part of an all-in-one registration stage are present as well as
     * the allInOneRegistration flag being set to true then replace any instances of those stage configs with a single
     * all-in-one stage config.  Those 4 stages are presently:
     *
     * <ul>
     *     <li>KBA</li>
     *     <li>Captcha</li>
     *     <li>Terms</li>
     *     <li>IDM User Details</li>
     * </ul>
     *
     * @param originalConfig the original process configuration as found on disk and/or in the repo
     * @return a potentially modified configuration with an all-in-one stage config in place of up to 4 others
     */
    JsonValue convertForAllInOneRegistration(JsonValue originalConfig) {
        if (!originalConfig.get(ALL_IN_ONE_REGISTRATION).defaultTo(false).asBoolean()) {
            return originalConfig;
        }

        JsonValue newConfig = originalConfig.clone();
        newConfig.put(STAGE_CONFIGS, array());
        JsonValue allInOneConfig = json(object(
                field("name", "allInOneRegistration"),
                field("configs", array())));

        for (JsonValue stageConfig : originalConfig.get(STAGE_CONFIGS)) {
            String stageName = stageConfig.get("name").asString();
            if (stageName.equals(IDMUserDetailsConfig.NAME)
                    || stageName.equals(CaptchaStageConfig.NAME)
                    || stageName.equals(TermsAndConditionsConfig.NAME)
                    || stageConfig.isDefined(KBA_CONFIG)) {
                allInOneConfig.get("configs").add(stageConfig.getObject());
            } else {
                newConfig.get(STAGE_CONFIGS).add(stageConfig.getObject());
            }
        }

        if (allInOneConfig.get("configs").asList().size() > 0) {
            List<Object> configs = newConfig.get(STAGE_CONFIGS).asList();
            configs.add(0, allInOneConfig.getObject());
            newConfig.put(STAGE_CONFIGS, configs);
            return newConfig;
        }

        return originalConfig;
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

    private ProgressStageProvider newProgressStageProvider(final Client httpClient) {
        return new ProgressStageProvider() {

            @Override
            public ProgressStage<StageConfig> get(Class<? extends ProgressStage<StageConfig>> progressStageClass) {
                Constructor<?>[] constructors = progressStageClass.getConstructors();

                if (constructors.length > 1) {
                    throw new StageConfigException("Only expected one constructor for the configured progress stage "
                            + progressStageClass);
                }

                try {
                    Constructor<? extends ProgressStage<StageConfig>> constructor =
                            progressStageClass.getConstructor(constructors[0].getParameterTypes());

                    Object[] parameters = getParameters(constructor);
                    return constructor.newInstance(parameters);

                } catch (NoSuchMethodException | InvocationTargetException |
                        IllegalAccessException | InstantiationException e) {
                    throw new StageConfigException("Unable to instantiate the configured progress stage", e);
                }
            }

            private Object[] getParameters(Constructor<?> constructor) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                Object[] parameters = new Object[parameterTypes.length];

                // Bind constructor parameters to stages
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (parameterTypes[i].equals(ConnectionFactory.class)) {
                        parameters[i] = connectionFactory;
                    } else if (parameterTypes[i].equals(Client.class)) {
                        parameters[i] = httpClient;
                    } else if (parameterTypes[i].equals(PropertyMappingService.class)) {
                        parameters[i] = mappingService;
                    } else if (parameterTypes[i].equals(SnapshotTokenHandler.class)) {
                        parameters[i] = tokenHandlerService
                                .getJwtTokenHandler(SHARED_KEY_ALIAS, SELF_SERVICE_CERT_ALIAS);
                    } else {
                        throw new StageConfigException("Unexpected parameter type for configured progress stage "
                                + parameters[i]);
                    }
                }

                return parameters;
            }
        };
    }

    private SnapshotTokenHandlerFactory newTokenHandlerFactory() {
        return new SnapshotTokenHandlerFactory() {
            @Override
            public SnapshotTokenHandler get(SnapshotTokenConfig snapshotTokenConfig) {
                switch (snapshotTokenConfig.getType()) {
                    case JwtTokenHandlerConfig.TYPE:
                        return createJwtTokenHandler((JwtTokenHandlerConfig) snapshotTokenConfig);
                    default:
                        throw new IllegalArgumentException("Unknown type " + snapshotTokenConfig.getType());
                }
            }
        };
    }

    private JwtTokenHandler createJwtTokenHandler(final JwtTokenHandlerConfig jwtTokenHandlerConfig) {
        return tokenHandlerService.getJwtTokenHandler(
                SHARED_KEY_ALIAS,
                jwtTokenHandlerConfig.getJweAlgorithm(),
                jwtTokenHandlerConfig.getEncryptionMethod(),
                SELF_SERVICE_CERT_ALIAS,
                jwtTokenHandlerConfig.getJwsAlgorithm(),
                jwtTokenHandlerConfig.getTokenLifeTimeInSeconds());
    }

    private ProcessStore newProcessStore() {
        return new ProcessStore() {
            final Map<String, JsonValue> store = new HashMap<>();
            @Override
            public void add(String s, JsonValue jsonValue) {
                store.put(s, jsonValue);
            }

            @Override
            public JsonValue remove(String s) {
                return store.remove(s);
            }
        };
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        LOGGER.debug("Deactivating Service {}", compContext.getProperties());
        try {
            unregisterServiceRegistration();
            identityProviderService.unregisterIdentityProviderListener(this);
        } catch (IllegalStateException e) {
            /* Catch if the service was already removed */
            serviceRegistration = null;
        } finally {
            processService = null;
            config = null;
            LOGGER.info("Self-service stopped.");
        }
    }

    private void unregisterServiceRegistration() {
        if (null != serviceRegistration) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }

    /** Implementation of IdentityProviderListener */

    @Override
    public void identityProviderConfigChanged() throws IdentityProviderServiceException {
        LOGGER.debug("Configuring {} with changes from IdentityProviderConfig {}", PID,
                identityProviderService.getIdentityProviders());
        unregisterServiceRegistration();

        // create self-service request handler
        processService = JsonAnonymousProcessServiceBuilder.newBuilder()
                .withClassLoader(this.getClass().getClassLoader())
                .withJsonConfig(convertForAllInOneRegistration(config.as(amendedConfig)))
                .withStageConfigMapping(IDMUserDetailsConfig.NAME, IDMUserDetailsConfig.class)
                .withStageConfigMapping(SocialUserClaimConfig.NAME, SocialUserClaimConfig.class)
                .withStageConfigMapping(AllInOneRegistrationConfig.NAME, AllInOneRegistrationConfig.class)
                .withProgressStageProvider(progressStageProvider)
                .withTokenHandlerFactory(newTokenHandlerFactory())
                .withProcessStore(newProcessStore())
                .build();

        // service registration - register the AnonymousProcessService directly as a RequestHandler
        serviceRegistration = context.getBundleContext().registerService(
                RequestHandler.class, processService, properties);
    }

}
