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
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.provisioner.Id;
import org.osgi.service.component.ComponentConstants;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OpenICFProvisionerServiceXMLConnectorTest extends OpenICFProvisionerServiceTestBase {

    private List<String> objectIDs = new ArrayList<String>();


    protected String updateRuntimeConfiguration(String config) throws Exception {
        URL xmlRoot = OpenICFProvisionerServiceXMLConnectorTest.class.getResource("/xml/");

        URI xsdIcfFilePath = xmlRoot.toURI().resolve("resource-schema-1.xsd");
        URI xsdFilePath = xmlRoot.toURI().resolve("ef2bc95b-76e0-48e2-86d6-4d4f44d4e4a4.xsd");
        URI xmlFilePath = xmlRoot.toURI().resolve(UUID.randomUUID().toString() + ".xml");

        config = config.replaceFirst("XSDICFFILEPATH", xsdIcfFilePath.getPath());
        config = config.replaceFirst("XSDFILEPATH", xsdFilePath.getPath());
        config = config.replaceFirst("XMLFILEPATH", xmlFilePath.getPath());
        return config;
    }

    @Test
    public void testGetSystemIdentifier() throws Exception {
        Assert.assertNotNull(getService().getSystemIdentifier());
        Assert.assertTrue(getService().getSystemIdentifier().is(new Id("system/XML/account")));

    }

    @Test
    public void testGetStatus() throws Exception {
        JsonValue status = new JsonValue(getService().getStatus());
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
            String id = "system/xml/account/";
            getService().create(id, object);
            assertThat((String) object.get("_id")).as("Result object must contain the new id").doesNotMatch(".*/(.*?)").matches("[\\w]{8}-[\\w]{4}-[\\w]{4}-[\\w]{4}-[\\w]{12}");
            String newID = (String) object.get("_id");
            objectIDs.add(id+newID);
        }
    }

    @Test(dependsOnMethods = {"testCreate"})
    public void testRead() throws Exception {
        for (String id : objectIDs) {
            Map<String, Object> connectorObject = getService().read(id);
            Assert.assertNotNull(connectorObject);
            assertThat((String) connectorObject.get("_id")).as("Result object must contain the new id").doesNotMatch(".*/(.*?)").matches("[\\w]{8}-[\\w]{4}-[\\w]{4}-[\\w]{4}-[\\w]{12}");
            //assertThat(connectorObject).includes(MapAssert.entry("_id", id));
        }
    }

    @Test(dependsOnMethods = {"testCreate"})
    public void testUpdate() throws Exception {
        for (String id : objectIDs) {
            Map<String, Object> updates = new HashMap<String, Object>(5);
            updates.put("__PASSWORD__", "TestPassw0rd");
            updates.put("__GROUPS__", Arrays.asList("TestGroup1", "TestGroup2"));
            updates.put("__DESCRIPTION__", "Test Description");
            updates.put("firstname", "Darth");
            updates.put("lastname-first-letter", null);
            getService().update(id, null, updates);
            Map<String, Object> connectorObject = getService().read(id);
            assertThat(connectorObject).excludes(MapAssert.entry("__PASSWORD__", "TestPassw0rd"));
            assertThat(connectorObject).includes(MapAssert.entry("__GROUPS__", Arrays.asList("TestGroup1", "TestGroup2")));
            assertThat(connectorObject).includes(MapAssert.entry("__DESCRIPTION__", "Test Description"));
            assertThat(connectorObject).includes(MapAssert.entry("firstname", "Darth"));
            //assertThat(connectorObject).includes(MapAssert.entry("lastname-first-letter", null));
            assertThat((String) connectorObject.get("_id")).as("Result object must contain the new id").doesNotMatch(".*/(.*?)").matches("[\\w]{8}-[\\w]{4}-[\\w]{4}-[\\w]{4}-[\\w]{12}");

            //assertThat(connectorObject.keySet()).excludes("__PASSWORD__", "secret-pin", "jpegPhoto", "yearly-wage");
            Assert.assertNotNull(connectorObject);
        }
    }

    @Test(dependsOnMethods = {"testCreate"})
    public void testDelete() throws Exception {
        getService().delete(objectIDs.get(0), null);
        objectIDs.remove(0);
    }

    @Test
    public void testPatch() throws Exception {

    }

    @Test(dependsOnMethods = {"testCreate"})
    public void testQuery() throws Exception {
        InputStream inputStream = OpenICFProvisionerServiceXMLConnectorTest.class.getResourceAsStream("/test/queryConnectorObjects.json");
        Assert.assertNotNull(inputStream);
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> testInput = mapper.readValue(inputStream, List.class);
        for (Map<String, Object> object : testInput) {
            Map<String, Object> result = getService().query("system/xml/account/", object);
            Assert.assertNotNull(result);
        }
    }

    @Test(dependsOnMethods = {"testCreate"})
    public void testQueryAll() throws Exception {
        Map<String, Object> result = getService().query("system/xml/account", null);
        Assert.assertNotNull(result);
        Object resultObject = result.get("result");
        if (resultObject instanceof List) {
            assertThat((List) resultObject).isNotEmpty();
        } else {
            Assert.fail("Result must be instance of List");
        }
    }
}
