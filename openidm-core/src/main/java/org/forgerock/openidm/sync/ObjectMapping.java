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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

/*
 * TODO:
 * This implementation was rushed; do some consolidation and cleanup work.
 * Make ObjectSet methods use JsonNode instead of Map; will clean up a lot of this code.
 * Did I mention the part where this implementation was rushed?
 */

package org.forgerock.openidm.sync;

// Java Standard Edition
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JSON-Fluent library
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

// ForgeRock OpenIDM
import org.forgerock.openidm.context.InvokeContext;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.Scripts;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
class ObjectMapping implements SynchronizationListener {

    /** TODO: Description. */
    private enum Status { SUCCESS, FAILURE }

    /** TODO: Description. */
    private final static Logger LOGGER = LoggerFactory.getLogger(ObjectMapping.class);

    /** TODO: Description. */
    private String name;

    /** TODO: Description. */
    private String sourceObjectSet;

    /** TODO: Description. */
    private String targetObjectSet;

    /** TODO: Description. */
    private Script validSource;

    /** TODO: Description. */
    private Script validTarget;

    /** TODO: Description. */
    private TargetCorrelation targetCorrelation;

    /** TODO: Description. */
    private ArrayList<PropertyMapping> properties = new ArrayList<PropertyMapping>();

    /** TODO: Description. */
    private ArrayList<Policy> policies = new ArrayList<Policy>();

    /** TODO: Description. */
    private Script onCreateScript;

    /** TODO: Description. */
    private Script onUpdateScript;

    /** TODO: Description. */
    private SynchronizationService service;

    /**
     * TODO: Description.
     *
     * @param config TODO.
     * @throws JsonNodeException TODO.
     */
    public ObjectMapping(SynchronizationService service, JsonNode config) throws JsonNodeException {
        this.service = service;
        name = config.get("name").required().asString();
        sourceObjectSet = config.get("source").required().asString();
        targetObjectSet = config.get("target").required().asString();
        validSource = Scripts.newInstance(config.get("validSource"));
        validTarget = Scripts.newInstance(config.get("validTarget"));
        if (config.isDefined("targetCorrelation")) {
            targetCorrelation = new TargetCorrelation(config.get("targetCorrelation"));
        }
        for (JsonNode node : config.get("properties").expect(List.class)) {
            properties.add(new PropertyMapping(node));
        }
        for (JsonNode node : config.get("policies").expect(List.class)) {
            policies.add(new Policy(node));
        }
        onCreateScript = Scripts.newInstance(config.get("onCreate"));
        onUpdateScript = Scripts.newInstance(config.get("onUpdate"));
        LOGGER.debug("Instantiated object mapping: {}", name + ": " + sourceObjectSet + "->" + targetObjectSet);
    }

    /**
     * TODO: Description.
     */
    public String getName() {
        return name;
    }

    /**
     * TODO: Description.
     *
     * @throws SynchronizationException TODO.
     */
    ObjectSet getRouter() throws SynchronizationException {
        return service.getRouter();
    }

    /**
     * TODO: Description.
     *
     * @param id fully-qualified source object identifier.
     * @return operation to perform for the given source object identifier.
     */
    private SourceSyncOperation newSyncOperation(String sourceId) {
        String[] split = new String[2];
        if (sourceId.equals(sourceObjectSet)) {
            split[0] = sourceObjectSet;
        } else if (sourceId.startsWith(sourceObjectSet + '/')) {
            split[0] = sourceObjectSet;
            if (sourceId.length() > sourceObjectSet.length() + 1) {
                split[1] = sourceId.substring(sourceObjectSet.length() + 1); // skip the slash
            }
        }
// TODO: one day bifurcate this for synchronous and asynchronous source operation
        SourceSyncOperation op = null;
        if (split[0] != null) { // operation is against source object set
            try {
                JsonNode source = readObject(sourceObjectSet, split[1]);
                op = new SourceSyncOperation();
                op.sourceObject = source;
            } catch (SynchronizationException se) {
                // failure to read source object is ignored
            }
        }
        return op;
    }

    /**
     * TODO: Description.
     *
     * @param query TODO.
     * @return TODO.
     * @throws SynchronizationException TODO.
     */
    private Map<String, Object> queryTargetObjectSet(Map<String, Object> query) throws SynchronizationException {
        try {
            return getRouter().query(targetObjectSet, query);
        } catch (ObjectSetException ose) {
            throw new SynchronizationException(ose);
        }
    }

