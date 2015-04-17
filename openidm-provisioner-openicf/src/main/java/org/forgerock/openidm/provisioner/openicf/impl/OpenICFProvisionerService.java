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
package org.forgerock.openidm.provisioner.openicf.impl;

import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.and;
import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.contains;
import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.containsAllValues;
import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.endsWith;
import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.equalTo;
import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.greaterThan;
import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.greaterThanOrEqualTo;
import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.lessThan;
import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.lessThanOrEqualTo;
import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.not;
import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.or;
import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.startsWith;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.audit.util.NullActivityLogger;
import org.forgerock.openidm.audit.util.RouterActivityLogger;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.SimpleSystemIdentifier;
import org.forgerock.openidm.provisioner.SystemIdentifier;
import org.forgerock.openidm.provisioner.impl.SystemObjectSetService;
import org.forgerock.openidm.provisioner.openicf.ConnectorInfoProvider;
import org.forgerock.openidm.provisioner.openicf.ConnectorReference;
import org.forgerock.openidm.provisioner.openicf.OperationHelper;
import org.forgerock.openidm.provisioner.openicf.commons.AttributeMissingException;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper;
import org.forgerock.openidm.provisioner.openicf.internal.ConnectorFacadeCallback;
import org.forgerock.openidm.provisioner.openicf.internal.SystemAction;
import org.forgerock.openidm.provisioner.openicf.syncfailure.SyncFailureHandler;
import org.forgerock.openidm.provisioner.openicf.syncfailure.SyncFailureHandlerFactory;
import org.forgerock.openidm.provisioner.openicf.syncfailure.SyncHandlerException;
import org.forgerock.openidm.router.RouteBuilder;
import org.forgerock.openidm.router.RouteEntry;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.openidm.router.RouterRegistry;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.openidm.util.ResourceUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.AuthenticationApiOp;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.api.operations.DeleteApiOp;
import org.identityconnectors.framework.api.operations.GetApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnConnectorApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnResourceApiOp;
import org.identityconnectors.framework.api.operations.SearchApiOp;
import org.identityconnectors.framework.api.operations.SyncApiOp;
import org.identityconnectors.framework.api.operations.TestApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectionBrokenException;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.ConnectorSecurityException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;
import org.identityconnectors.framework.common.exceptions.PasswordExpiredException;
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException;
import org.identityconnectors.framework.common.exceptions.PreconditionFailedException;
import org.identityconnectors.framework.common.exceptions.PreconditionRequiredException;
import org.identityconnectors.framework.common.exceptions.RetryableException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.ScriptContextBuilder;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SortKey;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.serializer.SerializerUtil;
import org.identityconnectors.framework.impl.api.remote.RemoteWrappedException;
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
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM OpenICF Provisioner Service")
})
public class OpenICFProvisionerService implements ProvisionerService, SingletonResourceProvider {

    // Public Constants
    public static final String PID = "org.forgerock.openidm.provisioner.openicf";

    //Private Constants
    private static final String REAUTH_HEADER = "X-OpenIDM-Reauth-Password";
    private static final String RUN_AS_USER = "runAsUser";
    private static final String ACCOUNT_USERNAME_ATTRIBUTES = "accountUserNameAttributes";

    private static final Logger logger = LoggerFactory.getLogger(OpenICFProvisionerService.class);

    // Monitoring event name prefix
    private static final String EVENT_PREFIX = "openidm/internal/system/";

    private static final int UNAUTHORIZED_ERROR_CODE = 401;

    private SimpleSystemIdentifier systemIdentifier = null;
    private OperationHelperBuilder operationHelperBuilder = null;
    private ConnectorFacadeCallback connectorFacadeCallback = null;
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
    private final ConcurrentMap<String, SystemAction> localSystemActionCache =
            new ConcurrentHashMap<String, SystemAction>();

    private final ConcurrentMap<String, RequestHandler> objectClassHandlers =
            new ConcurrentHashMap<String, RequestHandler>();

    /* Internal routing objects to register and remove the routes. */
    private RouteEntry routeEntry;

    /* Object Types*/
    private Map<String, ObjectClassInfoHelper> objectTypes;

    /**
     * Holder of non ObjectClass operations:
     *
     * <pre>
     * ValidateApiOp
     * TestApiOp
     * ScriptOnConnectorApiOp
     * ScriptOnResourceApiOp
     * SchemaApiOp
     * </pre>
     */
    private Map<Class<? extends APIOperation>, OperationOptionInfoHelper> systemOperations = null;

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    void bindConnectionFactory(final ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        // update activityLogger to use the "real" activity logger on the router
        this.activityLogger = new RouterActivityLogger(connectionFactory);
    }

    void unbindConnectionFactory(final ConnectionFactory connectionFactory) {
        this.connectionFactory = null;
        // ConnectionFactory has gone away, use null activity logger
        this.activityLogger = NullActivityLogger.INSTANCE;
    }

    @Reference(target = "(" + ServerConstants.ROUTER_PREFIX + "=/*)")
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

    /**
     * ConnectorInfoProvider service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected ConnectorInfoProvider connectorInfoProvider = null;

    /**
     * RouterRegistryService service.
     */
    @Reference(policy = ReferencePolicy.STATIC)
    protected RouterRegistry routerRegistry;

    /**
     * Cryptographic service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected CryptoService cryptoService = null;

    /**
     * SyncFailureHandlerFactory service.
     */
    @Reference
    protected SyncFailureHandlerFactory syncFailureHandlerFactory = null;

    /**
     * Reference to the ThreadSafe {@code ConnectorFacade} instance.
     */
    private final AtomicReference<ConnectorFacade> connectorFacade =
            new AtomicReference<ConnectorFacade>();

