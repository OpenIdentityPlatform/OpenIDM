package org.forgerock.openidm.managed;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import javax.script.ScriptException;

import org.forgerock.json.JsonValueException;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.script.ScriptRegistry;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Tests for {@link SchemaField}.
 */
public class SchemaFieldTest {

    private static SchemaField relationshipField;
    private static SchemaField relationshipFieldValidate;
    private static SchemaField relationshipReturnByDefaultField;
    private static SchemaField relationshipArrayField;
    private static SchemaField virtualField;
    private static SchemaField virtualReturnByDefaultField;
    private static SchemaField virtualArrayField;
    private static SchemaField coreField;
    private static SchemaField coreArrayField;
    private static SchemaField relationshipNullableField;
    private static SchemaField virtualNullableField;
    private static SchemaField coreNullableField;
    private static SchemaField coreArrayNullableField;

    @BeforeTest
    public void setup() throws JsonValueException, ScriptException {
        ScriptRegistry scriptRegistry = mock(ScriptRegistry.class);
        CryptoService cryptoService = mock(CryptoService.class);
        relationshipField = new SchemaField("field1", 
                json(object(
                        field("type", "relationship"),
                        field("properties", object(
                                field("_ref", object(
                                        field("type", "string"))))))), 
                scriptRegistry,
                cryptoService);
        relationshipFieldValidate = new SchemaField("fieldX", json(object(
                field("type", "relationship"),
                field("validate", true),
                field("properties", object(
                        field("_ref", object(
                                field("type", "string"))))))),
                scriptRegistry,
                cryptoService);
        relationshipReturnByDefaultField = new SchemaField("field2", 
                json(object(
                        field("type", "relationship"),
                        field("returnByDefault", true),
                        field("properties", object(
                                field("_ref", object(
                                        field("type", "string"))))))),
                scriptRegistry,
                cryptoService);
        relationshipArrayField = new SchemaField("field3", 
                json(object(
                        field("type", "array"),
                        field("items", object(
                                field("type", "relationship"),
                                field("properties", object(
                                        field("_ref", object(
                                                field("type", "string"))))))))),
                scriptRegistry,
                cryptoService);
        virtualField = new SchemaField("field4", 
                json(object(
                        field("type", "string"),
                        field("isVirtual", true))),
                scriptRegistry,
                cryptoService);
        virtualReturnByDefaultField = new SchemaField("field5", 
                json(object(
                        field("type", "string"),
                        field("returnByDefault", true),
                        field("isVirtual", true))),
                scriptRegistry,
                cryptoService);
        virtualArrayField = new SchemaField("field6", 
                json(object(
                        field("type", "array"),
                        field("isVirtual", true),
                        field("items", object(
                                field("type", "string"))))),
                scriptRegistry,
                cryptoService);
        coreField = new SchemaField( "field7", 
                json(object(
                        field("type", "string"))),
                scriptRegistry,
                cryptoService);
        coreArrayField = new SchemaField( "field8", 
                json(object(
                        field("type", "array"),
                        field("items", object(
                                field("type", "string"))))),
                scriptRegistry,
                cryptoService);
        relationshipNullableField = new SchemaField("field1", 
                json(object(
                        field("type", array("relationship", "null")),
                        field("properties", object(
                                field("_ref", object(
                                        field("type", "string"))))))),
                scriptRegistry,
                cryptoService);
        virtualNullableField = new SchemaField("field4", 
                json(object(
                        field("type", array("string", "null")),
                        field("isVirtual", true))),
                scriptRegistry,
                cryptoService);
        coreNullableField = new SchemaField("field7", 
                json(object(
                        field("type", array("string", "null")))),
                scriptRegistry,
                cryptoService);
        coreArrayNullableField = new SchemaField("field8", 
                json(object(
                        field("type", "array"),
                        field("items", object(
                                field("type", array("string", "null")))))),
                scriptRegistry,
                cryptoService);
    }
    
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

    @Test
    public void testValidatedRelationship() {
        assertFalse(relationshipField.isValidationRequired());
        assertTrue(relationshipFieldValidate.isValidationRequired());
    }

}
