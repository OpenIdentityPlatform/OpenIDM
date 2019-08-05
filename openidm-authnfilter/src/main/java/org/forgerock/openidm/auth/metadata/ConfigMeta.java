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
package org.forgerock.openidm.auth.metadata;

import static org.forgerock.openidm.auth.AuthenticationService.*;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.auth.AuthenticationService;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.forgerock.openidm.metadata.WaitForMetaData;

/**
 * Meta data provider to describe configuration
 * requirements of this bundle.
 */
public class ConfigMeta implements MetaDataProvider {

    /** Function to provide the resolvers array from an OIDC or OAUTH2 auth module configuration. */
    private static final Function<JsonValue, JsonValue> toResolvers =
            new Function<JsonValue, JsonValue>() {
                @Override
                public JsonValue apply(JsonValue authModuleConfig) {
                    return authModuleConfig.get(AUTH_MODULE_PROPERTIES_KEY).get(AUTH_MODULE_RESOLVERS_KEY);
                }
            };

    /** Function to provide the client secret pointer(s) from a resolver array. */
    private static final Function<JsonValue, Iterable<JsonPointer>> resolverToClientSecretPointers =
            new Function<JsonValue, Iterable<JsonPointer>>() {
                @Override
                public Iterable<JsonPointer> apply(JsonValue resolvers) {
                    return FluentIterable.from(resolvers).transform(toClientSecret);
                }
            };

    /** Function to retrieve the client secret property from the resolver properties. */
    private static Function<JsonValue, JsonPointer> toClientSecret =
            new Function<JsonValue, JsonPointer>() {
                @Override
                public JsonPointer apply(JsonValue resolverProperties) {
                    return resolverProperties.get(ProviderConfig.CLIENT_SECRET).getPointer();
                }
            };

    @Override
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias, JsonValue config)
            throws WaitForMetaData {
        if (!AuthenticationService.PID.equalsIgnoreCase(pidOrFactory)) {
            return null;
        }
        return FluentIterable.from(config.get(SERVER_AUTH_CONTEXT_KEY).get(AUTH_MODULES_KEY))
                .filter(oidcAndOauth2Modules)
                .transform(toResolvers)
                .transformAndConcat(resolverToClientSecretPointers)
                .toList();
    }

    @Override
    public void setCallback(MetaDataProviderCallback callback) {
        // callback not used
    }

}
