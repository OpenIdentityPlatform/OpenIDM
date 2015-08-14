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

package org.forgerock.openidm.config.manage;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.ResourceException.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.http.ResourcePath;
import org.forgerock.http.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
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
import org.forgerock.openidm.util.ResourceUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to OSGi configuration
 *
 */
@Component(
        name = "org.forgerock.openidm.config.manage",
        immediate = true,
        policy = ConfigurationPolicy.OPTIONAL
)
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Configuration Service"),
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "/config*")
})
@Service
public class ConfigObjectService implements RequestHandler, ClusterEventListener {

    final static Logger logger = LoggerFactory.getLogger(ConfigObjectService.class);
    
    final static QueryFilterVisitor<QueryFilter<JsonPointer>, Object, JsonPointer> VISITOR = new ConfigQueryFilterVisitor<Object>();

    final static String CONFIG_KEY = "jsonconfig";

    final static String EVENT_LISTENER_ID = "config";
    final static String EVENT_RESOURCE_PATH = "resourcePath";
    final static String EVENT_RESOURCE_ID = "resourceId";
    final static String EVENT_RESOURCE_OBJECT = "obj";
    final static String EVENT_RESOURCE_ACTION = "action";
    
    enum ConfigAction {
        CREATE, UPDATE, DELETE;
    }

    @Reference
    ConfigurationAdmin configAdmin;
    
