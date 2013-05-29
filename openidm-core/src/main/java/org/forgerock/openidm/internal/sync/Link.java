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

// TODO: Extend from something like FieldMap to handle the Java ↔ JSON translations.

package org.forgerock.openidm.internal.sync;

import java.util.HashMap;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Setup logging for the {@link Link}.
     */
    private final static Logger logger = LoggerFactory.getLogger(Link.class);

    // The mapping associated with this link view.
    // This link view is specific to the direction of this mapping context
    private final LinkType mapping;

    // The unique identifier of the link
    public String _id;

    // The MVCC revision of the link
    public String _rev;

    /**
     * Immutable id of the incoming vertex.
     */
    public String _in;

    /**
     * Immutable id of the outgoing vertex.
     */
    public String _out;

    // The id linked in the source object set of the mapping.
    // This link view is specific to the direction of the mapping context
    public String sourceId;

    // The id linked in the target object set of the mapping.
    // This link view is specific to the direction of the mapping context
    public String targetId;

    // Whether this link representation has been initialized.
    // Once initialized is true, _id == null can be interpreted as a link that
    // doesn't exist in our repository yet
    public boolean initialized = false;

    /**
     * TODO: Description.
     *
     * @param mapping
     *            TODO.
     */
    public Link(LinkType mapping) {
        this.mapping = mapping;
    }

    /**
     * For a local link identifier this creates an identifier for the link
     * stored in the repository.
     *
     * @param id
     *            the local (unqualified) link identifier
     * @return the qualified id, qualified to the repository
     */
    private static String linkId(String id) {
        // StringBuilder sb = new
        // StringBuilder("repo/link/").append(mapping.getLinkType().getName());
        StringBuilder sb = new StringBuilder("repo/link");
        if (id != null) {
            sb.append('/').append(id);
        }
        return sb.toString();
    }

    /**
     * Queries a single link and populates the object with its settings
     *
     * @param query
     *            The query parameters
     * @throws ResourceException
     *             if getting and initializing the link details fail
     */
    private void getLink(JsonValue query) throws ResourceException {
//        JsonValue results = linkQuery(mapping.getService().getRouter(), query);
//        if (results.size() == 1) {
//            fromJsonValue(results.get(0));
//        } else if (results.size() > 1) { // shouldn't happen if index is unique
//            throw new SynchronizationException("More than one link found");
//        }
    }

