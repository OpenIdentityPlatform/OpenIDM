/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a reconciliation of a set defined by query/queries,
 * typically a sub-set of source and/or target objects
 * @author aegloff
 */
public class ReconTypeByQuery extends ReconTypeBase {
    private static final Logger logger = LoggerFactory.getLogger(ReconTypeByQuery.class);

    // Defaulting to run target phase
    static final boolean DEFAULT_RUN_TARGET_PHASE = true;

    JsonValue sourceQuery;
    JsonValue targetQuery;

    public ReconTypeByQuery(ReconciliationContext reconContext) {
        super(reconContext, DEFAULT_RUN_TARGET_PHASE);

        sourceQuery = calcEffectiveQuery("sourceQuery",
                reconContext.getObjectMapping().getSourceObjectSet());
        targetQuery = calcEffectiveQuery("targetQuery",
                reconContext.getObjectMapping().getTargetObjectSet());
    }

    public ResultIterable querySource() throws SynchronizationException {
        return query(sourceQuery.get("resourceName").asString(), sourceQuery, reconContext, 
                ((Collection<String>) Collections.synchronizedList(new ArrayList<String>())), true);
    }

    public ResultIterable queryTarget() throws SynchronizationException {
        return query(targetQuery.get("resourceName").asString(), targetQuery, reconContext,
                Collections.synchronizedList(new ArrayList<String>()), reconContext.getObjectMapping().getLinkType().isTargetCaseSensitive());                
    }
}
