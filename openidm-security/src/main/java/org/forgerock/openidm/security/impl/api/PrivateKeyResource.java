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

import java.util.List;

import javax.validation.constraints.NotNull;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;
import org.forgerock.openidm.security.impl.PrivateKeyResourceProvider;

/**
 * Resource for {@link PrivateKeyResourceProvider} PrivateKey responses.
 */
@Title("Private Key Resource")
public class PrivateKeyResource {

    @NotNull
    @Description("A private key in PEM format")
    private String privateKey;

    @NotNull
    @Description("An array of certs each in PEM format")
    private List<String> certs;

    /**
     * Gets the private key in PEM format.
     *
     * @return the private key in PEM format
     */
    public String getPrivateKey() {
        return privateKey;
    }

    /**
     * Gets the list of certs in PEM format.
     *
     * @return the list of certs in PEM format
     */
    public List<String> getCerts() {
        return certs;
    }

}
