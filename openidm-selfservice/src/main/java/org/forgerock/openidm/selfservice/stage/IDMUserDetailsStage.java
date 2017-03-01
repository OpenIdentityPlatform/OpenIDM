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
 * Copyright 2016-2017 ForgeRock AS.
 */

package org.forgerock.openidm.selfservice.stage;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openidm.core.ServerConstants.HEADER_USERNAME;
import static org.forgerock.openidm.core.ServerConstants.HEADER_PASSWORD;
import static org.forgerock.openidm.idp.client.OAuthHttpClient.ACCESS_TOKEN;
import static org.forgerock.openidm.idp.client.OAuthHttpClient.CODE;
import static org.forgerock.openidm.idp.client.OAuthHttpClient.ID_TOKEN;
import static org.forgerock.openidm.idp.client.OAuthHttpClient.NONCE;
import static org.forgerock.openidm.idp.client.OAuthHttpClient.PROVIDER;
import static org.forgerock.openidm.idp.client.OAuthHttpClient.REDIRECT_URI;
import static org.forgerock.openidm.idp.impl.ProviderConfigMapper.buildIdpObject;
import static org.forgerock.openidm.selfservice.util.RequirementsBuilder.newArray;
import static org.forgerock.openidm.selfservice.util.RequirementsBuilder.oneOf;
import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_FIELD;
import static org.forgerock.services.context.ClientContext.newInternalClientContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.http.Client;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.idp.config.SingleMapping;
import org.forgerock.openidm.idp.client.OAuthHttpClient;
import org.forgerock.openidm.selfservice.impl.PropertyMappingService;
import org.forgerock.openidm.sync.PropertyMapping;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.selfservice.util.RequirementsBuilder;

import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.tokenhandler.TokenHandler;
import org.forgerock.tokenhandler.TokenHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stage is responsible for gathering social user profile details.
 * It expects the "mail" field to be populated in the context which
 * it uses to verify against the email address specified in the
 * passed in user object.
 */
public class IDMUserDetailsStage implements ProgressStage<IDMUserDetailsConfig> {

    private static final Logger logger = LoggerFactory.getLogger(IDMUserDetailsStage.class);

    private static final JwtReconstruction jwtReconstruction = new JwtReconstruction();

    private static final String VALIDATE_USER_PROFILE_TAG = "validateUserProfile";
    private static final String IDP_DATA_OBJECT = "idpData";
    private static final String CREDENTIAL_JWT = "credentialJwt";
    private static final String USERNAME = "userName";
    private static final String PASSWORD = "password";
    private static final String SUCCESS_URL = "successUrl";
    private static final String SOCIAL_REG_ENABLED = "socialRegistrationEnabled";
    private static final String REGISTRATION_FORM = "registrationForm";
    private static final String REGISTRATION_PROPERTIES = "registrationProperties";

    private final Client httpClient;
    private final PropertyMappingService mappingService;
    private final TokenHandler tokenHandler;
    private final ConnectionFactory connectionFactory;

    /**
     * Constructs a new user details stage.
     *
     * @param httpClient
     *         the http client
     */
    @Inject
    public IDMUserDetailsStage(@SelfService Client httpClient, @SelfService PropertyMappingService mappingService,
            @SelfService TokenHandler tokenHandler, @SelfService ConnectionFactory connectionFactory) {
        this.httpClient = httpClient;
        this.mappingService = mappingService;
        this.tokenHandler = tokenHandler;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, IDMUserDetailsConfig config)
            throws ResourceException {
        List<JsonValue> providers = new ArrayList<>(config.getProviders().size());
        for (ProviderConfig provider : config.getProviders()) {
            providers.add(json(object(
                    field("name", provider.getName()),
                    field("type", provider.getType()),
                    field("icon", provider.getIcon()),
                    field("client_id", provider.getClientId()),
                    field("scope", StringUtils.join(provider.getScope(), " ")),
                    field("authorization_endpoint", provider.getAuthorizationEndpoint())
            )));
        }
        return RequirementsBuilder
                .newInstance("New user details")
                .addProperty(PROVIDER, "string", "OAuth/OIDC IdP name")
                .addProperty(CODE, "string", "OAuth/OIDC authorization code")
                .addProperty(NONCE, "string", "One-time use random value")
                .addProperty(REDIRECT_URI, "string", "OAuth/OIDC redirect URI used to process the authorization code")
                .addProperty(ACCESS_TOKEN, "string", "OAuth/OIDC Access Token")
                .addProperty(ID_TOKEN, "string", "OAuth/OIDC ID Token")
                .addProperty("user", "object", "User Object", json(object()))
                .addDefinition("providers", newArray(oneOf(providers.toArray(new JsonValue[0]))))
                .addCustomField(SOCIAL_REG_ENABLED, json(config.isSocialRegistrationEnabled()))
                .addCustomField(REGISTRATION_FORM, json(config.getRegistrationForm()))
                .addCustomField(REGISTRATION_PROPERTIES, getRegistrationFormProperties(context, config))
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, IDMUserDetailsConfig config) throws ResourceException {

        final JsonValue user = context.getInput().get("user");
        if (user.isNotNull()) {
            return advanceWithUserObject(context, config, user);
        }
        return advanceWithSocialCredentials(context, config);
    }