    @Activate
    protected void activate(ComponentContext context) {
        try {
            factoryPid = (String)context.getProperties().get("config.factory-pid");
            jsonConfiguration = JSONEnhancedConfig.newInstance().getConfigurationAsJson(context);
            systemIdentifier = new SimpleSystemIdentifier(jsonConfiguration);

            if (!jsonConfiguration.get("enabled").defaultTo(true).asBoolean()) {
                logger.info("OpenICF Provisioner Service {} is disabled, \"enabled\" set to false in configuration",
                        systemIdentifier.getName());
                return;
            }

            loadLocalSystemActions(jsonConfiguration);

            connectorReference = ConnectorUtil.getConnectorReference(jsonConfiguration);

            syncFailureHandler = syncFailureHandlerFactory.create(jsonConfiguration.get("syncFailureHandler"));

            if (connectorInfoProvider.findConnectorInfo(connectorReference) == null) {
                if (connectorReference.getConnectorLocation().isLocal()) {
                    // Not possible to satisfy the connector reference, bail out
                    throw new InvalidException("Connector not found: " + connectorReference.getConnectorKey());
                } else {
                    // Connector may become available later
                    logger.warn("Remote OpenICF Connector {} could not be located, may not yet be connected to " +
                            "the remote connector server.", connectorReference);
                }
            }

            connectorFacadeCallback = new ConnectorFacadeCallback() {
                @Override
                public void addingConnectorInfo(ConnectorInfo connectorInfo,
                                                ConnectorFacadeFactory facadeFactory) {
                    try {
                        APIConfiguration config = connectorInfo.createDefaultAPIConfiguration();
                        operationHelperBuilder = new OperationHelperBuilder(systemIdentifier.getName(), jsonConfiguration,
                                config, cryptoService);
                        try {
                            // TODO Iterate over the supported type and register
                            boolean allowModification = !jsonConfiguration.get("readOnly").defaultTo(false).asBoolean();
                            if (!allowModification) {
                                logger.debug("OpenICF Provisioner Service {} is running in read-only mode", systemIdentifier.getName());
                            }

                            Map<String, Map<Class<? extends APIOperation>, OperationOptionInfoHelper>> objectOperations =
                                    ConnectorUtil.getOperationOptionConfiguration(jsonConfiguration);

                            objectTypes = ConnectorUtil.getObjectTypes(jsonConfiguration);

                            for (Map.Entry<String, ObjectClassInfoHelper> entry :
                                    objectTypes.entrySet()) {

                                objectClassHandlers.put(entry.getKey(),
                                        Resources.newCollection(
                                                new ObjectClassResourceProvider(
                                                        entry.getKey(),
                                                        entry.getValue(),
                                                        objectOperations.get(entry.getKey()),
                                                        allowModification)));
                            }

                            // TODO Fix this Map
                            // ValidateApiOp
                            // TestApiOp
                            // ScriptOnConnectorApiOp
                            // ScriptOnResourceApiOp
                            // SchemaApiOp
                            systemOperations = Collections.emptyMap();
                        } catch (Exception e) {
                            logger.error("OpenICF connector jsonConfiguration of {} has errors.", systemIdentifier.getName(), e);
                            throw new ComponentException(
                                    "OpenICF connector jsonConfiguration has errors and the service can not be initiated.", e);
                        }

                        ConnectorUtil.configureDefaultAPIConfiguration(jsonConfiguration, config, cryptoService);

                        final ConnectorFacade facade = facadeFactory.newInstance(config);

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
                                    } catch (Exception e) {
                                        logger.error("OpenICF connector test of {} failed!", systemIdentifier, e);
                                    }
                                } else {
                                    logger.debug("OpenICF connector of {} does not support test.", connectorReference);
                                    serviceAvailable = true;
                                }
                            }
                        }
                    } catch (Throwable t) {
                        logger.warn(t.getMessage());
                    } finally {
                        logger.info("OpenICF Provisioner Service component {} is activated{}",
                                systemIdentifier.getName(),
                                (null != connectorFacade.get()
                                        ? "."
                                        : " although the service is not available yet."));
                    }
                }

                @Override
                public void removedConnectorInfo(ConnectorInfo connectorInfo) {
                    connectorFacade.set(null);
                }
            };

            connectorInfoProvider.addConnectorFacadeCallback(connectorReference, connectorFacadeCallback);

            routeEntry = routerRegistry.addRoute(RouteBuilder.newBuilder()
                    .withTemplate("/system/" + systemIdentifier.getName())
                    .withSingletonResourceProvider(this)
                    .buildNext()
                    .withModeStartsWith()
                    .withTemplate("/system/" + systemIdentifier.getName() + ObjectClassRequestHandler.OBJECTCLASS_TEMPLATE)
                    .withRequestHandler(new ObjectClassRequestHandler())
                    .seal());

            logger.info("OpenICF Provisioner Service component {} is activated{}", systemIdentifier.getName(),
                    (null != connectorFacade.get()
                            ? "."
                            : " although the service is not available yet."));
        } catch (Exception e) {
            logger.error("OpenICF Provisioner Service configuration has errors", e);
            throw new ComponentException("OpenICF Provisioner Service configuration has errors", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (null != connectorFacadeCallback) {
            connectorInfoProvider.deleteConnectorFacadeCallback(connectorFacadeCallback);
            connectorFacadeCallback = null;
        }
        if (null != routeEntry) {
            routeEntry.removeRoute();
            routeEntry = null;
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

    /**
     * Handle ConnectorExceptions from ConnectorFacade invocations.  Maps each ConnectorException subtype to the
     * appropriate {@link ResourceException} for passing to {@code handleError}.  Optionally logs to activity log.
     *
     * @param context the ServerContext from the original request
     * @param request the original request
     * @param exception the ConnectorException that was thrown by the facade
     * @param resourceId the resourceId being operated on
     * @param before the object value "before" the request
     * @param after the object value "after" the request
     * @param handler the ResultHandler on which to call handleError
     * @param connectorExceptionActivityLogger the ActivityLogger to use to log the exception
     */
    private void handleConnectorException(ServerContext context, Request request, ConnectorException exception,
            String resourceContainer, String resourceId, JsonValue before, JsonValue after, ResultHandler<?> handler,
            ActivityLogger connectorExceptionActivityLogger) {

        // default message
        String message = MessageFormat.format("Operation {0} failed with {1} on system object: {2}",
                request.getRequestType(), exception.getClass().getSimpleName(), resourceId);

        try {
            throw exception;
        } catch (AlreadyExistsException e) {
            message = MessageFormat.format("System object {0} already exists", resourceId);
            handler.handleError(new ConflictException(message, exception));
        } catch (ConfigurationException e) {
            message = MessageFormat.format("Operation {0} failed with ConfigurationException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(new InternalServerErrorException(message, exception));
        } catch (ConnectionBrokenException e) {
            message = MessageFormat.format("Operation {0} failed with ConnectionBrokenException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(new ServiceUnavailableException(message, exception));
        } catch (ConnectionFailedException e) {
            message = MessageFormat.format("Connection failed during operation {0} on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(new ServiceUnavailableException(message, exception));
        } catch (ConnectorIOException e) {
            message = MessageFormat.format("Operation {0} failed with ConnectorIOException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(new ServiceUnavailableException(message, exception));
        } catch (OperationTimeoutException e) {
            message = MessageFormat.format("Operation {0} Timeout on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(new ServiceUnavailableException(message, exception));
        } catch (PasswordExpiredException e) {
            message = MessageFormat.format("Operation {0} failed with PasswordExpiredException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(new ForbiddenException(message, exception));
        } catch (InvalidPasswordException e) {
            message = MessageFormat.format("Invalid password has been provided to operation {0} for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(ResourceException.getException(UNAUTHORIZED_ERROR_CODE, message, exception));
        } catch (UnknownUidException e) {
            message = MessageFormat.format("Operation {0} could not find resource {1} on system object: {2}",
                    request.getRequestType().toString(), resourceId, resourceContainer);
            handler.handleError(
                    new NotFoundException(message, exception)
                            .setDetail(new JsonValue(new HashMap<String, Object>())));
        } catch (InvalidCredentialException e) {
            message = MessageFormat.format("Invalid credential has been provided to operation {0} for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(ResourceException.getException(UNAUTHORIZED_ERROR_CODE, message, exception));
        } catch (PermissionDeniedException e) {
            message = MessageFormat.format("Permission was denied on {0} operation for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(new ForbiddenException(message, exception));
        } catch (ConnectorSecurityException e) {
            message = MessageFormat.format("Operation {0} failed with ConnectorSecurityException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(new InternalServerErrorException(message, exception));
        } catch (InvalidAttributeValueException e) {
            message = MessageFormat.format("Attribute value conflicts with the attribute''s schema definition on " +
                    "operation {0} for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(new BadRequestException(message, exception));
        } catch (PreconditionFailedException e) {
            message = MessageFormat.format("The resource version for {0} does not match the version provided on " +
                    "operation {1} for system object: {2}",
                    resourceId, request.getRequestType().toString(), resourceContainer);
            handler.handleError(new org.forgerock.json.resource.PreconditionFailedException(message, exception));
        } catch (PreconditionRequiredException e) {
            message = MessageFormat.format("No resource version for resource {0} has been provided on operation {1} for system object: {2}",
                    resourceId , request.getRequestType().toString(), resourceContainer);
            handler.handleError(new org.forgerock.json.resource.PreconditionRequiredException(message, exception));
        } catch (RetryableException e) {
            message = MessageFormat.format("Request temporarily unavailable on operation {0} for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(new ServiceUnavailableException(message, exception));
        } catch (UnsupportedOperationException e) {
            message = MessageFormat.format("Operation {0} is no supported for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(new NotFoundException(message, exception));
        } catch (IllegalArgumentException e) {
            message = MessageFormat.format("Operation {0} failed with IllegalArgumentException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(new InternalServerErrorException(message, e));
        } catch (RemoteWrappedException e) {
            handleRemoteWrappedException(context, request, exception, resourceContainer, resourceId,
                    before, after, handler, connectorExceptionActivityLogger);
        } catch (ConnectorException e) {
            message = MessageFormat.format("Operation {0} failed with ConnectorException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            handler.handleError(new InternalServerErrorException(message, exception));
        } finally {
            // log the ConnectorException
            logger.debug(message, exception);
            try {
                connectorExceptionActivityLogger.log(context, request.getRequestType(), message, resourceId,
                        before, after, Status.FAILURE);
            } catch (ResourceException e) {
                // this means the ActivityLogger couldn't log request; log to error log
                logger.warn("Failed to write activity log", e);
            }
        }
    }

    /**
     * .NET Exceptions that may be wrapped in a RemoteWrappedException
     */
    private enum DotNetExceptionHelper {

        ArgumentException("System.ArgumentException") {
            Exception getMappedException(Exception e) {
                return new IllegalArgumentException(e.getMessage(), e.getCause());
            }
        },
        InvalidOperationException("System.InvalidOperationException") {
            Exception getMappedException(Exception e) {
                return new IllegalStateException(e.getMessage(), e.getCause());
            }
        },
        NullReferenceException("System.NullReferenceException") {
            Exception getMappedException(Exception e) {
                return new NullPointerException(e.getMessage());
            }
        },
        NotSupportedException("System.NotSupportedException") {
            Exception getMappedException(Exception e) {
                return new UnsupportedOperationException(e.getMessage(), e.getCause());
            }
        },
        UnknownDotNetException("") {
            Exception getMappedException(Exception e) {
                return new InternalServerErrorException(e.getMessage(), e.getCause());
            }
        };

        private final String exceptionName;

        private DotNetExceptionHelper(final String exceptionName) {
            this.exceptionName = exceptionName;
        }

        abstract Exception getMappedException(Exception e);

        ConnectorException getConnectorException(Exception e) {
            return new ConnectorException(e.getMessage(), getMappedException(e));
        }

        static DotNetExceptionHelper fromExceptionClass(String name) {
            for (DotNetExceptionHelper helper : values()) {
                if (helper.exceptionName.equals(name)) {
                    return helper;
                }

            }
            return UnknownDotNetException;
        }
    }

    /**
     * Checks the RemoteWrappedException to determine which Exception has been wrapped and handles
     * the appropriate Exception which is wrapped.
     *
     * @param context the ServerContext from the original request
     * @param request the original request
     * @param exception the ConnectorException that was thrown by the facade
     * @param resourceId the resourceId being operated on
     * @param before the object value "before" the request
     * @param after the object value "after" the request
     * @param handler the ResultHandler on which to call handleError
     * @param connectorExceptionActivityLogger the ActivityLogger to use to log the exception
     */
    private void handleRemoteWrappedException(ServerContext context,
                                              Request request,
                                              ConnectorException exception,
                                              String resourceContainer,
                                              String resourceId,
                                              JsonValue before,
                                              JsonValue after,
                                              ResultHandler<?> handler,
                                              ActivityLogger connectorExceptionActivityLogger) {

        RemoteWrappedException remoteWrappedException = (RemoteWrappedException) exception;
        final String message = exception.getMessage();
        final Throwable cause = exception.getCause();

        if (remoteWrappedException.is(AlreadyExistsException.class)) {
            handleConnectorException(context, request, new AlreadyExistsException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConfigurationException.class)) {
            handleConnectorException(context, request, new ConfigurationException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConnectionBrokenException.class)) {
            handleConnectorException(context, request, new ConnectionBrokenException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConnectionFailedException.class)) {
            handleConnectorException(context, request, new ConnectionFailedException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConnectorIOException.class)) {
            handleConnectorException(context, request, new ConnectorIOException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(InvalidAttributeValueException.class)) {
            handleConnectorException(context, request, new InvalidAttributeValueException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(InvalidCredentialException.class)) {
            handleConnectorException(context, request, new InvalidCredentialException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(InvalidPasswordException.class)) {
            handleConnectorException(context, request, new InvalidPasswordException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(OperationTimeoutException.class)) {
            handleConnectorException(context, request, new OperationTimeoutException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(PasswordExpiredException.class)) {
            handleConnectorException(context, request, new PasswordExpiredException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(PermissionDeniedException.class)) {
            handleConnectorException(context, request, new PermissionDeniedException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(PreconditionFailedException.class)) {
            handleConnectorException(context, request, new PreconditionFailedException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(PreconditionRequiredException.class)) {
            handleConnectorException(context, request, new PreconditionRequiredException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(RetryableException.class)) {
            handleConnectorException(context, request, RetryableException.wrap(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(UnknownUidException.class)) {
            handleConnectorException(context, request, new UnknownUidException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConnectorException.class)) {
            handleConnectorException(context, request, new ConnectorException(message, cause),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        } else {
            // handle .NET exceptions
            handleConnectorException(context, request,
                    DotNetExceptionHelper.fromExceptionClass(remoteWrappedException.getExceptionClass())
                            .getConnectorException(remoteWrappedException),
                    resourceContainer, resourceId, before, after, handler, connectorExceptionActivityLogger);
        }
    }

    /**
     * @return the smartevent Name for a given query
     */
    org.forgerock.openidm.smartevent.Name getQueryEventName(String objectClass, QueryRequest request) {
        String prefix = EVENT_PREFIX + getSystemIdentifierName() + "/" + objectClass + "/query/";

        if (request.getQueryId() != null) {
            return org.forgerock.openidm.smartevent.Name.get(prefix + request.getQueryId());
        } else if (request.getQueryExpression() != null) {
            return org.forgerock.openidm.smartevent.Name.get(prefix + "_query_expression");
        } else if (request.getQueryFilter() != null) {
            return org.forgerock.openidm.smartevent.Name.get(prefix + "_queryFilter");
        } else {
            // This should never happen...
            return org.forgerock.openidm.smartevent.Name.get(prefix + "_UNKNOWN");
        }
    }

    private enum ConnectorAction {
        script, test, livesync
    }

    @Override
    public void readInstance(ServerContext context, ReadRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Read operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void actionInstance(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        try {
            switch (request.getActionAsEnum(ConnectorAction.class)) {
                case script:
                    handleScriptAction(request, handler);
                    break;

                case test:
                    handleTestAction(context, request, handler);
                    break;

                case livesync:
                    handleLiveSyncAction(context, request, handler);
                    break;

                default:
                    throw new BadRequestException("Unsupported action: " + request.getAction());
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (ConnectorException e) {
            handleConnectorException(context, request, e, null, request.getResourceName(), null, null, handler, activityLogger);
        } catch (JsonValueException e) {
            handler.handleError(new BadRequestException(e.getMessage(), e));
        } catch (IllegalArgumentException e) { // from request.getActionAsEnum
            handler.handleError(new BadRequestException(e.getMessage(), e));
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    @Override
    public void patchInstance(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update operations are not supported");
        handler.handleError(e);
    }

    private void handleScriptAction(final ActionRequest request, final ResultHandler<JsonValue> handler)
            throws ResourceException {
        final String scriptId = request.getAdditionalParameter(SystemAction.SCRIPT_ID);
        if (StringUtils.isBlank(scriptId)) {
            handler.handleError(new BadRequestException("Missing required parameter: " + SystemAction.SCRIPT_ID));
            return;
        }

        if (!localSystemActionCache.containsKey(scriptId)) {
            handler.handleError(new BadRequestException("Script ID: " + scriptId + " is not defined."));
            return;
        }

        SystemAction action = localSystemActionCache.get(scriptId);

        String systemType = connectorReference.getConnectorKey().getConnectorName();
        List<ScriptContextBuilder> scriptContextBuilderList = action.getScriptContextBuilders(systemType);

        if (scriptContextBuilderList.isEmpty()) {
            handler.handleError(new BadRequestException("Script ID: " + scriptId +
                    " for systemType " + systemType + " is not defined."));
            return;
        }

        JsonValue result = new JsonValue(new HashMap<String, Object>());

        boolean onConnector = !"resource".equalsIgnoreCase(
                request.getAdditionalParameter(SystemAction.SCRIPT_EXECUTE_MODE));

        final ConnectorFacade facade = getConnectorFacade0(handler,
                onConnector ? ScriptOnConnectorApiOp.class : ScriptOnResourceApiOp.class);
        if (null == facade) {
            // getConnectorFacade0 already handles error when returning null
            return;
        }

        String variablePrefix = request.getAdditionalParameter(SystemAction.SCRIPT_VARIABLE_PREFIX);

        List<Map<String, Object>> resultList =
                new ArrayList<Map<String, Object>>(scriptContextBuilderList.size());
        result.put("actions", resultList);

        for (ScriptContextBuilder contextBuilder : scriptContextBuilderList) {
            boolean isShell = contextBuilder.getScriptLanguage().equalsIgnoreCase("Shell");
            for (Map.Entry<String, String> entry : request.getAdditionalParameters().entrySet()) {
                final String key = entry.getKey();
                if (SystemAction.SCRIPT_PARAMS.contains(key)) {
                    continue;
                }
                Object value = entry.getValue();
                Object newValue = value;
                if (isShell) {
                    if ("password".equalsIgnoreCase(key)) {
                        if (value instanceof String) {
                            newValue = new GuardedString(((String) value).toCharArray());
                        } else {
                            throw new BadRequestException("Invalid type for password.");
                        }
                    }
                    if ("username".equalsIgnoreCase(key)) {
                        if (value instanceof String == false) {
                            throw new BadRequestException("Invalid type for username.");
                        }
                    }
                    if ("workingdir".equalsIgnoreCase(key)) {
                        if (value instanceof String == false) {
                            throw new BadRequestException("Invalid type for workingdir.");
                        }
                    }
                    if ("timeout".equalsIgnoreCase(key)) {
                        if (!(value instanceof String) && !(value instanceof Number)) {
                            throw new BadRequestException("Invalid type for timeout.");
                        }
                    }
                    contextBuilder.addScriptArgument(key, newValue);
                    continue;
                }

                if (null != value) {
                    if (value instanceof Collection) {
                        newValue =
                                Array.newInstance(Object.class,
                                        ((Collection) value).size());
                        int i = 0;
                        for (Object v : (Collection) value) {
                            if (null == v || FrameworkUtil.isSupportedAttributeType(v.getClass())) {
                                Array.set(newValue, i, v);
                            } else {
                                // Serializable may not be
                                // acceptable
                                Array.set(newValue, i, v instanceof Serializable ? v : v.toString());
                            }
                            i++;
                        }

                    } else if (value.getClass().isArray()) {
                        // TODO implement the array support later
                    } else if (!FrameworkUtil.isSupportedAttributeType(value.getClass())) {
                        // Serializable may not be acceptable
                        newValue = value instanceof Serializable ? value : value.toString();
                    }
                }
                contextBuilder.addScriptArgument(key, newValue);
            }

            JsonValue content = request.getContent();
            // if there is no content(content.isNull()), skip adding content to script arguments
            if (content.isMap()) {
                for (Map.Entry<String, Object> entry : content.asMap().entrySet()) {
                    contextBuilder.addScriptArgument(entry.getKey(), entry.getValue());
                }
            } else if (!content.isNull()) {
                handler.handleError(new BadRequestException("Content is not of type Map"));
                return;
            }

            // ScriptContext scriptContext = script.getScriptContextBuilder().build();
            OperationOptionsBuilder operationOptionsBuilder = new OperationOptionsBuilder();

            // It's necessary to keep the backward compatibility with Waveset IDM
            if (null != variablePrefix && isShell) {
                operationOptionsBuilder.setOption("variablePrefix", variablePrefix);
            }

            Map<String, Object> actionResult = new HashMap<String, Object>(2);
            try {
                Object scriptResult = null;
                if (onConnector) {
                    scriptResult = facade.runScriptOnConnector(
                            contextBuilder.build(), operationOptionsBuilder.build());
                } else {
                    scriptResult = facade.runScriptOnResource(
                            contextBuilder.build(), operationOptionsBuilder.build());
                }
                actionResult.put("result", ConnectorUtil.coercedTypeCasting(scriptResult, Object.class));
            } catch (Throwable t) {
                logger.error("Script execution error.", t);
                actionResult.put("error", t.getMessage());
            }
            resultList.add(actionResult);
        }
        handler.handleResult(result);
    }

    private void handleTestAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleResult(new JsonValue(getStatus(context)));
    }

    private void handleLiveSyncAction(final ServerContext context, final ActionRequest request, final ResultHandler<JsonValue> handler)
            throws ResourceException {

        final String objectTypeName = getObjectTypeName(ObjectClass.ALL);

        if (objectTypeName == null) {
            throw new BadRequestException("__ALL__ object class is not configured");
        }

        final ActionRequest forwardRequest = Requests.newActionRequest(getSource(objectTypeName), request.getAction());

        // forward request to be handled in ObjectClassResourceProvider#actionCollection
        handler.handleResult(connectionFactory.getConnection().action(context, forwardRequest));
    }

    /**
     * Checks the {@code operation} permission before execution.
     *
     * @param operation
     * @return if {@code denied} is true and the {@code onDeny} equals
     *         {@link org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper.OnActionPolicy#ALLOW}
     *         returns false else true
     * @throws ResourceException
     *             if {@code denied} is true and the {@code onDeny} equals
     *             {@link org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper.OnActionPolicy#THROW_EXCEPTION}
     */
    private <V> ConnectorFacade getConnectorFacade0(final ResultHandler<V> handler,
            Class<? extends APIOperation> operation) throws ResourceException {
        final ConnectorFacade facade = getConnectorFacade();
        if (null == facade) {
            handler.handleError(new ServiceUnavailableException());
            return null;
        }
        OperationOptionInfoHelper operationOptionInfoHelper = null;
        ResourceException e = null;

        if (null == facade.getOperation(operation)) {
            e =
                    new NotSupportedException("Operation " + operation.getCanonicalName()
                            + " is not supported by the Connector");
        } else if (null != operationOptionInfoHelper
                && OperationOptionInfoHelper.OnActionPolicy.THROW_EXCEPTION
                        .equals(operationOptionInfoHelper.getOnActionPolicy())) {
            e =
                    new ForbiddenException("Operation " + operation.getCanonicalName()
                            + " is configured to be denied");

        }
        if (null != e) {
            handler.handleError(e);
            return null;
        }
        return facade;
    }

    private class ObjectClassRequestHandler implements RequestHandler {

        public static final String OBJECTCLASS = "objectclass";
        public static final String OBJECTCLASS_TEMPLATE = "/{objectclass}";

        protected String getObjectClass(ServerContext context) throws ResourceException {
            Map<String, String> variables = ResourceUtil.getUriTemplateVariables(context);
            if (null != variables && variables.containsKey(OBJECTCLASS)) {
                return variables.get(OBJECTCLASS);
            }
            throw new ForbiddenException(
                    "Direct access without Router to this service is forbidden.");
        }

        public void handleAction(ServerContext context, ActionRequest request,
                ResultHandler<JsonValue> handler) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    delegate.handleAction(context, request, handler);
                } else {
                    handler.handleError(new NotFoundException("Not found: " + objectClass));
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }

        public void handleCreate(ServerContext context, CreateRequest request,
                ResultHandler<Resource> handler) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    delegate.handleCreate(context, request, handler);
                } else {
                    handler.handleError(new NotFoundException("Not found: " + objectClass));
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }

        public void handleDelete(ServerContext context, DeleteRequest request,
                ResultHandler<Resource> handler) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    delegate.handleDelete(context, request, handler);
                } else {
                    handler.handleError(new NotFoundException("Not found: " + objectClass));
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }

        public void handlePatch(ServerContext context, PatchRequest request,
                ResultHandler<Resource> handler) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    delegate.handlePatch(context, request, handler);
                } else {
                    handler.handleError(new NotFoundException("Not found: " + objectClass));
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }

        public void handleQuery(ServerContext context, QueryRequest request,
                QueryResultHandler handler) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    delegate.handleQuery(context, request, handler);
                } else {
                    handler.handleError(new NotFoundException("Not found: " + objectClass));
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }

        public void handleRead(ServerContext context, ReadRequest request,
                ResultHandler<Resource> handler) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    delegate.handleRead(context, request, handler);
                } else {
                    handler.handleError(new NotFoundException("Not found: " + objectClass));
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }

        public void handleUpdate(ServerContext context, UpdateRequest request,
                ResultHandler<Resource> handler) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    delegate.handleUpdate(context, request, handler);
                } else {
                    handler.handleError(new NotFoundException("Not found: " + objectClass));
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }
    }

    /**
     * ActionRequest actions we support on /system/[systemName]/[objectClass/{id}
     */
    private enum ObjectClassAction {
        authenticate, resolveUsername, liveSync
    }

    /**
     * Handle request on /system/[systemName]/[objectClass]/{id}
     *
     * @ThreadSafe
     */
    private class ObjectClassResourceProvider implements CollectionResourceProvider {

        private final ObjectClassInfoHelper objectClassInfoHelper;
        private final Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations;
        private final boolean allowModification;
        private final String objectClass;

        private ObjectClassResourceProvider(String objectClass, ObjectClassInfoHelper objectClassInfoHelper,
                Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations,
                boolean allowModification) {
            this.objectClassInfoHelper = objectClassInfoHelper;
            this.operations = operations;
            this.allowModification = allowModification;
            this.objectClass = objectClass;
        }

        /**
         * Checks the {@code operation} permission before execution.
         *
         * @param operation
         * @return if {@code denied} is true and the {@code onDeny} equals
         *         {@link org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper.OnActionPolicy#ALLOW}
         *         returns false else true
         * @throws ResourceException
         *             if {@code denied} is true and the {@code onDeny} equals
         *             {@link org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper.OnActionPolicy#THROW_EXCEPTION}
         */
        private <V> ConnectorFacade getConnectorFacade0(final ResultHandler<V> handler,
                Class<? extends APIOperation> operation) throws ResourceException {
            final ConnectorFacade facade = getConnectorFacade();
            if (null == facade) {
                handler.handleError(new ServiceUnavailableException());
                return null;
            }
            OperationOptionInfoHelper operationOptionInfoHelper = operations.get(operation);
            ResourceException e = null;

            if (null == facade.getOperation(operation)) {
                e = new NotSupportedException(
                        "Operation " + operation.getCanonicalName() + " is not supported by the Connector");
            } else if (null != operationOptionInfoHelper
                    && (null != operationOptionInfoHelper.getSupportedObjectTypes())) {
                if (!operationOptionInfoHelper.getSupportedObjectTypes().contains(
                        objectClassInfoHelper.getObjectClass().getObjectClassValue())) {
                    e = new NotSupportedException(
                            "Actions are not supported for resource instances");
                } else if (OperationOptionInfoHelper.OnActionPolicy.THROW_EXCEPTION.equals(
                        operationOptionInfoHelper.getOnActionPolicy())) {
                    e = new ForbiddenException(
                            "Operation " + operation.getCanonicalName() + " is configured to be denied");
                }
            }
            if (null != e) {
                handler.handleError(e);
                return null;
            }
            return facade;
        }

        @Override
        public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
            try {
                switch (request.getActionAsEnum(ObjectClassAction.class)) {
                    case authenticate:
                        handleAuthenticate(context, request, handler);
                        break;
                    case liveSync:
                        handleLiveSync(context, request, handler);
                        break;
                    default:
                        throw new BadRequestException("Unsupported action: " + request.getAction());
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (JsonValueException e) {
                handler.handleError(new BadRequestException(e.getMessage(), e));
            } catch (IllegalArgumentException e) { // from request.getActionAsEnum
                handler.handleError(new BadRequestException(e.getMessage(), e));
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }

        private void handleAuthenticate(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler)
                throws ResourceException, IOException {
            try {
                final ConnectorFacade facade = getConnectorFacade0(handler, AuthenticationApiOp.class);
                if (null == facade) {
                    // getConnectorFacade0 already handles error when returning null
                    return;
                }

                final JsonValue params = new JsonValue(request.getAdditionalParameters());
                final String username = params.get("username").required().asString();
                final String password = params.get("password").required().asString();

                OperationOptions operationOptions = operations.get(AuthenticationApiOp.class)
                        .build(jsonConfiguration, objectClassInfoHelper)
                        .build();

                // Throw ConnectorException
                Uid uid = facade.authenticate(objectClassInfoHelper.getObjectClass(), username,
                        new GuardedString(password.toCharArray()), operationOptions);

                JsonValue result = new JsonValue(new HashMap<String, Object>());
                result.put(Resource.FIELD_CONTENT_ID, uid.getUidValue());
                if (null != uid.getRevision()) {
                    result.put(Resource.FIELD_CONTENT_REVISION, uid.getRevision());
                }
                handler.handleResult(result);
            } catch (ConnectorException e) {
                // handle ConnectorException from facade.authenticate:
                // log to activity log only if this is an external request
                // (let internal requests do their own logging upon the handleError...)
                handleConnectorException(context, request, e, null, null, null, null, handler,
                        ContextUtil.isExternal(context) ? activityLogger : NullActivityLogger.INSTANCE);
            }
        }

        private void handleLiveSync(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler)
                throws ResourceException {
            final ActionRequest forwardRequest =
                    Requests.newActionRequest(SystemObjectSetService.ROUTER_PREFIX, request.getAction())
                            .setAdditionalParameter("source", getSource(objectClass));

            // forward request to be handled in SystemObjectSetService#actionInstance
            handler.handleResult(connectionFactory
                    .getConnection()
                    .action(context, forwardRequest));
        }

        @Override
        public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                ResultHandler<JsonValue> handler) {
            handler.handleError(new NotSupportedException("Actions are not supported for resource instances"));
        }

        @Override
        public void createInstance(ServerContext context, CreateRequest request,
                ResultHandler<Resource> handler) {
            try {
                final ConnectorFacade facade = getConnectorFacade0(handler, CreateApiOp.class);
                if (null == facade) {
                    // getConnectorFacade0 already handles error when returning null
                    return;
                }
                final Set<Attribute> createAttributes =
                        objectClassInfoHelper.getCreateAttributes(request, cryptoService);

                OperationOptions operationOptions = operations.get(CreateApiOp.class)
                        .build(jsonConfiguration, objectClassInfoHelper)
                        .build();

                Uid uid = facade.create(objectClassInfoHelper.getObjectClass(),
                        AttributeUtil.filterUid(createAttributes), operationOptions);

                Resource resource = getCurrentResource(facade, uid, null);
                activityLogger.log(context, RequestType.CREATE, "message", getSource(objectClass, uid.getUidValue()), null, resource.getContent(), Status.SUCCESS);
                handler.handleResult(resource);
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (ConnectorException e) {
                handleConnectorException(context, request, e, getSource(objectClass), objectClassInfoHelper.getCreateResourceId(request), request.getContent(), null, handler, activityLogger);
            } catch (JsonValueException e) {
                handler.handleError(new BadRequestException(e.getMessage(), e));
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }

        @Override
        public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                ResultHandler<Resource> handler) {
            try {
                final ConnectorFacade facade = getConnectorFacade0(handler, DeleteApiOp.class);
                if (null == facade) {
                    // getConnectorFacade0 already handles error when returning null
                    return;
                }

                final Uid uid = request.getRevision() != null
                        ? new Uid(resourceId, request.getRevision())
                        : new Uid(resourceId);

                // do a read first (largely for logging)
                Resource before = getCurrentResource(facade, uid, null);

                OperationOptions operationOptions = operations.get(DeleteApiOp.class)
                        .build(jsonConfiguration, objectClassInfoHelper)
                        .build();

                facade.delete(objectClassInfoHelper.getObjectClass(), uid, operationOptions);

                JsonValue result = before.getContent().copy();
                result.put(Resource.FIELD_CONTENT_ID, uid.getUidValue());
                if (null != uid.getRevision()) {
                    result.put(Resource.FIELD_CONTENT_REVISION, uid.getRevision());
                }
                activityLogger.log(context, RequestType.DELETE, "message", getSource(objectClass, uid.getUidValue()), before.getContent(), null, Status.SUCCESS);
                handler.handleResult(new Resource(uid.getUidValue(), uid.getRevision(), result));
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (ConnectorException e) {
                handleConnectorException(context, request, e, getSource(objectClass), resourceId, null, null, handler, activityLogger);
            } catch (JsonValueException e) {
                handler.handleError(new BadRequestException(e.getMessage(), e));
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }

        @Override
        public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                ResultHandler<Resource> handler) {
            handler.handleError(new NotSupportedException("Patch operations are not supported"));
        }

        @Override
        public void queryCollection(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler) {
            EventEntry measure = Publisher.start(getQueryEventName( objectClass, request), request, null);
            try {
                final ConnectorFacade facade = getConnectorFacade0(handler, SearchApiOp.class);
                if (null == facade) {
                    // getConnectorFacade0 already handles error when returning null
                    return;
                }

                OperationOptionsBuilder operationOptionsBuilder = operations.get(SearchApiOp.class)
                            .build(jsonConfiguration, objectClassInfoHelper);

                Filter filter = null;

                if (request.getQueryId() != null) {
                    if (ServerConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                        operationOptionsBuilder.setAttributesToGet(Uid.NAME);
                    } else {
                        throw new BadRequestException("Unsupported _queryId: " + request.getQueryId());
                    }
                } else if (request.getQueryExpression() != null) {
                    filter = QueryFilter.valueOf(request.getQueryExpression()).accept(
                            RESOURCE_FILTER, objectClassInfoHelper);

                } else if (request.getQueryFilter() != null) {
                    // No filtering or query by filter.
                    filter = request.getQueryFilter().accept(RESOURCE_FILTER, objectClassInfoHelper);
                } else {
                    throw new BadRequestException("One of _queryId, _queryExpression, or _queryFilter is required.");
                }

                // If paged results are requested then decode the cookie in
                // order to determine
                // the index of the first result to be returned.
                final int pageSize = request.getPageSize();
                final String pagedResultsCookie = request.getPagedResultsCookie();
                final boolean pagedResultsRequested = request.getPageSize() > 0;
                if (pageSize > 0) {
                    operationOptionsBuilder.setPageSize(pageSize);
                }
                if (null != pagedResultsCookie) {
                    operationOptionsBuilder.setPagedResultsCookie(pagedResultsCookie);
                }
                operationOptionsBuilder.setPagedResultsOffset(request.getPagedResultsOffset());
                if (null != request.getSortKeys()) {
                    List<SortKey> sortKeys = new ArrayList<SortKey>(request.getSortKeys().size());
                    for (org.forgerock.json.resource.SortKey s: request.getSortKeys()){
                        sortKeys.add(new SortKey(s.getField().leaf(), s.isAscendingOrder()));
                    }
                    operationOptionsBuilder.setSortKeys(sortKeys);
                }

                // Override ATTRS_TO_GET if fields are specified within the Request
                if (!request.getFields().isEmpty()) {
                    objectClassInfoHelper.setAttributesToGet(operationOptionsBuilder, request.getFields());
                }

                final JsonValue logValue = json(array());
                SearchResult searchResult = facade.search(objectClassInfoHelper.getObjectClass(), filter,
                        new ResultsHandler() {
                            @Override
                            public boolean handle(ConnectorObject obj) {
                                try {
                                    Resource resource = objectClassInfoHelper.build(obj, cryptoService);
                                    logValue.add(resource.getContent().getObject());
                                    return handler.handleResource(resource);
                                } catch (Exception e) {
                                    handler.handleError(new InternalServerErrorException(e.getMessage(), e));
                                    return false;
                                }
                            }
                        }, operationOptionsBuilder.build());
                activityLogger.log(context, request.getRequestType(),
                        "query: " + request.getQueryId()
                        + ", queryExpression: " + request.getQueryExpression()
                        + ", queryFilter: " + (request.getQueryFilter() != null ? request.getQueryFilter().toString() : null)
                        + ", parameters: " + request.getAdditionalParameters(),
                        request.getQueryId(), null, logValue, Status.SUCCESS);
                handler.handleResult(
                        new QueryResult(
                                searchResult != null ? searchResult.getPagedResultsCookie() : null,
                                searchResult != null ? searchResult.getRemainingPagedResults() : -1));
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (ConnectorException e) {
                handleConnectorException(context, request, e, null, null, null, null, handler, activityLogger);
            } catch (JsonValueException e) {
                handler.handleError(new BadRequestException(e.getMessage(), e));
            } catch (AttributeMissingException e) {
                handler.handleError(new BadRequestException(e.getMessage(), e));
            } catch (IllegalArgumentException e) {
                handler.handleError(new BadRequestException(e.getMessage(), e));
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            } finally {
                measure.end();
            }
        }

        @Override
        public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                ResultHandler<Resource> handler) {
            try {
                final ConnectorFacade facade = getConnectorFacade0(handler, GetApiOp.class);
                if (null == facade) {
                    // getConnectorFacade0 already handles error when returning null
                    return;
                }

                Uid uid = new Uid(resourceId);
                ConnectorObject connectorObject = getConnectorObject(facade, uid, request.getFields());

                if (null != connectorObject) {
                    Resource resource = objectClassInfoHelper.build(connectorObject, cryptoService);
                    activityLogger.log(context, RequestType.READ, "message", getSource(objectClass, uid.getUidValue()), resource.getContent(), resource.getContent(), Status.SUCCESS);
                    handler.handleResult(resource);
                } else {
                    final String matchedUri = context.containsContext(RouterContext.class)
                            ? context.asContext(RouterContext.class).getMatchedUri()
                            : "unknown path";
                    handler.handleError(new NotFoundException("Object " + resourceId + " not found on " + matchedUri));
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (ConnectorException e) {
                handleConnectorException(context, request, e, getSource(objectClass), resourceId, null, null, handler, activityLogger);
            } catch (JsonValueException e) {
                handler.handleError(new BadRequestException(e.getMessage(), e));
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }

        @Override
        public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                ResultHandler<Resource> handler) {
            try {
                final ConnectorFacade facade = getConnectorFacade0(handler, UpdateApiOp.class);
                if (null == facade) {
                    // getConnectorFacade0 already handles error when returning null
                    return;
                }

                final Uid _uid = request.getRevision() != null
                        ? new Uid(resourceId, request.getRevision())
                        : new Uid(resourceId);

                // read resource before update for logging
                Resource before = getCurrentResource(facade, _uid, null);

                // TODO Fix for http://bugster.forgerock.org/jira/browse/CREST-29
                final Name newName = null;
                final Set<Attribute> replaceAttributes =
                        objectClassInfoHelper.getUpdateAttributes(request, newName,
                                cryptoService);

                OperationOptions operationOptions;
                OperationOptionsBuilder operationOptionsBuilder = operations.get(UpdateApiOp.class)
                        .build(jsonConfiguration, objectClassInfoHelper);

                final String reauthPassword = getReauthPassword(context);

                // if reauth and updating attribute requiring user credentials
                if (runAsUser(request, reauthPassword)) {
                    // get username attribute
                    final List<String> usernameAttrs =
                            jsonConfiguration.get(ConnectorUtil.OPENICF_CONFIGURATION_PROPERTIES)
                                    .get(ACCOUNT_USERNAME_ATTRIBUTES)
                                    .asList(String.class);
                    final String username = request.getContent().get(usernameAttrs.get(0)).asString();

                    if (StringUtils.isNotBlank(username)) {
                        operationOptionsBuilder.setRunAsUser(username)
                                .setRunWithPassword(new GuardedString(reauthPassword.toCharArray()));
                    }
                }

                operationOptions = operationOptionsBuilder.build();

                Uid uid = facade.update(objectClassInfoHelper.getObjectClass(), _uid,
                        AttributeUtil.filterUid(replaceAttributes), operationOptions);

                Resource resource = getCurrentResource(facade, uid, null);
                activityLogger.log(context, RequestType.UPDATE, "message", getSource(objectClass, uid.getUidValue()), before.getContent(), resource.getContent(), Status.SUCCESS);
                handler.handleResult(resource);
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (ConnectorException e) {
                handleConnectorException(context, request, e, getSource(objectClass), resourceId, request.getContent(), null, handler, activityLogger);
            } catch (JsonValueException e) {
                handler.handleError(new BadRequestException(e.getMessage(), e));
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }

        // see if there is a reauth password provided
        private String getReauthPassword(ServerContext context) {
            try {
                // get reauth password
                return  context.asContext(HttpContext.class).getHeaderAsString(REAUTH_HEADER);
            } catch (Exception e) {
                // there will not always be a HttpContext and this is acceptable so catch exception to
                // prevent the exception from  stopping the remaining update
                return null;
            }
        }

        // check if updating an attribute that requires user credentials
        private boolean runAsUser(UpdateRequest request, String reauthPassword) {
            final JsonValue properties = objectClassInfoHelper.getProperties();
            final Predicate<String> attributesToRunAsUser = new Predicate<String>() {
                @Override
                public boolean apply(String attribute) {
                    return !properties.get(attribute).isNull()
                            && properties.get(attribute).get(RUN_AS_USER).defaultTo(false).asBoolean();
                }
            };

            return StringUtils.isNotEmpty(reauthPassword)
                    && FluentIterable.from(request.getContent().asMap().keySet()).filter(attributesToRunAsUser).iterator().hasNext();
        }

        private Resource getCurrentResource(final ConnectorFacade facade, final Uid uid, final List<JsonPointer> fields)
            throws IOException, JsonCryptoException, ResourceException {

            final ConnectorObject co = getConnectorObject(facade, uid, fields);
            if (null != co) {
                return objectClassInfoHelper.build(co, cryptoService);
            } else {
                JsonValue result = new JsonValue(new HashMap<String, Object>());
                result.put(Resource.FIELD_CONTENT_ID, uid.getUidValue());
                if (null != uid.getRevision()) {
                    result.put(Resource.FIELD_CONTENT_REVISION, uid.getRevision());
                }
                return new Resource(uid.getUidValue(), uid.getRevision(), result);
            }
        }

        private ConnectorObject getConnectorObject(final ConnectorFacade facade, final Uid uid, final List<JsonPointer> fields)
            throws IOException, JsonCryptoException, ResourceException {

            final OperationOptions operationOptions;
            if (fields == null || fields.isEmpty()) {
                operationOptions = operations.get(GetApiOp.class)
                        .build(jsonConfiguration, objectClassInfoHelper)
                        .build();
            } else {
                OperationOptionsBuilder operationOptionsBuilder = new OperationOptionsBuilder();
                objectClassInfoHelper.setAttributesToGet(operationOptionsBuilder, fields);
                operationOptions = operationOptionsBuilder.build();
            }

            return facade.getObject(objectClassInfoHelper.getObjectClass(), uid, operationOptions);
        }

        /*
         * public JsonValue update(Id id, String rev, JsonValue object,
         * JsonValue params) throws Exception { OperationHelper helper =
         * operationHelperBuilder.build(id.getObjectType(), params,
         * cryptoService); if (allowModification &&
         * helper.isOperationPermitted(UpdateApiOp.class)) { JsonValue newName =
         * object.get(Resource.FIELD_CONTENT_ID); ConnectorObject
         * connectorObject = null; Set<Attribute> attributeSet = null;
         *
         * //TODO support case sensitive and insensitive rename detection! if
         * (newName.isString() &&
         * !id.getLocalId().equals(Id.unescapeUid(newName.asString()))) { //This
         * is a rename connectorObject = helper.build(UpdateApiOp.class,
         * newName.asString(), object); attributeSet =
         * AttributeUtil.filterUid(connectorObject.getAttributes()); } else {
         * connectorObject = helper.build(UpdateApiOp.class, id.getLocalId(),
         * object); attributeSet = new HashSet<Attribute>(); for (Attribute
         * attribute : connectorObject.getAttributes()) { if
         * (attribute.is(Name.NAME) || attribute.is(Uid.NAME)) { continue; }
         * attributeSet.add(attribute); } } OperationOptionsBuilder
         * operationOptionsBuilder =
         * helper.getOperationOptionsBuilder(UpdateApiOp.class, connectorObject,
         * params); Uid uid =
         * getConnectorFacade().update(connectorObject.getObjectClass(),
         * connectorObject.getUid(), attributeSet,
         * operationOptionsBuilder.build()); helper.resetUid(uid, object);
         * return object; } else {
         * logger.debug("Operation update of {} is not permitted", id); } return
         * null; }
         */
    }

    /**
     * Handle request on /system/[systemName]/schema
     *
     * @ThreadSafe
     */
    private static class SchemaResourceProvider implements SingletonResourceProvider {

        private final Resource schema;

        private SchemaResourceProvider(Resource schema) {
            this.schema = schema;
        }

        @Override
        public void readInstance(ServerContext context, ReadRequest request,
                ResultHandler<Resource> handler) {
            handler.handleResult(schema);
        }

        @Override
        public void actionInstance(ServerContext context, ActionRequest request,
                ResultHandler<JsonValue> handler) {
            final ResourceException e =
                    new NotSupportedException("Actions are not supported for resource instances");
            handler.handleError(e);
        }

        @Override
        public void patchInstance(ServerContext context, PatchRequest request,
                ResultHandler<Resource> handler) {
            final ResourceException e =
                    new NotSupportedException("Patch operations are not supported");
            handler.handleError(e);
        }

        @Override
        public void updateInstance(ServerContext context, UpdateRequest request,
                ResultHandler<Resource> handler) {
            final ResourceException e =
                    new NotSupportedException("Update operations are not supported");
            handler.handleError(e);
        }
    }
    
    /**
     * A container for information about a sync retry after a failure
     */
    private class SyncRetry {
        
        /**
         * The retry value, true if the sync should be retried, false otherwise.
         */
        boolean value;
        
        /**
         * The {@link Throwable} associated with the failure
         */
        Throwable throwable;
        
        public SyncRetry() {
            value = false;
            throwable = null;
        }

        /**
         * Returns the retry value.
         * 
         * @return true if the sync should be retried, false otherwise.
         */
        public boolean getValue() {
            return value;
        }

        /**
         * Sets the retry value.
         * 
         * @param value true if the sync should be retried, false otherwise.
         */
        public void setValue(boolean value) {
            this.value = value;
        }

        /**
         * Returns the {@link Throwable} associated with the failure
         * 
         * @return the {@link Throwable} associated with the failure
         */
        public Throwable getThrowable() {
            return throwable;
        }

        /**
         * Sets the {@link Throwable} associated with the failure.
         * 
         * @param throwable the {@link Throwable} associated with the failure.
         */
        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
        }
    }

    private static final QueryFilterVisitor<Filter, ObjectClassInfoHelper> RESOURCE_FILTER =
            new QueryFilterVisitor<Filter, ObjectClassInfoHelper>() {

                @Override
                public Filter visitAndFilter(final ObjectClassInfoHelper helper,
                        List<QueryFilter> subFilters) {
                    final Iterator<QueryFilter> iterator = subFilters.iterator();
                    if (iterator.hasNext()) {
                        return buildAnd(helper, iterator.next(), iterator);
                    } else {
                        throw new IllegalArgumentException("cannot parse 'and' QueryFilter with zero operands");
                    }
                }

                private Filter buildAnd(final ObjectClassInfoHelper helper, final QueryFilter left,
                        final Iterator<QueryFilter> iterator) {
                    if (iterator.hasNext()) {
                        final QueryFilter right = iterator.next();
                        return and(left.accept(this, helper), buildAnd(helper, right, iterator));
                    } else {
                        return left.accept(this, helper);
                    }
                }

                @Override
                public Filter visitOrFilter(ObjectClassInfoHelper helper,
                        List<QueryFilter> subFilters) {
                    final Iterator<QueryFilter> iterator = subFilters.iterator();
                    if (iterator.hasNext()) {
                        return buildOr(helper, iterator.next(), iterator);
                    } else {
                        throw new IllegalArgumentException("cannot parse 'or' QueryFilter with zero operands");
                    }
                }

                private Filter buildOr(final ObjectClassInfoHelper helper, final QueryFilter left,
                        final Iterator<QueryFilter> iterator) {
                    if (iterator.hasNext()) {
                        final QueryFilter right = iterator.next();
                        return or(left.accept(this, helper), buildAnd(helper, right, iterator));
                    } else {
                        return left.accept(this, helper);
                    }
                }

                @Override
                public Filter visitBooleanLiteralFilter(final ObjectClassInfoHelper helper,
                        final boolean value) {
                    return new Filter() {
                        public boolean accept(ConnectorObject obj) {
                            return value;
                        }
                        public <R extends Object, P extends Object> R accept(org.identityconnectors.framework.common.objects.filter.FilterVisitor<R,P> v, P p) {
                            // OpenICF team explained that
                            // return v.visitExtendedFilter(p, this);
                            // would not yet (1.4) work with all connectors and/or remotely.
                            // Instead the return null evaluates to always true.       
                            if (value) {
                                return null; // OpenICF contract evaluates null return to always true
                            } else {
                                throw new UnsupportedOperationException("visitBooleanLiteralFilter only supported for literal true, not false");
                            }
                        }
                    };
                }
                
                @Override
                public Filter visitContainsFilter(ObjectClassInfoHelper helper, JsonPointer field,
                        Object valueAssertion) {
                    return contains(helper.filterAttribute(field, valueAssertion));
                }

                @Override
                public Filter visitEqualsFilter(ObjectClassInfoHelper helper, JsonPointer field,
                        Object valueAssertion) {
                    return equalTo(helper.filterAttribute(field, valueAssertion));
                }

                /**
                 * EndsWith filter
                 */
                private static final String EW = "ew";
                /**
                 * ContainsAll filter
                 */
                private static final String CA = "ca";

                @Override
                public Filter visitExtendedMatchFilter(ObjectClassInfoHelper helper,
                        JsonPointer field, String matchingRuleId, Object valueAssertion) {
                    if (EW.equals(matchingRuleId)) {
                        return endsWith(helper.filterAttribute(field, valueAssertion));
                    } else if (CA.equals(matchingRuleId)) {
                        return containsAllValues(helper.filterAttribute(field, valueAssertion));
                    }
                    throw new IllegalArgumentException("ExtendedMatchFilter is not supported");
                }

                @Override
                public Filter visitGreaterThanFilter(ObjectClassInfoHelper helper,
                        JsonPointer field, Object valueAssertion) {
                    return greaterThan(helper.filterAttribute(field, valueAssertion));
                }

                @Override
                public Filter visitGreaterThanOrEqualToFilter(ObjectClassInfoHelper helper,
                        JsonPointer field, Object valueAssertion) {
                    return greaterThanOrEqualTo(helper.filterAttribute(field, valueAssertion));
                }

                @Override
                public Filter visitLessThanFilter(ObjectClassInfoHelper helper, JsonPointer field,
                        Object valueAssertion) {
                    return lessThan(helper.filterAttribute(field, valueAssertion));
                }

                @Override
                public Filter visitLessThanOrEqualToFilter(ObjectClassInfoHelper helper,
                        JsonPointer field, Object valueAssertion) {
                    return lessThanOrEqualTo(helper.filterAttribute(field, valueAssertion));
                }

                @Override
                public Filter visitNotFilter(ObjectClassInfoHelper helper, QueryFilter subFilter) {
                    return not(subFilter.accept(this, helper));
                }

                @Override
                public Filter visitPresentFilter(ObjectClassInfoHelper helper, JsonPointer field) {
                    throw new IllegalArgumentException("PresentFilter is not supported");
                }

                @Override
                public Filter visitStartsWithFilter(ObjectClassInfoHelper helper,
                        JsonPointer field, Object valueAssertion) {
                    return startsWith(helper.filterAttribute(field, valueAssertion));
                }
            };


    /**
     * Gets the unique {@link org.forgerock.openidm.provisioner.SystemIdentifier} of this instance.
     * <p/>
     * The service which refers to this service instance can distinguish between multiple instances by this value.
     *
     * @return
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
     * @return
     */
    private String getSource(final String objectClass, final String... optionalId) {
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
     * @param context the ServerContext of the request requesting the status
     * @return a Map of the current status of a connector
     */
    public Map<String, Object> getStatus(ServerContext context) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        JsonValue jv = new JsonValue(result);
        boolean ok = false;

        jv.put("name", systemIdentifier.getName());
        jv.put("enabled", jsonConfiguration.get("enabled").defaultTo(Boolean.TRUE).asBoolean());
        jv.put("config", "config/provisioner.openicf/" + factoryPid);
        jv.put("objectTypes", ConnectorUtil.getObjectTypes(jsonConfiguration).keySet());
        ConnectorReference connectorReference = ConnectorUtil.getConnectorReference(jsonConfiguration);
        if (connectorReference != null) {
            jv.put(ConnectorUtil.OPENICF_CONNECTOR_REF, ConnectorUtil.getConnectorKey(connectorReference.getConnectorKey()));
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
        SimpleSystemIdentifier testIdentifier = null;
        ConnectorReference connectorReference = null;
        try {
            testIdentifier = new SimpleSystemIdentifier(config);
            connectorReference = ConnectorUtil.getConnectorReference(jsonConfiguration);
        } catch (JsonValueException e) {
            jv.put("error", "OpenICF Provisioner Service jsonConfiguration has errors: " + e.getMessage());
            return jv.asMap();
        }

        ConnectorInfo connectorInfo = connectorInfoProvider.findConnectorInfo(connectorReference);
        if (null != connectorInfo) {
            ConnectorFacade facade = null;
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
                } catch (Throwable e) {
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

        final Iterable<Entry<String, ObjectClassInfoHelper>> objectClasses = FluentIterable.from(objectTypes.entrySet()).filter(objectClassFilter);

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
     * @throws org.forgerock.json.fluent.JsonValueException
     *             if the {@code previousStage} is not Map.
     * @see {@link ConnectorUtil#convertToSyncToken(org.forgerock.json.fluent.JsonValue)}
     *      or any exception happed inside the connector.
     */
    public JsonValue liveSynchronize(final String objectType, final JsonValue previousStage) throws ResourceException {

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
                                                    connectionFactory.getConnection().action(routerContext, onCreateRequest);

                                                    activityLogger.log(routerContext, RequestType.ACTION,
                                                                    "sync-create", onCreateRequest.getResourceName(),
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
                                                    connectionFactory.getConnection().action(routerContext, onUpdateRequest);

                                                    activityLogger.log(routerContext, RequestType.ACTION,
                                                            "sync-update", onUpdateRequest.getResourceName(),
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
                                                    connectionFactory.getConnection().action(routerContext, onDeleteRequest);

                                                    activityLogger.log(routerContext, RequestType.ACTION,
                                                            "sync-delete", onDeleteRequest.getResourceName(),
                                                            null, null, Status.SUCCESS);
                                                    break;
                                            }
                                        } catch (Exception e) {
                                            failedRecord[0] = SerializerUtil.serializeXmlObject(syncDelta, true);
                                            logger.debug("Failed to synchronize {} object, handle failure using {}",
                                                    syncDelta.getUid(), syncFailureHandler, e);
                                            Map<String, Object> syncFailureMap = new HashMap<String, Object>(6);
                                            syncFailureMap.put("token", syncDelta.getToken().getValue());
                                            syncFailureMap.put("systemIdentifier", systemIdentifier.getName());
                                            syncFailureMap.put("objectType", objectType);
                                            syncFailureMap.put("uid", syncDelta.getUid().getUidValue());
                                            syncFailureMap.put("failedRecord", failedRecord[0]);
                                            try {
                                                syncFailureHandler.invoke(syncFailureMap, e);
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
                            Map<String, Object> lastException = new LinkedHashMap<String, Object>(2);
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
}
