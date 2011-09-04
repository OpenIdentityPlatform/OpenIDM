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

package org.forgerock.openidm.sync.impl;

// Java Standard Edition
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import org.forgerock.json.fluent.JsonPatch;
import org.forgerock.json.fluent.JsonTransformer;

// ForgeRock OpenIDM
import org.forgerock.openidm.context.InvokeContext;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.Scripts;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.SynchronizationListener;
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
    private SynchronizationService service;

    /**
     * TODO: Description.
     *
     * @param service
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
        correlationQuery = Scripts.newInstance(config.get("correlationQuery"));
        for (JsonNode node : config.get("properties").expect(List.class)) {
            properties.add(new PropertyMapping(service, node));
        }
        for (JsonNode node : config.get("policies").expect(List.class)) {
            policies.add(new Policy(service, node));
        }
        onCreateScript = Scripts.newInstance(config.get("onCreate"));
        onUpdateScript = Scripts.newInstance(config.get("onUpdate"));
        LOGGER.debug("Instantiated {}", name);
    }

    /**
     * TODO: Description.
     * @return
     */
    SynchronizationService getService() {
        return service;
    }

    /**
     * TODO: Description.
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * TODO: Description.
     *
     * @param id fully-qualified source object identifier.
     * @param value TODO.
     * @throws SynchronizationException TODO.
     */
    private void doSourceSync(String id, JsonNode value) throws SynchronizationException {
        if (id.startsWith(sourceObjectSet + '/') && id.length() > sourceObjectSet.length() + 1) {
            LOGGER.trace("Start synchronizing of {} {}", id, null == value ? "without given value" : "with given value");
            String localId = id.substring(sourceObjectSet.length() + 1); // skip the slash
// TODO: one day bifurcate this for synchronous and asynchronous source operation
            SourceSyncOperation op = new SourceSyncOperation();
            op.sourceId = localId;
            if (value != null) {
                op.sourceObject = value;
                op.sourceObject.put("_id", localId); // unqualified
            }
            op.sync();
        }
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
            return service.getRouter().query(targetObjectSet, query);
        } catch (ObjectSetException ose) {
            throw new SynchronizationException(ose);
        }
    }

    /**
     * TODO: Description.
     *
     * @param objectSet TODO.
     * @return TODO.
     * @throws org.forgerock.openidm.sync.SynchronizationException
     */
