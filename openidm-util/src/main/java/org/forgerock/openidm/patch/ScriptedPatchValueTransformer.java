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

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.ResourceException;

/**
 * An abstract implementation for scripted transforms.
 */
public abstract class ScriptedPatchValueTransformer implements PatchValueTransformer {
    private static final String CONFIG_SCRIPT = "script";

    @Override
    public JsonValue getTransformedValue(PatchOperation patch, JsonValue subject) throws ResourceException {
        if (patch.getValue().get(CONFIG_SCRIPT).isNotNull()) {
            return evalScript(subject, patch.getValue().get(CONFIG_SCRIPT), patch.getField());
        }
        throw new BadRequestException("Expecting a " + CONFIG_SCRIPT + " member");
    }

    /**
     * Given subject and script
     * Returns a transformed value.
     *
     * @param subject The JsonValue to which to apply the patch operation(s).
     * @param scriptConfig The script config.
     * @param field The script field.
     * @return A transformed JsonValue.
     * @throws BadRequestException on null subject, null scriptConfig
     * @throws ResourceException on script execution error
     */
    public abstract JsonValue evalScript(JsonValue subject, JsonValue scriptConfig, JsonPointer field) throws ResourceException;
}