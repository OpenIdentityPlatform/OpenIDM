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

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openidm.idp.client.OAuthHttpClient.*;
import static org.forgerock.openidm.idp.impl.ProviderConfigMapper.buildIdpObject;
import static org.forgerock.openidm.selfservice.util.RequirementsBuilder.newArray;
import static org.forgerock.openidm.selfservice.util.RequirementsBuilder.oneOf;
import static org.forgerock.selfservice.core.ServiceUtils.INITIAL_TAG;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.http.Client;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.idp.client.OAuthHttpClient;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.idp.config.SingleMapping;
import org.forgerock.openidm.selfservice.impl.PropertyMappingService;
import org.forgerock.openidm.selfservice.util.RequirementsBuilder;
import org.forgerock.openidm.sync.PropertyMapping;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.selfservice.core.IllegalStageTagException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.annotations.SelfService;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This stage is responsible for gathering social user profile, identifying managed users that match this profile
 * and "claiming" a single match by adding the gathered social profile to the managed user profile and passing the
 * managed user id to the next stage.  The next stage may use that id to log the user in automatically.
 *
 * Caller is expected to pass the provider's name and the access and id tokens resulting from a successful
 * login via the provider.
 *
 * If this stage successfully claims an existing managed/user profile then that profile will be included in
 * the output of the stage in the "claimedProfile" attribute.  In all cases the provider and both tokens passed
 * by the caller will be returned to the caller.
 */
public final class SocialUserClaimStage implements ProgressStage<SocialUserClaimConfig> {

    private static final Logger logger = LoggerFactory.getLogger(SocialUserClaimStage.class);

    private static final String IDP_DATA_OBJECT = "idpData";

    // State keys
    private static final String CLAIMED_PROFILE = "claimedProfile";

    private final Client httpClient;
    private final PropertyMappingService mappingService;
    private final ConnectionFactory connectionFactory;

    /**
     * Constructs a new user registration stage.
     *
     * @param httpClient the http client
     * @param mappingService the source to target property mapping service
     */
    @Inject
    public SocialUserClaimStage(@SelfService Client httpClient,
            @SelfService PropertyMappingService mappingService, @SelfService ConnectionFactory connectionFactory) {
        this.httpClient = httpClient;
        this.mappingService = mappingService;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context, SocialUserClaimConfig config)
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
                .newInstance("Claim Profile")
                .addProperty(PROVIDER, "string", "Anonymous subject's identity provider")
                .addProperty(ACCESS_TOKEN, "string", "Anonymous subject's access token")
                .addProperty(ID_TOKEN, "string", "Anonymous subject's id token")
                .addDefinition("providers", newArray(oneOf(providers.toArray(new JsonValue[0]))))
                .build();
    }

    @Override
    public StageResponse advance(ProcessContext context, SocialUserClaimConfig config) throws ResourceException {
        if (context.getStageTag().equals(INITIAL_TAG)) {
            return fetchIdPProfileAndClaimExisting(context, config);
        }
        throw new IllegalStageTagException(context.getStageTag());
    }

    private StageResponse fetchIdPProfileAndClaimExisting(ProcessContext context, SocialUserClaimConfig config)
            throws ResourceException {
        final JsonValue provider = context.getInput().get(PROVIDER);
        final JsonValue accessToken = context.getInput().get(ACCESS_TOKEN);
        final JsonValue idToken = context.getInput().get(ID_TOKEN);
        final JsonValue tokens = json(object(
                field(ACCESS_TOKEN, accessToken.asString()),
                field(ID_TOKEN, idToken)
        ));
        final JsonValue userResponse = getSocialUser(provider.asString(), tokens, config, context);
        if (userResponse == null) {
            throw new BadRequestException("Unable to reach social provider or unknown provider given");
        }
        context.putSuccessAddition(PROVIDER, provider.asString());
        context.putSuccessAddition(ACCESS_TOKEN, accessToken.asString());
        context.putSuccessAddition(ID_TOKEN, idToken);

        JsonValue candidates = findCandidates(context, userResponse, config);
        switch (candidates.asList().size()) {
            case 0:
                // No candidates to claim
                return StageResponse.newBuilder().build();

            case 1:
                // Claim the matched user then exit the stage
                JsonValue matchedUser = candidates.asList(JsonValue.class).get(0);
                ProviderConfig providerConfig = getProviderConfig(provider.asString(), config.getProviders());
                if (providerConfig == null) {
                    throw new BadRequestException("No such provider configuration: " + provider.asString());
                }

                PatchOperation operation = PatchOperation.add("idpData/" + provider.asString(),
                        context.getState(IDP_DATA_OBJECT).get(provider.asString()));
                PatchRequest request = Requests.newPatchRequest(config.getIdentityServiceUrl(),
                        matchedUser.get("_id").asString(), operation);
                connectionFactory.getConnection().patch(context.getRequestContext(), request);

                context.putSuccessAddition(CLAIMED_PROFILE, config.getIdentityServiceUrl()
                        + "/" + matchedUser.get("_id").getObject());

                return StageResponse.newBuilder().build();

            default:
                // More than 1 match is a failure -- no idea which one to claim
                throw new BadRequestException("Cannot create new profile due to claim failure: Too many matches");
        }
    }

    private JsonValue findCandidates(ProcessContext context, JsonValue profile, SocialUserClaimConfig config)
            throws ResourceException {
        final JsonValue candidates = json(array());

        QueryFilter<JsonPointer> queryFilter = QueryFilters.parse(replaceTokens(config.getClaimQueryFilter(), profile));

        QueryRequest request = Requests.newQueryRequest(config.getIdentityServiceUrl()).setQueryFilter(queryFilter);
        connectionFactory.getConnection().query(context.getRequestContext(), request, new QueryResourceHandler() {
            @Override
            public boolean handleResource(ResourceResponse resourceResponse) {
                candidates.add(resourceResponse.getContent());
                return true;
            }
        });

        return candidates;
    }

    private String replaceTokens(String filter, JsonValue normalizedProfile) {
        Pattern pattern = Pattern.compile("(\\{\\{[^}]*\\}\\})");
        Matcher matcher = pattern.matcher(filter);
        String parsed = filter;
        while (matcher.find()) {
            String matched = matcher.group();
            String tagName = matched.substring(2, matched.length() - 2);
            if (normalizedProfile.get(tagName) != null) {
                parsed = parsed.replace(matched, normalizedProfile.get(tagName).asString());
            }
        }
        return parsed;
    }

    private JsonValue getSocialUser(final String providerName, final JsonValue tokens,
            final SocialUserClaimConfig config, final ProcessContext context)
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
