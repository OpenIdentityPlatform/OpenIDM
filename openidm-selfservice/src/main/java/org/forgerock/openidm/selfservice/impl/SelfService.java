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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openidm.selfservice.impl;

import static org.forgerock.http.handler.HttpClientHandler.*;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.json.resource.ResourcePath.*;
import static org.forgerock.openidm.util.ContextUtil.createInternalContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.apache.sync.SyncHttpClientProvider;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.spi.Loader;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.ProgressStageProvider;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.config.StageConfigException;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.json.JsonAnonymousProcessServiceBuilder;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandler;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandlerConfig;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.util.Options;
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
public class SelfService {
    static final String PID = "org.forgerock.openidm.selfservice";

    private static final Logger LOGGER = LoggerFactory.getLogger(SelfService.class);

    /** the boot.properties property for the self-service shared key alias */
    private static final String SHARED_KEY_PROPERTY = "openidm.config.crypto.selfservice.sharedkey.alias";

    /** the default self-service shared key alias */
    private static final String DEFAULT_SHARED_KEY_ALIAS = "openidm-selfservice-key";

    /** the router path to read the shared key */
    private static final String SHARED_KEY_ROUTER_PATH = "security/keystore/privatekey/"
            + IdentityServer.getInstance().getProperty(SHARED_KEY_PROPERTY, DEFAULT_SHARED_KEY_ALIAS);

    /** the JsonPointer location in the read-response for the encoded shared secret key */
    private static final JsonPointer ENCODED_SECRET_PTR = new JsonPointer("/secret/encoded");

    /** the registered parent router-path for self-service flows */
    static final String ROUTER_PREFIX = "selfservice";

    /** this config key is present if the config represents a self-service process */
    private static final String STAGE_CONFIGS = "stageConfigs";

    /** config key present if config requires KBA questions */
    private static final String KBA_CONFIG = "kbaConfig";

    // ----- Declarative Service Implementation

