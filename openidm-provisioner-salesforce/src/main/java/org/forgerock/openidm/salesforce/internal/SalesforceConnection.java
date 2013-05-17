/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openidm.salesforce.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.forgerock.json.resource.JsonResourceException;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Uniform;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.engine.Engine;
import org.restlet.engine.http.header.ChallengeWriter;
import org.restlet.engine.http.header.HeaderConstants;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sforce.ws.ConnectorConfig;

/**
 * Class to represent a Salesforce Connection
 * http://wiki.restlet.org/docs_2.1/13-restlet/28-restlet/392-restlet.html
 * http:/
 * /boards.developerforce.com/t5/REST-API-Integration/Having-trouble-getting
 * -Access-Token-with-username-password/td-p/278305
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class SalesforceConnection extends ClientResource {

    /**
     * Setup logging for the {@link SalesforceConnection}.
     */
    private static final Logger logger = LoggerFactory.getLogger(SalesforceConnection.class);

    public static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        mapper.getDeserializationConfig().set(
                DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Engine.getInstance().getRegisteredAuthenticators().add(new RawAuthenticatorHelper());
    }

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

    public SalesforceConnection(ClientResource resource) {
        super(resource);
    }

    public SalesforceConnection(SalesforceConfiguration configuration0)
            throws JsonResourceException {
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

        Client client = new Client(Protocol.HTTPS);
        client.setContext(getContext());

        /*
         * The connection timeout in milliseconds. The default value is 0,
         * meaning an infinite timeout
         */
        client.setConnectTimeout(configuration.getConnectTimeout());

        setNext(client);

        // Accept: application/json
        List<Preference<MediaType>> acceptedMediaTypes = new ArrayList<Preference<MediaType>>(1);
        acceptedMediaTypes.add(new Preference(MediaType.APPLICATION_JSON));
        getClientInfo().setAcceptedMediaTypes(acceptedMediaTypes);

        authenticate();
    }

    public boolean refreshAccessToken(final ConnectorConfig config) throws JsonResourceException {
        // First check
        final String expired = config.getSessionId();
        if (authentication.getAccessToken().equals(expired)) {
            // Sync the threads
            synchronized (this) {
                // Second check
                if (authentication.getAccessToken().equals(expired)) {
                    authenticate();
                    if (authentication.getAccessToken().equals(expired)) {
                        logger.warn("Expired token was not revoked and couldn't be refreshed");
                        return false;
                    } else {
                        logger.info("Expired token was successfully refreshed");
                    }
                }
            }
        }
        config.setSessionId(authentication.getAccessToken());
        return true;
    }

    public boolean refreshAccessToken(final Request request) throws JsonResourceException {
        // First check
        final String expired = fetchAccessToken(request);
        if (authentication.getAuthorization().equals(expired)) {
            // Sync the threads
            synchronized (this) {
                // Second check
                if (authentication.getAuthorization().equals(expired)) {
                    authenticate();
                    if (authentication.getAuthorization().equals(expired)) {
                        logger.warn("Expired token was not revoked and couldn't be refreshed");
                        return false;
                    } else {
                        logger.info("Expired token was successfully refreshed");
                    }
                }
            }
        }
        resetAccessToken(request);
        return true;
    }

    private String fetchAccessToken(final Request result) {
        ChallengeResponse additionalHeaders = result.getChallengeResponse();
        if (additionalHeaders != null) {
            return additionalHeaders.getRawValue();
        }
        return null;
    }

    public void resetAccessToken(final Request result) {
        ChallengeResponse additionalHeaders = result.getChallengeResponse();
        if (additionalHeaders != null) {
            additionalHeaders.setRawValue(authentication.getAuthorization());
        }
    }

    private void authenticate() throws JsonResourceException {
        Representation body = null;
        ClientResource cr = super.getChild(new Reference(""));
        try {
            revokeAccessToken();

            body = cr.post(configuration.getAuthenticationForm().getWebRepresentation());
            if (body instanceof EmptyRepresentation == false) {
                authentication = createJson(new JacksonRepresentation<Map>(body, Map.class));
            } else {
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR,
                        "OAuth2 request response is empty!");
            }
        } catch (ResourceException e) {
            throw getJsonResourceException(cr);
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
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
            ClientResource revoke = super.getChild(new Reference("./revoke"));
            revoke.setFollowingRedirects(true);
            revoke.getReference().addQueryParameter("token", authentication.getAccessToken());
            revoke.setMethod(Method.GET);
            revoke.handle();

            if (revoke.getResponse().getStatus().isError()) {
                // TODO is it expired?
                logger.error("Failed to revoke token - status:{} response:{} ", revoke
                        .getResponse().getStatus(),
                        null != revoke.getResponse().getEntity() ? revoke.getResponse()
                                .getEntityAsText() : "");
            } else {
                logger.info("Succeed to revoke token - status:{} response:{} ", revoke
                        .getResponse().getStatus(),
                        null != revoke.getResponse().getEntity() ? revoke.getResponse()
                                .getEntityAsText() : "");
            }
        }
    }

    public String getQueryExpression(String queryId) {
        return null != queryId ? configuration.getPredefinedQueries().get(queryId) : null;
    }

    public ConnectorConfig getConnectorConfig(String api) {
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
        config.setSessionId(getOAuthUser().getAccessToken());

        return config;
    }

    public JsonResourceException getJsonResourceException(ClientResource rc) {
        JsonResourceException jre =
                new JsonResourceException(rc.getResponse().getStatus().getCode(), rc.getResponse()
                        .getStatus().getDescription());
        Representation error = rc.getResponse().getEntity();
        if (null != error && error instanceof EmptyRepresentation == false) {
            try {
                Object res = new JacksonRepresentation<Object>(error, Object.class).getObject();
                if (res instanceof Map) {
                    jre.setDetail((Map<String, Object>) res);
                } else if (res instanceof List) {
                    Map<String, Object> details = new HashMap<String, Object>();
                    for (Object o : (List<Object>) res) {
                        if (o instanceof Map) {
                            if (null != ((Map) o).get("errorCode")) {
                                details.put((String) ((Map) o).get("errorCode"), o);
                            } else {
                                details.put(UUID.randomUUID().toString(), o);
                            }
                        }
                    }
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
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

    public void test() throws JsonResourceException {
        ClientResource resource = getChild("services/data/" + getVersion());
        test(resource, true);
    }

    public String getVersion() {
        return "v" + Double.toString(configuration.getVersion());
    }

    private void test(ClientResource resource, boolean tryReauth) throws JsonResourceException {
        try {
            resource.handle();
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
        }
        final Response response = resource.getResponse();
        if (response.getStatus().isError()) {

            JsonResourceException sfe = getJsonResourceException(resource);

            if (tryReauth && Status.CLIENT_ERROR_UNAUTHORIZED.equals(response.getStatus())) {
                // Re authenticate
                if (refreshAccessToken(resource.getRequest())) {
                    try {
                        resource.handle();
                    } catch (Exception e) {
                        throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
                    }
                    test(resource, false);
                } else {
                    throw new JsonResourceException(401, "AccessToken can not be renewed");
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
    public ClientResource getChild(Reference relativeRef) throws ResourceException {
        ClientResource result = null;

        if ((relativeRef != null) && relativeRef.isRelative()) {
            result = new SalesforceConnection(this);
            result.setReference(new Reference(authentication.getBaseReference(), relativeRef)
                    .getTargetRef());
            // -------------------------------------
            // Add user-defined extension headers
            // -------------------------------------
            // Series<Header> additionalHeaders = (Series<Header>)
            // result.getRequest().getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
            // if (additionalHeaders == null) {
            // additionalHeaders = new Series<Header>(Header.class);
            // result.getRequest().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS,
            // additionalHeaders);
            // }
            // additionalHeaders.add(HeaderConstants.HEADER_AUTHORIZATION,
            // authentication.getAuthorization());

            Form additionalHeaders = null;
            Object o = result.getRequest().getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
            if (o instanceof Form) {
                additionalHeaders = (Form) o;
            } else {
                additionalHeaders = new Form();
                result.getRequest().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS,
                        additionalHeaders);
            }

            result.setChallengeResponse(new ChallengeResponse(ChallengeScheme.HTTP_OAUTH,
                    authentication.getAuthorization()));

            // additionalHeaders.add(HeaderConstants.HEADER_AUTHORIZATION,
            // authentication.getAuthorization());

            additionalHeaders.add("X-PrettyPrint", "1");
        } else {
            // doError(Status.CLIENT_ERROR_BAD_REQUEST,
            // "The child URI is not relative.");
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
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
            JsonResourceException {
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
                throw new JsonResourceException(JsonResourceException.FORBIDDEN, (String) answer
                        .get(ERROR_DESC));
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

    public OAuthUser getOAuthUser() {
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
