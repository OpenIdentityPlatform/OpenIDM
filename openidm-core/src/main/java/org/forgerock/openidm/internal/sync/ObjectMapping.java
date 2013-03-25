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

// Java SE

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.internal.recon.ReconUtil;
import org.forgerock.openidm.internal.recon.ReconUtil.Triplet;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.engine.Utils;
import org.forgerock.script.engine.Utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Description.
 * 
 * @author Paul C. Bryan
 * @author aegloff
 */
class ObjectMapping {

    /**
     * Event names for monitoring ObjectMapping behavior
     */
    public static final Name EVENT_CREATE_OBJ = Name
            .get("openidm/internal/discovery-engine/sync/create-object");
    public static final Name EVENT_SOURCE_ASSESS_SITUATION = Name
            .get("openidm/internal/discovery-engine/sync/source/assess-situation");
    public static final Name EVENT_SOURCE_DETERMINE_ACTION = Name
            .get("openidm/internal/discovery-engine/sync/source/determine-action");
    public static final Name EVENT_SOURCE_PERFORM_ACTION = Name
            .get("openidm/internal/discovery-engine/sync/source/perform-action");
    public static final Name EVENT_CORRELATE_TARGET = Name
            .get("openidm/internal/discovery-engine/sync/source/correlate-target");
    public static final Name EVENT_UPDATE_TARGET = Name
            .get("openidm/internal/discovery-engine/sync/update-target");
    public static final Name EVENT_DELETE_TARGET = Name
            .get("openidm/internal/discovery-engine/sync/delete-target");
    public static final Name EVENT_TARGET_ASSESS_SITUATION = Name
            .get("openidm/internal/discovery-engine/sync/target/assess-situation");
    public static final Name EVENT_TARGET_DETERMINE_ACTION = Name
            .get("openidm/internal/discovery-engine/sync/target/determine-action");
    public static final Name EVENT_TARGET_PERFORM_ACTION = Name
            .get("openidm/internal/discovery-engine/sync/target/perform-action");
    public static final String EVENT_OBJECT_MAPPING_PREFIX =
            "openidm/internal/discovery-engine/sync/objectmapping/";

    /**
     * Date util used when creating ReconObject timestamps
     */
    private static final DateUtil dateUtil = DateUtil.getDateUtil("UTC");

    /** TODO: Description. */
    private enum Status {
        SUCCESS, FAILURE
    }

    /**
     * Setup logging for the {@link SynchronizationService}.
     */
    private static final Logger logger = LoggerFactory.getLogger(ObjectMapping.class);

    /** TODO: Description. */
    private String name;

    /** The name of the links set to use. Defaults to mapping name. */
    private String linkTypeName;

    /** The link type to use */
    private LinkType linkType;

    /**
     * Whether to link source IDs in a case sensitive fashion. Only effective if
     * this mapping defines links, is ignored if this mapping re-uses another
     * mapping's links Default to {@code TRUE}
     */
    private Boolean sourceIdsCaseSensitive;

    /**
     * Whether to link target IDs in a case sensitive fashion. Only effective if
     * this mapping defines links, is ignored if this mapping re-uses another
     * mapping's links Default to {@code TRUE}
     */
    private Boolean targetIdsCaseSensitive;

    /** TODO: Description. */
    private String sourceObjectSet;

    /** TODO: Description. */
    private String targetObjectSet;

    /** TODO: Description. */
    private Pair<JsonPointer, ScriptEntry> validSource;

    /** TODO: Description. */
    private Pair<JsonPointer, ScriptEntry> validTarget;

    /** TODO: Description. */
    private Pair<JsonPointer, ScriptEntry> correlationQuery;

    /** TODO: Description. */
    private ArrayList<PropertyMapping> properties = new ArrayList<PropertyMapping>();

    /** TODO: Description. */
    private ArrayList<Policy> policies = new ArrayList<Policy>();

    /** TODO: Description. */
    private Pair<JsonPointer, ScriptEntry> onCreateScript;

    /** TODO: Description. */
    private Pair<JsonPointer, ScriptEntry> onUpdateScript;

    /** TODO: Description. */
    private Pair<JsonPointer, ScriptEntry> onDeleteScript;

    /** TODO: Description. */
    private Pair<JsonPointer, ScriptEntry> onLinkScript;

    /** TODO: Description. */
    private Pair<JsonPointer, ScriptEntry> onUnlinkScript;

    /**
     * Whether to maintain links for sync-d targets Default to {@code TRUE}
     */
    private Boolean linkingEnabled;

    /**
     * Whether synchronization (automatic propagation of changes as they are
     * detected) is enabled on that mapping
     */
    private Boolean syncEnabled;

    /**
     * Create an instance of a mapping between source and target
     * 
     * @param service
     *            The associated sychronization service
     * @param config
     *            The configuration for this mapping
     * @throws JsonValueException
     *             if there is an issue initializing based on the configuration.
     */
    public ObjectMapping(ScriptRegistry service, JsonValue config) throws JsonValueException,
            ScriptException {
        name = config.get("name").required().asString();
        linkTypeName = config.get("links").defaultTo(name).asString();

        sourceObjectSet = config.get("source").required().asString();
        targetObjectSet = config.get("target").required().asString();

        sourceIdsCaseSensitive =
                config.get("sourceIdsCaseSensitive").defaultTo(Boolean.TRUE).asBoolean();
        targetIdsCaseSensitive =
                config.get("targetIdsCaseSensitive").defaultTo(Boolean.TRUE).asBoolean();

        JsonValue scriptValue = config.get("validSource");
        validSource = Pair.of(scriptValue.getPointer(), service.takeScript(scriptValue));

        scriptValue = config.get("validTarget");
        validTarget = Pair.of(scriptValue.getPointer(), service.takeScript(scriptValue));

        scriptValue = config.get("correlationQuery");
        correlationQuery = Pair.of(scriptValue.getPointer(), service.takeScript(scriptValue));

        for (JsonValue jv : config.get("properties").expect(List.class)) {
            properties.add(new PropertyMapping(service, jv));
        }
        for (JsonValue jv : config.get("policies").expect(List.class)) {
            policies.add(new Policy(service, jv));
        }

        scriptValue = config.get("onCreate");
        onCreateScript = Pair.of(scriptValue.getPointer(), service.takeScript(scriptValue));

        scriptValue = config.get("onUpdate");
        onUpdateScript = Pair.of(scriptValue.getPointer(), service.takeScript(scriptValue));

        scriptValue = config.get("onDelete");
        onDeleteScript = Pair.of(scriptValue.getPointer(), service.takeScript(scriptValue));

        scriptValue = config.get("onLink");
        onLinkScript = Pair.of(scriptValue.getPointer(), service.takeScript(scriptValue));

        scriptValue = config.get("onUnlink");
        onUnlinkScript = Pair.of(scriptValue.getPointer(), service.takeScript(scriptValue));

        syncEnabled = config.get("enableSync").defaultTo(Boolean.TRUE).asBoolean();
        linkingEnabled = config.get("enableLinking").defaultTo(Boolean.TRUE).asBoolean();
        logger.debug("ObjectMapping instantiated {}", name);
    }

