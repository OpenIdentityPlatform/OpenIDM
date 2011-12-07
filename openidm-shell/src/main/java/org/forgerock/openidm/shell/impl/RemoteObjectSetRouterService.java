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
 * $Id$
 */
package org.forgerock.openidm.shell.impl;

import org.forgerock.openidm.external.rest.RestService;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.Patch;
import org.restlet.data.Conditions;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.data.Tag;
import org.restlet.engine.http.header.HeaderConstants;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class RemoteObjectSetRouterService implements ObjectSet {

    private final ClientResource clientResource;
    private final URI openidmUri;

    public RemoteObjectSetRouterService() throws Exception {
        //TODO load properties and create ClientResource
        clientResource = RestService.createClientResource(null);
        openidmUri = new URI("http://localhost:8080/openidm/");
    }

    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        //TODO fix URL and response
        clientResource.setReference(getReference(id));
        JacksonRepresentation entity = new JacksonRepresentation(object);
        //TODO Validate HeaderConstants.HEADER_IF_NONE_MATCH = "\"*\"";
        entity.setTag(Tag.ALL);
        Representation response = clientResource.put(entity);
        clientResource.setConditions(new Conditions());
        JacksonRepresentation<Map> _response = null;
        if (response instanceof JacksonRepresentation) {
            _response = (JacksonRepresentation<Map>) response;
        } else if (response instanceof EmptyRepresentation == false) {
            _response = new JacksonRepresentation<Map>(response, Map.class);
        }
    }

    public Map<String, Object> read(String id) throws ObjectSetException {
        //TODO fix URL and response
        clientResource.setReference(getReference(id));
        Representation response = clientResource.get();
        JacksonRepresentation<Map> _response = null;
        if (response instanceof JacksonRepresentation) {
            _response = (JacksonRepresentation<Map>) response;
        } else if (response instanceof EmptyRepresentation == false) {
            _response = new JacksonRepresentation<Map>(response, Map.class);
        }
        return _response == null ? null : _response.getObject();
    }

    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        //TODO fix URL and response
        clientResource.setReference(getReference(id));
        JacksonRepresentation entity = new JacksonRepresentation(object);
        //TODO Validate HeaderConstants.HEADER_IF_MATCH = rev
        entity.setTag(new Tag(rev));
        Representation response = clientResource.put(entity);
        JacksonRepresentation<Map> _response = null;
        if (response instanceof JacksonRepresentation) {
            _response = (JacksonRepresentation<Map>) response;
        } else if (response instanceof EmptyRepresentation == false) {
            _response = new JacksonRepresentation<Map>(response, Map.class);
        }
    }

    public void delete(String id, String rev) throws ObjectSetException {
        //TODO fix URL and response
        clientResource.setReference(getReference(id));
        Object o = clientResource.getRequest().getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        Form headers = o instanceof Form ? (Form) o : new Form();
        headers.add(HeaderConstants.HEADER_IF_MATCH, rev);
        clientResource.getRequest().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, headers);
        Representation response = clientResource.delete();
        clientResource.setConditions(new Conditions());
        JacksonRepresentation<Map> _response = null;
        if (response instanceof JacksonRepresentation) {
            _response = (JacksonRepresentation<Map>) response;
        } else if (response instanceof EmptyRepresentation == false) {
            _response = new JacksonRepresentation<Map>(response, Map.class);
        }
    }

    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        //TODO fix URL and response
        Reference reference = getReference(id);
        if (params.get("_query-id") == null) {
            throw new BadRequestException("Required _query-id parameter is missing");
        }
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() instanceof String) {
                reference.addQueryParameter(entry.getKey(), (String) entry.getValue());
            }
        }
        clientResource.setReference(reference);
        Representation response = clientResource.get();
        JacksonRepresentation<Map> _response = null;
        if (response instanceof JacksonRepresentation) {
            _response = (JacksonRepresentation<Map>) response;
        } else if (response instanceof EmptyRepresentation == false) {
            _response = new JacksonRepresentation<Map>(response, Map.class);
        }
        return _response == null ? null : _response.getObject();
    }

    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        //TODO fix URL and response
        clientResource.setReference(getReference(id));
        JacksonRepresentation entity = new JacksonRepresentation(params);
        Representation response = clientResource.post(entity);
        JacksonRepresentation<Map> _response = null;
        if (response instanceof JacksonRepresentation) {
            _response = (JacksonRepresentation<Map>) response;
        } else if (response instanceof EmptyRepresentation == false) {
            _response = new JacksonRepresentation<Map>(response, Map.class);
        }
        return _response == null ? null : _response.getObject();
    }

    private Reference getReference(String id) {
        return new Reference(openidmUri.resolve(id));
    }
}
