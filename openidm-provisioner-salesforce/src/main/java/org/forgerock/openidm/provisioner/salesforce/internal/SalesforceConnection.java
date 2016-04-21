/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.provisioner.salesforce.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.util.JsonUtil;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Uniform;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.data.Header;
import org.restlet.engine.header.ChallengeWriter;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;
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
 * @author Laszlo Hordos
 */
public class SalesforceConnection extends ClientResource {

    /**
     * Setup logging for the {@link SalesforceConnection}.
     */
    private static final Logger logger = LoggerFactory.getLogger(SalesforceConnection.class);

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

    /**
     * Requests that the origin server accepts the entity enclosed in the
     * request as a new subordinate of the resource identified by the request
     * URI.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2068/rfc2068">HTTP PATCH
     *      method</a>
     */
    public static final Method PATCH = new Method("PATCH");

    public static final String INSTANCE_URL = "instance_url";

    public static final String ACCESS_TOKEN = "access_token";

    public static final String CLIENT_ID = "client_id";

    public static final String CLIENT_SECRET = "client_secret";

    public static final String ERROR = "error";

    public static final String ERROR_DESC = "error_description";

    public static final String ERROR_URI = "error_uri";

    public static final String EXPIRES_IN = "expires_in";

    public static final String GRANT_TYPE = "grant_type";

    public static final String PASSWORD = "password";

    public static final String AUTHORIZATION_CODE = "authorization_code";

    public static final String REFRESH_TOKEN = "refresh_token";

    public static final String SCOPE = "scope";

    public static final String CODE = "code";

    public static final String REDIRECT_URI = "redirect_uri";

    public static final String STATE = "state";

    public static final String USERNAME = "username";

    public static final String SIGNATURE = "signature";

    private SalesforceConfiguration configuration;

    private OAuthUser authentication = null;

    public SalesforceConnection(final ClientResource resource) {
        super(resource);
    }

    @SuppressWarnings("unchecked")
    public SalesforceConnection(SalesforceConfiguration configuration0) throws ResourceException {
        super(new Context(), configuration0.getLoginUrl());
        configuration0.validate();
        this.configuration = configuration0;

        /*
         * Time in milliseconds between two checks for idle and expired
         * connections. The check happens only if this property is set to a
         * value greater than 0.
         */
        getContext().getParameters().add("idleCheckInterval",
                Long.toString(configuration.getIdleCheckInterval()));

        /*
         * The time in ms beyond which idle connections are eligible for
         * reaping.
         */
        getContext().getParameters().add("idleTimeout",
                Long.toString(configuration.getIdleTimeout()));
        getContext().getParameters().add("maxConnectionsPerHost",
                Integer.toString(configuration.getMaxConnectionsPerHost()));
        getContext().getParameters().add("maxTotalConnections",
                Integer.toString(configuration.getMaxTotalConnections()));
        /*
         * The socket timeout value. A timeout of zero is interpreted as an
         * infinite timeout.
         */
        getContext().getParameters().add("socketTimeout",
                Integer.toString(configuration.getSocketTimeout()));
        /*
         * The minimum idle time, in milliseconds, for connections to be closed
         * when stopping the connector
         */
        getContext().getParameters().add("stopIdleTimeout",
                Integer.toString(configuration.getStopIdleTimeout()));

        /*
         * if the protocol will use Nagle's algorithm
         */
        getContext().getParameters().add("tcpNoDelay",
                Boolean.toString(configuration.getTcpNoDelay()));

        /*
         * The connection timeout in milliseconds. The default value is 0,
         * meaning an infinite timeout
         */
        getContext().getParameters().add("socketConnectTimeoutMs",
                Integer.toString(configuration.getConnectTimeout()));

        Client client = new Client(Protocol.HTTPS);
        client.setContext(getContext());

        setNext(client);

        // Accept: application/json
        List<Preference<MediaType>> acceptedMediaTypes = new ArrayList<Preference<MediaType>>(1);
        acceptedMediaTypes.add(new Preference(MediaType.APPLICATION_JSON));
        getClientInfo().setAcceptedMediaTypes(acceptedMediaTypes);

        // authenticate();
    }

