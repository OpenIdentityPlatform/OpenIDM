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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openidm.audit.util;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.PropertyAccessor;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

public class RouterActivityLoggerTest {

    public static final String TEST_MESSAGE = "test message";
    public static final String TEST_OBJECT_ID = "test_object_id";
    public static final String AUTHENTICATION_ID = "principal";
    private ServerContext context;
    private JsonValue before;
    private Request request;
    private JsonValue after;

    @BeforeClass
    public void setup() throws Exception {

        SecurityContext securityContext = new SecurityContext(new RootContext("test_id"), AUTHENTICATION_ID, null);
        context = new ServerContext(securityContext);

        before = json(object(
                field(Resource.FIELD_CONTENT_REVISION, "1"),
                field("test", "oldValue")
        ));

        request = Requests.newReadRequest("/bla/testPath");

        after = json(object(
                field(Resource.FIELD_CONTENT_REVISION, "2"),
                field("test", "newValue")
        ));

    }


    @Test
    public void testRootActivityLoggerWithBeforeAndAfter() throws Exception {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.create(any(Context.class), any(CreateRequest.class))).thenReturn(new Resource("ba", "1", null));
        ArgumentCaptor<CreateRequest> createRequestArgumentCaptor = ArgumentCaptor.forClass(CreateRequest.class);

        // when
        RouterActivityLogger activityLogger = new RouterActivityLogger(connectionFactory);
        activityLogger.log(context, request, TEST_MESSAGE, TEST_OBJECT_ID, before, after, Status.SUCCESS);

        // then
        verify(connection).create(any(Context.class), createRequestArgumentCaptor.capture());

        JsonValue content = createRequestArgumentCaptor.getValue().getContent();

        String capturedEventName = content.get(OpenIDMActivityAuditEventBuilder.EVENT_NAME).asString();
        assertThat(capturedEventName).isEqualTo(RouterActivityLogger.ACTIVITY_EVENT_NAME);

        String capturedRevision = content.get(OpenIDMActivityAuditEventBuilder.REVISION).asString();
        assertThat(capturedRevision).isEqualTo("2");

        String capturedMessage = content.get(OpenIDMActivityAuditEventBuilder.MESSAGE).asString();
        assertThat(capturedMessage).isEqualTo(TEST_MESSAGE);

        Status status = content.get(OpenIDMActivityAuditEventBuilder.STATUS).asEnum(Status.class);
        assertThat(status).isEqualTo(Status.SUCCESS);

        String runAs = content.get(OpenIDMActivityAuditEventBuilder.RUN_AS).asString();
        assertThat(runAs).isEqualTo(AUTHENTICATION_ID);

        RequestType requestType = request.getRequestType();
        assertThat(requestType).isEqualTo(RequestType.READ);

        String capturedAfter = content.get(OpenIDMActivityAuditEventBuilder.AFTER).asString();
        assertThat(capturedAfter).isNull();

        String capturedBefore = content.get(OpenIDMActivityAuditEventBuilder.BEFORE).asString();
        assertThat(capturedBefore).isNull();


    }

    @Test
    public void testRootActivityLoggerWithNullBeforeAndAfter() throws Exception {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.create(any(Context.class), any(CreateRequest.class))).thenReturn(new Resource("bl", "1", null));
        ArgumentCaptor<CreateRequest> createRequestArgumentCaptor = ArgumentCaptor.forClass(CreateRequest.class);

        // when
        RouterActivityLogger activityLogger = new RouterActivityLogger(connectionFactory);
        activityLogger.log(context, request, TEST_MESSAGE, TEST_OBJECT_ID, null, after, Status.SUCCESS);

        // then
        verify(connection).create(any(Context.class), createRequestArgumentCaptor.capture());

        JsonValue content = createRequestArgumentCaptor.getValue().getContent();

        String capturedEventName = content.get(OpenIDMActivityAuditEventBuilder.EVENT_NAME).asString();
        assertThat(capturedEventName).isEqualTo(RouterActivityLogger.ACTIVITY_EVENT_NAME);

        String capturedRevision = content.get(OpenIDMActivityAuditEventBuilder.REVISION).asString();
        assertThat(capturedRevision).isEqualTo("2");

        String capturedMessage = content.get(OpenIDMActivityAuditEventBuilder.MESSAGE).asString();
        assertThat(capturedMessage).isEqualTo(TEST_MESSAGE);

    }

    @Test
    public void testRootActivityLoggerWithBeforeAndNullAfter() throws Exception {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.create(any(Context.class), any(CreateRequest.class))).thenReturn(new Resource("bl", "1", null));
        ArgumentCaptor<CreateRequest> createRequestArgumentCaptor = ArgumentCaptor.forClass(CreateRequest.class);

        // when
        RouterActivityLogger activityLogger = new RouterActivityLogger(connectionFactory);
        activityLogger.log(context, request, TEST_MESSAGE, TEST_OBJECT_ID, before, null, Status.SUCCESS);

        // then
        verify(connection).create(any(Context.class), createRequestArgumentCaptor.capture());

        JsonValue content = createRequestArgumentCaptor.getValue().getContent();

        String capturedEventName = content.get(OpenIDMActivityAuditEventBuilder.EVENT_NAME).asString();
        assertThat(capturedEventName).isEqualTo(RouterActivityLogger.ACTIVITY_EVENT_NAME);

        // 1 is expected since the revision should come from the 'before'
        String capturedRevision = content.get(OpenIDMActivityAuditEventBuilder.REVISION).asString();
        assertThat(capturedRevision).isEqualTo("1");

        String capturedMessage = content.get(OpenIDMActivityAuditEventBuilder.MESSAGE).asString();
        assertThat(capturedMessage).isEqualTo(TEST_MESSAGE);

    }

    @Test
    public void testRootActivityLoggerWithLogFullObjectsOn() throws Exception {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.create(any(Context.class), any(CreateRequest.class))).thenReturn(new Resource("bl", "1", null));

        ArgumentCaptor<CreateRequest> createRequestArgumentCaptor = ArgumentCaptor.forClass(CreateRequest.class);

        // given
        IdentityServer.initInstance(new PropertyAccessor() {
            @Override
            public <T> T getProperty(String key, T defaultValue, Class<T> expected) {
                if (key.equals(RouterActivityLogger.OPENIDM_AUDIT_LOG_FULL_OBJECTS)) {
                    return (T) "true";
                }
                return defaultValue;
            }
        });

        // when
        RouterActivityLogger activityLogger = new RouterActivityLogger(connectionFactory);
        activityLogger.log(context, request, TEST_MESSAGE, TEST_OBJECT_ID, before, after, Status.SUCCESS);

        // then
        verify(connection).create(any(Context.class), createRequestArgumentCaptor.capture());

        JsonValue content = createRequestArgumentCaptor.getValue().getContent();

        String capturedAfter = content.get(OpenIDMActivityAuditEventBuilder.AFTER).asString();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> afterJson = mapper.readValue(capturedAfter, Map.class);

        String rev = afterJson.get(Resource.FIELD_CONTENT_REVISION);
        assertThat(rev).isEqualTo("2");

    }
}
