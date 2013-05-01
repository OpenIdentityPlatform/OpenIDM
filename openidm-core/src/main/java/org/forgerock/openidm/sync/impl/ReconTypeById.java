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
import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.objset.BadRequestException;

/**
 * Represents a reconciliation by id(s)
 * @author aegloff
 */
public class ReconTypeById implements ReconTypeHandler {
    ReconciliationContext reconContext;
    
    List<String> sourceIds;
    
    public ReconTypeById(ReconciliationContext reconContext) throws BadRequestException {
        this.reconContext = reconContext;
        
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
    
    public boolean isRunTargetPhase() {
        return false;
    }
}
