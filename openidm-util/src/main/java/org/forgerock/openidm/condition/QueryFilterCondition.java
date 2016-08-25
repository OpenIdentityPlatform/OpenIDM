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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.condition;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openidm.filter.JsonValueFilterVisitor;
import org.forgerock.services.context.Context;
import org.forgerock.util.query.QueryFilter;

/**
 * A Condition evaluated as a QueryFilter.
 */
class QueryFilterCondition implements Condition {
    private static final JsonValueFilterVisitor JSONVALUE_FILTER_VISITOR = new JsonValueFilterVisitor();

    /** the query filter to evaluate */
    private final QueryFilter<JsonPointer> queryFilter;

    /**
     * Construct the condition from a query filter.
     *
     * @param queryFilter the query filter to evaluate
     */
    QueryFilterCondition(QueryFilter<JsonPointer> queryFilter) {
        this.queryFilter = queryFilter;
    }

    @Override
    public boolean evaluate(Object content, Context context) throws JsonValueException {
        return queryFilter == null
                ? false
                : queryFilter.accept(JSONVALUE_FILTER_VISITOR, new JsonValue(content));
    }
}
