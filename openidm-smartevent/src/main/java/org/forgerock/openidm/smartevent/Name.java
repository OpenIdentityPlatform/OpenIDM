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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.forgerock.openidm.smartevent.core.DisabledPublisher;
import org.forgerock.openidm.smartevent.core.DisruptorShortPublisher;
import org.forgerock.openidm.smartevent.core.DisruptorReferringPublisher;
import org.forgerock.openidm.smartevent.core.MonitoringInfo;
import org.forgerock.openidm.smartevent.core.PluggablePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.SingleThreadedClaimStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;

/**
 * @author aegloff
 */
import com.lmax.disruptor.EventFactory;

/**
 * Represents the name and additional configuration for an event type
 * 
 * Allows callers to pre-construct, parse event hierarchies and set additional configuration
 * by declaring the Names outside of the publishing of the events
 * 
 * The fluent API allows for easier set-up directly in the declaration
 * example:
 * <code>public static final Name MY_EVENT = Name.get("internal/myevent").setTags(new String[] {MONITOR, APP_EVENT}).setXyz("dummy")</code>
 * 
 * @author aegloff
 *
 */
public class Name {

    // Holds the event stringified name to the Name instance mapping
    static ConcurrentMap<String, Name> names = new ConcurrentHashMap<String, Name>();

    /**
     * Stringified version of the event name
     */
    String stringifiedName;
    
    /**
     * Optional tags to associate with this event name
     */
    String[] tags;
    
    /**
     * Whether event processing should be enabled or not for this event name
     */
    boolean eventsEnabled = true;
    
    /**
     * Whether result details should be kept in the recent history
     */
    boolean resultHistoryEnabled = false;
    
    PluggablePublisher publisherImpl;
    
    private Name(String stringifiedName) {
        this.stringifiedName = stringifiedName;
        // Default Setting
        setEventsEnabled(Boolean.valueOf(System.getProperty("openidm.smartevent.enabled",
                Boolean.FALSE.toString())));
        // Name parsing can be added here
    }

    /**
     * Factory method to get the event Name object
     *
     * @param stringifiedName The string representation of the event name
     * @return the event Name object representing the requested event type
     */
    public final static Name get(String stringifiedName) {
        Name aName = names.get(stringifiedName);
        if (aName == null) {
            aName = new Name(stringifiedName);
            // Ensure first create/insert wins
            // (Because of avoiding locking, multiple could create and try to insert this concurrently)
            Name existing = names.putIfAbsent(stringifiedName, aName);
            if (existing != null) {
                aName = existing;
            }
        }
        return aName;
    }
    
    /**
     * Factory method to get the event Name object
     *
     * @param stringifiedName The string representation of the event name
     * @param tags optional additional tags to associate with this event type
     * @return the event Name object representing the requested event type
     */
    public final static Name get(String stringifiedName, String[] tags) {
        Name aName = get(stringifiedName);
        aName.tags = tags;
        return aName;
    }
    
    /**
     * Fluent API to set Tags on this event name
     * @param tags the tags to set
     * @return this Name instance for use as a fluent API
     */
    public Name setTags(String[] tags) {
        this.tags = tags;
        return this;
    }
    
    /**
     * Fluent API to set events publishing behavior
     * @param enabled true to enable event publishing for this Name, false to disable
     * @return this Name instance for use as a fluent API
     */
    public Name setEventsEnabled(boolean enabled) {
        eventsEnabled = enabled;
        if (enabled == true) {
            publisherImpl = DisruptorReferringPublisher.getInstance(); 
        } else {
            publisherImpl = DisabledPublisher.getInstance();
        }
        return this;
    }
    
    /**
     * @return the events publishing state
     */
    public boolean getEventsEnabled() {
        return eventsEnabled;
    }
    
    /**
     * Fluent API to set result history behavior
     * @param enabled true to enable keeping results in the recent event history
     * @return this Name instance for use as a fluent API
     */
    public Name setResultHistoryEnabled(boolean enabled) {
        resultHistoryEnabled = enabled;
        return this;
    }
    
    /**
     * @return whether the result history is enabled
     */
    public boolean getResultHistoryEnabled() {
        return resultHistoryEnabled;
    }
    
    /**
     * Get all currently registered event names 
     * The returned map should not be directly modified.
     *
     * @return a map pointing from stringified event name representation to the the event Name object 
     * for each event Name 
     */
    public final static Map<String, Name> getAllNames() {
        // TODO: consider making/wrapping as immutable
        return names;
    }
    
    /**
     * @return the name in String representation 
     */
    public final String asString() {
        return stringifiedName;
    }
}