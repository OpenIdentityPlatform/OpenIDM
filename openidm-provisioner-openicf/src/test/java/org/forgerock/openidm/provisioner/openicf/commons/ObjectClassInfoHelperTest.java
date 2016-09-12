/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.openidm.provisioner.openicf.commons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.json.JsonValue.json;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.provisioner.openicf.connector.TestConnector;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.Schema;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class used to test the functionality of {@link ObjectClassInfoHelper}
 */
public class ObjectClassInfoHelperTest {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final JsonValue schema = new JsonValue(new HashMap<String, Object>());
    private static final String OBJECT_TYPES = "objectTypes";

    @BeforeClass
    public void oneTimeSetup() throws URISyntaxException, IOException {
        schema.put(OBJECT_TYPES,
                json(OBJECT_MAPPER.readValue(getClass().getResource("/config/objectClassSchema.json"), Map.class)));
    }

    @Test
    public void testGetNativeJavaType() throws Exception {
        final TestConnector connector = new TestConnector();
        final Schema schema = connector.schema();
        final ObjectClassInfo account = schema.findObjectClassInfo("__ACCOUNT__");
        Map<String, Object> schemaMAP = ConnectorUtil.getObjectClassInfoMap(account);
        assertThat(schema).isNotNull();
        try {
            URL root = ObjectClassInfoHelperTest.class.getResource("/");
            OBJECT_MAPPER.writeValue(new File((new URL(root, "schema.json")).toURI()), schemaMAP);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testConnectorObjectName() throws Exception {
        final JsonValue configuration = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/config/SystemSchemaConfiguration.json"), Map.class));

        final ObjectClassInfoHelper helper = ObjectClassInfoHelperFactory
                .createObjectClassInfoHelper(configuration.get("objectTypes").get("__ACCOUNT__"));
        final JsonValue source = new JsonValue(new HashMap<String, Object>());
        source.put(ResourceResponse.FIELD_CONTENT_ID, "ID_NAME");
        source.put("__NAME__", "NAME_NAME");

        ConnectorObject co = helper.build(UpdateApiOp.class, "rename", source, null);
        assertThat(co.getName().getNameValue()).isEqualTo("rename");

        co = helper.build(UpdateApiOp.class, null, source, null);
        assertThat(co.getName().getNameValue()).isEqualTo("ID_NAME");
    }

    @Test
    public void testIsMultiValued() throws Exception {

        final ObjectClassInfoHelper objectClassInfoHelper = ObjectClassInfoHelperFactory.createObjectClassInfoHelper(
                schema.get(OBJECT_TYPES).get(ObjectClass.ACCOUNT_NAME));

        // build attribute
        Attribute descriptionAttribute = AttributeBuilder.build("__DESCRIPTION__",
                schema.get(OBJECT_TYPES).get(ObjectClass.ACCOUNT_NAME).get("__DESCRIPTION__").getObject());

        // verify that description is a single valued attribute
        assertThat(objectClassInfoHelper.isMultiValued(descriptionAttribute)).isFalse();
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testUnsupportedAttributeName() throws BadRequestException {
        final ObjectClassInfoHelper objectClassInfoHelper = ObjectClassInfoHelperFactory.createObjectClassInfoHelper(
                schema.get(OBJECT_TYPES).get(ObjectClass.ACCOUNT_NAME));

        // build attribute
        Attribute descriptionAttribute = AttributeBuilder.build("INVALID_ATTRIBUTE", "Invalid Attribute");

        // verify that description is a single valued attribute
        assertThat(objectClassInfoHelper.isMultiValued(descriptionAttribute)).isFalse();
    }
}
