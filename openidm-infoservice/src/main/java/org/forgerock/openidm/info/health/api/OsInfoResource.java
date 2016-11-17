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
package org.forgerock.openidm.info.health.api;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.ReadOnly;

/**
 * API Descriptor POJO that describes the exposed operating system information.
 *
 * @see org.forgerock.openidm.info.health.OsInfoResourceProvider
 */
public class OsInfoResource {

    private double availableProcessors;
    private double systemLoadAverage;
    private String operatingSystemArchitecture;
    private String operatingSystemName;
    private String operatingSystemVersion;

    /**
     * Returns count of available processors for this single node.
     *
     * @return Count of available processors for this single node.
     */
    @Description("Count of available processors for this single node")
    @ReadOnly
    public double getAvailableProcessors() {
        return availableProcessors;
    }

    /**
     * Returns system load average of this single node.
     *
     * @return system load average of this single node.
     */
    @Description("System load average of this single node")
    @ReadOnly
    public double getSystemLoadAverage() {
        return systemLoadAverage;
    }

    /**
     * Returns operating system architecture description.
     *
     * @return Operating system architecture description.
     */
    @Description("Operating system architecture description")
    @ReadOnly
    public String getOperatingSystemArchitecture() {
        return operatingSystemArchitecture;
    }

    /**
     * Returns the operating system name.
     *
     * @return The operating system name.
     */
    @Description("The operating system name")
    @ReadOnly
    public String getOperatingSystemName() {
        return operatingSystemName;
    }

    /**
     * Returns the operating system version.
     *
     * @return The operating system version.
     */
    @Description("The operating system version")
    @ReadOnly
    public String getOperatingSystemVersion() {
        return operatingSystemVersion;
    }
}
