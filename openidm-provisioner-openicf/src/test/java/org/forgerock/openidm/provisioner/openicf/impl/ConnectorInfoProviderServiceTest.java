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
 *
 * $Id$
 */

package org.forgerock.openidm.provisioner.openicf.impl;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.openidm.config.installer.JSONConfigInstaller;
import org.forgerock.openidm.provisioner.openicf.ConnectorInfoProvider;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelperTest;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorKey;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ConnectorInfoProviderServiceTest {

    private Dictionary properties = null;

    protected ConnectorInfoProvider testableConnectorInfoProvider = null;

    @BeforeTest
    public void beforeTest() throws Exception {
        InputStream inputStream = ConnectorInfoProviderServiceTest.class.getResourceAsStream("/config/ConnectorInfoProviderServiceConfiguration.json");
        Assert.assertNotNull(inputStream);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[1024];
        int read;
        while ((read = inputStream.read(temp)) > 0) {
            buffer.write(temp, 0, read);
        }
        String config = new String(buffer.toByteArray());

        properties = new Hashtable<String, Object>();
        properties.put(JSONConfigInstaller.JSON_CONFIG_PROPERTY, config);
        beforeMethod();
    }

    public void beforeMethod() throws Exception {
        Map<String, String> systemProperties = getTestSystemConfiguration();

        ComponentContext context = mock(ComponentContext.class);
        //stubbing
        when(context.getProperties()).thenReturn(properties);
        when(context.getBundleContext()).thenReturn(new BundleContextStub(systemProperties));
        InnerConnectorInfoProviderService instance = new InnerConnectorInfoProviderService();
        instance.activate(context);
        testableConnectorInfoProvider = instance;
    }

    //@Test
    public void testActivateProperly() throws Exception {
        Map<String, String> systemProperties = getTestSystemConfiguration();

        ComponentContext context = mock(ComponentContext.class);
        //stubbing
        when(context.getProperties()).thenReturn(properties);
        when(context.getBundleContext()).thenReturn(new BundleContextStub(systemProperties));
        InnerConnectorInfoProviderService instance = new InnerConnectorInfoProviderService();
        instance.activate(context);
    }


//    @Test(expectedExceptions = ComponentException.class)
//    public void testActivateNoConfiguration() throws Exception {
//        ComponentContext context = mock(ComponentContext.class);
//        //stubbing
//        when(context.getProperties()).thenReturn(new Hashtable<String, String>());
//        when(context.getBundleContext()).thenReturn(new BundleContextStub());
//        InnerConnectorInfoProviderService instance = new InnerConnectorInfoProviderService();
//        instance.activate(context);
//    }

    @Test
    public void testFindConnectorInfo() throws Exception {
        Map<String, String> systemProperties = getTestSystemConfiguration();
        ComponentContext context = mock(ComponentContext.class);
        //stubbing
        when(context.getProperties()).thenReturn(properties);
        when(context.getBundleContext()).thenReturn(new BundleContextStub(systemProperties));
        InnerConnectorInfoProviderService instance = new InnerConnectorInfoProviderService();
        instance.activate(context);


    }

    @Test
    public void testCreateSystemConfiguration() throws URISyntaxException {
        ConnectorInfo xmlConnectorInfo = null;
        ConnectorKey key = new ConnectorKey("org.forgerock.openicf.bundles.file.xml", "1.1.0.0-SNAPSHOT", "com.forgerock.openicf.xml.XMLConnector");
        for (ConnectorInfo info : testableConnectorInfoProvider.getAllConnectorInfo()) {
            if (key.equals(info.getConnectorKey())) {
                xmlConnectorInfo = info;
                break;
            }
        }
        Assert.assertNotNull(xmlConnectorInfo);
        APIConfiguration configuration = xmlConnectorInfo.createDefaultAPIConfiguration();
        URL xmlRoot = OpenICFProvisionerServiceXMLConnectorTest.class.getResource("/xml/");
        Assert.assertNotNull(xmlRoot);
        URI xsdIcfFilePath = xmlRoot.toURI().resolve("resource-schema-1.xsd");
        configuration.getConfigurationProperties().setPropertyValue("xsdIcfFilePath", xsdIcfFilePath.getPath());
        URI xsdFilePath = xmlRoot.toURI().resolve("ef2bc95b-76e0-48e2-86d6-4d4f44d4e4a4.xsd");
        configuration.getConfigurationProperties().setPropertyValue("xsdFilePath", xsdFilePath.getPath());
        URI xmlFilePath = xmlRoot.toURI().resolve("data.xml");
        configuration.getConfigurationProperties().setPropertyValue("xmlFilePath", xmlFilePath.getPath());

        try {
            ObjectMapper mapper = new ObjectMapper();
            URL root = ObjectClassInfoHelperTest.class.getResource("/");
            mapper.writeValue(new File((new URL(root, "XMLConnector_configuration.json")).toURI()),
                    testableConnectorInfoProvider.createSystemConfiguration(configuration, true));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testGetAllConnectorInfo() throws Exception {
        List<ConnectorInfo> result = testableConnectorInfoProvider.getAllConnectorInfo();
        assertThat(result).isNotNull().as("XML connector must be in /connectorServer/connectors/ directory").isNotEmpty();
    }


    private Map<String, String> getTestSystemConfiguration() {
        URL root = ConnectorInfoProviderServiceTest.class.getResource("/connectorServer/");
        Assert.assertNotNull(root);
        Map<String, String> systemProperties = new HashMap<String, String>(1);
        systemProperties.put("bundles.configuration.location", root.getPath());
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
}
