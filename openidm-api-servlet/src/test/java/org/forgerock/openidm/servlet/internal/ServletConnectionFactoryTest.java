/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.servlet.internal;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ConnectionProvider;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.PersistenceConfig;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.Router;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.script.scope.FunctionFactory;
import org.forgerock.script.source.DirectoryContainer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A test of the ServletConnectionFactory and underlying request handler.
 * 
 */
public class ServletConnectionFactoryTest {

    private Connection testable = null;

    @BeforeClass
    public void BeforeClass() throws Exception {

        URL config = ServletConnectionFactoryTest.class.getResource("/conf/router.json");
        Assert.assertNotNull(config, "router configuration is not found");

        JsonValue configuration =
                new JsonValue((new ObjectMapper()).readValue(new File(config.toURI()), Map.class));

        final Router requestHandler = new Router();
        requestHandler.addRoute("/audit/recon", new MemoryBackend());
        requestHandler.addRoute("/managed/user", new MemoryBackend());
        requestHandler.addRoute("/system/OpenDJ/account", new MemoryBackend());
        requestHandler.addRoute("/system/AD/account", new MemoryBackend());

        final ConnectionFactory connectionFactory = Resources.newInternalConnectionFactory(requestHandler);
        Bindings globalScope = new SimpleBindings();
        globalScope.put("openidm", FunctionFactory.getResource(connectionFactory));

        ScriptRegistryImpl sr =
                new ScriptRegistryImpl(new HashMap<String, Object>(), ServiceLoader
                        .load(ScriptEngineFactory.class), globalScope);
        sr.setPersistenceConfig(PersistenceConfig.builder().connectionProvider(
                new ConnectionProvider() {
                    @Override
                    public Connection getConnection(String connectionId) throws ResourceException {
                        return connectionFactory.getConnection();
                    }

                    @Override
                    public String getConnectionId(Connection connection) throws ResourceException {
                        return "DEFAULT";
                    }
                }).build());

        URL script = ServletConnectionFactoryTest.class.getResource("/script/");
        Assert.assertNotNull(script, "Failed to find /recon/script folder in test");
        sr.addSourceUnit(new DirectoryContainer("script", script));

        ServletConnectionFactory filterService = new ServletConnectionFactory();
        filterService.bindRequestHandler(requestHandler);
        filterService.bindScriptRegistry(sr);

        testable = Resources.newInternalConnection(filterService.init(configuration, requestHandler));
    }

    @Test
    public void testActivate() throws Exception {
        JsonValue content = new JsonValue(new HashMap<String, Object>());
        Resource user1 = testable.create(
                new RootContext(),
                Requests.newCreateRequest("/managed/user", content));
    }
}
