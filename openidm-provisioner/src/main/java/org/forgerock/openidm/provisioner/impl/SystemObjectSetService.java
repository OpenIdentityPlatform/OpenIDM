/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.provisioner.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.provisioner.ConnectorConfigurationHelper;
import org.forgerock.openidm.provisioner.Id;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.SystemIdentifier;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.router.RouteService;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.openidm.provisioner.ConnectorConfigurationHelper.CONNECTOR_NAME;
import static org.forgerock.openidm.provisioner.ConnectorConfigurationHelper.CONNECTOR_REF;
import static org.forgerock.openidm.provisioner.ConnectorConfigurationHelper.CONFIGURATION_PROPERTIES;


/**
 * SystemObjectSetService is a {@link SingletonResourceProvider} to manage provisioner/connector configuration
 * and to dispatch liveSync to the correct provisioner implementation.
 */
@Component(name = "org.forgerock.openidm.provisioner",
        policy = ConfigurationPolicy.IGNORE,
        metatype = true,
        description = "OpenIDM System Object Set Service",
        immediate = true)
@Service(value = {ScheduledService.class, SingletonResourceProvider.class})
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM System Object Set Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = ProvisionerService.ROUTER_PREFIX)
})
public class SystemObjectSetService implements ScheduledService, SingletonResourceProvider {

    private final static Logger logger = LoggerFactory.getLogger(SystemObjectSetService.class);

    /** the system (provisioner) type (within connectorRef) */
    private static final String SYSTEM_TYPE = "systemType";

    /** systemType prefix prepended per connectorRef list */
    private static final String PROVISIONER_PREFIX = "provisioner";

    /**
     * Actions supported on this resource provider
     */
    public enum SystemAction {
        /** Captures the changes on a remote system, the pushes those changes to OpenIDM */
        activeSync,
        /** Captures the changes on a remote system, the pushes those changes to OpenIDM */
        liveSync,
        /** Test a connector to see if the connection is available */
        test,
        /** Test an existing connector configuration */
        testConfig {
            @Override
            boolean requiresConnectorConfigurationHelper(JsonValue requestContent) {
                return true;
            }
        },
        /** Multi phase configuration event calls this to generate the response */
        createConfiguration {
            /**
             * ConnectorConfigurationHelper is required if there is request content
             */
            @Override
            boolean requiresConnectorConfigurationHelper(JsonValue requestContent) {
                return requestContent != null && requestContent.size() > 0;
            }
        },
        /** List the connector [types] available in the system */
        availableConnectors,
        /** Generates the core configuration for a connector */
        createCoreConfig {
            /**
             * ConnectorConfigurationHelper is required always
             */
            @Override
            boolean requiresConnectorConfigurationHelper(JsonValue requestContent) {
                return true;
            }
        },
        /** Generates the full configuration for a connector */
        createFullConfig {
            /**
             * ConnectorConfigurationHelper is required always
             */
            @Override
            boolean requiresConnectorConfigurationHelper(JsonValue requestContent) {
                return true;
            }
        };

        /**
         * Checks to see that ConnectorConfigurationHelper is needed - default to false
         */
        boolean requiresConnectorConfigurationHelper(JsonValue requestContent) {
            return false;
        }

        private static Set<SystemAction> liveSyncActions = EnumSet.of(activeSync, liveSync);

        /** Checks to see if action is live sync */
        boolean isLiveSync() {
            return liveSyncActions.contains(this);
        }
    }

    @Reference(referenceInterface = ProvisionerService.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            bind = "bindProvisionerService",
            unbind = "unbindProvisionerService",
            policy = ReferencePolicy.DYNAMIC,
            strategy = ReferenceStrategy.EVENT)
    private Map<SystemIdentifier, ProvisionerService> provisionerServices = new HashMap<SystemIdentifier, ProvisionerService>();

    protected void bindProvisionerService(ProvisionerService service, Map properties) {
        provisionerServices.put(service.getSystemIdentifier(), service);
    }

