/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.internal.sync;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.exception.ScriptThrownException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Description.
 * 
 * @author Paul C. Bryan
 */
class Policy {

    /**
     * Setup logging for the {@link Policy}.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(Policy.class);

    /** TODO: Description. */
    private final Situation situation;

    /** TODO: Description. */
    private final Action action;

    /** TODO: Description. */
    private final ScriptEntry script;

    /** TODO: Description. */
    private final ScriptEntry postAction;

    /**
     * TODO: Description.
     * 
     * @param service
     * @param config
     *            TODO.
     * @throws JsonValueException
     *             TODO.
     */
    public Policy(ScriptRegistry service, JsonValue config) throws JsonValueException,
            ScriptException {
        situation = config.get("situation").required().asEnum(Situation.class);
        JsonValue action = config.get("action").required();
        if (action.isString()) {
            this.action = action.asEnum(Action.class);
            this.script = null;
        } else {
            this.action = null;
            this.script = service.takeScript(action);
        }
        JsonValue pAction = config.get("postAction");
        if (pAction.isNull()) {
            this.postAction = null;
        } else {
            this.postAction = service.takeScript(pAction);
        }
    }

    /**
     * TODO: Description.
     * 
     * @return
     */
    public Situation getSituation() {
        return situation;
    }

    /**
     * TODO: Description.
     * 
     * @param source
     * @param target
     * @param syncOperation
     *            the parent {@link ObjectMapping.SyncOperation} instance
     * @return TODO.
     * @throws ResourceException
     *             TODO.
     */
    public Action getAction(final Context context, final Resource source, final Resource target,
            final ObjectMapping.SyncOperation syncOperation) throws ResourceException {
        Action result = null;
        if (action != null) { // static action specified
            result = action;
        } else if (script != null) { // action is dynamically determine
            Script scope = script.getScript(context);

            Map<String, Object> recon = new HashMap<String, Object>();
            scope.put("recon", recon);
            JsonValue actionParam = null;
             actionParam = ((ObjectMapping.SourceSyncOperation) syncOperation).toJsonValue();

            if (null != actionParam) {
                actionParam.put(ActionRequest.FIELD_ACTION, "performAction");
                recon.put("actionParam", actionParam.getObject());
            }

            scope.put("sourceAction", (syncOperation instanceof ObjectMapping.SourceSyncOperation));
            if (source != null) {
                scope.put("source", source.getContent().asMap());
            }
            if (target != null) {
                scope.put("target", target.getContent().asMap());
            }
            try {
                Object o = scope.eval();
                if (o instanceof String) {
                    return new JsonValue(o).asEnum(Action.class);
                } else {
                    throw new InternalServerErrorException("action script returned invalid value");
                }
            } catch (JsonValueException e) {
                throw new InternalServerErrorException("action script returned invalid action"
                        + e.getMessage(), e);
            } catch (ScriptThrownException e) {
                LOGGER.debug("action script encountered exception", e);
                throw e.toResourceException(ResourceException.INTERNAL_ERROR, e.getMessage());
            } catch (ScriptException se) {
                LOGGER.debug("action script encountered exception", se);
                throw new InternalServerErrorException(se);
            } catch (Exception e) {
                throw new InternalServerErrorException(e.getMessage(), e);
            }
        }
        return result;
    }

    public void evaluatePostAction(final Context context, final Resource source,
            final Resource target, Action action, boolean sourceAction) throws ResourceException {
        if (postAction != null) {
            Script scope = postAction.getScript(context);
            scope.put("sourceAction", sourceAction);
            scope.put("action", action.name());
            scope.put("situation", situation.name());
            if (source != null) {
                scope.put("source", source.getContent().asMap());
            }
            if (target != null) {
                scope.put("target", target.getContent().asMap());
            }
            try {
                scope.eval();
            } catch (ScriptThrownException e) {
                LOGGER.debug("action script encountered exception", e);
                throw e.toResourceException(ResourceException.INTERNAL_ERROR, e.getMessage());
            } catch (ScriptException se) {
                LOGGER.debug("action script encountered exception", se);
                throw new InternalServerErrorException(se.getMessage(), se);
            }
        }
    }
}