    public boolean isSyncEnabled() {
        return syncEnabled.booleanValue();
    }

    public boolean isLinkingEnabled() {
        return linkingEnabled.booleanValue();
    }

    /**
     * Mappings can share the same link tables. Establish the relationship
     * between the mappings and determine the proper link type to use
     * 
     * @param allMappings
     *            The list of all existing mappings
     */
    public void initRelationships(final List<ObjectMapping> allMappings) {
        linkType = LinkType.getLinkType(this, allMappings);
    }

    /**
     * @return The name of the object mapping
     */
    public String getName() {
        return name;
    }

    /**
     * @return The configured name of the link set to use for this object
     *         mapping
     */
    public String getLinkTypeName() {
        return linkTypeName;
    }

    /**
     * @return The resolved name of the link set to use for this object mapping
     */
    public LinkType getLinkType() {
        return linkType;
    }

    /**
     * @return The mapping source object set
     */
    public String getSourceObjectSet() {
        return sourceObjectSet;
    }

    /**
     * @return The mapping target object set
     */
    public String getTargetObjectSet() {
        return targetObjectSet;
    }

    /**
     * @return The setting for whether to link source IDs in a case sensitive
     *         fashion. Only effective if the mapping defines the links, not if
     *         the mapping re-uses another mapping's links
     */
    public boolean getSourceIdsCaseSensitive() {
        return sourceIdsCaseSensitive.booleanValue();
    }

    /**
     * @return The setting for whether to link target IDs in a case sensitive
     *         fashion. Only effective if the mapping defines the links, not if
     *         the mapping re-uses another mapping's links
     */
    public boolean getTargetIdsCaseSensitive() {
        return targetIdsCaseSensitive.booleanValue();
    }

    // /*
    // * Source and target collections are predefined and the link is
    // configured, we only need the id in the source collection and the type of
    // link in one-to-many or many-to-one relation.
    // */
    // public void onCreate(String id, JsonValue value) throws ResourceException
    // {
    // if (isSourceObject(id)) {
    // if (value == null || value.getObject() == null) { // notification
    // // without the
    // // actual value
    // value = LazyObjectAccessor.rawReadObject(service.getRouter(), id);
    // }
    // doSourceSync(id, value); // synchronous for now
    // }
    // }
    //
    // @Override
    // public void onUpdate(String id, JsonValue oldValue, JsonValue newValue)
    // throws ResourceException {
    // if (isSourceObject(id)) {
    // if (newValue == null || newValue.getObject() == null) { // notification
    // // without
    // // the
    // // actual
    // // value
    // newValue = LazyObjectAccessor.rawReadObject(service.getRouter(), id);
    // }
    // // TODO: use old value to project incremental diff without fetch of
    // // source
    // if (oldValue == null || oldValue.getObject() == null
    // || JsonPatch.diff(oldValue, newValue).size() > 0) {
    //
    // doSourceSync(id, newValue, false, null);
    // } else {
    // logger.trace("There is nothing to update on {}", id);
    // }
    // }
    // }
    //
    // @Override
    // public void onDelete(String id, JsonValue oldValue) throws
    // ResourceException {
    // if (isSourceObject(id)) {
    // doSourceSync(id, null, true, oldValue); // synchronous for now
    // }
    // }

    Request onCreateSync(final ServerContext context, Triplet triplet)
            throws ResourceException {
        logger.trace("Start source synchronization of {}", triplet);
        return null;
    }

    /**
     * Source synchronization
     * 
     * @param context
     *            fully-qualified source object identifier.
     * @param triplet
     *            null to have it query the source state if applicable, or
     *            JsonValue to tell it the value of the existing source to sync
     * @param newContent
     *            Whether the source object has been deleted
     * @throws ResourceException
     *             if sync-ing fails.
     */
     Request onUpdateSync(final ServerContext context, Triplet triplet, JsonValue newContent)
            throws ResourceException {
        logger.trace("Start source synchronization of {}", triplet);

        // TODO: one day bifurcate this for synchronous and asynchronous source
        // operation
        SourceSyncOperation op = new SourceSyncOperation();
        // op.oldValue = oldValue;
        // if (sourceDeleted) {
        // op.sourceObjectAccessor =
        // new LazyObjectAccessor(service, sourceObjectSet, localId, null);
        // } else if (value != null) {
        // value.put("_id", localId); // unqualified
        // op.sourceObjectAccessor =
        // new LazyObjectAccessor(service, sourceObjectSet, localId, value);
        // } else {
        // op.sourceObjectAccessor = new LazyObjectAccessor(service,
        // sourceObjectSet, localId);
        // }
        // op.sync();
        return null;
    }

    Request onDeleteSync(final ServerContext context, Triplet triplet) throws ResourceException {
        logger.trace("Start source synchronization of {}", triplet);

        return null;
    }

    // TODO: maybe move all this target stuff into a target object wrapper to
    // keep this class clean
    /**
     * TODO: Description.
     * 
     * @param target
     *            TODO.
     * @throws ResourceException
     *             TODO.
     */
     private Resource createTargetObject(final ServerContext context, Triplet.Vertex target)
            throws ResourceException {
        EventEntry measure = Publisher.start(EVENT_CREATE_OBJ, target, null);
        Resource targetObject = null;
        try {
            Resource targetResource = null;// TODO Resource from Vertex
            CreateRequest request =
                    Requests.newCreateRequest(linkType.targetResourceContainer(), target.getId(),
                            new JsonValue(target.map().get("content")));
            logger.trace("Create target object {}", request);
            targetObject = context.getConnection().create(context, request);
            measure.setResult(target);
        } catch (Throwable t) {
            logger.warn("Failed to create target object", t);
            throw ResourceUtil.adapt(t);
        } finally {
            measure.end();
        }
        return targetObject;
    }

    // TODO: maybe move all this target stuff into a target object wrapper to
    // keep this class clean
    /**
     * TODO: Description.
     * 
     * @param target
     *            TODO.
     * @throws ResourceException
     *             TODO.
     */
    private Resource updateTargetObject(final ServerContext context, Triplet.Vertex target)
            throws ResourceException {
        EventEntry measure = Publisher.start(EVENT_UPDATE_TARGET, target, null);
        Resource targetObject = null;
        try {
            Resource targetResource = null;// TODO Resource from Vertex
            UpdateRequest request =
                    Requests.newUpdateRequest(linkType.targetResourceContainer(), target.getId(),
                            new JsonValue(target.map().get("newContent")));
            request.setRevision(null);
            logger.trace("Update target object {}", request);
            targetObject = context.getConnection().update(context, request);
            measure.setResult(target);
        } catch (Throwable t) {
            logger.warn("Failed to create target object", t);
            throw ResourceUtil.adapt(t);
        } finally {
            measure.end();
        }
        return targetObject;
    }

