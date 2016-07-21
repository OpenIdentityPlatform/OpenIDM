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

import org.forgerock.http.Client;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.header.GenericHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.util.Uris;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.util.Function;
import org.forgerock.util.Reject;
import org.forgerock.util.encode.Base64url;
import org.forgerock.util.promise.NeverThrowsException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * OpenID Connect {@link SocialProvider} implementation used to
 * perform the OAuth 2.0 flow using the OpenID Connect specification.
 */
public class OpenIDConnectProvider implements SocialProvider {

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

    @Override
    public SocialUser getSocialUser(String code, String redirectUri) throws ResourceException {
        AuthDetails authDetails = getAuthDetails(code, redirectUri);

        final Claims claims = getClaims(authDetails);

        final JsonValue userInfo = sendGetRequest(
                URI.create(config.getUserInfoEndpoint()),
                authDetails.getAccessToken());

        SocialUser.Email email = new SocialUser.Email(userInfo.get("email").asString());

        return new SocialUser(
                // get the first part of their email address for the username
                email.getValue().substring(0, email.getValue().indexOf('@')),
                new SocialUser.Name(
                        userInfo.get("family_name").asString(),
                        userInfo.get("given_name").asString(),
                        userInfo.get("name").asString()),
                new SocialUser.Email[] { email },
                new Claims[] { claims },
                claims.getSubject());
    }

    @Override
    public AuthDetails getAuthDetails(String code, String redirectUri) throws ResourceException {
        AuthDetails authDetails = sendPostRequest(
                URI.create(config.getTokenEndpoint()),
                "application/x-www-form-urlencoded",
                "grant_type=authorization_code"
                        //                        + "&scope=openid%20email%20profile"
                        + "&redirect_uri=" + Uris.formEncodeParameterNameOrValue(redirectUri)
                        + "&code=" + Uris.formDecodeParameterNameOrValue(code)
                        + "&client_id=" + Uris.formEncodeParameterNameOrValue(config.getClientId())
                        + "&client_secret=" +  Uris.formDecodeParameterNameOrValue(config.getClientSecret()),
                AuthDetails.class);

        if (authDetails == null
                || authDetails.getIdToken() == null
                || authDetails.getAccessToken() == null) {
            throw new InternalServerErrorException("Unable to retrieve token");
        }

        return authDetails;
    }

    private Claims getClaims(AuthDetails authDetails) throws BadRequestException {
        final String[] jwt = authDetails.getIdToken().split("\\.");
        if (jwt.length < 2) {
            throw new BadRequestException("Unable to decode id_token JWT");
        }

        try {
            return mapper.readValue(new String(Base64url.decode(jwt[1])), Claims.class);
        } catch (IOException e) {
            throw new BadRequestException("Unable to decode claims", e);
        }
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