    /**
     * TODO: Description.
     *
     * @param objectSet TODO.
     * @return TODO.
     */
// TODO: Codify query-all-ids in ObjectSet or provide per-ObjectSet query for all IDs.
    private Iterable<String> queryAllIds(final String objectSet) throws SynchronizationException {
        return new Iterable<String>() {
            JsonNode list;
            {
                HashMap<String, Object> query = new HashMap<String, Object>();
                query.put(QueryConstants.QUERY_ID, "query-all-ids");
                try {
                    list = new JsonNode(getRouter().query(objectSet, query)).
                     get(QueryConstants.QUERY_RESULT).required().expect(List.class);
                } catch (JsonNodeException jne) {
                    throw new SynchronizationException(jne);
                } catch (ObjectSetException ose) {
                    throw new SynchronizationException(ose);
                }
            }
            @Override public Iterator<String> iterator() {
                final Iterator<JsonNode> iterator = list.iterator();
                return new Iterator() {
                    @Override public boolean hasNext() {
                        return iterator.hasNext();
                    }
                    @Override public String next() { // throws JsonNodeException
                        return iterator.next().get("_id").asString();
                    }
                    @Override public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * TODO: Description.
     *
     * @param objectSet TODO.
     * @param id TODO.
     * @throws NullPointerException if {@code targetId} is {@code null}.
     * @throws SynchronizationException TODO.
     */
    private JsonNode readObject(String objectSet, String id) throws SynchronizationException {
        if (id == null) {
            throw new NullPointerException();
        }
        try {
            return new JsonNode(getRouter().read(objectSet + '/' + id));
        } catch (NotFoundException nfe) { // target not found results in null
            return null;
        } catch (ObjectSetException ose) {
            LOGGER.debug("failed to read target object", ose);
            throw new SynchronizationException(ose);
        }
    }

// TODO: maybe move all this target stuff into a target object wrapper to keep this class clean
    /**
     * TODO: Description.
     *
     * @param targetId TODO.
     * @throws SynchronizationException TODO.
     */
    private void createTargetObject(JsonNode target) throws SynchronizationException {
        StringBuilder sb = new StringBuilder();
        sb.append(targetObjectSet);
        if (target.get("_id").isString()) {
            sb.append('/').append(target.get("_id").asString());
        }
        try {
            getRouter().create(sb.toString(), target.asMap());
        } catch (JsonNodeException jne) {
            throw new SynchronizationException(jne);
        } catch (ObjectSetException ose) {
            LOGGER.debug("failed to create target object", ose);
            throw new SynchronizationException(ose);
        }
    }

// TODO: maybe move all this target stuff into a target object wrapper to keep this class clean
    /**
     * TODO: Description.
     *
     * @param targetId TODO.
     * @throws SynchronizationException TODO.
     */
    private void updateTargetObject(JsonNode target) throws SynchronizationException {
        try {
            getRouter().update(targetObjectSet + '/' + target.get("_id").required().asString(),
             target.get("_rev").asString(), target.asMap());
        } catch (JsonNodeException jne) {
            throw new SynchronizationException(jne);
        } catch (ObjectSetException ose) {
            LOGGER.debug("failed to update target object", ose);
            throw new SynchronizationException(ose);
        }
    }

// TODO: maybe move all this target stuff into a target object wrapper to keep this class clean
    /**
     * TODO: Description.
     *
     * @param target TODO.
     * @throws SynchronizationException TODO.
     */
    private void deleteTargetObject(JsonNode target) throws SynchronizationException {
        if (target.get("_id").isString()) { // forgiving delete
            try {
                getRouter().delete(targetObjectSet + '/' + target.get("_id").asString(),
                 target.get("_rev").asString());
            } catch (JsonNodeException jne) {
                throw new SynchronizationException(jne);
            } catch (NotFoundException nfe) {
                // forgiving delete
            } catch (ObjectSetException ose) {
                LOGGER.debug("failed to delete target object", ose);
                throw new SynchronizationException(ose);
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
    private void applyMappings(JsonNode source, JsonNode target) throws SynchronizationException {
        for (PropertyMapping property : properties) {
            property.apply(source, target);
        }
    }

    @Override
    public void onCreate(String id, Map<String, Object> object) throws SynchronizationException {
        SyncOperation op = newSyncOperation(id); // synchronous for now
        if (op != null) {
            op.sourceObject = new JsonNode(object);
            op.sync();
        }
    }
    
    @Override
    public void onUpdate(String id, Map<String, Object> newValue) throws SynchronizationException {
        onUpdate(id, null, newValue);
    }

    @Override
    public void onUpdate(String id, Map<String, Object> oldValue, Map<String, Object> newValue)
    throws SynchronizationException {
        SyncOperation op = newSyncOperation(id); // synchronous for now
        if (op != null) {
// TODO: accommodate oldValue when asynchronous operations are supported
            op.sourceObject = new JsonNode(newValue);
            op.sync();
        }
    }

    @Override
    public void onDelete(String id) throws SynchronizationException {
        SyncOperation op = newSyncOperation(id); // synchronous for now
        if (op != null) {
            op.sync();
        }
    }

    public void recon(String reconId) throws SynchronizationException {
        InvokeContext.getContext().pushActivityId(reconId);
        try {
            doRecon(reconId);
        } finally {
            InvokeContext.getContext().popActivityId();
        }
    }
    
    /**
     * TEMPORARY. Future version will have this break-down into discrete units of work.
     */
    private void doRecon(String reconId) throws SynchronizationException {
        for (String sourceId : queryAllIds(sourceObjectSet)) {
            SourceSyncOperation op = new SourceSyncOperation();
            ReconEntry entry = new ReconEntry(op);
            entry.sourceId = sourceObjectSet + '/' + sourceId;
            op.sourceObject = readObject(sourceObjectSet, sourceId);
            op.reconId = reconId;
            try {
                op.sync();
            } catch (SynchronizationException se) {
                if (op.action != Action.EXCEPTION) {
                    entry.status = Status.FAILURE; // exception was not intentional
                }
                Throwable throwable = se;
                while (throwable.getCause() != null) { // want message associated with original cause
                    throwable = throwable.getCause();
                }
                entry.message = throwable.getMessage();
            }
            if (entry.status == Status.FAILURE || op.action != null) {
                entry.reconciling = "source";
                entry.targetId = qualifiedId(targetObjectSet, op.targetObject);
                logReconEntry(entry);
            }
        }
        for (String targetId : queryAllIds(targetObjectSet)) {
            TargetSyncOperation op = new TargetSyncOperation();
            ReconEntry entry = new ReconEntry(op);
            entry.targetId = targetObjectSet + '/' + targetId;
            op.targetObject = readObject(targetObjectSet, targetId);
            op.reconId = reconId;
            try {
                op.sync();
            } catch (SynchronizationException se) {
                if (op.action != Action.EXCEPTION) {
                    entry.status = Status.FAILURE; // exception was not intentional
                }
                Throwable throwable = se;
                while (throwable.getCause() != null) { // want message associated with original cause
                    throwable = throwable.getCause();
                }
                entry.message = throwable.getMessage();
            }
            if (entry.status == Status.FAILURE || op.action != null) {
                entry.reconciling = "target";
                entry.sourceId = qualifiedId(sourceObjectSet, op.sourceObject);
                logReconEntry(entry);
            }
        }
// TODO: cleanup orphan link objects (no matching source or target) here 
    }

    /**
     * Qualified Id if the object is not null, null if the object is null
     */
    private String qualifiedId(String objSet, JsonNode obj) {
        if (obj == null) {
            return null;
        }
        return objSet + '/' + obj.get("_id").getValue();
    }
    
    /**
     * TODO: Description.
     *
     * @param entry TODO.
     */
    private void logReconEntry(ReconEntry entry) throws SynchronizationException {
        try {
            getRouter().create("audit/recon", entry.toJsonNode().asMap());
        } catch (ObjectSetException ose) {
            throw new SynchronizationException(ose);
        }
    }

    /**
     * TODO: Description.
     */
    private abstract class SyncOperation {

        /** TODO: Description. */
        public String reconId;

        /** TODO: Description. */
        public JsonNode sourceObject;

        /** TODO: Description. */
        public JsonNode targetObject;

        /** TODO: Description. */
        public final Link linkObject = new Link(ObjectMapping.this);

        /** TODO: Description. */
        public Situation situation;

        /** TODO: Description. */
        public Action action;

        /**
         * TODO: Description.
         *
         * @throws SynchronizationException TODO.
         */
        public abstract void sync() throws SynchronizationException;

        /**
         * TODO: Description.
         *
         * @throws SynchronizationException TODO.
         */
        protected void determineAction() throws SynchronizationException {
            if (situation != null) {
                action = situation.getDefaultAction(); // start with a reasonable default
                for (Policy policy : policies) {
                    if (situation == policy.getSituation()) {
                        action = policy.getAction();
// TODO: Consider limiting what actions can be returned for the given situation.
                        break;
                    }
                }
            }
        }

        /**
         * TODO: Description.
         *
         * @throws SynchronziationException TODO.
         */
        protected void performAction() throws SynchronizationException {
            Action action = (this.action == null ? Action.IGNORE : this.action);
            try {
                switch (action) {
                case CREATE:
                    if (sourceObject == null) {
                        throw new SynchronizationException("no source object to create target from"); 
                    }
                    if (linkObject._id != null || targetObject != null) {
                        throw new SynchronizationException("target object already exists");
                    }
                    targetObject = new JsonNode(new HashMap<String, Object>());
                    applyMappings(sourceObject, targetObject); // apply property mappings to target
                    execScript(onCreateScript);
                    createTargetObject(targetObject);
                    // falls through to link the newly created target
                case LINK:
                    if (targetObject == null) {
                        throw new SynchronizationException("no target object to link");
                    }
                    linkObject.sourceId = sourceObject.get("_id").required().asString();
                    linkObject.targetId = targetObject.get("_id").required().asString();
                    linkObject.reconId = reconId;
                    linkObject.create();
                    if (action == Action.CREATE) {
                        break; // already created; no need to update again
                    }
                    // falls through to update the linked target
                case IGNORE:
                    if (sourceObject != null && targetObject != null) {
                        applyMappings(sourceObject, targetObject);
// TODO: Detect nothing updated to avoid unnecessary updates and potential cyclic updates.
                        execScript(onUpdateScript);
                        updateTargetObject(targetObject);
                    }
                    if (linkObject._id != null && reconId != null) { // note reconId in link
                        linkObject.reconId = reconId;
                        linkObject.update();
                    }
                         
                    break; // terminate LINK and IGNORE
                case DELETE:
                    if (targetObject != null) { // forgiving; does nothing if no target
                        deleteTargetObject(targetObject);
                        targetObject = null;
                    }
                    // falls through to unlink the deleted target
                case UNLINK:
                    if (linkObject._id != null) { // forgiving; does nothing if no link exists
                        linkObject.delete();
                    }
                    break; // terminate DELETE and UNLINK
                case EXCEPTION:
                    throw new SynchronizationException(); // aborts change; recon reports
                }
            }
            catch (JsonNodeException jne) {
                throw new SynchronizationException(jne);
            }
        }

        /**
         * TODO: Description.
         *
         * @return TODO.
         * @throws SynchronizationException TODO.
         */
        protected boolean isSourceValid() throws SynchronizationException {
            boolean result = false;
            if (sourceObject != null) { // must have a source object to be valid
                if (validSource != null) {
                    HashMap<String, Object> scope = new HashMap<String, Object>();
                    scope.put("source", sourceObject);
                    try {
                        Object o = validSource.exec(scope);
                        if (o == null || !(o instanceof Boolean)) {
                            throw new SynchronizationException("expecting boolean value from validSource");
                        }
                        result = (Boolean)o;
                    } catch (ScriptException se) {
                        throw new SynchronizationException(se);
                    }
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
         * @throws SynchronizationException TODO.
         */
        protected boolean isTargetValid() throws SynchronizationException {
            boolean result = false;
            if (targetObject != null) { // must have a target object to qualify
                if (validTarget != null) {
                    HashMap<String, Object> scope = new HashMap<String, Object>();
                    scope.put("target", targetObject);
                    try {
                        Object o = validTarget.exec(scope);
                        if (o == null || !(o instanceof Boolean)) {
                            throw new SynchronizationException("expecting boolean value from validTarget");
                        }
                        result = (Boolean)o;
                    } catch (ScriptException se) {
                        throw new SynchronizationException(se);
                    }
                } else { // no script means true
                    result = true;
                }
            }
            return result;
        }

        /**
         * TODO: Description.
         *
         * @param script TODO.
         * @throws SynchronizationException TODO.
         */
        private void execScript(Script script) throws SynchronizationException {
            if (script != null) {
                HashMap<String, Object> scope = new HashMap<String, Object>();
                scope.put("source", sourceObject);
                scope.put("target", targetObject);
                scope.put("situation", situation.toString());
                try {
                    script.exec(scope);
                } catch (ScriptException se) {
                    throw new SynchronizationException(se);
                }
            }
        }
    }

    /**
     * TODO: Description.
     */
    private class SourceSyncOperation extends SyncOperation {

        @Override
        public void sync() throws SynchronizationException {
            assessSituation();
            determineAction();
            performAction();
        }

        /**
         * TODO: Description.
         *
         * @throws SynchronizationException TODO.
         */
        private void assessSituation() throws SynchronizationException {
            situation = null;
            String sourceId = (sourceObject != null ? sourceObject.get("_id").asString() : null);
            if (sourceId != null) {
                linkObject.getLinkForSource(sourceId);
            }
            if (linkObject._id != null) {
                targetObject = readObject(targetObjectSet, linkObject.targetId);
            }
            if (isSourceValid()) { // source is valid for mapping
                if (linkObject._id != null) { // source object linked to target
                    if (targetObject != null) {
                        situation = Situation.CONFIRMED;
                    } else {
                        situation = Situation.MISSING;
                    }
                }
                else { // source object not linked to target
                    List<String> targets = correlateTarget();
                    if (targets.size() == 1) {
                        targetObject = readObject(targetObjectSet, targets.get(0));
                        situation = Situation.FOUND;
                    } else if (targets.size() == 0) {
                        situation = Situation.ABSENT;
                    } else {
                        situation = Situation.AMBIGUOUS;
                    }
                }
            }
            else { // mapping does not qualify for target
                if (linkObject._id != null) {
                    situation = Situation.UNQUALIFIED;
                } else {
                    situation = null; // TODO: provide a situation for this?
                }
            }
        }

        /**
         * TODO: Description.
         *
         * @return TODO.
         * @throws SynchronizationException TODO.
         */
        private List<String> correlateTarget() throws SynchronizationException {
            ArrayList<String> results = new ArrayList<String>();
            if (targetCorrelation != null) {
                HashMap<String, Object> queryScope = new HashMap<String, Object>();
                queryScope.put("source", sourceObject);
                try {
                    Object query = targetCorrelation.getQuery().exec(queryScope);
                    if (query == null || !(query instanceof Map)) {
                        throw new SynchronizationException("expected targetCorrelation query to yield a Map");
                    }
                    Map<String, Object> queryResult = queryTargetObjectSet((Map)query);
                    HashMap<String, Object> filterScope = new HashMap<String, Object>();
                    filterScope.put("result", queryResult);
                    Object filtered = targetCorrelation.getFilter().exec(filterScope);
                    if (filtered == null || !(filtered instanceof Iterable)) {
                        throw new SynchronizationException("expected targetCorrelation filter to yield a List");
                    }
                    for (Object value : (Iterable)filtered) {
                        results.add(value.toString());
                    }
                } catch (ScriptException se) {
                    throw new SynchronizationException(se);
                }
            }
            return results;
        }
    }

    /**
     * TODO: Description.
     */
    private class TargetSyncOperation extends SyncOperation {

        @Override
        public void sync() throws SynchronizationException {
            assessSituation();
            determineAction();
// TODO: Option here to just report what action would be performed?
            performAction();
        }

        /**
         * TODO: Description.
         *
         * @throws SynchronizationException TODO.
         */
        private void assessSituation() throws SynchronizationException {
            situation = null;
            if (!isTargetValid()) { // target is not valid for this mapping; ignore it
                return;
            }
            String targetId = (targetObject != null ? targetObject.get("_id").asString() : null);
            if (targetId != null) {
                linkObject.getLinkForTarget(targetId);
            }
            if (reconId != null && reconId.equals(linkObject.reconId)) {
                return; // optimization: already handled in previous phase; ignore it
            } else if (linkObject._id == null || linkObject.sourceId == null) {
                situation = Situation.UNQUALIFIED;
            } else {
                sourceObject = readObject(sourceObjectSet, linkObject.sourceId);
                if (sourceObject == null || !isSourceValid()) {
                    situation = Situation.UNQUALIFIED;
                } else { // proper link
                    situation = Situation.CONFIRMED;
                }
            }
        }
    }

    /**
     * TEMPORARY.
     */
    private class ReconEntry {

        /** TODO: Description. */
        public final SyncOperation op;

        /** TODO: Description. */
        public Status status = ObjectMapping.Status.SUCCESS;

        /** TODO: Description. */
        //public String objectId;
        
        /** TODO: Description. */
        public String sourceId;
        
        /** TODO: Description. */
        public String targetId;
        
        /** TODO: Description. */
        public String reconciling;

        /** TODO: Description. */
        public String message;

        public ReconEntry(SyncOperation op) {
            this.op = op;
        }

        /**
         * TODO: Description.
         *
         * @return TODO.
         */
        private JsonNode toJsonNode() {
            JsonNode node = new JsonNode(new HashMap<String, Object>());
            node.put("reconId", op.reconId);
            node.put("reconciling", reconciling);
            node.put("sourceObjectId", sourceId);
            node.put("targetObjectId", targetId);
            node.put("situation", (op.situation == null ? null : op.situation.toString()));
            node.put("action", (op.action == null ? null : op.action.toString()));
            node.put("status", status);
            node.put("message", message);
            return node;
        }
    }
}
