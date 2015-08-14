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

import java.lang.management.ManagementFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.forgerock.json.JsonValue;

import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.smartevent.core.StatisticsHandler;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Testing of smart event publisher
 * 
 */
public class PublisherTest {

    // Format for information purposes
    final static NumberFormat MILLISEC_FORMAT = new DecimalFormat("###,###,##0.###### ms");
    
    // Smart event names for the tests
    final static Name EVENT_BASIC_TEST = Name.get("openidm/test/basic");
    final static Name EVENT_PERF_SMOKE_TEST = Name.get("openidm/test/performance/smoketest");

    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    
    // Smart event also exposes statistics via JMX
    ObjectName getStatisticsMBean() throws MalformedObjectNameException {
        return new ObjectName(StatisticsHandler.MBEAN_NAME);
    }
    
    @Test(enabled = true)
    public void validateStartEndStatistics() throws Exception {
        Object dummyPayload = new JsonValue("{'test': 'some value'}");
        Object dummyContext1 = "Some context value";
        EventEntry measure = Publisher.start(EVENT_BASIC_TEST, dummyPayload, dummyContext1);
        Math.sqrt(342972);
        measure.end();
        Thread.currentThread().sleep(100); // Statistics is not necessarily updated in a synchronous fashion
        Object totals = mbs.getAttribute(getStatisticsMBean(), "Totals");
        Assert.assertTrue(totals instanceof Map);
        Object statisticsEntry = ((Map)totals).get(EVENT_BASIC_TEST.asString());
        Assert.assertNotNull(statisticsEntry, "Expected a statistic entry for " + EVENT_BASIC_TEST + ", but is null.");
    }
    
    /**
     * Do a number of start/end measurements after an initial warm up, 
     * single threaded
     * Because of varying test environments the 
     * failure condition for this basic performance smoke test
     * is set at a lax level. 
     * Real performance (printed on standard out) should be much higher
    */
    @Test(enabled=true)
    public void performanceSmokeTest() throws Exception {

        int warmup = 100000;
        int iterations = 100000;
        int maxTimeToAllow = 10000; // ms time to allow for this test not to fail
        
        Object dummyPayload = new JsonValue("{'test': 'value'}");
        Object dummyContext1 = "reconID: xyz123";

        for (int i = 0; i < warmup; i++) {
            EventEntry measure = Publisher.start(EVENT_PERF_SMOKE_TEST, dummyPayload, dummyContext1);
            measure.end();
        }
        Thread.currentThread().sleep(200); 
        
        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            EventEntry measure = Publisher.start(EVENT_PERF_SMOKE_TEST, dummyPayload, dummyContext1);
            measure.end();
        }
        long end = System.currentTimeMillis();
        long diff = end - start;
        System.out.println("Smartevent " + iterations + " iterations "
                + "took " + diff + " milliseconds. "
                + "Each start/end event took approx: " + MILLISEC_FORMAT.format(diff/(double)iterations)
                + ". This smoke test allows max " + maxTimeToAllow + " ms ");
        
        Assert.assertTrue(diff < maxTimeToAllow, "Performance warning: " + iterations + " did not complete in the expected max " + maxTimeToAllow + " ms");
    }
    
}
