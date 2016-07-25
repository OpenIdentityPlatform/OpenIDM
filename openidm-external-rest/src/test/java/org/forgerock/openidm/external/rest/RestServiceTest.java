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

package org.forgerock.openidm.external.rest;

import static org.forgerock.http.protocol.Status.NOT_FOUND;
import static org.forgerock.http.protocol.Status.OK;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openidm.external.rest.RestService.*;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.guava.common.io.ByteStreams;
import org.forgerock.http.Client;
import org.forgerock.http.Handler;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RestServiceTest {

    private static final String URL = "http://example.com";
    private static final String GET = "GET";

    private static final Map<String, Object> JSON_CONTENT_TYPE_HEADER = headers("application/json").build();
    private static final Map<String, Object> JSON_CONTENT_TYPE_WITH_CHARSET_HEADER =
            headers("application/json; charset=utf-8").build();
    private static final Map<String, Object> HTML_CONTENT_TYPE_HEADER = headers("text/html").build();
    private static final Map<String, Object> ATOM_XML_CONTENT_TYPE_HEADER = headers("application/atom+xml").build();
    private static final Map<String, Object> XML_CONTENT_TYPE_HEADER = headers("application/xml").build();
    private static final Map<String, Object> IMAGE_CONTENT_TYPE_HEADER = headers("image/png").build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final ClientRequestParams PARAMS_NONE;
    private static final ClientRequestParams PARAMS_MISSING_METHOD;
    private static final ClientRequestParams PARAMS_MALFORMED_URL;
    private static final ClientRequestParams PARAMS_METHOD;
    private static final ClientRequestParams PARAMS_FORCE_WRAP;
    private static final ClientRequestParams PARAMS_BASE64;
    private static final ClientRequestParams PARAMS_BASIC_AUTH;
    private static final ClientRequestParams PARAMS_BEARER_AUTH;
    private static final ClientRequestParams PARAMS_UNSUPPORTED_AUTH;

    static {
        PARAMS_NONE = new ClientRequestParams();

        PARAMS_MISSING_METHOD = new ClientRequestParams();
        PARAMS_MISSING_METHOD.url = URL;

        PARAMS_MALFORMED_URL = new ClientRequestParams();
        PARAMS_MALFORMED_URL.url = "not a url";
        PARAMS_MALFORMED_URL.method = GET;

        PARAMS_METHOD = new ClientRequestParams();
        PARAMS_METHOD.url = URL;
        PARAMS_METHOD.method = GET;

        PARAMS_FORCE_WRAP = new ClientRequestParams();
        PARAMS_FORCE_WRAP.url = URL;
        PARAMS_FORCE_WRAP.method = GET;
        PARAMS_FORCE_WRAP.forceWrap = true;

        PARAMS_BASE64 = new ClientRequestParams();
        PARAMS_BASE64.url = URL;
        PARAMS_BASE64.method = GET;
        PARAMS_BASE64.body = "dGVzdA==";
        PARAMS_BASE64.contentType = "text/plain";
        PARAMS_BASE64.base64 = true;

        PARAMS_BASIC_AUTH = new ClientRequestParams();
        PARAMS_BASIC_AUTH.url = URL;
        PARAMS_BASIC_AUTH.method = GET;
        PARAMS_BASIC_AUTH.headers = JSON_CONTENT_TYPE_HEADER;
        PARAMS_BASIC_AUTH.body = "{}";
        PARAMS_BASIC_AUTH.auth = json(object(
                field("type", "basic"),
                field("user", "myName"),
                field("password", "myPassword")));

        PARAMS_BEARER_AUTH = new ClientRequestParams();
        PARAMS_BEARER_AUTH.url = URL;
        PARAMS_BEARER_AUTH.method = GET;
        PARAMS_BEARER_AUTH.contentType = "application/json";
        PARAMS_BEARER_AUTH.body = "{}";
        PARAMS_BEARER_AUTH.auth = json(object(
                field("type", "bearer"),
                field("token", "xxx")));

        PARAMS_UNSUPPORTED_AUTH = new ClientRequestParams();
        PARAMS_UNSUPPORTED_AUTH.url = URL;
        PARAMS_UNSUPPORTED_AUTH.method = GET;
        PARAMS_UNSUPPORTED_AUTH.auth = json(object(
                field("type", "unknownType")));
    }

    @DataProvider(name = "testCallActionData")
    public Object[][] testCallActionData() throws Exception {
        return new Object[][]{
                // missing request payload
                {null, OK, JSON_CONTENT_TYPE_HEADER, null, null, BadRequestException.class},
                // missing required URL-field
                {PARAMS_NONE, OK, JSON_CONTENT_TYPE_HEADER, null, null, BadRequestException.class},
                // malformed URL-field
                {PARAMS_MALFORMED_URL, OK, JSON_CONTENT_TYPE_HEADER, null, null, BadRequestException.class},
                // missing required method-field
                {PARAMS_MISSING_METHOD, OK, JSON_CONTENT_TYPE_HEADER, null, null, BadRequestException.class},
                // 404 response
                {PARAMS_METHOD, NOT_FOUND, null, null, null, NotFoundException.class},
                // non-JSON, text response (html)
                {PARAMS_METHOD, OK, HTML_CONTENT_TYPE_HEADER, resourceAsString("/test.html"),
                        resourceAsJsonValue("/test.html.json"), null},
                // non-JSON, text response (xml)
                {PARAMS_METHOD, OK, XML_CONTENT_TYPE_HEADER, resourceAsString("/test.xml"),
                        resourceAsJsonValue("/test.xml.json"), null},
                // non-JSON, text response (atom-xml)
                {PARAMS_METHOD, OK, ATOM_XML_CONTENT_TYPE_HEADER, resourceAsString("/test.atom.xml"),
                        resourceAsJsonValue("/test.atom.xml.json"), null},
                // non-JSON, non-text response (binary)
                {PARAMS_METHOD, OK, IMAGE_CONTENT_TYPE_HEADER, resourceAsBytes("/test.png"),
                        resourceAsJsonValue("/test.png.json"), null},
                // JSON response
                {PARAMS_METHOD, OK, JSON_CONTENT_TYPE_HEADER, resourceAsJsonValue("/test.json"),
                        resourceAsJsonValue("/test.json"), null},
                // JSON response with charset defined in content-type
                {PARAMS_METHOD, OK, JSON_CONTENT_TYPE_WITH_CHARSET_HEADER, resourceAsJsonValue("/test.json"),
                        resourceAsJsonValue("/test.json"), null},
                // JSON response, with forceWrap=true flag
                {PARAMS_FORCE_WRAP, OK, JSON_CONTENT_TYPE_HEADER, resourceAsJsonValue("/test.json"),
                        resourceAsJsonValue("/test.json.json"), null},
                // base64 encoded request body
                {PARAMS_BASE64, OK, JSON_CONTENT_TYPE_HEADER, resourceAsJsonValue("/test.json"),
                        resourceAsJsonValue("/test.json"), null},
                // basic authentication, with content-type in header-map
                {PARAMS_BASIC_AUTH, OK, JSON_CONTENT_TYPE_HEADER, resourceAsJsonValue("/test.json"),
                        resourceAsJsonValue("/test.json"), null},
                // bearer-token authentication, with content-type in content-type-field
                {PARAMS_BEARER_AUTH, OK, JSON_CONTENT_TYPE_HEADER, resourceAsJsonValue("/test.json"),
                        resourceAsJsonValue("/test.json"), null},
                // unsupported authentication type
                {PARAMS_UNSUPPORTED_AUTH, OK, JSON_CONTENT_TYPE_HEADER, null, null, BadRequestException.class},
        };
    }

    @Test(dataProvider = "testCallActionData")
    public void testCallAction(final ClientRequestParams clientRequestParams, final Status clientResStatus,
            final Map<String, Object> clientResHeaders, final Object clientResBody, final JsonValue expectedJsonContent,
            final Class<? extends Throwable> expectedException) throws Exception {
        // given
        final ActionRequest actionRequest = createActionRequest(clientRequestParams);
        final Promise<Response, NeverThrowsException> responsePromise =
                newResultPromise(createClientResponse(clientResStatus, clientResHeaders, clientResBody));

        final RestService restService = new RestService();
        restService.client = createClient(responsePromise);

        // when
        final Promise<ActionResponse, ResourceException> result =
                restService.actionInstance(mock(Context.class), actionRequest);

        // then
        if (expectedException != null) {
            assertThat(result).failedWithException().isInstanceOf(expectedException);
            return;
        }

        // verify that JSON response has expected top-level fields
        final JsonValue actualJsonContent = result.get().getJsonContent();
        Assertions.assertThat(actualJsonContent.keys()).containsOnlyElementsOf(expectedJsonContent.keys());

        // non-JSON responses are converted into a JSON object with "headers" and "body" fields
        if (expectedJsonContent.isDefined(ARG_HEADERS)) {
            final JsonValue expectedHeaders = expectedJsonContent.get(ARG_HEADERS);
            final JsonValue actualHeaders = actualJsonContent.get(ARG_HEADERS);
            for (final String headerName : expectedJsonContent.get(ARG_HEADERS).keys()) {
                Assertions.assertThat(actualHeaders.get(headerName).asList(String.class))
                        .containsAll(expectedHeaders.get(headerName).asList(String.class));
            }
        }
        if (expectedJsonContent.isDefined(ARG_BODY)) {
            Assertions.assertThat(actualJsonContent.get(ARG_BODY).asString())
                    .isEqualTo(expectedJsonContent.get(ARG_BODY).asString());
        }
    }

    private Client createClient(final Promise<Response, NeverThrowsException> promise) {
        final Handler handler = mock(Handler.class);
        final Client client = new Client(handler);
        when(handler.handle(any(Context.class), any(Request.class))).thenReturn(promise);
        return client;
    }

    private Response createClientResponse(final Status status, Map<String, Object> headers, final Object payload) {
        final Response response = new Response(status);
        if (payload != null) {
            response.setEntity(payload);
        }
        if (headers != null) {
            response.getHeaders().addAll(headers);
        }
        return response;
    }

    private ActionRequest createActionRequest(final ClientRequestParams params) {
        final JsonValue content = params == null
                ? null
                : json(
                        object(
                                field(ARG_URL, params.url),
                                field(ARG_METHOD, params.method),
                                field(ARG_HEADERS, params.headers),
                                field(ARG_BODY, params.body),
                                field(ARG_CONTENT_TYPE, params.contentType),
                                field(ARG_BASE_64, params.base64),
                                field(ARG_FORCE_WRAP, params.forceWrap),
                                field(ARG_AUTHENTICATE, params.auth)
                        ));
        final ActionRequest actionRequest = mock(ActionRequest.class);
        when(actionRequest.getAction()).thenReturn("call");
        when(actionRequest.getResourcePath()).thenReturn("");
        when(actionRequest.getContent()).thenReturn(content);
        return actionRequest;
    }

    private static ImmutableMap.Builder<String, Object> headers(final String contentType) {
        final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        if (contentType != null) {
            builder.put(ContentTypeHeader.NAME, contentType);
        }
        return builder;
    }

    private JsonValue resourceAsJsonValue(final String resourcePath) throws Exception {
        try (final InputStream configStream = getClass().getResourceAsStream(resourcePath)) {
            return new JsonValue(OBJECT_MAPPER.readValue(configStream, Map.class));
        }
    }

    private String resourceAsString(final String resourcePath) throws Exception {
        try (final InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            return new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
        }
    }

    private byte[] resourceAsBytes(final String resourcePath) throws Exception {
        try (final InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            return ByteStreams.toByteArray(inputStream);
        }
    }

    private static class ClientRequestParams {
        String url;
        String method;
        Map<String, Object> headers;
        String body;
        String contentType;
        Boolean base64;
        Boolean forceWrap;
        JsonValue auth;
    }
}
