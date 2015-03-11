/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.crypto;

import java.util.List;

import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.crypto.JsonEncryptor;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonTransformer;
import org.forgerock.json.fluent.JsonValue;

/**
 * Provides encryption and decryption services to OpenIDM components.
 *
 */
public interface CryptoService {

    /**
     * TODO: Description.
     *
     * @param cipher
     *            the cipher with which to encrypt the value.
     * @param alias
     *            the key alias in the key store with which to encrypt the
     *            value.
     * @return TODO.
     * @throws JsonCryptoException
     *             TODO.
     */
    JsonEncryptor getEncryptor(String cipher, String alias) throws JsonCryptoException;

    /**
     * Returns a list of decryption transformers that can decrypt JSON values.
     *
     * @return a list of transformers that can decrypt JSON values.
     */
    List<JsonTransformer> getDecryptionTransformers();

    /**
     * Encrypts a JSON value.
     *
     * @param value
     *            the JSON value to be encrypted.
     * @param cipher
     *            the cipher with which to encrypt the value.
     * @param alias
     *            the key alias in the key store with which to encrypt the
     *            value.
     * @return a copy of the value, encrypted with the specified cipher and key.
     * @throws JsonCryptoException
     *             if and invalid cipher and/or alias is provided.
     * @throws JsonException
     *             if an exception occurred encrypting the value.
     */
    JsonValue encrypt(JsonValue value, String cipher, String alias) throws JsonCryptoException,
            JsonException;

    /**
     * Decrypts a JSON value and all of its children.
     *
     * @param value
     *            the JSON value to be decrypted.
     * @return a deep copy of the value, with all values decrypted.
     * @throws JsonException
     *             if an exception occurred decrypting the value.
     */
    JsonValue decrypt(JsonValue value);

    /**
     * Decrypts a String if in a format supported by this service.
     *
     * @param value
     *            the Stringified value to be decrypted.
     * @return The decrypted structure value, with all values decrypted.
     * @throws JsonException
     *             if an exception occurred decrypting the value.
     */
    JsonValue decrypt(String value);

    /**
     * Decrypts a JSON value and all of its children if necessary. If not,
     * returns the original object.
     *
     * <p>
     * Note that if the argument is null, this return a JsonValue wrapping null.
     *
     * @param value
     *            the JSON value to be decrypted.
     * @return The structure value, decrypted if necessary.
     * @throws JsonException
     *             if an exception occurred decrypting the value.
     */
    JsonValue decryptIfNecessary(JsonValue value);

    /**
     * Decrypts a String if in a format supported by this service and is
     * decryption is necessary. If decryption is unnecessary, it returns the
     * structure value.
     *
     * <p>
     * Note that if the argument is null, this will return a JsonValue wrapping
     * null.
     *
     * @param value
     *            the Stringified value to be decrypted.
     * @return The structure value, decrypted if necessary.
     * @throws JsonException
     *             if an exception occurred decrypting the value.
     */
    JsonValue decryptIfNecessary(String value);

    /**
     * Detects if a JSON value is encrypted in a format supported by this
     * service.
     *
     * @param value
     *            the JSON value to check.
     * @return true if encrypted, false otherwise.
     */
    boolean isEncrypted(JsonValue value);

    /**
     * Detects if a String is encrypted in a format supported by this service.
     *
     * @param value
     *            the JSON value to check.
     * @return true if encrypted, false otherwise.
     */
    boolean isEncrypted(String value);
}
