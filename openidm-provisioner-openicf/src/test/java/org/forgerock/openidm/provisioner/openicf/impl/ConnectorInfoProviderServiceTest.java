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
 * Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.openidm.provisioner.openicf.impl;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openicf.framework.ConnectorFrameworkFactory;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.provisioner.openicf.ConnectorReference;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.logging.impl.NoOpLogger;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorKey;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 *
 */
public class ConnectorInfoProviderServiceTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private Dictionary<String, Object> properties = null;
    private JsonValue connectorInfoProviderServiceConfiguration = null;

    protected ConnectorInfoProviderService testableConnectorInfoProvider = null;

    @BeforeTest
    public void beforeTest() throws Exception {
        System.setProperty(Log.LOGSPI_PROP, NoOpLogger.class.getName());
        InputStream inputStream =
                ConnectorInfoProviderServiceTest.class
                        .getResourceAsStream("/config/provisioner.openicf.connectorinfoprovider.json");
        assertThat(inputStream).isNotNull();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[1024];
        int read;
        while ((read = inputStream.read(temp)) > 0) {
            buffer.write(temp, 0, read);
        }
        String config = new String(buffer.toByteArray());
        connectorInfoProviderServiceConfiguration = new JsonValue(mapper.readValue(config, Map.class));

        properties = new Hashtable<>();
        properties.put(ComponentConstants.COMPONENT_NAME, getClass().getName());
        properties.put(JSONEnhancedConfig.JSON_CONFIG_PROPERTY, config);

        // Set root
        URL root = ConnectorInfoProviderServiceTest.class.getResource("/");
        assertThat(root).isNotNull();
        String rootPath = URLDecoder.decode(root.getPath(), "UTF-8");
        System.setProperty(ServerConstants.PROPERTY_SERVER_ROOT, rootPath);

        ComponentContext context = mock(ComponentContext.class);
        // stubbing
        when(context.getProperties()).thenReturn(properties);
        ConnectorInfoProviderService instance = new ConnectorInfoProviderService();
        instance.bindEnhancedConfig(new JSONEnhancedConfig());
        instance.bindConnectorFrameworkFactory(new ConnectorFrameworkFactory());
        instance.activate(context);
        testableConnectorInfoProvider = instance;
    }

    @AfterTest
    public void afterTest() throws Exception {
        ComponentContext context = mock(ComponentContext.class);
        // stubbing
        when(context.getProperties()).thenReturn(properties);
        testableConnectorInfoProvider.deactivate(context);
        testableConnectorInfoProvider = null;
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
        // TODO: Find a better way to do this that doesn't include hard-coding the connector revision
        ConnectorReference ref =
                new ConnectorReference(new ConnectorKey(
                        "org.forgerock.openicf.connectors.xml-connector", "1.1.0.3",
                        "org.forgerock.openicf.connectors.xml.XMLConnector"));
        assertThat(testableConnectorInfoProvider.findConnectorInfo(ref))
                .isNotNull()
                .overridingErrorMessage("XML Connector is missing");

    }

    @Test
    public void testCreateSystemConfiguration() throws URISyntaxException {
        // TODO: Find a better way to do this that doesn't include hard-coding the connector revision
        ConnectorReference connectorReference =
                new ConnectorReference(new ConnectorKey(
                        "org.forgerock.openicf.connectors.xml-connector", "1.1.0.3",
                        "org.forgerock.openicf.connectors.xml.XMLConnector"));
        ConnectorInfo xmlConnectorInfo = null;
        ConnectorKey key =
                new ConnectorKey("org.forgerock.openicf.connectors.xml-connector", "1.1.0.3",
                        "org.forgerock.openicf.connectors.xml.XMLConnector");
        for (ConnectorInfo info : testableConnectorInfoProvider.getAllConnectorInfo()) {
            if (key.equals(info.getConnectorKey())) {
                xmlConnectorInfo = info;
                break;
            }
        }

        assertThat(xmlConnectorInfo).isNotNull();
        APIConfiguration configuration = xmlConnectorInfo.createDefaultAPIConfiguration();
        URL xmlRoot = ConnectorInfoProviderServiceTest.class.getResource("/xml/");
        assertThat(xmlRoot).isNotNull();
        URI xsdIcfFilePath = xmlRoot.toURI().resolve("resource-schema-1.xsd");
        configuration.getConfigurationProperties().setPropertyValue("xsdIcfFilePath",
                new File(xsdIcfFilePath));
        URI xsdFilePath = xmlRoot.toURI().resolve("ef2bc95b-76e0-48e2-86d6-4d4f44d4e4a4.xsd");
        configuration.getConfigurationProperties().setPropertyValue("xsdFilePath",
                new File(xsdFilePath));
        URI xmlFilePath = xmlRoot.toURI().resolve("data.xml");
        configuration.getConfigurationProperties().setPropertyValue("xmlFilePath",
                new File(xmlFilePath));
        configuration.getConfigurationProperties().setPropertyValue("createFileIfNotExists", true);

        try {
            URL root = ConnectorInfoProviderServiceTest.class.getResource("/");
            mapper.writeValue(new File((new URL(root, "XMLConnector_configuration.json")).toURI()),
                    testableConnectorInfoProvider.createSystemConfiguration(connectorReference, configuration, true));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testGetAllConnectorInfo() throws Exception {
        List<ConnectorInfo> result = testableConnectorInfoProvider.getAllConnectorInfo();
        assertThat(result).isNotNull().as("XML connector must be in /connectors/ directory")
                .isNotEmpty();
    }

    @Test(enabled = false)
    public void testPropertiesToEncrypt() throws Exception {
        InputStream inputStream =
                ConnectorInfoProviderServiceTest.class
                        .getResourceAsStream("/config/org.forgerock.openidm.provisioner.openicf.impl.OpenICFProvisionerServiceSolarisConnectorTest.json");
        assertThat(inputStream).isNotNull();

        List<JsonPointer> result =
                testableConnectorInfoProvider.getPropertiesToEncrypt(OpenICFProvisionerService.PID,
                        null, new JsonValue(mapper.readValue(inputStream, Map.class)));
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
        assertThat(inputStream).isNotNull();

        result =
                testableConnectorInfoProvider.getPropertiesToEncrypt(OpenICFProvisionerService.PID,
                        null, new JsonValue(mapper.readValue(inputStream, Map.class)));
        assertThat(result).hasSize(0);

        result =
                testableConnectorInfoProvider.getPropertiesToEncrypt(
                        ConnectorInfoProviderService.PID, null,
                        connectorInfoProviderServiceConfiguration);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).toString()).isEqualTo("/remoteConnectorServers/0/key");
    }
}
