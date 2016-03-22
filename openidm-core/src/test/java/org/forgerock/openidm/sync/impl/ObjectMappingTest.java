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
 * Portions copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.Map;
import javax.script.Bindings;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openidm.sync.impl.ObjectMapping.SyncOperation;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.sync.ReconContext;
import org.forgerock.openidm.util.Scripts;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMappingTest {

    // Mocked interfaces
    private ScriptRegistry mockScriptRegistry = mock(ScriptRegistry.class);
    private ScriptEntry mockScriptEntry = mock(ScriptEntry.class);
    private Script mockScript = mock(Script.class);
    private Bindings mockBindings = mock(Bindings.class);
    
    @BeforeClass
    public void setUp() throws Exception {
        // Mock the script registry and script any requests
        when(mockScript.eval(any(Bindings.class))).thenReturn(true);
        when(mockScript.createBindings()).thenReturn(mockBindings);
        when(mockScriptEntry.getScript(any(Context.class))).thenReturn(mockScript);
        when(mockScriptRegistry.takeScript(any(JsonValue.class))).thenReturn(mockScriptEntry);      
        Scripts.init(mockScriptRegistry);
    }

    @AfterMethod
    public void tearDown() {
    }
    
    @Test
    public void testSourceCondition() throws Exception{
        SyncOperation testSyncOperation = createObjectMapping("/conf/sync.json").getSyncOperation();
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
        assertThat(testSyncOperation.checkSourceConditions("default")).isTrue();
        
        // Test failing of equals condition
        testSyncOperation.sourceObjectAccessor.getObject().put("equalsKey", "bar");
        assertThat(testSyncOperation.checkSourceConditions("default")).isFalse();
        
        // Test failing of includes condition
        testSyncOperation.sourceObjectAccessor.getObject().put("equalsKey", "foo");
        testSyncOperation.sourceObjectAccessor.getObject().get("includesKey").asList().remove("1");
        assertThat(testSyncOperation.checkSourceConditions("default")).isFalse();

        // Test failing of equals condition (with jsonPointer key)
        testSyncOperation.sourceObjectAccessor.getObject().put("equalsKey", "foo");
        testSyncOperation.sourceObjectAccessor.getObject().get("includesKey").asList().add("1");
        testSyncOperation.sourceObjectAccessor.getObject().get("json").get("pointer").put("key", "bar");
        assertThat(testSyncOperation.checkSourceConditions("default")).isFalse();
    }

    @Test
    public void testUpdateActionWithNullTargetObject() throws Exception {
        TestObjectMapping dummyMapping = createObjectMapping(json(
                object(
                        field("name", "testMapping"),
                        field("source", "testSource"),
                        field("target", "testTarget")
                )
        ));
        
        ObjectSetContext.push(new ReconContext(new RootContext("test_id"), dummyMapping.getName()));
        SyncOperation testSyncOperation = dummyMapping.getSyncOperation();
        testSyncOperation.situation = Situation.CONFIRMED;
        testSyncOperation.action = ReconAction.UPDATE;
        testSyncOperation.sourceObjectAccessor = new LazyObjectAccessor(null, null, "source1", json(null));
        testSyncOperation.targetObjectAccessor = new LazyObjectAccessor(null, null, "target1", null);
        dummyMapping.linkType = mock(LinkType.class);
        
        Link link = new Link(dummyMapping);
        link._id = "testId";
        link._rev = "testRev";
        link.setLinkQualifier("default");
        link.sourceId = testSyncOperation.sourceObjectAccessor.getLocalId();
        link.targetId = testSyncOperation.targetObjectAccessor.getLocalId();
        testSyncOperation.initializeLink(link);
        
        // Test that UPDATE action does not throw a NPE if targetObject == null
        testSyncOperation.performAction();
    }
    
    private TestObjectMapping createObjectMapping(String syncJson) throws Exception {
        URL config = ObjectMappingTest.class.getResource(syncJson);
        assertThat(config).as("sync configuration is not found").isNotNull();
        JsonValue syncConfig = new JsonValue((new ObjectMapper()).readValue(new File(config.toURI()), Map.class));
        return createObjectMapping(syncConfig.get("mappings").get(0));
    }
    
    private TestObjectMapping createObjectMapping(JsonValue syncConfig) throws Exception {
        return new TestObjectMapping(null, syncConfig);
    }
    
    class TestObjectMapping extends ObjectMapping {

        public TestObjectMapping(ConnectionFactory connectionFactory, JsonValue config) throws JsonValueException {
            super(connectionFactory, config);
        }

        public TestSyncOperation getSyncOperation() throws Exception {
            return new TestSyncOperation(new RootContext());
        }
        
        class TestSyncOperation extends SyncOperation {

            public TestSyncOperation(Context context) {
                super(context);
            }

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
