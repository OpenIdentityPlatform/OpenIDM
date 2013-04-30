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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.salesforce.internal.SalesforceConfiguration;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.util.ResourceUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
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
@Component(name = SalesforceRequestHandler.PID, immediate = true,
        policy = ConfigurationPolicy.REQUIRE)
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Salesforce sample"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "salesforce") })
public class SalesforceRequestHandler implements CollectionResourceProvider {

    public static final String PID = "org.forgerock.openidm.salesforce";

    /**
     * Setup logging for the {@link SalesforceRequestHandler}.
     */
    final static Logger logger = LoggerFactory.getLogger(SalesforceRequestHandler.class);

    private SalesforceConnection connection = null;

    private final ConcurrentMap<String, SObjectDescribe> schema =
            new ConcurrentHashMap<String, SObjectDescribe>();

    private ServiceRegistration<CollectionResourceProvider> sobjectsProviderRegistration;

    private final QueryResource queryResource = new QueryResource();

    private ServiceRegistration<RequestHandler> queryHandlerRegistration;

    @Activate
    void activate(ComponentContext context) throws Exception {

        Dictionary properties = context.getProperties();
        EnhancedConfig config = JSONEnhancedConfig.newInstance();

        String factoryPid = config.getConfigurationFactoryPid(context);
        if (StringUtils.isBlank(factoryPid)) {
            throw new IllegalArgumentException("Configuration must have property: "
                    + ServerConstants.CONFIG_FACTORY_PID);
        }

        properties.put(ServerConstants.ROUTER_PREFIX, "/salesforce/" + factoryPid
                + "/sobjects/{partition}");

        JsonValue configuration = config.getConfigurationAsJson(context);
        configuration.put(ServerConstants.CONFIG_FACTORY_PID, factoryPid);

        connection =
                new SalesforceConnection(parseConfiguration(configuration
                        .get("configurationProperties")));
        connection.test();

        sobjectsProviderRegistration =
                context.getBundleContext().registerService(CollectionResourceProvider.class, this,
                        properties);

        properties = new Hashtable();
        properties.put(ServerConstants.ROUTER_PREFIX, "/salesforce/" + factoryPid + "/query");

        queryHandlerRegistration =
                context.getBundleContext().registerService(RequestHandler.class, queryResource,
                        properties);

        logger.info("OAUTH Token: {}", connection.getOAuthUser().getAuthorization());

    }

    @Deactivate
    void deactivate(ComponentContext context) throws Exception {
        if (null != connection) {
            connection.dispose();
            connection = null;
        }
        if (null != sobjectsProviderRegistration) {
            sobjectsProviderRegistration.unregister();
            sobjectsProviderRegistration = null;
        }
    }

    protected String getPartition(ServerContext context) throws ResourceException {
        Map<String, String> variables = ResourceUtil.getUriTemplateVariables(context);
        if (null != variables && variables.containsKey("partition")) {
            return variables.get("partition");
        }
        throw new ForbiddenException("Direct access without Router to this service is forbidden.");
    }

