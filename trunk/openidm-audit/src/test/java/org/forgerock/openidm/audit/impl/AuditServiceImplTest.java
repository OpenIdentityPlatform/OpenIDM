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
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
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

import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

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
        public void query(ServerContext context, QueryRequest request, QueryResultHandler handler, String type, boolean formatted) throws ResourceException {
            // Not implemented
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
    public void testFilterActivityAuditContext() throws Exception {

        //Given
        AuditContext auditContext = new AuditContext(new RootContext());
        ServerContext context = new ServerContext(auditContext);
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
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
        JsonValue config = json(
                object(
                    field("eventTypes", object(
                        field("activity", object(
                            field("filter", object(
                                field("actions", array("create"))
                            ))
                        ))
                    ))
                ));

        auditService.auditFilter = auditService.auditLogFilterBuilder.build(config);
        ServerContext context = mock(ServerContext.class);
        @SuppressWarnings("unchecked")
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
    public void testFilterActivityAllExcluded() throws Exception {

        //Given
        JsonValue config = json(
                object(
                    field("eventTypes", object(
                        field("activity", object(
                            field("filter", object(
                                field("actions", array())
                            ))
                        ))
                    ))
                ));

        auditService.auditFilter = auditService.auditLogFilterBuilder.build(config);
        ServerContext context = mock(ServerContext.class);
        @SuppressWarnings("unchecked")
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
    public void testFilterActivityUnknownAction() throws Exception {

        //Given
        JsonValue config = json(
                object(
                    field("eventTypes", object(
                        field("activity", object(
                            field("filter", object(
                                field("actions", array("create"))
                            ))
                        ))
                    ))
                ));

        auditService.auditFilter = auditService.auditLogFilterBuilder.build(config);
        ServerContext context = mock(ServerContext.class);
        @SuppressWarnings("unchecked")
        ResultHandler<Resource> handler = mock(ResultHandler.class);

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
        JsonValue config = json(
                object(
                    field("eventTypes", object(
                        field("activity", object(
                            field("filter", object(
                                field("triggers", object(
                                    field("sometrigger", array("create"))
                                ))
                            ))
                        ))
                    ))
                ));

        auditService.auditFilter = auditService.auditLogFilterBuilder.build(config);
        TriggerContext triggerContext = new TriggerContext(new RootContext(), "sometrigger");
        ServerContext context = new ServerContext(triggerContext);
        @SuppressWarnings("unchecked")
        ResultHandler<Resource> handler = mock(ResultHandler.class);

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
        JsonValue config = json(
                object(
                    field("eventTypes", object(
                        field("activity", object(
                            field("filter", object(
                                field("triggers", object(
                                    field("sometrigger", array())
                                ))
                            ))
                        ))
                    ))
                ));

        auditService.auditFilter = auditService.auditLogFilterBuilder.build(config);
        ServerContext context = new ServerContext(new TriggerContext(new RootContext(), "sometrigger"));
        @SuppressWarnings("unchecked")
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
    public void testFilterTriggerActivityUnknownAction() throws Exception {

        // Given
        JsonValue config = json(
                object(
                    field("eventTypes", object(
                        field("activity", object(
                            field("filter", object(
                                field("triggers", object(
                                    field("sometrigger", array("create"))
                                ))
                            ))
                        ))
                    ))
                ));

        auditService.auditFilter = auditService.auditLogFilterBuilder.build(config);
        ServerContext context = new ServerContext(new TriggerContext(new RootContext(), "sometrigger"));
        @SuppressWarnings("unchecked")
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        // When
        JsonValue content = new JsonValue(new HashMap<String, Object>());
        content.put("_id", 1);
        content.put("data", "hello world");
        content.put("action", "unknown");
        auditService.handleCreate(context, Requests.newCreateRequest("activity", null, content), handler);

        // Then
        assertEquals(memory.size(), 1);
    }


    @Test
    public void testFilterTriggerReconLinkAction() throws Exception {

        // Given
        JsonValue config = json(
                object(
                    field("eventTypes", object(
                        field("activity", object(
                            field("filter", object(
                                field("triggers", object(
                                    field("recon", array("link"))
                                ))
                            ))
                        ))
                    ))
                ));

        auditService.auditFilter = auditService.auditLogFilterBuilder.build(config);
        ServerContext context = new ServerContext(new TriggerContext(new RootContext(), "recon"));
        @SuppressWarnings("unchecked")
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        // based on ObjectMapping.ReconEntry which uses action from SyncOperation.action which is an
        // org.forgerock.openidm.core.sync.impl.Action value
        JsonValue reconEntry = new JsonValue(new HashMap<String, Object>());
        reconEntry.put("_id", 1);
        reconEntry.put("data", "hello world");
        reconEntry.put("action", "LINK");

        // When
        auditService.handleCreate(context, Requests.newCreateRequest("recon", null, reconEntry), handler);

        // Then
        assertEquals(memory.size(), 1);
    }

    @Test
    public void testFilterTriggerReconUnknownAction() throws Exception {

        // Given
        JsonValue config = json(
                object(
                    field("eventTypes", object(
                        field("recon", object(
                            field("filter", object(
                                field("triggers", object(
                                    // create is an unknown action for recon, as it is not in Action - it should be ignored
                                    field("recon", array("create"))
                                ))
                            ))
                        ))
                    ))
                ));

        auditService.auditFilter = auditService.auditLogFilterBuilder.build(config);
        ServerContext context = new ServerContext(new TriggerContext(new RootContext(), "recon"));
        @SuppressWarnings("unchecked")
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        // based on ObjectMapping.ReconEntry which uses action from SyncOperation.action which is an
        // org.forgerock.openidm.core.sync.impl.Action value
        JsonValue reconEntry = new JsonValue(new HashMap<String, Object>());
        reconEntry.put("_id", 1);
        reconEntry.put("data", "hello world");
        reconEntry.put("action", "LINK");

        // When
        auditService.handleCreate(context, Requests.newCreateRequest("recon", null, reconEntry), handler);

        // Then
        assertEquals(memory.size(), 0);
    }

    @Test
    public void testFilterTriggerReconWithNoAction() throws Exception {

        // Given
        JsonValue config = json(
                object(
                    field("eventTypes", object(
                        field("activity", object(
                            field("filter", object(
                                field("triggers", object(
                                    // filter to log link and unlink only
                                    field("recon", array("link", "unlink"))
                                ))
                            ))
                        ))
                    ))
                ));

        auditService.auditFilter = auditService.auditLogFilterBuilder.build(config);
        ServerContext context = new ServerContext(new TriggerContext(new RootContext(), "recon"));
        @SuppressWarnings("unchecked")
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        // based on ObjectMapping.ReconEntry which uses action from SyncOperation.action which is an
        // org.forgerock.openidm.core.sync.impl.Action value
        // start/summary records log with action of null
        CreateRequest request = Requests.newCreateRequest("recon", null, json(object(field("action", null))));

        // When
        auditService.handleCreate(context, request, handler);

        // Then
        assertEquals(memory.size(), 1); // always log nulls
    }

}
