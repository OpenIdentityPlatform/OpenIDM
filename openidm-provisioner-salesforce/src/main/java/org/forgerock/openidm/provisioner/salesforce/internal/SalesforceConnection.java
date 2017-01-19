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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openidm.provisioner.salesforce.internal;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.util.Utils.closeSilently;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.client.utils.URIBuilder;
import org.forgerock.http.Client;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.header.GenericHeader;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Responses;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sforce.ws.ConnectorConfig;

/**
 * Class to represent a Salesforce Connection
 * http://wiki.restlet.org/docs_2.1/13-restlet/28-restlet/392-restlet.html
 * http://boards.developerforce.com/t5/REST-API-Integration/Having-trouble-getting-Access-Token-with-username-password/td-p/278305
 *
 */
public class SalesforceConnection {

    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    static final boolean REAUTH = true;
    /**
     * Setup logging for the {@link SalesforceConnection}.
     */
    private static final Logger logger = LoggerFactory.getLogger(SalesforceConnection.class);
    private static final int ACCESS_TOKEN_TIMEOUT_MILLIS = 300000;

    public static final ObjectMapper mapper = JsonUtil.build();

    static {
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE).disable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).disable(
                SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /*
     * public static final ObjectMapper mapper = new ObjectMapper();
     *
     * static {
     * mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
     * mapper.getDeserializationConfig().set(
     * DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false); }
     */

    public static final String INSTANCE_URL = "instance_url";

    public static final String ACCESS_TOKEN = "access_token";

    public static final String CLIENT_ID = "client_id";

    public static final String CLIENT_SECRET = "client_secret";

    public static final String ERROR = "error";

    public static final String ERROR_DESC = "error_description";

    public static final String GRANT_TYPE = "grant_type";

    public static final String PASSWORD = "password";

    public static final String REFRESH_TOKEN = "refresh_token";

    public static final String USERNAME = "username";

    public static final String SIGNATURE = "signature";

    private SalesforceConfiguration configuration;

    private  AtomicReference<OAuthUser> authentication;

    private Client authenticationClient;
    private Client provisioningClient;
    private HttpClientHandler httpClientHandler;

    @SuppressWarnings("unchecked")
    public SalesforceConnection(SalesforceConfiguration configuration, HttpClientHandler httpClientHandler) throws ResourceException {
        configuration.validate();
        this.configuration = configuration;
        this.httpClientHandler = httpClientHandler;
        Handler handlerWithAuthzHeader = Handlers.chainOf(httpClientHandler, new Filter() {
            @Override
            public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
                request.getHeaders().put(getAuthorizationHeader());
                return next.handle(context, request);
            }
        });
        provisioningClient = new Client(handlerWithAuthzHeader);
        authenticationClient = new Client((httpClientHandler));
        authentication = new AtomicReference<>();
    }


    public Promise<Void, ResourceException> refreshAccessToken(final ConnectorConfig config) {
        // First check
        final String sessionId = config.getSessionId();
        if (authenticationRequired(sessionId)) {
            // Sync the threads
            synchronized (this) {
                // Second check
                if (authenticationRequired(sessionId)) {
                    return authenticate().then(new Function<Void, Void, ResourceException>() {
                        @Override
                        public Void apply(Void value) throws ResourceException {
                            if (authentication.get().getAccessToken().equals(sessionId)) {
                                logger.warn("Expired token was successfully extended");
                            } else {
                                logger.info("Expired token was successfully refreshed");
                            }
                            return null;
                        }
                    });
                } else {
                    logger.info("Expired token was refreshed by other thread");
                    return Promises.newResultPromise(null);
                }
            }
        }
        config.setSessionId(authentication.get().getAccessToken());
        return Promises.newResultPromise(null);
    }

    public Promise<Void, ResourceException> refreshAccessToken(Header requestAuthorizationHeader) {
        // First check
        if (authenticationRequired(requestAuthorizationHeader)) {
            // Sync the threads
            synchronized (this) {
                // Second check
                if (authenticationRequired(requestAuthorizationHeader)) {
                    return authenticate().then(new Function<Void, Void, ResourceException>() {
                        @Override
                        public Void apply(Void value) throws ResourceException {
                            logger.info("Expired token was successfully extended or refreshed");
                            return null;
                        }
                    });
                } else {
                    logger.info("Expired token was refreshed by other thread");
                }
            }
        }
        return Promises.newResultPromise(null);
    }

    private boolean authenticationRequired(Header requestAuthorizationHeader) {
        //the filter which puts the authorization header in the invocation always sets a single value. See ctor of this class
        final String requestAccessToken = requestAuthorizationHeader.getValues().get(0);
        //return true if the current authz token matches that of the failed request, thus indicating that the token needs to be refreshed.
        return authenticationRequired(requestAccessToken);
    }

    private boolean authenticationRequired(String requestAccessToken) {
        //return true if the current authz token matches that of the failed request, and is more than 300 seconds old.
        // This indicates that the token needs to be refreshed.
        final OAuthUser oAuthUser = authentication.get();
        return oAuthUser.getAuthorization().equals(requestAccessToken) &&
                (System.currentTimeMillis() - oAuthUser.issued.getTime() > ACCESS_TOKEN_TIMEOUT_MILLIS);
    }

    private Promise<Void, ResourceException> authenticate() {
        final Request request = new Request();
        request.setMethod("POST");
        try {
            request.setUri(configuration.getLoginUrl());
        } catch (URISyntaxException e) {
            return ResourceException.newResourceException(ResourceException.INTERNAL_ERROR, e.getMessage()).asPromise();
        }
        request.getHeaders().put(ContentTypeHeader.NAME, "application/x-www-form-urlencoded");
        request.setEntity(configuration.getFormRepresentationOfAuthenticationState());
        return authenticationClient.send(request).then(
            new Function<Response, Void, ResourceException>() {
                @Override
                public Void apply(Response response) throws ResourceException {
                    try {
                        if (!response.getStatus().isSuccessful()) {
                            throw ResourceException.newResourceException(response.getStatus().getCode(), response.getStatus().toString());
                        }
                        final JsonValue responseJson = json(response.getEntity().getJson());
                        authentication.set(marshalOAuthUserFromResponse(responseJson));
                    } catch (IOException e) {
                        throw ResourceException.newResourceException(ResourceException.INTERNAL_ERROR, e.getMessage());
                    } finally {
                        closeSilently(response);
                    }
                    return null;
                }
            },
            //the send method 'throws' NeverThrowsException, so we don't have to handle the exception case here.
            Responses.<Void, ResourceException>noopExceptionFunction());
    }

    private void revokeAccessToken() {
        if (authentication.get() != null) {
            try {
                logger.debug("Attempt to revoke AccessToken!");
                final URIBuilder uriBuilder = new URIBuilder(authentication.get().getBaseReference() + "/services/oauth2/revoke");
                uriBuilder.addParameter("token", authentication.get().getAccessToken());

                final Request revokeRequest = new Request();
                revokeRequest.setUri(uriBuilder.build());
                revokeRequest.setMethod("GET");
                authenticationClient.send(revokeRequest).then(
                    new Function<Response, Void, NeverThrowsException>() {
                        @Override
                        public Void apply(Response response) throws NeverThrowsException {
                            try {
                                if (!response.getStatus().isSuccessful()) {
                                    logger.error("Failed to revoke token - status:{} response:{} ", response.getStatus(),
                                            response.getEntity() != null ? response.getEntity().getString() : "");
                                } else {
                                    logger.info("Success revoking token - status:{} response:{} ",
                                            response.getStatus(), response.getEntity() != null ?
                                            response.getEntity().getString() : "");
                                }
                            } catch (IOException e) {
                                logger.warn("Exception caught revoking access token: " + e, e);
                            } finally {
                                closeSilently(response);
                            }
                            return null;
                        }
                    });
            } catch (URISyntaxException e) {
                logger.warn("Exception caught revoking access token: " + e, e);
            }
        }
    }

    public ConnectorConfig getConnectorConfig(String api) throws ResourceException {
        if (StringUtils.isBlank(api)) {
            throw new IllegalArgumentException();
        }
        ConnectorConfig config = new ConnectorConfig();
        config.setConnectionTimeout(configuration.getConnectTimeout());
        if (null != configuration.getProxyHost()) {
            config.setProxy(configuration.getProxyHost(), configuration.getProxyPort());
        }
        config.setRestEndpoint(getOAuthUser().getBaseReference() +
                "/services/" + api + "/" + Double.toString(configuration.getVersion()));
        config.setServiceEndpoint(config.getRestEndpoint());
        config.setSessionId(getOAuthUser().getAccessToken());

        return config;
    }

    /**
     * This method will be called from a thread created in the SalesforceProvisionerService ctor immediately after
     * the SalesforceConnection is created. Thus this method will be called upon IDM restart, following successful
     * configuration of the salesforce connector. Sending requests requires the authentication context to be initialized.
     * Note that authenticate should not be called directly, as this test method is called as part of the getStatus
     * invocation on the salesforce connector, and we don't want to trigger specious reauths.
     * @throws ResourceException if the Salesforce context could not be successfully consumed.
     */
    public void test() throws ResourceException {
        initializeAuthenticationForTest().then(
            new Function<Void, Void, ResourceException>() {
                @Override
                public Void apply(Void aVoid) throws ResourceException {
                    final Request chfRequest = new Request();
                    chfRequest.setMethod("GET");
                    try {
                        chfRequest.setUri(getBaseInvocationUrl());
                    } catch (URISyntaxException e) {
                        throw ResourceException.newResourceException(ResourceException.INTERNAL_ERROR, e.getMessage());
                    }
                    test(chfRequest, REAUTH);
                    return null;
                }
            },
            new Function<ResourceException, Void, ResourceException>() {
                @Override
                public Void apply (ResourceException e) throws ResourceException {
                    throw e;
                }
            });
    }

    private Promise<Void, ResourceException> initializeAuthenticationForTest() throws ResourceException {
        if (authentication.get() == null) {
            return authenticate();
        }
        return Promises.newResultPromise(null);
    }

    private void test(Request chfRequest, boolean reauth) throws ResourceException {
        dispatchAuthorizedRequest(chfRequest, reauth);
    }

    public String getVersion() {
        return "v" + Double.toString(configuration.getVersion());
    }

    public Double getAPIVersion() {
        return configuration.getVersion();
    }

    public void dispose() {
        revokeAccessToken();
        closeSilently(httpClientHandler);
    }

    private OAuthUser marshalOAuthUserFromResponse(JsonValue responseJson) throws ForbiddenException {
        /*
         * <pre> { "id":
         * "https://login.salesforce.com/id/00Di0000000HKwAEAW/005i0000000gmLlAAI"
         * , "issued_at":"1359559707796", "scope":"id api refresh_token",
         * "instance_url":"https://na15.salesforce.com", "refresh_token":
         * "5Aep861z80Xevi74eWEi7DVt9fvzKs9C8G_8UytO14SZpBDo8GoacTiRHWl6BmAi0FeIrGk3rO0BQ=="
         * , "signature":"Ip1zeYMCTPojfmayzF10J9fNOhVr5EoaTdoc3b0RpcI=",
         * "access_token":
         * "00Di0000000HKwA!AQcAQN6vMrx8xrKKabrmPgw13uImvCBM2.ss5YurN_VhR4tbVCFgiY4zYtglmw5Yp0ZX3HD2etE5eQ5aSNtWLqjtWo7GQRd7"
         * } { "error":"invalid_grant",
         * "error_description":"expired authorization code" } </pre>
         */
        if (responseJson.isNotNull()) {
            if (responseJson.get(ERROR).isNotNull()) {
                throw new ForbiddenException(responseJson.get(ERROR_DESC).asString());
            }

            String id = null;
            if (responseJson.get("id").isString()) {
                id = responseJson.get("id").asString();
                logger.debug("Id = " + id);
            }

            Date issued = null;
            if (responseJson.get("issued_at").isString()) {
                issued = new Date(Long.parseLong(responseJson.get("issued_at").asString()));
                logger.debug("Issued at = " + issued);
            }

            String scope = null;
            if (responseJson.get("scope").isString()) {
                scope = responseJson.get("scope").asString();
                logger.debug("Scope at = " + scope);
            }

            String instanceUrl = configuration.getInstanceUrl();
            if (instanceUrl ==  null && responseJson.get(INSTANCE_URL).isString()) {
                instanceUrl = responseJson.get(INSTANCE_URL).asString();
                logger.debug("InstanceUrl = " + instanceUrl);
            }

            String refreshToken = null;
            if (responseJson.get(REFRESH_TOKEN).isString()) {
                instanceUrl = responseJson.get(REFRESH_TOKEN).asString();
                logger.debug("RefreshToken = " + refreshToken);
            }

            String signature = null;
            if (responseJson.get(SIGNATURE).isString()) {
                signature = responseJson.get(SIGNATURE).asString();
                logger.debug("Signature = " + signature);
            }

            String accessToken = null;
            if (responseJson.get(ACCESS_TOKEN).isString()) {
                accessToken = responseJson.get(ACCESS_TOKEN).asString();
                logger.debug("AccessToken = " + accessToken);
            }

            return new OAuthUser(id, issued, scope, instanceUrl, refreshToken, signature,
                    accessToken);
        }
        return null;
    }

    public OAuthUser getOAuthUser() throws ResourceException {
        if (null == authentication.get()) {
            synchronized (this) {
                if (null == authentication.get()) {
                    authenticate();
                }
            }
        }
        return authentication.get();
    }

    String getBaseInvocationUrl() {
        final String instanceUrl = configuration.getInstanceUrl();
        StringBuilder stringBuilder = new StringBuilder(instanceUrl);
        if (!instanceUrl.endsWith("/")) {
            stringBuilder.append("/");
        }
        stringBuilder.append("services/data/").append(getVersion());
        return stringBuilder.toString();
    }

    String getSObjectInvocationUrl(String type, String id) {
        StringBuilder sb = new StringBuilder(getBaseInvocationUrl());
        sb.append("/sobjects");
        if (type != null) {
            sb.append('/').append(type);
            if (id != null) {
                sb.append('/').append(id);
            }
        }
        return sb.toString();
    }

    String getGenericObjectInvocationUrl(String type, String id) {
        StringBuilder sb = new StringBuilder(getBaseInvocationUrl());
        if (type != null) {
            sb.append('/').append(type);
            if (id != null) {
                sb.append('/').append(id);
            }
        }
        return sb.toString();
    }

    String getQueryInvocationUrl(String id) {
        StringBuilder sb = new StringBuilder(getBaseInvocationUrl());
        if (id != null) {
            sb.append('/').append(id);
        }
        return sb.toString();
    }

    Header getAuthorizationHeader() {
        return new GenericHeader(AUTHORIZATION_HEADER_KEY, authentication.get().getAuthorization());
    }

    Promise<JsonValue, ResourceException> dispatchAuthorizedRequest(Request request)  {
        return dispatchAuthorizedRequest(request, REAUTH);
    }

    Promise<JsonValue, ResourceException> dispatchAuthorizedRequest(final Request request, final boolean reauth) {
        return provisioningClient.send(request).then(
            new Function<Response, JsonValue, ResourceException>() {
                @Override
                public JsonValue apply(Response response) throws ResourceException {
                    try {
                        if (!response.getStatus().isSuccessful()) {
                            if (reauth && (response.getStatus().getCode() == 401)) {
                                refreshAccessToken(request.getHeaders().get(SalesforceConnection.AUTHORIZATION_HEADER_KEY))
                                    .thenOnResult(new ResultHandler<Void>() {
                                        @Override
                                        public void handleResult(Void result) {
                                            dispatchAuthorizedRequest(request, !REAUTH);
                                        }
                                    });
                            } else {
                                String errorString;
                                try {
                                    errorString = response.getEntity().getString();
                                } catch (IOException e) {
                                    throw ResourceException.newResourceException(ResourceException.INTERNAL_ERROR, e.getMessage());
                                }
                                throw ResourceException.newResourceException(response.getStatus().getCode(), errorString);
                            }
                        }
                        //for update cases, salesforce returns 204 with no content in the entity, even though update succeeded.
                        // Return an empty object in this case
                        if (response.getEntity().isDecodedContentEmpty()) {
                            return json(new HashMap<>());
                        }
                        try {
                            return json(response.getEntity().getJson());
                        } catch (IOException e) {
                            throw ResourceException.newResourceException(ResourceException.INTERNAL_ERROR, e.getMessage());
                        }
                    } finally {
                        closeSilently(response);
                    }
                }
            },
            //the send method 'throws' NeverThrowsException, so we don't have to handle the exception case here.
            Responses.<JsonValue, ResourceException>noopExceptionFunction());
    }

    public static class OAuthUser {

        /**
         * The id.
         */
        private final String id;

        /**
         * The issued_at.
         */
        private final Date issued;

        /**
         * The scope.
         */
        private final String scope;

        /**
         * The instance URL.
         */
        private final String instanceUrl;

        /**
         * The refresh token.
         */
        private final String refreshToken;

        /**
         * The toke signature.
         */
        private final String signature;

        /**
         * The access token.
         */
        private final String accessToken;

        public OAuthUser(String id, Date issued, String scope, String instanceUrl,
                String refreshToken, String signature, String accessToken) {
            this.id = id;
            this.issued = issued;
            this.instanceUrl = instanceUrl;
            this.scope = scope;
            this.refreshToken = refreshToken;
            this.signature = signature;
            this.accessToken = accessToken;
        }

        public String getBaseReference() {
            return instanceUrl;
        }

        public String getAuthorization() {
            return "OAuth " + accessToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}
