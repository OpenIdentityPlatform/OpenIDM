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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openidm.provisioner.openicf.commons;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.openidm.util.FileUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ObjectClassInfoHelperFactoryTest {

    private static final ObjectClass CUSTOM_OBJECT_CLASS = new ObjectClass("CUSTOM");
    private static final ObjectClass TEST_OBJECT_CLASS = new ObjectClass("__TEST__");
    private static final JsonValue schema = new JsonValue(new HashMap<String, Object>());
    private static final String OBJECT_TYPES = "objectTypes";

    @BeforeClass
    public void oneTimeSetup() throws URISyntaxException, IOException {
        schema.put(OBJECT_TYPES, toJsonValue(FileUtil.readFile(new File(
                new File(ObjectClassInfoHelperFactoryTest.class.getResource("/").toURI()),
                "/config/objectClassSchema.json"))));

    }

    @Test
    public void testCreatingAccountObjectClass() {
        ObjectClassInfoHelper objectClassInfoHelper =
            ObjectClassInfoHelperFactory.createObjectClassInfoHelper(
                schema.get(OBJECT_TYPES).get(ObjectClass.ACCOUNT_NAME));
        assertThat(objectClassInfoHelper.getObjectClass().equals(ObjectClass.ACCOUNT)).isTrue();
    }

    @Test
    public void testCreatingGroupObjectClass() {
        ObjectClassInfoHelper objectClassInfoHelper =
            ObjectClassInfoHelperFactory.createObjectClassInfoHelper(
                    schema.get(OBJECT_TYPES).get(ObjectClass.GROUP_NAME));
        assertThat(objectClassInfoHelper.getObjectClass().equals(ObjectClass.GROUP)).isTrue();
    }

    @Test
    public void testCreatingAllObjectClass() {
        ObjectClassInfoHelper objectClassInfoHelper =
            ObjectClassInfoHelperFactory.createObjectClassInfoHelper(
                schema.get(OBJECT_TYPES).get(ObjectClass.ALL_NAME));
        assertThat(objectClassInfoHelper.getObjectClass().equals(ObjectClass.ALL)).isTrue();
    }

    @Test
    public void testCreatingTestObjectClass() {
        ObjectClassInfoHelper objectClassInfoHelper =
            ObjectClassInfoHelperFactory.createObjectClassInfoHelper(
                schema.get(OBJECT_TYPES).get("__TEST__"));
        assertThat(objectClassInfoHelper.getObjectClass().equals(TEST_OBJECT_CLASS)).isTrue();
    }

    @Test
    public void testCreatingCustomObjectClass() {
        ObjectClassInfoHelper objectClassInfoHelper =
                ObjectClassInfoHelperFactory.createObjectClassInfoHelper(
                        schema.get(OBJECT_TYPES).get("CUSTOM"));
        assertThat(objectClassInfoHelper.getObjectClass().equals(CUSTOM_OBJECT_CLASS)).isTrue();
    }

    @Test(expectedExceptions = SchemaException.class)
    public void testCreatingAllObjectClassWithProperties() {
        ObjectClassInfoHelperFactory.createObjectClassInfoHelper(schema.get(OBJECT_TYPES).get("ALL_FAIL"));
    }

    private static JsonValue toJsonValue(String json) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return new JsonValue(mapper.readValue(json, Map.class));
    }
}
