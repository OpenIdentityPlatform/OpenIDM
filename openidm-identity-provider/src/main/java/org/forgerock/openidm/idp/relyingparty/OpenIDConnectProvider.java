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
package org.forgerock.openidm.idp.relyingparty;

import static org.forgerock.json.JsonValue.json;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.http.Client;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.header.GenericHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.util.Uris;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.util.Function;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.NeverThrowsException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * OpenID Connect implementation used to
 * perform the OAuth 2.0 flow using the OpenID Connect specification.
 */
public class OpenIDConnectProvider {

    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    private final ProviderConfig config;
    private final Client httpClient;

    public OpenIDConnectProvider(ProviderConfig config, Client httpClient) {
        Reject.ifNull(config);
        Reject.ifNull(httpClient);
        this.config = config;
        this.httpClient = httpClient;
    }

    /**
     * Returns the user profile from the Idp.
     *
     * @param code authorization code used to specify grant type
     * @param redirectUri url that the idp will redirect to with response
     * @return
     * @throws ResourceException
     */
    public JsonValue getProfile(String code, String redirectUri) throws ResourceException {
        // Get the response from the token Endpoint
        Map tokenEndpointResponse = sendPostRequest(
                URI.create(config.getTokenEndpoint()),
                "application/x-www-form-urlencoded",
                "grant_type=authorization_code"
                        + "&scope=" + Uris.formDecodeParameterNameOrValue(StringUtils.join(config.getScope(), " "))
                        + "&redirect_uri=" + Uris.formEncodeParameterNameOrValue(redirectUri)
                        + "&code=" + Uris.formDecodeParameterNameOrValue(code)
                        + "&client_id=" + Uris.formEncodeParameterNameOrValue(config.getClientId())
                        + "&client_secret=" +  Uris.formDecodeParameterNameOrValue(config.getClientSecret()),
                Map.class);

        // Get the user profile from the identity provider using the access token
        return sendGetRequest(
                URI.create(config.getUserInfoEndpoint()),
                tokenEndpointResponse.get("access_token").toString());
    }

    /**
     * Returns a JWT ID Token used for OAuth2.0 flow when
     * the provider type is openid_connect.
     *
     * @param code authorization code used to specify grant type
     * @param redirectUri url that the idp will redirect to with response
     * @return
     * @throws ResourceException
     */
    public String getIdToken(String code, String redirectUri) throws ResourceException {
        Map response = sendPostRequest(
                URI.create(config.getTokenEndpoint()),
                "application/x-www-form-urlencoded",
                "grant_type=authorization_code"
                        + "&scope=" + Uris.formDecodeParameterNameOrValue(StringUtils.join(config.getScope(), " "))
                        + "&redirect_uri=" + Uris.formEncodeParameterNameOrValue(redirectUri)
                        + "&code=" + Uris.formDecodeParameterNameOrValue(code)
                        + "&client_id=" + Uris.formEncodeParameterNameOrValue(config.getClientId())
                        + "&client_secret=" +  Uris.formDecodeParameterNameOrValue(config.getClientSecret()),
                Map.class);

        if (response.get("id_token") == null) {
            throw new InternalServerErrorException("Unable to retrieve token");
        }
        return response.get("id_token").toString();
    }

    private JsonValue sendGetRequest(URI uri, String access_token) {
        Request request = new Request()
                .setMethod("GET")
                .setUri(uri);
        request.getHeaders().put(new GenericHeader("Authorization", "Bearer " + access_token));
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
                .getOrThrowUninterruptibly();
    }

    private <T> T sendPostRequest(URI uri, String contentType, Object body, final Class<T> clazz) {
        Request request = new Request()
                .setMethod("POST")
                .setUri(uri);
        if (contentType != null) {
            request.getHeaders().put(ContentTypeHeader.NAME, contentType);
        }
        if (body != null) {
            request.setEntity(body);
        }
        return httpClient
                .send(request)
                .then(
                        new Function<Response, T, NeverThrowsException>() {
                            @Override
                            public T apply(Response response) {
                                try {
                                    return mapper.convertValue(response.getEntity().getJson(), clazz);
                                } catch (IOException e) {
                                    throw new IllegalStateException("Unable to perform request", e);
                                }
                            }
                        },
                        new Function<NeverThrowsException, T, NeverThrowsException>() {
                            @Override
                            public T apply(NeverThrowsException e) {
                                // return null on Exceptions
                                return null;
                            }
                        })
                .getOrThrowUninterruptibly();
    }
}