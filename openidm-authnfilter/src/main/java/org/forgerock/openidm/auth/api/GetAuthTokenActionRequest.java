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

package org.forgerock.openidm.auth.api;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.Description;

/**
 * Request for {@link org.forgerock.openidm.auth.AuthenticationService} getAuthToken-action.
 */
public class GetAuthTokenActionRequest {

    private String provider;

    private String code;

    private String nonce;

    @JsonProperty("redirect_uri")
    private String redirectUri;

    /**
     * Gets provider name.
     *
     * @return Provider name
     */
    @NotNull
    @Description("Provider name")
    public String getProvider() {
        return provider;
    }

    /**
     * Sets provider name.
     *
     * @param provider Provider name
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * Gets authorization code used to specify grant type.
     *
     * @return Authorization code used to specify grant type
     */
    @NotNull
    @Description("Authorization code used to specify grant type")
    public String getCode() {
        return code;
    }

    /**
     * Sets authorization code used to specify grant type.
     *
     * @param code Authorization code used to specify grant type
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets random value saved by the client intended to compare with fetched claim.
     *
     * @return Random value saved by the client intended to compare with fetched claim
     */
    @NotNull
    @Description("Random value saved by the client intended to compare with fetched claim")
    public String getNonce() {
        return nonce;
    }

    /**
     * Sets random value saved by the client intended to compare with fetched claim.
     *
     * @param nonce Random value saved by the client intended to compare with fetched claim
     */
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    /**
     * Gets URL that the IDP will redirect to with response.
     *
     * @return URL that the IDP will redirect to with response
     */
    @NotNull
    @Description("URL that the IDP will redirect to with response")
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Sets URL that the IDP will redirect to with response.
     *
     * @param redirectUri URL that the IDP will redirect to with response
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
