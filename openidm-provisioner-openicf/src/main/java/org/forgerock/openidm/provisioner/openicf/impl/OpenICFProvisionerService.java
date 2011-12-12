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
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.objset.*;
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
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
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

    private final static Logger logger = LoggerFactory.getLogger(OpenICFProvisionerService.class);
    private ComponentContext context = null;
    private SimpleSystemIdentifier systemIdentifier = null;
    private OperationHelperBuilder operationHelperBuilder = null;
    private boolean allowModification = true;
    private final static ObjectMapper mapper = new ObjectMapper();

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
    @Override
    public void create(String id, Map<String, Object> object) throws ObjectSetException {
        String METHOD = "create";
        traceObject(METHOD, id, object);
        Id complexId = new Id(id);
        OperationHelper helper = operationHelperBuilder.build(complexId.getObjectType(), object, cryptoService);

        if (allowModification && helper.isOperationPermitted(CreateApiOp.class)) {
            try {
                ConnectorObject connectorObject = helper.build(CreateApiOp.class, object);
                OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(CreateApiOp.class, connectorObject, object);
                Uid uid = getConnectorFacade(helper.getRuntimeAPIConfiguration()).create(connectorObject.getObjectClass(), AttributeUtil.filterUid(connectorObject.getAttributes()), operationOptionsBuilder.build());
                helper.resetUid(uid, object);
            } catch (AlreadyExistsException e) {
                logger.error("System object {} already exists", id, e);
                throw new ConflictException(e);
            } catch (ConfigurationException e) {
                logger.error("Operation {} failed with ConfigurationException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectionBrokenException e) {
                logger.error("Operation {} failed with ConnectionBrokenException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectionFailedException e) {
                logger.error("Connection failed during operation {} on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectorIOException e) {
                logger.error("Operation {} failed with ConnectorIOException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (OperationTimeoutException e) {
                logger.error("Operation {} Timeout on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (PasswordExpiredException e) {
                logger.error("Operation {} failed with PasswordExpiredException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (InvalidPasswordException e) {
                logger.error("Invalid password has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (UnknownUidException e) {
                logger.error("Operation {} failed with UnknownUidException on system object: {}", new Object[]{METHOD, id}, e);
                throw new NotFoundException(e);
            } catch (InvalidCredentialException e) {
                logger.error("Invalid credential has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (PermissionDeniedException e) {
                logger.error("Permission was denied on {} operation for system object: {}", new Object[]{METHOD, id}, e);
                throw new ForbiddenException(e);
            } catch (ConnectorSecurityException e) {
                logger.error("Operation {} failed with ConnectorSecurityException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectorException e) {
                logger.error("Operation {} failed with ConnectorException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (Exception e) {
                logger.error("Operation {} failed with Exception on system object: {}", new Object[]{METHOD, id}, e);
                throw new ObjectSetException(e);
            }
        } else {
            logger.debug("Operation {} of {} is not permitted", METHOD, id);
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
    @Override
    public Map<String, Object> read(String id) throws ObjectSetException {
        String METHOD = "read";
        Id complexId = new Id(id);
        OperationHelper helper = operationHelperBuilder.build(complexId.getObjectType(), null, cryptoService);
        try {
            ConnectorFacade facade = getConnectorFacade(helper.getRuntimeAPIConfiguration());
            if (facade.getSupportedOperations().contains(GetApiOp.class)) {
                if (helper.isOperationPermitted(GetApiOp.class)) {
                    //ConnectorObject connectorObject = helper.build(GetApiOp.class, complexId.getLocalId(), null);
                    OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(GetApiOp.class, null, null);
                    ConnectorObject connectorObject = facade.getObject(helper.getObjectClass(), new Uid(complexId.getLocalId()), operationOptionsBuilder.build());
                    if (null != connectorObject) {
                        return helper.build(connectorObject);
                    }
                } else {
                    logger.debug("Operation {} of {} is not permitted", METHOD, id);
                }
            } else if (facade.getSupportedOperations().contains(SearchApiOp.class)) {
                if (helper.isOperationPermitted(SearchApiOp.class)) {
                    //ConnectorObject connectorObject = helper.build(SearchApiOp.class, complexId.getLocalId(), null);
                    OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(SearchApiOp.class, null, null);
                    Filter name = new EqualsFilter(new Name(complexId.getLocalId()));
                    facade.search(helper.getObjectClass(), name, helper.getResultsHandler(), operationOptionsBuilder.build());

                    if (!helper.getQueryResult().isEmpty()) {
                        return helper.getQueryResult().get(0);
                    }
                } else {
                    logger.debug("Operation {} of {} is not permitted", METHOD, id);
                }
            }
        } catch (AlreadyExistsException e) {
            logger.error("System object {} already exists", id, e);
            throw new ConflictException(e);
        } catch (ConfigurationException e) {
            logger.error("Operation {} failed with ConfigurationException on system object: {}", new Object[]{METHOD, id}, e);
            throw new InternalServerErrorException(e);
        } catch (ConnectionBrokenException e) {
            logger.error("Operation {} failed with ConnectionBrokenException on system object: {}", new Object[]{METHOD, id}, e);
            throw new ServiceUnavailableException(e);
        } catch (ConnectionFailedException e) {
            logger.error("Connection failed during operation {} on system object: {}", new Object[]{METHOD, id}, e);
            throw new ServiceUnavailableException(e);
        } catch (ConnectorIOException e) {
            logger.error("Operation {} failed with ConnectorIOException on system object: {}", new Object[]{METHOD, id}, e);
            throw new ServiceUnavailableException(e);
        } catch (OperationTimeoutException e) {
            logger.error("Operation {} Timeout on system object: {}", new Object[]{METHOD, id}, e);
            throw new ServiceUnavailableException(e);
        } catch (PasswordExpiredException e) {
            logger.error("Operation {} failed with PasswordExpiredException on system object: {}", new Object[]{METHOD, id}, e);
            throw new InternalServerErrorException(e);
        } catch (InvalidPasswordException e) {
            logger.error("Invalid password has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
            throw new InternalServerErrorException(e);
        } catch (UnknownUidException e) {
            logger.error("Operation {} failed with UnknownUidException on system object: {}", new Object[]{METHOD, id}, e);
            throw new NotFoundException(e);
        } catch (InvalidCredentialException e) {
            logger.error("Invalid credential has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
            throw new InternalServerErrorException(e);
        } catch (PermissionDeniedException e) {
            logger.error("Permission was denied on {} operation for system object: {}", new Object[]{METHOD, id}, e);
            throw new ForbiddenException(e);
        } catch (ConnectorSecurityException e) {
            logger.error("Operation {} failed with ConnectorSecurityException on system object: {}", new Object[]{METHOD, id}, e);
            throw new InternalServerErrorException(e);
        } catch (ConnectorException e) {
            logger.error("Operation {} failed with ConnectorException on system object: {}", new Object[]{METHOD, id}, e);
            throw new InternalServerErrorException(e);
        } catch (Exception e) {
            logger.error("Operation {} failed with Exception on system object: {}", new Object[]{METHOD, id}, e);
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
    @Override
    public void update(String id, String rev, Map<String, Object> object) throws ObjectSetException {
        String METHOD = "update";
        traceObject(METHOD, id, object);
        Id complexId = new Id(id);
        OperationHelper helper = operationHelperBuilder.build(complexId.getObjectType(), object, cryptoService);

        if (allowModification && helper.isOperationPermitted(UpdateApiOp.class)) {
            try {
                Object newName = object.get("_name");
                ConnectorObject connectorObject = null;
                Set<Attribute> attributeSet = null;
                if (newName instanceof String) {
                    //This is a rename
                    connectorObject = helper.build(UpdateApiOp.class, (String) newName, object);
                    attributeSet = AttributeUtil.filterUid(connectorObject.getAttributes());
                } else {
                    connectorObject = helper.build(UpdateApiOp.class, complexId.getLocalId(), object);
                    attributeSet = new HashSet<Attribute>();
                    for (Attribute attribute : connectorObject.getAttributes()) {
                        if (attribute.is(Name.NAME) || attribute.is(Uid.NAME)) {
                            continue;
                        }
                        attributeSet.add(attribute);
                    }
                }
                OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(UpdateApiOp.class, connectorObject, object);
                Uid uid = getConnectorFacade(helper.getRuntimeAPIConfiguration()).update(connectorObject.getObjectClass(), connectorObject.getUid(), attributeSet, operationOptionsBuilder.build());
                helper.resetUid(uid, object);
            } catch (AlreadyExistsException e) {
                logger.error("System object {} already exists", id, e);
                throw new ConflictException(e);
            } catch (ConfigurationException e) {
                logger.error("Operation {} failed with ConfigurationException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectionBrokenException e) {
                logger.error("Operation {} failed with ConnectionBrokenException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectionFailedException e) {
                logger.error("Connection failed during operation {} on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectorIOException e) {
                logger.error("Operation {} failed with ConnectorIOException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (OperationTimeoutException e) {
                logger.error("Operation {} Timeout on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (PasswordExpiredException e) {
                logger.error("Operation {} failed with PasswordExpiredException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (InvalidPasswordException e) {
                logger.error("Invalid password has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (UnknownUidException e) {
                logger.error("Operation {} failed with UnknownUidException on system object: {}", new Object[]{METHOD, id}, e);
                throw new NotFoundException(e);
            } catch (InvalidCredentialException e) {
                logger.error("Invalid credential has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (PermissionDeniedException e) {
                logger.error("Permission was denied on {} operation for system object: {}", new Object[]{METHOD, id}, e);
                throw new ForbiddenException(e);
            } catch (ConnectorSecurityException e) {
                logger.error("Operation {} failed with ConnectorSecurityException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectorException e) {
                logger.error("Operation {} failed with ConnectorException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (Exception e) {
                logger.error("Operation {} failed with Exception on system object: {}", new Object[]{METHOD, id}, e);
                throw new ObjectSetException(e);
            }
        } else {
            logger.debug("Operation {} of {} is not permitted", METHOD, id);
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
    @Override
    public void delete(String id, String rev) throws ObjectSetException {
        String METHOD = "delete";
        Id complexId = new Id(id);
        OperationHelper helper = operationHelperBuilder.build(complexId.getObjectType(), null, cryptoService);

        if (allowModification && helper.isOperationPermitted(DeleteApiOp.class)) {
            try {
                OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(DeleteApiOp.class, null, null);
                getConnectorFacade(helper.getRuntimeAPIConfiguration()).delete(helper.getObjectClass(), new Uid(complexId.getLocalId()), operationOptionsBuilder.build());
            } catch (AlreadyExistsException e) {
                logger.error("System object {} already exists", id, e);
                throw new ConflictException(e);
            } catch (ConfigurationException e) {
                logger.error("Operation {} failed with ConfigurationException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectionBrokenException e) {
                logger.error("Operation {} failed with ConnectionBrokenException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectionFailedException e) {
                logger.error("Connection failed during operation {} on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectorIOException e) {
                logger.error("Operation {} failed with ConnectorIOException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (OperationTimeoutException e) {
                logger.error("Operation {} Timeout on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (PasswordExpiredException e) {
                logger.error("Operation {} failed with PasswordExpiredException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (InvalidPasswordException e) {
                logger.error("Invalid password has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (UnknownUidException e) {
                logger.error("Operation {} failed with UnknownUidException on system object: {}", new Object[]{METHOD, id}, e);
                throw new NotFoundException(e);
            } catch (InvalidCredentialException e) {
                logger.error("Invalid credential has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (PermissionDeniedException e) {
                logger.error("Permission was denied on {} operation for system object: {}", new Object[]{METHOD, id}, e);
                throw new ForbiddenException(e);
            } catch (ConnectorSecurityException e) {
                logger.error("Operation {} failed with ConnectorSecurityException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectorException e) {
                logger.error("Operation {} failed with ConnectorException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (Exception e) {
                logger.error("Operation {} failed with Exception on system object: {}", new Object[]{METHOD, id}, e);
                throw new ObjectSetException(e);
            }
        } else {
            logger.debug("Operation {} of {} is not permitted", METHOD, id);
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
    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {

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
    @Override
    public Map<String, Object> query(String id, Map<String, Object> params) throws ObjectSetException {
        String METHOD = "query";
        Id complexId = new Id(id);
        OperationHelper helper = operationHelperBuilder.build(complexId.getObjectType(), null, cryptoService);
        Map<String, Object> result = new HashMap<String, Object>();
        if (helper.isOperationPermitted(SearchApiOp.class)) {
            try {
                OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(SearchApiOp.class, null, null);
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
                logger.error("System object {} already exists", id, e);
                throw new ConflictException(e);
            } catch (ConfigurationException e) {
                logger.error("Operation {} failed with ConfigurationException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectionBrokenException e) {
                logger.error("Operation {} failed with ConnectionBrokenException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectionFailedException e) {
                logger.error("Connection failed during operation {} on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectorIOException e) {
                logger.error("Operation {} failed with ConnectorIOException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (OperationTimeoutException e) {
                logger.error("Operation {} Timeout on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (PasswordExpiredException e) {
                logger.error("Operation {} failed with PasswordExpiredException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (InvalidPasswordException e) {
                logger.error("Invalid password has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (UnknownUidException e) {
                logger.error("Operation {} failed with UnknownUidException on system object: {}", new Object[]{METHOD, id}, e);
                throw new NotFoundException(e);
            } catch (InvalidCredentialException e) {
                logger.error("Invalid credential has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (PermissionDeniedException e) {
                logger.error("Permission was denied on {} operation for system object: {}", new Object[]{METHOD, id}, e);
                throw new ForbiddenException(e);
            } catch (ConnectorSecurityException e) {
                logger.error("Operation {} failed with ConnectorSecurityException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectorException e) {
                logger.error("Operation {} failed with ConnectorException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (Exception e) {
                logger.error("Operation {} failed with Exception on system object: {}", new Object[]{METHOD, id}, e);
                throw new ObjectSetException(e);
            }
        } else {
            logger.debug("Operation {} of {} is not permitted", METHOD, id);
        }
        return result;
    }

    /**
     * {
     * _script-type = Boo|SHELL|Groovy,
     * _script-expression = xxx,
     * _script-execute-on = onConnectorServer | onResource,
     * _script-variable-prefix = openidm_,
     * attr1 = x,
     * }
     *
     * @param id     identifies the object to perform the action on.
     * @param params the parameters of the action to perform.
     * @return
     * @throws ObjectSetException
     */
    @Override
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        String METHOD = "action";
        Id complexId = new Id(id);
        OperationHelper helper = operationHelperBuilder.build(complexId.getObjectType(), params, cryptoService);
        Map<String, Object> result = new HashMap<String, Object>();
        ConnectorScript script = new ConnectorScript(params);
        if (helper.isOperationPermitted(script.getAPIOperation())) {
            try {
                Object vpo = params.get(ConnectorScript.SCRIPT_VARIABLE_PREFIX);
                String variablePrefix = null;
                if (vpo instanceof String) {
                    variablePrefix = (String) vpo;
                }

                for (Map.Entry<String, Object> entry : params.entrySet()) {
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
                script.getScriptContextBuilder().addScriptArgument("openidm_id", id);

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
            } catch (AlreadyExistsException e) {
                logger.error("System object {} already exists", id, e);
                throw new ConflictException(e);
            } catch (ConfigurationException e) {
                logger.error("Operation {} failed with ConfigurationException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectionBrokenException e) {
                logger.error("Operation {} failed with ConnectionBrokenException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectionFailedException e) {
                logger.error("Connection failed during operation {} on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (ConnectorIOException e) {
                logger.error("Operation {} failed with ConnectorIOException on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (OperationTimeoutException e) {
                logger.error("Operation {} Timeout on system object: {}", new Object[]{METHOD, id}, e);
                throw new ServiceUnavailableException(e);
            } catch (PasswordExpiredException e) {
                logger.error("Operation {} failed with PasswordExpiredException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (InvalidPasswordException e) {
                logger.error("Invalid password has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (UnknownUidException e) {
                logger.error("Operation {} failed with UnknownUidException on system object: {}", new Object[]{METHOD, id}, e);
                throw new NotFoundException(e);
            } catch (InvalidCredentialException e) {
                logger.error("Invalid credential has been provided to operation {} for system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (PermissionDeniedException e) {
                logger.error("Permission was denied on {} operation for system object: {}", new Object[]{METHOD, id}, e);
                throw new ForbiddenException(e);
            } catch (ConnectorSecurityException e) {
                logger.error("Operation {} failed with ConnectorSecurityException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (ConnectorException e) {
                logger.error("Operation {} failed with ConnectorException on system object: {}", new Object[]{METHOD, id}, e);
                throw new InternalServerErrorException(e);
            } catch (Exception e) {
                logger.error("Operation {} failed with Exception on system object: {}", new Object[]{METHOD, id}, e);
                throw new ObjectSetException(e);
            }
        } else {
            logger.debug("Operation {} of {} is not permitted", METHOD, id);
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
            final OperationHelper helper = operationHelperBuilder.build(objectType, stage.asMap(), cryptoService);
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
                    OperationOptionsBuilder operationOptionsBuilder = helper.getOperationOptionsBuilder(SyncApiOp.class, null, previousStage.asMap());
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
                                            Map<String, Object> deltaObject = helper.build(syncDelta.getObject());
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
        } catch (ObjectSetException e) {
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

    private void traceObject(String action, String id, Map<String, Object> source) {
        if (logger.isTraceEnabled()) {
            if (null != source) {
                try {
                    StringWriter writer = new StringWriter();
                    mapper.writeValue(writer, source);
                    logger.info("Action: {}, Id: {}, Object: {}", new Object[]{action, id, writer});
                } catch (IOException e) {
                    //Don't care
                }
            }
        }
    }
}
