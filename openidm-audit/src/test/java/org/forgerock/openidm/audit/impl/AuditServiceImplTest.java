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
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openidm.audit.AuditService.AuditAction.availableHandlers;
import static org.forgerock.openidm.audit.AuditService.AuditAction.getChangedPasswordFields;
import static org.forgerock.openidm.audit.AuditService.AuditAction.getChangedWatchedFields;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.audit.events.AuditEventBuilder;
import org.forgerock.http.Context;
import org.forgerock.http.context.RootContext;
import org.forgerock.http.util.Json;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.events.handlers.impl.PassThroughAuditEventHandler;
import org.forgerock.openidm.audit.util.AuditTestUtils;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.Test;

/**
 * Test the audit service.
 */
public class AuditServiceImplTest {
    public static final String PASS_THROUGH_CONFIG_SCHEMA_JSON_FILE = "/PassThroughConfigSchema.json";
    public static final String AUDIT_JSON_FILE = "/audit.json";
    public static final String TEST_AUDIT_TOPIC = "test";
    public static final String ID = "id";

    @Test
    public void testAuditServiceActivation() throws Exception {
        //given
        final JSONEnhancedConfig jsonEnhancedConfig = mock(JSONEnhancedConfig.class);
        final ScriptRegistry scriptRegistry = mock(ScriptRegistry.class);
        final ScriptEntry scriptEntry = mock(ScriptEntry.class);
        final JsonValue config = AuditTestUtils.getJson(getResource(AUDIT_JSON_FILE));
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
        AuditServiceImpl auditService = createAuditService(AUDIT_JSON_FILE);

        final AuditEvent auditEvent = TestAuditEventBuilder.testAuditEventBuilder()
                .transactionId("transactionId")
                .eventName("eventName")
                .timestamp(System.currentTimeMillis())
                .authentication("testuser@forgerock.com")
                .toEvent();

        final CreateRequest createRequest = Requests.newCreateRequest(TEST_AUDIT_TOPIC, auditEvent.getValue());

        //when
        Promise<ResourceResponse, ResourceException> promise = auditService.handleCreate(
                new RootContext(), createRequest);

        //then
        AssertJPromiseAssert.assertThat(promise)
                .isNotNull()
                .succeeded();
        ResourceResponse resourceResponse = promise.getOrThrow();
        assertThat(resourceResponse.getContent().asMap()).isEqualTo(createRequest.getContent().asMap());
    }

    @Test
    public void testAuditServiceRead() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService(AUDIT_JSON_FILE);

        final ReadRequest readRequest = Requests.newReadRequest(TEST_AUDIT_TOPIC, ID);

        //when
        Promise<ResourceResponse, ResourceException> promise = auditService.handleRead(new RootContext(), readRequest);

        //then
        AssertJPromiseAssert.assertThat(promise)
                .isNotNull()
                .failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testAuditServiceUpdate() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService(AUDIT_JSON_FILE);

        final UpdateRequest updateRequest =
                Requests.newUpdateRequest(TEST_AUDIT_TOPIC, ID, new JsonValue(new LinkedHashMap<String, Object>()));

        //when
        Promise<ResourceResponse, ResourceException> promise = auditService.handleUpdate(
                new RootContext(), updateRequest);

        //then
        AssertJPromiseAssert.assertThat(promise)
                .isNotNull()
                .failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testAuditServiceDelete() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService(AUDIT_JSON_FILE);

        final DeleteRequest deleteRequest = Requests.newDeleteRequest(TEST_AUDIT_TOPIC, ID);

        //when
        Promise<ResourceResponse, ResourceException> promise = auditService.handleDelete(
                new RootContext(), deleteRequest);

        //then
        AssertJPromiseAssert.assertThat(promise)
                .isNotNull()
                .failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testAuditServicePatch() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService(AUDIT_JSON_FILE);

        final PatchRequest patchRequest =
                Requests.newPatchRequest(TEST_AUDIT_TOPIC, ID, PatchOperation.remove(new JsonPointer("/test")));

        //when
        Promise<ResourceResponse, ResourceException> promise = auditService.handlePatch(
                new RootContext(), patchRequest);

        //then
        AssertJPromiseAssert.assertThat(promise)
                .isNotNull()
                .failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testAuditServiceUnknownAction() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService(AUDIT_JSON_FILE);

        final ActionRequest actionRequest = Requests.newActionRequest(TEST_AUDIT_TOPIC, ID, "unknownAction");

        //when
        Promise<ActionResponse, ResourceException> promise = auditService.handleAction(new RootContext(),
                actionRequest);

