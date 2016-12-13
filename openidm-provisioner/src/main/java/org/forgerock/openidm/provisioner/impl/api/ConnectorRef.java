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

import org.forgerock.api.annotations.Default;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Summarizes identifiers and versions of an ICF connector.
 */
@Title("Connector Identifiers")
public class ConnectorRef {

    private String bundleName;
    private String bundleVersion;
    private String connectorName;
    private String connectorHostRef;
    private String displayName;

    /**
     * Gets OSGI bundle name.
     *
     * @return OSGI bundle name
     */
    @Description("OSGI bundle name")
    @NotNull
    public String getBundleName() {
        return bundleName;
    }

    /**
     * Sets OSGI bundle name.
     *
     * @param bundleName OSGI bundle name
     */
    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    /**
     * Gets OSGI bundle version.
     *
     * @return OSGI bundle version
     */
    @Description("OSGI bundle version")
    @NotNull
    public String getBundleVersion() {
        return bundleVersion;
    }

    /**
     * Sets OSGI bundle version.
     *
     * @param bundleVersion OSGI bundle version
     */
    public void setBundleVersion(String bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    /**
     * Gets connector name.
     *
     * @return Connector name
     */
    @Description("Connector name")
    @NotNull
    public String getConnectorName() {
        return connectorName;
    }

    /**
     * Sets connector name.
     *
     * @param connectorName Connector name
     */
    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    /**
     * Get connector-host reference.
     *
     * @return Connector-host reference
     */
    @Description("Name of remote connector host (leave blank for local connector)")
    @Default("#LOCAL")
    public String getConnectorHostRef() {
        return connectorHostRef;
    }

    /**
     * Set connector-host reference.
     *
     * @param connectorHostRef Connector-host reference
     */
    public void setConnectorHostRef(String connectorHostRef) {
        this.connectorHostRef = connectorHostRef;
    }

    /**
     * Gets human readable connector-name.
     *
     * @return Human readable connector-name
     */
    @Description("Human readable connector-name")
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets human readable connector-name.
     *
     * @param displayName Human readable connector-name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

}
