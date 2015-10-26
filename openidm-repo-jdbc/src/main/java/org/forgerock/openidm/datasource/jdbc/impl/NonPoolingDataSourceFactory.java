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

import static org.forgerock.guava.common.base.Strings.isNullOrEmpty;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import javax.sql.DataSource;

import org.forgerock.openidm.config.enhanced.InvalidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for a {@link DataSource} via config parameters that does not pool connections.
 */
class NonPoolingDataSourceFactory implements DataSourceFactory {
    private static final Logger logger = LoggerFactory.getLogger(NonPoolingDataSourceFactory.class);

    private static final String CONFIG_KERBEROS_PRINCIPAL = "kerberosServerPrincipal";
    private static final String CONFIG_SECURITY_MECHANISM = "securityMechanism";

    private final NonPoolingDataSourceConfig config;

    NonPoolingDataSourceFactory(NonPoolingDataSourceConfig config) {
        this.config = config;
    }

    @Override
    public DataSource newInstance() throws InvalidException {
        try {
            Class.forName(config.getDriverClass());
        } catch (ClassNotFoundException e) {
            throw new InvalidException(
                    "Could not find configured database driver " + config.getDriverClass() + " to start repository ",
                    e);
        }

        logger.info("Using DB connection configured via Driver Manager with Driver {} and URL {}",
                config.getDriverClass(), config.getJdbcUrl());
        final java.util.Properties properties = new java.util.Properties();
        if (!isNullOrEmpty(config.getUsername())) {
            properties.put("user", config.getUsername());

            if (!isNullOrEmpty(config.getPassword())) {
                properties.put("password", config.getPassword());
            }
        }
        if (!isNullOrEmpty(config.getKerberosServerPrincipal())) {
            properties.put(CONFIG_KERBEROS_PRINCIPAL, config.getKerberosServerPrincipal());
        }
        if (!isNullOrEmpty(config.getSecurityMechanism())) {
            properties.put(CONFIG_SECURITY_MECHANISM, config.getSecurityMechanism());
        }

        return new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return DriverManager.getConnection(config.getJdbcUrl(), properties);
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return DriverManager.getConnection(config.getJdbcUrl(), username, password);
            }

            @Override
            public PrintWriter getLogWriter() throws SQLException {
                return DriverManager.getLogWriter();
            }

            @Override
            public void setLogWriter(PrintWriter out) throws SQLException {
                DriverManager.setLogWriter(out);
            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
                DriverManager.setLoginTimeout(seconds);
            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return DriverManager.getLoginTimeout();
            }

            @Override
            public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
                throw new SQLFeatureNotSupportedException("parent logger not supported");
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return false;
            }
        };
    }

    public void shutdown(DataSource dataSource) {
        // nothing to do
    }
}