//    /**
//     * Issues a query on link(s)
//     *
//     * @param The
//     *            query parameters
//     * @return The query results
//     * @throws SynchronizationException
//     *             if getting and initializing the link details fail
//     */
//    private static JsonValue linkQuery(ObjectSet router, JsonValue query)
//            throws SynchronizationException {
//        JsonValue results = null;
//        try {
//            results =
//                    new JsonValue(router.query(linkId(null), query.asMap())).get(
//                            QueryConstants.QUERY_RESULT).required().expect(List.class);
//        } catch (JsonValueException jve) {
//            throw new SynchronizationException("Malformed link query response", jve);
//        } catch (ResourceException ose) {
//            throw new SynchronizationException("Link query failed", ose);
//        }
//        return results;
//    }

    /**
     * TODO: Description.
     *
     * @param value
     *            TODO.
     * @throws org.forgerock.json.fluent.JsonValueException
     */
    private void fromJsonValue(Resource value) throws JsonValueException {
        _id = value.getId()      ;
        _rev = value.getRevision();// optional
        if (mapping.useReverse()) {
            sourceId = value.getContent().get("secondId").required().asString();
            targetId = value.getContent().get("firstId").required().asString();
        } else {
            sourceId = value.getContent().get("firstId").required().asString();
            targetId = value.getContent().get("secondId").required().asString();
        }
        sourceId = mapping.normalizeSourceId(sourceId);
        targetId = mapping.normalizeTargetId(targetId);
        initialized = true;
    }

    /**
     * TODO: Description.
     *
     * @return TODO.
     */
    private JsonValue toJsonValue() {
        JsonValue jv = new JsonValue(new HashMap<String, Object>());

        sourceId = mapping.normalizeSourceId(sourceId);
        targetId = mapping.normalizeTargetId(targetId);

        jv.put("linkType", mapping.getName());
        if (mapping.useReverse()) {
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
     * @param context
     * @param sourceId
     *            the object mapping source system identifier
     * @throws ResourceException
     *             if the query could not be performed.
     */
    void getLinkForSource(final ServerContext context, String sourceId) throws ResourceException {
        sourceId = mapping.normalizeSourceId(sourceId);
        if (mapping.useReverse()) {
            getLinkFromSecond(context, sourceId);
        } else {
            getLinkFromFirst(context, sourceId);
        }
    }

    /**
     * Queries the links for a match on the first system (links can be
     * bi-directional)
     * <p>
     * This method expects a {@code "links-for-sourceId"} defined with a
     * parameter of {@code "sourceId"}.
     *
     * @param context
     * @param id
     *            The ID to look up the links
     * @throws ResourceException
     *             if the query could not be performed.
     */
    private void getLinkFromFirst(final ServerContext context, String id) throws ResourceException {
        clear();
        if (id != null) {
            JsonValue query = new JsonValue(new HashMap<String, Object>());
            //query.put(QueryConstants.QUERY_ID, "links-for-firstId");
            query.put("linkType", mapping.getName());
            query.put("firstId", id);
            getLink(query);
        }
    }

    /**
     * Gets the link for a given object mapping source
     *
     * @param context
     *            the object mapping target system identifier
     * @throws ResourceException
     *             if the query could not be performed.
     */
    void getLinkForTarget(final ServerContext context, String aTargetId) throws ResourceException {
        aTargetId = mapping.normalizeTargetId(aTargetId);
        if (mapping.useReverse()) {
            getLinkFromFirst(context, aTargetId);
        } else {
            getLinkFromSecond(context, aTargetId);
        }
    }

    /**
     * Queries the links for a match on the second system (links can be
     * bi-directional)
     * <p>
     * This method expects a {@code "links-for-targetId"} defined with a
     * parameter of {@code "targetId"}.
     *
     * @param context
     *            TODO.
     * @throws ResourceException
     *             TODO.
     */
    void getLinkFromSecond(final ServerContext context, String id) throws ResourceException {
        clear();
//        if (id != null) {
//            JsonValue query = new JsonValue(new HashMap<String, Object>());
//            query.put(QueryConstants.QUERY_ID, "links-for-secondId");
//            query.put("linkType", mapping.getLinkType().getName());
//            query.put("secondId", id);
//            getLink(query);
//        }
    }

    /**
     * Compares the given Id to the current targetId, taking into account the
     * settings for case sensitivity
     *
     * @param compareTargetId
     *            The target id to compare
     * @return true if the given Id is considered equivalent to the current
     *         target id
     */
    public boolean targetEquals(String compareTargetId) {
        String normalizedCompId = mapping.normalizeTargetId(compareTargetId);
        String normalizedTargetId = mapping.normalizeTargetId(targetId);
        if (normalizedTargetId != null) {
            return normalizedTargetId.equals(normalizedCompId);
        } else {
            return normalizedTargetId == normalizedCompId;
        }
    }

    /**
     * TODO: Description.
     *
     * @throws ResourceException
     *             TODO.
     */
    void create(final ServerContext context) throws ResourceException {
        CreateRequest request = Requests.newCreateRequest(linkId(null), _id, toJsonValue());
        try {
            Resource link = context.getConnection().create(context, request);
            this._id = link.getId();
            this._rev = link.getRevision();
            this.initialized = true;
        } catch (ResourceException e) {
            logger.warn("Failed to create link: {}--({})-->{}", sourceId, targetId, e);
            throw e;
        }
    }

    /**
     * TODO: Description.
     *
     * @throws ResourceException
     *             TODO.
     */
    void delete(final ServerContext context) throws ResourceException {
        if (_id != null) { // forgiving delete
            DeleteRequest request = Requests.newDeleteRequest(linkId(_id));
            request.setRevision(_rev);
            try {
                Resource link = context.getConnection().delete(context, request);
            } catch (ResourceException e) {
                logger.warn("Failed to delete lin: {}--({})-->{}", sourceId, targetId, e);
                throw e;
            }
            clear();
        }
    }

    /**
     * TODO: Description.
     *
     * @throws ResourceException
     *             TODO.
     */
    void update(final ServerContext context) throws ResourceException {
        if (_id == null) {
            throw new BadRequestException("Attempt to update non-existent link");
        }
        UpdateRequest update = Requests.newUpdateRequest(linkId(_id), toJsonValue());
        update.setRevision(_rev);
        try {
            Resource link = context.getConnection().update(context, update);
            this._rev = link.getRevision();
        } catch (ResourceException e) {
            logger.warn("Failed to update link: {}--({})-->{}", sourceId, targetId, e);
            throw e;
        }
    }
}
