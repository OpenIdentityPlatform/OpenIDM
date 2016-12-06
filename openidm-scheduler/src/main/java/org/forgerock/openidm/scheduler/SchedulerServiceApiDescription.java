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

import static org.forgerock.api.commons.CommonsApi.Errors.BAD_REQUEST;
import static org.forgerock.api.commons.CommonsApi.Errors.NOT_FOUND;
import static org.forgerock.api.commons.CommonsApi.Errors.VERSION_MISMATCH;

import java.util.Map;

import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.ApiError;
import org.forgerock.api.models.Create;
import org.forgerock.api.models.Delete;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Query;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.SubResources;
import org.forgerock.api.models.Update;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.openidm.scheduler.api.ScheduleConfigResource;
import org.forgerock.openidm.scheduler.api.SuccessResponse;
import org.forgerock.openidm.scheduler.api.TriggerResource;
import org.forgerock.openidm.scheduler.api.ValidateQuartzCronExpressionRequest;
import org.forgerock.openidm.scheduler.api.ValidateQuartzCronExpressionResponse;
import org.forgerock.openidm.util.JsonUtil;

/**
 * {@link ApiDescription} builder for {@link SchedulerService}.
 */
class SchedulerServiceApiDescription {

    private static final String INSTANCE_ID_PATH = "/{instanceId}";

    private static final ApiError versionMismatchError = ApiError.apiError()
            .reference(Reference.reference()
                    .value(VERSION_MISMATCH.getReference())
                    .build())
            .build();
    private static final ApiError badRequestError = ApiError.apiError()
            .reference(Reference.reference()
                    .value(BAD_REQUEST.getReference())
                    .build())
            .build();
    private static final ApiError notFoundError = ApiError.apiError()
            .reference(Reference.reference()
                    .value(NOT_FOUND.getReference())
                    .build())
            .build();
    private static final Schema triggerListResource =
            Schema.schema().schema(
                    JsonUtil.parseURL(SchedulerServiceApiDescription.class.getResource("api/triggerListResource.json"))
            ).build();

    private SchedulerServiceApiDescription() {
        // empty
    }

