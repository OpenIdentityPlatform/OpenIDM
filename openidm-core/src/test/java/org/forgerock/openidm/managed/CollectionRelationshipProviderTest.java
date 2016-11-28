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

import static org.forgerock.json.JsonValue.*;
import static org.mockito.Mockito.*;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.util.RelationshipUtil;
import org.forgerock.services.context.Context;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CollectionRelationshipProviderTest {
    private ManagedObjectSetService managedObjectSyncService;
    private ConnectionFactory connectionFactory;
    private ActivityLogger activityLogger;
    private SchemaField schemaField;
    private ResourcePath resourcePath;

    @BeforeTest
    public void setup() throws Exception {
        activityLogger = mock(ActivityLogger.class);
        managedObjectSyncService = mock(ManagedObjectSetService.class);
        connectionFactory = mock(ConnectionFactory.class);
        schemaField = mock(SchemaField.class);
        when(schemaField.getName()).thenReturn("bobo");
        resourcePath = ResourcePath.valueOf("managed/user/foo");
    }

    @DataProvider(name = "duplicateRelationshipData")
    public Object[][] createDuplicateRelationshipData() {
        return new Object[][] {
                { "ref1", "grantType1", "temporalConstraint1", "ref1", "grantType1", "temporalConstraint1" },
                { "ref1", null, null, "ref1", null, null },
                { "ref1", "grantType1", null, "ref1", "grantType1", null },
                { "ref1", null, "temporalConstraint1", "ref1", null, "temporalConstraint1" }
        };
    }

    /*
    Test the fact that CollectionRelationshipProvider#validateRelationshipField will only validate relationships which
    do not exist in the old object. Here no validation should occur, because the old and new relationship fields are
    the same.
     */
    @Test(dataProvider = "duplicateRelationshipData")
    public void testSkippedValidationOfExistingRelationships(String ref1, String grantType1, String temporalConstraint1, String ref2,
                                                 String grantType2, String temporalConstraint2) throws ResourceException {
        final JsonValue firstRelationshipField = json(array(makeRelationship(ref1, grantType1, temporalConstraint1)));
        final JsonValue secondRelationshipField = json(array(makeRelationship(ref2, grantType2, temporalConstraint2)));
        final RelationshipValidator relationshipValidator = mock(RelationshipValidator.class);
        doThrow(ResourceException.newResourceException(400)).when(
                relationshipValidator).validateRelationship(any(JsonValue.class), any(ResourcePath.class), any(Context.class), anyBoolean());
        final CollectionRelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed/widget"), schemaField, activityLogger, managedObjectSyncService, relationshipValidator);
        //if the relationshipValidator were invoked, it would throw an exception.
        relationshipProvider.validateRelationshipField(mock(Context.class), firstRelationshipField, secondRelationshipField, resourcePath, true);
    }

    @DataProvider(name = "distinctRelationshipData")
    public Object[][] createDistinctRelationshipData() {
        return new Object[][] {
                { "ref1", "grantType1", "temporalConstraint1", "ref1", "grantType2", "temporalConstraint1" },
                { "ref1", null, null, "ref2", null, null },
                { "ref1", "grantType1", null, "ref1", "grantType1", "temporalConstraint1" },
                { "ref1", null, "temporalConstraint1", "ref1", null, "temporalConstraint2" }
        };
    }

    /*
    Test the fact that CollectionRelationshipProvider#validateRelationshipField will only validate relationships which
    do not exist in the old object. Here validation should occur, because the old and new relationship fields are different.
     */
    @Test(dataProvider = "distinctRelationshipData", expectedExceptions = BadRequestException.class)
    public void testValidationOfNewRelationships(String ref1, String grantType1, String temporalConstraint1, String ref2,
                                                             String grantType2, String temporalConstraint2) throws ResourceException {
        final JsonValue firstRelationshipField = json(array(makeRelationship(ref1, grantType1, temporalConstraint1)));
        final JsonValue secondRelationshipField = json(array(makeRelationship(ref2, grantType2, temporalConstraint2)));
        final RelationshipValidator relationshipValidator = mock(RelationshipValidator.class);
        doThrow(ResourceException.newResourceException(400)).when(
                relationshipValidator).validateRelationship(any(JsonValue.class), any(ResourcePath.class), any(Context.class), anyBoolean());
        final CollectionRelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed/widget"), schemaField, activityLogger, managedObjectSyncService, relationshipValidator);
        //invocation of the relationshipValidator will throw an exception
        relationshipProvider.validateRelationshipField(mock(Context.class), firstRelationshipField, secondRelationshipField, resourcePath, true);
    }

    private Map<String, Object> makeRelationship(String referenceId, String grantType, String temporalConstraint) {
        return json(object(
                makeField(RelationshipUtil.REFERENCE_ID, referenceId),
                makeField(RelationshipUtil.REFERENCE_PROPERTIES, makeRefProperties(grantType, temporalConstraint))
        )).asMap();
    }

    private Map<String, Object> makeRefProperties(String grantType, String temporalConstraint) {
        return json(object(
                makeField(RelationshipValidator.GRANT_TYPE, grantType),
                makeField(RelationshipValidator.TEMPORAL_CONSTRAINTS, Collections.singletonList(temporalConstraint))
        )).asMap();
    }

    /*
    A 'special' field constructor which can simply return null, as opposed to JsonValue#field, which always creates an entry.
    */
    private Map.Entry<String, Object> makeField(String key, Object value) {
        if (value != null) {
            return new AbstractMap.SimpleImmutableEntry<>(key, value);
        }
        return null;
    }
}
