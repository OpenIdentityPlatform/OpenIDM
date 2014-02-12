/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.salesforce.internal.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.restlet.Response;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.forgerock.openidm.salesforce.internal.data.SObjectsResourceProvider.NEXT_RECORDS_URL;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class QueryResourceProvider extends SimpleJsonResource {

    /**
     * Setup logging for the {@link SObjectsResourceProvider}
     */
    final static Logger logger = LoggerFactory.getLogger(GenericResourceProvider.class);

    private final SalesforceConnection connection;

    public QueryResourceProvider(SalesforceConnection connection) {
        this.connection = connection;
    }

    @Override
    protected JsonValue query(JsonValue request) throws JsonResourceException {
        String queryExpression =
                request.get("params").get(QueryConstants.QUERY_EXPRESSION).required().asString();

        if (StringUtils.isBlank(queryExpression)) {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                    "Invalid 'null' param value: " + QueryConstants.QUERY_EXPRESSION);
        } else {
            List<Map<String, Object>> queryResult = new ArrayList<Map<String, Object>>();
            Integer totalSize = null;
            JsonValue result = null;
            do {
                try {
                    if (null == result) {
                        result =
                                sendClientRequest( "query", new Parameter(
                                        "q", queryExpression));
                    } else if (!result.get(NEXT_RECORDS_URL).isNull()) {
                        String url = result.get(NEXT_RECORDS_URL).asString();
                        String queryValue = url.substring(url.indexOf("query/"), url.length());

                        result = sendClientRequest(queryValue);
                    } else {
                        break;
                    }
                    if (result != null) {
                        for (JsonValue record : result.get("records")) {
                            if (record.isDefined("Id")) {
                                record.put(ServerConstants.OBJECT_PROPERTY_ID, record.get("Id")
                                        .asString());
                            }
                            queryResult.add(record.asMap());
                        }
                        if (totalSize == null) {
                            totalSize = result.get("totalSize").asInteger();
                        }
                        if (result.get("done").asBoolean()) {
                            break;
                        }
                    } else {
                        throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR,
                                "Unexpected Salesforce service response: 'null'");
                    }
                } catch (final IOException e) {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST, e);
                }
            } while (true);
            JsonValue searchResult = new JsonValue(new HashMap<String, Object>(2));
            if (totalSize != null) {
                searchResult.put("_resultCount", totalSize);
            }
            searchResult.put(QueryConstants.QUERY_RESULT, queryResult);
            return searchResult;
        }
    }

    private JsonValue sendClientRequest(String id, Parameter... parameters)
            throws JsonResourceException, IOException {

        StringBuilder sb =
                new StringBuilder("services/data/").append(connection.getVersion()).append('/')
                        .append(id);

        final ClientResource cr = connection.getChild(sb.toString());
        try {
            cr.setMethod(org.restlet.data.Method.GET);
            for (Parameter p : parameters) {
                cr.getReference().addQueryParameter(p.getName(), p.getValue());
            }
            logger.debug("Attempt to execute query: {}?{}", cr.getReference(), cr.getReference()
                    .getQuery());
            handleRequest(cr, true);
            Representation body = cr.getResponse().getEntity();
            if (null != body && body instanceof EmptyRepresentation == false) {
                JacksonRepresentation<Map> rep = new JacksonRepresentation<Map>(body, Map.class);
                return new JsonValue(rep.getObject());
            }
            return null;
        } finally {
            if (null != cr) {
                cr.release();
            }
        }
    }

    protected void handleRequest(final ClientResource resource, boolean tryReauth)
            throws JsonResourceException {
        try {
            resource.handle();
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
        }
        final Response response = resource.getResponse();
        if (response.getStatus().isError()) {

            JsonResourceException e = connection.getJsonResourceException(resource);

            if (tryReauth && Status.CLIENT_ERROR_UNAUTHORIZED.equals(response.getStatus())) {
                // Re authenticate
                if (connection.refreshAccessToken(resource.getRequest())) {
                    handleRequest(resource, false);
                } else {
                    throw new JsonResourceException(401, "AccessToken can not be renewed");
                }
            } else {
                throw e;
            }
            // throw new ResourceException(response.getStatus());
        }

    }
}
