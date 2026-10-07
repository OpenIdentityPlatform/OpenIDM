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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openidm.managed;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.util.RelationshipUtil;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Collections;

public class CollectionRelationshipProviderTest {
    private static final ResourcePath REFERRING_OBJECT_ID = new ResourcePath("managed/user/foo");
    private static final String VALID_DURATION = "2016-01-01T00:00:00.000Z/2016-01-02T00:00:00.000Z";
    private static final String REVERSED_DURATION = "2016-01-02T00:00:00.000Z/2016-01-01T00:00:00.000Z";
    private ManagedObjectSetService managedObjectSyncService;
    private ConnectionFactory connectionFactory;
    private ActivityLogger activityLogger;
    private final JsonValue userWithManager =
            json(
                    object(
                            field("mail", "test1@example.com"),
                            field("sn", "User"),
                            field("givenName", "Test"),
                            field("_id", "test1"),
                            field("password", "Password1"),
                            field("employeenumber", 100),
                            field("accountStatus", "active"),
                            field("telephoneNumber", ""),
                            field("roles", array()),
                            field("postalAddress", ""),
                            field("userName", "test1"),
                            field("stateProvince", ""),
                            field("authzRoles",
                                    array(
                                            object(
                                                    field("_ref", "repo/internal/role/openidm-authorized")
                                            )
                            )
                    )
                    ));
    private final JsonValue manager =
            json(
                    object(
                            field("mail", "mgr1@example.com"),
                            field("sn", "User"),
                            field("givenName", "Test"),
                            field("_id", "mgr1"),
                            field("password", "Password1"),
                            field("employeenumber", 100),
                            field("accountStatus", "active"),
                            field("telephoneNumber", ""),
                            field("roles", array()),
                            field("postalAddress", ""),
                            field("userName", "mgr1"),
                            field("stateProvince", ""),
                            field("authzRoles",
                                    array(
                                            object(
                                                    field("_ref", "repo/internal/role/openidm-authorized")
                                            )
                                    )
                            )
                    ));

    @BeforeTest
    public void setup() throws Exception {
        activityLogger = mock(ActivityLogger.class);
        managedObjectSyncService = mock(ManagedObjectSetService.class);
        connectionFactory = mock(ConnectionFactory.class);
    }

//    @Test
    public void testValidateFieldOnReverseRelationshipField() throws Exception {
        RootContext context = new RootContext();
        Connection connection = mock(Connection.class);

        // setup mock reads that validation will call
        JsonValue test1User = userWithManager.copy();
        when(connection.read(any(Context.class), argThat(new IsRouteMatcher("managed/user/test1"))))
                .thenReturn(newResourceResponse(test1User.get("_id").asString(), "1", test1User));
        JsonValue differentUser = test1User.copy();
        differentUser.put("_id", "differentUser");
        when(connection.read(any(Context.class), argThat(new IsRouteMatcher("managed/user/differentUser"))))
                .thenReturn(newResourceResponse(differentUser.get("_id").asString(), "1", differentUser));

        when(connectionFactory.getConnection()).thenReturn(connection);

        // setup our test to go against the reports/manager reverse relationship
        SchemaField schemaField = mock(SchemaField.class);
        when(schemaField.getName()).thenReturn("reports");
        when(schemaField.isReverseRelationship()).thenReturn(true);
        when(schemaField.getReversePropertyName()).thenReturn("manager");

        // create our provider that we will use to test.
        CollectionRelationshipProvider provider = new CollectionRelationshipProvider(connectionFactory,
                ResourcePath.resourcePath("managed/user"), schemaField, activityLogger, managedObjectSyncService);
        assertTrue(provider.relationshipValidator instanceof ReverseRelationshipValidator);

        // testing the condition where the original manager has 1 report, and are updating to 2 reports.
        // the existing report should not be validated.
        test1User.put("manager", object(field(RelationshipUtil.REFERENCE_ID, "managed/user/mgr1")));
        JsonValue mgrWith1Report = manager.copy();
        mgrWith1Report.put("reports", json(
                array(
                        object(field(RelationshipUtil.REFERENCE_ID, "managed/user/test1"))
                )));
        JsonValue mgrWith2Reports = manager.copy();
        mgrWith2Reports.put("reports", json(
                array(
                        object(field(RelationshipUtil.REFERENCE_ID, "managed/user/test1")),
                        object(field(RelationshipUtil.REFERENCE_ID, "managed/user/differentUser"))
                )));

        provider.validateRelationshipField(context, mgrWith1Report.get("reports"), mgrWith2Reports.get("reports"), REFERRING_OBJECT_ID, true);

        // testing the condition where a user already has a manager.
        try {
            differentUser.put("manager", object(field(RelationshipUtil.REFERENCE_ID, "managed/user/someOtherManager")));
            provider.validateRelationshipField(context, manager.get("reports"), mgrWith2Reports.get("reports"), REFERRING_OBJECT_ID, true);
            fail("expected to fail if the user already has a manager");
        } catch (DuplicateRelationshipException e) {
            // test passed.
        }
    }

    @Test(expectedExceptions = BadRequestException.class,
            expectedExceptionsMessageRegExp = "Temporal constraint duration " + REVERSED_DURATION + " .*")
    public void testCreateRejectsInvalidTemporalConstraint() throws Exception {
        final Connection connection = mock(Connection.class);
        when(connection.createAsync(any(Context.class), any(CreateRequest.class))).thenAnswer(invocation ->
                newResourceResponse("g1", "1", ((CreateRequest) invocation.getArguments()[1]).getContent()).asPromise());

        newRolesProvider(connection).createInstance(managedObjectContext(),
                Requests.newCreateRequest("", grant(null, REVERSED_DURATION))).getOrThrow();
    }