    /**
     * Use the external servlet connection factory so that self-service requests are subject to authz rules
     * as "external" requests.
     */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.router)")
    private ConnectionFactory connectionFactory;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;

    /** The KBA Configuration. */
    @Reference(policy = ReferencePolicy.STATIC)
    private KbaConfiguration kbaConfiguration;

    /** CryptoService - not used directly, but added to make sure shared key gets created before use */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private CryptoService cryptoService;

    private Dictionary<String, Object> properties = null;
    private JsonValue config;
    private RequestHandler processService;
    private ServiceRegistration<RequestHandler> serviceRegistration = null;

    @Activate
    void activate(ComponentContext context) throws Exception {
        LOGGER.debug("Activating Service with configuration {}", context.getProperties());
        try {
            config = enhancedConfig.getConfigurationAsJson(context);
            amendConfig();

            // create self-service request handler
            processService = JsonAnonymousProcessServiceBuilder.newBuilder()
                    .withClassLoader(this.getClass().getClassLoader())
                    .withJsonConfig(config)
                    .withProgressStageProvider(newProgressStageProvider(newHttpClient()))
                    .withTokenHandlerFactory(newTokenHandlerFactory())
                    .withProcessStore(newProcessStore())
                    .build();

            // begin service registration prep
            properties = context.getProperties();
            if (null == properties) {
                properties = new Hashtable<>();
            }

            String factoryPid = enhancedConfig.getConfigurationFactoryPid(context);
            if (StringUtils.isBlank(factoryPid)) {
                throw new IllegalArgumentException("Configuration must have property: "
                        + ServerConstants.CONFIG_FACTORY_PID);
            }
            properties.put(ServerConstants.ROUTER_PREFIX,
                    resourcePath(ROUTER_PREFIX).concat(resourcePath(factoryPid)).toString());

            // service registration - register the AnonymousProcessService directly as a RequestHandler
            serviceRegistration = context.getBundleContext().registerService(
                    RequestHandler.class, processService, properties);
        } catch (Exception ex) {
            LOGGER.warn("Configuration invalid, can not start self-service.", ex);
            throw ex;
        }
        LOGGER.info("Self-service started.");
    }

    private void amendConfig() throws ResourceException {
        for (JsonValue stageConfig : config.get(STAGE_CONFIGS)) {
            if (stageConfig.isDefined(KBA_CONFIG)) {
                // overwrite kbaConfig with config from KBA config service
                stageConfig.put(KBA_CONFIG, kbaConfiguration.getConfig().getObject());
            }
        }

        // force storage type to stateless
        config.put("storage", "stateless");
    }

    private Client newHttpClient() throws HttpApplicationException {
        return new Client(
                new HttpClientHandler(
                        Options.defaultOptions()
                                .set(OPTION_LOADER, new Loader() {
                                    @Override
                                    public <S> S load(Class<S> service, Options options) {
                                        return service.cast(new SyncHttpClientProvider());
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

                for (int i = 0; i < parameterTypes.length; i++) {
                    if (parameterTypes[i].equals(ConnectionFactory.class)) {
                        parameters[i] = connectionFactory;
                    } else if (parameterTypes[i].equals(Client.class)) {
                        parameters[i] = httpClient;
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
                        return new LazyJwtTokenHandler((JwtTokenHandlerConfig) snapshotTokenConfig);
                    default:
                        throw new IllegalArgumentException("Unknown type " + snapshotTokenConfig.getType());
                }
            }
        };
    }

    /**
     * A SnapshotTokenHandler that lazily initializes the decorated JwtTokenHandler.  This delays
     * the router call to fetch the shared key from the security manager until the system is fully started.
     */
    private class LazyJwtTokenHandler implements SnapshotTokenHandler {
        private final JwtTokenHandlerConfig config;
        private SnapshotTokenHandler handler;

        LazyJwtTokenHandler(JwtTokenHandlerConfig config) {
            this.config = config;
        }

        private SnapshotTokenHandler getHandler() {
            if (handler == null) {
                try {
                    // pull the shared key in from the keystore
                    JsonValue sharedKey = connectionFactory.getConnection()
                            .read(createInternalContext(), newReadRequest(SHARED_KEY_ROUTER_PATH))
                            .getContent()
                            .get(ENCODED_SECRET_PTR);
                    if (sharedKey == null) {
                        throw new RuntimeException("Selfservice shared key does not exist");
                    }
                    SigningManager signingManager = new SigningManager();
                    SigningHandler signingHandler = signingManager.newHmacSigningHandler(
                            sharedKey.asString().getBytes());

                    KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(config.getKeyPairAlgorithm());
                    keyPairGen.initialize(config.getKeyPairSize());

                    handler = new JwtTokenHandler(
                            config.getJweAlgorithm(),
                            config.getEncryptionMethod(),
                            keyPairGen.generateKeyPair(),
                            config.getJwsAlgorithm(),
                            signingHandler,
                            config.getTokenLifeTimeInSeconds());
                } catch (ResourceException e) {
                    throw new RuntimeException("Unable to read selfservice shared key", e);
                } catch (NoSuchAlgorithmException nsaE) {
                    throw new RuntimeException("Unable to create key pair for encryption", nsaE);
                }
            }
            return handler;
        }

        @Override
        public String generate(JsonValue jsonValue) throws ResourceException {
            return getHandler().generate(jsonValue);
        }

        @Override
        public void validate(String s) throws ResourceException {
            getHandler().validate(s);
        }

        @Override
        public JsonValue validateAndExtractState(String s) throws ResourceException {
            return getHandler().validateAndExtractState(s);
        }
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
            if (null != serviceRegistration) {
                serviceRegistration.unregister();
                serviceRegistration = null;
            }
        } catch (IllegalStateException e) {
            /* Catch if the service was already removed */
            serviceRegistration = null;
        } finally {
            processService = null;
            config = null;
            LOGGER.info("Self-service stopped.");
        }
    }

}
