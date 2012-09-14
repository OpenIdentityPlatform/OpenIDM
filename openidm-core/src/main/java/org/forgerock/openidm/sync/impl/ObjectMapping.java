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
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.sync.impl;

// Java SE
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.patch.JsonPatch;
import org.forgerock.json.resource.JsonResourceContext;
import org.forgerock.openidm.audit.util.ActivityLog;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.Scripts;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.SynchronizationListener;
import org.forgerock.openidm.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 * @author aegloff
 */
class ObjectMapping implements SynchronizationListener {

    /**
     * Event names for monitoring ObjectMapping behavior
     */
    public static final Name EVENT_CREATE_OBJ = Name.get("openidm/internal/discovery-engine/sync/create-object");
    public static final Name EVENT_SOURCE_ASSESS_SITUATION = Name.get("openidm/internal/discovery-engine/sync/source/assess-situation");
    public static final Name EVENT_SOURCE_DETERMINE_ACTION = Name.get("openidm/internal/discovery-engine/sync/source/determine-action");
    public static final Name EVENT_SOURCE_PERFORM_ACTION = Name.get("openidm/internal/discovery-engine/sync/source/perform-action");
    public static final Name EVENT_CORRELATE_TARGET = Name.get("openidm/internal/discovery-engine/sync/source/correlate-target");
    public static final Name EVENT_UPDATE_TARGET = Name.get("openidm/internal/discovery-engine/sync/update-target");
    public static final Name EVENT_DELETE_TARGET = Name.get("openidm/internal/discovery-engine/sync/delete-target");
    public static final Name EVENT_TARGET_ASSESS_SITUATION = Name.get("openidm/internal/discovery-engine/sync/target/assess-situation");
    public static final Name EVENT_TARGET_DETERMINE_ACTION = Name.get("openidm/internal/discovery-engine/sync/target/determine-action");
    public static final Name EVENT_TARGET_PERFORM_ACTION = Name.get("openidm/internal/discovery-engine/sync/target/perform-action");
    public static final String EVENT_OBJECT_MAPPING_PREFIX = "openidm/internal/discovery-engine/sync/objectmapping/";

    /**
     * Event names for monitoring Reconciliation behavior
     */
    public static final Name EVENT_RECON = Name.get("openidm/internal/discovery-engine/reconciliation");
    public static final Name EVENT_RECON_ID_QUERIES = Name.get("openidm/internal/discovery-engine/reconciliation/id-queries-phase");
    public static final Name EVENT_RECON_SOURCE = Name.get("openidm/internal/discovery-engine/reconciliation/source-phase");
    public static final Name EVENT_RECON_TARGET = Name.get("openidm/internal/discovery-engine/reconciliation/target-phase");

    /**
     * Date util used when creating ReconObject timestamps
     */
    private static final DateUtil dateUtil = DateUtil.getDateUtil("UTC");

    /** TODO: Description. */
    private enum Status { SUCCESS, FAILURE }

    /** TODO: Description. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectMapping.class);

    /** TODO: Description. */
    private String name;

    /** The name of the links set to use. Defaults to mapping name. */
    private String linkTypeName;

    /** The link type to use */
    LinkType linkType;

    /** TODO: Description. */
    private String sourceObjectSet;

    /** TODO: Description. */
    private String targetObjectSet;

    /** TODO: Description. */
    private Script validSource;

    /** TODO: Description. */
    private Script validTarget;

    /** TODO: Description. */
    private Script correlationQuery;

    /** TODO: Description. */
    private ArrayList<PropertyMapping> properties = new ArrayList<PropertyMapping>();

    /** TODO: Description. */
    private ArrayList<Policy> policies = new ArrayList<Policy>();

    /** TODO: Description. */
    private Script onCreateScript;

    /** TODO: Description. */
    private Script onUpdateScript;

    /** TODO: Description. */
    private Script onDeleteScript;

    /** TODO: Description. */
    private Script onLinkScript;

    /** TODO: Description. */
    private Script onUnlinkScript;

    /** TODO: Description. */
    private Script resultScript;
    
    /** 
     * Whether existing links should be fetched in one go along with the source and target id lists. 
     * false indicates links should be retrieved individually as they are needed.
     */
    private Boolean prefetchLinks;
    
    /** 
     * Whether when at the outset of correlation the target set is empty (query all ids returns empty),
     * it should try to correlate source entries to target when necessary.
     * Default to {@code FALSE} 
     */
    private Boolean correlateEmptyTargetSet;

    /** TODO: Description. */
    private SynchronizationService service;

    /** TODO: Description. */
    private ReconStats sourceStats = null;

    /** TODO: Description. */
    private ReconStats targetStats = null;

    /** TODO: Description. */
    private ReconStats globalStats = null;

    /**
     * TODO: Description.
     *
     * @param service
     * @param config TODO.
     * @throws JsonValueException TODO.
     */
    public ObjectMapping(SynchronizationService service, JsonValue config) throws JsonValueException {
        this.service = service;
        name = config.get("name").required().asString();
        linkTypeName = config.get("links").defaultTo(name).asString();
        sourceObjectSet = config.get("source").required().asString();
        targetObjectSet = config.get("target").required().asString();
        validSource = Scripts.newInstance("ObjectMapping", config.get("validSource"));
        validTarget = Scripts.newInstance("ObjectMapping", config.get("validTarget"));
        correlationQuery = Scripts.newInstance("ObjectMapping", config.get("correlationQuery"));
        for (JsonValue jv : config.get("properties").expect(List.class)) {
            properties.add(new PropertyMapping(service, jv));
        }
        for (JsonValue jv : config.get("policies").expect(List.class)) {
            policies.add(new Policy(service, jv));
        }
        onCreateScript = Scripts.newInstance("ObjectMapping", config.get("onCreate"));
        onUpdateScript = Scripts.newInstance("ObjectMapping", config.get("onUpdate"));
        onDeleteScript = Scripts.newInstance("ObjectMapping", config.get("onDelete"));
        onLinkScript = Scripts.newInstance("ObjectMapping", config.get("onLink"));
        onUnlinkScript = Scripts.newInstance("ObjectMapping", config.get("onUnlink"));
        resultScript = Scripts.newInstance("ObjectMapping", config.get("result"));
        prefetchLinks = config.get("prefetchLinks").defaultTo(Boolean.TRUE).asBoolean();
        correlateEmptyTargetSet = config.get("correlateEmptyTargetSet").defaultTo(Boolean.FALSE).asBoolean();
        
        LOGGER.debug("Instantiated {}", name);
    }

