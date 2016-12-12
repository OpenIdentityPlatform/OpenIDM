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
 * Copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openidm.provisioner.salesforce.internal;

import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.apache.async.AsyncHttpClientProvider;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.spi.Loader;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.util.Options;
import org.osgi.framework.Bundle;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.forgerock.http.handler.HttpClientHandler.OPTION_LOADER;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openidm.provisioner.ConnectorConfigurationHelper.CONFIGURATION_PROPERTIES;
import static org.forgerock.util.time.Duration.duration;

/**
 * Provides "connector" info for the Salesforce "connector".
 */
public class SalesforceConnectorUtil {
    static JsonValue getConnectorInfo(Bundle bundle) {
        return json(object(
                field("bundleName", bundle.getSymbolicName()),
                field("bundleVersion", bundle.getVersion().toString()),
                field("displayName", "Salesforce Connector"),
                field("connectorName", "org.forgerock.openidm.salesforce.Salesforce")
        ));
    }

    static final List<JsonPointer> PROPERTIES_TO_ENCRYPT = Arrays.asList(
            new JsonPointer(new String[] { CONFIGURATION_PROPERTIES, "clientSecret" }),
            new JsonPointer(new String[] { CONFIGURATION_PROPERTIES, "refreshToken" }));

    static SalesforceConfiguration parseConfiguration(JsonValue config, CryptoService cryptoService) {
        final JsonValue decrypted = getDecryptedConfigProperties(config, cryptoService);

        return SalesforceConnection.mapper.convertValue(
                decrypted.get(CONFIGURATION_PROPERTIES).required().expect(Map.class).asMap(),
                SalesforceConfiguration.class);
    }

    private static JsonValue getDecryptedConfigProperties(JsonValue properties, CryptoService cryptoService) {
        JsonValue decrypted = properties.copy();

        for (JsonPointer property : PROPERTIES_TO_ENCRYPT) {
            if (JsonCrypto.isJsonCrypto(properties.get(property))) {
                decrypted.put(property, cryptoService.decrypt(properties.get(property)).getObject());
            } else {
                decrypted.put(property, properties.get(property).getObject());
            }
        }

        return decrypted;
    }

    /**
     * Adapts an {@code Exception} to a {@code ResourceException}.
     *
     * @param t
     *            The exception which caused the request to fail.
     * @return The equivalent resource exception.
     */
    public static ResourceException adapt(final Throwable t) {
        int resourceResultCode;
        try {
            throw t;
        } catch (final ResourceException e) {
            return e;
        } catch (final JsonValueException e) {
            resourceResultCode = ResourceException.BAD_REQUEST;
        } catch (final Throwable e) {
            resourceResultCode = ResourceException.INTERNAL_ERROR;
        }
        return newResourceException(resourceResultCode, t.getMessage(), t);
    }

    /**
     * Builds an {@link AsyncHttpClientProvider} instance, which must be closed on shutdown/de-activation.
     *
     * @return {@link AsyncHttpClientProvider} instance
     */
    static HttpClientHandler newHttpClientHandler(SalesforceConfiguration configuration) {
        try {
            return new HttpClientHandler(
                    Options.defaultOptions()
                            .set(OPTION_LOADER, new Loader() {
                                @Override
                                public <S> S load(Class<S> service, Options options) {
                                    return service.cast(new AsyncHttpClientProvider());
                                }
                            })
                            .set(HttpClientHandler.OPTION_CONNECT_TIMEOUT, duration(configuration.getConnectTimeout() + " milliseconds"))
                            .set(HttpClientHandler.OPTION_SO_TIMEOUT, duration(
                                    configuration.getSocketTimeout() == 0 ? "unlimited" : configuration.getSocketTimeout() + " seconds"))
                            .set(HttpClientHandler.OPTION_SSLCONTEXT_ALGORITHM, "TLSv1.2")
            );
        } catch (HttpApplicationException e) {
            throw new IllegalStateException("Error while building HTTP Client Handler", e);
        }
    }

}
