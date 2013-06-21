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

package org.forgerock.openidm.repo.orientdb.internal;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.common.concur.lock.OLockException;
import com.orientechnologies.common.io.OIOUtils;
import com.orientechnologies.common.profiler.OProfiler;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.graph.OGraphDatabasePool;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.orientechnologies.orient.server.config.OServerEntryConfiguration;

/**
 * Component for embedded OrientDB server
 *
 * @author aegloff
 */
public class EmbeddedOServerService extends OServerMain {

    /**
     * Setup logging for the {@link EmbeddedOServerService}.
     */
    final static Logger logger = LoggerFactory.getLogger(EmbeddedOServerService.class);
    private static final String SERVER_DATABASE_PATH = "server.database.path";

    // Current configuration
    private JsonValue existingConfig;

    private String dbURL;
    private String user;
    private String password;

    EmbeddedOServerService activate(JsonValue config) throws Exception {
        try {

            JsonValue globalConfiguration = config.get("globalConfiguration").expect(Map.class);
            if (!globalConfiguration.isNull()) {
                OGlobalConfiguration.setConfiguration(globalConfiguration.asMap());
            }

            if (isEnable(config)) {
                OServerConfiguration serverConfig = getOrientDBConfig(config);
                logger.trace("Starting embedded OrientDB server.");
                OGlobalConfiguration.ENVIRONMENT_DUMP_CFG_AT_STARTUP.setValue(logger
                        .isTraceEnabled());
                create().startup(serverConfig);
                Orient.instance().getProfiler().registerHookValue("system.databases",
                        "List of databases configured in Server", OProfiler.METRIC_TYPE.TEXT,
                        new OProfiler.OProfilerHookValue() {
                            @Override
                            public Object getValue() {
                                final StringBuilder dbs = new StringBuilder();
                                for (String dbName : server().getAvailableStorageNames().keySet()) {
                                    if (dbs.length() > 0)
                                        dbs.append(',');
                                    dbs.append(dbName);
                                }
                                return dbs.toString();
                            }
                        });
                server().activate();

                // com.orientechnologies.orient.graph.gremlin.OGremlinHelper.global().create();
                // OGlobalConfiguration.CACHE_LEVEL1_ENABLED.setValue(false);
                // OGlobalConfiguration.STORAGE_KEEP_OPEN.setValue(Boolean.TRUE);
                // OGremlinHelper.global().create();

                logger.info("Starting embedded OrientDB server is succeeded.");
            }
        } catch (Exception ex) {
            logger.error("Failed to start embedded OrientDB server.", ex);
            throw ex;
        }
        return this;
    }

    EmbeddedOServerService deactivate() {
        DBHelper.closePools();
        if (server() != null) {
            server().shutdown();
            logger.debug("Embedded DB server stopped.");
        }
        Orient.instance().shutdown();
        // com.orientechnologies.orient.graph.gremlin.OGremlinHelper.global().destroy();
        return this;
    }

    /**
     * Initialize the instnace with the given configuration.
     *
     * This can configure managed (DS/SCR) instances, as well as explicitly
     * instantiated (bootstrap) instances.
     *
     * @param config
     *            the configuration
     */
    void init(JsonValue config) throws Exception {
        try {
            dbURL = getDBUrl(config);
            user = config.get(OrientDBRepoService.CONFIG_USER).defaultTo("admin").asString();
            logger.info("OObjectDatabasePool.global().acquire(\"{}\", \"{}\", \"****\");", dbURL,
                    user);
            password =
                    config.get(OrientDBRepoService.CONFIG_PASSWORD).defaultTo("admin").asString();

        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid, can not start OrientDB repository", ex);
            throw ex;
        }

        try {
            DBHelper.getPool(dbURL, user, password, config, true);
            logger.debug("Obtained pool {}");
        } catch (Exception ex) {
            logger.warn("Initializing database pool failed", ex);
            throw ex;
        }
    }

