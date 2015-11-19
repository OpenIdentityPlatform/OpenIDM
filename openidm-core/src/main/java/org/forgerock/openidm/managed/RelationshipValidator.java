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

import static java.text.MessageFormat.format;
import static org.forgerock.openidm.util.RelationshipUtil.REFERENCE_ID;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for Validators of relationship requests.  When a relationship action is determined to be invalid, the
 * validateRelationship method will throw a BadRequestException with a message indicating the details of the reason for
 * it being invalid.
 */
abstract class RelationshipValidator {

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
     * @param response The contents will hold the response from the read call made on the relationship field.
     * @throws BadRequestException if the response is invalid based on the implementation of the validator.
     */
    abstract void validateSuccessfulReadResponse(JsonValue relationshipField, ResourceResponse response)
            throws BadRequestException;

    /**
     * Validates that the relationshipField will not create an invalid condition.
     * Does a read on the relationshipField.
     *
     * @param relationshipField the field to be validated.
     * @param context context of the request working with the relationship.
     * @throws ResourceException BadRequestException when the relationship is invalid, otherwise for other issues.
     */
    final void validateRelationship(final JsonValue relationshipField, Context context)
            throws ResourceException {
        if (relationshipField.isCollection()) {
            String message = format("''{0}'' is invalid as a relationship reference", relationshipField.asCollection());
            logger.debug(message);
            throw new BadRequestException(message);
        }
        try {
            validateSuccessfulReadResponse(relationshipField, relationshipProvider.getConnection()
                    .read(context, newValidateRequest(relationshipField)));
        } catch (NotFoundException e) {
            String message = format("The referenced relationship ''{0}'' on ''{1}'', does not exist",
                    relationshipField.get(REFERENCE_ID).asString(), relationshipField.getPointer());
            logger.debug(message);
            throw new BadRequestException(message, e);
        }
    }
}
