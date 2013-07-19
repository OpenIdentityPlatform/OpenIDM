/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.scheduler;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;

public class ScheduleConfig {

    private Boolean enabled = null;
    private Boolean persisted = null;
    private String misfirePolicy = null;
    private String scheduleType = null;
    private Date startTime = null;
    private Date endTime = null;
    private String cronSchedule = null;
    private TimeZone timeZone = null;
    private String invokeService = null;
    private Object invokeContext = null;
    private String invokeLogLevel = null;
    private Boolean concurrentExecution = null;

    public ScheduleConfig(JsonValue config) {
        JsonValue enabledValue = config.get(SchedulerService.SCHEDULE_ENABLED);
        if (enabledValue.isString()) {
            enabled = Boolean.parseBoolean(enabledValue.defaultTo("true").asString());
        } else {
            enabled = enabledValue.defaultTo(Boolean.TRUE).asBoolean();
        }
        JsonValue persistedValue = config.get(SchedulerService.SCHEDULE_PERSISTED);
        if (persistedValue.isString()) {
            persisted = Boolean.parseBoolean(persistedValue.defaultTo("false").asString());
        } else {
            persisted = persistedValue.defaultTo(Boolean.FALSE).asBoolean();
        }
        JsonValue concurrentExecutionValue = config.get(SchedulerService.SCHEDULE_CONCURRENT_EXECUTION);
        if (concurrentExecutionValue.isString()) {
            concurrentExecution = Boolean.parseBoolean(concurrentExecutionValue.defaultTo("false").asString());
        } else {
            concurrentExecution = concurrentExecutionValue.defaultTo(Boolean.FALSE).asBoolean();
        }
        misfirePolicy = config.get(SchedulerService.SCHEDULE_MISFIRE_POLICY).defaultTo(SchedulerService.MISFIRE_POLICY_FIRE_AND_PROCEED).asString();
        if (!misfirePolicy.equals(SchedulerService.MISFIRE_POLICY_FIRE_AND_PROCEED) &&
                !misfirePolicy.equals(SchedulerService.MISFIRE_POLICY_DO_NOTHING)) {
            throw new InvalidException(new StringBuilder("Invalid misfire policy: ").append(misfirePolicy).toString());
        }
        cronSchedule = config.get(SchedulerService.SCHEDULE_CRON_SCHEDULE).asString();
        scheduleType = config.get(SchedulerService.SCHEDULE_TYPE).asString();
        invokeService = config.get(SchedulerService.SCHEDULE_INVOKE_SERVICE).asString();
        if (!StringUtils.isNotBlank(invokeService)) {
            throw new InvalidException("Invalid scheduler configuration, the "
                    + SchedulerService.SCHEDULE_INVOKE_SERVICE
                    + " property needs to be set but is empty. "
                    + "Complete config:" + config);
        } else {
            // service PIDs fragments are prefixed with openidm qualifier
            if (!invokeService.contains(".")) {
                String fragment = invokeService;
                invokeService = SchedulerService.SERVICE_RDN_PREFIX + fragment;
            }
        }
        invokeContext = config.get(SchedulerService.SCHEDULE_INVOKE_CONTEXT).getObject();
        invokeLogLevel = config.get(SchedulerService.SCHEDULE_INVOKE_LOG_LEVEL).defaultTo("info").asString();
        String timeZoneString = config.get(SchedulerService.SCHEDULE_TIME_ZONE).asString();
        String startTimeString = config.get(SchedulerService.SCHEDULE_START_TIME).asString();
        String endTimeString = config.get(SchedulerService.SCHEDULE_END_TIME).asString();
        if (StringUtils.isNotBlank(timeZoneString)) {
            timeZone = TimeZone.getTimeZone(timeZoneString);
            // JDK has fall-back behavior to GMT if it doesn't understand timezone passed
            if (!timeZoneString.equals(timeZone.getID())) {
                throw new InvalidException("Scheduler configured timezone is not understood: " + timeZoneString);
            }
        }
        if (StringUtils.isNotBlank(startTimeString)) {
            Calendar parsed = DatatypeConverter.parseDateTime(startTimeString);
            startTime = parsed.getTime();
            // TODO: enhanced logging for failure
        }

        if (StringUtils.isNotBlank(endTimeString)) {
            Calendar parsed = DatatypeConverter.parseDateTime(endTimeString);
            endTime = parsed.getTime();
            // TODO: enhanced logging for failure
        }

        if (StringUtils.isNotBlank(scheduleType)) {
            if (!scheduleType.equals(SchedulerService.SCHEDULE_TYPE_CRON)) {
                throw new InvalidException("Scheduler configuration contains unknown schedule type "
                        + scheduleType + ". Known types include " + SchedulerService.SCHEDULE_TYPE_CRON);
            }
        }
    }

