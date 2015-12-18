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

package org.forgerock.openidm.audit.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Response;
import org.forgerock.json.resource.Router;
import org.forgerock.services.TransactionId;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.services.context.TransactionIdContext;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test the router audit filter
 */
public class AuditFilterTest {

    private Context context;
    private ConnectionFactory connectionFactory;
    private MemoryBackend backend;

    @BeforeClass
    public void init() {
        Map<String, Object> authorization = new HashMap<>();
        authorization.put("authenticationId", "openidm");
        authorization.put("roles", Collections.singletonList("openidm"));

        context = ClientContext.buildExternalClientContext(
                new TransactionIdContext(
                        new SecurityContext(new RootContext(), "openidm", authorization),
                        new TransactionId()))
                .build();
    }

    @BeforeMethod
    public void initTest() {
        backend = new MemoryBackend();
        Router router = new Router();
        router.addRoute(uriTemplate("audit/access"), backend);
        connectionFactory = Resources.newInternalConnectionFactory(router);
    }

    @Test
    public void testResourceExceptionFromHandler() throws Exception {
        Request request = mock(Request.class);
        when(request.getRequestType()).thenReturn(RequestType.READ);

        try {
            AuditFilter filter = new AuditFilter(connectionFactory);
            filter.logAuditAccessEntry(context, request,
                    new AsyncFunction<Void, Response, ResourceException>() {
                        @Override
                        public Promise<? extends Response, ? extends ResourceException> apply(Void value) throws ResourceException {
                            return new NotFoundException().asPromise();
                        }
                    })
                    .getOrThrow();
            fail("expected NotFoundException");
        } catch (ResourceException e) {
            assertThat(e).isInstanceOf(NotFoundException.class);
        }
    }

    @Test
    public void testUnableToCreate() throws Exception {
        Request request = mock(Request.class);
        when(request.getRequestType()).thenReturn(RequestType.READ);

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.getConnection()).thenThrow(new BadRequestException());

        AuditFilter filter = new AuditFilter(connectionFactory);
        filter.logAuditAccessEntry(context, request,
                new AsyncFunction<Void, Response, ResourceException>() {
                    @Override
                    public Promise<? extends Response, ? extends ResourceException> apply(Void value) throws ResourceException {
                        return newResourceResponse("test", "1", json(true)).asPromise();
                    }
                })
                .getOrThrow();

        final AtomicLong count = new AtomicLong(0);
        QueryRequest query = newQueryRequest("");
        query.setQueryFilter(QueryFilter.<JsonPointer>alwaysTrue());
        backend.queryCollection(context, query, new QueryResourceHandler() {
            @Override
            public boolean handleResource(ResourceResponse resource) {
                count.incrementAndGet();
                return true;
            }
        }).getOrThrow();

        // assert no records were created
        assertThat(count.get()).isEqualTo(0);
    }

    @Test
    public void testLogAuditAccessEntry() throws Exception {
        Request request = mock(Request.class);
        when(request.getRequestType()).thenReturn(RequestType.READ);

        AuditFilter filter = new AuditFilter(connectionFactory);
        filter.logAuditAccessEntry(context, request,
                new AsyncFunction<Void, Response, ResourceException>() {
                    @Override
                    public Promise<? extends Response, ? extends ResourceException> apply(Void value) throws ResourceException {
                        return newResourceResponse("test", "1", json(true)).asPromise();
                    }
                })
                .getOrThrow();

        QueryRequest query = newQueryRequest("");
        query.setQueryFilter(QueryFilter.<JsonPointer>alwaysTrue());
        backend.queryCollection(context, query, new QueryResourceHandler() {
            @Override
            public boolean handleResource(ResourceResponse resource) {
                JsonValue content = resource.getContent();
                // assert random assortment of fields
                assertThat(content.get("roles").isList()).isTrue();
                assertThat(content.get("roles").asList(String.class).get(0)).isEqualTo("openidm");
                assertThat(content.get("transactionId").asString()).isEqualTo(
                        context.asContext(TransactionIdContext.class).getTransactionId().getValue());
                assertThat(content.get("request").get("protocol").asString()).isEqualTo("CREST");
                assertThat(content.get("request").get("operation").asString()).isEqualTo("READ");
                assertThat(content.get("eventName").asString()).isEqualTo("access");
                assertThat(content.get("userId").asString()).isEqualTo("openidm");
                return true;
            }
        });
    }
}
