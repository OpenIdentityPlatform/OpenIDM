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
 */
package org.forgerock.openidm.managed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.routing.UriRouterContext.uriRouterContext;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.services.context.RootContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SingletonRelationshipProviderTest {
    private ManagedObjectSetService managedObjectSyncService;
    private ConnectionFactory connectionFactory;
    private ActivityLogger activityLogger;
    private SchemaField schemaField;

    @BeforeTest
    public void setup() throws Exception {
        activityLogger = mock(ActivityLogger.class);
        managedObjectSyncService = mock(ManagedObjectSetService.class);
        connectionFactory = mock(ConnectionFactory.class);
        schemaField = mock(SchemaField.class);
        when(schemaField.getName()).thenReturn("bobo");
    }

    @Test
    public void testGetManagedObjectId() {
        final RelationshipValidator relationshipValidator = mock(RelationshipValidator.class);
        final SingletonRelationshipProvider relationshipProvider = new SingletonRelationshipProvider(connectionFactory,
                new ResourcePath("managed/widget"), schemaField, activityLogger, managedObjectSyncService, relationshipValidator);
        final String managedObjectId = "bjensen";
        final UriRouterContext parent = uriRouterContext(new RootContext()).templateVariable("managedObjectId", managedObjectId).build();
        final UriRouterContext context = uriRouterContext(parent).build();
        assertThat(relationshipProvider.getManagedObjectId(context)).isEqualTo(managedObjectId);
    }
}
