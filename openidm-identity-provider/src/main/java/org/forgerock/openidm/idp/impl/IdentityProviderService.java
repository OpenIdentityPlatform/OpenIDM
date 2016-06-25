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

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
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
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service used to store configuration about the various
 * identity providers. This will be used to inject the configuration
 * into other areas such as the AuthenticationService.
 */
@Component(name = IdentityProviderService.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service({ IdentityProviderService.class, SingletonResourceProvider.class })
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Identity Provider Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "/identityProviders")})
public class IdentityProviderService implements SingletonResourceProvider {

    /** The PID for this Component */
    public static final String PID = "org.forgerock.openidm.identityProviders";
    public final static String PROVIDERS = "providers";

    private static final Logger logger = LoggerFactory.getLogger(IdentityProviderService.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    private static final ResourceException NOT_SUPPORTED = new NotSupportedException("Operation is not implemented");

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    /** String refers to the name of the provider **/
    private final List<ProviderConfig> providerConfigs = new ArrayList<>();

    /** A {@link Function} that returns all available supported providers as a ProviderSchemaConfig bean */
    private static final Function<JsonValue, ProviderConfig> availableProviderConfigs =
            new Function<JsonValue, ProviderConfig>() {
                @Override
                public ProviderConfig apply(JsonValue value) {
                    return mapper.convertValue(value.asMap(), ProviderConfig.class);
                }
            };

    @Activate
    public void activate(ComponentContext context) throws Exception {
        logger.info("Activating Identity Provider Service with configuration {}", context.getProperties());
        JsonValue config = enhancedConfig.getConfigurationAsJson(context).get(PROVIDERS);
        for (final ProviderConfig providerConfig :
                FluentIterable.from(config).transform(availableProviderConfigs)) {
            providerConfigs.add(providerConfig);
        }
        logger.debug("OpenIDM Identity Provider Service is activated.");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        logger.info("Identity Provider Service provider is deactivated.");
    }

    /**
     * The String param in Map is referring to the
     * type of auth the identity provider supports.
     */
    @Reference(
            referenceInterface = IdentityProviderConfig.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    private ConcurrentHashMap<String, List<IdentityProviderConfig>> identityProviders;

    protected void bindIdentityProviderConfig(final IdentityProviderConfig config) {
        if (identityProviders == null) {
            identityProviders = new ConcurrentHashMap<>();
        }
        // for this to be true, we do not have any identityProviders of this type
        if (identityProviders.get(config.getIdentityProviderConfig().getType()) == null) {
            // initialize new array list to store providers of this type
            List<IdentityProviderConfig> providers = new ArrayList<>();
            providers.add(config);
            identityProviders.put(config.getIdentityProviderConfig().getType(), providers);
        } else {
            // we currently have existing configs of this type, just add to it
            identityProviders.get(config.getIdentityProviderConfig().getType()).add(config);
        }
    }

    protected void unbindIdentityProviderConfig(final IdentityProviderConfig config) {
        if (identityProviders.get(config.getIdentityProviderConfig().getType()) != null) {
            logger.debug("Removed the {} identity provider.", config.getIdentityProviderConfig().getName());
            identityProviders.get(config.getIdentityProviderConfig().getType()).remove(config);
        }
    }

    /**
     * Returns all identityProviders that have been registered.
     * @return
     */
    public List<ProviderConfig> getIdentityProviders() {
        if (identityProviders == null) {
            identityProviders = new ConcurrentHashMap<>();
        }
        List<ProviderConfig> allProviders = new ArrayList<>();
        for (List<IdentityProviderConfig> authType : identityProviders.values()) {
            for (IdentityProviderConfig config: authType) {
                allProviders.add(config.getIdentityProviderConfig());
            }
        }
        return allProviders;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest actionRequest) {
        try {
            if (actionRequest.getAction().equals("availableProviders")) {
                return newActionResponse(json(object(field("providers", providerConfigs)))).asPromise();
            } else {
                return new BadRequestException("Not a supported action.").asPromise();
            }
        } catch (JsonValueException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest patchRequest) {
        return NOT_SUPPORTED.asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest readRequest) {
        JsonValue identityProviders = json(array());
        for (ProviderConfig config : getIdentityProviders()) {
            JsonValue provider = json(mapper.convertValue(config, Map.class));
            provider.remove(ProviderConfig.CLIENT_SECRET);
            identityProviders.add(provider.asMap());
        }
        return newResourceResponse(null, null,
                json(object(field(PROVIDERS, identityProviders.asList()))))
                .asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest updateRequest) {
        return NOT_SUPPORTED.asPromise();
    }
}
