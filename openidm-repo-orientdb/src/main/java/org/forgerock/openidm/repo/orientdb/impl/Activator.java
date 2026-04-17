/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
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
package org.forgerock.openidm.repo.orientdb.impl;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.config.persistence.ConfigBootstrapHelper;
import org.forgerock.openidm.repo.RepoBootService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi bundle activator
 */
public class Activator implements BundleActivator {
    final static Logger logger = LoggerFactory.getLogger(Activator.class);

    // Bootstrap repository
    OrientDBRepoService bootSvc;
    
    public void start(BundleContext context) {
        logger.trace("OrientDB bundle starting");
        
        JsonValue repoConfig = ConfigBootstrapHelper.getRepoBootConfig("orientdb", context);
         
        if (repoConfig != null) {
            logger.info("Bootstrapping OrientDB repository");
            // Only take the configuration strictly needed for bootstrapping the repository
            // Also, bootstrap property keys are lower case, Repo expects camel case
            Map<String,Object> bootConfig = new HashMap<String,Object>();
            bootConfig.put(OrientDBRepoService.CONFIG_DB_URL, repoConfig.get(OrientDBRepoService.CONFIG_DB_URL.toLowerCase()).getObject());
            bootConfig.put(OrientDBRepoService.CONFIG_USER, repoConfig.get(OrientDBRepoService.CONFIG_USER.toLowerCase()).getObject());
            bootConfig.put(OrientDBRepoService.CONFIG_PASSWORD, repoConfig.get(OrientDBRepoService.CONFIG_PASSWORD.toLowerCase()).getObject());
            bootConfig.put(OrientDBRepoService.CONFIG_POOL_MIN_SIZE, repoConfig.get(OrientDBRepoService.CONFIG_POOL_MIN_SIZE.toLowerCase()).getObject());
            bootConfig.put(OrientDBRepoService.CONFIG_POOL_MAX_SIZE, repoConfig.get(OrientDBRepoService.CONFIG_POOL_MAX_SIZE.toLowerCase()).getObject());
            // Forward the full DB schema (dbStructure) to the bootstrap repo so that
            // all OrientDB classes/clusters/indexes are created during the first
            // storage open.  DBHelper.checkDB temporarily disables USE_WAL while
            // setting up the schema, but USE_WAL is read at storage-open time, so
            // the disable only takes effect on the very first open.  If the
            // bootstrap creates the storage with only the "config" class, the WAL
            // writer is started and any later schema additions performed by the
            // full-service activation pay the full O(N²) WAL flush cost.  By
            // creating the entire schema during bootstrap (when USE_WAL is
            // genuinely off) the second getPool call simply finds every class
            // already present and skips all expensive schema work.
            Object dbStructure = repoConfig.get(OrientDBRepoService.CONFIG_DB_STRUCTURE.toLowerCase()).getObject();
            if (dbStructure != null) {
                bootConfig.put(OrientDBRepoService.CONFIG_DB_STRUCTURE, dbStructure);
            }

            // Init the bootstrap repo
            bootSvc = OrientDBRepoService.getRepoBootService(bootConfig);
             
            // Register bootstrap repo
            Hashtable<String, String> prop = new Hashtable<String, String>();
            prop.put("service.pid", "org.forgerock.openidm.bootrepo.orientdb");
            prop.put("openidm.router.prefix", "bootrepo");
            prop.put("db.type", "OrientDB");
            context.registerService(RepoBootService.class.getName(), bootSvc, prop);
            logger.info("Registered bootstrap repo service");
        } else {
            logger.debug("No OrientDB configuration detected");
        }
        logger.trace("OrientDB bundle started");
    }

    public void stop(BundleContext context) {
        if (bootSvc != null) {
            logger.debug("Cleaning up OrientDB bootstrap repository");
            bootSvc.cleanup();
        }
        logger.trace("OrientDB bundle stopped");
    }
}
