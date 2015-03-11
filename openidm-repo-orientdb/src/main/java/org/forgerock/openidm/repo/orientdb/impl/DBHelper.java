/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.orientdb.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.OStorage;
import java.util.Collection;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * A Helper to interact with the OrientDB
 */
public class DBHelper {
    final static Logger logger = LoggerFactory.getLogger(DBHelper.class);
    
    private static Map<String, ODatabaseDocumentPool> pools = new HashMap<String, ODatabaseDocumentPool>();

    /**
     * Get the DB pool for the given URL. May return an existing pool instance.
     * Also can initialize/create/update the DB to meet the passed
     * configuration if setupDB is enabled
     * 
     * Do not close the returned pool directly as it may be used by others.
     * 
     * To cleanly shut down the application, call closePools at the end
     * 
     * @param dbURL the orientdb URL
     * @param user the orientdb user to connect
     * @param password the orientdb password to connect
     * @param minSize the orientdb pool minimum size
     * @param maxSize the orientdb pool maximum size
     * @param completeConfig the full configuration for the DB
     * @param setupDB true if it should also check the DB exists in the state
     * to match the passed configuration, and to set it up to match 
     * @return the pool
     * @throws org.forgerock.openidm.config.enhanced.InvalidException
     */
    public synchronized static ODatabaseDocumentPool getPool(String dbURL, String user, String password, 
            int minSize, int maxSize, JsonValue completeConfig, boolean setupDB) throws InvalidException {

        ODatabaseDocumentTx setupDbConn = null;
        ODatabaseDocumentPool pool = null;
        try {       
            if (setupDB) {
                logger.debug("Check DB exists in expected state for pool {}", dbURL);
                setupDbConn = checkDB(dbURL, user, password, completeConfig);
            }
            logger.debug("Getting pool {}", dbURL);
            pool = pools.get(dbURL);
            if (pool == null) {
                pool = initPool(dbURL, user, password, minSize, maxSize);
                pools.put(dbURL, pool);
            }
        } finally {
            if (setupDbConn != null) {
                setupDbConn.close();
            }
        }
        
        return pool;
    }
    
