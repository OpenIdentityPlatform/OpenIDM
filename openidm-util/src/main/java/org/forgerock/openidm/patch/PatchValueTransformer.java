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

package org.forgerock.openidm.patch;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.ResourceException;

/**
 * This interface provides a method to retrieve a value to be used in a patch operation
 * based on any criteria using any available means.
 */
public interface PatchValueTransformer {
    /**
     * Return the value to be used for a given patch operation.
     *
     * @param patch The patch operation.
     * @param subject The patch subject document.  Subject is unused by default, made available
     *               for use by custom transforms.
     * @return JsonValue to be used for a given patch operation
     * @return subject on patch null or empty
     * @throws BadRequestException on null subject, null scriptConfig
     * @throws ResourceException on script execution error
     */
    JsonValue getTransformedValue(PatchOperation patch, JsonValue subject) throws ResourceException;
}