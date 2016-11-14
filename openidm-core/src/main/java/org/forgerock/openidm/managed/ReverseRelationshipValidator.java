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

import static java.text.MessageFormat.format;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_FIELD_FIRST_ID;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_FIELD_PROPERTIES;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_FIELD_SECOND_ID;
import static org.forgerock.openidm.util.RelationshipUtil.REFERENCE_ID;
import static org.forgerock.openidm.util.RelationshipUtil.REFERENCE_PROPERTIES;
import static org.forgerock.openidm.managed.RelationshipProvider.REPO_RESOURCE_PATH;

import java.util.Collection;
import java.util.LinkedList;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.services.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for reverse (aka bi-directional) relationships.
 * If the relationship is not found then the relationshipField is invalid, otherwise then the reverse field's
 * property either needs to be null as a singleton or a collection to accept the new relationship link.
 */
public class ReverseRelationshipValidator extends RelationshipValidator {
    private static final Logger logger = LoggerFactory.getLogger(ReverseRelationshipValidator.class);

    private final String EDGE_QUERY_ID = "find-relationship-edges";
    private final String EDGE_QUERY_VERTEX_1_ID = "vertex1Id";
    private final String EDGE_QUERY_VERTEX_1_FIELD_NAME = "vertex1FieldName";
    private final String EDGE_QUERY_VERTEX_2_ID = "vertex2Id";
    private final String EDGE_QUERY_VERTEX_2_FIELD_NAME = "vertex2FieldName";

    private final boolean relationshipIsArray;
    private final String relationshipPropertyName;
    private final String relationshipReversePropertyName;

    /**
     * Constructs the validator to validate reverse relationships.
     *
     * @param relationshipProvider the provider that owns this validator.
     */
    public ReverseRelationshipValidator(RelationshipProvider relationshipProvider) {
        super(relationshipProvider);
        relationshipIsArray = relationshipProvider.getSchemaField().isArray();
        relationshipPropertyName = relationshipProvider.getSchemaField().getName();
        relationshipReversePropertyName = relationshipProvider.getSchemaField().getReversePropertyName();
    }

    /**
     * Creates the ReadRequest used to validate the relationship. Note that if the RelationshipValidator is validating
     * a relationship which is an array, the reverseProperyName will not be added to the ReadRequest,
     * as this will trigger the execution of the find-relationships-for-resource query for the graph vertex identified by
     * the REFERENCE_ID, which will, in turn, return references to all graph vertices linked to this relationship, which
     * can generate a result-set with many thousands of entries. This reversePropertyName will be fetched only if the
     * relationship has a cardinality of 1 (e.g. represented by a SingletonRelationshipProvider), as this state aids in
     * relationship validation in the validateSuccessfulReadResponse method below.
     *
     * @param relationshipField the field to validate.
     * @return the request to invoke for validation.
     */
    protected ReadRequest newValidateRequest(JsonValue relationshipField) {
        final ReadRequest request = Requests.newReadRequest(relationshipField.get(REFERENCE_ID).asString());
        if (!relationshipIsArray) {
            request.addField(relationshipReversePropertyName);
        }
        return request;
    }

    /**
     * The reverse field's property either needs to be null as a singleton or a collection to accept the new
     * relationship link. Note however, that the reversePropertyName will only be set if the relationship is a singleton
     * relationship, in order to avoid the overhead of reading all relationship edges for non-singelton relationships.
     *
     * @param relationshipField the field to be validated.
     * @param referrerId the id of the object 'hosting' the relationships, aka the referrer; used to check whether
     *                          the referred-to object specified by the relationship already contains a reference to this referrer
     * @param resourceResponse the response from the validation read request. Contains the reverse-property state of the entity referred-to
     *                         by the relationship. Thus if the request adding a new role to a user (user is the resource/managed-object),
     *                         the relationshipField will define the
     *                         _ref to the role, and the resourceResponse will contain the user's role state. Likewise,
     *                         if the request is adding a new user to a role (role is the resource/managed-object),
     *                         the relationshipField will define the _ref to the user, and the resourceResponse
     *                         will contain the role's member state.
     * @param performDuplicateAssignmentCheck set to true if invocation state should be compared to repository state to determine if
     *                                        existing relationships are specified in the invocation
     * @throws DuplicateRelationshipException if the relationship is invalid
     */
    protected void validateSuccessfulReadResponse(Context context, JsonValue relationshipField, ResourcePath referrerId,
              ResourceResponse resourceResponse, boolean performDuplicateAssignmentCheck) throws ResourceException {
        if (performDuplicateAssignmentCheck) {
            if (relationshipIsArray) {
                validateCollectionRelationshipReadResponse(context, relationshipField, referrerId);
            } else {
                validateSingletonRelationshipReadResponse(resourceResponse);
            }
        }
    }

    private void validateCollectionRelationshipReadResponse(Context context, JsonValue relationshipField, ResourcePath referrerId)
        throws ResourceException {

        final Collection<ResourceResponse> repoRelationshipEdgesResponse =
                readRelationshipEndpointEdges(context, relationshipField, referrerId);
        for (ResourceResponse repoRelationshipEdgeResponse : repoRelationshipEdgesResponse) {
            final JsonValue repoRelationshipEdge = repoRelationshipEdgeResponse.getContent();
            if (repoRelationshipEdgeAndRelationshipEqual(repoRelationshipEdge, relationshipField)) {
                throw new DuplicateRelationshipException(format(
                        "Managed object ''{0}'' has already been assigned relationship ''{1}''.",
                        referrerId, relationshipField.get(REFERENCE_ID).asString()));
            }
        }
    }

