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

import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.util.DateUtil;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Utility class to convert between {@link ProviderConfig} and {@link JsonValue} representation.
 */
public class ProviderConfigMapper {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String RAW_PROFILE = "rawProfile";
    private static final String SUBJECT = "subject";
    private static final String ENABLED = "enabled";
    private static final String SCOPE = "scope";
    private static final  String DATE_COLLECTED = "dateCollected";

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * A {@link Function} that returns a JSON provider configuration as a ProviderConfig bean
     */
    public static final Function<JsonValue, ProviderConfig> toProviderConfig =
            new Function<JsonValue, ProviderConfig>() {
                @Override
                public ProviderConfig apply(JsonValue value) {
                    return mapper.convertValue(value.asMap(), ProviderConfig.class);
                }
            };

    /**
     * A {@link Function} that returns a ProviderConfig bean as JSON
     */
    public static final Function<ProviderConfig, JsonValue> toJsonValue =
            new Function<ProviderConfig, JsonValue>() {
                @Override
                public JsonValue apply(ProviderConfig providerConfig) {
                    return json(mapper.convertValue(providerConfig, Map.class));
                }
            };
    /**
     * Converts a {@link ProviderConfig} to {@link JsonValue}.
     *
     * @param config of {@link ProviderConfig} to convert to {@link JsonValue}
     * @return {@link ProviderConfig as {@link JsonValue}}
     */
    public static JsonValue toJsonValue(ProviderConfig config) {
        return toJsonValue.apply(config);
    }

    /**
     * Converts a List of {@Link ProviderConfig} to {@link JsonValue}.
     *
     * @param configList of {@link ProviderConfig}
     * @return {@link ProviderConfig} list as a {@link JsonValue}
     */
    public static final JsonValue toJsonValue(List<ProviderConfig> configList) {
        JsonValue json = json(array());
        for (ProviderConfig config : configList) {
            json.add(toJsonValue.apply(config).asMap());
        }
        return json;
    }

    /**
     * Converts a {@link JsonValue} to {@link ProviderConfig}.
     *
     * @param value {@link JsonValue} of a {@link ProviderConfig}
     * @return {@link ProviderConfig}
     */
    public static ProviderConfig toProviderConfig(JsonValue value) {
        return toProviderConfig.apply(value);
    }

    /**
     * Builds a {@link JsonValue} object that contains user identity provider data when
     * given a raw identity profile and the associated provider that that particular
     * identity profile resides on.
     *
     * @param providerConfig {@link ProviderConfig} configuration of the Identity Provider
     * @param profile raw profile data retrieved from the Identity Provider
     *
     * @return {@link JsonValue} containing appropriate identity provider details
     */
    public final static JsonValue buildIdpObject(final ProviderConfig providerConfig, final JsonValue profile) {
        return json(object(
                field(SUBJECT, profile.get(providerConfig.getAuthenticationId()).asString()),
                field(ENABLED, true),
                field(SCOPE, providerConfig.getScope()),
                field(DATE_COLLECTED, DateUtil.getDateUtil(ServerConstants.TIME_ZONE_UTC).now()),
                field(RAW_PROFILE, profile.getObject())));
    }

    /** A {@link Predicate} that returns whether the provider is enabled */
    public static Predicate<ProviderConfig> providerEnabled = new Predicate<ProviderConfig>() {
        @Override
        public boolean apply(ProviderConfig providerConfig) {
            return providerConfig.isEnabled();
        }
    };

    private ProviderConfigMapper() {
        // prevent construction
    }
}
