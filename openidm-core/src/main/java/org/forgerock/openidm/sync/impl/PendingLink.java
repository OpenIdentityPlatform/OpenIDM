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

import org.forgerock.json.resource.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;


/**
 * Pending link handling
 * Helps establish links as soon as target identifiers
 * become known.
 *
 * @author aegloff
 */
class PendingLink {

    private final static Logger logger = LoggerFactory.getLogger(PendingLink.class);

    private static final String PENDING_LINK = "pendingLink";

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
        JsonValue pendingLink = null;
        JsonValue pendingLinkContext = null;
        Context context = ObjectSetContext.get();
//        while ((pendingLink == null || pendingLink.isNull())  && context != null && !context.isNull()) {
//            pendingLink = context.get(PENDING_LINK);
//            if (pendingLink != null && !(pendingLink.isNull())) {
//                pendingLinkContext = context;
//                logger.debug("Pending link found in context {}", pendingLink);
//            }
//            context = context.getParent();
//        }

        // Find right object mapping and create the link
        if (pendingLink != null && !(pendingLink.isNull())) {
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
                    pendingLinkContext.remove(PENDING_LINK);
                    logger.debug("Pending link for mapping {} between {}-{} created", new Object[] {mappingName, sourceId, resourceContainer + "/" + resourceId});
                    break;
                }
            }
        }
    }

    /**
     * Add pending link info to the context
     * @param context to add the pending link info to
     */
    public static void populate(Context context, String objectMappingName, String sourceId, JsonValue sourceObject,
            String reconId, Situation situation) {
        Map<String, Object> pendingLink = new HashMap<String, Object>();
        pendingLink.put(MAPPING_NAME, objectMappingName);
        pendingLink.put(SOURCE_ID, sourceId);
        pendingLink.put(SOURCE_OBJECT, sourceObject);
        pendingLink.put(RECON_ID, reconId);
        pendingLink.put(ORIGINAL_SITUATION, situation.toString());
        //TODO FIXME
        //context.put(PENDING_LINK, pendingLink);
    }

    /**
     * Remove the pending link info from the context because
     * it was processed or the caller is taking responsibility
     * for it.
     *
     * @param context level to remove the pending link from
     */
    public static void clear(Context context) {
        //context.remove(PENDING_LINK);
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
        //TODO FIXME
        boolean wasLinked = false; //!context.isDefined(PENDING_LINK);
        return wasLinked;
    }

}