    public void initRelationships(SynchronizationService syncSvc, List<ObjectMapping> allMappings) {
        linkType = LinkType.getLinkType(this, allMappings);
    }

    /**
     * TODO: Description.
     * @return
     */
    SynchronizationService getService() {
        return service;
    }

    /**
     * @return The name of the object mapping
     */
    public String getName() {
        return name;
    }

    /**
     * @return The configured name of the link set to use for this object mapping
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
     * TODO: Description.
     *
     * @param id fully-qualified source object identifier.
     * @param value TODO.
     * @throws SynchronizationException TODO.
     */
    private void doSourceSync(String id, JsonValue value) throws SynchronizationException {
        LOGGER.trace("Start source synchronization of {} {}", id, (value == null ? "without a value" : "with a value"));

        String localId = id.substring(sourceObjectSet.length() + 1); // skip the slash
// TODO: one day bifurcate this for synchronous and asynchronous source operation
        SourceSyncOperation op = new SourceSyncOperation();
        if (value != null) {
            value.put("_id", localId); // unqualified
            op.sourceObjectAccessor = new LazyObjectAccessor(service, sourceObjectSet, localId, value);
        } else {
            op.sourceObjectAccessor = new LazyObjectAccessor(service, sourceObjectSet, localId);
        }
        op.sync();
    }

    /**
     * TODO: Description.
     *
     * @param query TODO.
     * @return TODO.
     * @throws SynchronizationException TODO.
     */
    private Map<String, Object> queryTargetObjectSet(Map<String, Object> query) 
            throws SynchronizationException {
        try {
            return service.getRouter().query(targetObjectSet, query);
        } catch (ObjectSetException ose) {
            throw new SynchronizationException(ose);
        }
    }

    /**
     * Get all IDs for a given object set as a list
     *
     * @param objectSet the object set to query
     * @return the list of (unqualified) ids
     * @throws SynchronizationException if retrieving or processing the ids failed
     */
    private List<String> queryAllIds(final String objectSet, ReconciliationContext reconContext) 
            throws SynchronizationException {
        List<String> ids = new ArrayList<String>();
        HashMap<String, Object> query = new HashMap<String, Object>();
        query.put(QueryConstants.QUERY_ID, "query-all-ids");
        try {
            JsonValue objList = new JsonValue(service.getRouter().query(objectSet, query)).get(QueryConstants.QUERY_RESULT).required().expect(List.class);
            for (JsonValue obj : objList) {
                ids.add(obj.get("_id").asString());
            }
        } catch (JsonValueException jve) {
            throw new SynchronizationException(jve);
        } catch (ObjectSetException ose) {
            throw new SynchronizationException(ose);
        }
        checkCanceled(reconContext); // Throws an exception if reconciliation was canceled
        return ids;
    }

    /**
     * Get all IDs for a given object set as a iterable.
     * May allow for further optimizations over direct list access and is the preferred way to access.
     *
     * @param objectSet the object set to query
     * @return the list of (unqualified) ids
     * @throws SynchronizationException if retrieving or processing the ids failed
     */
    private Iterable<String> queryAllIdsIterable(final String objectSet, final ReconciliationContext reconContext) 
            throws SynchronizationException {
        // For now we pull all into memory immediately
        return queryAllIds(objectSet, reconContext);
    }

// TODO: maybe move all this target stuff into a target object wrapper to keep this class clean
    /**
     * TODO: Description.
     *
     * @param target TODO.
     * @throws SynchronizationException TODO.
     */
    private LazyObjectAccessor createTargetObject(JsonValue target) throws SynchronizationException {
        EventEntry measure = Publisher.start(EVENT_CREATE_OBJ, target, null);
        LazyObjectAccessor targetObject = null;
        StringBuilder sb = new StringBuilder();
        sb.append(targetObjectSet);
        if (target.get("_id").isString()) {
            sb.append('/').append(target.get("_id").asString());
        }
        String id = sb.toString();
        LOGGER.trace("Create target object {}", id);
        try {
            service.getRouter().create(id, target.asMap());
            targetObject = new LazyObjectAccessor(service, targetObjectSet, target.get("_id").asString(), target);
            measure.setResult(target);
        } catch (JsonValueException jve) {
            throw new SynchronizationException(jve);
        } catch (ObjectSetException ose) {
            LOGGER.warn("Failed to create target object", ose);
            throw new SynchronizationException(ose);
        } finally {
            measure.end();
        }
        return targetObject;
    }

// TODO: maybe move all this target stuff into a target object wrapper to keep this class clean
    /**
     * TODO: Description.
     *
     * @param target TODO.
     * @throws SynchronizationException TODO.
     */
    private void updateTargetObject(JsonValue target) throws SynchronizationException {
        EventEntry measure = Publisher.start(EVENT_UPDATE_TARGET, target, null);
        try {
            String id = LazyObjectAccessor.qualifiedId(targetObjectSet, 
                    target.get("_id").required().asString());
            LOGGER.trace("Update target object {}", id);
            service.getRouter().update(id, target.get("_rev").asString(), target.asMap());
            measure.setResult(target);
        } catch (JsonValueException jve) {
            throw new SynchronizationException(jve);
        } catch (ObjectSetException ose) {
            LOGGER.warn("Failed to update target object", ose);
            throw new SynchronizationException(ose);
        } finally {
            measure.end();
        }
    }

// TODO: maybe move all this target stuff into a target object wrapper to keep this class clean
    /**
     * TODO: Description.
     *
     * @param target TODO.
     * @throws SynchronizationException TODO.
     */
    private void deleteTargetObject(JsonValue target) throws SynchronizationException {
        if (target.get("_id").isString()) { // forgiving delete
            EventEntry measure = Publisher.start(EVENT_DELETE_TARGET, target, null);
            try {
                String id = LazyObjectAccessor.qualifiedId(targetObjectSet, 
                        target.get("_id").required().asString());
                LOGGER.trace("Delete target object {}", id);
                service.getRouter().delete(id, target.get("_rev").asString());
            } catch (JsonValueException jve) {
                throw new SynchronizationException(jve);
            } catch (NotFoundException nfe) {
                // forgiving delete
            } catch (ObjectSetException ose) {
                LOGGER.warn("Failed to delete target object", ose);
                throw new SynchronizationException(ose);
            } finally {
                measure.end();
            }
        }
    }