    /**
     * Builds {@link ApiDescription}.
     *
     * @return {@link ApiDescription} instance or {@code null} if a fatal error occurred and was logged
     */
    static ApiDescription build() {
        final SubResources subResources = SubResources.subresources()
                .put(JobRequestHandler.JOB_RESOURCE_PATH, buildJobRequestHandlerResource())
                .put(JobRequestHandler.JOB_RESOURCE_PATH + INSTANCE_ID_PATH,
                        buildJobRequestHandlerForInstancesResource())
                .put(TriggerRequestHandler.TRIGGER_RESOURCE_PATH, buildTriggerRequestHandlerResource())
                .put(TriggerRequestHandler.TRIGGER_RESOURCE_PATH + INSTANCE_ID_PATH,
                        buildTriggerRequestHandlerForInstancesResource())
                .put(RepoProxyRequestHandler.ACQUIRED_TRIGGERS_RESOURCE_PATH, Resource.resource()
                        .title("Scheduler - Acquired Triggers")
                        .description("Returns an array of the triggers that have been acquired, per node.")
                        .mvccSupported(false)
                        .resourceSchema(triggerListResource)
                        .read(Read.read()
                                .description("Queries the repo for acquired triggers.")
                                .build())
                        .build())
                .put(RepoProxyRequestHandler.WAITING_TRIGGERS_RESOURCE_PATH, Resource.resource()
                        .title("Scheduler - Waiting Triggers")
                        .description("Returns an array of the triggers that have not yet been acquired.")
                        .mvccSupported(false)
                        .resourceSchema(triggerListResource)
                        .read(Read.read()
                                .description("Queries the repo for waiting triggers.")
                                .build())
                        .build())
                .build();

        return ApiDescription.apiDescription()
                .id("temp")
                .version("0")
                .paths(Paths.paths()
                        .put("/", VersionedPath.versionedPath()
                                .put(VersionedPath.UNVERSIONED, Resource.resource()
                                        .title("Scheduler")
                                        .description(
                                                "Exposes Quartz job scheduling as well as trigger "
                                                        + "configurations/statistics.")
                                        .mvccSupported(false)
                                        .action(Action.action()
                                                .name("validateQuartzCronExpression")
                                                .description(
                                                        "Validates a cron expression.")
                                                .request(Schema.schema()
                                                        .type(ValidateQuartzCronExpressionRequest.class)
                                                        .build())
                                                .response(Schema.schema()
                                                        .type(ValidateQuartzCronExpressionResponse.class)
                                                        .build())
                                                .build())
                                        .subresources(subResources)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private static Resource buildTriggerRequestHandlerResource() {
        return Resource.resource()
                .title("Scheduler - Triggers")
                .description("Provides access to query for Quartz Triggers.")
                .mvccSupported(false)
                .resourceSchema(Schema.schema()
                        .type(TriggerResource.class)
                        .build())
                .query(Query.query()
                        .description("Queries the existing triggers.")
                        .type(QueryType.FILTER)
                        .error(badRequestError)
                        .build())
                .build();
    }

    private static Resource buildTriggerRequestHandlerForInstancesResource() {
        return Resource.resource()
                .title("Scheduler - Trigger Instance")
                .description("Provides read ability for Quartz Triggers.")
                .mvccSupported(false)
                .parameter(Parameter.parameter()
                        .name("instanceId")
                        .type("String")
                        .source(ParameterSource.PATH)
                        .description("ID of the trigger.")
                        .build())
                .resourceSchema(Schema.schema()
                        .type(TriggerResource.class)
                        .build())
                .read(Read.read()
                        .description("Obtains the details of the specified trigger.")
                        .error(badRequestError)
                        .error(notFoundError)
                        .build())
                .build();
    }

    private static Resource buildJobRequestHandlerForInstancesResource() {
        return Resource.resource()
                .title("Scheduler - Job Instance")
                .description("Provides access to Job creation, statistics, and manipulation.")
                .mvccSupported(false)
                .parameter(Parameter.parameter()
                        .name("instanceId")
                        .type("String")
                        .source(ParameterSource.PATH)
                        .description("ID of the job.")
                        .build())
                .resourceSchema(Schema.schema()
                        .type(ScheduleConfigResource.class)
                        .build())
                .create(Create.create()
                        .description(
                                "Creates or updates a schedule with the specified ID.")
                        .error(versionMismatchError)
                        .mode(CreateMode.ID_FROM_CLIENT)
                        .build())
                .read(Read.read()
                        .description("Obtains the details of the specified schedule.")
                        .error(badRequestError)
                        .error(notFoundError)
                        .build())
                .update(Update.update()
                        .description("Update the job schedule.")
                        .error(badRequestError)
                        .error(notFoundError)
                        .build())
                .delete(Delete.delete()
                        .description("Deletes the specified schedule.")
                        .error(badRequestError)
                        .error(notFoundError)
                        .build())
                .build();
    }

    private static Resource buildJobRequestHandlerResource() {
        return Resource.resource()
                .title("Scheduler - Jobs")
                .description("Provides access to Job creation, statistics, and manipulation.")
                .mvccSupported(false)
                .resourceSchema(Schema.schema()
                        .type(ScheduleConfigResource.class)
                        .build())
                .query(Query.query()
                        .description(
                                "Queries the existing defined schedules.")
                        .type(QueryType.FILTER)
                        .error(badRequestError)
                        .build())
                .action(Action.action()
                        .name("create")
                        .description("Creates a schedule with a system-generated ID.")
                        .request(Schema.schema()
                                .type(ScheduleConfigResource.class)
                                .build())
                        .response(Schema.schema()
                                .type(ScheduleConfigResource.class)
                                .build())
                        .build())
                .action(Action.action()
                        .name("listCurrentlyExecutingJobs")
                        .description("Returns a list of the jobs that are currently running.")
                        .response(Schema.schema()
                                .type(ScheduleConfigResource[].class)
                                .build())
                        .build())
                .action(Action.action()
                        .name("pauseJobs")
                        .description("Suspends all scheduled jobs.")
                        .response(Schema.schema()
                                .type(SuccessResponse.class)
                                .build())
                        .build())
                .action(Action.action()
                        .name("resumeJobs")
                        .description("Resumes all suspended scheduled jobs.")
                        .response(Schema.schema()
                                .type(SuccessResponse.class)
                                .build())
                        .build())
                .build();
    }
}
