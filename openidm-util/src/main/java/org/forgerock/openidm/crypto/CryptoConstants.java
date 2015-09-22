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
 * Portions copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.crypto;

/**
 * Constants for use with the Cryptography Service
 */
public class CryptoConstants {
    
    /**
     * A salted hash encryption storage type.
     */
    public static final String STORAGE_TYPE_HASH = "salted-hash";
    
    /**
     * The name of the message digest algorithm that should be used to generate MD5 hashes.
     */
    public static final String ALGORITHM_MD5 = "MD5";

    /**
     * The name of the message digest algorithm that should be used to generate SHA-1 hashes.
     */
    public static final String ALGORITHM_SHA_1 = "SHA-1";

    /**
     * The name of the message digest algorithm that should be used to generate 256-bit SHA-2 hashes.
     */
    public static final String ALGORITHM_SHA_256 = "SHA-256";

    /**
     * The name of the message digest algorithm that should be used to generate 384-bit SHA-2 hashes.
     */
    public static final String ALGORITHM_SHA_384 = "SHA-384";

    /**
     * The name of the message digest algorithm that should be used to generate 512-bit SHA-2 hashes.
     */
    public static final String ALGORITHM_SHA_512 = "SHA-512";
}
