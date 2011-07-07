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
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.openidm.provisioner.openicf.OperationHelper;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.impl.api.APIConfigurationImpl;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OperationHelperImplTest {

    private OperationHelperBuilder builder;

    private String multipleBooleans = "{\"OR\": [{ \"AND\": [ { \"Equals\": { \"field\" : \"firstname\" } }, { \"StartsWith\": {\"field\" : \"lastname\", \"values\": [ \"R\" ]} } ] }, { \"LessThan\": { \"field\" : \"age\" } } ] }";

    private String superChained = "{ \"AND\": [ { \"Equals\": { \"field\" : \"address\", \"values\": [ \"Oslo\" ] } }, { \"EndsWith\": { \"field\" : \"test\", \"values\" : [ \"tull\" ] } }, { \"OR\": [ { \"EndsWith\": { \"field\" : \"lastname\", \"values\" : [ \"en\" ] } }, { \"EndsWith\": { \"field\" : \"lastname\", \"values\" : [ \"on\" ] } } ] } ] }";

    @BeforeTest
    public void beforeTest() throws Exception {
        String configurationFile = "/config/" + OpenICFProvisionerServiceXMLConnectorTest.class.getCanonicalName() + ".json";
        InputStream inputStream = OperationHelperImplTest.class.getResourceAsStream(configurationFile);
        Assert.assertNotNull(inputStream, "Missing Configuration File at: " + configurationFile);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonConfiguration = new JsonNode(mapper.readValue(inputStream, Map.class));

        APIConfiguration config = new APIConfigurationImpl();
        builder = new OperationHelperBuilder("xml",jsonConfiguration, config);
    }


    @Test
    public void testBuild() throws Exception {
        OperationHelper helper = builder.build("account", null);

        ObjectMapper mapper = new ObjectMapper();
        Filter filter = helper.build(mapper.readValue(superChained, Map.class), null);
        Assert.assertNotNull(filter);

        Map<String, Object> params = new HashMap<String, Object>();

        List<Object> firstnameList = new ArrayList<Object>();
        firstnameList.add("John");
        params.put("firstname", firstnameList);

        List<Object> lastnameList = new ArrayList<Object>();
        lastnameList.add("15");
        params.put("age", lastnameList);

        filter = helper.build(mapper.readValue(multipleBooleans, Map.class), params);
        Assert.assertNotNull(filter);

    }
}
