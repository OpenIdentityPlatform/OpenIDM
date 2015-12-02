/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.provisioner.salesforce.internal;

import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.provisioner.salesforce.internal.SalesforceConnectorUtil.adapt;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.audit.util.NullActivityLogger;
import org.forgerock.openidm.audit.util.RouterActivityLogger;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.provisioner.ConnectorConfigurationHelper;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.SystemIdentifier;
import org.forgerock.openidm.provisioner.SimpleSystemIdentifier;
import org.forgerock.openidm.provisioner.salesforce.internal.data.SObjectDescribe;
import org.forgerock.openidm.provisioner.salesforce.internal.metadata.MetadataResourceProvider;
import org.forgerock.openidm.provisioner.salesforce.internal.schema.SchemaHelper;
import org.forgerock.openidm.repo.util.StringSQLRenderer;
import org.forgerock.openidm.repo.util.StringSQLQueryFilterVisitor;
import org.forgerock.openidm.router.RouteBuilder;
import org.forgerock.openidm.router.RouteEntry;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.openidm.router.RouterRegistry;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SalesforceProvisionerService is an API adapter for the Salesforce Data API as a {@link ProvisionerService}.
 *
 * @author Laszlo Hordos
 * @author brmiller
 */
@Component(name = SalesforceProvisionerService.PID,
        policy = ConfigurationPolicy.REQUIRE,
        metatype = true,
        description = "Salesforce Provisioner Service",
        immediate = true)
@Service(value = {ProvisionerService.class})
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Salesforce Provisioner Service") })
public class SalesforceProvisionerService implements ProvisionerService, SingletonResourceProvider {

    public static final String PID = "org.forgerock.openidm.provisioner.salesforce";

    /**
     * Setup logging for the {@link SalesforceProvisionerService}.
     */
    final static Logger logger = LoggerFactory.getLogger(SalesforceProvisionerService.class);

    private static final String REVISION_FIELD = "LastModifiedDate";
    private static final String NEXT_RECORDS_URL  = "nextRecordsUrl";

    private SimpleSystemIdentifier systemIdentifier = null;
    private JsonValue jsonConfiguration = null;
    private String factoryPid = null;

    private SalesforceConnection connection = null;
    private SalesforceConfiguration config = null;

    /** use null-object activity logger until/unless ConnectionFactory binder updates it */
    private ActivityLogger activityLogger = NullActivityLogger.INSTANCE;

    /**
     * RouterRegistryService service.
     */
    @Reference(policy = ReferencePolicy.STATIC)
    protected RouterRegistry routerRegistryService;

    /**
     * The Connection Factory
     */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    void bindConnectionFactory(final ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        this.activityLogger = new RouterActivityLogger(connectionFactory);
    }

    void unbindConnectionFactory(final RouteService service) {
        this.connectionFactory = null;
        // ConnectionFactory has gone away, use null activity logger
        this.activityLogger = NullActivityLogger.INSTANCE;
    }

    /**
     * Cryptographic service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected CryptoService cryptoService = null;

    /**
     * Enhanced configuration service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;

    private final ConcurrentMap<String, SObjectDescribe> schema = new ConcurrentHashMap<String, SObjectDescribe>();

    /* Internal routing objects to register and remove the routes. */
    private RouteEntry routeEntry;

