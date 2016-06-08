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

package org.forgerock.openidm.script;

import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.script.ScriptEntry;
import org.forgerock.services.context.Context;

/**
 * This interface provides a method to execute script given script and bindings.
 */
public interface ScriptExecutor {
    /**
     * Return a transformed value.
     *
     * @param context The request context
     * @param script The script
     * @param bindings The bindings
     * @return JsonValue The result of the script execution
     * @throws ResourceException on script execution error
     */
    JsonValue execScript(Context context, ScriptEntry script, Map<String, Object> bindings) throws ResourceException;
}