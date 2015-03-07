/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.sync.impl;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openidm.sync.impl.ObjectMapping.SyncOperation;
import org.forgerock.script.ScriptRegistry;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMappingTest {

    private TestObjectMapping testMapping = null;
    
    @BeforeClass
    public void setUp() throws Exception {
        URL config = ObjectMappingTest.class.getResource("/conf/sync.json");
        Assert.assertNotNull(config, "sync configuration is not found");
        JsonValue syncConfig = new JsonValue((new ObjectMapper()).readValue(new File(config.toURI()), Map.class));
        JsonValue mappingConfig = syncConfig.get("mappings").get(0);
        Scripts.init(mock(ScriptRegistry.class));
        testMapping = new TestObjectMapping(null, mappingConfig);
    }

    @AfterMethod
    public void tearDown() {
    }
    
    @Test
    public void testSourceCondition() throws Exception{
        SyncOperation testSyncOperation = testMapping.getSyncOperation();
        String testString = "{ \"equalsKey\" : \"foo\", \"includesKey\" : [ \"1\", \"2\", \"3\" ], " +
                "\"json\" : { \"pointer\" : { \"key\" : \"foo\" } } }";
        JsonValue testValue = new JsonValue((new ObjectMapper()).readValue(testString.getBytes(), Map.class));
        testSyncOperation.sourceObjectAccessor = new LazyObjectAccessor(null, null, "test1", testValue);
        
        // Test passing of three conditions: equals, includes, jsonPointer-equals
        assertTrue(testSyncOperation.checkSourceConditions());
        
        // Test failing of equals condition
        testSyncOperation.sourceObjectAccessor.getObject().put("equalsKey", "bar");
        assertFalse(testSyncOperation.checkSourceConditions());
        
        // Test failing of includes condition
        testSyncOperation.sourceObjectAccessor.getObject().put("equalsKey", "foo");
        testSyncOperation.sourceObjectAccessor.getObject().get("includesKey").asList().remove("1");
        assertFalse(testSyncOperation.checkSourceConditions());

        // Test failing of equals condition (with jsonPointer key)
        testSyncOperation.sourceObjectAccessor.getObject().put("equalsKey", "foo");
        testSyncOperation.sourceObjectAccessor.getObject().get("includesKey").asList().add("1");
        testSyncOperation.sourceObjectAccessor.getObject().get("json").get("pointer").put("key", "bar");
        assertFalse(testSyncOperation.checkSourceConditions());
        
    }
    
    class TestObjectMapping extends ObjectMapping {

        public TestObjectMapping(SynchronizationService service, JsonValue config) throws JsonValueException {
            super(service, config);
        }

        public TestSyncOperation getSyncOperation() throws Exception {
            return new TestSyncOperation();
        }
        
        class TestSyncOperation extends SyncOperation {

            @Override
            public JsonValue sync() throws SynchronizationException {
                return toJsonValue();
            }

            @Override
            protected boolean isSourceToTarget() {
                return false;
            }
            
        }
    }
}
