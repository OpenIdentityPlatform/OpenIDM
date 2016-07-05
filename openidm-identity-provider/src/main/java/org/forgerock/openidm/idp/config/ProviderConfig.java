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
package org.forgerock.openidm.idp.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration of a Provider using the OIDC identity layer.
 */
public class ProviderConfig {

    public final static String CLIENT_SECRET = "client_secret";

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("icon")
    private String icon;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("userinfo_endpoint")
    private String userInfoEndpoint;

    @JsonProperty("well-known")
    private String wellKnown;

    /** Default true, can disable
     * identity provider by setting to false
     * explicitly in config */
    @JsonProperty("enabled")
    private boolean enabled = true;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getIcon() {
        return icon;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    public String getWellKnown() {
        return wellKnown;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
