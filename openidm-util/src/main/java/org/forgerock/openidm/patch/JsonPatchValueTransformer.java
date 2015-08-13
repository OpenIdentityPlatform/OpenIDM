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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.patch;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;

/**
 * RFC6902 expects the patch value to be a predetermined, static value to be used in the
 * patch operation's execution.  This class provides a means to extend that behavior to
 * allow for values that require transformation based on the patch target document.
 *
 * This impl is the default used by {@link JsonPatch} but may be overridden by calling
 * JsonPatch.patch(JsonValue original, JsonValue patch, JsonPatchValueTransform custom).
 */
public interface JsonPatchValueTransformer {
    /**
     * Return the value to be used for a given patch operation.
     *
     * @param target the patch target document.  Target is unused by default, made available
     *               for use by custom transforms.
     * @param op the patch operation.
     * @return The value from target pointed to by op, transformed as required by the implementation.
     * @throws JsonValueException when the value cannot be located in the target.
     */
    Object getTransformedValue(JsonValue target, JsonValue op);
}

