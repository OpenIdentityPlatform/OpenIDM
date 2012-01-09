/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.openidm.shell.impl;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class HttpRemoteJsonResource implements JsonResource {


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

        /*Client client = new Client(Protocol.HTTP);
        client.setContext(context);
        remoteClient.setNext(client);*/

        // Accept: application/json
        List<Preference<MediaType>> acceptedMediaTypes = new ArrayList<Preference<MediaType>>(1);
        acceptedMediaTypes.add(new Preference(MediaType.APPLICATION_JSON));
        remoteClient.getClientInfo().setAcceptedMediaTypes(acceptedMediaTypes);

        ChallengeResponse rc = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "openidm-admin", "openidm-admin");
        remoteClient.setChallengeResponse(rc);


        // -------------------------------------
        //  Add user-defined extension headers
        // -------------------------------------
        /*
        New Restlet 2.1 API
        Series<org.restlet.engine.header.Header> additionalHeaders =
                (Series<org.restlet.engine.header.Header>) remoteClient.getRequest().getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        if (additionalHeaders == null) {
            additionalHeaders = new Series<org.restlet.engine.header.Header>(org.restlet.engine.header.Header.class);
            remoteClient.getRequest().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, additionalHeaders);
        }*/

        /*org.restlet.data.Form additionalHeaders = (org.restlet.data.Form) remoteClient.getRequest().getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        if (additionalHeaders == null) {
            additionalHeaders = new org.restlet.data.Form();
            remoteClient.getRequest().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, additionalHeaders);
        }

        additionalHeaders.add("X-OpenIDM-Username", "openidm-admin");
        additionalHeaders.add("X-OpenIDM-Password", "openidm-admin");
        additionalHeaders.add("X-PrettyPrint", "1");*/

    }

    @Override
    public JsonValue handle(JsonValue jsonValues) throws JsonResourceException {
        Representation response = null;
        ClientResource clientResource = null;
        try {
            String id = jsonValues.get("id").required().asString();
            Reference remoteRef = new Reference(id);

            // Prepare query params
            JsonValue params = jsonValues.get("params");
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
            JsonValue value = jsonValues.get("value");
            if (!value.isNull()) {
                request = new JacksonRepresentation<Map>(value.expect(Map.class).asMap());
            }

            // Prepare ETag
            Conditions conditions = new Conditions();
            JsonValue rev = jsonValues.get("rev");

            switch (jsonValues.get("method").required().asEnum(SimpleJsonResource.Method.class)) {
                case create:
                    conditions.setNoneMatch(Arrays.asList(Tag.ALL));
                    clientResource.getRequest().setConditions(conditions);
                    response = clientResource.put(request);
                    break;
                case read:
                    if (!rev.isNull()) {
                        conditions.setMatch(getTag(rev.asString()));
                        clientResource.getRequest().setConditions(conditions);
                    }
                    response = clientResource.get();
                    break;
                case update:
                    conditions.setMatch(getTag(rev.required().asString()));
                    clientResource.getRequest().setConditions(conditions);
                    response = clientResource.put(request);
                    break;
                case delete:
                    conditions.setMatch(getTag(rev.required().asString()));
                    clientResource.getRequest().setConditions(conditions);
                    response = clientResource.delete();
                    break;
                case patch:
                    conditions.setMatch(getTag(rev.required().asString()));
                    clientResource.getRequest().setConditions(conditions);
                    clientResource.setMethod(PATCH);
                    clientResource.getRequest().setEntity(request);
                    response = clientResource.handle();
                    break;
                case query:
                    response = clientResource.get();
                    break;
                case action:
                    response = clientResource.post(request);
                    break;
                default:
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
            }

            if (!clientResource.getStatus().isSuccess()) {
                throw new JsonResourceException(clientResource.getStatus().getCode(), clientResource.getStatus().getDescription(), clientResource.getStatus().getThrowable());
            }

            JsonValue result = null;

            if (null != response && response instanceof EmptyRepresentation == false) {
                result = new JsonValue(new JacksonRepresentation(response, Map.class).getObject());
            } else {
                result = new JsonValue(null);
            }
            return result;
        } catch (JsonValueException jve) {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST, jve);
        } catch (ResourceException e) {
            StringBuilder sb = new StringBuilder(e.getStatus().getDescription());
            if (null != clientResource) {
                try {
                    sb.append(" ").append(clientResource.getResponse().getEntity().getText());
                } catch (IOException e1) {
                }
            }
            throw new JsonResourceException(e.getStatus().getCode(), sb.toString(), e.getCause());
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getMessage(), e);
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
