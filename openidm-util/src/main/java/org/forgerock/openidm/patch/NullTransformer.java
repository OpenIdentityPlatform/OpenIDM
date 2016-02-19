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

package org.forgerock.openidm.patch;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.PatchOperation;

/**
 * A NullTransformer class doesn't transform.
 *
 * Used by callers that do not need the facility to execute transform scripts.
 */
public class NullTransformer implements PatchValueTransformer {
    /** a singleton instance of the NullTransfomer */
    public static final PatchValueTransformer NULL_TRANSFORMER = new NullTransformer();

    private NullTransformer() {
    }

    @Override
    public JsonValue getTransformedValue(PatchOperation patch, JsonValue subject) throws BadRequestException {
        return subject;
    }
}