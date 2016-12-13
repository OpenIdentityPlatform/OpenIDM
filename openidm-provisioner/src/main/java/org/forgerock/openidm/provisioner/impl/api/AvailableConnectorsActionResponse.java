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

import javax.validation.constraints.NotNull;
import java.util.List;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Response from {@code availableConnectors}-action.
 */
@Title("Available Connectors")
public class AvailableConnectorsActionResponse {

    private List<ConnectorRefExtended> connectorRef;

    /**
     * Gets all available connectors.
     *
     * @return All available connectors
     */
    @Description("All available connectors")
    @NotNull
    public List<ConnectorRefExtended> getConnectorRef() {
        return connectorRef;
    }

    /**
     * Sets all available connectors.
     *
     * @param connectorRef All available connectors
     */
    public void setConnectorRef(List<ConnectorRefExtended> connectorRef) {
        this.connectorRef = connectorRef;
    }

}