        //then
        AssertJPromiseAssert.assertThat(promise)
                .failedWithException()
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    public void testActionForChangedFields() throws Exception {

        //given - depends on 'mail' being configured as a watched field.
        AuditServiceImpl auditService = createAuditService(AUDIT_JSON_FILE);
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
        ActionRequest changedFieldsRequest = Requests.newActionRequest(TEST_AUDIT_TOPIC, ID, getChangedWatchedFields.name());
        changedFieldsRequest.setContent(testContent);

        // when
        Promise<ActionResponse, ResourceException> promise = auditService.handleAction(new RootContext(),
                changedFieldsRequest);

        // then
        AssertJPromiseAssert.assertThat(promise).succeeded();
        ActionResponse actionResponse = promise.getOrThrow();
        List<String> changedFields = actionResponse.getJsonContent().asList(String.class);
        assertThat(changedFields.size()).isEqualTo(1);
        assertThat(changedFields.get(0)).isEqualTo("/mail");
    }

    @Test
    public void testActionForChangedPassword() throws Exception {
        // given
        AuditServiceImpl auditService = createAuditService(AUDIT_JSON_FILE);
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

        ActionRequest changedPasswordRequest = Requests.newActionRequest(TEST_AUDIT_TOPIC, ID, getChangedPasswordFields.name());
        changedPasswordRequest.setContent(testContent);

        //when
        Promise<ActionResponse, ResourceException> promise = auditService.handleAction(new RootContext(),
                changedPasswordRequest);

        //then
        AssertJPromiseAssert.assertThat(promise).succeeded();
        ActionResponse response = promise.getOrThrow();
        List<String> changedPasswords = response.getJsonContent().asList(String.class);
        assertThat(changedPasswords.size()).isEqualTo(1);
        assertThat(changedPasswords.get(0)).isEqualTo("/password");
    }

    @Test
    public void testAuditServiceQuery() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService(AUDIT_JSON_FILE);

        final QueryRequest queryRequest = Requests.newQueryRequest(TEST_AUDIT_TOPIC);

        final QueryResourceHandler queryResultHandler = mock(QueryResourceHandler.class);

        //when
        Promise<QueryResponse, ResourceException> promise = auditService.handleQuery(new RootContext(), queryRequest,
                queryResultHandler);

        //then
        AssertJPromiseAssert.assertThat(promise)
                .isNotNull()
                .failedWithException()
                .isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testExceptionFormatter() throws Exception {
        //given
        AuditServiceImpl auditService = createAuditService(AUDIT_JSON_FILE);

        final AuditEvent auditEvent = TestAuditEventBuilder.testAuditEventBuilder()
                .transactionId("transactionId")
                .eventName("eventName")
                .timestamp(System.currentTimeMillis())
                .authentication("testuser@forgerock.com")
                .exception(new Exception("Test Exception"))
                .toEvent();

        final CreateRequest createRequest = Requests.newCreateRequest(TEST_AUDIT_TOPIC, auditEvent.getValue());

        //when
        Promise<ResourceResponse, ResourceException> promise = auditService.handleCreate(
                new RootContext(), createRequest);

        //then
        AssertJPromiseAssert.assertThat(promise).succeeded();
        JsonValue content = promise.get().getContent();

        final JsonValue expectedResource = createRequest.getContent();
        expectedResource.put("exception", "Exception Formatted");

        assertThat(content).isNotNull();
        assertThat(content.asMap()).isEqualTo(createRequest.getContent().asMap());
    }

    @Test
    public void testAvailableHandlersAction() throws Exception {
        final AuditServiceImpl auditService = createAuditService(AUDIT_JSON_FILE);

        //when
        Promise<ActionResponse, ResourceException> promise = auditService.handleAction(
                new RootContext(), Requests.newActionRequest("", availableHandlers.name()));

        //then
        AssertJPromiseAssert.assertThat(promise).succeeded();
        ActionResponse actionResponse = promise.get();
        final JsonValue result = actionResponse.getJsonContent();
        assertThat(result.keys().size()).isEqualTo(1);
        assertThat(result.get(0).get("class").asString())
                .isEqualTo(PassThroughAuditEventHandler.class.getName());

        // { "type": "object", "properties": { "message": { "type": "string" } } }
        final JsonValue expectedConfig;
        try (InputStream in = getClass().getResourceAsStream(PASS_THROUGH_CONFIG_SCHEMA_JSON_FILE)) {
            expectedConfig = json(Json.readJson(new InputStreamReader(in)));
        }
        assertThat(result.get(0).get("config").asMap()).isEqualTo(expectedConfig.asMap());
    }

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
