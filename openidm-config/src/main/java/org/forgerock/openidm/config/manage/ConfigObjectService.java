/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

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
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
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
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceName;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.cluster.ClusterEvent;
import org.forgerock.openidm.cluster.ClusterEventListener;
import org.forgerock.openidm.cluster.ClusterEventType;
import org.forgerock.openidm.cluster.ClusterManagementService;
import org.forgerock.openidm.config.crypto.ConfigCrypto;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.config.installer.JSONConfigInstaller;
import org.forgerock.openidm.config.persistence.ConfigBootstrapHelper;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.metadata.WaitForMetaData;
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
        name = "org.forgerock.openidm.config.enhanced",
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
    
    final static QueryFilterVisitor<QueryFilter, Object> VISITOR = new ConfigQueryFilterVisitor<Object>();

    final static String CONFIG_KEY = "jsonconfig";

    final static String EVENT_LISTENER_ID = "config";
    final static String EVENT_RESOURCE_NAME = "resourceName";
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
    protected ConnectionFactory connectionFactory;

    private ConfigCrypto configCrypto;

    @Override
    public void handleRead(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        try {
            Resource resource = new Resource(request.getResourceName(), null, new JsonValue(read(request.getResourceNameObject())));
            handler.handleResult(resource);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }
    
    /**
     * Gets an object from the object set by identifier.
     * <p/>
     * The object may contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency supported by OpenIDM.
     *
     * @param resourceName the identifier of the resource to retrieve from the object set.
     * @return the requested object.
     * @throws NotFoundException   if the specified object could not be found.
     * @throws ForbiddenException  if access to the object is forbidden.
     * @throws BadRequestException if the passed identifier is invalid
     */
    public JsonValue read(ResourceName resourceName) throws ResourceException {
        logger.debug("Invoking read {}", resourceName.toString());
        JsonValue result = null;

        try {

            if (resourceName.isEmpty()) {
                // List all configurations
                result = new JsonValue(new HashMap<String, Object>());
                Configuration[] rawConfigs = configAdmin.listConfigurations(null);
                List configList = new ArrayList();
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
                Configuration config = findExistingConfiguration(new ParsedId(resourceName));
                if (config == null) {
                    throw new NotFoundException("No configuration exists for id " + resourceName.toString());
                }
                Dictionary props = config.getProperties();
                JSONEnhancedConfig enhancedConfig = new JSONEnhancedConfig();
                result =  enhancedConfig.getConfiguration(props, resourceName.toString(), false);
                result.put("_id", resourceName.toString());
                logger.debug("Read configuration for service {}", resourceName);
            }
        } catch (ResourceException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.warn("Failure to load configuration for {}", resourceName, ex);
            throw new InternalServerErrorException("Failure to load configuration for " + resourceName + ": " + ex.getMessage(), ex);
        }
        return result;
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler) {
        try {
            JsonValue content = request.getContent();
            create(request.getResourceNameObject(), request.getNewResourceId(), content.asMap(), false);
            // Create and send the ClusterEvent for the created configuration 
            sendClusterEvent(ConfigAction.CREATE, request.getResourceNameObject(), request.getNewResourceId(), content.asMap());
            Resource resource = new Resource(request.getNewResourceId(), null, content);
            handler.handleResult(resource);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    /**
     * Creates a new object in the object set.
     * <p/>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
     *
     * @param resourceName for multi-instance config, the factory pid to use
     * @param id the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param obj    the contents of the object to create in the object set.
     * @throws NotFoundException           if the specified id could not be resolved.
     * @throws ForbiddenException          if access to the object or object set is forbidden.
     * @throws PreconditionFailedException if an object with the same ID already exists.
     * @throws BadRequestException         if the passed identifier is invalid
     */
    public void create(ResourceName resourceName, String id, Map<String, Object> obj, boolean allowExisting) throws ResourceException {
        logger.debug("Invoking create configuration {} {} {}", new Object[] { resourceName.toString(), id, obj });
        if (id == null || id.length() == 0) {
            throw new BadRequestException("The passed identifier to create is null");
        }
        ParsedId parsedId = new ParsedId(resourceName, id);
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
            logger.warn("Invalid configuration provided for {}:" + ex.getMessage(), resourceName.toString(), ex);
            throw new BadRequestException("Invalid configuration provided for " + resourceName.toString() + ": " + ex.getMessage(), ex);
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
    public void handleUpdate(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        try {
            String rev = request.getRevision();
            JsonValue content = request.getContent();
            update(request.getResourceNameObject(), rev, content.asMap());
            // Create and send the ClusterEvent for the updated configuration 
            sendClusterEvent(ConfigAction.UPDATE, request.getResourceNameObject(), null, content.asMap());
            Resource resource = new Resource(request.getResourceName(), null, content);
            handler.handleResult(resource);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
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
     * @param resourceName the identifier of the resource to be updated
     * @param rev    the version of the object to update; or {@code null} if not provided.
     * @param obj    the contents of the object to put in the object set.
     * @throws ConflictException           if version is required but is {@code null}.
     * @throws ForbiddenException          if access to the object is forbidden.
     * @throws NotFoundException           if the specified object could not be found.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     * @throws BadRequestException         if the passed identifier is invalid
     */
    public void update(ResourceName resourceName, String rev, Map<String, Object> obj) throws ResourceException {
        logger.debug("Invoking update configuration {} {}", resourceName.toString(), rev);
        if (resourceName.isEmpty()) {
            throw new BadRequestException("The passed identifier to update is empty");
        }
        else if (resourceName.size() > 2) {
            throw new BadRequestException("The passed identifier to update has more than two parts");
        }
        
        ParsedId parsedId = new ParsedId(resourceName);
        try {    
            Configuration config = findExistingConfiguration(parsedId);

            Dictionary existingConfig = (config == null ? null : config.getProperties());
            if (existingConfig == null) {
                throw new NotFoundException("No existing configuration found for " + resourceName.toString() + ", can not update the configuration.");
            }
            
            existingConfig = configCrypto.encrypt(parsedId.getPidOrFactoryPid(), parsedId.instanceAlias, existingConfig, new JsonValue(obj));
            config.update(existingConfig);
            logger.debug("Updated existing configuration for {} with {}", resourceName.toString(), existingConfig);
        } catch (ResourceException ex) {
            throw ex;
        } catch (JsonValueException ex) {
            logger.warn("Invalid configuration provided for {}:" + ex.getMessage(), resourceName.toString(), ex);
            throw new BadRequestException("Invalid configuration provided for " + resourceName.toString() + ": " + ex.getMessage(), ex);
        } catch (WaitForMetaData ex) {
            logger.info("No meta-data provider available yet to update and encrypt configuration for {}, retry later.", parsedId.toString(), ex);
            throw new InternalServerErrorException("No meta-data provider available yet to create and encrypt configuration for "
                    + parsedId.toString() + ", retry later.", ex);
        } catch (Exception ex) {
            logger.warn("Failure to update configuration for {}", resourceName.toString(), ex);
            throw new InternalServerErrorException("Failure to update configuration for " + resourceName.toString() + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request,
            ResultHandler<Resource> handler) {
        try {
            String rev = request.getRevision();
            JsonValue result = delete(request.getResourceNameObject(), rev);
            // Create and send the ClusterEvent for the deleted configuration 
            sendClusterEvent(ConfigAction.DELETE, request.getResourceNameObject(), null, null);
            Resource resource = new Resource(request.getResourceName(), null, result);
            handler.handleResult(resource);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    /**
     * Deletes the specified object from the object set.
     *
     * @param resourceName the identifier of the resource to be deleted.
     * @param rev    the version of the object to delete or {@code null} if not provided.
     * @return the deleted object.
     * @throws NotFoundException           if the specified object could not be found.
     * @throws ForbiddenException          if access to the object is forbidden.
     * @throws ConflictException           if version is required but is {@code null}.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    public JsonValue delete(ResourceName resourceName, String rev) throws ResourceException {
        logger.debug("Invoking delete configuration {} {}", resourceName.toString(), rev);
        if (resourceName.isEmpty()) {
            throw new BadRequestException("The passed identifier to delete is null");
        }
        try {
            ParsedId parsedId = new ParsedId(resourceName);
            Configuration config = findExistingConfiguration(parsedId);
            if (config == null) {
                throw new NotFoundException("No existing configuration found for " + resourceName.toString() + ", can not delete the configuration.");
            }
            
            Dictionary existingConfig = config.getProperties();
            JSONEnhancedConfig enhancedConfig = new JSONEnhancedConfig();
            JsonValue value = enhancedConfig.getConfiguration(existingConfig, resourceName.toString(), false);

            if (existingConfig == null) {
                throw new NotFoundException("No existing configuration found for " + resourceName.toString() + ", can not delete the configuration.");
            }
            config.delete();
            logger.debug("Deleted configuration for {}", resourceName.toString());
            
            return value;
        } catch (ResourceException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.warn("Failure to delete configuration for {}", resourceName.toString(), ex);
            throw new InternalServerErrorException("Failure to delete configuration for " + resourceName.toString() + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void handleQuery(final ServerContext context, final QueryRequest request, final QueryResultHandler handler) {
        logger.debug("Invoking query");
        QueryRequest queryRequest = Requests.copyOfQueryRequest(request);
        QueryFilter filter = request.getQueryFilter();

        queryRequest.setResourceName("repo/config");
        if (filter != null) {
            queryRequest.setQueryFilter(asConfigQueryFilter(filter));
        }

        try {
            connectionFactory.getConnection().query(context, queryRequest, new QueryResultHandler() {
                
                @Override
                public void handleError(ResourceException error) {
                    handler.handleError(error);
                }

                @Override
                public boolean handleResource(Resource resource) {
                    JsonValue content = resource.getContent();
                    JsonValue config = content.get(CONFIG_KEY).copy();
                    String id = ConfigBootstrapHelper.getId(content.get(ConfigBootstrapHelper.CONFIG_ALIAS).asString(), 
                            content.get(ConfigBootstrapHelper.SERVICE_PID).asString(), 
                            content.get(ConfigBootstrapHelper.SERVICE_FACTORY_PID).asString());
                    handler.handleResource(new Resource(id, null, config));
                    return true;
                }

                @Override
                public void handleResult(QueryResult result) {
                    handler.handleResult(result);
                }
                
            });
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        final ResourceException e = new NotSupportedException("Action operations are not supported");
        handler.handleError(e);
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
                ResourceName resourceName = ResourceName.valueOf(details.get(EVENT_RESOURCE_NAME).asString());
                String id = details.get(EVENT_RESOURCE_ID).isNull() ? null : details.get(EVENT_RESOURCE_ID).asString();
                Map<String, Object> obj = details.get(EVENT_RESOURCE_OBJECT).isNull() ? null : details.get(EVENT_RESOURCE_OBJECT).asMap();
                switch (action) {
                case CREATE:
                    create(resourceName, id, obj, true);
                    break;
                case UPDATE:
                    update(resourceName, null, obj);
                    break;
                case DELETE:
                    delete(resourceName, null);
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
    private void sendClusterEvent(ConfigAction action, ResourceName name, String id, Map<String, Object> obj) {
        if (clusterManagementService != null && clusterManagementService.isEnabled()) {
            JsonValue details = json(object(
                    field(EVENT_RESOURCE_ACTION, action.toString()),
                    field(EVENT_RESOURCE_NAME, name.toString()),
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
    
    String getParsedId(String resourceName) throws ResourceException {
        return new ParsedId(ResourceName.valueOf(resourceName)).toString();
    }
    
    boolean isFactoryConfig(String resourceName) throws ResourceException {
        return new ParsedId(ResourceName.valueOf(resourceName)).isFactoryConfig();
    }
    
    static QueryFilter asConfigQueryFilter(QueryFilter filter) {
        return filter.accept(VISITOR, null);
    }
    
    /**
     * A {@link QueryFilterVisitor} implementation which modifies the {@link JsonPointer} fields by prepending them
     * with the appropriate key where the full config object is located.
     */
    private static class ConfigQueryFilterVisitor<P> implements QueryFilterVisitor<QueryFilter, P> {

        private final static String CONFIG_FACTORY_PID = "/" + ConfigBootstrapHelper.CONFIG_ALIAS;
        private final static String SERVICE_FACTORY_PID = "/" + ConfigBootstrapHelper.SERVICE_FACTORY_PID;
        private final static String SERVICE_PID = "/" + ConfigBootstrapHelper.SERVICE_PID;

        @Override
        public QueryFilter visitAndFilter(P parameter, List<QueryFilter> subFilters) {
            return QueryFilter.and(visitQueryFilters(subFilters));
        }

        @Override
        public QueryFilter visitBooleanLiteralFilter(P parameter, boolean value) {
            return value ? QueryFilter.alwaysTrue() : QueryFilter.alwaysFalse();
        }

        @Override
        public QueryFilter visitContainsFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.contains(getConfigPointer(field), valueAssertion);
        }

        @Override
        public QueryFilter visitEqualsFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.equalTo(getConfigPointer(field), valueAssertion);
        }

        @Override
        public QueryFilter visitExtendedMatchFilter(P parameter, JsonPointer field, String operator, Object valueAssertion) {
            return QueryFilter.comparisonFilter(getConfigPointer(field), operator, valueAssertion);
        }

        @Override
        public QueryFilter visitGreaterThanFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.greaterThan(getConfigPointer(field), valueAssertion);
        }

        @Override
        public QueryFilter visitGreaterThanOrEqualToFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.greaterThanOrEqualTo(getConfigPointer(field), valueAssertion);
        }

        @Override
        public QueryFilter visitLessThanFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.lessThan(getConfigPointer(field), valueAssertion);
        }

        @Override
        public QueryFilter visitLessThanOrEqualToFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.lessThanOrEqualTo(getConfigPointer(field), valueAssertion);
        }

        @Override
        public QueryFilter visitNotFilter(P parameter, QueryFilter subFilter) {
            return QueryFilter.not(subFilter.accept(new ConfigQueryFilterVisitor<Object>(), null));
        }

        @Override
        public QueryFilter visitOrFilter(P parameter, List<QueryFilter> subFilters) {
            return QueryFilter.or(visitQueryFilters(subFilters));
        }

        @Override
        public QueryFilter visitPresentFilter(P parameter, JsonPointer field) {
            return QueryFilter.present(getConfigPointer(field));
        }

        @Override
        public QueryFilter visitStartsWithFilter(P parameter, JsonPointer field, Object valueAssertion) {
            return QueryFilter.startsWith(getConfigPointer(field), valueAssertion);
        }
        
        /**
         * Visits each {@link QueryFilter} in a list of filters and returns a list of the
         * visited filters.
         * 
         * @param subFilters a list of the filters to visit
         * @return a list of visited filters
         */
        private List<QueryFilter> visitQueryFilters(List<QueryFilter> subFilters) {
            List<QueryFilter> visitedFilters = new ArrayList<QueryFilter>();
            for (QueryFilter filter : subFilters) {
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
    ParsedId getParsedId(ResourceName name, String id) throws BadRequestException {
        return new ParsedId(name, id);
    }

    /**
     * A class for converting resource names and IDs to qualified PIDs that represent managed services.
     */
    class ParsedId {

        public String pid;
        public String factoryPid;
        public String instanceAlias;

        public ParsedId(ResourceName resourceName) throws BadRequestException {
            // OSGi pid with spaces is disallowed; replace any spaces we get to be kind
            ResourceName stripped = resourceName.toString().contains(" ")
                    ? ResourceName.valueOf(resourceName.toString().replaceAll(" ", "_"))
                    : resourceName;

            switch (stripped.size()) {
                case 2:
                    factoryPid = stripped.parent().toString();
                    instanceAlias = stripped.leaf();
                    break;

                case 1:
                    pid = stripped.toString();
                    break;

                default:
                    throw new BadRequestException("The passed resourceName has more than two parts");
            }

            if (null != factoryPid) {
                logger.trace("Factory configuration pid: {} instance alias: {}", factoryPid, instanceAlias);
            } else {
                logger.trace("Managed service configuration pid: {}", pid);
            }
        }

        public ParsedId(ResourceName resourceName, String id) throws BadRequestException {
            if (resourceName.isEmpty()) {
                // single-instance config
                pid = id;
            } else {
                // multi-instance config
                factoryPid = resourceName.toString();
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
