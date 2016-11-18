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

import static org.forgerock.openidm.security.impl.SecurityResourceProvider.DEFAULT_ALGORITHM;
import static org.forgerock.openidm.security.impl.SecurityResourceProvider.DEFAULT_SIGNATURE_ALGORITHM;

import javax.validation.constraints.NotNull;

import org.forgerock.api.annotations.Default;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Resource for {@link org.forgerock.openidm.security.impl.KeystoreResourceProvider}'s Certificate Signing requests.
 */
@Title("Certificate Signing Request Action")
public class GenerateCsrRequestAction extends Issuer {

    // For annotation the value needs to be a constant string.
    private static final String DEFAULT_KEY_SIZE =
            org.forgerock.openidm.security.impl.SecurityResourceProvider.DEFAULT_KEY_SIZE + "";

    @Description("Alias of the CSR")
    @NotNull
    private String alias;

    @Description("Algorithm of the CSR")
    @Default(DEFAULT_ALGORITHM)
    private String algorithm;

    @Description("Signature Algorithm")
    @Default(DEFAULT_SIGNATURE_ALGORITHM)
    private String signatureAlgorithm;

    @Description("Size of the key, in bits")
    @Default(DEFAULT_KEY_SIZE)
    private int keySize;

    /**
     * Returns the alias of the certificate.
     *
     * @return the alias of the certificate.
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Returns the algorithm used to create the certificate.
     *
     * @return the algorithm used to create the certificate.
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the algorithm used to create the signature.
     *
     * @return the algorithm used to create the signature.
     */
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    /**
     * Returns the key size in bits.
     *
     * @return the key size in bits.
     */
    public int getKeySize() {
        return keySize;
    }
}