    private JsonValue getRegistrationFormProperties(ProcessContext context, IDMUserDetailsConfig config)
            throws ResourceException {
        JsonValue properties = json(object());
        JsonValue required = json(array());

        JsonValue schema = fetchSchema(context, config);

        // First pull in all of those the config asks for (if we have them)
        for (String prop : config.getRegistrationProperties()) {
            if (schema.isDefined("properties") && schema.get("properties").isDefined(prop)) {
                properties.put(prop, schema.get("properties").get(prop));
                if (schema.get("required").contains(prop)) {
                    required.add(prop);
                }
            }
        }

        // Then add any required properties the config missed
        for (String prop : schema.get("properties").keys()) {
            if (!config.getRegistrationProperties().contains(prop) && schema.get("required").contains(prop)) {
                properties.put(prop, schema.get("properties").get(prop));
                required.add(prop);
            }
        }

        return json(object(
                field("properties", properties),
                field("required", required)
        ));
    }

    protected JsonValue fetchSchema(ProcessContext context, IDMUserDetailsConfig config) throws ResourceException {
        ReadRequest request = Requests.newReadRequest("/config/managed");
        ResourceResponse response = connectionFactory.getConnection()
                .read(newInternalClientContext(context.getRequestContext()), request);

        JsonValue schema = null;
        for (JsonValue obj : response.getContent().get("objects")) {
            if (obj.get("name").asString().equals(
                    config.getIdentityServiceUrl().substring(config.getIdentityServiceUrl().lastIndexOf("/") + 1))) {
                schema = obj.get("schema");
                break;
            }
        }
        if (schema == null || schema.isNull()) {
            throw new NotFoundException("No schema found for " + config.getIdentityServiceUrl());
        }
        return schema;
    }

    private StageResponse advanceWithUserObject(ProcessContext context, IDMUserDetailsConfig config, JsonValue user)
            throws ResourceException {
        processEmail(context, config, user);

        final JsonValue userState = ensureUserInContext(context);

        final Map<String, Object> properties = user.asMap();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            userState.put(key, value);
        }

        userState.put(IDP_DATA_OBJECT, context.getState(IDP_DATA_OBJECT));
        context.putState(USER_FIELD, userState);

        // Pass OAuth/OIDC credentials through
        context.putSuccessAddition(PROVIDER, context.getState(PROVIDER));
        context.putSuccessAddition(ACCESS_TOKEN, context.getState(ACCESS_TOKEN));
        context.putSuccessAddition(ID_TOKEN, context.getState(ID_TOKEN));

        if (user.get(USERNAME).isNotNull() && user.get(PASSWORD).isNotNull()) {
            try {
                context.putSuccessAddition(CREDENTIAL_JWT, tokenHandler.generate(json(object(
                        field(HEADER_USERNAME, user.get(USERNAME)),
                        field(HEADER_PASSWORD, user.get(PASSWORD))
                ))));
            } catch (TokenHandlerException e) {
                throw new InternalServerErrorException(e);
            }
        }

