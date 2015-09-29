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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.selfservice.impl;

import static org.forgerock.json.resource.ResourcePath.*;

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
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.selfservice.core.AnonymousProcessService;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.ProgressStageFactory;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.exceptions.StageConfigException;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.json.JsonConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountStage;
import org.forgerock.selfservice.stages.email.VerifyUserIdConfig;
import org.forgerock.selfservice.stages.email.VerifyUserIdStage;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionStage;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfig;
import org.forgerock.selfservice.stages.registration.UserRegistrationStage;
import org.forgerock.selfservice.stages.reset.ResetStage;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandler;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandlerConfig;
import org.forgerock.selfservice.stages.user.UserDetailsConfig;
import org.forgerock.selfservice.stages.user.UserDetailsStage;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service supports self-registration, password reset, and forgotten username
 * per the Commons Self-Service stage configuration.
 */
@Component(name = SelfService.PID, immediate = true, configurationFactory = true, policy=ConfigurationPolicy.REQUIRE)
@Properties({
    @Property(name = "service.description", value = "OpenIDM SelfService Service"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
})
public class SelfService {
    public static final String PID = "org.forgerock.openidm.selfservice";

    private static final Logger LOGGER = LoggerFactory.getLogger(SelfService.class);

    /** the registered parent router-path for self-service flows */
    private static final ResourcePath ROUTER_PATH = resourcePath("selfservice");

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

    private Dictionary<String, Object> properties = null;
    private JsonValue config;
    private AnonymousProcessService processService;
    private ServiceRegistration<RequestHandler> serviceRegistration = null;

    @Activate
    void activate(ComponentContext context) throws Exception {
        LOGGER.debug("Activating Service with configuration {}", context.getProperties());
        try {
            // get and amend config
            config = enhancedConfig.getConfigurationAsJson(context);
            amendConfig();

            // create self-service request handler
            processService =
                    new AnonymousProcessService(JsonConfig.buildProcessInstanceConfig(config),
                            newProgressStageFactory(),
                            newTokenHandlerFactory(),
                            newProcessStore());

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
            properties.put(ServerConstants.ROUTER_PREFIX, ROUTER_PATH.concat(resourcePath(factoryPid)).toString());

            // service registration - register the AnonymousProcessService directly as a RequestHandler
            serviceRegistration = context.getBundleContext().registerService(
                    RequestHandler.class, processService, properties);
        } catch (Exception ex) {
            LOGGER.warn("Configuration invalid, can not start self-service.", ex);
            throw ex;
        }
        LOGGER.info("Self-service started.");
    }

    private void amendConfig() {
        // set serviceUrl for stage configs to use external/email
        for (JsonValue stageConfig : config.get("stageConfigs")) {
            if (stageConfig.isDefined("email")) {
                stageConfig.get("email").put("serviceUrl", "external/email");
            }
        }
        // force storage type to stateless
        config.put("storage", "stateless");
    }

    private ProgressStageFactory newProgressStageFactory() {
        final Map<Class<? extends StageConfig>, ProgressStage<?>> progressStages = new HashMap<>();

        progressStages.put(VerifyEmailAccountConfig.class, new VerifyEmailAccountStage(connectionFactory));
        progressStages.put(VerifyUserIdConfig.class, new VerifyUserIdStage(connectionFactory));
        progressStages.put(ResetStageConfig.class, new ResetStage(connectionFactory));
        progressStages.put(UserRegistrationConfig.class, new UserRegistrationStage(connectionFactory));
        progressStages.put(UserDetailsConfig.class, new UserDetailsStage(connectionFactory));
        progressStages.put(SecurityAnswerDefinitionConfig.class, new SecurityAnswerDefinitionStage(connectionFactory));

        return new ProgressStageFactory() {
            @Override
            public <C extends StageConfig> ProgressStage<C> get(C config) {
                ProgressStage<?> untypedStage = progressStages.get(config.getClass());

                if (untypedStage == null) {
                    throw new StageConfigException("Unable to find matching stage for config " + config.getName());
                }

                @SuppressWarnings("unchecked")
                ProgressStage<C> typedStage = (ProgressStage<C>) untypedStage;
                return typedStage;
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

            private SnapshotTokenHandler createJwtTokenHandler(JwtTokenHandlerConfig config) {
                try {
                    SigningManager signingManager = new SigningManager();
                    SigningHandler signingHandler = signingManager.newHmacSigningHandler(config.getSharedKey());

                    KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(config.getKeyPairAlgorithm());
                    keyPairGen.initialize(config.getKeyPairSize());

                    return new JwtTokenHandler(
                            config.getJweAlgorithm(),
                            config.getEncryptionMethod(),
                            keyPairGen.generateKeyPair(),
                            config.getJwsAlgorithm(),
                            signingHandler,
                            config.getTokenLifeTimeInSeconds());

                } catch (NoSuchAlgorithmException nsaE) {
                    throw new RuntimeException("Unable to create key pair for encryption", nsaE);
                }
            }
        };
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
