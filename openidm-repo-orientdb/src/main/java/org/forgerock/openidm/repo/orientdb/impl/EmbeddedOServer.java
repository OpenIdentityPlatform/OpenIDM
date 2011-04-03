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

import java.lang.reflect.Field;
import java.util.ArrayList;

import com.orientechnologies.orient.core.config.OEntryConfiguration;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.config.OServerCommandConfiguration;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkListenerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkProtocolConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.config.OServerStorageConfiguration;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Work with the OrientDB team so the workaround of extending is not necessary anymore
// and configuration can be provided externally
/**
 * Embedded OrientDB Server Workaround
 */
public class EmbeddedOServer extends OServer {
    final static Logger logger = LoggerFactory.getLogger(EmbeddedOServer.class);

    private static Thread serverThread;
    private static OServer server;
    
    public EmbeddedOServer() throws ClassNotFoundException, MalformedObjectNameException, NullPointerException,
    InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        super();
    }
    
    @Override
    protected void loadConfiguration() {
        configuration = new OServerConfiguration();
        configuration.handlers = new ArrayList<OServerHandlerConfiguration>();
        OServerHandlerConfiguration handler = new OServerHandlerConfiguration();
        handler.clazz = "com.orientechnologies.orient.server.handler.distributed.ODistributedServerManager";
        configuration.handlers.add(handler);
        handler.parameters = new OServerParameterConfiguration[] {
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
        command1.parameters = new OEntryConfiguration[0];
        listener2.commands = new OServerCommandConfiguration[] {
                command1
        };
        listener2.parameters = new OServerParameterConfiguration[0];
        configuration.network.listeners.add(listener2);
        
        OServerStorageConfiguration storage1 = new OServerStorageConfiguration();
        storage1.name = "temp";
        storage1.path = "memory:temp";
        storage1.userName = "admin";
        storage1.userPassword = "admin";
        storage1.loadOnStartup = true;
        OServerStorageConfiguration storage2 = new OServerStorageConfiguration();
        storage2.name = "openidm";
        storage2.path = "local:./db/openidm";
        storage2.userName = "admin";
        storage2.userPassword = "admin";
        storage2.loadOnStartup = true;
        configuration.storages = new OServerStorageConfiguration[] {
                storage1,
                storage2
        };
        configuration.users = new OServerUserConfiguration[] {
                new OServerUserConfiguration("root", "3358BE3413F53E0D3DDA03C95C0A3F8357D0D160F8186EDA0C191CE9A4FA271B", "*")
        };
        configuration.properties = new OEntryConfiguration[] {
                new OEntryConfiguration("server.cache.staticResources", "false"),
                new OEntryConfiguration("log.console.level", "info"),
                new OEntryConfiguration("log.file.level", "fine")
        };

        //loadStorages();
        //loadUsers();
    }

    public static void startEmbedded() {
         logger.debug("Start embedded DB server");
         try {
             serverThread = new Thread() {
                 public void run() {
                     try {
                         // TODO: work with OrientDB team to also address the following
                         // If embedded server affects logging configuration work with OrientDB team
                         // Server also needs this property, work with OrientDB team if they can remove this
                         System.setProperty("ORIENTDB_HOME", new java.io.File(".").getAbsolutePath());
                         server = new EmbeddedOServer();
                         // can't "just" start OServer as some code uses OServerMain.server()
                         // Hack until the OrientDB team fixes that.
                         Field serverField = OServerMain.class.getDeclaredField("server");
                         serverField.setAccessible(true);
                         serverField.set(null, server);
                         server.startup();
                     } catch (Exception ex) {
                         logger.warn("Embedded server failure", ex);
                     }
                 }
             };
             serverThread.start();
         } catch (Exception ex) {
             ex.printStackTrace();
         }
     }
    
    public static void stopEmbedded() {
        if (server != null) {
            server.shutdown();
            logger.debug("Stop embedded DB server");
        }
    }
}