    /**
     * Updates the username and password for the default admin user
     * 
     * @param dbURL the orientdb URL
     * @param oldUser the old orientdb user to update
     * @param oldPassword the old orientdb password to update
     * @param newUser the new orientdb user
     * @param newPassword the new orientdb password
     */
    public synchronized static void updateDbCredentials(String dbURL, String oldUser, String oldPassword, 
            String newUser, String newPassword) {

        ODatabaseDocumentTx db = null;
        try {
            db = new ODatabaseDocumentTx(dbURL);
            db.open(oldUser, oldPassword);
            OSecurity security = db.getMetadata().getSecurity();
            // Delete the old admin user
            security.dropUser(oldUser);
            // Create new admin user with new username and password
            security.createUser(newUser, newPassword, security.getRole(ORole.ADMIN));
        } catch (Exception e) {
            logger.error("Error updating DB credentials", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
    
    /**
     * Closes all pools managed by this helper
     * Call at application shut-down to cleanly shut down the pools.
     */
    public synchronized static void closePools() {
        logger.debug("Close DB pools");
        for (ODatabaseDocumentPool pool : pools.values()) {
            try {
                pool.close();
                logger.trace("Closed pool {}", pool);
            } catch (Exception ex) {
                logger.info("Faillure reported in closing pool {}", pool, ex);
            }
        }
        // release all our closed pool references
        pools.clear();
        pools = new HashMap<String, ODatabaseDocumentPool>();
    }
    
    /**
     * Close and remove a pool managed by this helper
     */
    public synchronized static void closePool(String dbUrl, ODatabaseDocumentPool pool) {
        logger.debug("Close DB pool for {} {}", dbUrl, pool);
        try {
            pools.remove(dbUrl);
            pool.close();
            logger.trace("Closed pool for {} {}", dbUrl, pool);
        } catch (Exception ex) {
            logger.info("Failure reported in closing pool {} {}", dbUrl, pool, ex);
        }
    }
    
    /**
     * Initialize the DB pool.
     * @param dbURL the orientdb URL
     * @param user the orientdb user to connect
     * @param password the orientdb password to connect
     * @param minSize the orientdb pool minimum size
     * @param maxSize the orientdb pool maximum size
     * @return the initialized pool
     * @throws org.forgerock.openidm.config.enhanced.InvalidException
     */
    private static ODatabaseDocumentPool initPool(String dbURL, String user, String password,  int minSize, int maxSize)
            throws InvalidException {
        logger.trace("Initializing DB Pool {}", dbURL);
        
        // Enable transaction log
        OGlobalConfiguration.TX_USE_LOG.setValue(true);
        
        // Immediate disk sync for commit 
        OGlobalConfiguration.TX_COMMIT_SYNCH.setValue(true);
        
        // Have the storage closed when the DB is closed.
        OGlobalConfiguration.STORAGE_KEEP_OPEN.setValue(false);
        
        boolean success = false;
        int maxRetry = 10;
        int retryCount = 0;
        ODatabaseDocumentPool pool = null;
        
        // Initialize and try to verify the DB. Retry maxRetry times.
        do {
            retryCount++;
            if (pool != null) {
                pool.close();
            }
            pool = new ODatabaseDocumentPool();
            pool.setup(minSize, maxSize);
            warmUpPool(pool, dbURL, user, password, 1);
            
            boolean finalTry = (retryCount >= maxRetry);
            success = test(pool, dbURL, user, password, finalTry);
        } while (!success && retryCount < maxRetry);
        
        if (!success) {
            logger.warn("DB could not be verified.");
        } else {
            logger.info("DB verified on try {}", retryCount);
        }
        
        logger.debug("Opened and initialized pool {}", pool);

        return pool;
    }
    
    /**
     * Perform a basic access on the DB for a rudimentary test 
     * @return whether the basic access succeeded
     */
    private static boolean test(ODatabaseDocumentPool pool, String dbURL, String user,
            String password, boolean finalTry) {
        
        ODatabaseDocumentTx db = null;
        try {
            logger.info("Verifying the DB.");
            db = pool.acquire(dbURL, user, password);
            Iterator<ODocument> iter = db.browseClass("config"); // Config always should exist
            if (iter.hasNext()) {
                iter.next();
            }
        } catch (OException ex) {
            if (finalTry) {
                logger.info("Exceptions encountered in verifying the DB", ex);
            } else {
                logger.debug("DB exception in testing.", ex);
            }
            return false;
        } finally {
            if (db != null) {
                db.close();
            } 
        }
        return true;
    }
    
    /**
     * Ensure the min size pool entries are initilized.
     * Cuts down on some (small) initial latency with lazy init
     * Do not call with a min past the real pool max, it will block.
     */
    private static void warmUpPool(ODatabaseDocumentPool pool, String dbURL, String user,
            String password, int minSize) {
        
        logger.trace("Warming up pool up to minSize {}", Integer.valueOf(minSize));
        List<ODatabaseDocumentTx> list = new ArrayList<ODatabaseDocumentTx>();
        for (int count=0; count < minSize; count++) {
            logger.trace("Warming up entry {}", Integer.valueOf(count));
            try {
                list.add(pool.acquire(dbURL, user, password));
            } catch (Exception ex) {
                logger.warn("Issue in warming up db pool, entry {}", Integer.valueOf(count), ex);
            }
        }
        for (ODatabaseDocumentTx entry : list) {
            try {
                if (entry != null) {
                    entry.close();
                }
            } catch (Exception ex) {
                logger.warn("Issue in connection close during warming up db pool, entry {}", entry, ex);
            }
        }
    }
    
    /**
     * Ensures the DB is present in the expected form.
     * @return the db reference. The caller MUST reliably close that DB when done with it, e.g. in a finally.
     * Be aware that an in-memory DB will disappear if there is no connection open to it,
     * and the keep open setting is not explicitly set to true
     */
    private static ODatabaseDocumentTx checkDB(String dbURL, String user, String password, JsonValue completeConfig) 
            throws InvalidException {
        
        // TODO: Creation/opening of db may be not be necessary if we require this managed externally
        ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbURL);

        // To add support for remote DB checking/creation one 
        // would need to use OServerAdmin instead
        // boolean dbExists = new OServerAdmin(dbURL).connect(user, password).existsDatabase();

        // Local DB we can auto populate 
        if (isLocalDB(dbURL) || isMemoryDB(dbURL)) {
            if (db.exists()) {
                logger.info("Using DB at {}", dbURL);
                db.open(user, password); 
                populateSample(db, completeConfig);
            } else { 
                logger.info("DB does not exist, creating {}", dbURL);
                db.create();
                // Delete default admin user
                OSecurity security = db.getMetadata().getSecurity();
                security.dropUser(OUser.ADMIN);
                // Create new admin user with new username and password
                security.createUser(user, password, security.getRole(ORole.ADMIN));
                populateSample(db, completeConfig);
            } 
        } else {
            logger.info("Using remote DB at {}", dbURL);
        }
        return db;
    }
    
    /**
     * Whether the URL represents a local DB
     * @param dbURL the OrientDB db url
     * @return true if local, false if remote
     * @throws InvalidException if the dbURL is null or otherwise known to be invalid
     */
    public static boolean isLocalDB(String dbURL) throws InvalidException {
        if (dbURL == null) {
            throw new InvalidException("dbURL is not set");
        }
        return dbURL.startsWith("local:") || dbURL.startsWith("plocal");
    }

    /**
     * Whether the URL represents a memory DB
     * @param dbURL the OrientDB db url
     * @return true if local, false if remote
     * @throws InvalidException if the dbURL is null or otherwise known to be invalid
     */
    public static boolean isMemoryDB(String dbURL) throws InvalidException {
    	if (dbURL == null) {
    		throw new InvalidException("dbURL is not set");
    	}
    	return dbURL.startsWith("memory:");
    }
    
    // TODO: Review the initialization mechanism
    private static void populateSample(ODatabaseDocumentTx db, JsonValue completeConfig) 
            throws InvalidException {
        
        JsonValue dbStructure = completeConfig.get(OrientDBRepoService.CONFIG_DB_STRUCTURE);
        if (dbStructure == null) {
            logger.warn("No database structure defined in the configuration." + completeConfig);
        } else {
            JsonValue orientDBClasses = dbStructure.get(OrientDBRepoService.CONFIG_ORIENTDB_CLASS);
            OSchema schema = db.getMetadata().getSchema();
            
            // Default always to create Config class for bootstrapping
            if (orientDBClasses == null || orientDBClasses.isNull()) {
                orientDBClasses = json(object());
            }
            
            orientDBClasses.put("config", object());
                            
            logger.info("Setting up database");
            for (Object key : orientDBClasses.keys()) {
                String orientClassName = (String) key;
                JsonValue orientClassConfig = orientDBClasses.get(orientClassName);

                boolean classAlreadyExists = schema.existsClass(orientClassName);
                createOrUpdateOrientDBClass(db, schema, orientClassName, orientClassConfig);
                if (!classAlreadyExists && "internal_user".equals(orientClassName)) {
                    populateDefaultUsers(orientClassName, db);
                }
            }
        }
    }
    
    // Populates the default user, the pwd needs to be changed by the installer
    private static void populateDefaultUsers(String defaultTableName, ODatabaseDocumentTx db) throws InvalidException {
        
        String defaultAdminUser = "openidm-admin";
        // Default password needs to be replaced after installation
        String defaultAdminPwd = "openidm-admin";
        String defaultAdminRoles = "openidm-admin,openidm-authorized";
        populateDefaultUser(defaultTableName, db, defaultAdminUser,
                defaultAdminPwd, defaultAdminRoles);
        logger.trace("Created default user {}. Please change the assigned default password.", 
                defaultAdminUser);
        
        String anonymousUser = "anonymous";
        String anonymousPwd = "anonymous";
        String anonymousRoles = "openidm-reg";
        populateDefaultUser(defaultTableName, db, anonymousUser, anonymousPwd, anonymousRoles);
        logger.trace("Created default user {} for registration purposes.", anonymousUser);
    }    
    
    private static void populateDefaultUser(String defaultTableName, ODatabaseDocumentTx db,
            String user, String pwd, String roles) throws InvalidException {
        
        JsonValue defaultAdmin = new JsonValue(new HashMap<String, Object>());
        defaultAdmin.put("_openidm_id", user);
        defaultAdmin.put("userName", user);
        defaultAdmin.put("password", pwd);
        defaultAdmin.put("roles", roles);
        
        try {
            ODocument newDoc = DocumentUtil.toDocument(defaultAdmin.asMap(), null, db, defaultTableName);
            newDoc.save();
        } catch (ConflictException ex) {
            throw new InvalidException("Unexpected failure during DB set-up of default user", ex);
        }
    }
    
    private static String uniqueIndexName(String orientClassName, String[] propertyNames) {
        // Determine a unique name to use for the index
        // Naming pattern used is <class>!property1[!propertyN]*!Idx
        StringBuilder sb = new StringBuilder(orientClassName);
        sb.append("!");
        for (String entry : propertyNames) {
            sb.append(entry);
            sb.append("!");
        }
        sb.append("Idx");
        return sb.toString();
    }

    private static void createProperty(OClass orientClass, String propName, String propertyType) {
        try {
            // Create property type object
            OType orientPropertyType = OType.valueOf(propertyType.toUpperCase());
            // Create property
            orientClass.createProperty(propName, orientPropertyType);
        } catch (IllegalArgumentException ex) {
            throw new InvalidException("Invalid property type '"
                    + propertyType + "' in configuration for property '" 
                    + propName + " on " + orientClass.getName()
                    + " valid values: { " + StringUtils.join(OType.values(), ", ") + " }"
                    + " failure message: " + ex.getMessage(), ex);
        }
    }

    private static void createIndex(OClass orientClass, String indexType, String[] propertyNames, String propertyType) {
            logger.info("Creating index on properties {} of type {} with index type {} on {} for OrientDB class ", 
                    propertyNames, propertyType, indexType, orientClass.getName());
            try {
                // Create the index
                String indexName = uniqueIndexName(orientClass.getName(), propertyNames);
                OClass.INDEX_TYPE orientIndexType = OClass.INDEX_TYPE.valueOf(indexType.toUpperCase());
                orientClass.createIndex(indexName, orientIndexType, propertyNames);
            } catch (IllegalArgumentException ex) {
                throw new InvalidException("Invalid index type '" + indexType + 
                        "' in configuration on properties "
                        + propertyNames + " of type " + propertyType + " on " 
                        + orientClass.getName() + " valid values: { "
                        + StringUtils.join(OClass.INDEX_TYPE.values(), ", ") + " }"
                        + " failure message: " + ex.getMessage(), ex);
            }
        }

    private static void createOrUpdateOrientDBClass(ODatabaseDocumentTx db, OSchema schema, 
            String orientClassName, JsonValue orientClassConfig) {
        
        OIndexManager indexManager = db.getMetadata().getIndexManager();
        OClass orientClass = schema.getClass(orientClassName);
        if (orientClass == null) {
            logger.info("OrientDB class {} does not exist and is being created.", orientClassName);
            orientClass = schema.createClass(orientClassName, 
                    db.addCluster(orientClassName, 
                    OStorage.CLUSTER_TYPE.PHYSICAL));
        }

        List<String> indexProperties = new ArrayList<String>();
        JsonValue indexes = orientClassConfig.get(OrientDBRepoService.CONFIG_INDEX);
        for (JsonValue index : indexes) {
            String propertyType = index.get(OrientDBRepoService.CONFIG_PROPERTY_TYPE).asString();
            String indexType = index.get(OrientDBRepoService.CONFIG_INDEX_TYPE).asString();
            String propertyName = index.get(OrientDBRepoService.CONFIG_PROPERTY_NAME).asString();
            ArrayList<String> propNamesList = new ArrayList<String>();
            if (propertyName != null) { 
                propNamesList.add(propertyName);
            } else {
                propNamesList.addAll(index.get(OrientDBRepoService.CONFIG_PROPERTY_NAMES).asList(String.class));
                if (propNamesList.isEmpty()) {
                    throw new InvalidException("Invalid index configuration. "
                            + "Missing property name(s) on index configuration for property type "
                            + propertyType + " with index type " + indexType + " on " + orientClassName);
                }
            }
            
            // Add new Class properties
            for (String propName : propNamesList) {
                if (!orientClass.existsProperty(propName)) {
                    logger.info("Creating property {} of type {}", new Object[] {propName, propertyType});
                    createProperty(orientClass, propName, propertyType);
                }
            }

            // Add or re-create indexes. No need to rebuild indexes as automatic
            // indexes are rebuilt by OrientDB when they are created.
            String[] propertyNames = propNamesList.toArray(new String[propNamesList.size()]);
            if (propertyNames.length > 0) {
                String indexName = uniqueIndexName(orientClass.getName(), propertyNames);
                OIndex<?> oIndex = orientClass.getClassIndex(indexName);
                if (oIndex != null && !oIndex.getType().equalsIgnoreCase(indexType)) {
                    indexManager.dropIndex(indexName);
                    oIndex = null;
                }
                if (oIndex == null) {
                    createIndex(orientClass, indexType, propertyNames, propertyType);
                }
            }
           // Add to master list of configured class properties
           indexProperties.addAll(propNamesList);
        }
        
        // Remove obsolete indexes but do not remove the associated
        // Class properties as we are using a hybrid schema and do not
        // know who created the Class properties
        Collection<OProperty> classProperties = orientClass.properties();
        for (OProperty property : classProperties) {
            String propName = property.getName();
            if (!indexProperties.contains(propName))
            {
                Set<OIndex<?>> propIndexes = indexManager.getClassInvolvedIndexes(orientClass.getName(), propName);
                for (OIndex<?> propIndex : propIndexes) {
                    // Ensure that we only drop indexes which we created and
                    // match the OpenIDM index naming convention
                    String indexRegex = uniqueIndexName(orientClass.getName(), new String[]{".*"});
                    if (propIndex.getName().matches(indexRegex)) {
                        indexManager.dropIndex(propIndex.getName());
                    }
                }
            }
        }
    }
}