    // TODO: maybe move all this target stuff into a target object wrapper to
    // keep this class clean
    /**
     * TODO: Description.
     * 
     * @param target
     *            TODO.
     * @throws ResourceException
     *             TODO.
     */
    private Resource deleteTargetObject(final ServerContext context, Triplet.Vertex target)
            throws ResourceException {
        if (null != target.getId()) { // forgiving delete
            EventEntry measure = Publisher.start(EVENT_DELETE_TARGET, target, null);
            Resource targetObject = null;
            try {
                Resource targetResource = null;// TODO Resource from Vertex
                DeleteRequest request =
                        Requests.newDeleteRequest(linkType.targetResourceContainer(), target
                                .getId());
                request.setRevision(null);
                logger.trace("Delete target object {}", request);
                targetObject = context.getConnection().delete(context, request);
                measure.setResult(target);
            } catch (Throwable t) {
                logger.warn("Failed to create target object", t);
                throw ResourceUtil.adapt(t);
            } finally {
                measure.end();
            }
            return targetObject;
        }
        return null;
    }

    /**
     * TODO: Description.
     * 
     * @param source
     *            TODO.
     * @param target
     *            TODO.
     * @throws ResourceException
     *             TODO.
     */
    private void applyMappings(final Context context, JsonValue source, JsonValue target)
            throws ResourceException {
        EventEntry measure = Publisher.start(getObjectMappingEventName(), source, null);
        try {
            for (PropertyMapping property : properties) {
                property.apply(context, source, target);
            }
            measure.setResult(target);
        } finally {
            measure.end();
        }
    }

    /**
     * @return an event name for monitoring this object mapping
     */
    private Name getObjectMappingEventName() {
        return Name.get(EVENT_OBJECT_MAPPING_PREFIX + name);
    }

    /**
     * Returns {@code true} if the specified object identifer is in this
     * mapping's source object set.
     */
    private boolean isSourceObject(String id) {
        return (id.startsWith(sourceObjectSet + '/') && id.length() > sourceObjectSet.length() + 1);
    }

    /**
     * Perform the reconciliation action on a pre-assessed job.
     * <p/>
     * For the input parameters see
     * {@link ObjectMapping.SourceSyncOperation#toJsonValue()} or
     * {@link ObjectMapping.TargetSyncOperation#toJsonValue()}.
     * <p/>
     * Script example:
     * 
     * <pre>
     *     try {
     *          openidm.action('sync',recon.actionParam)
     *     } catch(e) {
     * 
     *     };
     * </pre>
     * 
     * @param params
     * @throws ResourceException
     */
    // public void performAction(JsonValue params) throws ResourceException {
    // // If reconId is set this action is part of a reconciliation run
    // String reconId = params.get("reconId").asString();
    // JsonValue context = ObjectSetContext.get();
    // if (reconId != null) {
    // context.add("trigger", "recon");
    // }
    //
    // try {
    // JsonValue rootContext = JsonResourceContext.getRootContext(context);
    // Action action = params.get("action").required().asEnum(Action.class);
    // SyncOperation op = null;
    // ReconEntry entry = null;
    // ResourceException exception = null;
    // try {
    // if (params.get("target").isNull()) {
    // SourceSyncOperation sop = new SourceSyncOperation();
    // op = sop;
    // sop.fromJsonValue(params);
    //
    // entry = new ReconEntry(sop, rootContext, dateUtil);
    // entry.sourceId =
    // LazyObjectAccessor
    // .qualifiedId(sourceObjectSet, sop.getSourceObjectId());
    // if (null == sop.getSourceObject()) {
    // exception =
    // new ResourceException("Source object " + entry.sourceId
    // + " does not exists");
    // throw exception;
    // }
    // // TODO blank check
    // String targetId = params.get("targetId").asString();
    // if (null != targetId) {
    // op.targetObjectAccessor =
    // new LazyObjectAccessor(service, targetObjectSet, targetId);
    // if (null == sop.getTargetObject()) {
    // exception =
    // new ResourceException("Target object " + targetId
    // + " does not exists");
    // throw exception;
    // }
    // }
    // sop.assessSituation();
    // } else {
    // TargetSyncOperation top = new TargetSyncOperation();
    // op = top;
    // top.fromJsonValue(params);
    // String targetId = params.get("targetId").required().asString();
    //
    // entry = new ReconEntry(top, rootContext, dateUtil);
    // entry.targetId = LazyObjectAccessor.qualifiedId(targetObjectSet,
    // targetId);
    // top.targetObjectAccessor =
    // new LazyObjectAccessor(service, targetObjectSet, targetId);
    // if (null == top.getTargetObject()) {
    // exception =
    // new ResourceException("Target object " + entry.targetId
    // + " does not exists");
    // throw exception;
    // }
    // top.assessSituation();
    // }
    // Situation situation =
    // params.get("situation").required().asEnum(Situation.class);
    // op.action = action;
    // if (!situation.equals(op.situation)) {
    // exception =
    // new ResourceException("Expected situation does not match. Expect: "
    // + situation.name() + " Found: " + op.situation.name());
    // throw exception;
    // }
    // op.performAction();
    // } catch (ResourceException se) {
    // if (op.action != Action.EXCEPTION) {
    // entry.status = Status.FAILURE; // exception was not
    // // intentional
    // if (reconId != null) {
    // logger.warn("Unexpected failure during source reconciliation {}",
    // reconId,
    // se);
    // } else {
    // logger.warn("Unexpected failure in performing action {}", params, se);
    // }
    // }
    // Throwable throwable = se;
    // while (throwable.getCause() != null) { // want message
    // // associated with
    // // original cause
    // throwable = throwable.getCause();
    // }
    // if (se != throwable) {
    // entry.message = se.getMessage() + ". Root cause: " +
    // throwable.getMessage();
    // } else {
    // entry.message = throwable.getMessage();
    // }
    // }
    // if (reconId != null && !Action.NOREPORT.equals(action)
    // && (entry.status == Status.FAILURE || op.action != null)) {
    // entry.timestamp = new Date();
    // if (op instanceof SourceSyncOperation) {
    // entry.reconciling = "source";
    // if (op.getTargetObject() != null) {
    // entry.targetId =
    // LazyObjectAccessor.qualifiedId(targetObjectSet, op
    // .getTargetObject().get("_id").asString());
    // }
    // entry.setAmbiguousTargetIds(((SourceSyncOperation)
    // op).getAmbiguousTargetIds());
    // } else {
    // entry.reconciling = "target";
    // if (op.getSourceObject() != null) {
    // entry.sourceId =
    // LazyObjectAccessor.qualifiedId(sourceObjectSet, op
    // .getSourceObject().get("_id").asString());
    // }
    // }
    // logReconEntry(entry);
    // }
    // if (exception != null) {
    // throw exception;
    // }
    // } finally {
    // if (reconId != null) {
    // context.remove("trigger");
    // }
    // }
    // }

