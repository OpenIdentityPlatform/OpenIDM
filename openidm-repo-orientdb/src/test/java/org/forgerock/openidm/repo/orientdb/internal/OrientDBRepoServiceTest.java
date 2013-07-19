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

package org.forgerock.openidm.repo.orientdb.internal;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.fest.assertions.api.Assertions;
import org.fest.assertions.core.Condition;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.patch.JsonPatch;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.PropertyAccessor;
import org.forgerock.openidm.util.JsonUtil;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.OServerShutdownMain;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.orientechnologies.orient.server.config.OServerConfigurationLoaderXml;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class OrientDBRepoServiceTest {

    public static final String LAUNCHER_INSTALL_LOCATION = "launcher.install.location";
    public static final String LAUNCHER_INSTALL_URL = "launcher.install.url";
    public static final String LAUNCHER_WORKING_LOCATION = "launcher.working.location";
    public static final String LAUNCHER_WORKING_URL = "launcher.working.url";
    public static final String LAUNCHER_PROJECT_LOCATION = "launcher.project.location";
    public static final String LAUNCHER_PROJECT_URL = "launcher.project.url";

    @DataProvider(name = "dp")
    static public Iterator<Object[]> createData() throws Exception {
        List<Object[]> tests = new ArrayList<Object[]>();
        try {
            IdentityServer.initInstance(new PropertyAccessor() {
                @Override
                public <T> T getProperty(String key, T defaultValue, Class<T> expected) {
                    if (String.class.isAssignableFrom(expected)) {
                        try {
                            if (LAUNCHER_INSTALL_LOCATION.equals(key)
                                    || LAUNCHER_PROJECT_LOCATION.equals(key)
                                    || LAUNCHER_WORKING_LOCATION.equals(key)) {
                                return (T) URLDecoder.decode(OrientDBRepoServiceTest.class
                                        .getResource("/").getPath(), "utf-8");
                            } else if (LAUNCHER_INSTALL_URL.equals(key)
                                    || LAUNCHER_PROJECT_URL.equals(key)
                                    || LAUNCHER_WORKING_URL.equals(key)) {
                                return (T) OrientDBRepoServiceTest.class.getResource("/")
                                        .toString();
                            }
                        } catch (UnsupportedEncodingException e) {
                            /* ignore */
                        }
                    }
                    return null;
                }
            });
        } catch (IllegalStateException e) {
            /* ignore */
        }

        ObjectMapper mapper = JsonUtil.build();
        mapper.registerModule(new AfterburnerModule());
        JsonValue configuration =
                new JsonValue(mapper.readValue(OrientDBRepoServiceTest.class
                        .getResourceAsStream("/config/repo.orientdb.json"), Map.class));

        // Test#1
        configuration.getTransformers().add(new JSONEnhancedConfig.PropertyTransformer(false));
        configuration.applyTransformers();
        tests.add(new Object[] { configuration.copy() });

        // Test#2
        configuration.remove("embeddedServer");
        configuration.put(OrientDBRepoService.CONFIG_DB_URL, "remote:localhost/openidm");
        // configuration.put("dbUrl", "remote:localhost/openidm");
        tests.add(new Object[] { configuration.copy() });

        return tests.iterator();
    }

    private final JsonValue configuration;

    private Connection connection;
    private OrientDBRepoService repoService;

    @Factory(dataProvider = "dp")
    public OrientDBRepoServiceTest(final JsonValue configuration) throws Exception {
        this.configuration = configuration;
    }

    @BeforeClass
    public void setUp() throws Exception {
        if (!EmbeddedOServerService.isEnable(configuration)) {
            String OHOME =
                    URLDecoder.decode(OrientDBRepoServiceTest.class.getResource("/").getPath(),
                            "utf-8");
            System.setProperty("orientdb.config.file", OHOME + "config/orientdb-server-config.xml");
            System.setProperty("java.util.logging.config.file", OHOME
                    + "config/orientdb-server-log.properties");
            System.setProperty(Orient.ORIENTDB_HOME, OHOME);
            System.setProperty("java.awt.headless", "true");

            URL[] subclasses =
                    new URL[] {
                        OServerMain.class.getProtectionDomain().getCodeSource().getLocation(),
                        Orient.class.getProtectionDomain().getCodeSource().getLocation(),
                        com.orientechnologies.orient.enterprise.channel.binary.OChannelBinaryServer.class
                                .getProtectionDomain().getCodeSource().getLocation()

                    };
            final URLClassLoader loader =
                    new IsolatedClassLoader(subclasses, ClassLoader.getSystemClassLoader());

            if (Orient.class.equals(loader.loadClass(Orient.class.getName()))) {
                fail();
            }

            Method method =
                    loader.loadClass(OServerMain.class.getName()).getMethod("main", String[].class);
            method.invoke(null, new Object[] { new String[] {} });
        }
        Dictionary properties = new Hashtable<String, Object>(3);
        properties.put(ComponentConstants.COMPONENT_ID, 42);
        properties.put(ComponentConstants.COMPONENT_NAME, getClass().getCanonicalName());
        properties.put(JSONEnhancedConfig.JSON_CONFIG_PROPERTY, JsonUtil
                .writeValueAsString(configuration));

        ComponentContext context = mock(ComponentContext.class);
        // stubbing
        when(context.getProperties()).thenReturn(properties);

        repoService = new OrientDBRepoService();
        repoService.activate(context);

        Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "/repo/{partition}", repoService);
        connection = Resources.newInternalConnection(router);
    }

    @AfterClass
    public void tearDown() throws Exception {
        if (EmbeddedOServerService.isEnable(configuration)) {
            repoService.embeddedServer.getConnection().drop();
        } else {
            OServerAdmin admin =
                    new OServerAdmin(repoService.embeddedServer.getDBUrl(configuration));
            admin.connect("root",
                    "7A5DEAB30884B4C8026A047F13D4A67BDEDC7CA227AA8F4D477727EABE5541B4");
            admin.dropDatabase();
            OServerShutdownMain.main(new String[] { "localhost", "",
                "7A5DEAB30884B4C8026A047F13D4A67BDEDC7CA227AA8F4D477727EABE5541B4" });
        }
        repoService.deactivate(null);
    }

    @Test
    public void testHandlePatch() throws Exception {
        final JsonValue content = newContent();
        CreateRequest createRequest = Requests.newCreateRequest("/repo/managed/user", content);
        Resource resource0 = connection.create(new RootContext(), createRequest);
        assertNotNull(resource0.getId());

        PatchRequest patchRequest =
                Requests.newPatchRequest("/repo/managed/user", resource0.getId());
        patchRequest.setRevision(resource0.getRevision());
        patchRequest.addPatchOperation(PatchOperation.add("attribute", "New Value"));
        patchRequest.addPatchOperation(PatchOperation.increment("integer", 8));
        patchRequest.addPatchOperation(PatchOperation.increment("number", -0.14));
        patchRequest.addPatchOperation(PatchOperation.remove("string"));
        patchRequest.addPatchOperation(PatchOperation.replace("boolean", false));

        Resource resource1 = connection.patch(new RootContext(), patchRequest);

        ReadRequest readRequest = Requests.newReadRequest("/repo/managed/user", resource1.getId());
        Resource resource1Read = connection.read(new RootContext(), readRequest);
        assertThat(resource1Read.getContent().asMap()).contains(entry("attribute", "New Value"),
                entry("integer", 50), entry("number", 3.0), entry("boolean", false))
                .doesNotContainKey("string");
    }

    @Test
    public void testHandleRead() throws Exception {
        final JsonValue content = newContent();
        CreateRequest createRequest = Requests.newCreateRequest("/repo/config", "audit", content);
        Resource resource0 = connection.create(new RootContext(), createRequest);
        assertEquals(resource0.getId(), "audit");

        createRequest = Requests.newCreateRequest("/repo/config/system", "ldap", content);
        Resource resource1 = connection.create(new RootContext(), createRequest);
        assertEquals(resource1.getId(), "ldap");

        ReadRequest readRequest = Requests.newReadRequest("/repo/config", "audit");
        Resource resource0Read = connection.read(new RootContext(), readRequest);
        assertThat(resource0Read).is(
                new ResourceCondition("audit", resource0.getRevision(), content));

        readRequest = Requests.newReadRequest("/repo/config/system", "ldap");
        Resource resource1Read = connection.read(new RootContext(), readRequest);
        assertThat(resource1Read).is(
                new ResourceCondition("ldap", resource1.getRevision(), content));
    }

    @Test
    public void testHandleQuery() throws Exception {
        QueryRequest queryRequest = Requests.newQueryRequest("/repo/internal/user");
        queryRequest.setQueryId("get-by-field-value");
        queryRequest.setAdditionalQueryParameter("field", "userName");
        queryRequest.setAdditionalQueryParameter("value", "anonymous");
        Collection<Resource> results = new ArrayList<Resource>();
        QueryResult result = connection.query(new RootContext(), queryRequest, results);
        Assert.assertEquals(results.size(), 1);

        queryRequest = Requests.newQueryRequest("/repo/internal/user");
        queryRequest.setQueryId("get-users-of-role");
        queryRequest.setAdditionalQueryParameter("role", "openidm-authorized");
        results = new ArrayList<Resource>();
        result = connection.query(new RootContext(), queryRequest, results);
        Assert.assertEquals(results.size(), 1);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testHandleReadNonExisting() throws Exception {
        ReadRequest readRequest = Requests.newReadRequest("/repo/managed/user", "NOT_EXIST");
        connection.read(new RootContext(), readRequest);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testHandleReadNonExistingClass() throws Exception {
        ReadRequest readRequest = Requests.newReadRequest("/repo/nonexist", "NOT_EXIST");
        connection.read(new RootContext(), readRequest);
    }

    @Test
    public void testHandleCreate() throws Exception {
        // Request#1
        final JsonValue content = newContent();
        CreateRequest createRequest = Requests.newCreateRequest("/repo/managed/user", content);
        Resource resource0 = connection.create(new RootContext(), createRequest);
        ORecordId rid = new ORecordId(resource0.getId());
        assertEquals(rid.getClusterPosition().intValue(), 0);
        assertEquals(resource0.getRevision(), "1");

        ReadRequest readRequest = Requests.newReadRequest("/repo/managed/user", resource0.getId());
        Resource actual = connection.read(new RootContext(), readRequest);
        assertThat(actual).is(new ResourceCondition(null, "1", content));

        // Request#2
        createRequest = Requests.newCreateRequest("/repo/managed/user", "1", content);
        Resource resource1 = connection.create(new RootContext(), createRequest);
        try {
            new ORecordId(resource1.getId());
            fail("id must be '1'");
        } catch (IllegalArgumentException e) {
            /* expected */
        }
        assertEquals(resource1.getId(), "1");
        assertEquals(resource1.getRevision(), "0");
        readRequest = Requests.newReadRequest("/repo/managed/user", resource1.getId());
        actual = connection.read(new RootContext(), readRequest);
        assertThat(actual).is(new ResourceCondition(null, "0", content));

        // Request#3
        createRequest = Requests.newCreateRequest("/repo/custom/object", "2", content);
        Resource resource2 = connection.create(new RootContext(), createRequest);
        assertEquals(resource2.getId(), "2");
        assertEquals(resource2.getRevision(), "0");

        // Request#4 - Already exits
        try {
            connection.create(new RootContext(), createRequest);
        } catch (ConflictException e) {
            assertThat(e).hasMessageContaining("2");
        }

        // Request#5 - Route is /repo/{partition}
        try {
            connection.create(new RootContext(), Requests.newCreateRequest("/repo", content));
        } catch (NotFoundException e) {
            assertThat(e).hasMessageContaining("Resource '/repo' not found");
        }
    }

    @Test
    public void testHandleUpdate() throws Exception {
        final JsonValue content = newContent();
        CreateRequest createRequest = Requests.newCreateRequest("/repo/managed/user", content);
        Resource resource0 = connection.create(new RootContext(), createRequest);
        assertNotNull(resource0.getId());

        content.put("attribute", "New Value");
        UpdateRequest updateRequest =
                Requests.newUpdateRequest("/repo/managed/user", resource0.getId(), content);
        updateRequest.setRevision(resource0.getRevision());

        Resource resource1 = connection.update(new RootContext(), updateRequest);

        ReadRequest readRequest = Requests.newReadRequest("/repo/managed/user", resource1.getId());
        Resource resource1Read = connection.read(new RootContext(), readRequest);
        assertThat(resource1Read.getContent().asMap()).contains(entry("attribute", "New Value"));
    }

    @Test
    public void testHandleDelete() throws Exception {
        final JsonValue content = newContent();
        CreateRequest createRequest = Requests.newCreateRequest("/repo/managed/user", content);
        Resource resource0 = connection.create(new RootContext(), createRequest);
        Resource resource1 = connection.create(new RootContext(), createRequest);
        Resource resource2 = connection.create(new RootContext(), createRequest);

        DeleteRequest deleteRequest =
                Requests.newDeleteRequest("/repo/managed/user", resource0.getId());
        connection.delete(new RootContext(), deleteRequest);

        deleteRequest = Requests.newDeleteRequest("/repo/managed/user", resource1.getId());
        deleteRequest.setRevision(resource1.getRevision());
        connection.delete(new RootContext(), deleteRequest);

        // This request is wrong but we should handle it.
        deleteRequest = Requests.newDeleteRequest("/repo/managed/user", resource2.getId());
        deleteRequest.setRevision("*");
        connection.delete(new RootContext(), deleteRequest);
    }

    @Test(expectedExceptions = PreconditionFailedException.class)
    public void testHandleDeleteWithWrongVersion() throws Exception {
        final JsonValue content = newContent();
        CreateRequest createRequest = Requests.newCreateRequest("/repo/managed/user", content);
        Resource resource0 = connection.create(new RootContext(), createRequest);

        DeleteRequest deleteRequest =
                Requests.newDeleteRequest("/repo/managed/user", resource0.getId());
        deleteRequest.setRevision("33");
        connection.delete(new RootContext(), deleteRequest);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testHandleDeleteNonExisting() throws Exception {
        final JsonValue content = newContent();
        CreateRequest createRequest = Requests.newCreateRequest("/repo/managed/user", content);
        Resource resource0 = connection.create(new RootContext(), createRequest);

        DeleteRequest deleteRequest =
                Requests.newDeleteRequest("/repo/managed/user", resource0.getId());
        try {
            connection.delete(new RootContext(), deleteRequest);
        } catch (Exception e) {
            Assertions.fail("Deleting existing document", e);
        }
        connection.delete(new RootContext(), deleteRequest);
    }

    @Test
    public void testOServerConfiguration() throws Exception {
        ObjectMapper mapper = JsonUtil.build();

        OServerConfigurationLoaderXml loader =
                new OServerConfigurationLoaderXml(OServerConfiguration.class,
                        OrientDBRepoServiceTest.class
                                .getResourceAsStream("/config/orientdb-server-config.xml"));
        OServerConfiguration configuration = loader.load();

        Map<String, Object> config = mapper.convertValue(configuration, Map.class);

        assertThat(config).containsKey("location").containsKey("handlers").containsKey("hooks")
                .containsKey("network").containsKey("storages").containsKey("users").containsKey(
                        "security").containsKey("properties");

        FileOutputStream fop = null;
        File file;
        try {
            file =
                    new File(URLDecoder.decode(OrientDBRepoServiceTest.class.getResource("/")
                            .getPath(), "utf-8")
                            + "/orientdb-server-config.json");

            fop = new FileOutputStream(file);

            // if file does not exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            // get the content in bytes
            byte[] contentInBytes =
                    JsonUtil.writePrettyValueAsString(new JsonValue(config)).getBytes();

            fop.write(contentInBytes);
            fop.flush();
            fop.close();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                /* ignore */
            }
        }
    }

    private JsonValue newContent() {
        JsonValue object = new JsonValue(newMap());
        object.put("map", newMap());
        // https://github.com/nuvolabase/orientdb/issues/1521
        object.remove("list");
        return object;
    }

    private Map<String, Object> newMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("string", "Another Brick in the Wall");
        map.put("boolean", true);
        map.put("integer", 42);
        map.put("number", 3.14);
        Map<String, Object> mapList = new HashMap<String, Object>();
        mapList.put("e", 2.71);
        map.put("list", Arrays.asList(new Object[] { "We Built This City", 42, 3.14, false, null,
            Arrays.asList(new Object[] { "Marvin", "Pluto" }), mapList }));
        map.put("null", null);
        return map;
    }

    static class ResourceCondition extends Condition<Resource> {

        private final JsonValue content;
        private final String id;
        private final String revision;

        ResourceCondition(String id, String revision, JsonValue content) {
            super("Resource must match!");
            this.content = content;
            this.id = id;
            this.revision = revision;
        }

        @Override
        public boolean matches(Resource value) {
            for (String key : value.getContent().keys()) {
                if (key.startsWith("_")) {
                    value.getContent().remove(key);
                }
            }
            JsonValue patch = JsonPatch.diff(content, value.getContent());
            if (patch.size() > 0) {
                as(description() + " Diff: " + patch.toString());
            }
            return patch.size() == 0 && (null == id ? true : id.equals(value.getId()))
                    && (null == revision ? true : revision.equals(value.getRevision()));
        }
    }

    private static class IsolatedClassLoader extends URLClassLoader {
        private IsolatedClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        /**
         * Overrides <code>super.loadClass()</code>, to change loading model to
         * child-first.
         */
        @Override
        protected synchronized Class<?> loadClass(final String name, final boolean resolve)
                throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException ex) {
                    c = getParent().loadClass(name);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }

    }
}
