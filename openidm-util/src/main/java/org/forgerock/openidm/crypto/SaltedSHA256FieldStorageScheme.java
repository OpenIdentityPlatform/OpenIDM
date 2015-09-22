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

import static org.forgerock.openidm.crypto.CryptoConstants.ALGORITHM_SHA_256;

/**
 * This class defines a field storage scheme based on the 256-bit SHA-2 algorithm defined in FIPS 180-2.
 */
public class SaltedSHA256FieldStorageScheme extends FieldStorageSchemeImpl {

    /** 
     * Size of the digest in bytes.
     */
    private static final int SHA256_LENGTH = 256 / 8;

    /**
     * Creates a new instance of this field storage scheme.
     */
    public SaltedSHA256FieldStorageScheme() throws Exception {
        super(SHA256_LENGTH, ALGORITHM_SHA_256);
    }
}

