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

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.forgerock.openidm.core.IdentityServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.openidm.repo.RepositoryService;

import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.config.OServerCommandConfiguration;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.orientechnologies.orient.server.config.OServerEntryConfiguration;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkListenerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkProtocolConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.config.OServerStorageConfiguration;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;

/**
 * Component for embedded OrientDB server
 *
 * @author aegloff
 */
public class EmbeddedOServerService {
    final static Logger logger = LoggerFactory.getLogger(EmbeddedOServerService.class);

    OServer orientDBServer;

    void activate(JsonNode config) throws Exception {
        logger.trace("Activating Service with configuration {}", config);
        try {
            String enabled = config.get("embeddedServer").get("enabled").defaultTo("false").asString();
            if ("true".equalsIgnoreCase(enabled)) {
                orientDBServer = OServerMain.create();
                OServerConfiguration serverConfig = getOrientDBConfig(config);
                orientDBServer.startup(serverConfig);
            }
        } catch (Exception ex) {
            logger.warn("Could not start OrientDB embedded server, service disabled.", ex);
            throw ex;
        }
    }

    void deactivate() {
        if (orientDBServer != null) {
            orientDBServer.shutdown();
            logger.debug("Embedded DB server stopped.");
        }
    }

    // TODO: make configurable
    protected OServerConfiguration getOrientDBConfig(JsonNode config) {

        OServerConfiguration configuration = new OServerConfiguration();

        configuration.handlers = new ArrayList<OServerHandlerConfiguration>();
        OServerHandlerConfiguration handler = new OServerHandlerConfiguration();
        handler.clazz = "com.orientechnologies.orient.server.handler.distributed.ODistributedServerManager";
        configuration.handlers.add(handler);
        handler.parameters = new OServerParameterConfiguration[]{
                new OServerParameterConfiguration("name", "default"),
                new OServerParameterConfiguration("security.algorithm", "Blowfish"),
                new OServerParameterConfiguration("network.multicast.address", "235.1.1.1"),
                new OServerParameterConfiguration("network.multicast.port", "2424"),
                new OServerParameterConfiguration("network.multicast.heartbeat", "10"),
                new OServerParameterConfiguration("server.update.delay", "5000"),
                new OServerParameterConfiguration("server.electedForLeadership", "true"),
                new OServerParameterConfiguration("security.key", "hw3CgjSzqm8I/axu"),
        };

        configuration.network = new OServerNetworkConfiguration();
        configuration.network.protocols = new ArrayList<OServerNetworkProtocolConfiguration>();
        OServerNetworkProtocolConfiguration protocol1 = new OServerNetworkProtocolConfiguration();
        protocol1.name = "binary";
        protocol1.implementation = "com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary";
        configuration.network.protocols.add(protocol1);
        OServerNetworkProtocolConfiguration protocol2 = new OServerNetworkProtocolConfiguration();
        protocol2.name = "http";
        protocol2.implementation = "com.orientechnologies.orient.server.network.protocol.http.ONetworkProtocolHttpDb";
        configuration.network.protocols.add(protocol2);
        OServerNetworkProtocolConfiguration protocol3 = new OServerNetworkProtocolConfiguration();
        protocol3.name = "distributed";
        protocol3.implementation = "com.orientechnologies.orient.server.network.protocol.distributed.ONetworkProtocolDistributed";
        configuration.network.protocols.add(protocol3);

        configuration.network.listeners = new ArrayList<OServerNetworkListenerConfiguration>();
        OServerNetworkListenerConfiguration listener1 = new OServerNetworkListenerConfiguration();
        listener1.ipAddress = "127.0.0.1";
        listener1.portRange = "2424-2430";
        listener1.protocol = "distributed";
        configuration.network.listeners.add(listener1);
        OServerNetworkListenerConfiguration listener2 = new OServerNetworkListenerConfiguration();
        listener2.ipAddress = "127.0.0.1";
        listener2.portRange = "2480-2490";
        listener2.protocol = "http";
        OServerCommandConfiguration command1 = new OServerCommandConfiguration();
        command1.pattern = "POST|*.action GET|*.action";
        command1.implementation = "com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostAction";
        command1.parameters = new OServerEntryConfiguration[0];

        // Access to the studio web app
        OServerCommandConfiguration command2 = new OServerCommandConfiguration();
        command2.pattern = "GET|www GET|studio/ GET| GET|*.htm GET|*.html GET|*.xml GET|*.jpeg GET|*.jpg GET|*.png GET|*.gif GET|*.js GET|*.css GET|*.swf GET|*.ico GET|*.txt";
        command2.implementation = "com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetStaticContent";
        command2.parameters = new OServerEntryConfiguration[2];
        command2.parameters[0] = new OServerEntryConfiguration("http.cache:*.htm *.html", "Cache-Control: no-cache, no-store, max-age=0, must-revalidate\r\nPragma: no-cache");
        command2.parameters[1] = new OServerEntryConfiguration("http.cache:default", "Cache-Control: max-age=120");

        listener2.commands = new OServerCommandConfiguration[]{
                command1,
                command2
        };

        listener2.parameters = new OServerParameterConfiguration[0];
        configuration.network.listeners.add(listener2);

        OServerStorageConfiguration storage1 = new OServerStorageConfiguration();
        storage1.name = "temp";
        storage1.path = "memory:temp";
        storage1.userName = "admin";
        storage1.userPassword = "admin";
        storage1.loadOnStartup = false;
        File dbFolder = IdentityServer.getFileForPath("db/openidm");
        String dbURL = config.get(OrientDBRepoService.CONFIG_DB_URL).defaultTo("local:" + dbFolder.getAbsolutePath()).asString();
        String user = config.get(OrientDBRepoService.CONFIG_USER).defaultTo("admin").asString();
        String pwd = config.get(OrientDBRepoService.CONFIG_PASSWORD).defaultTo("admin").asString();

        OServerStorageConfiguration storage2 = new OServerStorageConfiguration();
        storage2.name = "openidm";
        storage2.path = dbURL;
        storage2.userName = user;
        storage2.userPassword = pwd;
        storage2.loadOnStartup = false;

        configuration.storages = new OServerStorageConfiguration[]{
                storage1,
                storage2
        };

        // TODO: make configurable
        configuration.users = new OServerUserConfiguration[]{
                new OServerUserConfiguration("root", "3358BE3413F53E0D3DDA03C95C0A3F8357D0D160F8186EDA0C191CE9A4FA271B", "*")
        };
        configuration.properties = new OServerEntryConfiguration[]{
                new OServerEntryConfiguration("server.cache.staticResources", "false"),
                new OServerEntryConfiguration("orientdb.www.path", "db/util/orientdb/studio")
                //new OServerEntryConfiguration("log.console.level", "info"),
                //new OServerEntryConfiguration("log.file.level", "fine")

        };

        return configuration;
    }

}