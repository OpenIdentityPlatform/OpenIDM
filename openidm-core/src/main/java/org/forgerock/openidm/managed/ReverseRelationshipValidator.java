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

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
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
     * @param referrerId the id of the object 'hosting' the relationships, aka the referrer; used to check whether
     *                          the referred-to object specified by the relationship already contains a reference to this referrer
     * @param resourceResponse the response from the validation read request. Contains the reverse-property state of the entity referred-to
     *                         by the relationship. Thus if the request adding a new role to a user (user is the resource/managed-object),
     *                         the relationshipField will define the
     *                         _ref to the role, and the resourceResponse will contain the user's role state. Likewise,
     *                         if the request is adding a new user to a role (role is the resource/managed-object),
     *                         the relationshipField will define the _ref to the user, and the resourceResponse
     *                         will contain the role's member state.
     * @throws BadRequestException when the relationship is invalid
     */
    protected void validateSuccessfulReadResponse(JsonValue relationshipField, ResourcePath referrerId, ResourceResponse resourceResponse)
            throws BadRequestException {
        String reversePropertyName = getRelationshipProvider().getSchemaField().getReversePropertyName();
        JsonValue reversePropertyState = resourceResponse.getContent().get(reversePropertyName);
        if (reversePropertyState.isCollection()) {
            for (JsonValue relationship : reversePropertyState) {
                if (relationship.get(REFERENCE_ID).asString().equals(referrerId.toString()) &&
                        refPropStateEqual(relationship.get(REFERENCE_PROPERTIES), relationshipField.get(REFERENCE_PROPERTIES))) {
                    throw new BadRequestException(format(
                            "Managed object ''{0}'' has already been assigned relationship ''{1}''.",
                            referrerId, relationshipField.get(REFERENCE_ID).asString()));

                }
            }
        // Ensure that a singleton relationship is available for assignment
        } else if (reversePropertyState.isNotNull()) {
            throw new BadRequestException(format(
                    "In relationship endpoint ''{0}'', field ''{1}'' of managed object ''{2}'' is neither null nor a collection, " +
                            "and thus not available for assignment.",
                    getRelationshipProvider().resourceContainer.toString() + "/" + getRelationshipProvider().schemaField.getName(),
                    getRelationshipProvider().schemaField.getReversePropertyName(),
                    resourceResponse.getId()));
        }
    }
}
