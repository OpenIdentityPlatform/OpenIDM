/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 *
 * $Id$
 */

package org.forgerock.openidm.provisioner.openicf.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
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
import org.forgerock.openidm.provisioner.openicf.impl.script.ConnectorScript;
import org.forgerock.openidm.sync.SynchronizationListener;
import org.identityconnectors.framework.api.APIConfiguration;
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
import org.osgi.service.component.ComponentConstants;
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
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = OpenICFProvisionerService.PID, policy = ConfigurationPolicy.REQUIRE,
        description = "OpenIDM System Object Set Service", immediate = true)
@Service(value = {ProvisionerService.class})
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM System Object Set Service")
})
public class OpenICFProvisionerService implements ProvisionerService {

    //Public Constants
    public static final String PID = "org.forgerock.openidm.provisioner.openicf";

    private static final Logger logger = LoggerFactory.getLogger(OpenICFProvisionerService.class);

    private ComponentContext context = null;
    private SimpleSystemIdentifier systemIdentifier = null;
    private OperationHelperBuilder operationHelperBuilder = null;
    private boolean allowModification = true;
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * ConnectorInfoProvider service.
     */
    @Reference
    private ConnectorInfoProvider connectorInfoProvider = null;


    /**
     * Cryptographic service.
     */
    @Reference
    protected CryptoService cryptoService = null;


    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
        ConnectorInfo connectorInfo = null;
        JsonValue jsonConfiguration = null;
        ConnectorReference connectorReference = null;
        try {
            jsonConfiguration = (new JSONEnhancedConfig()).getConfigurationAsJson(context);
            connectorReference = ConnectorUtil.getConnectorReference(jsonConfiguration);
            connectorInfo = connectorInfoProvider.findConnectorInfo(connectorReference);
        } catch (Exception e) {
            logger.error("ERROR - Invalid Configuration and/or ConnectorReference", e);
            throw new ComponentException("Invalid Configuration and/or ConnectorReference", e);
        }
        if (null == connectorInfo) {
            logger.error("ERROR - ConnectorInfo can not be retrieved for {}", connectorReference);
            throw new ComponentException("ConnectorInfo can not be retrieved for " + connectorReference);
        }
        logger.info("OK - ConnectorInfo was found.");
        try {
            systemIdentifier = new SimpleSystemIdentifier(jsonConfiguration);
            operationHelperBuilder = new OperationHelperBuilder(systemIdentifier.getName(), jsonConfiguration, connectorInfo.createDefaultAPIConfiguration());
            allowModification = !jsonConfiguration.get("readOnly").defaultTo(false).asBoolean();
        } catch (Exception e) {
            logger.error("ERROR - Invalid Configuration", e);
            throw new ComponentException("Invalid Configuration, service can not be started", e);
        }
        logger.info("OK - Configuration accepted.");
        try {
            ConnectorFacade facade = getConnectorFacade(operationHelperBuilder.getRuntimeAPIConfiguration());
            if (facade.getSupportedOperations().contains(TestApiOp.class)) {
                try {
                    facade.test();
                    logger.debug("OK - Test of {} succeeded!", systemIdentifier);
                } catch (Exception e) {
                    logger.warn("Test of {} failed when service was activated! Remote system may be unavailable or the It can be configuration problem.", systemIdentifier, e);
                }
            } else {
                logger.debug("Test is not supported on {}", connectorReference);
            }
        } catch (Throwable e) {
            //TODO Do we need this catch?
            logger.error("ERROR - Test of {} failed.", systemIdentifier, e);
            throw new ComponentException("Connector test failed.", e);
        }
        logger.info("OK - OpenICFProvisionerService component with '{}' is activated.", systemIdentifier);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        logger.info("Component {} is deactivated.", systemIdentifier);
        this.context = null;
        this.systemIdentifier = null;
        this.operationHelperBuilder = null;
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

