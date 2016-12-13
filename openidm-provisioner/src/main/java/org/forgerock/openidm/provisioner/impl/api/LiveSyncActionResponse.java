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

package org.forgerock.openidm.provisioner.impl.api;

import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_REVISION;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Response for {@code liveSync}-action.
 */
@Title("Live-Sync Response")
public class LiveSyncActionResponse {

    private String id;
    private String rev;
    private ConnectorData connectorData;

    @Description("Live-Sync identifier")
    @JsonProperty(FIELD_CONTENT_ID)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Description("Live-Sync revision")
    @JsonProperty(FIELD_CONTENT_REVISION)
    public String getRev() {
        return rev;
    }

    public void setRev(String rev) {
        this.rev = rev;
    }

    @Description("Live-Sync metadata")
    public ConnectorData getConnectorData() {
        return connectorData;
    }

    public void setConnectorData(ConnectorData connectorData) {
        this.connectorData = connectorData;
    }

}
