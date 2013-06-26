/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All Rights Reserved
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


import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.openidm.audit.util.ActivityLog;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.provisioner.Id;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.SystemIdentifier;
import org.forgerock.openidm.provisioner.openicf.ConnectorInfoProvider;
import org.forgerock.openidm.provisioner.openicf.ConnectorReference;
import org.forgerock.openidm.provisioner.openicf.OperationHelper;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.forgerock.openidm.provisioner.openicf.internal.SystemAction;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.sync.SynchronizationListener;
import org.identityconnectors.common.event.ConnectorEvent;
import org.identityconnectors.common.event.ConnectorEventHandler;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.operations.*;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.serializer.SerializerUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.*;

/**
 * The OpenICFProvisionerService is the implementation of {@link CollectionResourceProvider} interface
 * with <a href="http://openicf.forgerock.org">OpenICF</a>.
 * <p/>
 *
 * @author Laszlo Hordos
 */
@Component(name = OpenICFProvisionerService.PID, policy = ConfigurationPolicy.REQUIRE,
        description = "OpenIDM System Object Set Service", immediate = true)
@Service(value = {ProvisionerService.class})
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM System Object Set Service")
})
public class OpenICFProvisionerService implements ProvisionerService, ConnectorEventHandler {

    //Public Constants
    public static final String PID = "org.forgerock.openidm.provisioner.openicf";

    private static final Logger logger = LoggerFactory.getLogger(OpenICFProvisionerService.class);

    // Monitoring event name prefix
    private static final String EVENT_PREFIX = "openidm/internal/system/";

    private SimpleSystemIdentifier systemIdentifier = null;
    private OperationHelperBuilder operationHelperBuilder = null;
    private boolean allowModification = true;
    private ConnectorFacade connectorFacade = null;
    private boolean serviceAvailable = false;
    private JsonValue jsonConfiguration = null;
    private ConnectorReference connectorReference = null;
    private Map<String, SystemAction> systemActions = new HashMap<String, SystemAction>();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * ConnectorInfoProvider service.
     */
    @Reference
    private ConnectorInfoProvider connectorInfoProvider = null;

    @Reference(referenceInterface = JsonResource.class,
            target = "(service.pid=org.forgerock.openidm.router)")
    private JsonResource router;

    /**
     * Cryptographic service.
     */
    @Reference
    protected CryptoService cryptoService = null;


    @Activate
    protected void activate(ComponentContext context) {
        try {
            jsonConfiguration = JSONEnhancedConfig.newInstance().getConfigurationAsJson(context);
            systemIdentifier = new SimpleSystemIdentifier(jsonConfiguration);
            allowModification = !jsonConfiguration.get("readOnly").defaultTo(false).asBoolean();
            if (!allowModification) {
                logger.debug("OpenICF Provisioner Service {} is running in read-only mode", systemIdentifier);
            }
            connectorReference = ConnectorUtil.getConnectorReference(jsonConfiguration);
        } catch (Exception e) {
            logger.error("OpenICF Provisioner Service configuration has errors", e);
            throw new ComponentException("OpenICF Provisioner Service configuration has errors", e);
        }

        ConnectorInfo connectorInfo = connectorInfoProvider.findConnectorInfo(connectorReference);
        if (null != connectorInfo) {
            logger.info("OpenICF ConnectorInfo of {} was found.", connectorReference);
            init(connectorInfo);
        } else if (connectorReference.getConnectorLocation().equals(ConnectorReference.ConnectorLocation.LOCAL)) {
            logger.error("OpenICF ConnectorInfo can not be loaded for {} from #LOCAL", connectorReference);
            throw new ComponentException("OpenICF ConnectorInfo can not be retrieved for " + connectorReference);
        } else {
            /*
            This should never happen because the configuration has to be encrypted and the encryption
            requires the ConnectorInfo so it's a safe assumption. If this block is executed then
            there was some change in the initialisation mechanism.
             */
            logger.info("OpenICF ConnectorInfo for {} is not available yet.", connectorReference);
        }
        if (!connectorReference.getConnectorLocation().equals(ConnectorReference.ConnectorLocation.LOCAL)) {
            connectorInfoProvider.addConnectorEventHandler(connectorReference, this);
        }
        if (jsonConfiguration.isDefined("systemActions")) {
            for (JsonValue actionValue : jsonConfiguration.get("systemActions").expect(List.class)) {
                SystemAction action = new SystemAction(actionValue);
                systemActions.put(action.getName(), action);
            }
        }
        logger.info("OpenICF Provisioner Service component {} is activated{}", systemIdentifier,
                (serviceAvailable ? "." : " although the service is not available yet."));
    }

