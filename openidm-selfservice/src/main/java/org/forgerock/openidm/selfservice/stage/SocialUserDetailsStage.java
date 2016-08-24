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
import static org.forgerock.openidm.selfservice.util.RequirementsBuilder.newObject;
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
import org.forgerock.openidm.sync.PropertyMapping;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.openidm.selfservice.util.RequirementsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stage is responsible for gathering social user profile details.
 * It expects the "mai" field to be populated in the context which
 * it uses to verify against the email address specified in the
 * passed in user object.
 */
public final class SocialUserDetailsStage implements ProgressStage<SocialUserDetailsConfig> {

    private static final Logger logger = LoggerFactory.getLogger(SocialUserDetailsStage.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    private static final String VALIDATE_USER_PROFILE_TAG = "validateUserProfile";

    private final Client httpClient;

    /**
     * Constructs a new user details stage.
     *
     * @param httpClient
     *         the http client
     */
    @Inject
    public SocialUserDetailsStage(@SelfService Client httpClient) {
        this.httpClient = httpClient;
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
                .addProperty("user", getUserSchemaRequirements(json(object())))
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
            processEmail(context, config, user);

            final JsonValue userState = ensureUserInContext(context);
            final Map<String, Object> properties = user.asMap();
            updateUserJsonValue(userState, properties);
            context.putState(USER_FIELD, userState);

            return StageResponse.newBuilder().build();
        }

        final JsonValue provider = context.getInput().get("provider");
        final JsonValue code = context.getInput().get("code");
        final JsonValue redirectUri = context.getInput().get("redirect_uri");
        if (provider.isNotNull() && code.isNotNull() && redirectUri.isNotNull()) {
            final JsonValue userResponse = getSocialUser(provider.asString(), code.asString(), redirectUri.asString(), config);
            if (userResponse == null) {
                throw new BadRequestException("Unable to reach social provider or unknown provider given");
            }

            context.putState(USER_FIELD, userResponse.getObject());

            final JsonValue requirements = RequirementsBuilder
                    .newInstance("Verify user profile")
                    .addRequireProperty("user", getUserSchemaRequirements(userResponse))
                    .build();

            return StageResponse.newBuilder()
                    .setStageTag(VALIDATE_USER_PROFILE_TAG)
                    .setRequirements(requirements)
                    .build();
        }

        throw new BadRequestException("Should respond with either user or provider/code");
    }

    private RequirementsBuilder getUserSchemaRequirements(JsonValue user) {
        return newObject("User details")
                .addRequireProperty("id", "string", "User ID", user.get("id").asString())
                .addRequireProperty("username", "string", "Username", user.get("username").asString())
                .addProperty("profileUrl", "string", "Profile URL", user.get("profileUrl").asString())
                .addProperty("photoUrl", "string", "Photo URL", user.get("photoUrl").asString())
                .addProperty("preferredLanguage", "string", "Preferred Language", user.get("preferredLanguage").asString())
                .addProperty("locale", "string", "Locale", user.get("locale").asString())
                .addProperty("timezone", "string", "Timezone", user.get("timezone").asString())
                .addRequireProperty("active", "boolean", "Active", user.get("active").asBoolean())
                .addRequireProperty("name", newObject("Name")
                        .addRequireProperty("familyName", "string", "Family Name", user.get("name").get("familyName").asString())
                        .addRequireProperty("givenName", "string", "Given Name", user.get("name").get("giveName").asString())
                        .addProperty("middleName", "string", "Middle Name", user.get("name").get("middleName").asString())
                        .addProperty("honorificPrefix", "string", "Prefix", user.get("name").get("honorificPrefix").asString())
                        .addProperty("honorificSuffix", "string", "Suffix", user.get("name").get("honorificSuffix").asString())
                        .addProperty("fullName", "string", "Suffix", user.get("name").get("fullName").asString())
                        .addProperty("nickname", "string", "Suffix", user.get("name").get("nickname").asString())
                        .addProperty("displayName", "string", "Suffix", user.get("name").get("displayName").asString())
                        .addProperty("title", "string", "Suffix", user.get("name").get("title").asString()))
                .addRequireProperty("email", newArray(1, newObject("Email")
                        .addRequireProperty("address", "string", "Email Address", user.get("email").get(0).get("address").asString())
                        .addProperty("type", "string", "Type", user.get("email").get(0).get("type").asString())
                        .addProperty("primary", "boolean", "Primary", user.get("email").get(0).get("primary").asBoolean())))
                .addProperty("address", newArray(0, newObject("Address"), user.get("address").asList()))
                .addProperty("phone", newArray(0, newObject("Phone"), user.get("phone").asList()));
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

    private void updateUserJsonValue(final JsonValue userState, final Map<String, Object> properties) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            userState.put(key, value);
        }
    }

    private JsonValue getSocialUser(final String providerName, final String code, final String redirectUri,
            final SocialUserDetailsConfig config) throws ResourceException {
        final OAuthHttpClient providerHttpClient = getHttpClient(providerName, config.getProviders());
        return (providerHttpClient == null)
                ? null
                : normalizeProfile(providerHttpClient.getProfile(code, redirectUri),
                        getProviderConfig(providerName, config.getProviders()));
    }

    private JsonValue normalizeProfile(final JsonValue profile, final ProviderConfig config) {
        final JsonValue target = json(object());
        final Context context = new RootContext();
        if (config.getPropertyMap() != null) {
            try {
                for (final SingleMapping mapping : config.getPropertyMap()) {
                    final PropertyMapping property = new PropertyMapping(mapping.asJsonValue());
                    property.apply(profile, null, target, null, null, context);
                }
            } catch (SynchronizationException e) {
                logger.warn("Unable to map profile data to common format", e);
                return null;
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