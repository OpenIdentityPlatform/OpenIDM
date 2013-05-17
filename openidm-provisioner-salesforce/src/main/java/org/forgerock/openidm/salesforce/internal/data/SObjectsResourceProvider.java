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

package org.forgerock.openidm.salesforce.internal.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.salesforce.internal.ServerContext;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
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

        ClientResource rc = getClientResource(type, null);

        JsonValue create = new JsonValue(describe.beforeCreate(request.get("value")));
        logger.error("Create sobjects/{} \n content: \n{}\n", type, create);

        rc.getRequest().setEntity(new JacksonRepresentation<Map>(create.asMap()));
        rc.setMethod(org.restlet.data.Method.POST);
        handleRequest(rc, true);
        Representation body = rc.getResponse().getEntity();
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

    }

    @Override
    protected JsonValue read(JsonValue request) throws JsonResourceException {

        String[] ids = getIds();
        if (ids.length == 2) {
            ClientResource rc = getClientResource(ids[0], ids[1]);
            handleRequest(rc, true);
            Representation body = rc.getResponse().getEntity();
            if (null != body && body instanceof EmptyRepresentation == false) {
                JacksonRepresentation<Map> rep = new JacksonRepresentation<Map>(body, Map.class);
                JsonValue result = new JsonValue(rep.getObject());
                if (result.isDefined("Id")) {
                    result.put(ServerConstants.OBJECT_PROPERTY_ID, result.get("Id").required()
                            .asString());
                }
                return result;
            } else {
                throw new JsonResourceException(JsonResourceException.NOT_FOUND);
            }
        } else {
            throw new JsonResourceException(JsonResourceException.FORBIDDEN,
                    "Reading the Collection is not allowed");
        }
    }

    @Override
    protected JsonValue query(JsonValue request) throws JsonResourceException {
        JsonValue params = request.get("params");
        StringBuilder sb =
                new StringBuilder("services/data/").append(connection.getVersion())
                        .append("/query");
        if (params.isDefined(QueryConstants.QUERY_EXPRESSION)) {

            ClientResource rc = connection.getChild(sb.toString());

            rc.getReference().addQueryParameter(
                    "q",
                    request.get("params").get(QueryConstants.QUERY_EXPRESSION).required()
                            .asString());
            rc.setMethod(org.restlet.data.Method.GET);
            logger.debug("Attempt to execute query: {}?{}", rc.getReference(), rc.getReference()
                    .getQuery());
            handleRequest(rc, true);
            Representation body = rc.getResponse().getEntity();
            List<Map<String, Object>> queryResult = new ArrayList<Map<String, Object>>();
            JsonValue searchResult = new JsonValue(new HashMap<String, Object>(1));
            searchResult.put(QueryConstants.QUERY_RESULT, queryResult);

            if (null != body && body instanceof EmptyRepresentation == false) {
                JacksonRepresentation<Map> rep = new JacksonRepresentation<Map>(body, Map.class);
                JsonValue result = new JsonValue(rep.getObject());
                for (JsonValue record : result.get("records")) {
                    if (record.isDefined("Id")) {
                        record.put(ServerConstants.OBJECT_PROPERTY_ID, record.get("Id").asString());
                    }
                    queryResult.add(record.asMap());
                }
            }
            return searchResult;
        } else {
            String queryId = params.get(QueryConstants.QUERY_ID).required().asString();
            String queryExpression = connection.getQueryExpression(queryId);
            if (QueryConstants.QUERY_ALL_IDS.equals(queryId) || null != queryExpression) {
                if (null == queryExpression) {
                    Matcher matcher = ServerContext.get().getMatcher();
                    queryExpression = "SELECT id FROM " + matcher.group(1);
                }
                ClientResource rc = connection.getChild(sb.toString());

                rc.getReference().addQueryParameter("q", queryExpression);

                rc.setMethod(org.restlet.data.Method.GET);
                handleRequest(rc, true);
                Representation body = rc.getResponse().getEntity();

                List<Map<String, Object>> queryResult = new ArrayList<Map<String, Object>>();
                JsonValue searchResult = new JsonValue(new HashMap<String, Object>(1));
                searchResult.put(QueryConstants.QUERY_RESULT, queryResult);

                if (null != body && body instanceof EmptyRepresentation == false) {
                    JacksonRepresentation<Map> rep =
                            new JacksonRepresentation<Map>(body, Map.class);
                    JsonValue result = new JsonValue(rep.getObject());
                    for (JsonValue record : result.get("records")) {
                        if (QueryConstants.QUERY_ALL_IDS.equals(queryId)) {
                            Map<String, Object> r = new HashMap<String, Object>(1);
                            r.put(ServerConstants.OBJECT_PROPERTY_ID, record.get("Id").asString());
                            queryResult.add(r);
                        } else {
                            if (record.isDefined("Id")) {
                                record.put(ServerConstants.OBJECT_PROPERTY_ID, record.get("Id")
                                        .asString());
                            }
                            queryResult.add(record.asMap());

                        }
                    }
                }
                return searchResult;

            }
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                    "Unsupported queryId");
        }
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

            ClientResource rc = getClientResource(ids[0], ids[1]);
            rc.setMethod(SalesforceConnection.PATCH);

            JsonValue update = new JsonValue(describe.beforeUpdate(request.get("value")));
            logger.info("Update sobjects/{} \n content: \n{}\n", type, update);

            rc.getRequest().setEntity(new JacksonRepresentation<Map>(update.asMap()));

            handleRequest(rc, true);

            Representation body = rc.getResponse().getEntity();
            if (null != body && body instanceof EmptyRepresentation == false) {
                JacksonRepresentation<Map> rep = new JacksonRepresentation<Map>(body, Map.class);
                JsonValue result = new JsonValue(rep.getObject());
                // result.put(ServerConstants.OBJECT_PROPERTY_ID,
                // result.get("Id").required().asString());
                return result;
            } else {
                JsonValue result = new JsonValue(new HashMap<String, Object>());
                result.put(ServerConstants.OBJECT_PROPERTY_ID, ids[1]);
                return result;
            }
            // JacksonRepresentation<Map> rep = new
            // JacksonRepresentation<Map>(rc.handle(), Map.class);
            // JsonValue result = new JsonValue(rep.getObject());
            // return new JsonValue(new HashMap<String,Object>());
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
        }
    }

    @Override
    protected JsonValue delete(JsonValue request) throws JsonResourceException {
        String[] ids = getIds();
        if (ids.length == 2) {
            ClientResource rc = getClientResource(ids[0], ids[1]);
            rc.setMethod(org.restlet.data.Method.DELETE);
            handleRequest(rc, true);
            Representation body = rc.getResponse().getEntity();
            if (null != body && body instanceof EmptyRepresentation == false) {
                JacksonRepresentation<Map> rep = new JacksonRepresentation<Map>(body, Map.class);
                JsonValue result = new JsonValue(rep.getObject());
                // result.put(ServerConstants.OBJECT_PROPERTY_ID,
                // result.get("Id").required().asString());
                return result;
            } else {
                JsonValue result = new JsonValue(new HashMap<String, Object>());
                result.put(ServerConstants.OBJECT_PROPERTY_ID, ids[1]);
                return result;
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
                    ClientResource rc = getClientResource(type, "describe");
                    rc.setMethod(org.restlet.data.Method.GET);

                    handleRequest(rc, true);
                    Representation body = rc.getResponse().getEntity();
                    if (null != body && body instanceof EmptyRepresentation == false) {
                        try {
                            JacksonRepresentation<Map> rep =
                                    new JacksonRepresentation<Map>(body, Map.class);
                            describe = SObjectDescribe.newInstance(rep.getObject());
                        } catch (Exception e) {
                            logger.error("Failed to parse the Describe from: {}, response: ", rc
                                    .getReference(), body);
                        }
                    }
                    if (null == describe) {
                        throw new JsonResourceException(JsonResourceException.NOT_FOUND,
                                "Metadata not found for type: " + type);
                    } else {
                        schema.put(type, describe);
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
            // throw new ResourceException(response.getStatus());
        }

    }

    // private JsonResourceException getJsonResourceException(ClientResource rc)
    // {
    // JsonResourceException jre =
    // new JsonResourceException(rc.getResponse().getStatus().getCode(),
    // rc.getResponse()
    // .getStatus().getDescription());
    // Representation error = rc.getResponse().getEntity();
    // if (null != error) {
    // Map<String, Object> details = new HashMap<String, Object>();
    // for (Object o : new JacksonRepresentation<List>(error,
    // List.class).getObject()) {
    // if (o instanceof Map) {
    // details.put((String) ((Map) o).get("errorCode"), o);
    // }
    // }
    // jre.setDetail(details);
    // }
    // logger.error("Remote REST error: \n{}\n", jre.toJsonValue().toString());
    // return jre;
    // }

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

    // Custom

    private void updateCollaborationGroupMemberships(String collaborationGroup, List<Object> members)
            throws JsonResourceException {
        JsonValue request = new JsonValue(new HashMap<String, Object>());
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put(
                QueryConstants.QUERY_EXPRESSION,
                "SELECT Id, MemberId, CollaborationGroup.OwnerId from CollaborationGroupMember where CollaborationGroupId = '"
                        + collaborationGroup + "'");
        request.put("params", params);
        JsonValue response = query(request);

        for (JsonValue item : response.get(QueryConstants.QUERY_RESULT)) {
            if (!members.remove(members.contains(item.get("MemberId").asString()))) {
                if (item.get("MemberId").asString().equals(
                        item.get(new JsonPointer("CollaborationGroup/OwnerId")).asString())) {
                    logger.info("Skip to remove the owner");
                    break;
                }
                logger.info(
                        "Attempt to delete member: sobjects/User/{} from sobjects/CollaborationGroup/{}",
                        item.get("MemberId").asString(), collaborationGroup);
                request = new JsonValue(new HashMap<String, Object>());
                request.put("id", "sobjects/CollaborationGroupMember/" + item.get("Id").asString());

                try {
                    delete(request);
                } catch (JsonResourceException e) {
                    logger.error(
                            "Failed to delete member: sobjects/User/{} from sobjects/CollaborationGroup/{}",
                            item.get("MemberId").asString(), collaborationGroup);
                }
            }
        }

        for (Object newMember : members) {
            logger.info(
                    "Attempt to add new member: sobjects/User/{} to sobjects/CollaborationGroup/{}",
                    newMember, collaborationGroup);
            Map value = new HashMap<String, Object>();
            value.put("CollaborationGroupId", collaborationGroup);
            value.put("MemberId", newMember);
            value.put("CollaborationRole", "Standard");
            value.put("NotificationFrequency", "N");
            request = new JsonValue(new HashMap<String, Object>());
            request.put("id", "sobjects/CollaborationGroupMember");
            request.put("value", value);
            try {
                create(request);
            } catch (JsonResourceException e) {
                logger.info(
                        "Failed to add new member: sobjects/User/{} to sobjects/CollaborationGroup/{}",
                        newMember, collaborationGroup);
            }
        }
    }

    private void updateGroupMemberships(String group, List<Object> members)
            throws JsonResourceException {
        JsonValue request = new JsonValue(new HashMap<String, Object>());
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put(QueryConstants.QUERY_EXPRESSION,
                "SELECT Id, UserOrGroupId from GroupMember where GroupId = '" + group + "'");
        request.put("params", params);
        JsonValue response = query(request);

        for (JsonValue item : response.get(QueryConstants.QUERY_RESULT)) {
            if (!members.remove(members.contains(item.get("UserOrGroupId").asString()))) {
                logger.info("Attempt to delete member: sobjects/User/{} from sobjects/Group/{}",
                        item.get("UserOrGroupId").asString(), group);
                request = new JsonValue(new HashMap<String, Object>());
                request.put("id", "sobjects/GroupMember/" + item.get("Id").asString());

                try {
                    delete(request);
                } catch (JsonResourceException e) {
                    logger.error(
                            "Failed to delete member: sobjects/User/{} from sobjects/Group/{}",
                            item.get("UserOrGroupId").asString(), group);
                }
            }
        }

        for (Object newMember : members) {
            logger.info("Attempt to add new member: sobjects/User/{} to sobjects/Group/{}",
                    newMember, group);
            Map value = new HashMap<String, Object>();
            value.put("GroupId", group);
            value.put("UserOrGroupId", newMember);
            request = new JsonValue(new HashMap<String, Object>());
            request.put("id", "sobjects/GroupMember");
            request.put("value", value);
            try {
                create(request);
            } catch (JsonResourceException e) {
                logger.info("Failed to add new member: sobjects/User/{} to sobjects/Group/{}",
                        newMember, group);
            }
        }
    }

    private void updatePermissionSetAssignments(String permissionSet, List<Object> members)
            throws JsonResourceException {
        JsonValue request = new JsonValue(new HashMap<String, Object>());
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put(QueryConstants.QUERY_EXPRESSION,
                "SELECT Id, AssigneeId from PermissionSetAssignment where PermissionSetId = '"
                        + permissionSet + "'");
        request.put("params", params);
        JsonValue response = query(request);

        for (JsonValue item : response.get(QueryConstants.QUERY_RESULT)) {
            if (!members.remove(members.contains(item.get("AssigneeId").asString()))) {

                logger.info(
                        "Attempt to delete member: sobjects/User/{} from sobjects/PermissionSet/{}",
                        item.get("AssigneeId").asString(), permissionSet);
                request = new JsonValue(new HashMap<String, Object>());
                request.put("id", "sobjects/PermissionSetAssignment/" + item.get("Id").asString());

                try {
                    delete(request);
                } catch (JsonResourceException e) {
                    logger.error(
                            "Failed to delete member: sobjects/User/{} from sobjects/PermissionSet/{}",
                            item.get("AssigneeId").asString(), permissionSet);
                }
            }
        }

        for (Object newMember : members) {
            logger.info("Attempt to add new member: sobjects/User/{} to sobjects/PermissionSet/{}",
                    newMember, permissionSet);
            Map value = new HashMap<String, Object>();
            value.put("PermissionSetId", permissionSet);
            value.put("AssigneeId", newMember);
            request = new JsonValue(new HashMap<String, Object>());
            request.put("id", "sobjects/PermissionSetAssignment");
            request.put("value", value);
            try {
                create(request);
            } catch (JsonResourceException e) {
                logger.info(
                        "Failed to add new member: sobjects/User/{} to sobjects/PermissionSet/{}",
                        newMember, permissionSet);
            }
        }
    }
}
