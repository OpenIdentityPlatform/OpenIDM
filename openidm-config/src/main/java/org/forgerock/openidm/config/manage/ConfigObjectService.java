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
 * Portions Copyrighted 2024 3A Systems LLC.
 */


package org.forgerock.openidm.config.manage;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.util.ResourceUtil.notSupported;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.ApiProducer;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.cluster.ClusterEvent;
import org.forgerock.openidm.cluster.ClusterEventListener;
import org.forgerock.openidm.cluster.ClusterEventType;
import org.forgerock.openidm.cluster.ClusterManagementService;
import org.forgerock.openidm.config.crypto.ConfigCrypto;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.installer.JSONConfigInstaller;
import org.forgerock.openidm.config.persistence.ConfigBootstrapHelper;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.metadata.WaitForMetaData;
import org.forgerock.openidm.patch.JsonValuePatch;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.script.ScriptedPatchValueTransformerFactory;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.ResultHandler;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to OSGi configuration.
 *
 */
@Component(
        name = ConfigObjectService.PID,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.OPTIONAL,
        property = {
                Constants.SERVICE_PID + "=" + ConfigObjectService.PID,
                ServerConstants.ROUTER_PREFIX + "=/config*"
        },
        service = RequestHandler.class)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM Configuration Service")
public class ConfigObjectService implements RequestHandler, ClusterEventListener, Describable<ApiDescription, Request> {

    static final String PID = "org.forgerock.openidm.config.manage";

    private static final Logger logger = LoggerFactory.getLogger(ConfigObjectService.class);
    private final ConfigAuditEventLogger auditLogger;

    private static final QueryFilterVisitor<QueryFilter<JsonPointer>, Object, JsonPointer> VISITOR = new ConfigQueryFilterVisitor<Object>();

    private static final String CONFIG_KEY = "jsonconfig";

    private static final String EVENT_LISTENER_ID = "config";
    private static final String EVENT_RESOURCE_PATH = "resourcePath";
    private static final String EVENT_RESOURCE_ID = "resourceId";
    private static final String EVENT_RESOURCE_OBJECT = "obj";
    private static final String EVENT_RESOURCE_ACTION = "action";
    private static final String EVENT_PATCH_OPERATIONS = "patchOperations";

    public void bindScriptedPatchValueTransformerFactory(ScriptedPatchValueTransformerFactory scriptedPatchValueTransformerFactory) {
        this.scriptedPatchValueTransformerFactory=scriptedPatchValueTransformerFactory;
    }

    private enum ConfigAction {
        CREATE, UPDATE, DELETE, PATCH
    }

    @Reference
    ConfigurationAdmin configAdmin;

    private final ApiDescription apiDescription;

