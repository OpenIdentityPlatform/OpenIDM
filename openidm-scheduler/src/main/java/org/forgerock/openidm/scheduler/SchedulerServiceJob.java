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
import java.util.HashMap;

import org.osgi.util.tracker.ServiceTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Scheduler service job using Quartz
 * 
 * Invokes the configured ScheduledService in OSGi
 * 
 * @author aegloff
 */

public class SchedulerServiceJob implements Job {

    final static Logger logger = LoggerFactory.getLogger(SchedulerServiceJob.class);

    public SchedulerServiceJob() {
        // Instances of Job must have a public no-argument constructor.
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap data = context.getMergedJobDataMap();
        String invokeService = (String) data.get(ScheduledService.CONFIGURED_INVOKE_SERVICE);
        Object invokeContext = data.get(ScheduledService.CONFIGURED_INVOKE_CONTEXT);
        ServiceTracker scheduledServiceTracker = (ServiceTracker) data.get(SchedulerService.SERVICE_TRACKER);
        logger.debug("Job to invoke service with PID {} and invoke context {} with scheduler context {}", 
                new Object[] {invokeService, invokeContext, context});

        Map<String,Object> scheduledServiceContext = new HashMap<String,Object>();
        scheduledServiceContext.putAll(data);
        scheduledServiceContext.remove(SchedulerService.SERVICE_TRACKER);
        scheduledServiceContext.put(ScheduledService.INVOKER_NAME, "Scheduled " +
                context.getJobDetail().getName() + "-" + context.getScheduledFireTime());
        scheduledServiceContext.put(ScheduledService.SCHEDULED_FIRE_TIME, context.getScheduledFireTime());
        scheduledServiceContext.put(ScheduledService.ACTUAL_FIRE_TIME, context.getFireTime());
        scheduledServiceContext.put(ScheduledService.NEXT_FIRE_TIME, context.getNextFireTime());
        
        ScheduledService scheduledService = (ScheduledService) scheduledServiceTracker.getService();
        if (scheduledService == null) {
            // TODO: consider guarding against too frequent logging
            logger.info("Scheduled service {} to invoke currently not found, not (yet) registered. ", invokeService);
        } else {
            try {
                logger.info("Scheduled service \"{}\" found, invoking.", context.getJobDetail().getFullName());
                scheduledService.execute(scheduledServiceContext);
                logger.info("Scheduled service \"{}\" invoke completed successfully.", context.getJobDetail().getFullName());
            } catch (Exception ex) {
                logger.warn("Scheduled service \"{}\" invocation reported failure: {}",
                        new Object[]{context.getJobDetail().getFullName(), ex.getMessage()}, ex);
            }
        }
    }
}
