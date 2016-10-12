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
package org.forgerock.openidm.keystore.impl;

import static org.forgerock.openidm.core.IdentityServer.CONFIG_CRYPTO_ALIAS;
import static org.forgerock.openidm.core.IdentityServer.CONFIG_CRYPTO_ALIAS_SELF_SERVICE;
import static org.forgerock.security.keystore.KeyStoreType.PKCS11;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.lang3.tuple.Pair;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.keystore.KeyStoreDetails;
import org.forgerock.openidm.util.CertUtil;
import org.forgerock.security.keystore.KeyStoreBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the default {@link KeyStoreInitializer}. The {@link DefaultKeyStoreInitializer} will attempt to create
 * the default keys and ssl cert if they do not already exist in the keystore/truststore.
 */
class DefaultKeyStoreInitializer implements KeyStoreInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultKeyStoreInitializer.class);
    private static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA512WithRSAEncryption";
    private static final String DEFAULT_ALGORITHM = "RSA";
    private static final int DEFAULT_KEY_SIZE = 2048;

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

    @Override
    public KeyStore initializeKeyStore(final KeyStoreDetails keyStoreDetails) throws GeneralSecurityException {
        final KeyStore keyStore = loadKeyStore(keyStoreDetails);

        if (PKCS11.equals(keyStoreDetails.getType())) {
            logger.debug("Can't generate default keys when using PKCS11");
            return keyStore;
        }

        for (final Map.Entry<String, String> alias : configAliases.entrySet()) {
            final Key key = keyStore.getKey(alias.getKey(), keyStoreDetails.getPassword().toCharArray());
            if (key == null) {
                // Initialize the keys
                logger.debug("Initializing secret key entry {} in the keystore", alias);
                try {
                    generateDefaultKey(keyStore, alias.getKey(), keyStoreDetails.getFilename(),
                            keyStoreDetails.getPassword().toCharArray(), alias.getValue());
                } catch (final IOException e) {
                    logger.error("Unable to generate default key with alias: {}", alias.getKey());
                    throw new GeneralSecurityException(
                            "Unable to generate default key with alias: " + alias.getKey(), e);
                }
            }
        }
        return keyStore;
    }

    @Override
    public KeyStore initializeTrustStore(final KeyStore keyStore, KeyStoreDetails keyStoreDetails)
            throws GeneralSecurityException {
        final KeyStore trustStore = loadKeyStore(keyStoreDetails);

        if (PKCS11.equals(keyStoreDetails.getType())) {
            logger.debug("Can't generate default keys when using PKCS11");
            return keyStore;
        }

        // Create localhost cert
        final String alias =
                IdentityServer.getInstance().getProperty(IdentityServer.HTTPS_KEYSTORE_CERT_ALIAS, "openidm-localhost");

        try {
            if (trustStore.getKey(alias, keyStoreDetails.getPassword().toCharArray()) != null
                    || keyStore.getKey(alias, keyStoreDetails.getPassword().toCharArray()) != null) {
                logger.debug("Cant generate https cert since it already exists in the keystore or truststore");
            } else {
                final Pair<X509Certificate, PrivateKey> pair = CertUtil.generateCertificate("localhost",
                        "OpenIDM Self-Signed Certificate", "None", "None", "None", "None",
                        DEFAULT_ALGORITHM, DEFAULT_KEY_SIZE, DEFAULT_SIGNATURE_ALGORITHM, null, null);
                final Certificate cert = pair.getKey();
                final PrivateKey key = pair.getValue();
                final KeyStore.PrivateKeyEntry entry = new KeyStore.PrivateKeyEntry(key, new Certificate[]{cert});
                keyStore.setEntry(
                        alias, entry, new KeyStore.PasswordProtection(keyStoreDetails.getPassword().toCharArray()));
                trustStore.setEntry(
                        alias, entry, new KeyStore.PasswordProtection(keyStoreDetails.getPassword().toCharArray()));
            }
        } catch (final Exception e) {
            logger.error("Unable to create ssl certificate", e);
            throw new GeneralSecurityException(e.getMessage(), e);
        }
        return trustStore;
    }

    /**
     * Generates a default secret key entry in the keystore.
     *
     * @param ks the keystore
     * @param alias the alias of the secret key
     * @param location the keystore location
     * @param password the keystore password
     * @param algorithm the key generator algorithm
     * @throws IOException if unable to open the keystore location
     * @throws GeneralSecurityException if unable to generate secret key
     */
    private void generateDefaultKey(final KeyStore ks, final String alias, final String location, final char[] password,
            final String algorithm) throws IOException, GeneralSecurityException {
        SecretKey newKey = KeyGenerator.getInstance(algorithm).generateKey();
        ks.setEntry(alias, new KeyStore.SecretKeyEntry(newKey), new KeyStore.PasswordProtection(password));
        try (final OutputStream out = new FileOutputStream(location)) {
            ks.store(out, password);
        }
    }

    /**
     * Loads a keystore given some {@link KeyStoreDetails}.
     * @param keyStoreDetails the {@link KeyStoreDetails}.
     * @return the loaded {@link KeyStore}.
     * @throws GeneralSecurityException if unable to load the keystore.
     */
    private KeyStore loadKeyStore(final KeyStoreDetails keyStoreDetails) throws GeneralSecurityException {
        final KeyStore keyStore;
        try {
            keyStore = new KeyStoreBuilder()
                    .withKeyStoreType(keyStoreDetails.getType())
                    .withProvider(keyStoreDetails.getProvider())
                    .withPassword(keyStoreDetails.getPassword())
                    .withKeyStoreFile(keyStoreDetails.getFilename())
                    .build();
        } catch (final FileNotFoundException e) {
            logger.error("Unable to load keystore file: {}", keyStoreDetails.getFilename(), e);
            throw new GeneralSecurityException("Unable to load keystore file: " + keyStoreDetails.getFilename(), e);
        }
        return keyStore;
    }
}
