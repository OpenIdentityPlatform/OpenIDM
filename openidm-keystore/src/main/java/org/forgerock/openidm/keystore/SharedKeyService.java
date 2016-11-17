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
package org.forgerock.openidm.keystore;

import java.security.Key;
import java.security.KeyPair;

import org.forgerock.json.crypto.JsonCryptoException;

/**
 * A service interface for obtaining shared keys directly from the Keystore.
 */
public interface SharedKeyService {
    /**
     * Retrieve the shared key stored in the keystore by <em>alias</em>.
     *
     * @param alias the key alias
     * @return the Key
     * @throws Exception
     */
    Key getSharedKey(String alias) throws Exception;

    /**
     * Retrieve the KeyPair from the keystore byt the denoted <em>alias</em>.
     *
     * @param alias the keypair alias
     * @return the KeyPair
     * @throws JsonCryptoException
     */
    KeyPair getKeyPair(String alias) throws JsonCryptoException;
}
