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
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.objset.ConflictException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.storage.OStorage.CLUSTER_TYPE;
import com.orientechnologies.orient.core.storage.impl.local.OClusterLocal;

/**
 * A Helper to interact with the OrientDB
 * @author aegloff
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
     * @throws org.forgerock.openidm.config.InvalidException
     */
    public synchronized static ODatabaseDocumentPool getPool(String dbURL, String user, String password, 
            int minSize, int maxSize, JsonValue completeConfig, boolean setupDB) throws InvalidException {

        if (setupDB) {
            logger.debug("Check DB exists in expected state for pool {}", dbURL);
            checkDB(dbURL, user, password, completeConfig);
        }
        logger.debug("Getting pool {}", dbURL);
        ODatabaseDocumentPool pool = pools.get(dbURL);
        if (pool == null) {
            pool = initPool(dbURL, user, password, minSize, maxSize, completeConfig);
            pools.put(dbURL, pool);
        }
        return pool;
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
        pools = new HashMap(); // release all our closed pool references
    }
    
    /**
     * Initialize the DB pool.
     * @param dbURL the orientdb URL
     * @param user the orientdb user to connect
     * @param password the orientdb password to connect
     * @param minSize the orientdb pool minimum size
     * @param maxSize the orientdb pool maximum size
     * @param completeConfig
     * @return the initialized pool
     * @throws org.forgerock.openidm.config.InvalidException
     */
    private static ODatabaseDocumentPool initPool(String dbURL, String user, String password, 
            int minSize, int maxSize, JsonValue completeConfig) throws InvalidException {
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
            // Moving from 0.9.25 to 1.0 RC had to change this
            //ODatabaseDocumentPool pool = ODatabaseDocumentPool.global();
            pool.setup(minSize, maxSize);
            warmUpPool(pool, dbURL, user, password, minSize);
            
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
            java.util.Iterator iter = db.browseClass("config"); // Config always should exist
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
     */
    private static void checkDB(String dbURL, String user, String password, JsonValue completeConfig) 
            throws InvalidException {
        
        // TODO: Creation/opening of db may be not be necessary if we require this managed externally
        ODatabaseDocumentTx db = null;
        try {
            db = new ODatabaseDocumentTx(dbURL); 
            if (db.exists()) {
                logger.info("Using DB at {}", dbURL);
                db.open(user, password); 
                // Check if structure changed
                JsonValue dbStructure = completeConfig.get(OrientDBRepoService.CONFIG_DB_STRUCTURE);
                populateSample(db, completeConfig);
            } else { 
                JsonValue dbStructure = completeConfig.get(OrientDBRepoService.CONFIG_DB_STRUCTURE);
                logger.info("DB does not exist, creating {}", dbURL);
                db.create(); 	       
                populateSample(db, completeConfig);
            } 
        } finally {
            if (db != null) {
                db.close();
            }
        }
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
                orientDBClasses = new JsonValue(new java.util.HashMap());
            }
            
            Map cfgIndexes = new java.util.HashMap();
            orientDBClasses.put("config", cfgIndexes);
                            
            logger.info("Setting up database");
            if (orientDBClasses != null) {
                for (Object key : orientDBClasses.keys()) {
                    String orientClassName = (String) key;
                    JsonValue orientClassConfig = (JsonValue) orientDBClasses.get(orientClassName);
                    if (schema.existsClass(orientClassName)) {
                        // TODO: update indexes too if changed
                        logger.trace("OrientDB class {} already exists, skipping", orientClassName);
                    } else {
                        createOrientDBClass(db,schema, orientClassName, orientClassConfig);
                        if ("internal_user".equals(orientClassName)) {
                            populateDefaultUsers(orientClassName, db, completeConfig);
                        }
                    }
                }
            }
            schema.save(); 
            
        }
    }
    
    private static void createOrientDBClass(ODatabaseDocumentTx db, OSchema schema,
            String orientClassName, JsonValue orientClassConfig) {
        
        logger.info("Creating OrientDB class {}", orientClassName);
        OClass orientClass = schema.createClass(orientClassName, 
                db.getStorage().addCluster(orientClassName, 
                OStorage.CLUSTER_TYPE.PHYSICAL));
        
        JsonValue indexes = orientClassConfig.get(OrientDBRepoService.CONFIG_INDEX);
        for (JsonValue index : indexes) {
            String propertyType = index.get(OrientDBRepoService.CONFIG_PROPERTY_TYPE).asString();
            String indexType = index.get(OrientDBRepoService.CONFIG_INDEX_TYPE).asString();
            
            String propertyName = index.get(OrientDBRepoService.CONFIG_PROPERTY_NAME).asString();
            String[] propertyNames = null; 
            if (propertyName != null) {
                propertyNames = new String[] {propertyName};
            } else {
                List propNamesList = index.get(OrientDBRepoService.CONFIG_PROPERTY_NAMES).asList();
                if (propNamesList == null) {
                    throw new InvalidException("Invalid index configuration. " 
                            + "Missing property name(s) on index configuration for property type "
                            + propertyType + " with index type " + indexType + " on " + orientClassName);
                }
                propertyNames = (String[]) propNamesList.toArray(new String[0]);
            }
            
            // Determine a unique name to use for the index
            // Naming pattern used is <class>|property1[|propertyN]*|Idx
            StringBuilder indexName = new StringBuilder(orientClassName);
            indexName.append("|"); // Not using dot as is reserved for (simple index) naming convention
            for (String entry : propertyNames) {
                indexName.append(entry);
                indexName.append("|");
            }
            indexName.append("Idx");
            
            logger.info("Creating index on propertis {} of type {} with index type {} on {} for OrientDB class ", 
                    new Object[] {propertyNames, propertyType, indexType, orientClassName});

            OType orientPropertyType = null;
            try {
                orientPropertyType = OType.valueOf(propertyType.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new InvalidException("Invalid property type '" + propertyType + 
                        "' in configuration on properties "
                        + propertyNames + " with index type " + indexType + " on " 
                        + orientClassName + " valid values: " + OType.values() 
                        + " failure message: " + ex.getMessage(), ex);
            }
            
            try {
                OClass.INDEX_TYPE orientIndexType = OClass.INDEX_TYPE.valueOf(indexType.toUpperCase());
                OProperty prop = orientClass.createProperty(propertyName, orientPropertyType);
                orientClass.createIndex(indexName.toString(), orientIndexType, propertyNames);
            } catch (IllegalArgumentException ex) {
                throw new InvalidException("Invalid index type '" + indexType + 
                        "' in configuration on properties "
                        + propertyNames + " of type " + propertyType + " on " 
                        + orientClassName + " valid values: " + OClass.INDEX_TYPE.values() 
                        + " failure message: " + ex.getMessage(), ex);
            }
        }
    }
    
    // Populates the default user, the pwd needs to be changed by the installer
    private static void populateDefaultUsers(String defaultTableName, ODatabaseDocumentTx db, 
            JsonValue completeConfig) throws InvalidException {
        
        String defaultAdminUser = "openidm-admin";
        // Default password needs to be replaced after installation
        String defaultAdminPwd = "{\"$crypto\":{\"value\":{\"iv\":\"fIevcJYS4TMxClqcK7covg==\",\"data\":"
                + "\"Tu9o/S+j+rhOIgdp9uYc5Q==\",\"cipher\":\"AES/CBC/PKCS5Padding\",\"key\":\"openidm-sym-default\"},"
                + "\"type\":\"x-simple-encryption\"}}";
        String defaultAdminRoles = "openidm-admin,openidm-authorized";
        populateDefaultUser(defaultTableName, db, completeConfig, defaultAdminUser, 
                defaultAdminPwd, defaultAdminRoles);
        logger.trace("Created default user {}. Please change the assigned default password.", 
                defaultAdminUser);
        
        String anonymousUser = "anonymous";
        String anonymousPwd = "anonymous";
        String anonymousRoles = "openidm-reg";
        populateDefaultUser(defaultTableName, db, completeConfig, anonymousUser, anonymousPwd, anonymousRoles);
        logger.trace("Created default user {} for registration purposes.", anonymousUser);
    }    
    
    private static void populateDefaultUser(String defaultTableName, ODatabaseDocumentTx db, 
            JsonValue completeConfig, String user, String pwd, String roles) throws InvalidException {        
        
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
}
