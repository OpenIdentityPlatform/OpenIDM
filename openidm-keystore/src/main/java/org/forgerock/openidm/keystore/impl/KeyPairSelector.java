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
 * Copyright 2016 ForgeRock AS
 */
package org.forgerock.openidm.keystore.impl;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.crypto.simple.SimpleKeyStoreSelector;

/**
 * KeySelector extension that allows retrieval of private keys, public keys, and private/public keypairs.
 */
class KeyPairSelector extends SimpleKeyStoreSelector {

    private final KeyStore keyStore;
    private final char[] password;

    /**
     * Construct this key selector.
     *
     * @param keyStore the KeyStore
     * @param password the password
     */
    KeyPairSelector(KeyStore keyStore, String password) {
        super(keyStore, password);
        this.keyStore = keyStore;
        this.password = password.toCharArray();
    }

    /**
     * Retrieve a private key from the keystore.
     *
     * @param alias the key alias
     * @return the PrivateKey, null if could not be found
     * @throws JsonCryptoException on failure accessing the keystore
     */
    PrivateKey getPrivateKey(String alias) throws JsonCryptoException {
        try {
            return (PrivateKey) keyStore.getKey(alias, password);
        } catch (GeneralSecurityException e) {
            throw new JsonCryptoException(e);
        }
    }

    /**
     * Retrieve a public key from the keystore.
     *
     * @param alias the key alias
     * @return the PublicKey, null if could not be found
     * @throws JsonCryptoException on failure accessing the keystore
     */
    PublicKey getPublicKey(String alias) throws JsonCryptoException {
        try {
            final Certificate certificate = keyStore.getCertificate(alias);
            if (certificate == null) {
                return null;
            }
            return certificate.getPublicKey();
        } catch (GeneralSecurityException e) {
            throw new JsonCryptoException(e);
        }
    }

    /**
     * Retrieve a public/private key-pair from the keystore.
     *
     * @param alias the keypair alias
     * @return the KeyPair
     * @throws JsonCryptoException on failure accessing the keystore
     */
    KeyPair getKeyPair(String alias) throws JsonCryptoException {
        PrivateKey privateKey = getPrivateKey(alias);
        PublicKey publicKey = getPublicKey(alias);
        if (privateKey == null || publicKey == null) {
            return null;
        }
        return new KeyPair(publicKey, privateKey);
    }
}
