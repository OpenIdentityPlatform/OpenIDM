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

package org.forgerock.openidm.scheduler.impl;

import org.forgerock.openidm.scheduler.ScheduleConfig;
import org.quartz.CronTrigger;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import java.text.ParseException;
import java.util.Calendar;

import static org.forgerock.openidm.scheduler.SchedulerService.GROUP_NAME;

/**
 * @see TriggerFactory
 */
public enum TriggerType implements TriggerFactory {
    /** Creates a Quartz Trigger which fires according to a cron schedule  */
    cron {
        @Override
        public Trigger newTrigger(ScheduleConfig scheduleConfig, String triggerName) throws SchedulerException {
            try {
                final CronTrigger cronTrigger =
                        new CronTrigger(TRIGGER_NAME_PREFACE + triggerName, GROUP_NAME, scheduleConfig.getCronSchedule());
                if (scheduleConfig.getTimeZone() != null) {
                    cronTrigger.setTimeZone(scheduleConfig.getTimeZone());
                }
                if (scheduleConfig.getMisfirePolicy().equals(MISFIRE_POLICY_FIRE_AND_PROCEED)) {
                    cronTrigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
                } else if (scheduleConfig.getMisfirePolicy().equals(MISFIRE_POLICY_DO_NOTHING)) {
                    cronTrigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
                }
                TriggerType.setCommonTriggerState(cronTrigger, scheduleConfig, triggerName);
                return cronTrigger;
            } catch (ParseException e) {
                throw new SchedulerException(e.getMessage(), e);
            }
        }
    },
    /** Creates a Quartz Trigger which fires a specified number of times  */
    simple {
        @Override
        public Trigger newTrigger(ScheduleConfig scheduleConfig, String triggerName) throws SchedulerException {
            final SimpleTrigger simpleTrigger = new SimpleTrigger(TRIGGER_NAME_PREFACE + triggerName, GROUP_NAME);
            if (scheduleConfig.getStartTime() == null) {
                // A simple-trigger with no start-time will fire immediately, and will fire and be removed
                // from the JobStore before it can be re-read and returned in the JobRequestHandler#handleCreate
                // method, as it appears that Scheduler#scheduleJob can be effectively synchronous. Some additional
                // latency may heighten the chances that this job can be picked-up by a different cluster node.
                Calendar fireTime = Calendar.getInstance();
                fireTime.add(Calendar.SECOND, SIMPLE_TRIGGER_FIRE_LATENCY_CONSTANT);
                simpleTrigger.setStartTime(fireTime.getTime());
            }
            simpleTrigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
            TriggerType.setCommonTriggerState(simpleTrigger, scheduleConfig, triggerName);
            return simpleTrigger;
        }
    };

    private static final String TRIGGER_NAME_PREFACE = "trigger-";
    private static final String MISFIRE_POLICY_DO_NOTHING = "doNothing";
    private static final String MISFIRE_POLICY_FIRE_AND_PROCEED = "fireAndProceed";
    private static final int SIMPLE_TRIGGER_FIRE_LATENCY_CONSTANT = 3;

    private static void setCommonTriggerState(Trigger trigger, ScheduleConfig scheduleConfig, String triggerName) {
        trigger.setJobName(triggerName);
        trigger.setJobGroup(GROUP_NAME);

        if (scheduleConfig.getStartTime() != null) {
            trigger.setStartTime(scheduleConfig.getStartTime()); // TODO: review time zone consistency with cron trigger timezone
        }

        if (scheduleConfig.getEndTime() != null) {
            trigger.setEndTime(scheduleConfig.getEndTime());
        }
    }
}
