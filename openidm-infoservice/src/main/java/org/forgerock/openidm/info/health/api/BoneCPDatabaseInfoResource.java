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
 * Api POJO for {@link org.forgerock.openidm.info.health.DatabaseInfoResourceProvider}
 */
public class BoneCPDatabaseInfoResource {
    private long connectionWaitTimeAvg;
    private long statementExecuteTimeAvg;
    private long statementPrepareTimeAvg;
    private int totalLeasedConnections;
    private int totalFreeConnections;
    private int totalCreatedConnections;
    private int cacheHits;
    private int cacheMiss;
    private int statementsCached;
    private int statementsPrepared;
    private int connectionsRequested;
    private long cumulativeConnectionWaitTime;
    private long cumulativeStatementExecutionTime;
    private long cumulativeStatementPrepareTime;
    private double cacheHitRatio;
    private int statementsExecuted;

    /**
     * Returns Connection wait time average.
     *
     * @return Connection wait time average.
     */
    @Description("Returns Connection wait time average in ms")
    @ReadOnly
    public long getConnectionWaitTimeAvg() {
        return connectionWaitTimeAvg;
    }

    /**
     * Returns Statement Execution time average.
     *
     * @return Statement Execution time average.
     */
    @Description("Returns Statement Execution time average in ms")
    @ReadOnly
    public long getStatementExecuteTimeAvg() {
        return statementExecuteTimeAvg;
    }

    /**
     * Returns Statement Prepare time average.
     *
     * @return Statement Prepare time average.
     */
    @Description("Returns Statement Prepare time average in ms")
    @ReadOnly
    public long getStatementPrepareTimeAvg() {
        return statementPrepareTimeAvg;
    }

    /**
     * Returns Total leased connections.
     *
     * @return Total leased connections.
     */
    @Description("Returns Total leased connections")
    @ReadOnly
    public int getTotalLeasedConnections() {
        return totalLeasedConnections;
    }

    /**
     * Returns Total Free Connections.
     *
     * @return Total Free Connections.
     */
    @Description("Returns Total Free Connections")
    @ReadOnly
    public int getTotalFreeConnections() {
        return totalFreeConnections;
    }

    /**
     * Returns Total Created Connections.
     *
     * @return Total Created Connections.
     */
    @Description("Returns Total Created Connections")
    @ReadOnly
    public int getTotalCreatedConnections() {
        return totalCreatedConnections;
    }

    /**
     * Returns Count of hits on the cache.
     *
     * @return Count of hits on the cache.
     */
    @Description("Returns Count of hits on the cache")
    @ReadOnly
    public int getCacheHits() {
        return cacheHits;
    }

    /**
     * Returns Count of cache misses.
     *
     * @return Count of cache misses.
     */
    @Description("Returns Count of cache misses")
    @ReadOnly
    public int getCacheMiss() {
        return cacheMiss;
    }

    /**
     * Returns Count of statements cached.
     *
     * @return Count of statements cached.
     */
    @Description("Returns Count of statements cached")
    @ReadOnly
    public int getStatementsCached() {
        return statementsCached;
    }

    /**
     * Returns Count of statements prepared.
     *
     * @return Count of statements prepared.
     */
    @Description("Returns Count of statements prepared")
    @ReadOnly
    public int getStatementsPrepared() {
        return statementsPrepared;
    }

    /**
     * Returns Count of connections requested.
     *
     * @return Count of connections requested.
     */
    @Description("Returns Count of connections requested")
    @ReadOnly
    public int getConnectionsRequested() {
        return connectionsRequested;
    }

    /**
     * Returns Sum of time waiting for connections.
     *
     * @return Sum of time waiting for connections.
     */
    @Description("Returns Sum of time waiting for connections in ms")
    @ReadOnly
    public long getCumulativeConnectionWaitTime() {
        return cumulativeConnectionWaitTime;
    }

    /**
     * Returns Sum of time executing statements.
     *
     * @return Sum of time executing statements.
     */
    @Description("Returns Sum of time executing statements in ms")
    @ReadOnly
    public long getCumulativeStatementExecutionTime() {
        return cumulativeStatementExecutionTime;
    }

    /**
     * Returns Sum of time spent preparing statements.
     *
     * @return Sum of time spent preparing statements.
     */
    @Description("Returns Sum of time spent preparing statements in ms")
    @ReadOnly
    public long getCumulativeStatementPrepareTime() {
        return cumulativeStatementPrepareTime;
    }

    /**
     * Returns Cache Hit/Miss Ratio
     *
     * @return Cache Hit/Miss Ratio
     */
    @Description("Returns Cache Hit/Miss Ratio")
    @ReadOnly
    public double getCacheHitRatio() {
        return cacheHitRatio;
    }

    /**
     * Returns Count of statements executed.
     *
     * @return Count of statements executed.
     */
    @Description("Returns Count of statements executed")
    @ReadOnly
    public int getStatementsExecuted() {
        return statementsExecuted;
    }
}
