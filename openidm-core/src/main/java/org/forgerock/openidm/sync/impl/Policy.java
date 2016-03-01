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
package org.forgerock.openidm.sync.impl;


// Java Standard Edition
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.http.HttpUtils;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.condition.Conditions;
import org.forgerock.openidm.util.Script;
import org.forgerock.openidm.util.Scripts;
import org.forgerock.services.context.Context;
import org.forgerock.openidm.condition.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

/**
 *  Policies to determine an action for a given {@link org.forgerock.openidm.sync.impl.Situation}.
 */
class Policy {

    /**
     * Set up logging for {@link org.forgerock.openidm.sync.impl.Policy}.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(Policy.class);

    /**
     * Synchronization Situation {@link org.forgerock.openidm.sync.impl.Situation}.
     */
    private final Situation situation;

    /**
     * Reconciliation Action {@link org.forgerock.openidm.sync.ReconAction}. 
     */
    private final ReconAction action;

    /**
     * Script for action.
     */
    private final Script script;
    
    /**
     * Script for postAction.
     */
    private Script postAction;

    /**
     * Condition used to determine if policy should be enforced.
     */
    private Condition condition;
    
    /**
     * A Constructor for the Policy class.
     *
     * @param config a {@link JsonValue} object representing the policy configuration.
     */
    public Policy(JsonValue config) {
        situation = config.get("situation").required().asEnum(Situation.class);
        JsonValue action = config.get("action").required();
        condition =  Conditions.newCondition(config.get("condition"));
        if (action.isString()) {
            this.action = action.asEnum(ReconAction.class);
            this.script = null;
        } else {
            this.action = null;
            // Scripts.newInstance will copy the scope variables from action
            this.script = Scripts.newScript(action);
        }
        JsonValue pAction = config.get("postAction");
        if (!pAction.isNull()) {
            this.postAction = Scripts.newScript(pAction);
        }
    }

    /**
     * Returns the condition for this policy.
     *  
     * @return a {@link org.forgerock.openidm.sync.impl.Condition} object representing 
     * * a condition for this policy
     */
    public Condition getCondition() {
        return condition;
    }
    
    /**
     * Returns the situation for this policy.
     * 
     * @return a {@link Situation} object representing the situation for this policy
     */
    public Situation getSituation() {
        return situation;
    }

    /**
     * Returns the action for this policy. The action may be dynamically determined by a script.
     *
     * @param source a {@link LazyObjectAccessor} representing the source object
     * @param target a {@link LazyObjectAccessor} representing the target object
     * @param syncOperation the parent {@link ObjectMapping.SyncOperation} instance
     * @param linkQualifier the linkQualifier for the policy
     * @param context a {@link Context} associated with this call
     * @return a {@link ReconAction} object representing the action for this policy
     * @throws SynchronizationException TODO.
     */
    public ReconAction getAction(LazyObjectAccessor source, 
                                 LazyObjectAccessor target, 
                                 final ObjectMapping.SyncOperation syncOperation, 
                                 String linkQualifier,
                                 Context context) throws SynchronizationException {
        if (action != null) { // static action specified
            return action;
        }
        if (script != null) { // action is dynamically determined
            Map<String, Object> scope = new HashMap<String, Object>();
            Map<String, Object> recon = new HashMap<String, Object>();
            scope.put("recon", recon);
            JsonValue actionParam = syncOperation.toJsonValue();
            actionParam.put(HttpUtils.PARAM_ACTION, "performAction");
            recon.put("actionParam", actionParam.getObject());

            scope.put("sourceAction", (syncOperation instanceof ObjectMapping.SourceSyncOperation));
            scope.put("linkQualifier", linkQualifier);
            if (source != null) {
                scope.put("source", source.asMap());
            }
            if (target != null) {
                scope.put("target", target.asMap());
            }
            try {
                return ReconAction.valueOf(script.exec(scope, context).toString());
            } catch (NullPointerException npe) {
                throw new SynchronizationException("action script returned null value");
            } catch (IllegalArgumentException iae) {
                throw new SynchronizationException("action script returned invalid action");
            } catch (ScriptException se) {
                LOGGER.debug("action script encountered exception", se);
                throw new SynchronizationException(se);
            }
        }
        return ReconAction.IGNORE;
    }

    /**
     * Evaluates a post action script, if present.
     * 
     * @param source a {@link LazyObjectAccessor} representing the source object
     * @param target a {@link LazyObjectAccessor} representing the target object
     * @param action the {@link ReconAction} that was performed
     * @param linkQualifier the linkQualifier               
     * @param sourceAction true if this is a source sync operation, false if it is a target sync operation
     * @param reconId a {@link String} representing the ID of the reconciliation
     * @param context a {@link Context} associated with this call
     * @throws SynchronizationException
     */
    public void evaluatePostAction(LazyObjectAccessor source, 
                                   LazyObjectAccessor target, 
                                   ReconAction action, 
                                   boolean sourceAction, 
                                   String linkQualifier,
                                   String reconId,
                                   Context context) throws SynchronizationException {
        if (postAction != null) {
            Map<String, Object> scope = new HashMap<String, Object>();
            scope.put("linkQualifier", linkQualifier);
            scope.put("sourceAction", sourceAction);
            scope.put("action", action.name());
            scope.put("situation", situation.name());
            scope.put("reconId", reconId);
            if (source != null) {
                scope.put("source", source.asMap());
            }
            if (target != null) {
                scope.put("target", target.asMap());
            }
            try {
                postAction.exec(scope, context);
            } catch (ScriptException se) {
                LOGGER.debug("action script encountered exception", se);
                throw new SynchronizationException(se);
            }
        }
    }
}
