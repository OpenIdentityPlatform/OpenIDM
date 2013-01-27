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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.PersistenceConfig;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
//import org.forgerock.openidm.util.LogUtil;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ResourceJob invokes a {@link Request} with given {@link ServerContext}.
 * 
 * @author Laszlo Hordos
 */
//@PersistJobDataAfterExecution
//@DisallowConcurrentExecution
public class ResourceJob implements Job {

    /**
     * Setup logging for the {@link ResourceJob}.
     */
    final static Logger logger = LoggerFactory.getLogger(ResourceJob.class);

    // Default to INFO
    //private LogUtil.LogLevel logLevel = LogUtil.LogLevel.INFO;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();

        if (data.get(CommonJobFactory.PERSISTENCE_CONFIG) instanceof PersistenceConfig) {
            throw new JobExecutionException("Failed to get the required property: "
                    + CommonJobFactory.PERSISTENCE_CONFIG);
        }
        PersistenceConfig persistenceConfig =
                (PersistenceConfig) data.get(CommonJobFactory.PERSISTENCE_CONFIG);

        String invokeLogLevel = data.getString("scheduler.invokeLogLevel");
        //logLevel = LogUtil.asLogLevel(invokeLogLevel);

        // Restore Context
        JsonValue savedContext = (JsonValue) data.get("scheduler.invokeContext");
        JsonValue savedRequest = (JsonValue) data.get("scheduler.invokeRequest");

        try {
            ServerContext serverContext =
                    ServerContext.loadFromJson(savedContext, persistenceConfig);

            // Matt adds the method to restore the request
            // Request request = Requests.loadFromJson(savedRequest);
            Request request = Requests.newActionRequest("/test", "test");
            try {


                //LogUtil.logAtLevel(logger, logLevel, "Scheduled service \"{}\" found, invoking.", context.getJobDetail().getDescription());
                if (request instanceof ActionRequest) {

                    //Shall we wait for the response
                    if (context.getJobDetail().isPersistJobDataAfterExecution()) {
                        serverContext.getConnection()
                                .action(serverContext, (ActionRequest) request);
                    } else {
                        serverContext.getConnection().actionAsync(serverContext,
                                (ActionRequest) request, new ResultHandler<JsonValue>() {
                                    @Override
                                    public void handleError(ResourceException error) {

                                    }

                                    @Override
                                    public void handleResult(JsonValue result) {

                                    }
                                });
                    }
                } else {
                    // TODO add them later
                }
                //LogUtil.logAtLevel(logger, logLevel, "Scheduled service \"{}\" invoke completed successfully.", context .getJobDetail().getDescription());
            } catch (ResourceException e) {
                logger.error("Failed load ServerContext", e);
                throw new JobExecutionException(e);
            }

        } catch (ResourceException e) {
            logger.error("Failed load ServerContext", e);
            throw new JobExecutionException(e);
        }
    }
}
