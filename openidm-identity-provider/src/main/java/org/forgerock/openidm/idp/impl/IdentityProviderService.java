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
package org.forgerock.openidm.idp.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.forgerock.http.handler.HttpClientHandler.OPTION_LOADER;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.idp.impl.ProviderConfigMapper.*;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.SingletonProvider;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.apache.async.AsyncHttpClientProvider;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.spi.Loader;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.idp.client.OAuthHttpClient;
import org.forgerock.openidm.idp.impl.api.IdentityProviderServiceResource;
import org.forgerock.openidm.idp.impl.api.IdentityProviderServiceResourceWithNoSecret;
import org.forgerock.services.context.Context;
import org.forgerock.util.Options;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service used to store configuration about the various
 * identity providers. This will be used to inject the configuration
 * into other areas such as the AuthenticationService.
 */
@SingletonProvider(@Handler(
        id = "identityProviderService:0",
        title = "Identity Provider Service",
        description = "Service that handles Identity Provider configuration",
        mvccSupported = false,
        resourceSchema = @Schema(fromType = IdentityProviderServiceResourceWithNoSecret.class)))
@Component(name = IdentityProviderService.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service({ IdentityProviderService.class, SingletonResourceProvider.class })
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Identity Provider Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "/identityProviders")})
public class IdentityProviderService implements SingletonResourceProvider {

    /** The PID for this Component */
    public static final String PID = "org.forgerock.openidm.identityProviders";
    public static final String PROVIDERS = "providers";

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(IdentityProviderService.class);

    private static final JwtReconstruction jwtReconstruction = new JwtReconstruction();

    /** Constant unsupported exception for 501 response to unimplemented methods */
    private static final ResourceException NOT_SUPPORTED = new NotSupportedException("Operation is not implemented");

    /** Transformation function to remove client secret */
    public static final Function<JsonValue, JsonValue> withoutClientSecret =
            new Function<JsonValue, JsonValue>() {
                @Override
                public JsonValue apply(JsonValue value) {
                    JsonValue copy = value.copy();
                    copy.remove(ProviderConfig.CLIENT_SECRET);
                    return copy;
                }
            };

    /** Transformation function to remove client secret */
    private static final Function<JsonValue, Object> toUnwrappedJsonValue =
            new Function<JsonValue, Object>() {
                @Override
                public Object apply(JsonValue value) {
                    return value.getObject();
                }
            };

