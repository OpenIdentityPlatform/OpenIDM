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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.MutableLong;

/**
 * Event handler for monitoring and statistics
 * Expects a single thread assigned to gather the statistics
 * 
 * @author aegloff
 */
public class StatisticsHandler implements 
        EventHandler<DisruptorReferringEventEntry>,
        StatisticsHandlerMBean {
    
    private final static Logger logger = LoggerFactory.getLogger(StatisticsHandler.class);
    
    public final static String MBEAN_NAME = "OpenIDM:type=Statistics";
    
    final static NumberFormat MILLISEC_FORMAT = new DecimalFormat("###,###,##0.### ms");
    
    /**
     * Access to the ring buffer for monitoring/history display purposes
     */
    //Disruptor<DisruptorEventEntry> disruptor;
    Disruptor<DisruptorReferringEventEntry> disruptor;

    /**
     * Keep track of monitoring data per event Name
     */
    public Map<String, MonitoringInfo> map = new HashMap<String, MonitoringInfo>();
    
    public StatisticsHandler(
            /*Disruptor<DisruptorEventEntry> disruptor*/
            Disruptor<DisruptorReferringEventEntry> disruptor){
        
        this.disruptor = disruptor;
        
        javax.management.MBeanServer mbs = java.lang.management.ManagementFactory.getPlatformMBeanServer();
        javax.management.ObjectName statName = null;
        try {
           statName = new javax.management.ObjectName(MBEAN_NAME);
           mbs.registerMBean(this, statName);
        } catch(Exception ex) {
           logger.info("Failed to register statistics MBean", ex);;
        }
    }
    
    /**
     * @inheritDoc
     */
    public Map getTotals() {
        Map stats = new TreeMap();
        for (java.util.Map.Entry<String, MonitoringInfo> entry : map.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().toString());
        }
        return stats;
    }
    
    /**
     * @inheritDoc
     */
    public Map getRecent() {
        // TODO: consider adding history for not yet end()-ed events
        // Present history ordered by start time, with latest start time first
        Map recent = new TreeMap(Collections.reverseOrder());
        try {
            com.lmax.disruptor.RingBuffer<DisruptorReferringEventEntry > buf = disruptor.getRingBuffer();
            if (buf != null) {
                for (int count = 0; count < buf.getBufferSize(); count++) {
                    DisruptorReferringEventEntry entry = buf.get(count);
                    if (entry != null && entry.startTime > 0) {
                        recent.put(Long.valueOf(entry.startTime), entry.toString());
                    }
                }
            }
        } catch (RuntimeException ex) {
            logger.info("Failure in getting recent event history", ex);
        }
        return recent;
    }
    
    /**
     * @inheritDoc
     */
    public void resetAllStatistics() {
        for (MonitoringInfo entry : map.values()) {
            entry.reset();
        }
    }
    
    /**
     * @inheritDoc
     */
    public void resetStatistics(String eventName) {
        MonitoringInfo entry = map.get(eventName);
        if (entry != null) {
            entry.reset();
        } else {
            throw new IllegalArgumentException("Event name " + eventName + " does not match an existing name.");
        }
    }
    
    // TODO: move out of statistics
    /**
     * @inheritDoc
     */
    public void setEventsEnabled(String eventName, boolean enabled) {
        Name entry = Name.get(eventName);
        if (entry != null) {
            entry.setEventsEnabled(enabled);
        } else {
            throw new IllegalArgumentException("Event name " + eventName + " does not match an existing name.");
        }
    }
    
    // TODO: move out of statistics
    /**
     * @inheritDoc
     */
    public Map<String, Boolean> getEventsEnabledMap() {
        Map<String, Boolean> enabledMap = new TreeMap<String, Boolean>();
        for (Map.Entry<String, Name> entry : Name.getAllNames().entrySet()) {
            enabledMap.put(entry.getKey(), Boolean.valueOf(entry.getValue().getEventsEnabled()));
        }
        return enabledMap;
    }
    
    // TODO: move out of statistics
    /**
     * @inheritDoc
     */
    public void setResultHistoryEnabled(String eventName, boolean enabled) {
        Name entry = Name.get(eventName);
        if (entry != null) {
            entry.setResultHistoryEnabled(enabled);
        } else {
            throw new IllegalArgumentException("Event name " + eventName + " does not match an existing name.");
        }
    }
    
    // TODO: move out of statistics
    /**
     * @inheritDoc
     */
    public Map<String, Boolean> getResultHistoryEnabledMap() {
        Map<String, Boolean> enabledMap = new TreeMap<String, Boolean>();
        for (Map.Entry<String, Name> entry : Name.getAllNames().entrySet()) {
            enabledMap.put(entry.getKey(), Boolean.valueOf(entry.getValue().getResultHistoryEnabled()));
        }
        return enabledMap;
    }
    
    /**
     * TODO: if DisruptorShortEventEntry gets removed, remove as well
     * 
     * Consumes the events off the ring buffer
     */
    public void onEvent(final DisruptorShortEventEntry eventEntry, final long sequence, final boolean endOfBatch) throws Exception {
        long diff = eventEntry.endTime-eventEntry.startTime;

        // Oddly enough in initial tests this seems slower than requiring map lookups
        /*
        MonitoringInfo info = eventEntry.eventName.monitoring;
        info.totalTime += diff;
        ++info.totalInvokes;
        */
        
        MonitoringInfo entry = map.get(eventEntry.eventName.asString());
        if (entry == null) {
            entry = new MonitoringInfo();
            map.put(eventEntry.eventName.asString(), entry);
        }
        entry.totalTime += diff;
        entry.totalInvokes++;
    }
    
    // TODO: more research on latency of batched end time option
    // Mark the beginning of processing a batch of events 
    boolean newBatch = true;
    long batchTime = -1;
    
    /**
     * Consumes the events off the ring buffer
     */
    public void onEvent(final DisruptorReferringEventEntry eventEntryWrap, final long sequence, final boolean endOfBatch) throws Exception {
        // TODO: provide switch for batch or individual end time
        //if (newBatch) {
        //    batchTime = System.nanoTime();
        //}
        // eventEntryWrap.endTime = batchTime;
        
        EventEntryImpl eventEntry = eventEntryWrap.delegate;
        long diff = eventEntryWrap.getRawDuration();
        
        MonitoringInfo entry = map.get(eventEntry.eventName.asString());
        if (entry == null) {
            entry = new MonitoringInfo();
            map.put(eventEntry.eventName.asString(), entry);
        }
        entry.totalTime += diff;
        entry.totalInvokes++;
        if (endOfBatch) {
            newBatch = true;
        } else {
            newBatch = false;
        }
    }
    
    /**
     * Helper to format nanosecond difference in human readable ms
     * if a negative value is passed, returns "N/A"
     */
    static String formatNsAsMs(long nanoseconds) {
        // Convert from nanoseconds to milliseconds
        if (nanoseconds >= 0) {
            return MILLISEC_FORMAT.format(nanoseconds/1000000d);
        } else {
            return "N/A";
        }
    }
    
}