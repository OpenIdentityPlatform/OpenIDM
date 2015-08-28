package org.forgerock.openidm.managed;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

/**
 * Tests for {@link SchemaField}.
 */
public class SchemaFieldTest {

    private static SchemaField relationshipField = new SchemaField("field1", json(object(
            field("type", "relationship"),
            field("properties", object(
                    field("_ref", object(
                            field("type", "string"))))))));
    
    private static SchemaField relationshipReturnByDefaultField = new SchemaField("field2", json(object(
            field("type", "relationship"),
            field("returnByDefault", true),
            field("properties", object(
                    field("_ref", object(
                            field("type", "string"))))))));

    private static SchemaField relationshipArrayField = new SchemaField("field3", json(object(
            field("type", "array"),
            field("items", object(
                    field("type", "relationship"),
                    field("properties", object(
                            field("_ref", object(
                                    field("type", "string"))))))))));

    private static SchemaField virtualField = new SchemaField("field4", json(object(
            field("type", "string"),
            field("isVirtual", true))));

    private static SchemaField virtualReturnByDefaultField = new SchemaField("field5", json(object(
            field("type", "string"),
            field("returnByDefault", true),
            field("isVirtual", true))));

    private static SchemaField virtualArrayField = new SchemaField("field6", json(object(
            field("type", "array"),
            field("items", object(
                    field("type", "string"),
                    field("isVirtual", true))))));

    private static SchemaField coreField = new SchemaField("field7", json(object(
            field("type", "string"))));

    private static SchemaField coreArrayField = new SchemaField("field8", json(object(
            field("type", "array"),
            field("items", object(
                    field("type", "string"))))));
    
    private static SchemaField relationshipNullableField = new SchemaField("field1", json(object(
            field("type", array("relationship", "null")),
            field("properties", object(
                    field("_ref", object(
                            field("type", "string"))))))));

    private static SchemaField virtualNullableField = new SchemaField("field4", json(object(
            field("type", array("string", "null")),
            field("isVirtual", true))));

    private static SchemaField coreNullableField = new SchemaField("field7", json(object(
            field("type", array("string", "null")))));

    private static SchemaField coreArrayNullableField = new SchemaField("field8", json(object(
            field("type", "array"),
            field("items", object(
                    field("type", array("string", "null")))))));

    @Test
    public void testRelationshipField() {
        assertTrue(relationshipField.isRelationship());
        assertTrue(relationshipArrayField.isRelationship());
        assertTrue(!coreField.isRelationship());
    }
    
    @Test
    public void testVirtualField() {
        assertTrue(virtualField.isVirtual());
        assertTrue(virtualArrayField.isVirtual());
        assertTrue(!coreField.isVirtual());
    }
    
    @Test
    public void testCoreField() {
        assertTrue(!coreField.isVirtual() && !coreField.isRelationship());
        assertTrue(!coreArrayField.isVirtual() && !coreArrayField.isRelationship());
    }
    
    @Test
    public void testReturnByDefaultField() {
        assertTrue(!relationshipField.isReturnedByDefault());
        assertTrue(relationshipReturnByDefaultField.isReturnedByDefault());
        assertTrue(!relationshipArrayField.isReturnedByDefault());
        assertTrue(!virtualField.isReturnedByDefault());
        assertTrue(virtualReturnByDefaultField.isReturnedByDefault());
        assertTrue(!virtualArrayField.isReturnedByDefault());
        assertTrue(coreField.isReturnedByDefault());
        assertTrue(coreArrayField.isReturnedByDefault());
    }
    
    @Test
    public void testNullableField() {
        assertTrue(relationshipNullableField.isNullable());
        assertTrue(relationshipNullableField.isRelationship());
        assertTrue(virtualNullableField.isNullable());
        assertTrue(virtualNullableField.isVirtual());
        assertTrue(coreNullableField.isNullable());
        assertTrue(!coreNullableField.isVirtual());
        assertTrue(!coreNullableField.isRelationship());
        assertTrue(coreArrayNullableField.isNullable());
        assertTrue(!coreArrayNullableField.isVirtual());
        assertTrue(!coreArrayNullableField.isRelationship());
    }
    
    @Test
    public void testArrayField() {
        assertTrue(!virtualField.isArray());
        assertTrue(virtualArrayField.isArray());
        assertTrue(!relationshipField.isArray());
        assertTrue(relationshipArrayField.isArray());
        assertTrue(!coreField.isArray());
        assertTrue(coreArrayField.isArray());
    }
    
}
