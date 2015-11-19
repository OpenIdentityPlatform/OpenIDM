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
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.util.RelationshipUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for reverse (aka bi-directional) relationships.
 * If the relationship is not found then the relationshipField is invalid, otherwise then the reverse field's
 * property either needs to be null as a singleton or a collection to accept the new relationship link.
 */
public class ReverseRelationshipValidator extends RelationshipValidator {

    private static final Logger logger = LoggerFactory.getLogger(ReverseRelationshipValidator.class);

    /**
     * Constructs the validator to validate reverse relationships.
     *
     * @param relationshipProvider the provider that owns this validator.
     */
    public ReverseRelationshipValidator(RelationshipProvider relationshipProvider) {
        super(relationshipProvider);
    }

    /**
     * The read request will lookup the relationshipField and ask for the reverse property to validate its setting
     * is compatible with the requested relationship field.
     *
     * @param relationshipField the field to validate.
     * @return the request to invoke for validation.
     */
    protected ReadRequest newValidateRequest(JsonValue relationshipField) {
        return Requests.newReadRequest(relationshipField.get(REFERENCE_ID).asString())
                .addField(getRelationshipProvider().getSchemaField().getReversePropertyName());
    }

    /**
     * The reverse field's property either needs to be null as a singleton or a collection to accept the new
     * relationship link.
     *
     * @param relationshipField the field to be validated.
     * @param resourceResponse the response from the validation read request.
     * @throws BadRequestException when the relationship is invalid
     */
    protected void validateSuccessfulReadResponse(JsonValue relationshipField, ResourceResponse resourceResponse)
            throws BadRequestException {
        String reversePropertyName = getRelationshipProvider().getSchemaField().getReversePropertyName();
        JsonValue reverseProperty = resourceResponse.getContent().get(reversePropertyName);
        if (reverseProperty.isNotNull() && !reverseProperty.isCollection()) {
            String ref = relationshipField.get(REFERENCE_ID).asString();
            logger.debug(format(
                    "Reference ''{0}''->''{1}'' is in conflict with existing reverse reference ''{1}/{2}''->''{3}''",
                    relationshipField.getPointer(), ref, reversePropertyName,
                    reverseProperty.get(RelationshipUtil.REFERENCE_ID).asString()));
            // The message is different for the log vs the exception to avoid leaking the reverseProperty's value
            throw new BadRequestException(format(
                    "Reference ''{0}''->''{1}'' is in conflict with existing reverse reference ''{1}/{2}''",
                    relationshipField.getPointer(), ref, reversePropertyName));
        }
    }
}
