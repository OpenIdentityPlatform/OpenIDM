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
 * Portions copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
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
        JsonValue testValue = json(
                object(
                        field("equalsKey", "foo"),
                        field("includesKey", array("1", "2", "3")),
                        field("json", object(
                                field("pointer", object(
                                        field("key", "foo")
                                ))
                        ))
                ));
        testSyncOperation.sourceObjectAccessor = new LazyObjectAccessor(null, null, "test1", testValue);
        
        // Test passing of three conditions: equals, includes, jsonPointer-equals
        assertTrue(testSyncOperation.checkSourceConditions("default"));
        
        // Test failing of equals condition
        testSyncOperation.sourceObjectAccessor.getObject().put("equalsKey", "bar");
        assertFalse(testSyncOperation.checkSourceConditions("default"));
        
        // Test failing of includes condition
        testSyncOperation.sourceObjectAccessor.getObject().put("equalsKey", "foo");
        testSyncOperation.sourceObjectAccessor.getObject().get("includesKey").asList().remove("1");
        assertFalse(testSyncOperation.checkSourceConditions("default"));

        // Test failing of equals condition (with jsonPointer key)
        testSyncOperation.sourceObjectAccessor.getObject().put("equalsKey", "foo");
        testSyncOperation.sourceObjectAccessor.getObject().get("includesKey").asList().add("1");
        testSyncOperation.sourceObjectAccessor.getObject().get("json").get("pointer").put("key", "bar");
        assertFalse(testSyncOperation.checkSourceConditions("default"));
        
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
