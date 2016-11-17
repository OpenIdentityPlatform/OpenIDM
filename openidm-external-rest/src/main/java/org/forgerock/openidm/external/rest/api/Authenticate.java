package org.forgerock.openidm.external.rest.api;

import javax.validation.constraints.NotNull;

import org.forgerock.api.annotations.Description;

public class Authenticate {
    @NotNull
    @Description("Authentication type (basic or bearer)")
    private String type;
    @Description("User for basic-authentication")
    private String user;
    @Description("Password for basic-authentication")
    private String password;
    @Description("Token for bearer-authentication")
    private String token;

    /**
     * Simple getter to return the auth type to request.
     *
     * @return the auth type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Simple setter to set the auth type to request.
     *
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * Simple getter to return the auth user to request.
     *
     * @return the auth user.
     */
    public String getUser() {
        return this.user;
    }

    /**
     * Simple setter to set the auth user to request.
     *
     */
    public void setUser(final String user) {
        this.user = user;
    }

    /**
     * Simple getter to return the auth password to request.
     *
     * @return the auth password.
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Simple setter to set the auth password to request.
     *
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * Simple getter to return the auth token to request.
     *
     * @return the auth token.
     */
    public String getToken() {
        return this.token;
    }

    /**
     * Simple setter to set the auth token to request.
     *
     */
    public void setToken(final String token) {
        this.token = token;
    }
}
