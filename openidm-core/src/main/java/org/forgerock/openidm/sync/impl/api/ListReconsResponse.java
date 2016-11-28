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

package org.forgerock.openidm.sync.impl.api;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Lists recons for {@link org.forgerock.openidm.sync.impl.ReconciliationService}.
 */
public class ListReconsResponse {

    private List<ReconciliationServiceResource> reconciliations;

    /**
     * Gets list of recons.
     *
     * @return List of recons or empty-list
     */
    @NotNull
    public List<ReconciliationServiceResource> getReconciliations() {
        return reconciliations;
    }

    /**
     * sets list of recons.
     *
     * @param reconciliations List of recons or empty-list
     */
    public void setReconciliations(List<ReconciliationServiceResource> reconciliations) {
        this.reconciliations = reconciliations;
    }

}
