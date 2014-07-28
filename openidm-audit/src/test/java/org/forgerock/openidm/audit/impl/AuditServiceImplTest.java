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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.audit.impl;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.sync.TriggerContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test the audit service.
 */
public class AuditServiceImplTest {

    private AuditServiceImpl auditService;

    private Collection<Map<String, Object>> memory = new ArrayList<Map<String, Object>>();

    private AuditLogger auditLogger = new AuditLogger() {

        @Override
        public void setConfig(JsonValue config) {
        }

        @Override
        public void cleanup() {
            memory.clear();
        }

        @Override
        public boolean isUsedForQueries() {
            return true;
        }

        @Override
        public boolean isIgnoreLoggingFailures() {
            return true;
        }

        @Override
        public void create(ServerContext context, String type, Map<String, Object> object) throws ResourceException {
            memory.add(object);
        }

        @Override
        public Map<String, Object> read(ServerContext context, String type, String localId) throws ResourceException {
            for (Map<String, Object> object : memory) {
                if (localId.equals(object.get("_id"))) {
                    return object;
                }
            }
            throw new NotFoundException("audit log " + localId + " not found");
        }

        @Override
        public Map<String, Object> query(ServerContext context, String type, Map<String, String> params, boolean formatted) throws ResourceException {
            return null; // not implemented
        }
    };

    @BeforeMethod
    public void setUp() {
        auditService = new AuditServiceImpl();
        auditService.globalAuditLoggers.add(auditLogger);
    }

    @AfterMethod
    public void reset() {
        auditLogger.cleanup();
    }

    @Test
    public void testGetActionFilters() {
        //Given
        /*
        "eventTypes" : {
            "activity" : {
                "filter" : {
                    "actions" : [ "create" ]
                },
            },
        }
        */
        JsonValue config = new JsonValue(
                new HashMap<String, Object>() {{
                    put("eventTypes", new HashMap<String, Object>() {{
                        put("activity", new HashMap<String, Object>() {{
                            put("filter", new HashMap<String, Object>() {{
                                put("actions", new ArrayList<String>() {{
                                    add("create");
                                }});
                            }});
                        }});
                    }});
                }});

        // When
        Map<String, Set<RequestType>> actionFilters = auditService.getActionFilters(config);

        //Then
        assertTrue(actionFilters.containsKey("activity"));
        assertTrue(actionFilters.get("activity").contains(RequestType.CREATE));
    }

    @Test
    public void testGetActionFilterWithNonRequestTypeAction() {
        //Given
        /*
        "eventTypes" : {
            "activity" : {
                "filter" : {
                    "actions" : [ "skittle" ]
                },
            },
        }
        */
        JsonValue config = new JsonValue(
                new HashMap<String, Object>() {{
                    put("eventTypes", new HashMap<String, Object>() {{
                        put("activity", new HashMap<String, Object>() {{
                            put("filter", new HashMap<String, Object>() {{
                                put("actions", new ArrayList<String>() {{
                                    add("skittle");
                                }});
                            }});
                        }});
                    }});
                }});

        // When
        Map<String, Set<RequestType>> actionFilters = auditService.getActionFilters(config);

        // Then
        assertTrue(actionFilters.containsKey("activity"));
        assertTrue(actionFilters.get("activity").isEmpty());
    }


    @Test
    public void testGetTriggerFilters() {
        //Given
        /*
        "eventTypes" : {
            "activity" : {
                "filter" : {
                     "triggers" : {
                         "recon" : [
                             "create"
                         ]
                     }
                 },
            },
        }
        */
        JsonValue config = new JsonValue(
                new HashMap<String, Object>() {{
                    put("eventTypes", new HashMap<String, Object>() {{
                        put("activity", new HashMap<String, Object>() {{
                            put("filter", new HashMap<String, Object>() {{
                                put("triggers", new HashMap<String, Object>() {{
                                    put("recon", new ArrayList<String>() {{
                                        add("create");
                                    }});
                                }});
                            }});
                        }});
                    }});
                }});

        // When
        Map<String, Map<String, Set<RequestType>>> triggerFilters = auditService.getTriggerFilters(config);

        //Then
        assertTrue(triggerFilters.containsKey("activity"));
        assertTrue(triggerFilters.get("activity").containsKey("recon"));
        assertTrue(triggerFilters.get("activity").get("recon").contains(RequestType.CREATE));
    }

    @Test
    public void testGetTriggerFilterWithNonRequestTypeAction() {
        //Given
        /*
        "eventTypes" : {
            "activity" : {
                "filter" : {
                     "triggers" : {
                         "recon" : [
                             "skittle"
                         ]
                     }
                 },
            },
        }
        */
        JsonValue config = new JsonValue(
                new HashMap<String, Object>() {{
                    put("eventTypes", new HashMap<String, Object>() {{
                        put("activity", new HashMap<String, Object>() {{
                            put("filter", new HashMap<String, Object>() {{
                                put("triggers", new HashMap<String, Object>() {{
                                    put("recon", new ArrayList<String>() {{
                                        add("skittle");
                                    }});
                                }});
                            }});
                        }});
                    }});
                }});

        // When
        Map<String, Map<String, Set<RequestType>>> triggerFilters = auditService.getTriggerFilters(config);

        // Then
        assertTrue(triggerFilters.containsKey("activity"));
        assertTrue(triggerFilters.get("activity").containsKey("recon"));
        assertTrue(triggerFilters.get("activity").get("recon").isEmpty());
    }

