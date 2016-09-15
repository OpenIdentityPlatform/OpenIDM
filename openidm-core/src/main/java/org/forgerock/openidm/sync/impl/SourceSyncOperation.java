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

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.services.context.Context;

class SourceSyncOperation extends SyncOperation {

    /**
     * A container for the correlation queries or script.
     */
    private final Correlation correlation;

    /**
     * Whether when at the outset of correlation the target set is empty (query all ids returns empty),
     * it should try to correlate source entries to target when necessary.
     * Default to {@code FALSE}
     */
    private final boolean correlateEmptyTargetSet;

    // If it can not uniquely identify a target, the list of ambiguous target ids
    private List<String> ambiguousTargetIds;

    /**
     * Creates source-{@link SyncOperation}.
     *
     * @param context Context
     */
    SourceSyncOperation(ObjectMapping objectMapping, Context context) {
        super(objectMapping, context);
        correlation = new Correlation(objectMapping);
        correlateEmptyTargetSet = objectMapping.getConfig().get("correlateEmptyTargetSet").defaultTo(false).asBoolean();
    }

    @Override
    @SuppressWarnings("fallthrough")
    public JsonValue sync() throws SynchronizationException {
        JsonValue oldTargetValue = json(null);
        try {
            EventEntry measureSituation = Publisher.start(ObjectMapping.EVENT_SOURCE_ASSESS_SITUATION, getSourceObjectId(), null);
            try {
                assessSituation();
            } finally {
                measureSituation.end();
            }
            EventEntry measureDetermine = Publisher.start(ObjectMapping.EVENT_SOURCE_DETERMINE_ACTION, getSourceObjectId(), null);
            boolean linkExisted = (getLinkId() != null);

            try {
                determineAction(getContext());
            } finally {
                measureDetermine.end();

                // Retain oldTargetValue before performing any actions
                if (targetObjectAccessor != null && action != ReconAction.IGNORE) {
                    oldTargetValue = targetObjectAccessor.getObject();
                }
            }
            EventEntry measurePerform = Publisher.start(ObjectMapping.EVENT_SOURCE_PERFORM_ACTION, getSourceObjectId(), null);
            try {
                performAction();
            } finally {
                measurePerform.end();
                if (reconContext != null){
                    // The link ID presence after the action can not be interpreted as an indication if the link has been created
                    reconContext.getStatistics().getSourceStat().processed(getSourceObjectId(), getTargetObjectId(),
                            linkExisted, getLinkId(), linkCreated, situation, action);
                }
            }

            JsonValue syncResult = toJsonValue();
            syncResult.put("oldTargetValue", oldTargetValue != null ? oldTargetValue.getObject() : null);
            return syncResult;
        } catch (SynchronizationException e) {
            JsonValue syncResult = toJsonValue();
            syncResult.put("oldTargetValue", oldTargetValue != null ? oldTargetValue.getObject() : null);
            e.setDetail(syncResult);
            throw e;
        }
    }

    protected boolean isSourceToTarget() {
        return true;
    }

    /**
     * @return all found matching target identifier(s), or a 0 length array if none.
     * More than one target identifier is possible for ambiguous matches
     */
    public String[] getTargetIds() {
        String[] targetIds = null;
        if (ambiguousTargetIds != null) {
            targetIds = ambiguousTargetIds.toArray(new String[ambiguousTargetIds.size()]);
        } else if (getTargetObjectId() != null) {
            targetIds = new String[] { getTargetObjectId() };
        } else {
            targetIds = new String[0];
        }
        return targetIds;
    }

    /**
     * @return the ambiguous target identifier(s), or an empty list if no ambiguous entries are present
     */
    public List<String> getAmbiguousTargetIds() {
        return ambiguousTargetIds;
    }

    private void setAmbiguousTargetIds(JsonValue results) {
        ambiguousTargetIds = new ArrayList<>(results.size());
        for (JsonValue resultValue : results) {
            String anId = resultValue.get("_id").required().asString();
            ambiguousTargetIds.add(anId);
        }
    }

    public void fromJsonValue(JsonValue params) {
        reconId = params.get("reconId").asString();
        sourceObjectAccessor = new LazyObjectAccessor(objectMapping.getConnectionFactory(), objectMapping.getSourceObjectSet(), params.get("sourceId").required().asString());
        ignorePostAction = params.get("ignorePostAction").defaultTo(false).asBoolean();
        linkObject.setLinkQualifier(params.get("linkQualifier").defaultTo(Link.DEFAULT_LINK_QUALIFIER).asString());
    }

