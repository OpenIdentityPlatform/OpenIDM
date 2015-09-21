/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.config.manage;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openidm.config.manage.ConfigObjectService.asConfigQueryFilter;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

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

import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.config.crypto.ConfigCrypto;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.metadata.impl.ProviderListener;
import org.forgerock.util.query.QueryFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Test class for {@link ConfigObjectService}
 */
public class ConfigObjectServiceTest {

    @SuppressWarnings("rawtypes")
	private Dictionary properties = null;
    private ConfigObjectService configObjectService;

    private ResourcePath rname;
    private String id;
    private Map<String,Object> config;
    private ConfigurationAdmin configAdmin;
    private EnhancedConfig enhancedConfig;

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
        when(context.getProperties()).thenReturn(properties);
        configObjectService = new ConfigObjectService();

        configAdmin = new MockConfigurationAdmin();

        // no accessible bindConfigurationAdmin() method
        Field field = ConfigObjectService.class.getDeclaredField("configAdmin");
        field.setAccessible(true);
        field.set(configObjectService, configAdmin);

        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundles()).thenReturn(new Bundle[0]);

        // Init the ConfigCrypto instance used by ConfigObjectService
        ConfigCrypto.getInstance(bundleContext, mock(ProviderListener.class));

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.getConnection()).thenReturn(mock(Connection.class));

        enhancedConfig = mock(EnhancedConfig.class);
        configObjectService.bindEnhancedConfig(enhancedConfig);
        configObjectService.bindConnectionFactory(connectionFactory);

        configObjectService.activate(context);

        rname = new ResourcePath("testobject");
        id = "testid";
        config = new HashMap<String,Object>();
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
        Assert.assertEquals(asConfigQueryFilter(filter1).toString(), "/jsonconfig/field1 eq \"value1\"");
        Assert.assertEquals(asConfigQueryFilter(filter2).toString(), "(/jsonconfig/field1 eq \"value1\" and /service__pid eq \"value2\")");
        Assert.assertEquals(asConfigQueryFilter(filter3).toString(), "/jsonconfig/field1 pr");
        Assert.assertEquals(asConfigQueryFilter(filter4).toString(), "/jsonconfig/field1 lt 1");
        Assert.assertEquals(asConfigQueryFilter(filter5).toString(), "true");
    }
    
    @Test(priority=2)
    public void testParsedResourceName() throws Exception {
        
        try {
            configObjectService.getParsedId("");
            Assert.fail("Invalid id: ''");
        } catch (BadRequestException e) {
        }
        try {
            configObjectService.getParsedId("//");
            Assert.fail("Invalid id: '//'");
        } catch (IllegalArgumentException e) {
        }
        try {
            configObjectService.getParsedId("a/b/c");
            Assert.fail("Invalid id: 'a/b/c'");
        } catch (BadRequestException e) {
        }

        Assert.assertEquals(configObjectService.getParsedId("/a"), "a");
        Assert.assertFalse(configObjectService.isFactoryConfig("/a"));

        Assert.assertEquals(configObjectService.getParsedId("a"), "a");
        Assert.assertFalse(configObjectService.isFactoryConfig("a"));

        Assert.assertEquals(configObjectService.getParsedId("b/"), "b");
        Assert.assertFalse(configObjectService.isFactoryConfig("b/"));

        Assert.assertEquals(configObjectService.getParsedId("c/d"), "c-d");
        assertTrue(configObjectService.isFactoryConfig("c/d"));

        Assert.assertEquals(configObjectService.getParsedId("e/d/"), "e-d");
        assertTrue(configObjectService.isFactoryConfig("e/d/"));

        Assert.assertEquals(configObjectService.getParsedId(" f "), "_f_");
        Assert.assertFalse(configObjectService.isFactoryConfig(" f "));

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

        Dictionary properties = config.getProperties();
        EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
        JsonValue value = enhancedConfig.getConfiguration(properties, rname.toString(), false);
        assertTrue(value.keys().contains("property1"));
        Assert.assertEquals(value.get("property1").asString(), "value1");
    }

    @Test(priority=4, expectedExceptions = PreconditionFailedException.class)
    public void testCreateDupeFail() throws Exception {
        configObjectService.create(rname, id, json(config), false).getOrThrow();
        throw new Exception("Duplicate object not detected");
    }

    @Test(priority=5)
    public void testCreateDupeOk() throws Exception {
        when(enhancedConfig.getConfiguration(any(Dictionary.class), any(String.class), eq(false)))
                .thenReturn(json(config));

        configObjectService.create(rname, id, json(config), true).getOrThrow();

        ConfigObjectService.ParsedId parsedId = configObjectService.getParsedId(rname, id);
        Configuration config = configObjectService.findExistingConfiguration(parsedId);
        assertNotNull(config);
        assertNotNull(config.getProperties());
    }

    @SuppressWarnings("rawtypes")
	@Test(priority=6)
    public void testUpdate() throws Exception {
        config.put("property1", "newvalue1");
        config.put("property2", "newvalue2");

        when(enhancedConfig.getConfiguration(any(Dictionary.class), any(String.class), eq(false)))
                .thenReturn(json(config));

        configObjectService.update(rname, id, json(config)).getOrThrow();

        ConfigObjectService.ParsedId parsedId = configObjectService.getParsedId(rname, id);
        Configuration config = configObjectService.findExistingConfiguration(parsedId);
        assertNotNull(config);
        assertNotNull(config.getProperties());

        Dictionary properties = config.getProperties();
        JSONEnhancedConfig enhancedConfig = new JSONEnhancedConfig();
        JsonValue value = enhancedConfig.getConfiguration(properties, rname.toString(), false);
        assertTrue(value.keys().contains("property1"));
        Assert.assertEquals(value.get("property1").asString(), "newvalue1");
    }

    @Test(priority=7)
    public void testQuery() throws Exception {
        configObjectService.handleQuery(mock(Context.class),
                mock(QueryRequest.class), mock(QueryResourceHandler.class)).getOrThrow();
    }

    @Test(priority=8)
    public void testDelete() throws Exception {
        when(enhancedConfig.getConfiguration(any(Dictionary.class), any(String.class), eq(false))).thenReturn(
                json(object(field(ResourceResponse.FIELD_CONTENT_REVISION, "revX"))));

        configObjectService.delete(rname, "0").getOrThrow();

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

        configObjectService.update(rname, id, json(config)).getOrThrow();
    }


    /**
     * mock(ConfigurationAdmin.class) requires enough when() clauses to be functional to justify building this as an
     * explicit inner class for readability and debugging.
     */
    private class MockConfigurationAdmin implements ConfigurationAdmin {
        Map<String,Configuration> configurations = new HashMap<String, Configuration>();

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
        public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
            List<Configuration> configs = new ArrayList<Configuration>();

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
		Dictionary dictionary = null;
        Boolean deleted = false;

        String bundleLocation = "root";

        @Override
        public String getPid() {
            return deleted ? null : pid;
        }

        @Override
        public Dictionary getProperties() {
            return deleted ? null : dictionary;
        }

        @Override
        public void update(Dictionary properties) throws IOException {
            dictionary = properties;
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
        public void setBundleLocation(String bundleLocation) {
            this.bundleLocation = bundleLocation;
        }

        @Override
        public String getBundleLocation() {
            return deleted ? null : bundleLocation;
        }
    }
}