        return StageResponse.newBuilder().build();
    }

    private StageResponse advanceWithSocialCredentials(ProcessContext context, IDMUserDetailsConfig config)
            throws ResourceException {
        if (!config.isSocialRegistrationEnabled()) {
            throw new BadRequestException("User object is absent and social registration is not enabled");
        }

        // This is the first pass through this stage.  Gather the user profile to offer up for registration.
        JsonValue userResponse;

        final JsonValue code = context.getInput().get(CODE);
        final JsonValue nonce = context.getInput().get(NONCE);
        final JsonValue redirectUri = context.getInput().get(REDIRECT_URI);
        final JsonValue provider = context.getInput().get(PROVIDER);
        JsonValue accessToken = context.getInput().get(ACCESS_TOKEN);
        JsonValue idToken = context.getInput().get(ID_TOKEN);
        if (provider.isNotNull() && code.isNotNull() && nonce.isNotNull() && redirectUri.isNotNull()) {
            JsonValue tokens = getTokens(provider.asString(), code.asString(), nonce.asString(), redirectUri.asString(),
                    config);
            if (tokens != null && tokens.isNotNull()) {
                accessToken = tokens.get(ACCESS_TOKEN);
                idToken = tokens.get(ID_TOKEN);
            }
            userResponse = getSocialUser(provider.asString(), tokens, config, context);
        } else if (provider.isNotNull() && accessToken.isNotNull() && idToken.isNotNull()) {
            final JsonValue tokens = json(object(
                    field(ACCESS_TOKEN, accessToken.asString()),
                    field(ID_TOKEN, idToken)
            ));
            userResponse = getSocialUser(provider.asString(), tokens, config, context);
        } else {
            throw new BadRequestException("Should respond with user or provider plus code or accessToken");
        }
        if (userResponse == null) {
            throw new InternalServerErrorException("Unable to reach social provider or unknown provider given");
        }

        context.putState(USER_FIELD, userResponse.getObject());

        // Pass these on so they can be returned at the end of this stage
        context.putState(PROVIDER, provider.asString());
        context.putState(ACCESS_TOKEN, accessToken.asString());
        context.putState(ID_TOKEN, idToken);

        if (config.getSuccessUrl() != null) {
            context.putSuccessAddition(SUCCESS_URL, config.getSuccessUrl());
        }

        if (userObjectPassesPolicyValidation(context, userResponse, config)) {
            return advanceWithUserObject(context, config, userResponse);
        }

        final JsonValue requirements = RequirementsBuilder
                .newInstance("Verify user profile")
                .addProperty("user", "object", "User Object", userResponse.getObject())
                .addCustomField(SOCIAL_REG_ENABLED, json(config.isSocialRegistrationEnabled()))
                .addCustomField(REGISTRATION_FORM, json(config.getRegistrationForm()))
                .addCustomField(REGISTRATION_PROPERTIES, getRegistrationFormProperties(context, config))
                .build();

        return StageResponse.newBuilder()
                .setStageTag(VALIDATE_USER_PROFILE_TAG)
                .setRequirements(requirements)
                .build();
    }

    private boolean userObjectPassesPolicyValidation(ProcessContext context, JsonValue user,
            IDMUserDetailsConfig config) throws ResourceException {
        ActionRequest request = Requests.newActionRequest("/policy" + config.getIdentityServiceUrl() + "/-",
                "validateObject").setContent(user);
            ActionResponse response = connectionFactory.getConnection().action(context.getRequestContext(), request);
            JsonValue result = response.getJsonContent().get("result");
            return result.isNotNull() && result.asBoolean();
    }

    private void processEmail(final ProcessContext context, final IDMUserDetailsConfig config, final JsonValue user)
            throws BadRequestException {
        final JsonValue emailFieldUser = user.get(new JsonPointer(config.getIdentityEmailField()));
        if (emailFieldUser == null) {
            // don't set the mail field because it does not have a value
            return;
        }
        if (context.getState(USER_FIELD) != null
                && emailFieldUser.asString().equals(context.getState(USER_FIELD).get(EMAIL_FIELD).asString())) {
            context.putState("skipValidation", true);
        }
        context.putState(EMAIL_FIELD, emailFieldUser.asString());
    }

    private JsonValue ensureUserInContext(final ProcessContext context) {
        JsonValue user = context.getState(USER_FIELD);
        if (user == null) {
            user = json(object());
            context.putState(USER_FIELD, user);
        }
        return user;
    }

    private JsonValue getTokens(final String providerName, final String code, final String nonce,
            final String redirectUri, final IDMUserDetailsConfig config)
            throws ResourceException {
        final OAuthHttpClient providerHttpClient = getHttpClient(providerName, config.getProviders());
        if (providerHttpClient == null) {
            return null;
        }
        try {
            return providerHttpClient.getTokens(jwtReconstruction, code, nonce, redirectUri).getOrThrow();
        } catch (InterruptedException e) {
            throw ResourceException.newResourceException(ResourceException.INTERNAL_ERROR, e.getMessage());
        }
    }

    private JsonValue getSocialUser(final String providerName, final JsonValue tokens,
            final IDMUserDetailsConfig config, final ProcessContext context)
            throws ResourceException {
        final OAuthHttpClient providerHttpClient = getHttpClient(providerName, config.getProviders());
        if (providerHttpClient == null) {
            return null;
        }
        final ProviderConfig providerConfig = getProviderConfig(providerName, config.getProviders());
        final JsonValue rawProfile = providerHttpClient.getProfile(tokens);
        context.putState(IDP_DATA_OBJECT,
                json(object(field(providerName, buildIdpObject(providerConfig, rawProfile).getObject()))));

        final JsonValue commonFormat = normalizeProfile(rawProfile, providerConfig);
        return mappingService.apply(commonFormat, context.getRequestContext());
    }

    private JsonValue normalizeProfile(final JsonValue profile, final ProviderConfig config)
            throws SynchronizationException {
        final JsonValue target = json(object());
        final Context context = new RootContext();
        if (config.getPropertyMap() != null) {
            for (final SingleMapping mapping : config.getPropertyMap()) {
                final PropertyMapping property = new PropertyMapping(mapping.asJsonValue());
                property.apply(profile, null, target, null, null, context);
            }
        }
        target.add("rawProfile", profile);
        return target;
    }

    private OAuthHttpClient getHttpClient(final String providerName, final List<ProviderConfig> providers)
            throws InternalServerErrorException {
        final ProviderConfig config = getProviderConfig(providerName, providers);
        if (config == null) {
            return null;
        }
        return new OAuthHttpClient(config, httpClient);
    }

    private ProviderConfig getProviderConfig(final String providerName, final List<ProviderConfig> providers) {
        for (final ProviderConfig provider : providers) {
            if (provider.getName().equals(providerName)) {
                return provider;
            }
        }
        return null;
    }
}
