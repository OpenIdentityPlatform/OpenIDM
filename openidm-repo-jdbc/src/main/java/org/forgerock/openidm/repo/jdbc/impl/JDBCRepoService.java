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
 */
package org.forgerock.openidm.repo.jdbc.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.objset.*;
import org.forgerock.openidm.osgi.OsgiName;
import org.forgerock.openidm.osgi.ServiceUtil;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.RepoBootService;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.repo.jdbc.DatabaseType;
import org.forgerock.openidm.repo.jdbc.TableHandler;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository service implementation using JDBC
 *
 * @author aegloff
 */
@Component(name = "org.forgerock.openidm.repo.jdbc", immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service(value = {RepositoryService.class, ObjectSet.class})
// Omit the RepoBootService interface from the managed service
@Properties({
        @Property(name = "service.description", value = "Repository Service using JDBC"),
        @Property(name = "service.vendor", value = "ForgeRock AS"),
        @Property(name = "openidm.router.prefix", value = "repo"),
        @Property(name = "db.type", value = "JDBC")
})
public class JDBCRepoService implements RepositoryService, RepoBootService {

    final static Logger logger = LoggerFactory.getLogger(JDBCRepoService.class);

    ObjectMapper mapper = new ObjectMapper();

    // Keys in the JSON configuration
    public static final String CONFIG_JNDI_NAME = "jndiName";
    public static final String CONFIG_JTA_NAME = "jtaName";
    public static final String CONFIG_DB_TYPE = "dbType";
    public static final String CONFIG_DB_DRIVER = "dbDriver";
    public static final String CONFIG_DB_URL = "dbUrl";
    public static final String CONFIG_USER = "user";
    public static final String CONFIG_PASSWORD = "password";
    public static final String CONFIG_DB_SCHEMA = "dbSchema";
    public static final String CONFIG_MAX_BATCH_SIZE = "maxBatchSize";

    private boolean useDataSource;
    private String jndiName;
    private String jtaName;
    private DataSource ds;
    private String dbDriver;
    private String dbUrl;
    private String user;
    private String password;

    Map<String, TableHandler> tableHandlers;
    TableHandler defaultTableHandler;

    EnhancedConfig enhancedConfig = new JSONEnhancedConfig();

    /**
     * Gets an object from the repository by identifier. The returned object is not validated
     * against the current schema and may need processing to conform to an updated schema.
     * <p/>
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency supported by OrientDB and OpenIDM.
     *
     * @param fullId the identifier of the object to retrieve from the object set.
     * @return the requested object.
     * @throws NotFoundException   if the specified object could not be found.
     * @throws ForbiddenException  if access to the object is forbidden.
     * @throws BadRequestException if the passed identifier is invalid
     */
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);

        if (fullId == null || localId == null) {
            throw new NotFoundException("The repository requires clients to supply an identifier for the object to create. Full identifier: "
                    + fullId + " local identifier: " + localId);
        } else if (type == null) {
            throw new NotFoundException("The object identifier did not include sufficient information to determine the object type: " + fullId);
        }

        Connection connection = null;
        Map<String, Object> result = null;
        try {
            connection = getConnection();
            TableHandler handler = getTableHandler(type);
            result = handler.read(fullId, type, localId, connection);

            connection.close();
        } catch (SQLException ex) {
            throw new InternalServerErrorException("Reading object failed " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new InternalServerErrorException("Conversion of read object failed", ex);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    logger.warn("Failure during connection close ", ex);
                }
            }
        }

        return result;
    }

    /**
     * Creates a new object in the object set.
     * <p/>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
     *
     * @param fullId the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param obj    the contents of the object to create in the object set.
     * @throws NotFoundException           if the specified id could not be resolved.
     * @throws ForbiddenException          if access to the object or object set is forbidden.
     * @throws PreconditionFailedException if an object with the same ID already exists.
     */
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);

        if (fullId == null || localId == null) {
            throw new NotFoundException("The repository requires clients to supply an identifier for the object to create. Full identifier: "
                    + fullId + " local identifier: " + localId);
        } else if (type == null) {
            throw new NotFoundException("The object identifier did not include sufficient information to determine the object type: " + fullId);
        }

        Connection connection = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            getTableHandler(type).create(fullId, type, localId, obj, connection);

            connection.commit();
            logger.debug("Commited created object for id: {}", fullId);

        } catch (SQLException ex) {
            rollback(connection);
            // TODO: detect duplicate inserts with appropriate exception rather than generic

            //if (isCauseIndexException()) {
            //    throw new PreconditionFailedException("Create rejected as Object with same ID already exists and was detected. " 
            //            + ex.getMessage(), ex);
            //} else {
            throw new InternalServerErrorException("Creating object failed " + ex.getMessage(), ex);
            //}
        } catch (java.io.IOException ex) {
            rollback(connection);
            throw new InternalServerErrorException("Conversion of object to create failed", ex);
        } catch (RuntimeException ex) {
            rollback(connection);
            throw new InternalServerErrorException("Creating object failed with unexpected failure: " + ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    logger.warn("Failure during connection close ", ex);
                }
            }
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
     * @param fullId the identifier of the object to be put, or {@code null} to request a generated identifier.
     * @param rev    the version of the object to update; or {@code null} if not provided.
     * @param obj    the contents of the object to put in the object set.
     * @throws ConflictException           if version is required but is {@code null}.
     * @throws ForbiddenException          if access to the object is forbidden.
     * @throws NotFoundException           if the specified object could not be found.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     * @throws BadRequestException         if the passed identifier is invalid
     */
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {

        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);

        if (rev == null) {
            throw new ConflictException("Object passed into update does not have revision it expects set.");
        }

        Connection connection = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            getTableHandler(type).update(fullId, type, localId, rev, obj, connection);

            connection.commit();
            logger.debug("Commited updated object for id: {}", fullId);
        } catch (SQLException ex) {
            rollback(connection);
            throw new InternalServerErrorException("Updating object failed " + ex.getMessage(), ex);
        } catch (java.io.IOException ex) {
            rollback(connection);
            throw new InternalServerErrorException("Conversion of object to update failed", ex);
        } catch (RuntimeException ex) {
            rollback(connection);
            throw new InternalServerErrorException("Updating object failed with unexpected failure: " + ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    logger.warn("Failure during connection close ", ex);
                }
            }
        }
    }

    /**
     * Deletes the specified object from the object set.
     *
     * @param fullId the identifier of the object to be deleted.
     * @param rev    the version of the object to delete or {@code null} if not provided.
     * @throws NotFoundException           if the specified object could not be found.
     * @throws ForbiddenException          if access to the object is forbidden.
     * @throws ConflictException           if version is required but is {@code null}.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    public void delete(String fullId, String rev) throws ObjectSetException {

        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);

        if (rev == null) {
            throw new ConflictException("Object passed into delete does not have revision it expects set.");
        }

        Connection connection = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            getTableHandler(type).delete(fullId, type, localId, rev, connection);

            connection.commit();
            logger.debug("Commited deleted object for id: {}", fullId);
        } catch (IOException ex) {
            rollback(connection);
            throw new InternalServerErrorException("Deleting object failed " + ex.getMessage(), ex);
        } catch (SQLException ex) {
            rollback(connection);
            throw new InternalServerErrorException("Deleting object failed " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            rollback(connection);
            throw new InternalServerErrorException("Deleting object failed with unexpected failure: " + ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    logger.warn("Failure during connection close ", ex);
                }
            }
        }
    }

    /**
     * Currently not supported by this implementation.
     * <p/>
     * Applies a patch (partial change) to the specified object in the object set.
     *
     * @param id    the identifier of the object to be patched.
     * @param rev   the version of the object to patch or {@code null} if not provided.
     * @param patch the partial change to apply to the object.
     * @throws ConflictException           if patch could not be applied object state or if version is required.
     * @throws ForbiddenException          if access to the object is forbidden.
     * @throws NotFoundException           if the specified object could not be found.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new UnsupportedOperationException();
    }

    /**
     * Performs the query on the specified object and returns the associated results.
     * <p/>
     * Queries are parametric; a set of named parameters is provided as the query criteria.
     * The query result is a JSON object structure composed of basic Java types.
     * <p/>
     * The returned map is structured as follow:
     * - The top level map contains meta-data about the query, plus an entry with the actual result records.
     * - The <code>QueryConstants</code> defines the map keys, including the result records (QUERY_RESULT)
     *
     * @param fullId identifies the object to query.
     * @param params the parameters of the query to perform.
     * @return the query results, which includes meta-data and the result records in JSON object structure format.
     * @throws NotFoundException   if the specified object could not be found.
     * @throws BadRequestException if the specified params contain invalid arguments, e.g. a query id that is not
     *                             configured, a query expression that is invalid, or missing query substitution tokens.
     * @throws ForbiddenException  if access to the object or specified query is forbidden.
     */
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        // TODO: replace with common utility
        String type = fullId;
        logger.trace("Full id: {} Extracted type: {}", fullId, type);

        Map<String, Object> result = new HashMap<String, Object>();
        Connection connection = null;
        try {
            connection = getConnection();
            long start = System.currentTimeMillis();
            List<Map<String, Object>> docs = getTableHandler(type).query(type, params, connection);
            long end = System.currentTimeMillis();
            result.put(QueryConstants.QUERY_RESULT, docs);
            // TODO: split out conversion time 
            //result.put(QueryConstants.STATISTICS_CONVERSION_TIME, Long.valueOf(convEnd-convStart));

            result.put(QueryConstants.STATISTICS_QUERY_TIME, Long.valueOf(end - start));

            if (logger.isDebugEnabled()) {
                logger.debug("Query result contains {} records, took {} ms and took {} ms to convert result.",
                        new Object[]{((List) result.get(QueryConstants.QUERY_RESULT)).size(),
                                result.get(QueryConstants.STATISTICS_QUERY_TIME),
                                result.get(QueryConstants.STATISTICS_CONVERSION_TIME)});
            }
        } catch (SQLException ex) {
            throw new InternalServerErrorException("Querying failed: " + ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    logger.warn("Failure during connection close", ex);
                }
            }
        }

        return result;

    }

    public Map<String, Object> action(String fullId, Map<String, Object> params) throws ObjectSetException {
        throw new UnsupportedOperationException("JDBC repository does not support action");
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

    // TODO: replace with common utility to handle ID, this is temporary
    private String getLocalId(String id) {
        String localId = null;
        int lastSlashPos = id.lastIndexOf("/");
        if (lastSlashPos > -1) {
            localId = id.substring(id.lastIndexOf("/") + 1);
        }
        logger.trace("Full id: {} Extracted local id: {}", id, localId);
        return localId;
    }

    // TODO: replace with common utility to handle ID, this is temporary
    private String getObjectType(String id) {
        String type = null;
        int lastSlashPos = id.lastIndexOf("/");
        if (lastSlashPos > -1) {
            int startPos = 0;
            // This should not be necessary as relative URI should not start with slash
            if (id.startsWith("/")) {
                startPos = 1;
            }
            type = id.substring(startPos, lastSlashPos);
            logger.trace("Full id: {} Extracted type: {}", id, type);
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
            // For future lookups remember the handler determined for this specific type
            tableHandlers.put(type, handler);
            return handler;
        }
    }

    /**
     * Populate and return a repository service that knows how to query and maniuplate configuration.
     *
     * @param repoConfig the bootstrap configuration
     * @return the boot repository service. This instance is not managed by SCR and needs to be manually registered.
     */
    static RepoBootService getRepoBootService(Map repoConfig, BundleContext context) {
        JDBCRepoService bootRepo = new JDBCRepoService();
        JsonNode cfg = new JsonNode(repoConfig);
        bootRepo.init(cfg, context);
        return bootRepo;
    }

    @Activate
    void activate(ComponentContext compContext) {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        JsonNode config = null;
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

    void init(JsonNode config, BundleContext bundleContext) throws InvalidException {
        try {
            String enabled = config.get("enabled").asString();
            if ("false".equals(enabled)) {
                logger.debug("JDBC repository not enabled");
                throw new RuntimeException("JDBC repository not enabled.");
            }

            // Data Source configuration
            jndiName = config.get(CONFIG_JNDI_NAME).asString();
            jtaName = config.get(CONFIG_JTA_NAME).asString();
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
                    throw new InvalidException("Current platform context does not support lookup of repository DB via JNDI. "
                            + " Configure DB initialization via direct " + CONFIG_DB_DRIVER + " configuration instead.");
                }

                useDataSource = true;
                ds = (DataSource) ctx.lookup(jndiName); // e.g. "java:comp/env/jdbc/MySQLDB"
            } else if (!StringUtils.isBlank(jtaName)) {
                // e.g. osgi:service/javax.sql.DataSource/(osgi.jndi.service.name=jdbc/openidm)
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
                dbDriver = config.get(CONFIG_DB_DRIVER).asString();
                if (dbDriver == null || dbDriver.trim().length() == 0) {
                    throw new InvalidException("Either a JNDI name (" + CONFIG_JNDI_NAME + "), " +
                            "or a DB driver lookup (" + CONFIG_DB_DRIVER + ") needs to be configured to connect to a DB.");
                }
                dbUrl = config.get(CONFIG_DB_URL).required().asString();
                user = config.get(CONFIG_USER).required().asString();
                password = config.get(CONFIG_PASSWORD).defaultTo("").asString();
                logger.info("Using DB connection configured via Driver Manager with Driver {} and URL", dbDriver, dbUrl);
                try {
                    Class.forName(dbDriver);
                } catch (ClassNotFoundException ex) {
                    logger.error("Could not find configured database driver " + dbDriver + " to start repository ", ex);
                    throw new InvalidException("Could not find configured database driver "
                            + dbDriver + " to start repository ", ex);
                }
            }

            // Table handling configuration
            String dbSchemaName = config.get(CONFIG_DB_SCHEMA).defaultTo("openidm").asString();
            JsonNode genericQueries = config.get("queries").get("genericTables");
            int maxBatchSize = config.get(CONFIG_MAX_BATCH_SIZE).defaultTo(100).asInteger();

            tableHandlers = new HashMap<String, TableHandler>();
            JsonNode defaultMapping = config.get("resourceMapping").get("default");
            String defaultMainTable = defaultMapping.get("mainTable").defaultTo("genericobjects").asString();
            String defaultPropTable = defaultMapping.get("propertiesTable").defaultTo("genericobjectproperties").asString();

            //TODO Make safe the database type detection
            DatabaseType databaseType = DatabaseType.valueOf(config.get(CONFIG_DB_TYPE).defaultTo(DatabaseType.ANSI_SQL99.name()).asString());

            defaultTableHandler = getGenericTableHandler(databaseType, defaultMainTable, defaultPropTable, dbSchemaName, genericQueries, maxBatchSize);

            logger.debug("Using default table handler: {}", defaultTableHandler);

            // Default the configuration table for bootstrap
            GenericTableHandler defaultConfigHandler = getGenericTableHandler(
                    databaseType,
                    "configobjects",
                    "configobjectproperties",
                    dbSchemaName,
                    genericQueries,
                    1);
            tableHandlers.put("config", defaultConfigHandler);

            JsonNode genericMapping = config.get("resourceMapping").get("genericMapping");
            if (!genericMapping.isNull()) {
                for (String key : genericMapping.keys()) {
                    JsonNode value = genericMapping.get(key);
                    if (key.endsWith("/*")) {
                        // For matching purposes strip the wildcard at the end
                        key = key.substring(0, key.length() - 1);
                    }
                    TableHandler handler = getGenericTableHandler(
                            databaseType,
                            value.get("mainTable").required().asString(),
                            value.get("propertiesTable").required().asString(),
                            dbSchemaName,
                            genericQueries,
                            maxBatchSize);

                    tableHandlers.put(key, handler);
                    logger.debug("For pattern {} added handler: {}", key, handler);
                }
            }

            JsonNode explicitQueries = config.get("queries").get("explicitTables");
            JsonNode explicitMapping = config.get("resourceMapping").get("explicitMapping");
            if (!explicitMapping.isNull()) {
                for (Object keyObj : explicitMapping.keys()) {
                    JsonNode value = explicitMapping.get((String) keyObj);
                    String key = (String) keyObj;
                    if (key.endsWith("/*")) {
                        // For matching purposes strip the wildcard at the end
                        key = key.substring(0, key.length() - 1);
                    }
                    TableHandler handler = new MappedTableHandler(
                            value.get("table").required().asString(),
                            value.get("objectToColumn").required().asMap(),
                            dbSchemaName,
                            explicitQueries);
                    tableHandlers.put(key, handler);
                    logger.debug("For pattern {} added handler: {}", key, handler);
                }
            }

        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid, can not start JDBC repository.", ex);
            throw new InvalidException("Configuration invalid, can not start JDBC repository.", ex);
        } catch (NamingException ex) {
            throw new InvalidException("Could not find configured jndiName " + jndiName + " to start repository ", ex);
        }

        try {
            // Check if we can get a connection
            Connection testConn = getConnection();
        } catch (Exception ex) {
            logger.warn("JDBC Repository start-up experienced a failure getting a DB connection: " + ex.getMessage()
                    + ". If this is not temporary or resolved, Repository operation will be affected.", ex);
        }
    }

    GenericTableHandler getGenericTableHandler(DatabaseType databaseType, String mainTable, String propertiesTable,
                                               String dbSchemaName, JsonNode queries, int maxBatchSize) {

        GenericTableHandler handler = null;

        // TODO: make pluggable
        switch (databaseType) {
            case DB2:
                handler = new DB2TableHandler(
                        mainTable,
                        propertiesTable,
                        dbSchemaName,
                        queries,
                        maxBatchSize);
                break;
            default:
                handler = new GenericTableHandler(
                        mainTable,
                        propertiesTable,
                        dbSchemaName,
                        queries,
                        maxBatchSize);
        }
        return handler;
    }

    /* Currently rely on deactivate/activate to be called by DS if config changes instead
    @Modified
    void modified(ComponentContext compContext) {
        logger.info("Configuration of repository changed.");
        deactivate(compContext);
        activate(compContext);
    }
    */


    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext);
        logger.info("Repository stopped.");
    }
}
