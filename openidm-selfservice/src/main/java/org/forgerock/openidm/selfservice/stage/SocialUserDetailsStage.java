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

import org.forgerock.http.Client;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.idp.relyingparty.OpenIDConnectProvider;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.openidm.selfservice.util.RequirementsBuilder;
import org.forgerock.openidm.idp.relyingparty.SocialUser;
import org.forgerock.openidm.idp.relyingparty.SocialProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Stage is responsible for gathering social user profile details.
 * It expects the "mai" field to be populated in the context which
 * it uses to verify against the email address specified in the
 * passed in user object.
 */
public final class SocialUserDetailsStage implements ProgressStage<SocialUserDetailsConfig> {

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
                    field("authorization_endpoint", provider.getAuthorizationEndpoint())
            )));
        }
        return RequirementsBuilder
                .newInstance("New user details")
                .addProperty("user", getUserSchemaRequirements(new SocialUser(), true))
                .addProperty("provider", "string", "OAuth IDP name")
                .addProperty("code", "string", "OAuth Access code")
                .addProperty("redirect_uri", "string", "OAuth redirect URI used to obtain the authorization code")
                .addDefinition("providers", newArray(oneOf(providers.toArray(new JsonValue[0]))))
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, SocialUserDetailsConfig config) throws ResourceException {
        JsonValue user = context.getInput().get("user");
        if (user.isNotNull()) {
            processEmail(context, config, user);

            JsonValue userState = ensureUserInContext(context);
            Map<String, Object> properties = user.asMap();
            updateUserJsonValue(userState, properties);
            context.putState(USER_FIELD, userState);

            return StageResponse.newBuilder().build();
        }

        JsonValue provider = context.getInput().get("provider");
        JsonValue code = context.getInput().get("code");
        JsonValue redirectUri = context.getInput().get("redirect_uri");
        if (provider.isNotNull() && code.isNotNull() && redirectUri.isNotNull()) {
            SocialUser userResponse = getSocialUser(provider.asString(), code.asString(), redirectUri.asString(), config);
            if (userResponse == null) {
                throw new BadRequestException("Unable to reach social provider or unknown provider given");
            }

            context.putState(USER_FIELD, mapper.convertValue(userResponse, Map.class));

            JsonValue requirements = RequirementsBuilder
                    .newInstance("Verify user profile")
                    .addRequireProperty("user", getUserSchemaRequirements(userResponse, false))
                    .build();

            return StageResponse.newBuilder()
                    .setStageTag(VALIDATE_USER_PROFILE_TAG)
                    .setRequirements(requirements)
                    .build();
        }

        throw new BadRequestException("Should respond with either user or provider/code");
    }

    private RequirementsBuilder getUserSchemaRequirements(SocialUser user, boolean passwordRequired) {
        return (passwordRequired
                ? newObject("User details").addRequireProperty("password", "string", "Password")
                : newObject("User details").addProperty("password", "string", "Password"))

                .addRequireProperty("userName", "string", "User name", user.getUserName())
                .addRequireProperty("name", newObject("Name")
                        .addRequireProperty("familyName", "string", "Family Name", user.getName().getFamilyName())
                        .addRequireProperty("givenName", "string", "Given Name", user.getName().getGivenName())
                        .addProperty("middleName", "string", "Middle Name", user.getName().getMiddleName())
                        .addProperty("honorificPrefix", "string", "Prefix", user.getName().getHonorificPrefix())
                        .addProperty("honorificSuffix", "string", "Suffix", user.getName().getHonorificSuffix()))
                .addRequireProperty("emails", newArray(
                        newObject("Email")
                                .addRequireProperty("value", "string", "Value")
                                .addProperty("type", "string", "Type")
                                .addProperty("primary", "boolean", "Primary"),
                        mapper.convertValue(user.getEmails(), List.class)));
    }

    private void processEmail(ProcessContext context, SocialUserDetailsConfig config, JsonValue user)
            throws BadRequestException {
        if (context.containsState(EMAIL_FIELD)) {
            JsonValue emailFieldContext = context.getState(EMAIL_FIELD);
            JsonValue emailFieldUser = user.get(new JsonPointer(config.getIdentityEmailField()));
            if (emailFieldUser == null) {
                user.put(new JsonPointer(config.getIdentityEmailField()), emailFieldContext.asString());
            } else if (!emailFieldUser.asString().equalsIgnoreCase(emailFieldContext.asString())) {
                throw new BadRequestException("Email address mismatch");
            }
        } else {
            JsonValue emailFieldUser = user.get(new JsonPointer(config.getIdentityEmailField()));
            if (emailFieldUser != null) {
                context.putState(EMAIL_FIELD, emailFieldUser.asString());
            }
        }
    }

    private JsonValue ensureUserInContext(ProcessContext context) {
        JsonValue user = context.getState(USER_FIELD);
        if (user == null) {
            user = json(object());
            context.putState(USER_FIELD, user);
        }
        return user;
    }

    private void updateUserJsonValue(JsonValue userState, Map<String, Object> properties) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            userState.put(key, value);
        }
    }

    private SocialUser getSocialUser(String providerName, String code, String redirectUri, SocialUserDetailsConfig config)
            throws ResourceException {
        SocialProvider provider = getSocialProvider(providerName, config.getProviders());
        return (provider == null) ? null : provider.getSocialUser(code, redirectUri);
    }

    public SocialProvider getSocialProvider(String providerName, List<ProviderConfig> providers)
            throws InternalServerErrorException {
        ProviderConfig config = getProviderConfig(providerName, providers);
        if (config == null) {
            return null;
        }

        if ("openid_connect".equalsIgnoreCase(config.getType())) {
            return new OpenIDConnectProvider(config, httpClient);
        } else {
            throw new InternalServerErrorException(config.getName() + " is not a recognized social provider");
        }
    }

    private ProviderConfig getProviderConfig(String providerName, List<ProviderConfig> providers) {
        for (ProviderConfig provider : providers) {
            if (provider.getName().equals(providerName)) {
                return provider;
            }
        }
        return null;
    }
}