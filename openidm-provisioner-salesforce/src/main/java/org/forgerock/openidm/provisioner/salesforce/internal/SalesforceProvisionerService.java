/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openidm.provisioner.salesforce.internal;

import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.provisioner.salesforce.internal.SalesforceConnectorUtil.adapt;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import org.apache.http.client.utils.URIBuilder;
import org.forgerock.http.protocol.Request;
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
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.RetryableException;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promises;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SalesforceProvisionerService is an API adapter for the Salesforce Data API as a {@link ProvisionerService}.
 *
 */
@Component(name = SalesforceProvisionerService.PID,
        policy = ConfigurationPolicy.REQUIRE,
        immediate = true)
@Service(value = {ProvisionerService.class})
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Salesforce Provisioner Service") })
public class SalesforceProvisionerService implements ProvisionerService, SingletonResourceProvider {

    public static final String PID = "org.forgerock.openidm.provisioner.salesforce";

    private static final String SALESFORCE_ID_KEY = "Id";
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

    private static final int QUERY_REQUEST_TIMEOUT_SECONDS = 20;

    /**
     * RouterRegistryService service.
     */
    @Reference(policy = ReferencePolicy.STATIC)
    protected RouterRegistry routerRegistryService;

    /**
     * The Connection Factory
     */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.router.internal)")
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
    protected volatile CryptoService cryptoService = null;

    /**
     * Enhanced configuration service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

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
        try {
            connection = new SalesforceConnection(config, SalesforceConnectorUtil.newHttpClientHandler(config));
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
        } catch (Exception e) {
            logger.warn("Couldn't initialize Salesforce connection - activating in incomplete state", e);
        }

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
        } catch (RetryableException e) {
            // authenticate or the test throws this, temporary error
            logger.info("Service temporarily unavailable; cannot start Salesforce Connector: " + e.getMessage());
            if (!ignoreResourceException) {
                throw e;
            }
        } catch (ResourceException e) {
            logger.warn("Service unavailable; cannot start Salesforce Connector: " + e.getMessage());
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
        public Promise<ResourceResponse, ResourceException> createInstance(final Context context, final CreateRequest request) {
            try {
                final String type = getPartition(context);
                return getSObjectDescribe(type).thenAsync(
                    new AsyncFunction<SObjectDescribe, ResourceResponse, ResourceException>() {
                        @Override
                        public Promise<ResourceResponse, ResourceException> apply(SObjectDescribe describe) throws ResourceException {
                            Request chfRequest = newRequestForUri(connection.getSObjectInvocationUrl(type, null));
                            chfRequest.setMethod("POST");
                            final Map<String, Object> beforeCreateMap = describe.beforeCreate(request.getContent());
                            chfRequest.setEntity(beforeCreateMap);
                            logger.trace("Create sobjects/{} \n content: \n{}\n", type, chfRequest.getEntity());
                            return connection.dispatchAuthorizedRequest(chfRequest).thenAsync(
                                new AsyncFunction<JsonValue, ResourceResponse, ResourceException>() {
                                    @Override
                                    public Promise<ResourceResponse, ResourceException> apply(JsonValue response) throws ResourceException {
                                        //Curious, but follows restlet logic. May be because SF returns 204 with no content for some invocations,
                                        //so response map is populated with invocation map.
                                        response.asMap().putAll(beforeCreateMap);
                                        if (response.get("success").asBoolean()) {
                                            response.put(ResourceResponse.FIELD_CONTENT_ID, response.get("id").getObject());
                                        }
                                        if (response.isDefined("errors") && response.get("errors").size() > 0) {
                                            throw ResourceException.newResourceException(ResourceException.INTERNAL_ERROR, "Create failed: " + response.get("errors"));
                                        } else {
                                            final ResourceResponse resource = newResourceResponse(response.get("id").asString(), "", response);
                                            activityLogger.log(context, request, "message", getSource(type, resource.getId()), null,
                                                    resource.getContent(), Status.SUCCESS);
                                            return resource.asPromise();
                                        }
                                    }
                                },
                                new AsyncFunction<ResourceException, ResourceResponse, ResourceException>() {
                                    @Override
                                    public Promise<ResourceResponse, ResourceException> apply(ResourceException e) throws ResourceException {
                                        activityLogger.log(context, request, "message", getSource(type, request.getNewResourceId()),
                                                request.getContent(), null, Status.FAILURE);
                                        return e.asPromise();
                                    }
                                });
                        }
                    },
                    new AsyncFunction<ResourceException, ResourceResponse, ResourceException>() {
                        @Override
                        public Promise<ResourceResponse, ResourceException> apply(ResourceException value) throws ResourceException {
                            return ResourceException.newResourceException(ResourceException.BAD_REQUEST, "Type not supported: " + type).asPromise();
                        }
                    }
                );
            } catch (Throwable t) {
                return adapt(t).asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, final String resourceId,
                                                                           final DeleteRequest request) {
            try {
                final String type = getPartition(context);
                final Request deleteRequest = newRequestForUri(connection.getSObjectInvocationUrl(type, resourceId));
                    deleteRequest.setMethod("DELETE");
                    return connection.dispatchAuthorizedRequest(deleteRequest).thenAsync(
                        new AsyncFunction<JsonValue, ResourceResponse, ResourceException>() {
                            @Override
                            public Promise<ResourceResponse, ResourceException> apply(JsonValue response) throws ResourceException {
                                final ResourceResponse resourceResponse = newResourceResponse(resourceId, request.getRevision(), response);
                                response.put(ResourceResponse.FIELD_CONTENT_ID, resourceResponse.getId());
                                activityLogger.log(context, request, "message", getSource(type, resourceResponse.getId()),
                                        resourceResponse.getContent(), null, Status.SUCCESS);
                                return resourceResponse.asPromise();
                            }
                        },
                        new AsyncFunction<ResourceException, ResourceResponse, ResourceException>() {
                            @Override
                            public Promise<ResourceResponse, ResourceException> apply(ResourceException value) throws ResourceException {
                                activityLogger.log(context, request, "message", getSource(type, resourceId), null, null,
                                        Status.FAILURE);
                                return value.asPromise();
                            }
                        }
                    );
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
        public Promise<ResourceResponse, ResourceException> readInstance(final Context context, final String resourceId,
                                                                         final ReadRequest request) {
            try {
                final String type = getPartition(context);
                final String invocationUrl = connection.getSObjectInvocationUrl(type, resourceId);
                final Request chfRequest = newRequestForUri(invocationUrl);
                chfRequest.setMethod("GET");
                return connection.dispatchAuthorizedRequest(chfRequest).thenAsync(
                        new AsyncFunction<JsonValue, ResourceResponse, ResourceException>() {
                            @Override
                            public Promise<ResourceResponse, ResourceException> apply(JsonValue value) throws ResourceException {
                                value.put(ResourceResponse.FIELD_CONTENT_ID, value.get(SALESFORCE_ID_KEY).asString());
                                final ResourceResponse resource =
                                        newResourceResponse(value.get(SALESFORCE_ID_KEY).asString(), value.get(REVISION_FIELD).asString(), value);
                                auditActivity(context, request, "message", invocationUrl, resource.getContent(),
                                        resource.getContent(), Status.SUCCESS);
                                return resource.asPromise();
                            }
                        },
                        new AsyncFunction<ResourceException, ResourceResponse, ResourceException>() {
                            @Override
                            public Promise<ResourceResponse, ResourceException> apply(ResourceException value) throws ResourceException {
                                auditActivity(context, request, "message", invocationUrl, null,
                                        null, Status.FAILURE);
                                return value.asPromise();
                            }
                        }
                );
            } catch (ResourceException e) {
                return e.asPromise();
            }
        }
        @Override
        public Promise<ResourceResponse, ResourceException> updateInstance(final Context context, final String resourceId,
                                                                           final UpdateRequest request) {
            try {
                final String type = getPartition(context);
                return getSObjectDescribe(type).thenAsync(
                    new AsyncFunction<SObjectDescribe, ResourceResponse, ResourceException>() {
                        @Override
                        public Promise<ResourceResponse, ResourceException> apply(SObjectDescribe value) throws ResourceException {
                            Request chfRequest = newRequestForUri(connection.getSObjectInvocationUrl(type, resourceId));
                            chfRequest.setMethod("PATCH");
                            final Map<String, Object> beforeUpdateMap = value.beforeUpdate(request.getContent());
                            chfRequest.setEntity(beforeUpdateMap);
                            logger.trace("Update sobjects/{} \n content: \n{}\n", type, chfRequest.getEntity());
                            return connection.dispatchAuthorizedRequest(chfRequest).thenAsync(
                                    new AsyncFunction<JsonValue, ResourceResponse, ResourceException>() {
                                        @Override
                                        public Promise<ResourceResponse, ResourceException> apply(JsonValue response) throws ResourceException {
                                            //Curious, but follows restlet logic. May be because SF returns 204 with no content for some invocations,
                                            //so response map is populated with invocation map.
                                            response.asMap().putAll(beforeUpdateMap);
                                            response.put(ResourceResponse.FIELD_CONTENT_ID, resourceId);
                                            final ResourceResponse resource = newResourceResponse(response.get("id").asString(), "", response);
                                            activityLogger.log(context, request, "message", getSource(type, resource.getId()),
                                                    resource.getContent(), null, Status.SUCCESS);
                                            return resource.asPromise();
                                        }
                                    },
                                    new AsyncFunction<ResourceException, ResourceResponse, ResourceException>() {
                                        @Override
                                        public Promise<ResourceResponse, ResourceException> apply(ResourceException value) throws ResourceException {
                                            activityLogger.log(context, request, "message", getSource(type, resourceId), request.getContent(),
                                                    null, Status.FAILURE);
                                            return value.asPromise();
                                        }
                                    }
                            );
                        }
                    },
                    new AsyncFunction<ResourceException, ResourceResponse, ResourceException>() {
                        @Override
                        public Promise<ResourceResponse, ResourceException> apply(ResourceException value) throws ResourceException {
                            return new BadRequestException("Type not supported: " + type).asPromise();
                        }
                    }
                );
            } catch (Throwable t) {
                return adapt(t).asPromise();
            }
        }

        protected Promise<SObjectDescribe, ResourceException> getSObjectDescribe(final String type) throws ResourceException {
            SObjectDescribe describe = schema.get(type);
            if (describe == null) {
                synchronized (schema) {
                    describe = schema.get(type);
                    if (describe == null) {
                        final Request chfRequest = newRequestForUri(connection.getSObjectInvocationUrl(type, "describe"));
                        chfRequest.setMethod("GET");
                        return connection.dispatchAuthorizedRequest(chfRequest).thenAsync(
                            new AsyncFunction<JsonValue, SObjectDescribe, ResourceException>() {
                                @Override
                                public Promise<SObjectDescribe, ResourceException> apply(JsonValue value) throws ResourceException {
                                    SObjectDescribe localDescribe = SObjectDescribe.newInstance(value.asMap());
                                    if (localDescribe == null) {
                                        throw ResourceException.newResourceException(
                                                ResourceException.NOT_FOUND, "Metadata not found for type " + type);
                                    }
                                    schema.put(type, localDescribe);
                                    return Promises.newResultPromise(localDescribe);
                                }
                            },
                            new AsyncFunction<ResourceException, SObjectDescribe, ResourceException>() {
                                @Override
                                public Promise<SObjectDescribe, ResourceException> apply(ResourceException value) throws ResourceException {
                                    return value.asPromise();
                                }
                            }
                        );
                    } else {
                        return Promises.<SObjectDescribe, ResourceException>newResultPromise(describe);
                    }
                }
            } else {
                return Promises.<SObjectDescribe, ResourceException>newResultPromise(describe);
            }
        }
    }


    private static final StringSQLQueryFilterVisitor<Void> SALESFORCE_QUERY_FILTER_VISITOR =
            new StringSQLQueryFilterVisitor<Void>() {

                private String getField(JsonPointer field) {
                    if (field.size() != 1) {
                        throw new IllegalArgumentException("Only one level JsonPointer supported");
                    }

                    if (ResourceResponse.FIELD_CONTENT_ID.equals(field.leaf())) {
                        return SALESFORCE_ID_KEY;
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
                    result = dispatchQueryRequest("query", Collections.singletonMap("q", queryExpression));
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
                    result = dispatchQueryRequest(queryValue);
                } else {
                    break;
                }
                if (result != null) {
                    for (JsonValue record : result.get("records")) {
                        if (record.isDefined(SALESFORCE_ID_KEY)) {
                            record.put(ResourceResponse.FIELD_CONTENT_ID, record.get(SALESFORCE_ID_KEY).asString());
                        }
                        handler.handleResource(
                                newResourceResponse(record.get(SALESFORCE_ID_KEY).asString(), record.get(REVISION_FIELD).asString(), record));
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

    private JsonValue dispatchQueryRequest(String id) throws ResourceException {
        return dispatchQueryRequest(id, Collections.EMPTY_MAP);
    }

    private JsonValue dispatchQueryRequest(String id, Map<String, String> queryParams) throws ResourceException {
        try {
            final URIBuilder uriBuilder = new URIBuilder(connection.getQueryInvocationUrl(id));
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                uriBuilder.addParameter(entry.getKey(), entry.getValue());
            }
            final Request request = newRequestForUri(uriBuilder.build());
            request.setMethod("GET");
            logger.debug("Attempt to execute query: {}", request.getUri().toString());
            return connection.dispatchAuthorizedRequest(request).getOrThrow(QUERY_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (URISyntaxException | InterruptedException | TimeoutException e) {
            throw ResourceException.newResourceException(ResourceException.INTERNAL_ERROR, e.getMessage());
        }
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

    class GenericResourceProvider implements CollectionResourceProvider {

        private final String type;

        GenericResourceProvider(String type) {
            this.type = type;
        }

        @Override
        public Promise<ResourceResponse, ResourceException> readInstance(Context context, String instanceName,
                ReadRequest readRequest) {
            try {
                final Request chfRequest = newRequestForUri(connection.getGenericObjectInvocationUrl(type, instanceName));
                chfRequest.setMethod("GET");
                return connection.dispatchAuthorizedRequest(chfRequest).thenAsync(
                    new AsyncFunction<JsonValue, ResourceResponse, ResourceException>() {
                        @Override
                        public Promise<ResourceResponse, ResourceException> apply(JsonValue responseJson) throws ResourceException {
                            if (responseJson.get(SALESFORCE_ID_KEY).isString()) {
                                responseJson.put(ResourceResponse.FIELD_CONTENT_ID, responseJson.get(SALESFORCE_ID_KEY).asString());
                            }
                            final ResourceResponse resource =
                                    newResourceResponse(responseJson.get(SALESFORCE_ID_KEY).asString(), "", responseJson);
                            return resource.asPromise();
                        }
                    },
                    new AsyncFunction<ResourceException, ResourceResponse, ResourceException>() {
                        @Override
                        public Promise<ResourceResponse, ResourceException> apply(ResourceException value) throws ResourceException {
                            return value.asPromise();
                        }
                    }
                );
            } catch (ResourceException e) {
                return e.asPromise();
            }
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

    private Request newRequestForUri(String uri) throws ResourceException {
        final Request chfRequest = new Request();
        try {
            chfRequest.setUri(uri);
            return chfRequest;
        } catch (URISyntaxException e) {
            throw ResourceException.newResourceException(
                    ResourceException.INTERNAL_ERROR, e.getMessage());
        }
    }

    private Request newRequestForUri(URI uri) throws ResourceException {
        final Request chfRequest = new Request();
        chfRequest.setUri(uri);
        return chfRequest;
    }

    private void auditActivity(Context context, org.forgerock.json.resource.Request request, String message, String objectId,
                             JsonValue before, JsonValue after, Status status) {
        try {
            activityLogger.log(context, request, message, objectId, before, after, status);
        } catch (ResourceException e) {
            logger.error("Exception caught logging audit message", e);
        }
    }
}
