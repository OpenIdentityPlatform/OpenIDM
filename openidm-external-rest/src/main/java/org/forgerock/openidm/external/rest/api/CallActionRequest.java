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
package org.forgerock.openidm.external.rest.api;

import javax.validation.constraints.NotNull;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.forgerock.api.annotations.Description;

/**
 * Configuration of an REST Service request.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CallActionRequest {
    @NotNull
    @Description("The URL to request")
    private String url;

    @NotNull
    @Description("The request HTTP method (e.g., get, post, etc.)")
    private String method;

    @Description("Header key/value pairs to pass to external REST endpoint")
    private Map<String, String> headers;

    @Description("Optional content-type header for request payload (default is 'application/json; charset=utf-8')")
    private String contentType;

    @Description("The HTTP request payload")
    private String body;

    @Description("Indicates that body is a base-64 encoded binary value")
    private boolean base64;

    @Description("The indicator that the response must be wrapped in the headers/body JSON message-format, " +
            "even if the response was JSON and would otherwise have been passed-through unchanged")
    private boolean forceWrap;

    @Description("Configures basic- or bearer-authentication on external endpoint")
    private Authenticate authenticate;

    /**
     * Simple getter to return the URL to request.
     *
     * @return the URL to request.
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * Simple setter to set the URL to request.
     *
     */
    public void setUrl(final String url) {
        this.url = url;
    }

    /**
     * Simple getter to return the request HTTP method (e.g., get, post, etc.).
     *
     * @return the request HTTP method (e.g., get, post, etc.).
     */
    public String getMethod() {
        return this.method;
    }

    /**
     * Simple setter to set the request HTTP method (e.g., get, post, etc.).
     *
     */
    public void setMethod(final String method) {
        this.method = method;
    }

    /**
     * Simple getter to return the HTTP request headers.
     *
     * @return the HTTP request headers.
     */
    public Map<String, String> getHeaders() {
        return this.headers;
    }

    /**
     * Simple setter to set the HTTP request headers.
     *
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Simple getter to return the content type header.
     *
     * @return the content type header.
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Simple setter to set the content type header.
     *
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Simple getter to return the HTTP request payload.
     *
     * @return the HTTP request payload.
     */
    public String getBody() {
        return this.body;
    }

    /**
     * Simple setter to set the HTTP request payload.
     *
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Simple getter to return the indicator that body is base-64
     * encoded, and should be decoded prior to transmission.
     *
     * @return the indicator that body is base-64
     * encoded, and should be decoded prior to transmission.
     */
    public boolean getBase64() {
        return this.base64;
    }

    /**
     * Simple setter to set the indicator that body is base-64
     * encoded, and should be decoded prior to transmission.
     *
     */
    public void setBase64(boolean base64) {
        this.base64 = base64;
    }

    /**
     * Simple getter to return the indicator that the response must be
     * wrapped in the headers/body JSON message-format, even if the
     * response was JSON and would otherwise have been passed-through unchanged.
     *
     * @return the indicator that the response must be
     * wrapped in the headers/body JSON message-format, even if the
     * response was JSON and would otherwise have been passed-through unchanged.
     */
    public boolean getForceWrap() {
        return this.forceWrap;
    }

    /**
     * Simple setter to set the indicator that the response must be
     * wrapped in the headers/body JSON message-format, even if the
     * response was JSON and would otherwise have been passed-through unchanged.
     *
     */
    public void setForceWrap(boolean forceWrap) {
        this.forceWrap = forceWrap;
    }

    /**
     * Simple getter to return the HTTP request authentication.
     *
     * @return the HTTP request authentication.
     */
    public Authenticate getAuthenticate() {
        return this.authenticate;
    }

    /**
     * Simple setter to set the HTTP request authentication.
     *
     */
    public void setAuthenticate(Authenticate authenticate) {
        this.authenticate = authenticate;
    }
}