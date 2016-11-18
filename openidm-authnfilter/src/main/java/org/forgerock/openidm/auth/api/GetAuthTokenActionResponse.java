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
 * Response to {@link org.forgerock.openidm.auth.AuthenticationService} getAuthToken-action.
 */
public class GetAuthTokenActionResponse {

    @JsonProperty("auth_token")
    private String authToken;

    /**
     * Gets auth token.
     *
     * @return Auth token
     */
    @NotNull
    @Description("Auth token")
    public String getAuthToken() {
        return authToken;
    }

    /**
     * Sets auth token.
     *
     * @param authToken Auth token
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

}
