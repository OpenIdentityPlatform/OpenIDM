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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.forgerock.openidm.config.enhanced.InvalidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for a {@link DataSource} via JNDI, e.g. "java:comp/env/jdbc/MySQLDB"
 */
class JndiDataSourceFactory implements DataSourceFactory {

    private static final Logger logger = LoggerFactory.getLogger(JndiDataSourceFactory.class);

    private static final String CONFIG_DB_DRIVER = "driverClass";

    private final JndiDataSourceConfig config;

    JndiDataSourceFactory(JndiDataSourceConfig config) {
        this.config = config;
    }

    @Override
    public DataSource newInstance() throws InvalidException {
        logger.info("Using DB connection configured via the (JNDI) naming context");
        try {
            InitialContext ctx = new InitialContext();
            return (DataSource) ctx.lookup(config.getJndiName());
        } catch (NamingException e) {
            logger.error("Getting JNDI initial context failed: " + e.getMessage(), e);
            throw new InvalidException(
                    "Current platform context does not support lookup of repository DB via JNDI. "
                            + " Configure DB initialization via direct " + CONFIG_DB_DRIVER
                            + " configuration instead.");
        }
    }

    @Override
    public void shutdown(DataSource dataSource) {
        // nothing to do
    }
}
