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

package org.forgerock.openidm.util;

import java.util.concurrent.atomic.AtomicLong;

import org.forgerock.guava.common.util.concurrent.AtomicDouble;

/**
 * Thread-safe statistics class for time-durations, that calculates accurate {@link #count() count}, {@link #min() min},
 * and {@link #max() max} values, and approximate {@link #mean() mean} and {@link #stdDev() standard-deviation},
 * with minimal memory consumption and low-latency under thread-contention.
 * <p>
 * Online mean and standard-deviation are calculated using the B. P. Welford (1962)
 * <a href="http://www.johndcook.com/blog/standard_deviation/">technique</a>.
 */
public class DurationStatistics {

    private static final long INITIAL_DELTA_MIN = Long.MAX_VALUE;
    private static final double POSITIVE_EPSILON = 0.00000001;
    private static final double NEGATIVE_EPSILON = -POSITIVE_EPSILON;

    private final AtomicLong deltaCount;
    private final AtomicLong deltaSum;
    private final AtomicLong deltaMax;
    private final AtomicLong deltaMin;
    private final AtomicDouble deltaMean;
    private final AtomicDouble deltaStdDev;

    /**
     * Creates a new instance.
     */
    public DurationStatistics() {
        deltaCount = new AtomicLong();
        deltaSum = new AtomicLong();
        deltaMax = new AtomicLong();
        deltaMin = new AtomicLong(INITIAL_DELTA_MIN);
        deltaMean = new AtomicDouble();
        deltaStdDev = new AtomicDouble();
    }

    /**
     * Gets a start-time, in nanoseconds, for the current thread. Calling thread must be the same thread used to call
     * {@link #stopNanoTime(long)}.
     *
     * @return Start-time, in nanoseconds
     */
    public static long startNanoTime() {
        return System.nanoTime();
    }

    /**
     * Calculates and records the time-duration, since {@link #startNanoTime()} occurred in the current thread.
     * Calling thread must be the same thread used to call {@link #startNanoTime()}.
     *
     * @param startNanoTime Start-time, in nanoseconds
     */
    public void stopNanoTime(final long startNanoTime) {
        if (startNanoTime < 0) {
            throw new IllegalArgumentException("startNanoTime must be non-negative");
        }
        final long delta = System.nanoTime() - startNanoTime;
        if (delta < 0) {
            // this would only happen if method-contract was violated
            throw new IllegalStateException("Unexpected large startNanoTime value");
        }
        updateMean(deltaCount.incrementAndGet(), delta);
        updateMin(delta);
        updateMax(delta);
        deltaSum.addAndGet(delta);
    }

    private void updateMean(long n, final long delta) {
        final double value = (double) delta;
        double oldMean;
        double newMean;
        while (true) {
            oldMean = deltaMean.get();
            newMean = oldMean + (value - oldMean) / (double) n;
            if (!deltaMean.compareAndSet(oldMean, newMean)) {
                // need to try-again, because deltaMean was updated by another thread
                n = deltaCount.get();
                continue;
            }
            break;
        }

        final double adjustment = (value - oldMean) * (value - newMean);
        if (adjustment > POSITIVE_EPSILON || adjustment < NEGATIVE_EPSILON) {
            // only update deltaStdDev when adjustment is non-zero, compared to epsilon
            while (true) {
                final double oldStDev = deltaStdDev.get();
                if (!deltaStdDev.compareAndSet(oldStDev, oldStDev + adjustment)) {
                    // need to try-again, because deltaStdDev was updated by another thread
                    continue;
                }
                break;
            }
        }
    }

    private void updateMin(final long delta) {
        while (true) {
            final long min = deltaMin.get();
            if (min > delta) {
                if (!deltaMin.compareAndSet(min, delta)) {
                    // need to try-again, because deltaMin was updated by another thread
                    continue;
                }
            }
            break;
        }
    }

    private void updateMax(final long delta) {
        while (true) {
            final long max = deltaMax.get();
            if (max < delta) {
                if (!deltaMax.compareAndSet(max, delta)) {
                    // need to try-again, because deltaMax was updated by another thread
                    continue;
                }
            }
            break;
        }
    }

    /**
     * Get running-maximum time-delta, in nanoseconds.
     *
     * @return Max time-delta, in nanoseconds
     */
    public long max() {
        return deltaMax.get();
    }

    /**
     * Get running-minimum time-delta, in nanoseconds.
     *
     * @return Min time-delta, in nanoseconds
     */
    public long min() {
        final long min = deltaMin.get();
        return min == INITIAL_DELTA_MIN ? 0 : min;
    }

    /**
     * Get (approximate) running-average time-delta, in nanoseconds.
     *
     * @return Approximate average time-delta, in nanoseconds
     */
    public long mean() {
        return (long) deltaMean.get();
    }

    /**
     * Get (approximate) running-standard-deviation of time-delta, in nanoseconds.
     *
     * @return Approximate standard-deviation of time-delta, in nanoseconds
     */
    public long stdDev() {
        final long m = deltaCount.get() - 1;
        return m > 0 ? (long) Math.sqrt(deltaStdDev.get() / (double) m) : 0;
    }

    /**
     * Get number of times {@link #stopNanoTime(long)} was called, to record a data point.
     *
     * @return Number of data points recorded
     */
    public long count() {
        return deltaCount.get();
    }

    /**
     * Get aggregate summation of time-deltas, in nanoseconds.
     *
     * @return Summation of time-deltas, in nanoseconds
     */
    public long sum() {
        return deltaSum.get();
    }

    /**
     * Converts nanoseconds to milliseconds, and rounds the result.
     *
     * @param nano Nanoseconds
     * @return Milliseconds
     */
    public static long nanoToMillis(final long nano) {
        return nano / 1_000_000;
    }
}
