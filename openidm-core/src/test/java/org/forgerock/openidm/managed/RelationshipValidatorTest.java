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

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.util.RelationshipUtil;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

/**
 * Tests {@link RelationshipValidator} and its implementations.
 */
public class RelationshipValidatorTest {

    public static final JsonValue SINGLETON_POPULATED_VALUE =
            json(object(field("reversePropertyName", "im_populated")));
    public static final JsonValue TEST_RELATIONSHIP =
            json(object(field(RelationshipUtil.REFERENCE_ID, "managed/widgetPart/part1")));
    private static final String RELATIONSHIP_ID = "the_id";
    private ManagedObjectSetService managedObjectSyncService;
    private ConnectionFactory connectionFactory;
    private ActivityLogger activityLogger;
    private final Random random = new Random();

    @BeforeTest
    public void setup() throws Exception {
        activityLogger = mock(ActivityLogger.class);
        managedObjectSyncService = mock(ManagedObjectSetService.class);
        connectionFactory = mock(ConnectionFactory.class);
    }

    /**
     * Tests the case when the reverse side of a relationship is already populated.
     * The expectation is that the lookup of the 'reversePropertyName' on the requested reference should be null or
     * a collection.
     *
     * @throws Exception
     */
//    @Test
    public void testReverseRelationship() throws Exception {
        Connection connection = mock(Connection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        // given
        SchemaField schemaField = mock(SchemaField.class);
        when(schemaField.isReverseRelationship()).thenReturn(true);
        when(schemaField.getName()).thenReturn("testField");
        when(schemaField.getReversePropertyName()).thenReturn("reversePropertyName");
        CollectionRelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed/widget"), schemaField, activityLogger, managedObjectSyncService);

        // test that we are dealing with a reverse validator.
        assertTrue(relationshipProvider.relationshipValidator instanceof ReverseRelationshipValidator);

        // now test that when the reverse relationship is already populated, that it returns as BadRequest.
        ResourceResponse foundRelationshipResponse = mock(ResourceResponse.class);
        when(foundRelationshipResponse.getContent()).thenReturn(SINGLETON_POPULATED_VALUE);
        when(connection.read(any(Context.class), any(ReadRequest.class))).thenReturn(foundRelationshipResponse);
        try {
            relationshipProvider.relationshipValidator.validateRelationship(TEST_RELATIONSHIP, ResourcePath.valueOf(RELATIONSHIP_ID), new RootContext(), true);
            fail("Expected to get BadRequestException");
        } catch (ResourceException e) {
            assertTrue(e instanceof PreconditionFailedException, "Expected to get PreconditionFailedException");
        }

        // now test when the reverseProperty isn't populated - which should be a valid condition.
        when(foundRelationshipResponse.getContent()).thenReturn(json(object(field("reversePropertyName", null))));
        try {
            relationshipProvider.relationshipValidator.validateRelationship(TEST_RELATIONSHIP, ResourcePath.valueOf(RELATIONSHIP_ID), new RootContext(), true);
        } catch (ResourceException e) {
            fail("Expected no exception.");
        }

        // test when the linked relationship isn't found that it returns as BadRequest.
        when(connection.read(any(Context.class), any(ReadRequest.class))).thenThrow(new NotFoundException());
        try {
            relationshipProvider.relationshipValidator.validateRelationship(TEST_RELATIONSHIP, ResourcePath.valueOf(RELATIONSHIP_ID),new RootContext(), true);
            fail("Expected to get BadRequestException");
        } catch (ResourceException e) {
            assertTrue(e instanceof BadRequestException, "Expected to get BadRequestException");
        }
    }

//    @Test
    public void testForwardRelationship() throws Exception {
        // given
        Connection connection = mock(Connection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        SchemaField schemaField = mock(SchemaField.class);
        when(schemaField.isReverseRelationship()).thenReturn(false);
        when(schemaField.getName()).thenReturn("testField");
        CollectionRelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed/widget"), schemaField, activityLogger, managedObjectSyncService);

        // test that we are dealing with a Forward validator.
        assertTrue(relationshipProvider.relationshipValidator instanceof ForwardRelationshipValidator);

        // test when the linked relationship IS found that it should be a valid case.
        ResourceResponse foundRelationshipResponse = mock(ResourceResponse.class);
        when(foundRelationshipResponse.getContent()).thenReturn(json(object(field("name","i_exist"))));
        when(connection.read(any(Context.class), any(ReadRequest.class))).thenReturn(foundRelationshipResponse);
        try {
            relationshipProvider.relationshipValidator.validateRelationship(TEST_RELATIONSHIP, ResourcePath.valueOf(RELATIONSHIP_ID), new RootContext(), true);
        } catch (ResourceException e) {
            fail("Expected no BadRequestException");
        }

        // test when the linked relationship isn't found that it returns as BadRequest.
        when(connection.read(any(Context.class), any(ReadRequest.class))).thenThrow(new NotFoundException());
        try {
            relationshipProvider.relationshipValidator.validateRelationship(TEST_RELATIONSHIP, ResourcePath.valueOf(RELATIONSHIP_ID), new RootContext(), true);
            fail("Expected to get BadRequestException");
        } catch (ResourceException e) {
            assertTrue(e instanceof BadRequestException, "Expected to get BadRequestException");
        }
    }

    @DataProvider(name = "relationshipData")
    public Object[][] createRelationshipData() {
        return new Object[][] {
                { "ref1", "grantType1", "temporalConstraint1", "ref2", "grantType2", "temporalConstraint2" },
                { "ref1", "grantType1", "temporalConstraint1", "ref1", null, "temporalConstraint2" },
                { "ref1", "grantType1", "temporalConstraint1", "ref1", "grantType1", null },
                { "ref1", null, null, "ref2", null, null }
        };
    }

    @Test(dataProvider = "relationshipData")
    public void testDuplicateRelationshipSuccess(String ref1, String grantType1, String temporalConstraint1, String ref2,
                                                 String grantType2, String temporalConstraint2) throws DuplicateRelationshipException  {
        final SchemaField schemaField = mock(SchemaField.class);
        when(schemaField.isReverseRelationship()).thenReturn(false);
        when(schemaField.getName()).thenReturn("testField");
        final JsonValue relationshipList = json(array(makeRelationship(ref1, grantType1, temporalConstraint1), makeRelationship(ref2, grantType2, temporalConstraint2)));
        final CollectionRelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed/widget"), schemaField, activityLogger, managedObjectSyncService);
        relationshipProvider.relationshipValidator.checkForDuplicateRelationshipsInInvocationState(relationshipList);
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


    @Test(dataProvider = "duplicateRelationshipData", expectedExceptions = DuplicateRelationshipException.class)
    public void testDuplicateRelationshipFailure(String ref1, String grantType1, String temporalConstraint1, String ref2,
                                                 String grantType2, String temporalConstraint2) throws DuplicateRelationshipException  {
        final SchemaField schemaField = mock(SchemaField.class);
        when(schemaField.isReverseRelationship()).thenReturn(false);
        when(schemaField.getName()).thenReturn("testField");
        final JsonValue relationshipList = json(array(makeRelationship(ref1, grantType1, temporalConstraint1), makeRelationship(ref2, grantType2, temporalConstraint2)));
        final CollectionRelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed/widget"), schemaField, activityLogger, managedObjectSyncService);
        relationshipProvider.relationshipValidator.checkForDuplicateRelationshipsInInvocationState(relationshipList);
    }

    @DataProvider(name = "distinctRefProperties")
    public Object[][] createDistinctRefPropertyData() {
        return new Object[][] {
                { "grantType1", "temporalConstraint1", "grantType1", null },
                { "grantType1", null, null, null },
                { null, "temporalConstraint1", null, "temporalConstraint2" },
                { "grantType1", "temporalConstraint1", "grantType2", "temporalConstraint1" },
                { "grantType1", "temporalConstraint1", "grantType1", "temporalConstraint2" }
        };
    }

    @Test(dataProvider = "distinctRefProperties")
    public void testDistinctRefProperties(String grantType1, String temporalConstraint1, String grantType2, String temporalConstraint2) throws BadRequestException  {
        SchemaField schemaField = mock(SchemaField.class);
        when(schemaField.isReverseRelationship()).thenReturn(false);
        when(schemaField.getName()).thenReturn("testField");
        final JsonValue firstRefProps = json(makeRefProperties(grantType1, temporalConstraint1));
        final JsonValue secondRefProps = json(makeRefProperties(grantType2, temporalConstraint2));
        CollectionRelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed/widget"), schemaField, activityLogger, managedObjectSyncService);
        assertFalse(relationshipProvider.relationshipValidator.refPropStateEqual(firstRefProps, secondRefProps),
                "ref props should be flagged as distinct");
    }

    @DataProvider(name = "duplicateRefProperties")
    public Object[][] createDuplicateRefPropertyData() {
        return new Object[][] {
                { "grantType1", "temporalConstraint1", "grantType1", "temporalConstraint1" },
                { "grantType1", null, "grantType1", null },
                { null, "temporalConstraint1", null, "temporalConstraint1" },
                { "grantType1", "temporalConstraint1", "grantType1", "temporalConstraint1" }
        };
    }

    @Test(dataProvider = "duplicateRefProperties")
    public void testDuplicateRefProperties(String grantType1, String temporalConstraint1, String grantType2,
                                           String temporalConstraint2) throws BadRequestException  {
        SchemaField schemaField = mock(SchemaField.class);
        when(schemaField.isReverseRelationship()).thenReturn(false);
        when(schemaField.getName()).thenReturn("testField");
        final JsonValue firstRefProps = json(makeRefProperties(grantType1, temporalConstraint1));
        final JsonValue secondRefProps = json(makeRefProperties(grantType2, temporalConstraint2));
        CollectionRelationshipProvider relationshipProvider = new CollectionRelationshipProvider(connectionFactory,
                new ResourcePath("managed/widget"), schemaField, activityLogger, managedObjectSyncService);
        assertTrue(relationshipProvider.relationshipValidator.refPropStateEqual(firstRefProps, secondRefProps),
                "ref props should be flagged as identical");
    }

    private Map<String, Object> makeRelationship(String referenceId, String grantType, String temporalConstraint) {
        return json(object(
                makeField(RelationshipUtil.REFERENCE_ID, referenceId),
                makeField(RelationshipUtil.REFERENCE_PROPERTIES, makeRefProperties(grantType, temporalConstraint))
        )).asMap();
    }

    private Map<String, Object> makeRefProperties(String grantType, String temporalConstraint) {
        return json(object(
                // simulate both a patch and a create (id present, or not)
                random.nextInt(10) > 5 ? makeField("_id", "bobo") : makeField("_id", null),
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
