/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openidm.selfservice.stage;

import static org.forgerock.selfservice.core.ServiceUtils.INITIAL_TAG;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.http.Client;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.selfservice.impl.PropertyMappingService;
import org.forgerock.openidm.selfservice.util.RequirementsBuilder;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.crypto.CryptoService;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.stages.captcha.CaptchaStage;
import org.forgerock.selfservice.stages.captcha.CaptchaStageConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionStage;
import org.forgerock.selfservice.stages.terms.TermsAndConditionsConfig;
import org.forgerock.selfservice.stages.terms.TermsAndConditionsStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This stage aggregates the IDMUserDetails, Captcha, TermsAndConditions and SecurityAnswerDefinition stages into a
 * single stage.
 */
public final class AllInOneRegistrationStage implements ProgressStage<AllInOneRegistrationConfig> {

    private static final Logger logger = LoggerFactory.getLogger(AllInOneRegistrationStage.class);

    private static final JwtReconstruction jwtReconstruction = new JwtReconstruction();

    private final Client httpClient;
    private final PropertyMappingService mappingService;
    private final SnapshotTokenHandler tokenHandler;

    private final ConnectionFactory connectionFactory;
    private final CryptoService cryptoService;

    /**
     * Constructs a new all-in-one registration stage.
     *
     * @param httpClient the http client
     * @param mappingService the service used to map profile attributes to managed object attributes
     * @param tokenHandler the JWT encrypter for passing user credentials
     * @param connectionFactory a CREST connection factory
     */
    @Inject
    public AllInOneRegistrationStage(@SelfService Client httpClient, @SelfService PropertyMappingService mappingService,
            @SelfService SnapshotTokenHandler tokenHandler, @SelfService ConnectionFactory connectionFactory) {
        this.httpClient = httpClient;
        this.mappingService = mappingService;
        this.tokenHandler = tokenHandler;

        this.connectionFactory = connectionFactory;
        this.cryptoService = new CryptoService();
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, AllInOneRegistrationConfig config)
            throws ResourceException {

        JsonValue requirements = RequirementsBuilder.newInstance("All-In-One Registration").build();
        List<String> stagesList = new ArrayList<>();

        if (configExists(config, CaptchaStageConfig.NAME)) {
            requirements = merge(requirements, getCaptchaRequirements(context, config));
            stagesList.add(CaptchaStageConfig.NAME);
        }
        if (configExists(config, TermsAndConditionsConfig.NAME)) {
            requirements = merge(requirements, getTermsAndConditionsRequirements(context, config));
            stagesList.add(TermsAndConditionsConfig.NAME);
        }
        if (configExists(config, SecurityAnswerDefinitionConfig.NAME)) {
            requirements = merge(requirements, getSecurityAnswerDefinitionRequirements(context, config));
            stagesList.add(SecurityAnswerDefinitionConfig.NAME);
        }
        if (configExists(config, IDMUserDetailsConfig.NAME)) {
            requirements = merge(requirements, getIDMUserDetailsRequirements(context, config));
            stagesList.add(IDMUserDetailsConfig.NAME);
        }

        return merge(requirements, RequirementsBuilder.newInstance("none")
                .addCustomField("stages", new JsonValue(stagesList))
                .build());
    }

    private boolean configExists(AllInOneRegistrationConfig config, String stageName) throws ResourceException {
        try {
            getConfig(config, stageName);
            return true;
        } catch (InternalServerErrorException e) {
            return false;
        }
    }

    private StageConfig getConfig(AllInOneRegistrationConfig config, String stageName) throws ResourceException {
        for (StageConfig conf : config.getConfigs()) {
            if (conf.getName().equals(stageName)) {
                return conf;
            }
        }
        throw new InternalServerErrorException("Unknown configuration: " + stageName);
    }

    private JsonValue getIDMUserDetailsRequirements(ProcessContext context, AllInOneRegistrationConfig config)
            throws ResourceException {
        return new IDMUserDetailsStage(httpClient, mappingService, tokenHandler, connectionFactory)
                .gatherInitialRequirements(context,
                        (IDMUserDetailsConfig) getConfig(config, IDMUserDetailsConfig.NAME));
    }

    private JsonValue getCaptchaRequirements(ProcessContext context, AllInOneRegistrationConfig config)
            throws ResourceException {
        return new CaptchaStage(httpClient).gatherInitialRequirements(context,
                (CaptchaStageConfig) getConfig(config, CaptchaStageConfig.NAME));
    }

    private JsonValue getTermsAndConditionsRequirements(ProcessContext context, AllInOneRegistrationConfig config)
            throws ResourceException {
        return new TermsAndConditionsStage().gatherInitialRequirements(context,
                (TermsAndConditionsConfig) getConfig(config, TermsAndConditionsConfig.NAME));
    }

    private JsonValue getSecurityAnswerDefinitionRequirements(ProcessContext context, AllInOneRegistrationConfig config)
            throws ResourceException {
        return new SecurityAnswerDefinitionStage(connectionFactory).gatherInitialRequirements(context,
                (SecurityAnswerDefinitionConfig) getConfig(config, SecurityAnswerDefinitionConfig.NAME));
    }

