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
 * a highly concurrent manner.
 * 
 * Start event issues EventEntry without limitation on the outstanding start/end
 * events, making it suitable for long(er) time span events/measurements
 * 
 */
public class DisruptorReferringPublisher implements PluggablePublisher {

    static PluggablePublisher INSTANCE = new DisruptorReferringPublisher();

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
    static Disruptor<DisruptorReferringEventEntry> disruptor =
            new Disruptor<DisruptorReferringEventEntry>(DisruptorReferringEventEntry.EVENT_FACTORY,
                    RING_SIZE, executor, ProducerType.MULTI, new SleepingWaitStrategy());
    static RingBuffer<DisruptorReferringEventEntry> ringBuffer;

    static {
        disruptor.handleEventsWith(new StatisticsHandler(disruptor));
        ringBuffer = disruptor.start();
    }

    static long sequence = -1;

    /**
     * Factory method
     */
    public static PluggablePublisher getInstance() {
        return INSTANCE;
    }

    /**
     * @inheritDoc
     */
    public final EventEntry start(Name eventName, Object payload, Object context) {
        // For start event, do not hold a place in the ringbuffer (yet)
        // to avoid limiting long running measurements
        EventEntryImpl eventEntry = new EventEntryImpl();
        eventEntry.eventName = eventName;
        eventEntry.publisher = this;
        eventEntry.payload = payload;
        eventEntry.context = context;
        eventEntry.start();

        // TODO: consider adding option to monitor outstanding requests

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
    public final void end(Name eventName, EventEntry delegate) {
        sequence = ringBuffer.next();
        final DisruptorReferringEventEntry eventEntry = ringBuffer.claimAndGetPreallocated(sequence);
        eventEntry.publisher = this;
        EventEntryImpl delegateImpl = (EventEntryImpl) delegate;
        eventEntry.delegate = delegateImpl;
        // Copy data into ring buffer for optimization
        eventEntry.startTime = delegateImpl.startTime;
        eventEntry.end();
        ringBuffer.publish(sequence);
    }
}
