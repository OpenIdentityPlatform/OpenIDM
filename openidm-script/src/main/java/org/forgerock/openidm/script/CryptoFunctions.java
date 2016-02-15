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
package org.forgerock.openidm.script;

import org.forgerock.json.JsonValue;
import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.script.scope.Function;
import org.forgerock.script.scope.FunctionFactory;
import org.forgerock.script.scope.Parameter;

import java.util.List;
import java.util.Map;

/**
 * Exposes crypto-related functions that can be provided to a script to invoke.
 */
public class CryptoFunctions {

    private CryptoFunctions() {
        // prevent instantiation
    }

    /**
     * hash(any value, string algorithm)
     *
     * @param cryptoService the CryptoService
     * @return a Function that computes a hash
     */
    public static Function<JsonValue> newHashFunction(final CryptoService cryptoService) {
        return new Function<JsonValue>() {

            static final long serialVersionUID = 1L;

            public JsonValue call(Parameter scope, Function<?> callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                if (arguments.length == 2) {
                    JsonValue value = null;
                    String algorithm = null;
                    if (arguments[0] instanceof Map
                            || arguments[0] instanceof List
                            || arguments[0] instanceof String
                            || arguments[0] instanceof Number
                            || arguments[0] instanceof Boolean) {
                        value = new JsonValue(arguments[0]);
                    } else if (arguments[0] instanceof JsonValue) {
                        value = (JsonValue) arguments[0];
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage( "hash", arguments));
                    }
                    if (arguments[1] instanceof String) {
                        algorithm = (String) arguments[1];
                    } else if (arguments[1] == null) {
                        algorithm = ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_HASHING_ALGORITHM;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage( "hash", arguments));
                    }

                    try {
                        return cryptoService.hash(value, algorithm);
                    } catch (JsonCryptoException e) {
                        throw new InternalServerErrorException(e.getMessage(), e);
                    }
                } else {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage( "hash", arguments));
                }
            }
        };
    }

    /**
     * encrypt(any value, string cipher, string alias)
     *
     * @param cryptoService the CryptoService
     * @return a Function that encrypts the input
     */
    public static Function<JsonValue> newEncryptFunction(final CryptoService cryptoService) {
        return new Function<JsonValue>() {

            static final long serialVersionUID = 1L;

            public JsonValue call(Parameter scope, Function<?> callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                if (arguments.length == 3) {
                    JsonValue value = null;
                    String cipher = null;
                    String alias = null;
                    if (arguments[0] instanceof Map
                            || arguments[0] instanceof List
                            || arguments[0] instanceof String
                            || arguments[0] instanceof Number
                            || arguments[0] instanceof Boolean) {
                        value = new JsonValue(arguments[0]);
                    } else if (arguments[0] instanceof JsonValue) {
                        value = (JsonValue) arguments[0];
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("encrypt", arguments));
                    }
                    if (arguments[1] instanceof String) {
                        cipher = (String) arguments[1];
                    } else if (arguments[1] == null) {
                        cipher = ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER;
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("encrypt", arguments));
                    }

                    if (arguments[2] instanceof String) {
                        alias = (String) arguments[2];
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("encrypt", arguments));
                    }
                    try {
                        return cryptoService.encrypt(value, cipher, alias);
                    } catch (JsonCryptoException e) {
                        throw new InternalServerErrorException(e.getMessage(), e);
                    }
                } else {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("encrypt", arguments));
                }
            }
        };
    }

    /**
     * decrypt(any value)
     *
     * @param cryptoService the CryptoService
     * @return a Function that decrypts the input
     */
    public static Function<JsonValue> newDecryptFunction(final CryptoService cryptoService) {
        return new Function<JsonValue>() {

            static final long serialVersionUID = 1L;

            public JsonValue call(Parameter scope, Function<?> callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                if (arguments.length == 1
                        && (arguments[0] instanceof Map || arguments[0] instanceof JsonValue)) {
                    return cryptoService.decrypt(arguments[0] instanceof JsonValue
                            ? (JsonValue) arguments[0]
                            : new JsonValue(arguments[0]));
                } else {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("decrypt", arguments));
                }
            }
        };
    }

    /**
     * isEncrypted(any value)
     *
     * @return a Function that tests whether the input is encrypted
     */
    public static Function<Boolean> newIsEncryptedFunction() {
        return new Function<Boolean>() {

            static final long serialVersionUID = 1L;

            public Boolean call(Parameter scope, Function<?> callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                if (arguments == null || arguments.length == 0) {
                    return false;
                } else if (arguments.length == 1) {
                    return JsonCrypto.isJsonCrypto(arguments[0] instanceof JsonValue
                            ? (JsonValue) arguments[0]
                            : new JsonValue(arguments[0]));
                } else {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("isEncrypted", arguments));
                }
            }
        };
    }

    /**
     * isHashed(any value)
     *
     * @param cryptoService the CryptoService
     * @return a Function that tests whether the input is hashed
     */
    public static Function<Boolean> newIsHashedFunction(final CryptoService cryptoService) {
        return new Function<Boolean>() {

            static final long serialVersionUID = 1L;

            public Boolean call(Parameter scope, Function<?> callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                if (arguments == null || arguments.length == 0) {
                    return false;
                } else if (arguments.length == 1) {
                    return cryptoService.isHashed(arguments[0] instanceof JsonValue
                            ? (JsonValue) arguments[0]
                            : new JsonValue(arguments[0]));
                } else {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("isHashed", arguments));
                }
            }
        };
    }

    /**
     * matches(String plaintext, any value)
     *
     * @param cryptoService the CryptoService
     * @return
     */
    public static Function<Boolean> newMatchesFunction(final CryptoService cryptoService) {
        return new Function<Boolean>() {

            static final long serialVersionUID = 1L;

            public Boolean call(Parameter scope, Function<?> callback, Object... arguments)
                    throws ResourceException, NoSuchMethodException {
                if (arguments == null || arguments.length == 0 || arguments.length == 1) {
                    return false;
                } else if (arguments.length == 2) {
                    try {
                        return cryptoService.matches(arguments[0].toString(),
                                arguments[1] instanceof JsonValue
                                        ? (JsonValue) arguments[1]
                                        : new JsonValue(arguments[1]));
                    } catch (JsonCryptoException e) {
                        throw new InternalServerErrorException(e.getMessage(), e);
                    }
                } else {
                    throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage("matches", arguments));
                }
            }
        };
    }
}
