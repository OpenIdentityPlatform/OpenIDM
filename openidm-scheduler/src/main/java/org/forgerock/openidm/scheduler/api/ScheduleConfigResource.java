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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.scheduler.api;

import static org.forgerock.openidm.scheduler.ScheduleConfig.SCHEDULER_TIME_FORMAT;
import static org.forgerock.openidm.scheduler.SchedulerService.SCHEDULE_TYPE_CRON;
import static org.forgerock.openidm.scheduler.SchedulerService.SCHEDULE_TYPE_SIMPLE;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.Default;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Format;
import org.forgerock.api.annotations.Title;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.scheduler.ScheduleConfig;
import org.forgerock.openidm.scheduler.SchedulerService;

/**
 * Resource for {@link SchedulerService} Job Configuration.
 * This class is very close to {@link ScheduleConfig} but API descriptor friendly.
 */
@Title("Attributes of the Quartz Schedule")
public class ScheduleConfigResource {

    private boolean enabled = true;
    private boolean persisted;
    private String misfirePolicy;
    private String triggerType;
    private Date startTime;
    private Date endTime;
    private String cronSchedule;
    private String timeZone;
    private String invokeService;
    private Map<String, Object> invokeContext;
    private String invokeLogLevel;
    private boolean concurrentExecution;
    private TriggerResource[] triggers;

    /**
     * Returns true if the schedule is enabled.
     *
     * @return true if the schedule is enabled.
     */
    @Description("Enable job schedule")
    @Default("true")
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns true when the schedule config is persisted in the repo vs. stored in memory.
     *
     * @return true when the schedule config is persisted in the repo vs. stored in memory.
     */
    @Description("True when in the repo vs. stored in memory")
    @Default("false")
    public boolean isPersisted() {
        return persisted;
    }

    /**
     * Returns the misfire policy for this schedule.
     *
     * @return the misfire policy for this schedule.
     */
    @Description("Misfire handling ('" + SchedulerService.MISFIRE_POLICY_FIRE_AND_PROCEED + "', '"
            + SchedulerService.MISFIRE_POLICY_DO_NOTHING + "')")
    @Default(SchedulerService.MISFIRE_POLICY_FIRE_AND_PROCEED)
    public String getMisfirePolicy() {
        return misfirePolicy;
    }

    /**
     * Returns the schedule type which can either be 'cron' or 'simple'.
     *
     * @return the schedule type which can either be 'cron' or 'simple'.
     */
    @JsonProperty("type")
    @Description("Schedule type ('" + SCHEDULE_TYPE_CRON + "', '" + SCHEDULE_TYPE_SIMPLE + "')")
    @Default("cron")
    public String getScheduleType() {
        return triggerType;
    }

    /**
     * Returns the start time for the job schedule.
     *
     * @return the start time for the job schedule.
     */
    @Description("Start time for job in the following format, without TZ: '" + SCHEDULER_TIME_FORMAT + "'")
    @Format("date-time")
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Returns the end time for the job schedule.
     *
     * @return the end time for the job schedule.
     */
    @Description("End time for job in the following format, without TZ: '" + SCHEDULER_TIME_FORMAT + "'")
    @Format("date-time")
    public Date getEndTime() {
        return endTime;
    }

    /**
     * Returns the string representation of the cron schedule.
     *
     * @return the string representation of the cron schedule.
     */
    @Description("Cron schedule to use for schedule types of 'cron'")
    @JsonProperty("schedule")
    public String getCronSchedule() {
        return cronSchedule;
    }

    /**
     * Returns the TimeZone ID used for the schedule start and end time values.
     *
     * @return the TimeZone ID used for the schedule start and end time values.
     */
    @Description("The TimeZone ID used for the schedule start and end time values. See Java documentation for "
            + "acceptable timezone formats")
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Returns the service PID that the schedule will invoke.
     *
     * @return the service PID that the schedule will invoke.
     */
    @Description("Service PID of the service to be invoked. If no '.' is provided, it is prefixed with '"
            + ServerConstants.SERVICE_RDN_PREFIX + "'")
    public String getInvokeService() {
        return invokeService;
    }

    /**
     * Returns the context to be passed to the Service when the schedule invokes the invokeService.
     *
     * @return the context to be passed to the Service when the schedule invokes the invokeService.
     */
    @Description("Context to be passed to the invokeService")
    public Map<String, Object> getInvokeContext() {
        return invokeContext;
    }

    /**
     * Returns the log level to be utilized while invoking the job.
     *
     * @return The log level to be utilized while invoking the job.
     */
    @Description("The SLF4J log level used while invoking the job")
    @Default("info")
    public String getInvokeLogLevel() {
        return invokeLogLevel;
    }

    /**
     * Returns if the job will be able to run while any previous run is still executing.
     *
     * @return True if the job will be able to run while any previous run is still executing.
     */
    @Description("Enables the job to run again while any previous run is still executing")
    @Default("false")
    public boolean getConcurrentExecution() {
        return concurrentExecution;
    }

    /**
     * Returns array of triggers that are acquired by this OpenIDM instance.
     *
     * @return Array of triggers that are acquired by this OpenIDM instance.
     */
    @Description("Array of triggers that are acquired by this OpenIDM instance")
    public TriggerResource[] getTriggers() {
        return triggers;
    }
}