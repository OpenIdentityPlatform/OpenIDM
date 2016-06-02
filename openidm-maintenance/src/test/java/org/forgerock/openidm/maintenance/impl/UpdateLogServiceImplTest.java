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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.maintenance.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.Mockito.mock;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.openidm.maintenance.upgrade.UpdateLogEntry;
import org.forgerock.openidm.mocks.MockRequestHandler;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.testng.annotations.Test;

/**
 * Test RequestHandler for managing history of product updates.
 */
public class UpdateLogServiceImplTest {

    private UpdateLogServiceImpl updateLogService = new UpdateLogServiceImpl();

    @Test
    public void actionIsNotSupported() {
        assertThat(updateLogService.handleAction(mock(Context.class), mock(ActionRequest.class)))
                .failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void createIsNotSupported() {
        assertThat(updateLogService.handleCreate(mock(Context.class), mock(CreateRequest.class)))
                .failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void deleteIsNotSupported() {
        assertThat(updateLogService.handleDelete(mock(Context.class), mock(DeleteRequest.class)))
                .failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void patchIsNotSupported() {
        assertThat(updateLogService.handlePatch(mock(Context.class), mock(PatchRequest.class)))
                .failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testQuery() throws ResourceException {
        final MockRequestHandler handler = new MockRequestHandler();
        // create a resource to query
        handler.addResource(newResourceResponse("1", "1", new UpdateLogEntry().toJson()));

        updateLogService.bindConnectionFactory(newConnectionFactory(handler));

        // perform the query
        final QueryRequest request = newQueryRequest("").setQueryFilter(QueryFilter.<JsonPointer>alwaysTrue());
        final Promise<QueryResponse, ResourceException> promise =
                updateLogService.handleQuery(new RootContext(), request, new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(ResourceResponse resource) {
                        return true;
                    }
                });
        assertThat(promise).succeeded();
        assertThat(handler.getRequests()).hasSize(1);
    }

    @Test
    public void testRead() throws ResourceException {
        final MockRequestHandler handler = new MockRequestHandler();
        // create a resource to read
        handler.addResource(newResourceResponse("1", "1", new UpdateLogEntry().toJson()));

        updateLogService.bindConnectionFactory(newConnectionFactory(handler));

        // perform the read
        final ReadRequest request = newReadRequest("1");
        final Promise<ResourceResponse, ResourceException> promise =
                updateLogService.handleRead(new RootContext(), request);
        assertThat(promise).succeeded();
        assertThat(handler.getRequests()).hasSize(1);
    }

    @Test
    public void testLogUpdate() throws ResourceException {
        final MockRequestHandler handler = new MockRequestHandler();

        updateLogService.bindConnectionFactory(newConnectionFactory(handler));
        updateLogService.logUpdate(new UpdateLogEntry());
        assertThat(handler.getRequests()).hasSize(1);
    }

    @Test
    public void testUpdateUpdate() throws ResourceException {
        final MockRequestHandler handler = new MockRequestHandler();

        updateLogService.bindConnectionFactory(newConnectionFactory(handler));
        updateLogService.updateUpdate(new UpdateLogEntry().setId("1"));
        assertThat(handler.getRequests()).hasSize(1);
    }

    private IDMConnectionFactory newConnectionFactory(final RequestHandler handler) {
        return new IDMConnectionFactory() {
            private ConnectionFactory delegate = Resources.newInternalConnectionFactory(handler);

            @Override
            public Connection getExternalConnection() throws ResourceException {
                return delegate.getConnection();
            }

            @Override
            public Promise<Connection, ResourceException> getExternalConnectionAsync() {
                return delegate.getConnectionAsync();
            }

            @Override
            public void close() {
            }

            @Override
            public Connection getConnection() throws ResourceException {
                return delegate.getConnection();
            }

            @Override
            public Promise<Connection, ResourceException> getConnectionAsync() {
                return delegate.getConnectionAsync();
            }
        };
    }
}
