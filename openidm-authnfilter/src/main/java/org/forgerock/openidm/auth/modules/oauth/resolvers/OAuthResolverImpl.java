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
package org.forgerock.openidm.auth.modules.oauth.resolvers;

import org.forgerock.http.Client;
import org.forgerock.http.header.GenericHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.auth.modules.oauth.exceptions.OAuthVerificationException;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.forgerock.json.JsonValue.json;

/**
 * Implementation for OAuth 2.0 resolvers.
 */
public class OAuthResolverImpl implements OAuthResolver {

    private final Map config;
    private final Client httpClient;
    private final String resolver;
    private final String subjectKey;

    private String subject;

    public OAuthResolverImpl(final String resolver, final Map<String, Object> resolverConfig, final Client httpClient) {
        this.config = resolverConfig;
        this.resolver = resolver;
        this.httpClient = httpClient;
        this.subjectKey = resolverConfig.get(AUTHENTICATION_ID).toString();
    }

    /**
     * Validates the supplied access token against this OAuth Idp.
     *
     * Validation is done by making a request to the user info endpoint
     * to determine if the provided access token is valid on the Idp.
     * If it is valid, a user profile is returned, from which we will extract
     * the subject that we will use to query a known profile later to determine
     * if there is an account associated with that subject.
     *
     * @param accessToken access token to verify.
     * @throws OAuthVerificationException if the accessToken is unable to be verified
     */
    @Override
    public void validateIdentity(final String accessToken) throws OAuthVerificationException {
        final JsonValue response = sendGetRequest(
                URI.create(config.get(USER_INFO_ENDPOINT).toString()), accessToken);
        if (response.get(this.subjectKey) != null) {
            this.subject = response.get(this.subjectKey).asString();
        } else {
            throw new OAuthVerificationException("Unable to validate identity.");
        }
    }

    @Override
    public String getSubject() {
        return this.subject;
    }

    @Override
    public String getName() {
        return this.resolver;
    }

    private JsonValue sendGetRequest(URI uri, String access_token) {
        final Request request = new Request()
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
}