    /**
     * Reconcile a given source ID
     * 
     * @param context
     * @param sourceId
     *            the id to reconcile
     * @throws ResourceException
     *             if there is a failure reported in reconciling this id
     */
    void reconSourceById(final ServerContext context, String sourceId) throws ResourceException {
        SourceSyncOperation op = new SourceSyncOperation();
        ReconEntry entry = new ReconEntry(op, null, dateUtil);
        op.sourceObjectAccessor = null;// new LazyObjectAccessor(service,
                                       // sourceObjectSet, sourceId);

        // entry.sourceId = LazyObjectAccessor.qualifiedId(sourceObjectSet,
        // sourceId);

        try {
            op.sync(context);
        } catch (ResourceException se) {
            if (op.action != Action.EXCEPTION) {
                entry.status = Status.FAILURE; // exception was not intentional
                logger.warn("Unexpected failure during source reconciliation {}", se);
            }
            Throwable throwable = se;
            while (throwable.getCause() != null) { // want message associated
                                                   // with original cause
                throwable = throwable.getCause();
            }
            if (se != throwable) {
                entry.message = se.getMessage() + ". Root cause: " + throwable.getMessage();
            } else {
                entry.message = throwable.getMessage();
            }
        }
        String[] targetIds = op.getTargetIds();
        for (String handledId : targetIds) {
            // If target system has case insensitive IDs, remove without regard
            // to case
            String normalizedHandledId = linkType.normalizeTargetId(handledId);

            logger.trace("Removed target from remaining targets: {}", normalizedHandledId);
        }

        if (!Action.NOREPORT.equals(op.action)
                && (entry.status == Status.FAILURE || op.action != null)) {
            entry.timestamp = new Date();
            entry.reconciling = "source";
            try {
                if (op.hasTargetObject()) {
                    entry.targetId =
                            linkType.targetResourceContainer() + "/" + op.getTargetObjectId();
                }
            } catch (ResourceException ex) {
                entry.message =
                        "Failure in preparing recon entry " + ex.getMessage() + " for target: "
                                + op.getTargetObjectId() + " original status: " + entry.status
                                + " message: " + entry.message;
                entry.status = Status.FAILURE;
            }
            entry.setAmbiguousTargetIds(op.getAmbiguousTargetIds());
            logReconEntry(context, entry);
        }
    }

    /**
     * TODO: Description.
     * 
     * @param entry
     *            TODO.
     * @throws ResourceException
     *             TODO.
     */
    private void logReconEntry(final ServerContext context, ReconEntry entry)
            throws ResourceException {
        CreateRequest request = Requests.newCreateRequest("audit/recon", entry.toJsonValue());
        context.getConnection().create(context, request);
    }

    private void logReconStart(final ServerContext context, String reconId, JsonValue rootContext,
            JsonValue rcontext) throws ResourceException {
        // ReconEntry reconStartEntry =
        // new ReconEntry(null, rootContext, ReconEntry.RECON_START, dateUtil);
        // reconStartEntry.timestamp = new Date();
        // reconStartEntry.reconId = reconId;
        // reconStartEntry.message =
        // "Reconciliation initiated by " + ActivityLog.getRequester(context);
        // logReconEntry(reconStartEntry);
    }

    // private void logReconEnd(ReconciliationContext reconContext, JsonValue
    // rootContext,
    // JsonValue context) throws ResourceException {
    // ReconEntry reconEndEntry =
    // new ReconEntry(null, rootContext, ReconEntry.RECON_END, dateUtil);
    // reconEndEntry.timestamp = new Date();
    // reconEndEntry.reconId = reconContext.getReconId();
    // String simpleSummary = reconContext.getStatistics().simpleSummary();
    // reconEndEntry.message = simpleSummary;
    // logReconEntry(reconEndEntry);
    // logger.info("Reconciliation completed. " + simpleSummary);
    // }

    /**
     * Execute a sync engine action explicitly, without going through situation
     * assessment.
     * 
     * @param sourceObject
     *            the source object if applicable to the action
     * @param targetObject
     *            the target object if applicable to the action
     * @param situation
     *            an optional situation that was originally assessed. Null if
     *            not the result of an earlier situation assessment.
     * @param action
     *            the explicit action to invoke
     * @param reconId
     *            an optional identifier for the recon context if this is done
     *            in the context of reconciliation
     */
    // public void explicitOp(JsonValue sourceObject, JsonValue targetObject,
    // Situation situation,
    // Action action, String reconId) throws ResourceException {
    // ExplicitSyncOperation linkOp = new ExplicitSyncOperation();
    // // linkOp.init(sourceObject, targetObject, situation, action, reconId);
    // linkOp.sync();
    // }

    /**
     * TODO: Description.
     */
    abstract class SyncOperation {

        private ReconUtil.Triplet triplet;

        /** Access to the source object */
        public Triplet.Vertex sourceObjectAccessor;
        /** Access to the target object */
        public Triplet.Vertex targetObjectAccessor;
        /**
         * Optional value of the object before the change that triggered this
         * sync, or null if not supplied
         */
        public JsonValue oldValue;

        /**
         * Holds the link representation An initialized link can be interpreted
         * as representing state retrieved from the repository, i.e. a
         * linkObject with id of null represents a link that does not exist
         * (yet)
         */
        public LinkType linkType = null;
        // This operation newly created the link.
        // linkObject above may not be set for newly created links
        boolean linkCreated;

        /** TODO: Description. */
        public Situation situation;
        /** TODO: Description. */
        public Action action;
        public boolean ignorePostAction = false;
        public Policy activePolicy = null;

        /**
         * TODO: Description.
         * 
         * @throws ResourceException
         *             TODO.
         */
        public abstract void sync(ServerContext context) throws ResourceException;

        protected abstract boolean isSourceToTarget();

        /**
         * @return the source object, potentially loaded on demand and/or
         *         cached, or null if does not exist
         * @throws ResourceException
         *             if on-demand load of the object failed
         */
        protected Resource getSourceObject() throws ResourceException {
            // if (sourceObjectAccessor == null ||
            // sourceObjectAccessor.getLocalId() == null) {
            // return null;
            // } else {
            // return sourceObjectAccessor.getObject();
            // }
            return null;
        }

        /**
         * @return the target object, potentially loaded on demand and/or
         *         cached, or null if does not exist
         * @throws ResourceException
         *             if on-demand load of the object failed
         */
        protected Resource getTargetObject() throws ResourceException {
            // if (targetObjectAccessor == null
            // || (!targetObjectAccessor.isLoaded() &&
            // targetObjectAccessor.getLocalId() == null)) {
            // return null;
            // } else {
            // return targetObjectAccessor.getObject();
            // }
            return null;
        }

