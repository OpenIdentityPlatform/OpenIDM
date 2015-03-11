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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.lmax.disruptor.dsl.ProducerType;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;

/**
 * Publisher that uses the disruptor pattern and RingBuffer to process events in
 * a highly concurrent manner
 * 
 * @deprecated Considering removal as it limits the number of open start events
 *             to the ring buffer size Plus it is highly sensitive to requiring
 *             end() to be called on the EventEntry
 */
public class DisruptorShortPublisher implements PluggablePublisher {

    static PluggablePublisher INSTANCE = new DisruptorShortPublisher();

    /**
     * Ring size has considerable implications:
     * 
     * - Pre-allocation of memory/event type objects - Deliberate backpressure
     * when ring is full, i.e. consumers can't keep up with producers - When
     * used for monitoring may keep objects alive for recent history purposes
     * Ring size needs to be a power of 2
     */
    static int RING_SIZE = (1024 * 2);

    // TODO: consider optimizations for number of ring buffers and sizing
    // To process events with different requirements such as statistics vs.
    // application events
    static Executor executor = Executors.newCachedThreadPool();
    static Disruptor<DisruptorShortEventEntry> disruptor = new Disruptor<DisruptorShortEventEntry>(
            DisruptorShortEventEntry.EVENT_FACTORY,RING_SIZE, executor, ProducerType.SINGLE, new SleepingWaitStrategy());
    static RingBuffer<DisruptorShortEventEntry> ringBuffer;

    static {
        // TODO: commented out whilst this event not used
        // disruptor.handleEventsWith(new StatisticsHandler(disruptor));
        ringBuffer = disruptor.start();
    }

    static long sequence = -1;

    public static PluggablePublisher getInstance() {
        return INSTANCE;
    }

    /**
     * @inheritDoc
     */
    public final EventEntry start(Name eventName, Object payload, Object context) {
        sequence = ringBuffer.next();
        final DisruptorShortEventEntry eventEntry = ringBuffer.claimAndGetPreallocated(sequence);
        eventEntry.endTime = 0;
        eventEntry.startTime = 0;
        eventEntry.eventName = eventName;
        eventEntry.publisher = this;
        eventEntry.payload = payload;
        eventEntry.context = context;
        eventEntry.result = null;
        eventEntry.start();
        return eventEntry;
    }

    /**
     * @inheritDoc
     */
    public final void setResult(Object result, EventEntry delegate) {
    }

    /**
     * @inheritDoc
     */
    public final void end(Name eventName, EventEntry callingEntry) {
        // This end() gets called indirectly after the EventEntry end() gets
        // invoked by the user
        ringBuffer.publish(sequence);
    }
}