    public boolean refreshAccessToken(final ConnectorConfig config) throws ResourceException {
        // First check
        final String expired = config.getSessionId();
        if (authentication.getAccessToken().equals(expired)) {
            // Sync the threads
            synchronized (this) {
                // Second check
                if (authentication.getAccessToken().equals(expired)
                        && (System.currentTimeMillis() - authentication.issued.getTime() > 300000)) {
                    authenticate();
                    if (authentication.getAccessToken().equals(expired)) {
                        logger.warn("Expired token was successfully extended");
                    } else {
                        logger.info("Expired token was successfully refreshed");
                    }
                } else {
                    logger.info("Expired token was refreshed by other thread");
                }
            }
        }
        config.setSessionId(authentication.getAccessToken());
        return true;
    }

    public boolean refreshAccessToken(final Request request) throws ResourceException {
        // First check
        final String expired = fetchAccessToken(request);
        if (authentication.getAuthorization().equals(expired)) {
            // Sync the threads
            synchronized (this) {
                // Second check
                if (authentication.getAuthorization().equals(expired)
                        && (System.currentTimeMillis() - authentication.issued.getTime() > 300000)) {
                    authenticate();
                    if (authentication.getAuthorization().equals(expired)) {
                        logger.warn("Expired token was successfully extended");
                    } else {
                        logger.info("Expired token was successfully refreshed");
                    }
                } else {
                    logger.info("Expired token was refreshed by other thread");
                }
            }
        }
        resetAccessToken(request);
        return true;
    }