        /**
         * The set unqualified (local) source object ID That a source identifier
         * is set does not automatically imply that the source object exists.
         * 
         * @return local identifier of the source object, or null if none
         */
        protected String getSourceObjectId() {
            return triplet.source().exits() ? triplet.source().getId() : null;
        }

        /**
         * The set unqualified (local) targt object ID That a target identifier
         * is set does not automatically imply that the target object exists.
         * 
         * @return local identifier of the target object, or null if none
         */
        protected String getTargetObjectId() {
            return triplet.target().exits() ? triplet.target().getId() : null;
        }

        /**
         * @return Whether the target representation is loaded, i.e. the
         *         getObject represents what it found. IF a target was not
         *         found, the state is loaded with a payload / object of null.
         */
        protected boolean isTargetLoaded() {
            return targetObjectAccessor == null /*
                                                 * ? false :
                                                 * targetObjectAccessor
                                                 * .isLoaded()
                                                 */;
        }

        /**
         * @return Whether the source object exists. May cause the loading of
         *         the (lazy) source object, or in the context of reconciliation
         *         may check against the bulk existing source/target IDs if the
         *         object existed at that point.
         * @throws ResourceException
         *             if on-demand load of the object failed
         */
        protected boolean hasSourceObject() throws ResourceException {
            boolean defined = false;
            // if (sourceObjectAccessor == null ||
            // sourceObjectAccessor.getLocalId() == null) {
            // defined = false;
            // } else {
            // if (sourceObjectAccessor.isLoaded() &&
            // sourceObjectAccessor.getObject() != null) {
            // // Check against already laoded/defined object first,
            // // without causing new load
            // defined = true;
            // } else if (reconContext != null && reconContext.getSourceIds() !=
            // null) {
            // // If available, check against all queried existing IDs
            // defined =
            // reconContext.getSourceIds().contains(sourceObjectAccessor.getLocalId());
            // } else {
            // // If no lists of existing ids is available, do a load of
            // // the object to check
            // defined = (sourceObjectAccessor.getObject() != null);
            // }
            // }
            return defined;
        }

        /**
         * @return Whether the target object exists. May cause the loading of
         *         the (lazy) source object, or in the context of reconciliation
         *         may check against the bulk existing source/target IDs if the
         *         object existed at that point.
         * @throws ResourceException
         *             if on-demand load of the object failed
         */
        protected boolean hasTargetObject() throws ResourceException {
            boolean defined = false;

            // if (isTargetLoaded()) {
            // // Check against already loaded/defined object first, without
            // // causing new load
            // defined = (targetObjectAccessor.getObject() != null);
            // } else if (targetObjectAccessor == null ||
            // targetObjectAccessor.getLocalId() == null) {
            // // If it's not loaded, but no id to load is available it has no
            // // target
            // defined = false;
            // } else {
            // // Either check against a list of all targets, or load to check
            // // for existence
            // if (reconContext != null && reconContext.getTargetIds() != null)
            // {
            // // If available, check against all queried existing IDs
            // // If target system has case insensitive IDs, compare
            // // without regard to case
            // String normalizedTargetId =
            // linkType.normalizeTargetId(targetObjectAccessor.getLocalId());
            // defined =
            // reconContext.getTargetIds().contains(normalizedTargetId);
            // } else {
            // // If no lists of existing ids is available, do a load of
            // // the object to check
            // defined = (targetObjectAccessor.getObject() != null);
            // }
            // }

            return defined;
        }

        /**
         * @return true if it knows there were no objects in the target set
         *         during a bulk query at the outset of reconciliation. false if
         *         there were objects, or it does not know. Does not take into
         *         account objects getting added during reconciliation, or data
         *         getting added by another process concurrently
         */
        // protected boolean hadEmptyTargetObjectSet() {
        // if (reconContext != null && reconContext.getTargetIds() != null) {
        // // If available, check against all queried existing IDs
        // return (reconContext.getTargetIds().size() == 0);
        // } else {
        // return false;
        // }
        // }

        /**
         * @return the found unqualified (local) link ID, null if none
         */
        protected String getLinkId() {
            // if (linkObject != null && linkObject.initialized) {
            // return linkObject._id;
            // } else {
            // return null;
            // }
            return null;
        }

        /**
         * Initializes the link representation
         * 
         * @param link
         *            the link object for links that were found/exist in the
         *            repository, null to represent no existing link
         */
        protected void initializeLink(Link link) {
            // if (link != null) {
            // this.linkObject = link;
            // } else {
            // // Keep track of the fact that we did not find a link
            // this.linkObject.clear();
            // this.linkObject.initialized = true;
            // }
        }

        protected Action getAction() {
            return (this.action == null ? Action.IGNORE : this.action);
        }

        /**
         * TODO: Description.
         * 
         * @param context
         *            The Context assigned to this operation.
         * @throws ResourceException
         *             TODO.
         */
        protected void determineAction(final Context context) throws ResourceException {
            if (situation != null) {
                // start with a reasonable default
                action = situation.getDefaultAction();
                for (Policy policy : policies) {
                    if (situation == policy.getSituation()) {
                        activePolicy = policy;
                        // action = activePolicy.getAction(context,
                        // sourceObjectAccessor, targetObjectAccessor, this);
                        // TODO: Consider limiting what actions can be returned
                        // for the given situation.
                        break;
                    }
                }
            }
            logger.debug("Determined action to be {}", action);
        }

