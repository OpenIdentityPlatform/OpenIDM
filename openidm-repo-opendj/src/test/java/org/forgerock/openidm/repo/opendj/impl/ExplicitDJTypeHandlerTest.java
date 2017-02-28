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
 * Copyright 2017 ForgeRock AS.
 */
package org.forgerock.openidm.repo.opendj.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.json.resource.ResourcePath.resourcePath;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.json.resource.SortKey.ascendingOrder;
import static org.forgerock.json.resource.SortKey.descendingOrder;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNull;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.SortKey;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.openidm.router.RouteEntry;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.testng.annotations.Test;

public class ExplicitDJTypeHandlerTest {

    private static final Promise<ResourceResponse, ResourceException> EMPTY_PROMISE =
            newResourceResponse("newObject", null, json(object())).asPromise();

    private static final JsonValue SINGLE_UNIQUE_CONSTRAINT_CONFIG =
            json(object(field(ExplicitDJTypeHandler.UNIQUE_CONSTRAINTS, array(array("uniqueField1", "uniqueField2")))));
    private static final JsonValue MULTIPLE_UNIQUE_CONSTRAINT_CONFIG =
            json(object(field(ExplicitDJTypeHandler.UNIQUE_CONSTRAINTS, array(
                    array("uniqueField1", "uniqueField2"), array("uniqueField2", "uniqueField3")))));
    private static final JsonValue EMPTY_CONFIG = json(object());

    @Test
    public void shouldNormalizeQueryIds() {
        // given
        final RequestHandler repoHandler = mock(RequestHandler.class);
        final ExplicitDJTypeHandler typeHandler =
                new ExplicitDJTypeHandler(resourcePath("/test"), repoHandler, json(object()), json(object(
                        field("testQuery", object(
                                field("_queryFilter", "true"),
                                field("_fields", "field1,field2"),
                                field("_sortKeys", "-foo,+bar")
                        ))
                )), json(object()));

        // when
        typeHandler.handleQuery(mock(Context.class),
                newQueryRequest("/test").setQueryId("testQuery"), mock(QueryResourceHandler.class));

        // then
        final ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(repoHandler).handleQuery(any(Context.class), captor.capture(), any(QueryResourceHandler.class));
        final QueryRequest request = captor.getValue();

        assertThat(request.getQueryId()).isNull();
        assertThat(request.getQueryFilter().toString()).isEqualTo("true");
        assertThat(request.getFields()).contains(new JsonPointer("field1"), new JsonPointer("field2"));
        // TODO waiting for https://stash.forgerock.org/projects/COMMONS/repos/forgerock-commons/pull-requests/323/diff
        assertThat(request.getSortKeys().get(0).toString()).isEqualTo("-/foo");
        assertThat(request.getSortKeys().get(1).toString()).isEqualTo("+/bar");
        //assertThat(request.getSortKeys()).containsExactly(descendingOrder("foo"), ascendingOrder("bar"));
    }

    /*
     * TODO - these are integration tests. Need unit tests on UniqueAttributeResolver directly
     */

//    @Test
//    public void shouldCreateUniqueResource() {
//        // given
//        final RequestHandler repoHandler = mock(RequestHandler.class);
//        final ExplicitDJTypeHandler typeHandler = createTypeHandler(repoHandler, SINGLE_UNIQUE_CONSTRAINT_CONFIG);
//
//        when(repoHandler.handleCreate(any(Context.class), any(CreateRequest.class))).thenReturn(EMPTY_PROMISE);
//
//        // when
//        when(repoHandler.handleQuery(any(Context.class), any(QueryRequest.class), any(QueryResourceHandler.class)))
//                .thenReturn(mock(QueryResponse.class).asPromise());
//        final JsonValue resourceContent = json(object(
//                field("uniqueField1", "uniqueValue"),
//                field("uniqueField2", "uniqueValue1")
//        ));
//        final Promise<ResourceResponse, ResourceException> result = createResource(typeHandler, resourceContent);
//
//        // then
//        assertThat(result).succeeded().isNotNull();
//    }

