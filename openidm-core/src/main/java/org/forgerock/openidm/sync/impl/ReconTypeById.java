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
import java.util.Collections;
import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;

/**
 * Represents a reconciliation by id(s).
 */
public class ReconTypeById extends ReconTypeBase {

    /**
     *  Defaulting to NOT run target phase
     */
    static final boolean DEFAULT_RUN_TARGET_PHASE = false;
    
    /**
     * A {@link List} of source IDs.
     */
    List<String> sourceIds;

    /**
     * A {@link JsonValue} representing a target query. Only used if target phase is enabled.
     */
    private JsonValue targetQuery;

    /**
     * A constructor.
     * 
     * @param reconContext a {@link ReconciliationContext} object.
     * @throws BadRequestException
     */
    public ReconTypeById(ReconciliationContext reconContext) throws BadRequestException {
        super(reconContext, DEFAULT_RUN_TARGET_PHASE);

        targetQuery = calcEffectiveQuery("targetQuery", reconContext.getObjectMapping().getTargetObjectSet());

        JsonValue idsValue = reconContext.getReconParams().get("ids");
        if (idsValue.isNull()) {
            throw new BadRequestException(
                    "Action reconById requires a parameter 'ids' with the identifier(s) to reconcile");
        }

        // TODO: allow multiple ids
        sourceIds = new ArrayList<>();
        String rawIds = idsValue.asString();
        sourceIds.add(rawIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReconQueryResult querySource(int pageSize, String pagingCookie) {
        return new ReconQueryResult(new ResultIterable(sourceIds, null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultIterable queryTarget() throws SynchronizationException {
        return query(targetQuery.get("resourceName").asString(), 
                targetQuery, 
                reconContext,
                Collections.synchronizedList(new ArrayList<String>()), 
                reconContext.getObjectMapping().getLinkType().isTargetCaseSensitive(), 
                QuerySide.TARGET,
                0,
                null).getResultIterable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue getReconParameters() {
        return json(object(
                field("sourceIds", sourceIds),
                field("targetQuery", targetQuery.getObject())
        ));
    }
}
