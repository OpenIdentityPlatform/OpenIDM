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
package org.forgerock.openidm.sync.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.assertj.core.data.MapEntry;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceName;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.ServerContext;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

import java.util.Collection;

public class ReconciliationServiceTest {

    @Test
    public void testReadReconById() throws Exception {
        //given
        final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        final ReconciliationService reconciliationService = createReconciliationService(connectionFactory);

        final ReadRequest readRequest = Requests.newReadRequest(new ResourceName("resource"));
        final ResultHandler<Resource> resultHandler= mockResultHandler(Resource.class);
        final Connection connection = mock(Connection.class);
        final ArgumentCaptor<QueryRequest> queryRequestArgumentCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        final ArgumentCaptor<Resource> resourceArgumentCaptor = ArgumentCaptor.forClass(Resource.class);
        final ServerContext context = mock(ServerContext.class);

        final Resource expectedResource = new Resource("id", "rev",
                JsonValue.json(object(field("messageDetail", object(field("key", "value"))))));

        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.query(any(ServerContext.class), any(QueryRequest.class), any(Collection.class)))
                .then(new Answer<QueryResult>() {
                    @Override
                    public QueryResult answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Collection<Resource> collection = (Collection<Resource>) invocationOnMock.getArguments()[2];
                        collection.add(expectedResource);
                        return new QueryResult();
                    }
                });

        //when
        reconciliationService.handleRead(context, readRequest, resultHandler);

        //then
        verify(connection).query(any(Context.class), queryRequestArgumentCaptor.capture(), any(Collection.class));
        verify(resultHandler).handleResult(resourceArgumentCaptor.capture());

        final Resource resource = resourceArgumentCaptor.getValue();
        assertThat(resource.getId()).isEqualTo(readRequest.getResourceNameObject().leaf());
        assertThat(resource.getRevision()).isNull();
        assertThat(resource.getContent().asMap()).containsExactly(MapEntry.entry("key", "value"));

    }

    @Test
    public void testReadWithEmptyReconAuditLog() throws Exception {
        //given
        final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        final ReconciliationService reconciliationService = createReconciliationService(connectionFactory);

        final ReadRequest readRequest = Requests.newReadRequest(new ResourceName("resource"));
        final ResultHandler<Resource> resultHandler= mockResultHandler(Resource.class);
        final Connection connection = mock(Connection.class);
        final ArgumentCaptor<QueryRequest> queryRequestArgumentCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        final ArgumentCaptor<ResourceException> resourceExceptionArgumentCaptor =
                ArgumentCaptor.forClass(ResourceException.class);
        final ServerContext context = mock(ServerContext.class);

        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.query(any(ServerContext.class), any(QueryRequest.class), any(Collection.class)))
                .thenReturn(new QueryResult());

        //when
        reconciliationService.handleRead(context, readRequest, resultHandler);

        //then
        verify(connection).query(any(Context.class), queryRequestArgumentCaptor.capture(), any(Collection.class));
        verify(resultHandler).handleError(resourceExceptionArgumentCaptor.capture());

        assertThat(resourceExceptionArgumentCaptor.getValue()).isInstanceOf(NotFoundException.class);

    }

    @SuppressWarnings("unchecked")
    private static <T> ResultHandler<T> mockResultHandler(Class<T> type) {
        return mock(ResultHandler.class);
    }

    private ReconciliationService createReconciliationService(final ConnectionFactory connectionFactory) {
        final ReconciliationService reconciliationService = new ReconciliationService();
        reconciliationService.bindConnectionFactory(connectionFactory);
        return reconciliationService;
    }
}
