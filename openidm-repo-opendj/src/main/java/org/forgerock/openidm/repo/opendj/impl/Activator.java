/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2015 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.opendj.impl;

import com.forgerock.opendj.grizzly.GrizzlyTransportProvider;
import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.json.JsonValue;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.server.embedded.EmbeddedDirectoryServer;
import org.forgerock.opendj.server.embedded.EmbeddedDirectoryServerException;
import org.forgerock.openidm.config.persistence.ConfigBootstrapHelper;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.repo.RepoBootService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static org.forgerock.opendj.server.embedded.ConfigParameters.configParams;
import static org.forgerock.opendj.server.embedded.ConnectionParameters.connectionParams;
import static org.forgerock.opendj.server.embedded.EmbeddedDirectoryServer.manageEmbeddedDirectoryServer;
import static org.forgerock.opendj.server.embedded.SetupParameters.setupParams;

/**
 * OSGi bundle activator
 */
public class Activator implements BundleActivator {
    final static Logger logger = LoggerFactory.getLogger(Activator.class);

    private EmbeddedDirectoryServer embeddedServer;
    private RepoBootService repoBootService;
    
    public void start(BundleContext context) {
        logger.info("OpenDJ bundle starting");

        // TODO: Setup RepoBootService

        final JsonValue repoConfig = ConfigBootstrapHelper.getRepoBootConfig("opendj", context);

        final Path djRootDir = IdentityServer.getFileForPath("db/openidm-dj/opendj").toPath();
        final Path djConfig = djRootDir.resolve("config").resolve("config.ldif");
        final String schemaLdif = IdentityServer.getFileForPath("db/opendj/schema/openidm.ldif").toString();
        final String dataLdif = IdentityServer.getFileForPath("db/opendj/scripts/populate_users.ldif").toString();

        if (repoConfig != null && repoConfig.get("embedded").isNotNull() && repoConfig.get("embedded").asBoolean()) {
            logger.info("Starting embedded OpenDJ instance");

            embeddedServer =
                    manageEmbeddedDirectoryServer(
                            configParams()
                                    .serverRootDirectory(djRootDir.toString())
                                    .configurationFile(djConfig.toString()),
                            connectionParams()
                                    .hostName("localhost")
                                    .ldapPort(1389)
                                    .bindDn("cn=Directory Manager")
                                    .bindPassword("password")
                                    .adminPort(4444),
                            System.out,
                            System.err);

            // if config exists server is setup
            if (djConfig.toFile().exists()) {
                try {
                    embeddedServer.start();
                } catch (EmbeddedDirectoryServerException e) {
                    logger.error("Failed to start embedded OpenDJ instance", e);
                    return;
                }
            } else {
                try {
                    logger.info("Performing initial setup of embedded OpenDJ instance");
                    embeddedServer.setup(
                            setupParams()
                                    .baseDn("dc=openidm,dc=forgerock,dc=com")
                                    .backendType("pdb")
                                    .ldifFile(schemaLdif)
                                    .ldifFile(dataLdif)
                                    .jmxPort(1689));
                } catch (EmbeddedDirectoryServerException e) {
                    logger.error("Failed to setup embedded OpenDJ instance", e);
                    return;
                }
            }
        }

        logger.info("OpenDJ bundle started");
    }

    public void stop(BundleContext context) {
        logger.info("OpenDJ bundle stopped");
        if (embeddedServer != null) {
            embeddedServer.stop(this.getClass().getName(), LocalizableMessage.raw("DJ bundle shutdown"));
        }
    }
}