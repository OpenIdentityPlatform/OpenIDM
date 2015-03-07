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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl;

import org.forgerock.json.resource.ResourceException;

import java.util.concurrent.ExecutorService;

/**
 * Provide JMX / MBean access for monitoring and management of reconciliation.
 */
public interface ReconciliationServiceMBean {

    /**
     * Gets the recon thread pool.
     * @return {@link java.util.concurrent.ExecutorService ExecutorService} object.
     */
    public ExecutorService getThreadPool();

    /**
     * Gets the number of active threads in the recon thread pool.
     * @return the number of active threads in the thread pool.
     * @throws ResourceException if there is an error getting the active threads.
     */
    public int getActiveThreads() throws ResourceException;

    /**
     * Gets the core number of threads in the recon thread pool.
     * @return the core number of threads in the thread pool.
     * @throws ResourceException if there is an error getting the core number of threads.
     */
    public int getCorePoolSize() throws ResourceException;

    /**
     * Gets the current number of threads in the recon thread pool.
     * @return the current number of threads in the recon thread pool.
     * @throws ResourceException if there is an error getting the current number of threads in the thread pool.
     */
    public int getPoolSize() throws ResourceException;

    /**
     * Gets the largest number of threads that have ever simultaneously been in the recon thread pool.
     * @return the largest number of threads that have ever simultaneously been in the thread pool.
     * @throws ResourceException if there is an error getting the largest number of threads that have ever
     *      simultaneously been in the pool.
     */
    public int getLargestPoolSize() throws ResourceException;

    /**
     * Gets the maximum allowed number of threads in the recon thread pool.
     * @return the maximum allowed number of threads in the thread pool.
     * @throws ResourceException if there is an error getting maximum allowed number of threads.
     */
    public int getMaximumPoolSize() throws ResourceException;
}
