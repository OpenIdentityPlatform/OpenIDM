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
 * Portions Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openidm.managed;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import javax.script.ScriptException;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.resource.InternalServerErrorException;
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

    private static SchemaField secureHashField(final String algorithm, final CryptoService cryptoService)
            throws JsonValueException, ScriptException {
        return new SchemaField("password",
                json(object(
                        field("type", "string"),
                        field("secureHash", object(field("algorithm", algorithm))))),
                mock(ScriptRegistry.class),
                cryptoService);
    }

    @Test
    public void testOnStoreLeavesStoredHashAloneUnderLegacyAlgorithm() throws Exception {
        // an update that keeps the stored hash must not reach hash(), which rejects SHA-1
        final CryptoService cryptoService = mock(CryptoService.class);
        when(cryptoService.isHashed(any(JsonValue.class))).thenReturn(true);
        final SchemaField schemaField = secureHashField("SHA-1", cryptoService);
        final JsonValue storedHash = json(object(field("$crypto", object())));
        final JsonValue value = json(object(field("password", storedHash.getObject())));

        // onStore(null, ...) is safe: execScript skips a field without an onStore script
        schemaField.onStore(null, value);

        verify(cryptoService, never()).hash(any(JsonValue.class), anyString());
        assertEquals(value.get("password").getObject(), storedHash.getObject());
    }

    @Test
    public void testOnStoreHashesNewValueWithConfiguredAlgorithm() throws Exception {
        final CryptoService cryptoService = mock(CryptoService.class);
        final JsonValue hashed = json(object(field("$crypto", object())));
        when(cryptoService.hash(any(JsonValue.class), anyString())).thenReturn(hashed);
        final SchemaField schemaField = secureHashField("SHA-256", cryptoService);
        final JsonValue value = json(object(field("password", "secret")));

        schemaField.onStore(null, value);

        verify(cryptoService).hash(any(JsonValue.class), eq("SHA-256"));
        assertEquals(value.get("password").getObject(), hashed.getObject());
    }

    @Test(expectedExceptions = InternalServerErrorException.class)
    public void testOnStoreFailsForNewValueUnderLegacyAlgorithm() throws Exception {
        final CryptoService cryptoService = mock(CryptoService.class);
        when(cryptoService.hash(any(JsonValue.class), anyString()))
                .thenThrow(new JsonCryptoException("SHA-1 is no longer supported for creating new hashes"));
        final SchemaField schemaField = secureHashField("SHA-1", cryptoService);

        schemaField.onStore(null, json(object(field("password", "secret"))));
    }

}
