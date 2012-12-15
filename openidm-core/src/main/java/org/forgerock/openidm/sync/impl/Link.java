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

// TODO: Extend from something like FieldMap to handle the Java ↔ JSON translations.

package org.forgerock.openidm.sync.impl;

// Java Standard Edition
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JSON Fluent library
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

// OpenIDM
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.sync.SynchronizationException;

/**
 * Uni-directional view of a link.
 * 
 * Link Types and Links in the repository are bi-directional.
 * 
 * This view represents one direction of that Link to match the direction of the 
 * current mapping context (source/target object set).
 *
 * @author Paul C. Bryan
 * @author aegloff
 */
class Link {

    private final static Logger LOGGER = LoggerFactory.getLogger(Link.class);

    // The mapping associated with this link view. 
    // This link view is specific to the direction of this mapping context 
    private final ObjectMapping mapping;

    // The unique identifier of the link
    public String _id;

    // The MVCC revision of the link
    public String _rev;
   
    // The id linked in the source object set of the mapping. 
    // This link view is specific to the direction of the mapping context 
    public String sourceId;

    // The id linked in the target object set of the mapping. 
    // This link view is specific to the direction of the mapping context 
    public String targetId;
    
    // Whether this link representation has been initialized.
    // Once initialized is true, _id == null can be interpreted as a link that doesn't exist in our repository yet
    public boolean initialized = false;

    /**
     * TODO: Description.
     *
     * @param mapping TODO.
     */
    public Link(ObjectMapping mapping) {
        this.mapping = mapping;
    }

    /**
     * For a local link identifier this creates an identifier for the link stored in the repository.
     * @param id the local (unqualified) link identifier
     * @return the qualified id, qualified to the repository
     */
    private static String linkId(String id) {
        //StringBuilder sb = new StringBuilder("repo/link/").append(mapping.getLinkType().getName());
        StringBuilder sb = new StringBuilder("repo/link");
        if (id != null) {
            sb.append('/').append(id);
        }
        return sb.toString();
    }

    /**
     * Queries a single link and populates the object with its settings
     *
     * @param The query parameters
     * @throws SynchronizationException if getting and initializing the link details fail
     */
    private void getLink(JsonValue query) throws SynchronizationException {
        JsonValue results = linkQuery(mapping.getService().getRouter(), query);
        if (results.size() == 1) {
            fromJsonValue(results.get(0));
        } else if (results.size() > 1) { // shouldn't happen if index is unique
            throw new SynchronizationException("More than one link found");
        }
    }
    
    /**
     * Issues a query on link(s)
     *
     * @param The query parameters
     * @return The query results
     * @throws SynchronizationException if getting and initializing the link details fail
     */
    private static JsonValue linkQuery(ObjectSet router, JsonValue query) throws SynchronizationException {
        JsonValue results = null;
        try {
            results = new JsonValue(router.query(linkId(null), 
                    query.asMap())).get(QueryConstants.QUERY_RESULT).required().expect(List.class);
        } catch (JsonValueException jve) {
            throw new SynchronizationException("Malformed link query response", jve);
        } catch (ObjectSetException ose) {
            throw new SynchronizationException("Link query failed", ose);
        }
        return results;
    }

    /**
     * TODO: Description.
     *
     * @param value TODO.
     * @throws org.forgerock.json.fluent.JsonValueException
     */
    private void fromJsonValue(JsonValue jv) throws JsonValueException {
        _id = jv.get("_id").required().asString();
        _rev = jv.get("_rev").asString(); // optional
        if (mapping.getLinkType().useReverse()) {
            sourceId = jv.get("secondId").required().asString();
            targetId = jv.get("firstId").required().asString();
        } else {
            sourceId = jv.get("firstId").required().asString();
            targetId = jv.get("secondId").required().asString();
        }
        sourceId = mapping.getLinkType().normalizeSourceId(sourceId);
        targetId = mapping.getLinkType().normalizeTargetId(targetId);
        initialized = true;
    }

    /**
     * TODO: Description.
     *
     * @return TODO.
     */
    private JsonValue toJsonValue() {
        JsonValue jv = new JsonValue(new HashMap<String, Object>());
        
        sourceId = mapping.getLinkType().normalizeSourceId(sourceId);
        targetId = mapping.getLinkType().normalizeTargetId(targetId);

        jv.put("linkType", mapping.getLinkType().getName());
        if (mapping.getLinkType().useReverse()) {
            jv.put("secondId", sourceId);
            jv.put("firstId", targetId);
        } else {
            jv.put("firstId", sourceId);
            jv.put("secondId", targetId);
        }
        return jv;
    }

    /**
     * TODO: Description.
     */
    void clear() {
        this._id = null;
        this._rev = null;
        this.sourceId = null;
        this.targetId = null;
    }

    /**
     * Gets the link for a given object mapping source
     *
     * @param aSourceId the object mapping source system identifier
     * @throws SynchronizationException if the query could not be performed.
     */
    void getLinkForSource(String aSourceId) throws SynchronizationException {
        aSourceId = mapping.getLinkType().normalizeSourceId(aSourceId);
        if (mapping.getLinkType().useReverse()) {
            getLinkFromSecond(aSourceId);
        } else {
            getLinkFromFirst(aSourceId);
        }
    }

