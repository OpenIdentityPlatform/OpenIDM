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
 * Copyright 2011-2016 ForgeRock AS
 */

// TODO: Expose as a set of resource actions.
package org.forgerock.openidm.crypto.impl;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValueFunctions.identity;

import java.security.Key;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.crypto.JsonDecryptFunction;
import org.forgerock.json.crypto.JsonEncryptor;
import org.forgerock.json.crypto.simple.SimpleDecryptor;
import org.forgerock.json.crypto.simple.SimpleEncryptor;
import org.forgerock.json.crypto.simple.SimpleKeySelector;
import org.forgerock.json.crypto.simple.SimpleKeyStoreSelector;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.FieldStorageScheme;
import org.forgerock.openidm.crypto.KeyRepresentation;
import org.forgerock.openidm.crypto.SaltedMD5FieldStorageScheme;
import org.forgerock.openidm.crypto.SaltedSHA1FieldStorageScheme;
import org.forgerock.openidm.crypto.SaltedSHA256FieldStorageScheme;
import org.forgerock.openidm.crypto.SaltedSHA384FieldStorageScheme;
import org.forgerock.openidm.crypto.SaltedSHA512FieldStorageScheme;
import org.forgerock.openidm.crypto.SharedKeyService;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.util.Function;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cryptography Service
 */
@Component(
        name = "org.forgerock.openidm.crypto",
        immediate = true,
        policy = ConfigurationPolicy.OPTIONAL
)
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM cryptography service"),
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME)
})
@Service({ CryptoService.class, SharedKeyService.class })
public class CryptoServiceImpl implements CryptoService, SharedKeyService {

    private final static Logger logger = LoggerFactory.getLogger(CryptoServiceImpl.class);
    private Function<JsonValue, JsonValue, JsonValueException> decryptionFunction = identity();
    private SimpleKeySelector keySelector;

    @Reference(target="(service.pid=org.forgerock.openidm.impl.keystore)")
    private KeyStoreService keyStoreService;

    /**
     * Constructs an implementation of the {@link CryptoService}.
     */
    public CryptoServiceImpl() {
    }

    /**
     * Constructs an implementation of the {@link CryptoService} with a given
     * {@link SimpleKeySelector} and decryption {@link Function}.
     *
     * @param keySelector The {@link SimpleKeySelector}.
     * @param decryptionFunction A {@link Function}s to use for decryption.
     */
    public CryptoServiceImpl(final SimpleKeySelector keySelector,
            final Function<JsonValue, JsonValue, JsonValueException> decryptionFunction) {
        this.keySelector = keySelector;
        this.decryptionFunction = decryptionFunction;
    }

    public void bindKeyStoreService(final KeyStoreService keyStoreService) {
        // this method is necessary for CryptoServiceFactory to be able to bind the keystore service.
        this.keyStoreService = keyStoreService;
    }

    public void activate(@SuppressWarnings("unused") BundleContext context) {
        logger.debug("Activating cryptography service");
        try {
            keySelector =
                    new SimpleKeyStoreSelector(
                            keyStoreService.getKeyStore(),
                            keyStoreService.getKeyStoreDetails().getPassword());
            decryptionFunction = new JsonDecryptFunction(new SimpleDecryptor(keySelector));
        } catch (final JsonValueException jve) {
            logger.error("Exception when loading CryptoService configuration", jve);
            throw jve;
        }
    }

    public void deactivate(@SuppressWarnings("unused") BundleContext context) {
        decryptionFunction = identity();
        keySelector = null;
        logger.info("CryptoService stopped.");
    }

    @Override
    public JsonEncryptor getEncryptor(String cipher, String alias) throws JsonCryptoException {
        Key key = keySelector.select(alias);
        if (key == null) {
            String msg = "Encryption key " + alias + " not found";
            logger.error(msg);
            throw new JsonCryptoException(msg);
        }
        return new SimpleEncryptor(cipher, key, alias);
    }

    @Override
    public Function<JsonValue, JsonValue, JsonValueException> getDecryptionFunction() {
        return decryptionFunction;
    }

