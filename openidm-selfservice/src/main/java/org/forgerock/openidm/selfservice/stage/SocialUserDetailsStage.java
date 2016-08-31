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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.selfservice.stage;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openidm.selfservice.util.RequirementsBuilder.newArray;
import static org.forgerock.openidm.selfservice.util.RequirementsBuilder.oneOf;
import static org.forgerock.selfservice.stages.CommonStateFields.EMAIL_FIELD;
import static org.forgerock.selfservice.stages.CommonStateFields.USER_FIELD;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.http.Client;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.idp.config.SingleMapping;
import org.forgerock.openidm.idp.client.OAuthHttpClient;
import org.forgerock.openidm.selfservice.impl.PropertyMappingService;
import org.forgerock.openidm.sync.PropertyMapping;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.openidm.selfservice.util.RequirementsBuilder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;

import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stage is responsible for gathering social user profile details.
 * It expects the "mail" field to be populated in the context which
 * it uses to verify against the email address specified in the
 * passed in user object.
 */
public final class SocialUserDetailsStage implements ProgressStage<SocialUserDetailsConfig> {

    private static final Logger logger = LoggerFactory.getLogger(SocialUserDetailsStage.class);

    private static final String VALIDATE_USER_PROFILE_TAG = "validateUserProfile";
    private static final String IDP_DATA_OBJECT = "idpData";

    private final Client httpClient;
    private final PropertyMappingService mappingService;

    /**
     * Constructs a new user details stage.
     *
     * @param httpClient
     *         the http client
     */
    @Inject
    public SocialUserDetailsStage(@SelfService Client httpClient, @SelfService PropertyMappingService mappingService) {
        this.httpClient = httpClient;
        this.mappingService = mappingService;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context,
                                               SocialUserDetailsConfig config) throws ResourceException {

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
                .addProperty("user", "object", "User Object", json(object()))
                .addProperty("provider", "string", "OAuth IDP name")
                .addProperty("code", "string", "OAuth Access code")
                .addProperty("redirect_uri", "string", "OAuth redirect URI used to obtain the authorization code")
                .addDefinition("providers", newArray(oneOf(providers.toArray(new JsonValue[0]))))
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, SocialUserDetailsConfig config) throws ResourceException {
        final JsonValue user = context.getInput().get("user");
        if (user.isNotNull()) {
            // This is the second pass through this stage.  Update the user object and advance.
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

            return StageResponse.newBuilder().build();
        }

        // This is the first pass through this stage.  Gather the user profile to offer up for registration.
        final JsonValue code = context.getInput().get("code");
        final JsonValue redirectUri = context.getInput().get("redirect_uri");
        final JsonValue provider = context.getInput().get("provider");
        if (provider.isNotNull() && code.isNotNull() && redirectUri.isNotNull()) {
            final JsonValue userResponse = getSocialUser(provider.asString(), code.asString(), redirectUri.asString(),
                    config, context);
            if (userResponse == null) {
                throw new BadRequestException("Unable to reach social provider or unknown provider given");
            }

            context.putState(USER_FIELD, userResponse.getObject());

            final JsonValue requirements = RequirementsBuilder
                    .newInstance("Verify user profile")
                    .addProperty("user", "object", "User Object", userResponse.getObject())
                    .build();

            return StageResponse.newBuilder()
                    .setStageTag(VALIDATE_USER_PROFILE_TAG)
                    .setRequirements(requirements)
                    .build();
        }

        throw new BadRequestException("Should respond with either user or provider/code");
    }

    private void processEmail(final ProcessContext context, final SocialUserDetailsConfig config, final JsonValue user)
            throws BadRequestException {
        if (context.containsState(EMAIL_FIELD)) {
            final JsonValue emailFieldContext = context.getState(EMAIL_FIELD);
            final JsonValue emailFieldUser = user.get(new JsonPointer(config.getIdentityEmailField()));
            if (emailFieldUser == null) {
                user.put(new JsonPointer(config.getIdentityEmailField()), emailFieldContext.asString());
            } else if (!emailFieldUser.asString().equalsIgnoreCase(emailFieldContext.asString())) {
                throw new BadRequestException("Email address mismatch");
            }
        } else {
            final JsonValue emailFieldUser = user.get(new JsonPointer(config.getIdentityEmailField()));
            if (emailFieldUser != null) {
                context.putState(EMAIL_FIELD, emailFieldUser.asString());
            }
        }
    }

    private JsonValue ensureUserInContext(final ProcessContext context) {
        JsonValue user = context.getState(USER_FIELD);
        if (user == null) {
            user = json(object());
            context.putState(USER_FIELD, user);
        }
        return user;
    }

    private JsonValue getSocialUser(final String providerName, final String code, final String redirectUri,
            final SocialUserDetailsConfig config, final ProcessContext context) throws ResourceException {
        final OAuthHttpClient providerHttpClient = getHttpClient(providerName, config.getProviders());
        if (providerHttpClient == null) {
            return null;
        }
        final ProviderConfig providerConfig = getProviderConfig(providerName, config.getProviders());
        final JsonValue rawProfile = providerHttpClient.getProfile(code, redirectUri);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        context.putState(IDP_DATA_OBJECT, json(object(
                field(providerName, object(
                        field("subject", rawProfile.get(providerConfig.getAuthenticationId()).asString()),
                        field("enabled", true),
                        field("dateCollected", df.format(new Date())),
                        field("rawProfile", rawProfile)
                )))).getObject());

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