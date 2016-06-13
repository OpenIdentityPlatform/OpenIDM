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
 * Copyright © 2012-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.smartevent.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publisher that uses the blocking linked queue to process events.
 */
public class BlockingPublisher implements PluggablePublisher {

    private final static Logger logger = LoggerFactory.getLogger(BlockingPublisher.class);

    private static StatisticsHandler statisticsHandler = new StatisticsHandler(null);
        
    /**
     * Queue max size 
     */
    private final static int QUEUE_CAPACITY = (1024 * 2);

    private static BlockingQueue<EventEntry> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    
    private static Runnable consumer = new Runnable() {

        @Override
        public void run() {
            try {
                while (true) {
                    EventEntry entry = queue.take();
                    statisticsHandler.onEvent(entry, -1, true);
                }
            } catch (InterruptedException ex) {
                logger.debug("Stop processing event queue: {}", ex.getMessage());
                // Allow the processing to stop 
            }
        }
    };
    
    private final static PluggablePublisher INSTANCE = new BlockingPublisher();
    
    private Thread consumerThread;
  
    private BlockingPublisher() {
        consumerThread = new Thread(consumer);
        consumerThread.start();
        logger.debug("Consumer thread started");
    }
    
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
        EventEntryImpl eventEntry = new EventEntryImpl();
        eventEntry.eventName = eventName;
        eventEntry.publisher = this;
        eventEntry.payload = payload;
        eventEntry.context = context;
        eventEntry.start();

        return eventEntry;
    }

    /**
     * @inheritDoc
     */
    public final void setResult(Object result, EventEntry entry) {
    }

    /**
     * @inheritDoc
     */
    public final void end(Name eventName, EventEntry entry) {
        try {
            queue.put(entry);
        } catch (InterruptedException ex) {
            // Ignore not being able to collect event data
        }
    }
}
