/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.salesforce.internal;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
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

        SObjectDescribe describe = mapper.readValue(configURL, SObjectDescribe.class);

        Assert.assertTrue(describe.isCreateable());
    }
}
