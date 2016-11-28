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

package org.forgerock.openidm.sync.impl.api;

import org.forgerock.api.annotations.Description;

/**
 * Represents data from {@link org.forgerock.openidm.util.DurationStatistics}.
 */
public class DurationStats {

    private long min;
    private long max;
    private long mean;
    private long count;
    private long sum;
    private long stDev;

    /**
     * Get number of data points recorded.
     *
     * @return Number of data points recorded
     */
    @Description("Number of data points recorded")
    public long getCount() {
        return count;
    }

    /**
     * Set number of data points recorded.
     *
     * @param count Number of data points recorded
     */
    public void setCount(long count) {
        this.count = count;
    }

    /**
     * Get running-maximum time-delta, in nanoseconds.
     *
     * @return Max time-delta, in nanoseconds
     */
    @Description("Max time-delta, in nanoseconds")
    public long getMax() {
        return max;
    }

    /**
     * Set running-maximum time-delta, in nanoseconds.
     *
     * @param max Max time-delta, in nanoseconds
     */
    public void setMax(long max) {
        this.max = max;
    }

    /**
     * Get (approximate) running-average time-delta, in nanoseconds.
     *
     * @return Approximate average time-delta, in nanoseconds
     */
    @Description("Approximate average time-delta, in nanoseconds")
    public long getMean() {
        return mean;
    }

    /**
     * Set (approximate) running-average time-delta, in nanoseconds.
     *
     * @param mean Approximate average time-delta, in nanoseconds
     */
    public void setMean(long mean) {
        this.mean = mean;
    }

    /**
     * Get running-minimum time-delta, in nanoseconds.
     *
     * @return Min time-delta, in nanoseconds
     */
    @Description("Min time-delta, in nanoseconds")
    public long getMin() {
        return min;
    }

    /**
     * Set running-minimum time-delta, in nanoseconds.
     *
     * @param min Min time-delta, in nanoseconds
     */
    public void setMin(long min) {
        this.min = min;
    }

    /**
     * Get (approximate) running-standard-deviation of time-delta, in nanoseconds.
     *
     * @return Approximate standard-deviation of time-delta, in nanoseconds
     */
    @Description("Approximate standard-deviation of time-delta, in nanoseconds")
    public long getStDev() {
        return stDev;
    }

    /**
     * Set (approximate) running-standard-deviation of time-delta, in nanoseconds.
     *
     * @param stDev Approximate standard-deviation of time-delta, in nanoseconds
     */
    public void setStDev(long stDev) {
        this.stDev = stDev;
    }

    /**
     * Get aggregate summation of time-deltas, in nanoseconds.
     *
     * @return Summation of time-deltas, in nanoseconds
     */
    @Description("Summation of time-deltas, in nanoseconds")
    public long getSum() {
        return sum;
    }

    /**
     * Set aggregate summation of time-deltas, in nanoseconds.
     *
     * @param sum Summation of time-deltas, in nanoseconds
     */
    public void setSum(long sum) {
        this.sum = sum;
    }

}
