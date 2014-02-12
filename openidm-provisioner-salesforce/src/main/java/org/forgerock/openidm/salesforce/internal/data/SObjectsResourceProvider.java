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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.salesforce.internal.ServerContext;
import org.restlet.Response;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SObjectsResourceProvider is an adapter for Salesforce Data API.
 *
 * @author Laszlo Hordos
 */
public class SObjectsResourceProvider extends SimpleJsonResource {

    /**
     * Setup logging for the
     * {@link org.forgerock.openidm.salesforce.internal.data.SObjectsResourceProvider}
     * .
     */
    final static Logger logger = LoggerFactory.getLogger(SObjectsResourceProvider.class);

    public static final String NEXT_RECORDS_URL = "nextRecordsUrl";

    private final SalesforceConnection connection;

    public SObjectsResourceProvider(final SalesforceConnection connection) {
        this.connection = connection;
    }

    private final ConcurrentMap<String, SObjectDescribe> schema =
            new ConcurrentHashMap<String, SObjectDescribe>();

    @Override
    protected JsonValue create(JsonValue request) throws JsonResourceException {

        String[] ids = getIds();
        String type = ids[0];
        SObjectDescribe describe = getSObjectDescribe(type);
        if (null == describe) {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                    "Type not supported: " + type);
        }

