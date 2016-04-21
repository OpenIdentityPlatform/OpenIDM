/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.provisioner.salesforce.internal;

import static org.testng.Assert.assertNotNull;

import java.net.URL;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.provisioner.salesforce.internal.data.SObjectDescribe;
import org.forgerock.openidm.provisioner.salesforce.internal.metadata.MetadataResourceProvider;
import org.forgerock.openidm.provisioner.salesforce.internal.metadata.MetadataResourceProviderTest;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class SalesforceConnectionTest {


    @Test(enabled = false)
    public void testIsCreateable() throws Exception {
        URL configURL = SalesforceConnectionTest.class.getResource("/User.json");
        assertNotNull(configURL);

        SObjectDescribe describe =
                SalesforceConnection.mapper
                        .readValue(configURL.openStream(), SObjectDescribe.class);

        Assert.assertTrue(describe.isCreateable());
    }

    @Test
    public void testTest() throws Exception {
        URL configURL = SalesforceConnectionTest.class.getResource("/salesforce.json");
        assertNotNull(configURL);

        Map<String, Object> config =
                SalesforceConnection.mapper.readValue(configURL.openStream(), Map.class);

        SalesforceConfiguration configuration =
                SalesforceConnection.mapper.convertValue(config.get("configurationProperties"),
                        SalesforceConfiguration.class);

        Assert.assertEquals(Double.toString(configuration.getVersion()), "29.0",
                "API Version is not match");

        Assert.assertTrue(configuration.getPredefinedQueries().containsKey("active-only"));
    }

    @Test
    public void testLoadMetadata() throws Exception {

        byte[] zipFile =
                IOUtils.toByteArray(MetadataResourceProviderTest.class
                        .getResourceAsStream("/metadata/metadata.zip"));
        String type = "samlssoconfig";
        String id = "Migrated_SAML_Config";
        ResourceResponse metaResource = MetadataResourceProvider.getMetadataResource(zipFile, type, id);

        assertNotNull(metaResource);

        Assert.assertNull(MetadataResourceProvider.getMetadataResource(zipFile, type, "Not_EXITS"));

        try {
            MetadataResourceProvider.getMetadataResource(zipFile, "Not_Supported", id);
            Assert.fail();
        } catch (NotFoundException e) {
            /* expected */
        }
    }
}
