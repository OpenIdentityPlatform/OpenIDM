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
package org.forgerock.openidm.repo.orientdb.impl;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.scr.annotations.Property;
import org.forgerock.openidm.config.persistence.ConfigBootstrapHelper;
import org.forgerock.openidm.repo.RepoBootService;
import org.forgerock.openidm.repo.RepositoryService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi bundle activator
 * @author aegloff
 */
public class Activator implements BundleActivator {
    final static Logger logger = LoggerFactory.getLogger(Activator.class);

     public void start(BundleContext context) {
         logger.debug("OrientDB bundle starting");
         
         Map repoConfig = ConfigBootstrapHelper.getRepoBootConfig("orientdb", context);
         
         if (repoConfig != null) {
             logger.info("Bootstrapping OrientDB repository");
             // Only take the configuration strictly needed for bootstrapping the repository
             // Also, bootstrap property keys are lower case, Repo expects camel case
             Map<String,Object> bootConfig = new HashMap<String,Object>();
             bootConfig.put(OrientDBRepoService.CONFIG_DB_URL, repoConfig.get(OrientDBRepoService.CONFIG_DB_URL.toLowerCase()));
             bootConfig.put(OrientDBRepoService.CONFIG_USER, repoConfig.get(OrientDBRepoService.CONFIG_USER.toLowerCase()));
             bootConfig.put(OrientDBRepoService.CONFIG_PASSWORD, repoConfig.get(OrientDBRepoService.CONFIG_PASSWORD.toLowerCase()));
             
             // Init the bootstrap repo
             RepoBootService bootSvc = OrientDBRepoService.getRepoBootService(bootConfig);
             
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
         logger.debug("OrientDB bundle started");
     }

     public void stop(BundleContext context) {
         logger.trace("OrientDB bundle stopped");
     }
}