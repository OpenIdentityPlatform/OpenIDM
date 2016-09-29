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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a scheduler that is persisted to the OpenIDM repo.
 */
public class PersistedScheduler extends AbstractScheduler {
    private static final Logger logger = LoggerFactory.getLogger(PersistedScheduler.class);

    private final ConnectionFactory connectionFactory;

    /**
     * Constructs a {@link PersistedScheduler} with a given {@link org.quartz.Scheduler Quartz Scheduler} and a
     *      {@link ConnectionFactory}.
     * @param scheduler a {@link org.quartz.Scheduler Quartz Scheduler}
     * @param connectionFactory a {@link ConnectionFactory}
     */
    PersistedScheduler(final org.quartz.Scheduler scheduler, final ConnectionFactory connectionFactory) {
        super(scheduler);
        this.connectionFactory = connectionFactory;
    }

    /**
     * Reads a trigger from the repo with a given trigger id.
     * @param context the {@link Context} in use when this method was called
     * @param triggerId the id of the trigger
     * @param trigger the {@link Trigger} object
     * @param instanceId the node instance id
     * @return a {@link JsonValue} representation of the trigger stored in the OpenIDM repo.
     * @throws SchedulerException if unable to read the trigger from the repo.
     */
    JsonValue getTrigger(final Context context, final String triggerId, final Trigger trigger, final String instanceId)
            throws SchedulerException {
        // Read trigger from the repo.
        try {
            final ResourceResponse triggerResponse =
                    connectionFactory.getConnection()
                            .read(context, Requests.newReadRequest("/scheduler/trigger", triggerId));
            return triggerResponse.getContent();
        } catch (final ResourceException e) {
            logger.info("Unable to read trigger: {}", triggerId, e);
            return json(object());
        }
    }
}
