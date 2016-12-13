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

package org.forgerock.openidm.provisioner.openicf.impl.api;

import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_REVISION;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;
import org.forgerock.openidm.provisioner.impl.api.ConnectorData;

/**
 * Response for {@code authenticate}-action.
 */
@Title("Authenticate Response")
public class AuthenticateActionResponse {

    @JsonProperty(FIELD_CONTENT_ID)
    private String id;

    @JsonProperty(FIELD_CONTENT_REVISION)
    private String rev;

    /**
     * Gets object identifier.
     *
     * @return Object identifier
     */
    @Description("Object identifier")
    public String getId() {
        return id;
    }

    /**
     * Sets object identifier.
     *
     * @param id Object identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets object revision.
     *
     * @return Object revision
     */
    @Description("Object revision")
    public String getRev() {
        return rev;
    }

    /**
     * Sets object revision.
     *
     * @param rev Object revision
     */
    public void setRev(String rev) {
        this.rev = rev;
    }

}
