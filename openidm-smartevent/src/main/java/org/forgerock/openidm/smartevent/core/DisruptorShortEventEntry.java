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

import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;

import com.lmax.disruptor.EventFactory;

/**
 */

/**
 * Event entries that currently doubles as both - To (pre) populate the ring
 * buffer - The implementation of EventEntry exposed to the user This though
 * makes it not suitable for many long time measurements, althogh for short
 * measurement it means less object creation/collection
 * 
 * @deprecated Considering removal as it limits the number of open start events
 *             to the ring buffer size. Plus it is highly sensitive to requiring
 *             end() to be called on the EventEntry
 * 
 */
@Deprecated
public class DisruptorShortEventEntry implements EventEntry {

    Name eventName;
    long startTime;
    long endTime;
    Object payload;
    Object context;
    Object result;

    PluggablePublisher publisher;

    public final static EventFactory<DisruptorShortEventEntry> EVENT_FACTORY =
            new EventFactory<DisruptorShortEventEntry>() {
                public DisruptorShortEventEntry newInstance() {
                    return new DisruptorShortEventEntry();
                }
            };

    /**
     * @inheritDoc
     */
    final void start() {
        startTime = System.nanoTime();
        endTime = 0;
    }

    /**
     * @inheritDoc
     */
    public final void end() {
        // User called this end() method directly, delegate the event publishing
        publisher.end(eventName, this);
    }

    /**
     * @inheritDoc
     */
    public final void setResult(Object result) {
        this.result = result;
    }

    /**
     * @return duration of time taken between start() and end() or -1 if the
     *         measurement is not complete or available
     */
    public final long getDuration() {
        if (endTime != 0 && startTime != 0) {
            return endTime - startTime;
        } else {
            return -1;
        }
    }

    /**
     * @return duration of time taken between start() and end() in formatted
     *         form
     */
    String getFormattedDuration() {
        return StatisticsHandler.formatNsAsMs(getDuration());
    }

    public String toString() {
        if (eventName != null) {
            return "Event name: " + eventName.asString() + " duration: " + getFormattedDuration()
                    + " payload: " + payload + " context: " + context + " result: " + result;
        } else {
            return "";
        }
    }
}
