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
 * Portions copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.assertj.core.data.MapEntry;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.router.IDMConnectionFactoryWrapper;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

public class ReconciliationServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testReadReconById() throws Exception {
        //given
        final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        final ReconciliationService reconciliationService = createReconciliationService(connectionFactory);

        final ReadRequest readRequest = Requests.newReadRequest(new ResourcePath("resource"));
        final Connection connection = mock(Connection.class);
        final Context context = mock(Context.class);

        final ResourceResponse expectedResource = newResourceResponse("id", "rev",
                JsonValue.json(object(field("messageDetail", object(field("key", "value"))))));

        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.query(any(Context.class), any(QueryRequest.class), any(Collection.class)))
	        .then(new Answer<QueryResponse>() {
	        	@Override
	        	public QueryResponse answer(InvocationOnMock invocationOnMock) throws Throwable {
	        		Collection<ResourceResponse> collection = (Collection<ResourceResponse>) invocationOnMock.getArguments()[2];
	        		collection.add(expectedResource);
	        		return newQueryResponse();
	        	}
	        });

        Promise<ResourceResponse, ResourceException> readPromise = reconciliationService.handleRead(context, readRequest);
        AssertJPromiseAssert.assertThat(readPromise).succeeded();

        ResourceResponse readResponse = readPromise.get();
        assertThat(readResponse.getId()).isEqualTo(readRequest.getResourcePathObject().leaf());
        assertThat(readResponse.getRevision()).isNull();
        assertThat(readResponse.getContent().asMap()).containsExactly(MapEntry.entry("key", "value"));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReadWithEmptyReconAuditLog() throws Exception {
        //given
        final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        final ReconciliationService reconciliationService = createReconciliationService(connectionFactory);

        final ReadRequest readRequest = Requests.newReadRequest(new ResourcePath("resource"));
        final Connection connection = mock(Connection.class);
        final Context context = mock(Context.class);

        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.query(any(Context.class), any(QueryRequest.class), any(Collection.class)))
                .thenReturn(newQueryResponse());

        AssertJPromiseAssert.assertThat(reconciliationService.handleRead(context, readRequest))
        		.failedWithException()
        		.isInstanceOf(NotFoundException.class);

    }

    private ReconciliationService createReconciliationService(final ConnectionFactory connectionFactory) {
        final ReconciliationService reconciliationService = new ReconciliationService();
        reconciliationService.bindConnectionFactory(new IDMConnectionFactoryWrapper(connectionFactory));
        return reconciliationService;
    }
}