    /**
     * TODO http://code.google.com/p/orient/wiki/JavaMultiThreading
     * @return A connection from the pool. Call close on the connection when
     *         done to return to the pool.
     * @throws org.forgerock.json.resource.InternalServerErrorException
     */
    ODatabaseDocumentTx getConnection() throws ResourceException {
        ODatabaseDocumentTx db = null;
        int maxRetry = 100; // give it up to approx 10 seconds to recover
        int retryCount = 0;

        while (db == null && retryCount < maxRetry) {
            retryCount++;
            try {
                // TYPE_GRAPH.equals(db.get(ODatabase.ATTRIBUTES.TYPE))
                db = OGraphDatabasePool.global().acquire(dbURL, user, password);
                //db = ODatabaseDocumentPool.global().acquire(dbURL, user, password);
                if (retryCount > 1) {
                    logger.info("Succeeded in acquiring connection from pool in retry attempt {}",
                            retryCount);
                }
                retryCount = maxRetry;
            } catch (com.orientechnologies.orient.core.exception.ORecordNotFoundException ex) {
                // TODO: remove work-around once OrientDB resolves this
                // condition
                if (retryCount == maxRetry) {
                    logger.warn(
                            "Failure reported acquiring connection from pool, retried {} times before giving up.",
                            retryCount, ex);
                    throw new ServiceUnavailableException(
                            "Failure reported acquiring connection from pool, retried "
                                    + retryCount + " times before giving up: " + ex.getMessage(),
                            ex);
                } else {
                    logger.info("Pool acquire reported failure, retrying - attempt {}", retryCount);
                    logger.trace("Pool acquire failure detail ", ex);
                    try {
                        // Give the DB time to complete what it's doing before
                        // retrying
                        Thread.sleep(100);
                    } catch (InterruptedException iex) {
                        // ignore that sleep was interrupted
                    }
                }
            } catch (OLockException e) {
                logger.warn("Not more resources available in global pool. Requested resource: {}",
                        OIOUtils.getUnixFileName(user + "@" + dbURL));
                // TODO provide less information in exception @Security concern
                throw new ServiceUnavailableException(
                        "Not more resources available in global pool. Requested resource: "
                                + OIOUtils.getUnixFileName(user + "@" + dbURL));
            }
        }
        if (db != null) {
            return db;
        } else {
            throw new ServiceUnavailableException();
        }
    }

    /**
     * Checks the configuration if the embedded OrientDB service is enabled.
     *
     * @param config
     * @return
     */
    static boolean isEnable(JsonValue config) {
        // enabled flag should be Boolean, but allow for (deprecated) String
        // representation for now.
        JsonValue enabled = config.get("embeddedServer").get("enabled");
        return (enabled.isBoolean() && Boolean.TRUE.equals(enabled.asBoolean()) || enabled
                .isString()
                && "true".equalsIgnoreCase(enabled.asString()));
    }

    String getDBUrl(JsonValue configuration) {
        // <engine>:<db-type>:<db-name>[?<db-param>=<db-value>[&]]*
        String url = null;
        if (isEnable(configuration)) {
            url = server().getStorageURL("openidm");
            if (null == url) {
                url =
                        configuration.get(OrientDBRepoService.CONFIG_DB_URL).defaultTo(
                                "local:" + server().getDatabaseDirectory() + "openidm").asString();
            }
        } else {
            url =
                    configuration.get(OrientDBRepoService.CONFIG_DB_URL).defaultTo(
                            "remote:localhost/openidm").asString();
        }
        return url;
    }

    private OServerConfiguration getOrientDBConfig(JsonValue config) {

        ObjectMapper mapper = JsonUtil.build();

        OServerConfiguration configuration =
                mapper.convertValue(config.get("embeddedServer").asMap(),
                        OServerConfiguration.class);

        String OHOME = IdentityServer.getFileForWorkingPath("db").getAbsolutePath();

        System.setProperty("ORIENTDB_HOME", OHOME);

        Map<String, OServerEntryConfiguration> customProperties =
                new HashMap<String, OServerEntryConfiguration>();
        customProperties.put("server.database.path", new OServerEntryConfiguration(
                SERVER_DATABASE_PATH, OHOME + "/"));

        // Make sure the database path always ends with '/'
        if (configuration.properties != null) {
            for (OServerEntryConfiguration prop : configuration.properties) {
                if (SERVER_DATABASE_PATH.equals(prop.name)) {
                    if (!prop.value.endsWith("/")) {
                        prop.value = prop.value + "/";
                    }
                }
                customProperties.put(prop.name, prop);
            }
        }

        configuration.properties =
                customProperties.values().toArray(
                        new OServerEntryConfiguration[customProperties.size()]);
        return configuration;
    }
}
