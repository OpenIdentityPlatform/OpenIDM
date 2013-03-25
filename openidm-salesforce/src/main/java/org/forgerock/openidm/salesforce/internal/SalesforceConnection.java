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

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.engine.header.ChallengeWriter;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public SalesforceConnection(SalesforceConfiguration configuration0) throws ResourceException {
        super(new Context(), SalesforceConfiguration.LOGIN_URL);
        this.configuration = configuration0;

        Client client = new Client(Protocol.HTTPS);
        client.setContext(getContext());
        setNext(client);

        // Accept: application/json
        List<Preference<MediaType>> acceptedMediaTypes = new ArrayList<Preference<MediaType>>(1);
        acceptedMediaTypes.add(new Preference(MediaType.APPLICATION_JSON));
        getClientInfo().setAcceptedMediaTypes(acceptedMediaTypes);

        authenticate();

        // Hack for Restlet 2.0 don't need with 2.1
        // List<org.restlet.engine.security.AuthenticatorHelper> helpers =
        // Engine.getInstance().getRegisteredAuthenticators();
        // helpers.add(new AuthenticatorHelper(ChallengeScheme.HTTP_OAUTH, true,
        // false) {
        // @Override
        // public String formatResponse(ChallengeResponse challenge, Request
        // request,
        // Series<Parameter> httpHeaders) {
        // return challenge.getRawValue();
        // }
        // });
        // Engine.getInstance().setRegisteredAuthenticators(helpers);

    }

    public ChallengeResponse refreshAccessToken(ChallengeResponse expired) throws ResourceException {
        // First check
        if (authentication.getAuthorization().equals(expired.getRawValue())) {
            // Sync the threads
            synchronized (this) {
                // Second check
                if (authentication.getAuthorization().equals(expired.getRawValue())) {
                    authenticate();
                    logger.info("Success re-authentication");
                }
            }
        }
        return new ChallengeResponse(ChallengeScheme.HTTP_OAUTH, null, authentication
                .getAuthorization());
    }

    private void authenticate() throws ResourceException {
        Representation body = null;
        ClientResource cr = super.getChild(new Reference(""));
        try {
            revokeAccessToken();

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
            ClientResource revoke = super.getChild(new Reference("./revoke"));
            revoke.setFollowingRedirects(true);
            revoke.getReference().addQueryParameter("token", authentication.getAccessToken());
            revoke.setMethod(Method.GET);
            revoke.handle();
            logger.info("AccessToken revocation status:{} response:{} ", revoke.getResponse()
                    .getStatus(), revoke.getResponse().getEntityAsText());
            if (revoke.getResponse().getStatus().isError()) {
                logger.error("Failed to revoke the token: {}"
                        + revoke.getResponse().getEntityAsText());
            }
        }
    }

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

    public void test() throws ResourceException {
        Representation body = null;
        ClientResource rc = getChild("services/data/v26.0");
        try {
            body = rc.get();
        } catch (Exception e) {
            try {
                logger.error("Test failed: {}", rc.getResponse().getEntity().getText());
            } catch (IOException e1) {
                /* ignore */
            }
            throw new ServiceUnavailableException(e.getMessage(), e);
        } finally {
            if (body != null)
                body.release();
        }
    }

    public void dispose() {
        revokeAccessToken();
    }

    /**
     * {@inheritDoc}
     */
    public ClientResource getChild(Reference relativeRef)
            throws org.restlet.resource.ResourceException {
        ClientResource result = null;

        if ((relativeRef != null) && relativeRef.isRelative()) {
            result = new SalesforceConnection(this);
            result.setReference(new Reference(authentication.getBaseReference(), relativeRef)
                    .getTargetRef());
            // -------------------------------------
            // Add user-defined extension headers
            // -------------------------------------
            Series<Header> additionalHeaders =
                    (Series<Header>) result.getRequest().getAttributes().get(
                            HeaderConstants.ATTRIBUTE_HEADERS);
            if (additionalHeaders == null) {
                additionalHeaders = new Series<Header>(Header.class);
                result.getRequest().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS,
                        additionalHeaders);
            }
            additionalHeaders.add(HeaderConstants.HEADER_AUTHORIZATION, authentication
                    .getAuthorization());

            /*
             * Form additionalHeaders = null; Object o =
             * result.getRequest().getAttributes
             * ().get(HeaderConstants.ATTRIBUTE_HEADERS); if (o instanceof Form)
             * { additionalHeaders = (Form) o; } else { additionalHeaders = new
             * Form(); result.getRequest().getAttributes().put(HeaderConstants.
             * ATTRIBUTE_HEADERS, additionalHeaders); }
             * 
             * result.setChallengeResponse(new
             * ChallengeResponse(ChallengeScheme.HTTP_OAUTH,
             * authentication.getAuthorization()));
             */
            // additionalHeaders.add(HeaderConstants.HEADER_AUTHORIZATION,
            // authentication.getAuthorization());

            additionalHeaders.add("X-PrettyPrint", "1");
        } else {
            // doError(Status.CLIENT_ERROR_BAD_REQUEST,
            // "The child URI is not relative.");
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

            String instanceUrl = null;
            if (answer.get(INSTANCE_URL) instanceof String) {
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
