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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_FIELD_FIRST_ID;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_FIELD_SECOND_ID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.util.RelationshipUtil;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link RelationshipValidator} and its implementations.
 */
public class RelationshipValidatorTest {
    private static final class SchemaAndConnection {
        private final SchemaField schemaField;
        private final Connection connection;

        private SchemaAndConnection(SchemaField schemaField, Connection connection) {
            this.schemaField = schemaField;
            this.connection = connection;
        }
    }

    private static final String REVERSE_PROPERTY_NAME = "oneOrMany";

    private static final JsonValue SINGLETON_VERTEX_POPULATED_VALUE = json(object(field(REVERSE_PROPERTY_NAME, "im_populated")));

    private static final JsonValue SINGLETON_VERTEX_AVAILABLE_VALUE = json(object(field(REVERSE_PROPERTY_NAME, null)));

    private static final JsonValue ARRAY_VERTEX = json(object());

    private static final boolean PERFORM_DUPLICATE_ASSIGNMENT_CHECK = true;

    private static final String REFERENCE_ID_VALUE = "managed/widgetPart/part1";

    private static final JsonValue TEST_RELATIONSHIP = json(object(field(RelationshipUtil.REFERENCE_ID, REFERENCE_ID_VALUE)));

    private static final ResourcePath REFERRER_ID = ResourcePath.valueOf("managed/user/bobo");

    private static final JsonValue POPULATED_EDGE_RESPONSE = json(object(field(REPO_FIELD_FIRST_ID, REFERRER_ID.toString()),
            field(REPO_FIELD_SECOND_ID, REFERENCE_ID_VALUE)));

    private ManagedObjectSetService managedObjectSyncService;
    private IDMConnectionFactory connectionFactory;
    private ActivityLogger activityLogger;
    private final Random random = new Random();

    @BeforeTest
    public void setup() throws Exception {
        activityLogger = mock(ActivityLogger.class);
        managedObjectSyncService = mock(ManagedObjectSetService.class);
        connectionFactory = mock(IDMConnectionFactory.class);
    }

    /*
     * Tests the case in a many-to-one relationship where the referenced vertext is available for assignment.
     */
    @Test
    public void testManyToOneReverseRelationshipVertextAvailable() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = mocksForArrayReferrerReverse();
        final RelationshipValidator relationshipValidator = reverseRelationshipValidatorForSingletonVertex(schemaAndConnection.schemaField,
                connectionFactory);

        // test that we are dealing with a reverse validator.
        assertTrue(relationshipValidator instanceof ReverseRelationshipValidator);

