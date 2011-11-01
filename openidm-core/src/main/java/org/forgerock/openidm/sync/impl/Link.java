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
 * TODO: Description.
 *
 * @author Paul C. Bryan
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
        StringBuilder sb = new StringBuilder("repo/link/").append(mapping.getName());
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
        sourceId = jv.get("sourceId").required().asString();
        targetId = jv.get("targetId").required().asString();
        reconId = jv.get("reconId").asString(); // optional
    }

    /**
     * TODO: Description.
     *
     * @return TODO.
     */
    private JsonValue toJsonValue() {
        JsonValue jv = new JsonValue(new HashMap<String, Object>());
        jv.put("sourceId", sourceId);
        jv.put("targetId", targetId);
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
     * TODO: Description.
     * <p>
     * This method exects a {@code "sourceQuery"} defined with a parameter of
     * {@code "sourceId"}.
     *
     * @param sourceId TODO.
     * @throws SynchronizationException if the query could not be performed.
     */
    void getLinkForSource(String sourceId) throws SynchronizationException {
        clear();
        if (sourceId != null) {
            JsonValue query = new JsonValue(new HashMap<String, Object>());
            query.put(QueryConstants.QUERY_ID, "links-for-sourceId");
            query.put("sourceId", sourceId);
            getLink(query);
        }
    }

    /**
     * TODO: Description.
     * <p>
     * This method exects a {@code "sourceQuery"} defined with a parameter of
     * {@code "sourceId"}.
     *
     * @param targetId TODO.
     * @throws SynchronizationException TODO.
     */
    void getLinkForTarget(String targetId) throws SynchronizationException {
        clear();
        if (targetId != null) {
            JsonValue query = new JsonValue(new HashMap<String, Object>());
            query.put(QueryConstants.QUERY_ID, "links-for-targetId");
            query.put("targetId", targetId);
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
