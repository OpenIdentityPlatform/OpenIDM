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
 * Copyright © 2011-2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


/**
 * Basic unit tests, the main functionality is covered by functional tests
 *
 */
public class SchedulerServiceTest {

    @BeforeMethod
    public void beforeMethod() {
    }

    ComponentContext getMockedContext(Map enhancedConfig, SchedulerService sched) {

        ComponentContext mockedContext = mock(ComponentContext.class);
        BundleContext mockedBundleContext = mock(BundleContext.class);

        Dictionary compContextProperties = new Hashtable();

        EnhancedConfig mockedEnhancedConfig = mock(EnhancedConfig.class);
        when(mockedEnhancedConfig.getConfiguration(mockedContext)).thenReturn(enhancedConfig);
        sched.bindEnhancedConfig(mockedEnhancedConfig);

        when(mockedContext.getProperties()).thenReturn(compContextProperties);
        when(mockedContext.getBundleContext()).thenReturn(mockedBundleContext);

        return mockedContext;
    }

    @Test
    public void configParsingTest() throws ResourceException {
        // Check valid configuration succeeds
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(SchedulerService.SCHEDULE_TYPE, SchedulerService.SCHEDULE_TYPE_CRON);
        config.put(SchedulerService.SCHEDULE_START_TIME, "2011-05-03T10:00:00");
        config.put(SchedulerService.SCHEDULE_END_TIME, "2011-05-03T15:59:59");
        config.put(SchedulerService.SCHEDULE_CRON_SCHEDULE, "0 30 10-13 ? * WED,FRI");
        config.put(SchedulerService.SCHEDULE_TIME_ZONE, "America/Los_Angeles");
        config.put(SchedulerService.SCHEDULE_INVOKE_SERVICE, "active-sync");
        config.put(SchedulerService.SCHEDULE_INVOKE_CONTEXT, "system-x");

        ScheduleConfig scheduleConfig = new ScheduleConfig(new JsonValue(config));

        // mimimize trying to these impl details, basic sanity check on one
        assertNotNull(scheduleConfig.getStartTime());
    }

    @Test(enabled = false, expectedExceptions = ResourceException.class)
    public void invalidConfigParsingTest() throws ResourceException {
        // Check invalid configuration fails
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(SchedulerService.SCHEDULE_TYPE, SchedulerService.SCHEDULE_TYPE_CRON);
        config.put(SchedulerService.SCHEDULE_START_TIME, "2011-05-03T10:00:00");
        config.put(SchedulerService.SCHEDULE_END_TIME, "2011-05-03T15:59:59");
        config.put(SchedulerService.SCHEDULE_CRON_SCHEDULE, "0 30 10-13 ? * WED,FRI");
        config.put(SchedulerService.SCHEDULE_TIME_ZONE, "America/Los_Angeles");
        // test missing config.put(SchedulerService.SCHEDULE_INVOKE_SERVICE, "active-sync");
        config.put(SchedulerService.SCHEDULE_INVOKE_CONTEXT, "system-x");

        ScheduleConfig scheduleConfig = new ScheduleConfig(new JsonValue(config));

    }
}