// TODO: Codify query-all-ids in ObjectSet or provide per-ObjectSet query for all IDs.
    private Iterable<String> queryAllIds(final String objectSet) throws SynchronizationException {
        return new Iterable<String>() {
            JsonNode list;
            {
                HashMap<String, Object> query = new HashMap<String, Object>();
                query.put(QueryConstants.QUERY_ID, "query-all-ids");
                try {
                    list = new JsonNode(service.getRouter().query(objectSet, query)).
                     get(QueryConstants.QUERY_RESULT).required().expect(List.class);
                } catch (JsonNodeException jne) {
                    throw new SynchronizationException(jne);
                } catch (ObjectSetException ose) {
                    throw new SynchronizationException(ose);
                }
            }
            @Override public Iterator<String> iterator() {
                final Iterator<JsonNode> iterator = list.iterator();
                return new Iterator<String>() {
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
     * @return
     */
    private JsonNode readObject(String objectSet, String id) throws SynchronizationException {
        if (id == null) {
            throw new NullPointerException();
        }
        try {
            return decryptable(new JsonNode(service.getRouter().read(objectSet + '/' + id)));
        } catch (NotFoundException nfe) { // target not found results in null
            return null;
        } catch (ObjectSetException ose) {
            LOGGER.warn("Failed to read target object", ose);
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
    private void createTargetObject(JsonNode target) throws SynchronizationException {
        StringBuilder sb = new StringBuilder();
        sb.append(targetObjectSet);
        if (target.get("_id").isString()) {
            sb.append('/').append(target.get("_id").asString());
        }
        try {
            LOGGER.trace("Create target object: {}", sb.toString());
            service.getRouter().create(sb.toString(), target.asMap());
        } catch (JsonNodeException jne) {
            throw new SynchronizationException(jne);
        } catch (ObjectSetException ose) {
            LOGGER.warn("Failed to create target object", ose);
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
    private void updateTargetObject(JsonNode target) throws SynchronizationException {
        try {
            String id = targetObjectSet + '/' + target.get("_id").required().asString();
            LOGGER.trace("Update target object: {}", id);
            service.getRouter().update(id ,
             target.get("_rev").asString(), target.asMap());
        } catch (JsonNodeException jne) {
            throw new SynchronizationException(jne);
        } catch (ObjectSetException ose) {
            LOGGER.warn("Failed to update target object", ose);
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
                String id = targetObjectSet + '/' + target.get("_id").required().asString();
                LOGGER.trace("Delete target object: {}", id);
                service.getRouter().delete(id,
                        target.get("_rev").asString());
            } catch (JsonNodeException jne) {
                throw new SynchronizationException(jne);
            } catch (NotFoundException nfe) {
                // forgiving delete
            } catch (ObjectSetException ose) {
                LOGGER.warn("Failed to delete target object", ose);
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

    /**
     * Wraps the given node in decryption transformers. This causes on-the-fly decryption
     * of values, should any encrypted values be accessed.
     *
     * @param node the node to be wrapped.
     * @return the node wrapped with decryption transformers.
     */
    private JsonNode decryptable(JsonNode node) {
        ArrayList<JsonTransformer> transformers = new ArrayList<JsonTransformer>(node.getTransformers());
        transformers.addAll(service.getCryptoService().getDecryptionTransformers());
        return new JsonNode(node.getValue(), node.getPointer(), transformers);
    }

    @Override
    public void onCreate(String id, JsonNode value) throws SynchronizationException {
        doSourceSync(id, decryptable(value)); // synchronous for now
    }
    
    @Override
    public void onUpdate(String id, JsonNode oldValue, JsonNode newValue) throws SynchronizationException {
        oldValue = decryptable(oldValue);
        newValue = decryptable(newValue);
// TODO: use old value to project incremental diff without fetch of source
        if (oldValue == null || JsonPatch.diff(oldValue, newValue).size() > 0) {
            doSourceSync(id, newValue); // synchronous for now
        } else {
            LOGGER.trace("There is nothing to update on {}", id);
        }
    }

    @Override
    public void onDelete(String id) throws SynchronizationException {
        doSourceSync(id, null); // synchronous for now
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
     * @param reconId
     * @throws org.forgerock.openidm.sync.SynchronizationException
     */
    private void doRecon(String reconId) throws SynchronizationException {
        for (String sourceId : queryAllIds(sourceObjectSet)) {
            SourceSyncOperation op = new SourceSyncOperation();
            op.sourceId = sourceId;
            op.sourceObject = readObject(sourceObjectSet, sourceId);
            op.reconId = reconId;
            ReconEntry entry = new ReconEntry(op);
            entry.sourceId = sourceObjectSet + '/' + sourceId;
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
            if (entry.status == Status.FAILURE || op.action != null) {
                entry.timestamp = new Date();
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
            if (entry.status == Status.FAILURE || op.action != null) {
                entry.timestamp = new Date();
                entry.reconciling = "target";
                entry.sourceId = qualifiedId(sourceObjectSet, op.sourceObject);
                logReconEntry(entry);
            }
        }
// TODO: cleanup orphan link objects (no matching source or target) here 
    }

    /**
     * Qualified Id if the object is not null, null if the object is null
     * @param objSet
     * @param obj
     * @return
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
     * @throws org.forgerock.openidm.sync.SynchronizationException
     */
    private void logReconEntry(ReconEntry entry) throws SynchronizationException {
        try {
            service.getRouter().create("audit/recon", entry.toJsonNode().asMap());
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
                        action = policy.getAction(sourceObject, targetObject);
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
                    execScript("onCreate", onCreateScript);
                    createTargetObject(targetObject);
                    // falls through to link the newly created target
                case UPDATE:
                case LINK:
                    if (targetObject == null) {
                        throw new SynchronizationException("no target object to link");
                    }
                    if (linkObject._id == null) {
                        linkObject.sourceId = sourceObject.get("_id").required().asString();
                        linkObject.targetId = targetObject.get("_id").required().asString();
                        linkObject.reconId = reconId;
                        linkObject.create();
                    }
// TODO: Detect change of source id, and update link accordingly.
                    if (action == Action.CREATE || action == Action.LINK) {
                        break; // do not update target
                    }
                    if (sourceObject != null && targetObject != null) {
                        JsonNode oldTarget = targetObject.copy();
                        applyMappings(sourceObject, targetObject);
                        execScript("onUpdate", onUpdateScript);
                        if (JsonPatch.diff(oldTarget, targetObject).size() > 0) { // only update if target changes
                            updateTargetObject(targetObject);
                        }
                    }
                    if (linkObject._id != null && reconId != null && !reconId.equals(linkObject.reconId)) {
                        linkObject.reconId = reconId; // note changed reconId in link (optimization)
                        linkObject.update();
                    }
                    break; // terminate UPDATE
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
            } catch (JsonNodeException jne) {
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
                    Map<String, Object> scope = service.newScope();
                    scope.put("source", sourceObject.copy().asMap());
                    try {
                        Object o = validSource.exec(scope);
                        if (o == null || !(o instanceof Boolean)) {
                            throw new SynchronizationException("Expecting boolean value from validSource");
                        }
                        result = (Boolean)o;
                    } catch (ScriptException se) {
                        LOGGER.debug("{} validSource script encountered exception", name, se);
                        throw new SynchronizationException(se);
                    }
                } else { // no script means true
                    result = true;
                }
            }
            LOGGER.trace("isSourceValid of {} evaluated: {}", null != sourceObject ? sourceObject.get("_id").getValue() : "[NULL]", result);
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
                    Map<String, Object> scope = service.newScope();
                    scope.put("target", targetObject.copy().asMap());
                    try {
                        Object o = validTarget.exec(scope);
                        if (o == null || !(o instanceof Boolean)) {
                            throw new SynchronizationException("Expecting boolean value from validTarget");
                        }
                        result = (Boolean)o;
                    } catch (ScriptException se) {
                        LOGGER.debug("{} validTarget script encountered exception", name, se);
                        throw new SynchronizationException(se);
                    }
                } else { // no script means true
                    result = true;
                }
            }
            LOGGER.trace("isTargetValid of {} evaluated: {}", null != targetObject ? targetObject.get("_id").getValue() : "[NULL]", result);
            return result;
        }

        /**
         * TODO: Description.
         *
         * @param type TODO.
         * @param script TODO.
         * @throws SynchronizationException TODO.
         */
        private void execScript(String type, Script script) throws SynchronizationException {
            if (script != null) {
                Map<String, Object> scope = service.newScope();
                scope.put("source", sourceObject.copy().asMap());
                scope.put("target", targetObject.copy().asMap());
                scope.put("situation", situation.toString());
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
     * TODO: Description.
     */
    private class SourceSyncOperation extends SyncOperation {

        /** TODO: Description. */
        public String sourceId;

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
                    JsonNode results = correlateTarget();
                    if (results == null) { // no correlationQuery defined
                        situation = Situation.ABSENT;
                    } else if (results.size() == 1) {
                        targetObject = readObject(targetObjectSet,
                         results.get((Integer)0).required().get("_id").required().asString());
                        situation = Situation.FOUND;
                    } else if (results.size() == 0) {
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
            LOGGER.debug("Mapping '{}' assessed situation of {} to be {}", new Object[]{name, sourceId, situation});
        }

        /**
         * TODO: Description.
         *
         * @return TODO.
         * @throws SynchronizationException TODO.
         */
        private JsonNode correlateTarget() throws SynchronizationException {
            JsonNode result = null;
            if (correlationQuery != null) {
                Map<String, Object> queryScope = service.newScope();
                queryScope.put("source", sourceObject.copy().asMap());
                try {
                    Object query = correlationQuery.exec(queryScope);
                    if (query == null || !(query instanceof Map)) {
                        throw new SynchronizationException("Expected correlationQuery script to yield a Map");
                    }
                    result = new JsonNode(queryTargetObjectSet((Map)query)).get(QueryConstants.QUERY_RESULT).required();
                } catch (ScriptException se) {
                    LOGGER.debug("{} correlationQuery script encountered exception", name, se);
                    throw new SynchronizationException(se);
                }
            }
            return result;
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

        // TODO: replace with proper formatter
        SimpleDateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        
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
            node.put("timestamp", isoFormatter.format(timestamp));
            node.put("situation", (op.situation == null ? null : op.situation.toString()));
            node.put("action", (op.action == null ? null : op.action.toString()));
            node.put("status", (status == null ? null : status.toString()));
            node.put("message", message);
            return node;
        }
    }
}
