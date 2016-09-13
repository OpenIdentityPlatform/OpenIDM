/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.provisioner.openicf.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.identityconnectors.framework.common.objects.ObjectClass.GROUP_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.testng.annotations.Test;

/**
 * Tests the {@link ObjectClassResourceProvider} class.
 */
public class ObjectClassResourceProviderTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Tests the blocked behavior of a create with a client provided id (ie PUT).
     *
     * @throws Exception on failure.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testUnsupportedCreateWithId() throws Exception {
        // given
        JsonValue jsonConfiguration = readConfig("/config/provisioner.openicf-xml.json");
        ObjectClassInfoHelper objectClassInfoHelper = mock(ObjectClassInfoHelper.class);

        ConnectorFacade connectorFacade = mock(ConnectorFacade.class);
        when(connectorFacade.getOperation(any(APIOperation.class.getClass())))
                .thenReturn(mock(CreateApiOp.class));
        when(connectorFacade.create(any(ObjectClass.class), any(Set.class), any(OperationOptions.class)))
                .thenReturn(new Uid("newUid"));

        OpenICFProvisionerService provisionerService = mock(OpenICFProvisionerService.class);
        when(provisionerService.getConnectorFacade()).thenReturn(connectorFacade);
        when(provisionerService.getActivityLogger()).thenReturn(mock(ActivityLogger.class));
        Map<String, Map<Class<? extends APIOperation>, OperationOptionInfoHelper>> objectOperations =
                ConnectorUtil.getOperationOptionConfiguration(jsonConfiguration);
        Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations = objectOperations.get(GROUP_NAME);
        ObjectClassResourceProvider resourceProvider = new ObjectClassResourceProvider(GROUP_NAME,
                objectClassInfoHelper, operations, provisionerService, jsonConfiguration);
        Map<String, List<String>> httpHeaders = new HashMap<>();

        // when: Upsert should fail as the create With id should fail
        Promise<ResourceResponse, ResourceException> upsertPromise = resourceProvider.handleCreate(
                        createHttpContext("PUT", httpHeaders),
                        Requests.newCreateRequest("", "fakeId", json(object())));
        // then
        assertThat(upsertPromise)
                .isNotNull()
                .failedWithException()
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("UPSERT");

        // when: try again but this time with the if-none-match header set (ie, still a http PUT)
        httpHeaders.put("if-none-match", Collections.singletonList("*"));
        upsertPromise = resourceProvider.handleCreate(
                createHttpContext("PUT", httpHeaders),
                Requests.newCreateRequest("", "fakeId", json(object())));
        // then
        assertThat(upsertPromise)
                .isNotNull()
                .failedWithException()
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Create with client provided ID");

        // when: a direct (ie no http context) Create with passed ID should also fail
        Promise<ResourceResponse, ResourceException> createPromise = resourceProvider.handleCreate(new RootContext(),
                Requests.newCreateRequest("", "fakeId", json(object())));
        // then
        assertThat(createPromise)
                .isNotNull()
                .failedWithException()
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Create with client provided ID");
    }

    private HttpContext createHttpContext(String method, Map<String, List<String>> httpHeaders) {
        return new HttpContext(
                json(
                        object(
                                field(HttpContext.ATTR_METHOD, method),
                                field(HttpContext.ATTR_PARAMETERS, object()),
                                field(HttpContext.ATTR_HEADERS, httpHeaders)
                        )
                ),
                ClassLoader.getSystemClassLoader());
    }

    private JsonValue readConfig(String testConfigFile) throws IOException {
        InputStream inputStream = ObjectClassResourceProviderTest.class.getResourceAsStream(testConfigFile);
        assertThat(inputStream).isNotNull();
        return json(mapper.readValue(inputStream, Map.class));
    }

}
