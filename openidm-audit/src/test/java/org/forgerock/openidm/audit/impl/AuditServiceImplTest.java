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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openidm.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.openidm.audit.AuditService.AuditAction.getChangedPasswordFields;
import static org.forgerock.openidm.audit.AuditService.AuditAction.getChangedWatchedFields;
import static org.forgerock.openidm.audit.util.AuditTestUtils.mockResultHandler;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.audit.events.AuditEventBuilder;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.util.AuditTestUtils;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.mockito.ArgumentCaptor;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.Test;

/**
 * Test the audit service.
 */
public class AuditServiceImplTest {

    //private Collection<Map<String, Object>> memory = new ArrayList<>();

    @Test
    public void testAuditServiceActivation() throws Exception {
        //given
        final JSONEnhancedConfig jsonEnhancedConfig = mock(JSONEnhancedConfig.class);
        final ScriptRegistry scriptRegistry = mock(ScriptRegistry.class);
        final ScriptEntry scriptEntry = mock(ScriptEntry.class);
        final JsonValue config = AuditTestUtils.getJson(getResource("/audit.json"));
        final AuditServiceImpl auditService = new AuditServiceImpl();

        auditService.bindEnhancedConfig(jsonEnhancedConfig);
        auditService.bindScriptRegistry(scriptRegistry);

        when(jsonEnhancedConfig.getConfigurationAsJson(any(ComponentContext.class))).thenReturn(config);
        when(scriptRegistry.takeScript(any(JsonValue.class))).thenReturn(scriptEntry);

        //when
        auditService.activate(mock(ComponentContext.class));

        //if test fails it will throw an exception
    }

    @Test
    public void testAuditServiceCreate() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService("/audit.json");

        final AuditEvent auditEvent = TestAuditEventBuilder.testAuditEventBuilder()
                .transactionId("transactionId")
                .eventName("eventName")
                .timestamp(System.currentTimeMillis())
                .authentication("testuser@forgerock.com")
                .toEvent();

        final CreateRequest createRequest = Requests.newCreateRequest("test", auditEvent.getValue());
        final ResultHandler<Resource> resourceResultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handleCreate(new ServerContext(new RootContext()), createRequest, resourceResultHandler);