    void assessSituation() throws SynchronizationException {
        situation = null;

        // If linking is disabled, set link to null
        if (!objectMapping.isLinkingEnabled()) {
            initializeLink(null);
        }

        // In case the link was not pre-read get it here
        if (getSourceObjectId() != null && linkObject.initialized == false) {
            final long sourceLinkQueryStart = ObjectMapping.startNanoTime(reconContext);
            linkObject.getLinkForSource(getSourceObjectId());
            ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.sourceLinkQuery, sourceLinkQueryStart);
        }

        // If an existing link was found, set the targetObjectAccessor
        if (linkObject._id != null) {
            JsonValue preloaded = null;
            if (reconContext != null) {
                // If there is a pre-loaded target value, use it
                if (reconContext.hasTargetsValues()) {
                    preloaded = reconContext.getTargets().get(linkObject.targetId);
                }
            }
            if (preloaded != null) {
                targetObjectAccessor = new LazyObjectAccessor(
                        objectMapping.getConnectionFactory(), objectMapping.getTargetObjectSet(), linkObject.targetId, preloaded);
            } else {
                targetObjectAccessor = new LazyObjectAccessor(
                        objectMapping.getConnectionFactory(), objectMapping.getTargetObjectSet(), linkObject.targetId);
            }
        }

        if (!hasSourceObject()) {
            /*
            For sync of delete. For recon these are assessed instead in target phase

            no source, link, target & valid target     -  source missing
            no source, link, target & not valid target - target ignored
            no source, link, no target                 - link only
            no source, no link - can't correlate (no previous object available) - all gone
            no source, no link - (correlate)
                                 no target                       - all gone
                                 1 target & valid (source)       - unassigned
                                 1 target & not valid (source)   - target ignored
                                 > 1 target & valid (source)     - ambiguous
                                 > 1 target & not valid (source) - unqualified
             */

            if (linkObject._id != null) {
                if (hasTargetObject()) {
                    if (isTargetValid()) {
                        situation = Situation.SOURCE_MISSING;
                    } else {
                        // target is not valid for this mapping; ignore it
                        situation = Situation.TARGET_IGNORED;
                    }
                } else {
                    situation = Situation.LINK_ONLY;
                }
            } else {
                if (oldValue == null || oldValue.isNull()) {
                    // If there is no previous value known we can not correlate
                    situation = Situation.ALL_GONE;
                } else {
                    // Correlate the old value to potential target(s)
                    JsonValue results = correlateTarget(oldValue);
                    boolean valid = isSourceValid(oldValue);
                    if (results == null || results.size() == 0) {
                        // Results null means no correlation query defined, size 0 we know there is no target
                        situation = Situation.ALL_GONE;
                    } else if (results.size() == 1) {
                        JsonValue resultValue = results.get((Integer) 0).required();
                        targetObjectAccessor = getCorrelatedTarget(resultValue);
                        if (valid) {
                            situation = Situation.UNASSIGNED;
                        } else {
                            // target is not valid for this mapping; ignore it
                            situation = Situation.TARGET_IGNORED;
                        }
                    } else if (results.size() > 1) {
                        if (valid) {
                            // Note this situation is used both when there is a source and a deleted source
                            // with multiple matching targets
                            situation = Situation.AMBIGUOUS;
                        } else {
                            situation = Situation.UNQUALIFIED;
                        }
                    }
                }
            }
        } else if (isSourceValid() && checkSourceConditions(getLinkQualifier())) { // source is valid for mapping
            if (linkObject._id != null) { // source object linked to target
                if (hasTargetObject()) {
                    situation = Situation.CONFIRMED;
                } else {
                    situation = Situation.MISSING;
                }
            } else { // source object not linked to target
                JsonValue results = correlateTarget();
                if (results == null) { // no correlationQuery defined
                    situation = Situation.ABSENT;
                } else if (results.size() == 1) {
                    JsonValue resultValue = results.get(0).required();
                    targetObjectAccessor = getCorrelatedTarget(resultValue);

                    Link checkExistingLink = new Link(objectMapping);
                    checkExistingLink.setLinkQualifier(getLinkQualifier());

                    final long targetLinkQueryStart = ObjectMapping.startNanoTime(reconContext);
                    checkExistingLink.getLinkForTarget(targetObjectAccessor.getLocalId());
                    ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.targetLinkQuery, targetLinkQueryStart);

                    if (checkExistingLink._id == null || checkExistingLink.sourceId == null) {
                        situation = Situation.FOUND;
                    } else {
                        situation = Situation.FOUND_ALREADY_LINKED;
                        // TODO: consider enhancements:
                        // For reporting, should it log existing link and source
                        // What actions should be available for a found, already linked
                    }
                } else if (results.size() == 0) {
                    situation = Situation.ABSENT;
                } else {
                    situation = Situation.AMBIGUOUS;
                    setAmbiguousTargetIds(results);
                }
            }
        } else { // mapping does not qualify for target
            if (linkObject._id != null) {
                situation = Situation.UNQUALIFIED;
            } else {
                JsonValue results = correlateTarget();
                if (results == null || results.size() == 0) {
                    situation = Situation.SOURCE_IGNORED; // source not valid for mapping, and no link or target exist
                } else if (results.size() == 1) {
                    // TODO: Consider if we can optimize out the read for unqualified conditions
                    JsonValue resultValue = results.get((Integer) 0).required();
                    targetObjectAccessor = getCorrelatedTarget(resultValue);
                    situation = Situation.UNQUALIFIED;
                } else if (results.size() > 1) {
                    situation = Situation.UNQUALIFIED;
                    setAmbiguousTargetIds(results);
                }
            }
            if (reconContext != null) {
                reconContext.getStatistics().getSourceStat().addNotValid(getSourceObjectId());
            }
        }
        LOGGER.debug("Mapping '{}' assessed situation of {} to be {}", objectMapping.getName(), getSourceObjectId(), situation);
    }

    /**
     * Correlates (finds an associated) target for the source object
     * @see #correlateTarget(JsonValue)
     *
     * @return JsonValue if found, null if none
     * @throws SynchronizationException if the correlation failed.
     */
    private JsonValue correlateTarget() throws SynchronizationException {
        return correlateTarget(null);
    }

    /**
     * Correlates (finds an associated) target for the given source
     * @param sourceObjectOverride optional explicitly supplied source object to correlate,
     * or null to use the source object associated with the sync operation
     *
     * @return JsonValue if found, null if none
     * @throws SynchronizationException if the correlation failed.
     */
    @SuppressWarnings("unchecked")
    private JsonValue correlateTarget(JsonValue sourceObjectOverride) throws SynchronizationException {
        JsonValue result = null;
        // TODO: consider if there are cases where this would better be lazy and not get the full target
        if (hasTargetObject()) {
            result = json(array(getTargetObject()));
        } else if (correlation.hasCorrelation(getLinkQualifier()) && (correlateEmptyTargetSet || !hadEmptyTargetObjectSet())) {


            EventEntry measure = Publisher.start(ObjectMapping.EVENT_CORRELATE_TARGET, getSourceObject(), null);

            final JsonValue sourceObject = (sourceObjectOverride != null)
                    ? sourceObjectOverride
                    : getSourceObject();
            if (sourceObject == null) {
                throw new SynchronizationException("Source object " + getSourceObjectId() + " no longer exists");
            }
            Map<String, Object> scope = new HashMap<String, Object>();
            scope.put("source", sourceObject.asMap());
            try {
                result = correlation.correlate(scope, getLinkQualifier(), getContext(), reconContext);
            } finally {
                measure.end();
            }
        }
        return result;
    }

    /**
     * Given a result entry from a correlation query get the full correlated target object
     * @param resultValue an entry from the correlation query result list.
     * May already be the full target object, or just contain the id.
     * @return the target object
     * @throws SynchronizationException
     */
    private LazyObjectAccessor getCorrelatedTarget(JsonValue resultValue) throws SynchronizationException {
        // TODO: Optimize to get the entire object with one query if it's sufficient
        LazyObjectAccessor fullObj = null;
        if (hasNonSpecialAttribute(resultValue.keys())) { //Assume this is a full object
            fullObj = new LazyObjectAccessor(objectMapping.getConnectionFactory(), objectMapping.getTargetObjectSet(), resultValue.get("_id").required().asString(), resultValue);


        } else {
            fullObj = new LazyObjectAccessor(objectMapping.getConnectionFactory(), objectMapping.getTargetObjectSet(), resultValue.get("_id").required().asString());
            //fullObj.getObject();
        }
        return fullObj;
    }

    /**
     * Primitive implementation to decide if the object is a "full" or a partial.
     *
     * @param keys attribute names of object
     * @return true if the {@code keys} has value not starting with "_" char
     */
    private boolean hasNonSpecialAttribute(Collection<String> keys) {
        for (String attr : keys) {
            if (!attr.startsWith("_")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JsonValue toJsonValue() throws SynchronizationException {
        JsonValue jsonValue = super.toJsonValue();
        jsonValue.put("ambiguousTargetIds", getAmbiguousTargetIds());
        return jsonValue;
    }
}
