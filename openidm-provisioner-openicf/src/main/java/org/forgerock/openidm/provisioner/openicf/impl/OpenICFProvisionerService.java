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
 * Copyright 2011-2016 ForgeRock AS.
 */
package org.forgerock.openidm.provisioner.openicf.impl;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

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
import org.forgerock.guava.common.base.Predicate;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.audit.util.NullActivityLogger;
import org.forgerock.openidm.audit.util.RouterActivityLogger;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.SimpleSystemIdentifier;
import org.forgerock.openidm.provisioner.SystemIdentifier;
import org.forgerock.openidm.provisioner.openicf.ConnectorInfoProvider;
import org.forgerock.openidm.provisioner.openicf.ConnectorReference;
import org.forgerock.openidm.provisioner.openicf.OperationHelper;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper;
import org.forgerock.openidm.provisioner.openicf.internal.SystemAction;
import org.forgerock.openidm.provisioner.openicf.syncfailure.SyncFailureHandler;
import org.forgerock.openidm.provisioner.openicf.syncfailure.SyncFailureHandlerFactory;
import org.forgerock.openidm.provisioner.openicf.syncfailure.SyncHandlerException;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.router.RouteBuilder;
import org.forgerock.openidm.router.RouteEntry;
import org.forgerock.openidm.router.RouterRegistry;
import org.forgerock.util.promise.Promise;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.ScriptOnConnectorApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnResourceApiOp;
import org.identityconnectors.framework.api.operations.SyncApiOp;
import org.identityconnectors.framework.api.operations.TestApiOp;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ScriptContextBuilder;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.serializer.SerializerUtil;
import org.identityconnectors.framework.impl.api.local.LocalConnectorFacadeImpl;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The OpenICFProvisionerService is the implementation of
 * {@link CollectionResourceProvider} interface with <a
 * href="http://openicf.forgerock.org">OpenICF</a>.
 * <p/>
 */
@Component(name = OpenICFProvisionerService.PID,
        policy = ConfigurationPolicy.REQUIRE,
        metatype = true,
        description = "OpenIDM OpenICF Provisioner Service",
        immediate = true)
@Service(value = {ProvisionerService.class})
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM OpenICF Provisioner Service"),
    @Property(name = "suppressMetatypeWarning", value = "true")
})
public class OpenICFProvisionerService implements ProvisionerService, SingletonResourceProvider {

    // Public Constants
    public static final String PID = "org.forgerock.openidm.provisioner.openicf";

    private static final Logger logger = LoggerFactory.getLogger(OpenICFProvisionerService.class);

    private SimpleSystemIdentifier systemIdentifier = null;
    private OperationHelperBuilder operationHelperBuilder = null;
    private Promise<ConnectorInfo, RuntimeException> connectorFacadeCallback = null;
    private boolean serviceAvailable = false;
    private JsonValue jsonConfiguration = null;
    private ConnectorReference connectorReference = null;
    private SyncFailureHandler syncFailureHandler = null;
    private String factoryPid = null;

    /** use null-object activity logger until/unless ConnectionFactory binder updates it */
    private ActivityLogger activityLogger = NullActivityLogger.INSTANCE;

    /**
     * Cache the SystemActions from local and {@code provisioner.json} jsonConfiguration.
     */
    private final ConcurrentMap<String, SystemAction> localSystemActionCache = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, RequestHandler> objectClassHandlers = new ConcurrentHashMap<>();

    /* Internal routing objects to register and remove the routes. */
    private RouteEntry routeEntry;

