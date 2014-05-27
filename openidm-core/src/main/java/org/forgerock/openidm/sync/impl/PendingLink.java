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
 * Copyright © 2011-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.sync.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.sync.PendingLinkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Pending link handling. 
 * Creates and populates a PendingLinkContext to help establish 
 * links as soon as target identifiers become known.
 *
 * @author aegloff
 * @author ckienle
 */
class PendingLink {

    private final static Logger logger = LoggerFactory.getLogger(PendingLink.class);

    private static final String ORIGINAL_SITUATION = "originalSituation";
    private static final String RECON_ID = "reconId";
    private static final String SOURCE_OBJECT = "sourceObject";
    private static final String SOURCE_ID = "sourceId";
    private static final String MAPPING_NAME = "mappingName";

    /**
     * Detect and handle pending links if present
     *
     * @param mappings all the mappings to scan
     * @param resourceContainer the target identifier path
     * @param resourceId the target identifier to link to
     * @param targetObject the full target object, useful for the onLink script
     * @throws SynchronizationException if linking failed
     */
    public static void handlePendingLinks(ArrayList<ObjectMapping> mappings, String resourceContainer, String resourceId, JsonValue targetObject) throws SynchronizationException {
        // Detect if there is a pending link
        PendingLinkContext pendingLinkContext = null;
        Context context = ObjectSetContext.get();
        try {
            pendingLinkContext = context.asContext(PendingLinkContext.class);
        } catch (IllegalArgumentException e) {
            logger.debug("No PendingLinkContext found");
            return;
        }

        // Find right object mapping and create the link
        if (pendingLinkContext.isPending()) {
            JsonValue pendingLink = new JsonValue (pendingLinkContext.getPendingLink());
            String mappingName = pendingLink.get(MAPPING_NAME).required().asString();
            String sourceId = pendingLink.get(SOURCE_ID).required().asString();
            JsonValue sourceObject = pendingLink.get(SOURCE_OBJECT);
            String reconId = pendingLink.get(RECON_ID).asString();
            String origSituation = pendingLink.get(ORIGINAL_SITUATION).asString();
            Situation situation = Situation.valueOf(origSituation);

            for (ObjectMapping mapping : mappings) {
                if (mapping.getName().equals(mappingName) && resourceContainer.equals(mapping.getTargetObjectSet())) {
                    logger.debug("Matching mapping {} found for pending link to {}", mappingName, resourceContainer + "/" + resourceId);
                    mapping.explicitOp(sourceObject, targetObject, situation, Action.LINK, reconId);
                    pendingLinkContext.clear();
                    logger.debug("Pending link for mapping {} between {}-{} created", new Object[] {mappingName, sourceId, resourceContainer + "/" + resourceId});
                    break;
                }
            }
        }
    }


    /**
     * Creates and populates a PendingLinkContext.
     * 
     * @param context the context to use as the parent context 
     * @param objectMappingName The name of the mapping
     * @param sourceId the source ID
     * @param sourceObject the source Object
     * @param reconId the reconciliation ID
     * @param situation the original situation
     * @return the created PendingLinkContext
     */
    public static ServerContext populate(Context context, String objectMappingName, String sourceId, JsonValue sourceObject, String reconId, Situation situation) {
        // Create the pending link map
        Map<String, Object> pendingLink = new HashMap<String, Object>();
        pendingLink.put(MAPPING_NAME, objectMappingName);
        pendingLink.put(SOURCE_ID, sourceId);
        pendingLink.put(SOURCE_OBJECT, sourceObject);
        pendingLink.put(RECON_ID, reconId);
        pendingLink.put(ORIGINAL_SITUATION, situation.toString());
        // Create new PendingLinkContext with the passed in context as the parent
        PendingLinkContext pendingLinkContext = new PendingLinkContext(context, pendingLink);
        return pendingLinkContext;
    }

    /**
     * Clears a PendingLinkContext if found, removing the pending link data and marking it as no longer pending.
     *
     * @param context the Context to get the PendingLinkContext from
     */
    public static void clear(Context context) {
        try {
            PendingLinkContext pendingLinkContext = context.asContext(PendingLinkContext.class);
            pendingLinkContext.clear();
        } catch (IllegalArgumentException e) {
            logger.debug("No PendingLinkContext found");
        }
    }

    /**
     * Check if the pending link that was in the context
     * actually was linked
     *
     * It's critical that the context passed in
     * originally was populated with the pending link info
     * to get an accurate response as it may use the absence
     * of the pending link info as a marker or the pending link
     * having been processed and removed.
     *
     * @param context the context that originally had the pending link info
     * @return true if the pending link was linked, false if it still needs linking
     */
    public static boolean wasLinked(Context context) {
        try {
            PendingLinkContext pendingLinkContext = context.asContext(PendingLinkContext.class);
            return !pendingLinkContext.isPending();
        } catch (IllegalArgumentException e) {
            logger.debug("No PendingLinkContext found");
            return false;
        }
    }

}

