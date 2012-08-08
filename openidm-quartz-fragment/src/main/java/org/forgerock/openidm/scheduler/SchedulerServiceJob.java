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

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceContext;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Generates a new scheduler service context, suitable for inclusion as a parent context
     * to a resource via the router. For now, this is simply placed on the ObjectSetContext
     * stack prior to the call to the scheduled service, but in the future should be sent in
     * a request context via the router. This does not create a faux request context; that is
     * incumbent on the invoked service to establish if necessary.
     */
    private JsonValue newSchedulerContext(Map<String, Object> ssc) {
        JsonValue context = JsonResourceContext.newContext("scheduler", JsonResourceContext.newRootContext());
        HashMap<String, Object> security = new HashMap<String, Object>();
        security.put("user", ssc.get(ScheduledService.INVOKER_NAME));
        context.put("security", security);
        context.put("scheduled-time", ssc.get(ScheduledService.SCHEDULED_FIRE_TIME));
        context.put("actual-time", ssc.get(ScheduledService.ACTUAL_FIRE_TIME));
        context.put("next-time", ssc.get(ScheduledService.NEXT_FIRE_TIME));
        context.put("invoke-service", ssc.get(ScheduledService.CONFIGURED_INVOKE_SERVICE));
        context.put("invoke-context", ssc.get(ScheduledService.CONFIGURED_INVOKE_CONTEXT));
        return context;
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap data = context.getMergedJobDataMap();
        String invokeService = (String) data.get(ScheduledService.CONFIGURED_INVOKE_SERVICE);
        Object invokeContext = data.get(ScheduledService.CONFIGURED_INVOKE_CONTEXT);
        //ServiceTracker scheduledServiceTracker = (ServiceTracker) data.get(SchedulerService.SERVICE_TRACKER);
        ServiceTracker scheduledServiceTracker = (ServiceTracker) getServiceTracker(invokeService);
        logger.debug("Job to invoke service with PID {} and invoke context {} with scheduler context {}", 
                new Object[] {invokeService, invokeContext, context});
        logger.debug("Job to invoke service with PID {} with scheduler context {}", new Object[] {invokeService, context});

        Map<String,Object> scheduledServiceContext = new HashMap<String,Object>();
        scheduledServiceContext.putAll(data);
        //scheduledServiceContext.remove(SchedulerService.SERVICE_TRACKER);
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
// TODO: Migrate calls to router; pass context in request.
                ObjectSetContext.push(newSchedulerContext(scheduledServiceContext));
                try {
                    scheduledService.execute(scheduledServiceContext);
                } finally {
                    ObjectSetContext.pop();
                }
                logger.info("Scheduled service \"{}\" invoke completed successfully.", context.getJobDetail().getFullName());
            } catch (Exception ex) {
                logger.warn("Scheduled service \"{}\" invocation reported failure: {}",
                        new Object[]{context.getJobDetail().getFullName(), ex.getMessage(), ex});
            }
        }
    }
    
    ServiceTracker getServiceTracker(String servicePID) throws InvalidException {
        Filter filter = null;
        BundleContext context = null;
        try {
            context = FrameworkUtil.getBundle(SchedulerServiceJob.class).getBundleContext();
            filter = FrameworkUtil.createFilter("(&(" + Constants.OBJECTCLASS + "=" + ScheduledService.class.getName() + ")" 
                    + "(service.pid=" + servicePID + "))");
        } catch (InvalidSyntaxException ex) {
            throw new InvalidException("Failure in setting up scheduler to find service to invoke. One possible cause is an invalid " 
                    + "invokeService property. :  " + ex.getMessage(), ex);
        }
        ServiceTracker serviceTracker = new ServiceTracker(context, filter, null);
        serviceTracker.open();
        
        return serviceTracker;
    }
}
