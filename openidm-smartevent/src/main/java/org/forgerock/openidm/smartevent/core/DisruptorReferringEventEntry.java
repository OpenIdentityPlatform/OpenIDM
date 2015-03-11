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

import com.lmax.disruptor.EventFactory;

/**
 */

/**
 * Ring buffer event entry for the disruptor Used to prepopulate the ring buffer
 * with a (fixed) number of entries for the purpose of processing the associated
 * EventEntry
 * 
 * 
 */
public class DisruptorReferringEventEntry {

    // The event entry to process
    EventEntryImpl delegate;
    // For processing optimization some data may get copied into the ring buffer
    // entry directly
    long startTime;
    long endTime;

    PluggablePublisher publisher;

    public final static EventFactory<DisruptorReferringEventEntry> EVENT_FACTORY =
            new EventFactory<DisruptorReferringEventEntry>() {
                public DisruptorReferringEventEntry newInstance() {
                    return new DisruptorReferringEventEntry();
                }
            };

    /**
     * @inheritDoc
     */
    public final void end() {
        // Disabled when using batched end time
        // The low latency batching framework will assign the time
        endTime = System.nanoTime();
    }

    /**
     * @return duration of time taken between start() and end() or -1 if the
     *         measurement is not complete or available
     */
    public final long getDuration() {
        if (endTime != 0 && startTime != 0) {
            return getRawDuration();
        } else {
            return -1;
        }
    }

    // Internal optimization when it is known that
    // both end and start time exist
    // Result is undefined if this precondition is not true.
    final long getRawDuration() {
        return endTime - startTime;
    }

    /**
     * @return duration of time taken between start() and end() in formatted
     *         form
     */
    String getFormattedDuration() {
        return StatisticsHandler.formatNsAsMs(getDuration());
    }

    public String toString() {
        if (delegate != null && delegate.eventName != null) {
            return "Event name: "
                    + delegate.eventName.asString()
                    + " duration: "
                    + getFormattedDuration()
                    + " payload: "
                    + delegate.payload
                    + " context: "
                    + delegate.context
                    + " publisher set a result: "
                    + delegate.publisherResultSet
                    + (delegate.eventName.getResultHistoryEnabled() ? " result: "
                            + delegate.publisherResult : " result history is disabled.");
        } else {
            return "";
        }
    }
}
