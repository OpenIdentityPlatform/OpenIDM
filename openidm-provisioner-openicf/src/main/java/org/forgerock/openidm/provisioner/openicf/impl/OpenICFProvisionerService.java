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

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.forgerock.services.context.Context;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
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
import org.forgerock.openidm.provisioner.openicf.commons.AttributeMissingException;
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
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilterVisitor;
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
import org.identityconnectors.framework.impl.api.local.LocalConnectorFacadeImpl;
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

    private static final QueryFilterVisitor<Filter, ObjectClassInfoHelper, JsonPointer> RESOURCE_FILTER =
            new OpenICFFilterAdapter();

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
    @Reference(policy = ReferencePolicy.STATIC)
    protected IDMConnectionFactory connectionFactory;

    void bindConnectionFactory(final IDMConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        // update activityLogger to use the "real" activity logger on the router
        this.activityLogger = new RouterActivityLogger(connectionFactory);
    }

    void unbindConnectionFactory(final IDMConnectionFactory connectionFactory) {
        this.connectionFactory = null;
        // ConnectionFactory has gone away, use null activity logger
        this.activityLogger = NullActivityLogger.INSTANCE;
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
     * Enhanced configuration service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;

    /**
     * Reference to the ThreadSafe {@code ConnectorFacade} instance.
     */
    private final AtomicReference<ConnectorFacade> connectorFacade =
            new AtomicReference<ConnectorFacade>();

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
                                                            objectOperations.get(entry.getKey())));
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
                            } catch (Throwable t) {
                                logger.warn(t.getMessage());
                            }
                        }
                    });

            routeEntry = routerRegistry.addRoute(RouteBuilder.newBuilder()
                    .withTemplate(ProvisionerService.ROUTER_PREFIX + "/" + systemIdentifier.getName())
                    .withSingletonResourceProvider(this)
                    .buildNext()
                    .withModeStartsWith()
                    .withTemplate(ProvisionerService.ROUTER_PREFIX + "/" + systemIdentifier.getName() + ObjectClassRequestHandler.OBJECTCLASS_TEMPLATE)
                    .withRequestHandler(new ObjectClassRequestHandler())
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

    /**
     * Handle ConnectorExceptions from ConnectorFacade invocations.  Maps each ConnectorException subtype to the
     * appropriate {@link ResourceException} for passing to {@code handleError}.  Optionally logs to activity log.
     *
     * @param context the Context from the original request
     * @param request the original request
     * @param exception the ConnectorException that was thrown by the facade
     * @param resourceId the resourceId being operated on
     * @param before the object value "before" the request
     * @param after the object value "after" the request
     * @param connectorExceptionActivityLogger the ActivityLogger to use to log the exception
     */
    private ResourceException adaptConnectorException(Context context, Request request, ConnectorException exception,
        String resourceContainer, String resourceId, JsonValue before, JsonValue after,
        ActivityLogger connectorExceptionActivityLogger) {

        // default message
        String message = MessageFormat.format("Operation {0} failed with {1} on system object: {2}",
                request.getRequestType(), exception.getClass().getSimpleName(), resourceId);

        try {
            throw exception;
        } catch (AlreadyExistsException e) {
            message = MessageFormat.format("System object {0} already exists", resourceId);
            return new org.forgerock.json.resource.PreconditionFailedException(message, exception);
        } catch (ConfigurationException e) {
            message = MessageFormat.format("Operation {0} failed with ConfigurationException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new InternalServerErrorException(message, exception);
        } catch (ConnectionBrokenException e) {
            message = MessageFormat.format("Operation {0} failed with ConnectionBrokenException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ServiceUnavailableException(message, exception);
        } catch (ConnectionFailedException e) {
            message = MessageFormat.format("Connection failed during operation {0} on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ServiceUnavailableException(message, exception);
        } catch (ConnectorIOException e) {
            message = MessageFormat.format("Operation {0} failed with ConnectorIOException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ServiceUnavailableException(message, exception);
        } catch (OperationTimeoutException e) {
            message = MessageFormat.format("Operation {0} Timeout on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ServiceUnavailableException(message, exception);
        } catch (PasswordExpiredException e) {
            message = MessageFormat.format("Operation {0} failed with PasswordExpiredException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ForbiddenException(message, exception);
        } catch (InvalidPasswordException e) {
            message = MessageFormat.format("Invalid password has been provided to operation {0} for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return ResourceException.getException(UNAUTHORIZED_ERROR_CODE, message, exception);
        } catch (UnknownUidException e) {
            message = MessageFormat.format("Operation {0} could not find resource {1} on system object: {2}",
                    request.getRequestType().toString(), resourceId, resourceContainer);
            return new NotFoundException(message, exception).setDetail(new JsonValue(new HashMap<String, Object>()));
        } catch (InvalidCredentialException e) {
            message = MessageFormat.format("Invalid credential has been provided to operation {0} for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return ResourceException.getException(UNAUTHORIZED_ERROR_CODE, message, exception);
        } catch (PermissionDeniedException e) {
            message = MessageFormat.format("Permission was denied on {0} operation for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ForbiddenException(message, exception);
        } catch (ConnectorSecurityException e) {
            message = MessageFormat.format("Operation {0} failed with ConnectorSecurityException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new InternalServerErrorException(message, exception);
        } catch (InvalidAttributeValueException e) {
            message = MessageFormat.format("Attribute value conflicts with the attribute''s schema definition on " +
                    "operation {0} for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new BadRequestException(message, exception);
        } catch (PreconditionFailedException e) {
            message = MessageFormat.format("The resource version for {0} does not match the version provided on " +
                    "operation {1} for system object: {2}",
                    resourceId, request.getRequestType().toString(), resourceContainer);
            return new org.forgerock.json.resource.PreconditionFailedException(message, exception);
        } catch (PreconditionRequiredException e) {
            message = MessageFormat.format("No resource version for resource {0} has been provided on operation {1} for system object: {2}",
                    resourceId , request.getRequestType().toString(), resourceContainer);
            return new org.forgerock.json.resource.PreconditionRequiredException(message, exception);
        } catch (RetryableException e) {
            message = MessageFormat.format("Request temporarily unavailable on operation {0} for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ServiceUnavailableException(message, exception);
        } catch (UnsupportedOperationException e) {
            message = MessageFormat.format("Operation {0} is no supported for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new NotFoundException(message, exception);
        } catch (IllegalArgumentException e) {
            message = MessageFormat.format("Operation {0} failed with IllegalArgumentException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new InternalServerErrorException(message, e);
        } catch (RemoteWrappedException e) {
            return adaptRemoteWrappedException(context, request, exception, resourceContainer, resourceId,
                    before, after, connectorExceptionActivityLogger);
        } catch (ConnectorException e) {
            message = MessageFormat.format("Operation {0} failed with ConnectorException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new InternalServerErrorException(message, exception);
        } finally {
            // log the ConnectorException
            logger.debug(message, exception);
            try {
                connectorExceptionActivityLogger.log(context, request, message, resourceId,
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
     * Checks the RemoteWrappedException to determine which Exception has been wrapped and returns
     * the appropriated corresponding exception.
     *
     * @param context the Context from the original request
     * @param request the original request
     * @param exception the ConnectorException that was thrown by the facade
     * @param resourceId the resourceId being operated on
     * @param before the object value "before" the request
     * @param after the object value "after" the request
     * @param connectorExceptionActivityLogger the ActivityLogger to use to log the exception
     */
    private ResourceException adaptRemoteWrappedException(Context context, Request request,
            ConnectorException exception, String resourceContainer, String resourceId, JsonValue before,
            JsonValue after, ActivityLogger connectorExceptionActivityLogger) {

        RemoteWrappedException remoteWrappedException = (RemoteWrappedException) exception;
        final String message = exception.getMessage();
        final Throwable cause = exception.getCause();

        if (remoteWrappedException.is(AlreadyExistsException.class)) {
            return adaptConnectorException(context, request, new AlreadyExistsException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConfigurationException.class)) {
            return adaptConnectorException(context, request, new ConfigurationException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConnectionBrokenException.class)) {
            return adaptConnectorException(context, request, new ConnectionBrokenException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConnectionFailedException.class)) {
            return adaptConnectorException(context, request, new ConnectionFailedException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConnectorIOException.class)) {
            return adaptConnectorException(context, request, new ConnectorIOException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(InvalidAttributeValueException.class)) {
            return adaptConnectorException(context, request, new InvalidAttributeValueException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(InvalidCredentialException.class)) {
            return adaptConnectorException(context, request, new InvalidCredentialException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(InvalidPasswordException.class)) {
            return adaptConnectorException(context, request, new InvalidPasswordException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(OperationTimeoutException.class)) {
            return adaptConnectorException(context, request, new OperationTimeoutException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(PasswordExpiredException.class)) {
            return adaptConnectorException(context, request, new PasswordExpiredException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(PermissionDeniedException.class)) {
            return adaptConnectorException(context, request, new PermissionDeniedException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(PreconditionFailedException.class)) {
            return adaptConnectorException(context, request, new PreconditionFailedException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(PreconditionRequiredException.class)) {
            return adaptConnectorException(context, request, new PreconditionRequiredException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(RetryableException.class)) {
            return adaptConnectorException(context, request, RetryableException.wrap(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(UnknownUidException.class)) {
            return adaptConnectorException(context, request, new UnknownUidException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConnectorException.class)) {
            return adaptConnectorException(context, request, new ConnectorException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else {
            // handle .NET exceptions
            return adaptConnectorException(context, request,
                    DotNetExceptionHelper.fromExceptionClass(remoteWrappedException.getExceptionClass())
                            .getConnectorException(remoteWrappedException),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
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
             return adaptConnectorException(context, request, e, null, request.getResourcePath(), null, null,
                     activityLogger)
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

            List<Map<String, Object>> resultList =
                    new ArrayList<Map<String, Object>>(scriptContextBuilderList.size());
            result.put("actions", resultList);

            for (ScriptContextBuilder contextBuilder : scriptContextBuilderList) {
                boolean isShell = contextBuilder.getScriptLanguage().equalsIgnoreCase("Shell");
                for (Entry<String, String> entry : request.getAdditionalParameters().entrySet()) {
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
                                return new BadRequestException("Invalid type for password.").asPromise();
                            }
                        }
                        if ("username".equalsIgnoreCase(key)) {
                            if (value instanceof String == false) {
                                return new BadRequestException("Invalid type for username.").asPromise();
                            }
                        }
                        if ("workingdir".equalsIgnoreCase(key)) {
                            if (value instanceof String == false) {
                                return new BadRequestException("Invalid type for workingdir.").asPromise();
                            }
                        }
                        if ("timeout".equalsIgnoreCase(key)) {
                            if (!(value instanceof String) && !(value instanceof Number)) {
                                return new BadRequestException("Invalid type for timeout.").asPromise();
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
            return newActionResponse(result).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

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
     * @param operation
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
        OperationOptionInfoHelper operationOptionInfoHelper = null;

        if (null == facade.getOperation(operation)) {
            throw new NotSupportedException("Operation " + operation.getCanonicalName()
                            + " is not supported by the Connector");
        } else if (null != operationOptionInfoHelper
                && OperationOptionInfoHelper.OnActionPolicy.THROW_EXCEPTION
                        .equals(operationOptionInfoHelper.getOnActionPolicy())) {
            throw new ForbiddenException("Operation " + operation.getCanonicalName()
                            + " is configured to be denied");
        }
        return facade;
    }

    private class ObjectClassRequestHandler implements RequestHandler {

        public static final String OBJECTCLASS = "objectclass";
        public static final String OBJECTCLASS_TEMPLATE = "/{objectclass}";

        protected String getObjectClass(Context context) throws ResourceException {
            Map<String, String> variables = ResourceUtil.getUriTemplateVariables(context);
            if (null != variables && variables.containsKey(OBJECTCLASS)) {
                return variables.get(OBJECTCLASS);
            }
            throw new ForbiddenException(
                    "Direct access without Router to this service is forbidden.");
        }

        public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    return delegate.handleAction(context, request);
                } else {
                    throw new NotFoundException("Not found: " + objectClass);
                }
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            }
        }

        public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    return delegate.handleCreate(context, request);
                } else {
                    throw new NotFoundException("Not found: " + objectClass);
                }
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            }
        }

        public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    return delegate.handleDelete(context, request);
                } else {
                    throw new NotFoundException("Not found: " + objectClass);
                }
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            }
        }

        public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    return delegate.handlePatch(context, request);
                } else {
                    throw new NotFoundException("Not found: " + objectClass);
                }
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            }
        }

        public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
                QueryResourceHandler handler) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    return delegate.handleQuery(context, request, handler);
                } else {
                    throw new NotFoundException("Not found: " + objectClass);
                }
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            }
        }

        public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    return delegate.handleRead(context, request);
                } else {
                    throw new NotFoundException("Not found: " + objectClass);
                }
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            }
        }

        public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
            try {
                String objectClass = getObjectClass(context);
                RequestHandler delegate = objectClassHandlers.get(objectClass);
                if (null != delegate) {
                    return delegate.handleUpdate(context, request);
                } else {
                    throw new NotFoundException("Not found: " + objectClass);
                }
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
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
    private class ObjectClassResourceProvider implements RequestHandler {

        private final ObjectClassInfoHelper objectClassInfoHelper;
        private final Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations;
        private final String objectClass;

        private ObjectClassResourceProvider(String objectClass, ObjectClassInfoHelper objectClassInfoHelper,
                Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations) {
            this.objectClassInfoHelper = objectClassInfoHelper;
            this.operations = operations;
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
        private ConnectorFacade getConnectorFacade0(Class<? extends APIOperation> operation) throws ResourceException {
            final ConnectorFacade facade = getConnectorFacade();
            if (null == facade) {
                throw new ServiceUnavailableException();
            }
            OperationOptionInfoHelper operationOptionInfoHelper = operations.get(operation);

            if (null == facade.getOperation(operation)) {
                throw new NotSupportedException(
                        "Operation " + operation.getCanonicalName() + " is not supported by the Connector");
            } else if (null != operationOptionInfoHelper
                    && (null != operationOptionInfoHelper.getSupportedObjectTypes())) {
                if (!operationOptionInfoHelper.getSupportedObjectTypes().contains(
                        objectClassInfoHelper.getObjectClass().getObjectClassValue())) {
                    throw new NotSupportedException(
                            "Actions are not supported for resource instances");
                } else if (OperationOptionInfoHelper.OnActionPolicy.THROW_EXCEPTION.equals(
                        operationOptionInfoHelper.getOnActionPolicy())) {
                    throw new ForbiddenException(
                            "Operation " + operation.getCanonicalName() + " is configured to be denied");
                }
            }
            return facade;
        }

        private Promise<ActionResponse, ResourceException> handleAuthenticate(Context context, ActionRequest request)
                throws IOException {
            try {
                final ConnectorFacade facade = getConnectorFacade0(AuthenticationApiOp.class);
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
                result.put(ResourceResponse.FIELD_CONTENT_ID, uid.getUidValue());
                if (null != uid.getRevision()) {
                    result.put(ResourceResponse.FIELD_CONTENT_REVISION, uid.getRevision());
                }
                return newActionResponse(result).asPromise();
            } catch (ConnectorException e) {
                // handle ConnectorException from facade.authenticate:
                // log to activity log only if this is an external request
                // (let internal requests do their own logging upon the handleError...)
                throw adaptConnectorException(context, request, e, null, null, null, null,
                        ContextUtil.isExternal(context) ? activityLogger : NullActivityLogger.INSTANCE);
            }
        }

        private Promise<ActionResponse, ResourceException> handleLiveSync(
                Context context, ActionRequest request) throws ResourceException {
            final ActionRequest forwardRequest =
                    Requests.newActionRequest(ProvisionerService.ROUTER_PREFIX, request.getAction())
                            .setAdditionalParameter("source", getSource(objectClass));

            // forward request to be handled in SystemObjectSetService#actionInstance
            return connectionFactory.getConnection().action(context, forwardRequest).asPromise();

        }

        public Promise<ActionResponse, ResourceException> handleAction(
                Context context, ActionRequest request) {
            try {
                switch (request.getActionAsEnum(ObjectClassAction.class)) {
                    case authenticate:
                        return handleAuthenticate(context, request);
                    case liveSync:
                        return handleLiveSync(context, request);
                    default:
                        throw new BadRequestException("Unsupported action: " + request.getAction());
                }
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (JsonValueException e) {
                return new BadRequestException(e.getMessage(), e).asPromise();
            } catch (IllegalArgumentException e) { // from request.getActionAsEnum
                return new BadRequestException(e.getMessage(), e).asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
            try {
                final ConnectorFacade facade = getConnectorFacade0(CreateApiOp.class);
                final Set<Attribute> createAttributes =
                        objectClassInfoHelper.getCreateAttributes(request, cryptoService);

                OperationOptions operationOptions = operations.get(CreateApiOp.class)
                        .build(jsonConfiguration, objectClassInfoHelper)
                        .build();

                Uid uid = facade.create(objectClassInfoHelper.getObjectClass(),
                        AttributeUtil.filterUid(createAttributes), operationOptions);

                ResourceResponse resource = getCurrentResource(facade, uid, null);
                activityLogger.log(context, request, "message", getSource(objectClass, uid.getUidValue()),
                        null, resource.getContent(), Status.SUCCESS);
                return resource.asPromise();
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (ConnectorException e) {
                return adaptConnectorException(context, request, e, getSource(objectClass),
                        objectClassInfoHelper.getFullResourceId(request), request.getContent(), null, activityLogger)
                        .asPromise();
            } catch (JsonValueException e) {
                return new BadRequestException(e.getMessage(), e).asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleDelete(
                Context context, DeleteRequest request) {
            String resourceId = objectClassInfoHelper.getFullResourceId(request);
            try {
                if (resourceId.isEmpty()) {
                    throw new BadRequestException(
                            "The resource collection " + request.getResourcePath() + " cannot be deleted");
                }
                final ConnectorFacade facade = getConnectorFacade0(DeleteApiOp.class);
                final Uid uid = request.getRevision() != null
                        ? new Uid(resourceId, request.getRevision())
                        : new Uid(resourceId);

                // do a read first (largely for logging)
                ResourceResponse before = getCurrentResource(facade, uid, null);

                OperationOptions operationOptions = operations.get(DeleteApiOp.class)
                        .build(jsonConfiguration, objectClassInfoHelper)
                        .build();

                facade.delete(objectClassInfoHelper.getObjectClass(), uid, operationOptions);

                JsonValue result = before.getContent().copy();
                result.put(ResourceResponse.FIELD_CONTENT_ID, uid.getUidValue());
                if (null != uid.getRevision()) {
                    result.put(ResourceResponse.FIELD_CONTENT_REVISION, uid.getRevision());
                }
                activityLogger.log(context, request, "message", getSource(objectClass,
                        uid.getUidValue()), before.getContent(), null, Status.SUCCESS);
                return newResourceResponse(uid.getUidValue(), uid.getRevision(), result).asPromise();
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (ConnectorException e) {
                return adaptConnectorException(context, request, e,
                        getSource(objectClass), resourceId, null, null, activityLogger)
                        .asPromise();
            } catch (JsonValueException e) {
                return new BadRequestException(e.getMessage(), e).asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handlePatch(
                Context context, PatchRequest request) {
            JsonValue beforeValue = null;
            String resourceId = objectClassInfoHelper.getFullResourceId(request);
            try {
                if (resourceId.isEmpty()) {
                    throw new BadRequestException(
                            "The resource collection " + request.getResourcePath() + " cannot be patched");
                }
                
                final ConnectorFacade facade = getConnectorFacade0(UpdateApiOp.class);
                final Uid _uid = request.getRevision() != null
                        ? new Uid(resourceId, request.getRevision())
                        : new Uid(resourceId);

                // read resource before update for logging
                ResourceResponse before = getCurrentResource(facade, _uid, null);
                beforeValue = before.getContent();
                
                final Set<String> attributeNames = new HashSet<String>();
                final Set<Attribute> addedAttributes = new HashSet<Attribute>();
                final Set<Attribute> removedAttributes = new HashSet<Attribute>();
                final Set<Attribute> updatedAttributes = new HashSet<Attribute>();
                for (PatchOperation operation : request.getPatchOperations()) {
                	Attribute attribute = objectClassInfoHelper.getPatchAttribute(operation, beforeValue, cryptoService);	
                	if (attribute != null) {
						if (operation.isAdd()) {
							addedAttributes.add(attribute);
						} else if (operation.isRemove()) {
							removedAttributes.add(attribute);
						} else {
							updatedAttributes.add(attribute);
						}
						attributeNames.add(attribute.getName());
					}
                }

                OperationOptions operationOptions;
                OperationOptionsBuilder operationOptionsBuilder = operations.get(UpdateApiOp.class)
                        .build(jsonConfiguration, objectClassInfoHelper);

                final String reauthPassword = getReauthPassword(context);

                // if reauth and updating attribute requiring user credentials
                if (runAsUser(attributeNames, reauthPassword)) {
                    // get username attribute
                    final List<String> usernameAttrs =
                            jsonConfiguration.get(ConnectorUtil.OPENICF_CONFIGURATION_PROPERTIES)
                                    .get(ACCOUNT_USERNAME_ATTRIBUTES)
                                    .asList(String.class);
                    final String username = beforeValue.get(usernameAttrs.get(0)).asString();

                    if (StringUtils.isNotBlank(username)) {
                        operationOptionsBuilder.setRunAsUser(username)
                                .setRunWithPassword(new GuardedString(reauthPassword.toCharArray()));
                    }
                }

                operationOptions = operationOptionsBuilder.build();

                Uid uid = _uid;
                if (addedAttributes.size() > 0) {
                	// Perform any add operations
                	uid = facade.addAttributeValues(objectClassInfoHelper.getObjectClass(), uid, 
                			AttributeUtil.filterUid(addedAttributes), operationOptions);
                }
                if (removedAttributes.size() > 0) {
                	// Perform any remove operations
                    try {
                        uid = facade.removeAttributeValues(objectClassInfoHelper.getObjectClass(), uid,
                                AttributeUtil.filterUid(removedAttributes), operationOptions);
                    } catch (ConnectorException e) {
                        logger.debug("Error removing attribute values for object {}", uid, e);
                    }
                }
                if (updatedAttributes.size() > 0) {
                	// Perform any increment or replace operations
                	uid = facade.update(objectClassInfoHelper.getObjectClass(), uid, 
                			AttributeUtil.filterUid(updatedAttributes), operationOptions);
                }
                
                ResourceResponse resource = getCurrentResource(facade, uid, null);
                activityLogger.log(context, request, "message", getSource(objectClass, uid.getUidValue()),
                        beforeValue, resource.getContent(), Status.SUCCESS);
                return resource.asPromise();
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (ConnectorException e) {
                return adaptConnectorException(context, request, e, getSource(objectClass),
                        resourceId, beforeValue, null, activityLogger)
                        .asPromise();
            } catch (JsonValueException e) {
                return new BadRequestException(e.getMessage(), e).asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            }
        }

        @Override
        public Promise<QueryResponse, ResourceException> handleQuery(
                final Context context, final QueryRequest request, final QueryResourceHandler handler) {
            EventEntry measure = Publisher.start(getQueryEventName( objectClass, request), request, null);
            String resourceId = objectClassInfoHelper.getFullResourceId(request);
            try {
                if (!resourceId.isEmpty()) {
                    throw new BadRequestException(
                            "The resource instance " + request.getResourcePath() + " cannot be queried");
                }
                
                final ConnectorFacade facade = getConnectorFacade0(SearchApiOp.class);
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
                    filter = QueryFilters.parse(request.getQueryExpression()).accept(
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
                final Exception[] ex = new Exception[] { null };
                SearchResult searchResult = facade.search(objectClassInfoHelper.getObjectClass(), filter,
                        new ResultsHandler() {
                            @Override
                            public boolean handle(ConnectorObject obj) {
                                try {
                                    ResourceResponse resource = objectClassInfoHelper.build(obj, cryptoService);
                                    logValue.add(resource.getContent().getObject());
                                    return handler.handleResource(resource);
                                } catch (Exception e) {
                                    ex[0] = e;
                                    // TODO ICF needs a way to handle exceptions through the facade
                                    return false;
                                }
                            }
                        }, operationOptionsBuilder.build());
                if (ex[0] != null) {
                    throw new InternalServerErrorException(ex[0].getMessage(), ex[0]);
                }
                activityLogger.log(context, request,
                        "query: " + request.getQueryId()
                                + ", queryExpression: " + request.getQueryExpression()
                                + ", queryFilter: " + (request.getQueryFilter() != null ? request.getQueryFilter().toString() : null)
                                + ", parameters: " + request.getAdditionalParameters(),
                        request.getQueryId(), null, logValue, Status.SUCCESS);

                // TODO Support count policy and totalPagedResults
                return newResultPromise(
                        newQueryResponse(searchResult != null ? searchResult.getPagedResultsCookie() : null));
            } catch (EmptyResultSetException e) {
                // cause an empty-result to be returned
                return newResultPromise(newQueryResponse());
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (ConnectorException e) {
                return adaptConnectorException(context, request, e, null, null, null, null, activityLogger).asPromise();
            } catch (IllegalArgumentException | JsonValueException e) {
                return new BadRequestException(e.getMessage(), e).asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            } finally {
                measure.end();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleRead(
                Context context, ReadRequest request) {
            String resourceId = objectClassInfoHelper.getFullResourceId(request);
            try {
                if (resourceId.isEmpty()) {
                    throw new BadRequestException(
                            "The resource collection " + request.getResourcePath() + " cannot be read");
                }

                final ConnectorFacade facade = getConnectorFacade0(GetApiOp.class);
                Uid uid = new Uid(resourceId);
                ConnectorObject connectorObject = getConnectorObject(facade, uid, request.getFields());

                if (null != connectorObject) {
                    ResourceResponse resource = objectClassInfoHelper.build(connectorObject, cryptoService);
                    activityLogger.log(context, request, "message", getSource(objectClass, uid.getUidValue()),
                            resource.getContent(), resource.getContent(), Status.SUCCESS);
                    return resource.asPromise();
                } else {
                    final String matchedUri = context.containsContext(UriRouterContext.class)
                            ? context.asContext(UriRouterContext.class).getMatchedUri()
                            : "unknown path";
                    throw new NotFoundException("Object " + resourceId + " not found on " + matchedUri);

                }
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (ConnectorException e) {
                return adaptConnectorException(context, request, e,
                        getSource(objectClass), resourceId, null, null, activityLogger)
                        .asPromise();
            } catch (JsonValueException e) {
                return new BadRequestException(e.getMessage(), e).asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleUpdate(
                Context context, UpdateRequest request) {
            JsonValue content = request.getContent();
            String resourceId = objectClassInfoHelper.getFullResourceId(request);
            try {
                if (resourceId.isEmpty()) {
                    throw new BadRequestException(
                            "The resource collection " + request.getResourcePath() + " cannot be updated");
                }

                final ConnectorFacade facade = getConnectorFacade0(UpdateApiOp.class);
                final Uid _uid = request.getRevision() != null
                        ? new Uid(resourceId, request.getRevision())
                        : new Uid(resourceId);

                // read resource before update for logging
                ResourceResponse before = getCurrentResource(facade, _uid, null);

                // TODO Fix for http://bugster.forgerock.org/jira/browse/CREST-29
                final Name newName = null;
                final Set<Attribute> replaceAttributes =
                        objectClassInfoHelper.getUpdateAttributes(request, newName, cryptoService);

                OperationOptions operationOptions;
                OperationOptionsBuilder operationOptionsBuilder = operations.get(UpdateApiOp.class)
                        .build(jsonConfiguration, objectClassInfoHelper);

                final String reauthPassword = getReauthPassword(context);

                // if reauth and updating attributes requiring user credentials
                if (runAsUser(content.asMap().keySet(), reauthPassword)) {
                    // get username attribute
                    final List<String> usernameAttrs =
                            jsonConfiguration.get(ConnectorUtil.OPENICF_CONFIGURATION_PROPERTIES)
                                    .get(ACCOUNT_USERNAME_ATTRIBUTES)
                                    .asList(String.class);
                    final String username = content.get(usernameAttrs.get(0)).asString();

                    if (StringUtils.isNotBlank(username)) {
                        operationOptionsBuilder.setRunAsUser(username)
                                .setRunWithPassword(new GuardedString(reauthPassword.toCharArray()));
                    }
                }

                operationOptions = operationOptionsBuilder.build();

                Uid uid = facade.update(objectClassInfoHelper.getObjectClass(), _uid,
                        AttributeUtil.filterUid(replaceAttributes), operationOptions);

                ResourceResponse resource = getCurrentResource(facade, uid, null);
                activityLogger.log(context, request, "message", getSource(objectClass, uid.getUidValue()),
                        before.getContent(), resource.getContent(), Status.SUCCESS);
                return resource.asPromise();
            } catch (ResourceException e) {
                return e.asPromise();
            } catch (ConnectorException e) {
                return adaptConnectorException(context, request, e, getSource(objectClass),
                        resourceId, content, null, activityLogger)
                        .asPromise();
            } catch (JsonValueException e) {
                return new BadRequestException(e.getMessage(), e).asPromise();
            } catch (Exception e) {
                return new InternalServerErrorException(e.getMessage(), e).asPromise();
            }
        }

        // see if there is a reauth password provided
        private String getReauthPassword(Context context) {
            try {
                // get reauth password
                return context.asContext(HttpContext.class).getHeaderAsString(REAUTH_HEADER);
            } catch (Exception e) {
                // there will not always be a HttpContext and this is acceptable so catch exception to
                // prevent the exception from  stopping the remaining update
                return null;
            }
        }

        /**
         * Checks if any of the supplied attributes require re-authentication to update.
         * 
         * @param attributes the attributes being updated
         * @param reauthPassword the re-authentication password
         * @return true if a password is re-authentication is required, false otherwise.
         */
        private boolean runAsUser(Set<String> attributes, String reauthPassword) {
            final JsonValue properties = objectClassInfoHelper.getProperties();
            final Predicate<String> attributesToRunAsUser = new Predicate<String>() {
                @Override
                public boolean apply(String attribute) {
                    return !properties.get(attribute).isNull()
                            && properties.get(attribute).get(RUN_AS_USER).defaultTo(false).asBoolean();
                }
            };

            return StringUtils.isNotEmpty(reauthPassword)
                    && FluentIterable.from(attributes).filter(attributesToRunAsUser).iterator().hasNext();
        }

        private ResourceResponse getCurrentResource(final ConnectorFacade facade,
            final Uid uid, final List<JsonPointer> fields) throws IOException, JsonCryptoException {

            final ConnectorObject co = getConnectorObject(facade, uid, fields);
            if (null != co) {
                return objectClassInfoHelper.build(co, cryptoService);
            } else {
                JsonValue result = new JsonValue(new HashMap<String, Object>());
                result.put(ResourceResponse.FIELD_CONTENT_ID, uid.getUidValue());
                if (null != uid.getRevision()) {
                    result.put(ResourceResponse.FIELD_CONTENT_REVISION, uid.getRevision());
                }
                return newResourceResponse(uid.getUidValue(), uid.getRevision(), result);
            }
        }

        private ConnectorObject getConnectorObject(final ConnectorFacade facade,
            final Uid uid, final List<JsonPointer> fields) throws IOException, JsonCryptoException {

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
    }

    /**
     * Handle request on /system/[systemName]/schema
     *
     * @ThreadSafe
     */
    private static class SchemaResourceProvider implements SingletonResourceProvider {

        private final ResourceResponse schema;

        private SchemaResourceProvider(ResourceResponse schema) {
            this.schema = schema;
        }

        @Override
        public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
            return schema.asPromise();
        }

        @Override
        public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
            return new NotSupportedException("Actions are not supported for resource instances").asPromise();
        }

        @Override
        public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
            return new NotSupportedException("Patch operations are not supported").asPromise();
        }

        @Override
        public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
            return new NotSupportedException("Update operations are not supported").asPromise();
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
     * @param context the Context of the request requesting the status
     * @return a Map of the current status of a connector
     */
    public Map<String, Object> getStatus(Context context) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
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
                } catch (InvalidCredentialException e) {
                    jv.put("error", "Connection Error");
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
                                            Map<String, Object> syncFailureMap = new HashMap<String, Object>(6);
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

    /**
     * Package level setter to allow unit tests to set the logger.
     * @param activityLogger
     */
    void setActivityLogger(ActivityLogger activityLogger) {
        this.activityLogger = activityLogger;
    }
}