    protected void unbindProvisionerService(ProvisionerService service, Map properties) {
        for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
            if (service.equals(entry.getValue())) {
                provisionerServices.remove(entry.getKey());
                break;
            }
        }
    }

    @Reference(target = "("+ServerConstants.ROUTER_PREFIX + "=/*)")
    RouteService routeService;
    ServerContext routerContext = null;

    private void bindRouteService(final RouteService service) throws ResourceException {
        routeService = service;
        routerContext = service.createServerContext();
    }

    private void unbindRouteService(final RouteService service) {
        routeService = null;
        routerContext = null;
    }

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    @Reference(referenceInterface = ConnectorConfigurationHelper.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            bind = "bindConnectorConfigurationHelper",
            unbind = "unbindConnectorConfigurationHelper",
            policy = ReferencePolicy.DYNAMIC)
    private Map<String, ConnectorConfigurationHelper> connectorConfigurationHelpers = new HashMap<String, ConnectorConfigurationHelper>();

    protected void bindConnectorConfigurationHelper(ConnectorConfigurationHelper helper, Map properties) throws ResourceException {
        connectorConfigurationHelpers.put(helper.getProvisionerType(), helper);
    }

    protected void unbindConnectorConfigurationHelper(ConnectorConfigurationHelper helper, Map properties) {
        connectorConfigurationHelpers.remove(helper.getProvisionerType());
    }

    @Override
    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            final ProvisionerService ps;
            final JsonValue content = request.getContent();
            final JsonValue id = content.get("id");
            final JsonValue name = content.get("name");
            final SystemAction action = request.getActionAsEnum(SystemAction.class);
            String provisionerType = null;

            if (action.requiresConnectorConfigurationHelper(content)) {
                final String connectorName = content.get(CONNECTOR_REF).get(CONNECTOR_NAME).asString();
                if (connectorName == null) {
                    throw new NotFoundException("No connector name provided");
                }
                provisionerType = getProvisionerType(connectorName);
                if (provisionerType == null || !connectorConfigurationHelpers.containsKey(provisionerType)) {
                    throw new ServiceUnavailableException("The required service is not available");
                }
            }

            final ConnectorConfigurationHelper helper = connectorConfigurationHelpers.get(provisionerType);

            switch (action) {
            case createConfiguration:
                //  Multi phase configuration event calls this to generate the response for the next phase.
                if (content.size() == 0) {
                    // Stage 1 : list available connectors
                    handler.handleResult(getAvailableConnectors());
                } else if (isGenerateConnectorCoreConfig(content)) {
                    // Stage 2: generate basic configuration
                    handler.handleResult(helper.generateConnectorCoreConfig(content));
                } else if (isGenerateFullConfig(content)) {
                    // Stage 3: generate/validate full configuration
                    handler.handleResult(helper.generateConnectorFullConfig(content));
                } else {
                    // illegal request ??
                    handler.handleResult(json(object()));
                }
                break;
            case testConfig:
                JsonValue config = content;
                if (!id.isNull()) {
                    throw new BadRequestException("A system ID must not be specified in the request");
                }
                if (name.isNull()) {
                    throw new BadRequestException("Invalid configuration to test: no 'name' specified");
                }
                ps = locateServiceForTest(name);
                if (ps != null) {
                    handler.handleResult(new JsonValue(ps.testConfig(config)));
                } else {
                    // service for config-name doesn't exist; test it using the ConnectorConfigurationHelper
                    handler.handleResult(new JsonValue(helper.test(config)));
                }
                break;
            case test:
                if (id.isNull()) {
                    List<Object> list = new ArrayList<Object>();
                    for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
                        list.add(entry.getValue().getStatus(context));
                    }
                    handler.handleResult(new JsonValue(list));
                } else {
                    ps = locateServiceForTest(id);
                    if (ps == null) {
                        throw new NotFoundException("System: " + id.asString() + " is not available.");
                    } else {
                        handler.handleResult(new JsonValue(ps.getStatus(context)));
                    }
                }
                break;
            case activeSync:
            case liveSync:
                JsonValue params = new JsonValue(request.getAdditionalParameters());
                String source = params.get("source").asString();
                if (source == null) {
                    logger.debug("liveSync requires an explicit source parameter, source is : {}", source );
                    throw new BadRequestException("liveSync action requires either an explicit source parameter, "
                            + "or needs to be called on a specific provisioner URI");
                } else {
                    logger.debug("liveSync called with explicit source parameter {}", source);
                }
                handler.handleResult(liveSync(source, Boolean.valueOf(params.get("detailedFailure").asString())));
                break;
            case availableConnectors:
                // stage 1 - direct action to get available connectors
                handler.handleResult(getAvailableConnectors());
                break;
            case createCoreConfig:
                // stage 2 - direct action to create core configuration
                handler.handleResult(helper.generateConnectorCoreConfig(content));
                break;
            case createFullConfig:
                // stage 3 - direct action to create full configuration
                handler.handleResult(helper.generateConnectorFullConfig(content));
                break;
            default:
                handler.handleError(new BadRequestException("Unsupported actionId: " + request.getAction()));
                return;
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (IllegalArgumentException e) {
            // from getActionAsEnum
            handler.handleError(new BadRequestException(e.getMessage(), e));
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    private String getProvisionerType(String connectorName) throws ResourceException {
        for (Map.Entry<String, ConnectorConfigurationHelper> entry : connectorConfigurationHelpers.entrySet()) {
            for (JsonValue connectorRef : entry.getValue().getAvailableConnectors().get(CONNECTOR_REF)) {
                if (connectorRef.get(CONNECTOR_NAME).asString().equals(connectorName)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Validates that the connectorRef is defined in the connector configuration
     *
     * @param requestConfig connector configuration
     * @return true if connectorRef is not null and configurationProperties is null; false otherwise
     */
    private boolean isGenerateConnectorCoreConfig(JsonValue requestConfig) {
        return !requestConfig.get(CONNECTOR_REF).isNull()
                && !requestConfig.get(CONNECTOR_REF).get(CONNECTOR_NAME).isNull()
                && requestConfig.get(CONFIGURATION_PROPERTIES).isNull();
    }

    /**
     * Validates that connectorRef and configurationProperties inside the connector configuration are both not null
     *
     * @param requestConfig connector configuration
     * @return true if both connectorRef and configurationProperties are not null; false otherwise
     */
    private boolean isGenerateFullConfig(JsonValue requestConfig) {
        return !requestConfig.get(CONNECTOR_REF).isNull()
                && !requestConfig.get(CONNECTOR_REF).get(CONNECTOR_NAME).isNull()
                && !requestConfig.get(CONFIGURATION_PROPERTIES).isNull();
    }

    private JsonValue getAvailableConnectors() throws ResourceException {
        JsonValue availableConnectors = json(array());
        for (Map.Entry<String, ConnectorConfigurationHelper> helperEntry : connectorConfigurationHelpers.entrySet()) {
            for (JsonValue connectorRef : helperEntry.getValue().getAvailableConnectors().get(CONNECTOR_REF)) {
                connectorRef.put(SYSTEM_TYPE, getSystemType(helperEntry.getKey()));
                availableConnectors.add(connectorRef.getObject());
            }
        }
        return json(object(field(CONNECTOR_REF, availableConnectors.getObject())));
    }

    /**
     * The system type is comprised by the prefix "provisioner." and the provionser's type; e.g. openicf
     *
     * @param provisionerType the provisioner type
     * @return the system type
     */
    private String getSystemType(String provisionerType) {
        return new StringBuilder(PROVISIONER_PREFIX).append(".").append(provisionerType).toString();
    }

    @Override
    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Read are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Patch are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update are not supported for resource instances");
        handler.handleError(e);
    }

    /**
     * Invoked by the scheduler when the scheduler triggers.
     *
     * @param schedulerContext Context information passed by the scheduler service
     * @throws org.forgerock.openidm.quartz.impl.ExecutionException
     *          if execution of the scheduled work failed.
     *          Implementations can also throw RuntimeExceptions which will get logged.
     */
    public void execute(ServerContext context, Map<String, Object> schedulerContext) throws ExecutionException {
        try {
            JsonValue params = new JsonValue(schedulerContext).get(CONFIGURED_INVOKE_CONTEXT);
            if (params.get("action").asEnum(SystemAction.class).isLiveSync()) {
                String source = params.get("source").required().asString();
                liveSync(source, true);
            }
        } catch (JsonValueException jve) {
            throw new ExecutionException(jve);
        } catch (ResourceException e) {
            throw new ExecutionException(e);
        } catch (IllegalArgumentException e) {
            // not a liveSync action, so no-op
        } catch (RuntimeException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void auditScheduledService(final ServerContext context, final AuditEvent auditEvent)
            throws ExecutionException {
        try {
            connectionFactory.getConnection().create(
                    context, Requests.newCreateRequest("audit/access", auditEvent.getValue()));
        } catch (ResourceException e) {
            logger.error("Unable to audit scheduled service {}", auditEvent.toString());
            throw new ExecutionException("Unable to audit scheduled service", e);
        }
    }

    /**
     * Live sync the specified provisioner resource
     * @param source the URI of the provisioner instance to live sync
     * @param detailedFailure whether in the case of failures additional details such as the
     * record content of where it failed should be included in the response
     */
    private JsonValue liveSync(String source, boolean detailedFailure) throws ResourceException {
        JsonValue response;
        Id id = new Id(source);
        String previousStageResourceContainer = "repo/synchronisation/pooledSyncStage";
        String previousStageId = id.toString().replace("/", "").toUpperCase();
        Resource previousStage = null;
        try {
            ReadRequest readRequest = Requests.newReadRequest(previousStageResourceContainer, previousStageId);
            previousStage = connectionFactory.getConnection().read(routerContext, readRequest);

            response = locateService(id).liveSynchronize(id.getObjectType(),
                    previousStage != null && previousStage.getContent() != null ? previousStage.getContent() : null);
            UpdateRequest updateRequest = Requests.newUpdateRequest(previousStageResourceContainer, previousStageId, response);
            updateRequest.setRevision(previousStage.getRevision());
            connectionFactory.getConnection().update(routerContext, updateRequest);
        } catch (ResourceException e) { // NotFoundException?
            if (previousStage != null) {
                throw e;
            }
            response = locateService(id).liveSynchronize(id.getObjectType(), null);
            if (response != null) {
                CreateRequest createRequest = Requests.newCreateRequest(previousStageResourceContainer, previousStageId, response);
                connectionFactory.getConnection().create(routerContext, createRequest);
            }
        }
        if (response != null && !detailedFailure) {
            // The detailedFailure option handling ideally should move into provisioners
            response.get("lastException").remove("syncDelta");
        }
        return response;
    }

    private ProvisionerService locateService(Id identifier) throws ResourceException {
        for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
            if (entry.getKey().is(identifier)) {
                return entry.getValue();
            }
        }
        throw new ServiceUnavailableException("System: " + identifier + " is not available.");
    }

    private ProvisionerService locateServiceForTest(JsonValue requestId) throws ResourceException {
        if (requestId.isNull()) {
            return null;
        }

        Id id = new Id(requestId.asString() + "/test");
        for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
            if (entry.getKey().is(id)) {
                return entry.getValue();
            }
        }

        return null;
    }
}
