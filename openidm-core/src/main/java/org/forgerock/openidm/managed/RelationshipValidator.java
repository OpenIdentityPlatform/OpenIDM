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
import static org.forgerock.openidm.util.RelationshipUtil.REFERENCE_ID;

import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for Validators of relationship requests.  When a relationship action is determined to be invalid, the
 * validateRelationship method will throw a BadRequestException with a message indicating the details of the reason for
 * it being invalid.
 */
abstract class RelationshipValidator {
    static final String TEMPORAL_CONSTRAINTS = "temporalConstraints";
    static final String GRANT_TYPE = "_grantType";

    private static final Logger logger = LoggerFactory.getLogger(RelationshipValidator.class);

    /**
     * The relationship provider that owns this validator.
     */
    private RelationshipProvider relationshipProvider;

    RelationshipValidator(RelationshipProvider relationshipProvider) {
        this.relationshipProvider = relationshipProvider;
    }

    /**
     * Simple getter to return the provider that owns this validator.
     *
     * @return the provider that owns this validator.
     */
    RelationshipProvider getRelationshipProvider() {
        return relationshipProvider;
    }

    /**
     * Return the readRequest which should succeed to indicate a valid relationship and return any content that
     * will assist with further validation.
     *
     * @param relationshipField the field to validate.
     * @param context the original invocation Context. Needed to query the repo.
     * @return the request needed to test validity.
     */
    abstract ReadRequest newValidateRequest(JsonValue relationshipField, Context context);

    /**
     * Implement to test the response provided from the read of the relationship.
     * @param context the request Context
     * @param relationshipField The field that is being validated.
     * @param referrerId the id of the object 'hosting' the relationships, aka the referrer; used to check whether
     *                          the referred-to object specified by the relationship already contains a reference to this referrer
     * @param response The contents will hold the response from the read call made on the relationship field.
     * @param performDuplicateAssignmentCheck set to true if invocation state should be compared to repository state to determine if
     *                                        existing relationships are specified in the invocation
     * @throws BadRequestException if the response is invalid based on the implementation of the validator.
     */
    abstract void validateSuccessfulReadResponse(Context context, JsonValue relationshipField, ResourcePath referrerId, ResourceResponse response,
            boolean performDuplicateAssignmentCheck) throws ResourceException;

    /**
     * Validates that the relationshipField will not create an invalid condition.
     * Does a read on the relationshipField.
     *
     *
     * @param relationshipField the field defining an individual relationship which will be validated.
     * @param referrerId the id of the object 'hosting' the relationships, aka the referrer; used to check whether
     *                          the referred-to object specified by the relationship already contains a reference to this referrer
     * @param context context of the request working with the relationship.
     * @param performDuplicateAssignmentCheck set to true if invocation state should be compared to repository state to determine if
     *                                        existing relationships are specified in the invocation
     * @throws ResourceException BadRequestException when the relationship is invalid, otherwise for other issues.
     */
    final void validateRelationship(final JsonValue relationshipField, ResourcePath referrerId, Context context,
                                    boolean performDuplicateAssignmentCheck)
            throws ResourceException {
        if (relationshipField.isNull()) {
            // if the new object has the relationshipField removed, we do not need to validate the null
            // relationshipField because there is no relationship to validate
            return;
        }
        if (relationshipField.isCollection()) {
            String message = format("''{0}'' is invalid as a relationship reference", relationshipField.asCollection());
            logger.debug(message);
            throw new BadRequestException(message);
        }
        try {
            validateSuccessfulReadResponse(context, relationshipField, referrerId, relationshipProvider.getConnection()
                    .read(context, newValidateRequest(relationshipField, context)), performDuplicateAssignmentCheck);
        } catch (NotFoundException e) {
            String message = format("The referenced relationship ''{0}'', does not exist",
                    relationshipField.get(REFERENCE_ID).asString());
            logger.debug(message);
            throw new BadRequestException(message, e);
        }
    }

    /**
     * Called to determine if the _refProperties of two relationships are equal.
     * @param existingRefProps the _refProperties of the existing relationship whose _ref matches that
     *                                          of the toBeAddedRelationship
     * @param toBeAddedRefProps the _refProperties of  the to-be-added relationship whose _ref matches
     *                                           that of an existing relationship
     * @return true if both the grantType and temporalConstraint fields are equal
     */
    @VisibleForTesting
    boolean refPropStateEqual(JsonValue existingRefProps, JsonValue toBeAddedRefProps) {
        return RelationshipEqualityHash.relationshipRefPropertiesEqual(existingRefProps, toBeAddedRefProps);
    }

    /**
     * It is possible that a create request specifies duplicate relationships - i.e. a role creation request specifies
     * duplicate managed users, a managed user creation specifies multiple duplicate role references, or multiple, identical
     * relationships are specified when consuming a relationship endpont. References are
     * considered equal if they refer to the same managed object, and have the same grantType and temporalConstraints state.
     * @param relationships the reference set of a particular relationship - e.g. roles or members
     * @throws DuplicateRelationshipException if there is a duplicate reference detected
     */
    @VisibleForTesting
    void checkForDuplicateRelationshipsInInvocationState(JsonValue relationships) throws DuplicateRelationshipException {
        if (relationships.isCollection()) {
            final Set<RelationshipEqualityHash> relationshipSet = new HashSet<>(relationships.size());
            for (JsonValue relationship : relationships) {
                if (!relationshipSet.add(new RelationshipEqualityHash(relationship))) {
                    throw new DuplicateRelationshipException("Duplicate relationship specified in relationship collection.");
                }
            }
        }
    }
}
