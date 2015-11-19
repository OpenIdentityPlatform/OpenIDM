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

import static org.forgerock.openidm.util.RelationshipUtil.REFERENCE_ID;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;

/**
 * This validates forward only relationships (ie not bi-directional or reverse)
 */
public class ForwardRelationshipValidator extends RelationshipValidator {

    /**
     * Constructs a validator to validate forward only relationships.
     *
     * @param relationshipProvider the provider that owns this validator.
     */
    public ForwardRelationshipValidator(RelationshipProvider relationshipProvider) {
        super(relationshipProvider);
    }

    /**
     * Returns the readrequest that will retrieve the relationship field using the _ref id.
     *
     * @param relationshipField the field to validate/retrieve.
     * @return the constructed read request.
     */
    protected ReadRequest newValidateRequest(JsonValue relationshipField) {
        return Requests.newReadRequest(relationshipField.get(REFERENCE_ID).asString());
    }

    @Override
    protected void validateSuccessfulReadResponse(JsonValue relationshipField, ResourceResponse response) {
        // nothing more to validate. It is valid if the read request passes.
    }
}