            //TODO component.id and component.name can cause problems in JsonPath
            jv.put(ComponentConstants.COMPONENT_ID, context.getProperties().get(ComponentConstants.COMPONENT_ID));
            jv.put(ComponentConstants.COMPONENT_NAME, context.getProperties().get(ComponentConstants.COMPONENT_NAME));
            ConnectorFacade connectorFacade = getConnectorFacade(operationHelperBuilder.getRuntimeAPIConfiguration());
            connectorFacade.test();
            jv.put("ok", true);
        } catch (Exception e) {
            result.put("error", e.getMessage());
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
                        return create(id, value.required(), params);
                    case read:
                        return read(id, params);
                    case update:
                        return update(id, rev, value.required(), params);
                    case delete:
                        return delete(id, rev, params);
                    case query:
// FIXME: According to the JSON resource specification (now published), query parameters are
// required. There is a unit test that attempts to query all merely by executing query
// without any parameters. As a result, the commented-out line below—which conforms to the
// spec—breaks during unit testing.
//                    return query(id, params.required());
                        return query(id, params);
                    case action:
                        return action(id, value, params.required());
                    default:
                        throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
                }
            } catch (AlreadyExistsException e) {
                logger.error("System object {} already exists", id, e);
                throw new JsonResourceException(JsonResourceException.CONFLICT, e.getClass().getSimpleName(), e);
            } catch (ConfigurationException e) {
                logger.error("Operation {} failed with ConfigurationException on system object: {}", new Object[]{METHOD, id}, e);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            } catch (ConnectionBrokenException e) {
                logger.error("Operation {} failed with ConnectionBrokenException on system object: {}", new Object[]{METHOD, id}, e);
                throw new JsonResourceException(JsonResourceException.UNAVAILABLE, e.getClass().getSimpleName(), e);
            } catch (ConnectionFailedException e) {
                logger.error("Connection failed during operation {} on system object: {}", new Object[]{METHOD, id}, e);
                throw new JsonResourceException(JsonResourceException.UNAVAILABLE, e.getClass().getSimpleName(), e);
            } catch (ConnectorIOException e) {
                logger.error("Operation {} failed with ConnectorIOException on system object: {}", new Object[]{METHOD, id}, e);
                throw new JsonResourceException(JsonResourceException.UNAVAILABLE, e.getClass().getSimpleName(), e);
            } catch (OperationTimeoutException e) {
                logger.error("Operation {} Timeout on system object: {}", new Object[]{METHOD, id}, e);
                throw new JsonResourceException(JsonResourceException.UNAVAILABLE, e.getClass().getSimpleName(), e);
            } catch (PasswordExpiredException e) {
                logger.error("Operation {} failed with PasswordExpiredException on system object: {}", new Object[]{METHOD, id}, e);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            } catch (InvalidPasswordException e) {
                logger.error("Invalid password has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            } catch (UnknownUidException e) {
                logger.error("Operation {} failed with UnknownUidException on system object: {}", new Object[]{METHOD, id}, e);
                throw new JsonResourceException(JsonResourceException.NOT_FOUND, e.getClass().getSimpleName(), e);
            } catch (InvalidCredentialException e) {
                logger.error("Invalid credential has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            } catch (PermissionDeniedException e) {
                logger.error("Permission was denied on {} operation for system object: {}", new Object[]{METHOD, id}, e);
                throw new JsonResourceException(JsonResourceException.FORBIDDEN, e.getClass().getSimpleName(), e);
            } catch (ConnectorSecurityException e) {
                logger.error("Operation {} failed with ConnectorSecurityException on system object: {}", new Object[]{METHOD, id}, e);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            } catch (ConnectorException e) {
                logger.error("Operation {} failed with ConnectorException on system object: {}", new Object[]{METHOD, id}, e);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            } catch (JsonResourceException e) {
                // rethrow the the expected JsonResourceException
                throw e;
            } catch (Exception e) {
                logger.error("Operation {} failed with Exception on system object: {}", new Object[]{METHOD, id}, e);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e.getClass().getSimpleName(), e);
            }
        } catch (JsonValueException jve) {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST, jve);
        }
    }

    public JsonValue create(Id id, JsonValue object, JsonValue params) throws Exception {
        OperationHelper helper = operationHelperBuilder.build(id.getObjectType(), params, cryptoService);
        if (allowModification && helper.isOperationPermitted(CreateApiOp.class)) {
            ConnectorObject connectorObject = helper.build(CreateApiOp.class, object);
            OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(CreateApiOp.class, connectorObject, params);
            Uid uid = getConnectorFacade(helper.getRuntimeAPIConfiguration()).create(connectorObject.getObjectClass(), AttributeUtil.filterUid(connectorObject.getAttributes()), operationOptionsBuilder.build());
            helper.resetUid(uid, object);
            return object;
        } else {
            logger.debug("Operation create of {} is not permitted", id);
        }
        return null;
    }

    public JsonValue read(Id id, JsonValue params) throws Exception {
        OperationHelper helper = operationHelperBuilder.build(id.getObjectType(), params, cryptoService);
        ConnectorFacade facade = getConnectorFacade(helper.getRuntimeAPIConfiguration());
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
            Object newName = object.get(ServerConstants.OBJECT_PROPERTY_ID);
            ConnectorObject connectorObject = null;
            Set<Attribute> attributeSet = null;

            //TODO support case sensitive and insensitive rename detection!
            if (newName instanceof String && !id.getLocalId().equals(newName)) {
                //This is a rename
                connectorObject = helper.build(UpdateApiOp.class, (String) newName, object);
                attributeSet = AttributeUtil.filterUid(connectorObject.getAttributes());
            } else {
                connectorObject = helper.build(UpdateApiOp.class, id.getLocalId(), object);
                attributeSet = new HashSet<Attribute>();
                for (Attribute attribute : connectorObject.getAttributes()) {
                    if (attribute.is(Name.NAME) || attribute.is(Uid.NAME)) {
                        continue;
                    }
                    attributeSet.add(attribute);
                }
            }
            OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(UpdateApiOp.class, connectorObject, params);
            Uid uid = getConnectorFacade(helper.getRuntimeAPIConfiguration()).update(connectorObject.getObjectClass(), connectorObject.getUid(), attributeSet, operationOptionsBuilder.build());
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
            getConnectorFacade(helper.getRuntimeAPIConfiguration()).delete(helper.getObjectClass(), new Uid(id.getLocalId()), operationOptionsBuilder.build());
        } else {
            logger.debug("Operation DELETE of {} is not permitted", id);
        }
        return null;
    }


    public JsonValue query(Id id, JsonValue params) throws Exception {
        OperationHelper helper = operationHelperBuilder.build(id.getObjectType(), params, cryptoService);
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        if (helper.isOperationPermitted(SearchApiOp.class)) {
            OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(SearchApiOp.class, null, null);
            Filter filter = null;
            if (null != params) {
                Map<String, Object> query = params.get("query").asMap();
                String queryId = params.get("_query-id").asString();
                if (query != null) {
                    filter = helper.build(query, params.get("params").asMap());
                } else if (queryId != null) {
                    if (queryId.equals("query-all-ids")) {
                        // TODO: optimize query for ids, for now default to query all
                        operationOptionsBuilder.setAttributesToGet(Uid.NAME);
                    } else {
                        // Unknown query id
                        throw new JsonResourceException(JsonResourceException.BAD_REQUEST, "Unknown query id: " + queryId);
                    }
                } else {
                    // Neither a query expression or query id defined,
                    // default to query all
                }
            }
            getConnectorFacade(helper.getRuntimeAPIConfiguration()).search(helper.getObjectClass(), filter, helper.getResultsHandler(), operationOptionsBuilder.build());
            result.put("result", helper.getQueryResult());
        } else {
            logger.debug("Operation QUERY of {} is not permitted", id);
        }
        return result;
    }


    public JsonValue action(Id id, JsonValue entity, JsonValue params) throws JsonResourceException {
        OperationHelper helper = operationHelperBuilder.build(id.getObjectType(), params, cryptoService);
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        ConnectorScript script = new ConnectorScript(params);
        if (helper.isOperationPermitted(script.getAPIOperation())) {
            JsonValue vpo = params.get(ConnectorScript.SCRIPT_VARIABLE_PREFIX);
            String variablePrefix = null;
            if (!vpo.isNull() && vpo.isString()) {
                variablePrefix = vpo.asString();
            }

            for (Map.Entry<String, Object> entry : params.asMap().entrySet()) {
                if (entry.getKey().startsWith("_")) {
                    continue;
                }
                Object value = entry.getValue();
                Object newValue = value;
                if (null != value) {
                    if (value instanceof Collection) {
                        newValue = Array.newInstance(Object.class, ((Collection) value).size());
                        int i = 0;
                        for (Object v : (Collection) value) {
                            if (null == v || FrameworkUtil.isSupportedAttributeType(v.getClass())) {
                                Array.set(newValue, i, v);
                            } else {
                                //Serializable may not be acceptable
                                Array.set(newValue, i, v instanceof Serializable ? v : v.toString());
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

                if (null != variablePrefix) {
                    script.getScriptContextBuilder().addScriptArgument(variablePrefix + entry.getKey(), newValue);
                } else {
                    script.getScriptContextBuilder().addScriptArgument(entry.getKey(), newValue);
                }
            }
            script.getScriptContextBuilder().addScriptArgument("openidm_id", id.toString());

            Object scriptResult = null;
            ConnectorFacade facade = getConnectorFacade(helper.getRuntimeAPIConfiguration());
            ScriptContext scriptContext = script.getScriptContextBuilder().build();
            OperationOptions oo = script.getOperationOptionsBuilder().build();
            try {
                if (ConnectorScript.ExecutionMode.CONNECTOR.equals(script.getExecMode())) {
                    scriptResult = facade.runScriptOnConnector(scriptContext, oo);
                } else {
                    scriptResult = facade.runScriptOnResource(scriptContext, oo);
                }
            } catch (Throwable t) {
                logger.error("Script execution error.", t);
                result.put("error", t.getMessage());
            }
            result.put("result", ConnectorUtil.coercedTypeCasting(scriptResult, Object.class));

        } else {
            logger.debug("Operation ACTION of {} is not permitted", id);
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
    public JsonValue liveSynchronize(String objectType, JsonValue previousStage, final SynchronizationListener synchronizationListener) {
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
                ConnectorFacade connector = getConnectorFacade(helper.getRuntimeAPIConfiguration());
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
                        logger.debug("Execute sync(ObjectClass:{}, SyncToken:{})", new Object[]{helper.getObjectClass().getObjectClassValue(), token});
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
                                            synchronizationListener.onDelete(helper.resolveQualifiedId(syncDelta.getUid()).toString());
                                            break;
                                    }
                                    lastToken[0] = syncDelta.getToken();
                                } catch (Exception e) {
                                    failedRecord[0] = SerializerUtil.serializeXmlObject(syncDelta, true);
                                    logger.error("Failed synchronise {} object", syncDelta.getUid(), e);
                                    throw new ConnectorException("Failed synchronise " + syncDelta.getUid() + " object", e);
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
                        logger.error("Live synchronization of {} failed on {}", new Object[]{objectType, systemIdentifier.getName()}, t);
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
            logger.error("Failed to get OperationHelper", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            // catch helper.getOperationOptionsBuilder(
            logger.error("Failed to get OperationOptionsBuilder", e);
            throw new RuntimeException(e);
        }
        return stage;
    }

    ConnectorFacade getConnectorFacade(APIConfiguration runtimeAPIConfiguration) {
        ConnectorFacadeFactory connectorFacadeFactory = ConnectorFacadeFactory.getInstance();
        return connectorFacadeFactory.newInstance(runtimeAPIConfiguration);
    }

    private void traceObject(SimpleJsonResource.Method action, Id id, JsonValue source) {
        if (logger.isTraceEnabled()) {
            if (null != source) {
                try {
                    StringWriter writer = new StringWriter();
                    mapper.writeValue(writer, source.getObject());
                    logger.info("Action: {}, Id: {}, Object: {}", new Object[]{action, id, writer});
                } catch (IOException e) {
                    //Don't care
                }
            }
        }
    }
}
