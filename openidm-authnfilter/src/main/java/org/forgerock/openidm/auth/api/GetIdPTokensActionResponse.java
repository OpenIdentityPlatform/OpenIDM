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
 * Copyright 2016-2017 ForgeRock AS.
 */

package org.forgerock.openidm.auth.api;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Response to {@link org.forgerock.openidm.auth.AuthenticationService} getAuthToken-action.
 */
@Title("Get IdP Tokens Action Response")
public class GetIdPTokensActionResponse {

    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("access_token")
    private String accessToken;
    
    /**
     * Gets id token.
     *
     * @return Id token
     */
    @NotNull
    @Description("ID token")
    public String getIdToken() {
        return idToken;
    }

    /**
     * Sets id token.
     *
     * @param idToken ID token
     */
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    /**
     * Gets access token.
     *
     * @return Access token
     */
    @NotNull
    @Description("Access token")
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets access token.
     *
     * @param accessToken Access token
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