        // test when the reverseProperty isn't populated - which should be a valid condition.
        ResourceResponse foundRelationshipResponse = mock(ResourceResponse.class);
        when(foundRelationshipResponse.getContent()).thenReturn(SINGLETON_VERTEX_AVAILABLE_VALUE);
        when(schemaAndConnection.connection.read(any(Context.class), any(ReadRequest.class))).thenReturn(foundRelationshipResponse);
        relationshipValidator.validateRelationship(TEST_RELATIONSHIP, REFERRER_ID, new RootContext(),
                PERFORM_DUPLICATE_ASSIGNMENT_CHECK);
    }

    /*
     * Tests the case in a many-to-one relationship where the reverse side of a relationship is already populated
     */
    @Test(expectedExceptions = BadRequestException.class)
    public void testManyToOneReverseRelationshipVertexPopulated() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = mocksForArrayReferrerReverse();
        final RelationshipValidator relationshipValidator = reverseRelationshipValidatorForSingletonVertex(schemaAndConnection.schemaField,
                connectionFactory);

        // test that we are dealing with a reverse validator.
        assertTrue(relationshipValidator instanceof ReverseRelationshipValidator);

        // now test that when the reverse relationship is already populated, that it returns as BadRequest.
        ResourceResponse foundRelationshipResponse = mock(ResourceResponse.class);
        when(foundRelationshipResponse.getContent()).thenReturn(SINGLETON_VERTEX_POPULATED_VALUE);
        when(schemaAndConnection.connection.read(any(Context.class), any(ReadRequest.class))).thenReturn(foundRelationshipResponse);
        relationshipValidator.validateRelationship(TEST_RELATIONSHIP, REFERRER_ID, new RootContext(),
                PERFORM_DUPLICATE_ASSIGNMENT_CHECK);
    }

    /*
     * Tests the case in a many-to-one relationship where the referred-to vertex in the request does not exist
     */
    @Test(expectedExceptions = BadRequestException.class)
    public void testManyToOneReverseRelationshipRefMissing() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = mocksForArrayReferrerReverse();
        final RelationshipValidator relationshipValidator = reverseRelationshipValidatorForSingletonVertex(schemaAndConnection.schemaField,
                connectionFactory);

        // test that we are dealing with a reverse validator.
        assertTrue(relationshipValidator instanceof ReverseRelationshipValidator);

        // test when the linked relationship isn't found that it returns as BadRequest.
        when(schemaAndConnection.connection.read(any(Context.class), any(ReadRequest.class))).thenThrow(new NotFoundException());
        relationshipValidator.validateRelationship(TEST_RELATIONSHIP, REFERRER_ID,new RootContext(),
                PERFORM_DUPLICATE_ASSIGNMENT_CHECK);
    }

    /*
     * Tests the case in a one-to-one relationship where the referenced vertex is available for assignment
     */
    @Test
    public void testOneToOneReverseRelationship() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = mocksForSingletonReferrerReverse();
        final RelationshipValidator relationshipValidator =
                reverseRelationshipValidatorForSingletonVertex(schemaAndConnection.schemaField, connectionFactory);

        // test that we are dealing with a reverse validator.
        assertTrue(relationshipValidator instanceof ReverseRelationshipValidator);

        // test when the reverseProperty isn't populated - which should be a valid condition.
        ResourceResponse foundRelationshipResponse = mock(ResourceResponse.class);
        when(schemaAndConnection.connection.read(any(Context.class), any(ReadRequest.class))).thenReturn(foundRelationshipResponse);
        when(foundRelationshipResponse.getContent()).thenReturn(SINGLETON_VERTEX_AVAILABLE_VALUE);
        relationshipValidator.validateRelationship(TEST_RELATIONSHIP, REFERRER_ID, new RootContext(),
                PERFORM_DUPLICATE_ASSIGNMENT_CHECK);
    }

    /*
     * Tests the case in a one-to-one relationship where the reverse side of a relationship already has a value.
     */
    @Test(expectedExceptions = BadRequestException.class)
    public void testOneToOneReverseRelationshipVertexOccupied() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = mocksForSingletonReferrerReverse();
        final RelationshipValidator relationshipValidator =
                reverseRelationshipValidatorForSingletonVertex(schemaAndConnection.schemaField, connectionFactory);

        // test that we are dealing with a reverse validator.
        assertTrue(relationshipValidator instanceof ReverseRelationshipValidator);

        // now test that when the reverse relationship is already populated, that it returns as BadRequest.
        ResourceResponse foundRelationshipResponse = mock(ResourceResponse.class);
        when(foundRelationshipResponse.getContent()).thenReturn(SINGLETON_VERTEX_POPULATED_VALUE);
        when(schemaAndConnection.connection.read(any(Context.class), any(ReadRequest.class))).thenReturn(foundRelationshipResponse);
        relationshipValidator.validateRelationship(TEST_RELATIONSHIP, REFERRER_ID, new RootContext(),
                PERFORM_DUPLICATE_ASSIGNMENT_CHECK);
    }

    /*
     * Tests the case in a one-to-one relationship where the referenced vertex is missing.
     */
    @Test(expectedExceptions = BadRequestException.class)
    public void testOneToOneReverseRelationshipRefAbsent() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = mocksForSingletonReferrerReverse();
        final RelationshipValidator relationshipValidator = reverseRelationshipValidatorForSingletonVertex(schemaAndConnection.schemaField, connectionFactory);

        // test that we are dealing with a reverse validator.
        assertTrue(relationshipValidator instanceof ReverseRelationshipValidator);

        // test when the linked relationship isn't found that it returns as BadRequest.
        when(schemaAndConnection.connection.read(any(Context.class), any(ReadRequest.class))).thenThrow(new NotFoundException());
        relationshipValidator.validateRelationship(TEST_RELATIONSHIP, REFERRER_ID,new RootContext(),
                PERFORM_DUPLICATE_ASSIGNMENT_CHECK);
    }

    /*
     * tests the case in a many-to-many relationship where the relationship edge is not already present
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testManyToManyReverseRelationshipEdgeAbsent() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = mocksForArrayReferrerReverse();
        final RelationshipValidator relationshipValidator = reverseRelationshipValidatorForArrayVertex(schemaAndConnection.schemaField, connectionFactory);

        // test that we are dealing with a reverse validator.
        assertTrue(relationshipValidator instanceof ReverseRelationshipValidator);

        // when the edge query does not return an existing edge
        when(schemaAndConnection.connection.query(any(Context.class), any(QueryRequest.class), any(Collection.class))).thenReturn(null);
        relationshipValidator.validateRelationship(TEST_RELATIONSHIP, REFERRER_ID, new RootContext(),
                PERFORM_DUPLICATE_ASSIGNMENT_CHECK);
    }

    /*
     * Tests the case in a many-to-many relationship where the referred-to vertex is not present.
     */
    @Test(expectedExceptions = BadRequestException.class)
    public void testManyToManyRefAbsent() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = mocksForArrayReferrerReverse();
        final RelationshipValidator relationshipValidator = reverseRelationshipValidatorForArrayVertex(schemaAndConnection.schemaField, connectionFactory);

        // test that we are dealing with a reverse validator.
        assertTrue(relationshipValidator instanceof ReverseRelationshipValidator);

        // test when the linked relationship isn't found that it returns as BadRequest.
        when(schemaAndConnection.connection.read(any(Context.class), any(ReadRequest.class))).thenThrow(new NotFoundException());
        relationshipValidator.validateRelationship(TEST_RELATIONSHIP, REFERRER_ID,new RootContext(),
                PERFORM_DUPLICATE_ASSIGNMENT_CHECK);
    }

    /*
     * Tests the case in a many-to-many relationship where a relationship edge already matching the to-be-added
     * relationship is present.
     */
    @Test(expectedExceptions = DuplicateRelationshipException.class)
    @SuppressWarnings("unchecked")
    public void testManyToManyReverseRelationshipEdgePresent() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = mocksForArrayReferrerReverse();
        final RelationshipValidator relationshipValidator =
                reverseRelationshipValidatorForArrayVertex(schemaAndConnection.schemaField, connectionFactory);

        // test that we are dealing with a reverse validator.
        assertTrue(relationshipValidator instanceof ReverseRelationshipValidator);

        // now test when the edge query returns an edge matching the to-be-created edge
        ResourceResponse foundRelationshipResponse = mock(ResourceResponse.class);
        when(foundRelationshipResponse.getContent()).thenReturn(ARRAY_VERTEX);
        when(schemaAndConnection.connection.read(any(Context.class), any(ReadRequest.class))).thenReturn(foundRelationshipResponse);
        /*
        The query request 'harvests' its results via the Collection parameter which functions as an out parameter. Use the
        Answer construct to populate this Collection with an edge query response which indicates that the relationship edge
        is already present.
         */
        when(schemaAndConnection.connection.query(any(Context.class), any(QueryRequest.class), any(Collection.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Collection)invocation.getArguments()[2]).add(populatedEdgeQueryResponse());
                return null;
            }
        });
        relationshipValidator.validateRelationship(TEST_RELATIONSHIP, REFERRER_ID, new RootContext(),
                PERFORM_DUPLICATE_ASSIGNMENT_CHECK);
    }

    /**
     * The RelationshipValidator tests require mocked SchemaField and Connection state. SchemaField reflect the
     * state of the referring-vertex, and the Connection is used to mock read and query responses.
     */
    private SchemaAndConnection mocksForSingletonReferrerReverse() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = commonReverseMockState();
        when(schemaAndConnection.schemaField.isArray()).thenReturn(false);
        return schemaAndConnection;
    }

    /*
     * The RelationshipValidator tests require mocked SchemaField and Connection state. SchemaField reflect the
     * state of the referring-vertex, and the Connection is used to mock read and query responses.
     */
    private SchemaAndConnection mocksForArrayReferrerReverse() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = commonReverseMockState();
        when(schemaAndConnection.schemaField.isArray()).thenReturn(true);
        return schemaAndConnection;
    }

    private SchemaAndConnection commonReverseMockState() throws ResourceException {
        Connection connection = mock(Connection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        SchemaField schemaField = mock(SchemaField.class);
        when(schemaField.isReverseRelationship()).thenReturn(true);
        when(schemaField.getName()).thenReturn("reports");
        when(schemaField.getReversePropertyName()).thenReturn(REVERSE_PROPERTY_NAME);
        return new SchemaAndConnection(schemaField, connection);
    }

    /*
     * Tests the case in a one-to-many relationship where the referred-to vertex is not present.
     */
    @Test(expectedExceptions = BadRequestException.class)
    public void testOneToManyRefAbsent() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = mocksForArrayReferrerReverse();
        final RelationshipValidator relationshipValidator = reverseRelationshipValidatorForArrayVertex(schemaAndConnection.schemaField, connectionFactory);

        // test that we are dealing with a reverse validator.
        assertTrue(relationshipValidator instanceof ReverseRelationshipValidator);

        // test when the linked relationship isn't found that it returns as BadRequest.
        when(schemaAndConnection.connection.read(any(Context.class), any(ReadRequest.class))).thenThrow(new NotFoundException());
        relationshipValidator.validateRelationship(TEST_RELATIONSHIP, REFERRER_ID,new RootContext(),
                PERFORM_DUPLICATE_ASSIGNMENT_CHECK);
    }

    /*
     * Tests the case in a one-to-many relationship where a relationship edge already matching the to-be-added
     * relationship is present. Note that this check is not performed in a one-to-many case, because:
     * 1. for a CREATE, the many cannot refer back to the one because the one is in the process of being created
     * 2. for a UPDATE or a PATCH, request invocation state will over-write repo state, so the only necessary check
     * is that the referenced vertex exists. See ReverseRelationshipValidator#validateSuccessfulReadResponse for details.
     *
     * Thus this test will insure that the creation of a one-to-many relationship with an already existing edge will
     * not fail, as this code path is not even checked by the reverse-relationship-validator.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testOneToManyReverseRelationshipEdgePresent() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = mocksForSingletonReferrerReverse();
        final RelationshipValidator relationshipValidator =
                reverseRelationshipValidatorForArrayVertex(schemaAndConnection.schemaField, connectionFactory);

        // test that we are dealing with a reverse validator.
        assertTrue(relationshipValidator instanceof ReverseRelationshipValidator);

        // now test when the edge query returns an edge matching the to-be-created edge
        ResourceResponse foundRelationshipResponse = mock(ResourceResponse.class);
        when(foundRelationshipResponse.getContent()).thenReturn(ARRAY_VERTEX);
        when(schemaAndConnection.connection.read(any(Context.class), any(ReadRequest.class))).thenReturn(foundRelationshipResponse);
        /*
        The query request 'harvests' its results via the Collection parameter which functions as an out parameter. Use the
        Answer construct to populate this Collection with an edge query response which indicates that the relationship edge
        is already present.
         */
        when(schemaAndConnection.connection.query(any(Context.class), any(QueryRequest.class), any(Collection.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Collection)invocation.getArguments()[2]).add(populatedEdgeQueryResponse());
                return null;
            }
        });
        relationshipValidator.validateRelationship(TEST_RELATIONSHIP, REFERRER_ID, new RootContext(),
                PERFORM_DUPLICATE_ASSIGNMENT_CHECK);
    }

    @Test
    public void testForwardRelationshipRefFound() throws ResourceException {
        final SchemaAndConnection schemaAndConnection = forwardMockState();

        final RelationshipValidator relationshipValidator = relationshipValidator(schemaAndConnection.schemaField);
        // test that we are dealing with a Forward validator.
        assertTrue(relationshipValidator instanceof ForwardRelationshipValidator);

        // test when the linked relationship IS found that it should be a valid case.
        ResourceResponse foundRelationshipResponse = mock(ResourceResponse.class);
        when(foundRelationshipResponse.getContent()).thenReturn(json(object(field("name","i_exist"))));
        when(schemaAndConnection.connection.read(any(Context.class), any(ReadRequest.class))).thenReturn(foundRelationshipResponse);
        relationshipValidator.validateRelationship(TEST_RELATIONSHIP, REFERRER_ID, new RootContext(),
                PERFORM_DUPLICATE_ASSIGNMENT_CHECK);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testForwardRelationshipRefMissing() throws Exception {
        final SchemaAndConnection schemaAndConnection = forwardMockState();

        final RelationshipValidator relationshipValidator = relationshipValidator(schemaAndConnection.schemaField);

        // test that we are dealing with a Forward validator.
        assertTrue(relationshipValidator instanceof ForwardRelationshipValidator);

        // test when the linked relationship isn't found that it returns as BadRequest.
        when(schemaAndConnection.connection.read(any(Context.class), any(ReadRequest.class))).thenThrow(new NotFoundException());
        relationshipValidator.validateRelationship(TEST_RELATIONSHIP, REFERRER_ID, new RootContext(), PERFORM_DUPLICATE_ASSIGNMENT_CHECK);
    }

    private SchemaAndConnection forwardMockState() throws ResourceException {
        Connection connection = mock(Connection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        SchemaField schemaField = mock(SchemaField.class);
        when(schemaField.isReverseRelationship()).thenReturn(false);
        when(schemaField.isArray()).thenReturn(false);
        when(schemaField.getName()).thenReturn("reports");
        when(schemaField.getReversePropertyName()).thenReturn(REVERSE_PROPERTY_NAME);
        return new SchemaAndConnection(schemaField, connection);
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
                new ResourcePath("managed/widget"), schemaField, activityLogger, managedObjectSyncService, relationshipValidator(schemaField));
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
                new ResourcePath("managed/widget"), schemaField, activityLogger, managedObjectSyncService, relationshipValidator(schemaField));
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
                new ResourcePath("managed/widget"), schemaField, activityLogger, managedObjectSyncService, relationshipValidator(schemaField));
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
                new ResourcePath("managed/widget"), schemaField, activityLogger, managedObjectSyncService, relationshipValidator(schemaField));
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

    private RelationshipValidator relationshipValidator(SchemaField relationshipField) {
        if (relationshipField.isReverseRelationship()) {
            return new ReverseRelationshipValidator(connectionFactory, relationshipField);
        }
        return new ForwardRelationshipValidator(connectionFactory);
    }

    private RelationshipValidator reverseRelationshipValidatorForSingletonVertex(SchemaField relationshipField, ConnectionFactory connectionFactory) {
        return new ReverseRelationshipValidator(connectionFactory, relationshipField) {
            @Override
            protected ReverseRelationshipValidator.ReverseReferenceType getReverseReferenceType(String relationshipRef, Context context) {
                return ReverseReferenceType.RELATIONSHIP;
            }
        };
    }

    private RelationshipValidator reverseRelationshipValidatorForArrayVertex(SchemaField relationshipField, ConnectionFactory connectionFactory) {
        return new ReverseRelationshipValidator(connectionFactory, relationshipField) {
            @Override
            protected ReverseRelationshipValidator.ReverseReferenceType getReverseReferenceType(String relationshipRef, Context context) {
                return ReverseReferenceType.ARRAY;
            }
        };
    }

    private ResourceResponse populatedEdgeQueryResponse() {
        final ResourceResponse resourceResponse = mock(ResourceResponse.class);
        when(resourceResponse.getContent()).thenReturn(POPULATED_EDGE_RESPONSE);
        return resourceResponse;
    }
}
