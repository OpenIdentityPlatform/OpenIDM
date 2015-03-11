/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2013-2015 ForgeRock AS. All Rights Reserved
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
*
*/
package org.forgerock.openidm.sync.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

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
                ((Collection<String>) Collections.synchronizedList(new ArrayList<String>())), 
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
                Collections.synchronizedList(new ArrayList<String>()), 
                reconContext.getObjectMapping().getLinkType().isTargetCaseSensitive(), QuerySide.TARGET,
                0, null).getResultIterable();                
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
