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
 *
 * Make ObjectSet methods use JsonNode instead of Map--will help clean up a lot of this code.
 */

package org.forgerock.openidm.sync;

// Java Standard Edition
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// JSON-Fluent library
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

// ForgeRock OpenIDM
import org.forgerock.openidm.objset.NotFoundException;
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
    private String name;

    /** TODO: Description. */
    private String sourceObjectSet;

    /** TODO: Description. */
    private String targetObjectSet;

    /** TODO: Description. */
    private Script qualifiesForTarget;

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
        qualifiesForTarget = Scripts.newInstance(config.get("qualifiesForTarget"));
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
    }

    /**
     * TODO: Description.
     *
     * @param id fully-qualified source object identifier.
     * @return operation to perform for the given source object identifier.
     */
    private SynchronousOperation newOperation(String sourceId) {
        String[] split = new String[2];
        if (sourceId.equals(sourceObjectSet)) {
            split[0] = sourceObjectSet;
        }
        else if (sourceId.startsWith(sourceObjectSet + '/')) {
            split[0] = sourceObjectSet;
            if (sourceId.length() > sourceObjectSet.length() + 1) {
                split[1] = sourceId.substring(sourceObjectSet.length() + 1); // skip the slash
            }
        }
        SynchronousOperation op = null;
        if (split[0] != null) { // operation is against source object set
            op = new SynchronousOperation();
            op.sourceId = split[1]; // relative source object identifier
        }
        return op;
    }

    /**
     * TODO: Description.
     */
    private String getLinkObjectSetId() {
        return "link/" + name;
    }

    /**
     * TODO: Description.
     *
     * @param query TODO.
     * @return a link that matches the query, or {@code null} if no match found.
     * @throws JsonNodeException if the query result is malformed.
     * @throws ObjectSetException if the query failed.
     */
    private Link getLinkObject(JsonNode query) throws SynchronizationException {
        Link link = null;
        try {
            JsonNode results = new JsonNode(service.getRepository().query(getLinkObjectSetId(),
             query.asMap())).get(QueryConstants.QUERY_RESULT).required().expect(List.class);
            if (results.size() == 1) {
                link = new Link().fromJsonNode(results.get(0));
            }
            else if (results.size() > 1) { // shouldn't happen if index is unique
                throw new SynchronizationException("more than one link found");
            }
        }
        catch (JsonNodeException jne) {
            throw new SynchronizationException("malformed response", jne);
        }
        catch (NotFoundException nfe) {
            // link not found yields null value;
        }
        catch (ObjectSetException ose) {
            throw new SynchronizationException("link query failed", ose);
        }
        return link;
    }

    /**
     * TODO: Description.
     * <p>
     * This method exects a query defined named {@code "sourceQuery"} with a parameter of
     * {@code "sourceId"}.
     *
     * @param sourceId TODO
     * @return TODO.
     * @throws SynchronizationException if the query could not be performed.
     */
    private Link getLinkObjectForSourceId(String sourceId) throws SynchronizationException {
        JsonNode query = new JsonNode(new HashMap<String, Object>());
        query.put(QueryConstants.QUERY_ID, "sourceQuery");
        query.put("sourceId", sourceId);
        return getLinkObject(query);
    }

    /**
     * TODO: Description.
     *
     * @param link TODO.
     * @throws SynchronizationException TODO.
     */
    private void createLinkObject(Link link) throws SynchronizationException {
        link._id = UUID.randomUUID().toString(); // client-assigned identifier
        try {
            service.getRepository().create(getLinkObjectSetId() + '/' + link._id, link.toJsonNode().asMap());
        }
        catch (ObjectSetException ose) {
            throw new SynchronizationException(ose);
        }
    }

    /**
     * TODO: Description.
     *
     * @param link TODO.
     * @throws SynchronizationException TODO.
     */
    private void deleteLinkObject(Link link) throws SynchronizationException {
        try {
            service.getRepository().delete(getLinkObjectSetId() + '/' + link._id, link._rev);
        }
        catch (ObjectSetException ose) {
            throw new SynchronizationException(ose);
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
        }
        catch (ObjectSetException ose) {
            throw new SynchronizationException(ose);
        }
    }

    /**
     * TODO: Description.
     *
     * @param targetId TODO.
     * @throws NullPointerException if {@code targetId} is {@code null}.
     * @throws SynchronizationException TODO.
     */
    private JsonNode getTargetObject(String targetId) throws SynchronizationException {
        if (targetId == null) {
            throw new NullPointerException();
        }
        try {
            return new JsonNode(service.getRouter().read(targetObjectSet + '/' + targetId));
        }
        catch (NotFoundException nfe) { // target not found results in null
            return null;
        }
        catch (ObjectSetException ose) {
            throw new SynchronizationException(ose);
        }
    }

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
            service.getRouter().create(sb.toString(), target.asMap());
        }
        catch (JsonNodeException jne) {
            throw new SynchronizationException(jne);
        }
        catch (ObjectSetException ose) {
            throw new SynchronizationException(ose);
        }
    }

    /**
     * TODO: Description.
     *
     * @param targetId TODO.
     * @throws SynchronizationException TODO.
     */
    private void updateTargetObject(JsonNode target) throws SynchronizationException {
        try {
            service.getRouter().update(target.get("_id").required().asString(),
             target.get("_rev").asString(), target.asMap());
        }
        catch (JsonNodeException jne) {
            throw new SynchronizationException(jne);
        }
        catch (ObjectSetException ose) {
            throw new SynchronizationException(ose);
        }
    }

    /**
     * TODO: Description.
     *
     * @param target TODO.
     * @throws SynchronizationException TODO.
     */
    private void deleteTargetObject(JsonNode target) throws SynchronizationException {
        if (target.get("_id").isString()) { // forgiving delete
            try {
                service.getRouter().delete(targetObjectSet + '/' + target.get("_id").asString(),
                 target.get("_rev").asString());
            }
            catch (JsonNodeException jne) {
                throw new SynchronizationException(jne);
            }
            catch (NotFoundException nfe) {
                // forgiving delete
            }
            catch (ObjectSetException ose) {
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
        Operation op = newOperation(id); // synchronous for now
        if (op != null) {
            op.sourceObject = new JsonNode(object);
            op.perform();
        }
    }
    
    @Override
    public void onUpdate(String id, Map<String, Object> newValue) throws SynchronizationException {
        onUpdate(id, null, newValue);
    }

    @Override
    public void onUpdate(String id, Map<String, Object> oldValue, Map<String, Object> newValue)
    throws SynchronizationException {
        Operation op = newOperation(id); // synchronous for now
        if (op != null) {
            op.sourceObject = new JsonNode(newValue);
            op.perform();
        }
    }

    @Override
    public void onDelete(String id) throws SynchronizationException {
        Operation op = newOperation(id); // synchronous for now
        if (op != null) {
            op.perform();
        }
    }

    /**
     * TODO: Description.
     */
    private abstract class Operation {

        /** TODO: Description. */
        public String sourceId;

        /** TODO: Description. */
        public JsonNode sourceObject;

        /** TODO: Description. */
        public JsonNode targetObject;

        /** TODO: Description. */
        public Link linkObject;

        /** TODO: Description. */
        public Situation situation;

        /** TODO: Description. */
        public Action action;

        /**
         * TODO: Description.
         *
         * @throws SynchronizationException TODO.
         */
        public abstract void perform() throws SynchronizationException;

        // TODO: move some code up here to support AsynchronousOperation and
        // ReconciliationOperation
    }

    /**
     * TODO: Description.
     */
    private class SynchronousOperation extends Operation {

        @Override
        public void perform() throws SynchronizationException {
            assessSituation();
            determineAction();
            performAction();
        }

        /**
         * TODO: Description.
         *
         * @return TODO.
         * @throws SynchronizationException TODO.
         */
        private boolean qualifies() throws SynchronizationException {
            boolean result = false;
            if (sourceObject != null) { // must have a source object to qualify
                if (qualifiesForTarget != null) {
                    HashMap<String, Object> scope = new HashMap<String, Object>();
                    scope.put("source", sourceObject);
                    try {
                        Object o = qualifiesForTarget.exec(scope);
                        if (o == null || !(o instanceof Boolean)) {
                            throw new SynchronizationException("expecting boolean value from qualifiesFromTarget");
                        }
                        result = (Boolean)o;
                    }
                    catch (ScriptException se) {
                        throw new SynchronizationException(se);
                    }
                }
                else { // no script means true  
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
                }
                catch (ScriptException se) {
                    throw new SynchronizationException(se);
                }
            }
            return results;
        }

        /**
         * TODO: Description.
         *
         * @throws SynchronizationException TODO.
         */
        private void assessSituation() throws SynchronizationException {
            linkObject = getLinkObjectForSourceId(sourceId);
            if (linkObject != null) {
                targetObject = getTargetObject(linkObject.targetId);
            }
            if (qualifies()) {
                if (linkObject != null) {
                    if (targetObject != null) {
                        situation = Situation.CONFIRMED;
                    }
                    else {
                        situation = Situation.MISSING;
                    }
                }
                else {
                    List<String> targets = correlateTarget();
                    if (targets.size() == 1) {
                        targetObject = getTargetObject(targets.get(0));
                        situation = Situation.FOUND;
                    }
                    
                    if (targets.size() == 0) {
                        situation = Situation.ABSENT;
                    }
                    else {
                        situation = Situation.AMBIGUOUS;
                    }
                }
            }
            else {
                if (linkObject != null) {
                    situation = Situation.UNQUALIFIED;
                }
                else {
                    situation = null; // TODO: provide a situation for this?
                }
            }
        }

        /**
         * TODO: Description.
         *
         * @throws SynchronizationException TODO.
         */
        private void determineAction() throws SynchronizationException {
            if (situation == null) {
                return; // no situation means no action to take
            }
            action = situation.getDefaultAction(); // start with reasonable default
            for (Policy policy : policies) {
                if (situation == policy.getSituation()) {
                    Action a = policy.getAction();
                    if (a != null) {
                        action = a;
                    }
                    break;
                }
            }
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
                }
                catch (ScriptException se) {
                    throw new SynchronizationException(se);
                }
            }
        }

        /**
         * TODO: Description.
         *
         * @throws SynchronziationException TODO.
         */
        private void performAction() throws SynchronizationException {
            if (action == null) {
                return; // no action to take
            }
            try {
                switch (action) {
                case CREATE:
                    if (sourceObject == null) {
                        throw new SynchronizationException("no source object to create target from"); 
                    }
                    if (linkObject != null || targetObject != null) {
                        throw new SynchronizationException("target object already exists");
                    }
                    targetObject = new JsonNode(new HashMap<String, Object>());
                    applyMappings(sourceObject, targetObject); // apply property mappings to target
                    execScript(onCreateScript);
                    createTargetObject(targetObject);
                    // fall through to link the newly created target
                case LINK:
                    if (targetObject == null) {
                        throw new SynchronizationException("no target object to link");
                    }
                    linkObject = new Link();
                    linkObject.sourceId = sourceId;
                    linkObject.targetId = targetObject.get("_id").required().asString();
                    createLinkObject(linkObject);
                    if (action == Action.CREATE) {
                        break; // already created; no need to update again
                    }
                    // fall through to update the linked target
                case IGNORE:
                    if (sourceObject != null && targetObject != null) {
                        applyMappings(sourceObject, targetObject);
                        execScript(onUpdateScript);
                        updateTargetObject(targetObject);
                    }
                    break; // terminate LINK and IGNORE
                case DELETE:
                    if (targetObject != null) { // forgiving; does nothing if no target linked
                        deleteTargetObject(targetObject);
                        targetObject = null;
                    }
                    // fall through to unlink the deleted target
                case UNLINK:
                    if (linkObject != null) { // forgiving; does nothing if no link exists
                        deleteLinkObject(linkObject);
                        linkObject = null;
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
    }
}
