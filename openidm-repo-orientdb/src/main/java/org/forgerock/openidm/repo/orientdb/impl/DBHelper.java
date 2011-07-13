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
package org.forgerock.openidm.repo.orientdb.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.openidm.config.InvalidException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OProperty.INDEX_TYPE;
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

    /**
     * Initialize the DB pool.
     * @param dbURL the orientdb URL
     * @param user the orientdb user to connect
     * @param password the orientdb password to connect
     * @param minSize the orientdb pool minimum size
     * @param maxSize the orientdb pool maximum size
     * @return the initialized pool
     */
    public static ODatabaseDocumentPool initPool(String dbURL, String user, String password, 
            int minSize, int maxSize, JsonNode completeConfig) throws InvalidException {
        logger.trace("Initializing DB Pool");
        
        // Enable transaction log
        OGlobalConfiguration.TX_USE_LOG.setValue(true);
        
        // Immediate disk sync for commit 
        OGlobalConfiguration.TX_COMMIT_SYNCH.setValue(true);
        
        checkDB(dbURL, user, password, completeConfig);
        
        ODatabaseDocumentPool pool = new ODatabaseDocumentPool(); 
        //ODatabaseDocumentPool pool = ODatabaseDocumentPool.global(); // Moving from 0.9.25 to 1.0 RC had to change this
        pool.setup(minSize, maxSize);
        warmUpPool(pool, dbURL, user, password, minSize);
        
        return pool;
    }
    
    /**
     * Ensure the min size pool entries are initilized.
     * Cuts down on some (small) initial latency with lazy init
     * Do not call with a min past the real pool max, it will block.
     */
    private static void warmUpPool(ODatabaseDocumentPool pool, String dbURL, String user, String password, int minSize) {
        logger.trace("Warming up pool up to minSize {}", Integer.valueOf(minSize));
        List<ODatabaseDocumentTx> list = new ArrayList<ODatabaseDocumentTx>();
        for (int count=0; count < minSize; count++) {
            logger.trace("Warming up entry {}", Integer.valueOf(count));
            list.add(pool.acquire(dbURL, user, password));
        }
        for (ODatabaseDocumentTx entry : list) {
            pool.release(entry);
        }
    }
    
    /**
     * Ensures the DB is present in the expected form.
     */
    private static void checkDB(String dbURL, String user, String password, JsonNode completeConfig) throws InvalidException{
        // TODO: Creation/opening of db may be not be necessary if we require this managed externally
        ODatabaseDocumentTx db = null;
        try {
            db = new ODatabaseDocumentTx(dbURL); 
            if (db.exists()) {
                logger.info("Using DB at {}", dbURL);
                db.open(user, password); 
            } else { 
                JsonNode dbStructure = completeConfig.get(OrientDBRepoService.CONFIG_DB_STRUCTURE);
                if (dbStructure == null) {
                    logger.warn("No database exists, and no database structure defined in the configuration to populate one.");
                    throw new InvalidException("No database exists, and no database structure defined in the configuration to populate one.");
                } else {
                    logger.info("DB does not exist, creating {}", dbURL);
                    db.create(); 	       
                    populateSample(db, completeConfig);
                }
            } 
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    // TODO: Review the initialization mechanism
    private static void populateSample(ODatabaseDocumentTx db, JsonNode completeConfig) throws InvalidException {
        
        JsonNode dbStructure = completeConfig.get(OrientDBRepoService.CONFIG_DB_STRUCTURE);
        if (dbStructure == null) {
            logger.warn("No database structure defined in the configuration." + completeConfig);
        } else {
            JsonNode orientDBClasses = dbStructure.get(OrientDBRepoService.CONFIG_ORIENTDB_CLASS);
            OSchema schema = db.getMetadata().getSchema();
            logger.info("Setting up database");
            if (orientDBClasses != null) {
                for (Object key : orientDBClasses.keys()) {
                    String orientClassName = (String) key;
                    JsonNode orientClassConfig = (JsonNode) orientDBClasses.get(orientClassName);
                   
                    logger.info("Creating OrientDB class {}", orientClassName);
                    OClass orientClass = schema.createClass(orientClassName, db.getStorage().addCluster(orientClassName, OStorage.CLUSTER_TYPE.PHYSICAL));
                    
                    JsonNode indexes = orientClassConfig.get(OrientDBRepoService.CONFIG_INDEX);
                    for (JsonNode index : indexes) {
                        String propertyName = index.get(OrientDBRepoService.CONFIG_PROPERTY_NAME).asString();
                        String propertyType = index.get(OrientDBRepoService.CONFIG_PROPERTY_TYPE).asString();
                        String indexType = index.get(OrientDBRepoService.CONFIG_INDEX_TYPE).asString();
                        
                        logger.info("Creating index on property {} of type {} with index type {} on {} for OrientDB class ", 
                                new Object[] {propertyName, propertyType, indexType, orientClassName});
            
                        OType orientPropertyType = null;
                        try {
                            orientPropertyType = OType.valueOf(propertyType.toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            throw new InvalidException("Invalid property type '" + propertyType + "' in configuration on property "
                                    + propertyName + " with index type " + indexType + " on " 
                                    + orientClassName + " valid values: " + OType.values() 
                                    + " failure message: " + ex.getMessage(), ex);
                        }
                        
                        try {
                            OProperty.INDEX_TYPE orientIndexType = OProperty.INDEX_TYPE.valueOf(indexType.toUpperCase());
                            
                            OProperty prop = orientClass.createProperty(propertyName, orientPropertyType);
                            prop.createIndex(orientIndexType);
                        } catch (IllegalArgumentException ex) {
                            throw new InvalidException("Invalid index type '" + indexType + "' in configuration on property "
                                    + propertyName + " of type " + propertyType + " on " 
                                    + orientClassName + " valid values: " + OProperty.INDEX_TYPE.values() 
                                    + " failure message: " + ex.getMessage(), ex);
                        }
                    }
                }
            }
            schema.save(); 
        }
    }
}