    public ScheduleConfig(CronTrigger trigger, JobDataMap map, boolean persisted, boolean concurrentExecution) {
        this.persisted = persisted;
        this.concurrentExecution = concurrentExecution;
        this.enabled = true;
        this.cronSchedule = trigger.getCronExpression();
        this.startTime = trigger.getStartTime();
        this.endTime = trigger.getEndTime();
        this.timeZone = trigger.getTimeZone();
        this.invokeService = (String)map.get(ScheduledService.CONFIGURED_INVOKE_SERVICE);
        this.invokeLogLevel = (String)map.get(ScheduledService.CONFIGURED_INVOKE_LOG_LEVEL);
        this.invokeContext = map.get(ScheduledService.CONFIGURED_INVOKE_CONTEXT);
        this.scheduleType = SchedulerService.SCHEDULE_TYPE_CRON;
        int mp = trigger.getMisfireInstruction();
        if (mp == CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING) {
            this.misfirePolicy = SchedulerService.MISFIRE_POLICY_DO_NOTHING;
        } else {
            this.misfirePolicy = SchedulerService.MISFIRE_POLICY_FIRE_AND_PROCEED;
        }
    }

    public JsonValue getConfig() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(SchedulerService.SCHEDULE_ENABLED, getEnabled());
        map.put(SchedulerService.SCHEDULE_PERSISTED, getPersisted());
        map.put(SchedulerService.SCHEDULE_MISFIRE_POLICY, getMisfirePolicy());
        map.put(SchedulerService.SCHEDULE_CRON_SCHEDULE, getCronSchedule());
        map.put(SchedulerService.SCHEDULE_TYPE, getScheduleType());
        map.put(SchedulerService.SCHEDULE_INVOKE_SERVICE, getInvokeService());
        map.put(SchedulerService.SCHEDULE_INVOKE_CONTEXT, getInvokeContext());
        map.put(SchedulerService.SCHEDULE_INVOKE_LOG_LEVEL, getInvokeLogLevel());
        map.put(SchedulerService.SCHEDULE_TIME_ZONE, getTimeZone());
        map.put(SchedulerService.SCHEDULE_START_TIME, getStartTime());
        map.put(SchedulerService.SCHEDULE_END_TIME, getEndTime());
        map.put(SchedulerService.SCHEDULE_CONCURRENT_EXECUTION, getConcurrentExecution());
        return new JsonValue(map);
    }

    public Boolean getEnabled() {
        return enabled;
    }
    
    public Boolean getPersisted() {
        return persisted;
    }
    
    public String getMisfirePolicy() {
        return misfirePolicy;
    }
    
    public String getScheduleType() {
        return scheduleType;
    }
    
    public Date getStartTime() {
        return startTime;
    }
    
    public Date getEndTime() {
        return endTime;
    }
    
    public String getCronSchedule() {
        return cronSchedule;
    }
    
    public TimeZone getTimeZone() {
        return timeZone;
    }
    
    public String getInvokeService() {
        return invokeService;
    }
    
    public Object getInvokeContext() {
        return invokeContext;
    }
    
    public String getInvokeLogLevel() {
        return invokeLogLevel;
    }
    
    public Boolean getConcurrentExecution() {
        return concurrentExecution;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setPersisted(Boolean persisted) {
        this.persisted = persisted;
    }

    public void setMisfirePolicy(String misfirePolicy) {
        this.misfirePolicy = misfirePolicy;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setCronSchedule(String cronSchedule) {
        this.cronSchedule = cronSchedule;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public void setInvokeService(String invokeService) {
        this.invokeService = invokeService;
    }

    public void setInvokeContext(Object invokeContext) {
        this.invokeContext = invokeContext;
    }

    public void setInvokeLogLevel(String invokeLogLevel) {
        this.invokeLogLevel = invokeLogLevel;
    }
    
    public void setConcurrentExecution(Boolean concurrentExecution) {
        this.concurrentExecution = concurrentExecution;
    }
}
