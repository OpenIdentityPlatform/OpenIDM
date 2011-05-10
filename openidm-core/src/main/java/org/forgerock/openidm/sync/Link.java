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

// JSON-Fluent library
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
class Link {

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
     * @param node TODO.
     * @return TODO.
     */
    public Link fromJsonNode(JsonNode node) throws JsonNodeException {
        _id = node.get("_id").required().asString();
        _rev = node.get("_rev").asString();
        sourceId = node.get("sourceId").required().asString();
        targetId = node.get("targetId").required().asString();
        reconId = node.get("reconId").asString(); // optional
        return this;
    }

    /**
     * TODO: Description.
     *
     * @return TODO.
     */
    public JsonNode toJsonNode() {
        JsonNode node = new JsonNode(new HashMap<String, Object>());
        node.put("_id", _id);
        node.put("sourceId", sourceId);
        node.put("targetId", targetId);
        node.put("reconId", reconId);
        return node;
    }
}
