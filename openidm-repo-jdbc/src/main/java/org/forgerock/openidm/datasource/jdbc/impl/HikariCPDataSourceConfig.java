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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariConfigMXBean;

import java.util.LinkedHashMap;

/**
 * Config object for HikariCP DataSources.
 */
class HikariCPDataSourceConfig extends AbstractConnectionDataSourceConfig {

    private Object connectionPool;

    public HikariConfig getConnectionPool() {
        if (connectionPool instanceof LinkedHashMap) {
            HikariConfig config=new HikariConfig();
            if (((LinkedHashMap)connectionPool).containsKey("maximumPoolSize")){
                config.setMaximumPoolSize((Integer) ((LinkedHashMap) connectionPool).get("maximumPoolSize"));
            }
            connectionPool=config;
        }
        return (HikariConfig)connectionPool;
    }

    @Override
    public <R, P> R accept(DataSourceConfigVisitor<R, P> visitor, P parameters) {
        return visitor.visit(this, parameters);
    }
}