    /**
     * TODO: Description.
     *
     * @param source TODO.
     * @param target TODO.
     * @throws SynchronizationException TODO.
     */
    private void applyMappings(JsonValue source, JsonValue target) throws SynchronizationException {
        EventEntry measure = Publisher.start(getObjectMappingEventName(), source, null);
        try {
            for (PropertyMapping property : properties) {
                property.apply(source, target);
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
     * Returns {@code true} if the specified object identifer is in this mapping's source
     * object set.
     */
    private boolean isSourceObject(String id) {
        return (id.startsWith(sourceObjectSet + '/') && id.length() > sourceObjectSet.length() + 1);
    }

    @Override
    public void onCreate(String id, JsonValue value) throws SynchronizationException {
        if (isSourceObject(id)) {
            if (value == null || value.getObject() == null) { // notification without the actual value
                value = LazyObjectAccessor.rawReadObject(service.getRouter(), id);
            }
            doSourceSync(id, value); // synchronous for now
        }
    }

    @Override
    public void onUpdate(String id, JsonValue oldValue, JsonValue newValue) throws SynchronizationException {
        if (isSourceObject(id)) {
            if (newValue == null || newValue.getObject() == null) { // notification without the actual value
                newValue = LazyObjectAccessor.rawReadObject(service.getRouter(), id);
            }
            // TODO: use old value to project incremental diff without fetch of source
            if (oldValue == null || oldValue.getObject() == null || JsonPatch.diff(oldValue, newValue).size() > 0) {
                doSourceSync(id, newValue); // synchronous for now
            } else {
                LOGGER.trace("There is nothing to update on {}", id);
            }
        }
    }

    @Override
    public void onDelete(String id) throws SynchronizationException {
        if (isSourceObject(id)) {
            doSourceSync(id, null); // synchronous for now
        }
    }

    public void recon(ReconciliationContext reconContext) throws SynchronizationException {
        EventEntry measure = Publisher.start(EVENT_RECON, reconContext.getReconId(), null);
        doRecon(reconContext);
        measure.end();
    }

    /**
     * Perform the reconciliation action on a pre-assessed job.
     * <p/>
     * For the input parameters see {@link org.forgerock.openidm.sync.impl.ObjectMapping.SourceSyncOperation#toJsonValue()} or
     * {@link org.forgerock.openidm.sync.impl.ObjectMapping.TargetSyncOperation#toJsonValue()}.
     * <p/>
     * Script example:
     * <pre>
     *     try {
     *          openidm.action('sync',recon.actionParam)
     *     } catch(e) {
     *
     *     };
     * </pre>
     * @param params
     * @throws SynchronizationException
     */
    public void performAction(JsonValue params) throws SynchronizationException {
        String reconId = params.get("reconId").required().asString();
        JsonValue context = ObjectSetContext.get();
        context.add("trigger", "recon");

        try {
            JsonValue rootContext = JsonResourceContext.getRootContext(context);
            Action action = params.get("action").required().asEnum(Action.class);
            SyncOperation op = null;
            ReconEntry entry = null;
            SynchronizationException exception = null;
            try {
                if (params.get("target").isNull()) {
                    SourceSyncOperation sop = new SourceSyncOperation();
                    op = sop;
                    sop.fromJsonValue(params);

                    entry = new ReconEntry(sop, rootContext, dateUtil);
                    entry.sourceId = LazyObjectAccessor.qualifiedId(sourceObjectSet, sop.getSourceObjectId());
                    if (null == sop.getSourceObject()) {
                        exception = new SynchronizationException(
                                "Source object " + entry.sourceId + " does not exists");
                        throw exception;
                    }
                    //TODO blank check
                    String targetId = params.get("targetId").asString();
                    if (null != targetId){
                        op.targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, targetId);
                        if (null == sop.getTargetObject()) {
                            exception = new SynchronizationException(
                                    "Target object " + targetId + " does not exists");
                            throw exception;
                        }
                    }
                    sop.assessSituation();
                } else {
                    TargetSyncOperation top = new TargetSyncOperation();
                    op = top;
                    top.fromJsonValue(params);
                    String targetId = params.get("targetId").required().asString();

                    entry = new ReconEntry(top, rootContext, dateUtil);
                    entry.targetId = LazyObjectAccessor.qualifiedId(targetObjectSet, targetId);
                    top.targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, targetId);
                    if (null == top.getTargetObject()) {
                        exception = new SynchronizationException(
                                "Target object " + entry.targetId + " does not exists");
                        throw exception;
                    }
                    top.assessSituation();
                }
                Situation situation = params.get("situation").required().asEnum(Situation.class);
                op.action = action;
                if (!situation.equals(op.situation)) {
                    exception = new SynchronizationException(  "Expected situation does not match. Expect: " + situation.name() + " Found: " + op.situation.name());
                    throw  exception;
                }
                op.performAction();
            } catch (SynchronizationException se) {
                if (op.action != Action.EXCEPTION) {
                    entry.status = Status.FAILURE; // exception was not intentional
                    LOGGER.warn("Unexpected failure during source reconciliation {}", reconId, se);
                }
                Throwable throwable = se;
                while (throwable.getCause() != null) { // want message associated with original cause
                    throwable = throwable.getCause();
                }
                if (se != throwable) {
                    entry.message = se.getMessage() + ". Root cause: " + throwable.getMessage();
                } else {
                    entry.message = throwable.getMessage();
                }
            }
            if (!Action.NOREPORT.equals(action) && (entry.status == Status.FAILURE || op.action != null)) {
                entry.timestamp = new Date();
                if (op instanceof SourceSyncOperation) {
                    entry.reconciling = "source";
                    if (op.getTargetObject() != null) {
                        entry.targetId = LazyObjectAccessor.qualifiedId(targetObjectSet, 
                                op.getTargetObject().get("_id").asString());
                    }
                    entry.setAmbiguousTargetIds(((SourceSyncOperation) op).getAmbiguousTargetIds());
                } else {
                    entry.reconciling = "target";
                    if (op.getSourceObject() != null) {
                        entry.sourceId = LazyObjectAccessor.qualifiedId(sourceObjectSet, 
                                op.getSourceObject().get("_id").asString());
                    }
                }
                logReconEntry(entry);
            }
            if (exception != null) {
                throw exception;
            }
        } finally {
            context.remove("trigger");
        }
    }

    private void doResults() throws SynchronizationException {
        if (resultScript != null) {
            Map<String, Object> scope = service.newScope();
            scope.put("source", sourceStats.asMap());
            scope.put("target", targetStats.asMap());
            scope.put("global", globalStats.asMap());
            try {
                resultScript.exec(scope);
            } catch (ScriptException se) {
                LOGGER.debug("{} result script encountered exception", name, se);
                throw new SynchronizationException(se);
            }
        }
    }

    /**
     * TEMPORARY. Future version will have this break-down into discrete units of work.
     * @param reconId
     * @throws org.forgerock.openidm.sync.SynchronizationException
     */
    private void doRecon(ReconciliationContext reconContext) throws SynchronizationException {

        String reconId = reconContext.getReconId();
        EventEntry measureIdQueries = Publisher.start(EVENT_RECON_ID_QUERIES, reconId, null);
        JsonValue context = ObjectSetContext.get();
        try {
            context.add("trigger", "recon");
            JsonValue rootContext = JsonResourceContext.getRootContext(context);
            logReconStart(reconId, rootContext, context);
            sourceStats = new ReconStats(reconId,sourceObjectSet);
            globalStats = new ReconStats(reconId,name);
            globalStats.start();

            // Get all the source and target identifiers before we assess the situations
            sourceStats.startAllIds();
            List sourceIds = queryAllIds(sourceObjectSet, reconContext);
            Iterator<String> sourceIdsIter = sourceIds.iterator();
            reconContext.setSourceIds(sourceIds); // TODO: consider if query/recon functionality should go in that class
            sourceStats.endAllIds();
            if (!sourceIdsIter.hasNext()) {
                throw new SynchronizationException("Cowardly refusing to perform reconciliation with an empty source object set");
            }
            
            targetStats = new ReconStats(reconId, targetObjectSet);
            targetStats.startAllIds();
            List<String> remainingTargetIds = queryAllIds(targetObjectSet, reconContext);
            reconContext.setTargetIds(new ArrayList(remainingTargetIds)); // TODO: cleanup
            targetStats.endAllIds();
            
            // Optionally get all links up front as well
            Map<String, Link> allLinks = null;
            if (prefetchLinks) {
                allLinks = Link.getLinksForMapping(ObjectMapping.this);
                //reconContext.setAllLinks(allLinks); // TODO: cleanup
            }
            
            measureIdQueries.end();

            EventEntry measureSource = Publisher.start(EVENT_RECON_SOURCE, reconId, null);
            sourceStats.start();
            while (sourceIdsIter.hasNext()) {
                checkCanceled(reconContext);
                String sourceId = sourceIdsIter.next();
                SourceSyncOperation op = new SourceSyncOperation();
                op.reconContext = reconContext;
                ReconEntry entry = new ReconEntry(op, rootContext, dateUtil);
                op.sourceObjectAccessor = new LazyObjectAccessor(service, sourceObjectSet, sourceId);
                if (allLinks != null) {
                    op.initializeLink(allLinks.get(sourceId));
                }
                entry.sourceId = LazyObjectAccessor.qualifiedId(sourceObjectSet, sourceId);
                sourceStats.entries++;
                op.reconId = reconId;
                try {
                    op.sync();
                } catch (SynchronizationException se) {
                    if (op.action != Action.EXCEPTION) {
                        entry.status = Status.FAILURE; // exception was not intentional
                        LOGGER.warn("Unexpected failure during source reconciliation {}", reconId, se);
                    }
                    Throwable throwable = se;
                    while (throwable.getCause() != null) { // want message associated with original cause
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
                    remainingTargetIds.remove(handledId);
                }

                if (!Action.NOREPORT.equals(op.action) && (entry.status == Status.FAILURE || op.action != null)) {
                    entry.timestamp = new Date();
                    entry.reconciling = "source";
                    if (op.hasTargetObject()) {
                        entry.targetId = LazyObjectAccessor.qualifiedId(targetObjectSet, op.getTargetObjectId());
                    }
                    entry.setAmbiguousTargetIds(op.getAmbiguousTargetIds());
                    logReconEntry(entry);
                }
            }
            sourceStats.end();
            measureSource.end();

            EventEntry measureTarget = Publisher.start(EVENT_RECON_TARGET, reconId, null);
            targetStats.start();
            for (String targetId : remainingTargetIds) {
                checkCanceled(reconContext);
                TargetSyncOperation op = new TargetSyncOperation();
                op.reconContext = reconContext;
                ReconEntry entry = new ReconEntry(op, rootContext, dateUtil);
                entry.targetId = LazyObjectAccessor.qualifiedId(targetObjectSet, targetId);
                targetStats.entries++;
                op.reconId = reconId;
                try {
                    op.targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, targetId);
                    op.sync();
                } catch (SynchronizationException se) {
                    if (op.action != Action.EXCEPTION) {
                        entry.status = Status.FAILURE; // exception was not intentional
                        LOGGER.warn("Unexpected failure during target reconciliation {}", reconId, se);
                    }
                    Throwable throwable = se;
                    while (throwable.getCause() != null) { // want message associated with original cause
                        throwable = throwable.getCause();
                    }
                    if (se != throwable) {
                        entry.message = se.getMessage() + ". Root cause: " + throwable.getMessage();
                    } else {
                        entry.message = throwable.getMessage();
                    }
                }
                if (!Action.NOREPORT.equals(op.action) && (entry.status == Status.FAILURE || op.action != null)) {
                    entry.timestamp = new Date();
                    entry.reconciling = "target";
                    if (op.getSourceObjectId() != null) {
                        entry.sourceId = LazyObjectAccessor.qualifiedId(sourceObjectSet, op.getSourceObjectId());
                    }
                    logReconEntry(entry);
                }
            }
            targetStats.end();
            measureTarget.end();
            globalStats.end();
            logReconEnd(reconId, rootContext, context);
            doResults();
        } finally {
            context.remove("trigger");
            reconContext.setComplete();
        }

// TODO: cleanup orphan link objects (no matching source or target) here
    }

    /**
     * Check if a given reconciliaiton instance has requested to be canceled.
     * The run should be aborted as soon as possible.
     * @param reconContext the specific recon run
     * @throws SynchronizationException if the reconciliation has been aborted
     */
    private void checkCanceled(ReconciliationContext reconContext) throws SynchronizationException {
        if (reconContext.isCanceled()) {
            throw new SynchronizationException("Reconciliation canceled: " + reconContext.getReconId());
        }
    }
    
    /**
     * TODO: Description.
     *
     * @param entry TODO.
     * @throws SynchronizationException TODO.
     */
    private void logReconEntry(ReconEntry entry) throws SynchronizationException {
        try {
            service.getRouter().create("audit/recon", entry.toJsonValue().asMap());
        } catch (ObjectSetException ose) {
            throw new SynchronizationException(ose);
        }
    }

    private void logReconStart(String reconId, JsonValue rootContext, JsonValue context) throws SynchronizationException {
        ReconEntry reconStartEntry = new ReconEntry(null, rootContext, ReconEntry.RECON_START, dateUtil);
        reconStartEntry.timestamp = new Date();
        reconStartEntry.reconId = reconId;
        reconStartEntry.message = "Reconciliation initiated by " + ActivityLog.getRequester(context);
        logReconEntry(reconStartEntry);
    }

    private void logReconEnd(String reconId, JsonValue rootContext, JsonValue context) throws SynchronizationException {
        ReconEntry reconEndEntry = new ReconEntry(null, rootContext, ReconEntry.RECON_END, dateUtil);
        reconEndEntry.timestamp = new Date();
        reconEndEntry.reconId = reconId;
        String simpleSummary = ReconStats.simpleSummary(globalStats, sourceStats, targetStats);
        reconEndEntry.message = simpleSummary;
        logReconEntry(reconEndEntry);
        LOGGER.info("Reconciliation completed. " + simpleSummary);
    }

    /*
     * Execute a sync engine action explicitly, without going through situation assessment.
     * @param sourceObject the source object if applicable to the action
     * @param targetObject the target object if applicable to the action
     * @param situation an optional situation that was originally assessed. Null if not the result of an earlier situation assessment.
     * @param action the explicit action to invoke
     * @param reconId an optional identifier for the recon context if this is done in the context of reconciliation
     */
    public void explicitOp(JsonValue sourceObject, JsonValue targetObject, Situation situation, Action action, String reconId)
            throws SynchronizationException {
        ExplicitSyncOperation linkOp = new ExplicitSyncOperation();
        linkOp.init(sourceObject, targetObject, situation, action, reconId);
        linkOp.sync();
    }

    /**
     * TODO: Description.
     */
     abstract class SyncOperation {

        /** TODO: Description. */
        public String reconId;
        /** An optional reconciliation context */
        public ReconciliationContext reconContext;
        /** Access to the source object */
        public LazyObjectAccessor sourceObjectAccessor;
        /** Access to the target object */
        public LazyObjectAccessor targetObjectAccessor;

        /** 
         * Holds the link representation
         * An initialized link can be interpreted as representing state retrieved from the repository,
         * i.e. a linkObject with id of null represents a link that does not exist (yet)
         */
        public Link linkObject = new Link(ObjectMapping.this); 
        /** TODO: Description. */
        public Situation situation;
        /** TODO: Description. */
        public Action action;
        public boolean ignorePostAction = false;
        public Policy activePolicy = null;

        /**
         * TODO: Description.
         *
         * @throws SynchronizationException TODO.
         */
        public abstract void sync() throws SynchronizationException;

        protected abstract boolean isSourceToTarget();
        
        
        /**
         * @return the source object, potentially loaded on demand and/or cached, or null if does not exist
         * @throws SynchronizationException if on-demand load of the object failed
         */
        protected JsonValue getSourceObject() throws SynchronizationException {
            if (sourceObjectAccessor == null || sourceObjectAccessor.getLocalId() == null) {
                return null;
            } else {
                return sourceObjectAccessor.getObject();
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
                return targetObjectAccessor.getObject();
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
            return targetObjectAccessor == null ? false : targetObjectAccessor.isLoaded();
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
                    defined = (sourceObjectAccessor.getObject() != null);
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
            if (targetObjectAccessor == null || (!isTargetLoaded() && targetObjectAccessor.getLocalId() == null)) {
                defined = false;
            } else {
                if (isTargetLoaded() && targetObjectAccessor.getObject() != null) {
                    // Check against already laoded/defined object first, without causing new load
                    defined = true;
                } else if (reconContext != null && reconContext.getTargetIds() != null) {
                    // If available, check against all queried existing IDs 
                    defined = reconContext.getTargetIds().contains(targetObjectAccessor.getLocalId());
                } else {
                    // If no lists of existing ids is available, do a load of the object to check 
                    defined = (targetObjectAccessor.getObject() != null);
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
            if (reconContext != null && reconContext.getTargetIds() != null) {
                // If available, check against all queried existing IDs 
                return (reconContext.getTargetIds().size() == 0);
            } else {
                return false;
            }
        }
        
        /**
         * Initializes the link representation
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

        protected Action getAction() {
            return (this.action == null ? Action.IGNORE : this.action);
        }

        /**
         * TODO: Description.
         * @param sourceAction sourceAction true if the {@link Action} is determined for the {@link SourceSyncOperation}
         * and false if the action is determined for the {@link TargetSyncOperation}.
         * @throws SynchronizationException TODO.
         */
        protected void determineAction(boolean sourceAction) throws SynchronizationException {
            if (situation != null) {
                action = situation.getDefaultAction(); // start with a reasonable default
                for (Policy policy : policies) {
                    if (situation == policy.getSituation()) {
                        activePolicy = policy;
                        action = activePolicy.getAction(sourceObjectAccessor, targetObjectAccessor, this);
// TODO: Consider limiting what actions can be returned for the given situation.
                        break;
                    }
                }
            }
            LOGGER.debug("Determined action to be {}", action);
        }

        /**
         * TODO: Description.
         *
         * @throws SynchronizationException TODO.
         */
        @SuppressWarnings("fallthrough")
        protected void performAction() throws SynchronizationException {
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
                                    throw new SynchronizationException("no source object to create target from");
                                }
                                if (getTargetObject() != null) {
                                    throw new SynchronizationException("target object already exists");
                                }
                                JsonValue createTargetObject = new JsonValue(new HashMap<String, Object>());
                                applyMappings(getSourceObject(), createTargetObject); // apply property mappings to target
                                targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, 
                                        createTargetObject.get("_id").asString(), createTargetObject);
                                execScript("onCreate", onCreateScript);

                                JsonValue context = ObjectSetContext.get();
                                // Allow the early link creation as soon as the target identifier is known
                                String sourceId = getSourceObjectId();
                                PendingLink.populate(context, ObjectMapping.this.name, sourceId, getSourceObject(), reconId,
                                        situation);

                                targetObjectAccessor = createTargetObject(createTargetObject);
                                boolean wasLinked = PendingLink.wasLinked(context);
                                if (wasLinked) {
                                    LOGGER.debug(
                                            "Pending link for {} during {} has already been created, skipping additional link processing",
                                            sourceId, reconId);
                                    break;
                                } else {
                                    LOGGER.debug(
                                            "Pending link for {} during {} not yet resolved, proceed to link processing",
                                            sourceId, reconId);
                                    PendingLink.clear(context); // We'll now handle link creation ourselves
                                }
                                // falls through to link the newly created target
                            case UPDATE:
                            case LINK:
                                String targetId = getTargetObjectId();
                                if (getTargetObjectId() == null) {
                                    throw new SynchronizationException("no target object to link");
                                }

                                if (linkObject._id == null) {
                                    try {
                                        createLink(getSourceObjectId(), targetId, reconId);
                                    } catch (SynchronizationException ex) {
                                        // Allow for link to have been created in the meantime, e.g. programmatically
                                        // create would fail with a failed precondition for link already existing
                                        // Try to read again to see if that is the issue
                                        linkObject.getLinkForSource(getSourceObjectId());
                                        if (linkObject._id == null) {
                                            LOGGER.warn("Failed to create link between {}-{}", 
                                                    new Object[] {LazyObjectAccessor.qualifiedId(sourceObjectSet, getSourceObjectId()), 
                                                    LazyObjectAccessor.qualifiedId(targetObjectSet, targetId), ex});
                                            throw ex; // it was a different issue
                                        }
                                    }
                                }
                                if (linkObject._id != null && !targetId.equals(linkObject.targetId)) {
                                    linkObject.targetId = targetId;
                                    linkObject.update();
                                }
// TODO: Detect change of source id, and update link accordingly.
                                if (action == Action.CREATE || action == Action.LINK) {
                                    break; // do not update target
                                }
                                if (getSourceObject() != null && getTargetObject() != null) {
                                    JsonValue oldTarget = getTargetObject().copy();
                                    applyMappings(getSourceObject(), getTargetObject());
                                    execScript("onUpdate", onUpdateScript);
                                    if (JsonPatch.diff(oldTarget, getTargetObject())
                                            .size() > 0) { // only update if target changes
                                        updateTargetObject(getTargetObject());
                                    }
                                }
                                break; // terminate UPDATE
                            case DELETE:
                                if (getTargetObjectId() != null) { // forgiving; does nothing if no target
                                    execScript("onDelete", onDeleteScript);
                                    deleteTargetObject(getTargetObject());
                                    targetObjectAccessor = null;
                                }
                                // falls through to unlink the deleted target
                            case UNLINK:
                                if (linkObject._id != null) { // forgiving; does nothing if no link exists
                                    execScript("onUnlink", onUnlinkScript);
                                    linkObject.delete();
                                }
                                break; // terminate DELETE and UNLINK
                            case EXCEPTION:
                                throw new SynchronizationException(); // aborts change; recon reports
                        }
                    } catch (JsonValueException jve) {
                        throw new SynchronizationException(jve);
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
                        postAction(isSourceToTarget());
                    }
                    break;
                case ASYNC:
                case IGNORE:
            }
        }

