/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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
import java.util.Collections;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;

import org.forgerock.json.resource.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a reconciliation by id(s)
 * @author aegloff
 */
public class ReconTypeById extends ReconTypeBase {
    private static final Logger logger = LoggerFactory.getLogger(ReconTypeByQuery.class);

    // Defaulting to NOT run target phase
    static final boolean DEFAULT_RUN_TARGET_PHASE = false;

    List<String> sourceIds;

    // Only used if target phase is enabled
    JsonValue targetQuery;

    public ReconTypeById(ReconciliationContext reconContext) throws BadRequestException {
        super(reconContext, DEFAULT_RUN_TARGET_PHASE);

        targetQuery = calcEffectiveQuery("targetQuery",
                reconContext.getObjectMapping().getTargetObjectSet());

        JsonValue idsValue = reconContext.getReconParams().get("ids");
        if (idsValue.isNull()) {
            throw new BadRequestException(
                    "Action reconById requires a parameter 'ids' with the identifier(s) to reconcile");
        }

        // TODO: allow multiple ids
        sourceIds = new ArrayList();
        String rawIds = idsValue.asString();
        sourceIds.add(rawIds);
    }

    public List<String> querySourceIds() {
        return sourceIds;
    }

    public List<String> queryTargetIds() throws SynchronizationException {
        List<String> targetIds = (List<String>) query(targetQuery.get("resourceName").asString(), targetQuery, reconContext,
                Collections.synchronizedList(new ArrayList<String>()), reconContext.getObjectMapping().getLinkType().isTargetCaseSensitive());
        return targetIds;
    }
}
