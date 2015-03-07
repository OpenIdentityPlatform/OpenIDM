///*
// * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
// *
// * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
// *
// * The contents of this file are subject to the terms
// * of the Common Development and Distribution License
// * (the License). You may not use this file except in
// * compliance with the License.
// *
// * You can obtain a copy of the License at
// * http://forgerock.org/license/CDDLv1.0.html
// * See the License for the specific language governing
// * permission and limitations under the License.
// *
// * When distributing Covered Code, include this CDDL
// * Header Notice in each file and include the License file
// * at http://forgerock.org/license/CDDLv1.0.html
// * If applicable, add the following below the CDDL Header,
// * with the fields enclosed by brackets [] replaced by
// * your own identifying information:
// * "Portions Copyrighted [year] [name of copyright owner]"
// */
//
//package org.forgerock.openidm.provisioner.openicf.commons;
//
//import org.codehaus.jackson.map.ObjectMapper;
//import org.forgerock.json.fluent.JsonValue;
//import org.forgerock.json.resource.Resource;
//import org.forgerock.openidm.core.ServerConstants;
//import org.forgerock.openidm.provisioner.openicf.connector.TestConnector;
//import org.identityconnectors.framework.api.operations.UpdateApiOp;
//import org.identityconnectors.framework.common.objects.ConnectorObject;
//import org.identityconnectors.framework.common.objects.ObjectClassInfo;
//import org.identityconnectors.framework.common.objects.Schema;
//import org.testng.Assert;
//import org.testng.annotations.Test;
//
//import java.io.File;
//import java.io.InputStream;
//import java.net.URL;
//import java.util.HashMap;
//import java.util.Map;
//
//
//public class ObjectClassInfoHelperTest {
//    @Test
//    public void testGetNativeJavaType() throws Exception {
//        TestConnector connector = new TestConnector();
//        Schema schema = connector.schema();
//        ObjectClassInfo account = schema.findObjectClassInfo("__ACCOUNT__");
//        Map schemaMAP = ConnectorUtil.getObjectClassInfoMap(account);
//        Assert.assertNotNull(schema);
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            URL root = ObjectClassInfoHelperTest.class.getResource("/");
//            mapper.writeValue(new File((new URL(root, "schema.json")).toURI()), schemaMAP);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
//
//
//    @Test
//    public void testConnectorObjectName() throws Exception {
//        InputStream inputStream = ObjectClassInfoHelperTest.class.getResourceAsStream("/config/SystemSchemaConfiguration.json");
//        Assert.assertNotNull(inputStream);
//        ObjectMapper mapper = new ObjectMapper();
//        JsonValue configuration = new JsonValue(mapper.readValue(inputStream, Map.class));
//
//        ObjectClassInfoHelper helper = new ObjectClassInfoHelper(configuration.get("objectTypes").get("__ACCOUNT__"));
//        JsonValue source = new JsonValue(new HashMap<String, Object>());
//        source.put(Resource.FIELD_CONTENT_ID, "ID_NAME");
//        source.put("__NAME__", "NAME_NAME");
//
//        ConnectorObject co = helper.build(UpdateApiOp.class, "rename", source, null);
//        Assert.assertEquals(co.getName().getNameValue(), "rename");
//
//        co = helper.build(UpdateApiOp.class, null, source, null);
//        Assert.assertEquals(co.getName().getNameValue(), "ID_NAME");
//    }
//
//
//}
