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
import org.fest.assertions.MapAssert;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.openidm.config.installer.JSONConfigInstaller;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.openicf.ConnectorInfoProvider;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OpenICFProvisionerServiceXMLConnectorTest {

    private TestLocalConnectorInfoProviderStub connectorInfoProvider = new TestLocalConnectorInfoProviderStub();
    private Dictionary properties = null;
    private ProvisionerService service = null;
    private List<String> objectIDs = new ArrayList<String>();


    @BeforeTest
    public void BeforeTest() throws Exception {
        String configurationFile = "/config/" + OpenICFProvisionerServiceXMLConnectorTest.class.getCanonicalName() + ".json";

        InputStream inputStream = TestLocalConnectorInfoProviderStub.class.getResourceAsStream(configurationFile);
        Assert.assertNotNull(inputStream, "Missing Configuration File at: " + configurationFile);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[1024];
        int read;
        while ((read = inputStream.read(temp)) > 0) {
            buffer.write(temp, 0, read);
        }

        String config = new String(buffer.toByteArray());
        URL xmlRoot = OpenICFProvisionerServiceXMLConnectorTest.class.getResource("/xml/");

        URI xsdIcfFilePath = xmlRoot.toURI().resolve("resource-schema-1.xsd");
        URI xsdFilePath = xmlRoot.toURI().resolve("ef2bc95b-76e0-48e2-86d6-4d4f44d4e4a4.xsd");
        URI xmlFilePath = xmlRoot.toURI().resolve(UUID.randomUUID().toString() + ".xml");

        config = config.replaceFirst("XSDICFFILEPATH", xsdIcfFilePath.getPath());
        config = config.replaceFirst("XSDFILEPATH", xsdFilePath.getPath());
        config = config.replaceFirst("XMLFILEPATH", xmlFilePath.getPath());

        properties = new Hashtable<String, Object>(3);
        properties.put(JSONConfigInstaller.JSON_CONFIG_PROPERTY, config);
        //Answer to the Ultimate Question of Life, the Universe, and Everything (42)
        properties.put(ComponentConstants.COMPONENT_ID, 42);
        properties.put(ComponentConstants.COMPONENT_NAME, OpenICFProvisionerServiceXMLConnectorTest.class.getCanonicalName());

        service = new OpenICFProvisionerService();

        Method bind = OpenICFProvisionerService.class.getDeclaredMethod("bind", ConnectorInfoProvider.class);
        Assert.assertNotNull(bind);
        bind.invoke(service, connectorInfoProvider);
        Method activate = OpenICFProvisionerService.class.getDeclaredMethod("activate", ComponentContext.class);
        Assert.assertNotNull(activate);

        ComponentContext context = mock(ComponentContext.class);
        //stubbing
        when(context.getProperties()).thenReturn(properties);
        activate.invoke(service, context);

    }


    @Test
    public void testGetSystemIdentifier() throws Exception {
        Assert.assertNotNull(service.getSystemIdentifier());
        Assert.assertTrue(service.getSystemIdentifier().is(new URI("system/XML/")));

    }

    @Test
    public void testGetStatus() throws Exception {
        JsonNode status = new JsonNode(service.getStatus());
        status.expect(Map.class).get("name").required().asString().equals("XML");
        assertThat(status.asMap()).as("OK MUST be true").includes(MapAssert.entry("ok", true));
        assertThat(status.asMap()).includes(MapAssert.entry(ComponentConstants.COMPONENT_ID, 42));
    }

    @Test
    public void testCreate() throws Exception {
        InputStream inputStream = OpenICFProvisionerServiceXMLConnectorTest.class.getResourceAsStream("/test/createConnectorObjects.json");
        Assert.assertNotNull(inputStream);
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> testInput = mapper.readValue(inputStream, List.class);
        for (Map<String, Object> object : testInput) {
            String id = "/system/xml/account/";
            service.create(id, object);
            assertThat((String) object.get("_id")).as("Result object must contain the new id").matches(".*/account/(.*?)");
            String newID = (String) object.get("_id");
            objectIDs.add(newID);
        }
    }

    @Test(dependsOnMethods = {"testCreate"})
    public void testRead() throws Exception {
        for (String id : objectIDs) {
            Map<String, Object> connectorObject = service.read("/system/xml" + id);
            Assert.assertNotNull(connectorObject);
        }
    }

    @Test
    public void testUpdate() throws Exception {

    }

    @Test
    public void testDelete() throws Exception {

    }

    @Test
    public void testPatch() throws Exception {

    }

    //@Test(dependsOnMethods = {"testCreate"})
    public void testQuery() throws Exception {
        InputStream inputStream = OpenICFProvisionerServiceXMLConnectorTest.class.getResourceAsStream("/test/queryConnectorObjects.json");
        Assert.assertNotNull(inputStream);
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> testInput = mapper.readValue(inputStream, List.class);
        for (Map<String, Object> object : testInput) {
            Map<String, Object> result = service.query("/system/xml/account/something", null);
        }
    }
}
