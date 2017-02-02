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
 * Copyright 2017 ForgeRock AS.
 */
package org.forgerock.openidm.selfservice.impl;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.openidm.idp.impl.ProviderConfigMapper.providerEnabled;

import javax.inject.Provider;

import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.base.Predicate;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.idp.impl.IdentityProviderService;
import org.forgerock.openidm.idp.impl.ProviderConfigMapper;
import org.forgerock.openidm.selfservice.stage.IDMUserDetailsConfig;
import org.forgerock.openidm.selfservice.stage.SocialUserClaimConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerVerificationConfig;
import org.forgerock.util.promise.NeverThrowsException;

/**
 * Manages the configuration amendment from how JSON is stored in OpenIDM and the business concerns
 * related to Self-Service stage configuration and the injection/augmentation from other sources.
 */
class SelfServiceConfigAmendment implements org.forgerock.util.Function<JsonValue, JsonValue, NeverThrowsException> {
    /** this config key is present if the config represents a self-service process */
    private static final String STAGE_CONFIGS = "stageConfigs";

    /** config key present if config requires KBA questions */
    private static final String KBA_CONFIG = "kbaConfig";

    /** the config attribute in the SecurityQuestionsDefinitionConfig for the number of questions to define */
    private static final String KBA_ANSWER_DEFINITION_STAGE_CONFIG = "numberOfAnswersUserMustSet";
    /** the corresponding attribute the UI needs when presenting to the user the number of questions to define */
    private static final String KBA_CONFIG_MINIMUM_ANSWERS_DEFINE = "minimumAnswersToDefine";

    /** the config attribute in the SecurityQuestionsVerificationConfig for the number of questions to answer */
    private static final String KBA_ANSWER_VERIFICATION_STAGE_CONFIG = "numberOfQuestionsUserMustAnswer";
    /** the corresponding attribute the UI needs when presenting to the user the number of questions to answer */
    private static final String KBA_CONFIG_MINIMUM_ANSWERS_VERIFY = "minimumAnswersToVerify";

    private final Provider<IdentityProviderService> identityProviderServiceProvider;
    private final Provider<KbaConfiguration> kbaConfigurationProvider;

    /** a Function that evaluates one function or another depending on the evaluation result of a Predicate */
    private static class ConditionalTransform<F, T> implements Function<F, T> {
        private Predicate<F> predicate;
        private Function<F, T> trueFunction;
        private Function<F, T> falseFunction;

        ConditionalTransform(Predicate<F> predicate, Function<F, T> trueFunction, Function<F, T> falseFunction) {
            this.predicate = predicate;
            this.trueFunction = trueFunction;
            this.falseFunction = falseFunction;
        }
        @Override
        public T apply(F f) {
            return predicate.apply(f)
                    ? trueFunction.apply(f)
                    : falseFunction.apply(f);
        }
    }

    /** a factory method to produce an if-then-else Function that executes f1 if the predicate is true, f2 otherwise */
    private static <F, T> Function<F, T> when(Predicate<F> predicate, Function<F, T> f1, Function<F, T> f2) {
        return new ConditionalTransform<>(predicate, f1, f2);
    }

    /** factory method to produce a if-then when with default, no-op else Function */
    private static Function<JsonValue, JsonValue> when(Predicate<JsonValue> predicate, Function<JsonValue, JsonValue> function) {
        return when(predicate, function, identity);
    }

    /** an identity Function that returns the input JsonValue */
    private static final Function<JsonValue, JsonValue> identity = new Function<JsonValue, JsonValue>() {
        @Override
        public JsonValue apply(JsonValue jsonValue) {
            return jsonValue;
        }
    };

    /** convert the object to a JsonValue */
    private static final Function<Object, JsonValue> toJsonValue = new Function<Object, JsonValue>() {
        @Override
        public JsonValue apply(Object object) {
            return json(object);
        }
    };

    /** unwrap the JsonValue */
    private static final Function<JsonValue, Object> toObject = new Function<JsonValue, Object>() {
        @Override
        public Object apply(JsonValue jsonValue) {
            return jsonValue.getObject();
        }
    };

    /** whether a stage config has a kbaConfig attribute */
    private static final Predicate<JsonValue> hasKbaConfig = new Predicate<JsonValue>() {
        @Override
        public boolean apply(JsonValue stageConfig) {
            return stageConfig.isDefined(KBA_CONFIG);
        }
    };

