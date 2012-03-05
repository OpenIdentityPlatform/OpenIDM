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

import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SingleThreadedClaimStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;

/**
 * Allows to plug in event publishers, and replace them dynamically
 * @author aegloff
 */
public interface PluggablePublisher {
    
    /**
     * Pluggable implementation of the start event handling,
     * used internally
     * @see org.forgerock.smartevent.Publisher
     */
    EventEntry start(Name eventName, Object payload, Object context);
    
    /**
     * Pluggable implementation of the result event handling,
     * used internally. Invoked indirectly as part of the 
     * <code>org.forgerock.smartevent.EventEntry.setResult()</code>
     * processing
     * @see org.forgerock.smartevent.Publisher
     */
    void setResult(Object result, EventEntry callingEntry);
    
    /**
     * Pluggable implementation of the end event handling,
     * used internally. Invoked indirectly as part of the 
     * <code>org.forgerock.smartevent.EventEntry.end()</code>
     * processing
     * @see org.forgerock.smartevent.Publisher
     */
    void end(Name eventName, EventEntry callingEntry);
}
