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
 * Copyright 2016-2017 ForgeRock AS.
 */
package org.forgerock.openidm.provisioner.openicf.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openidm.provisioner.openicf.impl.OpenICFProvisionerService.CAUD_TRANSACTION_ID;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.identityconnectors.framework.common.objects.ObjectClass.GROUP_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openidm.audit.util.NullActivityLogger;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper;
import org.forgerock.services.TransactionId;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.context.TransactionIdContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.AuthenticationApiOp;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.api.operations.DeleteApiOp;
import org.identityconnectors.framework.api.operations.GetApiOp;
import org.identityconnectors.framework.api.operations.SearchApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

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

        final ObjectClassResourceProvider resourceProvider =
                createObjectClassResourceProvider(connectorFacade, jsonConfiguration, objectClassInfoHelper);

        final Map<String, List<String>> httpHeaders = new HashMap<>();

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

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatePopulatesTransactionId() throws Exception {
        // given
        final JsonValue jsonConfiguration = readConfig("/config/provisioner.openicf-xml.json");
        final ObjectClassInfoHelper objectClassInfoHelper = mock(ObjectClassInfoHelper.class);

        final ConnectorFacade connectorFacade = mock(ConnectorFacade.class);
        when(connectorFacade.getOperation(any(APIOperation.class.getClass()))).thenReturn(mock(CreateApiOp.class));
        when(connectorFacade.create(any(ObjectClass.class), any(Set.class), any(OperationOptions.class)))
                .thenReturn(new Uid("newUid"));

        final ObjectClassResourceProvider resourceProvider =
                createObjectClassResourceProvider(connectorFacade, jsonConfiguration, objectClassInfoHelper);
        final TransactionIdContext context = new TransactionIdContext(new RootContext(), new TransactionId());

        // when
        final Promise<ResourceResponse, ResourceException> result = resourceProvider.handleCreate(
                context,
                Requests.newCreateRequest("", json(object())));

        final ArgumentCaptor<OperationOptions> argumentCaptor = ArgumentCaptor.forClass(OperationOptions.class);
        verify(connectorFacade).create(any(ObjectClass.class), any(Set.class), argumentCaptor.capture());

        // then
        assertThat(result).isNotNull().succeeded();
        assertThat(argumentCaptor.getValue().getOptions())
                .isNotNull()
                .contains(new AbstractMap.SimpleEntry<>(CAUD_TRANSACTION_ID, context.getTransactionId().getValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadPopulatesTransactionId() throws Exception {
        // given
        final JsonValue jsonConfiguration = readConfig("/config/provisioner.openicf-xml.json");
        final ObjectClassInfoHelper objectClassInfoHelper = mock(ObjectClassInfoHelper.class);
        when(objectClassInfoHelper.getFullResourceId(any(Request.class))).thenReturn("someId");
        when(objectClassInfoHelper.build(any(ConnectorObject.class), any(CryptoService.class)))
                .thenReturn(Responses.newResourceResponse("someId", null, json(object())));

        final ConnectorFacade connectorFacade = mock(ConnectorFacade.class);
        when(connectorFacade.getOperation(any(APIOperation.class.getClass()))).thenReturn(mock(GetApiOp.class));
        when(connectorFacade.getObject(any(ObjectClass.class), any(Uid.class), any(OperationOptions.class)))
                .thenReturn(createConnectorObject());

        final ObjectClassResourceProvider resourceProvider =
                createObjectClassResourceProvider(connectorFacade, jsonConfiguration, objectClassInfoHelper);
        final TransactionIdContext context = new TransactionIdContext(new RootContext(), new TransactionId());

        // when
        final Promise<ResourceResponse, ResourceException> result = resourceProvider.handleRead(
                context,
                Requests.newReadRequest("", "someId"));

        final ArgumentCaptor<OperationOptions> argumentCaptor = ArgumentCaptor.forClass(OperationOptions.class);
        verify(connectorFacade).getObject(any(ObjectClass.class), any(Uid.class), argumentCaptor.capture());

        // then
        assertThat(result).isNotNull().succeeded();
        assertThat(argumentCaptor.getValue().getOptions())
                .isNotNull()
                .contains(new AbstractMap.SimpleEntry<>(CAUD_TRANSACTION_ID, context.getTransactionId().getValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdatePopulatesTransactionId() throws Exception {
        // given
        final JsonValue jsonConfiguration = readConfig("/config/provisioner.openicf-xml.json");
        final ObjectClassInfoHelper objectClassInfoHelper = mock(ObjectClassInfoHelper.class);
        when(objectClassInfoHelper.getFullResourceId(any(Request.class))).thenReturn("someId");
        when(objectClassInfoHelper.build(any(ConnectorObject.class), any(CryptoService.class)))
                .thenReturn(Responses.newResourceResponse("someId", null, json(object())));

        final ConnectorFacade connectorFacade = mock(ConnectorFacade.class);
        when(connectorFacade.getOperation(any(APIOperation.class.getClass()))).thenReturn(mock(UpdateApiOp.class));
        when(connectorFacade.update(
                any(ObjectClass.class), any(Uid.class), any(Set.class), any(OperationOptions.class)))
                .thenReturn(new Uid("someId"));

        final ObjectClassResourceProvider resourceProvider =
                createObjectClassResourceProvider(connectorFacade, jsonConfiguration, objectClassInfoHelper);
        final TransactionIdContext context = new TransactionIdContext(new RootContext(), new TransactionId());

        // when
        final Promise<ResourceResponse, ResourceException> result = resourceProvider.handleUpdate(
                context,
                Requests.newUpdateRequest("", "someId", json(object())));

        final ArgumentCaptor<OperationOptions> argumentCaptor = ArgumentCaptor.forClass(OperationOptions.class);
        verify(connectorFacade)
                .update(any(ObjectClass.class), any(Uid.class), any(Set.class), argumentCaptor.capture());

        // then
        assertThat(result).isNotNull().succeeded();
        assertThat(argumentCaptor.getValue().getOptions())
                .isNotNull()
                .contains(new AbstractMap.SimpleEntry<>(CAUD_TRANSACTION_ID, context.getTransactionId().getValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeletePopulatesTransactionId() throws Exception {
        // given
        final JsonValue jsonConfiguration = readConfig("/config/provisioner.openicf-xml.json");
        final ObjectClassInfoHelper objectClassInfoHelper = mock(ObjectClassInfoHelper.class);
        when(objectClassInfoHelper.getFullResourceId(any(Request.class))).thenReturn("someId");
        when(objectClassInfoHelper.build(any(ConnectorObject.class), any(CryptoService.class)))
                .thenReturn(Responses.newResourceResponse("someId", null, json(object())));

        final ConnectorFacade connectorFacade = mock(ConnectorFacade.class);
        when(connectorFacade.getOperation(any(APIOperation.class.getClass()))).thenReturn(mock(DeleteApiOp.class));
        doNothing().when(connectorFacade).delete(any(ObjectClass.class), any(Uid.class), any(OperationOptions.class));

        final ObjectClassResourceProvider resourceProvider =
                createObjectClassResourceProvider(connectorFacade, jsonConfiguration, objectClassInfoHelper);
        final TransactionIdContext context = new TransactionIdContext(new RootContext(), new TransactionId());

        // when
        final Promise<ResourceResponse, ResourceException> result = resourceProvider.handleDelete(
                context,
                Requests.newDeleteRequest("", "someId"));

        final ArgumentCaptor<OperationOptions> argumentCaptor = ArgumentCaptor.forClass(OperationOptions.class);
        verify(connectorFacade).delete(any(ObjectClass.class), any(Uid.class), argumentCaptor.capture());

        // then
        assertThat(result).isNotNull().succeeded();
        assertThat(argumentCaptor.getValue().getOptions())
                .isNotNull()
                .contains(new AbstractMap.SimpleEntry<>(CAUD_TRANSACTION_ID, context.getTransactionId().getValue()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPatchPopulatesTransactionId() throws Exception {
        // given
        final JsonValue jsonConfiguration = readConfig("/config/provisioner.openicf-xml.json");
        final ObjectClassInfoHelper objectClassInfoHelper = mock(ObjectClassInfoHelper.class);
        when(objectClassInfoHelper.getFullResourceId(any(Request.class))).thenReturn("someId");
        when(objectClassInfoHelper.build(any(ConnectorObject.class), any(CryptoService.class)))
                .thenReturn(Responses.newResourceResponse("someId", null, json(object())));
        when(objectClassInfoHelper.getPatchAttribute(any(PatchOperation.class), any(JsonValue.class),
                any(CryptoService.class))).thenReturn(AttributeBuilder.build("someField", "someValue"));

        final ConnectorFacade connectorFacade = mock(ConnectorFacade.class);
        when(connectorFacade.getOperation(any(APIOperation.class.getClass()))).thenReturn(mock(UpdateApiOp.class));
        when(connectorFacade.update(
                any(ObjectClass.class), any(Uid.class), any(Set.class), any(OperationOptions.class)))
                .thenReturn(new Uid("someId"));

        final ObjectClassResourceProvider resourceProvider =
                createObjectClassResourceProvider(connectorFacade, jsonConfiguration, objectClassInfoHelper);
        final TransactionIdContext context = new TransactionIdContext(new RootContext(), new TransactionId());

        // when
        final Promise<ResourceResponse, ResourceException> result = resourceProvider.handlePatch(
                context,
                Requests.newPatchRequest("", "someId", PatchOperation.add("someField", "newValue")));

        final ArgumentCaptor<OperationOptions> argumentCaptor = ArgumentCaptor.forClass(OperationOptions.class);
        verify(connectorFacade)
                .update(any(ObjectClass.class), any(Uid.class), any(Set.class), argumentCaptor.capture());

        // then
        assertThat(result).isNotNull().succeeded();
        assertThat(argumentCaptor.getValue().getOptions())
                .isNotNull()
                .contains(new AbstractMap.SimpleEntry<>(CAUD_TRANSACTION_ID, context.getTransactionId().getValue()));
    }

    @Test
    @SuppressWarnings({"unchecked"})
    public void testActionPopulatesTransactionId() throws Exception {
        // given
        final JsonValue jsonConfiguration = readConfig("/config/provisioner.openicf-xml.json");
        final ObjectClassInfoHelper objectClassInfoHelper = mock(ObjectClassInfoHelper.class);
        when(objectClassInfoHelper.getFullResourceId(any(Request.class))).thenReturn("");
        when(objectClassInfoHelper.build(any(ConnectorObject.class), any(CryptoService.class)))
                .thenReturn(Responses.newResourceResponse("someId", null, json(object())));
        when(objectClassInfoHelper.getPatchAttribute(any(PatchOperation.class), any(JsonValue.class),
                any(CryptoService.class))).thenReturn(AttributeBuilder.build("someField", "someValue"));

        final ConnectorFacade connectorFacade = mock(ConnectorFacade.class);
        when(connectorFacade.getOperation(any(APIOperation.class.getClass())))
                .thenReturn(mock(AuthenticationApiOp.class));
        when(connectorFacade.authenticate(
                any(ObjectClass.class), anyString(), any(GuardedString.class), any(OperationOptions.class)))
                .thenReturn(new Uid("someUid"));

        final ObjectClassResourceProvider resourceProvider =
                createObjectClassResourceProvider(connectorFacade, jsonConfiguration, objectClassInfoHelper);
        final TransactionIdContext context = new TransactionIdContext(new RootContext(), new TransactionId());

        // when
        final Promise<ActionResponse, ResourceException> result = resourceProvider.handleAction(
                context,
                Requests.newActionRequest("", ObjectClassResourceProvider.ObjectClassAction.authenticate.name())
                        .setAdditionalParameter("username", "someUserName")
                        .setAdditionalParameter("password", "somePassword"));

        final ArgumentCaptor<OperationOptions> argumentCaptor = ArgumentCaptor.forClass(OperationOptions.class);
        verify(connectorFacade)
                .authenticate(any(ObjectClass.class), anyString(), any(GuardedString.class), argumentCaptor.capture());

        // then
        assertThat(result).isNotNull().succeeded();
        assertThat(argumentCaptor.getValue().getOptions())
                .isNotNull()
                .contains(new AbstractMap.SimpleEntry<>(CAUD_TRANSACTION_ID, context.getTransactionId().getValue()));
    }

    @Test
    @SuppressWarnings({"unchecked"})
    public void testQueryPopulatesTransactionId() throws Exception {
        // given
        final JsonValue jsonConfiguration = readConfig("/config/provisioner.openicf-xml.json");
        final ObjectClassInfoHelper objectClassInfoHelper = mock(ObjectClassInfoHelper.class);
        when(objectClassInfoHelper.getFullResourceId(any(Request.class))).thenReturn("");
        when(objectClassInfoHelper.build(any(ConnectorObject.class), any(CryptoService.class)))
                .thenReturn(Responses.newResourceResponse("someId", null, json(object())));
        when(objectClassInfoHelper.getPatchAttribute(any(PatchOperation.class), any(JsonValue.class),
                any(CryptoService.class))).thenReturn(AttributeBuilder.build("someField", "someValue"));

        final ConnectorFacade connectorFacade = mock(ConnectorFacade.class);
        when(connectorFacade.getOperation(any(APIOperation.class.getClass()))).thenReturn(mock(SearchApiOp.class));
        when(connectorFacade.search(
                any(ObjectClass.class), any(Filter.class), any(ResultsHandler.class), any(OperationOptions.class)))
                .thenReturn(new SearchResult());

        final ObjectClassResourceProvider resourceProvider =
                createObjectClassResourceProvider(connectorFacade, jsonConfiguration, objectClassInfoHelper);
        final TransactionIdContext context = new TransactionIdContext(new RootContext(), new TransactionId());

        // when
        final Promise<QueryResponse, ResourceException> result = resourceProvider.handleQuery(
                context,
                Requests.newQueryRequest("").setQueryFilter(QueryFilter.<JsonPointer>alwaysTrue()),
                new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(ResourceResponse resourceResponse) {
                        return true;
                    }
                });

        final ArgumentCaptor<OperationOptions> argumentCaptor = ArgumentCaptor.forClass(OperationOptions.class);
        verify(connectorFacade)
                .search(any(ObjectClass.class), any(Filter.class), any(ResultsHandler.class), argumentCaptor.capture());

        // then
        assertThat(result).isNotNull().succeeded();
        assertThat(argumentCaptor.getValue().getOptions())
                .isNotNull()
                .contains(new AbstractMap.SimpleEntry<>(CAUD_TRANSACTION_ID, context.getTransactionId().getValue()));
    }

    private ConnectorObject createConnectorObject() {
        final Set<Attribute> attributes = new HashSet<>();
        attributes.add(AttributeBuilder.build(Uid.NAME, "someUid"));
        attributes.add(AttributeBuilder.build(Name.NAME, "someUid"));
        return new ConnectorObject(new ObjectClass("objectClass"), attributes);
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

    private OpenICFProvisionerService createProvisionerService(final ConnectorFacade connectorFacade) {
        final OpenICFProvisionerService provisionerService = mock(OpenICFProvisionerService.class);
        when(provisionerService.getConnectorFacade()).thenReturn(connectorFacade);
        when(provisionerService.getActivityLogger()).thenReturn(new NullActivityLogger());
        return provisionerService;
    }

    private ObjectClassResourceProvider createObjectClassResourceProvider(final ConnectorFacade connectorFacade,
            final JsonValue jsonConfiguration, final ObjectClassInfoHelper objectClassInfoHelper) {
        final OpenICFProvisionerService provisionerService = createProvisionerService(connectorFacade);
        final Map<String, Map<Class<? extends APIOperation>, OperationOptionInfoHelper>> objectOperations =
                ConnectorUtil.getOperationOptionConfiguration(jsonConfiguration);
        final Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations =
                objectOperations.get(GROUP_NAME);
        return new ObjectClassResourceProvider(GROUP_NAME, objectClassInfoHelper, operations, provisionerService,
                jsonConfiguration);
    }

}
