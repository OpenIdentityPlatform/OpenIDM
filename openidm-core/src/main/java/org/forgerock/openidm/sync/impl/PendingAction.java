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
 * Portions copyright 2011-2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.sync.PendingActionContext;
import org.forgerock.openidm.sync.ReconAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Pending action handling. Creates and populates a PendingActionContext to 
 * help perform actions as soon as target identifiers become known.
 *
 */
class PendingAction {

    private final static Logger logger = LoggerFactory.getLogger(PendingAction.class);

    private static final String ORIGINAL_SITUATION = "originalSituation";
    private static final String RECON_ID = "reconId";
    private static final String SOURCE_OBJECT = "sourceObject";
    private static final String MAPPING_NAME = "mappingName";

    /**
     * Detect and handle pending actions if present
     *
     * @param mappings all the mappings to scan
     * @param resourceContainer the target identifier path
     * @param resourceId the target identifier
     * @param targetObject the full target object
     * @throws SynchronizationException if the action failed
     */
    public static void handlePendingActions(Context context, ReconAction action, ArrayList<ObjectMapping> mappings,
            String resourceContainer, String resourceId, JsonValue targetObject) throws SynchronizationException {
        // Detect if there is a pending action matching the supplied action
        PendingActionContext pendingActionContext = null;
        try {
            pendingActionContext = getPendingActionContext(context, action);
        } catch (IllegalArgumentException e) {
            logger.debug("No PendingActionContext found");
            return;
        }

        // Find right object mapping and perform the action
        if (pendingActionContext.isPending()) {
            JsonValue pendingAction = new JsonValue (pendingActionContext.getPendingActionData());
            String mappingName = pendingAction.get(MAPPING_NAME).required().asString();
            JsonValue sourceObject = pendingAction.get(SOURCE_OBJECT);
            String reconId = pendingAction.get(RECON_ID).asString();
            String origSituation = pendingAction.get(ORIGINAL_SITUATION).asString();
            Situation situation = Situation.valueOf(origSituation);

            for (ObjectMapping mapping : mappings) {
                if (mapping.getName().equals(mappingName) && resourceContainer.equals(mapping.getTargetObjectSet())) {
                    logger.debug("Matching mapping {} found for pending action {} to {}", mappingName, action.toString(), 
                            resourceContainer + "/" + resourceId);
                    mapping.explicitOp(sourceObject, targetObject, situation, action, reconId);
                    pendingActionContext.clear();
                    logger.debug("Pending action {} for mapping {} on resource {} performed", 
                            new Object[] {action.toString(), mappingName, resourceContainer + "/" + resourceId});
                    break;
                }
            }
        }
    }


    /**
     * Creates and populates a PendingActionContext.
     * 
     * @param context the context to use as the parent context 
     * @param action the pending action
     * @param mappingName The name of the object mapping
     * @param sourceId the source ID
     * @param sourceObject the source Object
     * @param reconId the reconciliation ID
     * @param situation the original situation
     * @return the created PendingActionContext
     */
    public static Context createPendingActionContext(Context context, ReconAction action, String mappingName,
            JsonValue sourceObject, String reconId, Situation situation) {
        // Create the pending action data map
        Map<String, Object> pendingActionMap = new HashMap<String, Object>();
        pendingActionMap.put(MAPPING_NAME, mappingName);
        pendingActionMap.put(SOURCE_OBJECT, sourceObject);
        pendingActionMap.put(RECON_ID, reconId);
        pendingActionMap.put(ORIGINAL_SITUATION, situation.toString());
        // Create new PendingActionContext with the passed in context as the parent
        PendingActionContext pendingActionContext = new PendingActionContext(context, pendingActionMap, action.toString());
        return pendingActionContext;
    }

    /**
     * Clears a PendingActionContext if found, removing the pending action data and marking it as no longer pending.
     *
     * @param context the Context to get the PendingActionContext from
     */
    public static void clear(Context context) {
        try {
            PendingActionContext pendingActionContext = context.asContext(PendingActionContext.class);
            pendingActionContext.clear();
        } catch (IllegalArgumentException e) {
            logger.debug("No PendingActionContext found");
        }
    }

    /**
     * Search for a PendingActionContext matching the supplied action and check if the action was performed
     *
     * It's critical that the context passed in originally was populated with the pending action data to get 
     * an accurate response as it may use the absence of the pending action data as a marker or the pending 
     * action having been performed and removed.
     *
     * @param context the context chain that originally contained the PendingActionContext
     * @param action the pending action
     * @return true if the pending action was performed, false otherwise
     */
    public static boolean wasPerformed(Context context, ReconAction action) {
        try {
            PendingActionContext pendingActionContext = getPendingActionContext(context, action);
            // Check if this PendingActionContext matches the supplied action
            if (pendingActionContext.getPendingAction().equals(action.toString())) {
                return !pendingActionContext.isPending();
            }
        } catch (IllegalArgumentException e) {
            logger.debug("No PendingActionContext found");
        }
        return false;
    }

    /**
     * Search for a PendingActionContext matching the supplied action.
     * 
     * @param context the context chain that may contain the PendingActionContext
     * @param action the pending action
     * @return the PendingActionContext associated with the supplied action.
     * @throws IllegalArgumentException
     */
    public static PendingActionContext getPendingActionContext(Context context, ReconAction action)
            throws IllegalArgumentException {
        Context currentContext = context;
        while (currentContext != null) {
            PendingActionContext pendingActionContext = context.asContext(PendingActionContext.class);
            // Check if this PendingActionContext matches the supplied action
            if (pendingActionContext.getPendingAction().equals(action.toString())) {
                return pendingActionContext;
            }
            currentContext = pendingActionContext.getParent();
        }
        // Should never get here. If we do, then throw a IllegalArguementContext
        throw new IllegalArgumentException("No PendingActionContext found");
    }
}

