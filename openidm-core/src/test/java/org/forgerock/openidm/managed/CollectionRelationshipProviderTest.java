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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.managed;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.util.RelationshipUtil;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.mockito.ArgumentMatcher;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class CollectionRelationshipProviderTest {
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

    @Test
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

        provider.validateRelationshipField(context, mgrWith1Report.get("reports"), mgrWith2Reports.get("reports"));

        // testing the condition where a user already has a manager.
        try {
            differentUser.put("manager", object(field(RelationshipUtil.REFERENCE_ID, "managed/user/someOtherManager")));
            provider.validateRelationshipField(context, manager.get("reports"), mgrWith2Reports.get("reports"));
            fail("expected to fail if the user already has a manager");
        } catch (BadRequestException e) {
            // test passed.
        }
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
