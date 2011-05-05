/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.recon.impl;

import org.forgerock.openidm.recon.Situation;

import java.util.Map;

/**
 * After correlation the {@link SituationAnalyzer} takes the {@code sourceObject} and
 * {@code targetObject}, introspects their linking and relationship using the {@link RelationshipIndexImpl},
 * then determines the {@link Situation} that applies to the relationship.
 */
public class SituationAnalyzer {

    RelationshipIndexImpl relationshipIndex = new RelationshipIndexImpl();

    /**
     * @param sourceObject to analyse
     * @param targetObject to analyse
     * @return {@link Situation} of the relationship or linking between objects
     * @throws ReconciliationException
     */
    public Situation determineSituation(Map<String, Object> sourceObject, Map<String, Object> targetObject)
            throws ReconciliationException {
        return Situation.PENDING; // TODO replace me
    }
}
