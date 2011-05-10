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

// JSON-Fluent library
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

// ForgeRock OpenIDM
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.Scripts;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
class TargetCorrelation {

    /** TODO: Description. */
    private final Script query;

    /** TODO: Description. */
    private final Script filter;

    /**
     * TODO: Description.
     *
     * @param config TODO.
     * @throws JsonNodeException TODO.
     */
    public TargetCorrelation(JsonNode config) throws JsonNodeException {
        query = Scripts.newInstance(config.get("query").required());
        filter = Scripts.newInstance(config.get("filter").required());
    }

    /**
     * TODO: Description.
     */
    public Script getQuery() {
        return query;
    }

    /**
     * TODO: Description.
     */
    public Script getFilter() {
        return filter;
    }
}
