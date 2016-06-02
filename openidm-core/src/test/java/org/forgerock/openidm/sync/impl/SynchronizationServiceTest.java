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
 * Portions copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newActionRequest;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.sync.impl.SynchronizationService.ACTION_PARAM_RESOURCE_NAME;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.util.Scripts;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SynchronizationServiceTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonValue syncConfig;

    @BeforeClass
    public void init() throws IOException {
        syncConfig = json(mapper.readValue(
                SynchronizationServiceTest.class.getResourceAsStream("/conf/sync.json"), Map.class));
        Scripts.init(mock(ScriptRegistry.class));
    }

    @Test
    public void testAuditScheduledService() throws Exception {
        //given
        final SynchronizationService synchronizationService = new SynchronizationService();
        final IDMConnectionFactory connectionFactory = mock(IDMConnectionFactory.class);
        final Connection connection = mock(Connection.class);
        final Context context = mock(Context.class);
        final AuditEvent auditEvent = mock(AuditEvent.class);
        final ArgumentCaptor<CreateRequest> argumentCaptor = ArgumentCaptor.forClass(CreateRequest.class);

        synchronizationService.bindConnectionFactory(connectionFactory);

        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.create(any(Context.class), argumentCaptor.capture()))
                .thenReturn(newResourceResponse("id", "rev", null));
        when(auditEvent.getValue()).thenReturn(json(object()));

        //when
        synchronizationService.auditScheduledService(context, auditEvent);

        //then
        verify(connection).create(any(Context.class), any(CreateRequest.class));
        assertThat(argumentCaptor.getValue().getContent()).isEqualTo(auditEvent.getValue());
        assertThat(argumentCaptor.getValue().getResourcePath()).isEqualTo("audit/access");
    }

    @Test
    public void getLinkedViewShouldReturnMissingDataOnFailureToReadTarget() throws Exception {
        final IDMConnectionFactory connectionFactory = mock(IDMConnectionFactory.class);
        final Connection connection = mock(Connection.class);
        final EnhancedConfig enhancedConfig = mock(EnhancedConfig.class);

        when(enhancedConfig.getConfiguration(any(ComponentContext.class))).thenReturn(syncConfig.asMap());
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connectionFactory.getExternalConnection()).thenReturn(connection);

        // when querying the links table call the ResourceHandler with a mock link object
        final ArgumentCaptor<QueryRequest> queryRequestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        when(connection.query(any(Context.class), queryRequestCaptor.capture(), any(QueryResourceHandler.class)))
                .thenAnswer(new Answer<QueryResponse>() {
                    public QueryResponse answer(InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        Context context = (Context) args[0];
                        QueryRequest request = (QueryRequest) args[1];
                        if (request.getResourcePath().equals("repo/link")) {
                            QueryResourceHandler resourceHandler = (QueryResourceHandler) args[2];
                            resourceHandler.handleResource(newResourceResponse("id", "1", json(object(
                                    field("linkType", "testMapping"),
                                    field("firstId", "managd/user/bjensen"),
                                    field("secondId", "0123-456789ab-cdef"),
                                    field("linkQualifier", "default")
                            ))));
                        }
                        return newQueryResponse();
                    }
                });

        // when reading the target system, thrown an exception to simulate its unavailability
        final ArgumentCaptor<ReadRequest> readRequestCaptor = ArgumentCaptor.forClass(ReadRequest.class);
        when(connection.readAsync(any(Context.class), readRequestCaptor.capture()))
                .thenAnswer(new Answer<Promise<ResourceResponse, ResourceException>>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> answer(InvocationOnMock invocationOnMock) {
                        return new ServiceUnavailableException("Unable to read target").asPromise();
                    }
                });

        final SyncMappings mappings = new SyncMappings();
        mappings.bindEnhancedConfig(enhancedConfig);
        mappings.activate(mock(ComponentContext.class));

        final SynchronizationService synchronizationService = new SynchronizationService();
        synchronizationService.bindConnectionFactory(connectionFactory);
        synchronizationService.bindMappings(mappings);

        final ActionRequest actionRequest = newActionRequest("", "getLinkedResources")
                .setAdditionalParameter(ACTION_PARAM_RESOURCE_NAME, "managed/user/bjensen");
        Promise<ActionResponse, ResourceException> promise = synchronizationService.actionInstance(
                mock(Context.class), actionRequest);

        assertThat(promise).succeeded();
        JsonValue resource = promise.get().getJsonContent().get(0);
        assertThat(resource).stringAt("resourceName").isEqualTo("system/ldap/account/0123-456789ab-cdef");
        assertThat(resource).hasNull("content");
        assertThat(resource).stringAt("error").isEqualTo("Unable to read target");
        assertThat(resource).stringAt("linkQualifier").isEqualTo("default");
        assertThat(resource).stringAt("linkType").isEqualTo("testMapping");
    }
}
