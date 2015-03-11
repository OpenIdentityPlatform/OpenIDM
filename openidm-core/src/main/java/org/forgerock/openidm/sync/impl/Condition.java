/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openidm.sync.impl;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openidm.sync.impl.Scripts.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a condition on which a property mapping may be applied, or a policy may be enforced.
 */
class Condition {
    
    /** 
     * Logger 
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(Condition.class);
    
    /**
     * The types of conditions.
     */
    private enum Type {
        /**
         * A condition evaluated by a script.
         */
        SCRIPTED,
        /**
         * A condition evaluated by a matching "linkQualifier".
         */
        LINK_QUALIFIER,
        /**
         * A condition which always passes. This is used if a null configuration is passed in.
         */
        TRUE
    }

    /**
     * This condition's type
     */
    private Type type;
    
    /**
     * The link qualifier if configured
     */
    private String linkQualifier;
    
    /**
     * The condition script if configured
     */
    private Script script;
    
    /**
     * The constructor.
     * 
     * @param config the condition configuration
     */
    public Condition(JsonValue config) {
        if (config.isNull()) {
            init(Type.TRUE, null, null);
        } else if (config.asMap().containsKey("linkQualifier")) {
            init(Type.LINK_QUALIFIER, config.get("linkQualifier").asString(), null);
        } else {
            init(Type.SCRIPTED, null, Scripts.newInstance(config));
        }
    }
    
    /**
     * Initializes the condition fields.
     * 
     * @param type the conditions type.
     * @param linkQualifier the link qualifier.
     * @param script the condition script.
     */
    private void init(Type type, String linkQualifier, Script script) {
        this.type = type;
        this.linkQualifier = linkQualifier;
        this.script = script;
    }
    
    /**
     * Evaluates the condition.  Returns true if the condition is met, false otherwise.
     * 
     * @param params parameters to use during evaluation.
     * @param linkQualifier the link qualifier associated with the current sync
     * @return true if the condition is met, false otherwise.
     * @throws SynchronizationException if errors are encountered.
     */
    public boolean evaluate(JsonValue params, String linkQualifier) 
            throws SynchronizationException {
        switch (type) {
        case TRUE:
            return true;
        case LINK_QUALIFIER:
            return this.linkQualifier.equals(linkQualifier);
        case SCRIPTED:
            Map<String, Object> scope = new HashMap<String, Object>();
            try {
                if (params.isMap()) {
                    scope.putAll(params.asMap());
                }
                Object o = script.exec(scope);
                if (o == null || !(o instanceof Boolean) || Boolean.FALSE.equals(o)) {
                    return false; // property mapping is not applicable; do not apply
                }
                return true;
            } catch (JsonValueException jve) {
                LOGGER.warn("Unexpected JSON value exception while evaluating condition", jve);
                throw new SynchronizationException(jve);
            } catch (ScriptException se) {
                LOGGER.warn("Script encountered exception while evaluating condition", se);
                throw new SynchronizationException(se);
            }
        default:
            return false;
        }
    }
}
