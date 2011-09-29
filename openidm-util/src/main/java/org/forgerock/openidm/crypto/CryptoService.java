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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.crypto;

// Java Standard Edition
import java.util.List;

// JSON Fluent library
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonTransformer;

// JSON Cryptographic library
import org.forgerock.json.crypto.JsonCryptoException;

/**
 * Provides encryption and decryption services to OpenIDM components.
 *
 * @author Paul C. Bryan
 * @author aegloff
 */
public interface CryptoService {
    
    /**
     * Returns a transformer that can encrypt JSON nodes.
     *
     * @param cipher the cipher with which to encrypt a node.
     * @param alias the key alias in the key store with which to encrypt a node.
     * @return a transformer that can encrypt JSON nodes with the given cipher and key.
     * @throws JsonCryptoException if an invalid cipher and/or alias is provided.
     */
    JsonTransformer getEncryptionTransformer(String cipher, String alias) throws JsonCryptoException;

    /**
     * Returns a list of decryption transformers that can decrypt JSON nodes.
     *
     * @return a list of transformers that can decrypt JSON nodes.
     */
    List<JsonTransformer> getDecryptionTransformers();

    /**
     * Encrypts a JSON node.
     *
     * @param node the JSON node to be encrypted.
     * @param cipher the cipher with which to encrypt the node.
     * @param alias the key alias in the key store with which to encrypt the node.
     * @return a copy of the node, encrypted with the specified cipher and key.
     * @throws JsonCryptoException if and invalid cipher and/or alias is provided.
     * @throws JsonException if an exception occurred encrypting the node.
     */
    JsonNode encrypt(JsonNode node, String cipher, String alias) throws JsonCryptoException, JsonException;

    /**
     * Decrypts a JSON node and all of its children.
     *
     * @param node the JSON node to be decrypted.
     * @return a deep copy of the node, with all values decrypted.
     * @throws JsonException if an exception occurred decrypting the node.
     */
    JsonNode decrypt(JsonNode node) throws JsonException;
    
    /**
     * Detects if a JSON node is encrypted in a format supported by this service
     *
     * @param node the JSON node to check
     * @return true if encrypted, false otherwise
     */
    boolean isEncrypted(JsonNode node);
}
