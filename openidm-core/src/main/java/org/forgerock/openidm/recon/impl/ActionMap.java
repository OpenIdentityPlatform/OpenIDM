/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.recon.impl;

import java.util.Map;
import java.util.HashMap;

import org.forgerock.openidm.action.ActionException;
import org.forgerock.openidm.action.ActionFactory;
import org.forgerock.openidm.action.impl.ActionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.openidm.action.Action;

/**
 * A utility class to map {@link org.forgerock.openidm.action.Action}s returned from {@code scripts} that were executed
 * for a given {@link org.forgerock.openidm.recon.Situation} .
 */
public class ActionMap {

    private static final long serialVersionUID = 1L;
    final static Logger logger = LoggerFactory.getLogger(ActionMap.class);

    private Map<String, Action> actionMapping;

    private ActionFactory actionFactory = new ActionFactoryImpl();

    /**
     * Construct a {@link org.forgerock.openidm.script.Script} string result to an
     * {@link org.forgerock.openidm.action.Action} mapping, for
     * actions that should be fired determined by the resulting {@link org.forgerock.openidm.recon.Situation}.
     *
     * @param actionMappingConfig
     */
    public ActionMap(Map<String, Object> actionMappingConfig) {
        actionMapping = buildMapForActions(actionMappingConfig);
    }

    /**
     * Convert the configuration to appropriate mappings. Class.forName is done
     * on the action value and the action name is used for the key.
     *
     * @param actionMappingConfig
     * @return anActionMapping
     */
    private Map<String, Action> buildMapForActions(Map<String, Object> actionMappingConfig) {
        Map<String, Action> actionMap = new HashMap<String, Action>();
        for (Map.Entry<String, Object> entry : actionMappingConfig.entrySet()) {
            try {
                Action action = actionFactory.newInstance((String) entry.getValue());
                actionMap.put(entry.getKey(), action);
            } catch (ActionException aex) {
                logger.error("Class for Action implementation was not found {}", entry.getValue());
            }
        }
        return actionMap;
    }

    /**
     * For the given {@link org.forgerock.openidm.script.Script} result, return the
     * {@link org.forgerock.openidm.action.Action} that needs to be executed.
     *
     * @param scriptResultKey for the {@link org.forgerock.openidm.action.Action} that needs to be applied.
     * @return anAction to execute
     */
    public Action getActionFor(String scriptResultKey) {
        return actionMapping.get(scriptResultKey);
    }

    /**
     * Debugging toString
     *
     * @return nested toString values
     */
    @Override
    public String toString() {
        return "ActionMap{" +
                "actionMapping=" + actionMapping +
                '}';
    }
}