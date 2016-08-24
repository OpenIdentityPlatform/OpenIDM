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
package org.forgerock.openidm.idp.client;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;


import org.apache.commons.lang3.StringUtils;
import org.forgerock.http.Client;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.header.GenericHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.util.Uris;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.util.Function;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.NeverThrowsException;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OAuth Http Client used to perform the OAuth flow
 * for both OpenID Connect and OAuth 2.0.
 */
public class OAuthHttpClient {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(OAuthHttpClient.class);

    /** Supported OAuth modules **/
    private static final String OAUTH = "OAUTH";
    private static final String OPENID_CONNECT = "OPENID_CONNECT";
    private static final String ID_TOKEN = "id_token";
    private static final String ACCESS_TOKEN ="access_token";

    private final ProviderConfig config;
    private final Client httpClient;

    public OAuthHttpClient(final ProviderConfig config, Client httpClient) {
        this.config = Reject.checkNotNull(config);
        this.httpClient = Reject.checkNotNull(httpClient);
    }

    /**
     * Returns the user profile from the identity provider.
     *
     * @param code authorization code used to specify grant type
     * @param redirectUri url that the idp will redirect to with response
     * @return
     * @throws ResourceException
     */
    public JsonValue getProfile(final String code, final String redirectUri) throws ResourceException {
        // Get the response from the token Endpoint
        final JsonValue tokenEndpointResponse = sendPostRequest(
                URI.create(config.getTokenEndpoint()),
                "application/x-www-form-urlencoded",
                "grant_type=authorization_code"
                        + "&scope=" + Uris.formDecodeParameterNameOrValue(StringUtils.join(config.getScope(), " "))
                        + "&redirect_uri=" + Uris.formEncodeParameterNameOrValue(redirectUri)
                        + "&code=" + Uris.formDecodeParameterNameOrValue(code)
                        + "&client_id=" + Uris.formEncodeParameterNameOrValue(config.getClientId())
                        + "&client_secret=" +  Uris.formDecodeParameterNameOrValue(config.getClientSecret()));

        // Get the user profile from the identity provider using the access token
        return sendGetRequest(
                URI.create(config.getUserInfoEndpoint()),
                tokenEndpointResponse.get(ACCESS_TOKEN).asString());
    }

    /**
     * Returns an auth token used for oauth flow.
     *
     * @param code authorization code used to specify grant type
     * @param redirectUri url that the idp will redirect to with response
     * @return
     * @throws ResourceException
     */
    public String getAuthToken(final String code, final String redirectUri) throws ResourceException {
        final JsonValue response = sendPostRequest(
                URI.create(config.getTokenEndpoint()),
                "application/x-www-form-urlencoded",
                "grant_type=authorization_code"
                        + "&redirect_uri=" + Uris.formEncodeParameterNameOrValue(redirectUri)
                        + "&code=" + Uris.formDecodeParameterNameOrValue(code)
                        + "&client_id=" + Uris.formEncodeParameterNameOrValue(config.getClientId())
                        + "&client_secret=" +  Uris.formDecodeParameterNameOrValue(config.getClientSecret()));
        logger.debug("Response from identity provider is: " + response.toString());
        switch (config.getType()) {
        case OAUTH :
            return getAccessToken(response);
        case OPENID_CONNECT :
            return getJwtToken(response);
        default:
            throw new InternalServerErrorException(
                    "Authentication module of type " + config.getType() + " is not supported.");
        }
    }

    private String getJwtToken(final JsonValue response) throws ResourceException {
        if (response.get(ID_TOKEN).isNull()) {
            throw new NotFoundException("Unable to retrieve token.");
        }
        return response.get(ID_TOKEN).asString();
    }

    private String getAccessToken(final JsonValue response) throws ResourceException {
        if (response.get(ACCESS_TOKEN).isNull()) {
            throw new NotFoundException("Unable to retrieve token.");
        }
        return response.get(ACCESS_TOKEN).asString();
    }

    private JsonValue sendGetRequest(final URI uri, final String access_token) {
        final Request request = new Request()
                .setMethod("GET")
                .setUri(uri);
        request.getHeaders().put(new GenericHeader("Authorization", "Bearer " + access_token));
        try {
            return httpClient
                    .send(request)
                    .then(
                            new Function<Response, JsonValue, NeverThrowsException>() {
                                @Override
                                public JsonValue apply(Response response) {
                                    try {
                                        return json(response.getEntity().getJson());
                                    } catch (IOException e) {
                                        throw new IllegalStateException("Unable to perform request", e);
                                    }
                                }
                            },
                            new Function<NeverThrowsException, JsonValue, NeverThrowsException>() {
                                @Override
                                public JsonValue apply(NeverThrowsException e) {
                                    // return null on Exceptions
                                    return null;
                                }
                            })
                    .getOrThrowUninterruptibly(IdentityServer.getPromiseTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for results", e);
            return json(object());
        }
    }

    private JsonValue sendPostRequest(final URI uri, final String contentType, final Object body) {
        final Request request = new Request()
                .setMethod("POST")
                .setUri(uri);
        if (contentType != null) {
            request.getHeaders().put(ContentTypeHeader.NAME, contentType);
        }
        if (body != null) {
            request.setEntity(body);
        }
        try {
            return httpClient
                    .send(request)
                    .then(
                            new Function<Response, JsonValue, NeverThrowsException>() {
                                @Override
                                public JsonValue apply(Response response) {
                                    try {
                                        return json(response.getEntity().getJson());
                                    } catch (IOException e) {
                                        throw new IllegalStateException("Unable to perform request", e);
                                    }
                                }
                            },
                            new Function<NeverThrowsException, JsonValue, NeverThrowsException>() {
                                @Override
                                public JsonValue apply(NeverThrowsException e) {
                                    // return null on Exceptions
                                    return null;
                                }
                            })
                    .getOrThrowUninterruptibly(IdentityServer.getPromiseTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for results", e);
            return json(object());
        }
    }
}