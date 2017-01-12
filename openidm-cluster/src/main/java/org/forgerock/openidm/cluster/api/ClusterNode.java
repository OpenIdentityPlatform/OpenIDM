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

package org.forgerock.openidm.cluster.api;

import java.util.Map;

import javax.validation.constraints.NotNull;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Format;

/**
 * A single cluster node instance.
 */
public class ClusterNode {
    @NotNull
    @Description("Database-generated cluster node row identifier")
    private String id;

    @Description("Cluster node identifier")
    private String instanceId;

    @Description("Cluster node startup timestamp")
    @Format("date-time")
    private String startup;

    @Description("Cluster node running state (running, down, processing-down)")
    private String state;

    @Description("Cluster node shutdown timestamp or empty-string")
    @Format("date-time")
    private String shutdown;

    @Description("Cluster node recovery info")
    private Map<String, Object> recovery;

    /**
     * Gets cluster node database row identifier.
     *
     * @return Cluster node database row identifier
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * Gets cluster node identifier.
     *
     * @return Cluster node identifier
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Gets cluster node startup timestamp.
     *
     * @return Cluster node startup timestamp
     */
    public String getStartup() {
        return startup;
    }

    /**
     * Gets cluster node running state.
     *
     * @return Cluster node running state
     */
    public String getState() {
        return state;
    }


    /**
     * Gets cluster node shutdown timestamp.
     *
     * @return Cluster node shutdown timestamp or empty-string
     */
    public String getShutdown() {
        return shutdown;
    }

    /**
     * Gets cluster node recovery info.
     *
     * @return Cluster node recovery info
     */
    public Map<String, Object> getRecovery() {
        return recovery;
    }
}