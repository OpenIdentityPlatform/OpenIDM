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

package org.forgerock.openidm.repo.jdbc.impl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.patch.JsonPatch;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.osgi.OsgiName;
import org.forgerock.openidm.osgi.ServiceUtil;
import org.forgerock.openidm.repo.RepoBootService;
import org.forgerock.openidm.repo.jdbc.DatabaseType;
import org.forgerock.openidm.repo.jdbc.ErrorType;
import org.forgerock.openidm.repo.jdbc.TableHandler;
import org.forgerock.openidm.repo.jdbc.impl.pool.DataSourceFactory;
import org.forgerock.openidm.repo.jdbc.impl.query.TableQueries;
import org.forgerock.openidm.util.Accessor;
import org.forgerock.openidm.util.ResourceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository service implementation using JDBC
 *
 * @author aegloff
 * @author brmiller
 */
@Component(name = JDBCRepoService.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE,
        enabled = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Repository Service using JDBC"),
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/repo/*"),
    @Property(name = "db.type", value = "JDBC") })
public class JDBCRepoService implements RequestHandler, RepoBootService {

    final static Logger logger = LoggerFactory.getLogger(JDBCRepoService.class);

    public static final String PID = "org.forgerock.openidm.repo.jdbc";

    private static ServiceRegistration sharedDataSource = null;

    // Keys in the JSON configuration
    public static final String CONFIG_CONNECTION = "connection";
    public static final String CONFIG_JNDI_NAME = "jndiName";
    public static final String CONFIG_JTA_NAME = "jtaName";
    public static final String CONFIG_DB_TYPE = "dbType";
    public static final String CONFIG_DB_DRIVER = "driverClass";
    public static final String CONFIG_DB_URL = "jdbcUrl";
    public static final String CONFIG_USER = "username";
    public static final String CONFIG_PASSWORD = "password";
    public static final String CONFIG_DB_SCHEMA = "defaultCatalog";
    public static final String CONFIG_MAX_BATCH_SIZE = "maxBatchSize";

    private boolean useDataSource;
    private String jndiName;
    private DataSource ds;
    private String dbDriver;
    private String dbUrl;
    private String user;
    private String password;

    private int maxTxRetry = 5;

    Map<String, TableHandler> tableHandlers;
    TableHandler defaultTableHandler;

    final EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
    JsonValue config;

    @Override
    public void handleRead(ServerContext context, ReadRequest request,
            ResultHandler<Resource> handler) {
        try {
            handler.handleResult(read(request));
        } catch (final ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    @Override
    public Resource read(ReadRequest request) throws ResourceException {
        // Parse the remaining resourceName
        String fullId = request.getResourceName();
        String[] resourceName = ResourceUtil.parseResourceName(fullId);
        if (resourceName == null) {
            throw new BadRequestException(
                    "The repository requires clients to supply an identifier for the object to read.");
        }

        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);

        Connection connection = null;
        Resource result = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(true); // Ensure this does not get
                                            // transaction isolation handling
            TableHandler handler = getTableHandler(type);
            if (handler == null) {
                throw ResourceException.getException(ResourceException.INTERNAL_ERROR,
                        "No handler configured for resource type " + type);
            }
            result = handler.read(fullId, type, localId, connection);
        } catch (SQLException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("SQL Exception in read of {} with error code {}, sql state {}",
                        new Object[] { fullId, ex.getErrorCode(), ex.getSQLState(), ex });
            }
            throw new InternalServerErrorException("Reading object failed " + ex.getMessage(), ex);
        } catch (ResourceException ex) {
            logger.debug("ResourceException in read of {}", fullId, ex);
            throw ex;
        } catch (IOException ex) {
            logger.debug("IO Exception in read of {}", fullId, ex);
            throw new InternalServerErrorException("Conversion of read object failed", ex);
        } finally {
            CleanupHelper.loggedClose(connection);
        }

        return result;
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler) {
        try {
            handler.handleResult(create(request));
        } catch (final ResourceException e) {
        	e.printStackTrace();
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    @Override
    public Resource create(CreateRequest request) throws ResourceException {
    	// Parse the remaining resourceName
        String fullId = request.getResourceName();
        String newId = request.getNewResourceId();
        if (newId != null) {
        	fullId = request.getResourceName() + "/" + newId;
        }
        
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);
        
        if (localId == null) {
            throw new BadRequestException(
                    "The repository requires clients to supply an identifier for the object to read.");
        }

    	Map<String, Object> obj = request.getContent().asMap();

        Connection connection = null;
        boolean retry = false;
        int tryCount = 0;
        do {
            TableHandler handler = getTableHandler(type);
            if (handler == null) {
                throw ResourceException.getException(ResourceException.INTERNAL_ERROR,
                        "No handler configured for resource type " + type);
            }
            retry = false;
            ++tryCount;
            try {
                connection = getConnection();
                connection.setAutoCommit(false);

                handler.create(fullId, type, localId, obj, connection);

                connection.commit();
                logger.debug("Commited created object for id: {}", fullId);

            } catch (SQLException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("SQL Exception in create of {} with error code {}, sql state {}",
                            new Object[] { fullId, ex.getErrorCode(), ex.getSQLState(), ex });
                }
                rollback(connection);
                boolean alreadyExisted = handler.isErrorType(ex, ErrorType.DUPLICATE_KEY);
                if (alreadyExisted) {
                    throw new PreconditionFailedException(
                            "Create rejected as Object with same ID already exists and was detected. "
                                    + "(" + ex.getErrorCode() + "-" + ex.getSQLState() + ")"
                                    + ex.getMessage(), ex);
                }
                if (handler.isRetryable(ex, connection)) {
                    if (tryCount <= maxTxRetry) {
                        retry = true;
                        logger.debug("Retryable exception encountered, retry {}", ex.getMessage());
                    }
                }
                if (!retry) {
                    throw new InternalServerErrorException("Creating object failed " + "("
                            + ex.getErrorCode() + "-" + ex.getSQLState() + ")" + ex.getMessage(),
                            ex);
                }
            } catch (ResourceException ex) {
                logger.debug("ResourceException in create of {}", fullId, ex);
                rollback(connection);
                throw ex;
            } catch (java.io.IOException ex) {
                logger.debug("IO Exception in create of {}", fullId, ex);
                rollback(connection);
                throw new InternalServerErrorException("Conversion of object to create failed", ex);
            } catch (RuntimeException ex) {
                logger.debug("Runtime Exception in create of {}", fullId, ex);
                rollback(connection);
                throw new InternalServerErrorException(
                        "Creating object failed with unexpected failure: " + ex.getMessage(), ex);
            } finally {
                CleanupHelper.loggedClose(connection);
            }
        } while (retry);

        // Return the newly created resource
        return read(Requests.newReadRequest(fullId));

    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        try {
            handler.handleResult(update(request));
        } catch (final ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    @Override
    public Resource update(UpdateRequest request) throws ResourceException {
        // Parse the remaining resourceName
        String fullId = request.getResourceName();
        String[] resourceName = ResourceUtil.parseResourceName(fullId);
        if (resourceName == null) {
            throw new BadRequestException(
                    "The repository requires clients to supply an identifier for the object to read.");
        }

        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);
        Map<String, Object> obj = request.getNewContent().asMap();
        String rev = request.getRevision();

        Connection connection = null;
        Integer previousIsolationLevel = null;
        boolean retry = false;
        int tryCount = 0;
        do {
            TableHandler handler = getTableHandler(type);
            if (handler == null) {
                throw ResourceException.getException(ResourceException.INTERNAL_ERROR,
                        "No handler configured for resource type " + type);
            }
            retry = false;
            ++tryCount;
            try {
                connection = getConnection();
                previousIsolationLevel = new Integer(connection.getTransactionIsolation());
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                connection.setAutoCommit(false);

                handler.update(fullId, type, localId, rev, obj, connection);

                connection.commit();
                logger.debug("Commited updated object for id: {}", fullId);
            } catch (SQLException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("SQL Exception in update of {} with error code {}, sql state {}",
                            new Object[] { fullId, ex.getErrorCode(), ex.getSQLState(), ex });
                }
                rollback(connection);
                if (handler.isRetryable(ex, connection)) {
                    if (tryCount <= maxTxRetry) {
                        retry = true;
                        logger.debug("Retryable exception encountered, retry {}", ex.getMessage());
                    }
                }
                if (!retry) {
                    throw new InternalServerErrorException("Updating object failed "
                            + ex.getMessage(), ex);
                }
            } catch (ResourceException ex) {
                logger.debug("ResourceException in update of {}", fullId, ex);
                rollback(connection);
                throw ex;
            } catch (java.io.IOException ex) {
                logger.debug("IO Exception in update of {}", fullId, ex);
                rollback(connection);
                throw new InternalServerErrorException("Conversion of object to update failed", ex);
            } catch (RuntimeException ex) {
                logger.debug("Runtime Exception in update of {}", fullId, ex);
                rollback(connection);
                throw new InternalServerErrorException(
                        "Updating object failed with unexpected failure: " + ex.getMessage(), ex);
            } finally {
                if (connection != null) {
                    try {
                        if (previousIsolationLevel != null) {
                            connection.setTransactionIsolation(previousIsolationLevel.intValue());
                        }
                    } catch (SQLException ex) {
                        logger.warn("Failure in resetting connection isolation level ", ex);
                    }
                    CleanupHelper.loggedClose(connection);
                }
            }
        } while (retry);

        // Return the newly created resource
        return read(Requests.newReadRequest(fullId));
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request,
            ResultHandler<Resource> handler) {
        try {
            handler.handleResult(delete(request));
        } catch (final ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    @Override
    public Resource delete(DeleteRequest request) throws ResourceException {
        // Parse the remaining resourceName
        Resource result = null;
        String fullId = request.getResourceName();
        String[] resourceName = ResourceUtil.parseResourceName(fullId);
        if (resourceName == null) {
            throw new BadRequestException(
                    "The repository requires clients to supply an identifier for the object to read.");
        }

        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);
        String rev = request.getRevision();

        if (rev == null) {
            throw new ConflictException(
                    "Object passed into delete does not have revision it expects set.");
        }

        Connection connection = null;
        boolean retry = false;
        int tryCount = 0;
        do {
            TableHandler handler = getTableHandler(type);
            if (handler == null) {
                throw ResourceException.getException(ResourceException.INTERNAL_ERROR,
                        "No handler configured for resource type " + type);
            }

            retry = false;
            ++tryCount;
            try {
                connection = getConnection();
                connection.setAutoCommit(false);

                // Read in the resource before deleting
                result = handler.read(fullId, type, localId, connection);

                handler.delete(fullId, type, localId, rev, connection);

                connection.commit();
                logger.debug("Commited deleted object for id: {}", fullId);
            } catch (IOException ex) {
                logger.debug("IO Exception in delete of {}", fullId, ex);
                rollback(connection);
                throw new InternalServerErrorException("Deleting object failed " + ex.getMessage(),
                        ex);
            } catch (SQLException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("SQL Exception in delete of {} with error code {}, sql state {}",
                            new Object[] { fullId, ex.getErrorCode(), ex.getSQLState(), ex });
                }
                rollback(connection);
                if (handler.isRetryable(ex, connection)) {
                    if (tryCount <= maxTxRetry) {
                        retry = true;
                        logger.debug("Retryable exception encountered, retry {}", ex.getMessage());
                    }
                }
                if (!retry) {
                    throw new InternalServerErrorException("Deleting object failed "
                            + ex.getMessage(), ex);
                }
            } catch (ResourceException ex) {
                logger.debug("ResourceException in delete of {}", fullId, ex);
                rollback(connection);
                throw ex;
            } catch (RuntimeException ex) {
                logger.debug("Runtime Exception in delete of {}", fullId, ex);
                rollback(connection);
                throw new InternalServerErrorException(
                        "Deleting object failed with unexpected failure: " + ex.getMessage(), ex);
            } finally {
                CleanupHelper.loggedClose(connection);
            }
        } while (retry);

        return result;
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
        	List<Resource> results = query(request);
        	for (Resource result : results) {
        		handler.handleResource(result);
        	}
            handler.handleResult(new QueryResult());
        } catch (final ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    @Override
    public List<Resource> query(QueryRequest request) throws ResourceException {
    	String fullId = request.getResourceName();
        String type = fullId;
        logger.trace("Full id: {} Extracted type: {}", fullId, type);
        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(request.getAdditionalQueryParameters());
        params.put(TableQueries.QUERY_ID, request.getQueryId());
        params.put(TableQueries.QUERY_EXPRESSION, request.getQueryExpression());

        Connection connection = null;
        try {
            TableHandler tableHandler = getTableHandler(type);
            if (tableHandler == null) {
                throw ResourceException.getException(ResourceException.INTERNAL_ERROR,
                        "No handler configured for resource type " + type);
            }
            connection = getConnection();
            connection.setAutoCommit(true); // Ensure we do not implicitly
                                            // start transaction isolation

            List<Map<String, Object>> docs = tableHandler.query(type, params, connection);
            List<Resource> results = new ArrayList<Resource>();
            for (Map<String, Object> resultMap : docs) {
                String id = (String) resultMap.get("_id");
                String rev = (String) resultMap.get("_rev");
                JsonValue value = new JsonValue(resultMap);
                Resource resultResource = new Resource(id, rev, value);
                results.add(resultResource);
            }
            return results;
        } catch (SQLException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("SQL Exception in query of {} with error code {}, sql state {}",
                        new Object[] { fullId, ex.getErrorCode(), ex.getSQLState(), ex });
            }
            throw new InternalServerErrorException("Querying failed: " + ex.getMessage(), ex);
        } catch (ResourceException ex) {
            logger.debug("ResourceException in query of {}", fullId, ex);
            throw ex;
        } finally {
            CleanupHelper.loggedClose(connection);
        }
    }
    
    @Override
    public void handleAction(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Action operations are not supported");
        handler.handleError(e);
    }

    // Utility method to cleanly roll back including logging
    private void rollback(Connection connection) {
        if (connection != null) {
            try {
                logger.debug("Rolling back transaction.");
                connection.rollback();
            } catch (SQLException ex) {
                logger.warn("Rolling back transaction reported failure ", ex);
            }
        }
    }
    
    private String trimStartingSlash(String id) {
    	if (id.startsWith("/") && id.length() > 1) {
    		return id.substring(1);
    	}
    	return id;
    }

    // TODO: replace with common utility to handle ID, this is temporary
    private String getLocalId(String id) {
    	String tmpId = trimStartingSlash(id);
        String localId = null;
        int lastSlashPos = tmpId.lastIndexOf("/");
        if (lastSlashPos > -1) {
            localId = tmpId.substring(tmpId.lastIndexOf("/") + 1);
        }
        logger.trace("Full id: {} Extracted local id: {}", tmpId, localId);
        return localId;
    }

    // TODO: replace with common utility to handle ID, this is temporary
    private String getObjectType(String id) {
    	String tmpId = trimStartingSlash(id);
        String type = null;
        int lastSlashPos = tmpId.lastIndexOf("/");
        if (lastSlashPos > -1) {
            int startPos = 0;
            // This should not be necessary as relative URI should not start
            // with slash
            if (tmpId.startsWith("/")) {
                startPos = 1;
            }
            type = tmpId.substring(startPos, lastSlashPos);
            logger.trace("Full id: {} Extracted type: {}", tmpId, type);
        }
        return type;
    }

    Connection getConnection() throws SQLException {
        if (useDataSource) {
            return ds.getConnection();
        } else {
            return DriverManager.getConnection(dbUrl, user, password);
        }
    }

    TableHandler getTableHandler(String type) {
        TableHandler handler = tableHandlers.get(type);
        if (handler != null) {
            return handler;
        } else {
            handler = defaultTableHandler;
            for (String key : tableHandlers.keySet()) {
                if (type.startsWith(key)) {
                    handler = tableHandlers.get(key);
                    logger.debug("Use table handler configured for {} for type {} ", key, type);
                }
            }
            // For future lookups remember the handler determined for this
            // specific type
            tableHandlers.put(type, handler);
            return handler;
        }
    }

    /**
     * Populate and return a repository service that knows how to query and
     * manipulate configuration.
     *
     * @param repoConfig
     *            the bootstrap configuration
     * @param context
     * @return the boot repository service. This newBuilder is not managed by
     *         SCR and needs to be manually registered.
     */
    static RepoBootService getRepoBootService(JsonValue repoConfig, BundleContext context) { 
    	JDBCRepoService bootRepo = new JDBCRepoService(); 
    	bootRepo.init(repoConfig, context); 
    	return bootRepo; 
    }

    /**
     * Activates the JDBC Repository Service
     *
     * @param compContext
     *            The component context
     */
    @Activate
    void activate(ComponentContext compContext) {
    	logger.debug("Activating Service with configuration {}", compContext.getProperties());
    	try {
    		config = enhancedConfig.getConfigurationAsJson(compContext);
    	} catch (RuntimeException ex) {
    		logger.warn("Configuration invalid and could not be parsed, can not start JDBC repository: "
    				+ ex.getMessage(), ex);
    		throw ex;
    	}
    	init(config, compContext.getBundleContext());
    	logger.info("Repository started.");
    }

    /**
     * Deactivates the JDBC Repository Service
     *
     * @param compContext
     *            the component context
     */
    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext);
        logger.info("Repository stopped.");
    }

    /**
     * Handles configuration updates without interrupting the service
     *
     * @param compContext
     *            the component context
     */
    @Modified
    void modified(ComponentContext compContext) throws Exception {
        logger.debug("Reconfiguring the JDBC Repository Service with configuration {}", compContext
                .getProperties());
        try {
            JsonValue newConfig = enhancedConfig.getConfigurationAsJson(compContext);
            if (hasConfigChanged(config, newConfig)) {
                deactivate(compContext);
                activate(compContext);
                logger.info("Reconfigured the JDBC Repository Service {}", compContext
                        .getProperties());
            }
        } catch (Exception ex) {
            logger.warn("Configuration invalid, can not reconfigure the JDBC Repository Service.",
                    ex);
            throw ex;
        }
    }

    /**
     * Compares the current configuration with a new configuration to determine
     * if the configuration has changed
     *
     * @param existingConfig
     *            the current configuration object
     * @param newConfig
     *            the new configuration object
     * @return true if the configurations differ, false otherwise
     */
    private boolean hasConfigChanged(JsonValue existingConfig, JsonValue newConfig) {
        return JsonPatch.diff(existingConfig, newConfig).size() > 0;
    }

    /**
     * Initializes the JDBC Repository Service with the supplied configuration
     *
     * @param config
     *            the configuration object
     * @param bundleContext
     *            the bundle context
     * @throws InvalidException
     */
    void init(JsonValue config, BundleContext bundleContext) throws InvalidException {
        try {
            String enabled = config.get("enabled").asString();
            if ("false".equals(enabled)) {
                logger.debug("JDBC repository not enabled");
                throw new RuntimeException("JDBC repository not enabled.");
            }

            JsonValue connectionConfig =
                    config.get(CONFIG_CONNECTION).isNull() ? config : config.get(CONFIG_CONNECTION);

            maxTxRetry = connectionConfig.get("maxTxRetry").defaultTo(5).asInteger().intValue();

            // Data Source configuration
            jndiName = connectionConfig.get(CONFIG_JNDI_NAME).asString();
            String jtaName = connectionConfig.get(CONFIG_JTA_NAME).asString();
            if (jndiName != null && jndiName.trim().length() > 0) {
                // Get DB connection via JNDI
                logger.info("Using DB connection configured via Driver Manager");
                InitialContext ctx = null;
                try {
                    ctx = new InitialContext();
                } catch (NamingException ex) {
                    logger.warn("Getting JNDI initial context failed: " + ex.getMessage(), ex);
                }
                if (ctx == null) {
                    throw new InvalidException(
                            "Current platform context does not support lookup of repository DB via JNDI. "
                                    + " Configure DB initialization via direct " + CONFIG_DB_DRIVER
                                    + " configuration instead.");
                }

                useDataSource = true;
                ds = (DataSource) ctx.lookup(jndiName); // e.g.
                                                        // "java:comp/env/jdbc/MySQLDB"
            } else if (!StringUtils.isBlank(jtaName)) {
                // e.g.
                // osgi:service/javax.sql.DataSource/(osgi.jndi.service.name=jdbc/openidm)
                OsgiName lookupName = OsgiName.parse(jtaName);
                Object service = ServiceUtil.getService(bundleContext, lookupName, null, true);
                if (service instanceof DataSource) {
                    useDataSource = true;
                    ds = (DataSource) service;
                } else {
                    throw new RuntimeException("DataSource can not be retrieved for: " + jtaName);
                }
            } else {
                // Get DB Connection via Driver Manager
                dbDriver = connectionConfig.get(CONFIG_DB_DRIVER).asString();
                if (dbDriver == null || dbDriver.trim().length() == 0) {
                    throw new InvalidException("Either a JNDI name (" + CONFIG_JNDI_NAME + "), "
                            + "or a DB driver lookup (" + CONFIG_DB_DRIVER
                            + ") needs to be configured to connect to a DB.");
                }
                dbUrl = connectionConfig.get(CONFIG_DB_URL).required().asString();
                user = connectionConfig.get(CONFIG_USER).required().asString();
                password = connectionConfig.get(CONFIG_PASSWORD).defaultTo("").asString();
                logger.info(
                        "Using DB connection configured via Driver Manager with Driver {} and URL",
                        dbDriver, dbUrl);
                try {
                    Class.forName(dbDriver);
                } catch (ClassNotFoundException ex) {
                    logger.error("Could not find configured database driver " + dbDriver
                            + " to start repository ", ex);
                    throw new InvalidException("Could not find configured database driver "
                            + dbDriver + " to start repository ", ex);
                }
                Boolean enableConnectionPool =
                        connectionConfig.get("enableConnectionPool").defaultTo(Boolean.FALSE)
                                .asBoolean();
                if (null == sharedDataSource) {
                    Dictionary<String, String> serviceParams = new Hashtable<String, String>(1);
                    serviceParams.put("osgi.jndi.service.name", "jdbc/openidm");
                    sharedDataSource =
                            bundleContext.registerService(DataSource.class.getName(),
                                    DataSourceFactory.newInstance(connectionConfig), serviceParams);
                }
                if (enableConnectionPool) {
                    ds = DataSourceFactory.newInstance(connectionConfig);
                    useDataSource = true;
                    logger.info("DataSource connection pool enabled.");
                } else {
                    logger.info("No DataSource connection pool enabled.");
                }
            }

            // Table handling configuration
            String dbSchemaName = connectionConfig.get(CONFIG_DB_SCHEMA).defaultTo(null).asString();
            JsonValue genericQueries = config.get("queries").get("genericTables");
            int maxBatchSize =
                    connectionConfig.get(CONFIG_MAX_BATCH_SIZE).defaultTo(100).asInteger();

            tableHandlers = new HashMap<String, TableHandler>();
            // TODO Make safe the database type detection
            DatabaseType databaseType =
                    DatabaseType.valueOf(connectionConfig.get(CONFIG_DB_TYPE).defaultTo(
                            DatabaseType.ANSI_SQL99.name()).asString());

            JsonValue defaultMapping = config.get("resourceMapping").get("default");
            if (!defaultMapping.isNull()) {
                defaultTableHandler =
                        getGenericTableHandler(databaseType, defaultMapping, dbSchemaName,
                                genericQueries, maxBatchSize);
                logger.debug("Using default table handler: {}", defaultTableHandler);
            } else {
                logger.warn("No default table handler configured");
            }

            // Default the configuration table for bootstrap
            JsonValue defaultTableProps = new JsonValue(new HashMap());
            defaultTableProps.put("mainTable", "configobjects");
            defaultTableProps.put("propertiesTable", "configobjectproperties");
            defaultTableProps.put("searchableDefault", Boolean.FALSE);
            GenericTableHandler defaultConfigHandler =
                    getGenericTableHandler(databaseType, defaultTableProps, dbSchemaName,
                            genericQueries, 1);
            tableHandlers.put("config", defaultConfigHandler);

            JsonValue genericMapping = config.get("resourceMapping").get("genericMapping");
            if (!genericMapping.isNull()) {
                for (String key : genericMapping.keys()) {
                    JsonValue value = genericMapping.get(key);
                    if (key.endsWith("/*")) {
                        // For matching purposes strip the wildcard at the end
                        key = key.substring(0, key.length() - 1);
                    }
                    TableHandler handler =
                            getGenericTableHandler(databaseType, value, dbSchemaName,
                                    genericQueries, maxBatchSize);

                    tableHandlers.put(key, handler);
                    logger.debug("For pattern {} added handler: {}", key, handler);
                }
            }

            JsonValue explicitQueries = config.get("queries").get("explicitTables");
            JsonValue explicitMapping = config.get("resourceMapping").get("explicitMapping");
            if (!explicitMapping.isNull()) {
                for (Object keyObj : explicitMapping.keys()) {
                    JsonValue value = explicitMapping.get((String) keyObj);
                    String key = (String) keyObj;
                    if (key.endsWith("/*")) {
                        // For matching purposes strip the wildcard at the end
                        key = key.substring(0, key.length() - 1);
                    }
                    TableHandler handler =
                            getMappedTableHandler(databaseType, value, value.get("table")
                                    .required().asString(), value.get("objectToColumn").required()
                                    .asMap(), dbSchemaName, explicitQueries, maxBatchSize);

                    tableHandlers.put(key, handler);
                    logger.debug("For pattern {} added handler: {}", key, handler);
                }
            }

        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid, can not start JDBC repository.", ex);
            throw new InvalidException("Configuration invalid, can not start JDBC repository.", ex);
        } catch (NamingException ex) {
            throw new InvalidException("Could not find configured jndiName " + jndiName
                    + " to start repository ", ex);
        } catch (InternalServerErrorException ex) {
            throw new InvalidException(
                    "Could not initialize mapped table handler, can not start JDBC repository.", ex);
        }

        Connection testConn = null;
        try {
            // Check if we can get a connection
            testConn = getConnection();
            testConn.setAutoCommit(true); // Ensure we do not implicitly start
                                          // transaction isolation
        } catch (Exception ex) {
            logger.warn(
                    "JDBC Repository start-up experienced a failure getting a DB connection: "
                            + ex.getMessage()
                            + ". If this is not temporary or resolved, Repository operation will be affected.",
                    ex);
        } finally {
            if (testConn != null) {
                try {
                    testConn.close();
                } catch (SQLException ex) {
                    logger.warn("Failure during test connection close ", ex);
                }
            }
        }
    }

    GenericTableHandler getGenericTableHandler(DatabaseType databaseType, JsonValue tableConfig,
            String dbSchemaName, JsonValue queries, int maxBatchSize) {

        GenericTableHandler handler = null;

        // TODO: make pluggable
        switch (databaseType) {
        case DB2:
            handler =
                    new DB2TableHandler(tableConfig, dbSchemaName, queries, maxBatchSize,
                            new DB2SQLExceptionHandler());
            break;
        case ORACLE:
            handler =
                    new OracleTableHandler(tableConfig, dbSchemaName, queries, maxBatchSize,
                            new DefaultSQLExceptionHandler());
            break;
        case POSTGRESQL:
            handler =
                    new PostgreSQLTableHandler(tableConfig, dbSchemaName, queries, maxBatchSize,
                            new DefaultSQLExceptionHandler());
            break;
        case MYSQL:
            handler =
                    new GenericTableHandler(tableConfig, dbSchemaName, queries, maxBatchSize,
                            new MySQLExceptionHandler());
            break;
        case SQLSERVER:
            handler =
                    new MSSQLTableHandler(tableConfig, dbSchemaName, queries, maxBatchSize,
                            new DefaultSQLExceptionHandler());
            break;
        case H2:
            handler =
                    new H2TableHandler(tableConfig, dbSchemaName, queries, maxBatchSize,
                            new DefaultSQLExceptionHandler());
            break;

        default:
            handler =
                    new GenericTableHandler(tableConfig, dbSchemaName, queries, maxBatchSize,
                            new DefaultSQLExceptionHandler());
        }
        return handler;
    }

    MappedTableHandler getMappedTableHandler(DatabaseType databaseType, JsonValue tableConfig,
            String table, Map objectToColumn, String dbSchemaName, JsonValue explicitQueries,
            int maxBatchSize) throws InternalServerErrorException {

        final Accessor<CryptoService> cryptoServiceAccessor = new Accessor<CryptoService>() {
            public CryptoService access() {
                return null;
            }
        };

        MappedTableHandler handler = null;

        // TODO: make pluggable
        switch (databaseType) {
        case DB2:
            handler =
                    new MappedTableHandler(table, objectToColumn, dbSchemaName, explicitQueries,
                            new DB2SQLExceptionHandler(), cryptoServiceAccessor);
            break;
        case ORACLE:
            handler =
                    new MappedTableHandler(table, objectToColumn, dbSchemaName, explicitQueries,
                            new DefaultSQLExceptionHandler(), cryptoServiceAccessor);
            break;
        case POSTGRESQL:
            handler =
                    new MappedTableHandler(table, objectToColumn, dbSchemaName, explicitQueries,
                            new DefaultSQLExceptionHandler(), cryptoServiceAccessor);
            break;
        case MYSQL:
            handler =
                    new MappedTableHandler(table, objectToColumn, dbSchemaName, explicitQueries,
                            new MySQLExceptionHandler(), cryptoServiceAccessor);
            break;
        case SQLSERVER:
            handler =
                    new MSSQLMappedTableHandler(table, objectToColumn, dbSchemaName,
                            explicitQueries, new DefaultSQLExceptionHandler(),
                            cryptoServiceAccessor);
            break;
        default:
            handler =
                    new MappedTableHandler(table, objectToColumn, dbSchemaName, explicitQueries,
                            new DefaultSQLExceptionHandler(), cryptoServiceAccessor);
        }
        return handler;
    }
}
