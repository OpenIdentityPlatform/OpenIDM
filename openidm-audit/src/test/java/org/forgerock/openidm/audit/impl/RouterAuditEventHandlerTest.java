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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openidm.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_REVISION;
import static org.forgerock.openidm.audit.impl.RouterAuditEventHandler.EVENT_ID;
import static org.forgerock.util.query.QueryFilter.*;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.audit.events.EventTopicsMetaDataBuilder;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RouterAuditEventHandlerTest {

    public static final String TEST_ID = "testId";
    public static final String AUDIT_DB_PATH = "audit/db/";
    public static final String ACCESS = "access";
    private RouterAuditEventHandler routerAuditEventHandler;
    private MemoryBackend memoryBackend;

    @BeforeMethod
    public void setUp() throws ResourceException {
        Router router = new Router();
        memoryBackend = new MemoryBackend();
        router.addRoute(Router.uriTemplate(AUDIT_DB_PATH + ACCESS), memoryBackend);

        final RouterAuditEventHandlerConfiguration config = new RouterAuditEventHandlerConfiguration();
        config.setResourcePath("audit/db");
        config.setName("router");
        config.setTopics(Collections.singleton(ACCESS));
        routerAuditEventHandler = new RouterAuditEventHandler(
                config, EventTopicsMetaDataBuilder.coreTopicSchemas().build(),
                Resources.newInternalConnectionFactory(router));
    }

    @Test
    public void testReadEntry() throws Exception {
        // given
        final JsonValue content = json(object(
                field("somedata", "foo"),
                field(FIELD_CONTENT_ID, TEST_ID)
        ));
        Promise<ResourceResponse, ResourceException> createPromise =
                routerAuditEventHandler.publishEvent(new RootContext(), ACCESS, content);
        assertThat(createPromise).succeeded();
        ResourceResponse createResponse = createPromise.getOrThrow();

        // when
        Promise<ResourceResponse, ResourceException> promise =
                routerAuditEventHandler.readEvent(new RootContext(), ACCESS, TEST_ID);

        // then
        assertThat(promise).succeeded();
        ResourceResponse readResponse = promise.getOrThrow();
        assertThat(readResponse.getId()).isEqualTo(TEST_ID);
        assertThat(readResponse.getContent().get(FIELD_CONTENT_ID).required().asString())
                .isEqualTo(TEST_ID);
        assertThat(readResponse.getId()).isEqualTo(createResponse.getId());
        readResponse.getContent().remove(FIELD_CONTENT_REVISION);
        createResponse.getContent().remove(FIELD_CONTENT_REVISION);
        assertThat(readResponse.getContent().isEqualTo(createResponse.getContent())).isTrue();
    }

    @Test
    public void testQueryEntries() throws Exception {
        // given
        final JsonValue content = json(object(
                field("somedata", "foo"),
                field(FIELD_CONTENT_ID, TEST_ID)
        ));
        Promise<ResourceResponse, ResourceException> createPromise =
                routerAuditEventHandler.publishEvent(new RootContext(), ACCESS, content);
        assertThat(createPromise).succeeded();
        createPromise.getOrThrow();

        // when
        QueryFilter<JsonPointer> queryFilter =
                and(
                        equalTo(new JsonPointer(FIELD_CONTENT_ID), TEST_ID),
                        startsWith(new JsonPointer("somedata"), "fo")
                );
        QueryRequest queryRequest = newQueryRequest(ACCESS).setQueryFilter(queryFilter);
        final List<ResourceResponse> responses = new ArrayList<>();
        Promise<QueryResponse, ResourceException> promise =
                routerAuditEventHandler.queryEvents(new RootContext(), ACCESS, queryRequest,
                        new QueryResourceHandler() {
                            @Override
                            public boolean handleResource(ResourceResponse resource) {
                                responses.add(resource);
                                return true;
                            }
                        });
        // then
        assertThat(promise).succeeded();
        promise.getOrThrow();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(TEST_ID);
        assertThat(responses.get(0).getContent().get(FIELD_CONTENT_ID).required().asString())
                .isEqualTo(TEST_ID);
    }

    @Test
    public void testCreateEntry() throws Exception {
        // given
        final JsonValue content = json(object(
                field("somedata", "foo"),
                field(FIELD_CONTENT_ID, TEST_ID)
        ));

        // when
        Promise<ResourceResponse, ResourceException> promise =
                routerAuditEventHandler.publishEvent(new RootContext(), ACCESS, content);

        // then
        assertThat(promise).succeeded();
        ResourceResponse resource = promise.getOrThrow();
        assertThat(resource).isNotNull();
        assertThat(resource.getContent().isEqualTo(content.put(FIELD_CONTENT_REVISION, "0"))).isTrue();

        QueryRequest queryRequest = newQueryRequest(new ResourcePath(AUDIT_DB_PATH + ACCESS));
        queryRequest.setQueryFilter(equalTo(new JsonPointer(EVENT_ID), TEST_ID));

        final List<JsonValue> contentRecords = new ArrayList<>();
        Promise<QueryResponse, ResourceException> queryPromise = memoryBackend.queryCollection(
                new RootContext(), queryRequest, new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(ResourceResponse resource) {
                        JsonValue resourceContent = resource.getContent();
                        contentRecords.add(resourceContent);
                        return true;
                    }
                });
        assertThat(queryPromise).succeeded();
        queryPromise.getOrThrow();
        assertThat(contentRecords).hasSize(1);
        JsonValue dbContent = contentRecords.get(0);
        assertThat(dbContent.get(EVENT_ID).required().asString()).isEqualTo(TEST_ID);
        assertThat(content.get("somedata").asString().equals(dbContent.get("somedata").asString())).isTrue();
    }

}