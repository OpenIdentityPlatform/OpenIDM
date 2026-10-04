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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openidm.idp.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Arrays;

import org.forgerock.http.Client;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.Test;

/**
 * Tests the nonce check of {@link OAuthHttpClient}.
 */
public class OAuthHttpClientTest {

    private static final String TOKEN_ENDPOINT = "https://idp.example.com/token";
    private static final String USERINFO_ENDPOINT = "https://idp.example.com/userinfo";
    private static final String NONCE = "request-nonce";
    private static final String REDIRECT_URI = "https://openidm.example.com/";

    @Test
    public void testGetProfileIgnoresNonceForOAuth() throws Exception {
        final OAuthHttpClient client = newClient("OAUTH", USERINFO_ENDPOINT, idToken(null));

        final JsonValue profile = client.getProfile(new JwtReconstruction(), "code", NONCE, REDIRECT_URI);

        assertThat(profile.get("sub").asString()).isEqualTo("userinfo-subject");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetProfileRejectsMissingNonceForOpenIdConnect() throws Exception {
        final OAuthHttpClient client = newClient("OPENID_CONNECT", USERINFO_ENDPOINT, idToken(null));

        client.getProfile(new JwtReconstruction(), "code", NONCE, REDIRECT_URI);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetProfileRejectsWrongNonceForOpenIdConnect() throws Exception {
        final OAuthHttpClient client = newClient("OPENID_CONNECT", null, idToken("other-nonce"));

        client.getProfile(new JwtReconstruction(), "code", NONCE, REDIRECT_URI);
    }

    @Test
    public void testGetProfileAcceptsMatchingNonceForOpenIdConnect() throws Exception {
        final OAuthHttpClient client = newClient("OPENID_CONNECT", null, idToken(NONCE));

        final JsonValue profile = client.getProfile(new JwtReconstruction(), "code", NONCE, REDIRECT_URI);

        assertThat(profile.get("sub").asString()).isEqualTo("id-token-subject");
    }

    @Test
    public void testGetAuthTokenIgnoresIdTokenForOAuth() throws Exception {
        final OAuthHttpClient client = newClient("OAUTH", USERINFO_ENDPOINT, idToken(null));

        assertThat(client.getAuthToken(new JwtReconstruction(), "code", NONCE, REDIRECT_URI).getOrThrow())
                .isEqualTo("access-token");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testGetAuthTokenRejectsMissingNonceForOpenIdConnect() throws Exception {
        final OAuthHttpClient client = newClient("OPENID_CONNECT", USERINFO_ENDPOINT, idToken(null));

        client.getAuthToken(new JwtReconstruction(), "code", NONCE, REDIRECT_URI).getOrThrow();
    }

    /**
     * Builds a client whose provider answers the token endpoint with an access token and the given
     * id_token, and the userinfo endpoint with a profile of its own.
     */
    private static OAuthHttpClient newClient(final String type, final String userInfoEndpoint,
            final String idToken) {
        final ProviderConfig config = new ProviderConfig();
        config.setName("test");
        config.setType(type);
        config.setTokenEndpoint(TOKEN_ENDPOINT);
        config.setUserInfoEndpoint(userInfoEndpoint);
        config.setClientId("client-id");
        config.setClientSecret("client-secret");
        config.setScope(Arrays.asList("openid", "profile", "email"));

        final Handler provider = new Handler() {
            @Override
            public Promise<Response, NeverThrowsException> handle(final Context context, final Request request) {
                final Response response = new Response(Status.OK);
                if (TOKEN_ENDPOINT.equals(request.getUri().toString())) {
                    response.setEntity(object(
                            field("access_token", "access-token"),
                            field("token_type", "Bearer"),
                            field("id_token", idToken)));
                } else if (USERINFO_ENDPOINT.equals(request.getUri().toString())) {
                    response.setEntity(object(field("sub", "userinfo-subject")));
                } else {
                    response.setStatus(Status.NOT_FOUND);
                }
                return newResultPromise(response);
            }
        };
        return new OAuthHttpClient(config, new Client(provider));
    }

    /** Returns a signed id_token, with the given nonce claim unless it is {@code null}. */
    private static String idToken(final String nonce) {
        final JwtClaimsSet claims = new JwtBuilderFactory().claims()
                .iss("https://idp.example.com")
                .sub("id-token-subject")
                .build();
        if (nonce != null) {
            claims.setClaim(OAuthHttpClient.NONCE, nonce);
        }
        return new JwtBuilderFactory()
                .jws(new SigningManager().newHmacSigningHandler("0123456789abcdef0123456789abcdef".getBytes()))
                .headers().alg(JwsAlgorithm.HS256).done()
                .claims(claims)
                .build();
    }
}