    public static SalesforceConfiguration parseConfiguration(JsonValue config) {
        return SalesforceConnection.mapper.convertValue(
                config.required().expect(Map.class).asMap(), SalesforceConfiguration.class);
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler) {
        try {
            String type = getPartition(context);
            SObjectDescribe describe = getSObjectDescribe(type);
            if (null != describe) {
                ClientResource rc = getClientResource("sobjects/" + type);
                JsonValue create = new JsonValue(describe.beforeCreate(request.getContent()));
                logger.error("Create sobjects/{} \n content: \n{}\n", type, create);

                rc.getRequest().setEntity(new JacksonRepresentation<Map>(create.asMap()));
                rc.setMethod(org.restlet.data.Method.POST);
                handleRequest(rc, true);
                Representation body = rc.getResponse().getEntity();
                if (null != body && body instanceof EmptyRepresentation == false) {
                    JacksonRepresentation<Map> rep =
                            new JacksonRepresentation<Map>(body, Map.class);
                    JsonValue result = new JsonValue(rep.getObject());
                    if (result.get("success").asBoolean()) {
                        result.put(Resource.FIELD_CONTENT_ID, result.get("id").getObject());
                        try {
                            if ("CollaborationGroup".equals(type)) {
                                JsonValue members = request.getContent().get("value").get("member");
                                // TODO Get the ID from response
                                if (members.isList()) {
                                    updateGroupMemberships(context, null/*
                                                                         * id.
                                                                         * substring
                                                                         * (id .
                                                                         * lastIndexOf
                                                                         * ('/'
                                                                         * ) +
                                                                         * 1)
                                                                         */, members.asList());
                                }
                            }
                        } catch (ResourceException e) {
                            logger.debug("Failed to update members");
                        }
                    }
                    if (result.isDefined("errors")) {
                        if (result.get("errors").size() > 0) {
                            handler.handleError(new InternalServerErrorException(
                                    "Failed to create FIX ME"));
                        }
                    }
                } else {
                    handler.handleError(new InternalServerErrorException("Failed to create FIX ME?"));
                }
            } else {
                handler.handleError(new BadRequestException("Type not supported: " + type));
            }
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
            ResultHandler<Resource> handler) {
        try {
            String type = getPartition(context);
            ClientResource rc = getClientResource("/sobjects/" + type + "/" + resourceId);
            handleRequest(rc, true);
            Representation body = rc.getResponse().getEntity();
            if (null != body && body instanceof EmptyRepresentation == false) {
                JacksonRepresentation<Map> rep = new JacksonRepresentation<Map>(body, Map.class);
                JsonValue result = new JsonValue(rep.getObject());
                if (result.isDefined("Id")) {
                    result.put(Resource.FIELD_CONTENT_ID, result.get("Id").required().asString());
                }
                handler.handleResult(new Resource(result.get("Id").asString(), result.get(
                        "Revision").asString(), result));
            } else {
                handler.handleError(new NotFoundException());
            }
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        try {
            if (request.getQueryId() != null) {
                if (ServerConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                    String type = getPartition(context);
                    SObjectDescribe describe = getSObjectDescribe(type);
                    if (null != describe) {

                        ClientResource rc = getClientResource("query");

                        // TODO add fileds to the query
                        rc.getReference().addQueryParameter("q", "SELECT id FROM " + type);

                        rc.setMethod(org.restlet.data.Method.GET);
                        handleRequest(rc, true);
                        Representation body = rc.getResponse().getEntity();

                        if (null != body && body instanceof EmptyRepresentation == false) {
                            JacksonRepresentation<Map> rep =
                                    new JacksonRepresentation<Map>(body, Map.class);
                            JsonValue result = new JsonValue(rep.getObject());
                            for (JsonValue record : result.get("records")) {
                                Map<String, Object> r = new HashMap<String, Object>(1);
                                r.put(Resource.FIELD_CONTENT_ID, record.get("Id").asString());
                                // TODO Common method
                                Resource resource =
                                        new Resource(record.get("Id").asString(), record.get(
                                                "Revision").asString(), record);
                                handler.handleResource(resource);
                            }
                        }
                        // TODO support paging
                        handler.handleResult(new QueryResult());
                    } else {
                        handler.handleError(new BadRequestException("Type not supported: " + type));
                    }
                } else {
                    handler.handleError(new BadRequestException("Unsupported QueryID"
                            + request.getQueryId()));
                }
            } else {
                queryResource.handleQuery(context, request, handler);
            }
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
            ResultHandler<Resource> handler) {
        try {
            String type = getPartition(context);
            SObjectDescribe describe = getSObjectDescribe(type);
            if (null != describe) {

                String id = "sobjects" + type + "/" + resourceId;

                ClientResource rc = getClientResource(id);
                rc.setMethod(SalesforceConnection.PATCH);

                JsonValue update = new JsonValue(describe.beforeUpdate(request.getNewContent()));
                logger.info("Update sobjects/{} \n content: \n{}\n", type, update);

                rc.getRequest().setEntity(new JacksonRepresentation<Map>(update.asMap()));

                handleRequest(rc, true);

                if ("CollaborationGroup".equals(type)) {
                    JsonValue members = request.getNewContent().get("member");
                    if (members.isList()) {
                        updateGroupMemberships(context, id.substring(id.lastIndexOf('/') + 1),
                                members.asList());
                    }
                }

                Representation body = rc.getResponse().getEntity();
                if (null != body && body instanceof EmptyRepresentation == false) {
                    JacksonRepresentation<Map> rep =
                            new JacksonRepresentation<Map>(body, Map.class);
                    JsonValue result = new JsonValue(rep.getObject());
                    // result.put(Resource.FIELD_CONTENT_ID,
                    // result.get("Id").required().asString());
                    result.put(Resource.FIELD_CONTENT_ID, resourceId);
                    handler.handleResult(new Resource(resourceId, request.getRevision(), result));
                } else {

                    JsonValue result = new JsonValue(new HashMap<String, Object>());
                    result.put(Resource.FIELD_CONTENT_ID, resourceId);
                    handler.handleResult(new Resource(resourceId, request.getRevision(), result));
                }
            } else {
                handler.handleError(new BadRequestException("Type not supported: " + type));
            }
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
            ResultHandler<Resource> handler) {
        try {
            String type = getPartition(context);
            SObjectDescribe describe = getSObjectDescribe(type);
            if (null != describe) {
                String id = "sobjects" + type + "/" + resourceId;

                ClientResource rc = getClientResource(id);
                rc.setMethod(org.restlet.data.Method.DELETE);
                handleRequest(rc, true);
                Representation body = rc.getResponse().getEntity();
                if (null != body && body instanceof EmptyRepresentation == false) {
                    JacksonRepresentation<Map> rep =
                            new JacksonRepresentation<Map>(body, Map.class);
                    JsonValue result = new JsonValue(rep.getObject());
                    // result.put(Resource.FIELD_CONTENT_ID,
                    // result.get("Id").required().asString());

                    result.put(Resource.FIELD_CONTENT_ID, resourceId);
                    handler.handleResult(new Resource(resourceId, request.getRevision(), result));
                } else {

                    JsonValue result = new JsonValue(new HashMap<String, Object>());
                    result.put(Resource.FIELD_CONTENT_ID, resourceId);
                    handler.handleResult(new Resource(resourceId, request.getRevision(), result));
                }

            } else {
                handler.handleError(new BadRequestException("Type not supported: " + type));
            }
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource collection");
        handler.handleError(e);
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    protected SObjectDescribe getSObjectDescribe(String type) throws ResourceException {
        SObjectDescribe describe = schema.get(type);
        if (null == describe) {
            synchronized (schema) {
                describe = schema.get(type);
                if (null == describe) {
                    ClientResource rc = getClientResource("sobjects/" + type + "/describe");
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
                        throw new NotFoundException("Metadata not found for type: " + type);
                    } else {
                        schema.put(type, describe);
                    }
                }
            }
        }
        return describe;
    }

    protected void handleRequest(final ClientResource resource, boolean tryReauth)
            throws ResourceException {
        try {
            resource.handle();
        } catch (Exception e) {
            throw new InternalServerErrorException(e);
        }
        final Response response = resource.getResponse();
        if (response.getStatus().isError()) {

            ResourceException e = connection.getResourceException(resource);

            if (tryReauth && Status.CLIENT_ERROR_UNAUTHORIZED.equals(response.getStatus())) {
                // Re authenticate
                if (connection.refreshAccessToken(resource.getRequest())) {
                    handleRequest(resource, false);
                } else {
                    throw ResourceException.getException(401, "AccessToken can not be renewed");
                }
            } else {
                throw e;
            }
            // throw new ResourceException(response.getStatus());
        }

    }

    // private ResourceException getResourceException(ClientResource rc)
    // {
    // ResourceException jre =
    // new ResourceException(rc.getResponse().getStatus().getCode(),
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

    private ClientResource getClientResource(String id) {
        return connection.getChild(id == null ? "services/data/" + connection.getVersion()
                : "services/data/" + connection.getVersion() + "/" + id);
    }

    // Custom

    private void updateGroupMemberships(final ServerContext context,
            final String collaborationGroup, final List<Object> members) throws ResourceException {

        QueryRequest request = Requests.newQueryRequest("/");
        request.setQueryExpression("SELECT Id, MemberId, CollaborationGroup.OwnerId from CollaborationGroupMember where CollaborationGroupId = '"
                + collaborationGroup + "'");

        queryResource.handleQuery(context, request, new QueryResultHandler() {
            @Override
            public void handleError(ResourceException error) {

            }

            @Override
            public boolean handleResource(final Resource resource) {

                if (!members.remove(members.contains(resource.getContent().get("MemberId")
                        .asString()))) {
                    if (resource.getContent().get("MemberId").asString().equals(
                            resource.getContent()
                                    .get(new JsonPointer("CollaborationGroup/OwnerId")).asString())) {
                        logger.info("Skip to remove the owner");
                        return true;
                    }
                    logger.info(
                            "Attempt to delete member: sobjects/user/{} from sobjects/CollaborationGroup/{}",
                            resource.getContent().get("MemberId").asString(), collaborationGroup);

                    // request = new JsonValue(new HashMap<String, Object>());
                    // request.put("id", "sobjects/CollaborationGroupMember/" +
                    // resource.getContent().get("Id").asString());
                    //
                    // try {
                    // //TODO fix the type
                    // delete(context, null);
                    // } catch (ResourceException e) {
                    // logger.error(
                    // "Failed to delete member: sobjects/user/{} from sobjects/CollaborationGroup/{}",
                    // resource.getContent().get("MemberId").asString(),
                    // collaborationGroup);
                    // }
                }

                return false; // To change body of implemented methods use File
                              // | Settings | File Templates.
            }

            @Override
            public void handleResult(QueryResult result) {

            }
        });

        for (Object newMember : members) {
            logger.info(
                    "Attempt to add new member: sobjects/user/{} to sobjects/CollaborationGroup/{}",
                    newMember, collaborationGroup);
            Map value = new HashMap<String, Object>();
            value.put("CollaborationGroupId", collaborationGroup);
            value.put("MemberId", newMember);
            value.put("CollaborationRole", "Standard");
            value.put("NotificationFrequency", "N");
            // request = new JsonValue(new HashMap<String, Object>());
            // request.put("id", "sobjects/CollaborationGroupMember");
            // request.put("value", value);
            // try {
            // create(request);
            // } catch (ResourceException e) {
            // logger.info(
            // "Failed to add new member: sobjects/user/{} to sobjects/CollaborationGroup/{}",
            // newMember, collaborationGroup);
            // }
        }
    }

    public class QueryResource implements RequestHandler {

        public void handleQuery(ServerContext context, QueryRequest request,
                QueryResultHandler handler) {
            try {
                if (null != request.getQueryExpression()) {
                    ClientResource rc = getClientResource("query");

                    rc.getReference().addQueryParameter("q", request.getQueryExpression());
                    rc.setMethod(org.restlet.data.Method.GET);
                    logger.debug("Attempt to execute query: {}?{}", rc.getReference(), rc
                            .getReference().getQuery());
                    handleRequest(rc, true);
                    Representation body = rc.getResponse().getEntity();

                    if (null != body && body instanceof EmptyRepresentation == false) {
                        JacksonRepresentation<Map> rep =
                                new JacksonRepresentation<Map>(body, Map.class);
                        JsonValue result = new JsonValue(rep.getObject());
                        for (JsonValue record : result.get("records")) {
                            record.put(Resource.FIELD_CONTENT_ID, record.get("Id").asString());
                            // TODO Common method
                            Resource resource =
                                    new Resource(record.get("Id").asString(), record
                                            .get("Revision").asString(), record);
                            handler.handleResource(resource);
                        }
                    }
                    // TODO support paging
                    handler.handleResult(new QueryResult());
                } else if (null != request) {
                    handler.handleError(new NotSupportedException(
                            "queryId and queryFilter is not yet supported"));
                }
            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        public void handleAction(ServerContext context, ActionRequest request,
                ResultHandler<JsonValue> handler) {
            final ResourceException e =
                    new NotSupportedException("Actions are not supported for resource instances");
            handler.handleError(e);
        }

        public void handleCreate(ServerContext context, CreateRequest request,
                ResultHandler<Resource> handler) {
            final ResourceException e =
                    new NotSupportedException("Create operations are not supported");
            handler.handleError(e);
        }

        public void handleDelete(ServerContext context, DeleteRequest request,
                ResultHandler<Resource> handler) {
            final ResourceException e =
                    new NotSupportedException("Delete operations are not supported");
            handler.handleError(e);
        }

        public void handlePatch(ServerContext context, PatchRequest request,
                ResultHandler<Resource> handler) {
            final ResourceException e =
                    new NotSupportedException("Patch operations are not supported");
            handler.handleError(e);
        }

        public void handleRead(ServerContext context, ReadRequest request,
                ResultHandler<Resource> handler) {
            final ResourceException e =
                    new NotSupportedException("Read operations are not supported");
            handler.handleError(e);
        }

        public void handleUpdate(ServerContext context, UpdateRequest request,
                ResultHandler<Resource> handler) {
            final ResourceException e =
                    new NotSupportedException("Update operations are not supported");
            handler.handleError(e);
        }

    }
}
