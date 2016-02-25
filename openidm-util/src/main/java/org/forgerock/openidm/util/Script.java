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
 * Portions copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.openidm.util;

import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.forgerock.script.ScriptEntry;
import org.forgerock.services.context.Context;

/**
 * This class wraps a {@link ScriptEntry} object representing a script stored in the {@link ScriptRegistry} and 
 * provides a method for executing the script with a given {@link Context} and scope variables. 
 */
public class Script {

    private final ScriptEntry entry;

    /**
     * A constructor.
     * 
     * @param entry a {@link ScriptEntry} object.
     */
    Script(ScriptEntry entry) {
        this.entry = entry;
    }

    /**
     * Executes the script with a given {@link Context} and scope variables.
     * 
     * @param scope a {@link Map} of scope variables.
     * @param context a {@link Context} associated with the script execution.
     * @return an {@link Object} returned from the script execution.
     * @throws ScriptException
     */
    public Object exec(Map<String, Object> scope, Context context) throws ScriptException {
        org.forgerock.script.Script s = entry.getScript(context);
        Bindings b = s.createBindings();
        b.putAll(scope);
        return s.eval(b);
    };

}