        /**
         * TODO: Description.
         * 
         * @throws ResourceException
         *             TODO.
         */
        @SuppressWarnings("fallthrough")
        protected Request performAction(final ServerContext context, boolean dryRun)
                throws ResourceException {
            switch (getAction()) {
            case CREATE:
            case UPDATE:
            case LINK:
            case DELETE:
            case UNLINK:
            case EXCEPTION:
                try {
                    switch (getAction()) {
                    case CREATE:
                        if (getSourceObject() == null) {
                            throw new InternalServerErrorException(
                                    "no source object to create target from");
                        }
                        if (getTargetObject() != null) {
                            throw new InternalServerErrorException("target object already exists");
                        }

                        JsonValue targetContent = new JsonValue(new HashMap<String, Object>());
                        // apply property mappings to target
                        applyMappings(context, getSourceObject().getContent(), targetContent);

                        targetObjectAccessor = null;
                        // new LazyObjectAccessor(service, targetObjectSet,
                        // createTargetObject
                        // .get("_id").asString(), createTargetObject);
                        execScript(context, onCreateScript);

                        // Allow the early link creation as soon as the target
                        // identifier is known
                        String sourceId = getSourceObjectId();
                        if (isLinkingEnabled()) {
                            // PendingLink.populate(context,
                            // ObjectMapping.this.name, sourceId,
                            // getSourceObject(), reconId, situation);
                        }

                        /* targetObjectAccessor = */createTargetObject(context, triplet.target());

                        if (!isLinkingEnabled()) {
                            logger.debug(
                                    "Linking disabled for {} during {}, skipping additional link processing",
                                    sourceId);
                            break;
                        }

                        boolean wasLinked = false;// PendingLink.wasLinked(context);
                        if (wasLinked) {
                            linkCreated = true;
                            logger.debug(
                                    "Pending link for {} during {} has already been created, skipping additional link processing",
                                    sourceId);
                            break;
                        } else {
                            logger.debug(
                                    "Pending link for {} during {} not yet resolved, proceed to link processing",
                                    sourceId);
                            // PendingLink.clear(context); // We'll now handle
                            // link
                            // creation ourselves
                        }
                        // falls through to link the newly created target
                    case UPDATE:
                    case LINK:
                        String targetId = getTargetObjectId();
                        if (getTargetObjectId() == null) {
                            throw new InternalServerErrorException("no target object to link");
                        }

                        if (isLinkingEnabled() && !triplet.link().exits()) {
                            try {
                                createLink(context, getSourceObjectId(), targetId);
                                linkCreated = true;
                            } catch (ResourceException ex) {
                                // Allow for link to have been created in the
                                // meantime, e.g. programmatically
                                // create would fail with a failed precondition
                                // for link already existing
                                // Try to read again to see if that is the issue
                                // linkObject.getLinkForSource(getSourceObjectId());
                                // if (linkObject._id == null) {
                                // logger.warn("Failed to create link between {}-{}",
                                // new Object[] {
                                // LazyObjectAccessor.qualifiedId(sourceObjectSet,
                                // getSourceObjectId()),
                                // LazyObjectAccessor.qualifiedId(targetObjectSet,
                                // targetId), ex });
                                // throw ex; // it was a different issue
                                // }
                            }
                        }
                        // if (isLinkingEnabled() && linkObject._id != null
                        // && !linkObject.targetEquals(targetId)) {
                        // linkObject.targetId = targetId;
                        // linkObject.update();
                        // }
                        // TODO: Detect change of source id, and update link
                        // accordingly.
                        if (action == Action.CREATE || action == Action.LINK) {
                            break; // do not update target
                        }
                        if (getSourceObject() != null && getTargetObject() != null) {
                            // JsonValue oldTarget = getTargetObject().copy();
                            // applyMappings(context, getSourceObject(),
                            // getTargetObject());
                            execScript(context, onUpdateScript);
                            // only update if target changes
                            // if (JsonPatch.diff(oldTarget,
                            // getTargetObject()).size() > 0) {
                            updateTargetObject(context, triplet.target());
                            // }
                        }
                        break; // terminate UPDATE
                    case DELETE:
                        if (getTargetObjectId() != null && getTargetObject() != null) { // forgiving;
                                                                                        // does
                                                                                        // nothing
                                                                                        // if
                                                                                        // no
                                                                                        // target
                            execScript(context, onDeleteScript);
                            deleteTargetObject(context, triplet.target());
                            // Represent as not existing anymore so it gets
                            // removed from processed targets
                            // targetObjectAccessor = new
                            // LazyObjectAccessor(service, targetObjectSet,
                            // getTargetObjectId(), null);
                        }
                        // falls through to unlink the deleted target
                    case UNLINK:
                        if (!triplet.link().exits()) { // forgiving; does
                                                       // nothing if no link
                                                       // exists
                            execScript(context, onUnlinkScript);
                            // linkObject.delete();
                        }
                        break; // terminate DELETE and UNLINK
                    case EXCEPTION:
                        throw new InternalServerErrorException("Situation " + situation
                                + " marked as EXCEPTION"); // aborts change;
                                                           // recon reports
                    }
                } catch (JsonValueException jve) {
                    throw new BadRequestException(jve);
                }
            case REPORT:
            case NOREPORT:
                if (!ignorePostAction) {
                    if (null == activePolicy) {
                        for (Policy policy : policies) {
                            if (situation == policy.getSituation()) {
                                activePolicy = policy;
                                break;
                            }
                        }
                    }
                    postAction(context);
                }
                break;
            case ASYNC:
            case IGNORE:
            }
            return null;
        }

        /**
         * TODO: Description.
         *.
         * @throws ResourceException
         *             TODO.
         */
        protected void postAction(final Context context)
                throws ResourceException {
            if (null != activePolicy) {
                // activePolicy.evaluatePostAction(context,
                // sourceObjectAccessor, targetObjectAccessor, action,
                // sourceAction);
            }
        }

        protected void createLink(final Context context, String sourceId, String targetId)
                throws ResourceException {
            Link linkObject = null;// new Link(ObjectMapping.this);
            execScript(context, onLinkScript);
            linkObject.sourceId = sourceId;
            linkObject.targetId = targetId;
            // linkObject.create();
            initializeLink(linkObject);
            logger.debug("Established link sourceId: {} targetId: {} in reconId: {}", new Object[] {
                sourceId, targetId });
        }

        /**
         * Evaluates source valid for the supplied sourceObjectOverride, or the
         * source object associated with the sync operation if null
         * 
         * @return whether valid for this mapping or not.
         * @throws ResourceException
         *             if evaluation failed.
         */
        protected boolean isSourceValid(final Context context) throws ResourceException {
            boolean result = false;
            // must have a source object to be valid
            if (hasSourceObject()) {
                if (validSource != null) {
                    Object o = execScript(context, validSource);
                    if (o == null || !(o instanceof Boolean)) {
                        throw new InternalServerErrorException(
                                "Expecting boolean value from validSource");
                    }
                    result = (Boolean) o;
                } else { // no script means true
                    result = true;
                }
            }
            return result;
        }

        /**
         * TODO: Description.
         * 
         * @return TODO.
         * @throws ResourceException
         *             TODO.
         */
        protected boolean isTargetValid(final Context context) throws ResourceException {
            boolean result = false;
            // must have a target object to qualify
            if (hasTargetObject()) {
                if (validTarget != null && getTargetObject() != null) {
                    Object o = execScript(context, validTarget);
                    if (o == null || !(o instanceof Boolean)) {
                        throw new InternalServerErrorException(
                                "Expecting boolean value from validTarget");
                    }
                    result = (Boolean) o;
                } else { // no script means true
                    result = true;
                }
            }
            logger.trace("isTargetValid of {} evaluated: {}", getTargetObjectId(), result);
            return result;
        }

