/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.external.rest;

import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.codehaus.jackson.map.ObjectMapper;

import org.forgerock.json.fluent.JsonValue;

import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;

import org.osgi.service.component.ComponentContext;

import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.*;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Deprecated
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetJsonResource;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.PreconditionFailedException;

/**
 * External REST connectivity
 *
 * @author aegloff
 */
@Component(name = RestService.PID, immediate = true, policy = ConfigurationPolicy.OPTIONAL, enabled = true)
@Service
@Properties({
        @Property(name = "service.description", value = "REST connectivity"),
        @Property(name = "service.vendor", value = "ForgeRock AS"),
        @Property(name = "openidm.router.prefix", value = "external/rest")
})
public class RestService extends ObjectSetJsonResource {
    final static Logger logger = LoggerFactory.getLogger(RestService.class);
    public static final String PID = "org.forgerock.openidm.external.rest";

    // Keys in the JSON configuration
    //public static final String CONFIG_X = "X";

    // Keys in the request parameters to override config
    public static final String ARG_URL = "_url";
    public static final String ARG_RESULT_FORMAT = "_result-format";
    public static final String ARG_BODY = "_body";
    public static final String ARG_CONTENT_TYPE = "_content-type";
    public static final String ARG_HEADERS = "_headers";
    public static final String ARG_AUTHENTICATE = "_authenticate";
    public static final String ARG_METHOD = "_method";

    EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
    ObjectMapper mapper = new ObjectMapper();

