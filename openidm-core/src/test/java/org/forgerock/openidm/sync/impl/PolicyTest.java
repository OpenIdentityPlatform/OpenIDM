package org.forgerock.openidm.sync.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.script.Bindings;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.Matchers.anyMap;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PolicyTest {

    private JsonValue mappingConfig;
    private Map<String, List<Policy>> policies = new HashMap<String, List<Policy>>();

    @BeforeClass
    public void setUp() throws Exception {
        URL config = ObjectMappingTest.class.getResource("/conf/sync.json");
        Assert.assertNotNull(config, "sync configuration is not found");
        
        JsonValue syncConfig = new JsonValue((new ObjectMapper()).readValue(new File(config.toURI()), Map.class));
        mappingConfig = syncConfig.get("mappings").get(0);

        ScriptRegistry scriptRegistryMock = mock(ScriptRegistry.class);
        Scripts.init(scriptRegistryMock);

        ScriptEntry scriptEntryMock = mock(ScriptEntry.class);
        Script scriptMock = mock(org.forgerock.script.Script.class);

        when(scriptRegistryMock.takeScript(any(JsonValue.class))).thenReturn(scriptEntryMock);
        when(scriptEntryMock.getScript(any(Context.class))).thenReturn(scriptMock);

        Bindings bindingsMock = mock(Bindings.class);
        when(scriptMock.createBindings()).thenReturn(bindingsMock);
        doNothing().when(bindingsMock).putAll(anyMap());
        when(scriptMock.eval(bindingsMock)).thenReturn(ReconAction.IGNORE);
        
    }

    @AfterMethod
    public void tearDown() throws Exception {
        policies = new HashMap<String, List<Policy>>();
    }

    @Test
    public void testGetCondition() throws Exception {
        
        initPolicies();

        assertEquals(policies.size(), 8);

        assertTrue(getPolicy(Situation.CONFIRMED).getCondition()
                .evaluate(json(object()), Link.DEFAULT_LINK_QUALIFIER));

        assertTrue(getPolicy(Situation.FOUND).getCondition()
                .evaluate(json(object()), Link.DEFAULT_LINK_QUALIFIER));

        assertEquals((policies.get(Situation.ABSENT.toString())).size(), 2);
        assertTrue(getPolicy(Situation.ABSENT).getCondition()
                .evaluate(json(object()), "user"));

        assertTrue(getPolicy(Situation.AMBIGUOUS).getCondition()
                .evaluate(json(object()), Link.DEFAULT_LINK_QUALIFIER));

        assertTrue(getPolicy(Situation.MISSING).getCondition()
                .evaluate(json(object()), Link.DEFAULT_LINK_QUALIFIER));

        assertTrue(getPolicy(Situation.SOURCE_MISSING).getCondition()
                .evaluate(json(object()), Link.DEFAULT_LINK_QUALIFIER));

        assertTrue(getPolicy(Situation.UNQUALIFIED).getCondition()
                .evaluate(json(object()), Link.DEFAULT_LINK_QUALIFIER));

        assertTrue(getPolicy(Situation.UNASSIGNED).getCondition()
                .evaluate(json(object()), Link.DEFAULT_LINK_QUALIFIER));
    }
    
    @Test
    public void testGetSituation() throws Exception {

        initPolicies();

        assertEquals(policies.size(), 8);

        assertEquals(getPolicy(Situation.CONFIRMED).getSituation(), Situation.CONFIRMED);

        assertEquals(getPolicy(Situation.FOUND).getSituation(), Situation.FOUND);

        assertEquals((policies.get(Situation.ABSENT.toString())).size(), 2);
        assertEquals(getPolicy(Situation.ABSENT).getSituation(), Situation.ABSENT);

        assertEquals(getPolicy(Situation.AMBIGUOUS).getSituation(), Situation.AMBIGUOUS);

        assertEquals(getPolicy(Situation.MISSING).getSituation(), Situation.MISSING);

        assertEquals(getPolicy(Situation.SOURCE_MISSING).getSituation(), Situation.SOURCE_MISSING);

        assertEquals(getPolicy(Situation.UNQUALIFIED).getSituation(), Situation.UNQUALIFIED);

        assertEquals(getPolicy(Situation.UNASSIGNED).getSituation(), Situation.UNASSIGNED);
        
    }

    private void initPolicies() {
        for (JsonValue jv : mappingConfig.get("policies").expect(List.class)) {
            String situation = jv.get("situation").asString();
            if (policies.containsKey(situation)) {
                List<Policy> policy = policies.get(situation);
                policy.add(new Policy(jv));
                continue;
            }
            final List<Policy> l = new ArrayList<Policy>();
            policies.put(situation, l);
            l.add( new Policy(jv));
        }
    }

    private Policy getPolicy(Situation s) {
        for (Policy policy : policies.get(s.toString())) {
            if (s == policy.getSituation()) {
                return policy;
            }
        }
        return null;
    }

    @Test
    public void testGetAction() throws Exception {

        initPolicies();

        assertEquals(policies.size(), 8);

        ObjectMapping.SyncOperation syncOperationMock = mock(ObjectMapping.SyncOperation.class);
        when(syncOperationMock.toJsonValue()).thenReturn(json(object()));

        Map<String, Object> sourceMap = new HashMap<String, Object>();
        LazyObjectAccessor lazyObjectAccessorSourceMock = mock(LazyObjectAccessor.class);
        when(lazyObjectAccessorSourceMock.asMap()).thenReturn(sourceMap);

        Map<String, Object> targetMap = new HashMap<String, Object>();
        LazyObjectAccessor lazyObjectAccessorTargetMock = mock(LazyObjectAccessor.class);
        when(lazyObjectAccessorTargetMock.asMap()).thenReturn(targetMap);

        assertEquals(ReconAction.IGNORE, 
                getPolicy(Situation.CONFIRMED)
                        .getAction(lazyObjectAccessorSourceMock,
                                lazyObjectAccessorTargetMock,
                                syncOperationMock,
                                Link.DEFAULT_LINK_QUALIFIER));

        assertEquals(ReconAction.IGNORE, 
                getPolicy(Situation.FOUND)
                        .getAction(lazyObjectAccessorSourceMock,
                                lazyObjectAccessorTargetMock,
                                syncOperationMock,
                                Link.DEFAULT_LINK_QUALIFIER));

        assertEquals(ReconAction.IGNORE, 
                getPolicy(Situation.ABSENT)
                        .getAction(lazyObjectAccessorSourceMock,
                                lazyObjectAccessorTargetMock,
                                syncOperationMock,
                                Link.DEFAULT_LINK_QUALIFIER));

        assertEquals(ReconAction.IGNORE, 
                getPolicy(Situation.AMBIGUOUS)
                        .getAction(lazyObjectAccessorSourceMock,
                                lazyObjectAccessorTargetMock,
                                syncOperationMock,
                                Link.DEFAULT_LINK_QUALIFIER));

        assertEquals(ReconAction.IGNORE, 
                getPolicy(Situation.MISSING)
                        .getAction(lazyObjectAccessorSourceMock,
                                lazyObjectAccessorTargetMock,
                                syncOperationMock,
                                Link.DEFAULT_LINK_QUALIFIER));

        assertEquals(ReconAction.IGNORE, 
                getPolicy(Situation.SOURCE_MISSING)
                        .getAction(lazyObjectAccessorSourceMock,
                                lazyObjectAccessorTargetMock,
                                syncOperationMock,
                                Link.DEFAULT_LINK_QUALIFIER));

        assertEquals(ReconAction.IGNORE, 
                getPolicy(Situation.UNQUALIFIED)
                        .getAction(lazyObjectAccessorSourceMock,
                                lazyObjectAccessorTargetMock,
                                syncOperationMock,
                                Link.DEFAULT_LINK_QUALIFIER));

        assertEquals(ReconAction.IGNORE, 
                getPolicy(Situation.UNASSIGNED)
                        .getAction(lazyObjectAccessorSourceMock,
                                lazyObjectAccessorTargetMock,
                                syncOperationMock,
                                Link.DEFAULT_LINK_QUALIFIER));


    }

    @Test
    public void testEvaluatePostActionWithPostActionConfigured() throws Exception {
        JsonValue postAction =  null;
        
        for (JsonValue jv : mappingConfig.get("policies").expect(List.class)) {
            String situation = jv.get("situation").asString();
            // capture ABSENT map for testing of postAction
            if (Situation.ABSENT.toString().equals(situation)) {
                postAction = jv;
            }
            if (policies.containsKey(situation)) {
                List<Policy> policy = policies.get(situation);
                policy.add(new Policy(jv));
                continue;
            }
            final List<Policy> l = new ArrayList<Policy>();
            policies.put(situation, l);
            l.add( new Policy(jv));
        }
        
        Policy pAbsent = getPolicy(Situation.ABSENT);
        assertTrue(postAction != null && !postAction.get("postAction").isNull());

        Map<String, Object> sourceMap = new HashMap<String, Object>();
        LazyObjectAccessor lazyObjectAccessorSourceMock = mock(LazyObjectAccessor.class);
        when(lazyObjectAccessorSourceMock.asMap()).thenReturn(sourceMap);

        Map<String, Object> targetMap = new HashMap<String, Object>();
        LazyObjectAccessor lazyObjectAccessorTargetMock = mock(LazyObjectAccessor.class);
        when(lazyObjectAccessorTargetMock.asMap()).thenReturn(targetMap);

        ObjectMapping.SyncOperation syncOperationMock = mock(ObjectMapping.SyncOperation.class);
        when(syncOperationMock.toJsonValue()).thenReturn(json(object()));

        ReconAction absentAction = pAbsent.getAction(lazyObjectAccessorSourceMock, 
                lazyObjectAccessorTargetMock,syncOperationMock, Link.DEFAULT_LINK_QUALIFIER);

        pAbsent.evaluatePostAction(
                    lazyObjectAccessorSourceMock,
                    lazyObjectAccessorTargetMock,
                    absentAction,
                    true,
                    Link.DEFAULT_LINK_QUALIFIER);

    }
}