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

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.openidm.router.RouteEntry;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.Test;

public class AbstractDJTypeHandlerTest {

    private static final Promise<ResourceResponse, ResourceException> EMPTY_PROMISE =
            Responses.newResourceResponse("newObject", null, json(object())).asPromise();

    private static final JsonValue SINGLE_UNIQUE_CONSTRAINT_CONFIG =
            json(object(field(AbstractDJTypeHandler.UNIQUE_CONSTRAINTS, array(array("uniqueField1", "uniqueField2")))));
    private static final JsonValue MULTIPLE_UNIQUE_CONSTRAINT_CONFIG =
            json(object(field(AbstractDJTypeHandler.UNIQUE_CONSTRAINTS, array(
                    array("uniqueField1", "uniqueField2"), array("uniqueField2", "uniqueField3")))));
    private static final JsonValue EMPTY_CONFIG = json(object());

    /*
     * TODO - these are integration tests. Need unit tests on UniqueAttributeResolver directly
     */

//    @Test
//    public void shouldCreateUniqueResource() {
//        // given
//        final RequestHandler repoHandler = mock(RequestHandler.class);
//        final TestAbstractDJTypeHandler typeHandler = createTypeHandler(repoHandler, SINGLE_UNIQUE_CONSTRAINT_CONFIG);
//
//        when(repoHandler.handleCreate(any(Context.class), any(CreateRequest.class))).thenReturn(EMPTY_PROMISE);
//
//        // when
//        final JsonValue resourceContent = json(object(
//                field("uniqueField1", "uniqueValue"),
//                field("uniqueField2", "uniqueValue")
//        ));
//        final Promise<ResourceResponse, ResourceException> result = createResource(typeHandler, resourceContent);
//
//        // then
//        assertThat(result).succeeded().isNotNull();
//    }
//
//    @Test
//    public void shouldNotCreateNonUniqueResource() {
//        // given
//        final Router repoHandler = new Router();
//        repoHandler.addRoute(Router.uriTemplate("testResource"), new MemoryBackend());
//        final TestAbstractDJTypeHandler typeHandler = createTypeHandler(repoHandler, SINGLE_UNIQUE_CONSTRAINT_CONFIG);
//
//        // when
//        final JsonValue resourceContent = json(object(
//                field("uniqueField1", "uniqueValue"),
//                field("uniqueField2", "uniqueValue")
//        ));
//        // create initial entry
//        Promise<ResourceResponse, ResourceException> result = createResource(typeHandler, resourceContent);
//        assertThat(result).succeeded().isNotNull();
//
//        // attempt to create entry with same content
//        result = createResource(typeHandler, resourceContent);
//
//        // then
//        assertThat(result).failedWithException().isInstanceOf(ConflictException.class);
//    }
//
//    @Test
//    public void shouldCreateUniqueResourceWithMultipleUniqueConstraints() {
//        // given
//        final RequestHandler repoHandler = mock(RequestHandler.class);
//        final TestAbstractDJTypeHandler typeHandler = createTypeHandler(repoHandler, MULTIPLE_UNIQUE_CONSTRAINT_CONFIG);
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
//
//    @Test
//    public void shouldNotCreateNonUniqueResourceWithMultipleUniqueConstraints() {
//        // given
//        final Router repoHandler = new Router();
//        repoHandler.addRoute(Router.uriTemplate("testResource"), new MemoryBackend());
//        final TestAbstractDJTypeHandler typeHandler = createTypeHandler(repoHandler, MULTIPLE_UNIQUE_CONSTRAINT_CONFIG);
//
//        // when
//        JsonValue resourceContent = json(object(
//                field("uniqueField2", "uniqueValue"),
//                field("uniqueField3", "uniqueValue")
//        ));
//        // create initial entry
//        Promise<ResourceResponse, ResourceException> result = createResource(typeHandler, resourceContent);
//        assertThat(result).succeeded().isNotNull();
//
//        // attempt to create entry with same content using the first unique constraint
//        result = createResource(typeHandler, resourceContent);
//        assertThat(result).failedWithException().isInstanceOf(ConflictException.class);
//
//        resourceContent = json(object(
//                field("uniqueField1", "uniqueValue"),
//                field("uniqueField2", "uniqueValue")
//        ));
//        // create initial entry
//        createResource(typeHandler, resourceContent);
//
//        // attempt to create entry with same content using the second unique constraint
//        result = createResource(typeHandler, resourceContent);
//        assertThat(result).failedWithException().isInstanceOf(ConflictException.class);
//    }
//
//    @Test
//    public void shouldCreateResourceWhenUniqueSettingsAreNotSet() {
//        // given
//        final RequestHandler repoHandler = mock(RequestHandler.class);
//        final TestAbstractDJTypeHandler typeHandler = createTypeHandler(repoHandler, EMPTY_CONFIG);
//
//        when(repoHandler.handleCreate(any(Context.class), any(CreateRequest.class))).thenReturn(EMPTY_PROMISE);
//
//        // when
//        final JsonValue resourceContent = json(object(
//                field("uniqueField1", "uniqueValue"),
//                field("uniqueField2", "uniqueValue")
//        ));
//        final Promise<ResourceResponse, ResourceException> result = createResource(typeHandler, resourceContent);
//
//        // then
//        verify(repoHandler).handleCreate(any(Context.class), any(CreateRequest.class));
//        assertThat(result).succeeded().isNotNull();
//    }

    private Promise<ResourceResponse, ResourceException> createResource(final TestAbstractDJTypeHandler typeHandler,
            final JsonValue resourceContent) {
        final CreateRequest createRequest = Requests.newCreateRequest("testResource", resourceContent);
        // create initial entry
        final Promise<ResourceResponse, ResourceException> result =
                typeHandler.handleCreate(new RootContext(), createRequest);
        return result;
    }

    private TestAbstractDJTypeHandler createTypeHandler(final RequestHandler repoHandler, final JsonValue config) {
        return new TestAbstractDJTypeHandler(
                ResourcePath.resourcePath("testResource"),
                repoHandler,
                new TestRouteEntry(),
                config,
                json(object()),
                json(object()));
    }

    private static final class TestRouteEntry implements RouteEntry {
        @Override
        public boolean removeRoute() {
            return false;
        }
    }

    private static final class TestAbstractDJTypeHandler extends AbstractDJTypeHandler {

        public TestAbstractDJTypeHandler(final ResourcePath resourcePath, final RequestHandler repoHandler,
                final RouteEntry routeEntry, final JsonValue config, final JsonValue queries,
                final JsonValue commands) {
            super(resourcePath, repoHandler, config, queries, commands);
        }

        @Override
        protected JsonValue outputTransformer(JsonValue jsonValue) throws ResourceException {
            return jsonValue;
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
            return null;
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
            return null;
        }
    }

}
