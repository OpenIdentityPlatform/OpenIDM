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

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Connector pool configuration.
 */
@Title("Connector Pool Config")
public class PoolConfigOption {

    private long maxObjects;
    private long maxIdle;
    private long maxWait;
    private long minEvictableIdleTimeMillis;
    private long minIdle;

    /**
     * Gets max number of idle and active instances.
     *
     * @return Max number of idle and active instances
     */
    @Description("Max number of idle and active instances")
    public long getMaxObjects() {
        return maxObjects;
    }

    /**
     * Sets max number of idle and active instances.
     *
     * @param maxObjects Max number of idle and active instances
     */
    public void setMaxObjects(long maxObjects) {
        this.maxObjects = maxObjects;
    }

    /**
     * Gets max number of idle instances.
     *
     * @return Max number of idle instances
     */
    @Description("Max number of idle instances")
    public long getMaxIdle() {
        return maxIdle;
    }

    /**
     * Sets max number of idle instances.
     *
     * @param maxIdle Max number of idle instances
     */
    public void setMaxIdle(long maxIdle) {
        this.maxIdle = maxIdle;
    }

    /**
     * Gets max wait time (ms) for return, or 0 for no timeout.
     *
     * @return Max wait time (ms) for return, or 0 for no timeout
     */
    @Description("Max wait time (ms) for return, or 0 for no timeout")
    public long getMaxWait() {
        return maxWait;
    }

    /**
     * Sets max wait time (ms) for return, or 0 for no timeout.
     *
     * @param maxWait Max wait time (ms) for return, or 0 for no timeout
     */
    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    /**
     * Gets max idle time (ms) before removal, or 0 for no timeout.
     *
     * @return Max idle time (ms) before removal, or 0 for no timeout
     */
    @Description("Max idle time (ms) before removal, or 0 for no timeout")
    public long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    /**
     * Sets max idle time (ms) before removal, or 0 for no timeout.
     *
     * @param minEvictableIdleTimeMillis Max idle time (ms) before removal, or 0 for no timeout
     */
    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    /**
     * Gets min number of idle instances.
     *
     * @return Min number of idle instances
     */
    @Description("Min number of idle instances")
    public long getMinIdle() {
        return minIdle;
    }

    /**
     * Gets min number of idle instances.
     *
     * @param minIdle Min number of idle instances
     */
    public void setMinIdle(long minIdle) {
        this.minIdle = minIdle;
    }

}