    /**
     * Queries the links for a match on the first system (links can be bi-directional)
     * <p>
     * This method expects a {@code "links-for-sourceId"} defined with a parameter of
     * {@code "sourceId"}.
     *
     * @param id The ID to look up the links
     * @throws SynchronizationException if the query could not be performed.
     */
    private void getLinkFromFirst(String id) throws SynchronizationException {
        clear();
        if (id != null) {
            JsonValue query = new JsonValue(new HashMap<String, Object>());            
            query.put(QueryConstants.QUERY_ID, "links-for-firstId");
            query.put("linkType", mapping.getLinkType().getName());
            query.put("firstId", id);
            getLink(query);
        }
    }

    /**
     * Gets the link for a given object mapping source
     *
     * @param targetId the object mapping target system identifier
     * @throws SynchronizationException if the query could not be performed.
     */
    void getLinkForTarget(String aTargetId) throws SynchronizationException {
        aTargetId = mapping.getLinkType().normalizeTargetId(aTargetId);
        if (mapping.getLinkType().useReverse()) {
            getLinkFromFirst(aTargetId);
        } else {
            getLinkFromSecond(aTargetId);
        }
    }
    
    /**
     * Queries the links for a match on the second system (links can be bi-directional)
     * <p>
     * This method expects a {@code "links-for-targetId"} defined with a parameter of
     * {@code "targetId"}.
     *
     * @param targetId TODO.
     * @throws SynchronizationException TODO.
     */
    void getLinkFromSecond(String id) throws SynchronizationException {
        clear();
        if (id != null) {
            JsonValue query = new JsonValue(new HashMap<String, Object>());
            query.put(QueryConstants.QUERY_ID, "links-for-secondId");
            query.put("linkType", mapping.getLinkType().getName());
            query.put("secondId", id);
            getLink(query);
        }
    }
    
    /**
     * Queries all the links for a given mapping, indexed by the source identifier
     * <p>
     * This method expects a {@code "links-for-linkType"} defined with a parameter of
     * {@code "linkType"}.
     *
     * @param mapping the mapping to look up the links for 
     * @throws SynchronizationException if the query could not be performed.
     * @return the mapping from source identifier to the link object for it
     */
    public static Map<String, Link> getLinksForMapping(ObjectMapping mapping) throws SynchronizationException {
        Map<String, Link> sourceIdToLink = new ConcurrentHashMap<String, Link>();
        if (mapping != null) {
            JsonValue query = new JsonValue(new HashMap<String, Object>());
            query.put(QueryConstants.QUERY_ID, "links-for-linkType");
            query.put("linkType", mapping.getLinkType().getName());
            JsonValue queryResults = linkQuery(mapping.getService().getRouter(), query);
            for (JsonValue entry : queryResults) {
                Link link = new Link(mapping);
                link.fromJsonValue(entry);
                sourceIdToLink.put(link.sourceId, link);
            }
        }
        return sourceIdToLink;
    }
    
    /** Compares the given Id to the current targetId,
     * taking into account the settings for case sensitivity
     * @param compareTargetId The target id to compare
     * @return true if the given Id is considered equivalent to the current target id
     */
    public boolean targetEquals(String compareTargetId) { 
        String normalizedCompId = mapping.getLinkType().normalizeTargetId(compareTargetId);
        String normalizedTargetId = mapping.getLinkType().normalizeTargetId(targetId);
        if (normalizedTargetId != null) {
            return normalizedTargetId.equals(normalizedCompId);
        } else {
            return normalizedTargetId == normalizedCompId;
        }
    }

    /**
     * TODO: Description.
     *
     * @throws SynchronizationException TODO.
     */
    void create() throws SynchronizationException {
        _id = UUID.randomUUID().toString(); // client-assigned identifier
        JsonValue jv = toJsonValue();
        try {
            mapping.getService().getRouter().create(linkId(_id), jv.asMap());
        } catch (ObjectSetException ose) {
            LOGGER.debug("Failed to create link", ose);
            throw new SynchronizationException(ose);
        }
        this._id = jv.get("_id").required().asString();
        this._rev = jv.get("_rev").asString(); // optional
        this.initialized = true;
    }

    /**
     * TODO: Description.
     *
     * @throws SynchronizationException TODO.
     */
    void delete() throws SynchronizationException {
        if (_id != null) { // forgiving delete
            try {
                mapping.getService().getRouter().delete(linkId(_id), _rev);
            } catch (ObjectSetException ose) {
                LOGGER.warn("Failed to delete link", ose);
                throw new SynchronizationException(ose);
            }
            clear();
        }
    }

    /**
     * TODO: Description.
     *
     * @throws SynchronizationException TODO.
     */
    void update() throws SynchronizationException {
        if (_id == null) {
            throw new SynchronizationException("Attempt to update non-existent link");
        }
        JsonValue jv = toJsonValue();
        try {
            mapping.getService().getRouter().update(linkId(_id), _rev, jv.asMap());
        } catch (ObjectSetException ose) {
            LOGGER.warn("Failed to update link", ose);
            throw new SynchronizationException(ose);
        }
        this._rev = jv.get("_rev").asString(); // optional
    }
}
