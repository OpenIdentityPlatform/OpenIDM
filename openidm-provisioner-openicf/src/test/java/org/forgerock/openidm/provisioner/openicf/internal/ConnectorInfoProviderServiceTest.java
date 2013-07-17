/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.provisioner.openicf.internal;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.provisioner.openicf.ConnectorReference;
import org.forgerock.openidm.provisioner.openicf.impl.BundleContextStub;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorKey;
import org.osgi.service.component.ComponentContext;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 *
 */
public class ConnectorInfoProviderServiceTest {

    private Dictionary properties = null;
    private JsonValue connectorInfoProviderServiceConfiguration = null;

    protected ConnectorInfoProviderService testableConnectorInfoProvider = null;

    @BeforeTest
    public void beforeTest() throws Exception {
        InputStream inputStream =
                ConnectorInfoProviderServiceTest.class
                        .getResourceAsStream("/config/provisioner.openicf.connectorinfoprovider.json");
        Assert.assertNotNull(inputStream);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[1024];
        int read;
        while ((read = inputStream.read(temp)) > 0) {
            buffer.write(temp, 0, read);
        }
        String config = new String(buffer.toByteArray());
        connectorInfoProviderServiceConfiguration =
                new JsonValue((new ObjectMapper()).readValue(config, Map.class));

        properties = new Hashtable<String, Object>();
        properties.put(JSONEnhancedConfig.JSON_CONFIG_PROPERTY, config);
        beforeMethod();
    }

    public void beforeMethod() throws Exception {
        Map<String, String> systemProperties = getTestSystemConfiguration();

        ComponentContext context = mock(ComponentContext.class);
        // stubbing
        when(context.getProperties()).thenReturn(properties);
        when(context.getBundleContext()).thenReturn(new BundleContextStub(systemProperties));
        InnerConnectorInfoProviderService instance = new InnerConnectorInfoProviderService();
        instance.activate(context);
        testableConnectorInfoProvider = instance;
    }

    // @Test
    public void testActivateProperly() throws Exception {
        Map<String, String> systemProperties = getTestSystemConfiguration();

        ComponentContext context = mock(ComponentContext.class);
        // stubbing
        when(context.getProperties()).thenReturn(properties);
        when(context.getBundleContext()).thenReturn(new BundleContextStub(systemProperties));
        InnerConnectorInfoProviderService instance = new InnerConnectorInfoProviderService();
        instance.activate(context);
    }

    // @Test(expectedExceptions = ComponentException.class)
    // public void testActivateNoConfiguration() throws Exception {
    // ComponentContext context = mock(ComponentContext.class);
    // //stubbing
    // when(context.buildServiceProperties()).thenReturn(new Hashtable<String,
    // String>());
    // when(context.getBundleContext()).thenReturn(new BundleContextStub());
    // InnerConnectorInfoProviderService newBuilder = new
    // InnerConnectorInfoProviderService();
    // newBuilder.activate(context);
    // }

    @Test
    public void testFindConnectorInfo() throws Exception {
        Map<String, String> systemProperties = getTestSystemConfiguration();
        ComponentContext context = mock(ComponentContext.class);
        // stubbing
        when(context.getProperties()).thenReturn(properties);
        when(context.getBundleContext()).thenReturn(new BundleContextStub(systemProperties));
        InnerConnectorInfoProviderService instance = new InnerConnectorInfoProviderService();
        instance.activate(context);

        ConnectorReference ref =
                new ConnectorReference(new ConnectorKey(
                        "org.forgerock.openicf.connectors.file.openicf-xml-connector", "1.1.0.0",
                        "com.forgerock.openicf.xml.XMLConnector"));
        Assert.assertNotNull(testableConnectorInfoProvider.findConnectorInfo(ref),
                "XML Connector is missing");

    }

