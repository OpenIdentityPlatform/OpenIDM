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

import javax.validation.constraints.NotNull;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.ReadOnly;
import org.forgerock.api.annotations.Title;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resource for {@link org.forgerock.openidm.security.impl.CertificateResourceProvider} and
 * {@link org.forgerock.openidm.security.impl.KeystoreResourceProvider} Certificate responses.
 */
@Title("Certificate Resource")
public class CertificateResource {

    @Description("ID, aka alias, of the certificate")
    @NotNull
    @JsonProperty("_id")
    private String alias;

    @NotNull
    @Description("Certificate in PEM format")
    private String cert;

    @ReadOnly
    @Description("Type of Certificate, for example 'jceks'")
    private String type;

    @ReadOnly
    @Description("Public Key")
    private PublicKeyResource publicKey;

    @ReadOnly
    @Description("Start time of certificate in ms")
    private int notBefore;

    @ReadOnly
    @Description("End time of certificate in ms")
    private int notAfter;

    @ReadOnly
    @Description("Issuer of the certificate")
    private Issuer issuer;

    /**
     * Gets the certificate in PEM format.
     *
     * @return the certificate in PEM format
     */
    public String getCert() {
        return cert;
    }

    /**
     * Type of the certificate.
     *
     * @return Type of the certificate.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the public key.
     *
     * @return the public key.
     */
    public PublicKeyResource getPublicKey() {
        return publicKey;
    }

    /**
     * Start time of certificate validity as a millisecond offset from unix epoch.
     *
     * @return Start time of certificate validity as a millisecond offset from unix epoch.
     */
    public int getNotBefore() {
        return notBefore;
    }

    /**
     * End time of certificate validity as a millisecond offset from unix epoch.
     *
     * @return End time of certificate validity as a millisecond offset from unix epoch.
     */
    public int getNotAfter() {
        return notAfter;
    }

    /**
     * Returns the issuer of the certificate.
     *
     * @return the issuer of the certificate.
     */
    public Issuer getIssuer() {
        return issuer;
    }

}