    /**
     *
     * @param repoRelationshipEdge a repo-centric representation of a relationship edge, composed of firstId, secondId,
     *                             firstPropertyName, secondPropertyName, and properties, as returned by the
     *                             readRelationshipEndpointEdges
     * @param relationship the user-centric representation of a relationship with _ref and _refProperties
     * @return true iff the two relationship representations are equivalent
     */
    private boolean repoRelationshipEdgeAndRelationshipEqual(JsonValue repoRelationshipEdge, JsonValue relationship) {
        final String relationshipRef = relationship.get(REFERENCE_ID).asString();
        return  (
                relationshipRef.equals(repoRelationshipEdge.get(REPO_FIELD_FIRST_ID).asString()) ||
                relationshipRef.equals(repoRelationshipEdge.get(REPO_FIELD_SECOND_ID).asString())
                ) && refPropStateEqual(repoRelationshipEdge.get(REPO_FIELD_PROPERTIES), relationship.get(REFERENCE_PROPERTIES));
    }

    /**
     * Executes the query which returns the edges between the graph vertices defined by the referrerId
     * (e.g. managed-user) and the vertex defining the relationship reference (e.g. managed-role). Note that this
     * referral is bi-directional: the referrer could just as well be the managed-role, and and the relationship reference
     * could be to a managed-user. The fundamental point is that each relationship terminus defines a graph vertex, and
     * the relationship is defined by edges between these vertices. This method returns the edges between two graph
     * vertices, as our relationship models a multi-graph: a graph where multiple edges between vertices are possible.
     *
     * The two vertices are identified by:
     * 1. the referrerId and the relationshipPropertyName: the referrerId defines the managed-object against whom the relationship
     * create or update operation was performed, and the relationshipPropertyName is the name of the schemaField defined in
     * managed.json which defines the relationship, which essentially defines a graph vertex: i.e. roles or members
     * 2. the reversePropertyName and the _ref value in the relationshipField: the _ref value defines the identity of the
     * graph vertex to which the referrerId will establish a relationship. The reversePropertyName is the schemaField value
     * defining the relationship vertex which identifies the 'edge collection' in the vertex, or the field which refers back
     * to the referring vertex.
     * @param context the request Context
     * @param relationshipField the relationship field to be validated. Defines the reference from the referrerId to
     *                          another graph vertex identified by the _ref property
     * @param referrerId the identity of the graph vertex originating the relationship request.
     * @return a JsonValue encapsulating a collection of objects each of which define the edges between the two graph vertices.
     * Note that this state reflects the state of the relationships table, with fields of: firstId, firstPropertyName, secondId
     * and secondPropertyName, and properties (which contain the relationship _refProperties).
     */
    private Collection<ResourceResponse> readRelationshipEndpointEdges(Context context, JsonValue relationshipField, ResourcePath referrerId) throws ResourceException {
        final EventEntry measure = Publisher.start(
                Name.get("openidm/internal/reverseRelationshipValidator/readRelationshipEndpointEdges"), null, null);
        try {
            final String vertex1Id = referrerId.toString();
            final String vertex1FieldName = relationshipPropertyName;
            final String vertex2Id = relationshipField.get(REFERENCE_ID).asString();
            final String vertex2FieldName = relationshipReversePropertyName;
            logger.debug("Going to query for relationship-defining edge between vertices <{0},{1}> and <{2},{3}>."
                    , vertex1Id, vertex1FieldName, vertex2Id, vertex2FieldName);
            final QueryRequest edgeQueryRequest = Requests.newQueryRequest(REPO_RESOURCE_PATH)
                    .setQueryId(EDGE_QUERY_ID)
                    .setAdditionalParameter(EDGE_QUERY_VERTEX_1_ID, vertex1Id)
                    .setAdditionalParameter(EDGE_QUERY_VERTEX_1_FIELD_NAME, vertex1FieldName)
                    .setAdditionalParameter(EDGE_QUERY_VERTEX_2_ID, vertex2Id)
                    .setAdditionalParameter(EDGE_QUERY_VERTEX_2_FIELD_NAME, vertex2FieldName);
            Collection<ResourceResponse> edgeResponses = new LinkedList<>();
            getRelationshipProvider().getConnection().query(context, edgeQueryRequest, edgeResponses);
            return edgeResponses;
        } finally {
            measure.end();
        }
    }

    private void validateSingletonRelationshipReadResponse(ResourceResponse resourceResponse) throws PreconditionFailedException {
        JsonValue reversePropertyState = resourceResponse.getContent().get(relationshipReversePropertyName);
        if (!reversePropertyState.isCollection() && reversePropertyState.isNotNull()) {
            throw new DuplicateRelationshipException(format(
                    "In relationship endpoint ''{0}'', field ''{1}'' of managed object ''{2}'' is neither null nor a collection, " +
                            "and thus not available for assignment.",
                    getRelationshipProvider().resourceContainer.toString() + "/" + getRelationshipProvider().schemaField.getName(),
                    getRelationshipProvider().schemaField.getReversePropertyName(),
                    resourceResponse.getId()));
        }
    }
}
