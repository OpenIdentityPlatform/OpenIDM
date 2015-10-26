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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.datasource.jdbc.impl;

import javax.sql.DataSource;

import java.util.UUID;

import com.jolbox.bonecp.BoneCPDataSource;
import org.forgerock.openidm.core.IdentityServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the BoneCP data source containing the BoneCP config.
 */
class BoneCPDataSourceFactory implements DataSourceFactory {
    private final static Logger logger = LoggerFactory.getLogger(BoneCPDataSourceFactory.class);

    private final BoneCPDataSourceConfig config;

    BoneCPDataSourceFactory(BoneCPDataSourceConfig config) {
        this.config = config;
    }

    public DataSource newInstance() {
        BoneCPDataSource ds = config.getConnectionPool();
        // copy appropriate properties from the main connection config
        ds.setDriverClass(config.getDriverClass());
        ds.setJdbcUrl(config.getJdbcUrl());
        ds.setUsername(config.getUsername());
        ds.setPassword(config.getPassword());
        ds.setDefaultCatalog(config.getDatabaseName()); // BoneCP calls this "defaultCatalog"
        ds.setConnectionTimeoutInMs(config.getConnectionTimeout());

        // set IDM standard settings
        ds.setConnectionHook(new BoneCPDatabaseShutdownHook());
        ds.setTransactionRecoveryEnabled(true);// Important: This should be enabled
        ds.setAcquireRetryAttempts(10);//default is 5
        ds.setReleaseHelperThreads(5);
        ds.setStatisticsEnabled(Boolean.parseBoolean(
                IdentityServer.getInstance().getProperty("openidm.bonecp.statistics.enabled", "false")));
        if (ds.getPoolName() == null || ds.getPoolName().isEmpty()) {
            ds.setPoolName(UUID.randomUUID().toString());
        }
        // Default if not explicitly set
        if (ds.getMaxConnectionsPerPartition() < 1) {
            ds.setMinConnectionsPerPartition(1);
            ds.setMaxConnectionsPerPartition(20);
        }

        /*
        // Settings to enable connection testing with BoneCP 0.8 snapshot
        ds.setDetectUnclosedStatements(true); // Debug setting
        ds.setCloseConnectionWatchTimeoutInMs(1000); // Debug setting, not for production
        ds.setCloseConnectionWatch(true); // Debug setting, not for production
        ds.setStatementsCacheSize(20); // This caching may already be done by DB driver
        */

        logger.debug("BoneCPDataSource: {}", ds);
        return ds;
    }

    public void shutdown(DataSource dataSource) {
        //close the datasource connection pool
        if (dataSource instanceof BoneCPDataSource) {
            ((BoneCPDataSource) dataSource).close();
        }
    }
}
