/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openidm.crypto.impl;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;

import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.crypto.simple.SimpleKeySelector;

/**
 * Interface to select and keys from a keystore.  Includes an update method
 * update the keystore.
 */
public class UpdatableKeyStoreSelector implements SimpleKeySelector {

    /** Key store to select keys from. */
    private KeyStore keyStore;

    /** Password to retrieve keys with. */
    private char[] password;

    /**
     * Constructs a simple key store selector.
     *
     * @param keyStore the key store to select keys from.
     * @param password the password to use to decrypt selected keys.
     */
    public UpdatableKeyStoreSelector(KeyStore keyStore, String password) {
        this.keyStore = keyStore;
        this.password = password.toCharArray();
    }

    @Override
    public Key select(String key) throws JsonCryptoException {
        try {
            return keyStore.getKey(key, password);
        } catch (GeneralSecurityException gse) {
            throw new JsonCryptoException(gse);
        }
    }
    
    /**
     * Updates the KeyStore and password
     * 
     * @param keyStore the new KeyStore
     * @param password the new password
     */
    public void update(KeyStore keyStore, String password) {
        this.keyStore = keyStore;
        this.password = password.toCharArray();
    }

}
