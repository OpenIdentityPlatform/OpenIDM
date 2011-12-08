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
import java.util.UUID;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JSON Fluent library
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

// OpenIDM
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

    /** TODO: Description. */
    private final static Logger LOGGER = LoggerFactory.getLogger(Link.class);

    /** TODO: Description. */
    private final ObjectMapping mapping;

    /** TODO: Description. */
    public String _id;

    /** TODO: Description. */
    public String _rev;

    /** TODO: Description. */
    public String sourceId;

    /** TODO: Description. */
    public String targetId;

    /** TODO: Description. */
    public String reconId;

    /**
     * TODO: Description.
     *
     * @param mapping TODO.
     */
    public Link(ObjectMapping mapping) {
        this.mapping = mapping;
    }

    /**
     * TODO: Description.
     * @param id
     * @return
     */
    private String linkId(String id) {
        StringBuilder sb = new StringBuilder("repo/link/").append(mapping.getLinkType().getName());
        if (id != null) {
            sb.append('/').append(id);
        }
        return sb.toString();
    }

    /**
     * TODO: Description.
     *
     * @param query TODO.
     * @throws SynchronizationException TODO.
     */
    private void getLink(JsonValue query) throws SynchronizationException {
        try {
            JsonValue results = new JsonValue(mapping.getService().getRouter().query(linkId(null),
             query.asMap())).get(QueryConstants.QUERY_RESULT).required().expect(List.class);
            if (results.size() == 1) {
                fromJsonValue(results.get(0));
            } else if (results.size() > 1) { // shouldn't happen if index is unique
                throw new SynchronizationException("More than one link found");
            }
        } catch (JsonValueException jve) {
            throw new SynchronizationException("Malformed link query response", jve);
        } catch (ObjectSetException ose) {
            throw new SynchronizationException("Link query failed", ose);
        }
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
            sourceId = jv.get("targetId").required().asString();
            targetId = jv.get("sourceId").required().asString();
        } else {
            sourceId = jv.get("sourceId").required().asString();
            targetId = jv.get("targetId").required().asString();
        }
        reconId = jv.get("reconId").asString(); // optional
    }

    /**
     * TODO: Description.
     *
     * @return TODO.
     */
    private JsonValue toJsonValue() {
        JsonValue jv = new JsonValue(new HashMap<String, Object>());
        if (mapping.getLinkType().useReverse()) {
            jv.put("sourceId", targetId);
            jv.put("targetId", sourceId);
        } else {
            jv.put("sourceId", sourceId);
            jv.put("targetId", targetId);
        }
        jv.put("reconId", reconId);
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
        this.reconId = null;
    }

    /**
     * Gets the link for a given object mapping source
     *
     * @param sourceId the object mapping source system identifier
     * @throws SynchronizationException if the query could not be performed.
     */
    void getLinkForSource(String sourceId) throws SynchronizationException {
        if (mapping.getLinkType().useReverse()) {
            getLinkFromSecond(sourceId);
        } else {
            getLinkFromFirst(sourceId);
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
// TODO: refactor link properties naming
            query.put(QueryConstants.QUERY_ID, "links-for-sourceId");
            query.put("sourceId", id);
            getLink(query);
        }
    }

    /**
     * Gets the link for a given object mapping source
     *
     * @param sourceId the object mapping source system identifier
     * @throws SynchronizationException if the query could not be performed.
     */
    void getLinkForTarget(String targetId) throws SynchronizationException {
        if (mapping.getLinkType().useReverse()) {
            getLinkFromFirst(targetId);
        } else {
            getLinkFromSecond(targetId);
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
            query.put(QueryConstants.QUERY_ID, "links-for-targetId");
            query.put("targetId", id);
            getLink(query);
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
            LOGGER.warn("Failed to create link", ose);
            throw new SynchronizationException(ose);
        }
        this._id = jv.get("_id").required().asString();
        this._rev = jv.get("_rev").asString(); // optional
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
