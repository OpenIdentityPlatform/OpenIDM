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
 * Copyright 2011-2016 ForgeRock AS.
 */
package org.forgerock.openidm.external.rest;

import static org.forgerock.http.handler.HttpClientHandler.OPTION_LOADER;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.util.Utils.closeSilently;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.guava.common.net.MediaType;
import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.apache.async.AsyncHttpClientProvider;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Entity;
import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.spi.Loader;
import org.forgerock.json.JsonValueException;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.util.Function;
import org.forgerock.util.Options;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that acts as a HTTP client proxy to external REST services.
 */
@Component(name = RestService.PID, immediate = true, policy = ConfigurationPolicy.IGNORE)
@Service
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "External REST Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "/external/rest")})
public class RestService implements SingletonResourceProvider {

    static final String PID = "org.forgerock.openidm.external.rest";

    private final static Logger logger = LoggerFactory.getLogger(RestService.class);

    private static final Pattern xmlSubtypePattern = Pattern.compile("^(?:xml|[^+]+\\+xml)$", Pattern.CASE_INSENSITIVE);

    private static final String CALL_ACTION_NAME = "call";

    /**
     * <em>Required</em> {@code _action=call} JSON key for URL to request. The corresponding JSON value is type
     * {@code string}.
     */
    public static final String ARG_URL = "url";

    /**
     * <em>Required</em> {@code _action=call} JSON key for request HTTP method (e.g., get, post, etc.).
     * The corresponding JSON value is type {@code string}.
     */
    public static final String ARG_METHOD = "method";

    /**
     * <em>Optional</em> {@code _action=call} JSON key for HTTP request headers. The corresponding JSON value is an
     * {@code object} of key-value pairs.
     */
    public static final String ARG_HEADERS = "headers";

    /**
     * <em>Optional</em> {@code _action=call} JSON key for HTTP request {@code Content-Type} header, which is only
     * applied to the request if {@link #ARG_HEADERS} does not provide a {@code Content-Type}. The default
     * {@code Content-Type}, when none is provided, is {@code application/json; charset=utf-8}.
     * The corresponding JSON value is type {@code string}.
     */
    public static final String ARG_CONTENT_TYPE = "contentType";

    /**
     * <em>Optional</em> {@code _action=call} JSON key for HTTP request payload. The corresponding JSON value is type
     * {@code string}.
     */
    public static final String ARG_BODY = "body";

    /**
     * <em>Optional</em> {@code _action=call} JSON key for HTTP request, indicating that {@link #ARG_BODY} is base-64
     * encoded, and should be decoded prior to transmission. The corresponding JSON value is type {@code boolean}.
     */
    public static final String ARG_BASE_64 = "base64";

    /**
     * <em>Optional</em> {@code _action=call} JSON key for HTTP request, indicating that the response must be
     * wrapped in the headers/body JSON message-format, even if the response was JSON and would otherwise have
     * been passed-through unchanged.
     */
    public static final String ARG_FORCE_WRAP = "forceWrap";

    /**
     * <em>Optional</em> {@code _action=call} JSON key for configuring authentication to use with HTTP request. To
     * use basic-authentication, provide a value with format,
     * <pre>
     *     {
     *         "type": "basic",
     *         "user": "...",
     *         "password": "..."
     *     }
     * </pre>
     * To use a bearer-token,
     * <pre>
     *     {
     *         "type": "bearer",
     *         "token": "..."
     *     }
     * </pre>
     */
    public static final String ARG_AUTHENTICATE = "authenticate";

    private static final String JSON_UTF_8_CONTENT_TYPE = MediaType.JSON_UTF_8.toString();

    private HttpClientHandler httpClientHandler;

