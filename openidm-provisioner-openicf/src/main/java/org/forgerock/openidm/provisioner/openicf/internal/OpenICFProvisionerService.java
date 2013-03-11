/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.provisioner.openicf.internal;

import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.*;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConflictException;
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
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.AuditLogger;
import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.provisioner.openicf.ConnectorInfoProvider;
import org.forgerock.openidm.provisioner.openicf.ConnectorReference;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper;
import org.forgerock.openidm.router.RouteBuilder;
import org.forgerock.openidm.router.RouteEntry;
import org.forgerock.openidm.router.RouterRegistryService;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.util.ResourceUtil;
import org.identityconnectors.common.StringUtil;
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
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;
import org.identityconnectors.framework.common.exceptions.PasswordExpiredException;
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.ScriptContextBuilder;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
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
 * 
 * @author Laszlo Hordos
 */
@Component(name = OpenICFProvisionerService.PID, policy = ConfigurationPolicy.REQUIRE,
        description = "OpenIDM System Object Set Service", immediate = true)
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM System Object Set Service") })
public class OpenICFProvisionerService implements SingletonResourceProvider {

    // Public Constants
    public static final String PID = "org.forgerock.openidm.provisioner.openicf";

    /**
     * Setup logging for the {@link OpenICFProvisionerService}.
     */
    //private static final LocalizedLogger logger = LocalizedLogger.getLocalizedLogger(OpenICFProvisionerService.class);
    private static final Logger logger = LoggerFactory.getLogger(OpenICFProvisionerService.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Monitoring event name prefix
    private static final String EVENT_PREFIX = "openidm/internal/system/";

    /**
     * ConnectorInfoProvider service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected ConnectorInfoProvider connectorInfoProvider = null;

    private void bindConnectorInfoProvider(final ConnectorInfoProvider service) {
        connectorInfoProvider = service;
    }

    private void unbindConnectorInfoProvider(final ConnectorInfoProvider service) {
        connectorInfoProvider = null;
    }

    /**
     * ConnectorInfoProvider service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected AuditService auditService = null;

    private void bindAuditService(final AuditService service) {
        auditService = service;
    }

    private void unbindAuditService(final AuditService service) {
        auditService = null;
    }
    
    /**
     * RouterRegistryService service.
     */
    @Reference(policy = ReferencePolicy.STATIC)
    protected RouterRegistryService routerRegistryService;

    private void bindRouterRegistryService(final RouterRegistryService service) {
        routerRegistryService = service;
    }

    private void unbindRouterRegistryService(final RouterRegistryService service) {
        routerRegistryService = null;
    }

    /**
     * Cryptographic service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected CryptoService cryptoService = null;

    private void bindCryptoService(final CryptoService service) {
        cryptoService = service;
    }

    private void unbindCryptoService(final CryptoService service) {
        cryptoService = null;
    }

    /**
     * Reference to the ThreadSafe  {@code ConnectorFacade} instance.
     */
    private final AtomicReference<ConnectorFacade> connectorFacade = new AtomicReference<ConnectorFacade>();

    /**
     * Cache the SystemActions from local and {@code provisioner.json} configuration.
     */
    private final ConcurrentMap<String, SystemAction> localSystemActionCache = new ConcurrentHashMap<String, SystemAction>();

    private final ConcurrentMap<String, RequestHandler> objectClassHandlers = new ConcurrentHashMap<String, RequestHandler>();

    /**
     * System name for better logging only
     */
    private String systemName = null;

    private ConnectorFacadeCallback connectorFacadeCallback = null;

    private ConnectorReference connectorReference = null;

    // ----- Internal routing objects to register and remove the routes.

    private RouteEntry routeEntry;

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

    @Activate
    protected void activate(ComponentContext context) {
        try {
            final JsonValue configuration =
                    JSONEnhancedConfig.newInstance().getConfigurationAsJson(context);

            systemName = getSystemName(context, configuration);

            loadLocalSystemActions(configuration);

            connectorReference = ConnectorUtil.getConnectorReference(configuration);
             
            init(configuration);

            connectorFacadeCallback = new ConnectorFacadeCallback() {
                @Override
                public void addingConnectorInfo(ConnectorInfo connectorInfo, ConnectorFacadeFactory facadeFactory) {
                    try {
                        APIConfiguration config = connectorInfo.createDefaultAPIConfiguration();
                        ConnectorUtil.configureDefaultAPIConfiguration(configuration, config);

                        final ConnectorFacade facade = facadeFactory.newInstance(config);

                        if (null == facade) {
                            logger.warn("OpenICF ConnectorFacade of {} is not available",
                                    connectorReference);
                        } else {
                            if (connectorFacade.compareAndSet(null, facade)) {
                                if (null != routeEntry) {
                                    // This should not happen but keep it in case
                                    routeEntry.removeRoute();
                                }

                                if (facade.getSupportedOperations().contains(TestApiOp.class)) {
                                    try {
                                        facade.test();
                                        logger.debug("OpenICF connector test of {} succeeded!", systemName);
                                    } catch (Throwable e) {
                                        logger.error("OpenICF connector test of {} failed!", systemName, e);
                                    }
                                } else {
                                    logger.debug("OpenICF connector of {} does not support test.",
                                            connectorReference);
                                }
                            }
                        }
                    } catch (Throwable t) {
                        logger.error("//TODO FIX ME", t);
                    } finally {
                        logger.info("OpenICF Provisioner Service component {} is activated{}", systemName,
                                (null != connectorFacade.get() ? "."
                                        : " although the service is not available yet."));
                    }
                }

                @Override
                public void removedConnectorInfo(ConnectorInfo connectorInfo) {
                    connectorFacade.set(null);
                }
            };


            connectorInfoProvider.addConnectorFacadeCallback(connectorReference, connectorFacadeCallback);
            
            routeEntry = routerRegistryService.addRoute(RouteBuilder.newBuilder().withTemplate("/system/" + systemName).withSingletonResourceProvider(this)
                    .buildNext().withModeStartsWith().withTemplate(
                            "/system/" + systemName
                                    + ObjectClassRequestHandler.OBJECTCLASS_TEMPLATE)
                    .withRequestHandler(new ObjectClassRequestHandler()).seal());

            logger.info("OpenICF Provisioner Service component {} is activated{}", systemName,
                    (null != connectorFacade.get() ? "." : " although the service is not available yet."));
        } catch (Exception e) {
            logger.error("OpenICF Provisioner Service configuration has errors", e);
            throw new ComponentException("OpenICF Provisioner Service configuration has errors", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (null != connectorFacadeCallback){
        connectorInfoProvider.deleteConnectorFacadeCallback(connectorFacadeCallback);
        connectorFacadeCallback = null;
        }
        if (null != routeEntry) {
        routeEntry.removeRoute();
            routeEntry = null;
        }
        connectorFacade.set(null);
        logger.info("OpenICF Provisioner Service component {} is deactivated.", systemName);
    }


    private String getSystemName(ComponentContext context, JsonValue configuration) {
        String name = null;
        if (configuration.isDefined("name")) {
            name = configuration.get("name").required().asString();
        } else {
            name = (String) context.getProperties().get(ServerConstants.CONFIG_FACTORY_PID);
        }
        if (StringUtil.isBlank(name)) {
            throw new ComponentException(
                    "Failed to determine the system name from configuration.");
        } else {
            name = name.trim();
            if (!StringUtils.isAlphanumeric(name)) {
                throw new ComponentException("System name is not alphanumeric: " + name);
            }
        }
        return name;
    }

    private void loadLocalSystemActions(JsonValue configuration) {
        //TODO delay initialization /config/system

        if (configuration.isDefined("systemActions")) {
            for (JsonValue actionValue : configuration.get("systemActions").expect(List.class)) {
                SystemAction action = new SystemAction(actionValue);
                localSystemActionCache.put(action.getName(), action);
            }
        }
    }

    private void init(JsonValue configuration) {
        try {
            // TODO Iterate over the supported type and register
            boolean allowModification = !configuration.get("readOnly").defaultTo(false).asBoolean();
            if (!allowModification) {
                logger.debug("OpenICF Provisioner Service {} is running in read-only mode",
                        systemName);
            }

            Map<String, Map<Class<? extends APIOperation>, OperationOptionInfoHelper>> objectOperations =
                    ConnectorUtil.getOperationOptionConfiguration(configuration);

            for (Map.Entry<String, ObjectClassInfoHelper> entry : ConnectorUtil.getObjectTypes(
                    configuration).entrySet()) {
                
                objectClassHandlers.put(entry.getKey(), Resources.newCollection(new ObjectClassResourceProvider(entry.getValue(), objectOperations
                        .get(entry.getKey()), allowModification)));               
            }

            // TODO Fix this Map
            // ValidateApiOp
            // TestApiOp
            // ScriptOnConnectorApiOp
            // ScriptOnResourceApiOp
            // SchemaApiOp
            systemOperations = Collections.emptyMap();
        } catch (Exception e) {
            logger.error("OpenICF connector configuration of {} has errors.", systemName, e);
            throw new ComponentException(
                    "OpenICF connector configuration has errors and the service can not be initiated.",
                    e);
        }
        logger.debug("OpenICF connector configuration has no errors.");
    }


    ConnectorFacade getConnectorFacade() {
        return connectorFacade.get();
    }

    /**
     * Gets a brief stats report about the current status of this service
     * newBuilder. </p/> {@code "name" : "LDAP", "component.id" : "1",
     * "component.name" :
     * "org.forgerock.openidm.provisioner.openicf.ProvisionerService", "ok" :
     * true }
     * 
     * @return
     */
    /*
     * public Map<String, Object> getStatus() { Map<String, Object> result = new
     * LinkedHashMap<String, Object>(); try { JsonValue jv = new
     * JsonValue(result); jv.put("name", systemIdentifier.getName());
     * ConnectorFacade connectorFacade = getConnectorFacade(); try {
     * connectorFacade.test(); } catch (UnsupportedOperationException e) {
     * jv.put("reason", "TEST UnsupportedOperation"); } jv.put("ok", true); }
     * catch (Throwable e) { result.put("error", e.getMessage()); } return
     * result; }
     */
    //TODO include e.getMessage() both in the audit log and the propagated exception
    protected void handleError(Request request, Exception exception, ResultHandler<?> handler) {

        if (exception instanceof AlreadyExistsException) {
            if (logger.isDebugEnabled()) {
                logger.error("System object {} already exists", request.getResourceName(),
                        exception);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new ConflictException(exception));
        } else if (exception instanceof ConfigurationException) {
            if (logger.isDebugEnabled()) {
                // logger.error("Operation {} failed with ConfigurationException on system object: {}",
                // new Object[]{METHOD, id}, e);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new InternalServerErrorException(exception));
        } else if (exception instanceof ConnectionBrokenException) {
            if (logger.isDebugEnabled()) {
                // logger.error("Operation {} failed with ConnectionBrokenException on system object: {}",
                // new Object[]{METHOD, id}, e);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new ServiceUnavailableException(exception));
        } else if (exception instanceof ConnectionFailedException) {
            if (logger.isDebugEnabled()) {
                // logger.error("Connection failed during operation {} on system object: {}",
                // new Object[]{METHOD, id}, exception);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new ServiceUnavailableException(exception));
        } else if (exception instanceof ConnectorIOException) {
            if (logger.isDebugEnabled()) {
                // logger.error("Operation {} failed with ConnectorIOException on system object: {}",
                // new Object[]{METHOD, id}, exception);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new ServiceUnavailableException(exception));
        } else if (exception instanceof OperationTimeoutException) {
            if (logger.isDebugEnabled()) {
                // logger.error("Operation {} Timeout on system object: {}", new
                // Object[]{METHOD, id}, exception);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new ServiceUnavailableException(exception));
        } else if (exception instanceof PasswordExpiredException) {
            if (logger.isDebugEnabled()) {
                // logger.error("Operation {} failed with PasswordExpiredException on system object: {}",
                // new Object[]{METHOD, id}, exception);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new InternalServerErrorException(exception));
        } else if (exception instanceof InvalidPasswordException) {
            if (logger.isDebugEnabled()) {
                // logger.error("Invalid password has been provided to operation {} for system object: {}",
                // new Object[]{METHOD, id}, exception);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new InternalServerErrorException(exception));
        } else if (exception instanceof UnknownUidException) {
            if (logger.isDebugEnabled()) {
                // logger.error("Operation {} failed with UnknownUidException on system object: {}",
                // new Object[]{METHOD, id}, exception);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new NotFoundException("Not found " + request.getResourceName(),exception).setDetail(new JsonValue(new HashMap<String, Object>())));
        } else if (exception instanceof InvalidCredentialException) {
            if (logger.isDebugEnabled()) {
                // logger.error("Invalid credential has been provided to operation {} for system object: {}",new
                // Object[]{METHOD, id}, exception);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new InternalServerErrorException(exception));
        } else if (exception instanceof PermissionDeniedException) {
            if (logger.isDebugEnabled()) {
                // logger.error("Permission was denied on {} operation for system object: {}",
                // new Object[]{METHOD, id}, exception);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new ForbiddenException(exception));
        } else if (exception instanceof ConnectorSecurityException) {
            if (logger.isDebugEnabled()) {
                // logger.error("Operation {} failed with ConnectorSecurityException on system object: {}",
                // new Object[]{METHOD, id}, exception);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new InternalServerErrorException(exception));
        } else if (exception instanceof ConnectorException) {
            if (logger.isDebugEnabled()) {
                // logger.error("Operation {} failed with ConnectorException on system object: {}",
                // new Object[]{METHOD, id}, exception);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new InternalServerErrorException(exception));
        } else if (exception instanceof ResourceException) {
            // rethrow the the expected JsonResourceException
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError((ResourceException) exception);
        } else if (exception instanceof JsonValueException) {
            if (logger.isDebugEnabled()) {
                // logger.error("Operation {} failed with Exception on system object: {}",
                // new Object[]{METHOD, id}, exception);
            }
            // ActivityLog.log(router, request, "Bad Request", null, before,
            // after, Status.FAILURE);
            handler.handleError(new BadRequestException(exception));
        } else {
            if (logger.isDebugEnabled()) {
                // logger.error("Operation {} failed with Exception on system object: {}",
                // new Object[]{METHOD, id}, exception);
            }
            // ActivityLog.log(router, request, "Operation " + METHOD.name() +
            // " failed with " + e.getClass().getSimpleName(), id.toString(),
            // before, after, Status.FAILURE);
            handler.handleError(new InternalServerErrorException(exception));
        }
    }

    // /**
    // * TODO: Description.
    // * <p/>
    // * This method catches any thrown {@code JsonValueException}, and rethrows
    // it as a
    // * {@link org.forgerock.json.resource.JsonResourceException#BAD_REQUEST}.
    // */
    // @Override
    // public JsonValue handle(JsonValue request) throws JsonResourceException {
    //
    // JsonValue before = null;
    // JsonValue after = null;
    // try {
    // try {
    // traceObject(METHOD, id, value);
    // switch (METHOD) {
    // case create:
    // before = value;
    // after = create(id, value.required(), params);
    // ActivityLog.log(router, request, "message", id.toString(), before, after,
    // Status.SUCCESS);
    // return after;
    // case read:
    // after = read(id, params);
    // ActivityLog.log(router, request, "message", id.toString(), before, after,
    // Status.SUCCESS);
    // return after;
    // case update:
    // before = value;
    // after = update(id, rev, value.required(), params);
    // ActivityLog.log(router, request, "message", id.toString(), before, after,
    // Status.SUCCESS);
    // return after;
    // case delete:
    // try {
    // before = read(id, params);
    // } catch (Exception e) {
    // logger.info("Operation read of {} failed before delete", id, e);
    // }
    // after = delete(id, rev, params);
    // ActivityLog.log(router, request, "message", id.toString(), before, after,
    // Status.SUCCESS);
    // return after;
    // case query:
    // // FIXME: According to the JSON resource specification (now published),
    // query parameters are
    // // required. There is a unit test that attempts to query all merely by
    // executing query
    // // without any parameters. As a result, the commented-out line
    // below—which conforms to the
    // // spec—breaks during unit testing.
    // // return query(id, params.required());
    // before = params;
    // after = query(id, params.required());
    // ActivityLog.log(router, request, "message", id.toString(), before, after,
    // Status.SUCCESS);
    // return after;
    // case action:
    // before = new JsonValue(new HashMap());
    // before.put("value", value);
    // before.put("params", params);
    // ActionId actionId =
    // params.get(ServerConstants.ACTION_NAME).required().asEnum(ActionId.class);
    // after = action(id, actionId, value, params.required());
    // ActivityLog.log(router, request, "message", id.toString(), before, after,
    // Status.SUCCESS);
    // return after;
    // default:
    // throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
    // }
    // } catch (AlreadyExistsException e) {
    // }

    /**
     * @return the smartevent Name for a given query
     */
    org.forgerock.openidm.smartevent.Name getQueryEventName(String objectClass, JsonValue params,
            Map<String, Object> query, String queryId) {
        String prefix = EVENT_PREFIX + systemName + "/" + objectClass + "/query/";
        if (params == null) {
            return org.forgerock.openidm.smartevent.Name.get(prefix + "_default_query");
        } else if (query != null) {
            return org.forgerock.openidm.smartevent.Name.get(prefix + "_query_expression");
        } else {
            return org.forgerock.openidm.smartevent.Name.get(prefix + queryId);
        }
    }

    /**
     * This newBuilder and this method can not be scheduled. The call MUST go
     * through the {@code org.forgerock.openidm.provisioner}
     * <p/>
     * Invoked by the scheduler when the scheduler triggers.
     * <p/>
     * Synchronization object: {@code "connectorData" : { "syncToken" :
     * "1305555929000", "nativeType" : "JAVA_TYPE_LONG" },
     * "synchronizationStatus" : { "errorStatus" : null, "lastKnownServer" :
     * "localServer", "lastModDate" : "2011-05-16T14:47:58.587Z", "lastModNum" :
     * 668, "lastPollDate" : "2011-05-16T14:47:52.875Z", "lastStartTime" :
     * "2011-05-16T14:29:07.863Z", "progressMessage" : "SUCCEEDED" } }}
     * <p/>
     * {@inheritDoc} Synchronise the changes from the end system for the given
     * {@code objectType}.
     * <p/>
     * OpenIDM takes active role in the synchronization process by asking the
     * end system to get all changed object. Not all system is capable to
     * fulfill this kind of request but if the end system is capable then the
     * implementation send each changes to the
     * {@link org.forgerock.openidm.sync.SynchronizationListener} and when it
     * finished it return a new <b>stage</b> object.
     * <p/>
     * The {@code previousStage} object is the previously returned value of this
     * method.
     * 
     * @param previousStage
     *            The previously returned object. If null then it's the first
     *            execution.
     * @param synchronizationListener
     *            The listener to send the changes to.
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
    // public JsonValue liveSynchronize(String objectType, JsonValue
    // previousStage, final ResultHandler<Resource> synchronizationListener) {
    // if (!serviceAvailable) return previousStage;
    // JsonValue stage = previousStage != null ? previousStage.copy() : new
    // JsonValue(new LinkedHashMap<String, Object>());
    // JsonValue connectorData = stage.get("connectorData");
    // SyncToken token = null;
    // if (!connectorData.isNull()) {
    // if (connectorData.isMap()) {
    // token = ConnectorUtil.convertToSyncToken(connectorData);
    // } else {
    // throw new
    // IllegalArgumentException("Illegal connectorData property. Value must be Map");
    // }
    // }
    // stage.remove("lastException");
    // try {
    // final OperationHelper helper = operationHelperBuilder.build(objectType,
    // stage, cryptoService);
    // if (helper.isOperationPermitted(SyncApiOp.class)) {
    // ConnectorFacade connector = getConnectorFacade();
    // SyncApiOp operation = (SyncApiOp)
    // connector.getOperation(SyncApiOp.class);
    // if (null == operation) {
    // throw new
    // UnsupportedOperationException(SyncApiOp.class.getCanonicalName());
    // }
    // if (null == token) {
    // token = operation.getLatestSyncToken(helper.getObjectClass());
    // logger.debug("New LatestSyncToken has been fetched. New token is: {}",
    // token);
    // } else {
    // final SyncToken[] lastToken = new SyncToken[]{token};
    // final String[] failedRecord = new String[1];
    // OperationOptionsBuilder operationOptionsBuilder =
    // helper.getOperationOptionsBuilder(SyncApiOp.class, null, previousStage);
    // try {
    // logger.debug("Execute sync(ObjectClass:{}, SyncToken:{})",
    // new Object[]{helper.getObjectClass().getObjectClassValue(), token});
    // operation.sync(helper.getObjectClass(), token, new SyncResultsHandler() {
    // /**
    // * Called to handle a delta in the stream. The Connector framework will
    // call
    // * this method multiple times, once for each result.
    // * Although this method is callback, the framework will invoke it
    // synchronously.
    // * Thus, the framework guarantees that once an application's call to
    // * {@link
    // org.identityconnectors.framework.api.operations.SyncApiOp#sync(org.identityconnectors.framework.common.objects.ObjectClass,
    // org.identityconnectors.framework.common.objects.SyncToken,
    // org.identityconnectors.framework.common.objects.SyncResultsHandler,
    // org.identityconnectors.framework.common.objects.OperationOptions)}
    // SyncApiOp#sync()} returns,
    // * the framework will no longer call this method
    // * to handle results from that <code>sync()</code> operation.
    // *
    // * @param syncDelta The change
    // * @return True iff the application wants to continue processing more
    // * results.
    // * @throws RuntimeException If the application encounters an exception.
    // This will stop
    // * iteration and the exception will propagate to
    // * the application.
    // */
    // public boolean handle(SyncDelta syncDelta) {
    // try {
    // switch (syncDelta.getDeltaType()) {
    // case CREATE_OR_UPDATE:
    // Resource deltaObject = helper.build(syncDelta.getObject());
    // if (null != syncDelta.getPreviousUid()) {
    // deltaObject.put("_previous-id",
    // Id.escapeUid(syncDelta.getPreviousUid().getUidValue()));
    // }
    // synchronizationListener.onUpdate(helper.resolveQualifiedId(syncDelta.getUid()).toString(),
    // null, new JsonValue(deltaObject));
    // break;
    // case DELETE:
    // synchronizationListener.onDelete(helper.resolveQualifiedId(syncDelta.getUid()).toString(),
    // null);
    // break;
    // }
    // lastToken[0] = syncDelta.getToken();
    // } catch (Exception e) {
    // failedRecord[0] = SerializerUtil.serializeXmlObject(syncDelta, true);
    // if (logger.isDebugEnabled()) {
    // logger.error("Failed synchronise {} object", syncDelta.getUid(), e);
    // }
    // throw new ConnectorException("Failed synchronise " + syncDelta.getUid() +
    // " object" + e.getMessage(), e);
    // }
    // return true;
    // }
    // }, operationOptionsBuilder.build());
    // } catch (Throwable t) {
    // Map<String, Object> lastException = new LinkedHashMap<String, Object>(2);
    // lastException.put("throwable", t.getMessage());
    // if (null != failedRecord[0]) {
    // lastException.put("syncDelta", failedRecord[0]);
    // }
    // stage.put("lastException", lastException);
    // if (logger.isDebugEnabled()) {
    // logger.error("Live synchronization of {} failed on {}",
    // new Object[]{objectType, systemName}, t);
    // }
    // } finally {
    // token = lastToken[0];
    // logger.debug("Synchronization is finished. New LatestSyncToken value: {}",
    // token);
    // }
    // }
    // if (null != token) {
    // stage.put("connectorData", ConnectorUtil.convertFromSyncToken(token));
    // }
    // }
    // } catch (ResourceException e) {
    // if (logger.isDebugEnabled()) {
    // logger.debug("Failed to get OperationHelper", e);
    // }
    // throw new RuntimeException(e);
    // } catch (Exception e) {
    // // catch helper.getOperationOptionsBuilder(
    // if (logger.isDebugEnabled()) {
    // logger.debug("Failed to get OperationOptionsBuilder", e);
    // }            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR,
    //"Failed to get OperationOptionsBuilder: " + e.getMessage(), e);
    //
    // }
    // return stage;
    // }

    private void traceObject(Request request) {
        if (logger.isTraceEnabled()) {
            if (null != request) {
                try {
                    StringWriter writer = new StringWriter();
                    // TODO Change request.saveToJson()
                    MAPPER.writerWithDefaultPrettyPrinter().writeValue(writer, request);
                    logger.info("Invoke action: ", writer);
                } catch (IOException e) {
                    // Don't care
                }
            }
        }
    }

    private enum ConnectorAction {
        script;
    }

    @Override
    public void readInstance(ServerContext context, ReadRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Read operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void actionInstance(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> resultHandler) {
        AuditLogger<ActionRequest, ResultHandler<JsonValue>> auditLogger = auditService.before(context,request,resultHandler);
        final ResultHandler<JsonValue> handler = auditLogger.getResultHandler();
        try {


            if (ConnectorAction.script.name().equalsIgnoreCase(request.getActionId())) {
                // TODO NPE check
                if (StringUtils.isBlank(request.getAdditionalActionParameters().get(
                        SystemAction.SCRIPT_ID))) {
                    handler.handleError(new BadRequestException("Missing required parameter: "+ SystemAction.SCRIPT_ID));
                    return;
                }
                SystemAction action =
                        localSystemActionCache.get(request.getAdditionalActionParameters().get(
                                SystemAction.SCRIPT_ID));

                String systemType = null;// connectorReference.getConnectorKey().getConnectorName();
                List<ScriptContextBuilder> scriptContextBuilderList =
                        action.getScriptContextBuilders(systemType);
                if (null != scriptContextBuilderList) {
                    // OperationHelper helper = operationHelperBuilder
                    // .build(id.getObjectType(),
                    // params, cryptoService);

                    JsonValue result = new JsonValue(new HashMap<String, Object>());

                    boolean onConnector =
                            !"resource".equalsIgnoreCase(request.getAdditionalActionParameters()
                                    .get(SystemAction.SCRIPT_EXECUTE_MODE));

                    final ConnectorFacade facade =
                            getConnectorFacade0(handler, onConnector ? ScriptOnConnectorApiOp.class
                                    : ScriptOnResourceApiOp.class);
                    if (null != facade) {

                        String variablePrefix =
                                request.getAdditionalActionParameters().get(
                                        SystemAction.SCRIPT_VARIABLE_PREFIX);

                        List<Map<String, Object>> resultList =
                                new ArrayList<Map<String, Object>>(scriptContextBuilderList.size());
                        result.put("actions", resultList);

                        for (ScriptContextBuilder contextBuilder : scriptContextBuilderList) {
                            boolean isShell =
                                    contextBuilder.getScriptLanguage().equalsIgnoreCase("Shell");
                            for (Map.Entry<String, String> entry : request
                                    .getAdditionalActionParameters().entrySet()) {
                                if (entry.getKey().startsWith("_")) {
                                    continue;
                                }
                                Object value = entry.getValue();
                                Object newValue = value;
                                if (isShell) {
                                    if ("password".equalsIgnoreCase(entry.getKey())) {
                                        if (value instanceof String) {
                                            newValue =
                                                    new GuardedString(((String) value)
                                                            .toCharArray());
                                        } else {
                                            throw new BadRequestException(
                                                    "Invalid type for password.");
                                        }
                                    }
                                    if ("username".equalsIgnoreCase(entry.getKey())) {
                                        if (value instanceof String == false) {
                                            throw new BadRequestException(
                                                    "Invalid type for username.");
                                        }
                                    }
                                    if ("workingdir".equalsIgnoreCase(entry.getKey())) {
                                        if (value instanceof String == false) {
                                            throw new BadRequestException(
                                                    "Invalid type for workingdir.");
                                        }
                                    }
                                    if ("timeout".equalsIgnoreCase(entry.getKey())) {
                                        if (value instanceof String == false
                                                && value instanceof Number == false) {
                                            throw new BadRequestException(
                                                    "Invalid type for timeout.");
                                        }
                                    }
                                    contextBuilder.addScriptArgument(entry.getKey(), newValue);
                                    continue;
                                }

                                if (null != value) {
                                    if (value instanceof Collection) {
                                        newValue =
                                                Array.newInstance(Object.class,
                                                        ((Collection) value).size());
                                        int i = 0;
                                        for (Object v : (Collection) value) {
                                            if (null == v
                                                    || FrameworkUtil.isSupportedAttributeType(v
                                                            .getClass())) {
                                                Array.set(newValue, i, v);
                                            } else {
                                                // Serializable may not be
                                                // acceptable
                                                Array.set(newValue, i,
                                                        v instanceof Serializable ? v : v
                                                                .toString());
                                            }
                                            i++;
                                        }

                                    } else if (value.getClass().isArray()) {
                                        // TODO implement the array support
                                        // later
                                    } else if (!FrameworkUtil.isSupportedAttributeType(value
                                            .getClass())) {
                                        // Serializable may not be acceptable
                                        newValue =
                                                value instanceof Serializable ? value : value
                                                        .toString();
                                    }
                                }
                                contextBuilder.addScriptArgument(entry.getKey(), newValue);
                            }
                            // contextBuilder.addScriptArgument("openidm_id",
                            // id.toString());

                            // ScriptContext scriptContext =
                            // script.getScriptContextBuilder().build();
                            OperationOptionsBuilder operationOptionsBuilder =
                                    new OperationOptionsBuilder();

                            // It's necessary to keep the backward compatibility
                            // with Waveset IDM
                            if (null != variablePrefix && isShell) {
                                operationOptionsBuilder.setOption("variablePrefix", variablePrefix);
                            }

                            Map<String, Object> actionResult = new HashMap<String, Object>(2);
                            try {
                                Object scriptResult = null;
                                if (onConnector) {
                                    scriptResult =
                                            facade.runScriptOnConnector(contextBuilder.build(),
                                                    operationOptionsBuilder.build());
                                } else {
                                    scriptResult =
                                            facade.runScriptOnResource(contextBuilder.build(),
                                                    operationOptionsBuilder.build());
                                }
                                actionResult.put("result", ConnectorUtil.coercedTypeCasting(
                                        scriptResult, Object.class));
                            } catch (Throwable t) {
                                if (logger.isDebugEnabled()) {
                                    logger.error("Script execution error.", t);
                                }
                                actionResult.put("error", t.getMessage());
                            }
                            resultList.add(actionResult);
                        }
                    }
                }
            } else {
                handler.handleError(new BadRequestException("Unsupported action: "
                        + request.getActionId()));
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (ConnectorException e) {
           handleError(request,e, handler);
        } catch (JsonValueException e) {
            handler.handleError(new BadRequestException(e));
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
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
            throw new ForbiddenException("Direct access without Router to this service is forbidden.");
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
                handler.handleError(new InternalServerErrorException(e));
            }
        }

        public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
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
                handler.handleError(new InternalServerErrorException(e));
            }
        }

        public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
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
                handler.handleError(new InternalServerErrorException(e));
            }
        }

        public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
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
                handler.handleError(new InternalServerErrorException(e));
            }
        }

        public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
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
                handler.handleError(new InternalServerErrorException(e));
            }
        }

        public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
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
                handler.handleError(new InternalServerErrorException(e));
            }
        }

        public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
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
                handler.handleError(new InternalServerErrorException(e));
            }
        }
    }

    private enum ObjectClassAction {
        authenticate, resolveUsername;
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

        private ObjectClassResourceProvider(ObjectClassInfoHelper objectClassInfoHelper,
                Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations, boolean allowModification) {
            this.objectClassInfoHelper = objectClassInfoHelper;
            this.operations = operations;
            this.allowModification = allowModification;
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
                e =
                        new NotSupportedException("Operation " + operation.getCanonicalName()
                                + " is not supported by the Connector");
            } else if (null != operationOptionInfoHelper
                    && (null != operationOptionInfoHelper.getSupportedObjectTypes())) {
                if (!operationOptionInfoHelper.getSupportedObjectTypes().contains(
                        objectClassInfoHelper.getObjectClass().getObjectClassValue())) {
                    e =
                            new NotSupportedException(
                                    "Actions are not supported for resource instances");
                } else if (OperationOptionInfoHelper.OnActionPolicy.THROW_EXCEPTION
                        .equals(operationOptionInfoHelper.getOnActionPolicy())) {
                    e =
                            new ForbiddenException("Operation " + operation.getCanonicalName()
                                    + " is configured to be denied");
                }
            }
            if (null != e) {
                handler.handleError(e);
                return null;
            }
            return facade;
        }

        @Override
        public void actionCollection(ServerContext context, ActionRequest request,
                ResultHandler<JsonValue> handler) {
            try {
                if (ObjectClassAction.authenticate.name().equalsIgnoreCase(request.getActionId())) {
                    final ConnectorFacade facade =
                            getConnectorFacade0(handler, AuthenticationApiOp.class);
                    if (null != facade) {
                        JsonValue params = new JsonValue(request.getAdditionalActionParameters());
                        String username = params.get("username").required().asString();
                        String password = params.get("password").required().asString();

                        OperationOptionInfoHelper helper =
                                operations.get(AuthenticationApiOp.class);
                        OperationOptions operationOptions = null;
                        if (null != helper) {
                            operationOptions = null; // helper.
                        }

                        // Throw ConnectorException
                        Uid uid =
                                facade.authenticate(objectClassInfoHelper.getObjectClass(),
                                        username, new GuardedString(password.toCharArray()),
                                        operationOptions);

                        JsonValue result = new JsonValue(new HashMap<String, Object>());
                        result.put(ServerConstants.OBJECT_PROPERTY_ID, uid.getUidValue());
                        if (null != uid.getRevision()) {
                            result.put(ServerConstants.OBJECT_PROPERTY_REV, uid.getRevision());
                        }

                        handler.handleResult(result);
                    }
                } else {
                    handler.handleError(new BadRequestException("Unsupported action: "
                            + request.getActionId()));
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (ConnectorException e) {
                handleError(request,e, handler);
            } catch (JsonValueException e) {
                handler.handleError(new BadRequestException(e));
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e));
            }
        }

        @Override
        public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                ResultHandler<JsonValue> handler) {
            final ResourceException e =
                    new NotSupportedException("Actions are not supported for resource instances");
            handler.handleError(e);
        }

        @Override
        public void createInstance(ServerContext context, CreateRequest request,
                ResultHandler<Resource> handler) {
            try {
                if (objectClassInfoHelper.isCreateable()) {
                    if (null == request.getNewResourceId()) {
                        final ConnectorFacade facade =
                                getConnectorFacade0(handler, CreateApiOp.class);
                        if (null != facade) {
                            final Set<Attribute> createAttributes =
                                    objectClassInfoHelper.getCreateAttributes(request,
                                            cryptoService);
                            OperationOptionInfoHelper helper = operations.get(CreateApiOp.class);
                            OperationOptions operationOptions = null;
                            if (null != helper) {
                                operationOptions = null; // helper.
                            }

                            Uid uid =
                                    facade.create(objectClassInfoHelper.getObjectClass(),
                                            AttributeUtil.filterUid(createAttributes),
                                            operationOptions);

                            returnResource(request, handler, facade, uid);
                        }
                    } else {
                        // If the __NAME__ attribute is not mapped then fail
                        final ResourceException e =
                                new NotSupportedException(
                                        "Create operations are not supported with client assigned id");
                        handler.handleError(e);
                    }
                } else {
                    // If the __NAME__ attribute is not mapped then fail
                    final ResourceException e =
                            new NotSupportedException("Create operations are not supported on "
                                    + objectClassInfoHelper.getObjectClass());
                    handler.handleError(e);
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (ConnectorException e) {
                handleError(request, e, handler);
            } catch (JsonValueException e) {
                handler.handleError(new BadRequestException(e.getMessage()));
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage()));
            }
        }

        @Override
        public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                ResultHandler<Resource> handler) {
            try {
                final ConnectorFacade facade = getConnectorFacade0(handler, DeleteApiOp.class);
                if (null != facade) {

                    OperationOptionInfoHelper helper = operations.get(DeleteApiOp.class);
                    OperationOptions operationOptions = null;
                    if (null != helper) {
                        operationOptions = null; // helper.
                    }
                    Uid uid = null != request.getRevision() ? new Uid(resourceId, request.getRevision()) :new Uid(resourceId);
                    facade.delete(objectClassInfoHelper.getObjectClass(), uid, operationOptions);

                    JsonValue result = new JsonValue(new HashMap<String, Object>());
                    result.put(ServerConstants.OBJECT_PROPERTY_ID, uid.getUidValue());
                    if (null != uid.getRevision()) {
                        result.put(ServerConstants.OBJECT_PROPERTY_REV, uid.getRevision());
                    }

                    handler.handleResult(new Resource(uid.getUidValue(), uid.getRevision(), result));
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (ConnectorException e) {
                handleError(request,e, handler);
            } catch (JsonValueException e) {
                handler.handleError(new BadRequestException(e.getMessage()));
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage()));
            }
        }

        @Override
        public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                ResultHandler<Resource> handler) {
            final ResourceException e =
                    new NotSupportedException("Patch operations are not supported");
            handler.handleError(e);
        }

        @Override
        public void queryCollection(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler) {
            EventEntry measure = null;// Publisher.start(getQueryEventName(id,
                                      // params, query.asMap(),
                                      // queryId.asString()), null, id);
            try {
                final ConnectorFacade facade = getConnectorFacade0(handler, SearchApiOp.class);
                if (null != facade) {

                    OperationOptionInfoHelper helper = operations.get(SearchApiOp.class);
                    OperationOptionsBuilder operationOptionsBuilder = null;
                    if (null != helper) {
                        operationOptionsBuilder = new OperationOptionsBuilder(); // helper.
                    }

                    Filter filter = null;

                    if (request.getQueryId() != null) {
                        if (ServerConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                            operationOptionsBuilder.setAttributesToGet(Uid.NAME);
                        } else {
                            handler.handleError(new BadRequestException("Unsupported _queryId: "
                                    + request.getQueryId()));
                            return;
                        }
                    } else if (request.getQueryExpression() != null) {
                        filter =
                                QueryFilter.valueOf(request.getQueryExpression()).accept(
                                        RESOURCE_FILTER, objectClassInfoHelper);

                    } else {
                        // No filtering or query by filter.
                        filter =
                                request.getQueryFilter().accept(RESOURCE_FILTER,
                                        objectClassInfoHelper);

                    }

                    // If paged results are requested then decode the cookie in
                    // order to determine
                    // the index of the first result to be returned.
                    final int pageSize = request.getPageSize();
                    final String pagedResultsCookie = request.getPagedResultsCookie();
                    final boolean pagedResultsRequested = request.getPageSize() > 0;

                    facade.search(objectClassInfoHelper.getObjectClass(), filter,
                            new ResultsHandler() {
                                @Override
                                public boolean handle(ConnectorObject obj) {
                                    try {
                                        return handler.handleResource(objectClassInfoHelper.build(
                                                obj, cryptoService));
                                    } catch (Exception e) {
                                        handler.handleError(new InternalServerErrorException(e));
                                        return false;
                                    }
                                }
                            }, operationOptionsBuilder.build());
                    if (request.getPageSize() > 0) {
                        // pagedResultsRequested
                        handler.handleResult(new QueryResult());
                        // handler.handleResult(new QueryResult(nextCookie,
                        // remaining));
                    } else {
                        handler.handleResult(new QueryResult());
                    }
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (ConnectorException e) {
                handleError(request,e, handler);
            } catch (JsonValueException e) {
                handler.handleError(new BadRequestException(e));
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e));
            } finally {
                measure.end();
            }
        }

        @Override
        public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                ResultHandler<Resource> handler) {
            try {
                final ConnectorFacade facade = getConnectorFacade0(handler, GetApiOp.class);
                if (null != facade) {

                    OperationOptionInfoHelper helper = operations.get(GetApiOp.class);
                    OperationOptionsBuilder OOBuilder = new OperationOptionsBuilder();
                    if (null == request.getFieldFilters() || request.getFieldFilters().isEmpty()) {
                        OOBuilder.setAttributesToGet(objectClassInfoHelper.getAttributesReturnedByDefault());
                    } else {
                        objectClassInfoHelper.setAttributesToGet(OOBuilder, request.getFieldFilters());
                    }
                    Uid uid = new Uid(resourceId);
                    ConnectorObject connectorObject =
                            facade.getObject(objectClassInfoHelper.getObjectClass(), uid,
                                    OOBuilder.build());

                    if (null != connectorObject) {
                        handler.handleResult(objectClassInfoHelper.build(connectorObject,
                                cryptoService));
                    } else {
                        handler.handleError(new NotFoundException(request.getResourceName()));
                    }
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (ConnectorException e) {
                handleError(request,e, handler);
            } catch (JsonValueException e) {
                handler.handleError(new BadRequestException(e));
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e));
            }
        }

        @Override
        public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                ResultHandler<Resource> handler) {
            try {
                final ConnectorFacade facade = getConnectorFacade0(handler, UpdateApiOp.class);
                if (null != facade) {
                    //TODO Fix for http://bugster.forgerock.org/jira/browse/CREST-29
                    final Name newName = null;
                    final Set<Attribute> replaceAttributes = objectClassInfoHelper.getUpdateAttributes(request, newName, cryptoService);

                    OperationOptionInfoHelper helper = operations.get(UpdateApiOp.class);
                    OperationOptions operationOptions = null;
                    if (null != helper) {
                        operationOptions = null; // helper.
                    }
                    Uid _uid = null != request.getRevision() ? new Uid(resourceId, request.getRevision()) :new Uid(resourceId);
                    Uid uid =
                            facade.update(objectClassInfoHelper.getObjectClass(), _uid,
                                    AttributeUtil.filterUid(replaceAttributes), operationOptions);

                    returnResource(request, handler, facade, uid);
                }
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (ConnectorException e) {
                handleError(request,e, handler);
            } catch (JsonValueException e) {
                handler.handleError(new BadRequestException(e.getMessage(), e));
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }

        private void returnResource(final Request request, final ResultHandler<Resource> handler,final  ConnectorFacade facade, final Uid uid) throws IOException, JsonCryptoException {
            OperationOptionsBuilder _getOOBuilder = new OperationOptionsBuilder();
            ConnectorObject co = null;
            if (objectClassInfoHelper.setAttributesToGet(_getOOBuilder, request
                    .getFieldFilters())) {
                try {
                    co =
                            facade.getObject(
                                    objectClassInfoHelper.getObjectClass(), uid,
                                    _getOOBuilder.build());
                } catch (Exception e) {
                    logger.error("Failed to read back the user", e);
                }
            }
            if (null != co) {
                handler.handleResult(objectClassInfoHelper.build(co, cryptoService));
            } else {
                JsonValue result = new JsonValue(new HashMap<String, Object>());
                result.put(ServerConstants.OBJECT_PROPERTY_ID, uid.getUidValue());
                if (null != uid.getRevision()) {
                    result.put(ServerConstants.OBJECT_PROPERTY_REV, uid
                            .getRevision());
                }

                handler.handleResult(new Resource(uid.getUidValue(), uid
                        .getRevision(), result));
            }
        }

        /*
         * public JsonValue update(Id id, String rev, JsonValue object,
         * JsonValue params) throws Exception { OperationHelper helper =
         * operationHelperBuilder.build(id.getObjectType(), params,
         * cryptoService); if (allowModification &&
         * helper.isOperationPermitted(UpdateApiOp.class)) { JsonValue newName =
         * object.get(ServerConstants.OBJECT_PROPERTY_ID); ConnectorObject
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

    private static final QueryFilterVisitor<Filter, ObjectClassInfoHelper> RESOURCE_FILTER =
            new QueryFilterVisitor<Filter, ObjectClassInfoHelper>() {

                @Override
                public Filter visitAndFilter(final ObjectClassInfoHelper helper,
                        List<QueryFilter> subFilters) {
                    final Iterator<QueryFilter> iterator = subFilters.iterator();
                    if (iterator.hasNext()) {
                        final QueryFilter left = iterator.next();
                        return buildAnd(helper, left, iterator);
                    } else {
                        return new Filter() {
                            @Override
                            public boolean accept(ConnectorObject obj) {
                                return true;
                            }
                        };
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
                        final QueryFilter left = iterator.next();
                        return buildOr(helper, left, iterator);
                    } else {
                        return new Filter() {
                            @Override
                            public boolean accept(ConnectorObject obj) {
                                return true;
                            }
                        };
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
                        @Override
                        public boolean accept(ConnectorObject obj) {
                            return value;
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
}
