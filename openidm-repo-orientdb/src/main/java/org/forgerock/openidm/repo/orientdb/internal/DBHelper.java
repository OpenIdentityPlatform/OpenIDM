/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.orientdb.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.common.io.OIOUtils;
import com.orientechnologies.orient.client.remote.OEngineRemote;
import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.exception.OConfigurationException;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.exception.OStorageException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.server.config.OServerConfiguration;

/**
 * A Helper to interact with the OrientDB
 *
 * @author aegloff
 */
public final class DBHelper {

    public static final String UNIQUE_PRIMARY_IDX = "UNIQUE_PRIMARY_IDX";

    private DBHelper() {
    }

    /**
     * Setup logging for the {@link DBHelper}.
     */
    final static Logger logger = LoggerFactory.getLogger(DBHelper.class);

    public static final String TYPE_GRAPH = "graph";
    public static final String TYPE_DOCUMENT = "document";
    public static final String CLASS_JSON_RESOURCE = "JSONResource";

    /**
     * Get the DB pool for the given URL. May return an existing pool
     * newBuilder. Also can initialize/create/update the DB to meet the passed
     * configuration if setupDB is enabled
     *
     * Do not close the returned pool directly as it may be used by others.
     *
     * To cleanly shut down the application, call closePools at the end
     *
     * @param dbURL
     *            the orientdb URL
     * @param user
     *            the orientdb user to connect
     * @param password
     *            the orientdb password to connect
     * @param completeConfig
     *            the full configuration for the DB
     * @param setupDB
     *            true if it should also check the DB exists in the state to
     *            match the passed configuration, and to set it up to match
     * @return the pool
     * @throws org.forgerock.openidm.config.enhanced.InvalidException
     */
    public synchronized static void getPool(String dbURL, String user, String password,
            JsonValue completeConfig, boolean setupDB) throws InvalidException, IOException {
        try {
            if (setupDB) {
                logger.debug("Check DB exists in expected state for pool {}", dbURL);
                checkDB(dbURL, user, password, completeConfig);
            }
            logger.debug("Getting pool {}", dbURL);

            if (!ODatabaseDocumentPool.global().getPools().containsKey(
                    OIOUtils.getUnixFileName(user + "@" + dbURL))) {
                initPool(dbURL, user, password);
            }
        } finally {

        }
    }

    /**
     * Closes all pools managed by this helper Call at application shut-down to
     * cleanly shut down the pools.
     */
    public synchronized static void closePools() {
        logger.debug("Close DB pools");
        try {
            ODatabaseDocumentPool.global().close();
            logger.trace("Successfully closed global pool {}");
        } catch (Exception ex) {
            logger.info("Failure reported in closing global pool", ex);
        }
    }

    /**
     * Initialize the DB pool.
     *
     * @param dbURL
     *            the orientdb URL
     * @param user
     *            the orientdb user to connect
     * @param password
     *            the orientdb password to connect
     * @return the initialized pool
     * @throws org.forgerock.openidm.config.enhanced.InvalidException
     */
    private static void initPool(String dbURL, String user, String password)
            throws InvalidException {
        logger.trace("Initializing DB Pool {}", dbURL);

        boolean success = false;
        int maxRetry = 10;
        int retryCount = 0;

        // Initialize and try to verify the DB. Retry maxRetry times.
        do {
            retryCount++;
            warmUpPool(dbURL, user, password);
            boolean finalTry = (retryCount >= maxRetry);
            success = test(dbURL, user, password, finalTry);
        } while (!success && retryCount < maxRetry);

        if (!success) {
            logger.warn("DB could not be verified.");
        } else {
            logger.info("DB verified on try {}", retryCount);
        }

        logger.debug("Opened and initialized global pool {}");
    }

