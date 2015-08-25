/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012-2015 ForgeRock AS. All Rights Reserved
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
*
*/

package org.forgerock.openidm.quartz.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.audit.events.AccessAuditEventBuilder;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.http.Context;
import org.forgerock.http.context.RootContext;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.util.LogUtil;
import org.forgerock.openidm.util.LogUtil.LogLevel;
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
 */

public class SchedulerServiceJob implements Job {

    final static Logger logger = LoggerFactory.getLogger(SchedulerServiceJob.class);

    // Default to INFO
    private LogLevel logLevel = LogLevel.INFO;

    public SchedulerServiceJob() {
        // Instances of Job must have a public no-argument constructor.
    }

    /**
     * Builds the Context
     * 
     * @param id  The authentication id
     * @return the SecurityContext for the scheduled job
     */
    private Context newScheduledContext(String id) {
        final Map<String, Object> authzid = new HashMap<String, Object>();
        authzid.put(SecurityContext.AUTHZID_ID, id);
        List<String> roles = new ArrayList<String>();
        roles.add("system");
        authzid.put(SecurityContext.AUTHZID_ROLES, roles);
        authzid.put(SecurityContext.AUTHZID_COMPONENT, "scheduler");
        return new SecurityContext(new RootContext(), id, authzid);
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();

        String invokeLogLevel = (String) data.get(ScheduledService.CONFIGURED_INVOKE_LOG_LEVEL);
        logLevel = LogUtil.asLogLevel(invokeLogLevel);

        String invokeService = (String) data.get(ScheduledService.CONFIGURED_INVOKE_SERVICE);
        Object invokeContext = data.get(ScheduledService.CONFIGURED_INVOKE_CONTEXT);
        ServiceTracker scheduledServiceTracker = (ServiceTracker) getServiceTracker(invokeService);

        logger.debug("Job to invoke service with PID {} and invoke context {} with scheduler context {}",
                new Object[] {invokeService, invokeContext, context});
        logger.debug("Job to invoke service with PID {} with scheduler context {}", invokeService, context);

        Map<String,Object> scheduledServiceContext = new HashMap<>();
        scheduledServiceContext.putAll(data);

        scheduledServiceContext.put(ScheduledService.INVOKER_NAME, "Scheduled " +
                context.getJobDetail().getName() + "-" + context.getScheduledFireTime());
        scheduledServiceContext.put(ScheduledService.SCHEDULED_FIRE_TIME, context.getScheduledFireTime());
        scheduledServiceContext.put(ScheduledService.ACTUAL_FIRE_TIME, context.getFireTime());
        scheduledServiceContext.put(ScheduledService.NEXT_FIRE_TIME, context.getNextFireTime());

        ScheduledService scheduledService = (ScheduledService) scheduledServiceTracker.getService();
        if (scheduledService == null) {
            logger.info("Scheduled service {} to invoke currently not found, not (yet) registered. ", invokeService);
        } else {
            final long startTime = System.currentTimeMillis();
            final Context scheduledContext =
                    newScheduledContext((String) scheduledServiceContext.get(ScheduledService.INVOKER_NAME));
            try {
                LogUtil.logAtLevel(logger, logLevel, "Scheduled service \"{}\" found, invoking.",
                        context.getJobDetail().getFullName());
                scheduledService.execute(scheduledContext, scheduledServiceContext);
                scheduledService.auditScheduledService(
                        scheduledContext,
                        createScheduledAuditEvent(scheduledContext, startTime, context, Status.SUCCESS, null));
                LogUtil.logAtLevel(logger, logLevel, "Scheduled service \"{}\" invoke completed successfully.",
                        context.getJobDetail().getFullName());
            } catch (Exception ex) {
                logger.warn("Scheduled service \"{}\" invocation reported failure: {}",
                        new Object[]{context.getJobDetail().getFullName(), ex.getMessage(), ex});
                try {
                    scheduledService.auditScheduledService(
                            scheduledContext,
                            createScheduledAuditEvent(scheduledContext, startTime, context, Status.FAILURE, ex));
                } catch (ExecutionException exception) {
                    logger.error("Unable to audit scheduled task {}", context.getJobDetail().getFullName(), exception);
                }
            }
        }
        scheduledServiceTracker.close();
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

    private AuditEvent createScheduledAuditEvent(final Context context, final long startTime,
                                                 final JobExecutionContext jobContext, final Status status,
                                                 final Exception e) {
        final AccessAuditEventBuilder auditEventBuilder = new AccessAuditEventBuilder();
        auditEventBuilder
                .authorizationIdFromSecurityContext(context)
                .resourceOperation("scheduler", "CREST", "ScheduledTask", jobContext.getJobDetail().getFullName())
                .transactionIdFromRootContext(context)
                .timestamp(System.currentTimeMillis())
                .authenticationFromSecurityContext(context)
                .eventName("access");

        final long elapsedTime = System.currentTimeMillis() - startTime;

        if (Status.SUCCESS.equals(status)) {
            auditEventBuilder.response(Status.SUCCESS.name(), elapsedTime);
        } else {
            auditEventBuilder.responseWithMessage(Status.FAILURE.name(), elapsedTime, e.getMessage());
        }

        return auditEventBuilder.toEvent();
    }
}
