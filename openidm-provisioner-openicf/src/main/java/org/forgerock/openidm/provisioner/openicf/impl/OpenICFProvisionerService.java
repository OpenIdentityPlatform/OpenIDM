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
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.context.InvokeContext;
import org.forgerock.openidm.objset.*;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.SystemIdentifier;
import org.forgerock.openidm.provisioner.openicf.ConnectorInfoProvider;
import org.forgerock.openidm.provisioner.openicf.ConnectorReference;
import org.forgerock.openidm.provisioner.openicf.OperationHelper;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.forgerock.openidm.provisioner.openicf.commons.Id;
import org.forgerock.openidm.provisioner.openicf.script.ConnectorScript;
import org.forgerock.openidm.sync.SynchronizationListener;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.operations.*;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.serializer.SerializerUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = "org.forgerock.openidm.provisioner.openicf", policy = ConfigurationPolicy.REQUIRE,
        description = "OpenIDM System Object Set Service", immediate = true)
@Service(value = {ProvisionerService.class})
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = "ForgeRock AS"),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM System Object Set Service")
})
public class OpenICFProvisionerService implements ProvisionerService {
    private final static Logger TRACE = LoggerFactory.getLogger(OpenICFProvisionerService.class);
    private ComponentContext context = null;
    private SimpleSystemIdentifier systemIdentifier = null;
    private OperationHelperBuilder operationHelperBuilder = null;


    @Reference(name = "ConnectorInfoProviderServiceReference", referenceInterface = ConnectorInfoProvider.class, bind = "bind", unbind = "unbind", cardinality = ReferenceCardinality.MANDATORY_UNARY, policy = ReferencePolicy.STATIC)
    private ConnectorInfoProvider connectorInfoProvider = null;


    protected void bind(ConnectorInfoProvider connectorInfoProvider) {
        TRACE.info("ConnectorInfoProvider is bound.");
        this.connectorInfoProvider = connectorInfoProvider;

    }

    protected void unbind(ConnectorInfoProvider connectorInfoProvider) {
        this.connectorInfoProvider = null;
        TRACE.info("ConnectorInfoProvider is unbound.");
    }

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
        ConnectorInfo connectorInfo = null;
        JsonNode jsonConfiguration = null;
        ConnectorReference connectorReference = null;
        try {
            jsonConfiguration = getConfiguration(context);
            connectorReference = ConnectorUtil.getConnectorReference(jsonConfiguration);
            connectorInfo = connectorInfoProvider.findConnectorInfo(connectorReference);
        } catch (Exception e) {
            TRACE.error("Invalid configuration", e);
            throw new ComponentException("Invalid configuration, connectorInfo can not be retrieved.", e);
        }
        if (null == connectorInfo) {
            throw new ComponentException("ConnectorInfo can not retrieved for " + connectorReference);
        }
        try {
            systemIdentifier = new SimpleSystemIdentifier(jsonConfiguration);
            operationHelperBuilder = new OperationHelperBuilder(systemIdentifier.getName(), jsonConfiguration, connectorInfo.createDefaultAPIConfiguration());
        } catch (Exception e) {
            TRACE.error("Invalid configuration", e);
            throw new ComponentException("Invalid configuration, service can not be started.", e);
        }

        try {
            ConnectorFacade facade = getConnectorFacade(operationHelperBuilder.getRuntimeAPIConfiguration());
            if (facade.getSupportedOperations().contains(TestApiOp.class)) {
                facade.test();
                TRACE.debug("Test of {} succeeded!", systemIdentifier);
            } else {
                TRACE.debug("Test is not supported.");
            }
        } catch (Exception e) {
            TRACE.error("Test of {} failed.", systemIdentifier, e);
            throw new ComponentException("Connector test failed.", e);
        }

        TRACE.info("OpenICFProvisionerService component with {} is activated.", systemIdentifier);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        TRACE.info("Component {} is deactivated.", systemIdentifier);
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
            JsonNode node = new JsonNode(result);
            node.put("name", systemIdentifier.getName());