        /**
         * TODO: Description.
         * @param sourceAction sourceAction true if the {@link Action} is determined for the {@link SourceSyncOperation}
         * and false if the action is determined for the {@link TargetSyncOperation}.
         * @throws SynchronizationException TODO.
         */
        protected void postAction(boolean sourceAction) throws SynchronizationException {
            if (null != activePolicy) {
                activePolicy.evaluatePostAction(sourceObjectAccessor, targetObjectAccessor, action, sourceAction);
            }
        }

        protected void createLink(String sourceId, String targetId, String reconId) throws SynchronizationException {
            Link linkObject = new Link(ObjectMapping.this);
            execScript("onLink", onLinkScript);
            linkObject.sourceId = sourceId;
            linkObject.targetId = targetId;
            linkObject.create();
            LOGGER.debug("Established link sourceId: {} targetId: {} in reconId: {}", new Object[] {sourceId, targetId, reconId});
        }

        /**
         * TODO: Description.
         *
         * @return TODO.
         * @throws SynchronizationException TODO.
         */
        protected boolean isSourceValid() throws SynchronizationException {
            boolean result = false;
            if (hasSourceObject()) { // must have a source object to be valid
                if (validSource != null) {
                    Map<String, Object> scope = service.newScope();
                    // TODO: This forced load into mmeory is necessary until we can do on demand get in script engine
                    scope.put("source", getSourceObject().asMap());
                    try {
                        Object o = validSource.exec(scope);
                        if (o == null || !(o instanceof Boolean)) {
                            throw new SynchronizationException("Expecting boolean value from validSource");
                        }
                        result = (Boolean) o;
                    } catch (ScriptException se) {
                        LOGGER.debug("{} validSource script encountered exception", name, se);
                        throw new SynchronizationException(se);
                    }
                } else { // no script means true
                    result = true;
                }
            }
            LOGGER.trace("isSourceValid of {} evaluated: {}", getSourceObjectId(), result);
            return result;
        }

