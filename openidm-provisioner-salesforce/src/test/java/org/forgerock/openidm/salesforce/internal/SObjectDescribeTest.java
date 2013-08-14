/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.salesforce.internal;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.openidm.salesforce.internal.data.SObjectDescribe;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URL;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class SObjectDescribeTest {
    @Test
    public void testIsCreateable() throws Exception {
        URL configURL = SalesforceConnectionTest.class.getResource("/User.json");
        Assert.assertNotNull(configURL);
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().set (
                DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,false);

        SObjectDescribe describe = mapper.readValue(configURL.openStream(), SObjectDescribe.class);

        Assert.assertTrue(describe.isCreateable());
    }
}
