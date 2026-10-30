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
 * Copyright 2016 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openidm.managed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.util.Utils.closeSilently;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.api.models.Action;
import org.forgerock.json.schema.validator.Constants;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ManagedObjectApiDescriptionTest {
    private static final String CONF_MANAGED_USER_WITH_ACTION = "/conf/managed-user-action.json";

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Tests getActions method.
     */
    @Test
    public void testGetActions() throws Exception {
        final JsonValue config = getResource(CONF_MANAGED_USER_WITH_ACTION);
        final JsonValue schema = config.get("schema").isNull() || config.get("schema").get(Constants.TYPE).isNull()
                ? json(object(field(Constants.TYPE, Constants.TYPE_OBJECT))) : config.get("schema").copy();
        final List<Action> actions = ManagedObjectApiDescription.getActions(config, schema);
        for(final Action action : actions) {
            if (action.getName().equalsIgnoreCase("triggerSyncCheck")) {
                assertThat(action.getResponse().getSchema().isEqualTo(ManagedObjectApiDescription.STATUS_RESPONSE_JSON));
            } else {
                assertThat(action.getResponse().getSchema().get(Constants.TYPE).asString()).isEqualTo(Constants.TYPE_OBJECT);
                if (action.getName().equalsIgnoreCase("toggleActive")) {
                    assertThat(action.getRequest().getSchema().get(Constants.TYPE).asString()).isEqualTo(Constants.TYPE_STRING);
                    assertThat(action.getResponse().getSchema().get(Constants.TYPE).asString()).isEqualTo(Constants.TYPE_OBJECT);
                    Parameter[] parameters = action.getParameters();
                    assertThat(parameters[0].getName()).isEqualTo("code");
                    assertThat(parameters[0].getType()).isEqualTo(Constants.TYPE_INTEGER);
                    assertThat(parameters[0].isRequired()).isEqualTo(true);
                    assertThat(parameters[1].getName()).isEqualTo("redirect_url");
                    assertThat(parameters[1].getType()).isEqualTo(Constants.TYPE_STRING);
                    assertThat(parameters[1].isRequired()).isEqualTo(false);
                    assertThat(parameters[2].getName()).isEqualTo("optional");
                    assertThat(parameters[2].getType()).isEqualTo(Constants.TYPE_BOOLEAN);
                    assertThat(parameters[2].isRequired()).isEqualTo(false);
                } else if (action.getName().equalsIgnoreCase("logMessage")) {
                    assertThat(action.getRequest()).isEqualTo(null);
                    assertThat(action.getParameters()).isEqualTo(null);
                } else if (action.getName().equalsIgnoreCase("scalar")) {
                    assertThat(action.getRequest()).isEqualTo(null);
                    assertThat(action.getParameters()).isEqualTo(null);
                }
            }
        }
    }

    @DataProvider(name = "relationshipTypes")
    public Object[][] createRelationshipTypeData() {
        return new Object[][] {
                { "relationship", "object" },
                // the type of a Nullable relationship
                { array("relationship", "null"), array("object", "null") },
                { array("null", "relationship"), array("null", "object") },
                // the types of a type array are unique
                { array("object", "relationship"), array("object") }
        };
    }

    @Test(dataProvider = "relationshipTypes")
    public void testReplaceRelationshipType(Object type, Object expectedType) {
        final JsonValue schema = json(object(field(Constants.TYPE, type)));

        ManagedObjectApiDescription.replaceRelationshipType(schema);

        assertThat(schema.get(Constants.TYPE).getObject()).isEqualTo(expectedType);
    }

    @DataProvider(name = "otherTypes")
    public Object[][] createOtherTypeData() {
        return new Object[][] {
                { json(object(field(Constants.TYPE, "array"))) },
                { json(object(field(Constants.TYPE, array("string", "null")))) },
                { json(object()) },
                // the items of a schema without items
                { json(null) }
        };
    }

    @Test(dataProvider = "otherTypes")
    public void testReplaceRelationshipTypeKeepsOtherTypes(JsonValue schema) {
        final Object before = schema.copy().getObject();

        ManagedObjectApiDescription.replaceRelationshipType(schema);

        assertThat(schema.getObject()).isEqualTo(before);
    }

    @Test
    public void testBuildReplacesRelationshipTypes() {
        final JsonValue config = json(object(field("schema", object(
                field(Constants.TYPE, Constants.TYPE_OBJECT),
                field(Constants.PROPERTIES, object(
                        field("manager", object(field(Constants.TYPE, "relationship"))),
                        field("reports", object(
                                field(Constants.TYPE, Constants.TYPE_ARRAY),
                                field(Constants.ITEMS, object(field(Constants.TYPE, "relationship")))))))))));
        final Map<JsonPointer, RelationshipProvider> relationshipProviders = new LinkedHashMap<>();
        relationshipProviders.put(new JsonPointer("/manager"), newRelationshipProvider(false));
        relationshipProviders.put(new JsonPointer("/reports"), newRelationshipProvider(true));
        final ManagedObjectSet objectSet = mock(ManagedObjectSet.class);
        when(objectSet.getName()).thenReturn("user");
        when(objectSet.getConfig()).thenReturn(config);
        when(objectSet.getRelationshipProviders()).thenReturn(relationshipProviders);

        final ApiDescription description = ManagedObjectApiDescription.build(objectSet);

        assertThat(description).isNotNull();
        final Resource resource = description.getPaths().get("").get(VersionedPath.UNVERSIONED);
        final JsonValue properties = resource.getResourceSchema().getSchema().get(Constants.PROPERTIES);
        assertThat(properties.get("manager").getObject()).isEqualTo(object(
                field(Constants.TYPE, Constants.TYPE_OBJECT),
                field("isRelationship", true)));
        assertThat(properties.get("reports").get("isRelationship").asBoolean()).isTrue();
        assertThat(properties.get("reports").get(Constants.ITEMS).get(Constants.TYPE).asString())
                .isEqualTo(Constants.TYPE_OBJECT);
        assertThat(resource.getItems().getSubresources().getNames()).containsOnly("/reports");
    }

    private static RelationshipProvider newRelationshipProvider(final boolean isArray) {
        final SchemaField schemaField = mock(SchemaField.class);
        when(schemaField.isArray()).thenReturn(isArray);
        final RelationshipProvider relationshipProvider = mock(RelationshipProvider.class);
        when(relationshipProvider.getSchemaField()).thenReturn(schemaField);
        return relationshipProvider;
    }

    private JsonValue getResource(final String resourceFile) throws IOException {
        final InputStream resource = getClass().getResourceAsStream(resourceFile);
        try {
            return json(mapper.readValue(resource, Map.class));
        } finally {
            if (resource != null) {
                closeSilently(resource);
            }
        }
    }
}