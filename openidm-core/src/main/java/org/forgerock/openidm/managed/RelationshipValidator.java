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
import static org.forgerock.openidm.util.RelationshipUtil.REFERENCE_PROPERTIES;

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
import java.util.Objects;
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
     * @return the request needed to test validity.
     */
    abstract ReadRequest newValidateRequest(JsonValue relationshipField);

    /**
     * Implement to test the response provided from the read of the relationship.
     *
     * @param relationshipField The field that is being validated.
     * @param referrerId the id of the object 'hosting' the relationships, aka the referrer; used to check whether
     *                          the referred-to object specified by the relationship already contains a reference to this referrer
     * @param response The contents will hold the response from the read call made on the relationship field.
     * @throws BadRequestException if the response is invalid based on the implementation of the validator.
     */
    abstract void validateSuccessfulReadResponse(JsonValue relationshipField, ResourcePath referrerId, ResourceResponse response)
            throws BadRequestException;

    /**
     * Validates that the relationshipField will not create an invalid condition.
     * Does a read on the relationshipField.
     *
     *
     * @param relationshipField the field defining an individual relationship which will be validated.
     * @param referrerId the id of the object 'hosting' the relationships, aka the referrer; used to check whether
     *                          the referred-to object specified by the relationship already contains a reference to this referrer
     * @param context context of the request working with the relationship.
     * @throws ResourceException BadRequestException when the relationship is invalid, otherwise for other issues.
     */
    final void validateRelationship(final JsonValue relationshipField, ResourcePath referrerId, Context context)
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
            validateSuccessfulReadResponse(relationshipField, referrerId, relationshipProvider.getConnection()
                    .read(context, newValidateRequest(relationshipField)));
        } catch (NotFoundException e) {
            String message = format("The referenced relationship ''{0}'' on ''{1}'', does not exist",
                    relationshipField.get(REFERENCE_ID).asString(), relationshipField.getPointer());
            logger.debug(message);
            throw new BadRequestException(message, e);
        }
    }

    /**
     * Called to determine if two relationships are equal. A relationship is considered equal iff the grantType and
     * temporalConstraint fields in the _refProperties are both equal.
     * @param existingRefProps the _refProperties of the existing relationship whose _ref matches that
     *                                          of the toBeAddedRelationship
     * @param toBeAddedRefProps the _refProperties of  the to-be-added relationship whose _ref matches
     *                                           that of an existing relationship
     * @return true if both the grantType and temporalConstraint fields are equal
     */
    @VisibleForTesting
    boolean refPropStateEqual(JsonValue existingRefProps, JsonValue toBeAddedRefProps) {
        return Objects.equals(existingRefProps.get(TEMPORAL_CONSTRAINTS).getObject(), toBeAddedRefProps.get(TEMPORAL_CONSTRAINTS).getObject()) &&
                Objects.equals(existingRefProps.get(GRANT_TYPE).getObject(), toBeAddedRefProps.get(GRANT_TYPE).getObject());
    }

    /**
     * It is possible that a create request specifies duplicate relationships - i.e. a role creation request specifies
     * duplicate managed users, a managed user creation specifies multiple duplicate role references, or multiple, identical
     * relationships are specified when consuming a relationship endpont. References are
     * considered equal if they refer to the same managed object, and have the same grantType and temporalConstraints state.
     * @param relationships the reference set of a particular relationship - e.g. roles or members
     * @throws BadRequestException if there is a duplicate reference detected
     */
    @VisibleForTesting
    void checkForDuplicateRelationships(JsonValue relationships) throws BadRequestException {
        if (relationships.isCollection()) {
            final Set<RelationshipEqualityHash> relationshipSet = new HashSet<>(relationships.size());
            for (JsonValue relationship : relationships) {
                if (!relationshipSet.add(new RelationshipEqualityHash(relationship))) {
                    throw new BadRequestException("Duplicate relationship specified in relationship collection.");
                }
            }
        }
    }

    /**
     * A class which over-rides hash-code (and thus, equals) to examine only those fields which constitute the uniqueness
     * of a relationship so that a HashSet can be used to determine whether duplicate relationships are in a list.
     *
     * Note that checkForDuplicateRelationships will be called when a managed object is patched. In this case, the
     * _refProperties of the existing relationship will have an _id. The relationship specified in the patch will not.
     * Thus it is important that the hash of this class be constituted only by the _ref of the relationship, and the
     * grantType and temporalConstraints of the relationship _refProperties.
     */
    private static final class RelationshipEqualityHash {
        private final JsonValue relationship;
        private final JsonValue relationshipRefProperties;

        private RelationshipEqualityHash(JsonValue relationship) {
            Objects.requireNonNull(relationship, "Provided relationship must be non-null.");
            this.relationship = relationship;
            this.relationshipRefProperties = relationship.get(REFERENCE_PROPERTIES);
        }

        @Override
        public int hashCode() {
            return Objects.hash(relationship.get(REFERENCE_ID).getObject(),
                    relationshipRefProperties.get(TEMPORAL_CONSTRAINTS).getObject(),
                    relationshipRefProperties.get(GRANT_TYPE).getObject());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other instanceof RelationshipEqualityHash) {
                final RelationshipEqualityHash otherHash = (RelationshipEqualityHash)other;
                final JsonValue refProperties = relationship.get(REFERENCE_PROPERTIES);
                final JsonValue otherRefProperties = otherHash.relationship.get(REFERENCE_PROPERTIES);
                return Objects.equals(refProperties.get(TEMPORAL_CONSTRAINTS).getObject(), otherRefProperties.get(TEMPORAL_CONSTRAINTS).getObject())  &&
                        Objects.equals(refProperties.get(GRANT_TYPE).getObject(), otherRefProperties.get(GRANT_TYPE).getObject()) &&
                        Objects.equals(relationship.get(REFERENCE_ID).getObject(), otherHash.relationship.get(REFERENCE_ID).getObject());
            }
            return false;
        }
    }
}