        /**
         * TODO: Description.
         *
         * @return TODO.
         * @throws SynchronizationException TODO.
         */
        protected boolean isTargetValid() throws SynchronizationException {
            boolean result = false;
            if (hasTargetObject()) { // must have a target object to qualify
                if (validTarget != null) {
                    Map<String, Object> scope = service.newScope();
                    scope.put("target", getTargetObject().asMap());
                    try {
                        Object o = validTarget.exec(scope);
                        if (o == null || !(o instanceof Boolean)) {
                            throw new SynchronizationException("Expecting boolean value from validTarget");
                        }
                        result = (Boolean) o;
                    } catch (ScriptException se) {
                        LOGGER.debug("{} validTarget script encountered exception", name, se);
                        throw new SynchronizationException(se);
                    }
                } else { // no script means true
                    result = true;
                }
            }
            LOGGER.trace("isTargetValid of {} evaluated: {}", getTargetObjectId(), result);
            return result;
        }

        /**
         * Executes the given script with the appropriate context information
         *
         * @param type The script hook name
         * @param script The script to execute
         * @throws SynchronizationException TODO.
         */
        private void execScript(String type, Script script) throws SynchronizationException {
            if (script != null) {
                Map<String, Object> scope = service.newScope();
                // TODO: Once script engine can do on-demand get replace these forced loads
                if (getSourceObjectId() != null) {
                    scope.put("source", getSourceObject().asMap());
                }
                // Target may not have ID yet, e.g. an onCreate with the target object defined, 
                // but not stored/id assigned.
                if (isTargetLoaded() || getTargetObjectId() != null) {
                    if (getTargetObject() != null) {
                        scope.put("target", getTargetObject().asMap());
                    }
                }
                if (situation != null) {
                    scope.put("situation", situation.toString());
                }
                try {
                    script.exec(scope);
                } catch (ScriptException se) {
                    LOGGER.debug("{} script encountered exception", name + " " + type, se);
                    throw new SynchronizationException(se);
                }
            }
        }
    }

    /**
     * Explicit execution of a sync operation where the appropriate
     * action is known without having to assess the situation and apply
     * policy to decide the action
     */
    private class ExplicitSyncOperation extends SyncOperation {

        protected boolean isSourceToTarget() {
            //TODO: detect by the source id match
            return true;
        }

        public void init(JsonValue sourceObject, JsonValue targetObject, Situation situation, Action action, String reconId) {
            this.reconId = reconId;
            this.sourceObjectAccessor = new LazyObjectAccessor(service, sourceObjectSet, sourceObject.get("_id").required().asString(), sourceObject);
            this.targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, targetObject.get("_id").required().asString(), targetObject);
            this.situation = situation;
            this.action = action;
            this.ignorePostAction = true;
        }

        @Override
        public void sync() throws SynchronizationException {
            LOGGER.debug("Initiate explicit operation call for situation: {}, action: {}", situation, action);
            performAction();
            LOGGER.debug("Complected explicit operation call for situation: {}, action: {}", situation, action);
        }
    }

    /**
     * TODO: Description.
     */
    class SourceSyncOperation extends SyncOperation {

        // If it can not uniquely identify a target, the list of ambiguous target ids
        public List<String> ambiguousTargetIds;

        @Override
        @SuppressWarnings("fallthrough")
        public void sync() throws SynchronizationException {
            EventEntry measureSituation = Publisher.start(EVENT_SOURCE_ASSESS_SITUATION, getSourceObjectId(), null);
            try {
                assessSituation();
            } finally {
                measureSituation.end();
            }
            EventEntry measureDetermine = Publisher.start(EVENT_SOURCE_DETERMINE_ACTION, getSourceObjectId(), null);
            try {
                determineAction(true);
            } finally {
                measureDetermine.end();
            }
            EventEntry measurePerform = Publisher.start(EVENT_SOURCE_PERFORM_ACTION, getSourceObjectId(), null);
            try {
                performAction();
            } finally {
                measurePerform.end();
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

        public void fromJsonValue(JsonValue params) {
            reconId = params.get("reconId").required().asString();
            sourceObjectAccessor = new LazyObjectAccessor(service, sourceObjectSet, params.get("sourceId").required().asString());
            ignorePostAction = params.get("ignorePostAction").defaultTo(false).asBoolean();
        }

        public JsonValue toJsonValue() {
            JsonValue actionParam = new JsonValue(new HashMap<String,Object>());
            actionParam.put("reconId", reconId);
            actionParam.put("mapping", ObjectMapping.this.getName());
            actionParam.put("situation", situation.name());
            actionParam.put("action", situation.getDefaultAction().name());
            actionParam.put("sourceId", getSourceObjectId());
            if (targetObjectAccessor != null && targetObjectAccessor.getLocalId() != null) {
                actionParam.put("targetId", targetObjectAccessor.getLocalId());
            }
            return actionParam;
        }
        /**
         * TODO: Description.
         *
         * @throws SynchronizationException TODO.
         */
        private void assessSituation() throws SynchronizationException {
            situation = null;
            if (getSourceObjectId() != null && linkObject.initialized == false) { // In case the link was not pre-read get it here
                linkObject.getLinkForSource(getSourceObjectId());
            }
            if (linkObject._id != null) {
                targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, linkObject.targetId);
            }
            if (isSourceValid()) { // source is valid for mapping
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
                        JsonValue resultValue = results.get((Integer) 0).required();
                        targetObjectAccessor = getCorrelatedTarget(resultValue);
                        situation = Situation.FOUND;
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
                if (sourceStats != null) {
                    sourceStats.addNotValid(getSourceObjectId());
                }
            }
            if (sourceStats != null){
                sourceStats.addSituation(getSourceObjectId(), situation);
            }
            LOGGER.debug("Mapping '{}' assessed situation of {} to be {}", new Object[]{name, getSourceObjectId(), situation});
        }

        /**
         * TODO: Description.
         *
         * @return TODO.
         * @throws SynchronizationException TODO.
         */
        @SuppressWarnings("unchecked")
        private JsonValue correlateTarget() throws SynchronizationException {
            JsonValue result = null;
            // TODO: consider if there are cases where this would better be lazy and not get the full target
            if (hasTargetObject()) {
                result = new JsonValue(new ArrayList<Map<String, Object>>(1));
                result.add(0, getTargetObject());
            } else if (correlationQuery != null && (correlateEmptyTargetSet || !hadEmptyTargetObjectSet())) {
                EventEntry measure = Publisher.start(EVENT_CORRELATE_TARGET, getSourceObject(), null);
                
                Map<String, Object> queryScope = service.newScope();
                queryScope.put("source", getSourceObject().asMap());
                try {
                    Object query = correlationQuery.exec(queryScope);
                    if (query == null || !(query instanceof Map)) {
                        throw new SynchronizationException("Expected correlationQuery script to yield a Map");
                    }
                    result = new JsonValue(queryTargetObjectSet((Map)query)).get(QueryConstants.QUERY_RESULT).required();
                } catch (ScriptException se) {
                    LOGGER.debug("{} correlationQuery script encountered exception", name, se);
                    throw new SynchronizationException(se);
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
                fullObj = new LazyObjectAccessor(service, targetObjectSet, resultValue.get("_id").required().asString(), resultValue);
            } else {
                fullObj = new LazyObjectAccessor(service, targetObjectSet, resultValue.get("_id").required().asString());
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
    }

    /**
     * TODO: Description.
     */
    class TargetSyncOperation extends SyncOperation {

        @Override
        public void sync() throws SynchronizationException {
            EventEntry measureSituation = Publisher.start(EVENT_TARGET_ASSESS_SITUATION, targetObjectAccessor, null);
            try {
                assessSituation();
            } finally {
                measureSituation.end();
            }
            EventEntry measureDetermine = Publisher.start(EVENT_TARGET_DETERMINE_ACTION, targetObjectAccessor, null);
            try {
                determineAction(false);
            } finally {
                measureDetermine.end();
            }
            EventEntry measurePerform = Publisher.start(EVENT_TARGET_PERFORM_ACTION, targetObjectAccessor, null);
            try {
// TODO: Option here to just report what action would be performed?
                performAction();
            } finally {
                measurePerform.end();
            }
        }

        protected boolean isSourceToTarget() {
            return false;
        }

        public void fromJsonValue(JsonValue params) {
            reconId = params.get("reconId").required().asString();
            ignorePostAction = params.get("ignorePostAction").defaultTo(false).asBoolean();
        }

        public JsonValue toJsonValue() {
            JsonValue actionParam = new JsonValue(new HashMap<String,Object>());
            actionParam.put("reconId",reconId);
            actionParam.put("mapping",ObjectMapping.this.getName());
            actionParam.put("situation",situation.name());
            actionParam.put("action",situation.getDefaultAction().name());
            actionParam.put("target","true");
            if (targetObjectAccessor != null && targetObjectAccessor.getLocalId() != null) {
                actionParam.put("targetId", targetObjectAccessor.getLocalId());
            }
            return actionParam;
        }

        /**
         * TODO: Description.
         *
         * @throws SynchronizationException TODO.
         */
        private void assessSituation() throws SynchronizationException {
            situation = null;
            String targetId = getTargetObjectId();
            if (!isTargetValid()) { // target is not valid for this mapping; ignore it
                situation = Situation.TARGET_IGNORED;
                if (targetStats != null && targetId != null) {
                    targetStats.addNotValid(targetId);
                    targetStats.addSituation(targetId, situation);
                }
                return;
            }
            if (targetId != null) {
                linkObject.getLinkForTarget(targetId);
            }
            if (linkObject._id == null || linkObject.sourceId == null) {
                situation = Situation.UNASSIGNED;
            } else {
                sourceObjectAccessor = new LazyObjectAccessor(service, sourceObjectSet, linkObject.sourceId);
                if (getSourceObject() != null) { // force load to double check
                    situation = Situation.SOURCE_MISSING;
                } else if (!isSourceValid()) {
                    situation = Situation.UNQUALIFIED; // Should not happen as done in source phase
                    LOGGER.warn("Unexpected situation in target reconciliation {} {} {} {}", new Object[] {situation, getSourceObject(), targetId, linkObject});
                } else { // proper link
                    situation = Situation.CONFIRMED; // Should not happen as done in source phase
                    LOGGER.warn("Unexpected situation in target reconciliation {} {} {} {}", new Object[] {situation, getSourceObject(), targetId, linkObject});
                }
            }
            if (targetStats != null){
                targetStats.addSituation(targetId, situation);
            }
        }
    }

    /**
     * TEMPORARY.
     */
    private class ReconEntry {

        public final static String RECON_START = "start";
        public final static String RECON_END = "summary";
        public final static String RECON_ENTRY = ""; // regular reconciliation entry has an empty entry type

        /** Type of the audit log entry. Allows for marking recon start / summary records */
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

        // A comma delimited formatted representation of any ambiguous identifiers
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
            return (reconId == null && op != null) ? op.reconId : reconId;
        }

        /**
         * Constructor that allows specifying the type of reconciliation log entry
         */
        public ReconEntry(SyncOperation op, JsonValue rootContext, String entryType, DateUtil dateUtil) {
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
            jv.put("situation", ((op == null || op.situation == null) ? null : op.situation.toString()));
            jv.put("action", ((op == null || op.action == null) ? null : op.action.toString()));
            jv.put("status", (status == null ? null : status.toString()));
            jv.put("message", message);
            return jv;
        }
    }
}