        /**
         * Executes the given script with the appropriate context information
         * 
         * @param context
         *            The script hook name
         * @param scriptPair
         *            The script to execute
         * @throws ResourceException
         *             TODO.
         */
        private Object execScript(final Context context,
                final Pair<JsonPointer, ScriptEntry> scriptPair) throws ResourceException {
            if (scriptPair != null) {
                if (scriptPair.snd.isActive()) {
                    throw new ServiceUnavailableException("Failed to execute inactive script: "
                            + scriptPair.snd.getName());
                }
                Script script = scriptPair.snd.getScript(context);

                Bindings bindings = script.createBindings();
                bindings.putAll(triplet.map());
                script.setBindings(bindings);

                // // TODO: Once script engine can do on-demand get replace
                // these
                // // forced loads
                // if (getSourceObjectId() != null) {
                // script.put("source", getSourceObject().asMap());
                // }
                // // Target may not have ID yet, e.g. an onCreate with the
                // target
                // // object defined,
                // // but not stored/id assigned.
                // if (isTargetLoaded() || getTargetObjectId() != null) {
                // if (getTargetObject() != null) {
                // script.put("target", getTargetObject().asMap());
                // }
                // }
                if (situation != null) {
                    script.put("situation", situation.toString());
                }
                try {
                    return script.eval();
                } catch (Throwable t) {
                    logger.debug("ObjectMapping/{} script {} encountered exception at {}", name,
                            scriptPair.snd.getName(), scriptPair.fst, t);
                    throw Utils.adapt(t);
                }
            }
            return null;
        }
    }

    /**
     * Explicit execution of a sync operation where the appropriate action is
     * known without having to assess the situation and apply policy to decide
     * the action
     */
    private class ExplicitSyncOperation extends SyncOperation {

        protected boolean isSourceToTarget() {
            // TODO: detect by the source id match
            return true;
        }

        public void init(Resource sourceObject, Resource targetObject, Situation situation,
                Action action) {
            // this.sourceObjectAccessor = sourceObject;
            // this.targetObjectAccessor = targetObject;
            this.situation = situation;
            this.action = action;
            this.ignorePostAction = true;
        }

        @Override
        public void sync(final ServerContext context) throws ResourceException {
            logger.debug("Initiate explicit operation call for situation: {}, action: {}",
                    situation, action);
            performAction(context, false);
            logger.debug("Complected explicit operation call for situation: {}, action: {}",
                    situation, action);
        }
    }

    /**
     * TODO: Description.
     */
    class SourceSyncOperation extends SyncOperation {

        // If it can not uniquely identify a target, the list of ambiguous
        // target ids
        public List<String> ambiguousTargetIds;

        @Override
        @SuppressWarnings("fallthrough")
        public void sync(final ServerContext context) throws ResourceException {
            EventEntry measureSituation =
                    Publisher.start(EVENT_SOURCE_ASSESS_SITUATION, getSourceObjectId(), null);
            try {
                assessSituation();
            } finally {
                measureSituation.end();
            }
            EventEntry measureDetermine =
                    Publisher.start(EVENT_SOURCE_DETERMINE_ACTION, getSourceObjectId(), null);
            boolean linkExisted = (getLinkId() != null);

            try {
                determineAction(context);
            } finally {
                measureDetermine.end();
            }
            EventEntry measurePerform =
                    Publisher.start(EVENT_SOURCE_PERFORM_ACTION, getSourceObjectId(), null);
            try {
                performAction(context, false);
            } finally {
                measurePerform.end();
            }
        }

        protected boolean isSourceToTarget() {
            return true;
        }

        /**
         * @return all found matching target identifier(s), or a 0 length array
         *         if none. More than one target identifier is possible for
         *         ambiguous matches
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
         * @return the ambiguous target identifier(s), or an empty list if no
         *         ambiguous entries are present
         */
        public List getAmbiguousTargetIds() {
            return ambiguousTargetIds;
        }

        private void setAmbiguousTargetIds(JsonValue results) {
            ambiguousTargetIds = new ArrayList<String>(results == null ? 0 : results.size());
            for (JsonValue resultValue : results) {
                String anId = resultValue.get("_id").required().asString();
                ambiguousTargetIds.add(anId);
            }
        }

        // public void fromJsonValue(JsonValue params) {
        // sourceObjectAccessor =
        // new LazyObjectAccessor(service, sourceObjectSet,
        // params.get("sourceId")
        // .required().asString());
        // ignorePostAction =
        // params.get("ignorePostAction").defaultTo(false).asBoolean();
        // }

        public JsonValue toJsonValue() {
            JsonValue actionParam = new JsonValue(new HashMap<String, Object>());
            actionParam.put("mapping", ObjectMapping.this.getName());
            actionParam.put("situation", situation.name());
            actionParam.put("action", situation.getDefaultAction().name());
            actionParam.put("sourceId", getSourceObjectId());
            // if (targetObjectAccessor != null &&
            // targetObjectAccessor.getLocalId() != null) {
            // actionParam.put("targetId", targetObjectAccessor.getLocalId());
            // }
            return actionParam;
        }

