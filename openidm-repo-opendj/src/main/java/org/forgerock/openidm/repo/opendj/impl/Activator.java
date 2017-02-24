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
 * Copyright 2017 ForgeRock AS.
 */
package org.forgerock.openidm.repo.opendj.impl;

import static org.forgerock.opendj.server.embedded.ConfigParameters.configParams;
import static org.forgerock.opendj.server.embedded.ConnectionParameters.connectionParams;
import static org.forgerock.opendj.server.embedded.EmbeddedDirectoryServer.manageEmbeddedDirectoryServer;
import static org.forgerock.opendj.server.embedded.ImportParameters.importParams;
import static org.forgerock.opendj.server.embedded.SetupParameters.setupParams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.logging.LogManager;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.json.JsonValue;
import org.forgerock.opendj.server.embedded.EmbeddedDirectoryServer;
import org.forgerock.opendj.server.embedded.EmbeddedDirectoryServerException;
import org.forgerock.opendj.setup.LicenseFile;
import org.forgerock.openidm.config.persistence.ConfigBootstrapHelper;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.repo.RepoBootService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi bundle activator
 */
public class Activator implements BundleActivator {
    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    private EmbeddedDirectoryServer embeddedServer;

    @Override
    public void start(BundleContext context) throws Exception {
        logger.info("OpenDJ bundle starting");

        final JsonValue repoConfig = ConfigBootstrapHelper.getRepoBootConfig("opendj", context);

        if (repoConfig == null) {
            logger.debug("No OpenDJ repo config detected");
            logger.debug("OpenDJ repo bundle not started");
            return;
        }

        if (repoConfig.get("embedded").defaultTo(true).asBoolean()) {
            embeddedServer = initializeEmbeddedServer();
            context.registerService(EmbeddedDirectoryServer.class.getName(), embeddedServer, null);
        }

        // Bootstrap repo
        RepoBootService bootSvc = OpenDJRepoService.getRepoBootService(embeddedServer, repoConfig);

        // Register bootstrap repo
        Hashtable<String, String> prop = new Hashtable<String, String>();
        prop.put(Constants.SERVICE_PID, "org.forgerock.openidm.bootrepo.opendj");
        prop.put("openidm.router.prefix", "bootrepo");
        prop.put("db.type", "OpenDJ");
        prop.put("db.dirname", "opendj");

        context.registerService(RepoBootService.class.getName(), bootSvc, prop);

        logger.info("Registered bootstrap repository service");
        logger.info("OpenDJ bundle started");
    }

    @Override
    public void stop(BundleContext context) {
        logger.info("OpenDJ bundle stopped");
        if (embeddedServer != null) {
            embeddedServer.stop(this.getClass().getName(), LocalizableMessage.raw("DJ bundle shutdown"));
        }
    }

    private EmbeddedDirectoryServer initializeEmbeddedServer() throws EmbeddedDirectoryServerException {
        logger.info("Starting embedded OpenDJ instance");

        final Path djRootDir = IdentityServer.getFileForPath("db/openidm-dj/opendj").toPath();
        final Path djConfig = djRootDir.resolve("config").resolve("config.ldif");

        // Set the OpenDJ INSTALL_ROOT
        System.setProperty("INSTALL_ROOT", djRootDir.toString());

        // approve the license file
        final LicenseFile licenseFile = LicenseFile.getInstance();
        licenseFile.approve(true);

        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            /*
             * DJ uses the context classloader in a way that is not compatible with OSGi.
             * Swap it for the current and then put it back after DJ initialization is complete.
             */
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            final EmbeddedDirectoryServer server =
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

            // config ldif does not exist, server has not been setup
            if (!djConfig.toFile().exists()) {
                setupEmbeddedServer(server);
            }

            if (!server.isRunning()) {
                try {
                    server.start();
                } catch (final EmbeddedDirectoryServerException e) {
                    logger.error("Failed to start embedded OpenDJ instance", e);
                    throw e;
                }
            }

            return server;
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    private void setupEmbeddedServer(final EmbeddedDirectoryServer server) {
        final File schemaLdif = IdentityServer.getFileForPath("db/opendj/schema/openidm.ldif");
        final String dataLdif = IdentityServer.getFileForPath("db/opendj/scripts/populate_users.ldif").toString();

        try {
            logger.info("Performing initial setup of embedded OpenDJ instance");
            server.setup(
                    setupParams()
                            .baseDn("dc=openidm,dc=forgerock,dc=com")
                            .backendType("pdb")
                            .jmxPort(1689));
        } catch (final EmbeddedDirectoryServerException e) {
            logger.error("Failed to setup embedded OpenDJ instance", e);
            return;
        }

        try {
            final File schemaDestination =
                    IdentityServer.getFileForPath("db/openidm-dj/opendj/config/schema/openidm.ldif");
            if (schemaDestination.getParentFile().getParentFile().mkdirs()) {
                logger.debug("opendj/onfig parentDirectory created");
            }
            if (schemaDestination.getParentFile().mkdirs()) {
                logger.debug("opendj/config/schema parentDirectory created");
            }
            Files.copy(
                    schemaLdif.toPath(),
                    schemaDestination.toPath());
        } catch (final IOException e) {
            logger.error("Failed to setup schema for embedded OpenDJ instance", e);
            return;
        }

        try {
            server.start();
        } catch (final EmbeddedDirectoryServerException e) {
            logger.error("Failed to start embedded OpenDJ instance", e);
            return;
        }

        try {
            server.importLDIF(
                    importParams()
                            .backendId("userRoot")
                            .ldifFile(dataLdif));
        } catch (final EmbeddedDirectoryServerException e) {
            logger.error("Failed to import ldif for embedded OpenDJ instance", e);
            return;
        }

        try {
            /*
             *  OpenDJ disables JDK logging when doing embeddedServer.importLDIF() and when that command
             *  eventually creates and LDAPConnection. OpenDJ also modifies the log level of all loggers
             *  when importing the LDIF if the verbose flag is sent. The JDK logging modification are done
             *  with the following utility class:
             *  https://stash.forgerock.org/projects/OPENDJ/repos/opendj/browse/opendj-server-legacy/src/main/java/org/opends/server/loggers/JDKLogging.java
             *
             *  Removing this functionality from OpenDJ would be a large task. It is easier to just reload
             *  logging.properties by reading the configuration here.
             */
            LogManager.getLogManager().readConfiguration();

            // tell the top level handler to use the root loggers handlers
            LogManager.getLogManager().getLogger("org").setUseParentHandlers(true);
        } catch (final IOException e) {
            logger.error("Unable to reload logging.properties");
        }
    }
}