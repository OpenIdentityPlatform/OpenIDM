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
package org.forgerock.openidm.scheduler;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.quartz.impl.TriggerWrapper;
import org.forgerock.services.context.Context;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 * Creates an in-memory {@link Scheduler}.
 */
public class MemoryScheduler extends AbstractScheduler {

    /**
     * Constructs the in-memory {@link Scheduler} with a given {@link org.quartz.Scheduler Quartz Scheduler}.
     * @param scheduler a {@link org.quartz.Scheduler Quartz Scheduler}
     */
    MemoryScheduler(final org.quartz.Scheduler scheduler) {
        super(scheduler);
    }

    /**
     * Gets a {@link JsonValue} representation of the given {@link Trigger}.
     * @param context the {@link Context} in use when this method was called
     * @param triggerId the id of the trigger
     * @param trigger the {@link Trigger} object
     * @param instanceId the node instance id
     * @return a {@link JsonValue} representation of the given {@link Trigger}
     * @throws SchedulerException if unable to create a {@link JsonValue} representation of the {@link Trigger}
     */
    JsonValue getTrigger(final Context context, final String triggerId, final Trigger trigger, final String instanceId)
            throws SchedulerException {
        // Can't read non-persisted trigger from the repo so create it from the trigger object.
        final boolean isAcquired = isAcquired(trigger);
        return new TriggerWrapper(trigger, isPaused(trigger), isAcquired, isAcquired ? instanceId : null).getValue();
    }
}
