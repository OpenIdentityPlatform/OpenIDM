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
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.util.query.QueryFilter.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.services.context.Context;
import org.forgerock.util.query.QueryFilter;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GenericDJTypeHandlerTest {

    private OpenDJRepoService repoService;

    @BeforeMethod
    public void setup() {
        this.repoService = mock(OpenDJRepoService.class);
    }

    @Test
    public void testPrefixesGenericQueryFilters() throws ResourceException {
        // given
        RequestHandler repoHandler = mock(RequestHandler.class);
        GenericDJTypeHandler handler =
                new GenericDJTypeHandler(new ResourcePath("/test"), repoHandler, json(object()), json(object()), json(object()));

        // when
        handler.handleQuery(mock(Context.class),
                newQueryRequest("/test")
                        .setQueryFilter(equalTo(new JsonPointer("field"), "val")),
                mock(QueryResourceHandler.class));

        // then
        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(repoHandler).handleQuery(any(Context.class), captor.capture(), any(QueryResourceHandler.class));
        assertThat(captor.getValue().getQueryFilter().toString()).contains("/fullobject/field eq \"val\"");
    }

    @Test
    public void testTransformsFullobjectInput() {
        // given
        RequestHandler repoHandler = mock(RequestHandler.class);

        JsonValue config = json(null);
        JsonValue queries = json(null);
        JsonValue commands = json(null);
        GenericDJTypeHandler handler = new GenericDJTypeHandler(new ResourcePath("/test"), repoHandler, config, queries, commands);

        // when

        // then
    }

    @Test
    public void testTransformsFullobjectOutput() {
        // given

        // when

        // then
    }
}
