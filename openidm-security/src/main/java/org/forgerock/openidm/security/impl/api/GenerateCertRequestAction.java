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
import org.forgerock.api.annotations.Format;
import org.forgerock.api.annotations.Title;

/**
 * Resource for {@link org.forgerock.openidm.security.impl.KeystoreResourceProvider}'s Generate Certificate requests.
 */
@Title("Certificate Request")
public class GenerateCertRequestAction {

    // For annotation the value needs to be a constant and a string.
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

    @Description("Size of the key to generate")
    @Default(DEFAULT_KEY_SIZE)
    private int keySize;

    @Description("Domain Name for the certificate")
    private String domainName;

    @Description("Start time of certificate in ISO8601")
    @Format("date-time")
    private String validFrom;

    @Description("End time of certificate in ISO8601")
    @Format("date-time")
    private String validTo;

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

    /**
     * Returns the domain name of the certificate.
     *
     * @return
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Returns the Start time of the certificate in ISO8601.
     *
     * @return the Start time of the certificate in ISO8601.
     */
    public String getValidFrom() {
        return validFrom;
    }

    /**
     * Returns the End time of the certificate in ISO8601.
     *
     * @return the End time of the certificate in ISO8601.
     */
    public String getValidTo() {
        return validTo;
    }
}
