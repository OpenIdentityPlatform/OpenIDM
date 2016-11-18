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
package org.forgerock.openidm.security.impl.api;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.ReadOnly;
import org.forgerock.api.annotations.Title;

/**
 * Public key resource for Certificates.
 */
@Title("Public Key")
class PublicKeyResource {

    @Description("Algorithm used for the key generation")
    @ReadOnly
    private String algorithm;

    @Description("Format of the key")
    @ReadOnly
    private String format;

    @Description("The public key in PEM format")
    @ReadOnly
    private String encoded;

    /**
     * Algorithm used for the key generation.
     *
     * @return Algorithm used for the key generation
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Format of the key.
     *
     * @return Format of the key.
     */
    public String getFormat() {
        return format;
    }

    /**
     * The key.
     *
     * @return the key.
     */
    public String getEncoded() {
        return encoded;
    }
}