    @Test
    public void shouldNotCreateNonUniqueResource() {
        // given
        final Router repoHandler = new Router();
        repoHandler.addRoute(Router.uriTemplate("testResource"), new MemoryBackend());
        final ExplicitDJTypeHandler typeHandler = createTypeHandler(repoHandler, SINGLE_UNIQUE_CONSTRAINT_CONFIG);

        // when
        final JsonValue resourceContent = json(object(
                field("uniqueField1", "uniqueValue"),
                field("uniqueField2", "uniqueValue")
        ));
        // create initial entry
        Promise<ResourceResponse, ResourceException> result = createResource(typeHandler, resourceContent);
        assertThat(result).succeeded().isNotNull();

        // attempt to create entry with same content
        result = createResource(typeHandler, resourceContent);

        // then
        assertThat(result).failedWithException().isInstanceOf(ConflictException.class);
    }

//    @Test
//    public void shouldCreateUniqueResourceWithMultipleUniqueConstraints() {
//        // given
//        final RequestHandler repoHandler = mock(RequestHandler.class);
//        final ExplicitDJTypeHandler typeHandler = createTypeHandler(repoHandler, MULTIPLE_UNIQUE_CONSTRAINT_CONFIG);
//
//        when(repoHandler.handleCreate(any(Context.class), any(CreateRequest.class))).thenReturn(EMPTY_PROMISE);
//
//        // when
//        final JsonValue resourceContent = json(object(
//                field("uniqueField2", "uniqueValue"),
//                field("uniqueField3", "uniqueValue")
//        ));
//        final Promise<ResourceResponse, ResourceException> result = createResource(typeHandler, resourceContent);
//
//        // then
//        verify(repoHandler).handleCreate(any(Context.class), any(CreateRequest.class));
//        assertThat(result).succeeded().isNotNull();
//    }

    @Test
    public void shouldNotCreateNonUniqueResourceWithMultipleUniqueConstraints() {
        // given
        final Router repoHandler = new Router();
        repoHandler.addRoute(Router.uriTemplate("testResource"), new MemoryBackend());
        final ExplicitDJTypeHandler typeHandler = createTypeHandler(repoHandler, MULTIPLE_UNIQUE_CONSTRAINT_CONFIG);

        // when
        JsonValue resourceContent = json(object(
                field("uniqueField2", "uniqueValue"),
                field("uniqueField3", "uniqueValue")
        ));
        // create initial entry
        Promise<ResourceResponse, ResourceException> result = createResource(typeHandler, resourceContent);
        assertThat(result).succeeded().isNotNull();

        // attempt to create entry with same content using the first unique constraint
        result = createResource(typeHandler, resourceContent);
        assertThat(result).failedWithException().isInstanceOf(ConflictException.class);

        resourceContent = json(object(
                field("uniqueField1", "uniqueValue"),
                field("uniqueField2", "uniqueValue")
        ));
        // create initial entry
        createResource(typeHandler, resourceContent);

        // attempt to create entry with same content using the second unique constraint
        result = createResource(typeHandler, resourceContent);
        assertThat(result).failedWithException().isInstanceOf(ConflictException.class);
    }

    @Test
    public void shouldCreateResourceWhenUniqueSettingsAreNotSet() {
        // given
        final RequestHandler repoHandler = mock(RequestHandler.class);
        final ExplicitDJTypeHandler typeHandler = createTypeHandler(repoHandler, EMPTY_CONFIG);

        when(repoHandler.handleCreate(any(Context.class), any(CreateRequest.class))).thenReturn(EMPTY_PROMISE);

        // when
        final JsonValue resourceContent = json(object(
                field("uniqueField1", "uniqueValue"),
                field("uniqueField2", "uniqueValue")
        ));
        final Promise<ResourceResponse, ResourceException> result = createResource(typeHandler, resourceContent);

        // then
        verify(repoHandler).handleCreate(any(Context.class), any(CreateRequest.class));
        assertThat(result).succeeded().isNotNull();
    }

    private Promise<ResourceResponse, ResourceException> createResource(final ExplicitDJTypeHandler typeHandler,
            final JsonValue resourceContent) {
        final CreateRequest createRequest = Requests.newCreateRequest("testResource", resourceContent);
        // create initial entry
        final Promise<ResourceResponse, ResourceException> result =
                typeHandler.handleCreate(new RootContext(), createRequest);
        return result;
    }

    private ExplicitDJTypeHandler createTypeHandler(final RequestHandler repoHandler, final JsonValue config) {
        return new ExplicitDJTypeHandler(
                resourcePath("testResource"),
                repoHandler,
                config,
                json(object()),
                json(object()));
    }
}