        //then
        verify(resourceResultHandler).handleResult(resourceCaptor.capture());
        verify(resourceResultHandler, never()).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceCaptor.getValue()).isNotNull();
        assertThat(resourceCaptor.getValue().getContent().asMap()).isEqualTo(createRequest.getContent().asMap());
    }

    @Test
    public void testAuditServiceRead() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService("/audit.json");

        final ReadRequest readRequest = Requests.newReadRequest("test", "id");

        final ResultHandler<Resource> resourceResultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handleRead(new ServerContext(new RootContext()), readRequest, resourceResultHandler);

        //then
        verify(resourceResultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resourceResultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testAuditServiceUpdate() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService("/audit.json");

        final UpdateRequest updateRequest =
                Requests.newUpdateRequest("test", "id", new JsonValue(new LinkedHashMap<String, Object>()));

        final ResultHandler<Resource> resourceResultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handleUpdate(new ServerContext(new RootContext()), updateRequest, resourceResultHandler);

        //then
        verify(resourceResultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resourceResultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testAuditServiceDelete() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService("/audit.json");

        final DeleteRequest deleteRequest = Requests.newDeleteRequest("test", "id");

        final ResultHandler<Resource> resourceResultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handleDelete(new ServerContext(new RootContext()), deleteRequest, resourceResultHandler);

        //then
        verify(resourceResultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resourceResultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testAuditServicePatch() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService("/audit.json");

        final PatchRequest patchRequest =
                Requests.newPatchRequest("test", "id", PatchOperation.remove(new JsonPointer("/test")));

        final ResultHandler<Resource> resourceResultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handlePatch(new ServerContext(new RootContext()), patchRequest, resourceResultHandler);

        //then
        verify(resourceResultHandler, never()).handleResult(resourceCaptor.capture());
        verify(resourceResultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testAuditServiceActionForBadAction() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService("/audit.json");

        final ActionRequest actionRequest = Requests.newActionRequest("test", "id", "actionId");

        final ResultHandler<JsonValue> jsonValueResultHandler = mockResultHandler(JsonValue.class);
        final ArgumentCaptor<JsonValue> resourceCaptor = ArgumentCaptor.forClass(JsonValue.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handleAction(new ServerContext(new RootContext()), actionRequest, jsonValueResultHandler);

        //then
        verify(jsonValueResultHandler, never()).handleResult(resourceCaptor.capture());
        verify(jsonValueResultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(BadRequestException.class);

    }

    @Test
    public void testActionForChangedFields() throws Exception {

        //given
        AuditServiceImpl auditService = createAuditService("/audit.json");
        JsonValue testContent = json(
                object(
                        field("before", object(
                                field("mail", "a@b.com"),
                                field("sn", "Blow")
                        )),
                        field("after", object(
                                field("mail", "x@y.z"),
                                field("sn", "Doe")
                        ))
                )
        );
        ActionRequest changedFieldsRequest = Requests.newActionRequest("test", "id", getChangedWatchedFields.name());
        changedFieldsRequest.setContent(testContent);
        final ResultHandler<JsonValue> jsonValueResultHandler = mockResultHandler(JsonValue.class);
        final ArgumentCaptor<JsonValue> resourceCaptor = ArgumentCaptor.forClass(JsonValue.class);

        // when
        auditService.handleAction(new ServerContext(new RootContext()), changedFieldsRequest, jsonValueResultHandler);

        // then
        verify(jsonValueResultHandler).handleResult(resourceCaptor.capture());
        List<String> changedFields = resourceCaptor.getValue().asList(String.class);
        assertThat(changedFields.size()).isEqualTo(1);
        assertThat(changedFields.get(0)).isEqualTo("/mail");
    }

    @Test
    public void testActionForChangedPassword() throws Exception {
        // given
        AuditServiceImpl auditService = createAuditService("/audit.json");
        JsonValue testContent = json(
                object(
                        field("before", object(
                                field("password", "pass1")
                        )),
                        field("after", object(
                                field("password", "pass2")
                        ))
                )
        );

        ActionRequest changedPasswordRequest = Requests.newActionRequest("test", "id", getChangedPasswordFields.name());
        changedPasswordRequest.setContent(testContent);
        final ResultHandler<JsonValue> jsonValueResultHandler = mockResultHandler(JsonValue.class);
        final ArgumentCaptor<JsonValue> resourceCaptor = ArgumentCaptor.forClass(JsonValue.class);

        //when
        auditService.handleAction(new ServerContext(new RootContext()), changedPasswordRequest, jsonValueResultHandler);

        //then
        verify(jsonValueResultHandler).handleResult(resourceCaptor.capture());
        List<String> changedPasswords = resourceCaptor.getValue().asList(String.class);
        assertThat(changedPasswords.size()).isEqualTo(1);
        assertThat(changedPasswords.get(0)).isEqualTo("/password");
    }

    @Test
    public void testAuditServiceQuery() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService("/audit.json");

        final QueryRequest queryRequest = Requests.newQueryRequest("test");

        final QueryResultHandler queryResultHandler = mock(QueryResultHandler.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handleQuery(new ServerContext(new RootContext()), queryRequest, queryResultHandler);

        //then
        verify(queryResultHandler, never()).handleResource(resourceCaptor.capture());
        verify(queryResultHandler).handleError(resourceExceptionCaptor.capture());

        assertThat(resourceExceptionCaptor.getValue()).isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testExceptionFormmatter() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService("/audit.json");

        final AuditEvent auditEvent = TestAuditEventBuilder.testAuditEventBuilder()
                .transactionId("transactionId")
                .eventName("eventName")
                .timestamp(System.currentTimeMillis())
                .authentication("testuser@forgerock.com")
                .exception(new Exception("Test Exception"))
                .toEvent();

        final CreateRequest createRequest = Requests.newCreateRequest("test", auditEvent.getValue());
        final ResultHandler<Resource> resourceResultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor =
                ArgumentCaptor.forClass(ResourceException.class);

        //when
        auditService.handleCreate(new ServerContext(new RootContext()), createRequest, resourceResultHandler);

        //then
        verify(resourceResultHandler).handleResult(resourceCaptor.capture());
        verify(resourceResultHandler, never()).handleError(resourceExceptionCaptor.capture());

        final JsonValue expectedResource = createRequest.getContent();
        expectedResource.put("exception", "Exception Formatted");

        assertThat(resourceCaptor.getValue()).isNotNull();
        assertThat(resourceCaptor.getValue().getContent().asMap()).isEqualTo(createRequest.getContent().asMap());
    }

    /*
    @Test(enabled = false)
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

    @Test(enabled = false)
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

    @Test(enabled = false)
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

    @Test(enabled = false)
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

    @Test(enabled = false)
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

    @Test(enabled = false)
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

    @Test(enabled = false)
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

    @Test(enabled = false)
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


    @Test(enabled = false)
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

    @Test(enabled = false)
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

    @Test(enabled = false)
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
    */

    private InputStream getResource(final String resourceName) {
        return getClass().getResourceAsStream(resourceName);
    }

    private AuditServiceImpl createAuditService(final String configFile) throws Exception {
        final JSONEnhancedConfig jsonEnhancedConfig = mock(JSONEnhancedConfig.class);
        final ScriptRegistry scriptRegistry = mock(ScriptRegistry.class);
        final ScriptEntry scriptEntry = mock(ScriptEntry.class);
        final Script script = mock(Script.class);
        final AuditServiceImpl auditService = new AuditServiceImpl();

        auditService.bindEnhancedConfig(jsonEnhancedConfig);
        auditService.bindScriptRegistry(scriptRegistry);

        final JsonValue config = AuditTestUtils.getJson(getResource(configFile));

        when(jsonEnhancedConfig.getConfigurationAsJson(any(ComponentContext.class))).thenReturn(config);
        when(scriptRegistry.takeScript(any(JsonValue.class))).thenReturn(scriptEntry);
        when(scriptEntry.getScript(any(Context.class))).thenReturn(script);
        when(script.eval()).thenReturn("Exception Formatted");

        auditService.activate(mock(ComponentContext.class));
        return auditService;
    }

    static class TestAuditEventBuilder<T extends TestAuditEventBuilder<T>>
            extends AuditEventBuilder<T> {

        @SuppressWarnings("rawtypes")
        public static TestAuditEventBuilder<?> testAuditEventBuilder() {
            return new TestAuditEventBuilder();
        }

        public T exception(Exception e) {
            this.jsonValue.put("exception", e);
            return self();
        }
    }

}
