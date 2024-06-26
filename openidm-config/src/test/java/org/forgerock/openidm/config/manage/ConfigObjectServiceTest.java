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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openidm.config.manage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.config.manage.ConfigObjectService.asConfigQueryFilter;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.openidm.cluster.ClusterEvent;
import org.forgerock.openidm.cluster.ClusterEventListener;
import org.forgerock.openidm.cluster.ClusterManagementService;
import org.forgerock.openidm.config.crypto.ConfigCrypto;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.metadata.impl.ProviderListener;
import org.forgerock.openidm.patch.PatchValueTransformer;
import org.forgerock.openidm.patch.ScriptedPatchValueTransformer;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.script.ScriptedPatchValueTransformerFactory;
import org.forgerock.services.TransactionId;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.context.TransactionIdContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Test class for {@link ConfigObjectService}.
 */
public class ConfigObjectServiceTest {

    @SuppressWarnings("rawtypes")
	private Dictionary<String, Object> properties = null;
    private ConfigObjectService configObjectService;

    private ResourcePath rname;
    private String id;
    private Map<String,Object> config;
    private EnhancedConfig enhancedConfig;
    private ClusterManagementService clusterManagementService;

    @SuppressWarnings("unchecked")
	@BeforeTest
    public void beforeTest() throws Exception {
        properties = new Hashtable<>();
        properties.put(ComponentConstants.COMPONENT_NAME, getClass().getName());

        // Set root
        URL root = ConfigObjectServiceTest.class.getResource("/");
        assertNotNull(root);
        String rootPath = URLDecoder.decode(root.getPath(), "UTF-8");
        System.setProperty(ServerConstants.PROPERTY_SERVER_ROOT, rootPath);

        // Mock up supporting objects and activate ConfigObjectService
        ComponentContext context = mock(ComponentContext.class);
        final ConfigAuditEventLogger auditLogger = mock(ConfigAuditEventLogger.class);
        when(context.getProperties()).thenReturn(properties);
        configObjectService = new ConfigObjectService(auditLogger);

        // no accessible bindConfigurationAdmin() method
        Field field = ConfigObjectService.class.getDeclaredField("configAdmin");
        field.setAccessible(true);
        field.set(configObjectService, new MockConfigurationAdmin());

        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundles()).thenReturn(new Bundle[0]);

        // Init the ConfigCrypto instance used by ConfigObjectService
        ConfigCrypto.getInstance(bundleContext, mock(ProviderListener.class));

        Connection connection = mock(Connection.class);
        when(connection.create(any(Context.class), any(CreateRequest.class)))
                .thenReturn(newResourceResponse("", "", json(object())));

        IDMConnectionFactory connectionFactory = mock(IDMConnectionFactory.class);
        when(connectionFactory.getConnection()).thenReturn(connection);

        enhancedConfig = new JSONEnhancedConfig();
        clusterManagementService = mock(ClusterManagementService.class);

        when(auditLogger.log(any(ConfigAuditState.class), any(Request.class), any(Context.class),
                any(ConnectionFactory.class))).thenReturn(Responses.newResourceResponse("id", null, null).asPromise());

        doNothing().when(clusterManagementService).register(anyString(), any(ClusterEventListener.class));
        when(clusterManagementService.isEnabled()).thenReturn(true);
        when(clusterManagementService.getInstanceId()).thenReturn("instanceId");
        doNothing().when(clusterManagementService).sendEvent(any(ClusterEvent.class));

        configObjectService.bindClusterManagementService(clusterManagementService);
        configObjectService.bindEnhancedConfig(enhancedConfig);
        configObjectService.bindConnectionFactory(connectionFactory);
        configObjectService.bindScriptedPatchValueTransformerFactory(new NullScriptedPatchValueTransformerFactory());


        configObjectService.activate(context);

