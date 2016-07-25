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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openidm.util.DurationStatistics.nanoToMillis;
import static org.forgerock.openidm.util.DurationStatistics.startNanoTime;

import org.testng.annotations.Test;

public class DurationStatisticsTest {

    private static final int ITERATIONS = 1000;

    @Test
    public void testEmpty() {
        final DurationStatistics statistics = new DurationStatistics();

        assertThat(statistics.count()).isEqualTo(0);
        assertThat(statistics.sum()).isEqualTo(0);
        assertThat(statistics.max()).isEqualTo(0);
        assertThat(statistics.min()).isEqualTo(0);
        assertThat(statistics.mean()).isEqualTo(0);
        assertThat(statistics.stdDev()).isEqualTo(0);
    }

    @Test
    public void testSingle() {
        final DurationStatistics statistics = new DurationStatistics();

        final long start = startNanoTime();
        sleepOneMillis();
        statistics.stopNanoTime(start);

        assertThat(statistics.count()).isEqualTo(1);

        assertThat(statistics.sum()).isEqualTo(statistics.min());

        assertThat(statistics.max()).isGreaterThan(0);

        assertThat(statistics.min()).isGreaterThan(0);
        assertThat(statistics.min()).isLessThanOrEqualTo(statistics.max());

        assertThat(statistics.mean()).isGreaterThan(0);
        assertThat(statistics.mean()).isGreaterThanOrEqualTo(statistics.min());
        assertThat(statistics.mean()).isLessThanOrEqualTo(statistics.max());

        assertThat(statistics.stdDev()).isEqualTo(0);
    }

    @Test
    public void testMultiple() {
        final DurationStatistics statistics = new DurationStatistics();

        for (int i = 0; i < ITERATIONS; ++i) {
            final long start = startNanoTime();
            sleepOneMillis();
            statistics.stopNanoTime(start);
        }

        assertThat(statistics.count()).isEqualTo(ITERATIONS);

        assertThat(statistics.sum()).isGreaterThanOrEqualTo(statistics.min() * ITERATIONS);

        assertThat(statistics.max()).isGreaterThanOrEqualTo(statistics.min());

        assertThat(statistics.min()).isGreaterThan(0);
        assertThat(statistics.min()).isLessThanOrEqualTo(statistics.max());

        assertThat(statistics.mean()).isGreaterThan(0);
        assertThat(statistics.mean()).isGreaterThanOrEqualTo(statistics.min());
        assertThat(statistics.mean()).isLessThanOrEqualTo(statistics.max());

        assertThat(statistics.stdDev()).isGreaterThan(0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testStopNanoTimeNegativeArgument() {
        final DurationStatistics statistics = new DurationStatistics();
        statistics.stopNanoTime(-1);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testStopNanoTimeContractViolation() {
        // argument sent to stopNanoTime must come from prior call to startNanoTime method
        final DurationStatistics statistics = new DurationStatistics();
        statistics.stopNanoTime(Long.MAX_VALUE);
    }

    @Test
    public void testNanoToMillis() {
        assertThat(nanoToMillis(1_000_000)).isEqualTo(1);
    }

    private void sleepOneMillis() {
        try {
            Thread.sleep(1);
        } catch (Exception e) {
            // ignore
        }
    }
}
