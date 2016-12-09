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

import static org.forgerock.http.routing.UriRouterContext.uriRouterContext;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_FIELD_FIRST_ID;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_FIELD_FIRST_PROPERTY_NAME;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_FIELD_SECOND_ID;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_FIELD_SECOND_PROPERTY_NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class RelationshipProviderTest {
    private static final String MANAGED_OBJECT_ID = "bobo";
    private static final String SCHEMA_FIELD_NAME = "manager";
    private ManagedObjectSetService managedObjectSyncService;
    private ConnectionFactory connectionFactory;
    private ActivityLogger activityLogger;
    private SchemaField schemaField;
    private Context context;

    @BeforeTest
    public void setup() throws Exception {
        activityLogger = mock(ActivityLogger.class);
        managedObjectSyncService = mock(ManagedObjectSetService.class);
        connectionFactory = mock(ConnectionFactory.class);
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
}
