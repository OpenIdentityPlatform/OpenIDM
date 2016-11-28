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

import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.Description;
import org.forgerock.json.resource.ResourceResponse;

/**
 * {@link org.forgerock.openidm.sync.impl.ReconciliationService} cancel-action response.
 */
public class ReconCancelResponse {

    @JsonProperty(FIELD_CONTENT_ID)
    private String id;

    private String action;

    private String status;

    /**
     * Gets recon ID.
     *
     * @return Recon ID
     */
    @NotNull
    @Description("Recon ID")
    public String getId() {
        return id;
    }

    /**
     * Sets recon ID.
     *
     * @param id Recon ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets action name.
     *
     * @return Action name
     */
    @NotNull
    @Description("Action name")
    public String getAction() {
        return action;
    }

    /**
     * Sets action name.
     *
     * @param action Action name
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Gets action status.
     *
     * @return Action status
     */
    @NotNull
    @Description("Action status")
    public String getStatus() {
        return status;
    }

    /**
     * Sets action status.
     *
     * @param status Action status
     */
    public void setStatus(String status) {
        this.status = status;
    }

}
