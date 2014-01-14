/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.salesforce.internal.data;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
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
import org.forgerock.json.resource.ResourceName;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.router.RouteBuilder;
import org.forgerock.openidm.router.RouteEntry;
import org.forgerock.openidm.router.RouterRegistry;
import org.forgerock.openidm.salesforce.internal.SalesforceConfiguration;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.salesforce.internal.async.AsyncBatchResourceProvider;
import org.forgerock.openidm.salesforce.internal.async.AsyncBatchResultResourceProvider;
import org.forgerock.openidm.salesforce.internal.async.AsyncJobResourceProvider;
import org.forgerock.openidm.salesforce.internal.metadata.MetadataResourceProvider;
import org.forgerock.openidm.util.ResourceUtil;
import org.osgi.framework.Constants;
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
 * A SalesforceRequestHandler is an adapter for Salesforce Data API.
 *
 * @author Laszlo Hordos
 */
@Component(name = SalesforceRequestHandler.PID, immediate = true,
        policy = ConfigurationPolicy.REQUIRE)
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Salesforce sample") })
public class SalesforceRequestHandler implements CollectionResourceProvider {

    public static final String PID = "org.forgerock.openidm.salesforce";

    /**
     * Setup logging for the {@link SalesforceRequestHandler}.
     */
    final static Logger logger = LoggerFactory.getLogger(SalesforceRequestHandler.class);

    private static final String REVISION_FIELD = "LastModifiedDate";

    /**
     * RouterRegistryService service.
     */
    @Reference(policy = ReferencePolicy.STATIC)
    protected RouterRegistry routerRegistryService;

    private SalesforceConnection connection = null;

    private final ConcurrentMap<String, SObjectDescribe> schema =
            new ConcurrentHashMap<String, SObjectDescribe>();

    private final QueryResource queryResource = new QueryResource();

    /* Internal routing objects to register and remove the routes. */
    private RouteEntry routeEntry;

    String organizationName = null;

    @Activate
    void activate(ComponentContext context) throws Exception {

        EnhancedConfig enhancedConfig = JSONEnhancedConfig.newInstance();

        organizationName = enhancedConfig.getConfigurationFactoryPid(context);
        if (StringUtils.isBlank(organizationName)) {
            logger.warn("Configuration invalid, factory configuration expected");
            throw new IllegalArgumentException("Configuration must have property: "
                    + ServerConstants.CONFIG_FACTORY_PID);
        }
        JsonValue configuration = null;

        try {
            configuration = enhancedConfig.getConfigurationAsJson(context);
        } catch (Exception ex) {
            logger.warn(
                    "Configuration invalid and could not be parsed, can not start Salesforce Connector: "
                            + ex.getMessage(), ex);
            throw ex;
        }

        if (!configuration.get("enabled").defaultTo(true).asBoolean()) {
            logger.info(
                    "Salesforce Connector {} is disabled, \"enabled\" set to false in configuration",
                    organizationName);
            return;
        }

        SalesforceConfiguration config = null;
        try {
            config = parseConfiguration(configuration.get("configurationProperties"));
        } catch (Exception ex) {
            // Invalid configuration value, permanent error.
            logger.warn(
                    "Configuration invalid and could not be used, can not start Salesforce Connector: "
                            + ex.getMessage(), ex);
            throw ex;
        }

        try {
            connection = new SalesforceConnection(config);
            connection.test();
        } catch (ResourceException e) {
            // authenticate or the test throws this, temporary error
            logger.warn("TODO define message that explains that the service is temporary not available now, can not start Salesforce Connector: ");
        } catch (Exception ex) {
            // Invalid configuration value, permanent error.
            logger.warn(
                    "Configuration invalid and could not be validated, can not start Salesforce Connector: "
                            + ex.getMessage(), ex);
            throw ex;
        }

        ResourceName system = new ResourceName("system", organizationName);

        RouteBuilder builder = RouteBuilder.newBuilder();

        if (false) {
            // TODO If the Bug with "/" resourceName fixed then change to this
            Router root = new Router();

            root.addRoute("/", new SystemResourceProvider());
            root.addRoute("async/job", new AsyncJobResourceProvider(connection));

            builder.withTemplate(system.toString()).withModeStartsWith().withRequestHandler(root)
                    .buildNext();
        } else {

            builder.withTemplate(system.toString()).withSingletonResourceProvider(
                    new SystemResourceProvider()).buildNext();

            builder.withTemplate(system.concat("async/job").toString())
                    .withCollectionResourceProvider(new AsyncJobResourceProvider(connection))
                    .buildNext();

            builder.withTemplate(system.concat("async/job/{jobId}/batch").toString())
                    .withCollectionResourceProvider(new AsyncBatchResourceProvider(connection))
                    .buildNext();

            builder.withTemplate(
                    system.concat("async/job/{jobId}/batch/{batchId}/result").toString())
                    .withCollectionResourceProvider(
                            new AsyncBatchResultResourceProvider(connection)).buildNext();

            builder.withTemplate(system.concat("sobjects/{partition}").toString())
                    .withCollectionResourceProvider(this).buildNext();

            builder.withTemplate(system.concat("licensing").toString())
                    .withCollectionResourceProvider(new GenericResourceProvider("licensing"))
                    .buildNext();
            builder.withTemplate(system.concat("connect").toString())
                    .withCollectionResourceProvider(new GenericResourceProvider("connect"))
                    .buildNext();
            builder.withTemplate(system.concat("search").toString())
                    .withCollectionResourceProvider(new GenericResourceProvider("search"))
                    .buildNext();
            builder.withTemplate(system.concat("tooling").toString())
                    .withCollectionResourceProvider(new GenericResourceProvider("tooling"))
                    .buildNext();
            builder.withTemplate(system.concat("chatter").toString())
                    .withCollectionResourceProvider(new GenericResourceProvider("chatter"))
                    .buildNext();
            builder.withTemplate(system.concat("recent").toString())
                    .withCollectionResourceProvider(new GenericResourceProvider("recent"))
                    .buildNext();

            builder.withTemplate(system.concat("query").toString()).withRequestHandler(
                    queryResource).buildNext();

            builder.withTemplate(system.concat("metadata/{metadataType}").toString())
                    .withCollectionResourceProvider(new MetadataResourceProvider(connection))
                    .buildNext();
        }

        routeEntry = routerRegistryService.addRoute(builder.seal());
    }