    @VisibleForTesting
    Client client;

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        httpClientHandler = newHttpClientHandler();
        client = new Client(httpClientHandler);
        logger.info("External REST connectivity started.");
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        if (httpClientHandler != null) {
            try {
                httpClientHandler.close();
            } catch (IOException e) {
                logger.error("An error occurred while closing the default HTTP client handler", e);
            }
        }
        logger.info("External REST connectivity stopped.");
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        return new NotSupportedException("Patch operations are not supported").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        return new NotSupportedException("Read operations are not supported").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        return new NotSupportedException("Update operations are not supported").asPromise();
    }

    /**
     * Supports {@code _action=call} action, to make a HTTP call to an external server. The expected request payload
     * is a JSON object containing the following fields,
     * <ul>
     * <li>{@link #ARG_URL url} (required)</li>
     * <li>{@link #ARG_METHOD method} (required)</li>
     * <li>{@link #ARG_HEADERS headers}</li>
     * <li>{@link #ARG_CONTENT_TYPE contentType}</li>
     * <li>{@link #ARG_BODY body}</li>
     * <li>{@link #ARG_BASE_64 base64}</li>
     * <li>{@link #ARG_FORCE_WRAP forceWrap}</li>
     * </ul>
     * <p>
     * If the response is JSON, then the raw JSON response will be returned, otherwise the following JSON object
     * will be returned for "text" content,
     * <pre>
     *     {
     *          "headers": { "Content-Type": ["..."] },
     *          "body": "..."
     *     }
     * </pre>
     * Non-text content will be base-64 encoded in the "body" field and returned as follows,
     * <pre>
     *     {
     *          "headers": { "Content-Type": ["..."] },
     *          "body": "...",
     *          "base64": true
     *     }
     * </pre>
     * Read the "Content-Type" header from the "headers" field, for guidance on how to handle the "body" content.
     *
     * @param context Context
     * @param actionRequest Action request
     * @return Action response
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(
            final Context context, final ActionRequest actionRequest) {

        logger.debug("Action invoked on {} with {}", actionRequest.getAction(), actionRequest);

        if (!CALL_ACTION_NAME.equalsIgnoreCase(actionRequest.getAction())) {
            return new BadRequestException("Invalid action call on "
                    + actionRequest.getResourcePath() + "/" + actionRequest.getAction()
                    + " : action name not supported: " + actionRequest.getAction()).asPromise();
        }

        final JsonValue content = actionRequest.getContent();
        if (content == null || !content.isMap() || content.asMap().isEmpty()) {
            return new BadRequestException("Invalid action call on "
                    + actionRequest.getResourcePath() + "/" + actionRequest.getAction()
                    + " : missing post body to define what to invoke.").asPromise();
        }

        final boolean forceWrap;
        final Request request;
        try {
            forceWrap = content.get(ARG_FORCE_WRAP).defaultTo(false).asBoolean();
            request = new Request()
                    .setMethod(content.get(ARG_METHOD).required().asString())
                    .setUri(content.get(ARG_URL).required().asString());

            final Map<String, Object> headerMap = content.get(ARG_HEADERS).defaultTo(Collections.emptyMap()).asMap();
            request.getHeaders().putAll(headerMap);

            final String body = content.get(ARG_BODY).asString();
            if (body != null) {
                if (!containsHeader(headerMap, ContentTypeHeader.NAME)) {
                    // content-type NOT provided in ARG_HEADERS, so use value in ARG_CONTENT_TYPE, with JSON fallback
                    final String contentType = content.get(ARG_CONTENT_TYPE)
                            .defaultTo(JSON_UTF_8_CONTENT_TYPE)
                            .asString();
                    request.getHeaders().put(ContentTypeHeader.NAME, contentType);
                }
                request.setEntity(content.get(ARG_BASE_64).defaultTo(false).asBoolean()
                        ? Base64.decode(body)
                        : body);
            }

            final JsonValue auth = content.get(ARG_AUTHENTICATE);
            if (!auth.isNull()) {
                final String type = auth.get("type").defaultTo("basic").asString();
                if ("basic".equalsIgnoreCase(type)) {
                    final String user = auth.get("user").required().asString();
                    final String password = auth.get("password").required().asString();
                    final String credentials = user + ":" + password;
                    request.getHeaders().put("Authorization", "Basic " + Base64.encode(credentials.getBytes()));
                    logger.debug("Using basic authentication for {} secret supplied: {}", user, !password.isEmpty());
                } else if ("bearer".equalsIgnoreCase(type)) {
                    final String token = auth.get("token").required().asString();
                    request.getHeaders().put("Authorization", "Bearer " + token);
                    logger.debug("Using bearer authentication");
                } else {
                    return new BadRequestException("Invalid auth type \"" + type + "\" on "
                            + actionRequest.getResourcePath() + "/" + actionRequest.getAction()).asPromise();
                }
            }
        } catch (URISyntaxException e) {
            return new BadRequestException("Invalid action call on "
                    + actionRequest.getResourcePath() + "/" + actionRequest.getAction()
                    + " : invalid 'url' JSON field: " + actionRequest.getContent().get(ARG_URL).toString()).asPromise();
        } catch (JsonValueException e) {
            return new BadRequestException("Invalid action call on "
                    + actionRequest.getResourcePath() + "/" + actionRequest.getAction()
                    + " : invalid or missing JSON field: " + e.getMessage()).asPromise();
        }

        return client.send(request).then(
                new Function<Response, ActionResponse, ResourceException>() {
                    @Override
                    public ActionResponse apply(final Response response) throws ResourceException {
                        try {
                            if (!response.getStatus().isSuccessful()) {
                                throw newResourceException(response.getStatus().getCode(), "HTTP request failed");
                            }

                            final Header contentTypeHeader = response.getHeaders().get(ContentTypeHeader.NAME);
                            final MediaType mediaType = contentTypeHeader != null
                                    ? MediaType.parse(contentTypeHeader.getFirstValue()).withoutParameters()
                                    : MediaType.JSON_UTF_8;

                            final Entity entity = response.getEntity();
                            try {
                                final JsonValue content;
                                if (!forceWrap && MediaType.JSON_UTF_8.is(mediaType)) {
                                    // pass-through JSON response unchanged
                                    content = json(entity.getJson());
                                } else {
                                    // wrap response body/headers in JSON
                                    final Map<String, List<String>> responseHeaders = response.getHeaders()
                                            .copyAsMultiMapOfStrings();
                                    content = json(object());
                                    content.put(ARG_HEADERS, responseHeaders);
                                    if (mediaType.is(MediaType.ANY_TEXT_TYPE)
                                            || MediaType.JSON_UTF_8.is(mediaType)
                                            || xmlSubtypePattern.matcher(mediaType.subtype()).matches()) {
                                        // text, xml, or json (with forceWrap)
                                        content.put(ARG_BODY, entity.getString());
                                    } else {
                                        // base64 encoded binary
                                        content.put(ARG_BODY, Base64.encode(entity.getBytes()));
                                        content.put(ARG_BASE_64, true);
                                    }
                                }
                                return Responses.newActionResponse(content);
                            } catch (IOException e) {
                                throw new InternalServerErrorException(e.getMessage(), e);
                            }
                        } finally {
                            closeSilently(response);
                        }
                    }
                },
                org.forgerock.http.protocol.Responses.<ActionResponse, ResourceException>noopExceptionFunction());
    }

    /**
     * Case-insensitive search for presence of a header.
     *
     * @param headerMap Map to search
     * @param headerName Header name
     * @return {@code true} if found and {@code false} otherwise
     */
    private boolean containsHeader(final Map<String, Object> headerMap, final String headerName) {
        for (final String name : headerMap.keySet()) {
            if (name.trim().equalsIgnoreCase(headerName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds an {@link AsyncHttpClientProvider} instance, which must be closed on shutdown/de-activation.
     *
     * @return {@link AsyncHttpClientProvider} instance
     */
    private HttpClientHandler newHttpClientHandler() {
        try {
            return new HttpClientHandler(
                    Options.defaultOptions()
                            .set(OPTION_LOADER, new Loader() {
                                @Override
                                public <S> S load(Class<S> service, Options options) {
                                    return service.cast(new AsyncHttpClientProvider());
                                }
                            }));
        } catch (HttpApplicationException e) {
            throw new RuntimeException("Error while building HTTP Client Handler", e);
        }
    }
}
