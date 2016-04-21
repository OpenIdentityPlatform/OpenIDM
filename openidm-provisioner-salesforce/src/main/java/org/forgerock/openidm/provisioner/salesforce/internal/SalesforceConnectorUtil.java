/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */
package org.forgerock.openidm.provisioner.salesforce.internal;

import org.forgerock.json.JsonValueException;
import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.crypto.CryptoService;
import org.osgi.framework.Bundle;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openidm.provisioner.ConnectorConfigurationHelper.CONFIGURATION_PROPERTIES;

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

}
