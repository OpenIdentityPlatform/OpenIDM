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

import org.forgerock.audit.DependencyProviderBase;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.audit.mocks.*;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.openidm.audit.util.AuditTestUtils.mockResultHandler;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RouterAuditEventHandlerTest {

    private ConnectionFactory connectionFactory;
    private RouterAuditEventHandler routerAuditEventHandler;
    private MockRequestHandler requestHandler;


    @BeforeMethod
    public void setUp() throws ResourceException {
        requestHandler = new MockRequestHandler();
        Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "audit/db", requestHandler);
        connectionFactory = Resources.newInternalConnectionFactory(router);

        routerAuditEventHandler = new RouterAuditEventHandler();
        RouterAuditEventHandlerConfiguration config = new RouterAuditEventHandlerConfiguration();
        config.setResourcePath("audit/db");
        routerAuditEventHandler.configure(config);
        routerAuditEventHandler.setDependencyProvider(new DependencyProviderBase() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> T getDependency(Class<T> aClass) throws ClassNotFoundException {
                if (ConnectionFactory.class.isAssignableFrom(aClass)) {
                    return (T) connectionFactory;
                } else {
                    return super.getDependency(aClass);
                }
            }
        });
    }

    @AfterMethod
    public void tearDown() {
        requestHandler.getRequests().clear();
    }


    @Test
    public void testReadEntry() throws ResourceException, IOException {
        //given
        final JsonValue content = json(object(field("somedata", "foo")));
        requestHandler.addResource(new Resource(Resource.FIELD_ID, Resource.FIELD_REVISION, content));
        final ResultHandler<Resource> resultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);

        //when
        routerAuditEventHandler.readInstance(new ServerContext(new RootContext()), Resource.FIELD_ID,
                Requests.newReadRequest("access"), resultHandler);

        //then
        assertThat(requestHandler.getRequests().size()).isEqualTo(1);
        final Request request = requestHandler.getRequests().get(0);
        assertThat(request).isInstanceOf(ReadRequest.class);
        assertThat(request.getResourceName()).isEqualTo("access/" + Resource.FIELD_ID);

        verify(resultHandler, never()).handleError(any(ResourceException.class));
        verify(resultHandler, times(1)).handleResult(resourceCaptor.capture());
        Resource resource = resourceCaptor.getValue();
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(content.asMap());

        requestHandler.getRequests().clear();
    }

    @Test
    public void testQueryEntries() throws ResourceException, IOException {
        //given
        final JsonValue content = json(object(field("somedata", "foo")));
        requestHandler.addResource(new Resource(Resource.FIELD_ID, Resource.FIELD_REVISION, content));

        final QueryResultHandler queryResultHandler = mock(QueryResultHandler.class);
        final ArgumentCaptor<QueryResult> queryResultCaptor = ArgumentCaptor.forClass(QueryResult.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        final ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);

        //when
        routerAuditEventHandler.queryCollection(
                new ServerContext(new RootContext()),
                Requests.newQueryRequest("access").setQueryFilter(QueryFilter.valueOf("/_id eq \"_id\"")),
                queryResultHandler);

        //then
        assertThat(requestHandler.getRequests().size() == 1);
        final Request request = requestHandler.getRequests().get(0);
        assertThat(request).isInstanceOf(QueryRequest.class);
        assertThat(request.getResourceName()).isEqualTo("access");

        verify(queryResultHandler, never()).handleError(resourceExceptionCaptor.capture());
        verify(queryResultHandler, times(1)).handleResource(resourceCaptor.capture());
        verify(queryResultHandler).handleResult(queryResultCaptor.capture());
        final Resource resource = resourceCaptor.getValue();
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(content.asMap());

        requestHandler.getRequests().clear();
    }

    @Test
    public void testCreateEntry() throws ResourceException, IOException {
        //given
        final JsonValue content = json(object(field("somedata", "foo")));
        final ResultHandler<Resource> resultHandler = mockResultHandler(Resource.class);
        final ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);

        //when
        routerAuditEventHandler.createInstance(new ServerContext(new RootContext()),
                Requests.newCreateRequest("access", Resource.FIELD_ID, content), resultHandler);

        //then
        assertThat(requestHandler.getRequests().size() == 1);
        final Request request = requestHandler.getRequests().get(0);
        assertThat(request).isInstanceOf(CreateRequest.class);
        assertThat(request.getResourceName()).isEqualTo("access");
        assertThat(((CreateRequest) request).getNewResourceId()).isEqualTo(Resource.FIELD_ID);

        verify(resultHandler, times(1)).handleResult(resourceCaptor.capture());
        verify(resultHandler, never()).handleError(any(ResourceException.class));
        Resource resource = resourceCaptor.getValue();
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(content.asMap());

        requestHandler.getRequests().clear();
    }

}
