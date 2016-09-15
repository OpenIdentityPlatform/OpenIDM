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

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.services.context.Context;

/**
 * Explicit execution of a sync operation where the appropriate
 * action is known without having to assess the situation and apply
 * policy to decide the action
 */
class ExplicitSyncOperation extends SyncOperation {

    ExplicitSyncOperation(ObjectMapping objectMapping, Context context) {
        super(objectMapping, context);
    }

    protected boolean isSourceToTarget() {
        //TODO: detect by the source id match
        return true;
    }

    public void init(JsonValue sourceObject, JsonValue targetObject, Situation situation, ReconAction action, String reconId) {
        String sourceObjectId = getObjectId(sourceObject);
        String targetObjectId = getObjectId(targetObject);
        this.sourceObjectAccessor = new LazyObjectAccessor(
                objectMapping.getConnectionFactory(), objectMapping.getSourceObjectSet(), sourceObjectId, sourceObject);
        this.targetObjectAccessor = new LazyObjectAccessor(
                objectMapping.getConnectionFactory(), objectMapping.getTargetObjectSet(), targetObjectId, targetObject);
        this.reconId = reconId;
        this.situation = situation;
        this.action = action;
        this.ignorePostAction = true;

        switch (action) {
        case UNLINK:
            try {
                if (sourceObjectId != null) {
                    linkObject.getLinkForSource(sourceObjectId);
                } else if (targetObjectId != null) {
                    linkObject.getLinkForTarget(targetObjectId);
                }
            } catch (SynchronizationException e) {
                LOGGER.debug("Unable to find link for explicit sync operation UNLINK");
            }
            break;
        default:
            break;
        }
    }

    @Override
    public JsonValue sync() throws SynchronizationException {
        LOGGER.debug("Initiate explicit operation call for situation: {}, action: {}", situation, action);
        performAction();
        LOGGER.debug("Complected explicit operation call for situation: {}, action: {}", situation, action);
        return toJsonValue();
    }

    /**
     * Returns an object's required ID or null if the object is null.
     *
     * @param object a source or target object
     * @return a String representing the object's ID
     */
    private String getObjectId(JsonValue object) {
        return object != null && !object.isNull()
                ? object.get("_id").required().asString()
                : null;
    }
}