    /**
     * Perform a basic access on the DB for a rudimentary test
     *
     * @return whether the basic access succeeded
     */
    private static boolean test(String dbURL, String user, String password, boolean finalTry) {

        ODatabaseDocumentTx db = null;
        try {
            logger.info("Verifying the DB.");
            db = ODatabaseDocumentPool.global().acquire(dbURL, user, password);
            // JSONResource always should exist
            OClass superClass = db.getMetadata().getSchema().getClass(CLASS_JSON_RESOURCE);

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
     * Ensure the min size pool entries are initilized. Cuts down on some
     * (small) initial latency with lazy init Do not call with a min past the
     * real pool max, it will block.
     */
    private static void warmUpPool(String dbURL, String user, String password) {
        int minSize =
                Math.min(OGlobalConfiguration.DB_POOL_MIN.getValueAsInteger(),
                        OGlobalConfiguration.DB_POOL_MAX.getValueAsInteger());

        logger.trace("Warming up pool up to minSize {}", Integer.valueOf(minSize));
        List<ODatabaseDocumentTx> list = new ArrayList<ODatabaseDocumentTx>(minSize);
        for (int count = 0; count < minSize; count++) {
            logger.trace("Warming up entry {}", Integer.valueOf(count));
            try {
                list.add(ODatabaseDocumentPool.global().acquire(dbURL, user, password));
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
                logger.warn("Issue in connection close during warming up db pool, entry {}", entry,
                        ex);
            }
        }
    }

    /**
     * Ensures the DB is present in the expected form.
     */
    private static void checkDB(String dbURL, String user, String password, JsonValue completeConfig)
            throws InvalidException, IOException {
        ODatabaseDocumentTx db = null;
        try {
            db = Orient.instance().getDatabaseFactory().createObjectDatabase(dbURL);
            if (dbURL.startsWith(OEngineRemote.NAME)) {
                try {
                    // OConfigurationException("Database 'openidm' is not configured on server");
                    db.open(user, password);
                } catch (OException e) {
                    if (e instanceof OConfigurationException || e instanceof OStorageException) {
                        // Assume the 'openidm' database does not exist.
                        OServerAdmin admin = null;
                        try {
                            admin = new OServerAdmin(dbURL);
                            try {
                                admin.connect(user, password);
                            } catch (final OException ex) {
                                final Throwable t = ex.getCause();
                                if (t instanceof OSecurityAccessException) {

                                    String rootName =
                                            completeConfig.get("rootName").defaultTo(
                                                    OServerConfiguration.SRV_ROOT_ADMIN).asString();
                                    String rootPassword =
                                            completeConfig
                                                    .get("rootPassword")
                                                    .defaultTo(
                                                            "7A5DEAB30884B4C8026A047F13D4A67BDEDC7CA227AA8F4D477727EABE5541B4")
                                                    .asString();

                                    if (StringUtils.isNotBlank(rootPassword)) {
                                        admin.connect(rootName, rootPassword);
                                    } else {
                                        throw new InvalidException(
                                                "Missing /rootName and /rootPassword configuration so the 'openidm' database can not be created automatically.");
                                    }
                                } else {
                                    throw ex;
                                }
                            }
                            if (!admin.existsDatabase()) {
                                if ("admin".equals(user) && "admin".equals(password)) {
                                    admin.createDatabase("document", "local");
                                } else {
                                    throw new InvalidException(
                                            "New database must use the default 'admin':'admin' name:password combination");
                                }
                            } else {
                                // The exception was not because the 'openidm'
                                // database does not exits
                                throw e;
                            }
                        } finally {
                            if (null != admin) {
                                try {
                                    admin.close();
                                } catch (Throwable t) {
                                    /* ignore */
                                    logger.trace("Failed to close OServerAdmin", e);
                                }
                            }
                        }
                    } else {
                        throw e;
                    }
                }
            } else {
                if (!db.exists()) {
                    if ("admin".equals(user) && "admin".equals(password)) {
                        db.create();
                    } else {
                        throw new InvalidException(
                                "New database must use the default 'admin':'admin' name:password combination");
                    }
                }
            }
            if (db.isClosed()) {
                db.open(user, password);
            }
            populateSample(db, completeConfig);
        } finally {
            if (null != db) {
                try {
                    db.close();
                } catch (Throwable t) {
                    logger.error("TODO: Debug this case", t);
                }
            }
        }
    }

    // TODO: Review the initialization mechanism
    private static void populateSample(final ODatabaseDocumentTx db, final JsonValue completeConfig)
            throws InvalidException {

        JsonValue dbStructure = completeConfig.get(OrientDBRepoService.CONFIG_DB_STRUCTURE);

        final OSchema schema = db.getMetadata().getSchema();
        OClass superClass = schema.getClass(CLASS_JSON_RESOURCE);
        if (null == superClass) {
            if (schema.existsClass("V")) {
                superClass = schema.createClass(CLASS_JSON_RESOURCE, schema.getClass("V"));
            } else {
                superClass = schema.createClass(CLASS_JSON_RESOURCE);
            }
            superClass.createProperty(DocumentUtil.ORIENTDB_PRIMARY_KEY, OType.STRING);
            superClass.createIndex(UNIQUE_PRIMARY_IDX, OClass.INDEX_TYPE.UNIQUE,
                    DocumentUtil.ORIENTDB_PRIMARY_KEY);
        }

        if (dbStructure == null) {
            logger.warn("No database structure defined in the configuration.");
        } else {
            JsonValue orientDBClasses = dbStructure.get(OrientDBRepoService.CONFIG_ORIENTDB_CLASS);

            // Default always to create Config class for bootstrapping
            if (orientDBClasses == null || orientDBClasses.isNull()) {
                orientDBClasses = new JsonValue(new HashMap());
            }

            Map cfgIndexes = new java.util.HashMap();
            orientDBClasses.put("config", cfgIndexes);

            logger.info("Setting up database");

            for (String orientClassName : orientDBClasses.keys()) {

                if (schema.existsClass(orientClassName)) {
                    // TODO: update indexes too if changed
                    logger.trace("OrientDB class {} already exists, skipping", orientClassName);
                } else {
                    createOrientDBClass(schema, superClass, orientClassName, orientDBClasses
                            .get(orientClassName));
                    if ("internal_user".equals(orientClassName)) {
                        populateDefaultUsers(orientClassName);
                    }
                }
            }
            schema.save();
        }
    }

    private static void createOrientDBClass(final OSchema schema, final OClass superClass,
            String orientClassName, JsonValue orientClassConfig) {

        logger.info("Creating OrientDB class {}", orientClassName);

        ODatabaseRecord graph = ODatabaseRecordThreadLocal.INSTANCE.get();

        // TODO Recheck this method call
        OClass orientClass =
                schema.createClass(orientClassName, superClass, graph.addCluster(orientClassName,
                        OStorage.CLUSTER_TYPE.PHYSICAL));

        JsonValue indexes =
                orientClassConfig.get(OrientDBRepoService.CONFIG_INDEX).expect(List.class);
        for (JsonValue index : indexes) {
            String[] propertyNames = null;

            String propertyName = index.get(OrientDBRepoService.CONFIG_PROPERTY_NAME).asString();
            if (StringUtils.isNotBlank(propertyName)) {
                propertyNames = new String[] { propertyName };
            } else {
                List<Object> propNamesList =
                        index.get(OrientDBRepoService.CONFIG_PROPERTY_NAMES).asList();
                if (propNamesList == null || propNamesList.isEmpty()) {
                    throw new JsonValueException(index,
                            "Invalid index configuration. Missing propertyName(s) on index configuration");
                }
                propertyNames = propNamesList.toArray(new String[propNamesList.size()]);
            }

            // Determine a unique name to use for the index
            // Naming pattern used is <class>|property1[|propertyN]*|Idx
            StringBuilder indexName = new StringBuilder(orientClassName).append("|");
            // Not using dot as is reserved for (simple index) naming convention
            for (String entry : propertyNames) {
                indexName.append(entry).append("|");
            }
            indexName.append("Idx");

            OType orientPropertyType =
                    index.get(OrientDBRepoService.CONFIG_PROPERTY_TYPE).asEnum(OType.class);

            // Check if a single property is being defined and create it if so
            for (String propName : propertyNames) {
                if (orientClass.getProperty(propName) != null) {
                    continue;
                }
                logger.info("Creating property {} of type {}", propName, orientPropertyType);
                // Create property
                orientClass.createProperty(propName, orientPropertyType);
            }

            // Create the index
            OClass.INDEX_TYPE orientIndexType =
                    index.get(OrientDBRepoService.CONFIG_INDEX_TYPE)
                            .asEnum(OClass.INDEX_TYPE.class);
            try {

                logger.info(
                        "Creating index on properties {} of type {} with index type {} on {} for OrientDB class ",
                        new Object[] { propertyNames, orientPropertyType, orientIndexType,
                            orientClassName });
                orientClass.createIndex(indexName.toString(), orientIndexType, propertyNames);
            } catch (IllegalArgumentException ex) {
                throw new InvalidException("Invalid index type '" + orientIndexType
                        + "' in configuration on properties " + propertyNames + " of type "
                        + orientPropertyType + " on " + orientClassName + " valid values: "
                        + OClass.INDEX_TYPE.values() + " failure message: " + ex.getMessage(), ex);
            }
        }
    }

    // Populates the default user, the pwd needs to be changed by the installer
    private static void populateDefaultUsers(String defaultTableName) {

        populateDefaultUser(defaultTableName, ADMIN);
        logger.trace("Created default user 'openidm-admin'. Please change the assigned default password.");

        populateDefaultUser(defaultTableName, ANONYMOUS);
        logger.trace("Created default user 'anonymous' for registration purposes.");
    }

    private static void populateDefaultUser(String defaultTableName, String user) {
        ODocument newDoc = new ODocument(defaultTableName);
        newDoc.fromJSON(user);
        newDoc.save();
    }

    /* @formatter:off */
    private static final String ADMIN =
            "{\n" +
            "   \"_openidm_id\":\"openidm-admin\",\n" +
            "   \"userName\":\"openidm-admin\",\n" +
            "   \"password\":{\n" +
            "      \"$crypto\":{\n" +
            "         \"value\":{\n" +
            "            \"iv\":\"fIevcJYS4TMxClqcK7covg==\",\n" +
            "            \"data\":\"Tu9o/S+j+rhOIgdp9uYc5Q==\",\n" +
            "            \"cipher\":\"AES/CBC/PKCS5Padding\",\n" +
            "            \"key\":\"openidm-sym-default\"\n" +
            "         },\n" +
            "         \"type\":\"x-simple-encryption\"\n" +
            "      }\n" +
            "   },\n" +
            "   \"roles\":[\n" +
            "      \"openidm-admin\",\n" +
            "      \"openidm-authorized\"\n" +
            "   ]\n" +
            "}";

    private static final String ANONYMOUS =
            "{\n" +
            "   \"_openidm_id\":\"anonymous\",\n" +
            "   \"userName\":\"anonymous\",\n" +
            "   \"password\":\"anonymous\",\n" +
            "   \"roles\":[\n" +
            "      \"openidm-reg\"\n" +
            "   ]\n" +
            "}";
    /* @formatter:on */
}
