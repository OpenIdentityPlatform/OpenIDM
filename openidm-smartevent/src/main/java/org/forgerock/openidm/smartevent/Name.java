/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openidm.smartevent;

import java.util.Arrays;
import java.util.Map;

import org.forgerock.guava.common.cache.CacheBuilder;
import org.forgerock.guava.common.cache.CacheLoader;
import org.forgerock.guava.common.cache.LoadingCache;

import org.forgerock.openidm.smartevent.core.DisabledPublisher;
import org.forgerock.openidm.smartevent.core.DisruptorReferringPublisher;
import org.forgerock.openidm.smartevent.core.BlockingPublisher;
import org.forgerock.openidm.smartevent.core.PluggablePublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */

/**
 * Represents the name and additional configuration for an event type
 * 
 * Allows callers to pre-construct, parse event hierarchies and set additional
 * configuration by declaring the Names outside of the publishing of the events
 * 
 * The fluent API allows for easier set-up directly in the declaration example:
 * <code>public static final Name MY_EVENT = Name.get("internal/myevent").setTags(new String[] {MONITOR, APP_EVENT}).setXyz("dummy")</code>
 * 
 * 
 */
public class Name {

    private final static Logger logger = LoggerFactory.getLogger(Name.class);

    /**
     * The available publisher types for handling event pub/sub
     * BLOCKING uses a blocking queue for the events
     * DISRUPTOR uses a non-blocking library with a ring buffer 
     */
    enum PublisherType {BLOCKING, DISRUPTOR};

    // Holds the event stringified name to the Name instance mapping
    static LoadingCache<String, Name> names = CacheBuilder.newBuilder()
            .maximumSize(Integer.valueOf(System.getProperty("openidm.smartevent.maxevents", "1000")))
            .build(
                    new CacheLoader<String, Name>() {
                        @Override
                        public Name load(String key) throws Exception {
                            return new Name(key);
                        }
                    });

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

    PublisherType publisherType;
    
    PluggablePublisher publisherImpl;

    private Name(String stringifiedName) {
        this.stringifiedName = stringifiedName;
        // Default Setting
        setEventsEnabled(Boolean.valueOf(System.getProperty("openidm.smartevent.enabled",
                Boolean.FALSE.toString())));
        this.publisherType = (PublisherType.valueOf(PublisherType.class, System.getProperty("openidm.smartevent.publishertype",
                PublisherType.BLOCKING.toString())));
        // Name parsing can be added here
    }

    /**
     * Factory method to get the event Name object
     * 
     * @param stringifiedName
     *            The string representation of the event name
     * @return the event Name object representing the requested event type
     */
    public final static Name get(String stringifiedName) {
        return names.getUnchecked(stringifiedName);
    }

    /**
     * Factory method to get the event Name object
     * 
     * @param stringifiedName
     *            The string representation of the event name
     * @param tags
     *            optional additional tags to associate with this event type
     * @return the event Name object representing the requested event type
     */
    public final static Name get(final String stringifiedName, final String[] tags) {
        return get(stringifiedName).setTags(tags);
    }

    /**
     * Fluent API to set Tags on this event name
     * 
     * @param tags
     *            the tags to set
     * @return this Name instance for use as a fluent API
     */
    public Name setTags(final String[] tags) {
        if (tags != null) {
            this.tags = Arrays.copyOf(tags, tags.length);
        }

        return this;
    }

    /**
     * Fluent API to set events publishing type
     * 
     * @param enabled
     *            true to enable event publishing for this Name, false to
     *            disable
     * @return this Name instance for use as a fluent API
     */
    public Name setEventsEnabled(boolean enabled) {
        eventsEnabled = enabled;
        if (enabled == true) {
            publisherImpl = createPublisher();
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
     * 
     * @param enabled
     *            true to enable keeping results in the recent event history
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
     * Get all currently registered event names The returned map should not be
     * directly modified.
     * 
     * @return a map pointing from stringified event name representation to the
     *         the event Name object for each event Name
     */
    public final static Map<String, Name> getAllNames() {
        // TODO: consider making/wrapping as immutable
        return names.asMap();
    }

    /**
     * @return the name in String representation
     */
    public final String asString() {
        return stringifiedName;
    }
    
    /**
     * Factory method, could eventually be moved out
     */
    private PluggablePublisher createPublisher() {
        if (PublisherType.DISRUPTOR.equals(publisherType)) {
            logger.debug("Event type: " + stringifiedName + " publisher: DISRUPTOR ");
            return DisruptorReferringPublisher.getInstance();
        } else {
            logger.debug("Event type: " + stringifiedName + " publisher: BLOCKING");
            return BlockingPublisher.getInstance();
        }
    }
}
