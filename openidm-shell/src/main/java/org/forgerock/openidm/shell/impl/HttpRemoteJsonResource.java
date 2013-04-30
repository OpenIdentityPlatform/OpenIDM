/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.FutureResult;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.UpdateRequest;
import org.restlet.Context;
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
import org.restlet.resource.ResourceException;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author $author$
 * @version $Revision$ $Date$
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

    private Reference baseReference;

    private final ClientResource remoteClient;

    public HttpRemoteJsonResource() {
        Context context = new Context();
        remoteClient = new ClientResource(context, "http://localhost:8080/openidm/");

        /*
         * Client client = new Client(Protocol.HTTP);
         * client.setContext(context); remoteClient.setNext(client);
         */

        // Accept: application/json
        List<Preference<MediaType>> acceptedMediaTypes = new ArrayList<Preference<MediaType>>(1);
        acceptedMediaTypes.add(new Preference(MediaType.APPLICATION_JSON));
        remoteClient.getClientInfo().setAcceptedMediaTypes(acceptedMediaTypes);

        ChallengeResponse rc =
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "openidm-admin", "openidm-admin");
        remoteClient.setChallengeResponse(rc);

        // -------------------------------------
        // Add user-defined extension headers
        // -------------------------------------
        /*
         * New Restlet 2.1 API Series<org.restlet.engine.header.Header>
         * additionalHeaders = (Series<org.restlet.engine.header.Header>)
         * remoteClient
         * .getRequest().getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
         * if (additionalHeaders == null) { additionalHeaders = new
         * Series<org.restlet
         * .engine.header.Header>(org.restlet.engine.header.Header.class);
         * remoteClient
         * .getRequest().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS,
         * additionalHeaders); }
         */

        /*
         * org.restlet.data.Form additionalHeaders = (org.restlet.data.Form)
         * remoteClient
         * .getRequest().getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
         * if (additionalHeaders == null) { additionalHeaders = new
         * org.restlet.data.Form();
         * remoteClient.getRequest().getAttributes().put
         * (HeaderConstants.ATTRIBUTE_HEADERS, additionalHeaders); }
         * 
         * additionalHeaders.add("X-OpenIDM-Username", "openidm-admin");
         * additionalHeaders.add("X-OpenIDM-Password", "openidm-admin");
         * additionalHeaders.add("X-PrettyPrint", "1");
         */

    }

    @Override
    public JsonValue action(org.forgerock.json.resource.Context context, ActionRequest request)
            throws org.forgerock.json.resource.ResourceException {
        return null;
    }

    @Override
    public FutureResult<JsonValue> actionAsync(org.forgerock.json.resource.Context context,
            ActionRequest request, ResultHandler<JsonValue> handler) {
        throw new NotImplementedException();
    }

    @Override
    public void close() {
        remoteClient.release();
    }

    @Override
    public Resource create(org.forgerock.json.resource.Context context, CreateRequest request)
            throws org.forgerock.json.resource.ResourceException {
        return null;
    }

    @Override
    public FutureResult<Resource> createAsync(org.forgerock.json.resource.Context context,
            CreateRequest request, ResultHandler<Resource> handler) {
        throw new NotImplementedException();
    }

    @Override
    public Resource delete(org.forgerock.json.resource.Context context, DeleteRequest request)
            throws org.forgerock.json.resource.ResourceException {
        return null;
    }

    @Override
    public FutureResult<Resource> deleteAsync(org.forgerock.json.resource.Context context,
            DeleteRequest request, ResultHandler<Resource> handler) {
        throw new NotImplementedException();
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Resource patch(org.forgerock.json.resource.Context context, PatchRequest request)
            throws org.forgerock.json.resource.ResourceException {
        throw new NotImplementedException();
    }

    @Override
    public FutureResult<Resource> patchAsync(org.forgerock.json.resource.Context context,
            PatchRequest request, ResultHandler<Resource> handler) {
        throw new NotImplementedException();
    }

    @Override
    public QueryResult query(org.forgerock.json.resource.Context context, QueryRequest request,
            QueryResultHandler handler) throws org.forgerock.json.resource.ResourceException {
        return null;
    }

    @Override
    public QueryResult query(org.forgerock.json.resource.Context context, QueryRequest request,
            Collection<? super Resource> results)
            throws org.forgerock.json.resource.ResourceException {
        return null;
    }

    @Override
    public FutureResult<QueryResult> queryAsync(org.forgerock.json.resource.Context context,
            QueryRequest request, QueryResultHandler handler) {
        throw new NotImplementedException();
    }

    @Override
    public Resource read(org.forgerock.json.resource.Context context, ReadRequest request)
            throws org.forgerock.json.resource.ResourceException {
        return null;
    }

    @Override
    public FutureResult<Resource> readAsync(org.forgerock.json.resource.Context context,
            ReadRequest request, ResultHandler<Resource> handler) {
        throw new NotImplementedException();
    }

    @Override
    public Resource update(org.forgerock.json.resource.Context context, UpdateRequest request)
            throws org.forgerock.json.resource.ResourceException {
        return null;
    }

    @Override
    public FutureResult<Resource> updateAsync(org.forgerock.json.resource.Context context,
            UpdateRequest request, ResultHandler<Resource> handler) {
        throw new NotImplementedException();
    }

    public JsonValue handle(Request jsonRequest)
            throws org.forgerock.json.resource.ResourceException {
        Representation response = null;
        ClientResource clientResource = null;
        try {
            String id = jsonRequest.getResourceName();
            Reference remoteRef = new Reference(id);

            // Prepare query params
            JsonValue params = new JsonValue(null);//jsonRequest.get("params");
            if (!params.isNull()) {
                for (Map.Entry<String, Object> entry : params.expect(Map.class).asMap().entrySet()) {
                    if (entry.getValue() instanceof String) {
                        remoteRef.addQueryParameter(entry.getKey(), (String) entry.getValue());
                    }
                }
            }
            clientResource = remoteClient.getChild(remoteRef);

            // Prepare payload
            Representation request = null;
            JsonValue value = new JsonValue(null);//jsonRequest.get("value");
            if (!value.isNull()) {
                request = new JacksonRepresentation<Map>(value.expect(Map.class).asMap());
            }

            // Prepare ETag
            Conditions conditions = new Conditions();
            JsonValue rev = null;//jsonRequest.get("rev");

            switch (jsonRequest.getRequestType()) {
            case CREATE:
                // TODO Use condition when
                // org.forgerock.json.resource.restlet.JsonServerResource#doHandle()
                // is fixed
                // conditions.setNoneMatch(Arrays.asList(Tag.ALL));
                clientResource.getRequest().setConditions(conditions);
                response = clientResource.put(request);
                break;
            case READ:
                if (!rev.isNull()) {
                    conditions.setMatch(getTag(rev.asString()));
                    clientResource.getRequest().setConditions(conditions);
                }
                response = clientResource.get();
                break;
            case UPDATE:
                conditions.setMatch(getTag(rev.required().asString()));
                clientResource.getRequest().setConditions(conditions);
                response = clientResource.put(request);
                break;
            case DELETE:
                conditions.setMatch(getTag(rev.required().asString()));
                clientResource.getRequest().setConditions(conditions);
                response = clientResource.delete();
                break;
            case PATCH:
                conditions.setMatch(getTag(rev.required().asString()));
                clientResource.getRequest().setConditions(conditions);
                clientResource.setMethod(PATCH);
                clientResource.getRequest().setEntity(request);
                response = clientResource.handle();
                break;
            case QUERY:
                response = clientResource.get();
                break;
            case ACTION:
                response = clientResource.post(request);
                break;
            default:
                throw new BadRequestException();
            }

            if (!clientResource.getStatus().isSuccess()) {
                throw org.forgerock.json.resource.ResourceException.getException(clientResource
                        .getStatus().getCode(), clientResource.getStatus().getDescription(),
                        clientResource.getStatus().getThrowable());
            }

            JsonValue result = null;

            if (null != response && response instanceof EmptyRepresentation == false) {
                result = new JsonValue(new JacksonRepresentation(response, Map.class).getObject());
            } else {
                result = new JsonValue(null);
            }
            return result;
        } catch (JsonValueException jve) {
            throw new BadRequestException(jve);
        } catch (ResourceException e) {
            StringBuilder sb = new StringBuilder(e.getStatus().getDescription());
            if (null != clientResource) {
                try {
                    sb.append(" ").append(clientResource.getResponse().getEntity().getText());
                } catch (IOException e1) {
                }
            }
            throw org.forgerock.json.resource.ResourceException.getException(e.getStatus()
                    .getCode(), sb.toString(), e.getCause());
        } catch (Exception e) {
            throw new InternalServerErrorException(e);
        } finally {
            if (null != response) {
                response.release();
            }
        }
    }

    private List<Tag> getTag(String tag) {
        List<Tag> result = new ArrayList<Tag>(1);
        if (null != tag && tag.trim().length() > 0) {
            result.add(Tag.parse(tag));
        }
        return result;
    }
}