    /* Object Types*/
    private Map<String, ObjectClassInfoHelper> objectTypes;

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC)
    protected IDMConnectionFactory connectionFactory;

    void bindConnectionFactory(final IDMConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        // update activityLogger to use the "real" activity logger on the router
        this.activityLogger = new RouterActivityLogger(connectionFactory);
    }

    @SuppressWarnings("unused")
    void unbindConnectionFactory(final IDMConnectionFactory connectionFactory) {
        this.connectionFactory = null;
        // ConnectionFactory has gone away, use null activity logger
        this.activityLogger = NullActivityLogger.INSTANCE;
    }

    /**
     * ConnectorInfoProvider service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected volatile ConnectorInfoProvider connectorInfoProvider = null;

    /**
     * RouterRegistryService service.
     */
    @Reference(policy = ReferencePolicy.STATIC)
    protected RouterRegistry routerRegistry;

    /**
     * Cryptographic service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected volatile CryptoService cryptoService = null;

    /**
     * SyncFailureHandlerFactory service.
     */
    @Reference
    protected SyncFailureHandlerFactory syncFailureHandlerFactory = null;

    /**
     * Enhanced configuration service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    /**
     * Reference to the ThreadSafe {@code ConnectorFacade} instance.
     */
    private final AtomicReference<ConnectorFacade> connectorFacade = new AtomicReference<>();

    @Activate
    protected void activate(ComponentContext context) {
        try {
            factoryPid = (String)context.getProperties().get("config.factory-pid");
            jsonConfiguration = enhancedConfig.getConfigurationAsJson(context);
            systemIdentifier = new SimpleSystemIdentifier(jsonConfiguration);

            if (!jsonConfiguration.get("enabled").defaultTo(true).asBoolean()) {
                logger.info("OpenICF Provisioner Service {} is disabled, \"enabled\" set to false in configuration",
                        systemIdentifier.getName());
                return;
            }

            loadLocalSystemActions(jsonConfiguration);

            connectorReference = ConnectorUtil.getConnectorReference(jsonConfiguration);

            syncFailureHandler = syncFailureHandlerFactory.create(jsonConfiguration.get("syncFailureHandler"));

            final OpenICFProvisionerService provisionerService = this;
            connectorInfoProvider.findConnectorInfoAsync(connectorReference).thenOnResult(
                    new org.forgerock.util.promise.ResultHandler<ConnectorInfo>() {
                        public void handleResult(ConnectorInfo connectorInfo) {
                            try {
                                APIConfiguration config = connectorInfo.createDefaultAPIConfiguration();
                                operationHelperBuilder =
                                        new OperationHelperBuilder(systemIdentifier.getName(), jsonConfiguration,
                                                config, cryptoService);
                                try {
                                    Map<String, Map<Class<? extends APIOperation>, OperationOptionInfoHelper>> objectOperations =
                                            ConnectorUtil.getOperationOptionConfiguration(jsonConfiguration);

                                    objectTypes = ConnectorUtil.getObjectTypes(jsonConfiguration);

                                    for (Map.Entry<String, ObjectClassInfoHelper> entry :
                                            objectTypes.entrySet()) {

                                        objectClassHandlers.put(entry.getKey(),
                                                    new ObjectClassResourceProvider(
                                                            entry.getKey(),
                                                            entry.getValue(),
                                                            objectOperations.get(entry.getKey()),
                                                            provisionerService,
                                                            jsonConfiguration));
                                    }
                                } catch (Exception e) {
                                    logger.error("OpenICF connector jsonConfiguration of {} has errors.", systemIdentifier.getName(), e);
                                    throw new ComponentException(
                                            "OpenICF connector jsonConfiguration has errors and the service can not be initiated.", e);
                                }

                                ConnectorUtil.configureDefaultAPIConfiguration(jsonConfiguration, config, cryptoService);

                                final ConnectorFacade facade = connectorInfoProvider.createConnectorFacade(config);

                                if (null == facade) {
                                    logger.warn("OpenICF ConnectorFacade of {} is not available", connectorReference);
                                } else {
                                    facade.validate();
                                    if (connectorFacade.compareAndSet(null, facade)) {
                                        if (facade.getSupportedOperations().contains(TestApiOp.class)) {
                                            try {
                                                facade.test();
                                                logger.debug("OpenICF connector test of {} succeeded!", systemIdentifier);
                                                serviceAvailable = true;
                                            } catch (InvalidCredentialException e) {
                                                logger.error("Connection error for {} ", systemIdentifier, e);
                                            } catch (Exception e) {
                                                logger.error("OpenICF connector test of {} failed!", systemIdentifier, e);
                                            }
                                        } else {
                                            logger.debug("OpenICF connector of {} does not support test.", connectorReference);
                                            serviceAvailable = true;
                                        }
                                    }
                                }
                                logger.info("OpenICF Provisioner Service component {} is activated.",
                                        systemIdentifier.getName());
                            } catch (Exception e) {
                                logger.warn("Failure to activate connector.", e);
                            }
                        }
                    });

            routeEntry = routerRegistry.addRoute(RouteBuilder.newBuilder()
                    .withTemplate(ProvisionerService.ROUTER_PREFIX + "/" + systemIdentifier.getName())
                    .withSingletonResourceProvider(this)
                    .buildNext()
                    .withModeStartsWith()
                    .withTemplate(ProvisionerService.ROUTER_PREFIX + "/" + systemIdentifier.getName()
                            + ObjectClassRequestHandler.OBJECTCLASS_TEMPLATE)
                    .withRequestHandler(new ObjectClassRequestHandler(objectClassHandlers))
                    .seal());

            logger.info("OpenICF Provisioner Service component {} route enabled{}", systemIdentifier.getName(),
                    (null != connectorFacade.get()
                            ? "."
                            : " although the service is not available yet."));
        } catch (Exception e) {
            logger.error("OpenICF Provisioner Service configuration has errors", e);
            throw new ComponentException("OpenICF Provisioner Service configuration has errors", e);
        }
    }

    @SuppressWarnings("UnusedParameters")
    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (null != connectorFacadeCallback) {
            connectorFacadeCallback.cancel(false);
            connectorFacadeCallback = null;
        }
        if (null != routeEntry) {
            routeEntry.removeRoute();
            routeEntry = null;
        }
        if (connectorFacade.get() instanceof LocalConnectorFacadeImpl) {
            ((LocalConnectorFacadeImpl) connectorFacade.get()).dispose();
        }
        connectorFacade.set(null);
        logger.info("OpenICF Provisioner Service component {} is deactivated.", systemIdentifier.getName());
        systemIdentifier = null;
    }

    private void loadLocalSystemActions(JsonValue configuration) {
        // TODO delay initialization /config/system

        if (configuration.isDefined("systemActions")) {
            for (JsonValue actionValue : configuration.get("systemActions").expect(List.class)) {
                SystemAction action = new SystemAction(actionValue);
                localSystemActionCache.put(action.getName(), action);
            }
        }
    }

    ConnectorFacade getConnectorFacade() {
        return connectorFacade.get();
    }

    private enum ConnectorAction {
        script, test, livesync
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        return new NotSupportedException("Read operations are not supported").asPromise();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(
            final Context context, final ActionRequest request) {
        try {
            switch (request.getActionAsEnum(ConnectorAction.class)) {
                case script:
                    return handleScriptAction(request);
                case test:
                    return handleTestAction(context, request);
                case livesync:
                    return handleLiveSyncAction(context, request);
                default:
                    return new BadRequestException("Unsupported action: " + request.getAction()).asPromise();
            }
        } catch (ConnectorException e) {
            return ExceptionHelper.adaptConnectorException(context, request, e, null, request.getResourcePath(),
                    null, null, activityLogger)
                    .asPromise();
        } catch (JsonValueException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (IllegalArgumentException e) {
            // from request.getActionAsEnum
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        return new NotSupportedException("Patch operations are not supported").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        return new NotSupportedException("Update operations are not supported").asPromise();
    }

    private Promise<ActionResponse, ResourceException> handleScriptAction(final ActionRequest request) {
        try {
            final String scriptId = request.getAdditionalParameter(SystemAction.SCRIPT_ID);
            if (StringUtils.isBlank(scriptId)) {
                return new BadRequestException("Missing required parameter: " + SystemAction.SCRIPT_ID).asPromise();
            }

            if (!localSystemActionCache.containsKey(scriptId)) {
                return new BadRequestException("Script ID: " + scriptId + " is not defined.").asPromise();
            }

            SystemAction action = localSystemActionCache.get(scriptId);

            String systemType = connectorReference.getConnectorKey().getConnectorName();
            final List<ScriptContextBuilder> scriptContextBuilderList = action.getScriptContextBuilders(systemType);

            if (scriptContextBuilderList.isEmpty()) {
                return new BadRequestException("Script ID: " + scriptId + " for systemType " + systemType
                        + " is not defined.")
                        .asPromise();
            }

            JsonValue result = new JsonValue(new HashMap<String, Object>());

            boolean onConnector = !"resource".equalsIgnoreCase(
                    request.getAdditionalParameter(SystemAction.SCRIPT_EXECUTE_MODE));

            final ConnectorFacade facade = getConnectorFacade0(onConnector
                    ? ScriptOnConnectorApiOp.class
                    : ScriptOnResourceApiOp.class);

            String variablePrefix = request.getAdditionalParameter(SystemAction.SCRIPT_VARIABLE_PREFIX);

            List<Map<String, Object>> resultList = new ArrayList<>(scriptContextBuilderList.size());
            result.put("actions", resultList);

            for (ScriptContextBuilder contextBuilder : scriptContextBuilderList) {
                boolean isShell = contextBuilder.getScriptLanguage().equalsIgnoreCase("Shell");
                for (Entry<String, String> entry : request.getAdditionalParameters().entrySet()) {
                    final String key = entry.getKey();
                    if (SystemAction.SCRIPT_PARAMS.contains(key)) {
                        continue;
                    }
                    String value = entry.getValue();
                    Object newValue = value;
                    if (isShell) {
                        if ("password".equalsIgnoreCase(key)) {
                            if (value != null) {
                                newValue = new GuardedString(value.toCharArray());
                            } else {
                                return new BadRequestException("Invalid type for password.").asPromise();
                            }
                        }
                        if ("username".equalsIgnoreCase(key) && value == null) {
                            return new BadRequestException("Invalid type for username.").asPromise();
                        }
                        if ("workingdir".equalsIgnoreCase(key) && value == null) {
                            return new BadRequestException("Invalid type for workingdir.").asPromise();
                        }
                        if ("timeout".equalsIgnoreCase(key) && value == null) {
                            return new BadRequestException("Invalid type for timeout.").asPromise();
                        }
                        contextBuilder.addScriptArgument(key, newValue);
                        continue;
                    }

                    contextBuilder.addScriptArgument(key, newValue);
                }

                JsonValue content = request.getContent();
                // if there is no content(content.isNull()), skip adding content to script arguments
                if (content.isMap()) {
                    for (Entry<String, Object> entry : content.asMap().entrySet()) {
                        contextBuilder.addScriptArgument(entry.getKey(), entry.getValue());
                    }
                } else if (!content.isNull()) {
                    return new BadRequestException("Content is not of type Map").asPromise();
                }

                // ScriptContext scriptContext = script.getScriptContextBuilder().build();
                OperationOptionsBuilder operationOptionsBuilder = new OperationOptionsBuilder();

                // It's necessary to keep the backward compatibility with Waveset IDM
                if (null != variablePrefix && isShell) {
                    operationOptionsBuilder.setOption("variablePrefix", variablePrefix);
                }

                Map<String, Object> actionResult = new HashMap<>(2);
                try {
                    final Object scriptResult;
                    if (onConnector) {
                        scriptResult = facade.runScriptOnConnector(
                                contextBuilder.build(), operationOptionsBuilder.build());
                    } else {
                        scriptResult = facade.runScriptOnResource(
                                contextBuilder.build(), operationOptionsBuilder.build());
                    }
                    actionResult.put("result", ConnectorUtil.coercedTypeCasting(scriptResult, Object.class));
                } catch (Exception e) {
                    logger.error("Script execution error.", e);
                    actionResult.put("error", e.getMessage());
                }
                resultList.add(actionResult);
            }
            return newActionResponse(result).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @SuppressWarnings("UnusedParameters")
    private Promise<ActionResponse, ResourceException> handleTestAction(Context context, ActionRequest request) {
        return newActionResponse(new JsonValue(getStatus(context))).asPromise();
    }

    private Promise<ActionResponse, ResourceException> handleLiveSyncAction(
            final Context context, final ActionRequest request) {

        final String objectTypeName = getObjectTypeName(ObjectClass.ALL);

        if (objectTypeName == null) {
            return new BadRequestException("__ALL__ object class is not configured").asPromise();
        }

        final ActionRequest forwardRequest = Requests.newActionRequest(getSource(objectTypeName), request.getAction());

        // forward request to be handled in ObjectClassResourceProvider#actionCollection
        try {
            return connectionFactory.getConnection().action(context, forwardRequest).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /**
     * Checks the {@code operation} permission before execution.
     *
     * @param operation operation for which to retrieve a facade
     * @return if {@code denied} is true and the {@code onDeny} equals
     *         {@link org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper.OnActionPolicy#ALLOW}
     *         returns false else true
     * @throws ResourceException
     *             if {@code denied} is true and the {@code onDeny} equals
     *             {@link org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper.OnActionPolicy#THROW_EXCEPTION}
     */
    private ConnectorFacade getConnectorFacade0(Class<? extends APIOperation> operation) throws ResourceException {
        final ConnectorFacade facade = getConnectorFacade();
        if (null == facade) {
            throw new ServiceUnavailableException();
        }

        if (null == facade.getOperation(operation)) {
            throw new NotSupportedException("Operation " + operation.getCanonicalName()
                            + " is not supported by the Connector");
        }
        return facade;
    }

    /**
     * Gets the unique {@link org.forgerock.openidm.provisioner.SystemIdentifier} of this instance.
     * <p/>
     * The service which refers to this service instance can distinguish between multiple instances by this value.
     *
     * @return the system identifier
     */
    public SystemIdentifier getSystemIdentifier() {
        return systemIdentifier;
    }

    public String getSystemIdentifierName() {
        return systemIdentifier.getName();
    }

    /**
     * Gets the fully qualified path name
     * @param objectClass the object class for the intended resource
     * @param optionalId ids to append to the fully qualified path
     * @return the fully qualified path name
     */
    String getSource(final String objectClass, final String... optionalId) {
        final StringBuilder sb = new StringBuilder("system")
                .append("/")
                .append(systemIdentifier.getName())
                .append("/")
                .append(objectClass);
        for (String id : optionalId) {
            sb.append("/").append(id);
        }
        return sb.toString();
    }

    /**
     * Gets a brief status report about the current status of this service instance.
     * <p>
     * An example response when the configuration is enabled
     * {@code {
     * "name" : "ldap",
     * "enabled" : true,
     * "config" : "config/provisioner.openicf/ldap"
     * "objectTypes":
     * [
     *  "group",
     *  "account"
     * ],
     * "connectorRef" :
     *  {
     *      "connectorName": "org.identityconnectors.ldap.LdapConnector",
     *      "bundleName": "org.forgerock.openicf.connectors.ldap-connector",
     *      "bundleVersion": "[1.1.0.1,1.1.2.0)"
     *  } ,
     * "ok" : true
     * }}
     *
     * An example response when the configuration is disabled
     * {@code {
     * "name": "ldap",
     * "enabled": false,
     * "config": "config/provisioner.openicf/ldap",
     * "objectTypes":
     * [
     *  "group",
     *  "account"
     * ],
     * "connectorRef":
     * {
     *      "connectorName": "org.identityconnectors.ldap.LdapConnector",
     *      "bundleName": "org.forgerock.openicf.connectors.ldap-connector",
     *      "bundleVersion": "[1.4.0.0,2.0.0.0)"
     * },
     * "error": "connector not available",
     * "ok": false
     * }}
     *
     * @param context the Context of the request requesting the status
     * @return a Map of the current status of a connector
     */
    public Map<String, Object> getStatus(Context context) {
        Map<String, Object> result = new LinkedHashMap<>();
        JsonValue jv = new JsonValue(result);
        boolean ok = false;

        jv.put("name", systemIdentifier.getName());
        jv.put("enabled", jsonConfiguration.get("enabled").defaultTo(Boolean.TRUE).asBoolean());
        jv.put("config", "config/provisioner.openicf/" + factoryPid);
        jv.put("objectTypes", ConnectorUtil.getObjectTypes(jsonConfiguration).keySet());
        ConnectorReference connectorReference = ConnectorUtil.getConnectorReference(jsonConfiguration);
        if (connectorReference != null) {
            jv.put(ConnectorUtil.OPENICF_CONNECTOR_REF, ConnectorUtil.getConnectorKey(
                    connectorReference.getConnectorKey()));
            ConnectorInfo connectorInfo = connectorInfoProvider.findConnectorInfo(connectorReference);
            if (connectorInfo != null) {
                jv.put("displayName", connectorInfo.getConnectorDisplayName());
            }
        }

        try {
            ConnectorFacade connectorFacade = getConnectorFacade();
            if (connectorFacade == null) {
                jv.put("error", "connector not available");
            } else {
                connectorFacade.test();
                ok = true;
            }
        } catch (UnsupportedOperationException e) {
            jv.put("error", "TEST UnsupportedOperation");
        } catch (InvalidCredentialException e) {
            jv.put("error", "Connection Error");
        } catch (Exception e) {
            jv.put("error", e.getMessage());
        }

        jv.put("ok", ok);
        return result;
    }

    public Map<String, Object> testConfig(JsonValue config) {
        JsonValue jv = json(object());
        jv.put("name", systemIdentifier.getName());
        jv.put("ok", false);
        SimpleSystemIdentifier testIdentifier;
        ConnectorReference connectorReference;
        try {
            testIdentifier = new SimpleSystemIdentifier(config);
            connectorReference = ConnectorUtil.getConnectorReference(jsonConfiguration);
        } catch (JsonValueException e) {
            jv.put("error", "OpenICF Provisioner Service jsonConfiguration has errors: " + e.getMessage());
            return jv.asMap();
        }

        ConnectorInfo connectorInfo = connectorInfoProvider.findConnectorInfo(connectorReference);
        if (null != connectorInfo) {
            ConnectorFacade facade;
            try {
                OperationHelperBuilder ohb = new OperationHelperBuilder(testIdentifier.getName(), config,
                        connectorInfo.createDefaultAPIConfiguration(), cryptoService);
                ConnectorFacadeFactory connectorFacadeFactory = ConnectorFacadeFactory.getInstance();
                facade = connectorFacadeFactory.newInstance(ohb.getRuntimeAPIConfiguration());
            } catch (Exception e) {
                jv.put("error", "OpenICF connector jsonConfiguration has errors: " + e.getMessage());
                return jv.asMap();
            }

            if (null != facade && facade.getSupportedOperations().contains(TestApiOp.class)) {
                try {
                    facade.test();
                } catch (UnsupportedOperationException e) {
                    jv.put("reason", "TEST UnsupportedOperation");
                } catch (InvalidCredentialException e) {
                    jv.put("error", "Connection Error");
                } catch (Exception e) {
                    jv.put("error", e.getMessage());
                    return jv.asMap();
                }
                jv.put("ok", true);
            } else if (null == facade) {
                jv.put("error", "OpenICF ConnectorFacade of " + connectorReference + " is not available");
            } else {
                jv.put("error", "OpenICF connector of " + connectorReference + " does not support test.");
            }
        } else if (connectorReference.getConnectorLocation().equals(ConnectorReference.ConnectorLocation.LOCAL)) {
            jv.put("error", "OpenICF ConnectorInfo can not be loaded for " + connectorReference + " from #LOCAL");
        } else {
            jv.put("error", "OpenICF ConnectorInfo for " + connectorReference + " is not available yet.");
        }
        return jv.asMap();
    }

    /**
     * Get the corresponding object type name from provisioner config
     * @param objectClass the objectClass to get the name of
     * @return the name of the objectClass or null if not found
     */
    protected String getObjectTypeName(final ObjectClass objectClass) {
        if (objectClass == null) {
            return null;
        }
        final Predicate<Entry<String, ObjectClassInfoHelper>> objectClassFilter = new Predicate<Entry<String, ObjectClassInfoHelper>>() {
            public boolean apply(Entry<String, ObjectClassInfoHelper> entry) {
                return objectClass.equals(entry.getValue().getObjectClass());
            }
        };

        final Iterable<Entry<String, ObjectClassInfoHelper>> objectClasses =
                FluentIterable.from(objectTypes.entrySet()).filter(objectClassFilter);

        return objectClasses.iterator().hasNext() ? objectClasses.iterator().next().getKey() : null;
    }

    /**
     * This newBuilder and this method can not be scheduled. The call MUST go
     * through the {@code org.forgerock.openidm.provisioner}
     * <p/>
     * Invoked by the scheduler when the scheduler triggers.
     * <p/>
     * Synchronization object: {@code "connectorData" : "syncToken" :
     * "1305555929000", "nativeType" : "JAVA_TYPE_LONG" },
     * "synchronizationStatus" : { "errorStatus" : null, "lastKnownServer" :
     * "localServer", "lastModDate" : "2011-05-16T14:47:58.587Z", "lastModNum" :
     * 668, "lastPollDate" : "2011-05-16T14:47:52.875Z", "lastStartTime" :
     * "2011-05-16T14:29:07.863Z", "progressMessage" : "SUCCEEDED" } }}
     * <p/>
     * {@inheritDoc} Synchronize the changes from the end system for the given
     * {@code objectType}.
     * <p/>
     * OpenIDM takes active role in the synchronization process by asking the
     * end system to get all changed object. Not all systems are capable to
     * fulfill this kind of request but if the end system is capable then the
     * implementation sends each change to a new request on the router and when
     * it is finished, it returns a new <b>stage</b> object.
     * <p/>
     * The {@code previousStage} object is the previously returned value of this
     * method.
     *
     * @param context the request context associated with the invocation
     * @param previousStage
     *            The previously returned object. If null then it's the first
     *            execution.
     * @return The new updated stage object. This will be the
     *         {@code previousStage} at buildNext call.
     * @throws IllegalArgumentException
     *             if the value of {@code connectorData} can not be converted to
     *             {@link SyncToken}.
     * @throws UnsupportedOperationException
     *             if the {@link SyncApiOp} operation is not implemented in
     *             connector.
     * @throws org.forgerock.json.JsonValueException
     *             if the {@code previousStage} is not Map.
     * @see {@link ConnectorUtil#convertToSyncToken(org.forgerock.json.JsonValue)}
     *      or any exception happed inside the connector.
     */
    public JsonValue liveSynchronize(final Context context, final String objectType, final JsonValue previousStage)
            throws ResourceException {

        if (!serviceAvailable) {
            return previousStage;
        }

        final JsonValue stage = previousStage != null
            ? previousStage.copy()
            : new JsonValue(new LinkedHashMap<String, Object>());

        JsonValue connectorData = stage.get("connectorData");
        SyncToken token = null;
        if (!connectorData.isNull()) {
            if (connectorData.isMap()) {
                token = ConnectorUtil.convertToSyncToken(connectorData);
            } else {
                throw new IllegalArgumentException("Illegal connectorData property. Value must be Map");
            }
        }
        stage.remove("lastException");

        try {
            final SyncRetry syncRetry = new SyncRetry();
            final OperationHelper helper = operationHelperBuilder.build(objectType, stage, cryptoService);

            if (helper.isOperationPermitted(SyncApiOp.class)) {
                ConnectorFacade connector = getConnectorFacade();
                SyncApiOp operation = (SyncApiOp) connector.getOperation(SyncApiOp.class);
                if (null == operation) {
                    throw new UnsupportedOperationException(SyncApiOp.class.getCanonicalName());
                }
                if (null == token) {
                    token = operation.getLatestSyncToken(helper.getObjectClass());
                    logger.debug("New LatestSyncToken has been fetched. New token is: {}", token);
                } else {
                    final SyncToken[] lastToken = new SyncToken[]{token};
                    final String[] failedRecord = new String[1];
                    OperationOptionsBuilder operationOptionsBuilder =
                            helper.getOperationOptionsBuilder(SyncApiOp.class, null, previousStage);

                    try {
                        logger.debug("Execute sync(ObjectClass:{}, SyncToken:{})",
                                new Object[] { helper.getObjectClass().getObjectClassValue(), token });
                        SyncToken syncToken = operation.sync(helper.getObjectClass(), token,
                                new SyncResultsHandler() {
                                    /**
                                     * Called to handle a delta in the stream. The Connector framework will call
                                     * this method multiple times, once for each result.
                                     * Although this method is callback, the framework will invoke it synchronously.
                                     * Thus, the framework guarantees that once an application's call to
                                     * {@link org.identityconnectors.framework.api.operations.SyncApiOp#sync(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.SyncToken, org.identityconnectors.framework.common.objects.SyncResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)} SyncApiOp#sync() returns,
                                     * the framework will no longer call this method
                                     * to handle results from that <code>sync()</code> operation.
                                     *
                                     * @param syncDelta The change
                                     * @return True iff the application wants to continue processing more results.
                                     * @throws RuntimeException If the application encounters an exception. This will
                                     * stop iteration and the exception will propagate to the application.
                                     */
                                    @SuppressWarnings("fallthrough")
                                    public boolean handle(SyncDelta syncDelta) {
                                        try {
                                            // Q: are we going to encode ids?
                                            final String resourceId = syncDelta.getUid().getUidValue();
                                            final String objectTypeName = getObjectTypeName(syncDelta.getObjectClass());
                                            final String resourceContainer = getSource(objectTypeName == null ? objectType : objectTypeName);
                                            final JsonValue content = new JsonValue(new LinkedHashMap<String, Object>(2));

                                            //rebuild the OperationHelper if the helper is for the __ALL__ object class
                                            final OperationHelper syncDeltaOperationHelper = helper.getObjectClass().equals(ObjectClass.ALL)
                                                    ? operationHelperBuilder.build(objectTypeName, stage, cryptoService)
                                                    : helper;

                                            switch (syncDelta.getDeltaType()) {
                                                case CREATE: {
                                                    JsonValue deltaObject = syncDeltaOperationHelper.build(syncDelta.getObject());
                                                    content.put("oldValue", null);
                                                    content.put("newValue", deltaObject.getObject());
                                                    // TODO import SynchronizationService.Action.notifyCreate and ACTION_PARAM_ constants
                                                    ActionRequest onCreateRequest = Requests.newActionRequest("sync", "notifyCreate")
                                                            .setAdditionalParameter("resourceContainer", resourceContainer)
                                                            .setAdditionalParameter("resourceId", resourceId)
                                                            .setContent(content);
                                                    connectionFactory.getConnection().action(context, onCreateRequest);

                                                    activityLogger.log(context, onCreateRequest,
                                                                    "sync-create", onCreateRequest.getResourcePath(),
                                                                    deltaObject, deltaObject, Status.SUCCESS);
                                                    break;
                                                }
                                                case UPDATE:
                                                case CREATE_OR_UPDATE: {
                                                    JsonValue deltaObject = syncDeltaOperationHelper.build(syncDelta.getObject());
                                                    content.put("oldValue", null);
                                                    content.put("newValue", deltaObject.getObject());
                                                    if (null != syncDelta.getPreviousUid()) {
                                                        deltaObject.put("_previous-id", syncDelta.getPreviousUid().getUidValue());
                                                    }
                                                    // TODO import SynchronizationService.Action.notifyUpdate and ACTION_PARAM_ constants
                                                    ActionRequest onUpdateRequest = Requests.newActionRequest("sync", "notifyUpdate")
                                                            .setAdditionalParameter("resourceContainer", resourceContainer)
                                                            .setAdditionalParameter("resourceId", resourceId)
                                                            .setContent(content);
                                                    connectionFactory.getConnection().action(context, onUpdateRequest);

                                                    activityLogger.log(context, onUpdateRequest,
                                                            "sync-update", onUpdateRequest.getResourcePath(),
                                                            deltaObject, deltaObject, Status.SUCCESS);
                                                    break;
                                                }
                                                case DELETE:
                                                    // TODO Pass along the old deltaObject - do we have it?
                                                    content.put("oldValue", null);
                                                    // TODO import SynchronizationService.Action.notifyDelete and ACTION_PARAM_ constants
                                                    ActionRequest onDeleteRequest = Requests.newActionRequest("sync", "notifyDelete")
                                                            .setAdditionalParameter("resourceContainer", resourceContainer)
                                                            .setAdditionalParameter("resourceId", resourceId)
                                                            .setContent(content);
                                                    connectionFactory.getConnection().action(context, onDeleteRequest);

                                                    activityLogger.log(context, onDeleteRequest,
                                                            "sync-delete", onDeleteRequest.getResourcePath(),
                                                            null, null, Status.SUCCESS);
                                                    break;
                                            }
                                        } catch (Exception e) {
                                            failedRecord[0] = SerializerUtil.serializeXmlObject(syncDelta, true);
                                            logger.debug("Failed to synchronize {} object, handle failure using {}",
                                                    syncDelta.getUid(), syncFailureHandler, e);
                                            Map<String, Object> syncFailureMap = new HashMap<>(6);
                                            syncFailureMap.put("token", syncDelta.getToken().getValue());
                                            syncFailureMap.put("systemIdentifier", systemIdentifier.getName());
                                            syncFailureMap.put("objectType", objectType);
                                            syncFailureMap.put("uid", syncDelta.getUid().getUidValue());
                                            syncFailureMap.put("failedRecord", failedRecord[0]);
                                            try {
                                                syncFailureHandler.invoke(context, syncFailureMap, e);
                                            } catch (SyncHandlerException syncHandlerException) {
                                                // Current contract of the failure handler is that throwing this exception indicates 
                                                // that it should retry for this entry
                                                syncRetry.setValue(true);
                                                syncRetry.setThrowable(syncHandlerException);
                                                logger.debug("Sync failure handler indicated to stop current change set processing until retry handling: {}", 
                                                        syncHandlerException.getMessage(), syncHandlerException);
                                            }
                                        }

                                        if (syncRetry.getValue()) {
                                            // Stop the processing of this result set. Next retry will start again after last token.
                                            return false; 
                                        } else {
                                            // success (either by original sync or by failure handler)
                                            // Continue the processing of the rest of the result set
                                            lastToken[0] = syncDelta.getToken();
                                            return true;
                                        }
                                    }
                        }, operationOptionsBuilder.build());
                        if (syncRetry.getValue()) {
                            Throwable throwable = syncRetry.getThrowable();
                            Map<String, Object> lastException = new LinkedHashMap<>(2);
                            lastException.put("throwable", throwable.getMessage());
                            if (null != failedRecord[0]) {
                                lastException.put("syncDelta", failedRecord[0]);
                            }
                            stage.put("lastException", lastException);
                            logger.debug("Live synchronization of {} failed on {}",
                                    new Object[] { objectType, systemIdentifier.getName() }, throwable);
                        } else {
                            if (syncToken != null) {
                                lastToken[0] = syncToken;
                            }
                        }
                    } finally {
                        token = lastToken[0];
                        logger.debug("Synchronization is finished. New LatestSyncToken value: {}", token);
                    }
                }
                if (null != token) {
                    stage.put("connectorData", ConnectorUtil.convertFromSyncToken(token));
                }
            }
        } catch (ResourceException e) {
            logger.debug("Failed to get OperationHelper", e);
            throw new RuntimeException(e);
        }  catch (UnsupportedOperationException e) {
            logger.debug("Failed to get OperationOptionsBuilder", e);
            throw new NotFoundException("Failed to get latest sync token", e).setDetail(new JsonValue(e.getMessage()));
        }  catch (Exception e) {
            logger.debug("Failed to get OperationOptionsBuilder", e);
            throw new InternalServerErrorException("Failed to get OperationOptionsBuilder: " + e.getMessage(), e);
        }
        return stage;
    }

    /**
     * Package level setter to allow unit tests to set the logger.
     * @param activityLogger the new activity logger
     */
    void setActivityLogger(ActivityLogger activityLogger) {
        this.activityLogger = activityLogger;
    }

    ActivityLogger getActivityLogger() {
        return activityLogger;
    }

    CryptoService getCryptoService() {
        return cryptoService;
    }
}