    @Test
    public void testUpdateKeepsUnchangedInvalidTemporalConstraint() throws Exception {
        // a managed object update persists every relationship it carries, including a stored invalid one
        final Connection connection = connectionWithStoredGrant(REVERSED_DURATION);
        final JsonValue grant = grant("g1", REVERSED_DURATION);
        grant.put(new JsonPointer("/_refProperties/_grantType"), "conditional");

        newRolesProvider(connection).updateInstance(managedObjectContext(), "g1",
                Requests.newUpdateRequest("", grant)).getOrThrow();

        verify(connection).updateAsync(any(Context.class), any(UpdateRequest.class));
    }

    @Test(expectedExceptions = BadRequestException.class,
            expectedExceptionsMessageRegExp = "Temporal constraint duration " + REVERSED_DURATION + " .*")
    public void testUpdateRejectsChangedInvalidTemporalConstraint() throws Exception {
        final Connection connection = connectionWithStoredGrant(VALID_DURATION);

        newRolesProvider(connection).updateInstance(managedObjectContext(), "g1",
                Requests.newUpdateRequest("", grant("g1", REVERSED_DURATION))).getOrThrow();
    }

    @Test
    public void testPatchRepairsInvalidTemporalConstraint() throws Exception {
        final Connection connection = connectionWithStoredGrant(REVERSED_DURATION);

        newRolesProvider(connection).patchInstance(managedObjectContext(), "g1",
                Requests.newPatchRequest("", PatchOperation.replace(
                        "/_refProperties/temporalConstraints/0/duration", VALID_DURATION))).getOrThrow();

        final ArgumentCaptor<UpdateRequest> update = ArgumentCaptor.forClass(UpdateRequest.class);
        verify(connection).updateAsync(any(Context.class), update.capture());
        assertEquals(update.getValue().getContent()
                .get(new JsonPointer("/properties/temporalConstraints/0/duration")).asString(), VALID_DURATION);
    }

    @Test(expectedExceptions = BadRequestException.class,
            expectedExceptionsMessageRegExp = "Temporal constraint duration " + REVERSED_DURATION + " .*")
    public void testPatchRejectsInvalidTemporalConstraint() throws Exception {
        final Connection connection = connectionWithStoredGrant(VALID_DURATION);

        newRolesProvider(connection).patchInstance(managedObjectContext(), "g1",
                Requests.newPatchRequest("", PatchOperation.replace(
                        "/_refProperties/temporalConstraints/0/duration", REVERSED_DURATION))).getOrThrow();
    }

    private CollectionRelationshipProvider newRolesProvider(final Connection connection) throws Exception {
        final ConnectionFactory factory = mock(ConnectionFactory.class);
        when(factory.getConnection()).thenReturn(connection);
        final SchemaField schemaField = mock(SchemaField.class);
        when(schemaField.isReverseRelationship()).thenReturn(false);
        when(schemaField.getName()).thenReturn("roles");
        return new CollectionRelationshipProvider(factory, ResourcePath.resourcePath("managed/user"), schemaField,
                activityLogger, managedObjectSyncService);
    }

    /** The context of a relationship request made by the managed object user/u1. */
    private static Context managedObjectContext() {
        return new ManagedObjectContext(new UriRouterContext(new RootContext(), "", "",
                Collections.singletonMap(RelationshipProvider.PARAM_MANAGED_OBJECT_ID, "u1")));
    }

    /** A connection whose repository holds the grant g1 of role r1 to user u1, and which accepts any update. */
    private static Connection connectionWithStoredGrant(final String duration) throws Exception {
        final Connection connection = mock(Connection.class);
        when(connection.readAsync(any(Context.class), any(ReadRequest.class))).thenAnswer(invocation ->
                newResourceResponse("g1", "1", json(object(
                        field(RelationshipProvider.REPO_FIELD_FIRST_ID, "managed/user/u1"),
                        field(RelationshipProvider.REPO_FIELD_FIRST_PROPERTY_NAME, "roles"),
                        field(RelationshipProvider.REPO_FIELD_SECOND_ID, "managed/role/r1"),
                        field(RelationshipProvider.REPO_FIELD_SECOND_PROPERTY_NAME, null),
                        field(RelationshipProvider.REPO_FIELD_PROPERTIES, object(
                                field(RelationshipValidator.TEMPORAL_CONSTRAINTS,
                                        array(object(field(RelationshipValidator.DURATION, duration))))))))
                ).asPromise());
        when(connection.updateAsync(any(Context.class), any(UpdateRequest.class))).thenAnswer(invocation ->
                newResourceResponse("g1", "2", ((UpdateRequest) invocation.getArguments()[1]).getContent()).asPromise());
        return connection;
    }

    /** A grant of role r1 with a temporal constraint, as a relationship request carries it. */
    private static JsonValue grant(final String id, final String duration) {
        final JsonValue refProperties = json(object(field(RelationshipValidator.TEMPORAL_CONSTRAINTS,
                array(object(field(RelationshipValidator.DURATION, duration))))));
        if (id != null) {
            refProperties.put("_id", id);
            refProperties.put("_rev", "1");
        }
        return json(object(
                field(RelationshipUtil.REFERENCE_ID, "managed/role/r1"),
                field(RelationshipUtil.REFERENCE_PROPERTIES, refProperties.getObject())));
    }

    private static class IsRouteMatcher extends ArgumentMatcher<ReadRequest> {
        private final String route;

        public IsRouteMatcher(String route) {
            this.route = route;
        }

        @Override
        public boolean matches(Object requestToMatch) {
            return (null != requestToMatch && ((ReadRequest) requestToMatch).getResourcePath().startsWith(route));
        }
    }

}
