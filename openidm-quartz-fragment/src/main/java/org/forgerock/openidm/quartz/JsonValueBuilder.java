/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.quartz;

import static org.quartz.CalendarIntervalScheduleBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.DailyTimeIntervalScheduleBuilder.*;
import static org.quartz.DateBuilder.*;
import static org.quartz.JobBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import static org.quartz.TriggerBuilder.*;


import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.Resource;
import org.quartz.Calendar;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ScheduleBuilder;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.jobs.NoOpJob;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.MutableTrigger;
import org.quartz.utils.Key;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public  class JsonValueBuilder {

    private static  ClassLoadHelper  classLoadHelper = new CascadingClassLoadHelper();

    public final static String SCHEDULE_MISFIRE_POLICY = "misfirePolicy";

    private static <T extends Trigger> void setMisfireHandlingInstruction(
            ScheduleBuilder<T> builder, JsonValue source) {
        if (source.isDefined(SCHEDULE_MISFIRE_POLICY)) {
            String methodName =
                    "withMisfireHandlingInstruction"
                            + source.get(SCHEDULE_MISFIRE_POLICY).asString();
            try {
                Method method = builder.getClass().getMethod(methodName);
                method.invoke(builder);
            } catch (NoSuchMethodException e) {
                throw new JsonValueException(source.get(SCHEDULE_MISFIRE_POLICY),
                        "Not supported misfireInstruction");
            } catch (InvocationTargetException e) {
                // This should not happen
                throw new JsonValueException(source.get(SCHEDULE_MISFIRE_POLICY), "Can not invoke "
                        + methodName);
            } catch (IllegalAccessException e) {
                // This should not happen
                throw new JsonValueException(source.get(SCHEDULE_MISFIRE_POLICY), "Can not invoke "
                        + methodName);
            }
        }
    }


    /**
     * <pre>
     *     {
     *         "name" : "AUTO",
     *         "group" : "DEFAULT",
     *         "description" : "",
     *         "jobClass" : "org.forgerock.openidm.quartz.ResourceJob.class",
     *         "durability" : false,
     *         "shouldRecover" : false,
     *         "jobData" : {}
     *     }
     * </pre>
     *
     * @param source
     * @return
     */
    public static JobDetail toJobDetail(JsonValue source) throws ClassNotFoundException {
        JobBuilder builder = newJob(classLoadHelper.loadClass(source.get("jobClass").required().asString(),Job.class));
        if (source.isDefined("name")) {
              builder.withIdentity(source.get("name").asString(), source.get("group").asString());
        } else {
            builder.withIdentity(Key.createUniqueName(null), source.get("group").asString());
        }
        builder.withDescription(source.get("description").asString());
        if (source.get("durability").asBoolean()) {
        builder.storeDurably();                    }
        if (source.get("shouldRecover").asBoolean()) {
            builder.requestRecovery();                    }

        if (source.isDefined("jobData") ) {
            builder.usingJobData(new JobDataMap(source.get("jobData").asMap()));
        }
        return builder.build();
    }


    public static Date toDate(JsonValue source) {
        return newDate().build();
    }



    /**
     * <pre>
     *     {
     *         "schedule" : {
     *             "type" : "cron"
     *         },
     *         "calendarName" : "Calendar Name",
     *         "description" : "description",
     *         "startTime" : "",
     *         "endTime" : "",
     *         "key" : "name-group",
     *         "priority" : 5,
     *         "jobData" : {
     *
     *         }
     *
     *     }
     * </pre>
     *
     * @param source
     * @return
     */
    public static <T extends Trigger> Trigger toTrigger(JsonValue source) {
        JsonValue schedule = source.get("schedule").required();
        String type = schedule.get("type").defaultTo("cron").asString();
        TriggerBuilder<Trigger> builder = newTrigger();
        if ("cron".equals(type)) {
            builder.withSchedule(toCronTrigger(source));
        } else if ("simple".equals(type)) {
            builder.withSchedule(toSimpleTrigger(source));
        } else if ("calendarInterval".equals(type)) {
            builder.withSchedule(toCalendarIntervalTrigger(source));
        } else if ("dailyTimeInterval".equals(type)) {
            builder.withSchedule(toDailyTimeIntervalTrigger(source));
        } else {
            throw new JsonValueException(schedule.get("type"),
                    "Type not in supported range['simple','cron','calendarInterval','dailyTimeInterval']"
                            + type);
        }

        JobKey key = null;

        builder.forJob(key);

        if (source.isDefined("calendarName")){
            builder.modifiedByCalendar(source.get("calendarName").required().asString());
        }
        if (source.isDefined("description")){
            builder.withDescription(source.get("description").required().asString());
        }
        if (source.isDefined("startTime")){
            builder.startAt(toDate(source.get("startTime").required()));
        }
        if (source.isDefined("endTime")){
            builder.endAt(toDate(source.get("endTime").required()));
        }

        if (source.isDefined("priority")){
            builder.withPriority(source.get("priority").required().asInteger());
        }
        if (source.isDefined("jobData")){
            builder.usingJobData(new JobDataMap(source.get("jobData").asMap()));
        }


        return builder.withSchedule(toSimpleTrigger(source)).build();
    }

    /**
     * <pre>
     *     {
     *         "interval" : "1 MINUTE",
     *         "repeatCount" : 5,
     *         "repeatForever" : null
     *     }
     * </pre>
     *
     * @param source
     * @return
     */
    public static SimpleScheduleBuilder toSimpleTrigger(JsonValue source) {
        SimpleScheduleBuilder builder = simpleSchedule();
        if (source.isDefined("repeatForever")) {
            builder.repeatForever();
        } else if (source.isDefined("repeatCount")) {
            builder.withRepeatCount(source.get("repeatCount").asInteger());
        }
        String[] interval = source.get("interval").required().asString().split(" ");
        if (interval.length != 2) {
            throw new JsonValueException(source.get("interval"),
                    "The interval value has incorrect syntax: " + interval);
        }

        try {
            IntervalUnit intervalUnit = IntervalUnit.valueOf(interval[1]);
            switch (intervalUnit) {
            case HOUR:
                builder.withIntervalInHours(Integer.parseInt(interval[0]));
                break;
            case MINUTE:
                builder.withIntervalInMinutes(Integer.parseInt(interval[0]));
                break;
            case SECOND:
                builder.withIntervalInSeconds(Integer.parseInt(interval[0]));
                break;
            case MILLISECOND:
                builder.withIntervalInMilliseconds(Integer.parseInt(interval[0]));
                break;
            }
        } catch (IllegalArgumentException e) {
            throw new JsonValueException(source.get("interval"), "Not valid IntervalUnit", e);
        }
        setMisfireHandlingInstruction(builder, source);
        return builder;
    }

    /**
     * <pre>
     *     {
     *         "cron" : "0 0/1 * * * ?",
     *         "timeZone" : "GMT"
     *     }
     * </pre>
     *
     * @param source
     * @return
     */
    public static  CronScheduleBuilder toCronTrigger(JsonValue source) {
        CronScheduleBuilder builder = cronSchedule(source.get("cron").required().asString());
        if (source.isDefined("timeZone")){
            builder.inTimeZone(TimeZone.getTimeZone(source.get("timeZone").required().asString()));
        }
        setMisfireHandlingInstruction(builder, source);
        return builder;
    }

    /**
     * <pre>
     *     {
     *         "name" : "AUTO",
     *         "group" : "DEFAULT",
     *         "interval" : "1 DAY",
     *         "misfirePolicy" : "doNothing",
     *         "timeZone" : "GMT",
     *         "preserveHourOfDayAcrossDaylightSavings" : false,
     *         "skipDayIfHourDoesNotExist" : false
     *     }
     * </pre>
     *
     * @param source
     * @return
     */
    public static  CalendarIntervalScheduleBuilder toCalendarIntervalTrigger(JsonValue source) {
        CalendarIntervalScheduleBuilder builder = calendarIntervalSchedule();
        if (source.isDefined("timeZone")){
            builder.inTimeZone(TimeZone.getTimeZone(source.get("timeZone").required().asString()));
        }
        if (source.isDefined("preserveHourOfDayAcrossDaylightSavings")){
            builder.preserveHourOfDayAcrossDaylightSavings(source.get("preserveHourOfDayAcrossDaylightSavings").required().asBoolean());
        }
        if (source.isDefined("skipDayIfHourDoesNotExist")){
            builder.skipDayIfHourDoesNotExist(source.get("skipDayIfHourDoesNotExist").required().asBoolean());
        }
        String[] interval = source.get("interval").required().asString().split(" ");
        if (interval.length != 2) {
            throw new JsonValueException(source.get("interval"),
                    "The interval value has incorrect syntax: " + interval);
        }
        try {
            setMisfireHandlingInstruction(builder, source);
            return builder.withInterval(
                    Integer.parseInt(interval[0]), IntervalUnit.valueOf(interval[1]));
        } catch (IllegalArgumentException e) {
            throw new JsonValueException(source.get("interval"), "Not valid IntervalUnit", e);
        }
    }

    /**
     * <pre>
     *     {
     *         "interval" : "1 MINUTE",
     *         "repeatCount" : 5,
     *         "repeatForever" : null
     *     }
     * </pre>
     *
     * @param source
     * @return
     */
    public static DailyTimeIntervalScheduleBuilder toDailyTimeIntervalTrigger(JsonValue source) {
        DailyTimeIntervalScheduleBuilder builder = dailyTimeIntervalSchedule();
        if (source.isDefined("repeatCount")) {
            builder.withRepeatCount(source.get("repeatCount").asInteger());
        }
        setMisfireHandlingInstruction(builder, source);
        return builder;
    }

    // -- Reverse direction

    /**
     * <pre>
     *     {
     *         "name" : "AUTO",
     *         "group" : "DEFAULT",
     *         "description" : "",
     *         "jobClass" : "org.forgerock.openidm.quartz.ResourceJob.class",
     *         "durability" : false,
     *         "shouldRecover" : false,
     *         "jobData" : {}
     *     }
     * </pre>
     * 
     * @param source
     * @return
     */
    public static JsonValue fromJobDetail(JobDetail source) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>());

        return result;
    }


    public static JsonValue fromDate(Date source) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>());
        return result;
    }

    public static JsonValue fromTrigger(Trigger source) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>());
        return result;
    }

    public static JsonValue fromSimpleTrigger(SimpleTrigger source) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>());
        return result;
    }


    /**
     * <pre>
     *     {
     *         "name" : "AUTO",
     *         "group" : "DEFAULT",
     *         "interval" : "1",
     *         "intervalUnit" : "D",
     *         "misfirePolicy" : "doNothing",
     *         "timeZone" : "GMT",
     *         "preserveHourOfDayAcrossDaylightSavings" : false,
     *         "skipDayIfHourDoesNotExist" : false
     *     }
     * </pre>
     *
     * @param source
     * @return
     */
    public static JsonValue fromCronTrigger(CronTrigger source) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>());
        return result;
    }


    /**
     * <pre>
     *     {
     *         "name" : "AUTO",
     *         "group" : "DEFAULT",
     *         "interval" : "1",
     *         "intervalUnit" : "D",
     *         "misfirePolicy" : "doNothing",
     *         "timeZone" : "GMT",
     *         "preserveHourOfDayAcrossDaylightSavings" : false,
     *         "skipDayIfHourDoesNotExist" : false
     *     }
     * </pre>
     *
     * @param source
     * @return
     */
    public static JsonValue fromCalendarIntervalTrigger(CalendarIntervalTrigger source) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>());
        result.put("name",source.getKey().getName());
        result.put("group",source.getKey().getGroup());
        result.put("interval",source.getRepeatInterval());
        result.put("intervalUnit",source.getRepeatIntervalUnit().name());
        result.put("misfirePolicy",source.getKey().getGroup());
        result.put("timeZone",source.getTimeZone().getID());
        result.put("preserveHourOfDayAcrossDaylightSavings", false);
        result.put("skipDayIfHourDoesNotExist",source.getKey().getGroup());
        return result;
    }

    public static JsonValue fromDailyTimeIntervalTrigger(DailyTimeIntervalTrigger source) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>());
        return result;
    }

    public void put(final JsonValue result, JsonPointer pointer, Object object)
            throws JsonValueException {
        JsonValue jv = result;
        String[] tokens = pointer.toArray();
        for (int n = 0; n < tokens.length - 1; n++) {
            jv = jv.get(tokens[n]);
            if (jv.isNull()) {
                jv.put(tokens[n], new LinkedHashMap<String, Object>());
            }
            jv = jv.get(tokens[n]).required();
        }
        jv.put(tokens[tokens.length - 1], object);
    }
}