    @Deactivate
    void deactivate(ComponentContext context) throws Exception {
        if (null != routeEntry) {
            routeEntry.removeRoute();
            routeEntry = null;
        }
        if (null != connection) {
            connection.dispose();
            connection = null;
        }
        // TODO Dispose CometD
    }

    public static SalesforceConfiguration parseConfiguration(JsonValue config) {
        return SalesforceConnection.mapper.convertValue(
                config.required().expect(Map.class).asMap(), SalesforceConfiguration.class);
    }

    protected String getPartition(ServerContext context) throws ResourceException {
        Map<String, String> variables = ResourceUtil.getUriTemplateVariables(context);
        if (null != variables && variables.containsKey("partition")) {
            return variables.get("partition");
        }
        throw new ForbiddenException("Direct access without Router to this service is forbidden.");
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler) {
        try {
            String type = getPartition(context);
            SObjectDescribe describe = getSObjectDescribe(type);
            if (null != describe) {
                final ClientResource cr = getClientResource(type, null);
                try {
                    JsonValue create = new JsonValue(describe.beforeCreate(request.getContent()));
                    logger.trace("Create sobjects/{} \n content: \n{}\n", type, create);

                    cr.getRequest().setEntity(new JacksonRepresentation<Map>(create.asMap()));
                    cr.setMethod(org.restlet.data.Method.POST);
                    handleRequest(cr, true);
                    Representation body = cr.getResponse().getEntity();
                    if (null != body && body instanceof EmptyRepresentation == false) {
                        JacksonRepresentation<Map> rep =
                                new JacksonRepresentation<Map>(body, Map.class);
                        JsonValue result = new JsonValue(rep.getObject());
                        String id = null;
                        String revison = null;
                        if (result.get("success").asBoolean()) {
                            id = result.get("id").asString();
                            revison = "";
                            result.put(Resource.FIELD_CONTENT_ID, result.get("id").getObject());
                        }
                        if (result.isDefined("errors")) {
                            if (result.get("errors").size() > 0) {
                                handler.handleError(new InternalServerErrorException(
                                        "Failed to create FIX ME"));
                            }
                        } else {
                            handler.handleResult(new Resource(id, revison, result));
                        }
                    } else {
                        handler.handleError(new InternalServerErrorException(
                                "Failed to create FIX ME?"));
                    }
                } finally {
                    if (null != cr) {
                        cr.release();
                    }
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
            final ClientResource cr = getClientResource(type, resourceId);
            try {
                handleRequest(cr, true);
                Representation body = cr.getResponse().getEntity();
                if (null != body && body instanceof EmptyRepresentation == false) {
                    JacksonRepresentation<Map> rep =
                            new JacksonRepresentation<Map>(body, Map.class);
                    JsonValue result = new JsonValue(rep.getObject());
                    if (result.isDefined("Id")) {
                        result.put(Resource.FIELD_CONTENT_ID, result.get("Id").required()
                                .asString());
                    }
                    handler.handleResult(new Resource(result.get("Id").asString(), result.get(
                            REVISION_FIELD).asString(), result));
                } else {
                    handler.handleError(new NotFoundException());
                }
            } finally {
                if (null != cr) {
                    cr.release();
                }
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

                String queryExpression = connection.getQueryExpression(request.getQueryId());
                if (ServerConstants.QUERY_ALL_IDS.equals(request.getQueryId())
                        || null != queryExpression) {
                    if (null == queryExpression) {
                        String type = getPartition(context);
                        queryExpression = "SELECT id FROM " + type;
                    }
                    final ClientResource rc = getClientResource("query");

                    rc.getReference().addQueryParameter("q", queryExpression);

                    rc.setMethod(org.restlet.data.Method.GET);
                    handleRequest(rc, true);
                    Representation body = rc.getResponse().getEntity();

                    if (null != body && body instanceof EmptyRepresentation == false) {
                        JacksonRepresentation<Map> rep =
                                new JacksonRepresentation<Map>(body, Map.class);
                        JsonValue result = new JsonValue(rep.getObject());
                        for (JsonValue record : result.get("records")) {
                            if (ServerConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                                Map<String, Object> r = new HashMap<String, Object>(1);
                                r.put(Resource.FIELD_CONTENT_ID, record.get("Id").asString());
                                Resource resource =
                                        new Resource(record.get("Id").asString(), record.get(
                                                REVISION_FIELD).asString(), record);
                                handler.handleResource(resource);
                            } else {
                                if (record.isDefined("Id")) {
                                    record.put(Resource.FIELD_CONTENT_ID, record.get("Id")
                                            .asString());
                                }
                                Resource resource =
                                        new Resource(record.get("Id").asString(), record.get(
                                                REVISION_FIELD).asString(), record);
                                handler.handleResource(resource);
                            }
                        }
                    }
                    // TODO support paging
                    handler.handleResult(new QueryResult());
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

                // String id = "sobjects" + type + "/" + resourceId;

                final ClientResource cr = getClientResource(type, resourceId);
                try {
                    cr.setMethod(SalesforceConnection.PATCH);

                    JsonValue update =
                            new JsonValue(describe.beforeUpdate(request.getNewContent()));
                    logger.trace("Update sobjects/{} \n content: \n{}\n", type, update);

                    cr.getRequest().setEntity(new JacksonRepresentation<Map>(update.asMap()));

                    handleRequest(cr, true);

                    Representation body = cr.getResponse().getEntity();
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
                } finally {
                    if (null != cr) {
                        cr.release();
                    }
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
            // SObjectDescribe describe = getSObjectDescribe(type);
            // if (null != describe) {

            final ClientResource cr = getClientResource(type, resourceId);
            try {
                cr.setMethod(org.restlet.data.Method.DELETE);
                handleRequest(cr, true);
                Representation body = cr.getResponse().getEntity();
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
            } finally {
                if (null != cr) {
                    cr.release();
                }
            }
            // } else {
            // handler.handleError(new
            // BadRequestException("Type not supported: " + type));
            // }
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        ResourceUtil.notSupportedOnCollection(request);
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        ResourceUtil.notSupportedOnInstance(request);
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
            ResultHandler<Resource> handler) {
        ResourceUtil.notSupportedOnInstance(request);
    }

    @SuppressWarnings("unchecked")
    protected SObjectDescribe getSObjectDescribe(String type) throws ResourceException {
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
                            throw new NotFoundException("Metadata not found for type: " + type);
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

    private ClientResource getClientResource(String id) {
        return connection.getChild(id == null ? "services/data/" + connection.getVersion()
                : "services/data/" + connection.getVersion() + "/" + id);
    }

    public class QueryResource implements RequestHandler {

        public void handleQuery(ServerContext context, QueryRequest request,
                QueryResultHandler handler) {
            try {
                if (null != request.getQueryExpression()) {
                    final ClientResource cr = getClientResource("query");
                    try {
                        cr.getReference().addQueryParameter("q", request.getQueryExpression());
                        cr.setMethod(org.restlet.data.Method.GET);
                        logger.debug("Attempt to execute query: {}?{}", cr.getReference(), cr
                                .getReference().getQuery());
                        handleRequest(cr, true);
                        Representation body = cr.getResponse().getEntity();

                        if (null != body && body instanceof EmptyRepresentation == false) {
                            JacksonRepresentation<Map> rep =
                                    new JacksonRepresentation<Map>(body, Map.class);
                            JsonValue result = new JsonValue(rep.getObject());
                            for (JsonValue record : result.get("records")) {
                                if (record.isDefined("Id")) {
                                    record.put(Resource.FIELD_CONTENT_ID, record.get("Id")
                                            .asString());
                                }
                                Resource resource =
                                        new Resource(record.get("Id").asString(), record.get(
                                                REVISION_FIELD).asString(), record);
                                handler.handleResource(resource);
                            }
                        }
                        // TODO support paging
                        handler.handleResult(new QueryResult());
                    } finally {
                        if (null != cr) {
                            cr.release();
                        }
                    }
                } else {
                    handler.handleError(new NotSupportedException(
                            "queryId and queryFilter is not yet supported"));
                }
            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        public void handleAction(ServerContext context, ActionRequest request,
                ResultHandler<JsonValue> handler) {
            ResourceUtil.notSupported(request);
        }

        public void handleCreate(ServerContext context, CreateRequest request,
                ResultHandler<Resource> handler) {
            ResourceUtil.notSupported(request);
        }

        public void handleDelete(ServerContext context, DeleteRequest request,
                ResultHandler<Resource> handler) {
            ResourceUtil.notSupported(request);
        }

        public void handlePatch(ServerContext context, PatchRequest request,
                ResultHandler<Resource> handler) {
            ResourceUtil.notSupported(request);
        }

        public void handleRead(ServerContext context, ReadRequest request,
                ResultHandler<Resource> handler) {
            ResourceUtil.notSupported(request);
        }

        public void handleUpdate(ServerContext context, UpdateRequest request,
                ResultHandler<Resource> handler) {
            ResourceUtil.notSupported(request);
        }

    }

    class GenericResourceProvider implements CollectionResourceProvider {

        private final String type;

        GenericResourceProvider(String type) {
            this.type = type;
        }

        @Override
        public void readInstance(ServerContext serverContext, String instanceName,
                ReadRequest readRequest, ResultHandler<Resource> resourceResultHandler) {
            try {

                final ClientResource cr = getClientResource(type, instanceName);
                try {
                    handleRequest(cr, true);
                    Representation body = cr.getResponse().getEntity();
                    if (null != body && body instanceof EmptyRepresentation == false) {
                        JacksonRepresentation<Map> rep =
                                new JacksonRepresentation<Map>(body, Map.class);
                        JsonValue result = new JsonValue(rep.getObject());
                        if (result.isDefined("Id")) {
                            result.put(Resource.FIELD_CONTENT_ID, result.get("Id").required()
                                    .asString());
                        }
                        resourceResultHandler.handleResult(new Resource(
                                result.get("Id").asString(), "", result));
                    } else {
                        resourceResultHandler.handleError(new NotFoundException(
                                "Resource not Found"));
                    }
                } finally {
                    if (null != cr) {
                        cr.release();
                    }
                }

            } catch (Throwable t) {
                resourceResultHandler.handleError(ResourceUtil.adapt(t));
            }
        }

        private ClientResource getClientResource(String type, String id) {
            StringBuilder sb = new StringBuilder("services/data/").append(connection.getVersion());
            if (null != type) {
                sb.append('/').append(type);
                if (null != id) {
                    sb.append('/').append(id);
                }
            }
            return connection.getChild(sb.toString());
        }

        @Override
        public void actionCollection(ServerContext serverContext, ActionRequest actionRequest,
                ResultHandler<JsonValue> jsonValueResultHandler) {
            ResourceUtil.notSupportedOnCollection(actionRequest);
        }

        @Override
        public void actionInstance(ServerContext serverContext, String s,
                ActionRequest actionRequest, ResultHandler<JsonValue> jsonValueResultHandler) {
            ResourceUtil.notSupportedOnInstance(actionRequest);
        }

        @Override
        public void createInstance(ServerContext serverContext, CreateRequest createRequest,
                ResultHandler<Resource> resourceResultHandler) {
            ResourceUtil.notSupportedOnInstance(createRequest);
        }

        @Override
        public void deleteInstance(ServerContext serverContext, String s,
                DeleteRequest deleteRequest, ResultHandler<Resource> resourceResultHandler) {
            ResourceUtil.notSupportedOnInstance(deleteRequest);
        }

        @Override
        public void patchInstance(ServerContext serverContext, String s, PatchRequest patchRequest,
                ResultHandler<Resource> resourceResultHandler) {
            ResourceUtil.notSupportedOnInstance(patchRequest);
        }

        @Override
        public void queryCollection(ServerContext serverContext, QueryRequest queryRequest,
                QueryResultHandler queryResultHandler) {
            ResourceUtil.notSupportedOnCollection(queryRequest);
        }

        @Override
        public void updateInstance(ServerContext serverContext, String s,
                UpdateRequest updateRequest, ResultHandler<Resource> resourceResultHandler) {
            ResourceUtil.notSupportedOnInstance(updateRequest);
        }
    }

    private class SystemResourceProvider implements SingletonResourceProvider {

        public final static String ACTION_TEST_CONFIGURATION = "testConfig";
        public final static String ACTION_TEST_CONNECTOR = "test";

        @Override
        public void actionInstance(ServerContext serverContext, ActionRequest actionRequest,
                ResultHandler<JsonValue> resultHandler) {
            try {

                if (ACTION_TEST_CONFIGURATION.equals(actionRequest.getAction())) {

                    JsonValue jv = new JsonValue(new LinkedHashMap<String, Object>());
                    jv.put("name", organizationName);
                    try {
                        parseConfiguration(
                                actionRequest.getContent().get("configurationProperties")
                                        .required()).validate();
                        jv.put("ok", true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        jv.put("ok", false);
                        jv.put("error", e.getMessage());
                    }
                    resultHandler.handleResult(jv);
                } else if (ACTION_TEST_CONNECTOR.equals(actionRequest.getAction())) {

                    JsonValue result = new JsonValue(new LinkedHashMap<String, Object>());
                    try {
                        result.put("name", organizationName);
                        ReadRequest readRequest =
                                Requests.newReadRequest("/system/" + organizationName
                                        + "/connect/organization");
                        serverContext.getConnection().read(serverContext, readRequest);
                        result.put("ok", true);
                    } catch (Throwable e) {
                        result.put("error", e.getMessage());
                        result.put("ok", false);
                    }
                    resultHandler.handleResult(result);
                } else {
                    resultHandler.handleError(new BadRequestException("Unsupported actionId"));
                }
            } catch (Throwable t) {
                resultHandler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void patchInstance(ServerContext serverContext, PatchRequest patchRequest,
                ResultHandler<Resource> resourceResultHandler) {
            ResourceUtil.notSupportedOnInstance(patchRequest);
        }

        @Override
        public void readInstance(ServerContext serverContext, ReadRequest readRequest,
                ResultHandler<Resource> resourceResultHandler) {
            ResourceUtil.notSupportedOnInstance(readRequest);
        }

        @Override
        public void updateInstance(ServerContext serverContext, UpdateRequest updateRequest,
                ResultHandler<Resource> resourceResultHandler) {
            ResourceUtil.notSupportedOnInstance(updateRequest);
        }
    }
}
