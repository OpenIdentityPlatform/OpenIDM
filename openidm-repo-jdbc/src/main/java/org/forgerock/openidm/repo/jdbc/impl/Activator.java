/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.jdbc.impl;

import java.util.Hashtable;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.config.persistence.ConfigBootstrapHelper;
import org.forgerock.openidm.datasource.DataSourceService;
import org.forgerock.openidm.datasource.jdbc.impl.JDBCDataSourceService;
import org.forgerock.openidm.repo.RepoBootService;
import org.forgerock.openidm.repo.jdbc.DatabaseType;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi bundle activator for JDBCRepoService.
 */
public class Activator implements BundleActivator {
    final static Logger logger = LoggerFactory.getLogger(Activator.class);

     public void start(BundleContext context) {
         logger.debug("JDBC bundle starting", context);

         JsonValue repoConfig = ConfigBootstrapHelper.getRepoBootConfig("jdbc", context);
         if (repoConfig == null) {
             logger.debug("No JDBC configuration detected");
             logger.debug("JDBC bundle started", context);
             return;
         }
         String dataSourcePid = repoConfig.get(JDBCRepoService.CONFIG_USE_DATASOURCE).asString();
         if (dataSourcePid == null) {
             logger.error("JDBC repository configured, but does not specify a datasource to use - "
                     + "the \"" + JDBCRepoService.CONFIG_USE_DATASOURCE + "\" config property is required "
                     + "and must be the <name> of a datasource.jdbc-<name>.json configuration.");
             logger.debug("JDBC bundle started", context);
             return;
         }

         JsonValue dataSourceConfig = ConfigBootstrapHelper.getDataSourceBootConfig("jdbc-" + dataSourcePid, context);
         if (dataSourceConfig == null) {
             logger.error("JDBC repository configured, but datasource \"" + dataSourcePid + "\" was not found - "
                     + " must specify or configure a valid datasource for JDBC repository to use.");
             logger.debug("JDBC bundle started", context);
             return;
         }

         logger.info("Bootstrapping JDBC repository");

         // Init the bootstrap connection manager
         DataSourceService dataSourceService = JDBCDataSourceService.getBootService(dataSourceConfig, context);
         // Init the bootstrap repo
         RepoBootService bootSvc = JDBCRepoService.getRepoBootService(context, dataSourceService, repoConfig);

         // Register bootstrap repo
         Hashtable<String, String> prop = new Hashtable<String, String>();
         prop.put(Constants.SERVICE_PID, "org.forgerock.openidm.bootrepo.jdbc");
         prop.put("openidm.router.prefix", "bootrepo");
         prop.put("db.type", "JDBC");
         prop.put("db.dirname", getDbDirname(repoConfig));

         context.registerService(RepoBootService.class.getName(), bootSvc, prop);
         logger.info("Registered bootstrap repository service");
         logger.debug("JDBC bundle started", context);
     }

    /**
     * Get the name of the directory in db/ for the currently configured repo
     * @param repoConfig The current repo configuration
     * @return The name of the directory in db/ for the current repo
     */
    private String getDbDirname(JsonValue repoConfig) {
        final DatabaseType databaseType = repoConfig.get(JDBCRepoService.CONFIG_DB_TYPE)
                .defaultTo(DatabaseType.ANSI_SQL99.name())
                .asEnum(DatabaseType.class);

        switch (databaseType) {
            case SQLSERVER:
                return "mssql";
            case MYSQL:
            case POSTGRESQL:
            case ORACLE:
            case DB2:
            case H2:
                return databaseType.toString().toLowerCase();
            case ANSI_SQL99:
            case ODBC:
            default:
                return null;
        }
    }

     public void stop(BundleContext context) {
         logger.debug("JDBC bundle stopped", context);
     }
}
