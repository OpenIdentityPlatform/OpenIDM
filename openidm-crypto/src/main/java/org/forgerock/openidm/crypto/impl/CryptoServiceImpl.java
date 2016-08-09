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

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openidm.core.IdentityServer.*;
import static org.forgerock.security.keystore.KeyStoreType.PKCS11;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonTransformer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.crypto.JsonCryptoTransformer;
import org.forgerock.json.crypto.JsonEncryptor;
import org.forgerock.json.crypto.simple.SimpleDecryptor;
import org.forgerock.json.crypto.simple.SimpleEncryptor;
import org.forgerock.openidm.core.IdentityServer;
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
import org.forgerock.openidm.crypto.factory.CryptoUpdateService;
import org.forgerock.openidm.util.ClusterUtil;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.security.keystore.KeyStoreBuilder;
import org.forgerock.security.keystore.KeyStoreType;
import org.forgerock.util.Utils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cryptography Service
 */
public class CryptoServiceImpl implements CryptoService, CryptoUpdateService, SharedKeyService {

    private final static Logger logger = LoggerFactory.getLogger(CryptoServiceImpl.class);
    private final ArrayList<JsonTransformer> decryptionTransformers = new ArrayList<>();
    private UpdatableKeyStoreSelector keySelector;

    /**
     * Constructs an implementation of the {@link CryptoService}.
     */
    public CryptoServiceImpl() {
    }

    /**
     * Constructs an implementation of the {@link CryptoService} with a given
     * {@link UpdatableKeyStoreSelector} and list of {@link JsonTransformer}'s.
     *
     * @param keySelector The {@link UpdatableKeyStoreSelector}.
     * @param decryptionTransformers A list of {@link JsonTransformer}'s to use for decryption.
     */
    public CryptoServiceImpl(final UpdatableKeyStoreSelector keySelector,
            final List<JsonTransformer> decryptionTransformers) {
        this.keySelector = keySelector;
        this.decryptionTransformers.addAll(decryptionTransformers);
    }

    /** Map of crypto secret key aliases and the algorithm they should use */
    private static final Map<String, String> configAliases = new LinkedHashMap<>(3);
    static {
        // each alias is stored under a specific property in boot.properties
        configAliases.put(
                IdentityServer.getInstance().getProperty(CONFIG_CRYPTO_ALIAS),
                "AES");
        configAliases.put(
                IdentityServer.getInstance().getProperty(CONFIG_CRYPTO_ALIAS_SELF_SERVICE),
                "AES");
        configAliases.put(
                // OPENIDM-6190 jwt-session signing key must be 256bit
                IdentityServer.getInstance().getProperty(
                        ServerConstants.JWTSESSION_SIGNING_KEY_ALIAS_PROPERTY,
                        ServerConstants.DEFAULT_JWTSESSION_SIGNING_KEY_ALIAS),
                "HmacSHA256");
    }

    public void activate(@SuppressWarnings("unused") BundleContext context) {
        logger.debug("Activating cryptography service");
        try {
            int keyCount = 0;
            final String password = IdentityServer.getInstance().getProperty(KEYSTORE_PASSWORD);
            final String instanceType =
                    IdentityServer.getInstance().getProperty(INSTANCE_TYPE, ClusterUtil.TYPE_STANDALONE);
            final KeyStoreType type =
                    Utils.asEnum(
                            IdentityServer.getInstance().getProperty(KEYSTORE_TYPE, KeyStore.getDefaultType()),
                            KeyStoreType.class);
            final String provider = IdentityServer.getInstance().getProperty(KEYSTORE_PROVIDER);
            final String location =
                    IdentityServer.getFileForPath(
                            IdentityServer.getInstance().getProperty(KEYSTORE_LOCATION),
                            IdentityServer.getInstance().getInstallLocation()).getAbsolutePath();

            final char[] clearPassword;
            final KeyStore keyStore;

            try {
                clearPassword = Main.unfold(password);
                if (PKCS11.equals(type)) {
                    keyStore = new KeyStoreBuilder()
                            .withKeyStoreType(type)
                            .withPassword(clearPassword)
                            .withProvider(provider)
                            .build();
                } else {
                    keyStore = new KeyStoreBuilder()
                            .withKeyStoreType(type)
                            .withPassword(clearPassword)
                            .withProvider(provider)
                            .withKeyStoreFile(location)
                            .build();
                }
                keySelector = new UpdatableKeyStoreSelector(keyStore, new String(clearPassword));
                decryptionTransformers.add(new JsonCryptoTransformer(new SimpleDecryptor(
                        keySelector)));
                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    logger.debug("Available cryptography key: {}", aliases.nextElement());
                    keyCount++;
                }
            } catch (final FileNotFoundException e) {
                throw new RuntimeException("Unable to open keystore with filename: " + location, e);
            } catch (final GeneralSecurityException e) {
                throw new RuntimeException("Unable to get clear text keystore password", e);
            }

            try {
                if ((instanceType.equals(ClusterUtil.TYPE_STANDALONE)
                        || instanceType.equals(ClusterUtil.TYPE_CLUSTERED_FIRST))
                        && !PKCS11.equals(type)) {
                    for (Map.Entry<String, String> alias : configAliases.entrySet()) {
                        Key key = keyStore.getKey(alias.getKey(), clearPassword);
                        if (key == null) {
                            // Initialize the keys
                            logger.debug("Initializing secret key entry {} in the keystore", alias);
                            generateDefaultKey(keyStore, alias.getKey(), location, clearPassword, alias.getValue());
                        }
                    }
                }
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException("Unable to initialize default keys in keystore", e);
            }
            logger.info("CryptoService is initialized with {} keys.", keyCount);
        } catch (final JsonValueException jve) {
            logger.error("Exception when loading CryptoService configuration", jve);
            throw jve;
        }
    }

    /**
     * Generates a default secret key entry in the keystore.
     * 
     * @param ks the keystore
     * @param alias the alias of the secret key
     * @param location the keystore location
     * @param password the keystore password
     * @param algorithm the key generator algorithm
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private void generateDefaultKey(KeyStore ks, String alias, String location, char[] password, String algorithm)
            throws IOException, GeneralSecurityException {
        SecretKey newKey = KeyGenerator.getInstance(algorithm).generateKey();
        ks.setEntry(alias, new SecretKeyEntry(newKey), new KeyStore.PasswordProtection(password));
        try (final OutputStream out = new FileOutputStream(location)) {
            ks.store(out, password);
        }
    }
    
    public void updateKeySelector(KeyStore ks, String password) {
        keySelector.update(ks, password);
        decryptionTransformers.add(new JsonCryptoTransformer(new SimpleDecryptor(keySelector)));
    }

    public void deactivate(@SuppressWarnings("unused") BundleContext context) {
        decryptionTransformers.clear();
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
    public List<JsonTransformer> getDecryptionTransformers() {
        return decryptionTransformers;
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
    public JsonValue decrypt(JsonValue value) throws JsonException {
        JsonValue result = null;
        if (value != null) {
            result = new JsonValue(value);
            result.getTransformers().addAll(0, getDecryptionTransformers());
            result.applyTransformers();
            result = result.copy();
        }
        return result;
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
