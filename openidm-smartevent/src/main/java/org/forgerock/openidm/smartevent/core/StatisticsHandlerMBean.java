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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.smartevent.core;

import java.util.Map;

//import com.lmax.disruptor.SingleThreadedClaimStrategy;
//import com.lmax.disruptor.SleepingWaitStrategy;
//import com.lmax.disruptor.dsl.Disruptor;

/**
 * This MBean interface is expected to change!!!
 * 
 * Provide JMX / MBean access for monitoring and management of SmartEvents and
 * in turn the instrumented application
 * 
 */
public interface StatisticsHandlerMBean {

    /**
     * @return The statistics totals
     */
    Map getTotals();

    /**
     * @return the recent history of events, mapping from start time to the
     *         event detail
     */
    Map getRecent();

    /**
     * Reset all statistics data for events
     */
    void resetAllStatistics();

    /**
     * Reset statistics data for given event name
     * 
     * @param eventName
     *            the event name in Stringified notation
     */
    void resetStatistics(String eventName);

    /**
     * Set the enabled status for a given event name Disabling an event means no
     * consumer will get notified
     * 
     * @param eventName
     *            the event name in Stringified notation
     * @param enabled
     *            true to enable event processing, false to disable
     */
    void setEventsEnabled(String eventName, boolean enabled);

    /**
     * @return A map from Stringified event name to its enabled status
     */
    Map<String, Boolean> getEventsEnabledMap();

    /**
     * Set the result history enabled status for a given event name
     * 
     * @param eventName
     *            the event name in Stringified notation
     * @param enabled
     *            true to keep result details in the recent event history How
     *            long it is kept depends on the history size and the frequency
     *            of new events Caution should be exercised on events with large
     *            result objects as it will affect system memory consumption
     */
    void setResultHistoryEnabled(String eventName, boolean enabled);

    /**
     * @return A map from Stringified event name to its result history enabled
     *         status
     */
    Map<String, Boolean> getResultHistoryEnabledMap();

}