    /**
     * The ClusterManagementService used for sending and receiving cluster events
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    volatile ClusterManagementService clusterManagementService;

    public void bindClusterManagementService(final ClusterManagementService clusterManagementService) {
        this.clusterManagementService = clusterManagementService;
        this.clusterManagementService.register(EVENT_LISTENER_ID, this);
    }

    public void unbindClusterManagementService(final ClusterManagementService clusterManagementService) {
        this.clusterManagementService.unregister(EVENT_LISTENER_ID);
        this.clusterManagementService = null;
    }

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC)
    private IDMConnectionFactory connectionFactory;

    protected void bindConnectionFactory(IDMConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /** Scripted Patch Value Transformer Factory. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile ScriptedPatchValueTransformerFactory scriptedPatchValueTransformerFactory;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    protected void bindEnhancedConfig(EnhancedConfig enhancedConfig) {
        this.enhancedConfig = enhancedConfig;
    }

    private ConfigCrypto configCrypto;

    public ConfigObjectService() {
        this(new ConfigAuditEventLogger());
    }

    public ConfigObjectService(final ConfigAuditEventLogger configAuditEventLogger) {
        this.auditLogger = configAuditEventLogger;
        this.apiDescription = ConfigObjectServiceApiDescription.build();
    }

    @Override
    public Promise<ResourceResponse, ResourceException>  handleRead(final Context context, final ReadRequest request) {
        return read(request.getResourcePathObject())
                .thenAsync(
                        new AsyncFunction<ConfigAuditState, ResourceResponse, ResourceException>() {
                            @Override
                            public Promise<ResourceResponse, ResourceException> apply(
                                    ConfigAuditState configAuditState) {
                                if (ContextUtil.isExternal(context)) {
                                    // Log audit event.
                                    auditLogger.log(configAuditState, request, context, connectionFactory);
                                }

                                return newResourceResponse(
                                        configAuditState.getId(),
                                        configAuditState.getRevision(),
                                        configAuditState.getAfter()).asPromise();
                            }
                        });
    }

    /**
     * Gets an object from the object set by identifier.
     * <p/>
     * The object may contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency supported by OpenIDM.
     *
     * @param resourcePath the identifier of the resource to retrieve from the object set.
     * @return promise with the populated state if successful, otherwise exception promises, ie:
     *    NotFoundException if the specified object could not be found.
     *    ForbiddenException  if access to the object is forbidden.
     *    BadRequestException if the passed identifier is invalid
     */
    @SuppressWarnings("rawtypes")
    private Promise<ConfigAuditState, ResourceException> read(ResourcePath resourcePath) {
        logger.debug("Invoking read {}", resourcePath.toString());
        JsonValue result;

        try {

            if (resourcePath.isEmpty()) {
                // List all configurations
                result = new JsonValue(new HashMap<String, Object>());
                Configuration[] rawConfigs = configAdmin.listConfigurations(null);
                List<Map<String, Object>> configList = new ArrayList<>();
                if (null != rawConfigs) {
                    for (Configuration conf : rawConfigs) {
                        Map<String, Object> configEntry = new LinkedHashMap<>();

                        String alias = null;
                        Dictionary properties = conf.getProperties();
                        if (properties != null) {
                            alias = (String) properties.get(JSONConfigInstaller.SERVICE_FACTORY_PID_ALIAS);
                        }
                        String pid = ConfigBootstrapHelper.unqualifyPid(conf.getPid());
                        String factoryPid = ConfigBootstrapHelper.unqualifyPid(conf.getFactoryPid());
                        // If there is an alias for factory config is available, make a nicer ID then the internal PID
                        String id = factoryPid != null && alias != null
                                ? factoryPid + "/" + alias
                                : pid;

                        configEntry.put("_id", id);
                        configEntry.put("pid", pid);
                        configEntry.put("factoryPid", factoryPid);
                        configList.add(configEntry);
                    }
                }
                result.put("configurations", configList);
                logger.debug("Read list of configurations with {} entries", configList.size());
            } else {
                Configuration config = findExistingConfiguration(new ParsedId(resourcePath));
                if (config == null) {
                    return new NotFoundException(
                            "No configuration exists for id " + resourcePath.toString())
                            .asPromise();
                }
                Dictionary<String, Object> props = config.getProperties();
                result =  enhancedConfig.getRawConfiguration(props, resourcePath.toString());
                result.put("_id", resourcePath.toString());
                logger.debug("Read configuration for service {}", resourcePath);
            }
        } catch (ResourceException ex) {
            logger.warn("Failure to load configuration for {}", resourcePath, ex);
            return ex.asPromise();
        } catch (Exception ex) {
            logger.warn("Failure to load configuration for {}", resourcePath, ex);
            return new InternalServerErrorException(
                    "Failure to load configuration for " + resourcePath + ": " + ex.getMessage(), ex).asPromise();
        }
        return newResultPromise(new ConfigAuditState(resourcePath.toString(),
                result.get(ResourceResponse.FIELD_CONTENT_REVISION).asString(), result, result));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
                                                                     final CreateRequest request) {
        return create(request.getResourcePathObject(), request.getNewResourceId(), request.getContent(), false)
                .thenAsync(
                        new AsyncFunction<ConfigAuditState, ResourceResponse, ResourceException>() {
                            @Override
                            public Promise<ResourceResponse, ResourceException> apply(
                                    ConfigAuditState configAuditState) throws ResourceException {

                                final JsonValue after = configAuditState.getAfter();
                                // Create and send the ClusterEvent for the created configuration
                                sendClusterEvent(ConfigAction.CREATE, request.getResourcePathObject(),
                                        request.getNewResourceId(), after);

                                // Log audit event.
                                auditLogger.log(configAuditState, request, context, connectionFactory);

                                return newResourceResponse(configAuditState.getId(),
                                        configAuditState.getRevision(), after).asPromise();
                            }
                        });
    }

