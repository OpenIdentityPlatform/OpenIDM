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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.managed;

import org.forgerock.json.resource.PreconditionFailedException;

/**
 * An exception to reject a request as attempting to create a relationship which already exists. Note that an exeception
 * with an error code of 412 (PreconditionFailedException) is necessary to participate in PUT upsert semantics. However,
 * this exception must be distinguished from the PreconditionFailedException because some methods - e.g. ManagedObjectSet#patchResource -
 * will retry operations if a PreconditionFailedException is encountered, as this is documented as indicative of
 * a _rev mismatch.
 */
public class DuplicateRelationshipException extends PreconditionFailedException {
    private static final long serialVersionUID = 1L;
    public DuplicateRelationshipException(String message) {
        super(message);
    }
}
