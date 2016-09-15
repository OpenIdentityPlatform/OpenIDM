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

import static org.forgerock.json.JsonValue.*;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.services.context.Context;

class TargetSyncOperation extends SyncOperation {

    /**
     * Creates target-{@link SyncOperation}.
     *
     * @param context Context
     */
    TargetSyncOperation(ObjectMapping objectMapping, Context context) {
        super(objectMapping, context);
    }

    @Override
    public JsonValue sync() throws SynchronizationException {
        try {
            EventEntry measureSituation = Publisher.start(ObjectMapping.EVENT_TARGET_ASSESS_SITUATION, targetObjectAccessor, null);
            try {
                assessSituation();
            } finally {
                measureSituation.end();
            }
            boolean linkExisted = (getLinkId() != null);

            EventEntry measureDetermine = Publisher.start(ObjectMapping.EVENT_TARGET_DETERMINE_ACTION, targetObjectAccessor, null);
            try {
                determineAction(getContext());
            } finally {
                measureDetermine.end();
            }
            EventEntry measurePerform = Publisher.start(ObjectMapping.EVENT_TARGET_PERFORM_ACTION, targetObjectAccessor, null);
            try {
                // TODO: Option here to just report what action would be performed?
                performAction();
            } finally {
                measurePerform.end();
                if (reconContext != null) {
                    reconContext.getStatistics().getTargetStat().processed(getSourceObjectId(), getTargetObjectId(),
                            linkExisted, getLinkId(), linkCreated, situation, action);
                }
            }
            return toJsonValue();
        } catch (SynchronizationException e) {
            e.setDetail(toJsonValue());
            throw e;
        }
    }

    protected boolean isSourceToTarget() {
        return false;
    }

    public void fromJsonValue(JsonValue params) {
        reconId = params.get("reconId").asString();
        ignorePostAction = params.get("ignorePostAction").defaultTo(false).asBoolean();
        linkObject.setLinkQualifier(params.get("linkQualifier").defaultTo(Link.DEFAULT_LINK_QUALIFIER).asString());
    }

    public JsonValue toJsonValue() throws SynchronizationException {
        return json(object(
                field("reconId", reconId),
                field("mapping", objectMapping.getName()),
                field("situation", situation!=null ? situation.name() : null),
                field("action", action !=null ? action.name() : null),
                field("target", "true"),
                field("oldSource", getSourceObject()),
                field("linkQualifier", linkObject.linkQualifier),
                (targetObjectAccessor != null && targetObjectAccessor.getLocalId() != null)
                    ? field("targetId", targetObjectAccessor.getLocalId())
                    : null));
    }

    void assessSituation() throws SynchronizationException {
        situation = null;
        String targetId = getTargetObjectId();

        // May want to consider an optimization to not query
        // if we don't need the link for the TARGET_IGNORED action
        if (targetId != null) {
            final long targetLinkQueryStart = ObjectMapping.startNanoTime(reconContext);
            linkObject.getLinkForTarget(targetId);
            ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.targetLinkQuery, targetLinkQueryStart);
        }

        if (!isTargetValid()) { // target is not valid for this mapping; ignore it
            situation = Situation.TARGET_IGNORED;
            if (reconContext != null && targetId != null) {
                reconContext.getStatistics().getTargetStat().addNotValid(targetId);
            }
            return;
        }
        if (linkObject._id == null || linkObject.sourceId == null) {
            situation = Situation.UNASSIGNED;
        } else {
            sourceObjectAccessor = new LazyObjectAccessor(objectMapping.getConnectionFactory(), objectMapping.getSourceObjectSet(), linkObject.sourceId);
            if (getSourceObject() == null) { // force load to double check
                situation = Situation.SOURCE_MISSING;
            } else if (!isSourceValid()) {
                situation = Situation.UNQUALIFIED; // Should happen rarely done in source phase
                LOGGER.info("Situation in target reconciliation that indicates source may have changed {} {} {} {}",
                        situation, getSourceObject(), targetId, linkObject);
            } else { // proper link
                situation = Situation.CONFIRMED; // Should happen rarely as done in source phase
                LOGGER.info("Situation in target reconciliation that indicates source may have changed {} {} {} {}",
                        situation, getSourceObject(), targetId, linkObject);
            }
        }
    }
}
