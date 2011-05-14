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

package org.forgerock.openidm.sync;

// Java Standard Edition
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JSON-Fluent library
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

// ForgeRock OpenIDM
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.repo.QueryConstants;

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

    public Link(ObjectMapping mapping) {
        this.mapping = mapping;
    }

    /**
     * TODO: Description.
     */
    private String linkId(String id) {
        StringBuilder sb = new StringBuilder("link/").append(mapping.getName());
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
    private void getLink(JsonNode query) throws SynchronizationException {
        try {
            JsonNode results = new JsonNode(mapping.getRepository().query(linkId(null),
             query.asMap())).get(QueryConstants.QUERY_RESULT).required().expect(List.class);
            if (results.size() == 1) {
                fromJsonNode(results.get(0));
            }
            else if (results.size() > 1) { // shouldn't happen if index is unique
                throw new SynchronizationException("more than one link found");
            }
        }
        catch (JsonNodeException jne) {
            throw new SynchronizationException("malformed link query response", jne);
        }
        catch (ObjectSetException ose) {
            throw new SynchronizationException("link query failed", ose);
        }
    }

    /**
     * TODO: Description.
     *
     * @param node TODO.
     */
    private void fromJsonNode(JsonNode node) throws JsonNodeException {
        _id = node.get("_id").required().asString();
        _rev = node.get("_rev").asString();
        sourceId = node.get("sourceId").required().asString();
        targetId = node.get("targetId").required().asString();
        reconId = node.get("reconId").asString(); // optional
    }

    /**
     * TODO: Description.
     *
     * @return TODO.
     */
    private JsonNode toJsonNode() {
        JsonNode node = new JsonNode(new HashMap<String, Object>());
        node.put("sourceId", sourceId);
        node.put("targetId", targetId);
        if (reconId != null) {
            node.put("reconId", reconId);
        }
        return node;
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
            JsonNode query = new JsonNode(new HashMap<String, Object>());
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
            JsonNode query = new JsonNode(new HashMap<String, Object>());
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
        try {
            mapping.getRepository().create(linkId(_id), toJsonNode().asMap());
        }
        catch (ObjectSetException ose) {
            LOGGER.debug("failed to create link", ose);
            throw new SynchronizationException(ose);
        }
    }

    /**
     * TODO: Description.
     *
     * @throws SynchronizationException TODO.
     */
    void delete() throws SynchronizationException {
        if (_id != null) { // forgiving delete
            try {
                mapping.getRepository().delete(linkId(_id), _rev);
            }
            catch (ObjectSetException ose) {
                LOGGER.debug("failed to delete link", ose);
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
            throw new SynchronizationException("attempt to update non-existent link");
        }
        try {
            mapping.getRepository().update(linkId(_id), _rev, toJsonNode().asMap());
        }
        catch (ObjectSetException ose) {
            LOGGER.debug("failed to update link", ose);
            throw new SynchronizationException(ose);
        }
    }
}
