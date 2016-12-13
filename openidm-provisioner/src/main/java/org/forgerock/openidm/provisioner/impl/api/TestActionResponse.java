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

import java.util.List;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Response for {@code test}-action.
 */
@Title("Test Response")
public class TestActionResponse extends TestConfigActionResponse {

    private boolean enabled;
    private String displayName;
    private String config;
    private ConnectorRef connectorRef;
    private List<String> objectTypes;

    /**
     * Gets enabled state.
     *
     * @return Enabled state
     */
    @Description("Enabled state")
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets enabled state.
     *
     * @param enabled Enabled state
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets human-readable connector-name.
     *
     * @return Human-readable connector-name
     */
    @Description("Human-readable connector-name")
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets human-readable connector-name.
     *
     * @param displayName Human-readable connector-name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets configuration endpoint.
     *
     * @return Configuration endpoint
     */
    @Description("Configuration endpoint")
    public String getConfig() {
        return config;
    }

    /**
     * Sets configuration endpoint.
     *
     * @param config Configuration endpoint
     */
    public void setConfig(String config) {
        this.config = config;
    }

    /**
     * Gets connector identifiers.
     *
     * @return Connector identifiers
     */
    @Description("Connector identifiers")
    public ConnectorRef getConnectorRef() {
        return connectorRef;
    }

    /**
     * Sets connector identifiers.
     *
     * @param connectorRef Connector identifiers
     */
    public void setConnectorRef(ConnectorRef connectorRef) {
        this.connectorRef = connectorRef;
    }

    /**
     * Gets object classes/types.
     *
     * @return Object classes/types
     */
    @Description("Object classes/types")
    public List<String> getObjectTypes() {
        return objectTypes;
    }

    /**
     * Sets object classes/types.
     *
     * @param objectTypes Object classes/types
     */
    public void setObjectTypes(List<String> objectTypes) {
        this.objectTypes = objectTypes;
    }

}
