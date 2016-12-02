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

import java.util.List;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * {@link org.forgerock.openidm.sync.impl.SynchronizationService} notifyCreate-action, notifyUpdate-action, notifyDelete-action response.
 */
@Title("Notify Action Response")
public class NotifyResponse {
    private List<SyncResults> syncResults;

    /**
     * Gets the list of sync results.
     *
     * @return The list of sync results
     */
    @Description("The list of sync results")
    public List<SyncResults> getSyncResults() {
        return syncResults;
    }

    /**
     * Sets the list of sync results.
     *
     * @param syncResults The list of sync results
     */
    public void setSyncResults(List<SyncResults> syncResults) {
        this.syncResults = syncResults;
    }
}
