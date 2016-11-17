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
 * Api pojo for {@link org.forgerock.openidm.info.health.ReconInfoResourceProvider}
 */
public class ReconInfoResource {
    private int activeThreads;
    private int corePoolSize;
    private int largestPoolSize;
    private int maximumPoolSize;
    private int currentPoolSize;

    /**
     * Returns count of active threads.
     *
     * @return count of active threads.
     */
    @Description("Count of active threads")
    @ReadOnly
    public int getActiveThreads() {
        return activeThreads;
    }

    /**
     * Returns size of core thread pool.
     *
     * @return Size of core thread pool.
     */
    @Description("Size of core thread pool")
    @ReadOnly
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * Returns the largest pool size that has been reached.
     *
     * @return the largest pool size that has been reached.
     */
    @Description("The largest pool size that has been reached")
    @ReadOnly
    public int getLargestPoolSize() {
        return largestPoolSize;
    }

    /**
     * Returns the maximum configured pool size.
     *
     * @return The maximum configured pool size.
     */
    @Description("The maximum configured pool size")
    @ReadOnly
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * Returns the current pool size.
     *
     * @return The current pool size.
     */
    @Description("The current pool size")
    @ReadOnly
    public int getCurrentPoolSize() {
        return currentPoolSize;
    }
}
