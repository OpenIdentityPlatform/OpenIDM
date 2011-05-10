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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.sync;

// Java Standard Edition
import java.util.HashMap;

// JSON-Fluent library
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

// ForgeRock OpenIDM
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.Scripts;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
class Policy {

    /** TODO: Description. */
    private final Situation situation;

    /** TODO: Description. */
    private final Action action;

// TODO: Do we even need a script or did I over-engineer this?
    /** TODO: Description. */
    private final Script script;

    /**
     * TODO: Description.
     *
     * @param config TODO.
     * @throws JsonNodeException TODO>
     */
    public Policy(JsonNode config) throws JsonNodeException {
        situation = config.get("situation").required().asEnum(Situation.class);
        JsonNode action = config.get("action").required();
        if (action.isString()) {
            this.action = action.asEnum(Action.class);
            this.script = null;
        }
        else {
            this.action = null;
            this.script = Scripts.newInstance(action);
        }
    }

    /**
     * TODO: Description.
     */
    public Situation getSituation() {
        return situation;
    }

    /**
     * TODO: Description.
     *
     * @return TODO.
     * @throws SynchronizationException TODO.
     */
    public Action getAction() throws SynchronizationException {
        Action result = null;
        if (action != null) { // static action specified
            result = action;
        }
        else if (script != null) { // action is dynamically determine 
// TODO: review what the scope this script call should be.
            HashMap<String, Object> scope = new HashMap<String, Object>();
            try {
                result = Enum.valueOf(Action.class, script.exec(scope).toString());
            }
            catch (NullPointerException npe) {
                throw new SynchronizationException("action script returned null value");
            }
            catch (IllegalArgumentException iae) {
                throw new SynchronizationException("action script returned invalid action");
            }
            catch (ScriptException se) {
                throw new SynchronizationException(se);
            }
        }
        return result;
    }
}
