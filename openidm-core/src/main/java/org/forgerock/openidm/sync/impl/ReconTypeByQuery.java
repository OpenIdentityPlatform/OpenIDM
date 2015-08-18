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
 * Portions copyright 2013-2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;

/**
 * Represents a reconciliation of a set defined by query/queries,
 * typically a sub-set of source and/or target objects
 */
public class ReconTypeByQuery extends ReconTypeBase {

    /**
     *  Defaulting to run target phase
     */
    static final boolean DEFAULT_RUN_TARGET_PHASE = true;
    
    /**
     * A {@link JsonValue} representing a source query.
     */
    JsonValue sourceQuery;

    /**
     * A {@link JsonValue} representing a target query. Only used if target phase is enabled
     */
    JsonValue targetQuery;

    /**
     * A constructor.
     * 
     * @param reconContext a {@link RconciliationContext} object.
     * @throws BadRequestException
     */
    public ReconTypeByQuery(ReconciliationContext reconContext) {
        super(reconContext, DEFAULT_RUN_TARGET_PHASE);

        sourceQuery = calcEffectiveQuery("sourceQuery", reconContext.getObjectMapping().getSourceObjectSet());
        targetQuery = calcEffectiveQuery("targetQuery",  reconContext.getObjectMapping().getTargetObjectSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReconQueryResult querySource(int pageSize, String pagingCookie) throws SynchronizationException {
        return query(sourceQuery.get("resourceName").asString(), 
                sourceQuery, 
                reconContext, 
                Collections.synchronizedSet(new LinkedHashSet<String>()), 
                true, 
                QuerySide.SOURCE,
                pageSize,
                pagingCookie);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultIterable queryTarget() throws SynchronizationException {
        return query(targetQuery.get("resourceName").asString(), targetQuery, reconContext,
                Collections.synchronizedSet(new LinkedHashSet<String>()), 
                reconContext.getObjectMapping().getLinkType().isTargetCaseSensitive(), QuerySide.TARGET,
                0, null
        ).getResultIterable();                
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue getReconParameters() {
        return json(object(
                field("sourceQuery", sourceQuery.getObject()),
                field("targetQuery", targetQuery.getObject())
        ));
    }
}