            //TODO component.id and component.name can cause problems in JsonPath
            node.put(ComponentConstants.COMPONENT_ID, context.getProperties().get(ComponentConstants.COMPONENT_ID));
            node.put(ComponentConstants.COMPONENT_NAME, context.getProperties().get(ComponentConstants.COMPONENT_NAME));
            ConnectorFacade connectorFacade = getConnectorFacade(operationHelperBuilder.getRuntimeAPIConfiguration());
            connectorFacade.test();
            node.put("ok", true);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * Creates a new object in the object set.
     * <p/>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version if optimistic concurrency
     * is supported.
     *
     * @param id     the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param object the contents of the object to create in the object set.
     * @throws org.forgerock.openidm.objset.NotFoundException
     *          if the specified id could not be resolve.
     * @throws org.forgerock.openidm.objset.ForbiddenException
     *          if access to the object or object set is forbidden.
     */
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        String METHOD = "create";
        Id complexId = new Id(id);
        OperationHelper helper = operationHelperBuilder.build(complexId.getObjectType(), object);

        if (helper.isOperationPermitted(CreateApiOp.class)) {
            try {
                ConnectorObject connectorObject = helper.build(CreateApiOp.class, object);
                OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(CreateApiOp.class, connectorObject, object);
                Uid uid = getConnectorFacade(helper.getRuntimeAPIConfiguration()).create(connectorObject.getObjectClass(), AttributeUtil.filterUid(connectorObject.getAttributes()), operationOptionsBuilder.build());
                helper.resetUid(uid, object);
            } catch (AlreadyExistsException e) {
                TRACE.error("System object {} already exists", id, e);
                throw new ConflictException(e);
            } catch (ConfigurationException e) {
                TRACE.error("Operation {} failed with ConfigurationException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectionBrokenException e) {
                TRACE.error("Operation {} failed with ConnectionBrokenException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectionFailedException e) {
                TRACE.error("Connection failed during operation {} on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectorIOException e) {
                TRACE.error("Operation {} failed with ConnectorIOException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (OperationTimeoutException e) {
                TRACE.error("Operation {} Timeout on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (PasswordExpiredException e) {
                TRACE.error("Operation {} failed with PasswordExpiredException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (InvalidPasswordException e) {
                TRACE.error("Invalid password has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (UnknownUidException e) {
                TRACE.error("Operation {} failed with UnknownUidException on system object: {}", new Object[]{METHOD, id}, e);
                throw new NotFoundException(e);
            } catch (InvalidCredentialException e) {
                TRACE.error("Invalid credential has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (PermissionDeniedException e) {
                TRACE.error("Permission was denied on {} operation for system object: {}", new Object[]{METHOD, id}, e);
                throw new ForbiddenException(e);
            } catch (ConnectorSecurityException e) {
                TRACE.error("Operation {} failed with ConnectorSecurityException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectorException e) {
                TRACE.error("Operation {} failed with ConnectorException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (Exception e) {
                TRACE.error("Operation {} failed with Exception on system object: {}", new Object[]{METHOD, id}, e);
                throw new ObjectSetException(e);
            }
        }
    }

    /**
     * Reads an object from the object set.
     * <p/>
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} if optimistic concurrency is supported. If optimistic
     * concurrency is not supported, then {@code _rev} must be absent or {@code null}.
     *
     * @param id the identifier of the object to retrieve from the object set.
     * @return the requested object.
     * @throws org.forgerock.openidm.objset.NotFoundException
     *          if the specified object could not be found.
     * @throws org.forgerock.openidm.objset.ForbiddenException
     *          if access to the object is forbidden.
     */
    public Map<String, Object> read(String id) throws ObjectSetException {
        String METHOD = "read";
        Id complexId = new Id(id);
        OperationHelper helper = operationHelperBuilder.build(complexId.getObjectType(), null);
        try {
            ConnectorFacade facade = getConnectorFacade(helper.getRuntimeAPIConfiguration());
            if (facade.getSupportedOperations().contains(GetApiOp.class)) {
                if (helper.isOperationPermitted(GetApiOp.class)) {
                    //ConnectorObject connectorObject = helper.build(GetApiOp.class, complexId.getLocalId(), null);
                    OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(GetApiOp.class, (ConnectorObject) null, null);
                    ConnectorObject connectorObject = facade.getObject(helper.getObjectClass(), new Uid(complexId.getLocalId()), operationOptionsBuilder.build());
                    if (null != connectorObject) {
                        return helper.build(connectorObject);
                    }
                }
            } else if (facade.getSupportedOperations().contains(SearchApiOp.class)) {
                if (helper.isOperationPermitted(SearchApiOp.class)) {
                    //ConnectorObject connectorObject = helper.build(SearchApiOp.class, complexId.getLocalId(), null);
                    OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(SearchApiOp.class, (ConnectorObject) null, null);
                    Filter name = new EqualsFilter(new Name(complexId.getLocalId()));
                    facade.search(helper.getObjectClass(), name, helper.getResultsHandler(), operationOptionsBuilder.build());

                    if (!helper.getQueryResult().isEmpty()) {
                        return helper.getQueryResult().get(0);
                    }
                }
            }
        } catch (AlreadyExistsException e) {
            TRACE.error("System object {} already exists", id, e);
            throw new ConflictException(e);
        } catch (ConfigurationException e) {
            TRACE.error("Operation {} failed with ConfigurationException on system object: {}", new Object[]{METHOD, id}, e);
            throw new InternalServerErrorException(e);
        } catch (ConnectionBrokenException e) {
            TRACE.error("Operation {} failed with ConnectionBrokenException on system object: {}", new Object[]{METHOD, id}, e);
            throw new ServiceUnavailableException(e);
        } catch (ConnectionFailedException e) {
            TRACE.error("Connection failed during operation {} on system object: {}", new Object[]{METHOD, id}, e);
            throw new ServiceUnavailableException(e);
        } catch (ConnectorIOException e) {
            TRACE.error("Operation {} failed with ConnectorIOException on system object: {}", new Object[]{METHOD, id}, e);
            throw new ServiceUnavailableException(e);
        } catch (OperationTimeoutException e) {
            TRACE.error("Operation {} Timeout on system object: {}", new Object[]{METHOD, id}, e);
            throw new ServiceUnavailableException(e);
        } catch (PasswordExpiredException e) {
            TRACE.error("Operation {} failed with PasswordExpiredException on system object: {}", new Object[]{METHOD, id}, e);
            throw new InternalServerErrorException(e);
        } catch (InvalidPasswordException e) {
            TRACE.error("Invalid password has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
            throw new InternalServerErrorException(e);
        } catch (UnknownUidException e) {
            TRACE.error("Operation {} failed with UnknownUidException on system object: {}", new Object[]{METHOD, id}, e);
            throw new NotFoundException(e);
        } catch (InvalidCredentialException e) {
            TRACE.error("Invalid credential has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
            throw new InternalServerErrorException(e);
        } catch (PermissionDeniedException e) {
            TRACE.error("Permission was denied on {} operation for system object: {}", new Object[]{METHOD, id}, e);
            throw new ForbiddenException(e);
        } catch (ConnectorSecurityException e) {
            TRACE.error("Operation {} failed with ConnectorSecurityException on system object: {}", new Object[]{METHOD, id}, e);
            throw new InternalServerErrorException(e);
        } catch (ConnectorException e) {
            TRACE.error("Operation {} failed with ConnectorException on system object: {}", new Object[]{METHOD, id}, e);
            throw new InternalServerErrorException(e);
        } catch (Exception e) {
            TRACE.error("Operation {} failed with Exception on system object: {}", new Object[]{METHOD, id}, e);
            throw new ObjectSetException(e);
        }
        throw new NotFoundException(id);
    }

    /**
     * Updates an existing specified object in the object set.
     * <p/>
     * This method updates the {@code _rev} property to the revised object version on update
     * if optimistic concurrency is supported.
     *
     * @param id     the identifier of the object to be updated.
     * @param rev    the version of the object to update; or {@code null} if not provided.
     * @param object the contents of the object to updated in the object set.
     * @throws org.forgerock.openidm.objset.ConflictException
     *          if version is required but is {@code null}.
     * @throws org.forgerock.openidm.objset.ForbiddenException
     *          if access to the object is forbidden.
     * @throws org.forgerock.openidm.objset.NotFoundException
     *          if the specified object could not be found.
     * @throws org.forgerock.openidm.objset.PreconditionFailedException
     *          if version did not match the existing object in the set.
     */
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        String METHOD = "update";
        Id complexId = new Id(id);
        OperationHelper helper = operationHelperBuilder.build(complexId.getObjectType(), object);

        if (helper.isOperationPermitted(UpdateApiOp.class)) {
            try {
                ConnectorObject connectorObject = helper.build(UpdateApiOp.class, complexId.getLocalId(), object);
                OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(UpdateApiOp.class, connectorObject, object);

                Uid uid = getConnectorFacade(helper.getRuntimeAPIConfiguration()).update(connectorObject.getObjectClass(), connectorObject.getUid(), AttributeUtil.filterUid(connectorObject.getAttributes()), operationOptionsBuilder.build());
                //object.put("_id", uid.getUidValue());
                helper.resetUid(uid, object);
            } catch (AlreadyExistsException e) {
                TRACE.error("System object {} already exists", id, e);
                throw new ConflictException(e);
            } catch (ConfigurationException e) {
                TRACE.error("Operation {} failed with ConfigurationException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectionBrokenException e) {
                TRACE.error("Operation {} failed with ConnectionBrokenException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectionFailedException e) {
                TRACE.error("Connection failed during operation {} on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectorIOException e) {
                TRACE.error("Operation {} failed with ConnectorIOException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (OperationTimeoutException e) {
                TRACE.error("Operation {} Timeout on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (PasswordExpiredException e) {
                TRACE.error("Operation {} failed with PasswordExpiredException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (InvalidPasswordException e) {
                TRACE.error("Invalid password has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (UnknownUidException e) {
                TRACE.error("Operation {} failed with UnknownUidException on system object: {}", new Object[]{METHOD, id}, e);
                throw new NotFoundException(e);
            } catch (InvalidCredentialException e) {
                TRACE.error("Invalid credential has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (PermissionDeniedException e) {
                TRACE.error("Permission was denied on {} operation for system object: {}", new Object[]{METHOD, id}, e);
                throw new ForbiddenException(e);
            } catch (ConnectorSecurityException e) {
                TRACE.error("Operation {} failed with ConnectorSecurityException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectorException e) {
                TRACE.error("Operation {} failed with ConnectorException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (Exception e) {
                TRACE.error("Operation {} failed with Exception on system object: {}", new Object[]{METHOD, id}, e);
                throw new ObjectSetException(e);
            }
        }
    }

    /**
     * Deletes the specified object from the object set.
     *
     * @param id  the identifier of the object to be deleted.
     * @param rev the version of the object to delete or {@code null} if not provided.
     * @throws org.forgerock.openidm.objset.NotFoundException
     *          if the specified object could not be found.
     * @throws org.forgerock.openidm.objset.ForbiddenException
     *          if access to the object is forbidden.
     * @throws org.forgerock.openidm.objset.ConflictException
     *          if version is required but is {@code null}.
     * @throws org.forgerock.openidm.objset.PreconditionFailedException
     *          if version did not match the existing object in the set.
     */
    public void delete(String id, String rev) throws ObjectSetException {
        String METHOD = "delete";
        Id complexId = new Id(id);
        OperationHelper helper = operationHelperBuilder.build(complexId.getObjectType(), null);

        if (helper.isOperationPermitted(DeleteApiOp.class)) {
            try {
                OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(DeleteApiOp.class, (ConnectorObject) null, null);
                getConnectorFacade(helper.getRuntimeAPIConfiguration()).delete(helper.getObjectClass(), new Uid(complexId.getLocalId()), operationOptionsBuilder.build());
            } catch (AlreadyExistsException e) {
                TRACE.error("System object {} already exists", id, e);
                throw new ConflictException(e);
            } catch (ConfigurationException e) {
                TRACE.error("Operation {} failed with ConfigurationException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectionBrokenException e) {
                TRACE.error("Operation {} failed with ConnectionBrokenException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectionFailedException e) {
                TRACE.error("Connection failed during operation {} on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectorIOException e) {
                TRACE.error("Operation {} failed with ConnectorIOException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (OperationTimeoutException e) {
                TRACE.error("Operation {} Timeout on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (PasswordExpiredException e) {
                TRACE.error("Operation {} failed with PasswordExpiredException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (InvalidPasswordException e) {
                TRACE.error("Invalid password has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (UnknownUidException e) {
                TRACE.error("Operation {} failed with UnknownUidException on system object: {}", new Object[]{METHOD, id}, e);
                throw new NotFoundException(e);
            } catch (InvalidCredentialException e) {
                TRACE.error("Invalid credential has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (PermissionDeniedException e) {
                TRACE.error("Permission was denied on {} operation for system object: {}", new Object[]{METHOD, id}, e);
                throw new ForbiddenException(e);
            } catch (ConnectorSecurityException e) {
                TRACE.error("Operation {} failed with ConnectorSecurityException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectorException e) {
                TRACE.error("Operation {} failed with ConnectorException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (Exception e) {
                TRACE.error("Operation {} failed with Exception on system object: {}", new Object[]{METHOD, id}, e);
                throw new ObjectSetException(e);
            }
        }
    }

    /**
     * Applies a patch (partial change) to the specified object in the object set.
     *
     * @param id    the identifier of the object to be patched.
     * @param rev   the version of the object to patch or {@code null} if not provided.
     * @param patch the partial change to apply to the object.
     * @throws org.forgerock.openidm.objset.ConflictException
     *          if patch could not be applied object state or if version is required.
     * @throws org.forgerock.openidm.objset.ForbiddenException
     *          if access to the object is forbidden.
     * @throws org.forgerock.openidm.objset.NotFoundException
     *          if the specified object could not be found.
     * @throws org.forgerock.openidm.objset.PreconditionFailedException
     *          if version did not match the existing object in the set.
     */
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Performs a query on the specified object and returns the associated results.
     * <p/>
     * Queries are parametric; a set of named parameters is provided as the query criteria.
     * The query result is a JSON object structure composed of basic Java types.
     *
     * @param id     identifies the object to query.
     * @param params the parameters of the query to perform.
     * @return the query results object.
     * @throws org.forgerock.openidm.objset.NotFoundException
     *                                  if the specified object could not be found.
     * @throws org.forgerock.openidm.objset.ForbiddenException
     *                                  if access to the object or specified query is forbidden.
     * @throws IllegalArgumentException
     */
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        String METHOD = "query";
        Id complexId = new Id(id);
        OperationHelper helper = operationHelperBuilder.build(complexId.getObjectType(), null);
        Map<String, Object> result = new HashMap<String, Object>();
        if (helper.isOperationPermitted(SearchApiOp.class)) {
            try {
                OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(SearchApiOp.class, (ConnectorObject) null, null);
                Filter filter = null;
                if (null != params) {
                    Map<String, Object> query = (Map<String, Object>) params.get("query");
                    String queryId = (String) params.get("_query-id");
                    if (query != null) {
                        filter = helper.build(query, (Map<String, Object>) params.get("params"));
                    } else if (queryId != null) {
                        if (queryId.equals("query-all-ids")) {
                            // TODO: optimize query for ids, for now default to query all
                            operationOptionsBuilder.setAttributesToGet(Uid.NAME);
                        } else {
                            // Unknown query id
                            throw new BadRequestException("Unknown query id: " + queryId);
                        }
                    } else {
                        // Neither a query expression or query id defined,
                        // default to query all
                    }
                }
                getConnectorFacade(helper.getRuntimeAPIConfiguration()).search(helper.getObjectClass(), filter, helper.getResultsHandler(), operationOptionsBuilder.build());
                result.put("result", helper.getQueryResult());
            } catch (AlreadyExistsException e) {
                TRACE.error("System object {} already exists", id, e);
                throw new ConflictException(e);
            } catch (ConfigurationException e) {
                TRACE.error("Operation {} failed with ConfigurationException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectionBrokenException e) {
                TRACE.error("Operation {} failed with ConnectionBrokenException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectionFailedException e) {
                TRACE.error("Connection failed during operation {} on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectorIOException e) {
                TRACE.error("Operation {} failed with ConnectorIOException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (OperationTimeoutException e) {
                TRACE.error("Operation {} Timeout on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (PasswordExpiredException e) {
                TRACE.error("Operation {} failed with PasswordExpiredException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (InvalidPasswordException e) {
                TRACE.error("Invalid password has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (UnknownUidException e) {
                TRACE.error("Operation {} failed with UnknownUidException on system object: {}", new Object[]{METHOD, id}, e);
                throw new NotFoundException(e);
            } catch (InvalidCredentialException e) {
                TRACE.error("Invalid credential has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (PermissionDeniedException e) {
                TRACE.error("Permission was denied on {} operation for system object: {}", new Object[]{METHOD, id}, e);
                throw new ForbiddenException(e);
            } catch (ConnectorSecurityException e) {
                TRACE.error("Operation {} failed with ConnectorSecurityException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectorException e) {
                TRACE.error("Operation {} failed with ConnectorException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (Exception e) {
                TRACE.error("Operation {} failed with Exception on system object: {}", new Object[]{METHOD, id}, e);
                throw new ObjectSetException(e);
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
     * Synchronisation object:
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
     * OpenIDM takes active role in the synchronisation process by asking the end system to get all changed object.
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
     * @throws org.forgerock.json.fluent.JsonNodeException
     *                                       if the  {@code previousStage} is not Map.
     * @see {@link ConnectorUtil#convertToSyncToken(org.forgerock.json.fluent.JsonNode)} or any exception happed inside the connector.
     */
    @Override
    public JsonNode activeSynchronise(String objectType, JsonNode previousStage, final SynchronizationListener synchronizationListener) {
        JsonNode stage = previousStage != null ? previousStage.copy() : new JsonNode(new LinkedHashMap<String, Object>());
        JsonNode connectorData = stage.get("connectorData");
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
            final OperationHelper helper = operationHelperBuilder.build(objectType, stage.asMap());
            if (helper.isOperationPermitted(SyncApiOp.class)) {
                ConnectorFacade connector = getConnectorFacade(helper.getRuntimeAPIConfiguration());
                SyncApiOp operation = (SyncApiOp) connector.getOperation(SyncApiOp.class);
                if (null == operation) {
                    throw new UnsupportedOperationException(SyncApiOp.class.getCanonicalName());
                }
                if (null == token) {
                    token = operation.getLatestSyncToken(helper.getObjectClass());
                } else {
                    final SyncToken[] lastToken = new SyncToken[1];
                    final String[] failedRecord = new String[1];
                    OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(SyncApiOp.class, null, previousStage.asMap());
                    InvokeContext.getContext().pushActivityId(UUID.randomUUID().toString());
                    try {
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
                                    @Override
                                    public boolean handle(SyncDelta syncDelta) {
                                        try {
                                            Map<String, Object> deltaObject = helper.build(syncDelta.getObject());
                                            switch (syncDelta.getDeltaType()) {
                                                case CREATE_OR_UPDATE:
                                                    if (null != syncDelta.getPreviousUid()) {
                                                        deltaObject.put("_previousid", Id.escapeUid(syncDelta.getPreviousUid()));
                                                    }
                                                    synchronizationListener.onUpdate(helper.resolveQualifiedId(syncDelta.getUid()).getPath(), deltaObject);
                                                    break;
                                                case DELETE:
                                                    synchronizationListener.onDelete(helper.resolveQualifiedId(syncDelta.getUid()).getPath());
                                                    break;
                                            }
                                            lastToken[0] = syncDelta.getToken();
                                        } catch (Exception e) {
                                            failedRecord[0] = SerializerUtil.serializeXmlObject(syncDelta, true);
                                            TRACE.error("Failed synchronise {} object", syncDelta.getUid(), e);
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
                        TRACE.error("Active synchronisation of {} failed on {}", new Object[]{objectType, systemIdentifier.getName()}, t);
                    } finally {
                        token = lastToken[0];
                        InvokeContext.getContext().popActivityId();
                    }
                }
                if (null != token) {
                    stage.put("connectorData", ConnectorUtil.convertFromSyncToken(token));
                }
            }
        } catch (ObjectSetException e) {
            TRACE.error("Failed to get OperationHelper", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            // catch helper.getOperationOptionsBuilder(
            TRACE.error("Failed to get OperationOptionsBuilder", e);
            throw new RuntimeException(e);
        }
        return stage;
    }


    private JsonNode getConfiguration(ComponentContext componentContext) {
        EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
        return new JsonNode(enhancedConfig.getConfiguration(componentContext));
    }

    private ConnectorFacade getConnectorFacade(APIConfiguration runtimeAPIConfiguration) {
        ConnectorFacadeFactory connectorFacadeFactory = ConnectorFacadeFactory.getInstance();
        return connectorFacadeFactory.newInstance(runtimeAPIConfiguration);
    }


    /**
     * @param connector
     * @param script
     * @param operationOptions
     * @throws UnsupportedOperationException
     */
    public void doExecuteScript(ConnectorFacade connector, ConnectorScript script, OperationOptionsBuilder operationOptions) {
        ScriptContext ctxt = new ScriptContext(script.getScriptContextBuilder().getScriptLanguage(), script.getScriptContextBuilder().getScriptText(), script.getScriptContextBuilder().getScriptArguments());
        if ("connector".equals(script.getExecMode())) {
            connector.runScriptOnConnector(ctxt, new OperationOptionsBuilder().build());
        } else if ("resource".equals(script.getExecMode())) {
            connector.runScriptOnResource(ctxt, new OperationOptionsBuilder().build());
        }
    }
}