        rname = new ResourcePath("testobject");
        id = "testid";
        config = new HashMap<>();
    }

    @AfterTest
    public void afterTest() throws Exception {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getProperties()).thenReturn(properties);
        configObjectService.deactivate(context);
        configObjectService = null;
        enhancedConfig = null;
    }

    @Test(priority=1)
    public void testVisitor() throws Exception {
        // Config fields
        String configField = "/field1";
        String nonConfigField = "/service__pid";
        
        // QueryFilter Strings
        String queryString1 = configField + " eq \"value1\"";
        String queryString2 = "" + queryString1 + " and " + nonConfigField + " eq \"value2\"";
        String queryString3 = configField + " pr";
        String queryString4 = configField + " lt 1";
        String queryString5 = "true";
        
        // QueryFilters
        QueryFilter<JsonPointer> filter1 = QueryFilters.parse(queryString1);
        QueryFilter<JsonPointer> filter2 = QueryFilters.parse(queryString2);
        QueryFilter<JsonPointer> filter3 = QueryFilters.parse(queryString3);
        QueryFilter<JsonPointer> filter4 = QueryFilters.parse(queryString4);
        QueryFilter<JsonPointer> filter5 = QueryFilters.parse(queryString5);
        
        // Assertions
        assertEquals(asConfigQueryFilter(filter1).toString(), "/jsonconfig/field1 eq \"value1\"");
        assertEquals(asConfigQueryFilter(filter2).toString(),
                "(/jsonconfig/field1 eq \"value1\" and /service__pid eq \"value2\")");
        assertEquals(asConfigQueryFilter(filter3).toString(), "/jsonconfig/field1 pr");
        assertEquals(asConfigQueryFilter(filter4).toString(), "/jsonconfig/field1 lt 1");
        assertEquals(asConfigQueryFilter(filter5).toString(), "true");
    }
    
    @Test(priority=2)
    public void testParsedResourceName() throws Exception {
        
        try {
            configObjectService.getParsedId("");
            fail("Invalid id: ''");
        } catch (BadRequestException e) {
            // do nothing
        }
        try {
            configObjectService.getParsedId("//");
            fail("Invalid id: '//'");
        } catch (IllegalArgumentException e) {
            // do nothing
        }
        try {
            configObjectService.getParsedId("a/b/c");
            fail("Invalid id: 'a/b/c'");
        } catch (BadRequestException e) {
            // do nothing
        }

        assertEquals(configObjectService.getParsedId("/a"), "a");
        assertFalse(configObjectService.isFactoryConfig("/a"));

        assertEquals(configObjectService.getParsedId("a"), "a");
        assertFalse(configObjectService.isFactoryConfig("a"));

        assertEquals(configObjectService.getParsedId("b/"), "b");
        assertFalse(configObjectService.isFactoryConfig("b/"));

        assertEquals(configObjectService.getParsedId("c/d"), "c/d");
        assertTrue(configObjectService.isFactoryConfig("c/d"));

        assertEquals(configObjectService.getParsedId("e/d/"), "e/d");
        assertTrue(configObjectService.isFactoryConfig("e/d/"));

        assertEquals(configObjectService.getParsedId(" f "), "_f_");
        assertFalse(configObjectService.isFactoryConfig(" f "));

    }

    @SuppressWarnings("rawtypes")
	@Test(priority=3)
    public void testCreateNew() throws Exception {
        config.put("property1", "value1");
        config.put("property2", "value2");

        configObjectService.create(rname, id, json(config), false).getOrThrow();

        ConfigObjectService.ParsedId parsedId = configObjectService.getParsedId(rname, id);
        Configuration config = configObjectService.findExistingConfiguration(parsedId);
        assertNotNull(config);
        assertNotNull(config.getProperties());

        Dictionary<String, Object> properties = config.getProperties();
        EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
        JsonValue value = enhancedConfig.getConfiguration(properties, rname.toString(), false);
        assertTrue(value.keys().contains("property1"));
        assertEquals(value.get("property1").asString(), "value1");
    }

    @Test(priority=4, expectedExceptions = PreconditionFailedException.class)
    public void testCreateDupeFail() throws Exception {
        configObjectService.create(rname, id, json(config), false).getOrThrow();
        throw new Exception("Duplicate object not detected");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test(priority=5)
    public void testCreateDupeOk() throws Exception {
        configObjectService.create(rname, id, json(config), true).getOrThrow();

        ConfigObjectService.ParsedId parsedId = configObjectService.getParsedId(rname, id);
        Configuration config = configObjectService.findExistingConfiguration(parsedId);
        assertNotNull(config);
        assertNotNull(config.getProperties());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
	@Test(priority=6)
    public void testUpdate() throws Exception {
        config.put("property1", "newvalue1");
        config.put("property2", "newvalue2");

        configObjectService.handleUpdate(new TransactionIdContext(new RootContext(), new TransactionId()),
                newUpdateRequest(rname, id, json(config))).getOrThrow();

        ConfigObjectService.ParsedId parsedId = configObjectService.getParsedId(rname, id);
        Configuration config = configObjectService.findExistingConfiguration(parsedId);
        assertNotNull(config);
        assertNotNull(config.getProperties());

        Dictionary properties = config.getProperties();
        JSONEnhancedConfig enhancedConfig = new JSONEnhancedConfig();
        JsonValue value = enhancedConfig.getConfiguration(properties, rname.toString(), false);
        assertTrue(value.keys().contains("property1"));
        assertEquals(value.get("property1").asString(), "newvalue1");
    }

    @Test(priority=7)
    public void testQuery() throws Exception {
        configObjectService.handleQuery(new TransactionIdContext(new RootContext(), new TransactionId()),
                newQueryRequest(""), mock(QueryResourceHandler.class)).getOrThrow();
    }

    @SuppressWarnings("unchecked")
    @Test(priority=8)
    public void testDelete() throws Exception {
        configObjectService.handleDelete(new TransactionIdContext(new RootContext(), new TransactionId()),
                newDeleteRequest(rname, "0")).getOrThrow();

        ConfigObjectService.ParsedId parsedId = configObjectService.getParsedId(rname, id);
        Configuration config = configObjectService.findExistingConfiguration(parsedId);

        // "deleting" the object does not remove it from the configAdmin but it does invalidate the config
        assertNotNull(config);
        assertNull(config.getProperties());
    }

    @Test(priority=9, expectedExceptions = NotFoundException.class)
    public void testUpdateFail() throws Exception {
        config.put("property1", "newnewvalue1");
        config.put("property2", "newnewvalue2");

        configObjectService.handleUpdate(new TransactionIdContext(new RootContext(), new TransactionId()),
                newUpdateRequest(rname, id, json(config))).getOrThrow();
    }

    @Test(priority = 10)
    public void testRead() throws Exception {
        // Given
        config.put("property1", "evaluatedProperty");
        config.put("prop", "&{property1}");
        configObjectService.create(rname, id, json(config), true);

        // When
        ResourceResponse response = configObjectService.handleRead(
                ClientContext.buildExternalClientContext(
                        new TransactionIdContext(new RootContext(), new TransactionId())).build(),
                newReadRequest(rname, id)).getOrThrow();

        // Then
        assertThat(response.getContent().get("prop").asString()).isEqualTo("&{property1}");
    }

    @Test
    public void testPatchSendsClusterEvent() {
        // given
        final CreateRequest createRequest =
                Requests.newCreateRequest("path", "configObject", json(object(field("configField", "value"))));
        configObjectService.handleCreate(new RootContext(), createRequest);
        final PatchRequest request =
                Requests.newPatchRequest("path", "configObject", PatchOperation.replace("configField", "newValue"));

        // when
        final Promise<ResourceResponse, ResourceException> results =
                configObjectService.handlePatch(new RootContext(), request);

        // then
        verify(clusterManagementService, times(2)).sendEvent(any(ClusterEvent.class));
        assertThat(results).isNotNull().succeeded();
    }


    /**
     * mock(ConfigurationAdmin.class) requires enough when() clauses to be functional to justify building this as an
     * explicit inner class for readability and debugging.
     */
    private class MockConfigurationAdmin implements ConfigurationAdmin {
        Map<String,Configuration> configurations = new HashMap<>();

        @Override
        public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
            Configuration config = new MockConfiguration();
            configurations.put(factoryPid, config);
            return config;
        }

        @Override
        public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
            Configuration config = new MockConfiguration();
            configurations.put(factoryPid, config);
            return config;
        }

        @Override
        public Configuration getConfiguration(String pid, String location) throws IOException {
            return configurations.containsKey(pid) ? configurations.get(pid) : null;
        }

        @Override
        public Configuration getConfiguration(String pid) throws IOException {
            return configurations.containsKey(pid) ? configurations.get(pid) : null;
        }

        @Override
        public Configuration getFactoryConfiguration(String s, String s1, String s2) throws IOException {
            return null;
        }

        @Override
        public Configuration getFactoryConfiguration(String s, String s1) throws IOException {
            return null;
        }

        @Override
        public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
            List<Configuration> configs = new ArrayList<>();

            for (String key : configurations.keySet()) {
                if (filter.contains(key)) {
                    configs.add(configurations.get(key));
                }
            }
            if (!configs.isEmpty()) {
                return configs.toArray(new Configuration[configs.size()]);
            }
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    private class MockConfiguration implements Configuration {
        String pid = "pid";
		Dictionary<String, Object> dictionary = null;
        Boolean deleted = false;

        String bundleLocation = "root";

        @Override
        public String getPid() {
            return deleted ? null : pid;
        }

        @Override
        public Dictionary<String, Object> getProperties() {
            return deleted ? null : dictionary;
        }

        @Override
        public Dictionary<String, Object> getProcessedProperties(ServiceReference<?> serviceReference) {
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void update(Dictionary<String, ?> properties) throws IOException {
            dictionary = (Dictionary<String, Object>) properties;
        }

        @Override
        public void delete() throws IOException {
            deleted = true;
        }

        @Override
        public String getFactoryPid() {
            return deleted ? null : pid;
        }

        @Override
        public void update() throws IOException {

        }

        @Override
        public boolean updateIfDifferent(Dictionary<String, ?> dictionary) throws IOException {
            return false;
        }

        @Override
        public void setBundleLocation(String bundleLocation) {
            this.bundleLocation = bundleLocation;
        }

        @Override
        public String getBundleLocation() {
            return deleted ? null : bundleLocation;
        }

        @Override
        public long getChangeCount() {
            return 0;
        }

        @Override
        public void addAttributes(ConfigurationAttribute... configurationAttributes) throws IOException {

        }

        @Override
        public Set<ConfigurationAttribute> getAttributes() {
            return null;
        }

        @Override
        public void removeAttributes(ConfigurationAttribute... configurationAttributes) throws IOException {

        }
    }

    private class NullScriptedPatchValueTransformerFactory extends ScriptedPatchValueTransformerFactory {

        @Override
        public PatchValueTransformer getPatchValueTransformer(final Context context) {
            return new ScriptedPatchValueTransformer() {
                @Override
                public JsonValue evalScript(JsonValue subject, JsonValue scriptConfig, JsonPointer field)
                        throws ResourceException {
                    return subject;
                }
            };
        }
    }
}