        /**
         * <pre>
         *  t  s
         *  a  o
         *  r  u
         *  g  r
         *  e  c
         *  t  e
         *  q  q
         *  u  u  t       s
         *  a  a  a       o
         *  l  l  r   l   u
         *  i  i  g   i   r
         *  f  f  e   n   c
         *  y  y  t   k   e
         *  ---------
         *  1  1 |1   1   1   =   7   CONFIRMED
         *  1  0 |1   1   1   =   7   CONFIRMED -> TARGET_IGNORED
         *  0  1 |1   1   1   =   7   CONFIRMED -> Not Assisted
         *  0  0 |1   1   1   =   7   CONFIRMED -> TARGET_IGNORED
         *  1  X |1   1   0   =   6   SOURCE_MISSING
         *  0  X |1   1   0   =   6   SOURCE_MISSING -> TARGET_IGNORED
         *  1  1 |1   0   1   =   5   FOUND
         *  1  0 |1   0   1   =   5   FOUND -> SOURCE_MISSING
         *  0  1 |1   0   1   =   5   FOUND
         *  0  0 |1   0   1   =   5   FOUND -> SOURCE_MISSING
         *  1  X |1   0   0   =   4   UNASSIGNED
         *  0  X |1   0   0   =   4   UNASSIGNED -> TARGET_IGNORED
         *  X  1 |0   1   1   =   3   MISSING
         *  X  0 |0   1   1   =   3   MISSING -> UNQUALIFIED
         *  X  X |0   1   0   =   2   LINK_ONLY
         *  X  1 |0   0   1   =   1   ABSENT
         *  X  0 |0   0   1   =   1   ABSENT -> SOURCE_IGNORED
         *  X  X |0   0   0   =   0   ALL_GONE
         * </pre>
         */
        private void assessSituation() throws ResourceException {
            situation = null;
            if (!isLinkingEnabled()) {
                initializeLink(null); // If we're not linking, set link to none
            }

            // if (getSourceObjectId() != null && linkObject.initialized ==
            // false) { // In
            // // case
            // // the
            // // link
            // // was
            // // not
            // // pre-read
            // // get
            // // it
            // // here
            // linkObject.getLinkForSource(getSourceObjectId());
            // }
            // if (linkObject._id != null) {
            // targetObjectAccessor =
            // new LazyObjectAccessor(service, targetObjectSet,
            // linkObject.targetId);
            // }
            //
            // if (!hasSourceObject()) {
            // /*
            // * For sync of delete. For recon these are assessed instead in
            // * target phase
            // *
            // * no source, link, target & valid target - source missing no
            // * source, link, target & not valid target - target ignored no
            // * source, link, no target - link only no source, no link -
            // * can't correlate (no previous object available) - all gone no
            // * source, no link - (correlate) no target - all gone 1 target &
            // * valid (source) - unassigned 1 target & not valid (source) -
            // * target ignored > 1 target & valid (source) - ambiguous > 1
            // * target & not valid (source) - unqualified
            // */
            //
            // if (linkObject._id != null) {
            // if (hasTargetObject()) {
            // if (isTargetValid()) {
            // situation = Situation.SOURCE_MISSING;
            // } else {
            // // target is not valid for this mapping; ignore it
            // situation = Situation.TARGET_IGNORED;
            // }
            // } else {
            // situation = Situation.LINK_ONLY;
            // }
            // } else {
            // if (oldValue == null) {
            // // If there is no previous value known we can not
            // // correlate
            // situation = Situation.ALL_GONE;
            // } else {
            // // Correlate the old value to potential target(s)
            // JsonValue results = correlateTarget(oldValue);
            // boolean valid = isSourceValid(oldValue);
            // if (results == null || results.size() == 0) {
            // // Results null means no correlation query defined,
            // // size 0 we know there is no target
            // situation = Situation.ALL_GONE;
            // } else if (results.size() == 1) {
            // JsonValue resultValue = results.get((Integer) 0).required();
            // targetObjectAccessor = getCorrelatedTarget(resultValue);
            // if (valid) {
            // situation = Situation.UNASSIGNED;
            // } else {
            // // target is not valid for this mapping; ignore
            // // it
            // situation = Situation.TARGET_IGNORED;
            // }
            // } else if (results.size() > 1) {
            // if (valid) {
            // // Note this situation is used both when there
            // // is a source and a deleted source
            // // with multiple matching targets
            // situation = Situation.AMBIGUOUS;
            // } else {
            // situation = Situation.UNQUALIFIED;
            // }
            // }
            // }
            // }
            // } else if (isSourceValid()) { // source is valid for mapping
            // if (linkObject._id != null) { // source object linked to target
            // if (hasTargetObject()) {
            // situation = Situation.CONFIRMED;
            // } else {
            // situation = Situation.MISSING;
            // }
            // } else { // source object not linked to target
            // JsonValue results = correlateTarget();
            // if (results == null) { // no correlationQuery defined
            // situation = Situation.ABSENT;
            // } else if (results.size() == 1) {
            // JsonValue resultValue = results.get((Integer) 0).required();
            // targetObjectAccessor = getCorrelatedTarget(resultValue);
            // situation = Situation.FOUND;
            // } else if (results.size() == 0) {
            // situation = Situation.ABSENT;
            // } else {
            // situation = Situation.AMBIGUOUS;
            // setAmbiguousTargetIds(results);
            // }
            // }
            // } else { // mapping does not qualify for target
            // if (linkObject._id != null) {
            // situation = Situation.UNQUALIFIED;
            // } else {
            // JsonValue results = correlateTarget();
            // if (results == null || results.size() == 0) {
            // situation = Situation.SOURCE_IGNORED; // source not
            // // valid for
            // // mapping, and no
            // // link or target
            // // exist
            // } else if (results.size() == 1) {
            // // TODO: Consider if we can optimize out the read for
            // // unqualified conditions
            // JsonValue resultValue = results.get((Integer) 0).required();
            // targetObjectAccessor = getCorrelatedTarget(resultValue);
            // situation = Situation.UNQUALIFIED;
            // } else if (results.size() > 1) {
            // situation = Situation.UNQUALIFIED;
            // setAmbiguousTargetIds(results);
            // }
            // }
            // }
            logger.debug("Mapping '{}' assessed situation of {} to be {}", new Object[] { name,
                getSourceObjectId(), situation });
        }

    }

    /**
     * TEMPORARY.
     */
    private static class ReconEntry {

        public final static String RECON_START = "start";
        public final static String RECON_END = "summary";
        public final static String RECON_ENTRY = ""; // regular reconciliation
                                                     // entry has an empty entry
                                                     // type

        /**
         * Type of the audit log entry. Allows for marking recon start / summary
         * records
         */
        public String entryType = RECON_ENTRY;
        /** TODO: Description. */
        public final SyncOperation op;
        /** The id identifying the reconciliation run */
        public String reconId;
        /** The root invocation context */
        public final JsonValue rootContext;
        /** TODO: Description. */
        public Date timestamp;
        /** TODO: Description. */
        public Status status = ObjectMapping.Status.SUCCESS;
        /** TODO: Description. */
        public String sourceId;
        /** TODO: Description. */
        public String targetId;
        /** TODO: Description. */
        public String reconciling;
        /** TODO: Description. */
        public String message;

        private DateUtil dateUtil;

        // A comma delimited formatted representation of any ambiguous
        // identifiers
        protected String ambigiousTargetIds;

        public void setAmbiguousTargetIds(List<String> ambiguousIds) {
            if (ambiguousIds != null) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (String id : ambiguousIds) {
                    if (!first) {
                        sb.append(", ");
                    }
                    first = false;
                    sb.append(id);
                }
                ambigiousTargetIds = sb.toString();
            } else {
                ambigiousTargetIds = "";
            }
        }

        private String getReconId() {
            return null;// (reconId == null && op != null) ? op.reconId :
                        // reconId;
        }

        /**
         * Constructor that allows specifying the type of reconciliation log
         * entry
         */
        public ReconEntry(SyncOperation op, JsonValue rootContext, String entryType,
                DateUtil dateUtil) {
            this.op = op;
            this.rootContext = rootContext;
            this.entryType = entryType;
            this.dateUtil = dateUtil;
        }

        /**
         * Constructor for regular reconciliation log entries
         */
        public ReconEntry(SyncOperation op, JsonValue rootContext, DateUtil dateUtil) {
            this(op, rootContext, RECON_ENTRY, dateUtil);
        }

        /**
         * TODO: Description.
         * 
         * @return TODO.
         */
        private JsonValue toJsonValue() {
            JsonValue jv = new JsonValue(new HashMap<String, Object>());
            jv.put("entryType", entryType);
            jv.put("rootActionId", rootContext.get("uuid").getObject());
            jv.put("reconId", getReconId());
            jv.put("reconciling", reconciling);
            jv.put("sourceObjectId", sourceId);
            jv.put("targetObjectId", targetId);
            jv.put("ambiguousTargetObjectIds", ambigiousTargetIds);
            jv.put("timestamp", dateUtil.formatDateTime(timestamp));
            jv.put("situation", ((op == null || op.situation == null) ? null : op.situation
                    .toString()));
            jv.put("action", ((op == null || op.action == null) ? null : op.action.toString()));
            jv.put("status", (status == null ? null : status.toString()));
            jv.put("message", message);
            return jv;
        }
    }

}