    private enum Action { availableProviders, getProfile }

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    /**
     * The String param in Map is referring to the
     * type of auth the identity provider supports.
     */
    @Reference(
            referenceInterface = IdentityProviderConfig.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    private final Map<String, List<IdentityProviderConfig>> identityProviders = new ConcurrentHashMap<>();

    protected void bindIdentityProviderConfig(final IdentityProviderConfig config)
            throws IdentityProviderServiceException {
        // for this to be true, we do not have any identityProviders of this type
        if (!identityProviders.containsKey(config.getIdentityProviderConfig().getType())) {
            // initialize new array list to store providers of this type
            List<IdentityProviderConfig> providers = new ArrayList<>();
            providers.add(config);
            identityProviders.put(config.getIdentityProviderConfig().getType(), providers);
        } else {
            // we currently have existing configs of this type, just add to it
            identityProviders.get(config.getIdentityProviderConfig().getType()).add(config);
        }
        notifyListeners();
    }

    protected void unbindIdentityProviderConfig(final IdentityProviderConfig config)
            throws IdentityProviderServiceException {
        if (identityProviders.get(config.getIdentityProviderConfig().getType()) != null) {
            logger.debug("Removed the {} identity provider.", config.getIdentityProviderConfig().getName());
            identityProviders.get(config.getIdentityProviderConfig().getType()).remove(config);
            notifyListeners();
        }
    }

    /** String refers to the name of the provider **/
    private final List<ProviderConfig> providerConfigs = new ArrayList<>();

    /** Key is typically the PID of the service implementing the listener  */
    private final Map<String, IdentityProviderListener> identityProviderListeners = new ConcurrentHashMap<>();

    @Activate
    public void activate(ComponentContext context) throws Exception {
        logger.info("Activating Identity Provider Service with configuration {}", context.getProperties());
        JsonValue config = enhancedConfig.getConfigurationAsJson(context).get(PROVIDERS);
        providerConfigs.addAll(FluentIterable.from(config).transform(toProviderConfig).toList());
        logger.debug("OpenIDM Identity Provider Service is activated.");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        identityProviderListeners.clear();
        logger.info("Identity Provider Service provider is deactivated.");
    }

    /**
     * Returns all identity providers that are of the specified type.
     *
     * @param type authentication module type
     * @return List of {@link ProviderConfig}
     */
    public List<ProviderConfig> getIdentityProviderByType(final String type) {
        final List<ProviderConfig> providers = new ArrayList<>();
        if (identityProviders == null || identityProviders.size() == 0) {
            logger.debug("No Identity Providers have been configured.");
            return providers;
        }
        for (final IdentityProviderConfig config : identityProviders.get(type)) {
            providers.add(config.getIdentityProviderConfig());
        }
        return providers;
    }

    /**
     * Returns all identityProviders that have been registered.
     * @return
     */
    public List<ProviderConfig> getIdentityProviders() {
        List<ProviderConfig> allProviders = new ArrayList<>();
        for (List<IdentityProviderConfig> authType : identityProviders.values()) {
            for (IdentityProviderConfig config: authType) {
                allProviders.add(config.getIdentityProviderConfig());
            }
        }
        return allProviders;
    }

    /**
     * Returns a {@link ProviderConfig} for the specified provider name.
     *
     * @param providerName name of the provider to retrieve configuration for
     * @return {@link ProviderConfig} associated with the provider name
     */
    public ProviderConfig getIdentityProvider(final String providerName) {
        for (List<IdentityProviderConfig> authType : identityProviders.values()) {
            for (IdentityProviderConfig config: authType) {
                if (config.getIdentityProviderConfig().getName().equals(providerName)) {
                    return config.getIdentityProviderConfig();
                }
            }
        }
        return null;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest actionRequest) {
        try {
            switch (actionRequest.getActionAsEnum(Action.class)) {
            case availableProviders:
                return getAvailableProviders();
            case getProfile:
                return getProfile(actionRequest.getContent());
            default:
                return new BadRequestException("Not a supported Action").asPromise();
            }
        } catch (JsonValueException | IllegalArgumentException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @org.forgerock.api.annotations.Action(operationDescription =
    @Operation(
            description = "Retrieves all Identity Provider Configurations that are supported.",
            errors = {
                    @ApiError(
                            code = 400,
                            description = "Indicates that the request could not be understood by "
                                    + "the resource due to malformed syntax.")
            }),
            name = "availableProviders",
            response = @Schema(fromType = IdentityProviderServiceResource.class))
    public Promise<ActionResponse, ResourceException> getAvailableProviders() {
        return newActionResponse(json(object(field(PROVIDERS, providerConfigs)))).asPromise();
    }

    @org.forgerock.api.annotations.Action(operationDescription =
    @Operation(
            description = "Retrieves a user's profile from an Identity Provider.",
            errors = {
                    @ApiError(
                            code = 400,
                            description = "Indicates that the request could not be understood by "
                                    + "the resource due to malformed syntax.")
            }),
            name = "getProfile",
            request = @Schema(schemaResource = "getProfileRequest.json"),
            response = @Schema(schemaResource = "getProfileResponse.json"))
    public Promise<ActionResponse, ResourceException> getProfile(final JsonValue content)
            throws HttpApplicationException, ResourceException {
        final ProviderConfig providerConfig =
                getIdentityProvider(content.get(OAuthHttpClient.PROVIDER).required().asString());
        final JsonValue profile = new OAuthHttpClient(
                getIdentityProvider(content.get(OAuthHttpClient.PROVIDER).required().asString()),
                newHttpClient())
                .getProfile(
                        jwtReconstruction,
                        content.get(OAuthHttpClient.CODE).required().asString(),
                        content.get(OAuthHttpClient.NONCE).required().asString(),
                        content.get(OAuthHttpClient.REDIRECT_URI).required().asString());
        return newActionResponse(buildIdpObject(providerConfig, profile)).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest patchRequest) {
        return NOT_SUPPORTED.asPromise();
    }

    @org.forgerock.api.annotations.Read(operationDescription =
    @Operation(
            description = "Retrieves all Identity Provider configurations with client-secrets removed.",
            errors = {
                    @ApiError(
                            code = 400,
                            description = "Indicates that the request could not be understood by "
                                    + "the resource due to malformed syntax.")
            }))
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest readRequest) {
        return newResourceResponse(null, null,
                json(object(field(PROVIDERS,
                        FluentIterable.from(getIdentityProviders())
                        .filter(providerEnabled)
                        .transform(toJsonValue)
                        .transform(withoutClientSecret)
                        .transform(toUnwrappedJsonValue)
                        .toList()
                ))))
                .asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest updateRequest) {
        return NOT_SUPPORTED.asPromise();
    }

    /**
     * Registers a IdentityProviderListener.
     *
     * @param listener {@link IdentityProviderListener} to be added
     */
    public void registerIdentityProviderListener(IdentityProviderListener listener) {
        identityProviderListeners.put(listener.getListenerName(), listener);
    }

    /**
     * Unregisters a IdentityProviderListener.
     *
     * @param listener {@link IdentityProviderListener} to be removed
     */
    public void unregisterIdentityProviderListener(IdentityProviderListener listener) {
        identityProviderListeners.remove(listener.getListenerName());
    }

    /**
     * Notifies the registered listeners of configuration changes
     * on any identity provider configuration.
     */
    public void notifyListeners() throws IdentityProviderServiceException {
        for (IdentityProviderListener listener : identityProviderListeners.values()) {
            listener.identityProviderConfigChanged();
        }
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
}