        final ClientResource cr = getClientResource(type, null);
        try {
            JsonValue create = new JsonValue(describe.beforeCreate(request.get("value")));
            logger.trace("Create sobjects/{} \n content: \n{}\n", type, create);

            cr.getRequest().setEntity(new JacksonRepresentation<Map>(create.asMap()));
            cr.setMethod(org.restlet.data.Method.POST);
            handleRequest(cr, true);
            Representation body = cr.getResponse().getEntity();
            if (null != body && body instanceof EmptyRepresentation == false) {
                JacksonRepresentation<Map> rep = new JacksonRepresentation<Map>(body, Map.class);
                JsonValue result = new JsonValue(rep.getObject());
                if (result.get("success").asBoolean()) {
                    result.put(ServerConstants.OBJECT_PROPERTY_ID, result.get("id").getObject());
                }
                if (result.isDefined("errors")) {
                    if (result.get("errors").size() > 0) {
                        throw new JsonResourceException(500, "Failed to create FIX ME");
                    }
                }
                return result;
            } else {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
            }
        } finally {
            if (null != cr) {
                cr.release();
            }
        }
    }

    @Override
    protected JsonValue read(JsonValue request) throws JsonResourceException {

        String[] ids = getIds();
        if (ids.length == 2) {
            final ClientResource cr = getClientResource(ids[0], ids[1]);
            try {
                handleRequest(cr, true);
                Representation body = cr.getResponse().getEntity();
                if (null != body && body instanceof EmptyRepresentation == false) {
                    JacksonRepresentation<Map> rep =
                            new JacksonRepresentation<Map>(body, Map.class);
                    JsonValue result = new JsonValue(rep.getObject());
                    if (result.isDefined("Id")) {
                        result.put(ServerConstants.OBJECT_PROPERTY_ID, result.get("Id").required()
                                .asString());
                    }
                    return result;
                } else {
                    throw new JsonResourceException(JsonResourceException.NOT_FOUND);
                }
            } finally {
                if (null != cr) {
                    cr.release();
                }
            }
        } else {
            throw new JsonResourceException(JsonResourceException.FORBIDDEN,
                    "Reading the Collection is not allowed");
        }
    }

    private JsonValue sendClientRequest(org.restlet.data.Method method, String id,
            Parameter... parameters) throws JsonResourceException, IOException {

        final ClientResource cr = getClientResource(id);
        try {
            cr.setMethod(method);
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

    private ClientResource getClientResource(String id) {
        return connection.getChild(id == null ? "services/data/" + connection.getVersion()
                : "services/data/" + connection.getVersion() + "/" + id);
    }

    @Override
    protected JsonValue query(JsonValue request) throws JsonResourceException {
        JsonValue params = request.get("params").required();

        String queryExpression = null;
        if (params.isDefined(QueryConstants.QUERY_EXPRESSION)) {
            queryExpression = params.get(QueryConstants.QUERY_EXPRESSION).asString();
            if (StringUtils.isBlank(queryExpression)) {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                        "Invalid 'null' param value: " + QueryConstants.QUERY_EXPRESSION);
            }
        } else if (params.isDefined(QueryConstants.QUERY_ID)) {
            String queryId = params.get(QueryConstants.QUERY_ID).asString();
            queryExpression = connection.getQueryExpression(queryId);
            if (QueryConstants.QUERY_ALL_IDS.equals(queryId)) {
                Matcher matcher = ServerContext.get().getMatcher();
                queryExpression = "SELECT id FROM " + matcher.group(1);
            } else if (StringUtils.isBlank(queryExpression)) {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                        "Unsupported queryId " + queryId);
            }
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                    "Invalid request missing parameter [" + QueryConstants.QUERY_EXPRESSION + ", "
                            + QueryConstants.QUERY_ID + "]");
        }

        List<Map<String, Object>> queryResult = new ArrayList<Map<String, Object>>();
        Integer totalSize = null;
        JsonValue result = null;
        do {
            try {
                if (null == result) {
                    result =
                            sendClientRequest(org.restlet.data.Method.GET, "query", new Parameter(
                                    "q", queryExpression));
                } else if (!result.get(NEXT_RECORDS_URL).isNull()) {
                    String url = result.get(NEXT_RECORDS_URL).asString();
                    String queryValue = url.substring(url.indexOf("query/"), url.length());

                    result = sendClientRequest(org.restlet.data.Method.GET, queryValue);
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

    @Override
    protected JsonValue update(JsonValue request) throws JsonResourceException {
        logger.trace("Salesforce Update: \n{}\n", request);
        // String id = request.get("id").required().asString();
        String[] ids = getIds();
        if (ids.length == 2) {
            String type = ids[0];
            SObjectDescribe describe = getSObjectDescribe(type);
            if (null == describe) {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                        "Type not supported: " + type);
            }

            final ClientResource cr = getClientResource(ids[0], ids[1]);
            try {
                cr.setMethod(SalesforceConnection.PATCH);

                JsonValue update = new JsonValue(describe.beforeUpdate(request.get("value")));
                logger.trace("Update sobjects/{} \n content: \n{}\n", type, update);

                cr.getRequest().setEntity(new JacksonRepresentation<Map>(update.asMap()));

                handleRequest(cr, true);

                Representation body = cr.getResponse().getEntity();
                if (null != body && body instanceof EmptyRepresentation == false) {
                    JacksonRepresentation<Map> rep =
                            new JacksonRepresentation<Map>(body, Map.class);
                    JsonValue result = new JsonValue(rep.getObject());
                    // result.put(ServerConstants.OBJECT_PROPERTY_ID,
                    // result.get("Id").required().asString());
                    return result;
                } else {
                    JsonValue result = new JsonValue(new HashMap<String, Object>());
                    result.put(ServerConstants.OBJECT_PROPERTY_ID, ids[1]);
                    return result;
                }
            } finally {
                if (null != cr) {
                    cr.release();
                }
            }
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
        }
    }

    @Override
    protected JsonValue delete(JsonValue request) throws JsonResourceException {
        String[] ids = getIds();
        if (ids.length == 2) {
            final ClientResource cr = getClientResource(ids[0], ids[1]);
            try {
                cr.setMethod(org.restlet.data.Method.DELETE);
                handleRequest(cr, true);
                Representation body = cr.getResponse().getEntity();
                if (null != body && body instanceof EmptyRepresentation == false) {
                    JacksonRepresentation<Map> rep =
                            new JacksonRepresentation<Map>(body, Map.class);
                    JsonValue result = new JsonValue(rep.getObject());
                    // result.put(ServerConstants.OBJECT_PROPERTY_ID,
                    // result.get("Id").required().asString());
                    return result;
                } else {
                    JsonValue result = new JsonValue(new HashMap<String, Object>());
                    result.put(ServerConstants.OBJECT_PROPERTY_ID, ids[1]);
                    return result;
                }
            } finally {
                if (null != cr) {
                    cr.release();
                }
            }
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
        }
    }

    protected SObjectDescribe getSObjectDescribe(String type) throws JsonResourceException {
        SObjectDescribe describe = schema.get(type);
        if (null == describe) {
            synchronized (schema) {
                describe = schema.get(type);
                if (null == describe) {
                    final ClientResource cr = getClientResource(type, "describe");
                    try {
                        cr.setMethod(org.restlet.data.Method.GET);

                        handleRequest(cr, true);
                        Representation body = cr.getResponse().getEntity();
                        if (null != body && body instanceof EmptyRepresentation == false) {
                            try {
                                JacksonRepresentation<Map> rep =
                                        new JacksonRepresentation<Map>(body, Map.class);
                                describe = SObjectDescribe.newInstance(rep.getObject());
                            } catch (Exception e) {
                                logger.error("Failed to parse the Describe from: {}, response: ",
                                        cr.getReference(), body);
                            }
                        }
                        if (null == describe) {
                            throw new JsonResourceException(JsonResourceException.NOT_FOUND,
                                    "Metadata not found for type: " + type);
                        } else {
                            schema.put(type, describe);
                        }
                    } finally {
                        if (null != cr) {
                            cr.release();
                        }
                    }
                }
            }
        }
        return describe;
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
        }
    }

    private ClientResource getClientResource(String type, String id) {
        StringBuilder sb =
                new StringBuilder("services/data/").append(connection.getVersion()).append(
                        "/sobjects");
        if (null != type) {
            sb.append('/').append(type);
            if (null != id) {
                sb.append('/').append(id);
            }
        }
        return connection.getChild(sb.toString());
    }

    private String[] getIds() {
        Matcher m = ServerContext.get().getMatcher();
        if (m.groupCount() == 2) {
            return new String[] { m.group(1) };
        } else {
            return new String[] { m.group(1), m.group(2) };
        }
    }
}
