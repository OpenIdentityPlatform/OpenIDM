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

/**
 * An abstract data source config with the minimum connection configuration.
 */
abstract class AbstractConnectionDataSourceConfig extends AbstractDataSourceConfig {
    private String driverClass;

    private String jdbcUrl;

    private String username;

    private String password;

    private int connectionTimeout = 30000;

    /**
     * Gets the datasource class name for the JDBC database.
     *
     * @return The JDBC driver class
     */
    public String getDriverClass() {
        return driverClass;
    }

    /**
     * Gets the JDBC database url.
     *
     * @return The JDBC database url.
     */
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * Gets the username to use to connect to the JDBC database.
     *
     * @return The username to used to connect to the JDBC database.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the password to use to connect to the JDBC database.
     *
     * @return The password to used to connect to the JDBC database.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the maximum amount of time to wait for a connection from the data source.
     *
     * @return The maximum amount of time to wait for a connection from the data source.
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
}