    /**
     * Creates a new object in the object set.
     * <p/>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
     *
     * @param resourcePath for multi-instance config, the factory pid to use
     * @param id the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param obj    the contents of the object to create in the object set.
     *  @return promise with config state populated if successful, otherwise exception promises, ie:
     *  NotFoundException:           if the specified id could not be resolved.
     *  ForbiddenException:         if access to the object or object set is forbidden.
     *  PreconditionFailedException: if an object with the same ID already exists.
     *  BadRequestException:         if the passed identifier is invalid
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Promise<ConfigAuditState, ResourceException> create(ResourcePath resourcePath, String id,
                                                               JsonValue obj, boolean allowExisting) {
        logger.debug("Invoking create configuration {} {} {}", resourcePath.toString(), id, obj);
        if (id == null || id.length() == 0) {
            return new BadRequestException("The passed identifier to create is null").asPromise();
        }

        ParsedId parsedId;
        try {
            parsedId = new ParsedId(resourcePath, id);
        } catch (BadRequestException e) {
            return e.asPromise();
        }

        try {

            Configuration config;
            if (parsedId.isFactoryConfig()) {
                if (findExistingConfiguration(parsedId) != null && !allowExisting) {
                    throw new PreconditionFailedException("Can not create a new factory configuration with ID "
                            + parsedId + ", configuration for this ID already exists.");
                }
                String qualifiedFactoryPid = ConfigBootstrapHelper.qualifyPid(parsedId.factoryPid);
                if ("org.forgerock.openidm.router".equalsIgnoreCase(qualifiedFactoryPid)) {
                    throw new BadRequestException("router config can not be factory config");
                }
                config = configAdmin.createFactoryConfiguration(qualifiedFactoryPid, null);
            } else {
                String qualifiedPid = ConfigBootstrapHelper.qualifyPid(parsedId.pid);
                config = configAdmin.getConfiguration(qualifiedPid, null);
            }
            if (config.getProperties() != null && !allowExisting) {
                throw new PreconditionFailedException("Can not create a new configuration with ID "
                        + parsedId + ", configuration for this ID already exists.");
            }

            Dictionary dict = configCrypto.encrypt(parsedId.getPidOrFactoryPid(), parsedId.instanceAlias, null, obj);
            if (parsedId.isFactoryConfig()) {
                // The alias for the PID as understood by file install
                dict.put(JSONConfigInstaller.SERVICE_FACTORY_PID_ALIAS, parsedId.instanceAlias);
            }

            config.update(dict);
            logger.debug("Created new configuration for {} with {}", parsedId.toString(), dict);

            JsonValue after = enhancedConfig.getRawConfiguration(config.getProperties(), resourcePath.toString());

            String revision = (null == after)
                    ? null
                    : after.get(ResourceResponse.FIELD_CONTENT_REVISION).asString();
            return newResultPromise(new ConfigAuditState(parsedId.toString(), revision, null, after));

        } catch (ResourceException ex) {
            return ex.asPromise();
        } catch (JsonValueException ex) {
            logger.warn("Invalid configuration provided for {}:" + ex.getMessage(), resourcePath.toString(), ex);
            return new BadRequestException(
                    "Invalid configuration provided for " + resourcePath.toString() + ": " + ex.getMessage(), ex)
                    .asPromise();
        } catch (WaitForMetaData ex) {
            logger.info("No meta-data provider available yet to create and encrypt configuration for {}, retry later.",
                    parsedId.toString(), ex);
            return new InternalServerErrorException(
                    "No meta-data provider available yet to create and encrypt configuration for "
                            + parsedId.toString() + ", retry later.", ex).asPromise();
        } catch (Exception ex) {
            logger.warn("Failure to create configuration for {}", parsedId.toString(), ex);
            return new InternalServerErrorException(
                    "Failure to create configuration for " + parsedId.toString() + ": " + ex.getMessage(), ex)
                    .asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
                                                                     final UpdateRequest request) {

        return update(request.getResourcePathObject(), request.getRevision(), request.getContent())
                .thenAsync(
                        new AsyncFunction<ConfigAuditState, ResourceResponse, ResourceException>() {
                            @Override
                            public Promise<ResourceResponse, ResourceException> apply(
                                    ConfigAuditState configAuditState) throws ResourceException {

                                final JsonValue after = configAuditState.getAfter();

                                // Create and send the ClusterEvent for the updated configuration
                                sendClusterEvent(ConfigAction.UPDATE, request.getResourcePathObject(), after);

                                // Log audit event.
                                auditLogger.log(configAuditState, request, context, connectionFactory);

                                return newResourceResponse(configAuditState.getId(), configAuditState.getRevision(),
                                        after).asPromise();
                            }
                        });
    }

    /**
     * Updates the specified object in the object set.
     * <p/>
     * This implementation requires MVCC and hence enforces that clients state what revision they expect
     * to be updating
     * <p/>
     * If successful, this method updates metadata properties within the passed object,
     * including: a new {@code _rev} value for the revised object's version
     *
     * @param resourcePath the identifier of the resource to be updated
     * @param rev    the version of the object to update; or {@code null} if not provided.
     * @param obj    the contents of the object to put in the object set.
     * @return promise populated with the config state if successful, otherwise exception promises, ie:
     *  ConflictException:           if version is required but is {@code null}.
     *  ForbiddenException:          if access to the object is forbidden.
     *  NotFoundException:           if the specified object could not be found.
     *  PreconditionFailedException: if version did not match the existing object in the set.
     *  BadRequestException:         if the passed identifier is invalid
     */
    @SuppressWarnings("rawtypes")
    private Promise<ConfigAuditState, ResourceException> update(ResourcePath resourcePath, String rev,
                                                               JsonValue obj) {
        logger.debug("Invoking update configuration {} {}", resourcePath.toString(), rev);
        if (resourcePath.isEmpty()) {
            return new BadRequestException("The passed identifier to update is empty").asPromise();
        }
        else if (resourcePath.size() > 2) {
            return new BadRequestException("The passed identifier to update has more than two parts").asPromise();
        }

        ParsedId parsedId;
        try {
            parsedId = new ParsedId(resourcePath);
        } catch (BadRequestException e) {
            return e.asPromise();
        }

        try {
            Configuration config = findExistingConfiguration(parsedId);

            Dictionary<String, Object> existingConfig = (config == null ? null : config.getProperties());
            if (existingConfig == null) {
                return new NotFoundException("No existing configuration found for " + resourcePath.toString() +
                        ", can not update the configuration.").asPromise();
            }

            // Strip the _rev and _id properties from the config
            obj.remove(ResourceResponse.FIELD_CONTENT_ID);
            obj.remove(ResourceResponse.FIELD_CONTENT_REVISION);

            JsonValue before = enhancedConfig.getRawConfiguration(config.getProperties(), resourcePath.toString());
            existingConfig = configCrypto.encrypt(parsedId.getPidOrFactoryPid(), parsedId.instanceAlias, existingConfig,
                    obj);
            config.update(existingConfig);
            JsonValue after = enhancedConfig.getRawConfiguration(config.getProperties(), resourcePath.toString());

            logger.debug("Updated existing configuration for {} with {}", resourcePath.toString(), existingConfig);
            return newResultPromise(
                    new ConfigAuditState(resourcePath.toString(),
                            after.get(ResourceResponse.FIELD_CONTENT_REVISION).asString(), before, after));

        } catch (ResourceException ex) {
            return ex.asPromise();
        } catch (JsonValueException ex) {
            logger.warn("Invalid configuration provided for {}:" + ex.getMessage(), resourcePath.toString(), ex);
            return new BadRequestException(
                    "Invalid configuration provided for " + resourcePath.toString() + ": " + ex.getMessage(),
                    ex).asPromise();
        } catch (WaitForMetaData ex) {
            logger.info("No meta-data provider available yet to update and encrypt configuration for {}, retry later.",
                    parsedId.toString(), ex);
            return new InternalServerErrorException(
                    "No meta-data provider available yet to create and encrypt configuration for "
                            + parsedId.toString() + ", retry later.", ex).asPromise();
        } catch (Exception ex) {
            logger.warn("Failure to update configuration for {}", resourcePath.toString(), ex);
            return new InternalServerErrorException(
                    "Failure to update configuration for " + resourcePath.toString() + ": " + ex.getMessage(), ex)
                    .asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(final Context context,
                                                                     final DeleteRequest request) {
        String rev = request.getRevision();
        return delete(request.getResourcePathObject(), rev)
                .thenAsync(
                        new AsyncFunction<ConfigAuditState, ResourceResponse, ResourceException>() {
                            @Override
                            public Promise<ResourceResponse, ResourceException> apply(
                                    ConfigAuditState configAuditState) throws ResourceException {

                                // Create and send the ClusterEvent for the deleted configuration
                                sendClusterEvent(ConfigAction.DELETE, request.getResourcePathObject());
                                // Log audit event.
                                auditLogger.log(configAuditState, request, context, connectionFactory);

                                return newResourceResponse(configAuditState.getId(),
                                        configAuditState.getRevision(),
                                        configAuditState.getBefore()).asPromise();

                            }
                        });
    }

    /**
     * Deletes the specified object from the object set.
     *
     * @param resourcePath the identifier of the resource to be deleted.
     * @param rev    the version of the object to delete or {@code null} if not provided.
     * @return promise populated with the original config if successful, otherwise exception promises, ie:
     * NotFoundException:           if the specified object could not be found.
     * ForbiddenException:          if access to the object is forbidden.
     * ConflictException:           if version is required but is {@code null}.
     * PreconditionFailedException: if version did not match the existing object in the set.
     */
    @SuppressWarnings("rawtypes")
    private Promise<ConfigAuditState, ResourceException> delete(ResourcePath resourcePath,
                                                               String rev) {
        logger.debug("Invoking delete configuration {} {}", resourcePath.toString(), rev);
        if (resourcePath.isEmpty()) {
            return new BadRequestException("The passed identifier to delete is null").asPromise();
        }
        try {
            ParsedId parsedId = new ParsedId(resourcePath);
            Configuration config = findExistingConfiguration(parsedId);
            if (config == null) {
                return new NotFoundException(
                        "No existing configuration found for " + resourcePath.toString()
                                + ", can not delete the configuration.").asPromise();
            }

            Dictionary<String, Object> existingConfig = config.getProperties();
            JsonValue value = enhancedConfig.getRawConfiguration(existingConfig, resourcePath.toString());

            if (existingConfig == null) {
                return new NotFoundException(
                        "No existing configuration found for " + resourcePath.toString()
                                + ", can not delete the configuration.").asPromise();
            }
            config.delete();
            logger.debug("Deleted configuration for {}", resourcePath.toString());

            return newResultPromise(new ConfigAuditState(resourcePath.toString(),
                    value.get(ResourceResponse.FIELD_CONTENT_REVISION).asString(), value, null));
        } catch (ResourceException ex) {
            return ex.asPromise();
        } catch (Exception ex) {
            logger.warn("Failure to delete configuration for {}", resourcePath.toString(), ex);
            return new InternalServerErrorException(
                    "Failure to delete configuration for " + resourcePath.toString() + ": " + ex.getMessage(), ex)
                    .asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context, final PatchRequest request) {
        return patch(context, request.getResourcePathObject(), request.getPatchOperations())
                .thenAsync(
                        new AsyncFunction<ConfigAuditState, ResourceResponse, ResourceException>() {
                            @Override
                            public Promise<ResourceResponse, ResourceException> apply(
                                    ConfigAuditState configAuditState) throws ResourceException {
                                sendClusterEvent(ConfigAction.PATCH, request.getResourcePathObject(),
                                        request.getPatchOperations());

                                // Log audit event.
                                auditLogger.log(configAuditState, request, context, connectionFactory);

                                return newResourceResponse(
                                        configAuditState.getId(),
                                        configAuditState.getRevision(),
                                        configAuditState.getAfter()).asPromise();

                            }
                        });
    }

    @SuppressWarnings("rawtypes")
    private Promise<ConfigAuditState, ResourceException> patch(final Context context, final ResourcePath resourcePath,
            final List<PatchOperation> patchOperation) {
        final ParsedId parsedId;
        try {
            parsedId = new ParsedId(resourcePath);
        } catch (BadRequestException e) {
            return e.asPromise();
        }

        try {
            final Configuration config = findExistingConfiguration(parsedId);

            Dictionary<String, Object> existingConfig = (config == null ? null : config.getProperties());
            if (existingConfig == null) {
                throw new NotFoundException("No existing configuration found for "
                        + resourcePath.toString()
                        + ", can not patch the configuration.");
            }

            final JsonValue before = enhancedConfig.getRawConfiguration(existingConfig, resourcePath.toString());
            final JsonValue after = before.copy();
            JsonValuePatch.apply(after, patchOperation, scriptedPatchValueTransformerFactory.getPatchValueTransformer(context));

            existingConfig = configCrypto.encrypt(
                    parsedId.getPidOrFactoryPid(), parsedId.instanceAlias, existingConfig, after);
            config.update(existingConfig);

            logger.debug("Patched existing configuration for {} with {}", resourcePath.toString(), existingConfig);
            return newResultPromise(
                    new ConfigAuditState(
                            resourcePath.toString(),
                            after.get(ResourceResponse.FIELD_CONTENT_REVISION).asString(),
                            before,
                            after));
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (JsonValueException e) {
            logger.warn("Invalid configuration provided for {}:" + e.getMessage(), resourcePath.toString(), e);
            return new BadRequestException("Invalid configuration provided for " + resourcePath.toString()
                    + ": " + e.getMessage(), e)
                    .asPromise();
        } catch (WaitForMetaData e) {
            logger.info("No meta-data provider available yet to patch and encrypt configuration for {}, retry later.",
                    parsedId.toString(), e);
            return new InternalServerErrorException(
                    "No meta-data provider available yet to patch and encrypt configuration for "
                            + parsedId.toString() + ", retry later.", e)
                    .asPromise();
        } catch (Exception e) {
            logger.warn("Failure to patch configuration for {}", resourcePath.toString(), e);
            return new InternalServerErrorException("Failure to patch configuration for " + resourcePath.toString()
                    + ": " + e.getMessage(), e)
                    .asPromise();
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest request,
                                                                 final QueryResourceHandler handler) {
        logger.debug("Invoking query");
        final QueryRequest queryRequest = Requests.copyOfQueryRequest(request);
        QueryFilter<JsonPointer> filter = request.getQueryFilter();

        queryRequest.setResourcePath("repo/config");
        if (filter != null) {
            queryRequest.setQueryFilter(asConfigQueryFilter(filter));
        } else if (QueryConstants.QUERY_ALL_IDS.equals(queryRequest.getQueryId())) {
            return new NotSupportedException(
                    request.getQueryId() + " is not supported").asPromise();
        }

        try {
            final JsonValue auditContent = json(array());

            return Promises.<QueryResponse, ResourceException>newResultPromise(
                    connectionFactory.getConnection().query(context, queryRequest, new QueryResourceHandler() {
                        @Override
                        public boolean handleResource(ResourceResponse resource) {
                            JsonValue content = resource.getContent();
                            JsonValue config = content.get(CONFIG_KEY).copy();
                            String id = ConfigBootstrapHelper.getId(
                                    content.get(ConfigBootstrapHelper.CONFIG_ALIAS).asString(),
                                    content.get(ConfigBootstrapHelper.SERVICE_PID).asString(),
                                    content.get(ConfigBootstrapHelper.SERVICE_FACTORY_PID).asString());
                            // filter out factory-group entries from result-set
                            if (id != null && config.isNotNull()) {
                                handler.handleResource(newResourceResponse(id, resource.getRevision(), config));
                                auditContent.add(config);
                            }
                            return true;
                        }
                    }))
                    .thenOnResult(new ResultHandler<QueryResponse>() {
                        @Override
                        public void handleResult(QueryResponse result) {
                            ConfigAuditState configAuditState = new ConfigAuditState(null, null, auditContent, null);
                            // Log audit event.
                            auditLogger.log(configAuditState, request, context, connectionFactory);
                        }
                    });
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        return notSupported(request).asPromise();
    }

    /**
     * Locate an existing configuration based on its id, which can be
     * a pid or for factory configurations the <factory pid>/<alias>
     * pids can be qualified or if they use the default openidm prefix unqualified
     *
     * @param parsedId the id
     * @return the configuration if found, null if not
     * @throws IOException
     * @throws InvalidSyntaxException
     */
    Configuration findExistingConfiguration(ParsedId parsedId) throws IOException, InvalidSyntaxException {

        String filter;
        if (parsedId.isFactoryConfig()) {
            String factoryPid = ConfigBootstrapHelper.qualifyPid(parsedId.factoryPid);
            filter = "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + factoryPid + ")(" + JSONConfigInstaller.SERVICE_FACTORY_PID_ALIAS + "=" + parsedId.instanceAlias + "))";
        } else {
            String pid = ConfigBootstrapHelper.qualifyPid(parsedId.pid);
            filter = "(" + Constants.SERVICE_PID + "=" + pid + ")";
        }
        logger.trace("List configurations with filter: {}", filter);
        Configuration[] configurations = configAdmin.listConfigurations(filter);
        logger.debug("Configs found: {}", (Object[]) configurations);
        if (configurations != null && configurations.length > 0) {
            return configurations[0];
        } else {
            return null;
        }
    }

    @Activate
    protected void activate(ComponentContext context) {
        logger.debug("Activating configuration management service");
        this.configCrypto = ConfigCrypto.getInstance(context.getBundleContext(), null);
    }

    @Deactivate
    protected void deactivate(@SuppressWarnings("unused") ComponentContext context) {
        logger.debug("Deactivating configuration management service");
    }

    @Override
    public boolean handleEvent(ClusterEvent event) {
        switch (event.getType()) {
            case CUSTOM:
                return handleCustomEvent(event);
            default:
                return true;
        }
    }

    private boolean handleCustomEvent(ClusterEvent event) {
        try {
            final JsonValue details = event.getDetails();
            final ConfigAction action = ConfigAction.valueOf(details.get(EVENT_RESOURCE_ACTION).asString());
            final ResourcePath resourcePath = ResourcePath.valueOf(details.get(EVENT_RESOURCE_PATH).asString());
            final String id = details.get(EVENT_RESOURCE_ID).isNull() ? null : details.get(EVENT_RESOURCE_ID).asString();
            final JsonValue obj = details.get(EVENT_RESOURCE_OBJECT).isNull() ? null : details.get(EVENT_RESOURCE_OBJECT);
            final List<PatchOperation> patchOperations = PatchOperation.valueOfList(details.get(EVENT_PATCH_OPERATIONS));
            switch (action) {
                case CREATE:
                    create(resourcePath, id, obj, true);
                    break;
                case UPDATE:
                    update(resourcePath, null, obj);
                    break;
                case DELETE:
                    delete(resourcePath, null);
                    break;
                case PATCH:
                    patch(ContextUtil.createInternalContext(), resourcePath, patchOperations);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error handling cluster event: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Creates and sends a ClusterEvent representing a config operation for a specified resource.
     *
     * @param action the action that was performed on the resource (create, update, delete, or patch)
     * @param name the resource name
     * @param id the new resource id (used for create)
     * @param obj the resource object (used for create and update)
     * @param patchOperations the {@link List<PatchOperation> list of patch operations} (used for patch)
     * @return a {@link ClusterEvent}
     */
    private ClusterEvent createClusterEvent(final ConfigAction action, final ResourcePath name,
            final String id, final JsonValue obj, final List<PatchOperation> patchOperations) {
        if (clusterManagementService != null && clusterManagementService.isEnabled()) {
            final JsonValue details = json(object(
                    field(EVENT_RESOURCE_ACTION, action.toString()),
                    field(EVENT_RESOURCE_PATH, name.toString()),
                    field(EVENT_RESOURCE_ID, id),
                    field(EVENT_RESOURCE_OBJECT, obj == null ? null : obj.getObject()),
                    field(EVENT_PATCH_OPERATIONS, serializePatchOperations(patchOperations).getObject())
            ));
            return new ClusterEvent(
                    ClusterEventType.CUSTOM,
                    clusterManagementService.getInstanceId(),
                    EVENT_LISTENER_ID,
                    details);
        }
        return null;
    }

    /**
     * Sends a ClusterEvent representing a config operation for a specified resource.
     *
     * @param action the action that was performed on the resource
     * @param name the resource name
     * @param id the new resource id
     * @param obj the resource object
     */
    private void sendClusterEvent(final ConfigAction action, final ResourcePath name, final String id,
            final JsonValue obj) {
        if (clusterManagementService != null && clusterManagementService.isEnabled()) {
            clusterManagementService.sendEvent(
                    createClusterEvent(action, name, id, obj, Collections.<PatchOperation>emptyList())
            );
        }
    }

    /**
     * Sends a ClusterEvent representing a config operation for a specified resource.
     *
     * @param action the action that was performed on the resource
     * @param name the resource name
     * @param obj the resource object
     */
    private void sendClusterEvent(final ConfigAction action, final ResourcePath name, final JsonValue obj) {
        if (clusterManagementService != null && clusterManagementService.isEnabled()) {
            clusterManagementService.sendEvent(
                    createClusterEvent(action, name, null, obj, Collections.<PatchOperation>emptyList())
            );
        }
    }

    /**
     * Sends a ClusterEvent representing a config operation for a specified resource.
     *
     * @param action the action that was performed on the resource
     * @param name the resource name
     * @param patchOperations the {@link List<PatchOperation> list of patch operations}
     */
    private void sendClusterEvent(final ConfigAction action, final ResourcePath name,
            final List<PatchOperation> patchOperations) {
        if (clusterManagementService != null && clusterManagementService.isEnabled()) {
            clusterManagementService.sendEvent(
                    createClusterEvent(action, name, null, null, patchOperations)
            );
        }
    }

    /**
     * Sends a ClusterEvent representing a config operation for a specified resource.
     *
     * @param action the action that was performed on the resource
     * @param name the resource name
     */
    private void sendClusterEvent(final ConfigAction action, final ResourcePath name) {
        if (clusterManagementService != null && clusterManagementService.isEnabled()) {
            clusterManagementService.sendEvent(
                    createClusterEvent(action, name, null, null, Collections.<PatchOperation>emptyList())
            );
        }
    }

    String getParsedId(String resourcePath) throws ResourceException {
        return new ParsedId(ResourcePath.valueOf(resourcePath)).toString();
    }

    boolean isFactoryConfig(String resourcePath) throws ResourceException {
        return new ParsedId(ResourcePath.valueOf(resourcePath)).isFactoryConfig();
    }

    static QueryFilter<JsonPointer> asConfigQueryFilter(QueryFilter<JsonPointer> filter) {
        return filter.accept(VISITOR, null);
    }

    private JsonValue serializePatchOperations(final List<PatchOperation> patchOperations) {
        final JsonValue ops = json(array());
        for (final PatchOperation patchOperation : patchOperations) {
            ops.add(patchOperation.toJsonValue().getObject());
        }
        return ops;
    }

    @Override
    public ApiDescription api(final ApiProducer<ApiDescription> producer) {
        return apiDescription;
    }

    @Override
    public ApiDescription handleApiRequest(final Context context, final Request request) {
        return apiDescription;
    }

    @Override
    public void addDescriptorListener(final Listener listener) {
        // empty
    }

    @Override
    public void removeDescriptorListener(final Listener listener) {
        // empty
    }

    /**
     * A {@link QueryFilterVisitor} implementation which modifies the {@link JsonPointer} fields by prepending them
     * with the appropriate key where the full config object is located.
     */
    private static class ConfigQueryFilterVisitor<P> implements QueryFilterVisitor<QueryFilter<JsonPointer>, P, JsonPointer> {

        private final static String CONFIG_FACTORY_PID = "/" + ConfigBootstrapHelper.CONFIG_ALIAS;
        private final static String SERVICE_FACTORY_PID = "/" + ConfigBootstrapHelper.SERVICE_FACTORY_PID;
        private final static String SERVICE_PID = "/" + ConfigBootstrapHelper.SERVICE_PID;

        @Override
        public QueryFilter<JsonPointer> visitAndFilter(P parameter, List<QueryFilter<JsonPointer>> subFilters) {
            return QueryFilter.and(visitQueryFilters(subFilters));
        }

        @Override
        public QueryFilter<JsonPointer> visitBooleanLiteralFilter(P parameter, boolean value) {
            return value ? QueryFilter.<JsonPointer>alwaysTrue() : QueryFilter.<JsonPointer>alwaysFalse();
        }

        @Override
        public QueryFilter<JsonPointer> visitContainsFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.contains(getConfigPointer(field), valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitEqualsFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.equalTo(getConfigPointer(field), valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitExtendedMatchFilter(P parameter, JsonPointer field, String operator, Object valueAssertion) {
            return QueryFilter.comparisonFilter(getConfigPointer(field), operator, valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitGreaterThanFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.greaterThan(getConfigPointer(field), valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitGreaterThanOrEqualToFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.greaterThanOrEqualTo(getConfigPointer(field), valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitLessThanFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.lessThan(getConfigPointer(field), valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitLessThanOrEqualToFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.lessThanOrEqualTo(getConfigPointer(field), valueAssertion);
        }

        @Override
        public QueryFilter<JsonPointer> visitNotFilter(P parameter, QueryFilter<JsonPointer> subFilter) {
            return QueryFilter.not(subFilter.accept(new ConfigQueryFilterVisitor<>(), null));
        }

        @Override
        public QueryFilter<JsonPointer> visitOrFilter(P parameter, List<QueryFilter<JsonPointer>> subFilters) {
            return QueryFilter.or(visitQueryFilters(subFilters));
        }

        @Override
        public QueryFilter<JsonPointer> visitPresentFilter(P parameter, JsonPointer field) {
            return QueryFilter.present(getConfigPointer(field));
        }

        @Override
        public QueryFilter<JsonPointer> visitStartsWithFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.startsWith(getConfigPointer(field), valueAssertion);
        }

        /**
         * Visits each {@link QueryFilter} in a list of filters and returns a list of the
         * visited filters.
         *
         * @param subFilters a list of the filters to visit
         * @return a list of visited filters
         */
        private List<QueryFilter<JsonPointer>> visitQueryFilters(List<QueryFilter<JsonPointer>> subFilters) {
            final List<QueryFilter<JsonPointer>> visitedFilters = new ArrayList<>();
            for (QueryFilter<JsonPointer> filter : subFilters) {
                visitedFilters.add(asConfigQueryFilter(filter));
            }
            return visitedFilters;
        }

        /**
         * Prepends the fields in the configuration object with the key where the object is stored.  
         * Will not modify fields outside of the configuration object.
         *
         * @param field a {@link JsonPointer} representing the field to modify.
         * @return a {@link JsonPointer} representing the modified field
         */
        private JsonPointer getConfigPointer(JsonPointer field) {
            if (CONFIG_FACTORY_PID.equals(field.toString())
                    || SERVICE_FACTORY_PID.equals(field.toString())
                    || SERVICE_PID.equals(field.toString())) {
                return field;
            }
            return new JsonPointer("/" + ConfigObjectService.CONFIG_KEY + field.toString());
        }
    }

    /**
     * In order to use findExistingConfiguration() you must have a ParsedId.  This method provides that as
     * a package-private and avoids the "containing class" error.
     *
     * @param name the resource path name
     * @param id the resource id
     * @return the {@link ParsedId}
     * @throws BadRequestException
     */
    ParsedId getParsedId(ResourcePath name, String id) throws BadRequestException {
        return new ParsedId(name, id);
    }

    /**
     * A class for converting resource names and IDs to qualified PIDs that represent managed services.
     */
    class ParsedId {

        public String pid;
        String factoryPid;
        String instanceAlias;

        ParsedId(ResourcePath resourcePath) throws BadRequestException {
            // OSGi pid with spaces is disallowed; replace any spaces we get to be kind
            ResourcePath stripped = resourcePath.toString().contains(" ")
                    ? ResourcePath.valueOf(resourcePath.toString().replaceAll(" ", "_"))
                    : resourcePath;

            switch (stripped.size()) {
                case 2:
                    factoryPid = stripped.parent().toString();
                    instanceAlias = stripped.leaf();
                    break;

                case 1:
                    pid = stripped.toString();
                    break;

                default:
                    throw new BadRequestException("The passed resourcePath has more than two parts");
            }

            if (null != factoryPid) {
                logger.trace("Factory configuration pid: {} instance alias: {}", factoryPid, instanceAlias);
            } else {
                logger.trace("Managed service configuration pid: {}", pid);
            }
        }

        ParsedId(ResourcePath resourcePath, String id) throws BadRequestException {
            if (resourcePath.isEmpty()) {
                // single-instance config
                pid = id;
            } else {
                // multi-instance config
                factoryPid = resourcePath.toString();
                instanceAlias = id;
            }
        }

        /**
         * @return is this ID represents a managed factory configuration, or false if it is a managed service configuraiton
         */
        boolean isFactoryConfig() {
            return (instanceAlias != null);
        }

        /**
         * Get the qualified pid of the managed service or managed factory depending on the configuration represented
         * Some APIs do not distinguish between single managed service PID and managed factory PID
         *
         * @return the qualified pid if this ID represents a managed service configuration, or the managed factory PID
         *         if it represents a managed factory configuration
         */
        String getPidOrFactoryPid() {
            return isFactoryConfig()
                    ? ConfigBootstrapHelper.qualifyPid(factoryPid)
                    : ConfigBootstrapHelper.qualifyPid(pid);
        }

        public String toString() {
            return isFactoryConfig()
                    ? (factoryPid + "/" + instanceAlias)
                    : pid;
        }
    }
}