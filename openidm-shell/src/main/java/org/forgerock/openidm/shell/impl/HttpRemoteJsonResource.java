/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
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

package org.forgerock.openidm.shell.impl;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.http.util.Json;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Conditions;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.data.Tag;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;


/**
 * A {@link Connection} to the remote OpenIDM instance.
 */
public class HttpRemoteJsonResource implements Connection {

    /**
     * Requests that the origin server accepts the entity enclosed in the
     * request as a new subordinate of the resource identified by the request
     * URI.
     *
     * @see <a
     *      href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.5">HTTP
     *      RFC - 9.5 POST</a>
     */
    public static final Method PATCH = new Method("PATCH");

    /**Base reference used for requesting this resource. */
    private Reference baseReference;

    /** Username used for authentication when accessing the resource. */
    private String username = "";

    /** Password used for authentication when accessing the resource. */
    private String password = "";

    /**
     * Construct the HttpRemoteJsonResource.
     */
    public HttpRemoteJsonResource() {
    }

    /**
     * Create a HttpRemoteJsonResource with credentials.
     *
     * @param uri URI of this resource.
     * @param username Username for HTTP basic authentication
     * @param password Password for HTTP basic authentication.
     */
    public HttpRemoteJsonResource(final String uri, final String username, final String password) {
        this.username = username;
        this.password = password;

        baseReference = new Reference(uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse action(Context context, ActionRequest request) throws ResourceException {
        JsonValue params = new JsonValue(request.getAdditionalParameters());
        JsonValue result = handle(request, request.getResourcePath(), params);
        return newActionResponse(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionAsync(Context context, ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceResponse create(Context context, CreateRequest request) throws ResourceException {
        JsonValue response = handle(request, request.getResourcePathObject().child(request.getNewResourceId()).toString(), null);
        return newResourceResponse(response.get("_id").asString(), response.get("_rev").asString(), response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createAsync(Context context, CreateRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceResponse delete(Context context, DeleteRequest request) throws ResourceException {
        final JsonValue response = handle(request, request.getResourcePath(), null);
        return newResourceResponse(response.get("_id").asString(), response.get("_rev").asString(), response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteAsync(Context context, DeleteRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceResponse patch(Context context, PatchRequest request) throws ResourceException {
        throw new NotSupportedException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchAsync(Context context, PatchRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResponse query(Context context, QueryRequest request, QueryResourceHandler handler) throws ResourceException {
        throw new NotSupportedException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResponse query(Context context, QueryRequest request, Collection<? super ResourceResponse> results) throws ResourceException {
        throw new NotSupportedException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryAsync(Context context, QueryRequest request, QueryResourceHandler handler) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceResponse read(Context context, ReadRequest request) throws ResourceException {
        JsonValue result = handle(request, request.getResourcePath(), null);
        return newResourceResponse(result.get("_id").asString(), result.get("_rev").asString(), result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readAsync(Context context, ReadRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceResponse update(Context context, UpdateRequest request) throws ResourceException {
        JsonValue result = handle(request, request.getResourcePath(), null);
        return newResourceResponse(result.get("_id").asString(), result.get("_rev").asString(), result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateAsync(Context context, UpdateRequest request) {
        return new NotSupportedException().asPromise();
    }

    private ClientResource getClientResource(Reference ref) {
        ClientResource clientResource = new ClientResource(new org.restlet.Context(), new Reference(baseReference, ref));

        List<Preference<MediaType>> acceptedMediaTypes = new ArrayList<Preference<MediaType>>(1);
        acceptedMediaTypes.add(new Preference<MediaType>(MediaType.APPLICATION_JSON));
        clientResource.getClientInfo().setAcceptedMediaTypes(acceptedMediaTypes);
        clientResource.getLogger().setLevel(Level.WARNING);

        ChallengeResponse rc = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, password);
        clientResource.setChallengeResponse(rc);

        return clientResource;
    }

    private JsonValue handle(Request request, String id, JsonValue params)
        throws org.forgerock.json.resource.ResourceException {
        Representation response = null;
        ClientResource clientResource = null;
        try {
            Reference remoteRef = new Reference(id);

            // Get the client resource corresponding to this request's resource name
            clientResource = getClientResource(remoteRef);

            // Prepare query params
            if (params != null && !params.isNull()) {
                for (Map.Entry<String, Object> entry : params.asMap().entrySet()) {
                    if (entry.getValue() instanceof String) {
                        clientResource.addQueryParameter(entry.getKey(), (String) entry.getValue());
                    }
                }
            }

            // Payload
            Representation representation = null;
            JsonValue value = getRequestValue(request);
            if (!value.isNull()) {
                representation = new JacksonRepresentation<Map<String, Object>>(value.asMap());
            }

            // ETag
            Conditions conditions = new Conditions();

            switch (request.getRequestType()) {
            case CREATE:
                conditions.setNoneMatch(Arrays.asList(Tag.ALL));
                clientResource.getRequest().setConditions(conditions);
                response = clientResource.put(representation);
                break;
            case READ:
                response = clientResource.get();
                break;
            case UPDATE:
                conditions.setMatch(getTag(((UpdateRequest) request).getRevision()));
                clientResource.getRequest().setConditions(conditions);
                response = clientResource.put(representation);
                break;
            case DELETE:
                conditions.setMatch(Arrays.asList(Tag.ALL));
                clientResource.getRequest().setConditions(conditions);
                response = clientResource.delete();
                break;
            case PATCH:
                conditions.setMatch(getTag(((PatchRequest) request).getRevision()));
                clientResource.getRequest().setConditions(conditions);
                clientResource.setMethod(PATCH);
                clientResource.getRequest().setEntity(representation);
                response = clientResource.handle();
                break;
            case QUERY:
                response = clientResource.get();
                break;
            case ACTION:
                clientResource.setQueryValue("_action", ((ActionRequest) request).getAction());
                response = clientResource.post(representation);
                break;
            default:
                throw new BadRequestException();
            }

            if (!clientResource.getStatus().isSuccess()) {
                throw org.forgerock.json.resource.ResourceException.getException(clientResource
                        .getStatus().getCode(), clientResource.getStatus().getDescription(),
                        clientResource.getStatus().getThrowable());
            }
            return (null != response && !(response instanceof EmptyRepresentation)
                    ? json(Json.readJson(response.getText()))
                    : json(null));
        } catch (JsonValueException jve) {
            throw new BadRequestException(jve);
        } catch (org.restlet.resource.ResourceException e) {
            StringBuilder sb = new StringBuilder(e.getStatus().getDescription());
            if (null != clientResource) {
                try {
                    sb.append(" ").append(clientResource.getResponse().getEntity().getText());
                } catch (IOException e1) {
                    // unable to add response text to exception message, proceed without
                }
            }
            throw ResourceException.getException(e.getStatus().getCode(), sb.toString(), e.getCause());
        } catch (Exception e) {
            throw new InternalServerErrorException(e);
        } finally {
            if (null != response) {
                response.release();
            }
        }
    }

    private JsonValue getRequestValue(Request request) throws Exception {
        switch (request.getRequestType()) {
        case CREATE:
            return ((CreateRequest) request).getContent();
        case UPDATE:
            return new JsonValue(((UpdateRequest) request).getContent());
        case PATCH:
            ObjectMapper mapper = new ObjectMapper();
            List<PatchOperation> ops = ((PatchRequest) request).getPatchOperations();
            JsonValue value = new JsonValue(new ArrayList<Object>());
            for (PatchOperation op : ops) {
                value.add(new JsonValue(mapper.readValue(op.toString(), Object.class)));
            }
            return value;
        case ACTION:
            JsonValue content = ((ActionRequest) request).getContent();
            if (content != null && !content.isNull()) {
                return content;
            } else {
                return new JsonValue(new HashMap<String, Object>());
            }
        }
        return new JsonValue(null);
    }

    private List<Tag> getTag(String tag) {
        List<Tag> result = new ArrayList<Tag>(1);
        if (null != tag && tag.trim().length() > 0) {
            result.add(Tag.parse(tag));
        }
        return result;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the port.
     *
     * @param port the port
     */
    public void setPort(int port) {
        baseReference.setHostPort(port);
    }

    /**
     * Sets the base URI.
     *
     * @param baseUri the base URI
     */
    public void setBaseUri(final String baseUri) {
        baseReference = new Reference(baseUri);
    }
}
