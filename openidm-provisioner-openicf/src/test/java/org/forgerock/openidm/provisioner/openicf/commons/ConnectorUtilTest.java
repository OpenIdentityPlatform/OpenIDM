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
 * $Id$
 */
package org.forgerock.openidm.provisioner.openicf.commons;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.provisioner.Id;
import org.forgerock.openidm.provisioner.SystemIdentifier;
import org.forgerock.openidm.provisioner.openicf.ConnectorReference;
import org.forgerock.openidm.provisioner.openicf.OperationHelper;
import org.forgerock.openidm.provisioner.openicf.connector.TestConfiguration;
import org.forgerock.openidm.provisioner.openicf.connector.TestConnector;
import org.forgerock.openidm.provisioner.openicf.impl.OperationHelperBuilder;
import org.forgerock.openidm.provisioner.openicf.impl.OperationHelperImpl;
import org.forgerock.openidm.provisioner.openicf.impl.SimpleSystemIdentifier;
import org.identityconnectors.common.Assertions;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.api.operations.*;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.serializer.SerializerUtil;
import org.identityconnectors.framework.impl.api.APIConfigurationImpl;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ConnectorUtilTest {

    private APIConfiguration runtimeAPIConfiguration = null;
    private JsonNode jsonConfiguration;

    @BeforeTest
    public void beforeTest() throws Exception {
        TestConfiguration configuration = new TestConfiguration();
        runtimeAPIConfiguration = TestHelpers.createTestConfiguration(TestConnector.class, configuration);

        InputStream inputStream = ConnectorUtilTest.class.getResourceAsStream("/config/TestSystemConnectorConfiguration.json");
        Assert.assertNotNull(inputStream);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[1024];
        int read;
        while ((read = inputStream.read(temp)) > 0) {
            buffer.write(temp, 0, read);
        }
        String config = new String(buffer.toByteArray());
        ObjectMapper mapper = new ObjectMapper();
        Map map = mapper.readValue(config, Map.class);
        jsonConfiguration = new JsonNode(map);
        Assert.assertNotNull(jsonConfiguration);
    }

    protected ConnectorFacade getFacade() {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        // **test only**
        return factory.newInstance(runtimeAPIConfiguration);
    }


    @Test
    public void testGetConfiguration() throws Exception {
        Map<String, Object> target = new LinkedHashMap<String, Object>();
        ConnectorUtil.createSystemConfigurationFromAPIConfiguration(runtimeAPIConfiguration, target);
        APIConfiguration clonedConfiguration = getRuntimeAPIConfiguration();
        ConnectorUtil.configureDefaultAPIConfiguration(new JsonNode(target), clonedConfiguration);
        //Assert.assertEquals(clonedConfiguration, runtimeAPIConfiguration);

        ObjectMapper mapper = new ObjectMapper();
        URL root = ConnectorUtilTest.class.getResource("/");
        mapper.writeValue(new File((new URL(root, "runtimeAPIConfiguration.json")).toURI()), target);
    }

    @Test
    public void testGetSchema() {
        ConnectorFacade connectorFacade = getFacade();
        Schema schema = connectorFacade.schema();
        Assert.assertNotNull(schema);
        Map<String, Object> schemaMAP = new LinkedHashMap<String, Object>(2);
        ConnectorUtil.setObjectAndOperationConfiguration(schema, schemaMAP);
        try {
            ObjectMapper mapper = new ObjectMapper();
            URL root = ObjectClassInfoHelperTest.class.getResource("/");
            mapper.writeValue(new File((new URL(root, "schema.json")).toURI()), schemaMAP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testOperationalSchema() throws Exception {
        InputStream inputStream = ConnectorUtilTest.class.getResourceAsStream("/config/SystemSchemaConfiguration.json");
        Assert.assertNotNull(inputStream);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode configuration = new JsonNode(mapper.readValue(inputStream, Map.class));
        Map<String, Map<Class<? extends APIOperation>, OperationOptionInfoHelper>> operationOptionHelpers = ConnectorUtil.getOperationOptionConfiguration(configuration);
        ObjectClassInfoHelper objectClassInfoHelper = org.mockito.Mockito.mock(ObjectClassInfoHelper.class);
        org.mockito.Mockito.when(objectClassInfoHelper.getObjectClass()).thenReturn(new ObjectClass("__ACCOUNT__"));
        OperationHelper helper = new OperationHelperImpl(new APIConfigurationImpl(), new Id("system/TEST/account"), objectClassInfoHelper, operationOptionHelpers.get("__ACCOUNT__"));

        Assert.assertTrue(helper.isOperationPermitted(CreateApiOp.class), "Create - ALLOWED");
        Assert.assertFalse(helper.isOperationPermitted(SyncApiOp.class), "Sync - DENIED");

        boolean authenticated = true;
        try {
            helper.isOperationPermitted(AuthenticationApiOp.class);
        } catch (ForbiddenException e) {
            authenticated = false;
        }
        Assert.assertFalse(authenticated, "Authentication - DENIED(Exception)");

        boolean operationSupported = true;
        try {
            helper.isOperationPermitted(ScriptOnResourceApiOp.class);
        } catch (ForbiddenException e) {
            operationSupported = false;
        }
        Assert.assertFalse(operationSupported, "ScriptOnResource - NotSupported(Exception)");

        Assert.assertFalse(helper.isOperationPermitted(ScriptOnConnectorApiOp.class), "ScriptOnConnector - DENIED");
        Assert.assertFalse(helper.isOperationPermitted(SearchApiOp.class), "Search - DENIED");
    }


    @Test
    public void testAPIConfiguration() throws JsonNodeException, SchemaException, URISyntaxException, ObjectSetException {
        ConnectorReference connectorReference = ConnectorUtil.getConnectorReference(jsonConfiguration);
        Assert.assertEquals(connectorReference.getConnectorHost(), ConnectorReference.SINGLE_LOCAL_CONNECTOR_MANAGER);
        ConnectorKey key = connectorReference.getConnectorKey();
        Assert.assertEquals(key.getBundleName(), "org.identityconnectors.ldap");
        Assert.assertEquals(key.getBundleVersion(), "1.0.5531");
        Assert.assertEquals(key.getConnectorName(), "org.identityconnectors.ldap.LdapConnector");

        SystemIdentifier systemIdentifier = new SimpleSystemIdentifier(jsonConfiguration);
        Assert.assertTrue(systemIdentifier.is(new Id("http://openidm.forgerock.org/openidm/system/LDAP_Central/user/CA2B382A-6FFB-11E0-80B7-902C4824019B")));
        Assert.assertTrue(systemIdentifier.is(new Id("system/LDAP_Central/account/")));
        Assert.assertFalse(systemIdentifier.is(new Id("http://openidm.forgerock.org/openidm/system/LDAP_None/user/CA2B382A-6FFB-11E0-80B7-902C4824019B")));
        Assert.assertFalse(systemIdentifier.is(new Id("system/LDAP_None/account")));

        OperationHelperBuilder operationHelperBuilder = new OperationHelperBuilder(((SimpleSystemIdentifier) systemIdentifier).getName(), jsonConfiguration, runtimeAPIConfiguration);

        OperationHelper helper = operationHelperBuilder.build("__ACCOUNT__", null);
        Assert.assertEquals(helper.getObjectClass().getObjectClassValue(), "__ACCOUNT__");
    }

    @Test(expectedExceptions = ObjectSetException.class, expectedExceptionsMessageRegExp = ".*__NONE__.*")
    public void testUnsupportedObjectType() throws JsonNodeException, SchemaException, URISyntaxException, ObjectSetException {
        OperationHelperBuilder operationHelperBuilder = new OperationHelperBuilder("test", jsonConfiguration, runtimeAPIConfiguration);
        OperationHelper helper = operationHelperBuilder.build("__NONE__", null);
    }

    @Test
    public void testCoercedTypeCasting() throws Exception {
        BigInteger bigInteger = ConnectorUtil.coercedTypeCasting(new Integer(20), BigInteger.class);
        Assert.assertEquals(bigInteger.intValue(), 20);
    }

    public APIConfiguration getRuntimeAPIConfiguration() {
        Assertions.nullCheck(runtimeAPIConfiguration, "runtimeAPIConfiguration");
        //clone in case application tries to modify
        //after the fact. this is necessary to
        //ensure thread-safety of a ConnectorFacade
        //also, runtimeAPIConfiguration is used as a key in the
        //pool, so it is important that it not be modified.
        APIConfigurationImpl _configuration = (APIConfigurationImpl) SerializerUtil.cloneObject(runtimeAPIConfiguration);
        //parent ref not included in the clone
        _configuration.setConnectorInfo(((APIConfigurationImpl) runtimeAPIConfiguration).getConnectorInfo());
        return _configuration;
    }
}
