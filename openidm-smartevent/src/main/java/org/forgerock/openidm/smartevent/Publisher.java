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

package org.forgerock.openidm.smartevent;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.forgerock.openidm.smartevent.core.StatisticsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SingleThreadedClaimStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;

/**
 * Publish smart events
 * 
 * Events can be used both for 
 * * monitoring/low level events
 * * application level events
 * 
 * @author aegloff
 */
public class Publisher {

    /**
     * For events that mark/span the beginning and end of something,
     * call this method to mark the beginning of the event window.
     * Upon reaching the end of the event window, end() SHOULD be called on the returned EventEntry, even in failure condiitons.
     * For example call end() in a try/finally; or at the very least the EventEntry should be unreferenced so it gets garbage collected.
     * Event entries where end() was not called do not qualify for inclusion in statistics and may not be in the event history.
     * @param eventName the object representing the hierarchical event name and its context. See <code>Name.get()</code>
     * @param payload Optional payload to send so target subscribers (and monitoring) can act upon it
     * @param context Optional context information to send so target subscribers (and monitoring) can act upon it
     */
    public final static EventEntry start(Name eventName, Object payload, Object context) {
        return eventName.publisherImpl.start(eventName, payload, context);
    }
    
    // TDOO: support sending of events without start/end relationship
    //public final static void send(Name eventName, Object payload, Object context) {
    //}
}