    /**
     * Currently not supported by this implementation.
     * <p/>
     * Gets an object from the repository by identifier.
     *
     * @param fullId the identifier of the object to retrieve from the object set.
     * @return the requested object.
     * @throws NotFoundException   if the specified object could not be found.
     * @throws ForbiddenException  if access to the object is forbidden.
     * @throws BadRequestException if the passed identifier is invalid
     */
    @Override
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        throw new UnsupportedOperationException();
    }

    /**
     * Currently not supported by this implementation.
     * <p/>
     * Creates a new object in the object set.
     *
     * @param fullId the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param obj    the contents of the object to create in the object set.
     * @throws NotFoundException           if the specified id could not be resolved.
     * @throws ForbiddenException          if access to the object or object set is forbidden.
     * @throws PreconditionFailedException if an object with the same ID already exists.
     */
    @Override
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        throw new UnsupportedOperationException();
    }

    /**
     * Currently not supported by this implementation.
     * <p/>
     * Updates the specified object in the object set.
     *
     * @param fullId the identifier of the object to be put, or {@code null} to request a generated identifier.
     * @param rev    the version of the object to update; or {@code null} if not provided.
     * @param obj    the contents of the object to put in the object set.
     * @throws ConflictException           if version is required but is {@code null}.
     * @throws ForbiddenException          if access to the object is forbidden.
     * @throws NotFoundException           if the specified object could not be found.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     * @throws BadRequestException         if the passed identifier is invalid
     */
    @Override
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {
        throw new UnsupportedOperationException();
    }

    /**
     * Currently not supported by this implementation.
     * <p/>
     * Deletes the specified object from the object set.
     *
     * @param fullId the identifier of the object to be deleted.
     * @param rev    the version of the object to delete or {@code null} if not provided.
     * @throws NotFoundException           if the specified object could not be found.
     * @throws ForbiddenException          if access to the object is forbidden.
     * @throws ConflictException           if version is required but is {@code null}.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    @Override
    public void delete(String fullId, String rev) throws ObjectSetException {
        throw new UnsupportedOperationException();
    }

    /**
     * Currently not supported by this implementation.
     * <p/>
     * Applies a patch (partial change) to the specified object in the object set.
     *
     * @param id    the identifier of the object to be patched.
     * @param rev   the version of the object to patch or {@code null} if not provided.
     * @param patch the partial change to apply to the object.
     * @throws ConflictException           if patch could not be applied object state or if version is required.
     * @throws ForbiddenException          if access to the object is forbidden.
     * @throws NotFoundException           if the specified object could not be found.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new UnsupportedOperationException();
    }

    /**
     * Currently not supported by this implementation.
     * <p/>
     * Performs the query on the specified object and returns the associated results.
     *
     * @param fullId identifies the object to query.
     * @param params the parameters of the query to perform.
     * @return the query results, which includes meta-data and the result records in JSON object structure format.
     * @throws NotFoundException   if the specified object could not be found.
     * @throws BadRequestException if the specified params contain invalid arguments, e.g. a query id that is not
     *                             configured, a query expression that is invalid, or missing query substitution tokens.
     * @throws ForbiddenException  if access to the object or specified query is forbidden.
     */
    @Override
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {

        //TODO: This is work in progress, expect enhancements and changes.

        logger.debug("Action invoked on {} with {}", id, params);
        Map<String, Object> result = null;

        if (params == null) {
            throw new BadRequestException("Invalid action call on " + id + " : missing parameters to define what to invoke.");
        }

        // Handle Document coming from external, currently wrapped in an _entity object
        // TODO: Review inbound Restlet Mapping
        if (params.get("_entity") != null) {
            params = (Map<String, Object>) params.get("_entity");
        }

        String url = (String) params.get(ARG_URL);
        String method = (String) params.get(ARG_METHOD);
        Map<String, String> auth = (Map<String, String>) params.get(ARG_AUTHENTICATE);
        Map<String, String> headers = (Map<String, String>) params.get(ARG_HEADERS);
        String contentType = (String) params.get(ARG_CONTENT_TYPE);
        String body = (String) params.get(ARG_BODY);
        String resultFormat = (String) params.get(ARG_RESULT_FORMAT);
        //int timeout = params.get("_timeout");

        // Whether the data type format to return to the caller should be inferred, or is explicitly defined
        boolean detectResultFormat = true;
        if (resultFormat != null && !resultFormat.equals("auto")) {
            detectResultFormat = false;
        }

        if (url == null) {
            throw new BadRequestException("Invalid action call on " + id + " : missing required argument " + ARG_URL);
        }

        try {
            ClientResource cr = new ClientResource(url);
            Map<String, Object> attrs = cr.getRequestAttributes();

            if (headers != null) {
                org.restlet.data.Form reqHeaders = (org.restlet.data.Form) attrs.get("org.restlet.http.headers");
                if (reqHeaders == null) {
                    reqHeaders = new org.restlet.data.Form();
                    attrs.put("org.restlet.http.headers", reqHeaders);
                }
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    reqHeaders.add((String) entry.getKey(), (String) entry.getValue());
                    logger.info("Added to header {}: {}", entry.getKey(), entry.getValue());
                }
            }

            if (auth != null) {
                String type = auth.get("type");
                if (type == null) {
                    type = "basic";
                }
                if ("basic".equalsIgnoreCase(type)) {
                    String identifier = auth.get("user");
                    String secret = auth.get("password");
                    logger.debug("Using basic authentication for {} secret supplied: {}", identifier, (secret != null));
                    ChallengeResponse challengeResponse = new ChallengeResponse(ChallengeScheme.HTTP_COOKIE, identifier, secret);
                    cr.setChallengeResponse(challengeResponse);
                    cr.getRequest().setChallengeResponse(challengeResponse);
                }
            }

            // Default method if none supplied
            if (method == null) {
                method = "post";
            }

            Representation representation = null;
            if ("get".equalsIgnoreCase(method)) {
                representation = cr.get(); //MediaType.APPLICATION_JSON);
            } else if ("post".equalsIgnoreCase(method)) {
                representation = cr.post(body);
            } else if ("put".equalsIgnoreCase(method)) {
                representation = cr.put(body);
            } else if ("delete".equalsIgnoreCase(method)) {
                representation = cr.delete();
            } else if ("head".equalsIgnoreCase(method)) {
                representation = cr.head();
            } else if ("options".equalsIgnoreCase(method)) {
                // TODO: media type arg?
                representation = cr.options();
            } else {
                throw new BadRequestException("Unknown method " + method);
            }

            String text = representation.getText();
            logger.debug("Response: {} Response Attributes: ", text, cr.getResponseAttributes());

            if ((!detectResultFormat && resultFormat.equals(MediaType.APPLICATION_JSON))
                    || (detectResultFormat && representation.getMediaType().isCompatible(MediaType.APPLICATION_JSON))) {
                try {
                    if (text != null && text.trim().length() > 0) {
                        result = mapper.readValue(text, Map.class);
                    }
                } catch (Exception ex) {
                    throw new InternalServerErrorException("Failure in parsing the response as JSON: " + text
                            + " Reported failure: " + ex.getMessage(), ex);
                }
            }
            cr.release();
        } catch (java.io.IOException ex) {
            throw new InternalServerErrorException("Failed to invoke " + params, ex);
        }

        logger.trace("Action result on {} : {}", id, result);

        return result;
    }


    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());

        JsonValue config = null;
        try {
            config = enhancedConfig.getConfigurationAsJson(compContext);
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid and could not be parsed, can not start external REST connectivity: "
                    + ex.getMessage(), ex);
            throw ex;
        }

        init(config);

        logger.info("External REST connectivity started.");
    }

    /**
     * Initialize the instance with the given configuration.
     *
     * @param config the configuration
     */
    void init(JsonValue config) {
    }

    /* Currently rely on deactivate/activate to be called by DS if config changes instead
    @Modified
    void modified(ComponentContext compContext) {
    }
    */


    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext);

        logger.info("External REST connectivity stopped.");
    }

    public static ClientResource createClientResource(JsonValue params) {
        //TODO use the https://wikis.forgerock.org/confluence/display/json/http-request
        String url = params.get(ARG_URL).required().asString();
        Context context = new Context();

        context.getParameters().set("maxTotalConnections", "16");
        context.getParameters().set("maxConnectionsPerHost", "8");

        ClientResource cr = new ClientResource(context, url);
        JsonValue _authenticate = params.get(ARG_AUTHENTICATE);

        if (!_authenticate.isNull()) {
            ChallengeScheme authType = ChallengeScheme.valueOf(_authenticate.get("type").asString());
            if (authType == null) {
                authType = ChallengeScheme.HTTP_BASIC;
            }
            if (ChallengeScheme.HTTP_BASIC.equals(authType)) {
                String identifier = _authenticate.get("user").required().asString();
                String secret = _authenticate.get("password").asString();
                logger.debug("Using basic authentication for {} secret supplied: {}", identifier, (secret != null));
                ChallengeResponse challengeResponse = new ChallengeResponse(authType, identifier, secret);
                cr.setChallengeResponse(challengeResponse);
                cr.getRequest().setChallengeResponse(challengeResponse);
            }
            if (ChallengeScheme.HTTP_COOKIE.equals(authType)) {

                String authenticationTokenPath = "openidm/j_security_check";

                // Prepare the request
                Request request = new Request(org.restlet.data.Method.POST, authenticationTokenPath
                        + authenticationTokenPath);

                Form loginForm = new Form();
                loginForm.add("j_username", "admin");
                loginForm.add("j_password", "admin");
                Representation repEnt = loginForm.getWebRepresentation();

                request.setEntity(repEnt);

                Client client = new Client(Protocol.HTTP);

                request.setEntity(repEnt);
                Response res = client.handle(request);

                String identifier = _authenticate.get("user").required().asString();
                String secret = _authenticate.get("password").asString();
                logger.debug("Using cookie authentication for {} secret supplied: {}", identifier, (secret != null));
                ChallengeResponse challengeResponse = new ChallengeResponse(authType, identifier, secret);
                cr.setChallengeResponse(challengeResponse);
                cr.getRequest().setChallengeResponse(challengeResponse);
            }
        }

        return cr;
    }
}
