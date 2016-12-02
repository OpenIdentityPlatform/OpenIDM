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
 * A class for notify actions sync results.
 */
@Title("Sync Results")
public class SyncResults {
    private List<SyncDetails> syncDetails;
    private String action;
    private boolean success;

    /**
     * Gets the list of sync details or empty list.
     *
     * @return The list of sync details or empty list.
     */
    @Description("The list of all sync details")
    public List<SyncDetails> getSyncDetails() {
        return syncDetails;
    }

    /**
     * Sets the list of sync details or empty list.
     *
     * @param syncDetails The list of sync details or empty list.
     */
    public void setSyncDetails(List<SyncDetails> syncDetails) {
        this.syncDetails = syncDetails;
    }

    /**
     * Gets the action.
     *
     * @return Action
     */
    @Description("The action")
    public String getAction() {
        return this.action;
    }

    /**
     * Sets the action.
     *
     * @param action The action
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Gets whether the action is success or not.
     *
     * @return success
     */
    @Description("Is action success or not")
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * Sets whether the action is success or not.
     *
     * @param success Is success?
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
}