    @Test
    public void testFilterActivityAuditContext() throws Exception {

        //Given
        AuditContext auditContext = new AuditContext(new RootContext());
        ServerContext context = new ServerContext(auditContext);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //When
        JsonValue content = new JsonValue(new HashMap<String, Object>());
        content.put("_id", 1);
        content.put("data", "hello world");
        content.put("action", "create");
        auditService.handleCreate(context, Requests.newCreateRequest("activity", null, content), handler);

        //Then
        assertEquals(memory.size(), 0);
    }

    @Test
    public void testFilterActivityNone() throws Exception {

        //Given
        ServerContext context = mock(ServerContext.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //When
        JsonValue content = new JsonValue(new HashMap<String, Object>());
        content.put("_id", 1);
        content.put("data", "hello world");
        content.put("action", "create");
        auditService.handleCreate(context, Requests.newCreateRequest("activity", null, content), handler);

        //Then
        assertEquals(memory.size(), 1);
    }

    @Test
    public void testFilterActivityExplicitlyIncluded() throws Exception {

        //Given
        ServerContext context = mock(ServerContext.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        auditService.actionFilters.put("activity", EnumSet.of(RequestType.CREATE));

        //When
        JsonValue content = new JsonValue(new HashMap<String, Object>());
        content.put("_id", 1);
        content.put("data", "hello world");
        content.put("action", "create");
        auditService.handleCreate(context, Requests.newCreateRequest("activity", null, content), handler);

        //Then
        assertEquals(memory.size(), 1);
    }

    @Test
    public void testFilterActivityAllExcluded() throws Exception {

        //Given
        ServerContext context = mock(ServerContext.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        auditService.actionFilters.put("activity", EnumSet.noneOf(RequestType.class));

        //When
        JsonValue content = new JsonValue(new HashMap<String, Object>());
        content.put("_id", 1);
        content.put("data", "hello world");
        content.put("action", "create");
        auditService.handleCreate(context, Requests.newCreateRequest("activity", null, content), handler);

        //Then
        assertEquals(memory.size(), 0);
    }

    @Test
    public void testFilterActivityUnknownAction() throws Exception {

        //Given
        ServerContext context = mock(ServerContext.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        auditService.actionFilters.put("activity", EnumSet.of(RequestType.CREATE));

        //When
        JsonValue content = new JsonValue(new HashMap<String, Object>());
        content.put("_id", 1);
        content.put("data", "hello world");
        content.put("action", "unknown");
        auditService.handleCreate(context, Requests.newCreateRequest("activity", null, content), handler);

        //Then
        assertEquals(memory.size(), 1);
    }

    @Test
    public void testFilterTriggerActivityExplicitlyIncluded() throws Exception {

        // Given
        TriggerContext triggerContext = new TriggerContext(new RootContext(), "sometrigger");
        ServerContext context = new ServerContext(triggerContext);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        auditService.triggerFilters.put("activity",
                new HashMap<String, Set<RequestType>>() {{
                    put("sometrigger", EnumSet.of(RequestType.CREATE));
                }});

        // When
        JsonValue content = new JsonValue(new HashMap<String, Object>());
        content.put("_id", 1);
        content.put("data", "hello world");
        content.put("action", "create");
        auditService.handleCreate(context, Requests.newCreateRequest("activity", null, content), handler);

        // Then
        assertEquals(memory.size(), 1);
    }

    @Test
    public void testFilterTriggerActivityAllExcluded() throws Exception {

        //Given
        TriggerContext triggerContext = new TriggerContext(new RootContext(), "sometrigger");
        ServerContext context = new ServerContext(triggerContext);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        auditService.triggerFilters.put("activity",
                new HashMap<String, Set<RequestType>>() {{
                    put("sometrigger", EnumSet.noneOf(RequestType.class));
                }});


        //When
        JsonValue content = new JsonValue(new HashMap<String, Object>());
        content.put("_id", 1);
        content.put("data", "hello world");
        content.put("action", "create");
        auditService.handleCreate(context, Requests.newCreateRequest("activity", null, content), handler);

        //Then
        assertEquals(memory.size(), 0);
    }

    @Test
    public void testFilterTriggerActivityUnknownAction() throws Exception {

        // Given
        TriggerContext triggerContext = new TriggerContext(new RootContext(), "sometrigger");
        ServerContext context = new ServerContext(triggerContext);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        auditService.triggerFilters.put("activity",
                new HashMap<String, Set<RequestType>>() {{
                    put("sometrigger", EnumSet.of(RequestType.CREATE));
                }});

        // When
        JsonValue content = new JsonValue(new HashMap<String, Object>());
        content.put("_id", 1);
        content.put("data", "hello world");
        content.put("action", "unknown");
        auditService.handleCreate(context, Requests.newCreateRequest("activity", null, content), handler);

        // Then
        assertEquals(memory.size(), 1);
    }

}
