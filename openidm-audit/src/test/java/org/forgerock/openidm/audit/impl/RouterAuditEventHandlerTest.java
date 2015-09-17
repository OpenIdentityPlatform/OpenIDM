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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.forgerock.audit.DependencyProviderBase;
import org.forgerock.http.context.RootContext;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.Router;
import org.forgerock.openidm.audit.mocks.MockRequestHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RouterAuditEventHandlerTest {

    private ConnectionFactory connectionFactory;
    private RouterAuditEventHandler routerAuditEventHandler;
    private MockRequestHandler requestHandler;


    @BeforeMethod
    public void setUp() throws ResourceException {
        requestHandler = new MockRequestHandler();
        Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, Router.uriTemplate("audit/db"), requestHandler);
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
    public void testReadEntry() throws Exception {
        //given
        final JsonValue content = json(object(field("somedata", "foo")));
        requestHandler.addResource(
                Responses.newResourceResponse(ResourceResponse.FIELD_ID, ResourceResponse.FIELD_REVISION, content));

        //when
        Promise<ResourceResponse, ResourceException> promise = routerAuditEventHandler.readEvent(new RootContext(),
                "access",
                ResourceResponse.FIELD_ID);

        //then
        assertThat(requestHandler.getRequests().size()).isEqualTo(1);
        final Request request = requestHandler.getRequests().get(0);
        assertThat(request).isInstanceOf(ReadRequest.class);
        assertThat(request.getResourcePath()).isEqualTo("access/" + ResourceResponse.FIELD_ID);

        ResourceResponse response = promise.getOrThrowUninterruptibly();

        assertThat(response).isNotNull();
        assertThat(response.getContent().asMap()).isEqualTo(content.asMap());

        requestHandler.getRequests().clear();
    }

    @Test
    public void testQueryEntries() throws Exception {
        //given
        final JsonValue content = json(object(field("somedata", "foo")));
        requestHandler.addResource(
                Responses.newResourceResponse(ResourceResponse.FIELD_ID, ResourceResponse.FIELD_REVISION, content));

        final QueryResourceHandler queryResultHandler = mock(QueryResourceHandler.class);
        final ArgumentCaptor<ResourceResponse> resourceCaptor = ArgumentCaptor.forClass(ResourceResponse.class);


        //when
        Promise<QueryResponse, ResourceException> promise = routerAuditEventHandler.queryEvents(new RootContext(),
                "access",
                Requests.newQueryRequest("access").setQueryFilter(QueryFilters.parse("/_id eq \"_id\"")),
                queryResultHandler);

        //then
        assertThat(requestHandler.getRequests().size() == 1);
        final Request request = requestHandler.getRequests().get(0);
        assertThat(request).isInstanceOf(QueryRequest.class);
        assertThat(request.getResourcePath()).isEqualTo("access");

        AssertJPromiseAssert.assertThat(promise).succeeded();

        verify(queryResultHandler, times(1)).handleResource(resourceCaptor.capture());
        final ResourceResponse resource = resourceCaptor.getValue();
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(content.asMap());

        requestHandler.getRequests().clear();
    }

    @Test
    public void testCreateEntry() throws Exception {
        //given
        final JsonValue content = json(object(
                field("somedata", "foo"),
                field(ResourceResponse.FIELD_CONTENT_ID, ResourceResponse.FIELD_ID)
        ));

        //when
        Promise<ResourceResponse, ResourceException> promise = routerAuditEventHandler.publishEvent(new RootContext(),
                "access", content);

        //then
        ResourceResponse resource = promise.getOrThrow();

        assertThat(requestHandler.getRequests().size() == 1);
        final Request request = requestHandler.getRequests().get(0);
        assertThat(request).isInstanceOf(CreateRequest.class);
        assertThat(request.getResourcePath()).isEqualTo("access");
        assertThat(((CreateRequest) request).getNewResourceId()).isEqualTo(ResourceResponse.FIELD_ID);

        assertThat(resource).isNotNull();
        assertThat(resource.getContent().asMap()).isEqualTo(content.asMap());

        requestHandler.getRequests().clear();
    }

}
