/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.internal.recon;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionProvider;
import org.forgerock.json.resource.InMemoryBackend;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PersistenceConfig;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptName;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.script.scope.FunctionFactory;
import org.forgerock.script.source.DirectoryContainer;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class ExecutorPreferencesTest {

    static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private final String name;

    private final JsonValue configuration;

    private final ScriptRegistry scriptRegistry;

    private final ScriptEntry entry;

    private final Router requestHandler;

    @Factory(dataProvider = "fileDataProvider")
    public ExecutorPreferencesTest(String name, JsonValue configuration) throws ScriptException {
        this.name = name;
        this.configuration = configuration;

        ServiceLoader<ScriptEngineFactory> engineFactories =
                ServiceLoader.load(ScriptEngineFactory.class);

        requestHandler = new Router();
        requestHandler.addRoute("/audit/recon", new InMemoryBackend());
        requestHandler.addRoute("/repo/link/account", new InMemoryBackend());
        requestHandler.addRoute("/managed/user", new InMemoryBackend());
        requestHandler.addRoute("/system/OpenDJ/account", new InMemoryBackend());
        requestHandler.addRoute("/system/AD/account", new InMemoryBackend());

        Bindings globalScope = new SimpleBindings();
        globalScope.put("openidm", FunctionFactory.getResource());
        globalScope.put("logger", FunctionFactory.getLogger(name));

        ScriptRegistryImpl sr =
                new ScriptRegistryImpl(new HashMap<String, Object>(), engineFactories, globalScope);
        sr.setPersistenceConfig(PersistenceConfig.builder().connectionProvider(
                new ConnectionProvider() {
                    @Override
                    public Connection getConnection(String connectionId) throws ResourceException {
                        return Resources.newInternalConnection(requestHandler);
                    }

                    @Override
                    public String getConnectionId(Connection connection) throws ResourceException {
                        return "DEFAULT";
                    }
                }).build());

        URL script = ExecutorPreferencesTest.class.getResource("/script/");
        Assert.assertNotNull(script, "Failed to find /recon/script folder in test");
        sr.addSourceUnit(new DirectoryContainer("script", script));

        entry = sr.takeScript(new ScriptName("setup-" + name + ".groovy", "groovy"));
        Assert.assertNotNull(entry, "setUp-test.groovy is not found!");

        requestHandler.addRoute("/sync", new Verifier(entry));

        Script s = entry.getScript(new RootContext());
        s.put("phase", "setup");

        // This should succeed if the system is ready for test.
        s.eval();

        scriptRegistry = sr;
    }


    @Test
    public void testGetConfiguration() throws Exception {

        ConfigurationProvider config =
                new ConfigurationProvider(name, configuration.get("relation"));
        config.setTarget(configuration.get("target"));
        config.setSource(configuration.get("source"));
        config.setLink(configuration.get("link"));
        config.setPolicies(configuration.get("policies"));

        Assert.assertNotNull(config.getSource());
        Assert.assertNotNull(config.getTarget());
        Assert.assertEquals(config.getRelation(), ConfigurationProvider.Mode.ONE_TO_ONE);

        ServerContext context =
                new ServerContext(new RootContext(), Resources
                        .newInternalConnection(requestHandler));

        ExecutorPreferences preferences =
                ExecutorPreferences.builder().scriptRegistry(scriptRegistry).configurationProvider(
                        config).build(name, context);

        ReconExecutor executor = new ReconExecutor(context, preferences);
        executor.execute();

        mapper.writeValue(System.out, executor.getStatistics().asMap());

        Script s = entry.getScript(new RootContext());
        s.put("phase", "assert");
        s.eval();
    }

    @DataProvider
    public static Iterator<Object[]> fileDataProvider(ITestContext context) throws Exception {

        URL confUrl = ExecutorPreferencesTest.class.getResource("/conf");
        Assert.assertNotNull(confUrl, "Failed to find /conf folder in test");
        File file = new File(confUrl.toURI());
        List<Object[]> arguments = new ArrayList<Object[]>();

        if (file.isDirectory()) {
            FileFilter filter = new FileFilter() {

                public boolean accept(File f) {
                    return (!f.isDirectory()) || (f.getName().endsWith(".json"));
                }
            };
            File[] files = file.listFiles(filter);
            for (File subFile : files) {
                if (!subFile.getName().startsWith("recon-")){
                    continue;
                }
                String name = subFile.getName().split("-")[1].replace(".json", "");

                //if (!name.equals("oto02")) continue;

                Map configuration = mapper.readValue(subFile, Map.class);
                arguments.add(new Object[] { name, new JsonValue(configuration) });
            }
        }
        return arguments.iterator();
    }

    class Verifier implements SingletonResourceProvider {

        private final ScriptEntry scriptEntry;

        Verifier(ScriptEntry scriptEntry) {
            this.scriptEntry = scriptEntry;
        }

        @Override
        public void actionInstance(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            try {
                Script script = scriptEntry.getScript(context);
                script.put("context", context);
                script.put("request", request);
                script.put("phase", "sync");
                handler.handleResult(new JsonValue(script.eval()));
            } catch (ScriptThrownException e) {
                handler.handleError(e.toResourceException(500, "sync script failed"));
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e.getMessage(), e));
            }
        }

        @Override
        public void patchInstance(final ServerContext context, final PatchRequest request,
                final ResultHandler<Resource> handler) {
            final ResourceException e =
                    new NotSupportedException("Patch operations are not supported");
            handler.handleError(e);
        }

        @Override
        public void readInstance(final ServerContext context, final ReadRequest request,
                final ResultHandler<Resource> handler) {
            final ResourceException e =
                    new NotSupportedException("Read operations are not supported");
            handler.handleError(e);
        }

        @Override
        public void updateInstance(final ServerContext context, final UpdateRequest request,
                final ResultHandler<Resource> handler) {
            final ResourceException e =
                    new NotSupportedException("Update operations are not supported");
            handler.handleError(e);
        }
    }
}
