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
package org.forgerock.openidm.scheduler;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Resources.newInternalConnectionFactory;
import static org.forgerock.openidm.scheduler.TriggerRequestHandler.TRIGGERS_REPO_RESOURCE_PATH;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.testng.annotations.Test;

public class TriggerRequestHandlerTest {

    private static final int NUMBER_OF_TRIGGERS = 12;

    @Test
    public void testCreateTrigger() {
        // given
        final ConnectionFactory connectionFactory = createConnectionFactory();
        final TriggerRequestHandler triggerRequestHandler = new TriggerRequestHandler(connectionFactory);

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                triggerRequestHandler.handleCreate(new RootContext(), Requests.newCreateRequest("", json(object())));

        // then
        assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testReadTrigger() throws ResourceException {
        // given
        final ConnectionFactory connectionFactory = createConnectionFactory();
        final TriggerRequestHandler triggerRequestHandler = new TriggerRequestHandler(connectionFactory);
        createTrigger(connectionFactory, "trigger1");

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                triggerRequestHandler.handleRead(new RootContext(), Requests.newReadRequest("", "trigger1"));

        // then
        assertThat(promise).succeeded().isNotNull();
    }

    @Test
    public void testUpdateTrigger() {
        // given
        final ConnectionFactory connectionFactory = createConnectionFactory();
        final TriggerRequestHandler triggerRequestHandler = new TriggerRequestHandler(connectionFactory);

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                triggerRequestHandler.handleUpdate(new RootContext(), Requests.newUpdateRequest("", json(object())));

        // then
        assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testDeleteTrigger() {
        // given
        final ConnectionFactory connectionFactory = createConnectionFactory();
        final TriggerRequestHandler triggerRequestHandler = new TriggerRequestHandler(connectionFactory);

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                triggerRequestHandler.handleDelete(new RootContext(), Requests.newDeleteRequest(""));

        // then
        assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testPatchTrigger() {
        // given
        final ConnectionFactory connectionFactory = createConnectionFactory();
        final TriggerRequestHandler triggerRequestHandler = new TriggerRequestHandler(connectionFactory);

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                triggerRequestHandler.handlePatch(new RootContext(), Requests.newPatchRequest(""));

        // then
        assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testAction() {
        // given
        final ConnectionFactory connectionFactory = createConnectionFactory();
        final TriggerRequestHandler triggerRequestHandler = new TriggerRequestHandler(connectionFactory);

        // when
        final Promise<ActionResponse, ResourceException> promise =
                triggerRequestHandler.handleAction(new RootContext(), Requests.newActionRequest("", ""));

        // then
        assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testQueryTrigger() throws ResourceException {
        // given
        final ConnectionFactory connectionFactory = createConnectionFactory();
        final TriggerRequestHandler triggerRequestHandler = new TriggerRequestHandler(connectionFactory);
        for (int i = 0; i < NUMBER_OF_TRIGGERS; i++) {
            createTrigger(connectionFactory, UUID.randomUUID().toString());
        }
        final QueryRequest request = Requests.newQueryRequest("").setQueryFilter(QueryFilter.<JsonPointer>alwaysTrue());

        // when
        final AtomicInteger count = new AtomicInteger();
        final Promise<QueryResponse, ResourceException> promise =
                triggerRequestHandler.handleQuery(new RootContext(), request, new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(ResourceResponse resourceResponse) {
                        count.incrementAndGet();
                        return true;
                    }
                });

        // then
        assertThat(promise).succeeded().isNotNull();
        Assertions.assertThat(count.get()).isEqualTo(NUMBER_OF_TRIGGERS);
    }

    private ConnectionFactory createConnectionFactory() {
        final Router router = new Router();
        router.addRoute(Router.uriTemplate(TRIGGERS_REPO_RESOURCE_PATH), new MemoryBackend());
        return newInternalConnectionFactory(router);
    }

    private void createTrigger(final ConnectionFactory connectionFactory, final String name) throws ResourceException {
        connectionFactory.getConnection().createAsync(
                new RootContext(),
                Requests.newCreateRequest(TRIGGERS_REPO_RESOURCE_PATH, name, JsonValue.json(object())));
    }
}
