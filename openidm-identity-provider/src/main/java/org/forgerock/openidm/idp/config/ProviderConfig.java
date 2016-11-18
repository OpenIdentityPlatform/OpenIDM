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

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.Description;

/**
 * Configuration of an Identity Provider.
 */
@Description("Authentication-provider model")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderConfig {

    public final static String CLIENT_SECRET = "client_secret";

    private String name;

    private String type;

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

    private List<String> scope;

    private String authenticationId;

    private List<SingleMapping> propertyMap;

    private Map<String, Object> schema;

    /** Default {@code true}, can disable identity provider by setting to {@code false} explicitly in config. */
    @JsonProperty
    private boolean enabled = true;

    /**
     * Gets unique provider name.
     *
     * @return Unique provider name
     */
    @NotNull
    @Description("Unique provider name")
    public String getName() {
        return name;
    }

    /**
     * Sets unique provider name.
     *
     * @param name Unique provider name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets authentication type.
     *
     * @return Authentication type
     */
    @NotNull
    @Description("Authentication type (e.g., OPENID_CONNECT, OAUTH)")
    public String getType() {
        return type;
    }

    /**
     * Sets authentication type.
     *
     * @param type Authentication type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets icon HTML.
     *
     * @return Icon HTML
     */
    @NotNull
    @Description("Icon HTML")
    public String getIcon() {
        return icon;
    }

    /**
     * Sets icon HTML.
     *
     * @param icon Icon HTML
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * Gets endpoint for authentication and authorization of a user.
     *
     * @return Endpoint for authentication and authorization of a user
     */
    @NotNull
    @Description("Endpoint for authentication and authorization of a user")
    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    /**
     * Sets endpoint for authentication and authorization of a user.
     *
     * @param authorizationEndpoint Endpoint for authentication and authorization of a user
     */
    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    /**
     * Gets endpoint for requesting access and ID tokens.
     *
     * @return Endpoint for requesting access and ID tokens
     */
    @NotNull
    @Description("Endpoint for requesting access and ID tokens")
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    /**
     * Sets endpoint for requesting access and ID tokens.
     *
     * @param tokenEndpoint Endpoint for requesting access and ID tokens
     */
    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    /**
     * Gets endpoint for requesting user information.
     *
     * @return Endpoint for requesting user information
     */
    @NotNull
    @Description("Endpoint for requesting user information")
    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    /**
     * Sets endpoint for requesting user information.
     *
     * @param userinfoEndpoint Endpoint for requesting user information
     */
    public void setUserInfoEndpoint(String userinfoEndpoint) {
        this.userInfoEndpoint = userinfoEndpoint;
    }

    /**
     * Gets well-known endpoint for OpenID Connect configuration key-value pairs.
     *
     * @return Well-known endpoint for OpenID Connect configuration key-value pairs
     */
    @Description("Well-known endpoint for OpenID Connect configuration key-value pairs")
    public String getWellKnown() {
        return wellKnown;
    }

    /**
     * Sets well-known endpoint for OpenID Connect configuration key-value pairs.
     *
     * @param wellKnown Well-known endpoint for OpenID Connect configuration key-value pairs
     */
    public void setWellKnown(String wellKnown) {
        this.wellKnown = wellKnown;
    }

    /**
     * Gets OAuth client ID.
     *
     * @return OAuth client ID
     */
    @NotNull
    @Description("OAuth client ID")
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets OAuth client ID.
     *
     * @param clientId OAuth client ID
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Gets OAuth client secret.
     *
     * @return OAuth client secret
     */
    @Description("OAuth client secret")
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Sets OAuth client secret.
     *
     * @param clientSecret OAuth client secret
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * Gets OAuth scopes being requested.
     *
     * @return OAuth scopes being requested
     */
    @NotNull
    @Description("OAuth scopes being requested")
    public List<String> getScope() {
        return scope;
    }

    /**
     * Sets OAuth scopes being requested.
     *
     * @param scope OAuth scopes being requested
     */
    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    /**
     * Gets property that maps to unique user identifier.
     *
     * @return Property that maps to unique user identifier
     */
    @NotNull
    @Description("Property that maps to unique user identifier")
    public String getAuthenticationId() {
        return authenticationId;
    }

    /**
     * Sets property that maps to unique user identifier.
     *
     * @param authenticationId Property that maps to unique user identifier
     */
    public void setAuthenticationId(String authenticationId) {
        this.authenticationId = authenticationId;
    }

    /**
     * Gets JSON Schema for generating form fields.
     *
     * @return JSON Schema for generating form fields
     */
    @NotNull
    @Description("JSON Schema for generating form fields")
    public Map<String, Object> getSchema() {
        return schema;
    }

    /**
     * Sets JSON Schema for generating form fields.
     *
     * @param schema JSON Schema for generating form fields
     */
    public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }

    /**
     * Gets property mapping from provider fields to OpenIDM fields.
     *
     * @return Property mapping from provider fields to OpenIDM fields
     */
    @NotNull
    @Description("Property mapping from provider fields to OpenIDM fields")
    public List<SingleMapping> getPropertyMap() {
        return propertyMap;
    }

    /**
     * Sets property mapping from provider fields to OpenIDM fields.
     *
     * @param propertyMap Property mapping from provider fields to OpenIDM fields
     */
    public void setPropertyMap(List<SingleMapping> propertyMap) {
        this.propertyMap = propertyMap;
    }

    /**
     * Gets enabled-state.
     *
     * @return {@code true} when enabled and {@code false} otherwise
     */
    @NotNull
    @Description("true when enabled and false otherwise")
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets enabled-state.
     *
     * @param enabled {@code true} when enabled and {@code false} otherwise
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