    @Test
    public void testCreateSystemConfiguration() throws URISyntaxException {
        ConnectorInfo xmlConnectorInfo = null;
        ConnectorKey key =
                new ConnectorKey("org.forgerock.openicf.connectors.file.openicf-xml-connector",
                        "1.1.0.0", "com.forgerock.openicf.xml.XMLConnector");
        for (ConnectorInfo info : testableConnectorInfoProvider.getAllConnectorInfo()) {
            if (key.equals(info.getConnectorKey())) {
                xmlConnectorInfo = info;
                break;
            }
        }
        Assert.assertNotNull(xmlConnectorInfo);
        APIConfiguration configuration = xmlConnectorInfo.createDefaultAPIConfiguration();
        URL xmlRoot = ConnectorInfoProviderServiceTest.class.getResource("/xml/");
        Assert.assertNotNull(xmlRoot);
        URI xsdIcfFilePath = xmlRoot.toURI().resolve("resource-schema-1.xsd");
        configuration.getConfigurationProperties().setPropertyValue("xsdIcfFilePath",
                new File(xsdIcfFilePath));
        URI xsdFilePath = xmlRoot.toURI().resolve("ef2bc95b-76e0-48e2-86d6-4d4f44d4e4a4.xsd");
        configuration.getConfigurationProperties().setPropertyValue("xsdFilePath",
                new File(xsdFilePath));
        URI xmlFilePath = xmlRoot.toURI().resolve("data.xml");
        configuration.getConfigurationProperties().setPropertyValue("xmlFilePath",
                new File(xmlFilePath));

        try {
            ObjectMapper mapper = new ObjectMapper();
            URL root = ConnectorInfoProviderServiceTest.class.getResource("/");
            mapper.writeValue(new File((new URL(root, "XMLConnector_configuration.json")).toURI()),
                    testableConnectorInfoProvider.createSystemConfiguration(configuration, true));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testGetAllConnectorInfo() throws Exception {
        List<ConnectorInfo> result = testableConnectorInfoProvider.getAllConnectorInfo();
        assertThat(result).isNotNull().as(
                "XML connector must be in /connectorServer/connectors/ directory").isNotEmpty();
    }

    private Map<String, String> getTestSystemConfiguration() throws Exception {
        URL root = ConnectorInfoProviderServiceTest.class.getResource("/connectorServer/");
        Assert.assertNotNull(root);
        String rootPath = URLDecoder.decode(root.getPath(), "UTF-8");
        Map<String, String> systemProperties = new HashMap<String, String>(1);
        systemProperties.put(ServerConstants.PROPERTY_SERVER_ROOT, rootPath);
        return systemProperties;
    }

    public class InnerConnectorInfoProviderService extends ConnectorInfoProviderService {
        @Override
        public void activate(ComponentContext context) {
            super.activate(context);
        }

        @Override
        public void deactivate(ComponentContext context) {
            super.deactivate(context);
        }
    }

    @Test()
    public void testPropertiesToEncrypt() throws Exception {
        InputStream inputStream =
                ConnectorInfoProviderServiceTest.class
                        .getResourceAsStream("/config/org.forgerock.openidm.provisioner.openicf.impl.OpenICFProvisionerServiceSolarisConnectorTest.json");
        Assert.assertNotNull(inputStream);
        Map config = (new ObjectMapper()).readValue(inputStream, Map.class);

        List<JsonPointer> result =
                testableConnectorInfoProvider.getPropertiesToEncrypt(OpenICFProvisionerService.PID,
                        null, new JsonValue(config));
        String[] expected =
                new String[] { "/configurationProperties/password",
                    "/configurationProperties/credentials", "/configurationProperties/privateKey",
                    "/configurationProperties/passphrase" };
        for (JsonPointer pointer : result) {
            assertThat(expected).contains(pointer.toString());
        }

        inputStream =
                ConnectorInfoProviderServiceTest.class
                        .getResourceAsStream("/config/provisioner.openicf-xml.json");
        Assert.assertNotNull(inputStream);
        config = (new ObjectMapper()).readValue(inputStream, Map.class);

        result =
                testableConnectorInfoProvider.getPropertiesToEncrypt(OpenICFProvisionerService.PID,
                        null, new JsonValue(config));
        assertThat(result).hasSize(0);

        result =
                testableConnectorInfoProvider.getPropertiesToEncrypt(
                        ConnectorInfoProviderService.PID, null,
                        connectorInfoProviderServiceConfiguration);
        assertThat(result).hasSize(1);
        Assert.assertEquals(result.get(0).toString(), "/remoteConnectorServers/0/key");
    }
}