    @Override
    public JsonValue encrypt(JsonValue value, String cipher, String alias)
            throws JsonCryptoException, JsonException {
        JsonValue result = null;
        if (value != null) {
            JsonEncryptor encryptor = getEncryptor(cipher, alias);
            result = new JsonCrypto(encryptor.getType(), encryptor.encrypt(value)).toJsonValue();
        }
        return result;
    }

    @Override
    public JsonValue decrypt(JsonValue value) throws JsonValueException {
        return value != null
                ? decryptionFunction.apply(value)
                : null;
    }

    @Override
    public JsonValue decrypt(String value) throws JsonException {
        JsonValue jsonValue = JsonUtil.parseStringified(value);
        return decrypt(jsonValue);
    }

    @Override
    public JsonValue decryptIfNecessary(JsonValue value) throws JsonException {
        if (value == null) {
            return new JsonValue(null);
        }
        if (value.isNull() || !isEncrypted(value)) {
            return value;
        }
        return decrypt(value);
    }

    @Override
    public JsonValue decryptIfNecessary(String value) throws JsonException {
        JsonValue jsonValue = null;
        if (value != null) {
            jsonValue = JsonUtil.parseStringified(value);
        }
        return decryptIfNecessary(jsonValue);
    }

    @Override
    public boolean isEncrypted(JsonValue value) {
        return JsonCrypto.isJsonCrypto(value);
    }

    @Override
    public boolean isEncrypted(String value) {
        return JsonUtil.isEncrypted(value);
    }

    @Override
    public JsonValue hash(JsonValue value, String algorithm) throws JsonException, JsonCryptoException {
        final FieldStorageScheme fieldStorageScheme = getFieldStorageScheme(algorithm);
        final String plainTextField = value.asString();
        final String encodedField = fieldStorageScheme.hashField(plainTextField);
        return json(object(
                field("$crypto", object(
                        field("value", object(
                                field("algorithm", algorithm),
                                field("data", encodedField))),
                        field("type", CryptoConstants.STORAGE_TYPE_HASH)))));
    }

    @Override
    public boolean isHashed(JsonValue value) {
        return value != null 
                &&!value.isNull() 
                && JsonCrypto.isJsonCrypto(value) 
                && value.get("$crypto").get("value").isDefined("algorithm");
    }
    
    /**
     * Returns a {@link FieldStorageScheme} instance based on the supplied algorithm.
     * 
     * @param algorithm a string representing a storage scheme algorithm
     * @return a field storage scheme implementation.
     * @throws JsonCryptoException
     */
    private FieldStorageScheme getFieldStorageScheme(String algorithm) throws JsonCryptoException {
        try {
            if (algorithm.equals(CryptoConstants.ALGORITHM_MD5)) {
                return new SaltedMD5FieldStorageScheme();
            } else if (algorithm.equals(CryptoConstants.ALGORITHM_SHA_1)) {
                return new SaltedSHA1FieldStorageScheme();
            } else if (algorithm.equals(CryptoConstants.ALGORITHM_SHA_256)) {
                return new SaltedSHA256FieldStorageScheme();
            } else if (algorithm.equals(CryptoConstants.ALGORITHM_SHA_384)) {
                return new SaltedSHA384FieldStorageScheme();
            } else if (algorithm.equals(CryptoConstants.ALGORITHM_SHA_512)) {
                return new SaltedSHA512FieldStorageScheme();
            } else {
                throw new JsonCryptoException("Unsupported field storage algorithm " + algorithm);
            }
        } catch (JsonCryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonCryptoException(e.getMessage(), e);
        }
    }

    @Override
    public boolean matches(String plainTextValue, JsonValue value) throws JsonCryptoException {
        if (isHashed(value)) {
            JsonValue cryptoValue = value.get("$crypto").get("value");
            String algorithm = cryptoValue.get("algorithm").asString();
            final FieldStorageScheme fieldStorageScheme = getFieldStorageScheme(algorithm);
            return fieldStorageScheme.fieldMatches(plainTextValue, cryptoValue.get("data").asString());
        }
        return false;
    }

    @Override
    public JsonValue getSharedKey(String alias) throws Exception {
        Key key = keySelector.select(alias);
        return KeyRepresentation.toJsonValue(alias, key);
    }

}
