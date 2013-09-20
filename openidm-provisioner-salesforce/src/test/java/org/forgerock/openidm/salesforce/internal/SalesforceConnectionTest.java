/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.salesforce.internal;

import java.net.URL;
import java.util.Map;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class SalesforceConnectionTest {
    @Test(enabled = true)
    public void testTest() throws Exception {
        URL configURL = SalesforceConnectionTest.class.getResource("/salesforce.json");
        Assert.assertNotNull(configURL);
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().set(
                DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Map<String, Object> config = mapper.readValue(configURL.openStream(), Map.class);

        SalesforceConfiguration configuration = mapper.convertValue(config.get("configurationProperties"),
                SalesforceConfiguration.class);

        Assert.assertEquals(Double.toString(configuration.getVersion()),"28.0", "API Version is not match");

        Assert.assertTrue(configuration.getPredefinedQueries().containsKey("active-only"));
        //SalesforceConnection connection = new SalesforceConnection(configuration);
    }
}