    private void init(ConnectorInfo connectorInfo) {
        try {
            operationHelperBuilder = new OperationHelperBuilder(systemIdentifier.getName(), jsonConfiguration,
                    connectorInfo.createDefaultAPIConfiguration());
        } catch (Exception e) {
            logger.error("OpenICF connector configuration of {} has errors.", systemIdentifier, e);
            throw new ComponentException(
                    "OpenICF connector configuration has errors and the service can not be initiated.", e);
        }
        logger.debug("OpenICF connector configuration has no errors.");
        ConnectorFacade facade = getConnectorFacade();
        if (null != facade && facade.getSupportedOperations().contains(TestApiOp.class)) {
            try {
                facade.test();
                logger.debug("OpenICF connector test of {} succeeded!", systemIdentifier);
                serviceAvailable = true;
            } catch (Throwable e) {
                logger.error("OpenICF connector test of {} failed!", systemIdentifier, e);
                //throw new ComponentException("OpenICF connector test failed.", e);
            }
        } else if (null == facade) {
            logger.warn("OpenICF ConnectorFacade of {} is not available", connectorReference);
        } else {
            serviceAvailable = true;
            logger.debug("OpenICF connector of {} does not support test.", connectorReference);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        connectorInfoProvider.deleteConnectorEventHandler(this);
        serviceAvailable = true;
        systemIdentifier = null;
        operationHelperBuilder = null;
        connectorFacade = null;
        logger.info("OpenICF Provisioner Service component {} is deactivated.", systemIdentifier);
    }

    public void handleEvent(ConnectorEvent connectorEvent) {
        logger.debug("ConnectorEvent received. Topic: {}, Source: {}", connectorEvent.getTopic(),
                connectorEvent.getSource());
        if (ConnectorEvent.CONNECTOR_REGISTERED.equals(connectorEvent.getTopic())) {
            ConnectorInfo connectorInfo = connectorInfoProvider.findConnectorInfo(connectorReference);
            if (null != connectorInfo) {
                logger.info("OpenICF ConnectorInfo of {} was found.", connectorReference);
                try {
                    init(connectorInfo);
                    logger.info("OpenICF Provisioner Service component {} is activated{}", systemIdentifier,
                            (serviceAvailable ? "." : " although the service is not available yet."));
                } catch (Throwable t) {

                }
            } else {
                logger.error("OpenICF ConnectorInfo for {} is not available.", connectorReference);
            }
        } else if (ConnectorEvent.CONNECTOR_UNREGISTERING.equals(connectorEvent.getTopic())) {
            serviceAvailable = false;
            logger.info("OpenICF Provisioner Service component {} is deactivated.", systemIdentifier);
            connectorFacade = null;
            operationHelperBuilder = null;
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

    /**
     * Gets a brief stats report about the current status of this service instance.
     * </p/>
     * {@code {
     * "name" : "LDAP",
     * "component.id" : "1",
     * "component.name" : "org.forgerock.openidm.provisioner.openicf.ProvisionerService",
     * "ok" : true
     * }}
     *
     * @return
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        try {
            JsonValue jv = new JsonValue(result);
            jv.put("name", systemIdentifier.getName());
            ConnectorFacade connectorFacade = getConnectorFacade();
            try {
                connectorFacade.test();
            } catch (UnsupportedOperationException e) {
                jv.put("reason", "TEST UnsupportedOperation");
            }
            jv.put("ok", true);
        } catch (Throwable e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    
    public Map<String, Object> testConfig(JsonValue config) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        JsonValue jv = new JsonValue(result);
        jv.add("name", systemIdentifier.getName());
        jv.add("ok", false);
        SimpleSystemIdentifier systemIdentifier = null;
        ConnectorReference connectorReference = null;
        try {
            systemIdentifier = new SimpleSystemIdentifier(config);
            connectorReference = ConnectorUtil.getConnectorReference(jsonConfiguration);
        } catch (JsonValueException e) {
            jv.add("error", "OpenICF Provisioner Service configuration has errors: " + e.getMessage());
            return result;
        }
        
        ConnectorInfo connectorInfo = connectorInfoProvider.findConnectorInfo(connectorReference);
        if (null != connectorInfo) {
            ConnectorFacade facade = null;
            try {
                OperationHelperBuilder ohb = new OperationHelperBuilder(systemIdentifier.getName(), config,
                        connectorInfo.createDefaultAPIConfiguration());
                ConnectorFacadeFactory connectorFacadeFactory = ConnectorFacadeFactory.getInstance();
                facade = connectorFacadeFactory.newInstance(ohb.getRuntimeAPIConfiguration());
            } catch (Exception e) {
                e.printStackTrace();
                jv.add("error", "OpenICF connector configuration has errors: " +  e.getMessage());
                return result;
            }
            
            if (null != facade && facade.getSupportedOperations().contains(TestApiOp.class)) {
                try {
                    facade.test();
                } catch (UnsupportedOperationException e) {
                    jv.put("reason", "TEST UnsupportedOperation");
                } catch (Throwable e) {
                    jv.put("error", e.getMessage());
                    return result;
                }
                jv.put("ok", true);
            } else if (null == facade) {
                jv.add("error", "OpenICF ConnectorFacade of " + connectorReference + " is not available");
            } else {
                jv.add("error", "OpenICF connector of " + connectorReference + " does not support test.");
            }
        } else if (connectorReference.getConnectorLocation().equals(ConnectorReference.ConnectorLocation.LOCAL)) {
            jv.add("error", "OpenICF ConnectorInfo can not be loaded for " + connectorReference + " from #LOCAL");
        } else {
            jv.add("error", "OpenICF ConnectorInfo for " + connectorReference + " is not available yet.");
        }
        return result;
    }

    /**
     * TODO: Description.
     * <p/>
     * This method catches any thrown {@code JsonValueException}, and rethrows it as a
     * {@link org.forgerock.json.resource.JsonResourceException#BAD_REQUEST}.
     */
    @Override
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        if (!serviceAvailable) {
           //TODO: better error message
           throw new JsonResourceException(JsonResourceException.UNAVAILABLE);
        }
        JsonValue before = null;
        JsonValue after = null;
        try {
            SimpleJsonResource.Method METHOD = request.get("method").required().asEnum(SimpleJsonResource.Method.class);
            Id id = new Id(request.get("id").required().asString());
            String rev = request.get("rev").asString();
            JsonValue value = request.get("value");
            JsonValue params = request.get("params");
            try {
                traceObject(METHOD, id, value);
                switch (METHOD) {
                    case create:
                        before = value;
                        after = create(id, value.required(), params);
                        ActivityLog.log(router, request, "message", id.toString(), before, after, Status.SUCCESS);
                        return after;
                    case read:
                        after = read(id, params);
                        ActivityLog.log(router, request, "message", id.toString(), before, after, Status.SUCCESS);
                        return after;
                    case update:
                        before = value;
                        after = update(id, rev, value.required(), params);
                        ActivityLog.log(router, request, "message", id.toString(), before, after, Status.SUCCESS);
                        return after;
                    case delete:
                        try {
                            before = read(id, params);
                        } catch (Exception e) {
                            logger.info("Operation read of {} failed before delete", id, e);
                        }
                        after = delete(id, rev, params);
                        ActivityLog.log(router, request, "message", id.toString(), before, after, Status.SUCCESS);
                        return after;
                    case query:
// FIXME: According to the JSON resource specification (now published), query parameters are
// required. There is a unit test that attempts to query all merely by executing query
// without any parameters. As a result, the commented-out line below—which conforms to the
// spec—breaks during unit testing.
//                    return query(id, params.required());
                        before = params;
                        after = query(id, params.required());
                        ActivityLog.log(router, request, "message", id.toString(), before, after, Status.SUCCESS);
                        return after;
                    case action:
                        before = new JsonValue(new HashMap());
                        before.put("value", value);
                        before.put("params", filterParamsToLog(params));
                        ActionId actionId = params.get(ServerConstants.ACTION_NAME).required().asEnum(ActionId.class);
                        after = action(id, actionId, value, params.required());
                        ActivityLog.log(router, request, "message", id.toString(), before, after, Status.SUCCESS);
                        return after;
                    default:
                        throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
                }
            } catch (AlreadyExistsException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("System object {} already exists", id, e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.CONFLICT, e.getClass().getSimpleName(), e);
            } catch (ConfigurationException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Operation {} failed with ConfigurationException on system object: {}",
                            new Object[]{METHOD, id}, e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            } catch (ConnectionBrokenException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Operation {} failed with ConnectionBrokenException on system object: {}",
                            new Object[]{METHOD, id}, e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.UNAVAILABLE, e.getClass().getSimpleName(), e);
            } catch (ConnectionFailedException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Connection failed during operation {} on system object: {}", new Object[]{METHOD, id},
                            e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.UNAVAILABLE, e.getClass().getSimpleName(), e);
            } catch (ConnectorIOException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Operation {} failed with ConnectorIOException on system object: {}",
                            new Object[]{METHOD, id}, e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.UNAVAILABLE, e.getClass().getSimpleName(), e);
            } catch (OperationTimeoutException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Operation {} Timeout on system object: {}", new Object[]{METHOD, id}, e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.UNAVAILABLE, e.getClass().getSimpleName(), e);
            } catch (PasswordExpiredException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Operation {} failed with PasswordExpiredException on system object: {}",
                            new Object[]{METHOD, id}, e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            } catch (InvalidPasswordException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Invalid password has been provided to operation {} for system object: {}",
                            new Object[]{METHOD, id}, e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            } catch (UnknownUidException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Operation {} failed with UnknownUidException on system object: {}",
                            new Object[]{METHOD, id}, e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.NOT_FOUND, e.getClass().getSimpleName(), e);
            } catch (InvalidCredentialException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Invalid credential has been provided to operation {} for system object: {}",
                            new Object[]{METHOD, id}, e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            } catch (PermissionDeniedException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Permission was denied on {} operation for system object: {}",
                            new Object[]{METHOD, id}, e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.FORBIDDEN, e.getClass().getSimpleName(), e);
            } catch (ConnectorSecurityException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Operation {} failed with ConnectorSecurityException on system object: {}",
                            new Object[]{METHOD, id}, e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            } catch (ConnectorException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Operation {} failed with ConnectorException on system object: {}",
                            new Object[]{METHOD, id}, e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            } catch (JsonResourceException e) {
                // rethrow the the expected JsonResourceException
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw e;
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Operation {} failed with Exception on system object: {}", new Object[]{METHOD, id},
                            e);
                }
                ActivityLog.log(router, request, "Operation " + METHOD.name() + " failed with " +
                        e.getClass().getSimpleName(), id.toString(), before, after, Status.FAILURE);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            }
        } catch (JsonValueException jve) {
            ActivityLog.log(router, request, "Bad Request", null, before, after, Status.FAILURE);
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST, jve);
        }
    }
    
    public JsonValue create(Id id, JsonValue object, JsonValue params) throws Exception {
        OperationHelper helper = operationHelperBuilder.build(id.getObjectType(), params, cryptoService);
        if (allowModification && helper.isOperationPermitted(CreateApiOp.class)) {
            ConnectorObject connectorObject = helper.build(CreateApiOp.class, object);
            OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(CreateApiOp.class, connectorObject, params);
            Uid uid = getConnectorFacade().create(connectorObject.getObjectClass(), AttributeUtil.filterUid(connectorObject.getAttributes()), operationOptionsBuilder.build());
            helper.resetUid(uid, object);
            return object;
        } else {
            logger.debug("Operation create of {} is not permitted", id);
        }
        return null;
    }

    public JsonValue read(Id id, JsonValue params) throws Exception {
        OperationHelper helper = operationHelperBuilder.build(id.getObjectType(), params, cryptoService);
        ConnectorFacade facade = getConnectorFacade();
        if (helper.isOperationPermitted(GetApiOp.class)) {
            OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(GetApiOp.class, null, params);
            ConnectorObject connectorObject = facade.getObject(helper.getObjectClass(), new Uid(id.getLocalId()), operationOptionsBuilder.build());
            if (null != connectorObject) {
                return helper.build(connectorObject);
            }
        } else {
            logger.debug("Operation read of {} is not permitted", id);
        }
        throw new JsonResourceException(JsonResourceException.NOT_FOUND, id.toString());
    }

    public JsonValue update(Id id, String rev, JsonValue object, JsonValue params) throws Exception {
        OperationHelper helper = operationHelperBuilder.build(id.getObjectType(), params, cryptoService);
        if (allowModification && helper.isOperationPermitted(UpdateApiOp.class)) {
            JsonValue newName = object.get(ServerConstants.OBJECT_PROPERTY_ID);
            ConnectorObject connectorObject = null;
            Set<Attribute> attributeSet = null;

            //TODO support case sensitive and insensitive rename detection!
            if (newName.isString() && !id.getLocalId().equals(Id.unescapeUid(newName.asString()))) {
                //This is a rename
                connectorObject = helper.build(UpdateApiOp.class, newName.asString(), object);
                attributeSet = AttributeUtil.filterUid(connectorObject.getAttributes());
            } else {
                connectorObject = helper.build(UpdateApiOp.class, id.getLocalId(), object);
                attributeSet = new HashSet<Attribute>();
                for (Attribute attribute : connectorObject.getAttributes()) {
                    if (attribute.is(Uid.NAME)) {
                        continue;
                    }
                    attributeSet.add(attribute);
                }
            }
            OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(UpdateApiOp.class, connectorObject, params);
            Uid uid = getConnectorFacade().update(connectorObject.getObjectClass(), connectorObject.getUid(), attributeSet, operationOptionsBuilder.build());
            helper.resetUid(uid, object);
            return object;
        } else {
            logger.debug("Operation update of {} is not permitted", id);
        }
        return null;
    }

    public JsonValue delete(Id id, String rev, JsonValue params) throws Exception {
        OperationHelper helper = operationHelperBuilder.build(id.getObjectType(), params, cryptoService);
        if (allowModification && helper.isOperationPermitted(DeleteApiOp.class)) {
            OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(DeleteApiOp.class, null, null);
            getConnectorFacade().delete(helper.getObjectClass(), new Uid(id.getLocalId()), operationOptionsBuilder.build());
        } else {
            logger.debug("Operation DELETE of {} is not permitted", id);
        }
        return null;
    }


    public JsonValue query(Id id, JsonValue params) throws Exception {
        OperationHelper helper = operationHelperBuilder.build(id.getObjectType(), params, cryptoService);
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        if (helper.isOperationPermitted(SearchApiOp.class)) {
            OperationOptionsBuilder operationOptionsBuilder = helper
                    .getOperationOptionsBuilder(SearchApiOp.class, null, null);
            JsonValue query = params.get("query");
            JsonValue queryId = params.get(QueryConstants.QUERY_ID);
            EventEntry measure = Publisher
                    .start(getQueryEventName(id, params, query.asMap(), queryId.asString()), null, id);
            try {
                Filter filter = null;
                if (!query.isNull()) {
                    filter = helper.build(query.asMap(), params.get("params").asMap());
                } else if (!queryId.isNull()) {
                    if (QueryConstants.QUERY_ALL_IDS.equals(queryId.asString())) {
                        // TODO: optimize query for ids, for now default to query all
                        operationOptionsBuilder.setAttributesToGet(Uid.NAME);
                    } else {
                        // Unknown query id
                        throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                                "Unknown query id: " + queryId);
                    }
                } else {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                            "Query request does not contain valid query");
                }
                getConnectorFacade().search(helper.getObjectClass(), filter, helper.getResultsHandler(),
                        operationOptionsBuilder.build());
                result.put("result", helper.getQueryResult());
                measure.setResult(result);
            } finally {
                measure.end();
            }
        } else {
            logger.debug("Operation QUERY of {} is not permitted", id);
        }
        return result;
    }

    /**
     * @return the smartevent Name for a given query
     */
    org.forgerock.openidm.smartevent.Name getQueryEventName(Id id, JsonValue params, Map<String, Object> query, String queryId) {
        String prefix = EVENT_PREFIX + id.getSystemName() + "/" + id.getObjectType() + "/query/";
        if (params == null) {
            return org.forgerock.openidm.smartevent.Name.get(prefix + "_default_query");
        } else if (query != null) {
            return org.forgerock.openidm.smartevent.Name.get(prefix + "_query_expression");
        } else {
            return org.forgerock.openidm.smartevent.Name.get(prefix + queryId);
        }
    }

    /** TODO: Description. */
    private enum ActionId {
        script, authenticate;
    }

    public JsonValue action(Id id, ActionId actionId, JsonValue entity, JsonValue params) throws Exception {
        JsonValue result = null;
        switch (actionId) {
            case script:
                SystemAction action = systemActions.get(params.get(SystemAction.SCRIPT_ID).required().asString());
                if (null != action) {
                    String systemType = connectorReference.getConnectorKey().getConnectorName();
                    List<ScriptContextBuilder> scriptContextBuilderList = action.getScriptContextBuilders(systemType);
                    if (null != scriptContextBuilderList) {
                        OperationHelper helper = operationHelperBuilder
                                .build(id.getObjectType(), params, cryptoService);
                        result = new JsonValue(new HashMap<String, Object>());

                        boolean onConnector = !"resource"
                                .equalsIgnoreCase(params.get(SystemAction.SCRIPT_EXECUTE_MODE).asString());

                        if (helper.isOperationPermitted(
                                onConnector ? ScriptOnConnectorApiOp.class : ScriptOnResourceApiOp.class)) {
                            JsonValue vpo = params.get(SystemAction.SCRIPT_VARIABLE_PREFIX);
                            String variablePrefix = null;
                            if (!vpo.isNull() && vpo.isString()) {
                                variablePrefix = vpo.asString();
                            }
                            List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>(
                                    scriptContextBuilderList.size());
                            result.put("actions", resultList);

                            for (ScriptContextBuilder contextBuilder : scriptContextBuilderList) {
                                boolean isShell = contextBuilder.getScriptLanguage().equalsIgnoreCase("Shell");
                                for (Map.Entry<String, Object> entry : params.asMap().entrySet()) {
                                    if (entry.getKey().startsWith("_")) {
                                        continue;
                                    }
                                    Object value = entry.getValue();
                                    Object newValue = value;
                                    if (isShell) {
                                        if ("password".equalsIgnoreCase(entry.getKey())) {
                                            if (value instanceof String) {
                                                newValue = new GuardedString(((String) value).toCharArray());
                                            } else {
                                                throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                                                        "Invalid type for password.");
                                            }
                                        }
                                        if ("username".equalsIgnoreCase(entry.getKey())) {
                                            if (value instanceof String == false) {
                                                throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                                                        "Invalid type for username.");
                                            }
                                        }
                                        if ("workingdir".equalsIgnoreCase(entry.getKey())) {
                                            if (value instanceof String == false) {
                                                throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                                                        "Invalid type for workingdir.");
                                            }
                                        }
                                        if ("timeout".equalsIgnoreCase(entry.getKey())) {
                                            if (value instanceof String == false && value instanceof Number == false ) {
                                                throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                                                        "Invalid type for timeout.");
                                            }
                                        }
                                        contextBuilder.addScriptArgument(entry.getKey(), newValue);
                                        continue;
                                    }

                                    if (null != value) {
                                        if (value instanceof Collection) {
                                            newValue = Array.newInstance(Object.class, ((Collection) value).size());
                                            int i = 0;
                                            for (Object v : (Collection) value) {
                                                if (null == v || FrameworkUtil.isSupportedAttributeType(v.getClass())) {
                                                    Array.set(newValue, i, v);
                                                } else {
                                                    //Serializable may not be acceptable
                                                    Array.set(newValue, i,
                                                            v instanceof Serializable ? v : v.toString());
                                                }
                                                i++;
                                            }

                                        } else if (value.getClass().isArray()) {
                                            //TODO implement the array support later
                                        } else if (!FrameworkUtil.isSupportedAttributeType(value.getClass())) {
                                            //Serializable may not be acceptable
                                            newValue = value instanceof Serializable ? value : value.toString();
                                        }
                                    }
                                    contextBuilder.addScriptArgument(entry.getKey(), newValue);
                                }
                                contextBuilder.addScriptArgument("openidm_id", id.toString());

                                //ScriptContext scriptContext = script.getScriptContextBuilder().build();
                                OperationOptionsBuilder operationOptionsBuilder = new OperationOptionsBuilder();

                                //It's necessary to keep the backward compatibility with Waveset IDM
                                if (null != variablePrefix && isShell) {
                                    operationOptionsBuilder.setOption("variablePrefix", variablePrefix);
                                }

                                Map<String, Object> actionResult = new HashMap<String, Object>(2);
                                try {
                                    Object scriptResult = null;
                                    if (onConnector) {
                                        scriptResult = getConnectorFacade().runScriptOnConnector(contextBuilder.build(),
                                                operationOptionsBuilder.build());
                                    } else {
                                        scriptResult = getConnectorFacade().runScriptOnResource(contextBuilder.build(),
                                                operationOptionsBuilder.build());
                                    }
                                    actionResult.put("result",
                                            ConnectorUtil.coercedTypeCasting(scriptResult, Object.class));
                                } catch (Throwable t) {
                                    if (logger.isDebugEnabled()) {
                                        logger.error("Script execution error.", t);
                                    }
                                    actionResult.put("error", t.getMessage());
                                }
                                resultList.add(actionResult);
                            }
                        } else {
                            logger.debug("Operation ACTION of {} is not permitted", id);
                        }
                        return result;
                    } else {
                        return null;
                    }
                } else {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                            "SystemAction not found: " + params.get("name").getObject());
                }
            case authenticate:
                OperationHelper helper = operationHelperBuilder.build(id.getObjectType(), params, cryptoService);
                if (helper.isOperationPermitted(AuthenticationApiOp.class)) {
                    OperationOptionsBuilder operationOptionsBuilder = helper
                            .getOperationOptionsBuilder(AuthenticationApiOp.class, null, null);

                    String username = params.get("username").required().asString();
                    String password = params.get("password").required().asString();

                    Uid uid = getConnectorFacade()
                            .authenticate(helper.getObjectClass(), username, new GuardedString(password.toCharArray()),
                                    operationOptionsBuilder.build());
                    result = new JsonValue(new HashMap<String, Object>());
                    helper.resetUid(uid, result);
                } else {
                    logger.debug("Operation AUTHENTICATE of {} is not permitted", id);
                }
        }
        return result;
    }

    /**
     * This instance and this method can not be scheduled. The call MUST go through the
     * {@code org.forgerock.openidm.provisioner}
     * <p/>
     * Invoked by the scheduler when the scheduler triggers.
     * <p/>
     * Synchronization object:
     * {@code
     * {
     * "connectorData" :
     * {
     * "syncToken" : "1305555929000",
     * "nativeType" : "JAVA_TYPE_LONG"
     * },
     * "synchronizationStatus" :
     * {
     * "errorStatus" : null,
     * "lastKnownServer" : "localServer",
     * "lastModDate" : "2011-05-16T14:47:58.587Z",
     * "lastModNum" : 668,
     * "lastPollDate" : "2011-05-16T14:47:52.875Z",
     * "lastStartTime" : "2011-05-16T14:29:07.863Z",
     * "progressMessage" : "SUCCEEDED"
     * }
     * }}
     * <p/>
     * {@inheritDoc}
     * Synchronise the changes from the end system for the given {@code objectType}.
     * <p/>
     * OpenIDM takes active role in the synchronization process by asking the end system to get all changed object.
     * Not all system is capable to fulfill this kind of request but if the end system is capable then the implementation
     * send each changes to the {@link org.forgerock.openidm.sync.SynchronizationListener} and when it finished it return a new <b>stage</b> object.
     * <p/>
     * The {@code previousStage} object is the previously returned value of this method.
     *
     * @param previousStage           The previously returned object. If null then it's the first execution.
     * @param synchronizationListener The listener to send the changes to.
     * @return The new updated stage object. This will be the {@code previousStage} at next call.
     * @throws IllegalArgumentException      if the value of {@code connectorData} can not be converted to {@link SyncToken}.
     * @throws UnsupportedOperationException if the {@link SyncApiOp} operation is not implemented in connector.
     * @throws org.forgerock.json.fluent.JsonValueException
     *                                       if the  {@code previousStage} is not Map.
     * @see {@link ConnectorUtil#convertToSyncToken(org.forgerock.json.fluent.JsonValue)} or any exception happed inside the connector.
     */
    public JsonValue liveSynchronize(String objectType, JsonValue previousStage, final SynchronizationListener synchronizationListener) 
            throws JsonResourceException {
        if (!serviceAvailable) return previousStage;
        JsonValue stage = previousStage != null ? previousStage.copy() : new JsonValue(new LinkedHashMap<String, Object>());
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
                    OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(SyncApiOp.class, null, previousStage);
                    try {
                        logger.debug("Execute sync(ObjectClass:{}, SyncToken:{})",
                                new Object[]{helper.getObjectClass().getObjectClassValue(), token});
                        operation.sync(helper.getObjectClass(), token, new SyncResultsHandler() {
                            /**
                             * Called to handle a delta in the stream. The Connector framework will call
                             * this method multiple times, once for each result.
                             * Although this method is callback, the framework will invoke it synchronously.
                             * Thus, the framework guarantees that once an application's call to
                             * {@link org.identityconnectors.framework.api.operations.SyncApiOp#sync(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.SyncToken, org.identityconnectors.framework.common.objects.SyncResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)}  SyncApiOp#sync()} returns,
                             * the framework will no longer call this method
                             * to handle results from that <code>sync()</code> operation.
                             *
                             * @param syncDelta The change
                             * @return True iff the application wants to continue processing more
                             *         results.
                             * @throws RuntimeException If the application encounters an exception. This will stop
                             *                          iteration and the exception will propagate to
                             *                          the application.
                             */
                            public boolean handle(SyncDelta syncDelta) {
                                try {
                                    switch (syncDelta.getDeltaType()) {
                                        case CREATE_OR_UPDATE:
                                            JsonValue deltaObject = helper.build(syncDelta.getObject());
                                            if (null != syncDelta.getPreviousUid()) {
                                                deltaObject.put("_previous-id", Id.escapeUid(syncDelta.getPreviousUid().getUidValue()));
                                            }
                                            synchronizationListener.onUpdate(helper.resolveQualifiedId(syncDelta.getUid()).toString(), null, new JsonValue(deltaObject));
                                            break;
                                        case DELETE:
                                            synchronizationListener.onDelete(helper.resolveQualifiedId(syncDelta.getUid()).toString(), null);
                                            break;
                                    }
                                    lastToken[0] = syncDelta.getToken();
                                } catch (Exception e) {
                                    failedRecord[0] = SerializerUtil.serializeXmlObject(syncDelta, true);
                                    if (logger.isDebugEnabled()) {
                                        logger.error("Failed synchronise {} object", syncDelta.getUid(), e);
                                    }
                                    throw new ConnectorException("Failed synchronise " + syncDelta.getUid() + " object. "
                                            + e.getMessage(), e);
                                }
                                return true;
                            }
                        }, operationOptionsBuilder.build());
                    } catch (Throwable t) {
                        Map<String, Object> lastException = new LinkedHashMap<String, Object>(2);
                        lastException.put("throwable", t.getMessage());
                        if (null != failedRecord[0]) {
                            lastException.put("syncDelta", failedRecord[0]);
                        }
                        stage.put("lastException", lastException);
                        if (logger.isDebugEnabled()) {
                            logger.error("Live synchronization of {} failed on {}",
                                    new Object[]{objectType, systemIdentifier.getName()}, t);
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
        } catch (JsonResourceException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to get OperationHelper", e);
            }
            throw e;
        } catch (Exception e) {
            // catch helper.getOperationOptionsBuilder(
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to get OperationOptionsBuilder", e);
            }
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, 
                    "Failed to get OperationOptionsBuilder: " + e.getMessage(), e);
        }
        return stage;
    }

    ConnectorFacade getConnectorFacade() {
        if (null == connectorFacade) {
            ConnectorFacadeFactory connectorFacadeFactory = ConnectorFacadeFactory.getInstance();
            connectorFacade = connectorFacadeFactory.newInstance(operationHelperBuilder.getRuntimeAPIConfiguration());
        }
        return connectorFacade;
    }
    
    private JsonValue filterParamsToLog(JsonValue params) {
        JsonValue result = params.copy();
        result.remove("password");
        return result;
    }

    private void traceObject(SimpleJsonResource.Method action, Id id, JsonValue source) {
        if (logger.isTraceEnabled()) {
            if (null != source) {
                try {
                    StringWriter writer = new StringWriter();
                    MAPPER.writeValue(writer, source.getObject());
                    logger.info("Action: {}, Id: {}, Object: {}", new Object[]{action, id, writer});
                } catch (IOException e) {
                    //Don't care
                }
            }
        }
    }
}
