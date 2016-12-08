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
import static org.forgerock.json.resource.Requests.*;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.condition.Condition;
import org.forgerock.openidm.condition.Conditions;
import org.forgerock.openidm.config.enhanced.InternalErrorException;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.sync.PropertyMapping;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.sync.SyncContext;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.util.Script;
import org.forgerock.openidm.util.Scripts;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.script.source.SourceUnit;
import org.forgerock.services.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class SyncOperation {

    protected static final Logger LOGGER = LoggerFactory.getLogger(SyncOperation.class);

    protected final ObjectMapping objectMapping;

    /** a script to execute when a target object is to be created */
    private final Script onCreateScript;

    /** a script to execute when a target object is to be updated */
    private final Script onUpdateScript;

    /** a script to execute when a target object is to be deleted */
    private final Script onDeleteScript;

    /** a script to execute when a source object is to be linked to a target object */
    private final Script onLinkScript;

    /** a script to execute when a source object and a target object are to be unlinked */
    private final Script onUnlinkScript;

    /** a script to execute when sync has been performed on managed/user object */
    private final Script postMapping;

    /** a script that determines if a source object is valid to be mapped */
    private final Script validSource;

    /** a script that determines if a target object is valid to be mapped */
    private final Script validTarget;

    /** a script that applies the effective assignments as part of the mapping */
    private Script defaultMapping;

    /** an additional set of key-value conditions to be met for a source object to be valid to be mapped */
    private final Condition sourceCondition;

    /** an array of property-mapping objects */
    private final List<PropertyMapping> properties = new ArrayList<>();

    /** a map of {@link Policy} objects */
    private Map<String, List<Policy>> policies = new HashMap<>();

    /**
     * A reconciliation ID
     */
    String reconId;

    /**
     * The request context of this sync operation
     */
    private Context context;

    /**
     * An optional reconciliation context
     */
    ReconciliationContext reconContext;

    /**
     * Access to the source object
     */
    LazyObjectAccessor sourceObjectAccessor;

    /**
     * Access to the target object
     */
    LazyObjectAccessor targetObjectAccessor;

    /**
     * Optional value of the source object before the change that triggered this sync, or null if not supplied
     */
    JsonValue oldValue;

    /**
     * Holds the link representation
     * An initialized link can be interpreted as representing state retrieved from the repository,
     * i.e. a linkObject with id of null represents a link that does not exist (yet)
     */
    Link linkObject;

    /**
     * Indicates whether the link was created during this operation.
     * (linkObject above may not be set for newly created links)
     */
    boolean linkCreated;

    /**
     * The current sync operation's situation
     */
    Situation situation;

    /**
     * The current sync operation's action
     */
    ReconAction action;

    /**
     * The current sync operation's active policy
     */
    private Policy activePolicy;

    /**
     * A boolean indicating whether to ignore any configured post action.
     */
    boolean ignorePostAction;

    public SyncOperation(ObjectMapping objectMapping, Context context) {
        this.objectMapping = objectMapping;
        this.context = new SyncContext(context, objectMapping.getName());
        linkObject = new Link(objectMapping);

        final JsonValue config = objectMapping.getConfig();
        validSource = Scripts.newScript(config.get("validSource"));
        validTarget = Scripts.newScript(config.get("validTarget"));
        sourceCondition = Conditions.newCondition(config.get("sourceCondition"));
        onCreateScript = Scripts.newScript(config.get("onCreate"));
        onUpdateScript = Scripts.newScript(config.get("onUpdate"));
        onDeleteScript = Scripts.newScript(config.get("onDelete"));
        onLinkScript = Scripts.newScript(config.get("onLink"));
        onUnlinkScript = Scripts.newScript(config.get("onUnlink"));
        defaultMapping = Scripts.newScript(config.get("defaultMapping").defaultTo(
                json(object(field(SourceUnit.ATTR_TYPE, "text/javascript"),
                        field(SourceUnit.ATTR_NAME, "roles/defaultMapping.js")))));
        postMapping = Scripts.newScript(config.get("postMapping").defaultTo(
                json(object(field(SourceUnit.ATTR_TYPE, "groovy"),
                        field(SourceUnit.ATTR_NAME, "roles/defaultPostMapping.groovy")))));

        for (JsonValue jv : config.get("properties").expect(List.class)) {
            properties.add(new PropertyMapping(jv));
        }

        for (JsonValue jv : config.get("policies").expect(List.class)) {
            String situation = jv.get("situation").asString();
            if (policies.containsKey(situation)) {
                List<Policy> policy = policies.get(situation);
                policy.add(new Policy(jv));
                continue;
            }
            List<Policy> policyArrayList = new ArrayList<Policy>();
            policies.put(situation, policyArrayList);
            policyArrayList.add(new Policy(jv));
        }
    }

    /**
     * Performs the sync operation.
     *
     * @return sync results of the {@link SyncOperation}
     * @throws SynchronizationException TODO.
     */
    public abstract JsonValue sync() throws SynchronizationException;

    protected abstract boolean isSourceToTarget();

    /**
     * Sets the link qualifier for the current sync operation.
     *
     * @param linkQualifier a link qualifier.
     */
    protected void setLinkQualifier(String linkQualifier) {
        this.linkObject.setLinkQualifier(linkQualifier);
    }

    /**
     * @return the source object, potentially loaded on demand and/or cached, or null if does not exist
     * @throws SynchronizationException if on-demand load of the object failed
     */
    protected JsonValue getSourceObject() throws SynchronizationException {
        if (sourceObjectAccessor == null || sourceObjectAccessor.getLocalId() == null) {
            return null;
        } else {
            final long startNanoTime = ObjectMapping.startNanoTime(reconContext, !sourceObjectAccessor.isLoaded());
            final JsonValue sourceObject = sourceObjectAccessor.getObject();
            ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.sourceObjectQuery, startNanoTime);
            return sourceObject;
        }
    }

    /**
     * @return the target object, potentially loaded on demand and/or cached, or null if does not exist
     * @throws SynchronizationException if on-demand load of the object failed
     */
    protected JsonValue getTargetObject() throws SynchronizationException {
        if (targetObjectAccessor == null || (!targetObjectAccessor.isLoaded() && targetObjectAccessor.getLocalId() == null)) {
            return null;
        } else {
            final long startNanoTime = ObjectMapping.startNanoTime(reconContext, !targetObjectAccessor.isLoaded());
            final JsonValue targetObject = targetObjectAccessor.getObject();
            ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.targetObjectQuery, startNanoTime);
            return targetObject;
        }
    }

    /**
     * The set unqualified (local) source object ID
     * That a source identifier is set does not automatically imply that the source object exists.
     * @return local identifier of the source object, or null if none
     */
    protected String getSourceObjectId() {
        return sourceObjectAccessor == null ? null : sourceObjectAccessor.getLocalId();
    }

    /**
     * The set unqualified (local) targt object ID
     * That a target identifier is set does not automatically imply that the target object exists.
     * @return local identifier of the target object, or null if none
     */
    protected String getTargetObjectId() {
        return targetObjectAccessor == null ? null : targetObjectAccessor.getLocalId();
    }

    /**
      * @return Whether the target representation is loaded, i.e. the getObject represents what it found.
      * IF a target was not found, the state is loaded with a payload / object of null.
     */
    protected boolean isTargetLoaded() {
        return targetObjectAccessor != null && targetObjectAccessor.isLoaded();
    }

    /**
     * Returns the linkQualifier associated with this SyncOperation
     * @return a String representing the linkQualifier
     */
    protected String getLinkQualifier() {
        return linkObject.linkQualifier;
    }

    /**
     * @return Whether the source object exists. May cause the loading of the (lazy) source object,
     * or in the context of reconciliation may check against the bulk existing source/target IDs if
     * the object existed at that point.
     * @throws SynchronizationException if on-demand load of the object failed
     */
    protected boolean hasSourceObject() throws SynchronizationException {
        boolean defined = false;
        if (sourceObjectAccessor == null || sourceObjectAccessor.getLocalId() == null) {
            defined = false;
        } else {
            if (sourceObjectAccessor.isLoaded() && sourceObjectAccessor.getObject() != null) {
                // Check against already laoded/defined object first, without causing new load
                defined = true;
            } else if (reconContext != null && reconContext.getSourceIds() != null) {
                // If available, check against all queried existing IDs
                defined = reconContext.getSourceIds().contains(sourceObjectAccessor.getLocalId());
            } else {
                // If no lists of existing ids is available, do a load of the object to check
                defined = (getSourceObject() != null);
            }
        }
        return defined;
    }

    /**
     * @return Whether the target object exists. May cause the loading of the (lazy) source object,
     * or in the context of reconciliation may check against the bulk existing source/target IDs if
     * the object existed at that point.
     * @throws SynchronizationException if on-demand load of the object failed
     */
    protected boolean hasTargetObject() throws SynchronizationException {
        boolean defined = false;

        if (isTargetLoaded()) {
            // Check against already laoded/defined object first, without causing new load
            defined = (targetObjectAccessor.getObject() != null);
        } else if (targetObjectAccessor == null || targetObjectAccessor.getLocalId() == null) {
            // If it's not loaded, but no id to load is available it has no target
            defined = false;
        } else {
            // Either check against a list of all targets, or load to check for existence
            if (reconContext != null && reconContext.getTargets() != null) {
                // If available, check against all queried existing IDs
                // If target system has case insensitive IDs, compare without regard to case
                String normalizedTargetId = objectMapping.getLinkType().normalizeTargetId(targetObjectAccessor.getLocalId());
                defined = reconContext.getTargets().containsKey(normalizedTargetId);
            } else {
                // If no lists of existing ids is available, do a load of the object to check
                defined = (getTargetObject() != null);
            }
        }

        return defined;
    }

    /**
     * @return true if it knows there were no objects in the target set during a bulk query
     * at the outset of reconciliation.
     * false if there were objects, or it does not know.
     * Does not take into account objects getting added during reconciliation, or data getting added
     * by another process concurrently
     */
    protected boolean hadEmptyTargetObjectSet() {
        if (reconContext != null && reconContext.getTargets() != null) {
            // If available, check against all queried existing IDs
            return (reconContext.getTargets().size() == 0);
        } else {
            return false;
        }
    }

    /**
     * Returns a String representing the link ID.
     *
     * @return the found unqualified (local) link ID, null if none
     */
    protected String getLinkId() {
        if (linkObject != null && linkObject.initialized) {
            return linkObject._id;
        } else {
            return null;
        }
    }

    /**
     * Initializes the link representation.
     *
     * @param link the link object for links that were found/exist in the repository, null to represent no existing link
     */
    protected void initializeLink(Link link) {
        if (link != null) {
            this.linkObject = link;
        } else {
            // Keep track of the fact that we did not find a link
            this.linkObject.clear();
            this.linkObject.initialized = true;
        }
    }

    /**
     * Returns all possible policies for a given situation.
     *
     * @param situation to get policies for
     * @return List of policies for given situation
     */
    private List<Policy> getPolicies(Situation situation) {
       if (policies.get(situation.toString()) == null) {
           return Collections.emptyList();
       }
        return policies.get(situation.toString());
    }

    /**
     * Sets the action and active policy based on the current situation.
     *
     * @throws SynchronizationException when cannot determine action from script
     */
    void determineAction(Context context) throws SynchronizationException {
        if (situation != null) {
            // start with a reasonable default
            action = situation.getDefaultAction();
            List<Policy> situationPolicies = getPolicies(situation);
            for (Policy policy : situationPolicies) {
                if (policy.getCondition().evaluate(
                        json(object(
                                field("object", getSourceObject()),
                                field("linkQualifier", getLinkQualifier()))), context)) {
                    activePolicy = policy;
                    final long startNanoTime = ObjectMapping.startNanoTime(reconContext, activePolicy.hasScript());
                    action = activePolicy.getAction(sourceObjectAccessor, targetObjectAccessor, this,
                            getLinkQualifier(), context);
                    ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.activePolicyScript, startNanoTime);
                    break;
                }
            }
        }
        LOGGER.debug("Determined action to be {}", action);
    }

    /**
     * Performs the current action for this SyncOperation
     *
     * @throws SynchronizationException TODO.
     */
    @SuppressWarnings("fallthrough")
    protected void performAction() throws SynchronizationException {
        switch (action) {
            case CREATE:
            case UPDATE:
            case LINK:
            case DELETE:
            case UNLINK:
            case EXCEPTION:
                try {
                    Context context = ObjectSetContext.get();
                    switch (action) {
                        case CREATE:
                            if (getSourceObject() == null) {
                                throw new SynchronizationException("no source object to create target from");
                            }
                            if (getTargetObject() != null) {
                                throw new SynchronizationException("target object already exists");
                            }
                            JsonValue createTargetObject = json(object());
                            // apply property mappings to target
                            applyMappings(context, getSourceObject(), oldValue, createTargetObject, json(null),
                                    linkObject.linkQualifier, reconContext);
                            targetObjectAccessor = new LazyObjectAccessor(
                                    objectMapping.getConnectionFactory(), objectMapping.getTargetObjectSet(),
                                    createTargetObject.get("_id").asString(), createTargetObject);
                            execScript("onCreate", onCreateScript);

                            // Allow the early link creation as soon as the target identifier is known
                            String sourceId = getSourceObjectId();
                            if (objectMapping.isLinkingEnabled()) {
                                // Create and populate the PendingActionContext for the LINK action
                                context = PendingAction.createPendingActionContext(context, ReconAction.LINK,
                                        objectMapping.getName(), getSourceObject(), reconId, situation);
                            }

                            targetObjectAccessor = createTargetObject(context, createTargetObject);

                            if (!objectMapping.isLinkingEnabled()) {
                                LOGGER.debug("Linking disabled for {} during {}, skipping additional link processing", sourceId, reconId);
                                // execute the post defaultPostMapping script to add lastSync attribute to managed user
                                execScript("postMapping", postMapping);
                                break;
                            }

                            boolean wasLinked = PendingAction.wasPerformed(context, ReconAction.LINK);
                            if (wasLinked) {
                                linkCreated = true;
                                LOGGER.debug("Pending link for {} during {} has already been created, skipping additional link processing", sourceId, reconId);
                                // execute the post defaultPostMapping script to add lastSync attribute to managed user
                                execScript("postMapping", postMapping);
                                break;
                            } else {
                                LOGGER.debug("Pending link for {} during {} not yet resolved, proceed to link processing", sourceId, reconId);
                                PendingAction.clear(context); // We'll now handle link creation ourselves
                            }
                            // falls through to link the newly created target
                        case UPDATE:
                        case LINK:
                            String targetId = getTargetObjectId();
                            if (getTargetObjectId() == null) {
                                throw new SynchronizationException("no target object to link");
                            }

                            // get a copy of the target before the onLink trigger,
                            // the onUpdate trigger or the mappings are applied
                            JsonValue oldTarget = getTargetObject() == null ? new JsonValue(null) : getTargetObject().copy();

                            if (objectMapping.isLinkingEnabled() && linkObject._id == null) {
                                try {
                                    createLink(context, getSourceObjectId(), targetId, reconId);
                                    linkCreated = true;
                                } catch (SynchronizationException ex) {
                                    // Allow for link to have been created in the meantime, e.g. programmatically
                                    // create would fail with a failed precondition for link already existing
                                    // Try to read again to see if that is the issue
                                    final long sourceLinkQueryStart = ObjectMapping.startNanoTime(reconContext);
                                    linkObject.getLinkForSource(getSourceObjectId());
                                    ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.sourceLinkQuery, sourceLinkQueryStart);
                                    if (linkObject._id == null) {
                                        LOGGER.warn("Failed to create link between {} and {}",
                                                LazyObjectAccessor.qualifiedId(objectMapping.getSourceObjectSet(), getSourceObjectId()),
                                                LazyObjectAccessor.qualifiedId(objectMapping.getTargetObjectSet(), targetId),
                                                ex);
                                        throw ex; // it was a different issue
                                    }
                                }
                            }
                            if (objectMapping.isLinkingEnabled() && linkObject._id != null && !linkObject.targetEquals(targetId)) {
                                linkObject.targetId = targetId;
                                linkObject.update(context);
                            }
                            // TODO: Detect change of source id, and update link accordingly.
                            if (action == ReconAction.CREATE || action == ReconAction.LINK) {
                                execScript("postMapping", postMapping);
                                break; // do not update target
                            }
                            if (getSourceObject() != null && getTargetObject() != null) {
                                applyMappings(context, getSourceObject(), oldValue, getTargetObject(), oldTarget,
                                        linkObject.linkQualifier, reconContext);
                                execScript("onUpdate", onUpdateScript, oldTarget);
                                // only update if target changes
                                if (!oldTarget.isEqualTo(getTargetObject())) {
                                    updateTargetObject(context, getTargetObject(), targetId, reconContext);
                                }
                            }
                            // execute the defaultPostMapping script to add lastSync attribute to managed user
                            execScript("postMapping", postMapping);
                            break; // terminate UPDATE
                        case DELETE:
                            if (objectMapping.isLinkingEnabled()) {
                                // Create and populate the PendingActionContext for the UNLINK action
                                context = PendingAction.createPendingActionContext(context, ReconAction.UNLINK,
                                        objectMapping.getName(), getSourceObject(), reconId, situation);
                            }
                            // forgiving; does nothing if no target
                            if (getTargetObjectId() != null && getTargetObject() != null) {
                                execScript("onDelete", onDeleteScript);
                                deleteTargetObject(context, getTargetObject(), reconContext);
                                // Represent as not existing anymore so it gets removed from processed targets
                                targetObjectAccessor = new LazyObjectAccessor(objectMapping.getConnectionFactory(),
                                        objectMapping.getTargetObjectSet(), getTargetObjectId(), null);
                            }
                            boolean wasUnlinked = PendingAction.wasPerformed(context, ReconAction.UNLINK);
                            if (wasUnlinked) {
                                LOGGER.debug("Pending unlink for {} during {} has already been performed, skipping additional unlink processing", getTargetObjectId(), reconId);
                                break;
                            } else {
                                LOGGER.debug("Pending unlink for {} during {} not yet performed, proceed to unlink processing", getTargetObjectId(), reconId);
                                PendingAction.clear(context); // We'll now handle unlinking
                            }
                            // falls through to unlink the deleted target
                        case UNLINK:
                            if (linkObject._id != null) { // forgiving; does nothing if no link exists
                                execScript("onUnlink", onUnlinkScript);

                                final long deleteLinkObjectStart = ObjectMapping.startNanoTime(reconContext);
                                linkObject.delete(context);
                                ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.deleteLinkObject, deleteLinkObjectStart);
                            }
                            break; // terminate DELETE and UNLINK
                        case EXCEPTION:
                            // aborts change; recon reports
                            throw new SynchronizationException("Situation " + situation + " marked as EXCEPTION");
                    }
                } catch (JsonValueException jve) {
                    throw new SynchronizationException(jve);
                } catch (ResourceException e) {
                    throw new SynchronizationException(e);
                }
            case REPORT:
            case NOREPORT:
                if (!ignorePostAction) {
                    if (null == activePolicy) {
                        List<Policy> situationPolicies = getPolicies(situation);
                        for (Policy policy : situationPolicies) {
                            // assigns the first policy found, as active policy
                            activePolicy = policy;
                            break;
                        }
                    }
                    postAction(isSourceToTarget());
                }
                break;
            case ASYNC:
            case IGNORE:
        }
    }

    /**
     * Evaluates a post action
     * @param sourceAction sourceAction true if the {@link ReconAction} is determined for the
     * {@link SourceSyncOperation}
     * and false if the action is determined for the {@link TargetSyncOperation}.
     * @throws SynchronizationException TODO.
     */
    protected void postAction(boolean sourceAction) throws SynchronizationException {
        if (null != activePolicy) {
            final long startNanoTime = ObjectMapping.startNanoTime(reconContext, activePolicy.hasPostActionScript());
            activePolicy.evaluatePostAction(sourceObjectAccessor, targetObjectAccessor, action, sourceAction,
                    getLinkQualifier(), reconId, context);
            ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.activePolicyPostActionScript, startNanoTime);
        }
    }

    protected void createLink(Context context, String sourceId, String targetId, String reconId)
            throws SynchronizationException {
        Link linkObject = new Link(objectMapping);
        linkObject.setLinkQualifier(this.linkObject.linkQualifier);
        execScript("onLink", onLinkScript);
        linkObject.sourceId = sourceId;
        linkObject.targetId = targetId;
        linkObject.create(context);
        initializeLink(linkObject);
        LOGGER.debug("Established link sourceId: {} targetId: {} in reconId: {}", sourceId, targetId, reconId);
    }

    /**
     * Evaluates the source condition on the source object
     *
     * @param linkQualifier the link qualifier for the current sync operation.
     * @return true if the conditions pass, false otherwise
     * @throws SynchronizationException
     */
    protected boolean checkSourceConditions(String linkQualifier) throws SynchronizationException {
        JsonValue params = json(object(
                field("source", sourceObjectAccessor.getObject()),
                field("linkQualifier", linkQualifier)));
        return sourceCondition.evaluate(params, context);
    }

    /**
     * Evaluated source valid on source object
     * @see SyncOperation#isSourceValid(JsonValue)
     */
    protected boolean isSourceValid() throws SynchronizationException {
        return isSourceValid(null);
    }

    /**
     * Evaluates source valid for the supplied sourceObjectOverride, or the source object
     * associated with the sync operation if null
     *
     * @return whether valid for this mapping or not.
     * @throws SynchronizationException if evaluation failed.
     */
    protected boolean isSourceValid(JsonValue sourceObjectOverride) throws SynchronizationException {
        boolean result = false;
        if (hasSourceObject() || sourceObjectOverride != null) { // must have a source object to be valid
            if (validSource != null) {
                final JsonValue sourceObject = (sourceObjectOverride != null)
                        ? sourceObjectOverride
                        : getSourceObject();
                if (sourceObject == null) {
                    throw new SynchronizationException("Source object " + getSourceObjectId() + " no longer exists");
                }

                Map<String, Object> scope = new HashMap<String, Object>();
                scope.put("source", sourceObject.asMap());
                scope.put("linkQualifier", getLinkQualifier());

                final long startNanoTime = ObjectMapping.startNanoTime(reconContext);
                try {
                    Object o = validSource.exec(scope, context);
                    if (o == null || !(o instanceof Boolean)) {
                        throw new SynchronizationException("Expecting boolean value from validSource");
                    }
                    result = (Boolean) o;
                } catch (ScriptThrownException ste) {
                    String errorMessage = objectMapping.getName() + " " + "validSource script encountered exception";
                    LOGGER.debug(errorMessage, ste);
                    throw new SynchronizationException(ste.toResourceException(ResourceException.INTERNAL_ERROR, errorMessage));
                } catch (ScriptException se) {
                    LOGGER.debug("{} validSource script encountered exception", objectMapping.getName(), se);
                    throw new SynchronizationException(se);
                } finally {
                    ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.validSourceScript, startNanoTime);
                }
            } else { // no script means true
                result = true;
            }
        }
        if (sourceObjectOverride == null) {
            LOGGER.trace("isSourceValid of {} evaluated: {}", getSourceObjectId(), result);
        }
        return result;
    }

    protected boolean isTargetValid() throws SynchronizationException {
        boolean result = false;
        if (hasTargetObject()) { // must have a target object to qualify
            if (validTarget != null && getTargetObject() != null) { // forces pulling object into memory
                Map<String, Object> scope = new HashMap<String, Object>();
                scope.put("target", getTargetObject().asMap());
                scope.put("linkQualifier", getLinkQualifier());

                final long startNanoTime = ObjectMapping.startNanoTime(reconContext);
                try {
                    Object o = validTarget.exec(scope, context);
                    if (o == null || !(o instanceof Boolean)) {
                        throw new SynchronizationException("Expecting boolean value from validTarget");
                    }
                    result = (Boolean) o;
                } catch (ScriptThrownException ste) {
                    String errorMessage = objectMapping.getName() + " " + "validTarget script encountered exception";
                    LOGGER.debug(errorMessage, ste);
                    throw new SynchronizationException(ste.toResourceException(ResourceException.INTERNAL_ERROR, errorMessage));
                } catch (ScriptException se) {
                    LOGGER.debug("{} validTarget script encountered exception", objectMapping.getName(), se);
                    throw new SynchronizationException(se);
                } finally {
                    ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.validTargetScript, startNanoTime);
                }
            } else { // no script means true
//TODO: is this the right result if getTargetObject was null?
                result = true;
            }
        }
        LOGGER.trace("isTargetValid of {} evaluated: {}", getTargetObjectId(), result);
        return result;
    }

    /**
     * @see #execScript with oldTarget null
     */
    private void execScript(String type, Script script) throws SynchronizationException {
        execScript(type, script, null);
    }

    /**
     * Executes the given script with the appropriate context information
     *
     * @param type The script hook name
     * @param script The script to execute
     * @param oldTarget optional old target object before any mappings were applied,
     * such as before an update
     * null if not applicable to this script hook
     * @throws SynchronizationException TODO.
     */
    private void execScript(String type, Script script, JsonValue oldTarget) throws SynchronizationException {
        if (script != null) {
            Map<String, Object> scope = new HashMap<String, Object>();
            scope.put("context", context);
            scope.put("linkQualifier", getLinkQualifier());
            scope.put("mappingConfig", objectMapping.getConfig());
            String sourceId = getSourceObjectId();
            String targetId = getTargetObjectId();
            // TODO: Once script engine can do on-demand get replace these forced loads
            if (sourceId != null) {
                JsonValue source = getSourceObject();
                scope.put("source", null != source ? source.getObject() : null);
            }
            scope.put("oldSource", oldValue);
            scope.put("sourceId", sourceId);

            // Target may not have ID yet (e.g. an onCreate with the target object defined, but not stored).
            if (isTargetLoaded() || targetId != null) {
                if (getTargetObject() != null) {
                    scope.put("target", getTargetObject().asMap());
                    if (oldTarget != null) {
                        scope.put("oldTarget", oldTarget.asMap());
                    }
                }
            }
            scope.put("targetId", targetId);

            if (situation != null) {
                scope.put("situation", situation.toString());
            }

            final long startNanoTime = ObjectMapping.startNanoTime(reconContext);
            try {
                script.exec(scope, context);
            } catch (ScriptThrownException se) {
                String errorMessage = objectMapping.getName() + " " + type + " script encountered exception";
                LOGGER.debug(errorMessage, se);
                throw new SynchronizationException(se.toResourceException(ResourceException.INTERNAL_ERROR, errorMessage));
            } catch (ScriptException se) {
                LOGGER.debug("{} script encountered exception", objectMapping.getName() + " " + type, se);
                throw new SynchronizationException(
                        new InternalErrorException(objectMapping.getName() + " " + type + " script encountered exception", se));
            } finally {
                ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.valueOf(type + "Script"), startNanoTime);
            }
        }
    }



    /**
     * Issues a request to create an object on the target.
     *
     * @param context the Context to use for the request
     * @param target the target object to create.
     * @throws SynchronizationException
     */
    private LazyObjectAccessor createTargetObject(Context context, JsonValue target) throws SynchronizationException {
        EventEntry measure = Publisher.start(ObjectMapping.EVENT_CREATE_OBJ, target, null);
        LazyObjectAccessor targetObject = null;
        LOGGER.trace("Create target object {}/{}", objectMapping.getTargetObjectSet(), target.get("_id").asString());
        try {
            CreateRequest request = newCreateRequest(objectMapping.getTargetObjectSet(), target.get("_id").asString(), target);
            ResourceResponse resource =  objectMapping.getConnectionFactory().getConnection().create(context, request);
            targetObject = new LazyObjectAccessor(objectMapping.getConnectionFactory(), objectMapping.getTargetObjectSet(), resource.getId(), resource.getContent());
            measure.setResult(target);
        } catch (JsonValueException jve) {
            throw new SynchronizationException(jve);
        } catch (ResourceException ose) {
            LOGGER.warn("Failed to create target object", ose);
            throw new SynchronizationException(ose);
        } finally {
            measure.end();
        }
        return targetObject;
    }

    /**
     * Issues a request to update an object on the target.
     *
     * @param context the Context to use for the request
     * @param target the target object to create.
     * @param reconContext Recon context or {@code null}
     * @throws SynchronizationException
     */
    private void updateTargetObject(Context context, JsonValue target, String targetId,
            ReconciliationContext reconContext) throws SynchronizationException {
        EventEntry measure = Publisher.start(ObjectMapping.EVENT_UPDATE_TARGET, target, null);
        final long startNanoTime = ObjectMapping.startNanoTime(reconContext);
        try {
            final String id = target.get("_id").required().asString();
            final String fullId = LazyObjectAccessor.qualifiedId(objectMapping.getTargetObjectSet(), id);
            // Do simple comparison first, only if it fails handle case sensitivity
            if (!targetId.equals(id) && objectMapping.isLinkingEnabled() &&
                    !objectMapping.getLinkType().normalizeTargetId(targetId).equals(objectMapping.getLinkType().normalizeTargetId(id))) {
                throw new SynchronizationException("target '_id' has changed");
            }
            LOGGER.trace("Update target object {}", fullId);
            UpdateRequest request = newUpdateRequest(fullId, target)
                    .setRevision(target.get("_rev").asString());
            objectMapping.getConnectionFactory().getConnection().update(context, request);
            measure.setResult(target);
        } catch (SynchronizationException se) {
            throw se;
        } catch (JsonValueException jve) {
            throw new SynchronizationException(jve);
        } catch (ResourceException ose) {
            LOGGER.warn("Failed to update target object", ose);
            throw new SynchronizationException(ose);
        } finally {
            ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.updateTargetObject, startNanoTime);
            measure.end();
        }
    }

    /**
     * Issues a request to delete an object on the target.
     *
     * @param context the Context to use for the request
     * @param target the target object to create.
     * @param reconContext Recon context
     * @throws SynchronizationException
     */
    private void deleteTargetObject(Context context, JsonValue target, ReconciliationContext reconContext) throws SynchronizationException {
        if (target != null && target.get("_id").isString()) { // forgiving delete
            EventEntry measure = Publisher.start(ObjectMapping.EVENT_DELETE_TARGET, target, null);
            final long startNanoTime = ObjectMapping.startNanoTime(reconContext);
            try {
                DeleteRequest request = newDeleteRequest(objectMapping.getTargetObjectSet(), target.get("_id").required().asString())
                        .setRevision(target.get("_rev").asString());
                LOGGER.trace("Delete target object {}", request.getResourcePath());
                objectMapping.getConnectionFactory().getConnection().delete(context, request);
            } catch (JsonValueException jve) {
                throw new SynchronizationException(jve);
            } catch (NotFoundException nfe) {
                // forgiving delete
            } catch (ResourceException ose) {
                LOGGER.warn("Failed to delete target object", ose);
                throw new SynchronizationException(ose);
            } finally {
                ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.deleteTargetObject, startNanoTime);
                measure.end();
            }
        }
    }

    /**
     * Apply the configured sync mappings
     *
     * @param context a {@link Context} for the current request.
     * @param source The current source object
     * @param oldSource an optional old source object before the change(s) that triggered the sync, null if not provided
     * @param target the current target object to modify
     * @param existingTarget the full existing target object
     * @param linkQualifier the linkQualifier associated with the current sync operation
     * @param reconContext Recon context or {@code null}
     * @throws SynchronizationException if applying the mappings fails.
     */
    private void applyMappings(Context context, JsonValue source, JsonValue oldSource, JsonValue target,
            JsonValue existingTarget, String linkQualifier, ReconciliationContext reconContext) throws SynchronizationException {
        EventEntry measure = Publisher.start(objectMapping.getObjectMappingEventName(), source, null);
        try {
            for (PropertyMapping property : properties) {
                final long startNanoTime = ObjectMapping.startNanoTime(reconContext, property.hasTransformScript());
                property.apply(source, oldSource, target, existingTarget, linkQualifier, context);
                ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.propertyMappingScript, startNanoTime);
            }
            // Apply default mapping, if configured
            applyDefaultMappings(context, source, oldSource, target, existingTarget, linkQualifier, reconContext);

            measure.setResult(target);
        } finally {
            measure.end();
        }
    }

    /**
     * Applies the default mapping by executing the default mapping script.
     *
     * @param context a {@link Context} for the current request.
     * @param source The current source object
     * @param oldSource an optional old source object before the change(s) that triggered the sync, null if not provided
     * @param target the current target object to modify
     * @param existingTarget the full existing target object
     * @param linkQualifier the linkQualifier associated with the current sync operation
     * @param reconContext Recon context or {@code null}
     * @return the result of the default mapping execution.
     * @throws SynchronizationException
     */
    private JsonValue applyDefaultMappings(Context context, JsonValue source, JsonValue oldSource, JsonValue target,
            JsonValue existingTarget, String linkQualifier, ReconciliationContext reconContext)
            throws SynchronizationException {
        JsonValue result = null;
        if (defaultMapping != null) {
            Map<String, Object> queryScope = new HashMap<String, Object>();
            queryScope.put("source", source.asMap());
            if (oldSource != null) {
                queryScope.put("oldSource", oldSource.asMap());
            }
            queryScope.put("target", target.asMap());
            queryScope.put("config", objectMapping.getConfig().asMap());
            queryScope.put("existingTarget", existingTarget.copy().asMap());
            queryScope.put("linkQualifier", linkQualifier);

            final long startNanoTime = ObjectMapping.startNanoTime(reconContext);
            try {
                result = json(defaultMapping.exec(queryScope, context));
            } catch (ScriptThrownException ste) {
                String errorMessage = objectMapping.getName() + " defaultMapping script encountered exception";
                LOGGER.debug(errorMessage, ste);
                throw new SynchronizationException(ste.toResourceException(ResourceException.INTERNAL_ERROR, errorMessage));
            } catch (ScriptException se) {
                LOGGER.debug("{} defaultMapping script encountered exception", objectMapping.getName(), se);
                throw new SynchronizationException(se);
            } finally {
                ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.defaultMappingScript, startNanoTime);
            }
        }
        return result;
    }

    protected Context getContext() {
        return context;
    }

    public JsonValue toJsonValue() throws SynchronizationException {
        return json(object(
                field("reconId", reconId),
                field("mapping", objectMapping.getName()),
                field("situation", situation != null ? situation.name() : null),
                field("action", action !=null ? action.name() : null),
                field("sourceId", getSourceObjectId()),
                field("linkQualifier", linkObject.linkQualifier),
                (targetObjectAccessor != null && targetObjectAccessor.getLocalId() != null)
                    ? field("targetId", targetObjectAccessor.getLocalId())
                    : null));
    }
}
