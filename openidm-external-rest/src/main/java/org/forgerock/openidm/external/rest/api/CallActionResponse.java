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

import java.util.Map;

import org.forgerock.api.annotations.Description;

public class CallActionResponse {
    @Description("Header key/value pairs from external REST endpoint")
    private Map<String, String> headers;

    @Description("The HTTP response payload")
    private String body;

    @Description("Indicates that body is a base-64 encoded binary value")
    private boolean base64;

    /**
     * Simple getter to return the HTTP response headers.
     *
     * @return the HTTP response headers.
     */
    public Map<String, String> getHeaders() {
        return this.headers;
    }

    /**
     * Simple setter to set the HTTP response headers.
     *
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Simple getter to return the HTTP response payload.
     *
     * @return the HTTP response payload.
     */
    public String getBody() {
        return this.body;
    }

    /**
     * Simple setter to set the HTTP response payload.
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
}