    @SuppressWarnings("unchecked")
    private String fetchAccessToken(final Request result) {
        Series<Header> additionalHeaders =
                (Series<Header>) result.getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        if (additionalHeaders != null) {
            return additionalHeaders.getFirst(HeaderConstants.HEADER_AUTHORIZATION).getValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void resetAccessToken(final Request result) {
        Series<Header> additionalHeaders =
                (Series<Header>) result.getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        if (additionalHeaders == null) {
            additionalHeaders = new Series<Header>(Header.class);
            result.getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, additionalHeaders);
        }
        additionalHeaders.set(HeaderConstants.HEADER_AUTHORIZATION, authentication
                .getAuthorization());
    }

    private void authenticate() throws org.forgerock.json.resource.ResourceException {
        Representation body = null;
        final ClientResource cr = super.getChild(new Reference(""));
        try {
            body = cr.post(configuration.getAuthenticationForm().getWebRepresentation());
            if (body instanceof EmptyRepresentation == false) {
                authentication = createJson(new JacksonRepresentation<Map>(body, Map.class));
            } else {
                throw new InternalServerErrorException("OAuth2 request response is empty!");
            }
        } catch (ResourceException e) {
            throw getResourceException(cr);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        } finally {
            if (null != body)
                body.release();
            if (null != cr) {
                cr.release();
            }
        }
    }

    private void revokeAccessToken() {
        // REVOKE
        if (null != authentication) {
            logger.debug("Attempt to revoke AccessToken!");
            // final ClientResource cr = super.getChild(new
            // Reference("./revoke"));
            final ClientResource cr =
                    new ClientResource(getContext(), new Reference(authentication
                            .getBaseReference(), "./services/oauth2/revoke").toUri());
            try {
                cr.setFollowingRedirects(true);
                cr.getReference().addQueryParameter("token", authentication.getAccessToken());
                cr.setMethod(Method.GET);
                cr.handle();

                if (cr.getResponse().getStatus().isError()) {
                    // TODO is it expired?
                    logger.error("Failed to revoke token - status:{} response:{} ", cr
                            .getResponse().getStatus(), null != cr.getResponse().getEntity() ? cr
                            .getResponse().getEntityAsText() : "");
                } else {
                    logger.info("Succeed to revoke token - status:{} response:{} ", cr
                            .getResponse().getStatus(), null != cr.getResponse().getEntity() ? cr
                            .getResponse().getEntityAsText() : "");
                }
            } finally {
                if (null != cr) {
                    cr.release();
                }
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

        config.setRestEndpoint(new Reference(getOAuthUser().getBaseReference(), new Reference(
                "services/" + api + "/" + Double.toString(configuration.getVersion())))
                .getTargetRef().toString());
        config.setServiceEndpoint(config.getRestEndpoint());
        config.setSessionId(getOAuthUser().getAccessToken());

        return config;
    }

    @SuppressWarnings("unchecked")
    public ResourceException getResourceException(ClientResource rc) {
        ResourceException jre =
                ResourceException.getException(rc.getResponse().getStatus().getCode(), rc
                        .getResponse().getStatus().getDescription());
        Representation error = rc.getResponse().getEntity();
        if (null != error && error instanceof EmptyRepresentation == false) {
            try {
                Object res = new JacksonRepresentation<Object>(error, Object.class).getObject();
                if (res instanceof Map) {
                    jre.setDetail(new JsonValue(res));
                } else if (res instanceof List) {
                    JsonValue details = new JsonValue(new HashMap<String, Object>());
                    StringBuilder msg = new StringBuilder();
                    for (Object o : (List<Object>) res) {
                        if (o instanceof Map) {
                            if (null != ((Map) o).get("errorCode")) {
                                details.put((String) ((Map) o).get("errorCode"), o);
                            } else {
                                details.put(UUID.randomUUID().toString(), o);
                            }
                            if (null != ((Map) o).get("message")) {
                                msg.append(((Map) o).get("message")).append("\n");
                            }
                        }
                    }
                    details.put("message", msg.toString());
                    logger.error("REST API error:\n {} \n ", mapper
                            .writerWithDefaultPrettyPrinter().writeValueAsString(res));
                    jre.setDetail(details);
                }
            } catch (Exception e) {
                logger.debug("Failed to parse the error response", e);
                /* Ignore the non JSON exceptions */
            }
        }
        logger.error("Remote REST error: \n{}\n", jre.toJsonValue().toString());
        return jre;
    }

    public void test() throws ResourceException {
        ClientResource resource = getChild("services/data/" + getVersion());
        try {
            test(resource, true);
        } finally {
            if (null != resource) {
                resource.release();
            }
        }
    }

    public String getVersion() {
        return "v" + Double.toString(configuration.getVersion());
    }

    public Double getAPIVersion() {
        return configuration.getVersion();
    }

    private void test(ClientResource resource, boolean tryReauth) throws ResourceException {
        try {
            resource.handle();
        } catch (Exception e) {
            throw new InternalServerErrorException(e);
        }
        final Response response = resource.getResponse();
        if (response.getStatus().isError()) {

            ResourceException sfe = getResourceException(resource);

            if (tryReauth && Status.CLIENT_ERROR_UNAUTHORIZED.equals(response.getStatus())) {
                // Re authenticate
                if (refreshAccessToken(resource.getRequest())) {
                    try {
                        resource.handle();
                    } catch (Exception e) {
                        throw new InternalServerErrorException(e);
                    }
                    test(resource, false);
                } else {
                    throw ResourceException.getException(401, "AccessToken can not be renewed");
                }
            } else {
                throw sfe;
            }
            // throw new ResourceException(response.getStatus());
        } else {
            if (null != response.getEntity()) {
                response.getEntityAsText();
            }
        }
    }

    public void dispose() {
        revokeAccessToken();
        try {
            Uniform next = getNext();
            if (next instanceof Client) {
                ((Client) next).stop();
            }
        } catch (Exception e) {
            logger.warn("Failed to stop Client", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ClientResource getChild(Reference relativeRef)
            throws org.restlet.resource.ResourceException {
        ClientResource result = null;

        if ((relativeRef != null) && relativeRef.isRelative()) {
            try {
                OAuthUser oAuthUser = getOAuthUser();
                result = new SalesforceConnection(this);

                result.setReference(new Reference(oAuthUser.getBaseReference(), relativeRef)
                        .getTargetRef());
            } catch (ResourceException e) {
                throw new org.restlet.resource.ResourceException(
                        Status.SERVER_ERROR_SERVICE_UNAVAILABLE, e);
            }
            // -------------------------------------
            // Add user-defined extension headers
            // -------------------------------------
            resetAccessToken(result.getRequest());

            // Series<Header> additionalHeaders =
            // (Series<Header>) result.getRequest().getAttributes().get(
            // HeaderConstants.ATTRIBUTE_HEADERS);
            // additionalHeaders.add("X-PrettyPrint", "1");
        } else {
            doError(Status.CLIENT_ERROR_BAD_REQUEST, "The child URI is not relative.");
            throw new org.restlet.resource.ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
        }
        return result;
    }

    /**
     * Converts successful JSON token body responses to OAuthUser.
     *
     * @param body
     *            Representation containing a successful JSON body element.
     * @return OAuthUser object containing accessToken, refreshToken and
     *         expiration time.
     */
    public OAuthUser createJson(JacksonRepresentation<Map> body) throws IOException,
            ResourceException {
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

        java.util.logging.Logger log = Context.getCurrentLogger();

        Map answer = body != null ? body.getObject() : null;

        if (null != answer) {

            if (answer.containsKey(ERROR)) {
                throw new ForbiddenException((String) answer.get(ERROR_DESC))
                        .setDetail(new JsonValue(answer.get(ERROR)));
            }

            String id = null;
            if (answer.get("id") instanceof String) {
                id = (String) answer.get("id");
                log.fine("Id = " + id);
            }

            Date issued = null;
            if (answer.get("issued_at") instanceof String) {
                issued = new Date(Long.parseLong((String) answer.get("issued_at")));
                log.fine("Issued at = " + issued);
            }

            String scope = null;
            if (answer.get("scope") instanceof String) {
                scope = (String) answer.get("scope");
                log.fine("Scope at = " + scope);
            }

            String instanceUrl = configuration.getInstanceUrl();
            if (null == instanceUrl && answer.get(INSTANCE_URL) instanceof String) {
                instanceUrl = (String) answer.get(INSTANCE_URL);
                log.fine("InstanceUrl = " + instanceUrl);
            }

            String refreshToken = null;
            if (answer.get(REFRESH_TOKEN) instanceof String) {
                instanceUrl = (String) answer.get(REFRESH_TOKEN);
                log.fine("RefreshToken = " + refreshToken);
            }

            String signature = null;
            if (answer.get(SIGNATURE) instanceof String) {
                signature = (String) answer.get(SIGNATURE);
                log.fine("Signature = " + signature);
            }

            String accessToken = null;
            if (answer.get(ACCESS_TOKEN) instanceof String) {
                accessToken = (String) answer.get(ACCESS_TOKEN);
                log.fine("AccessToken = " + accessToken);
            }

            return new OAuthUser(id, issued, scope, instanceUrl, refreshToken, signature,
                    accessToken);
        }
        return null;
    }

    public OAuthUser getOAuthUser() throws ResourceException {
        if (null == authentication) {
            synchronized (this) {
                if (null == authentication) {
                    authenticate();
                }
            }
        }
        return authentication;
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
        private final Reference instanceUrl;

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

        private final String authorization;

        public OAuthUser(String id, Date issued, String scope, String instanceUrl,
                String refreshToken, String signature, String accessToken) {
            this.id = id;
            this.issued = issued;
            this.instanceUrl = new Reference(instanceUrl);
            this.scope = scope;
            this.refreshToken = refreshToken;
            this.signature = signature;
            this.accessToken = accessToken;
            ChallengeWriter cw = new ChallengeWriter();
            cw.append(ChallengeScheme.HTTP_OAUTH.getTechnicalName()).appendSpace().append(
                    accessToken);
            this.authorization = cw.toString();

        }

        public Reference getBaseReference() {
            return instanceUrl;
        }

        public String getAuthorization() {
            return authorization;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}
