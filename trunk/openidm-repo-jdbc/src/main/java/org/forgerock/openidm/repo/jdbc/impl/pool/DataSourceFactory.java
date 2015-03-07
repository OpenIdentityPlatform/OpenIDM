/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2015 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.jdbc.impl.pool;

import com.jolbox.bonecp.BoneCPDataSource;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.core.IdentityServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.UUID;

/**
 * Creates the BoneCP data source containing the BoneCP config.
 */
public class DataSourceFactory {
    private final static Logger logger = LoggerFactory.getLogger(DataSourceFactory.class);

    public static DataSource newInstance(JsonValue config) {
        //TODO Make CP implementation independent
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        BoneCPDataSource ds = mapper.convertValue(config.asMap(), BoneCPDataSource.class);
        ds.setConnectionHook(new DatabaseShutdownHook());
        ds.setTransactionRecoveryEnabled(true);// Important: This should be enabled
        ds.setAcquireRetryAttempts(10);//default is 5
        ds.setReleaseHelperThreads(5);
        ds.setStatisticsEnabled(Boolean.parseBoolean(
                IdentityServer.getInstance().getProperty("openidm.bonecp.statistics.enabled", "false")));
        ds.setPoolName(UUID.randomUUID().toString());
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
}
