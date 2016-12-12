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

package org.forgerock.openidm.managed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.routing.UriRouterContext.uriRouterContext;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_FIELD_FIRST_ID;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_FIELD_FIRST_PROPERTY_NAME;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_FIELD_SECOND_ID;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_FIELD_SECOND_PROPERTY_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class RelationshipProviderTest {
    private static final String MANAGED_OBJECT_ID = "bobo";
    private static final String SCHEMA_FIELD_NAME = "manager";
    private ManagedObjectSetService managedObjectSyncService;
    private IDMConnectionFactory connectionFactory;
    private ActivityLogger activityLogger;
    private SchemaField schemaField;
    private Context context;

    @BeforeTest
    public void setup() throws Exception {
        activityLogger = mock(ActivityLogger.class);
        managedObjectSyncService = mock(ManagedObjectSetService.class);
        connectionFactory = mock(IDMConnectionFactory.class);
        schemaField = mock(SchemaField.class);
        when(schemaField.getName()).thenReturn(SCHEMA_FIELD_NAME);
        context = uriRouterContext(uriRouterContext(new RootContext()).build())
                .templateVariable("managedObjectId", MANAGED_OBJECT_ID).build();

    }

    @Test
    public void testCorrectContainerOwnership() throws NotFoundException {
        final RelationshipValidator relationshipValidator = mock(RelationshipValidator.class);
        final CollectionRelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed", "user"), schemaField, activityLogger, managedObjectSyncService, relationshipValidator);
        relationshipProvider.validateThatContainerOwnsRelationship(context, resourceResponse(MANAGED_OBJECT_ID), "irrelevant");
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testIncorrectContainerOwnership() throws NotFoundException {
        final RelationshipValidator relationshipValidator = mock(RelationshipValidator.class);
        final CollectionRelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed", "user"), schemaField, activityLogger, managedObjectSyncService, relationshipValidator);
        relationshipProvider.validateThatContainerOwnsRelationship(context, resourceResponse(MANAGED_OBJECT_ID + "error"), "irrelevant");
    }

    ResourceResponse resourceResponse(final String managedObjectId) {
        final ResourceResponse resourceResponse = mock(ResourceResponse.class);
        when(resourceResponse.getContent()).thenReturn(relationshipState(managedObjectId));
        return resourceResponse;
    }

    JsonValue relationshipState(String managedObjectId) {
        return json(object(
                field(REPO_FIELD_FIRST_ID, "managed/user/" + managedObjectId),
                field(REPO_FIELD_FIRST_PROPERTY_NAME, SCHEMA_FIELD_NAME),
                field(REPO_FIELD_SECOND_ID, "irrelevant"),
                field(REPO_FIELD_SECOND_PROPERTY_NAME, "irrelevant")));
    }

    @Test
    public void testReadResponseFieldsInExpandFieldsNoReferencedAdditions() throws ResourceException {
        final RelationshipValidator relationshipValidator = mock(RelationshipValidator.class);
        final RelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed", "user"), schemaField, activityLogger, managedObjectSyncService, relationshipValidator);
        final Connection mockConnection = mock(Connection.class);
        when(connectionFactory.getConnection()).thenReturn(mockConnection);
        final Context mockContext = mock(Context.class);
        final Request request = Requests.newReadRequest("managed/user/irrelevant");
        request.addField(SchemaField.FIELD_REFERENCE.toString(), SchemaField.FIELD_PROPERTIES.toString());
        final ResourceResponse response = Responses.newResourceResponse("id", "1", json(object()));
        relationshipProvider.expandFields(mockContext, request, response);
        assertThat(response.getFields()).isEmpty();
    }

    @Test
    public void testReadResponseFieldsInExpandFieldsDefaults() throws ResourceException {
        final RelationshipValidator relationshipValidator = mock(RelationshipValidator.class);
        final RelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed", "user"), schemaField, activityLogger, managedObjectSyncService, relationshipValidator);
        final Connection mockConnection = mock(Connection.class);
        when(connectionFactory.getConnection()).thenReturn(mockConnection);
        final Context mockContext = mock(Context.class);
        final Request request = Requests.newReadRequest("managed/user/irrelevant");
        final ResourceResponse response = Responses.newResourceResponse("id", "1", json(object()));
        relationshipProvider.expandFields(mockContext, request, response);
        assertThat(response.getFields()).isEmpty();
    }

    @Test
    public void testReadResponseFieldsInExpandFieldsReferencedWildcard() throws ResourceException {
        final RelationshipValidator relationshipValidator = mock(RelationshipValidator.class);
        final RelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed", "user"), schemaField, activityLogger, managedObjectSyncService, relationshipValidator);
        final Connection mockConnection = mock(Connection.class);
        when(connectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.read(any(Context.class), any(ReadRequest.class))).thenReturn(Responses.newResourceResponse("id", "1", json(object())));
        final Context mockContext = mock(Context.class);
        final Request request = Requests.newReadRequest("managed/user/irrelevant");
        request.addField(SchemaField.FIELD_ALL_RELATIONSHIPS.toString());
        final ResourceResponse response = Responses.newResourceResponse("id", "1",
                json(object(field("_ref", "managed/user/irrelevant"))));
        relationshipProvider.expandFields(mockContext, request, response);
        assertThat(response.getFields()).contains(new JsonPointer(""));
    }

    @Test
    public void testReadResponseFieldsInExpandFieldsReferencedWildcard2() throws ResourceException {
        final RelationshipValidator relationshipValidator = mock(RelationshipValidator.class);
        final RelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed", "user"), schemaField, activityLogger, managedObjectSyncService, relationshipValidator);
        final Connection mockConnection = mock(Connection.class);
        when(connectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.read(any(Context.class), any(ReadRequest.class))).thenReturn(Responses.newResourceResponse("id", "1", json(object())));
        final Context mockContext = mock(Context.class);
        final Request request = Requests.newReadRequest("managed/user/irrelevant");
        request.addField(SchemaField.FIELD_ALL.toString());
        final ResourceResponse response = Responses.newResourceResponse("id", "1",
                json(object(field("_ref", "managed/user/irrelevant"))));
        relationshipProvider.expandFields(mockContext, request, response);
        assertThat(response.getFields()).contains(new JsonPointer(""));
    }
}
