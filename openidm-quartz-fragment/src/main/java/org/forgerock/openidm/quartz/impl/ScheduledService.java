/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012-1014 ForgeRock AS. All Rights Reserved
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

import java.util.Map;

import org.forgerock.json.resource.ServerContext;

/**
 * OSGi services wanting to be schedulable via the Scheduler service must
 * implement this interface.
 *
 */

public interface ScheduledService {

    // Reserved keys for the ScheduledService execution context map
    final static String CONFIG_NAME = "scheduler.config-name";
    final static String INVOKER_NAME = "scheduler.invoker-name";
    final static String SCHEDULED_FIRE_TIME = "scheduler.scheduled-fire-time";
    final static String ACTUAL_FIRE_TIME = "scheduler.actual-fire-time";
    final static String NEXT_FIRE_TIME = "scheduler.next-fire-time";
    final static String CONFIGURED_INVOKE_SERVICE = "scheduler.invokeService";
    final static String CONFIGURED_INVOKE_CONTEXT = "scheduler.invokeContext";
    final static String CONFIGURED_INVOKE_LOG_LEVEL = "scheduler.invokeLogLevel";

    /**
     * Invoked by the scheduler when the scheduler triggers.
     *
     * @param context the ServerContext to use for the request
     * @param scheduledContext Context information passed by the scheduler service
     * @throws ExecutionException if execution of the scheduled work failed.
     * Implementations can also throw RuntimeExceptions which will get logged.
     */
    void execute(ServerContext context, Map<String, Object> scheduledContext) throws ExecutionException;
}