    /** whether the stage config represents a SecurityAnswerDefinitionStage config */
    private static final Predicate<JsonValue> isSecurityAnswerDefinition = new Predicate<JsonValue>() {
        @Override
        public boolean apply(JsonValue stageConfig) {
            return SecurityAnswerDefinitionConfig.NAME.equals(stageConfig.get("name").asString());
        }
    };

    /** whether the stage config represents a SecurityAnswerVerificationStage config */
    private static final Predicate<JsonValue> isSecurityAnswerVerification = new Predicate<JsonValue>() {
        @Override
        public boolean apply(JsonValue stageConfig) {
            return SecurityAnswerVerificationConfig.NAME.equals(stageConfig.get("name").asString());
        }
    };

    /** whether the stage has social providers */
    private static final Predicate<JsonValue> hasSocialProviders = new Predicate<JsonValue>() {
        @Override
        public boolean apply(JsonValue stageConfig) {
            final String stageName = stageConfig.get("name").asString();
            return IDMUserDetailsConfig.NAME.equals(stageName) || SocialUserClaimConfig.NAME.equals(stageName);
        }
    };

    /** Function to inject KBA config */
    private final Function<JsonValue, JsonValue> injectKbaConfig = new Function<JsonValue, JsonValue>() {
        @Override
        public JsonValue apply(JsonValue stageConfig) {
            return  stageConfig.put(KBA_CONFIG, kbaConfigurationProvider.get().getConfig().getObject());
        }
    };

    /** Function to inject the security answers a user must set property */
    private final Function<JsonValue, JsonValue> injectSecurityAnswersUserMustSet = new Function<JsonValue, JsonValue>() {
        @Override
        public JsonValue apply(JsonValue stageConfig) {
            return stageConfig.put(KBA_ANSWER_DEFINITION_STAGE_CONFIG,
                    kbaConfigurationProvider.get().getConfig().get(KBA_CONFIG_MINIMUM_ANSWERS_DEFINE).asInteger());
        }
    };

    /** Function to inject the security questsions a user must answer property */
    private final Function<JsonValue, JsonValue> injectSecurityQuestionsUserMustAnswer = new Function<JsonValue, JsonValue>() {
        @Override
        public JsonValue apply(JsonValue stageConfig) {
            return stageConfig.put(KBA_ANSWER_VERIFICATION_STAGE_CONFIG,
                    kbaConfigurationProvider.get().getConfig().get(KBA_CONFIG_MINIMUM_ANSWERS_VERIFY).asInteger());
        }
    };

    /** Function to inject the social providers */
    private final Function<JsonValue, JsonValue> injectSocialProviders = new Function<JsonValue, JsonValue>() {
        @Override
        public JsonValue apply(JsonValue stageConfig) {
            // add oauth provider config
            return stageConfig.put(IdentityProviderService.PROVIDERS,
                    ProviderConfigMapper.toJsonValue(
                            FluentIterable.from(identityProviderServiceProvider.get().getIdentityProviders())
                                    .filter(providerEnabled)
                                    .toList())
                            .asList());
        }
    };

    SelfServiceConfigAmendment(Provider<IdentityProviderService> identityProviderServiceProvider,
            Provider<KbaConfiguration> kbaConfigurationProvider) {
        this.identityProviderServiceProvider = identityProviderServiceProvider;
        this.kbaConfigurationProvider = kbaConfigurationProvider;
    }

    public JsonValue apply(final JsonValue originalConfig) {
        JsonValue amendedConfig = originalConfig.copy();

        amendedConfig.put(STAGE_CONFIGS,
                FluentIterable.from(amendedConfig.get(STAGE_CONFIGS).asList())
                        .transform(toJsonValue)
                        .transform(when(hasKbaConfig, injectKbaConfig))
                        .transform(when(isSecurityAnswerDefinition, injectSecurityAnswersUserMustSet))
                        .transform(when(isSecurityAnswerVerification, injectSecurityQuestionsUserMustAnswer))
                        .transform(when(hasSocialProviders, injectSocialProviders))
                        .transform(toObject)
                        .toList());

        // force storage type to stateless
        amendedConfig.put("storage", "stateless");

        return amendedConfig;
    }
}