    /**
     * Merge rhs into lhs, preserving lhs values rather than overwriting them.
     *
     * @param lhs starting document
     * @param rhs document containing additional values
     * @return merged document
     * @throws ResourceException
     */
    JsonValue merge(JsonValue lhs, JsonValue rhs) throws ResourceException {
        if (rhs.isList() && lhs.isList()) {
            for (Object val : rhs.asList()) {
                if (!lhs.contains(val)) {
                    lhs.add(val);
                }
            }
        } else if (rhs.isMap() && lhs.isMap()) {
            for (String key : rhs.keys()) {
                JsonValue right = rhs.get(key);
                if (lhs.get(key).isNull()) {
                    lhs.put(key, right.getObject());
                } else {
                    JsonValue left = lhs.get(key);
                    if (right.isMap() && left.isMap()) {
                        if (left.isNull()) {
                            lhs.put(key, right.getObject());
                        } else {
                            for (String mkey : right.asMap().keySet()) {
                                if (left.get(mkey).isNull()) {
                                    left.put(mkey, right.get(mkey));
                                } else {
                                    left.put(mkey, merge(left.get(mkey), right.get(mkey)));
                                }
                            }
                        }
                    } else if (right.isList() && left.isList()) {
                        for (int i = 0; i < right.size(); i++) {
                            left.add(right.get(i).getObject());
                        }
                    } else if ((right.isBoolean() && left.isBoolean())
                            || (right.isString() && left.isString())
                            || (right.isNumber() && left.isNumber())) {
                        lhs.put(key, left.getObject());
                    } else {
                        throw new BadRequestException("Unknown data type or data types mismatch");
                    }
                }
            }
        } else if ((rhs.isBoolean() && lhs.isBoolean())
                || (rhs.isString() && lhs.isString())
                || (rhs.isNumber() && lhs.isNumber())) {
            return lhs;
        } else {
            throw new BadRequestException("Unknown data type or data types mismatch");
        }
        return lhs;
    }

    @Override
    public StageResponse advance(ProcessContext context, AllInOneRegistrationConfig config) throws ResourceException {
        JsonValue requirements = RequirementsBuilder.newInstance("All-In-One Requirements").build();
        List<String> stagesList = new ArrayList<>();

        if (configExists(config, CaptchaStageConfig.NAME)
                && !context.containsState(CaptchaStageConfig.NAME + "-complete")) {
            CaptchaStageConfig conf = (CaptchaStageConfig) getConfig(config, CaptchaStageConfig.NAME);
            CaptchaStage stage = new CaptchaStage(httpClient);
            try {
                stage.advance(context, conf);
                context.putState(CaptchaStageConfig.NAME + "-complete", true);
            } catch (JsonValueException e) {
                requirements = merge(requirements, getCaptchaRequirements(context, config));
                stagesList.add(CaptchaStageConfig.NAME);
            }
        }

        if (configExists(config, TermsAndConditionsConfig.NAME)
                && !context.containsState(TermsAndConditionsConfig.NAME + "-complete")) {
            TermsAndConditionsConfig conf =
                    (TermsAndConditionsConfig) getConfig(config, TermsAndConditionsConfig.NAME);
            TermsAndConditionsStage stage = new TermsAndConditionsStage();
            try {
                stage.advance(context, conf);
                context.putState(TermsAndConditionsConfig.NAME + "-complete", true);
            } catch (BadRequestException e) {
                requirements = merge(requirements, getTermsAndConditionsRequirements(context, config));
                stagesList.add(TermsAndConditionsConfig.NAME);
            }
        }

        if (configExists(config, SecurityAnswerDefinitionConfig.NAME)
                && !context.containsState(SecurityAnswerDefinitionConfig.NAME + "-complete")) {
            SecurityAnswerDefinitionConfig conf =
                    (SecurityAnswerDefinitionConfig) getConfig(config, SecurityAnswerDefinitionConfig.NAME);
            SecurityAnswerDefinitionStage stage = new SecurityAnswerDefinitionStage(connectionFactory);
            try {
                stage.advance(context, conf);
                context.putState(SecurityAnswerDefinitionConfig.NAME + "-complete", true);
            } catch (ResourceException | IllegalArgumentException | JsonValueException e) {
                requirements = merge(requirements, getSecurityAnswerDefinitionRequirements(context, config));
                stagesList.add(SecurityAnswerDefinitionConfig.NAME);
            }
        }

        String stageTag = null;

        if (configExists(config, IDMUserDetailsConfig.NAME)
                && !context.containsState(IDMUserDetailsConfig.NAME + "-complete")) {
            IDMUserDetailsConfig conf = (IDMUserDetailsConfig) getConfig(config, IDMUserDetailsConfig.NAME);
            IDMUserDetailsStage stage = new IDMUserDetailsStage(
                    httpClient, mappingService, tokenHandler, connectionFactory);
            try {
                StageResponse response = stage.advance(context, conf);
                if (response.hasRequirements()) {
                    requirements = merge(requirements, response.getRequirements());
                    stagesList.add(IDMUserDetailsConfig.NAME);
                } else {
                    context.putState(IDMUserDetailsConfig.NAME + "-complete", true);
                }
                stageTag = response.getStageTag();
            } catch (BadRequestException e) {
                requirements = merge(requirements, getIDMUserDetailsRequirements(context, config));
                stagesList.add(IDMUserDetailsConfig.NAME);
            }
        }

        requirements = merge(requirements, RequirementsBuilder.newInstance("none")
                .addCustomField("stages", new JsonValue(stagesList))
                .build());

        return stagesList.size() > 0
            ? stageTag == null
                ? StageResponse.newBuilder().setRequirements(requirements).build()
                : StageResponse.newBuilder().setStageTag(stageTag).setRequirements(requirements).build()
            : StageResponse.newBuilder().build();
    }
}