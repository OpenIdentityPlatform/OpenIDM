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
import org.forgerock.api.annotations.Title;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resource for {@link org.forgerock.openidm.security.impl.KeystoreResourceProvider}'s Certificate Signing responses.
 */
@Title("Certificate Signing Request response")
public class GenerateCsrResponseAction {

    @Description("ID, aka alias, of the Certificate used for signing")
    @JsonProperty("_id")
    private String alias;

    @Description("Signed Certificate")
    private String csr;

    @Description("Public key")
    private PublicKeyResource publicKey;

    /**
     * Returns the alias of the certificate used for signing.
     *
     * @return the alias of the certificate used for signing.
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Returns the Signed certificate.
     *
     * @return The Signed certificate.
     */
    public String getCsr() {
        return csr;
    }

    /**
     * Returns the public key of the certificate.
     *
     * @return The public key of the certificate.
     */
    public PublicKeyResource getPublicKey() {
        return publicKey;
    }
}
