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

import com.jolbox.bonecp.BoneCPDataSource;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import org.forgerock.json.JsonPointer;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.fieldIfNotNull;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.datasource.DataSourceService;

import org.testng.annotations.Test;

/**
 * Test of JDBCDataSourceServiceTest
 */
public class JDBCDataSourceServiceTest {

    @Test
    public void testHikariDataSource() {
        // given
        JsonValue config = getDataSourceConfig("hikari");
        config.add(new JsonPointer("/connectionPool/maximumPoolSize"), 2);
        
        // when
        DataSourceService dataSourceService = JDBCDataSourceService.getBootService(config, null);
        
        //then
        assertThat(dataSourceService.getDataSource()).isInstanceOf(HikariDataSource.class);
        assertThat(canExhaustPool(dataSourceService.getDataSource(), 2)).isTrue();
        assertThat(dataSourceIsValid(dataSourceService.getDataSource())).isTrue();
    }
    
    @Test
    public void testBoneCPDataSource() {
        // given
        JsonValue config = getDataSourceConfig("bonecp");
        config.add(new JsonPointer("/connectionPool/partitionCount"), 1);
        config.add(new JsonPointer("/connectionPool/maxConnectionsPerPartition"), 2);
        config.add(new JsonPointer("/connectionPool/minConnectionsPerPartition"), 1);
        config.add(new JsonPointer("/connectionPool/acquireIncrement"), 1);
        
        // when
        DataSourceService dataSourceService = JDBCDataSourceService.getBootService(config, null);
        
        // then
        assertThat(dataSourceService.getDataSource()).isInstanceOf(BoneCPDataSource.class);
        assertThat(canExhaustPool(dataSourceService.getDataSource(), 2)).isTrue();
        assertThat(dataSourceIsValid(dataSourceService.getDataSource())).isTrue();
    }

    @Test
    public void testNonPoolingDataSource() {
        // given
        JsonValue config = getDataSourceConfig(null);
        
        // when
        DataSourceService dataSourceService = JDBCDataSourceService.getBootService(config, null);
        
        // then
        assertThat(dataSourceService.getDataSource()).isExactlyInstanceOf(
                NonPoolingDataSourceFactory.NonPoolingDataSource.class);
        assertThat(dataSourceIsValid(dataSourceService.getDataSource())).isTrue();
    }
    
    private boolean dataSourceIsValid(DataSource ds) {
        try {
            ds.getConnection().isValid(5);
        } catch (SQLException ex) {
            return false;
        }
        return true;
    }
    
    private JsonValue getDataSourceConfig(String type) {
        Object poolType = null;
        if (type != null) {
            poolType = object(
                    field("type", type)
            );
        }
        
        return new JsonValue(
                object(
                        field("driverClass", "org.hsqldb.jdbcDriver"),
                        field("jdbcUrl", "jdbc:hsqldb:mem:openidmtestdb"),
                        field("connectionTimeout", 5000),
                        fieldIfNotNull("connectionPool", poolType)
                )
        );
    }

    private boolean canExhaustPool(DataSource ds, int poolSize) {
        boolean exhausted = false;
        int numConnections = 0;
        Set<Connection> connections = new HashSet<>();
        
        try {
            while (numConnections <= poolSize) {
                connections.add(ds.getConnection());
                numConnections++;
            }
        } catch (SQLException ex) {
            if (numConnections == poolSize) {
                exhausted = true;
            }
        } finally {
            for (Connection c : connections) {
                try {
                    c.close();
                } catch (SQLException ex) {
                }
            }
        }
        return exhausted;
    }
}
