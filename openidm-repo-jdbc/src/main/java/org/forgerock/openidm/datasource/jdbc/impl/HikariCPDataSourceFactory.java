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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.datasource.jdbc.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.UUID;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the Hikari data source containing the Hikari config.
 */
public class HikariCPDataSourceFactory implements DataSourceFactory {
    private final static Logger logger = LoggerFactory.getLogger(HikariCPDataSourceFactory.class);
    
    private final HikariCPDataSourceConfig config;
    
    HikariCPDataSourceFactory(HikariCPDataSourceConfig config) {
        this.config = config;
    }
    
    @Override
    public DataSource newInstance() {
        HikariConfig cfg = config.getConnectionPool();

        cfg.setDriverClassName(config.getDriverClass());
        cfg.setJdbcUrl(config.getJdbcUrl());
        cfg.setUsername(config.getUsername());
        cfg.setPassword(config.getPassword());
        cfg.setCatalog(config.getDatabaseName());
        cfg.setConnectionTimeout(config.getConnectionTimeout());

        if (cfg.getPoolName() == null || cfg.getPoolName().isEmpty()) {
            cfg.setPoolName(UUID.randomUUID().toString());
        }
        
        logger.debug("HikariDataSource: {}", cfg);
        return new HikariDataSource(cfg);
    }
    
    public void shutdown(DataSource dataSource) {
        //close the datasource connection pool
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }
}
