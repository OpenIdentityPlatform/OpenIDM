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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.scheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.config.InvalidException;

import static org.mockito.Mockito.*;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


/**
 * Basic unit tests, the main functionality is covered by functional tests
 * 
 * @author aegloff
 */
public class SchedulerServiceTest {

//    private HashMap<String, Object> scope;
    SchedulerService schedulerService;

    @BeforeMethod
    public void beforeMethod() {
        schedulerService = new SchedulerService();
//        scope = new HashMap<String, Object>();
    }

    ComponentContext getMockedContext(Map enhancedConfig, SchedulerService sched) {

        ComponentContext mockedContext = mock(ComponentContext.class);
        BundleContext mockedBundleContext = mock(BundleContext.class);
        
        Dictionary compContextProperties = new Hashtable();
    
        EnhancedConfig mockedEnhancedConfig = mock(EnhancedConfig.class); 
        when(mockedEnhancedConfig.getConfiguration(mockedContext)).thenReturn(enhancedConfig);
        sched.enhancedConfig = mockedEnhancedConfig;
        
        when(mockedContext.getProperties()).thenReturn(compContextProperties);
        when(mockedContext.getBundleContext()).thenReturn(mockedBundleContext);

        return mockedContext;
    }
    
    @Test
    public void configParsingTest() throws InvalidException {
        // Check valid configuration succeeds
        SchedulerService schedulerService = new SchedulerService();
        
        Map config = new HashMap();
        config.put(SchedulerService.SCHEDULE_TYPE, SchedulerService.SCHEDULE_TYPE_CRON);
        config.put(SchedulerService.SCHEDULE_START_TIME, "2011-05-03T10:00:00");
        config.put(SchedulerService.SCHEDULE_END_TIME, "2011-05-03T15:59:59");
        config.put(SchedulerService.SCHEDULE_CRON_SCHEDULE, "0 30 10-13 ? * WED,FRI");
        config.put(SchedulerService.SCHEDULE_TIME_ZONE, "America/Los_Angeles");
        config.put(SchedulerService.SCHEDULE_INVOKE_SERVICE, "active-sync");
        config.put(SchedulerService.SCHEDULE_INVOKE_CONTEXT, "system-x");
        
        ComponentContext validConfig = getMockedContext(config, schedulerService);
        
        schedulerService.initConfig(validConfig);
        
        // mimimize trying to these impl details, basic sanity check on one
        assertNotNull(schedulerService.startTime); 
    }
    
    @Test(enabled = false, expectedExceptions = InvalidException.class)
    public void invalidConfigParsingTest() throws InvalidException {
        // Check invalid configuration fails
        SchedulerService schedulerService = new SchedulerService();
        
        Map config = new HashMap();
        config.put(SchedulerService.SCHEDULE_TYPE, SchedulerService.SCHEDULE_TYPE_CRON);
        config.put(SchedulerService.SCHEDULE_START_TIME, "2011-05-03T10:00:00");
        config.put(SchedulerService.SCHEDULE_END_TIME, "2011-05-03T15:59:59");
        config.put(SchedulerService.SCHEDULE_CRON_SCHEDULE, "0 30 10-13 ? * WED,FRI");
        config.put(SchedulerService.SCHEDULE_TIME_ZONE, "America/Los_Angeles");
        // test missing config.put(SchedulerService.SCHEDULE_INVOKE_SERVICE, "active-sync");
        config.put(SchedulerService.SCHEDULE_INVOKE_CONTEXT, "system-x");
        
        ComponentContext invalidConfig = getMockedContext(config, schedulerService);
        
        schedulerService.initConfig(invalidConfig);
        
    }
}