    /**
     * The ClusterManagementService used for sending and receiving cluster events
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    ClusterManagementService clusterManagementService;

    public void bindClusterManagementService(final ClusterManagementService clusterManagementService) {
        this.clusterManagementService = clusterManagementService;
        this.clusterManagementService.register(EVENT_LISTENER_ID, this);
    }

    public void unbindClusterManagementService(final ClusterManagementService clusterManagementService) {
        this.clusterManagementService.unregister(EVENT_LISTENER_ID);
        this.clusterManagementService = null;
    }
    
    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    private ConnectionFactory connectionFactory;

    protected void bindConnectionFactory(ConnectionFactory connectionFactory) {
    	this.connectionFactory = connectionFactory;
    }

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;

    protected void bindEnhancedConfig(EnhancedConfig enhancedConfig) {
    	this.enhancedConfig = enhancedConfig;
    }
    
    private ConfigCrypto configCrypto;

    @Override
    public Promise<ResourceResponse, ResourceException>  handleRead(final Context context, final ReadRequest request) {
        try {
        	return newResultPromise(newResourceResponse(request.getResourcePath(), null, new JsonValue(read(request.getResourcePathObject()))));
        } catch (ResourceException e) {
        	return newExceptionPromise(e);
        } catch (Exception e) {
        	return newExceptionPromise(newInternalServerErrorException(e.getMessage(), e));
        }
    }
    
    /**
     * Gets an object from the object set by identifier.
     * <p/>
     * The object may contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency supported by OpenIDM.
     *
     * @param resourcePath the identifier of the resource to retrieve from the object set.
     * @return the requested object.
     * @throws NotFoundException   if the specified object could not be found.
     * @throws ForbiddenException  if access to the object is forbidden.
     * @throws BadRequestException if the passed identifier is invalid
     */
    @SuppressWarnings("rawtypes")
    public JsonValue read(ResourcePath resourcePath) throws ResourceException {
        logger.debug("Invoking read {}", resourcePath.toString());
        JsonValue result = null;

        try {

            if (resourcePath.isEmpty()) {
                // List all configurations
                result = new JsonValue(new HashMap<String, Object>());
                Configuration[] rawConfigs = configAdmin.listConfigurations(null);
                List<Map<String, Object>> configList = new ArrayList<Map<String, Object>>();
                if (null != rawConfigs) {
                    for (Configuration conf : rawConfigs) {
                        Map<String, Object> configEntry = new LinkedHashMap<String, Object>();

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
                    throw new NotFoundException("No configuration exists for id " + resourcePath.toString());
                }
                Dictionary props = config.getProperties();
                result =  enhancedConfig.getConfiguration(props, resourcePath.toString(), false);
                result.put("_id", resourcePath.toString());
                logger.debug("Read configuration for service {}", resourcePath);
            }
        } catch (ResourceException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.warn("Failure to load configuration for {}", resourcePath, ex);
            throw new InternalServerErrorException("Failure to load configuration for " + resourcePath + ": " + ex.getMessage(), ex);
        }
        return result;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        try {
            JsonValue content = request.getContent();
            create(request.getResourcePathObject(), request.getNewResourceId(), content.asMap(), false);
            // Create and send the ClusterEvent for the created configuration 
            sendClusterEvent(ConfigAction.CREATE, request.getResourcePathObject(), request.getNewResourceId(), content.asMap());
            return newResultPromise(newResourceResponse(request.getNewResourceId(), null, content));
        } catch (ResourceException e) {
        	return newExceptionPromise(e);
        } catch (Exception e) {
        	return newExceptionPromise(newInternalServerErrorException(e.getMessage(), e));
        }
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
     * @throws NotFoundException           if the specified id could not be resolved.
     * @throws ForbiddenException          if access to the object or object set is forbidden.
     * @throws PreconditionFailedException if an object with the same ID already exists.
     * @throws BadRequestException         if the passed identifier is invalid
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void create(ResourcePath resourcePath, String id, Map<String, Object> obj, boolean allowExisting) throws ResourceException {
        logger.debug("Invoking create configuration {} {} {}", new Object[] { resourcePath.toString(), id, obj });
        if (id == null || id.length() == 0) {
            throw new BadRequestException("The passed identifier to create is null");
        }
        ParsedId parsedId = new ParsedId(resourcePath, id);
        try {
            Configuration config = null;
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
            
            Dictionary dict = configCrypto.encrypt(parsedId.getPidOrFactoryPid(), parsedId.instanceAlias, null, new JsonValue(obj));
            if (parsedId.isFactoryConfig()) {
                dict.put(JSONConfigInstaller.SERVICE_FACTORY_PID_ALIAS, parsedId.instanceAlias); // The alias for the PID as understood by fileinstall
            }

            config.update(dict);
            logger.debug("Created new configuration for {} with {}", parsedId.toString(), dict);
        } catch (ResourceException ex) {
            throw ex;
        } catch (JsonValueException ex) {
            logger.warn("Invalid configuration provided for {}:" + ex.getMessage(), resourcePath.toString(), ex);
            throw new BadRequestException("Invalid configuration provided for " + resourcePath.toString() + ": " + ex.getMessage(), ex);
        } catch (WaitForMetaData ex) {
            logger.info("No meta-data provider available yet to create and encrypt configuration for {}, retry later.", parsedId.toString(), ex);
            throw new InternalServerErrorException("No meta-data provider available yet to create and encrypt configuration for "
                    + parsedId.toString() + ", retry later.", ex);
        } catch (Exception ex) {
            logger.warn("Failure to create configuration for {}", parsedId.toString(), ex);
            throw new InternalServerErrorException("Failure to create configuration for " + parsedId.toString() + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        try {
            String rev = request.getRevision();
            JsonValue content = request.getContent();
            update(request.getResourcePathObject(), rev, content.asMap());
            // Create and send the ClusterEvent for the updated configuration 
            sendClusterEvent(ConfigAction.UPDATE, request.getResourcePathObject(), null, content.asMap());
            return newResultPromise(newResourceResponse(request.getResourcePath(), null, content));
        } catch (ResourceException e) {
        	return newExceptionPromise(e);
        } catch (Exception e) {
        	return newExceptionPromise(newInternalServerErrorException(e.getMessage(), e));
        }
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
     * @throws ConflictException           if version is required but is {@code null}.
     * @throws ForbiddenException          if access to the object is forbidden.
     * @throws NotFoundException           if the specified object could not be found.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     * @throws BadRequestException         if the passed identifier is invalid
     */
    @SuppressWarnings("rawtypes")
	public void update(ResourcePath resourcePath, String rev, Map<String, Object> obj) throws ResourceException {
        logger.debug("Invoking update configuration {} {}", resourcePath.toString(), rev);
        if (resourcePath.isEmpty()) {
            throw new BadRequestException("The passed identifier to update is empty");
        }
        else if (resourcePath.size() > 2) {
            throw new BadRequestException("The passed identifier to update has more than two parts");
        }
        
        ParsedId parsedId = new ParsedId(resourcePath);
        try {    
            Configuration config = findExistingConfiguration(parsedId);

            Dictionary existingConfig = (config == null ? null : config.getProperties());
            if (existingConfig == null) {
                throw new NotFoundException("No existing configuration found for " + resourcePath.toString() + ", can not update the configuration.");
            }
            
            existingConfig = configCrypto.encrypt(parsedId.getPidOrFactoryPid(), parsedId.instanceAlias, existingConfig, new JsonValue(obj));
            config.update(existingConfig);
            logger.debug("Updated existing configuration for {} with {}", resourcePath.toString(), existingConfig);
        } catch (ResourceException ex) {
            throw ex;
        } catch (JsonValueException ex) {
            logger.warn("Invalid configuration provided for {}:" + ex.getMessage(), resourcePath.toString(), ex);
            throw new BadRequestException("Invalid configuration provided for " + resourcePath.toString() + ": " + ex.getMessage(), ex);
        } catch (WaitForMetaData ex) {
            logger.info("No meta-data provider available yet to update and encrypt configuration for {}, retry later.", parsedId.toString(), ex);
            throw new InternalServerErrorException("No meta-data provider available yet to create and encrypt configuration for "
                    + parsedId.toString() + ", retry later.", ex);
        } catch (Exception ex) {
            logger.warn("Failure to update configuration for {}", resourcePath.toString(), ex);
            throw new InternalServerErrorException("Failure to update configuration for " + resourcePath.toString() + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException>  handleDelete(Context context, DeleteRequest request) {
        try {
            String rev = request.getRevision();
            JsonValue result = delete(request.getResourcePathObject(), rev);
            // Create and send the ClusterEvent for the deleted configuration 
            sendClusterEvent(ConfigAction.DELETE, request.getResourcePathObject(), null, null);
            return newResultPromise(newResourceResponse(request.getResourcePath(), null, result));
        } catch (ResourceException e) {
        	return newExceptionPromise(e);
        } catch (Exception e) {
        	return newExceptionPromise(newInternalServerErrorException(e.getMessage(), e));
        }
    }

    /**
     * Deletes the specified object from the object set.
     *
     * @param resourcePath the identifier of the resource to be deleted.
     * @param rev    the version of the object to delete or {@code null} if not provided.
     * @return the deleted object.
     * @throws NotFoundException           if the specified object could not be found.
     * @throws ForbiddenException          if access to the object is forbidden.
     * @throws ConflictException           if version is required but is {@code null}.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    @SuppressWarnings("rawtypes")
	public JsonValue delete(ResourcePath resourcePath, String rev) throws ResourceException {
        logger.debug("Invoking delete configuration {} {}", resourcePath.toString(), rev);
        if (resourcePath.isEmpty()) {
            throw new BadRequestException("The passed identifier to delete is null");
        }
        try {
            ParsedId parsedId = new ParsedId(resourcePath);
            Configuration config = findExistingConfiguration(parsedId);
            if (config == null) {
                throw new NotFoundException("No existing configuration found for " + resourcePath.toString() + ", can not delete the configuration.");
            }
            
            Dictionary existingConfig = config.getProperties();
            JsonValue value = enhancedConfig.getConfiguration(existingConfig, resourcePath.toString(), false);

            if (existingConfig == null) {
                throw new NotFoundException("No existing configuration found for " + resourcePath.toString() + ", can not delete the configuration.");
            }
            config.delete();
            logger.debug("Deleted configuration for {}", resourcePath.toString());
            
            return value;
        } catch (ResourceException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.warn("Failure to delete configuration for {}", resourcePath.toString(), ex);
            throw new InternalServerErrorException("Failure to delete configuration for " + resourcePath.toString() + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return newExceptionPromise(ResourceUtil.notSupported(request));
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest request, final QueryResourceHandler handler) {
        logger.debug("Invoking query");
        QueryRequest queryRequest = Requests.copyOfQueryRequest(request);
        QueryFilter<JsonPointer> filter = request.getQueryFilter();

        queryRequest.setResourcePath("repo/config");
        if (filter != null) {
            queryRequest.setQueryFilter(asConfigQueryFilter(filter));
        }

        try {
        	return newResultPromise(connectionFactory.getConnection().query(context, queryRequest, new QueryResourceHandler() {
				@Override
				public boolean handleResource(ResourceResponse resource) {
                    JsonValue content = resource.getContent();
                    JsonValue config = content.get(CONFIG_KEY).copy();
                    String id = ConfigBootstrapHelper.getId(content.get(ConfigBootstrapHelper.CONFIG_ALIAS).asString(), 
                            content.get(ConfigBootstrapHelper.SERVICE_PID).asString(), 
                            content.get(ConfigBootstrapHelper.SERVICE_FACTORY_PID).asString());
                    handler.handleResource(newResourceResponse(id, null, config));
                    return true;
				}
                
            }));
        } catch (ResourceException e) {
        	return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException>  handleAction(Context context, ActionRequest request) {
        return newExceptionPromise(ResourceUtil.notSupported(request));
    }

    /**
     * Locate an existing configuration based on its id, which can be
     * a pid or for factory configurations the <factory pid>/<alias>
     * pids can be qualified or if they use the default openidm prefix unqualified
     *
     * @param fullId the id
     * @return the configuration if found, null if not
     * @throws IOException
     * @throws InvalidSyntaxException
     */
    Configuration findExistingConfiguration(ParsedId parsedId) throws IOException, InvalidSyntaxException, BadRequestException {

        String filter = null;
        if (parsedId.isFactoryConfig()) {
            String factoryPid = ConfigBootstrapHelper.qualifyPid(parsedId.factoryPid);
            filter = "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + factoryPid + ")(" + JSONConfigInstaller.SERVICE_FACTORY_PID_ALIAS + "=" + parsedId.instanceAlias + "))";
        } else {
            String pid = ConfigBootstrapHelper.qualifyPid(parsedId.pid);
            filter = "(" + Constants.SERVICE_PID + "=" + pid + ")";
        }
        logger.trace("List configurations with filter: {}", filter);
        Configuration[] configurations = configAdmin.listConfigurations(filter);
        logger.debug("Configs found: {}", configurations);
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
    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivating configuration management service");
    }

    @Override
    public boolean handleEvent(ClusterEvent event) {
        switch (event.getType()) {
        case CUSTOM:
            try {
                JsonValue details = event.getDetails();
                ConfigAction action = ConfigAction.valueOf(details.get(EVENT_RESOURCE_ACTION).asString());
                ResourcePath resourcePath = ResourcePath.valueOf(details.get(EVENT_RESOURCE_PATH).asString());
                String id = details.get(EVENT_RESOURCE_ID).isNull() ? null : details.get(EVENT_RESOURCE_ID).asString();
                Map<String, Object> obj = details.get(EVENT_RESOURCE_OBJECT).isNull() ? null : details.get(EVENT_RESOURCE_OBJECT).asMap();
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
                }
            } catch (Exception e) {
                logger.error("Error handling cluster event: " + e.getMessage(), e);
                return false;
            }
        default:
            return true;
        }
    }
    
    /**
     * Creates and sends a ClusterEvent representing a config operation for a specified resource
     * 
     * @param action The action that was performed on the resource (create, update, or delete)
     * @param name The resource name
     * @param id The new resource id (used for create)
     * @param obj The resource object (used for create and update)
     */
    private void sendClusterEvent(ConfigAction action, ResourcePath name, String id, Map<String, Object> obj) {
        if (clusterManagementService != null && clusterManagementService.isEnabled()) {
            JsonValue details = json(object(
                    field(EVENT_RESOURCE_ACTION, action.toString()),
                    field(EVENT_RESOURCE_PATH, name.toString()),
                    field(EVENT_RESOURCE_ID, id),
                    field(EVENT_RESOURCE_OBJECT, obj)));
            ClusterEvent event = new ClusterEvent(
                    ClusterEventType.CUSTOM, 
                    clusterManagementService.getInstanceId(),
                    EVENT_LISTENER_ID,
                    details);
            clusterManagementService.sendEvent(event);
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
            return QueryFilter.not(subFilter.accept(new ConfigQueryFilterVisitor<Object>(), null));
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
            List<QueryFilter<JsonPointer>> visitedFilters = new ArrayList<QueryFilter<JsonPointer>>();
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
     * @param name
     * @param id
     * @return
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
        public String factoryPid;
        public String instanceAlias;

        public ParsedId(ResourcePath resourcePath) throws BadRequestException {
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

        public ParsedId(ResourcePath resourcePath, String id) throws BadRequestException {
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
        public boolean isFactoryConfig() {
            return (instanceAlias != null);
        }

        /**
         * Get the qualified pid of the managed service or managed factory depending on the configuration represented
         * Some APIs do not distinguish between single managed service PID and managed factory PID
         *
         * @return the qualified pid if this ID represents a managed service configuration, or the managed factory PID
         *         if it represents a managed factory configuration
         */
        public String getPidOrFactoryPid() {
            return isFactoryConfig()
                    ? ConfigBootstrapHelper.qualifyPid(factoryPid)
                    : ConfigBootstrapHelper.qualifyPid(pid);
        }

        public String toString() {
            return isFactoryConfig()
                    ? (factoryPid + "-" + instanceAlias)
                    : pid;
        }
    }
}
