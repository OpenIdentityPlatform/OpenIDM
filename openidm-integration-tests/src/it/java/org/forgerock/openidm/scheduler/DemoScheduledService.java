/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.scheduler;

import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;

import org.forgerock.openidm.scheduler.ScheduledService;
import org.forgerock.openidm.scheduler.ExecutionException;

import org.osgi.service.component.ComponentContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demo scheduled service. 
 * 
 * An example test configuration file in /conf would be: 
 * 
 * org.forgerock.scheduler-mytestschedule.json
 * {
 *     "schedule" : "0/10 * * * * ?",
 *     "invokeService" : "demo-scheduled",
 *     "invokeContext" : "test context"
 * 
 * This would invoke this service every 10 seconds
 * 
 */
@Component(name = "org.forgerock.openidm.demo-scheduled", immediate=true)
@Service 
@Properties({
    @Property(name = "service.description", value = "Demo Scheduled Service"),
    @Property(name = "service.vendor", value = "ForgeRock AS")
})
public class DemoScheduledService implements ScheduledService {
    final static Logger logger = LoggerFactory.getLogger(DemoScheduledService.class);
    
    /**
     * Invoked by the scheduler when the scheduler triggers.
     * 
     * @param context Context information passed by the scheduler service
     * @throws ExecutionException if execution of the scheduled work failed.
     * Implementations can also throw RuntimeExceptions which will get logged.
     */
    public void execute(Map<String, Object> context) throws ExecutionException {
        System.out.println("Demo service got invoked");
        System.out.println("Scheduled fire time " + context.get(SCHEDULED_FIRE_TIME));
        System.out.println("Actual fire time " + context.get(ACTUAL_FIRE_TIME));
        System.out.println("Next fire time " + context.get(NEXT_FIRE_TIME));
        System.out.println("Configured invoke service " + context.get(CONFIGURED_INVOKE_SERVICE));
        System.out.println("Configured invoke context " + context.get(CONFIGURED_INVOKE_CONTEXT));
    }
}