    @Activate
    void activate(ComponentContext context) throws Exception {
        factoryPid = (String) context.getProperties().get("config.factory-pid");

        try {
            jsonConfiguration = enhancedConfig.getConfigurationAsJson(context);
        } catch (Exception ex) {
            logger.warn("Configuration invalid and could not be parsed, can not start Salesforce Connector: "
                    + ex.getMessage(), ex);
            throw ex;
        }
        systemIdentifier = new SimpleSystemIdentifier(jsonConfiguration);

        if (!jsonConfiguration.get("enabled").defaultTo(true).asBoolean()) {
            logger.info("Salesforce Connector {} is disabled in configuration", systemIdentifier.getName());
            return;
        }

        try {
            config = SalesforceConnectorUtil.parseConfiguration(jsonConfiguration, cryptoService);
        } catch (Exception ex) {
            // Invalid configuration value, permanent error.
            logger.warn("Invlaid configuration - can not start Salesforce Connector: " + ex.getMessage(), ex);
            throw ex;
        }

        // Test the connection in a separate thread to not hold up the activate process
        connection = new SalesforceConnection(config);
        Executors.newSingleThreadExecutor().submit(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            testConnection(true);
                        } catch (Exception e) {
                            logger.warn("Couldn't test Salesforce connection - activating in incomplete state", e);
                        }
                    }
                });

        ResourcePath system = new ResourcePath("system", systemIdentifier.getName());

        RouteBuilder builder = RouteBuilder.newBuilder();

        builder.withTemplate(system.toString()).withSingletonResourceProvider(this).buildNext();

        // TODO register individual SObjectResourceProviders based on configured-object types
        builder.withTemplate(system.concat("{partition}").toString())
                .withCollectionResourceProvider(new SObjectResourceProvider()).buildNext();

        builder.withTemplate(system.concat("connect").toString())
                .withCollectionResourceProvider(new GenericResourceProvider("connect"))
                .buildNext();

        builder.withTemplate(system.concat("metadata/{metadataType}").toString())
                .withCollectionResourceProvider(new MetadataResourceProvider(connection))
                .buildNext();

        routeEntry = routerRegistryService.addRoute(builder.seal());

        logger.info("Salesforce Provisioner Service component {} is activated", systemIdentifier.getName());
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

        logger.info("Salesforce Provisioner Service component {} is deactivated.", systemIdentifier.getName());
        systemIdentifier = null;
    }

    private void testConnection(boolean ignoreResourceException) throws Exception {
        try {
            if (connection == null) {
                // connection may be null if we activated with "enabled" : false
                throw new ServiceUnavailableException("Salesforce Connector not available");
            }
            connection.test();
        } catch (ResourceException e) {
            // authenticate or the test throws this, temporary error
            logger.warn("Service temporarily unavailable; cannot start Salesforce Connector: " + e.getMessage());
            if (!ignoreResourceException) {
                throw e;
            }
        } catch (Exception ex) {
            // Invalid configuration value, permanent error.
            logger.warn("Configuration invalid and could not be validated, can not start Salesforce Connector: "
                    + ex.getMessage(), ex);
            throw ex;
        }
    }

    private String getPartition(Context context) throws ResourceException {
        Map<String, String> variables = ResourceUtil.getUriTemplateVariables(context);
        if (null != variables && variables.containsKey("partition")) {
            return variables.get("partition");
        }
        throw new ForbiddenException("Direct access without Router to this service is forbidden.");
    }


    // -- ProvisionerService implementation --

    @Override
    public SystemIdentifier getSystemIdentifier() {
        return systemIdentifier;
    }

    @Override
    public Map<String, Object> getStatus(Context serverContext) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        JsonValue jv = new JsonValue(result);

        jv.put("name", systemIdentifier.getName());
        jv.put("enabled", jsonConfiguration.get("enabled").defaultTo(Boolean.TRUE).asBoolean());
        jv.put("config", "config/provisioner.salesforce/" + factoryPid);
        jv.put("objectTypes", SchemaHelper.getObjectSchema().keys());
        jv.put("displayName", "Salesforce Connector");

        try {
            jv.put(ConnectorConfigurationHelper.CONNECTOR_REF,
                    jsonConfiguration.get(ConnectorConfigurationHelper.CONNECTOR_REF).getObject());

            testConnection(false);
            ReadRequest readRequest =
                    Requests.newReadRequest("system/" + systemIdentifier.getName() + "/connect/organization");
            connectionFactory.getConnection().read(serverContext, readRequest);
            result.put("ok", true);
        } catch (Throwable e) {
            result.put("error", e.getMessage());
            result.put("ok", false);
        }
        return result;
    }

    @Override
    public Map<String, Object> testConfig(JsonValue config) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("name", systemIdentifier.getName());
        try {
            SalesforceConnectorUtil.parseConfiguration(config, cryptoService).validate();
            map.put("ok", true);
        } catch (Exception e) {
            e.printStackTrace();
            map.put("ok", false);
            map.put("error", e.getMessage());
        }
        return map;
    }

    @Override
    public JsonValue liveSynchronize(Context context, String objectType, JsonValue previousStage) throws ResourceException {
        return previousStage;
    }


    // -- ProvisionerService implementation --

    private static final String ACTION_TEST_CONNECTOR = "test";


    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest actionRequest) {
        try {
            if (ACTION_TEST_CONNECTOR.equals(actionRequest.getAction())) {
                return newActionResponse(new JsonValue(getStatus(context))).asPromise();
            } else {
                return new BadRequestException("Unsupported action: " + actionRequest.getAction()).asPromise();
            }
        } catch (Exception e) {
            return adapt(e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest patchRequest) {
        return ResourceUtil.notSupportedOnInstance(patchRequest).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest readRequest) {
        return ResourceUtil.notSupportedOnInstance(readRequest).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest updateRequest) {
        return ResourceUtil.notSupportedOnInstance(updateRequest).asPromise();
    }

    private class SObjectResourceProvider implements CollectionResourceProvider {

        @Override
        public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
            return ResourceUtil.notSupportedOnCollection(request).asPromise();
        }

        @Override
        public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId, ActionRequest request) {
            return ResourceUtil.notSupportedOnInstance(request).asPromise();
        }

        @Override
        public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
            try {
                final String type = getPartition(context);
                final SObjectDescribe describe = getSObjectDescribe(type);
                if (null != describe) {
                    final ClientResource cr = getClientResource(type, null);
                    try {
                        final JsonValue create = new JsonValue(describe.beforeCreate(request.getContent()));
                        logger.trace("Create sobjects/{} \n content: \n{}\n", type, create);

                        cr.getRequest().setEntity(new JacksonRepresentation<Map>(create.asMap()));
                        cr.setMethod(Method.POST);
                        handleRequest(cr, true);
                        final Representation body = cr.getResponse().getEntity();
                        if (null != body && body instanceof EmptyRepresentation == false) {
                            final JacksonRepresentation<Map> rep =
                                    new JacksonRepresentation<Map>(body, Map.class);
                            final JsonValue result = new JsonValue(rep.getObject());
                            result.asMap().putAll(create.asMap());
                            if (result.get("success").asBoolean()) {
                                result.put(ResourceResponse.FIELD_CONTENT_ID, result.get("id").getObject());
                            }
                            if (result.isDefined("errors") && result.get("errors").size() > 0) {
                                return new InternalServerErrorException("Failed to create FIX ME").asPromise();
                            } else {
                                final ResourceResponse resource = newResourceResponse(result.get("id").asString(), "", result);
                                activityLogger.log(context, request, "message",
                                        getSource(type, resource.getId()), null,
                                        resource.getContent(), org.forgerock.openidm.audit.util.Status.SUCCESS);
                                return resource.asPromise();
                            }
                        } else {
                            return new InternalServerErrorException("Failed to create FIX ME?").asPromise();
                        }
                    } catch (ResourceException e) {
                        activityLogger.log(context, request, "message",
                                getSource(type, request.getNewResourceId()), request.getContent(),
                                null, org.forgerock.openidm.audit.util.Status.FAILURE);
                        return e.asPromise();
                    } finally {
                        if (null != cr) {
                            cr.release();
                        }
                    }
                } else {
                    return new BadRequestException("Type not supported: " + type).asPromise();
                }
            } catch (Throwable t) {
                return adapt(t).asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId, DeleteRequest request) {
            try {
                final String type = getPartition(context);
                final ClientResource cr = getClientResource(type, resourceId);
                try {
                    cr.setMethod(Method.DELETE);
                    handleRequest(cr, true);
                    final JsonValue result;
                    final Representation body = cr.getResponse().getEntity();
                    final ResourceResponse resource;
                    if (null != body && body instanceof EmptyRepresentation == false) {
                        final JacksonRepresentation<Map> rep =
                                new JacksonRepresentation<Map>(body, Map.class);
                        result = new JsonValue(rep.getObject());
                    } else {
                        result = new JsonValue(new HashMap<String, Object>());
                    }
                    resource = newResourceResponse(resourceId, request.getRevision(), result);
                    result.put(ResourceResponse.FIELD_CONTENT_ID, resource.getId());
                    activityLogger.log(context, request, "message", getSource(type, resource.getId()),
                            resource.getContent(), null, org.forgerock.openidm.audit.util.Status.SUCCESS);
                    return resource.asPromise();
                } catch (ResourceException e) {
                    activityLogger.log(context, request, "message",
                            getSource(type, resourceId), null,
                            null, org.forgerock.openidm.audit.util.Status.FAILURE);
                    return e.asPromise();
                } finally {
                    if (null != cr) {
                        cr.release();
                    }
                }
            } catch (Throwable t) {
                return adapt(t).asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId, PatchRequest request) {
            return ResourceUtil.notSupportedOnInstance(request).asPromise();
        }

        @Override
        public Promise<QueryResponse, ResourceException> queryCollection(final Context context, final QueryRequest request,
                final QueryResourceHandler handler) {

            String type = "?";
            try {
                type = getPartition(context);
                final String queryExpression;
                if (StringUtils.isNotBlank(request.getQueryId())) {
                    if (ServerConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                        queryExpression = "SELECT id FROM " + type;
                    } else if (config.queryIdExists(request.getQueryId())) {
                        queryExpression = config.getQueryExpression(request.getQueryId());
                    } else {
                        return new BadRequestException("Unsupported QueryId" + request.getQueryId()).asPromise();
                    }
                } else if (StringUtils.isNotBlank(request.getQueryExpression())) {
                    queryExpression = request.getQueryExpression();
                } else if (request.getQueryFilter() != null) {
                    queryExpression = buildQueryExpressionFromQueryFilter(type, request.getQueryFilter());
                } else {
                    return new BadRequestException("One of queryId, queryExpression, or queryFilter must be specified").asPromise();
                }

                return executeQuery(handler, queryExpression).asPromise();
            } catch (Throwable t) {
                final String queryRequestMessage;
                if (request.getQueryId() != null) {
                    queryRequestMessage = "queryId=" + request.getQueryId();
                } else if (request.getQueryExpression() != null) {
                    queryRequestMessage = "queryExpression=" + request.getQueryExpression();
                } else if (request.getQueryFilter() != null) {
                    queryRequestMessage = "queryFilter=" + request.getQueryFilter().toString();
                } else {
                    // can't happen
                    queryRequestMessage = "unknown query";
                }

                logger.error(t.getMessage() + " while executing " + queryRequestMessage + " on partition " + type, t);
                return adapt(t).asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId, ReadRequest request) {
            try {
                final String type = getPartition(context);
                final ClientResource cr = getClientResource(type, resourceId);
                try {
                    handleRequest(cr, true);
                    final Representation body = cr.getResponse().getEntity();
                    if (null != body && body instanceof EmptyRepresentation == false) {
                        final JacksonRepresentation<Map> rep =
                                new JacksonRepresentation<Map>(body, Map.class);
                        final JsonValue result = new JsonValue(rep.getObject());
                        result.put(ResourceResponse.FIELD_CONTENT_ID, result.get("Id").asString());
                        final ResourceResponse resource =
                                newResourceResponse(result.get("Id").asString(), result.get(REVISION_FIELD).asString(), result);
                        activityLogger.log(context, request, "message",
                                getSource(type, resource.getId()), resource.getContent(),
                                resource.getContent(), org.forgerock.openidm.audit.util.Status.SUCCESS);
                        return resource.asPromise();
                    } else {
                        return new NotFoundException().asPromise();
                    }
                } catch (ResourceException e) {
                    activityLogger.log(context, request, "message",
                            getSource(type, resourceId), null,
                            null, org.forgerock.openidm.audit.util.Status.FAILURE);
                    return e.asPromise();
                } finally {
                    if (null != cr) {
                        cr.release();
                    }
                }
            } catch (Throwable t) {
                return adapt(t).asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId, UpdateRequest request) {
            try {
                final String type = getPartition(context);
                final SObjectDescribe describe = getSObjectDescribe(type);
                if (null != describe) {
                    final ClientResource cr = getClientResource(type, resourceId);
                    try {
                        cr.setMethod(SalesforceConnection.PATCH);

                        final JsonValue update =
                                new JsonValue(describe.beforeUpdate(request.getContent()));
                        logger.trace("Update sobjects/{} \n content: \n{}\n", type, update);

                        cr.getRequest().setEntity(new JacksonRepresentation<Map>(update.asMap()));

                        handleRequest(cr, true);
                        final JsonValue result;
                        final Representation body = cr.getResponse().getEntity();
                        if (null != body && body instanceof EmptyRepresentation == false) {
                            final JacksonRepresentation<Map> rep =
                                    new JacksonRepresentation<Map>(body, Map.class);
                            result = new JsonValue(rep.getObject());
                        } else {
                            result = new JsonValue(new HashMap<String, Object>());
                        }
                        result.asMap().putAll(update.asMap());
                        result.put(ResourceResponse.FIELD_CONTENT_ID, resourceId);
                        final ResourceResponse resource = newResourceResponse(resourceId, request.getRevision(), result);
                        activityLogger.log(context, request, "message",
                                getSource(type, resource.getId()), null,
                                resource.getContent(), org.forgerock.openidm.audit.util.Status.SUCCESS);
                        return resource.asPromise();
                    } catch (ResourceException e) {
                        activityLogger.log(context, request, "message",
                                getSource(type, resourceId), request.getContent(),
                                null, org.forgerock.openidm.audit.util.Status.FAILURE);
                        return e.asPromise();
                    } finally {
                        if (null != cr) {
                            cr.release();
                        }
                    }
                } else {
                    return new BadRequestException("Type not supported: " + type).asPromise();
                }
            } catch (Throwable t) {
                return adapt(t).asPromise();
            }
        }
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
                        cr.setMethod(Method.GET);

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

    private static final StringSQLQueryFilterVisitor<Void> SALESFORCE_QUERY_FILTER_VISITOR =
            new StringSQLQueryFilterVisitor<Void>() {

                private String getField(JsonPointer field) {
                    if (field.size() != 1) {
                        throw new IllegalArgumentException("Only one level JsonPointer supported");
                    }

                    if (ResourceResponse.FIELD_CONTENT_ID.equals(field.leaf())) {
                        return "Id";
                    } else {
                        return field.leaf();
                    }
                }

                @Override
                public StringSQLRenderer visitValueAssertion(Void parameters, String operand, JsonPointer field, Object valueAssertion) {
                    return new StringSQLRenderer("(")
                        .append(getField(field))
                        .append(" ")
                        .append(operand)
                        .append(" '")
                        .append(String.valueOf(valueAssertion))
                        .append("')");
                }

                @Override
                public StringSQLRenderer visitPresentFilter(Void parameters, JsonPointer field) {
                    return new StringSQLRenderer("(")
                        .append(getField(field))
                        .append(" != null)");
                }
            };

    private String buildQueryExpressionFromQueryFilter(String type, QueryFilter queryFilter) {
        return "SELECT "
                + StringUtils.join(SchemaHelper.getObjectProperties(type), ", ")
                + " FROM " + type
                + " WHERE "
                + queryFilter.accept(SALESFORCE_QUERY_FILTER_VISITOR, null).toString();
    }

    private QueryResponse executeQuery(QueryResourceHandler handler, String queryExpression) throws ResourceException {
        Integer totalSize = null;
        JsonValue result = null;
        String pagedResultsCookie = null;

        //TODO Enable it when OpenIDM Sync Engine supports the paged search
        boolean OpenIDMisSmart = false; //QueryRequest.getPageSize <> 0

        do {
            try {
                if (null == result) {
                    result = sendClientRequest(Method.GET, "query", new Parameter("q", queryExpression));
                } else if (!result.get(NEXT_RECORDS_URL).isNull()) {
                    String url = result.get(NEXT_RECORDS_URL).asString();
                    String queryValue = url.substring(url.indexOf("query/"), url.length());
                    if (OpenIDMisSmart) {
                        // TODO Use the OpenIDM to iterate over the pages
                        pagedResultsCookie = queryValue.substring(6);
                        // TODO Calculate the remainingPagedResults
                        String pos = pagedResultsCookie.substring(pagedResultsCookie.indexOf('-') + 1);
                        return newQueryResponse(pagedResultsCookie, CountPolicy.EXACT, totalSize - Integer.getInteger(pos));
                    }
                    result = sendClientRequest(Method.GET, queryValue);
                } else {
                    break;
                }
                if (result != null) {
                    for (JsonValue record : result.get("records")) {
                        if (record.isDefined("Id")) {
                            record.put(ResourceResponse.FIELD_CONTENT_ID, record.get("Id").asString());
                        }
                        handler.handleResource(
                                newResourceResponse(record.get("Id").asString(), record.get(REVISION_FIELD).asString(), record));
                    }
                    if (totalSize == null) {
                        totalSize = result.get("totalSize").asInteger();
                    }
                    if (result.get("done").asBoolean()) {
                        break;
                    }
                } else {
                    throw new InternalServerErrorException("Unexpected Salesforce service response: 'null'");
                }
            } catch (final IOException e) {
                throw new InternalServerErrorException(e);
            }
        } while (true);
        return newQueryResponse();
    }

    private JsonValue sendClientRequest(Method method, String id,
                                        Parameter... parameters) throws ResourceException, IOException {

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

    protected void handleRequest(final ClientResource resource, boolean tryReauth) throws ResourceException {
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
        return connection.getChild(getSource(type, id));
    }

    private String getSource(String type, String id) {
        StringBuilder sb = new StringBuilder("services/data/").append(connection.getVersion()).append("/sobjects");
        if (null != type) {
            sb.append('/').append(type);
            if (null != id) {
                sb.append('/').append(id);
            }
        }
        return sb.toString();
    }

    private ClientResource getClientResource(String id) {
        return connection.getChild(id == null ? "services/data/" + connection.getVersion()
                : "services/data/" + connection.getVersion() + "/" + id);
    }

    class GenericResourceProvider implements CollectionResourceProvider {

        private final String type;

        GenericResourceProvider(String type) {
            this.type = type;
        }

        @Override
        public Promise<ResourceResponse, ResourceException> readInstance(Context context, String instanceName,
                ReadRequest readRequest) {
            try {
                final ClientResource cr = getClientResource(type, instanceName);
                try {
                    handleRequest(cr, true);
                    Representation body = cr.getResponse().getEntity();
                    if (null != body && body instanceof EmptyRepresentation == false) {
                        JacksonRepresentation<Map> rep = new JacksonRepresentation<Map>(body, Map.class);
                        JsonValue result = new JsonValue(rep.getObject());
                        if (result.isDefined("Id")) {
                            result.put(ResourceResponse.FIELD_CONTENT_ID, result.get("Id").required().asString());
                        }
                        return newResourceResponse(result.get("Id").asString(), "", result).asPromise();
                    } else {
                        return new NotFoundException("Resource not Found").asPromise();
                    }
                } finally {
                    if (null != cr) {
                        cr.release();
                    }
                }
            } catch (Throwable t) {
                return adapt(t).asPromise();
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
        public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest actionRequest) {
            return ResourceUtil.notSupportedOnCollection(actionRequest).asPromise();
        }

        @Override
        public Promise<ActionResponse, ResourceException> actionInstance(Context context, String s, ActionRequest actionRequest) {
            return ResourceUtil.notSupportedOnInstance(actionRequest).asPromise();
        }

        @Override
        public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest createRequest) {
            return ResourceUtil.notSupportedOnInstance(createRequest).asPromise();
        }

        @Override
        public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String s, DeleteRequest deleteRequest) {
            return ResourceUtil.notSupportedOnInstance(deleteRequest).asPromise();
        }

        @Override
        public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String s, PatchRequest patchRequest) {
            return ResourceUtil.notSupportedOnInstance(patchRequest).asPromise();
        }

        @Override
        public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest queryRequest,
                QueryResourceHandler queryResourceHandler) {
            return ResourceUtil.notSupportedOnCollection(queryRequest).asPromise();
        }

        @Override
        public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String s, UpdateRequest updateRequest) {
            return ResourceUtil.notSupportedOnInstance(updateRequest).asPromise();
        }
    }
}
