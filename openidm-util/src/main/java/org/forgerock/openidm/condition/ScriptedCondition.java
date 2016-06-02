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
package org.forgerock.openidm.condition;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openidm.util.Script;
import org.forgerock.services.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Condition implemented as a script.
 */
class ScriptedCondition implements Condition {
    
    /** Logger */
    private final static Logger logger = LoggerFactory.getLogger(Conditions.class);

    /**
     * The condition script
     */
    private final Script script;

    /**
     * Construct the scripted condition from the given script.
     *
     * @param script the script to evaluate
     */
    ScriptedCondition(Script script) {
        this.script = script;
    }

    @Override
    public boolean evaluate(Object content, Context context) throws JsonValueException {
        Map<String, Object> scope = new HashMap<>();
        JsonValue contentValue = new JsonValue(content);
        try {
            if (contentValue.isMap()) {
                scope.putAll(contentValue.asMap());
            }
            Object o = script.exec(scope, context);
            if (o == null || !(o instanceof Boolean) || Boolean.FALSE.equals(o)) {
                return false; // property mapping is not applicable; do not apply
            }
            return true;
        } catch (ScriptException se) {
            logger.warn("Script encountered exception while evaluating condition", se);
            throw new JsonValueException(contentValue, se);
        }
    }
}
